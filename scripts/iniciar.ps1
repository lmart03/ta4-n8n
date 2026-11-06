<#
.SYNOPSIS
  Un solo comando para dejar todo listo: contenedores, lanzador del robot (oculto) y la web.

.DESCRIPTION
  1. Levanta los contenedores si no están (docker compose up -d).
  2. Espera a que el backend responda.
  3. Inicia el lanzador del robot en segundo plano, sin ventana (registro en %LOCALAPPDATA%\TA4-RUC\lanzador.log).
  4. Abre la web en http://localhost:4280.

  -Instalar  además registra el lanzador para que arranque solo al iniciar sesión en Windows (tarea programada
             sin ventana). Con Docker Desktop abierto al iniciar sesión, los contenedores ya reinician solos
             (restart: unless-stopped), así que después de instalarlo no hay que ejecutar nada más.
  -Desinstalar quita esa tarea.
#>
param(
  [switch]$Instalar,
  [switch]$Desinstalar,
  [switch]$SinNavegador
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$launcher = Join-Path $PSScriptRoot 'lanzador-robot.ps1'
$taskName = 'TA4 lanzador robot'
$logDir = Join-Path $env:LOCALAPPDATA 'TA4-RUC'
$api = 'http://localhost:8080/api/v1/ruc'

if ($Desinstalar) {
  Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue
  Remove-Item -Path 'HKCU:\Software\Classes\ta4robot' -Recurse -Force -ErrorAction SilentlyContinue
  Write-Host 'Quitado: el lanzador ya no arranca solo ni se abre desde la web.'
  return
}

if ($Instalar) {
  $action = New-ScheduledTaskAction -Execute 'powershell.exe' `
    -Argument "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$launcher`""
  $trigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
  $settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
  Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings `
    -Description 'Lanzador local del robot UiPath del proyecto TA4' -Force | Out-Null
  Write-Host "Instalado: el lanzador arrancará solo al iniciar sesión ('$taskName')."

  # Enlace ta4robot://start: el botón «Encender robot» de la web lo usa para abrir el lanzador (sin ventana).
  # Solo afecta a este usuario (HKCU). Edge pregunta una vez «¿Abrir?»; con «Permitir siempre» no vuelve a preguntar.
  $protocolKey = 'HKCU:\Software\Classes\ta4robot'
  New-Item -Path "$protocolKey\shell\open\command" -Force | Out-Null
  Set-ItemProperty -Path $protocolKey -Name '(default)' -Value 'URL:TA4 robot'
  Set-ItemProperty -Path $protocolKey -Name 'URL Protocol' -Value ''
  Set-ItemProperty -Path "$protocolKey\shell\open\command" -Name '(default)' `
    -Value "powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$launcher`""
  Write-Host 'Instalado: el botón «Encender robot» de la web ya puede abrir el lanzador.'
}

# 1. Docker
# Docker escribe su progreso en stderr; con 'Stop', Windows PowerShell 5.1 lo tomaría por un error. Se decide por el código de salida.
$ErrorActionPreference = 'Continue'
docker info *> $null
if ($LASTEXITCODE -ne 0) { throw 'Docker no está en marcha. Abre Docker Desktop y vuelve a ejecutar este script.' }
cmd /c "docker compose -f ""$(Join-Path $root 'infra/compose.yaml')"" up -d 2>&1"
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron levantar los contenedores.' }
$ErrorActionPreference = 'Stop'

# 2. Backend listo
Write-Host 'Esperando al backend...' -NoNewline
$ready = $false
for ($i = 0; $i -lt 60 -and -not $ready; $i++) {
  try { Invoke-RestMethod -Uri "$api/robot/pending-start" -TimeoutSec 3 | Out-Null; $ready = $true }
  catch { Start-Sleep -Seconds 2; Write-Host '.' -NoNewline }
}
Write-Host ''
if (-not $ready) { throw 'El backend no respondió en 2 minutos. Revisa: docker compose -f infra/compose.yaml logs backend' }

# 3. Lanzador oculto (si ya hay uno, el nuevo sale solo gracias al mutex)
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
Start-Process powershell.exe -WindowStyle Hidden `
  -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', "`"$launcher`"" `
  -RedirectStandardOutput (Join-Path $logDir 'lanzador.log') `
  -RedirectStandardError (Join-Path $logDir 'lanzador.err.log')
Write-Host "Lanzador del robot en segundo plano. Registro: $logDir\lanzador.log"

# 4. Web
if (-not $SinNavegador) { Start-Process 'http://localhost:4280' }
Write-Host 'Listo. Adjunta el Excel y pulsa "Empezar flujo".' -ForegroundColor Green
