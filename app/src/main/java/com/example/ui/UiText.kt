package com.example.ui

import java.util.Locale

/**
 * Text and formatting helpers of the new "Liquid Glass Dark" UI.
 *
 * Why this file exists: REDESIGN.md §5.6 asks for new keys to be appended to `Trans.strings`,
 * but `Localization.kt` is on the do-not-touch list (§2). This file therefore keeps its own
 * dictionary for the keys the redesign introduced and **falls back to the frozen
 * [Trans] dictionary**, so every pre-existing key keeps working and `Localization.kt`
 * stays byte-identical.
 *
 * Rules kept from §5.6: one string = one key, `snake_case`, `en` + `ru` are mandatory,
 * other languages fall back to `en` (explicitly allowed by §5.6).
 */
object UiText {

    private val strings = mapOf(
        "whitelist_servers" to mapOf("en" to "Whitelists", "ru" to "Белые списки"),
        // ── Navigation (§6) ──
        "nav_home" to mapOf("en" to "Home", "ru" to "Главная"),
        "nav_servers" to mapOf("en" to "Servers", "ru" to "Серверы"),
        "nav_settings" to mapOf("en" to "Settings", "ru" to "Настройки"),
        "nav_profile" to mapOf("en" to "Profile", "ru" to "Профиль"),

        // ── Brand / common ──
        "app_title" to mapOf("en" to "RedShift", "ru" to "RedShift"),
        "cancel" to mapOf("en" to "Cancel", "ru" to "Отмена"),
        "unit_kbps" to mapOf("en" to "KB/s", "ru" to "КБ/с"),
        "unit_mbps" to mapOf("en" to "MB/s", "ru" to "МБ/с"),
        "unit_mb" to mapOf("en" to "MB", "ru" to "МБ"),
        "unit_gb" to mapOf("en" to "GB", "ru" to "ГБ"),
        "unit_ms" to mapOf("en" to "ms", "ru" to "мс"),
        "cd_back" to mapOf("en" to "Back", "ru" to "Назад"),
        "cd_close" to mapOf("en" to "Close", "ru" to "Закрыть"),
        "cd_refresh" to mapOf("en" to "Refresh", "ru" to "Обновить"),
        "cd_ping_all" to mapOf("en" to "Measure latency", "ru" to "Измерить пинг"),
        "cd_more" to mapOf("en" to "More", "ru" to "Ещё"),

        // ── Onboarding (§8.0) ──
        "onboarding_1_title" to mapOf("en" to "Secure internet, always", "ru" to "Безопасный интернет всегда"),
        "onboarding_1_desc" to mapOf(
            "en" to "RedShift encrypts your traffic and hides your real address from the network around you.",
            "ru" to "RedShift шифрует трафик и скрывает ваш настоящий адрес от окружающих сетей."
        ),
        "onboarding_2_title" to mapOf("en" to "Subscription from the Telegram bot", "ru" to "Подписка из Telegram-бота"),
        "onboarding_2_desc" to mapOf(
            "en" to "The bot @redpillcloudbot issues a link. Paste it into RedShift — servers appear automatically.",
            "ru" to "Бот @redpillcloudbot выдаёт ссылку. Вставьте её в RedShift — серверы появятся автоматически."
        ),
        "onboarding_3_title" to mapOf("en" to "Ready to connect", "ru" to "Готово к подключению"),
        "onboarding_3_desc" to mapOf(
            "en" to "Pick a server and press the button. Everything else RedShift does on its own.",
            "ru" to "Выберите сервер и нажмите кнопку. Всё остальное RedShift сделает сам."
        ),
        "onboarding_next" to mapOf("en" to "Next", "ru" to "Далее"),
        "onboarding_start" to mapOf("en" to "Start", "ru" to "Начать"),

        // ── Home (§8.1) ──
        "home_not_protected" to mapOf("en" to "Traffic is not protected", "ru" to "Трафик не защищён"),
        "home_vpn_active" to mapOf("en" to "VPN is active", "ru" to "VPN активен"),
        "home_vpn_idle" to mapOf("en" to "VPN is off", "ru" to "VPN выключен"),
        "home_vpn_starting" to mapOf("en" to "Establishing a tunnel", "ru" to "Устанавливаем туннель"),
        "session_traffic" to mapOf("en" to "Session traffic", "ru" to "Трафик сессии"),
        "current_server" to mapOf("en" to "Current server", "ru" to "Текущий сервер"),
        "no_server_selected" to mapOf("en" to "No server selected", "ru" to "Сервер не выбран"),
        "pick_server_hint" to mapOf("en" to "Choose a server in the Servers tab", "ru" to "Выберите сервер во вкладке «Серверы»"),
        "home_status_protected" to mapOf("en" to "Protected", "ru" to "Под защитой"),

        // ── Servers (§8.2) ──
        "server_active" to mapOf("en" to "Active", "ru" to "Активный"),
        "delete_server" to mapOf("en" to "Delete server", "ru" to "Удалить сервер"),
        "delete_server_confirm" to mapOf(
            "en" to "Remove this server from the list?",
            "ru" to "Удалить этот сервер из списка?"
        ),
        "delete" to mapOf("en" to "Delete", "ru" to "Удалить"),
        "empty_servers_desc" to mapOf(
            "en" to "No subscriptions yet. Add a link in Settings.",
            "ru" to "Подписок нет — добавьте ссылку в настройках."
        ),
        "add_subscription" to mapOf("en" to "Add subscription", "ru" to "Добавить подписку"),
        "import_title" to mapOf("en" to "Import subscription", "ru" to "Импорт подписки"),
        "import_hint" to mapOf("en" to "vpn:// link or subscription URL", "ru" to "Ссылка vpn:// или URL подписки"),
        "paste" to mapOf("en" to "Paste", "ru" to "Вставить"),
        "import_action" to mapOf("en" to "Import", "ru" to "Импортировать"),
        "importing" to mapOf("en" to "Importing…", "ru" to "Импорт…"),
        "import_empty_url" to mapOf("en" to "Paste a link first", "ru" to "Сначала вставьте ссылку"),
        "add_server_title" to mapOf("en" to "Add server", "ru" to "Добавить сервер"),
        "latency" to mapOf("en" to "Latency", "ru" to "Задержка"),

        // ── Settings (§8.3) ──
        "subscription_plan" to mapOf("en" to "Plan", "ru" to "Тариф"),
        "subscription_expiry" to mapOf("en" to "Valid until", "ru" to "Действует до"),
        "subscription_url" to mapOf("en" to "Link", "ru" to "Ссылка"),
        "subscription_refresh" to mapOf("en" to "Refresh", "ru" to "Обновить"),
        "subscription_remove" to mapOf("en" to "Remove link", "ru" to "Удалить ссылку"),
        "no_subscription" to mapOf("en" to "No subscription", "ru" to "Нет подписки"),
        "section_connection" to mapOf("en" to "Connection", "ru" to "Подключение"),
        "start_on_boot" to mapOf("en" to "Start on boot", "ru" to "Запуск при старте системы"),
        "auto_reconnect" to mapOf("en" to "Auto reconnect", "ru" to "Автопереподключение"),
        "allow_lan" to mapOf("en" to "Allow LAN", "ru" to "Разрешить доступ из LAN"),
        "ipv6_support" to mapOf("en" to "IPv6", "ru" to "IPv6"),
        "notifications" to mapOf("en" to "Notifications", "ru" to "Уведомления"),
        "section_routing" to mapOf("en" to "Routing", "ru" to "Маршрутизация"),
        "routing_mode" to mapOf("en" to "Mode", "ru" to "Режим"),
        "routing_global" to mapOf("en" to "Global", "ru" to "Глобально"),
        "routing_rule" to mapOf("en" to "Rules", "ru" to "Правила"),
        "routing_direct" to mapOf("en" to "Direct", "ru" to "Напрямую"),
        "bypass_local" to mapOf("en" to "Bypass local addresses", "ru" to "Не пропускать локальные адреса"),
        "bypass_lan" to mapOf("en" to "Bypass LAN", "ru" to "Не пропускать локальную сеть"),
        "bypass_china" to mapOf("en" to "China — direct", "ru" to "Китай — напрямую"),
        "bypass_russia" to mapOf("en" to "Russia — direct", "ru" to "Россия — напрямую"),
        "block_ads" to mapOf("en" to "Block ads", "ru" to "Блокировать рекламу"),
        "routing_hint" to mapOf(
            "en" to "Local networks always go direct so the router and LAN stay reachable. The rule below applies in \"Rules\" mode.",
            "ru" to "Локальная сеть всегда идёт напрямую — роутер и LAN остаются доступны. Правило ниже работает только в режиме «Правила»."
        ),
        "routing_global_desc" to mapOf(
            "en" to "All traffic goes through the VPN.",
            "ru" to "Весь трафик идёт через VPN."
        ),
        "routing_rule_desc" to mapOf(
            "en" to "Traffic is processed by the rules below, then everything else goes through the VPN.",
            "ru" to "Трафик обрабатывается по правилам ниже, всё остальное идёт через VPN."
        ),
        "routing_direct_desc" to mapOf(
            "en" to "Everything goes direct, without the VPN.",
            "ru" to "Весь трафик идёт напрямую, без VPN."
        ),
        "block_ads_desc" to mapOf(
            "en" to "Blocks known ad and tracker domains.",
            "ru" to "Блокирует известные рекламные и трекерные домены."
        ),
        "section_split_tunnel" to mapOf("en" to "Split tunneling", "ru" to "Раздельное туннелирование"),
        "split_tunnel" to mapOf("en" to "Split tunneling", "ru" to "Раздельное туннелирование"),
        "split_tunnel_desc" to mapOf(
            "en" to "Route only selected apps or sites through the VPN",
            "ru" to "Пропускать через VPN только выбранные приложения или сайты"
        ),
        "split_by_apps" to mapOf("en" to "Apps", "ru" to "Приложения"),
        "split_by_domains" to mapOf("en" to "Sites", "ru" to "Сайты"),
        "split_only_selected" to mapOf("en" to "VPN for selected only", "ru" to "VPN только для выбранных"),
        "split_exclude_selected" to mapOf("en" to "VPN for everything else", "ru" to "VPN для всего остального"),
        "split_selected_count" to mapOf("en" to "Selected: %1\$d", "ru" to "Выбрано: %1\$d"),
        "split_apps_title" to mapOf("en" to "Select apps", "ru" to "Выберите приложения"),
        "split_domains_title" to mapOf("en" to "Selected sites", "ru" to "Выбранные сайты"),
        "split_empty" to mapOf("en" to "Nothing selected", "ru" to "Ничего не выбрано"),
        "split_add_domain" to mapOf("en" to "Add site", "ru" to "Добавить сайт"),
        "split_domain_hint" to mapOf("en" to "example.com or *.com", "ru" to "example.com или *.com"),
        "split_apps_empty_desc" to mapOf(
            "en" to "No apps selected. Open the list and pick applications.",
            "ru" to "Приложения не выбраны. Откройте список и отметьте приложения."
        ),
        "search_apps" to mapOf("en" to "Search apps", "ru" to "Поиск приложений"),
        "split_apps_select_all" to mapOf("en" to "Select all", "ru" to "Выбрать все"),
        "split_apps_clear" to mapOf("en" to "Clear", "ru" to "Сбросить"),
        "section_auto_refresh" to mapOf("en" to "Auto refresh", "ru" to "Автообновление"),
        "auto_refresh" to mapOf("en" to "Auto refresh subscriptions", "ru" to "Автообновление подписок"),
        "refresh_interval" to mapOf("en" to "Interval", "ru" to "Интервал"),
        "every_n_hours" to mapOf("en" to "Every %1\$d h", "ru" to "Каждые %1\$d ч"),
        "section_data" to mapOf("en" to "Data", "ru" to "Данные"),
        "reset_cache" to mapOf("en" to "Reset cache", "ru" to "Сбросить кэш"),
        "reset_cache_confirm" to mapOf(
            "en" to "Clear the cached servers and subscriptions?",
            "ru" to "Очистить кэш серверов и подписок?"
        ),
        "cached_servers" to mapOf("en" to "Cached servers", "ru" to "Серверов в кэше"),
        "last_refresh" to mapOf("en" to "Last refresh", "ru" to "Последнее обновление"),
        "never" to mapOf("en" to "Never", "ru" to "Никогда"),
        "section_about" to mapOf("en" to "About", "ru" to "О приложении"),
        "version" to mapOf("en" to "Version", "ru" to "Версия"),
        "local_port" to mapOf("en" to "Local port", "ru" to "Локальный порт"),
        "support" to mapOf("en" to "Support", "ru" to "Поддержка"),

        // ── Profile (§8.4) ──
        "profile_title" to mapOf("en" to "Profile", "ru" to "Профиль"),
        "telegram_id" to mapOf("en" to "Telegram ID", "ru" to "Telegram ID"),
        "username" to mapOf("en" to "Username", "ru" to "Имя пользователя"),
        "not_signed_in" to mapOf("en" to "Not signed in", "ru" to "Не авторизован"),
        "signed_in" to mapOf("en" to "Signed in", "ru" to "Авторизован"),
        "logout" to mapOf("en" to "Log out", "ru" to "Выйти"),
        "logout_confirm" to mapOf("en" to "Log out of RedShift?", "ru" to "Выйти из RedShift?"),
        "support_bot" to mapOf("en" to "Support bot", "ru" to "Бот поддержки"),
        "no_limit" to mapOf("en" to "No limit", "ru" to "Без лимита"),
        "device_count" to mapOf("en" to "%1\$d of %2\$d", "ru" to "%1\$d из %2\$d"),
        "auto_select_best" to mapOf("en" to "Auto-select fastest", "ru" to "Автовыбор быстрого"),
        "auto_select_best_desc" to mapOf(
            "en" to "After measuring, pick the server with the lowest ping",
            "ru" to "После замера пинга выбрать сервер с минимальной задержкой"
        ),
        "cd_sort_by_ping" to mapOf("en" to "Sort by latency", "ru" to "Сортировка по пингу")
    )

