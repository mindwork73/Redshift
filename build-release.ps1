$ErrorActionPreference = "Stop"

# Reads keystore credentials from the local (never-committed) creds file,
# then builds a signed release AAB. Usage: .\build-release.ps1 [APP_VERSION]
$credsFile = "C:\Users\mindw\AppData\Local\Temp\opencode\keystore_creds.txt"
if (-not (Test-Path $credsFile)) {
    Write-Error "Keystore creds not found at $credsFile"
}

$creds = @{}
Get-Content $credsFile | ForEach-Object {
    $k, $v = $_ -split '=', 2
    if ($k) { $creds[$k] = $v }
}

if (-not $creds['store']) { Write-Error "store password missing" }

$env:APP_VERSION = if ($args.Count -gt 0) { $args[0] } else { "0.1.2" }
$env:KEYSTORE_PATH = if ($creds['storefile']) { $creds['storefile'] } else { "D:\rd3\my-upload-key.jks" }
$env:STORE_PASSWORD = $creds['store']
$env:KEY_PASSWORD = if ($creds['key']) { $creds['key'] } else { $creds['store'] }

Write-Output "Building APP_VERSION=$env:APP_VERSION release AAB..."
& "D:\rd3\gradlew.bat" ":app:bundleRelease" "--no-configuration-cache" 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle bundle failed (exit $LASTEXITCODE)"
}

$aab = "D:\rd3\app\build\outputs\bundle\release\app-release.aab"
if (Test-Path $aab) {
    Write-Output "AAB_OK size=$((Get-Item $aab).Length)"
} else {
    Write-Error "AAB not found at $aab"
}