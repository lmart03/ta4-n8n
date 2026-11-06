<#
.SYNOPSIS
  Lanzador local del robot de UiPath: ejecuta el robot cuando la web pulsa "Empezar flujo".

.DESCRIPTION
  El navegador no puede lanzar UiPath y el backend corre en Docker, que no alcanza este Windows. Este script
  hace de puente SIN abrir ningún puerto: cada pocos segundos pregunta al backend si hay un arranque pedido
  (GET /api/v1/ruc/robot/pending-start), lo toma (POST .../claim, así dos lanzadores no ejecutan lo mismo) y corre
  el robot con el id del trabajo. El robot informa su avance al backend por su cuenta.

  Déjalo abierto en una terminal mientras uses la web. Ctrl+C lo detiene.

.PARAMETER ApiBase
  Base de la API de RUC. Por defecto http://localhost:8080/api/v1/ruc

.PARAMETER ProjectDir
  Carpeta del proyecto de UiPath (la que tiene project.json). Por defecto rpa\RobotSunat del repositorio.

.PARAMETER PollSeconds
  Cada cuántos segundos revisa la cola.

.PARAMETER StartTimeoutSec
  Si el robot no llega a informar "EN_CONSULTA" en este tiempo (no abrió Edge o se quedó colgado arrancando), se
  corta y se reintenta.

.PARAMETER MaxAttempts
  Cuántas veces se ejecuta el robot por trabajo antes de rendirse (después entra el respaldo por API).

.PARAMETER SkipBuild
  No compila el proyecto al iniciar (úsalo si ya lo compilaste desde Studio).
