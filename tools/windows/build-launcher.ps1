# ==============================================================================
# Dual Front TD - Portable Windows Launcher Build Script
# ==============================================================================
[CmdletBinding()]
param()

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $ScriptDir)
Set-Location $ProjectRoot

$CscCandidates = @(
    "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe",
    "C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe"
)

$CscPath = $null
foreach ($c in $CscCandidates) {
    if (Test-Path $c) {
        $CscPath = $c
        break
    }
}

if (-not $CscPath) {
    Write-Error "Could not locate csc.exe in .NET Framework directory."
    exit 1
}

$SourceFile = Join-Path $ProjectRoot "tools\windows\RogueTDLauncher.cs"
$OutExe = Join-Path $ProjectRoot "Dual Front TD.exe"
$ApkDir = Join-Path $ProjectRoot "apk"

if (-not (Test-Path $ApkDir)) {
    New-Item -ItemType Directory -Path $ApkDir | Out-Null
}

Write-Host "Compiling Windows Launcher: $OutExe" -ForegroundColor Cyan
& $CscPath /out:"$OutExe" /target:exe /platform:anycpu "$SourceFile"

if ($LASTEXITCODE -eq 0 -and (Test-Path $OutExe)) {
    Write-Host "Successfully compiled Dual Front TD.exe in root!" -ForegroundColor Green

    # Extract version from app/build.gradle.kts
    $GradleFile = Join-Path $ProjectRoot "app\build.gradle.kts"
    $Version = "0.0.1.004"
    if (Test-Path $GradleFile) {
        $Content = Get-Content $GradleFile -Raw
        if ($Content -match 'versionName\s*=\s*"([^"]+)"') {
            $Version = $Matches[1].Trim()
        }
    }
    $VersionedExeName = "Dual-Front-TD-v$Version.exe"
    $VersionedExePath = Join-Path $ApkDir $VersionedExeName

    # Purge any outdated versioned EXEs from apk/ folder
    Get-ChildItem -Path $ApkDir -Filter "Dual-Front-TD-v*.exe" | Where-Object { $_.Name -ne $VersionedExeName } | Remove-Item -Force

    # Stage the versioned executable into apk/
    Copy-Item $OutExe $VersionedExePath -Force
    Write-Host "Successfully staged Windows launcher at: $VersionedExePath" -ForegroundColor Green
} else {
    Write-Error "Failed to compile Dual Front TD.exe"
    exit 1
}
