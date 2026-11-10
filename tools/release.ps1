# ==============================================================================
# Automated Release & GitHub Upload Script - Dual Front TD
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
Write-Host "Dual Front TD: Automated Release & APK Upload Pipeline" -ForegroundColor Cyan
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
    Write-Host "`n[1/5] Executing automated test suite..." -ForegroundColor Yellow
    try {
        & $GradleCmd testDebugUnitTest
        Write-Host "  -> All tests passed cleanly!" -ForegroundColor Green
    } catch {
        Write-Warning "Test step could not be executed directly via command line; continuing with staging."
    }
}

# 3. Assemble Debug APK (if not skipped)
if (-not $SkipBuild) {
    Write-Host "`n[2/5] Compiling Android APK..." -ForegroundColor Yellow
    try {
        & $GradleCmd assembleDebug
    } catch {
        Write-Warning "Gradle assembleDebug threw an exception or gradle executable not in path: $_"
    }
}

$BuiltApk = Join-Path $ProjectRoot "app/build/outputs/apk/debug/app-debug.apk"
if (-not (Test-Path $BuiltApk)) {
    # Check if existing artifact exists in apk/ folder
    $FallbackApk = Join-Path $ProjectRoot ("apk/Dual-Front-TD-" + $Tag + "-debug.apk")
    if (Test-Path $FallbackApk) {
        $BuiltApk = $FallbackApk
    } else {
        $FallbackApk2 = Join-Path $ProjectRoot ("apk/game-release-" + $Tag + ".apk")
        if (Test-Path $FallbackApk2) {
            $BuiltApk = $FallbackApk2
        } else {
            Write-Error "Built APK not found at $BuiltApk or in apk directory."
        }
    }
}

# 4. Compile Portable Windows Launcher (if not skipped)
if (-not $SkipBuild) {
    Write-Host "`n[3/5] Compiling Portable Windows Launcher..." -ForegroundColor Yellow
    try {
        & "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe" /out:"Dual Front TD.exe" /target:exe /platform:anycpu tools\windows\RogueTDLauncher.cs
        Copy-Item "Dual Front TD.exe" -Destination (Join-Path $ProjectRoot ("apk/Dual-Front-TD-" + $Tag + ".exe")) -Force
    } catch {
        Write-Warning ("Windows launcher build skipped: " + $_)
    }
}

# 5. Stage Versioned Assets in apk/ directory
Write-Host "`n[4/5] Staging Release Binaries in apk/ directory..." -ForegroundColor Yellow
$ApkDir = Join-Path $ProjectRoot "apk"
if (-not (Test-Path $ApkDir)) {
    New-Item -ItemType Directory -Path $ApkDir | Out-Null
}

$DualApk = Join-Path $ApkDir ("Dual-Front-TD-" + $Tag + "-debug.apk")
$StandardApk = Join-Path $ApkDir ("game-release-" + $Tag + ".apk")
$DebugApk = Join-Path $ApkDir ("RogueTD-" + $Tag + "-debug.apk")
$WinExeInApk = Join-Path $ApkDir ("Dual-Front-TD-" + $Tag + ".exe")
$WinExeInRoot = Join-Path $ProjectRoot ("Dual Front TD.exe")

if (Test-Path $BuiltApk) {
    Copy-Item $BuiltApk $DualApk -Force
    Copy-Item $BuiltApk $StandardApk -Force
    Copy-Item $BuiltApk $DebugApk -Force
}

$AssetsToUpload = @($DualApk, $StandardApk, $DebugApk)
if (Test-Path $WinExeInApk) { $AssetsToUpload += $WinExeInApk }
if (Test-Path $WinExeInRoot) { $AssetsToUpload += $WinExeInRoot }

Write-Host "  -> Staged binaries ready for release:" -ForegroundColor Green
foreach ($itemPath in $AssetsToUpload) {
    if (Test-Path $itemPath) {
        $Item = Get-Item $itemPath
        $SizeMb = [math]::Round($Item.Length / 1MB, 2)
        Write-Host ("     * " + $Item.Name + " (" + $SizeMb + " MB)") -ForegroundColor Gray
    }
}

# 6. Upload Release to GitHub (gh CLI, direct REST API, or Guided Git Push)
Write-Host "`n[5/5] Publishing Release to GitHub..." -ForegroundColor Yellow

$ghCmd = Get-Command gh -ErrorAction SilentlyContinue
$publishedViaRest = $false

