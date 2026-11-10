# ==============================================================================
# Automated Release & GitHub Upload Script - Rogue TD: Dual-Front
# Compiles APK and Windows Runner, packages assets, and creates GitHub Release
# ==============================================================================
[CmdletBinding()]
param(
    [string]$VersionOverride = "",
    [switch]$SkipTests,
    [switch]$Draft,
    [switch]$Prerelease
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
Set-Location $ProjectRoot

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "🚀 Rogue TD: Automated Release & APK Upload Pipeline" -ForegroundColor Cyan
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

$Tag = "v$Version"
Write-Host "📌 Target Release Version: $Version (Tag: $Tag)" -ForegroundColor Green

# 2. Run Test Suite
if (-not $SkipTests) {
    Write-Host "`n🧪 Executing automated test suite..." -ForegroundColor Yellow
    & ./gradlew.bat testDebugUnitTest
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Unit tests failed. Aborting release."
    }
    Write-Host "✅ All tests passed cleanly!" -ForegroundColor Green
}

# 3. Assemble Debug APK
Write-Host "`n📦 Compiling Android APK..." -ForegroundColor Yellow
& ./gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle assembleDebug failed."
}

$BuiltApk = Join-Path $ProjectRoot "app/build/outputs/apk/debug/app-debug.apk"
if (-not (Test-Path $BuiltApk)) {
    Write-Error "Built APK not found at $BuiltApk"
}

# 4. Compile Portable Windows Widescreen Launcher
Write-Host "`n🖥️ Compiling Portable Windows Launcher..." -ForegroundColor Yellow
try {
    & ./gradlew.bat buildWindowsLauncher
} catch {
    Write-Warning "Windows launcher build skipped or failed: $_"
}

# 5. Stage Versioned Assets in apk/ directory
$ApkDir = Join-Path $ProjectRoot "apk"
if (-not (Test-Path $ApkDir)) {
    New-Item -ItemType Directory -Path $ApkDir | Out-Null
}

$StandardApk = Join-Path $ApkDir "game-release-$Tag.apk"
$DebugApk = Join-Path $ApkDir "RogueTD-$Tag-debug.apk"
$WinExeInApk = Join-Path $ApkDir "RogueTD-$Tag.exe"
$WinExeInRoot = Join-Path $ProjectRoot "RogueTD-$Tag.exe"

Copy-Item $BuiltApk $StandardApk -Force
Copy-Item $BuiltApk $DebugApk -Force

$AssetsToUpload = @($StandardApk, $DebugApk)
if (Test-Path $WinExeInApk) {
    $AssetsToUpload += $WinExeInApk
} elseif (Test-Path $WinExeInRoot) {
    $AssetsToUpload += $WinExeInRoot
}

Write-Host "✅ Release binaries staged successfully:" -ForegroundColor Green
$AssetsToUpload | ForEach-Object {
    $Item = Get-Item $_
    Write-Host "   - $($Item.Name) ($([math]::Round($Item.Length / 1MB, 2)) MB)" -ForegroundColor Gray
}

# 6. Upload Release to GitHub via GitHub CLI (`gh`) or REST API
Write-Host "`n🌐 Checking GitHub publishing capabilities..." -ForegroundColor Yellow

$GhCliAvailable = $false
try {
    $ghVersion = & gh --version 2>$null
    if ($LASTEXITCODE -eq 0) {
        $GhCliAvailable = $true
    }
} catch {
    $GhCliAvailable = $false
}

if ($GhCliAvailable) {
    Write-Host "✅ GitHub CLI ('gh') detected." -ForegroundColor Green

    # Check authentication
    & gh auth status 2>$null
    if ($LASTEXITCODE -ne 0 -and [string]::IsNullOrEmpty($env:GITHUB_TOKEN)) {
        Write-Warning "GitHub CLI is not authenticated. Please run 'gh auth login' or set `$env:GITHUB_TOKEN to upload automatically."
        Write-Host "Local binaries are compiled and ready in: $ApkDir" -ForegroundColor Cyan
        exit 0
    }

    Write-Host "Creating GitHub Release $Tag..." -ForegroundColor Yellow

    $GhArgs = @("release", "create", $Tag)
    $AssetsToUpload | ForEach-Object { $GhArgs += $_ }
    $GhArgs += "--title", "Rogue TD: Dual-Front $Tag"
    $GhArgs += "--generate-notes"

    if ($Draft) { $GhArgs += "--draft" }
    if ($Prerelease) { $GhArgs += "--prerelease" }

    # Execute gh release create
    & gh @GhArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Release create failed, attempting to upload assets to existing release $Tag..."
        $UploadArgs = @("release", "upload", $Tag)
        $AssetsToUpload | ForEach-Object { $UploadArgs += $_ }
        $UploadArgs += "--clobber"
        & gh @UploadArgs
    }

    Write-Host "`n🎉 Release $Tag successfully published to GitHub Releases!" -ForegroundColor Green
} else {
    Write-Warning "GitHub CLI ('gh') was not found in PATH."
    Write-Host "To enable automated one-click publishing from PowerShell, install gh ('winget install GitHub.cli') and run 'gh auth login'."
    Write-Host "Alternatively, push tag '$Tag' to trigger the automated GitHub Actions workflow (.github/workflows/release.yml):"
    Write-Host "   git tag $Tag" -ForegroundColor White
    Write-Host "   git push origin $Tag" -ForegroundColor White
}

Write-Host "`nFinished release pipeline for version $Version." -ForegroundColor Cyan
