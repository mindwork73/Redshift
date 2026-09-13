# RedShift → Google Play: подготовка релиза к публикации

> Статус: ключ сгенерирован, AAB собран (0.1.3 / versionCode 10300), анкеты не заполнены.
> Обновляй этот файл по мере продвижения в Play Console.

## 1. Текущее состояние (2026-09-13)

| Артефакт | Путь | Статус |
|---|---|---|
| Upload-ключ | `D:\rd3\my-upload-key.jks` (alias `upload`, RSA 4096, валиден до 2054) | ✅ сгенерирован, **в git не залит** (`.gitignore` содержит `*.jks`) |
| Пароли ключа | `C:\Users\mindw\AppData\Local\Temp\opencode\keystore_creds.txt` | ⚠️ **сохранить в надёжное место** (потеря ключа = нельзя выпускать обновления) |
| Release-скрипт | `D:\rd3\build-release.ps1` | ✅ в git: `.\build-release.ps1 0.1.3` |
| Release AAB | `D:\rd3\app\build\outputs\bundle\release\app-release.aab` (0.1.3 / 10300) | ✅ собран, подпись `upload` подтверждена |
| Debug APK | `D:\rd3\app\build\outputs\apk\debug\app-debug.apk` (0.1.2 / 10100) | ✅ выложен в GitHub Release `v0.1.2` |
| Git ветка | `arena/01a08824-redshift`, репо `git@github.com:mindwork73/Redshift.git` | ✅ `366e34a` |

### Ключевые факты версионирования
- VersionCode/Name берутся из env `APP_VERSION` в `app/build.gradle.kts:21-24`.
- При каждом релизе поднимать `APP_VERSION` (format `MAJOR.MINOR.PATCH`): `0.1.3 → 0.1.4 →`.

### Сборка подписанного AAB (заново)
```powershell
& "D:\rd3\build-release.ps1" 0.1.4   # повысить версию при новом релизе
```

---

## 2. Cheat-sheet по Play Console

1. `https://play.google.com/console` → регистрация ($25, разово, без рефанда).
2. Create app: **app name** `RedShift VPN`, **package name** `com.aistudio.redshift.rkzvpt`.
3. Upload `app-release.aab` (Production / Internal testing / Closed test).
4. Заполнить:
   - **App content → Privacy policy** → вставить URL (см. §3 — выложить страницу, например на `https://redpillcloud.ru/privacy`).
   - **App content → Data safety** → см. §5.
   - **App content → Ads** → без рекламы: `No`.
   - **App content → Target audience** → `18+` (VPN).
   - **App access permissions** → оставить без изменений.
   - **Data safety + India data** → по форме.
5. Проверить через **Policy declaration** вопросы про VPN (в `Data safety`, а также специальную секцию для VPN приложений, если потребуется).

> Заметка: единый аккаунт на всех членов команды. Для сложных категорий Play запрашивает "доказательство валидности" — для VPN нужны: работающая ссылка на privacy policy, корректная декларация FGS `specialUse` (уже стоит), и объяснение `QUERY_ALL_PACKAGES`.

---

## 3. Privacy Policy (текст для страницы / URL)

### EN
```
PRIVACY POLICY — RedShift VPN

Effective date: 2026-09-13

1. What RedShift is
RedShift is an Android VPN client ("the App") that lets you connect to
third-party VPN servers chosen from a subscription list provided by you.

2. Data processed
- No account creation is required. Authentication is optional via Telegram.
- RedShift stores locally on your device: server list from your
  subscription URL, your selection, and app settings. Nothing is uploaded
  unless you connect to a VPN server.
- When you use a VPN server, traffic routing/processing is handled by the
  VPN provider (the server you connect to), not by the App developer.
- Diagnostics: debug builds may write local logs; logs are not transmitted.

3. Permissions used
- VpnService: required to create the local VPN tunnel.
- INTERNET/ACCESS_NETWORK_STATE: connectivity.
- POST_NOTIFICATIONS: VPN status notifications.
- FOREGROUND_SERVICE / WAKE_LOCK: keep the tunnel alive on Android 14+.
- QUERY_ALL_PACKAGES: only to show you the list of apps for the optional
  split-tunnelling ("route apps through VPN / bypass VPN") feature.
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: prevent Doze from killing the
  tunnel (user must confirm on the dedicated system screen).

4. Third parties
- VPN servers are operated by the subscription provider; see their policy.
- sing-box/AmneziaWG/gomobile libraries are bundled as native components.

5. Data retention & deletion
- RedShift keeps no server-side user data. To remove all local data,
  uninstall the app ("Data deletion" — uninstallation).

6. Contact
- For data requests contact the developer: support via @redpillcloud_bot.
```

