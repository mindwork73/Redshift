# RedShift — ФРОНТ "LIQUID GLASS DARK". Полный чертёж для реализации

Это единственный источник правды для **полного переписывания** интерфейса RedShift.
Вся работа выполняется ПО ЭТОМУ ДОКУМЕНТУ. Любое визуальное решение, не описанное здесь,
берётся по принципу «максимально близко к iOS 27 (Liquid Glass, тёмная тема)».

Русский язык документа — по желанию замени English equivalents при реализации,
сохранив смысл. Файлы, классы, ответы/состояния — называй по коду (Kotlin identifiers).

---

## 1. Цель

Переписать ВЕСЬ слой интерфейса (`com.example.ui`) **с нуля** в едином стиле
«тёмный Liquid Glass»: полупрозрачные стеклянные поверхности, глубокий чёрный фон,
iOS-подобная типографика, одна сдержанная акцентная гамма. Никаких «кусков старого»:
старый дизайн удаляется полностью.

Текущее состояние интерфейса — мусор из слоёв разных поколений. Его выбрасываем целиком.

Требования результата:
- собирается одной командой `./gradlew :app:assembleDebug` без правок build-файлов;
- не задевает никакой код вне `com.example.ui` (см. раздел «ЗАПРЕТЫ»);
- на экранах отображаются только **реальные данные** (никаких хардкодов чисел,
  счётчиков, статусов «премиум» по догадке — раздел «ЗАПРЕТ ФЕЙКОВ»);
- интерфейс полностью на русском/английском и ещё 18 языках через `Trans.get(key)`.

---

## 2. ЗАПРЕТЫ (критично, иначе ломается прод)

**НЕЛЬЗЯ трогать (это работает и это ядро):**

- весь пакет `com.example.service/*` (VPN, sing-box, DataStore, API-клиент, воркеры);
- `app/src/main/java/com/example/MainActivity.kt`; `RedShiftApp.kt`;
- **`com.example.ui.Models.kt`** — содержит `RedShiftState`, `Server`, `Subscription`,
  `RoutingRule`, `ConnectionState`, `RoutingMode`. Его не удалять, не переписывать,
  не менять публичный API. Остаётся как есть.
- **`com.example.ui.Localization.kt`** — `AppLanguage`, `LocalizationState`, `Trans`.
  Без изменений публичного API. (внутрь словаря новых ключей можно добавить — п. 5.6)
- `AndroidManifest.xml`, `build.gradle.kts`/`build.gradle`, `gradle.properties` —
  НЕ менять. Библиотеки/зависимости не добавлять.
- `res/` кроме случаев, оговорённых ниже — НЕ менять (иконка приложения остаётся).
- Логику авторизации/подключения/импорта НЕ переписывать — только вызывать
  существующие методы `RedShiftState` (раздел 4.4).
- `MyApplicationTheme` и точки входа сохранят сигнатуры:
  - `com.example.ui.MainAppContainer()` — корневой композируемый (вызывается из MainActivity);
  - `com.example.ui.theme.MyApplicationTheme` — тема (вызывается из MainActivity).

**Что МОЖНО/НУЖНО полностью удалить** (старый фронт, мусор):
`Components.kt`, `Screens.kt`, `HomeScreenPremium.kt`, `ServersScreenPremium.kt`,
`SettingsScreenPremium.kt`, `SubscriptionsScreenPremium.kt`, `RoutingRulesScreenPremium.kt`,
`OnboardingScreenPremium.kt`, `AdminDashboardScreenPremium.kt`, `PlanetCanvas.kt`,
`ServerCoords.kt`, папка с VideoBackground (если есть `VideoBackground.kt`). Вместо них —
новый набор файлов по разделу 9. `ui/theme/*` переписывается начисто (п. 5).

---

## 3. Данные фронта (что реально есть для отображения)

