<div align="center">

# RedShift

**VPN-клиент для RedPill Cloud** — Android-приложение для безопасного подключения к прокси-серверам через VpnService и sing-box.

[![Build APK](https://github.com/mindwork73/Redshift/actions/workflows/build.yml/badge.svg)](https://github.com/mindwork73/Redshift/actions/workflows/build.yml)

</div>

## Возможности

- **VpnService (TUN, fd-мод)** — захват всего трафика устройства через TUN-интерфейс (MTU 1280)
- **sing-box-lx (in-process, JNI)** — маршрутизация трафика через AmneziaWG, VLESS, VMess, Trojan, Shadowsocks, Hysteria2
- **AmneziaWG** — обфускация AmneziaWG работает на ядре sing-box-lx в режиме in-process (без CLI-процесса)
- **Импорт подписок** — парсинг Subscription URI (vless/vmess/trojan/ss/hy2/amneziawg) и JSON-конфигов
- **Авторизация через Telegram** — привязка по TG ID через REST API
- **Динамический SOCKS5-proxy** — адрес прокси получается с сервера (`/api/v1/proxy`)
- **Кеширование серверов** — DataStore + автоматическое обновление по расписанию
- **Админ-панель** — управление пользователями (grant/extend/revoke/reissue) через REST API
- **Киберпанк-тёмная тема** — Material 3, Jetpack Compose

## Архитектура

```
App (JNI) → sing-box-lx in-process → tun0 (VpnService, fd-мод) → VLESS/Hy2/AMWG сервер
```

- VpnService открывает TUN-интерфейс (fd передаётся в sing-box через JNI), MTU 1280
- sing-box-lx работает **внутри процесса** приложения (libsingbox.so), без внешних процессов
- TCP/UDP маршрутизируются sing-box по правилам (РФ → direct, остальное → VPN)
- DNS через DoH (8.8.8.8/dns-query) с detour на выбранный outbound

## Технологии

| Компонент | Технология |
|-----------|------------|
| Язык | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| VPN | VpnService (Android API 24+) |
| Прокси-ядро | sing-box-lx (JNI in-process) |
| Сеть | OkHttp, Moshi |
| Хранилище | DataStore Preferences |
| Фон | WorkManager |
| CI/CD | GitHub Actions (APG 9.1.1, JDK 17) |

## Сборка

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug

# Указать версию (влияет на versionCode)
APP_VERSION=0.2.4 ./gradlew assembleDebug
```

APK будет в `app/build/outputs/apk/debug/`.

## CI

При каждом пуше в `main` GitHub Actions собирает debug APK. Артефакты доступны на странице [Actions](https://github.com/mindwork73/Redshift/actions).

## Известные ограничения

- sing-box в fd-режиме не трогает MTU интерфейса (MTU задаётся VpnService = 1280)
- AmneziaWG Hparams должны совпадать на NL/EU серверах
- VPN работает без `auto_detect_interface` (в fd-режиме netlink запрещён для app uid)

## Лицензия

MIT