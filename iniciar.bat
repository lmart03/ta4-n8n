@echo off
rem Doble clic: levanta todo y abre la web. Usa "iniciar.bat /instalar" para que el lanzador arranque solo con Windows.
cd /d "%~dp0"
if /i "%~1"=="/instalar" (
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\iniciar.ps1 -Instalar
) else (
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\iniciar.ps1
)
if errorlevel 1 pause
