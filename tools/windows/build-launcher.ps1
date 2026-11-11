# ==============================================================================
# Dual Front TD - Portable ADB Launcher Build Script
# Compiles Dual-Front-TD-ADB.exe into the apk/ directory
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
$ApkDir = Join-Path $ProjectRoot "apk"
$OutExe = Join-Path $ApkDir "Dual-Front-TD-ADB.exe"

if (-not (Test-Path $ApkDir)) {
    New-Item -ItemType Directory -Path $ApkDir | Out-Null
}

# Purge any root executable and old versioned executables from ecosystem
Remove-Item (Join-Path $ProjectRoot "Dual Front TD.exe") -Force -ErrorAction SilentlyContinue
Get-ChildItem -Path $ApkDir -Filter "*.exe" | Where-Object { $_.Name -ne "Dual-Front-TD-ADB.exe" } | Remove-Item -Force -ErrorAction SilentlyContinue

Write-Host "Compiling ADB Runner: $OutExe" -ForegroundColor Cyan
& $CscPath /out:"$OutExe" /target:exe /platform:anycpu "$SourceFile"

if ($LASTEXITCODE -eq 0 -and (Test-Path $OutExe)) {
    Write-Host "Successfully compiled Dual-Front-TD-ADB.exe in apk/ directory!" -ForegroundColor Green
} else {
    Write-Error "Failed to compile Dual-Front-TD-ADB.exe"
    exit 1
}