Все поля ниже — **реальные рантайм-значения**, читать их напрямую из `RedShiftState`
и моделей. Ничего не придумывать.

### 3.1 `object RedShiftState` (com.example.ui)

Всё — `mutableStateOf`, реактивно.

- `connectionState: ConnectionState` — как сейчас (`DISCONNECTED | CONNECTING | CONNECTED`)
- `selectedServerId: String`
- `downloadSpeed: Double`, `uploadSpeed: Double` — КБ/с (kb/s)
- `sessionDurationSeconds: Long`
- `totalDataUsedMb: Double`
- `servers: SnapshotStateList<Server>` — реальный список доступных серверов
- `recentServers: SnapshotStateList<Server>` — недавние (макс 5)
- `routingMode: RoutingMode` — `GLOBAL | RULE | DIRECT`
- `isOnboarded: Boolean` — пройден ли онбординг
- `isLoggedIn: Boolean`, `telegramToken: String` (tg id), `userInfo: UserInfo?`
- `subscriptionPlan: String` — название тарифа («Premium» и т.п.)
- `subscriptionExpiry: String` — дата окончания (читать как строку, формат уже готов)
- `subscriptionUrl: String`
- Настройки (все — state): `startOnBoot`, `vpnNotification`, `killSwitch`, `autoReconnect`,
  `localPort: Int`, `dnsProvider: String`, `allowLan`, `ipv6Support`, `muxEnabled`,
  `muxConcurrency: Int`, `latencyThreshold: Int`, `bypassLocal`, `bypassLan`,
  `bypassChina`, `bypassRussia`, `blockAds`
- `isImporting: Boolean`, `importError: String?`, `loginError: String?`, `isLoadingUser: Boolean`
- `autoRefresh: Boolean`, `autoRefreshInterval: Int` (часы),
  `cachedServerCount: Int`, `lastRefreshTime: Long`
- `sortByPing: Boolean`
- `apiBaseUrl: String` (`https://api.redpillcloud.ru`)

### 3.2 `data class Server`

Поля для отображения: `id`, `flag` (эмодзи), `name` (локализова-нейтральное), `protocol`
(например `VLESS`, `AWG`, `Hysteria2`, `TUIC`, `Trojan`, `Shadowsocks`), `address`, `port`,
`latency: Int` (мс, 0 = ещё не измерен), `isCustom: Boolean`, `subscriptionUrl`.
Остальные `sub*` поля — конфиг, UI их не показывает.

### 3.3 Авторизация (com.example.service)

- `RedPillApiClient(baseUrl, adminToken)`; `apiClient.getUser(tgId): UserInfo?`,
  `getSubscription(tgId): SubInfo?`. Использовать только через методы `RedShiftState`
  (`login`, `refreshUserData`, `importSubscription`) — напрямую в UI не вызывать.
- `UserInfo(userId, username, createdAt, subscription: SubInfo?, deviceCount: Int)`
- `SubInfo(id, tariff, status, region, devicesLimit, startsAt, expiresAt, devices: List<DeviceInfo>)`
  — `devicesLimit` реальный лимит устройств (для экрана профиля).
- `DeviceInfo(deviceId, subscriptionId, userId, deviceIndex, deviceName, region, status)`

### 3.4 Методы `RedShiftState` (вызывать, НЕ переписывать)

