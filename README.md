<div align="center">

# RedShift

**VPN-клиент для RedPill Cloud** — современное Android-приложение на sing-box ядре: AmneziaWG, VLESS/Reality, Hysteria2, Trojan/SS и olcRTC. Быстрые протоколы, раздельное туннелирование, 20 языков, автообновление.

[![Downloads](https://img.shields.io/github/downloads/mindwork73/Redshift/total?style=flat-square&logo=github)](https://github.com/mindwork73/Redshift/releases/)
[![Last Version](https://img.shields.io/github/release/mindwork73/Redshift/all.svg?style=flat-square)](https://github.com/mindwork73/Redshift/releases/)
[![License](https://img.shields.io/github/license/mindwork73/Redshift?style=flat-square)](LICENSE)

</div>

Высокопроизводительный VPN-клиент с открытым ядром sing-box (in-process, JNI), оптимизированный для обхода блокировок и стабильной работы в сетях с агрессивным DPI.

<p align="center">
    <img alt="RedShift" src="snapshots/main.png" width="280px">
</p>

> Скриншот будет добавлен после первого релиза.

## Функции

### Протоколы

🛡️ **Максимум совместимости** — ядро sing-box-lx работает в процессe приложения (без отдельных бинарников) и поддерживает:

- **AmneziaWG** — обфусцированный WireGuard с кастомными HParams, устойчив к DPI
- **VLESS + Reality** — современный протокол с TLS-in-TLS реалистичностью
- **Hysteria2** — быстрый QUIC-based протокол для слабых каналов
- **Trojan / Shadowsocks** — классика с обфускацией
- **olcRTC (WebRTC)** — протокол на основе WebRTC для сверхжёстких сетей

### Подписки и серверы

📥 **Импорт подписок** — по ссылке или из буфера: `vless://`, `vmess://`, `trojan://`, `ss://`, `hy2://`, `amneziawg://` и JSON-конфиги.

🌍 **Авто-выбор сервера** — серверы проверяются по пингу, лучший выбирается автоматически. Поддерживается кеширование и фоновое обновление серверов.

🔑 **Авторизация через Telegram** — привязка по Telegram ID через REST API, статус подписки и срок действия показываются в приложении.

### Раздельное туннелирование

📱 **По приложениям** — выбирайте, какие приложения идут через VPN (режим "только выбранные"), а какие — в обход.

🌐 **По доменам** — тонкая настройка маршрутизации вручную.

### Безопасность и приватность

🚫 **Блокировка рекламы и трекеров** — DNS-фильтрация встроена в ядро.

🔒 **FakeIP и приватные адреса** — исключены из туннеля по умолчанию.

🔍 **Обход DPI и блокировок** — H2 с Salamander, obfs, пассивные DNS.

### Другое

- 🔄 **Автообновление** — проверка новых версий из GitHub Releases при запуске приложения
- 🌐 **20 языков** — интерфейс переведён на 20 языков, включая русский, английский, китайский, персидский и др.
- 🌓 **Киберпанк-тёмная тема** — Material 3, Jetpack Compose, анимированный фон
- ⚡ **Низкое потребление** — VpnService в fd-режиме, sing-box in-process без лишних процессов

## Технологии

| Компонент | Технология |
|-----------|------------|
| Язык | Kotlin 2.2 |
| UI | Jetpack Compose + Material 3 |
| VPN | VpnService (Android API 24+) |
| Ядро | sing-box-lx (JNI, in-process) |
| Сеть | OkHttp |
| Хранилище | DataStore Preferences |
| Фон | WorkManager |
| Обновления | GitHub Releases API |

## Скачать

[![Скачать с GitHub](https://img.shields.io/badge/Download-Latest-2ea44f?style=for-the-badge&logo=github)](https://github.com/mindwork73/Redshift/releases/latest)

Последний релиз: [Releases](https://github.com/mindwork73/Redshift/releases)

> ⚠️ Устанавливайте только последнюю версию. В релизе v0.1.2 режим раздельного туннелирования "только выбранные" не запускается; установите v0.1.4 или новее.

## Сборка

```bash
# Windows
gradlew.bat assembleDebug

# Указать версию (влияет на versionCode)
APP_VERSION=0.1.5 ./gradlew assembleDebug
```

APK появится в `app/build/outputs/apk/debug/`.

## Архитектура

```
App (UI, Compose) → RedShiftState → VpnService (fd) → sing-box-lx (JNI) → TUN → протокол → сервер RedPill Cloud
```

- VpnService открывает TUN-интерфейс (MTU 1280) и передаёт fd в sing-box через JNI — без root и без внешних процессов
- TCP/UDP маршрутизируется sing-box по правилам: разделение по приложениям/доменам, блокировка рекламы
- DNS через DoH с detour на выбранный outbound

## Поддержка

По вопросам и проблемам — в канал поддержки: [@redpillcloud_bot](https://t.me/redpillcloud_bot)

## Лицензия

MIT — см. [LICENSE](LICENSE)