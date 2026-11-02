# Handoff: фикс «холодного старта» olcRTC → встроить в Redshift (Android)

Задача для соседней сессии. Ниже всё, что нужно, чтобы внести готовый фикс в
приложение Redshift и пересобрать его. Фикс уже реализован, собран и проверен на
боевых серверах — задача свести его в клиент.

---

## 0. TL;DR

- **Симптом:** при подключении к «холодной» (свежей) комнате olcRTC (jitsi +
  datachannel) первый коннект часто падает и удаётся только со 2–3 раза.
- **Причина:** timing race. Клиент шлёт **одноразовый** `CLIENT_HELLO` сразу как
  открылся его мост, но серверный JVB-эндпоинт на холодной конференции
  регистрируется ~на секунду позже → JVB дропает бёрст, а клиент HELLO не
  перешлёт. Совпадает с апстрим-issue **#82 / #95**.
- **Фикс:** 3 файла в клиентском слое olcrtc (см. `olcrtc-coldstart-fix.patch`,
  +74/−36). Проверено: холодный старт на 4 свежих комнатах — 4/4 с первой
  попытки; полный `go test ./...` — зелёный.
- **Что делать:** пропатчить исходники olcrtc → пересобрать gomobile-AAR →
  заменить `D:\rd3\app\libs\olcrtc-mobile.aar` → пересобрать приложение.

---

## 1. Где что лежит

| Что | Где |
|---|---|
| Приложение Redshift (Android) | `D:\rd3` (git: `mindwork73/Redshift`) |
| Движок olcRTC внутри приложения | gomobile-AAR `D:\rd3\app\libs\olcrtc-mobile.aar` |
| Как подключён AAR | `app/build.gradle.kts`: `implementation(fileTree("libs"){ include("*.aar") })`; Go-класс = `mobile.Runtime` |
| Kotlin-обёртка URI | `app/src/main/java/com/example/service/OlcrtcUri.kt` |
| Использование движка | `app/src/main/java/com/example/service/RedShiftVpnService.kt` (`mobile.Runtime`, `setSocksPort`, `waitReady`, `setDeviceIDPath`, `SetLivenessOptions`, stop) |
| Исходники olcrtc (боевые) | NL `37.220.84.106:/root/olcrtc`, HEAD `cf9d3f7` (совпадает с ядром olcbox `08843d6`) |
| Готовый патч | `olcrtc-coldstart-fix.patch` (в этом же каталоге; копия на NL `/tmp/coldstart-fix.patch`) |
| Локальная копия исходников с фиксом | `C:\Users\mindw\AppData\Local\Temp\opencode\olcrtc-src` |

> SSH к NL: `& "C:\WINDOWS\System32\OpenSSH\ssh.exe" -i C:\Users\mindw\.ssh\id_ed25519 root@37.220.84.106 "<cmd>"`.
> Из PowerShell НЕ передавать inline-питон/`$()`/heredoc — писать скрипт локально, `scp`, запускать на сервере.

**Почему это можно сделать (в отличие от olcbox):** движок в Redshift — не
зашитый бинарник, а gomobile-AAR, который собирается из исходников olcrtc
командой `mage mobile`. Исходники у нас есть (NL `/root/olcrtc`, HEAD `cf9d3f7`).
API пакета `mobile` при этом не меняется, поэтому новый AAR — drop-in замена.

---

## 2. Патч (3 файла)

Патч лежит в `olcrtc-coldstart-fix.patch` (`git diff` от HEAD `cf9d3f7`, +74/−36).

Изменения:

1. **`internal/engine/jitsi/epoch.go`** — `acceptEpochFrame`: в ветке
   `requireTargetedPeer && onPeerData == nil` принимаем **незалоченный broadcast**
   и латчим его (порт апстрима `751b30b`), но **сохраняем** защиту от подмены
   targeted-эпохи из HEAD (латч **только** в broadcast-ветке).
2. **`internal/client/link.go`** — `waitForPeer` перенесён **перед**
   `openControlStream` и в `bringUpLink`, и в `tryReopenSession` (раньше HELLO
   уходил до ожидания пира → терялся).
3. **`internal/engine/jitsi/jitsi_test.go`** — тест
   `TestRequireTargetedPeerIgnoresBroadcastUntilConfirmed` заменён на
   `TestRequireTargetedPeerLatchesPreLatchBroadcastAndGuardsPeer`.

Полный diff:

```diff
@@ ПОЛНЫЙ DIFF СМ. ФАЙЛ olcrtc-coldstart-fix.patch (3 файла, +74/−36) @@
```

> Применять: `cd <olcrtc> && git apply /path/olcrtc-coldstart-fix.patch`
> (при конфликте — вручную; файл читаемый). После применения `git diff --stat`
> должен показать ровно 3 файла.

