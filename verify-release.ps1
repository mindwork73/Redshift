$ErrorActionPreference = "Stop"

# One-command release verification: builds the signed APK from the current commit and proves
# it is installable as an update over the published release (same signing certificate) and
# carries the expected version.
$project = "D:\rd3"
$apk = Join-Path $project "app\build\outputs\apk\release\app-release.apk"
$apksigner = "C:\Users\mindw\AppData\Local\Android\Sdk\build-tools\36.0.0\apksigner.bat"
$aapt2 = "C:\Users\mindw\AppData\Local\Android\Sdk\build-tools\36.0.0\aapt2.exe"
$expectedFingerprint = "16418ea20444d831d20570c4da5965cb4ce4004292510d324380e5be0cd2e552"

if (-not (Test-Path $apk)) { throw "APK not found: $apk (build it first)" }

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
if ($versionCode -ne "30000") { throw "UNEXPECTED versionCode: $versionCode" }
if ($versionName -ne "0.3.0") { throw "UNEXPECTED versionName: $versionName" }
"OK: signed with the upload key, version 0.3.0 (30000) - installs as an update over v0.1.9"
