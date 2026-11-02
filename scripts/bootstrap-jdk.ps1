#!/usr/bin/env pwsh
# Windows counterpart of scripts/bootstrap-jdk.sh.
# Installs the pinned Temurin 21 JDK into .local/jdk-21 without touching the system JDK.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$rootDir = Split-Path -Parent $PSScriptRoot
$localDir = Join-Path $rootDir '.local'
$jdkDir = Join-Path $localDir 'jdk-21'
$downloadDir = Join-Path $localDir 'downloads'
$tmpDir = Join-Path $localDir 'tmp'

$jdkVersion = '21.0.12+8'
$expectedSha256 = '9ba963ee2371874a74185d18bc7bb2ab9407df7683300855ed7606e0662321d0'
$archiveName = 'OpenJDK21U-jdk_x64_windows_hotspot_21.0.12_8.zip'
$downloadUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12%2B8/$archiveName"
$archivePath = Join-Path $downloadDir $archiveName

if (-not [Environment]::Is64BitOperatingSystem) {
    Write-Error 'Unsupported platform. This bootstrap supports 64-bit Windows only.'
    exit 1
}

function Test-PinnedJdk {
    param([string]$Path)

    $javaExe = Join-Path $Path 'bin/java.exe'
    $releaseFile = Join-Path $Path 'release'
    if (-not (Test-Path -LiteralPath $javaExe) -or -not (Test-Path -LiteralPath $releaseFile)) {
        return $false
    }

    $release = Get-Content -LiteralPath $releaseFile
    if ($release -notcontains 'IMPLEMENTOR="Eclipse Adoptium"') {
        return $false
    }
    if ($release -notcontains 'IMPLEMENTOR_VERSION="Temurin-21.0.12+8"') {
        return $false
    }

    & $javaExe -version *> $null
    return $LASTEXITCODE -eq 0
}

if (Test-Path -LiteralPath $jdkDir) {
    if (Test-PinnedJdk -Path $jdkDir) {
        Write-Host "Temurin $jdkVersion is already installed at $jdkDir"
        & (Join-Path $jdkDir 'bin/java.exe') -version
        exit 0
    }

    Write-Error "A different JDK already exists at $jdkDir. Remove it explicitly before bootstrapping."
    exit 1
}

New-Item -ItemType Directory -Force -Path $downloadDir, $tmpDir | Out-Null

if (-not (Test-Path -LiteralPath $archivePath)) {
    Write-Host "Downloading Temurin $jdkVersion to $archivePath"
    $partialPath = "$archivePath.part"
    try {
        curl.exe --fail --location --retry 3 --continue-at - --output $partialPath $downloadUrl
        if ($LASTEXITCODE -ne 0) {
            throw "curl exited with code $LASTEXITCODE"
        }
        Move-Item -LiteralPath $partialPath -Destination $archivePath -Force
    }
    finally {
        if (Test-Path -LiteralPath $partialPath) {
            Remove-Item -LiteralPath $partialPath -Force -ErrorAction SilentlyContinue
        }
    }
}

$actualSha256 = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualSha256 -ne $expectedSha256) {
    Write-Error "Checksum mismatch for $archiveName`nExpected: $expectedSha256`nActual:   $actualSha256"
    exit 1
}

$stagingDir = Join-Path $tmpDir ("jdk.{0}" -f [Guid]::NewGuid().ToString('N'))
try {
    New-Item -ItemType Directory -Force -Path $stagingDir | Out-Null
    Expand-Archive -LiteralPath $archivePath -DestinationPath $stagingDir

    $extractedDir = Get-ChildItem -LiteralPath $stagingDir -Directory | Select-Object -First 1
    if ($null -eq $extractedDir -or -not (Test-Path -LiteralPath (Join-Path $extractedDir.FullName 'bin/java.exe'))) {
        Write-Error "The downloaded archive did not contain a usable JDK: $archivePath"
        exit 1
    }

    Move-Item -LiteralPath $extractedDir.FullName -Destination $jdkDir
}
finally {
    Remove-Item -LiteralPath $stagingDir -Recurse -Force -ErrorAction SilentlyContinue
}

& (Join-Path $jdkDir 'bin/java.exe') -version
Write-Host "Installed project-local Temurin $jdkVersion at $jdkDir"