### RU
```
ПОЛИТИКА КОНФИДЕНЦИАЛЬНОСТИ — RedShift VPN

Дата вступления в силу: 13.09.2026

1. Что такое RedShift
RedShift — Android-VPN-клиент, позволяющий подключаться к сторонним VPN-
серверам, выбранным из списка подписки, которую предоставляет пользователь.

2. Обрабатываемые данные
- Регистрация аккаунта не требуется; вход (опционально) через Telegram.
- Локально на устройстве хранится: список серверов из вашей подписки,
  выбранный сервер и настройки приложения. За пределы устройства ничего
  не передаётся, кроме самого VPN-соединения, которое вы включаете.
- При подключении к VPN-серверу обработка трафика выполняется провайдером
  VPN (сервером), а не разработчиком приложения.
- Диагностика: debug-сборки могут писать локальные логи; они не передаются.

3. Используемые разрешения
- VpnService: для создания локального VPN-туннеля.
- INTERNET/ACCESS_NETWORK_STATE: работа с сетью.
- POST_NOTIFICATIONS: уведомления о статусе VPN.
- FOREGROUND_SERVICE / WAKE_LOCK: поддержание туннеля (Android 14+).
- QUERY_ALL_PACKAGES: только для списка приложений в функции раздельного
  туннелирования (выбор приложений «через VPN» / «мимо VPN»).
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: защита от Doze (согласие
  пользователя на системном экране).

4. Третьи стороны
- VPN-серверы управляются поставщиком подписки; см. его политику.
- Встроенные нативные компоненты: sing-box, AmneziaWG, gomobile.

5. Хранение и удаление
- Серверных данных пользователей RedShift не ведёт. Для удаления всех
  локальных данных — удалите приложение («Удаление данных» = деинсталляция).

6. Контакты
- Запросы по данным: разработчик, поддержка @redpillcloud_bot.
```

---

## 4. Описания для Play (краткие)

- **App name:** `RedShift VPN`
- **Short description (80 chars):** `Fast, reliable VPN client: WireGuard/VLESS/Hysteria2, split tunneling, auto-fastest server.`
- **Full description (≈4000 символов):** взять из README.md (раздел «Возможности»), добавить список протоколов и фич split tunneling + auto-select.

---

## 5. Data safety (форма Play) — ожидаемые ответы

| Вопрос | Ответ |
|---|---|
| Does your app collect or share any user personal data? | **No** (обработка трафика на стороне серверов провайдера; локальные данные не собираются приложением в облако) |
| Заявленные категории | (пусто либо только «App activity» — нет) |
| Is data encrypted in transit? | **Yes** (Android VPN traffic; открытые каналы к серверам используются провайдером) |
| Can users request data deletion? | **Yes → uninstall** (деинсталляция удаляет все локальные данные) |
| Is this app a VPN? | **Yes** (заполнить декларацию VPN в Play: описать зачем VpnService, что трафик обрабатывается провайдером, ссылка на privacy policy) |

Примечание: Play просит заполнить отдельную рамку про VPN-приложениям: зачем используется `VpnService`, что приложение не перехватывает трафик кроме самого туннеля, и т.п.

---

## 6. Политики, которые могут «завернуть» ап

| Тема | Статус | Что делать |
|---|---|---|
| `QUERY_ALL_PACKAGES` (`AndroidManifest.xml:12`) | ⚠️ красный флаг | Play требует: либо убрать, либо обосновать (split tunneling by apps). Мы его используем именно для этого. При отказе — убрать и показывать статичный список, но лучше обосновать. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (`AndroidManifest.xml:11`) | ⚠️ | Декларация в Data safety / «Declaration of use». Если ревью зарубит — удалить (сервис сам переживёт Doze, аккумулятор будет жить больше). |
| VPN-декларация | ⚠️ | Внутри Play Console — форма при загрузке AAB: обязательный текст про VPN. Ссылка на privacy policy обязательна; целевая аудитория 18+. |
| FGS `specialUse` (`AndroidManifest.xml:44`) | ✅ корректно | Декларация description понадобится при первом аплоаде (в Play) — `VPN service to route device traffic`. |
| Debug подпись | ✅ решено | Release ключ готов. |

---

## 7. Current blockers / Next steps

- [ ] Сохранить пароли ключа надёжно (у владельца).
- [ ] Регистрация в Play Console ($25).
- [ ] Создать приложение `com.aistudio.redshift.rkzvpt`.
- [ ] Загрузить `app-release.aab` (0.1.3+).
- [ ] Заполнить Data safety + Privacy policy URL + 18+.
- [ ] Подготовить скриншоты (2–8 шт.) и Feature graphic (1024x500), иконку 512x512.
- [ ] Пройти ревью VPN-заявки; при запросе — обосновать `QUERY_ALL_PACKAGES`.

---

## 8. Полезные команды

```powershell
# проверить подпись AAB (из расширенного bundle):
# aapt2 dump xmltree --file manifest/AndroidManifest.xml <base.apk>
# проверить, что ключ входа: keytool -list -v -keystore my-upload-key.jks

# быстрый тестовый релиз-путь без Play: собрать APK подписанный тем же ключом
$env:APP_VERSION="0.1.3"; $env:KEYSTORE_PATH="D:\rd3\my-upload-key.jks"
$env:STORE_PASSWORD="..."; $env:KEY_PASSWORD="..."
& "D:\rd3\gradlew.bat" ":app:assembleRelease"
# APK лежит в app/build/outputs/apk/release/
```