- `toggleVpn()` — подключить/отключить выбранный сервер
- `getSelectedServer(): Server?`
- `markServerUsed(serverId)`, `removeServer(serverId)`, `pingAllServers()`
- `login(tgId: Int)`, `refreshUserData(tgId: Int)`, `logout()`
- `importSubscription(url: String)` — импорт по ссылке (vpn:// или подписной URL)
- `setLanguage(lang: AppLanguage)`, `setAutoRefreshEnabled(enabled, intervalHours)`

### 3.5 `LocalizationState` / `Trans` (com.example.ui, не трогать)

- `LocalizationState.currentLanguage: AppLanguage`
- `LocalizationState.formatSpeed(speed: Double): String` — локализованная скорость
- `LocalizationState.formatDate(timestamp: Long): String`
- `Trans.get(key: String): String` — перевод; ключи смотреть в Localization.kt.
  Тестировать ключи обязательно (примеры: `premium`, `connect`, `disconnect`, `settings`,
  `servers`, `routing_rules`, `profile`, `subscription`, `traffic`, `language`, `version`,
  `support`, `logout`, `logout_confirm`, `onboarding_*`, `status_connected` — при
  отсутствии нужного ключа добавлять по правилу п. 5.6).

---

## 4. Запрет фейков

- Никогда не подставлять числа серверов, «12 servers», вымышленные пинги 99мс,
  поддельный премиум-статус, фейковые скорости. На экране — то, что в состоянии.
  Если данных нет (например, latency = 0) — показывать «—».
- «Premium» — только если `RedShiftState.subscriptionPlan` содержит «Premium»/«RedPill Premium»
  или контент тарифа пользователя. Обычный пользователь видит обычный интерфейс
  (без бриллиантов/доминации). Убрать любую эвристику «free/premium по порядку серверов».
- Нет значения — показывай нейтральное место/прочерк, НЕ цифру-заглушку.

---

## 5. Дизайн-система «Liquid Glass Dark»

### 5.1 Общие принципы

- Фон сцены — сплошной глубокий, почти чёрный. Контент лежит «на стекле».
- «Стекло» = полупрозрачная поверхность чуть светлее фона + тонкая светлая обводка
  + лёгкий верхний блик (стоит только у верхнего края карточки).
- Прозрачность реализуем обычной альфой и градиентами (никаких размытий в рантайме —
  Compose-блур дорогой; имитируем): поверхность = вертикальный градиент
  `fill(alpha 0.05→0.09)` поверх чёрного + `border(0.5–1dp, белый alpha 0.14)`.
- Все углы: скругления по шкале (п. 5.4). Никаких острых углов.
- Никаких ярких неоновых заливок целиком — акцент используется точечно
  (кнопка подключения, индикаторы, маленькие pill-бейджи).

### 5.2 Токены (пак `com.example.ui.theme`)

Обнов версии писать в `Color.kt` как `val` верхнего уровня, группировать в `VpnColors`.

| Токен | Значение | Назначение |
|---|---|---|
| Background | `#050507` | фон всей сцены |
| BackgroundGradientTop | `#0D1117` | старт вертикального градиента Home |
| BackgroundGradientBottom | `#050507` | конец градиента |
| GlassFill | `#FFFFFF` alpha 0.07 | заливка стеклянных карточек |
| GlassFillStrong | `#FFFFFF` alpha 0.11 | заливка активных элементов |
| GlassBorder | `#FFFFFF` alpha 0.16 | обводка стекла |
| GlassBorderTop | `#FFFFFF` alpha 0.28 | ближе к верхнему краю (блик) |
| TextPrimary | `#FFFFFF` | основной текст |
| TextSecondary | `#A2A2AF` | вторичный текст |
| TextTertiary | `#6E6E78` | подписи, плейсхолдеры |
| Accent | `#FF3B30` | фирменный «красная таблетка», кнопка подключения |
| AccentSoft | `#FF3B30` alpha 0.14 | фон подписи на акценте |
| Success | `#30D158` | подключено, зелёные индикаторы |
| SuccessSoft | `#30D158` alpha 0.14 | фон pill-«Подключено» |
| Warning | `#FFD60A` | пинг 100–250 мс, предупреждения |
| Danger | `#FF453A` | пинг >250 мс, ошибки |
| NavBarFill | `#0A0A0C` alpha 0.7 | нижняя навигация (подсветка не должна перекрывать контент) |
| Divider | `#FFFFFF` alpha 0.08 | разделители |

Единые смыслы:
- пинг `0` → «—»; `1..99` → Success; `100..250` → Warning; `>250` → Danger;
- подключён → Success; connecting → Accent/Warning; выкл → нейтральный.

### 5.3 Типографика (`Type.kt`)

iOS-подобная, системный шрифт (`FontFamily.Default`):

| Стиль | Размер/вес | Применение |
|---|---|---|
| Timer | 56sp, Light, `fontFeatureSettings="tnum"` | таймер сессии на Home |
| Display | 32sp, Bold | главный заголовок экрана |
| Title | 20sp, SemiBold | заголовки карточек |
| Body | 16sp, Regular | основной текст |
| BodyMedium | 15sp, Medium | активные значения, метрики |
| Caption | 13sp, Regular | подписи, вторичное |
| CaptionMedium | 12sp, Medium | бейджи, кейпы |
| Mono | 14sp, Regular | IP-адреса, порты (есть `FontFamily.Monospace` или tnum) |

Интервалы: заголовки межстрочные по умолчанию Compose.

### 5.4 Радиусы / отступы / толщины (`Theme.kt`/tokens по соглашению)

- Скругления: `Small 12`, `Medium 16`, `Large 20`, `XLarge 28`, `Pill 999`
- Отступы: сетка 4 → `4,8,12,16,20,24,32`
- Кнопка подключения — круг, диаметр `192.dp`
- Элемент списка серверов — высота `72.dp`
- Межсекционное расстояние `32.dp`

### 5.5 Иконки

Брать только из уже подключённого набора `androidx.compose.material.icons.filled`
(они уже в зависимостях). Нужный минимальный набор:
`PowerSettingsNew`, `Shield`, `Globe`, `Public`, `Menu`, `Settings`, `Person`,
`ArrowBack` (AutoMirrored), `ArrowForward`, `Refresh`, `Add`, `Close`, `Check`, `Search`,
`ChevronRight`, `VerifiedUser`, `Notifications`, `Lock`, `SdStorage`, `Wifi`,
`DataUsage`, `Info`, `Cloud`, `MoreVert`, `Delete`, `Logout`.
Флаги серверов — эмодзи из `Server.flag` (не рисовать).

### 5.6 Перевода (i18n)

Все видимые строки — через `Trans.get("ключ")`. Новые ключи добавлять в словарь
`Trans.strings` (Localization.kt): обязательно секции `en` и `ru`; остальные языки —
можно зеркалить ключ `en` (правило «минимум en+ru, желательно все»).
Ключи — короткие `snake_case`. Одна строка = один ключ.

---

## 6. Навигация

- Нижний таб-бар из 4 вкладок: **Home, Servers, Settings, Profile**.
- Обязательные иконки+подписи через `Trans.get` (`nav_home`, `nav_servers`, `nav_settings`, `nav_profile`).
- Активная вкладка: иконка Accent / `TextPrimary`; неактивная — `TextTertiary`.
- Контейнер: `Scaffold` с `bottomBar = RedN` (см. компонент 5), контент с учётом
  `paddingValues`. Переключение вкладок через `AnimatedContent` (fade, 250ms) или
  простой `Box`/`when` без анимации (на твоё усмотрение, но без «перелетаний»).
- Онбординг: если `!RedShiftState.isOnboarded` — показать онбординг (п. 8.0)
  поверх навигации (без табов). После завершения `RedShiftState.isOnboarded = true`.
- Модальные окна (подключение деталей сервера, выбор языка, импорт) — `ModalBottomSheet`.
  Системный «назад» закрывает лист/вкладку: использовать `BackHandler`.
- Нет глубоких внутренних стеков — весь контент вкладок живёт на своём экране.

---

## 7. Компоненты (примитивы) — единый стиль

Все компоненты — в файле `ui/components/` (см. п. 9). Имена новых классов — без суффикса
«Premium». Стиль везде одинаковый:

1. **`GlassSurface`** — базовое стекло: `clip(RoundedCornerShape(Large))`,
   `background(Brush.verticalGradient(listOf(White.alpha(0.09), White.alpha(0.05))))` +
   верхний блик (тонкая линия 0.6dp белого alpha 0.22 по верхней кромке, если уместно),
   `border(0.5–1.dp, White.alpha(0.16))`. Тень: мягкая `alpha 0.4` чёрная.
2. **`Screen`** — вертикальный контейнер экрана: `Box(fillMaxSize)` с фоном
   градиента `BackgroundGradientTop→BackgroundGradientBottom`, внутри `LazyColumn`
   (или `Column` с `verticalScroll`) с `navigationBarsPadding + statusBarsPadding`.
   Контентный паддинг `24.dp` горизонтально, `20.dp` по вертикали.
3. **`ScreenHeader`** — заголовок экрана: имя в `Display`, слева/справа опц. иконка-кнопка.
4. **`NavBar`** — нижняя навигация: контейнер `AlphaNav`, высота 80.dp(+insets),
   `navigationBarsPadding` внутри, таб-итем 4, разделитель сверху `Divider`.
5. **`ListItem`** — строка списка: высота `64–72.dp`, слева иконка (желательно овал-фон),
   по центру заголовок+подпись, справа значение и chevron. Разделитель снизу,
   кроме последнего элемента в секции.
6. **`SectionTitle`** — заголовок секции: `CaptionMedium`, `TextTertiary`, с отступом.
7. **`ConnectSwitch`** / **`ToggleRow`** — iOS-переключатель: стандартный
   `Material3 Switch` с главным цветом трека = Accent/Success, цвет гайки белый.
   (Стинш системный вид — не переизобретай).
8. **`Pill`** — скруглённый бейдж: фон `XSoft`, текст `CекстCaptionMedium`.
   Вариации: протокол сервера, пинг, статус.
9. **`GradientButton`** — основная кнопка: высота 56, радиус 16, фон
   `Brush.horizontalGradient(Accent→AccentDark)` (или Accent), текст белый Bold, `Body`.
   Для `enabled=false` — приглушённая (`TextTertiary`-фон, без градиента).
10. **`PowerButton`** — круглая кнопка подключения на Home:
    диаметр 192, два кольца пульса (анимация в разделе 8.1), внутри состояние-иконка.
    Пульсация реализуется через `rememberInfiniteTransition`: внешнее кольцо
    масштабируется 1→1.08 и прозрачность 0.3→0 поверх первого кольца. Только на Home.
11. **`StatCell`** — ячейка метрики (download/upload/ping): цифра в `BodyMedium`,
    подпись `Caption`/`TextTertiary`.

Тёмная тема включается жёстко (дизайн тёмный всегда; системные настройки iOS
не влияют на палитру). `MyApplicationTheme` по умолчанию `darkColorScheme` с токенами выше.

---

## 8. Экраны

### 8.0 Онбординг (первый запуск)

Показ: `!RedShiftState.isOnboarded`.
Структура: 3 слайда по горизонтали (`HorizontalPager`):
1) «Безопасный интернет всегда» — иконка Shield в стеклянном круге;
2) «Подписка из Telegram-бота» — короткое объяснение: бот @redpillcloudbot выдаёт ссылку;
3) «Готово к подключению» — кнопка `GradientButton` «Начать» → `onFinished()`.

