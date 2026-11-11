[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [ValidateRange(1, 6)]
    [int]$Cinematic,

    [Parameter(Mandatory=$false)]
    [string]$Version = "v001004"
)

$targetFilename = "${Version}_Cinematic${Cinematic}.mp4"
$targetDir = "C:\Users\tanne\AndroidStudioProjects\Youtube Team Assets\Raw_Gameplay_Intake"
$targetPath = Join-Path $targetDir $targetFilename

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " PULLING LATEST PHONE RECORDING -> $targetFilename" -ForegroundColor Yellow
Write-Host "==================================================" -ForegroundColor Cyan

# Find latest video on device in Movies/Screenrecords or DCIM/ScreenRecorder
$remoteDirs = @(
    "/sdcard/Movies/Screenrecords",
    "/sdcard/DCIM/ScreenRecorder",
    "/sdcard/Movies",
    "/sdcard/DCIM"
)

$foundFile = $null
foreach ($dir in $remoteDirs) {
    $files = adb shell "ls -t $dir/*.mp4 2>/dev/null"
    if ($files) {
        $first = ($files -split "`r?`n" | Where-Object { $_.Trim() -match "\.mp4$" } | Select-Object -First 1)
        if ($first) {
            $foundFile = $first.Trim()
            break
        }
    }
}

if (-not $foundFile) {
    Write-Error "Could not find any .mp4 screen recording on connected device."
    exit 1
}

Write-Host "Found newest recording on phone: $foundFile" -ForegroundColor Green
Write-Host "Pulling to: $targetPath..." -ForegroundColor Yellow

if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

adb pull $foundFile $targetPath

if (Test-Path $targetPath) {
    $bytes = (Get-Item $targetPath).Length
    $mb = [math]::Round($bytes / 1MB, 2)
    Write-Host "`nSUCCESS! Saved as $targetFilename ($mb MB)." -ForegroundColor Green
    Write-Host "Target: $targetPath" -ForegroundColor Cyan
    Write-Host "Your timeline in DaVinci Resolve Studio is ready to cook!" -ForegroundColor Yellow
} else {
    Write-Error "Failed to pull file from device."
}