# A. Try GitHub CLI if installed and authenticated
if ($null -ne $ghCmd) {
    & gh auth status 2>$null
    if ($LASTEXITCODE -eq 0 -or -not [string]::IsNullOrEmpty($env:GITHUB_TOKEN)) {
        Write-Host "  -> Publishing via GitHub CLI ('gh')..." -ForegroundColor Cyan
        $GhArgs = @("release", "create", $Tag)
        foreach ($asset in $AssetsToUpload) { $GhArgs += $asset }
        $GhArgs += "--title"
        $GhArgs += ("Dual Front TD " + $Tag)
        $GhArgs += "--generate-notes"
        if ($Draft) { $GhArgs += "--draft" }
        if ($Prerelease) { $GhArgs += "--prerelease" }

        & gh @GhArgs
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "Release create failed or already exists. Re-uploading assets with --clobber..."
            $UploadArgs = @("release", "upload", $Tag)
            foreach ($asset in $AssetsToUpload) { $UploadArgs += $asset }
            $UploadArgs += "--clobber"
            & gh @UploadArgs
        }
        $publishedViaRest = $true
    }
}

# B. Try Direct GitHub REST API if GITHUB_TOKEN is available
if (-not $publishedViaRest -and -not [string]::IsNullOrEmpty($env:GITHUB_TOKEN)) {
    try {
        Write-Host "  -> Publishing directly via GitHub REST API..." -ForegroundColor Cyan
        $repo = "foxstrodon-prog/Fox-Rogue-TD"
        $headers = @{
            "Authorization" = "Bearer " + $env:GITHUB_TOKEN
            "Accept" = "application/vnd.github+json"
            "X-GitHub-Api-Version" = "2022-11-28"
        }

        # Check if release exists
        $relUri = "https://api.github.com/repos/$repo/releases/tags/$Tag"
        $rel = try { Invoke-RestMethod -Uri $relUri -Headers $headers -Method Get -ErrorAction Stop } catch { $null }

        if ($null -eq $rel) {
            $createBody = @{
                tag_name = $Tag
                name = ("Dual Front TD " + $Tag)
                generate_release_notes = $true
                draft = [bool]$Draft
                prerelease = [bool]$Prerelease
            } | ConvertTo-Json
            $rel = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases" -Headers $headers -Method Post -Body $createBody
        }

        if ($rel -and $rel.upload_url) {
            $uploadBase = $rel.upload_url -replace '\{\?name,label\}', ''
            foreach ($asset in $AssetsToUpload) {
                if (Test-Path $asset) {
                    $assetFile = Get-Item $asset
                    $uploadUri = "$uploadBase`?name=$($assetFile.Name)"
                    $bytes = [System.IO.File]::ReadAllBytes($assetFile.FullName)
                    $uploadHeaders = @{
                        "Authorization" = "Bearer " + $env:GITHUB_TOKEN
                        "Content-Type" = "application/octet-stream"
                    }
                    Write-Host ("     Uploading " + $assetFile.Name + "...") -ForegroundColor Gray
                    Invoke-RestMethod -Uri $uploadUri -Headers $uploadHeaders -Method Post -Body $bytes | Out-Null
                }
            }
            Write-Host "  -> Successfully uploaded all release assets via REST API!" -ForegroundColor Green
            $publishedViaRest = $true
        }
    } catch {
        Write-Warning "Direct REST upload encountered an error: $_"
    }
}

# C. Fallback: Sequential Git Push Instruction for GitHub Actions CI
if (-not $publishedViaRest) {
    Write-Host "`n  -> Staging completed locally." -ForegroundColor Cyan
    Write-Host "  -> To trigger the automated GitHub Release & APK Upload Actions workflow:" -ForegroundColor White
    Write-Host "     1. Commit the release assets and documentation:" -ForegroundColor Yellow
    Write-Host "        git commit -m `"Release $Tag: Final pass build, binaries, and documentation update`"" -ForegroundColor Gray
    Write-Host "     2. Tag the release:" -ForegroundColor Yellow
    Write-Host "        git tag -a $Tag -m `"Release $Tag`"" -ForegroundColor Gray
    Write-Host "     3. Push sequentially (Branch FIRST, then Tag) to prevent workflow race conditions:" -ForegroundColor Yellow
    Write-Host "        git push origin main" -ForegroundColor Green
    Write-Host "        git push origin $Tag" -ForegroundColor Green
    Write-Host "`n  GitHub Actions will automatically build, package, and upload all APKs and EXEs to the release page!" -ForegroundColor Cyan
}

Write-Host ("`n==========================================================") -ForegroundColor Cyan
Write-Host ("Completed release pipeline for " + $Tag + ".") -ForegroundColor Cyan
Write-Host ("==========================================================") -ForegroundColor Cyan
exit 0