UI: затемнение – фоновый градиент + стеклянные карточки. Каждая страница —
заголовок (Display), подзаголовок (Body, TextSecondary), индикатор точек (3).
Кнопка внизу: на первых двух страницах «Далее», на третьей «Начать».
Свайп работает всегда. Все строки через `Trans.get` (ключи `onboarding_*`).

### 8.1 Home (вкладка 1)

Части сверху вниз:

```
┌───────────────────────────────────────────┐
│  [≡]  RedShift          [◈ Premium pill]  │ header: меню(не сейчас) | имя | тариф
├───────────────────────────────────────────┤
│                                           │
│           (пример: 00:12:34)              │ Timer (56sp tnum, центр)
│              Connected                    │ статус (Success/Accent/neutral)
│              secured, your connection     │ подписи (Caption, TextSecondary)
│                                           │
│          ╭───────────────╮                │
│        ╭─╯   PowerButton ╰─╮              │ (192dp, пульс-кольца)
│        ╰───────────────────╯              │   icon: Power / Spinner / Check
│                                           │
│   [РОССИЯ/НЛ]  RedPill NL                 │ текущий сервер — кросс индивидуально?
│   37.220.84.106 · 12ms                    │ (tap → Servers вкладка)
├───────────────────────────────────────────┤
│  ╭─ GlassSurface ────────────────────────╮│
│  │  ↓ Download       ↑ Upload    Ping    ││ StatCells ×3 (реальные)
│  │  12,3 МБ/s         8,1 kБ/s   12ms    ││
│  ╰───────────────────────────────────────╯│
│                                           │
│  ╭─ GlassSurface ────────────────────────╮│
│  │  ◉ Protected                          ││ карточка статуса (Success icon+текст)
│  │  VPN is active · 1,2 ГБ used          ││
│  ╰───────────────────────────────────────╯│
└───────────────────────────────────────────┘
```

