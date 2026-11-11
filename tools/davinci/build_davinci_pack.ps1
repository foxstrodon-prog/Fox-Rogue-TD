# ==============================================================================
# Dual Front TD - DaVinci Resolve Studio Procedural Video Pack Generator
# Generates high-res game element sprites, FCPXML timeline, and Fusion composition
# ==============================================================================
[CmdletBinding()]
param()

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$AssetDir = Join-Path $ScriptDir "assets"

if (-not (Test-Path $AssetDir)) {
    New-Item -ItemType Directory -Path $AssetDir -Force | Out-Null
}

Add-Type -AssemblyName System.Drawing

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Dual Front TD: DaVinci Resolve Video Pack Generator" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# ------------------------------------------------------------------------------
# 1. Generate Game Element PNG Sprites via System.Drawing
# ------------------------------------------------------------------------------
Write-Host "`n[1/3] Generating 4K-ready game element sprites..." -ForegroundColor Yellow

# Helper: Draw Anti-aliased graphics
function New-Canvas([int]$w, [int]$h) {
    $bmp = New-Object System.Drawing.Bitmap $w, $h
    $gfx = [System.Drawing.Graphics]::FromImage($bmp)
    $gfx.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $gfx.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $gfx.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    return @{ Bitmap = $bmp; Graphics = $gfx }
}

# Asset 1: Background Arena Grid (1080x1920)
$c = New-Canvas 1080 1920
$bgBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 7, 11, 20))
$c.Graphics.FillRectangle($bgBrush, 0, 0, 1080, 1920)

# Subtle tactical grid
$gridPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(40, 0, 240, 255)), 1
for ($x = 0; $x -le 1080; $x += 60) { $c.Graphics.DrawLine($gridPen, $x, 0, $x, 1920) }
for ($y = 0; $y -le 1920; $y += 60) { $c.Graphics.DrawLine($gridPen, 0, $y, 1080, $y) }

# Converging dual circuit lanes (Left Lane & Right Lane)
$laneBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(45, 14, 165, 233))
$laneBorderPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(180, 0, 229, 255)), 3

# Left Lane (x: 180 to 420)
$c.Graphics.FillRectangle($laneBrush, 180, 0, 240, 1500)
$c.Graphics.DrawRectangle($laneBorderPen, 180, 0, 240, 1500)

# Right Lane (x: 660 to 900)
$c.Graphics.FillRectangle($laneBrush, 660, 0, 240, 1500)
$c.Graphics.DrawRectangle($laneBorderPen, 660, 0, 240, 1500)

# Base Command Core at bottom (y: 1500 to 1800)
$coreBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(60, 0, 229, 255))
$c.Graphics.FillEllipse($coreBrush, 340, 1450, 400, 300)
$corePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 0, 240, 255)), 4
$c.Graphics.DrawEllipse($corePen, 340, 1450, 400, 300)

