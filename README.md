<div align="center">

# RedShift

**VPN-клиент для RedPill Cloud** — Android-приложение для безопасного подключения к прокси-серверам через VpnService и sing-box.

[![Build APK](https://github.com/mindwork73/Redshift/actions/workflows/build.yml/badge.svg)](https://github.com/mindwork73/Redshift/actions/workflows/build.yml)

</div>

## Возможности

- **VpnService** — захват всего трафика устройства через TUN-интерфейс
- **sing-box** — маршрутизация трафика через VLESS, VMess, Trojan, Shadowsocks, Hysteria2
- **Импорт подписок** — парсинг Subscription URI и JSON-конфигов
- **Авторизация через Telegram** — привязка по TG ID через REST API
- **Динамический SOCKS5-proxy** — адрес прокси получается с сервера (`/api/v1/proxy`)
- **Кеширование серверов** — DataStore + автоматическое обновление по расписанию
- **Админ-панель** — управление пользователями (grant/extend/revoke/reissue) через REST API
- **Киберпанк-тёмная тема** — Material 3, Jetpack Compose

## Архитектура

```
App → VpnService (TUN) → localhost:SOCKS5 → sing-box → VLESS/Hy2/... сервер
```

- VpnService открывает TUN-интерфейс, перехватывает TCP/UDP
- TCP-трафик направляется через SOCKS5 (на `127.0.0.1:10808`)
- sing-box расшифровывает и отправляет на удалённый прокси-сервер
- UDP обрабатывается напрямую (DNS и пр.)

## Технологии

| Компонент | Технология |
|-----------|------------|
| Язык | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| VPN | VpnService (Andr
oid API 24+) |
| Прокси-ядро | sing-box (ProcessBuilder) |
| Сеть | OkHttp, Retrofit + Moshi |
| Хранилище | DataStore Preferences, Room |
| Фон | WorkManager |
| CI/CD | GitHub Actions |
| API | FastAPI (Python) на сервере |

## Сборка

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

APK будет в `app/build/outputs/apk/debug/`.

## CI

При каждом пуше в `main` GitHub Actions собирает debug APK. Артефакты доступны на странице [Actions](https://github.com/mindwork73/Redshift/actions).

## Лицензия

MIT
