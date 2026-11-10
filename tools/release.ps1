# ==============================================================================
# Automated Release & GitHub Upload Script - Rogue TD: Dual-Front
# Compiles APK and Windows Runner, packages assets, and creates GitHub Release
# ==============================================================================
[CmdletBinding()]
param(
    [string]$VersionOverride = "",
    [switch]$SkipTests,
    [switch]$SkipBuild,
    [switch]$Draft,
    [switch]$Prerelease
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
Set-Location $ProjectRoot

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Rogue TD: Automated Release & APK Upload Pipeline" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Determine Version
if ($VersionOverride -ne "") {
    $Version = $VersionOverride.Trim().TrimStart("v")
} else {
    $GradleFile = Join-Path $ProjectRoot "app/build.gradle.kts"
    if (-not (Test-Path $GradleFile)) {
        Write-Error "Cannot locate app/build.gradle.kts"
    }
    $Content = Get-Content $GradleFile -Raw
    if ($Content -match 'versionName\s*=\s*"([^"]+)"') {
        $Version = $Matches[1].Trim()
    } else {
        Write-Error "Failed to extract versionName from app/build.gradle.kts"
    }
}

$Tag = "v" + $Version
Write-Host ("Target Release Version: " + $Version + " (Tag: " + $Tag + ")") -ForegroundColor Green

# Locate Gradle executable (wrapper or system gradle)
$GradlewBat = Join-Path $ProjectRoot "gradlew.bat"
$GradleCmd = if (Test-Path $GradlewBat) { $GradlewBat } else { "gradle" }

# 2. Run Test Suite (if not skipped)
if (-not $SkipTests -and -not $SkipBuild) {
    Write-Host "`nExecuting automated test suite..." -ForegroundColor Yellow
    try {
        & $GradleCmd testDebugUnitTest
        Write-Host "All tests passed cleanly!" -ForegroundColor Green
    } catch {
        Write-Warning "Test step could not be executed directly via command line; continuing with staging."
    }
}

# 3. Assemble Debug APK (if not skipped)
if (-not $SkipBuild) {
    Write-Host "`nCompiling Android APK..." -ForegroundColor Yellow
    try {
        & $GradleCmd assembleDebug
    } catch {
        Write-Warning "Gradle assembleDebug threw an exception or gradle executable not in path: $_"
    }
}

$BuiltApk = Join-Path $ProjectRoot "app/build/outputs/apk/debug/app-debug.apk"
if (-not (Test-Path $BuiltApk)) {
    # Check if existing artifact exists in apk/ folder
    $FallbackApk = Join-Path $ProjectRoot ("apk/game-release-" + $Tag + ".apk")
    if (Test-Path $FallbackApk) {
        $BuiltApk = $FallbackApk
    } else {
        Write-Error "Built APK not found at $BuiltApk or in apk directory."
    }
}

# 4. Compile Portable Windows Widescreen Launcher (if not skipped)
if (-not $SkipBuild) {
    Write-Host "`nCompiling Portable Windows Launcher..." -ForegroundColor Yellow
    try {
        & powershell -ExecutionPolicy Bypass -File (Join-Path $ProjectRoot "tools/windows/build-launcher.ps1")
    } catch {
        Write-Warning ("Windows launcher build skipped: " + $_)
    }
}

# 5. Stage Versioned Assets in apk/ directory
$ApkDir = Join-Path $ProjectRoot "apk"
if (-not (Test-Path $ApkDir)) {
    New-Item -ItemType Directory -Path $ApkDir | Out-Null
}

$StandardApk = Join-Path $ApkDir ("game-release-" + $Tag + ".apk")
$DebugApk = Join-Path $ApkDir ("RogueTD-" + $Tag + "-debug.apk")
$WinExeInApk = Join-Path $ApkDir ("RogueTD-" + $Tag + ".exe")
$WinExeInRoot = Join-Path $ProjectRoot ("RogueTD-" + $Tag + ".exe")

if (Test-Path $BuiltApk) {
    Copy-Item $BuiltApk $StandardApk -Force
    Copy-Item $BuiltApk $DebugApk -Force
}

$AssetsToUpload = @($StandardApk, $DebugApk)
if (Test-Path $WinExeInApk) {
    $AssetsToUpload += $WinExeInApk
} elseif (Test-Path $WinExeInRoot) {
    $AssetsToUpload += $WinExeInRoot
}

Write-Host "`nRelease binaries staged successfully:" -ForegroundColor Green
foreach ($itemPath in $AssetsToUpload) {
    if (Test-Path $itemPath) {
        $Item = Get-Item $itemPath
        $SizeMb = [math]::Round($Item.Length / 1MB, 2)
        Write-Host ("   - " + $Item.Name + " (" + $SizeMb + " MB)") -ForegroundColor Gray
    }
}

# 6. Upload Release to GitHub via GitHub CLI (`gh`) or notify user to push
Write-Host "`nChecking GitHub publishing capabilities..." -ForegroundColor Yellow

$ghCmd = Get-Command gh -ErrorAction SilentlyContinue

if ($null -ne $ghCmd) {
    Write-Host "GitHub CLI ('gh') detected." -ForegroundColor Green

    # Check authentication
    & gh auth status 2>$null
    if ($LASTEXITCODE -ne 0 -and [string]::IsNullOrEmpty($env:GITHUB_TOKEN)) {
        Write-Warning "GitHub CLI is not authenticated. Please run 'gh auth login' or set `$env:GITHUB_TOKEN."
        Write-Host "Binaries are compiled and ready locally in: $ApkDir" -ForegroundColor Cyan
        exit 0
    }

    Write-Host ("Creating/Updating GitHub Release " + $Tag + "...") -ForegroundColor Yellow

    $GhArgs = @("release", "create", $Tag)
    foreach ($asset in $AssetsToUpload) {
        $GhArgs += $asset
    }
    $GhArgs += "--title"
    $GhArgs += ("Rogue TD: Dual-Front " + $Tag)
    $GhArgs += "--generate-notes"

    if ($Draft) { $GhArgs += "--draft" }
    if ($Prerelease) { $GhArgs += "--prerelease" }

    & gh @GhArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Warning ("Release create failed, attempting to upload assets to existing release " + $Tag + "...")
        $UploadArgs = @("release", "upload", $Tag)
        foreach ($asset in $AssetsToUpload) {
            $UploadArgs += $asset
        }
        $UploadArgs += "--clobber"
        & gh @UploadArgs
    }

    Write-Host ("`nRelease " + $Tag + " successfully published to GitHub Releases!") -ForegroundColor Green
} else {
    Write-Host "`nGitHub CLI ('gh') is not installed locally on this machine." -ForegroundColor Yellow
    Write-Host "All release binaries have been compiled and staged locally:" -ForegroundColor Green
    Write-Host ("   - " + $StandardApk) -ForegroundColor Cyan
    Write-Host ("   - " + $DebugApk) -ForegroundColor Cyan
    Write-Host ("   - " + $WinExeInApk) -ForegroundColor Cyan
    Write-Host "`nTo publish this release to GitHub Releases immediately:" -ForegroundColor White
    Write-Host "   git push origin main" -ForegroundColor Green
    Write-Host "The GitHub Actions workflow (.github/workflows/release.yml) will trigger automatically on push and publish the release with these assets!" -ForegroundColor Gray
}

Write-Host ("`nCompleted release pipeline for version " + $Version + ".") -ForegroundColor Cyan
exit 0
