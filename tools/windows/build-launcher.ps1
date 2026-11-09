# Automated Windows Portable Launcher Build Script
$ErrorActionPreference = "Stop"

$rootDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $rootDir

# Extract version from app/build.gradle.kts
$gradleFile = Join-Path $rootDir "app\build.gradle.kts"
$version = "0.0.0.005"
if (Test-Path $gradleFile) {
    $match = Select-String -Path $gradleFile -Pattern 'versionName\s*=\s*"([^"]+)"'
    if ($match -and $match.Matches.Groups.Count -gt 1) {
        $version = $match.Matches.Groups[1].Value
    }
}

$exeName = "RogueTD-v$version.exe"
$srcFile = Join-Path $rootDir "tools\windows\RogueTDLauncher.cs"
$outExe = Join-Path $rootDir $exeName
$apkExe = Join-Path $rootDir "apk\$exeName"

$csc = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
if (-not (Test-Path $csc)) {
    $csc = "csc.exe"
}

Write-Host "Compiling portable Windows launcher for v$version..." -ForegroundColor Cyan
& $csc /nologo /target:exe /out:"$outExe" "$srcFile"

if (Test-Path $outExe) {
    Copy-Item $outExe $apkExe -Force
    Write-Host "[SUCCESS] Generated $exeName at root and in apk/ folder." -ForegroundColor Green
    Get-Item $outExe, $apkExe | Select-Object Name, Length, LastWriteTime
} else {
    Write-Error "Failed to compile $exeName."
}
