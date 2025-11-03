Param(
    [string]$Package = "com.firstapp.myapplication",
    [string]$BeforeLabel = "before",
    [string]$AfterLabel = "after",
    [int]$DelaySeconds = 2
)

# Simple PowerShell script to capture before/after screenshots from a connected Android device/emulator
# Usage examples:
# .\capture_comparison.ps1 -Package com.firstapp.myapplication
# .\capture_comparison.ps1 -Package com.firstapp.myapplication -DelaySeconds 5

function Check-Adb {
    $adb = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $adb) {
        Write-Error "adb not found in PATH. Install Android Platform Tools and add adb to PATH."
        exit 2
    }
}

function Capture($label) {
    $remotePath = "/sdcard/screen_$label.png"
    $localDir = Join-Path -Path (Get-Location) -ChildPath "screenshots"
    if (-not (Test-Path $localDir)) { New-Item -ItemType Directory -Path $localDir | Out-Null }
    $localPath = Join-Path -Path $localDir -ChildPath "$label.png"

    Write-Host "Capturing screenshot to device: $remotePath"
    adb shell screencap -p $remotePath | Out-Null
    Start-Sleep -Seconds 1
    Write-Host "Pulling screenshot to local: $localPath"
    adb pull $remotePath $localPath | Out-Null
    adb shell rm $remotePath | Out-Null
    return $localPath
}

Check-Adb

Write-Host "Ensure the app is visible on the device. You can launch it now."
Write-Host "Waiting $DelaySeconds seconds before capturing '$BeforeLabel'..."
Start-Sleep -Seconds $DelaySeconds
$beforePath = Capture $BeforeLabel

Write-Host "Now change the screen to the AFTER state (navigate, open dialog, etc)."
Write-Host "Waiting $DelaySeconds seconds before capturing '$AfterLabel'..."
Start-Sleep -Seconds $DelaySeconds
$afterPath = Capture $AfterLabel

$comparisonPath = Join-Path -Path (Get-Location) -ChildPath "screenshots\comparison.png"

Write-Host "Attempting to stitch images side-by-side into: $comparisonPath"
try {
    # Try using ImageMagick 'magick' if available
    $magick = Get-Command magick -ErrorAction SilentlyContinue
    if ($magick) {
        magick convert +append $beforePath $afterPath $comparisonPath
        Write-Host "Created comparison image: $comparisonPath"
    } else {
        Write-Warning "ImageMagick 'magick' not found. Comparison image not created."
        Write-Host "Screenshots saved to: $beforePath and $afterPath"
    }
} catch {
    Write-Warning "Failed to create comparison image: $_"
    Write-Host "Screenshots saved to: $beforePath and $afterPath"
}

Write-Host "Done. Open the screenshots folder to review: $(Join-Path (Get-Location) 'screenshots')"