    /** Keys known to this dictionary — used by the debug self-check. */
    val keys: Set<String> get() = strings.keys

    /**
     * Resolves [key]: new dictionary first, then the frozen [Trans] dictionary.
     * Reads [LocalizationState.currentLanguage], so composables recompose on language change.
     */
    fun get(key: String): String {
        val entry = strings[key]
        if (entry != null) {
            return entry[LocalizationState.currentLanguage.code] ?: entry["en"] ?: key
        }
        return Trans.get(key)
    }

    /** [get] with `String.format` arguments. */
    fun format(key: String, vararg args: Any?): String {
        return String.format(Locale.US, get(key), *args)
    }
}

/** Convenience alias — every visible string in the new UI goes through this. */
fun t(key: String): String = UiText.get(key)

/** Neutral placeholder for "no value" (REDESIGN.md §4: never a fake number). */
const val DASH = "—"

/** `HH:MM:SS` above one hour, otherwise `MM:SS` (REDESIGN.md §8.1). */
fun formatDuration(totalSeconds: Long): String {
    val safe = if (totalSeconds < 0) 0L else totalSeconds
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * `downloadSpeed` / `uploadSpeed` are kilobytes per second (§3.1).
 * [LocalizationState.formatSpeed] provides the locale-aware number; the unit is localised too.
 */
fun formatSpeedLabel(kiloBytesPerSecond: Double): String {
    val value = if (kiloBytesPerSecond < 0.0) 0.0 else kiloBytesPerSecond
    return if (value >= 1024.0) {
        "${LocalizationState.formatSpeed(value / 1024.0)} ${t("unit_mbps")}"
    } else {
        "${LocalizationState.formatSpeed(value)} ${t("unit_kbps")}"
    }
}

/** `totalDataUsedMb` → whole MB, whole GB above 1024 MB (§8.1). */
fun formatTraffic(megaBytes: Double): String {
    val value = if (megaBytes < 0.0) 0.0 else megaBytes
    return if (value >= 1024.0) {
        "${formatTrafficInt(value / 1024.0)} ${t("unit_gb")}"
    } else {
        "${formatTrafficInt(value)} ${t("unit_mb")}"
    }
}

/** Locale-aware whole number (no decimal places) — used for traffic figures. */
fun formatTrafficInt(value: Double): String {
    return try {
        java.text.DecimalFormat("#,##0").format(value)
    } catch (_: Exception) {
        value.toLong().toString()
    }
}

/** Latency: 0 means "not measured yet" → dash, never a fake number (§4). */
fun formatPing(latencyMs: Int): String {
    if (latencyMs <= 0) return DASH
    return "$latencyMs ${t("unit_ms")}"
}

/**
 * A tariff counts as premium only when the real subscription says so (§4).
 * There is deliberately no "premium by server order" heuristic.
 */
fun isPremiumPlan(plan: String?): Boolean {
    val value = plan?.trim().orEmpty()
    if (value.isEmpty()) return false
    val lowered = value.lowercase(Locale.US)
    return lowered.contains("premium") || lowered.contains("redpill premium")
}

/** Non-empty value or a dash — for ids, names, dates we may not have yet (§4). */
fun valueOrDash(value: String?): String {
    val trimmed = value?.trim().orEmpty()
    return if (trimmed.isEmpty()) DASH else trimmed
}

/** Shortens a subscription URL for one-line display (keeps host + path head). */
fun shortenUrl(url: String, maxLength: Int = 42): String {
    val trimmed = url.trim()
    if (trimmed.isEmpty()) return DASH
    return if (trimmed.length <= maxLength) trimmed else trimmed.take(maxLength - 1) + "…"
}
