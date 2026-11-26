param(
    [string]$Serial = "",
    [switch]$NoLaunch
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adb)) {
    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    if ($null -eq $adbCommand) {
        throw "adb was not found. Install Android platform-tools or add adb to PATH."
    }
    $adb = $adbCommand.Source
}

Set-Location $repoRoot

$devices = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" } | ForEach-Object {
    ($_ -split "\s+")[0]
}

if ([string]::IsNullOrWhiteSpace($Serial)) {
    $physicalDevices = @($devices | Where-Object { $_ -notmatch "^emulator-" })
    if ($physicalDevices.Count -eq 0) {
        throw "No physical phone is connected. Connect a phone with USB debugging enabled, or pass -Serial emulator-5554."
    }
    if ($physicalDevices.Count -gt 1) {
        throw "More than one physical phone is connected: $($physicalDevices -join ', '). Pass -Serial <device-id>."
    }
    $Serial = $physicalDevices[0]
}

Write-Host "Installing debug build on $Serial..."
$env:ANDROID_SERIAL = $Serial
& .\gradlew.bat installDebug
if ($LASTEXITCODE -ne 0) {
    throw "Gradle installDebug failed with exit code $LASTEXITCODE. The app was not reinstalled."
}

if (-not $NoLaunch) {
    Write-Host "Launching IZUN..."
    & $adb -s $Serial shell am force-stop com.example.caloriescounter | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to stop IZUN on $Serial. adb exited with code $LASTEXITCODE."
    }
    & $adb -s $Serial shell am start -n com.example.caloriescounter/.MainActivity
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to launch IZUN on $Serial. adb exited with code $LASTEXITCODE."
    }
}

Write-Host "Done."
