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

### Added
- SOCKS5 proxy config endpoint `GET /api/v1/proxy` in REST API
  - Reads `PROXY_HOST`/`PROXY_PORT` from `.env`, defaults to `216.57.106.89:995`
- `RedPillApiClient.getProxy()` — fetch remote proxy config from API
- `Models.kt` — dynamic proxy host/port resolution on connect with API fallback

### Changed
- API base URL: `http://216.57.106.89:8000` → `https://api.redpillcloud.ru` (HTTPS via Cloudflare)
- nginx `api.redpillcloud.ru` — added `location /api/` proxy to localhost:8000
- `redpill-api.service` — binds to `127.0.0.1:8000` instead of `0.0.0.0:8000`

### Added
- Subscription auto-refresh with WorkManager
  - `SubscriptionRefreshWorker.kt` — фоновый Worker, парсит подписку, кеширует серверы
  - `AutoRefreshScheduler.kt` — планирование периодических обновлений
  - SettingsStore: `auto_refresh`, `refresh_interval_hours`, `cached_servers_json`, etc
  - RedShiftState: загрузка кеша при старте, toggle auto-refresh
  - UI в Settings: switch + выбор интервала (1/3/6/12/24ч) + статус последнего обновления
- `work-runtime-ktx 2.9.1` в зависимости
