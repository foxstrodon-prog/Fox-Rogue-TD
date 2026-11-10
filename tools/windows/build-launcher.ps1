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
    Copy-Item $OutExe (Join-Path $ApkDir "Dual Front TD.exe") -Force
} else {
    Write-Error "Failed to compile Dual Front TD.exe"
    exit 1
}