---

## 3. Шаги для соседней сессии

### Шаг 1. Достать исходники olcrtc и пропатчить

Вариант A (уже есть на NL, git-clean):

```bash
ssh root@37.220.84.106
cd /root/olcrtc
git status                 # должно быть чисто
# скопировать патч на сервер, напр.: scp olcrtc-coldstart-fix.patch root@NL:/tmp/
git apply /tmp/olcrtc-coldstart-fix.patch
git diff --stat            # ожидаемо: 3 files changed
```

Вариант B (локально): взять копию `C:\Users\mindw\AppData\Local\Temp\opencode\olcrtc-src`
(она уже содержит правки) или клонировать репо на нужный коммит и применить патч.

### Шаг 2. Прогнать тесты (критерий корректности патча)

```bash
cd <olcrtc>
go test ./...              # ОЖИДАЕТСЯ: всё зелёное (был зелёным при проверке)
```

Тулчейн: `go.mod` требует Go `1.26.3`; системный go старее → нужен
`GOTOOLCHAIN=auto` (тулчейн на NL: `/root/go/pkg/mod/golang.org/toolchain@v0.0.1-go1.26.3.linux-amd64`).

### Шаг 3. Собрать новый gomobile-AAR

Нужны: **gomobile** + **Android SDK/NDK** (CGO-сборка под android).

```bash
go install golang.org/x/mobile/cmd/gomobile@latest
gomobile init

# точная команда, которой собирается AAR (см. magefile.go, цель Mobile):
gomobile bind \
  -target=android \
  -androidapi 21 \
  -ldflags "-s -w -checklinkname=0" \
  -o olcrtc.aar \
  ./mobile
# либо просто: mage mobile   (кладёт в build/olcrtc.aar)
```

> Если AAR ранее уже собирался — тулчейн где-то настроен; переиспользовать его.
> Сервер NL gomobile/SDK пока НЕ имеет (там только `xcaddy`), сборку AAR,
> вероятно, надо делать на машине с Android-тулчейном.

### Шаг 4. Заменить AAR в приложении

```powershell
Copy-Item "D:\rd3\app\libs\olcrtc-mobile.aar" "D:\rd3\app\libs\olcrtc-mobile.aar.bak" -Force
Copy-Item "<build>\olcrtc.aar" "D:\rd3\app\libs\olcrtc-mobile.aar" -Force
```

### Шаг 5. Пересобрать приложение

```powershell
# versionCode/versionName поднять при необходимости (app/build.gradle.kts)
& "D:\rd3\gradlew.bat" :app:assembleRelease
# на выходе: D:\rd3\app\build\outputs\apk\release\*.apk
```

---

## 4. Проверка (критерий приёмки)

1. Установить APK на устройство.
2. Создать **свежую** olcRTC jitsi-комнату (например на egovm/mamba) и
   подключиться по `olcrtc://jitsi?datachannel@<room>#<key>$<name>`.
3. **Ожидание:** первый коннект проходит с первой попытки (без 2–3 ретраев).
4. Регресс-контроль: старая сборка (текущий AAR) на той же свежей комнате —
   воспроизводит падение; новая — нет.

Справочные живые конфиги (для теста): NL jitsi — `meet.egovm.ru/redpill-olcrtc-nl`;
EU jitsi — `meet.mamba.group/redpill-eu-7349c4b9`. Подписка:
`https://redpillcloud.ru/sub/olcrtc/123520489`.

---

## 5. Риски / примечания

- **Провайдер:** фикс затрагивает клиентский слой (`client/link.go` — порядок
  `waitForPeer`) и jitsi-движок (`epoch.go`). Тестировать на **jitsi +
  datachannel**. Если приложение настроено на `telemost`, поведение прочих
  провайдеров не меняется; проверять именно jitsi-локации.
- **API-совместимость `mobile`:** использованные приложением методы присутствуют
  в HEAD (`SetSocksPort`, `SetDeviceIDPath`, `WaitReady`, `SetLivenessOptions`,
  `Stop`, …) → новый AAR drop-in. Если AAR в `libs` собирался из более старого
  коммита — сверить, что Java-класс `mobile.Runtime` экспортирует те же методы.
- **Сервер менять не нужно:** фикс чисто клиентский; прод-сервисы olcrtc на
  NL/EU не трогаются (их исходники уже возвращены в git-clean).
- **Не трогать** `D:\Open code\Redshift-Android` (запрет из AGENTS.md) — работать
  в `D:\rd3`.
- Патч проверен на NL/EU: `go test ./...` зелёный; холодный старт 4/4 (egovm-a,
  egovm-b, mamba-a, mamba-b) с фикс-клиентом против ORIGINAL-сервера.
