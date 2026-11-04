#!/usr/bin/env pwsh
# 建立 Windows 版可攜式 app-image 與 zip（解開即可執行）。
#
# 用法： .\scripts\package-windows.ps1
# 產出： build\package\eyePageFlow\                          app-image
#        build\package\eyePageFlow.ico                       應用程式圖示
#        build\package\eyePageFlow-<version>-windows-x64.zip 可分送的壓縮檔
#
# 不產生安裝檔：jpackage 的 MSI 產生器需要額外的 WiX Toolset，
# 本專案改以壓縮檔分送。解開 zip 後直接執行 eyePageFlow\eyePageFlow.exe。
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$rootDir = Split-Path -Parent $PSScriptRoot
$jdkDir = Join-Path $rootDir '.local/jdk-21'
$outDir = Join-Path $rootDir 'build/package'
$runtimeDir = Join-Path $outDir 'runtime'
$appName = 'eyePageFlow'
$appVersion = '0.1.0'
$appDir = Join-Path $outDir $appName
$zipPath = Join-Path $outDir "$appName-$appVersion-windows-x64.zip"

$jlinkExe = Join-Path $jdkDir 'bin/jlink.exe'
$jpackageExe = Join-Path $jdkDir 'bin/jpackage.exe'

foreach ($exe in @($jlinkExe, $jpackageExe)) {
    if (-not (Test-Path -LiteralPath $exe)) {
        Write-Error "JDK tool not found: $exe. Run .\scripts\bootstrap-jdk.ps1 first."
        exit 1
    }
}

# jpackage 與 installDist 的 launcher 都需要這組環境。
$env:JAVA_HOME = $jdkDir
$env:GRADLE_USER_HOME = Join-Path $rootDir '.local/gradle-home'
$env:PATH = "$(Join-Path $jdkDir 'bin');$env:PATH"

New-Item -ItemType Directory -Path $outDir -Force | Out-Null

Write-Host '==> 建置 installDist'
& (Join-Path $PSScriptRoot 'gradlew-local.ps1') --quiet installDist
if ($LASTEXITCODE -ne 0) {
    Write-Error "installDist failed (exit $LASTEXITCODE)."
    exit $LASTEXITCODE
}

$installDir = Join-Path $rootDir 'build/install/eyePageFlow'
$libDir = Join-Path $installDir 'lib'

Write-Host '==> 產生圖示'
$iconPath = Join-Path $outDir "$appName.ico"
& (Join-Path $installDir 'bin/eyePageFlow.bat') --icon $iconPath
if ($LASTEXITCODE -ne 0) {
    Write-Error "icon generation failed (exit $LASTEXITCODE)."
    exit $LASTEXITCODE
}

Write-Host '==> 產生 runtime image（jlink）'
if (Test-Path -LiteralPath $runtimeDir) {
    Remove-Item -LiteralPath $runtimeDir -Recurse -Force
}
& $jlinkExe `
    --module-path (Join-Path $jdkDir 'jmods') `
    --add-modules java.base,java.desktop,java.logging,java.prefs,jdk.charsets `
    --strip-debug --no-man-pages --no-header-files --compress=zip-6 `
    --output $runtimeDir
if ($LASTEXITCODE -ne 0) {
    Write-Error "jlink failed (exit $LASTEXITCODE)."
    exit $LASTEXITCODE
}

Write-Host '==> 打包 app-image（jpackage）'
if (Test-Path -LiteralPath $appDir) {
    Remove-Item -LiteralPath $appDir -Recurse -Force
}
$mainJar = Get-ChildItem -LiteralPath $libDir -Filter "$appName-*.jar" |
    Select-Object -First 1
if ($null -eq $mainJar) {
    Write-Error "Main jar not found in $libDir."
    exit 1
}
& $jpackageExe `
    --type app-image `
    --name $appName `
    --app-version $appVersion `
    --input $libDir `
    --main-jar $mainJar.Name `
    --main-class 'io.github.sawaichi9527.eyepageflow.MainKt' `
    --runtime-image $runtimeDir `
    --icon $iconPath `
    --dest $outDir
if ($LASTEXITCODE -ne 0) {
    Write-Error "jpackage failed (exit $LASTEXITCODE)."
    exit $LASTEXITCODE
}

$exePath = Join-Path $appDir "$appName.exe"
if (-not (Test-Path -LiteralPath $exePath)) {
    Write-Error "app-image launcher not found: $exePath"
    exit 1
}

# jlink 的 runtime 少了 jdk.charsets 時，Big5／GB18030 會 UnsupportedCharsetException，
# 但 GUI 看起來一切正常，只有開舊編碼檔案才會炸。--benchmark 會實際解一次 Big5。
#
# 注意：jpackage 產生的 eyePageFlow.exe 是 GUI 子系統程式，JVM 失敗時仍然回 0、
# 也不會輸出訊息，所以不能用它當 smoke test；這裡改用打包進去的 runtime/java.exe。
Write-Host '==> 驗證 app-image（Big5 解碼）'
$runtimeJava = Join-Path $appDir 'runtime/bin/java.exe'
& $runtimeJava -cp (Join-Path $appDir 'app/*') `
    'io.github.sawaichi9527.eyepageflow.MainKt' --benchmark
if ($LASTEXITCODE -ne 0) {
    Write-Error "app-image smoke test failed (exit $LASTEXITCODE); check jlink modules."
    exit 1
}

Write-Host '==> 產生壓縮檔'
if (Test-Path -LiteralPath $zipPath) {
    Remove-Item -LiteralPath $zipPath -Force
}
Compress-Archive -Path $appDir -DestinationPath $zipPath -CompressionLevel Optimal

$zipMb = [math]::Round((Get-Item -LiteralPath $zipPath).Length / 1MB, 1)
$appMb = [math]::Round(
    (Get-ChildItem -LiteralPath $appDir -Recurse -File |
        Measure-Object -Property Length -Sum).Sum / 1MB,
    1
)

Write-Host ''
Write-Host '完成：'
Write-Host "  app-image : $exePath  ($appMb MB)"
Write-Host "  icon      : $iconPath"
Write-Host "  zip       : $zipPath  ($zipMb MB)"
Write-Host ''
Write-Host '驗證：'
Write-Host "  $exePath"
Write-Host "  Expand-Archive -Path '$zipPath' -DestinationPath build\package\verify"
Write-Host '  build\package\verify\eyePageFlow\eyePageFlow.exe'