Данные:
- сервер: `RedShiftState.getSelectedServer()` (имя, флаг, адрес, latency);
- таймер: `sessionDurationSeconds` — формат `HH:MM:SS` или `MM:SS`;
- скорости: `downloadSpeed` / `uploadSpeed` / `LocalizationState.formatSpeed`;
- трафик сессии: `totalDataUsedMb` → ГБ/МБ с одним знаком;
- статус: по `connectionState`.

Поведение:
- кнопка колючает `RedShiftState.toggleVpn()`;
- при `CONNECTING` — спиннер внутри кнопки, кольца пульсируют Warning;
- при `CONNECTED` — иконка `PowerSettingsNew`/`Check` Success, кольца зелёные;
- при `DISCONNECTED` — иконка `PowerSettingsNew`, кольца нейтральные;
- тап на карточке сервера → переход вкладки "Servers";
- scroll: весь экран — `LazyColumn`? нет: без скролла (контент ровно на один экран,
  дизайн просчитан под высоту 800+dp; на малых экранах допускается verticalScroll).

Пункт меню (≡) НЕ реализовывать — вкладки уже есть внизу. (закрывающий «≡» — если
в контексте подписки редк. — можно опустить).

### 8.2 Servers (вкладка 2)

Структура:

```
┌───────────────────────────────────────────┐
│  Серверы                    [⟳] [＋]      │ ScreenHeader (без спины-стрелки)
├───────────────────────────────────────────┤
│  ╭─ GlassSurface ────────────────────────╮│
│  │  SectionTitle: Все серверы (N)        ││
│  │   ┌ item ──────────────────────────┐  ││
│  │   │ [флаг] RedPill NL   [VLESS] [12ms]││  ← активный выделен (фон AccentSoft/галка)
│  │   │        37.220.84.106              ││
│  │   └──────────────────────────────────┘  │
│  │   ... (строки реального списка)        ││
│  ╰───────────────────────────────────────╯│
└───────────────────────────────────────────┘
```

