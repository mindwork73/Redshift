# Changelog

## [Unreleased]

### Added
- REST API server (FastAPI) on NL server at `/opt/redpill-bot/api.py`
  - Endpoints: user info, subscription, devices, admin (grant/extend/revoke/reissue)
  - systemd service `redpill-api.service`, port 8000
- `RedPillApiClient.kt` — OkHttp client for the REST API
- `RedShiftState.login(tgId: Int)` — real API auth via Telegram user ID
- `RedShiftState.refreshUserData()` — pull latest subscription data
- Admin dashboard UI (hidden tab, appears when API token is configured in Settings)
  - Stats cards, Grant Access form, user list
- Login screen by Telegram ID in Settings (instead of placeholder token)
- `apiBaseUrl` and `apiAdminToken` state variables for API configuration

### Changed
- `Models.kt` — added `apiClient`, `userInfo`, `loginError`, `isLoadingUser` state
- `Screens.kt`:
  - Updated login form to accept numeric Telegram ID
  - Logged-in section shows username, plan, expiry, device count
  - Bottom bar shows ADMIN tab when `apiAdminToken` is set
- `RedPillApiClient.kt` default base URL changed to HTTP (no HTTPS yet)
