param(
    [string]$PackageName = "de.emailspeicher.zufallsklingelton"
)

$ErrorActionPreference = "Stop"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path -LiteralPath $adb)) {
    throw "adb not found: $adb"
}

$devices = & $adb devices | Select-String "^emulator-[0-9]+\s+device$"
if ($devices.Count -ne 1) {
    throw "Exactly one connected emulator is required. Found: $($devices.Count)"
}
$serial = ($devices[0].Line -split "\s+")[0]
$testPackage = "$PackageName.test/androidx.test.runner.AndroidJUnitRunner"

function Invoke-Instrumentation([string]$method) {
    & $adb -s $serial shell am instrument -w -r -e class "$PackageName.PlatformAcceptanceTest#$method" $testPackage
    if ($LASTEXITCODE -ne 0) { throw "Instrumentation failed: $method" }
}

function Wait-ForBoot {
    & $adb -s $serial wait-for-device
    $deadline = (Get-Date).AddMinutes(3)
    do {
        $boot = (& $adb -s $serial shell getprop sys.boot_completed).Trim()
        if ($boot -eq "1") { return }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Emulator did not finish booting in time."
}

Write-Host "Phone acceptance on $serial"
Invoke-Instrumentation "preparePhoneCall"
& $adb -s $serial emu gsm call 15551234567
Start-Sleep -Seconds 3
& $adb -s $serial emu gsm cancel
Start-Sleep -Seconds 3
Invoke-Instrumentation "assertPhoneCallChangedExactlyOnce"

Write-Host "Reboot acceptance on $serial"
Invoke-Instrumentation "prepareReboot"
& $adb -s $serial reboot
Wait-ForBoot
Invoke-Instrumentation "assertRebootDispatchedBootReceiver"

Write-Host "Platform acceptance passed."