Данные:
- `RedShiftState.servers` (реальный список! без лимитов «take(2)», без деления
  на free/premium, без подставных «N servers»);
- сортировка: сначала активный сервер, затем остальные; пинг сортировка на по желанию
  через `sortByPing`, но не обязательно;
- если `servers` пуст — экран-плейсхолдер «Подписок нет — добавь ссылку в настройках»
  + кнопка «Добавить подписку» → открывает импорт (п. 8.3, сегмент Subscription).

Строка сервера:
- слева флаг (эмодзи из `flag`, 28sp в круге 40dp стекла);
- заголовок `name` (Body); подпись `address` (Caption, TextTertiary);
- справа: `ProtocolPill` (протокол), `PingPill` (латентность), chevron/«подключить»;
- tap строки: `selectedServerId = server.id` + если не подключён — `toggleVpn()`;
  отмеченный активный сервер: фон `AccentSoft`, справа иконка `Check` Success от пинга.
- долгий тап на кастомном сервере (`isCustom == true`) → меню «Удалить» → `removeServer`.
- иконка `⟳` → `RedShiftState.pingAllServers()`;
- иконка `＋` → открыть «Добавить сервер» (см. ниже).

«Добавить сервер» — `ModalBottomSheet`: поле ввода URL (`OutlinedTextField`, тёмная
тема), кнопка «Импортировать»; нажатие: `RedShiftState.importSubscription(url)`;
показ состояния `isImporting` (спиннер) и `importError` (текст Danger). Пликсы
протоколов — из `server.protocol` пильным (VLESS, AWG, Hysteria2, TUIC, Trojan, SS).

