@echo off
rem Thin cmd.exe wrapper for scripts\bootstrap-jdk.ps1.
setlocal

set "SCRIPT_DIR=%~dp0"
where pwsh >nul 2>nul
if errorlevel 1 (
    echo pwsh 7 was not found on PATH. Install the opencode-extension-packs pwsh7 first.
    exit /b 1
)

pwsh -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%bootstrap-jdk.ps1" %*
exit /b %ERRORLEVEL%
