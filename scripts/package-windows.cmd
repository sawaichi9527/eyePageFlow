@echo off
rem Thin cmd.exe wrapper for scripts\package-windows.ps1.
rem Builds a portable app-image and zip; no installer is produced.
setlocal

set "SCRIPT_DIR=%~dp0"
where pwsh >nul 2>nul
if errorlevel 1 (
    echo pwsh 7 was not found on PATH. Install the opencode-extension-packs pwsh7 first.
    exit /b 1
)

pwsh -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%package-windows.ps1" %*
exit /b %ERRORLEVEL%
