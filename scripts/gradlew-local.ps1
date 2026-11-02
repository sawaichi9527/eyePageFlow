#!/usr/bin/env pwsh
# Windows counterpart of scripts/gradlew-local.sh.
# Runs the Gradle Wrapper with the project-local JDK and an isolated Gradle home.
[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$rootDir = Split-Path -Parent $PSScriptRoot
$jdkDir = Join-Path $rootDir '.local/jdk-21'
$gradlewBat = Join-Path $rootDir 'gradlew.bat'

if (-not (Test-Path -LiteralPath (Join-Path $jdkDir 'bin/java.exe'))) {
    Write-Error 'Project-local JDK not found. Run ./scripts/bootstrap-jdk.ps1 first.'
    exit 1
}

if (-not (Test-Path -LiteralPath $gradlewBat)) {
    Write-Error 'Gradle Wrapper not found: gradlew.bat'
    exit 1
}

$env:JAVA_HOME = $jdkDir
$env:GRADLE_USER_HOME = Join-Path $rootDir '.local/gradle-home'
$env:PATH = "$(Join-Path $jdkDir 'bin');$env:PATH"

& $gradlewBat @GradleArgs
exit $LASTEXITCODE
