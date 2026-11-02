@echo off
rem Windows counterpart of scripts/gradlew-local.sh for cmd.exe users.
rem Runs the Gradle Wrapper with the project-local JDK and an isolated Gradle home.
setlocal

set "ROOT_DIR=%~dp0.."
for %%I in ("%ROOT_DIR%") do set "ROOT_DIR=%%~fI"

if not exist "%ROOT_DIR%\.local\jdk-21\bin\java.exe" (
    echo Project-local JDK not found. Run scripts\bootstrap-jdk.cmd first.
    exit /b 1
)

if not exist "%ROOT_DIR%\gradlew.bat" (
    echo Gradle Wrapper not found: gradlew.bat
    exit /b 1
)

set "JAVA_HOME=%ROOT_DIR%\.local\jdk-21"
set "GRADLE_USER_HOME=%ROOT_DIR%\.local\gradle-home"
set "PATH=%JAVA_HOME%\bin;%PATH%"

call "%ROOT_DIR%\gradlew.bat" %*
exit /b %ERRORLEVEL%
