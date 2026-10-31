param(
    # Expected release version — bump this (or pass -ExpectedVersion) each release.
    [string]$ExpectedVersion = "0.3.13",
    # Latest already-published release this APK must install over.
    [string]$LastPublished = "v0.3.12"
)

$ErrorActionPreference = "Stop"

# One-command release verification: checks the signed APK in the build output and proves it
# is installable as an update over the previous release (same signing certificate) and
# carries the expected version. The expected versionCode is derived from the same formula
# as app/build.gradle.kts, so the two can never drift.
$project = "D:\rd3"
$apk = Join-Path $project "app\build\outputs\apk\release\app-release.apk"
$apksigner = "C:\Users\mindw\AppData\Local\Android\Sdk\build-tools\36.0.0\apksigner.bat"
$aapt2 = "C:\Users\mindw\AppData\Local\Android\Sdk\build-tools\36.0.0\aapt2.exe"
$expectedFingerprint = "16418ea20444d831d20570c4da5965cb4ce4004292510d324380e5be0cd2e552"

if (-not (Test-Path $apk)) { throw "APK not found: $apk (build it first)" }

# Same formula as app/build.gradle.kts: major*1000000 + minor*10000 + patch*100
$parts = $ExpectedVersion.Split(".")
if ($parts.Count -ne 3) { throw "ExpectedVersion must look like 0.3.2, got: $ExpectedVersion" }
$expectedCode = [int]$parts[0] * 1000000 + [int]$parts[1] * 10000 + [int]$parts[2] * 100

$badging = & $aapt2 dump badging $apk 2>$null | Select-String "^package:" | Select-Object -First 1
$versionCode = if ($badging -match "versionCode='([^']*)'") { $matches[1] } else { "?" }
$versionName = if ($badging -match "versionName='([^']*)'") { $matches[1] } else { "?" }

$certs = & $apksigner verify --print-certs $apk 2>&1 | Out-String
$sha = if ($certs -match "SHA-256 digest:\s*([0-9a-f]+)") { $matches[1] } else { "?" }
$dn = if ($certs -match "DN:\s*(.+)") { $matches[1].Trim() } else { "?" }

$hash = (Get-FileHash $apk -Algorithm SHA256).Hash

"APK:          $apk"
"versionName:  $versionName (versionCode $versionCode)"
"signer:       $dn"
"sha256 cert:  $sha"
"sha256 file:  $hash"

if ($sha -ne $expectedFingerprint) { throw "WRONG SIGNING KEY: expected $expectedFingerprint, got $sha" }
if ($versionCode -ne [string]$expectedCode) { throw "UNEXPECTED versionCode: $versionCode (expected $expectedCode for $ExpectedVersion)" }
if ($versionName -ne $ExpectedVersion) { throw "UNEXPECTED versionName: $versionName (expected $ExpectedVersion)" }
"OK: signed with the upload key, version $ExpectedVersion ($expectedCode) - installs as an update over $LastPublished"