$c.Bitmap.Save((Join-Path $AssetDir "bg_grid.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$c.Graphics.Dispose()
$c.Bitmap.Dispose()

# Asset 2: Booster Pad Cyan (200x200)
$c = New-Canvas 200 200
$padBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(80, 0, 240, 255))
$c.Graphics.FillEllipse($padBrush, 10, 10, 180, 180)
$padPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 0, 240, 255)), 5
$c.Graphics.DrawEllipse($padPen, 10, 10, 180, 180)
$innerPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(200, 255, 255, 255)), 2
$c.Graphics.DrawEllipse($innerPen, 35, 35, 130, 130)
$c.Bitmap.Save((Join-Path $AssetDir "booster_pad_cyan.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$c.Graphics.Dispose()
$c.Bitmap.Dispose()

# Asset 3: Booster Pad Magenta (200x200)
$c = New-Canvas 200 200
$padBrushM = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(80, 244, 63, 94))
$c.Graphics.FillEllipse($padBrushM, 10, 10, 180, 180)
$padPenM = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 244, 63, 94)), 5
$c.Graphics.DrawEllipse($padPenM, 10, 10, 180, 180)
$c.Bitmap.Save((Join-Path $AssetDir "booster_pad_magenta.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$c.Graphics.Dispose()
$c.Bitmap.Dispose()

# Asset 4: Kinetic Gatling Turret (180x180)
$c = New-Canvas 180 180
$baseBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 30, 41, 59))
$c.Graphics.FillEllipse($baseBrush, 15, 15, 150, 150)
$basePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 255, 184, 0)), 4
$c.Graphics.DrawEllipse($basePen, 15, 15, 150, 150)
$barrelBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 184, 0))
$c.Graphics.FillRectangle($barrelBrush, 80, 10, 20, 80)
$c.Graphics.FillEllipse($barrelBrush, 65, 65, 50, 50)
$c.Bitmap.Save((Join-Path $AssetDir "turret_gatling.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$c.Graphics.Dispose()
$c.Bitmap.Dispose()

# Asset 5: Level 4 Cosmic Stellar Star Turret (240x240)
$c = New-Canvas 240 240
$goldHaloBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(70, 255, 215, 0))
$c.Graphics.FillEllipse($goldHaloBrush, 10, 10, 220, 220)
$goldPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 255, 215, 0)), 4
$c.Graphics.DrawEllipse($goldPen, 30, 30, 180, 180)
$c.Graphics.DrawEllipse($goldPen, 60, 60, 120, 120)
# 8-point gold radiant star
$starBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 255, 255))
$c.Graphics.FillEllipse($starBrush, 95, 95, 50, 50)
$c.Graphics.DrawLine($goldPen, 120, 15, 120, 225)
$c.Graphics.DrawLine($goldPen, 15, 120, 225, 120)
$c.Graphics.DrawLine($goldPen, 45, 45, 195, 195)
$c.Graphics.DrawLine($goldPen, 195, 45, 45, 195)
$c.Bitmap.Save((Join-Path $AssetDir "turret_stellar.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$c.Graphics.Dispose()
$c.Bitmap.Dispose()

# Asset 6: Creep Swarm Monster (120x120)
$c = New-Canvas 120 120
$creepBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(230, 239, 68, 68))
$c.Graphics.FillEllipse($creepBrush, 20, 20, 80, 80)
$creepEyeBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 255, 0))
$c.Graphics.FillEllipse($creepEyeBrush, 35, 40, 15, 15)
$c.Graphics.FillEllipse($creepEyeBrush, 70, 40, 15, 15)
$c.Bitmap.Save((Join-Path $AssetDir "creep_swarm.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$c.Graphics.Dispose()
$c.Bitmap.Dispose()

# Asset 7: Shockwave Halo Ring (800x800)
$c = New-Canvas 800 800
$haloPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(200, 255, 215, 0)), 12
$c.Graphics.DrawEllipse($haloPen, 40, 40, 720, 720)
$c.Bitmap.Save((Join-Path $AssetDir "shockwave_ring.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$c.Graphics.Dispose()
$c.Bitmap.Dispose()

# Asset 8: Floating Squircle Action Button (240x260)
$c = New-Canvas 240 260
$squircleBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(230, 15, 23, 42))
$c.Graphics.FillRectangle($squircleBrush, 10, 10, 220, 240)
$squircleBorder = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 255, 215, 0)), 4
$c.Graphics.DrawRectangle($squircleBorder, 10, 10, 220, 240)
# Text
$titleFont = [System.Drawing.Font]::new("Arial", [float]14, [System.Drawing.FontStyle]::Bold)
$subFont = [System.Drawing.Font]::new("Arial", [float]9, [System.Drawing.FontStyle]::Regular)
$whiteBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
$goldBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 215, 0))
$grayBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 148, 163, 184))