### 8.3 Settings (вкладка 3)

Секции (все реальные поля `RedShiftState`):

1. **Подписка**
   - строка «Подписка» → открывает лист «Подписка»: тариф `subscriptionPlan`,
     срок `subscriptionExpiry`, текущая подписка-URL `subscriptionUrl` (показ кратко),
     кнопки: «Обновить» (`importSubscription(subscriptionUrl)`),
     «Удалить ссылку» (`subscriptionUrl = ""`, чистка).
   - строка «Импорт» → та же модалка, что из Servers (URL → `importSubscription`).
2. **Подключение**
   - `Start on boot` → `startOnBoot`
   - `Auto reconnect` → `autoReconnect`
   - `Allow LAN` → `allowLan`
   - `IPv6` → `ipv6Support`
   - `Notifications` → `vpnNotification`
   - `Kill switch` → `killSwitch`
3. **Routing** (Rule-based)
   - строка «Режим (GLOBAL/RULE/DIRECT)» — сегменты (3-дольный переключатель);
   - `Bypass local` → `bypassLocal`; `Bypass LAN` → `bypassLan`;
     `Bypass China` → `bypassChina`; `Bypass Russia` → `bypassRussia`;
     `Block ads` → `blockAds`.
4. **Язык** — строка «Язык/Language» → лист выбора (все `AppLanguage` из п. 3.5,
   выбор вызывает `LocalizationState.currentLanguage`), показывает «当前 язык»;
   «Следовать за системой» — убрать (всегда локальный выбор; currentLanguage не меняется).
5. **Автообновление** — `Auto refresh` → `autoRefresh` + интервал (стоввый выбор 6/12/24ч —
   `autoRefreshInterval`), строка «Разрешать обновления ×N раз в день».
6. **Данные** — строка «Сбросить кеш» кнопка → `RedShiftState.resetDefaultData()`
   + confirmation (AlertDialog «Сбросить?»).
7. **О приложении** — версия (BuildConfig.VERSION_NAME), порт локального `localPort`
   (Info), «Поддержка» — бот (можно строкой `@redpillcloud_bot` копия).

Разделы с `ToggleRow`, где применимо. Никаких выдуманных настроек — только поля из 3.1.
При записи полей: изменять соответствующие `mutableStateOf` поля (DataStore-сохранение
уже подвязано в init для части полей; для несохранённых — просто state).

### 8.4 Profile (вкладка 4)