#>
param(
  [string]$ApiBase = 'http://localhost:8080/api/v1/ruc',
  # Por defecto el proyecto que viene en el repositorio (rpa\RobotSunat); si no está, el de Documentos\UiPath.
  [string]$ProjectDir = $(foreach ($candidate in @((Join-Path $PSScriptRoot '..\rpa\RobotSunat'), (Join-Path ([Environment]::GetFolderPath('MyDocuments')) 'UiPath\RobotSunat'))) {
      if (Test-Path -LiteralPath (Join-Path $candidate 'project.json')) { (Resolve-Path -LiteralPath $candidate).Path; break }
    }),
  [int]$PollSeconds = 2,
  [int]$StartTimeoutSec = 50,
  [int]$MaxAttempts = 2,
  [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$GuidPattern = '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'

function Log([string]$message, [string]$color = 'Gray') {
  Write-Host ("[{0:HH:mm:ss}] {1}" -f (Get-Date), $message) -ForegroundColor $color
}

function Send-Status([string]$jobId, [string]$status, [string]$message) {
  try {
    $body = @{ status = $status; message = $message } | ConvertTo-Json -Compress
    Invoke-RestMethod -Method Post -Uri "$ApiBase/jobs/$jobId/status" -ContentType 'application/json; charset=utf-8' `
      -Body ([Text.Encoding]::UTF8.GetBytes($body)) | Out-Null
  } catch {
    Log "No pude informar el estado $status al backend: $($_.Exception.Message)" 'Yellow'
  }
}

# Un solo lanzador por usuario: si ya hay uno corriendo, este sale sin hacer nada.
$createdNew = $false
$mutex = [Threading.Mutex]::new($true, 'Local\TA4-lanzador-robot', [ref]$createdNew)
if (-not $createdNew) { Write-Host 'Ya hay un lanzador en marcha; no hace falta otro.'; exit 0 }

Add-Type -Namespace Win -Name Native -MemberDefinition @'
[DllImport("user32.dll")] public static extern bool IsIconic(IntPtr hWnd);
[DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
'@

# Una ventana de Edge de la consulta que quedó minimizada de una corrida anterior hace que el robot no vea la página
# (el robot se engancha a ella en vez de abrir otra). Restaurarla no cierra nada ni toca otras pestañas.
function Restore-MinimizedSunatWindows {
  Get-Process msedge -ErrorAction SilentlyContinue |
    Where-Object { $_.MainWindowHandle -ne 0 -and $_.MainWindowTitle -match 'RUC|SUNAT' } |
    ForEach-Object {
      if ([Win.Native]::IsIconic($_.MainWindowHandle)) {
        [void][Win.Native]::ShowWindow($_.MainWindowHandle, 9)  # SW_RESTORE
        Log 'Restauré una ventana minimizada de la consulta en Edge.' 'DarkGray'
      }
    }
}

# Ventanas de Edge que muestran la página de rechazo del firewall de SUNAT (por su identificador de ventana).
function Get-RejectedWindows {
  @(Get-Process msedge -ErrorAction SilentlyContinue |
    Where-Object { $_.MainWindowTitle -match 'Request Rejected|URL was rejected' } | ForEach-Object { $_.MainWindowHandle })
}

# ¿Hay una ventana de rechazo NUEVA (que no estuviera ya abierta antes de arrancar el robot)?
function Test-SunatRejected([object[]]$ignore = @()) {
  [bool](Get-RejectedWindows | Where-Object { $ignore -notcontains $_ } | Select-Object -First 1)
}

function Stop-Robot($runner) {
  taskkill /PID $runner.Id /T /F 2>&1 | Out-Null
  uip rpa execution cancel --project-dir $projectDirCli --output json 2>&1 | Out-Null
}

# Una ejecución del robot. Devuelve @{ Outcome = ok | rejected | stuck | failed; Detail = texto }.
#   rejected: el firewall de SUNAT bloqueó el acceso (reintentar no sirve).
#   stuck:    el robot no informó "EN_CONSULTA" a tiempo (no abrió Edge o se colgó arrancando).
#   failed:   el robot se detuvo con un error en una actividad.
function Invoke-RobotAttempt([string]$jobId, [string]$argsFile, [int]$attempt) {
  Restore-MinimizedSunatWindows
  $outFile = Join-Path ([IO.Path]::GetTempPath()) "ta4-robot-$jobId-$attempt.out"
  try {
    # El robot corre en un proceso aparte para seguir dando señal de vida a la web mientras trabaja (si no, la
    # pantalla diría "Robot desconectado" justo cuando está consultando).
    $runner = Start-Process -FilePath 'cmd.exe' -WindowStyle Hidden -PassThru -ArgumentList '/c',
      "uip rpa debug start --file-path Main.xaml --project-dir `"$projectDirCli`" --skip-build --input-arguments-file `"$($argsFile.Replace('\', '/'))`" --output json > `"$outFile`" 2>&1"
    $startedAt = Get-Date
    $reached = $false
    $staleRejections = @(Get-RejectedWindows)   # pestañas de rechazo viejas: no cuentan
    while (-not $runner.HasExited) {
      try { Invoke-RestMethod -Method Get -Uri "$ApiBase/robot/pending-start" | Out-Null } catch { }
      # Si SUNAT rechaza el envío, su página de error es inconfundible: no tiene sentido esperar a que el robot
      # agote su espera del enlace del ZIP. Se corta de inmediato para que entre el respaldo sin demora.
      if (Test-SunatRejected $staleRejections) {
        Stop-Robot $runner
        return @{ Outcome = 'rejected'; Detail = 'SUNAT rechazó el acceso del robot (The requested URL was rejected). Cierra las ventanas de Edge de la Consulta Múltiple y reintenta.' }
      }
      if (-not $reached) {
        try { $status = (Invoke-RestMethod -Method Get -Uri "$ApiBase/jobs/$jobId").status } catch { $status = $null }
        if ($status -and $status -ne 'LISTO_PARA_ROBOT') { $reached = $true }
        elseif (((Get-Date) - $startedAt).TotalSeconds -gt $StartTimeoutSec) {
          Stop-Robot $runner
          return @{ Outcome = 'stuck'; Detail = "El robot no llegó a abrir la consulta en SUNAT en $StartTimeoutSec s." }
        }
      }
      Start-Sleep -Seconds $PollSeconds
    }
    $output = (Get-Content -LiteralPath $outFile -Raw -Encoding UTF8) | Out-String
    Log ($output.Trim()) 'DarkGray'
    if ($output -match '"hasErrors":\s*true' -or $output -match 'Suspended') {
      # El depurador suspende el robot ante la excepción, así que su propio "catch" no llega a avisar al backend.
      $activity = if ($output -match '"Activity":"([^"\\]+)') { $Matches[1] } else { 'una actividad' }
      $kind = if ($output -match '"ExceptionType":"([^"\\]+)') { ($Matches[1] -split '\.')[-1] } else { 'error' }
      # Lo que mostraba Edge al fallar delata un bloqueo de SUNAT («The requested URL was rejected»...).
      $titles = @(Get-Process msedge -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowTitle } | ForEach-Object { $_.MainWindowTitle -replace ' - Personal.*$| - Microsoft.*Edge$', '' } |
        Select-Object -Unique)
      $seen = if ($titles.Count -gt 0) { " Edge mostraba: «$($titles -join '» / «')»." } else { '' }
      Stop-Robot $runner
      return @{ Outcome = 'failed'; Detail = "El robot se detuvo en «$activity» ($kind).$seen"; Activity = $activity }
    }
    return @{ Outcome = 'ok'; Detail = '' }
  } finally {
    Remove-Item -LiteralPath $outFile -Force -ErrorAction SilentlyContinue
  }
}

if (-not (Get-Command uip -ErrorAction SilentlyContinue)) {
  throw "No encuentro el comando 'uip'. Instálalo con: npm install -g @uipath/cli @uipath/rpa-tool (ver docs/GUIA_EJECUCION.md) y abre una terminal nueva."
}
if (-not (Test-Path -LiteralPath (Join-Path $ProjectDir 'project.json'))) {
  throw "No encuentro el proyecto de UiPath en '$ProjectDir'. Indica la carpeta con -ProjectDir."
}
$projectDirCli = $ProjectDir.Replace('\', '/')

if (-not $SkipBuild) {
  Log "Compilando el robot (una vez)..." 'Cyan'
  uip rpa build $projectDirCli --output json | Out-Null
  if ($LASTEXITCODE -ne 0) { throw 'La compilación del robot falló. Ábrelo en Studio y revisa los errores.' }
}

Log "Lanzador listo. Revisando $ApiBase cada $PollSeconds s. Ctrl+C para detener." 'Green'
$lastHealthWarning = [datetime]::MinValue

while ($true) {
  try {
    # Con una lista vacía, PowerShell 5.1 devuelve $null y @($null) tendría 1 elemento: se filtran los vacíos.
    $pending = @(Invoke-RestMethod -Method Get -Uri "$ApiBase/robot/pending-start") | Where-Object { $_ -and $_.id }
  } catch {
    if (((Get-Date) - $lastHealthWarning).TotalSeconds -gt 30) {
      Log "El backend no responde en $ApiBase ($($_.Exception.Message)). Sigo intentando..." 'Yellow'
      $lastHealthWarning = Get-Date
    }
    Start-Sleep -Seconds $PollSeconds
    continue
  }

  foreach ($job in $pending) {
    $jobId = [string]$job.id
    if ($jobId -notmatch $GuidPattern) { Log "Ignoro un id inválido: '$jobId'" 'Yellow'; continue }

    try {
      Invoke-RestMethod -Method Post -Uri "$ApiBase/robot/jobs/$jobId/claim" | Out-Null
    } catch {
      Log "El trabajo $jobId ya lo tomó otro lanzador." 'DarkGray'
      continue
    }

    Log "Arranque pedido para el trabajo $jobId. Ejecutando el robot..." 'Cyan'
    $argsFile = Join-Path ([IO.Path]::GetTempPath()) "ta4-robot-$jobId.json"
    [IO.File]::WriteAllText($argsFile, (@{ in_JobId = $jobId } | ConvertTo-Json -Compress), [Text.UTF8Encoding]::new($false))
    try {
      $result = @{ Outcome = 'failed'; Detail = 'El robot no se ejecutó.' }
      for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        if ($attempt -gt 1) { Log "Reintentando el robot (intento $attempt de $MaxAttempts)..." 'Yellow' }
        $result = Invoke-RobotAttempt $jobId $argsFile $attempt
        # Un rechazo del firewall no se arregla reintentando; un éxito tampoco necesita otro intento.
        if ($result.Outcome -eq 'ok' -or $result.Outcome -eq 'rejected') { break }
        Log "Intento $attempt sin éxito: $($result.Detail)" 'Yellow'
        if ($attempt -lt $MaxAttempts) { Start-Sleep -Seconds 2 }
      }
      switch ($result.Outcome) {
        'ok' { Log "Robot terminado para el trabajo $jobId." 'Green' }
        'rejected' {
          Log "SUNAT rechazó el acceso del robot en el trabajo $jobId; se corta la espera." 'Red'
          Send-Status $jobId 'FALLIDO' $result.Detail
        }
        default {
          Log "El robot falló en el trabajo $jobId tras $MaxAttempts intento(s): $($result.Detail)" 'Red'
          Send-Status $jobId 'FALLIDO' "$($result.Detail) Se reintentó $MaxAttempts veces. Cierra las ventanas de Edge de la Consulta Múltiple y reintenta."
        }
      }
    } catch {
      Log "No se pudo ejecutar el robot: $($_.Exception.Message)" 'Red'
      Send-Status $jobId 'FALLIDO' "El lanzador no pudo ejecutar el robot: $($_.Exception.Message)"
    } finally {
      # Si el robot quedó suspendido por una excepción, se cancela para dejar la sesión limpia.
      uip rpa execution cancel --project-dir $projectDirCli --output json 2>&1 | Out-Null
      Remove-Item -LiteralPath $argsFile -Force -ErrorAction SilentlyContinue
    }
  }
  Start-Sleep -Seconds $PollSeconds
}
