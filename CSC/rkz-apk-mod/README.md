# rkz-apk-mod — AmneziaWG-модификация приложения RedShift VPN

Набор патчей и инструментов, превращающих клиент **RedShift VPN** в полноценный
AmneziaWG/WireGuard-клиент на ядре [sing-box-lx](https://github.com/Leadaxe/sing-box-lx),
плюс раздельное туннелирование по приложениям (per-app split tunneling).

---

## О приложении

| Параметр | Значение |
|---|---|
| Название | RedShift VPN |
| Пакет | `com.aistudio.redshift.rkzvpt` |
| versionName | 1.0 |
| Технологии | Kotlin, Jetpack Compose UI, DataStore, WorkManager |
| Структура | 7 dex-файлов (`classes.dex`…`classes7.dex`), ~67 MB APK |

### Архитектура подключения

Ключевой класс — `com.example.service.RedShiftVpnService` (VpnService).
Ядро VPN — встроенный бинарник **sing-box** из `assets/singbox/arm64-v8a/sing-box`
(тот же файл продублирован как `lib/arm64-v8a/libsingbox.so`), запускается через
`ProcessBuilder` (`SingBoxManager`).

Два режима работы:

1. **TUN fd-passing (основной)** — для WireGuard/AmneziaWG-серверов
   (`isAmneziaProtocol`: протокол содержит `AMNEZIA`/`AWG`/`WIREGUARD`/`WG`):
   - сервис создаёт TUN сам через `VpnService.Builder` (`connectTunOnly()`),
   - fd интерфейса передаётся ядру через переменную окружения **`VPN_SERVICE_FD`**
     (`SingBoxManager.startWithTunFd`),
   - конфиг генерирует `SingBoxConfigGenerator.generateConfig()`: tun-inbound
     (`auto_route:false`, `stack:"system"`, sniff) + DNS + outbound (AWG) +
     route с rule-set `geoip-ru.srs` (RU-трафик напрямую) + Clash API `127.0.0.1:9090`.
2. **Mixed SOCKS fallback** — если запуск ядра не удался: mixed-inbound
   `127.0.0.1:10808`, а приложение гоняет собственный userspace TCP/IP-стек
   (`handleTcpPacket`→SOCKS5-handshake в ядро, `forwardUdp` — UDP напрямую через
   защищённый сокет **мимо туннеля**, `resolveDns` — прямой DNS).
   Этот режим медленный и небезопасный для UDP/DNS — именно поэтому важно,
   чтобы основной режим работал.

Прочее: подписки (`SubscriptionClient`, `SubscriptionRefreshWorker`), серверы
(vless/vmess/trojan/shadowsocks/hysteria2/wireguard-семейство), настройки в
DataStore (`files/datastore/redshift_settings.preferences_pb`).

### Проблемы стоковой версии

- Стоковое ядро — старый sing-box, который **не принимает** `VPN_SERVICE_FD`
  и не понимает современную схему wireguard-endpoint → основной режим падал и
  всегда срабатывал fallback на медленный mixed-режим.
- AmneziaWG 2.0 (диапазоны `h1-h4`, CPS-пакеты `i1-i5`) не поддерживался вовсе.

---

## Что сделано

### 1. Ядро sing-box-lx (Go)

Патчи: [`go-patches/sing-box-lx.patch`](go-patches/sing-box-lx.patch)

| Файл | Изменение |
|---|---|
| `cmd/sing-box/compat_lx.go` (новый) | Миграция легаси-конфигов: wireguard **outbound → endpoint**, сырой hex `i1-i5` → CPS `<b 0x..>`, старые поля tun (`inet4_address` и пр.). Идемпотентно |
| `cmd/sing-box/cmd_run.go` | Хук `applyLegacyCompat()` при чтении конфига (1 строка, помечена `// lx:`) |
| `protocol/direct/outbound.go` | Nil-guard `InterfaceMonitor()` — под app-uid netlink-монитор не создаётся, без guard'а SIGSEGV |
| `protocol/tun/inbound.go` | Поддержка `VPN_SERVICE_FD`: attach к готовому TUN вместо `/dev/net/tun`; фикс паники DNS-hijack на адресе `/32`; расширение префикса до `/30` для system-стека (реальные адреса fd не трогаются); форс `auto_route/strict_route = false` |

Сборка (обязателен **go1.26.6** — pinned в go.mod):

```
CGO_ENABLED=0 GOOS=android GOARCH=arm64 \
go build -trimpath \
  -tags "with_gvisor,with_quic,with_dhcp,with_wireguard,with_utls,with_clash_api,badlinkname,tfogo_checklinkname0,with_xhttp,with_awg,with_lx_command,with_openvpn,with_openconnect" \
  -ldflags "-X 'github.com/sagernet/sing-box/constant.Version=<ver>' -checklinkname=0 -s -w" \
  ./cmd/sing-box
```

### 2. Smali-патчи приложения

Только `classes4.dex`. Диффы: [`smali/`](smali/)

| Патч | Суть |
|---|---|
| `RedShiftVpnService.patch` | MTU обоих Builder'ов 1500 → **1280** (равен MTU туннеля WG — иначе большие QUIC/TCP-сегменты резались); вызов `SplitTunnelHelper.apply()` перед обоими `establish()` |
| `SingBoxConfigGenerator.patch` | MTU tun-inbound в генерируемом конфиге 9000 → **1280** |
| `SplitTunnelHelper.smali` (новый) | Читает `files/split_apps.txt` и применяет `addDisallowedApplication` / `addAllowedApplication`; каждая строка — отдельный try/catch, ошибки файла игнорируются |

### 3. Раздельное туннелирование по приложениям

Файл: `/data/data/com.aistudio.redshift.rkzvpt/files/split_apps.txt`
(шаблон: [`scripts/split_apps.txt`](scripts/split_apps.txt))

```
exclude <package>   # приложение идёт мимо VPN
include <package>   # ТОЛЬКО перечисленные идут через VPN
# комментарии игнорируются
```

Применяется при каждом подключении VPN. Без файла поведение не меняется.

### 4. Сборка модифицированного APK

```bash
# 1. Расшифровать APK (только исходники; ресурсы остаются raw)
apktool d --no-res base.apk -o apkdec

# 2. Наложить smali-патчи (см. smali/), добавить SplitTunnelHelper.smali

# 3a. Удалить ВСЕ смали-каталоги КРОМЕ smali_classes4,
#     а оригинальные classes{,2,3,5,6,7}.dex положить в apkdec/unknown/
#     ВАЖНО: см. «Грабли» ниже — полная пересборка dex ломает classes6!

# 3b. Собрать
apktool b apkdec -o base-smali.apk      # перед сборкой чистить apkdec/build/

# 4. Внедрить бинарники ядра
python scripts/repack.py base-smali.apk libsingbox.so base-mod.apk

# 5. Подписать (debug-ключ, v1+v2+v3)
uber-apk-signer --apks base-mod.apk --out signed/
```

Установка: обычный `adb install -r` (обновление, данные сохраняются).
На vivo переключатель «Установка через USB» сбрасывается после каждого
использования — тогда ставить вручную из `/sdcard/Download/`.

---

## Грабли (важно для воспроизводимости)

1. **Полная пересборка всех dex через apktool ломает приложение**:
   `IncompatibleClassChangeError: Found interface DrawScope, but class was
   expected` в `classes6.dex` (Compose). Решение — пересобирать только
   `classes4.dex`, остальные класть в проект нетронутыми через `unknown/`.
2. Перед `apktool b` удаляйте каталог `apkdec/build/` — иначе дубликаты
   записей dex в APK (`duplicate entry: classes.dex`).
3. `smali const/4` вмещает только −8…7 (для `0x8` нужен `const/16`);
   дальние переходы — `goto/16`.
4. Тест fd на устройстве без реального VpnService: скрипты
   [`scripts/tfd-test.sh`](scripts/tfd-test.sh) + `tfd.json`. Ошибка
   `bind: cannot assign requested address` в конце — ожидаема (fd=/dev/null);
   главное, что в логе есть `attaching tun inbound to VPN_SERVICE_FD=` и
   `started at tunN`.

## Статус

- ✅ Ядро собирается, fd-passing работает (проверено на устройстве)
- ✅ APK собирается, ставится обновлением, приложение стартует без крашей
- ⏳ Проверка скорости основного режима после включения VPN пользователем