```
┌───────────────────────────────────────────┐
│  Профиль                     [⟳]          │ Refresh → refreshUserData(tgId)
├───────────────────────────────────────────┤
│  ╭─ GlassSurface ────────────────────────╮│
│  │  [avatar-круг]  RedPill                ││  tg id или username
│  │  ID: 123456 · @username                ││
│  ╰───────────────────────────────────────╯│
│  ╭─ GlassSurface ────────────────────────╮│
│  │  Подписка:  Premium                    ││  subscriptionPlan
│  │  До:        12 мар 2026                ││  subscriptionExpiry
│  │  Устройства: 2/3                       ││  userInfo?.subscription?.devicesLimit
│  ╰───────────────────────────────────────╯│
│  [Выйти]            (GradientButton danger?)│  logout() + confirm dialog
└───────────────────────────────────────────┘
```

Данные:
- аккаунт: `telegramToken` (tg id), `userInfo?.username` — если null → «—»;
- тариф: `subscriptionPlan`; срок: `subscriptionExpiry`;
- лимит устройств: `userInfo?.subscription?.devicesLimit`
  (если null — строку «Устройства» не показывать или «—»);
- кнопка «Выйти» → `AlertDialog` подтверждение → `RedShiftState.logout()`.
- «Поддержка» — строка с ботом (копия @redpillcloud_bot).
Loading-state: при `isLoadingUser` — спиннер в заголовке / скелетон строк тарифа.

---

## 9. Файловая структура (новый `com.example.ui`)

```
com/example/ui/
  MainShell.kt              — MainAppContainer(): тема, онбординг?, Scaffold+NavBar+переключение вкладок
  Models.kt                 — ✅ НЕ ТРОГАТЬ (существующий, остаётся)
  Localization.kt           — ✅ НЕ ТРОГАТЬ (кроме добавления ключей в словарь)
  theme/Color.kt            — токены (раздел 5.2)
  theme/Theme.kt            — MyApplicationTheme + darkColorScheme (+ шрифты к 5.3)
  theme/Type.kt             — типографика (раздел 5.3)
  components/
    GlassSurface.kt         — примитив «стекло»
    Screen.kt               — Screen, ScreenHeader
    NavBar.kt               — нижняя навигация
    ListItem.kt             — ListItem, SectionTitle, ToggleRow
    Pill.kt                 — ProtocolPill, PingPill, СтатусPill
    Buttons.kt              — GradientButton, PowerButton
    StatCell.kt             — StatCell
    Sheets.kt               — GlassSheet-обёртка ModalBottomSheet + детали контента
  onboarding/OnboardingScreen.kt
  home/HomeScreen.kt
  servers/ServersScreen.kt
  settings/SettingsScreen.kt
  profile/ProfileScreen.kt
```

Старый мусор (`Screens.kt`, `*Premium*.kt`, `PlanetCanvas.kt`, `ServerCoords.kt`,
`Components.kt`, `VideoBackground.kt` если есть) УДАЛИТЬ.

Точки входа (обязательны, без них не соберётся):
- `com.example.ui.MainAppContainer` — новый `MainShell.kt`;
- `com.example.ui.theme.MyApplicationTheme`.

---

## 10. Чек-лист приёмки (выполни перед push)

- [ ] Ни одно API из раздела 2 не изменено; `service/*` нетронут (`git diff service`).
- [ ] Сборка: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] Нет следов старого дизайна: планет, `*Premium*`, фейков, `sampleServerCount`.
- [ ] Все строки через `Trans.get`; новые ключи добавлены в словарь (en+ru).
- [ ] Запрет фейков: на экранах только state.
- [ ] Тёмная тема всегда; стекло-стиль по разделу 5 везде, единые токены.
- [ ] Онбординг не блокирует после `isOnboarded=true`.
- [ ] Подключение работает: `toggleVpn()` → статусы в реальном времени.
- [ ] 4 вкладки навигации present. `ModalBottomSheet` для импорта/выбора языка.
- [ ] Сортировка/удаление серверов без падений.

---

## 11. Стиль коммитов

- Каждый шаг — осмысленный коммит: `feat(ui): home screen liquid glass`, и т.д.
- Никаких промежуточных «half-done» состояний дольше одной сессии.
- После завершения — `assembleDebug` зелёный, push.