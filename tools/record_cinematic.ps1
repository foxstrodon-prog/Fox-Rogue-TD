[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)]
    [ValidateRange(1, 6)]
    [int]$Cinematic,

    [Parameter(Mandatory=$false)]
    [string]$Version = "v001004",

    [Parameter(Mandatory=$false)]
    [int]$BitrateMbps = 20
)

$targetFilename = "${Version}_Cinematic${Cinematic}.mp4"
$targetDir = "C:\Users\tanne\AndroidStudioProjects\Youtube Team Assets\Raw_Gameplay_Intake"
$targetPath = Join-Path $targetDir $targetFilename
$remotePath = "/sdcard/$targetFilename"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host " RECORDING IN-GAME CINEMATIC $Cinematic ($targetFilename)" -ForegroundColor Yellow
Write-Host "==================================================" -ForegroundColor Cyan

# Check if ADB device is connected
$devices = adb devices | Select-String -Pattern "device$"
if (-not $devices) {
    Write-Error "No connected Android device found via ADB. Please connect your phone via USB or Wi-Fi debugging."
    exit 1
}

if (-not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

Write-Host "[1/3] Starting 30.00s screen record on device..." -ForegroundColor Green
Write-Host "-> Recording to device: $remotePath ($BitrateMbps Mbps)" -ForegroundColor DarkGray
adb shell screenrecord --time-limit 30 --bit-rate ($BitrateMbps * 1000000) $remotePath

Write-Host "[2/3] Pulling recording to intake folder..." -ForegroundColor Green
adb pull $remotePath $targetPath

Write-Host "[3/3] Cleaning up temporary recording on device..." -ForegroundColor Green
adb shell rm -f $remotePath

if (Test-Path $targetPath) {
    $bytes = (Get-Item $targetPath).Length
    $mb = [math]::Round($bytes / 1MB, 2)
    Write-Host "`nSUCCESS! Cinematic $Cinematic captured ($mb MB)." -ForegroundColor Green
    Write-Host "Target: $targetPath" -ForegroundColor Cyan
    Write-Host "Open DaVinci Resolve Studio and import the timeline - your clip is staged and ready!" -ForegroundColor Yellow
} else {
    Write-Error "Failed to pull $targetPath from device."
}