$c.Graphics.DrawString("STELLAR", $titleFont, $goldBrush, 30, 30)
$c.Graphics.DrawString("280c", $titleFont, $whiteBrush, 150, 30)
$c.Graphics.DrawString("* Cosmic Rays", $subFont, $grayBrush, 30, 120)
$c.Graphics.DrawString("* Barrier Bypass", $subFont, $grayBrush, 30, 160)
$c.Bitmap.Save((Join-Path $AssetDir "hud_squircle_button.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$c.Graphics.Dispose()
$c.Bitmap.Dispose()

Write-Host "  -> Generated 8 game element sprites in: $AssetDir" -ForegroundColor Green

# ------------------------------------------------------------------------------
# 2. Generate Apple FCPXML 1.8 Timeline for DaVinci Resolve Studio
# ------------------------------------------------------------------------------
Write-Host "`n[2/3] Generating DaVinci Resolve FCPXML 1.8 Timeline (1080x1920 @ 60fps)..." -ForegroundColor Yellow

$FcpxmlFile = Join-Path $ScriptDir "DualFrontTD_ViralShort_22s.fcpxml"

$BgPath = [System.Uri](Join-Path $AssetDir "bg_grid.png").AbsoluteUri
$PadCyanPath = [System.Uri](Join-Path $AssetDir "booster_pad_cyan.png").AbsoluteUri
$PadMagPath = [System.Uri](Join-Path $AssetDir "booster_pad_magenta.png").AbsoluteUri
$TurretGatPath = [System.Uri](Join-Path $AssetDir "turret_gatling.png").AbsoluteUri
$TurretStelPath = [System.Uri](Join-Path $AssetDir "turret_stellar.png").AbsoluteUri
$CreepPath = [System.Uri](Join-Path $AssetDir "creep_swarm.png").AbsoluteUri
$ShockwavePath = [System.Uri](Join-Path $AssetDir "shockwave_ring.png").AbsoluteUri
$SquirclePath = [System.Uri](Join-Path $AssetDir "hud_squircle_button.png").AbsoluteUri

$FcpxmlContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE fcpxml>
<fcpxml version="1.8">
    <resources>
        <format id="r1" name="FFVideoFormat1080x1920p60" frameDuration="1/60s" width="1080" height="1920"/>
        <asset id="a1" name="bg_grid" src="$BgPath" start="0s" duration="22s" hasVideo="1" format="r1"/>
        <asset id="a2" name="booster_pad_cyan" src="$PadCyanPath" start="0s" duration="22s" hasVideo="1" format="r1"/>
        <asset id="a3" name="booster_pad_magenta" src="$PadMagPath" start="0s" duration="22s" hasVideo="1" format="r1"/>
        <asset id="a4" name="turret_gatling" src="$TurretGatPath" start="0s" duration="22s" hasVideo="1" format="r1"/>
        <asset id="a5" name="turret_stellar" src="$TurretStelPath" start="0s" duration="22s" hasVideo="1" format="r1"/>
        <asset id="a6" name="creep_swarm" src="$CreepPath" start="0s" duration="22s" hasVideo="1" format="r1"/>
        <asset id="a7" name="shockwave_ring" src="$ShockwavePath" start="0s" duration="22s" hasVideo="1" format="r1"/>
        <asset id="a8" name="hud_squircle_button" src="$SquirclePath" start="0s" duration="22s" hasVideo="1" format="r1"/>
    </resources>
    <library>
        <event name="Dual Front TD Viral Short">
            <project name="Dual Front TD - 22s Viral Loop [4K 60FPS]">
                <sequence format="r1" duration="1320/60s" TCStart="0s" TCFormat="NDF">
                    <spine>
                        <!-- Track 1: Arena Background (0s to 22s) -->
                        <video ref="a1" offset="0s" name="Arena Grid" start="0s" duration="1320/60s">
                            <!-- Overlay Track 2: Booster Pads (Left & Right) -->
                            <video ref="a2" lane="1" offset="0s" name="Booster Pad Cyan" start="0s" duration="1320/60s"/>
                            <!-- Overlay Track 3: Creep Swarm Assault (0s to 14s) -->
                            <video ref="a6" lane="2" offset="0s" name="Creep Swarm" start="0s" duration="840/60s"/>
                            <!-- Overlay Track 4: Turrets (Gatling 0s to 11s, Stellar 11s to 22s) -->
                            <video ref="a4" lane="3" offset="0s" name="Gatling Turret (Pre-Swap &amp; Teleport)" start="0s" duration="660/60s"/>
                            <video ref="a5" lane="3" offset="660/60s" name="Level 4 Stellar Star (Ascension)" start="0s" duration="660/60s"/>
                            <!-- Overlay Track 5: Shockwave Detonation (14s to 17s) -->
                            <video ref="a7" lane="4" offset="840/60s" name="Apex Corona Shockwave" start="0s" duration="180/60s"/>
                            <!-- Overlay Track 6: Floating Squircle Upgrade HUD (6s to 12s) -->
                            <video ref="a8" lane="5" offset="360/60s" name="Squircle Upgrade Action Bar" start="0s" duration="360/60s"/>
                        </video>
                    </spine>
                </sequence>
            </project>
        </event>
    </library>
</fcpxml>
"@

Set-Content -Path $FcpxmlFile -Value $FcpxmlContent -Encoding UTF8
Write-Host "  -> Successfully written: $FcpxmlFile" -ForegroundColor Green

# ------------------------------------------------------------------------------
# 3. Generate Native DaVinci Resolve Fusion Composition (.comp)
# ------------------------------------------------------------------------------
Write-Host "`n[3/3] Generating DaVinci Resolve Fusion Composition (.comp)..." -ForegroundColor Yellow

$CompFile = Join-Path $ScriptDir "DualFrontTD_TacticalDopamine.comp"

$CompContent = @"
Composition {
	CurrentTime = 0,
	RenderRange = { 0, 1320 },
	GlobalRange = { 0, 1320 },
	CurrentFormat = "1080x1920",
	Tools = {
		BackgroundSpace = Background {
			Inputs = {
				Width = Input { Value = 1080, },
				Height = Input { Value = 1920, },
				["Gamut.SLogVersion"] = Input { Value = 2, },
				TopLeftRed = Input { Value = 0.027, },
				TopLeftGreen = Input { Value = 0.043, },
				TopLeftBlue = Input { Value = 0.078, },
				TopLeftAlpha = Input { Value = 1, },
			},
		},
		ShockwaveRing = EllipseMask {
			Inputs = {
				Width = Input {
					SourceOp = "ShockwaveScaleSpline",
					Source = "Value",
				},
				Height = Input {
					SourceOp = "ShockwaveScaleSpline",
					Source = "Value",
				},
				BorderWidth = Input { Value = 0.02, },
				Solid = Input { Value = 0, },
			},
		},
		ShockwaveScaleSpline = BezierSpline {
			SplineColor = { Red = 255, Green = 215, Blue = 0 },
			KeyFrames = {
				[840] = { 0.02, RH = { 870, 0.4 } },
				[960] = { 0.95, LH = { 900, 0.8 } }
			}
		},
		GoldShockwaveColor = Background {
			Inputs = {
				TopLeftRed = Input { Value = 1.0, },
				TopLeftGreen = Input { Value = 0.84, },
				TopLeftBlue = Input { Value = 0.0, },
				EffectMask = Input {
					SourceOp = "ShockwaveRing",
					Source = "Mask",
				}
			},
		},
		MergeShockwave = Merge {
			Inputs = {
				Background = Input {
					SourceOp = "BackgroundSpace",
					Source = "Output",
				},
				Foreground = Input {
					SourceOp = "GoldShockwaveColor",
					Source = "Output",
				},
				PerformDepthMerge = Input { Value = 0, },
			},
		},
		MediaOut1 = MediaOut {
			Inputs = {
				Index = Input { Value = "0", },
				Input = Input {
					SourceOp = "MergeShockwave",
					Source = "Output",
				},
			},
		}
	}
}
"@

Set-Content -Path $CompFile -Value $CompContent -Encoding UTF8
Write-Host "  -> Successfully written: $CompFile" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "DaVinci Resolve Video Pack generated successfully!" -ForegroundColor Green
Write-Host "  1. Timeline: $FcpxmlFile" -ForegroundColor White
Write-Host "  2. Fusion Comp: $CompFile" -ForegroundColor White
Write-Host "  3. Assets: $AssetDir" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan
