<div align="center">

# 🔴 RedShift

**VPN-клиент для RedPill Cloud** — Android-приложение с тёмной киберпанк-темой, полным списком серверов, профилем и раздельным туннелированием.

[![Build APK](https://github.com/mindwork73/Redshift/actions/workflows/build.yml/badge.svg)](https://github.com/mindwork73/Redshift/actions/workflows/build.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-purple.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-24%2B-green.svg)](https://developer.android.com)

**RedPill Cloud** — сервис приватных прокси и белых списков: межзональный (РФ) доступ к заблокированным сайтам через личные и арендованные IP.

</div>

---

## ✨ Возможности

### 🚀 Подключение и сеть
- **VpnService + TUN (fd-режим)** — захват трафика устройства, MTU 1280
- **sing-box-lx in-process (JNI)** — VLESS/Reality, VMess, Trojan, Shadowsocks, **Hysteria2**, **AmneziaWG**
- **Умный пинг серверов** — TCP-протоколы пингуются через свой же туннель (SOCKS), UDP (Hysteria/AWG) — прямым ICMP, честный RTT в любом состоянии
- **Правила маршрутизации** — РФ (`geoip-ru` + доменные суффиксы `.ru/.su/.рф/...`) → напрямую, остальное → VPN, 443/UDP → block от пирингов

### 🎛️ Раздельное туннелирование
- Полный список установленных приложений с иконками и поиском
- Режимы «только эти приложения» / «все, кроме этих» (по приложениям или доменам)
- Выбор «все / сбросить», живой счётчик выбранного

### 👤 Профиль и подписка
- Вход по **Telegram ID** через REST API `api.redpillcloud.ru`
- Карточка профиля: ник, Telegram ID, тариф, срок действия, лимит устройств
- Подписка импортируется и **автообновляется в фоне** (WorkManager)
- Умный дедуп серверов — ротация IP/имен не дублирует список

### 🖥️ Интерфейс
- Тёмная киберпанк-тема, Material 3, Jetpack Compose
- Главная с карточкой текущего сервера и трафиком сессии
- Вкладки: Главная / Серверы / Настройки / Профиль (+Админ при токене)

---

## 🏗️ Архитектура

```
┌────────────────────────────────────────────────────────┐
│                      RedShift (Android)                │
│                                                        │
│   Compose UI ──▶ Models (state) ──▶ Services           │
│                     │                 │                │
│   Telegram ID ──────┘                 ▼                │
│   REST API  ◀───── RedPillApiClient ── SubscriptionClient
│   DataStore ◀───────────────────── cached servers      │
│                                                        │
│   VpnService (TUN fd) ─▶ libsingbox.so (JNI)           │
│                                │                       │
│                                ▼                       │
│                    VLESS·VMess·Trojan·SS·Hy2·AWG       │
└────────────────────────────────────────────────────────┘
```

- **TUN** — VpnService создаёт интерфейс, fd передаётся в sing-box через JNI (`SingBoxNative.start(tunFd)`)
- **sing-box-lx** — работает внутри процесса приложения, без внешних процессов: `lib/arm64-v8a/libsingbox.so`
- **DNS** — DoH `8.8.8.8/dns-query` с detour на выбранный outbound (резолв до поднятия туннеля, без deadlock)
- **Трафик приложения** — собственные сокеты исключены из TUN и идут через локальный mixed-proxy (`127.0.0.1:10809`)

---

## 🧰 Технологии

| Компонент | Технология |
|-----------|------------|
| Язык | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| VPN | VpnService (Android API 24+) |
| Прокси-ядро | sing-box-lx 1.14.0 (JNI in-process) |
| Сеть | OkHttp, Moshi, Retrofit |
| Хранилище | DataStore Preferences |
| Фон | WorkManager |
| CI/CD | GitHub Actions (delegated-agp 9.1.1, JDK 17) |

---

## 🔧 Сборка

```bash
# Windows
gradlew.bat :app:assembleDebug

# Linux / macOS
./gradlew :app:assembleDebug

# Версия (влияет на versionCode)
APP_VERSION=0.2.21 ./gradlew :app:assembleDebug
```

APK появится в `app/build/outputs/apk/debug/app-debug.apk`.

> Подпись debug — локальный `debug.keystore` (в репозитории). Релизная сборка — через `KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD` из env.

---

## 📦 Релизы

Готовые APK — на странице [Releases](https://github.com/mindwork73/Redshift/releases). При каждом пуше в `main` GitHub Actions собирает debug-сборку, артефакт появляется в [Actions](https://github.com/mindwork73/Redshift/actions).

История изменений — в [`CHANGELOG.md`](./CHANGELOG.md).

---

## 📁 Структура

```
app/src/main/java/com/example/
├── MainActivity.kt               — точка входа, RedShiftState.init()
├── ui/
│   ├── Models.kt                 — глобальное состояние, серверы, подключение
│   ├── MainShell.kt              — каркас с нижней навигацией и оверлеями
│   ├── home/HomeScreen.kt        — главная: статус, сервер, трафик
│   ├── servers/ServersScreen.kt  — список серверов, пинг, выбор
│   ├── settings/                 — настройки + раздельное туннелирование
│   └── profile/ProfileScreen.kt  — профиль по TG ID
└── service/
    ├── RedShiftVpnService.kt     — VpnService + TUN
    ├── SingBoxManager.kt         — жизненный цикл sing-box
    ├── SingBoxConfigGenerator.kt — JSON-конфиг под каждый протокол
    ├── SubscriptionClient.kt     — парсинг v2ray/ss/hy2/vpn-подписок
    ├── RedPillApiClient.kt       — REST API redpillcloud.ru
    └── SubscriptionRefreshWorker.kt — автообновление подписки
```

---

## ⚠️ Известные ограничения

- sing-box в fd-режиме не трогает MTU интерфейса (MTU задаёт VpnService = 1280)
- AmneziaWG Hparams должны совпадать на NL/EU серверах
- VPN работает без `auto_detect_interface` (в fd-режиме netlink запрещён для app uid)
- Собственный пакет исключён из TUN — исходящий трафик приложения идёт через локальный SOCKS

---

## 📄 Лицензия

MIT — но код тесно связан с инфраструктурой RedPill Cloud (REST API, подписки) и бесполезен без неё.