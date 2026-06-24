package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.LayoutDirection
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppLanguage(val code: String, val nativeName: String, val isRtl: Boolean) {
    EN("en", "English", false),
    RU("ru", "Русский", false),
    ZH_CN("zh-CN", "简体中文", false),
    ZH_TW("zh-TW", "繁體中文", false),
    FA("fa", "فارسی", true),
    ES("es", "Español", false),
    FR("fr", "Français", false),
    DE("de", "Deutsch", false),
    JA("ja", "日本語", false),
    KO("ko", "한국어", false),
    AR("ar", "العربية", true),
    TR("tr", "Türkçe", false),
    VI("vi", "Tiếng Việt", false),
    ID("id", "Bahasa Indonesia", false),
    HI("hi", "हिन्दी", false),
    PT_BR("pt-BR", "Português (Brasil)", false),
    IT("it", "Italiano", false),
    UK("uk", "Українська", false),
    KK("kk", "Қазақша", false),
    HE("he", "עברית", true)
}

object LocalizationState {
    var currentLanguage by mutableStateOf(AppLanguage.EN)
    var followSystem by mutableStateOf(true)

    fun getLayoutDirection(): LayoutDirection {
        return if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    // Locale-aware formatting
    fun formatSpeed(speed: Double): String {
        val pattern = when (currentLanguage) {
            AppLanguage.DE, AppLanguage.FR, AppLanguage.RU, AppLanguage.UK, AppLanguage.KK -> "#,##0.0"
            else -> "#,##0.0"
        }
        val df = DecimalFormat(pattern)
        val formatted = df.format(speed)
        return when (currentLanguage) {
            AppLanguage.DE, AppLanguage.FR -> formatted.replace('.', ',')
            AppLanguage.RU, AppLanguage.UK, AppLanguage.KK -> formatted.replace(',', ' ').replace('.', ',')
            else -> formatted
        }
    }

    fun formatDate(timestamp: Long): String {
        val pattern = when (currentLanguage) {
            AppLanguage.RU, AppLanguage.UK, AppLanguage.KK, AppLanguage.DE, AppLanguage.FR, AppLanguage.IT, AppLanguage.ES -> "dd.MM.yyyy"
            AppLanguage.JA, AppLanguage.KO, AppLanguage.ZH_CN, AppLanguage.ZH_TW -> "yyyy/MM/dd"
            else -> "MM/DD/yyyy"
        }
        val sdf = SimpleDateFormat(pattern, Locale.US)
        return sdf.format(Date(timestamp))
    }
}

object Trans {
    private val strings = mapOf(
        "app_tagline" to mapOf(
            "en" to "See the truth. Route your traffic.",
            "ru" to "Увидь правду. Направляй свой трафик.",
            "zh-CN" to "看清真相。路由你的流量。",
            "zh-TW" to "看清真相。路由你的流量。",
            "fa" to "حقیقت را ببینید. ترافیک خود را هدایت کنید.",
            "es" to "Mira la verdad. Enruta tu tráfico.",
            "fr" to "Voyez la vérité. Routez votre trafic.",
            "de" to "Sieh die Wahrheit. Route deinen Traffic.",
            "ja" to "真実を見よ。トラフィックをルーティングせよ。",
            "ko" to "진실을 보라. 트래픽을 라우팅하라.",
            "ar" to "شاهد الحقيقة. وجه حركة مرورك.",
            "tr" to "Gerçeği gör. Trafiğini yönlendir.",
            "vi" to "Thấy sự thật. Điều hướng lưu lượng.",
            "id" to "Lihat kebenaran. Rute lalu lintas Anda.",
            "hi" to "सच्चाई देखें। अपने ट्रैफ़िक को रूट करें।",
            "pt-BR" to "Veja a verdade. Roteie seu tráfego.",
            "it" to "Guarda la verità. Instrada il tuo traffico.",
            "uk" to "Побач правду. Спрямовуй свій трафік.",
            "kk" to "Шындықты көр. Трафигіңді бағытта.",
            "he" to "ראה את האמת. נתב את התנועה שלך."
        ),
        "powered_by" to mapOf(
            "en" to "Powered by RedPill Cloud",
            "ru" to "Работает на RedPill Cloud",
            "zh-CN" to "由 RedPill Cloud 提供支持",
            "zh-TW" to "由 RedPill Cloud 提供支持",
            "fa" to "قدرت گرفته از RedPill Cloud",
            "es" to "Desarrollado por RedPill Cloud",
            "fr" to "Propulsé par RedPill Cloud",
            "de" to "Powered by RedPill Cloud",
            "ja" to "Powered by RedPill Cloud",
            "ko" to "Powered by RedPill Cloud",
            "ar" to "بدعم من RedPill Cloud",
            "tr" to "RedPill Cloud Tarafından Desteklenmektedir",
            "vi" to "Được cung cấp bởi RedPill Cloud",
            "id" to "Didukung oleh RedPill Cloud",
            "hi" to "RedPill Cloud द्वारा संचालित",
            "pt-BR" to "Distribuído por RedPill Cloud",
            "it" to "Sviluppato da RedPill Cloud",
            "uk" to "Працює на RedPill Cloud",
            "kk" to "RedPill Cloud негізінде жасалған",
            "he" to "מופעל על ידי RedPill Cloud"
        ),
        "disconnected" to mapOf(
            "en" to "Disconnected",
            "ru" to "Отключено",
            "zh-CN" to "已断开",
            "zh-TW" to "已斷開",
            "fa" to "قطع شده",
            "es" to "Desconectado",
            "fr" to "Déconnecté",
            "de" to "Getrennt",
            "ja" to "未接続",
            "ko" to "연결 끊김",
            "ar" to "غير متصل",
            "tr" to "Bağlantı Kesildi",
            "vi" to "Đã ngắt kết nối",
            "id" to "Terputus",
            "hi" to "डिस्कनेक्ट किया गया",
            "pt-BR" to "Desconectado",
            "it" to "Disconnesso",
            "uk" to "Відключено",
            "kk" to "Ажыратылған",
            "he" to "מנותק"
        ),
        "connecting" to mapOf(
            "en" to "Connecting...",
            "ru" to "Подключение...",
            "zh-CN" to "正在连接...",
            "zh-TW" to "正在連線...",
            "fa" to "در حال اتصال...",
            "es" to "Conectando...",
            "fr" to "Connexion...",
            "de" to "Verbinden...",
            "ja" to "接続中...",
            "ko" to "연결 중...",
            "ar" to "جاري الاتصال...",
            "tr" to "Bağlanıyor...",
            "vi" to "Đang kết nối...",
            "id" to "Menghubungkan...",
            "hi" to "कनेक्ट किया जा रहा है...",
            "pt-BR" to "Conectando...",
            "it" to "Connessione...",
            "uk" to "Підключення...",
            "kk" to "Қосылуда...",
            "he" to "מתחבר..."
        ),
        "connected" to mapOf(
            "en" to "Connected",
            "ru" to "Подключено",
            "zh-CN" to "已连接",
            "zh-TW" to "已連線",
            "fa" to "متصل شد",
            "es" to "Conectado",
            "fr" to "Connecté",
            "de" to "Verbunden",
            "ja" to "接続済み",
            "ko" to "연결됨",
            "ar" to "متصل",
            "tr" to "Bağlandı",
            "vi" to "Đã kết nối",
            "id" to "Terhubung",
            "hi" to "कनेक्टेड",
            "pt-BR" to "Conectado",
            "it" to "Connesso",
            "uk" to "Підключено",
            "kk" to "Қосылды",
            "he" to "מחובר"
        ),
        "download" to mapOf(
            "en" to "Download", "ru" to "Загрузка", "zh-CN" to "下载", "zh-TW" to "下載", "fa" to "دانلود",
            "es" to "Descarga", "fr" to "Téléchargement", "de" to "Download", "ja" to "ダウンロード", "ko" to "다운로드",
            "ar" to "تنزيل", "tr" to "İndirme", "vi" to "Tải xuống", "id" to "Unduh", "hi" to "डाउनलोड",
            "pt-BR" to "Download", "it" to "Download", "uk" to "Завантаження", "kk" to "Жүктеу", "he" to "הורדה"
        ),
        "upload" to mapOf(
            "en" to "Upload", "ru" to "Отдача", "zh-CN" to "上传", "zh-TW" to "上傳", "fa" to "آپلود",
            "es" to "Subida", "fr" to "Téléversement", "de" to "Upload", "ja" to "アップロード", "ko" to "업로드",
            "ar" to "رفع", "tr" to "Yükleme", "vi" to "Tải lên", "id" to "Unggah", "hi" to "अपलोड",
            "pt-BR" to "Upload", "it" to "Upload", "uk" to "Віддача", "kk" to "Жіберу", "he" to "העלאה"
        ),
        "tab_dashboard" to mapOf(
            "en" to "Home", "ru" to "Главная", "zh-CN" to "首页", "zh-TW" to "首頁", "fa" to "خانه",
            "es" to "Inicio", "fr" to "Accueil", "de" to "Dashboard", "ja" to "ホーム", "ko" to "대시보드",
            "ar" to "الرئيسية", "tr" to "Ana Sayfa", "vi" to "Trang chủ", "id" to "Dasbor", "hi" to "होम",
            "pt-BR" to "Início", "it" to "Dashboard", "uk" to "Головна", "kk" to "Басты", "he" to "ראשי"
        ),
        "tab_servers" to mapOf(
            "en" to "Servers", "ru" to "Серверы", "zh-CN" to "服务器", "zh-TW" to "伺服器", "fa" to "سرورها",
            "es" to "Servidores", "fr" to "Serveurs", "de" to "Server", "ja" to "サーバー", "ko" to "서버",
            "ar" to "الخوادم", "tr" to "Sunucular", "vi" to "Máy chủ", "id" to "Server", "hi" to "सर्वर",
            "pt-BR" to "Servidores", "it" to "Server", "uk" to "Сервери", "kk" to "Серверлер", "he" to "שרתים"
        ),
        "tab_add_server" to mapOf(
            "en" to "Add", "ru" to "Добавить", "zh-CN" to "添加", "zh-TW" to "新增", "fa" to "افزودن",
            "es" to "Añadir", "fr" to "Ajouter", "de" to "Hinzufügen", "ja" to "追加", "ko" to "추가",
            "ar" to "إضافة", "tr" to "Ekle", "vi" to "Thêm", "id" to "Tambah", "hi" to "जोड़ें",
            "pt-BR" to "Adicionar", "it" to "Aggiungi", "uk" to "Додати", "kk" to "Қосу", "he" to "הוספה"
        ),
        "tab_subscriptions" to mapOf(
            "en" to "Subs", "ru" to "Подписки", "zh-CN" to "订阅", "zh-TW" to "訂閱", "fa" to "اشتراک‌ها",
            "es" to "Suscripciones", "fr" to "Abonnements", "de" to "Abos", "ja" to "サブスク", "ko" to "구독",
            "ar" to "الاشتراكات", "tr" to "Abonelikler", "vi" to "Đăng ký", "id" to "Langganan", "hi" to "सदस्यता",
            "pt-BR" to "Assinaturas", "it" to "Abbonamenti", "uk" to "Підписки", "kk" to "Жазылымдар", "he" to "מנויים"
        ),
        "tab_rules" to mapOf(
            "en" to "Rules", "ru" to "Правила", "zh-CN" to "规则", "zh-TW" to "規則", "fa" to "قوانین",
            "es" to "Reglas", "fr" to "Règles", "de" to "Regeln", "ja" to "ルール", "ko" to "규칙",
            "ar" to "القواعد", "tr" to "Kurallar", "vi" to "Quy tắc", "id" to "Aturan", "hi" to "नियम",
            "pt-BR" to "Regras", "it" to "Regole", "uk" to "Правила", "kk" to "Ережелер", "he" to "חוקים"
        ),
        "tab_settings" to mapOf(
            "en" to "Settings", "ru" to "Настройки", "zh-CN" to "设置", "zh-TW" to "設定", "fa" to "تنظیمات",
            "es" to "Ajustes", "fr" to "Paramètres", "de" to "Einstellungen", "ja" to "設定", "ko" to "설정",
            "ar" to "الإعدادات", "tr" to "Ayarlar", "vi" to "Cài đặt", "id" to "Pengaturan", "hi" to "सेटिंग्स",
            "pt-BR" to "Configurações", "it" to "Impostazioni", "uk" to "Налаштування", "kk" to "Баптаулар", "he" to "הגדרות"
        ),
        "recent_servers" to mapOf(
            "en" to "Recent Servers", "ru" to "Недавние серверы", "zh-CN" to "最近服务器", "zh-TW" to "最近伺服器", "fa" to "سرورهای اخیر",
            "es" to "Servidores recientes", "fr" to "Serveurs récents", "de" to "Kürzliche Server", "ja" to "最近のサーバー", "ko" to "최근 서버",
            "ar" to "الخوادم الأخيرة", "tr" to "Son Sunucular", "vi" to "Máy chủ gần đây", "id" to "Server Terakhir", "hi" to "हालिया सर्वर",
            "pt-BR" to "Servidores Recentes", "it" to "Server recenti", "uk" to "Останні сервери", "kk" to "Жақындағы серверлер", "he" to "שרתים אחרונים"
        ),
        "see_all" to mapOf(
            "en" to "See All", "ru" to "Все", "zh-CN" to "查看全部", "zh-TW" to "查看全部", "fa" to "مشاهده همه",
            "es" to "Ver todo", "fr" to "Tout voir", "de" to "Alle", "ja" to "すべて見る", "ko" to "모두 보기",
            "ar" to "عرض الكل", "tr" to "Tümünü Gör", "vi" to "Xem tất cả", "id" to "Lihat Semua", "hi" to "सभी देखें",
            "pt-BR" to "Ver todos", "it" to "Vedi tutti", "uk" to "Усі", "kk" to "Бәрін көру", "he" to "ראה הכל"
        ),
        "add_server_btn" to mapOf(
            "en" to "Add Server", "ru" to "Добавить сервер", "zh-CN" to "添加服务器", "zh-TW" to "新增伺服器", "fa" to "افزودن سرور",
            "es" to "Añadir servidor", "fr" to "Ajouter serveur", "de" to "Server hinzufügen", "ja" to "サーバーを追加", "ko" to "서버 추가",
            "ar" to "إضافة خادم", "tr" to "Sunucu Ekle", "vi" to "Thêm máy chủ", "id" to "Tambah Server", "hi" to "सर्वर जोड़ें",
            "pt-BR" to "Adicionar Servidor", "it" to "Aggiungi server", "uk" to "Додати сервер", "kk" to "Серверді қосу", "he" to "הוסף שרת"
        ),
        "search_placeholder" to mapOf(
            "en" to "Search servers by name, address...",
            "ru" to "Поиск серверов по имени, адресу...",
            "zh-CN" to "按名称、地址搜索服务器...",
            "zh-TW" to "按名稱、地址搜尋伺服器...",
            "fa" to "جستجوی سرورها بر اساس نام، آدرس...",
            "es" to "Buscar servidores por nombre, dirección...",
            "fr" to "Rechercher des serveurs par nom, adresse...",
            "de" to "Server nach Name, Adresse suchen...",
            "ja" to "名前、アドレスでサーバーを検索...",
            "ko" to "이름, 주소로 서버 검색...",
            "ar" to "البحث عن الخوادم بالاسم أو العنوان...",
            "tr" to "Sunucuları ad, adres ile ara...",
            "vi" to "Tìm kiếm máy chủ bằng tên, địa chỉ...",
            "id" to "Cari server berdasarkan nama, alamat...",
            "hi" to "नाम, पते द्वारा सर्वर खोजें...",
            "pt-BR" to "Buscar servidores por nome, endereço...",
            "it" to "Cerca server per nome, indirizzo...",
            "uk" to "Пошук серверів за назвою, адресою...",
            "kk" to "Серверлерді аты немесе мекенжайы бойынша іздеу...",
            "he" to "חפש שרתים לפי שם, כתובת..."
        ),
        "get_started" to mapOf(
            "en" to "Get Started", "ru" to "Начать", "zh-CN" to "立即开始", "zh-TW" to "立即開始", "fa" to "شروع کنید",
            "es" to "Empezar", "fr" to "Démarrer", "de" to "Loslegen", "ja" to "始める", "ko" to "시작하기",
            "ar" to "ابدأ الآن", "tr" to "Başla", "vi" to "Bắt đầu", "id" to "Mulai", "hi" to "शुरू करें",
            "pt-BR" to "Começar", "it" to "Inizia", "uk" to "Почати", "kk" to "Бастау", "he" to "מתחילים"
        ),
        "manual_config" to mapOf(
            "en" to "Manual Config", "ru" to "Вручную", "zh-CN" to "手动配置", "zh-TW" to "手動配置", "fa" to "پیکربندی دستی",
            "es" to "Manual", "fr" to "Manuel", "de" to "Manuell", "ja" to "手動設定", "ko" to "수동 설정",
            "ar" to "إعداد يدوي", "tr" to "Manuel Ayar", "vi" to "Thủ công", "id" to "Manual", "hi" to "मैन्युअल कॉन्फ़िग",
            "pt-BR" to "Manual", "it" to "Manuale", "uk" to "Вручну", "kk" to "Қолмен орнату", "he" to "הגדרת ידנית"
        ),
        "subscription" to mapOf(
            "en" to "Subscription", "ru" to "Подписка", "zh-CN" to "订阅", "zh-TW" to "訂閱", "fa" to "اشتراک",
            "es" to "Suscripción", "fr" to "Abonnement", "de" to "Abonnement", "ja" to "サブスクリプション", "ko" to "구독",
            "ar" to "اشتراك", "tr" to "Abonelik", "vi" to "Đăng ký", "id" to "Langganan", "hi" to "सदस्यता",
            "pt-BR" to "Assinatura", "it" to "Abbonamento", "uk" to "Підписка", "kk" to "Жазылым", "he" to "מנוי"
        ),
        "empty_servers" to mapOf(
            "en" to "Add your first server to awaken",
            "ru" to "Добавьте первый сервер для пробуждения",
            "zh-CN" to "添加您的第一个服务器以觉醒",
            "zh-TW" to "新增您的第一個伺服器以覺醒",
            "fa" to "برای بیدار شدن اولین سرور خود را اضافه کنید",
            "es" to "Añade tu primer servidor para despertar",
            "fr" to "Ajoutez votre premier serveur pour vous réveiller",
            "de" to "Füge deinen ersten Server hinzu, um aufzuwachen",
            "ja" to "目覚めるために最初のサーバーを追加してください",
            "ko" to "깨어나기 위해 첫 번째 서버를 추가하세요",
            "ar" to "أضف خادمك الأول للاستيقاظ",
            "tr" to "Uyanmak için ilk sunucunu ekle",
            "vi" to "Thêm máy chủ đầu tiên để thức tỉnh",
            "id" to "Tambahkan server pertama untuk bangkit",
            "hi" to "जागने के लिए अपना पहला सर्वर जोड़ें",
            "pt-BR" to "Adicione seu primeiro servidor para acordar",
            "it" to "Aggiungi il tuo primo server per svegliarti",
            "uk" to "Додайте перший сервер для пробудження",
            "kk" to "Ояну үшін бірінші серверді қосыңыз",
            "he" to "הוסף את השרת הראשון שלך כדי להתעורר"
        ),
        "general_settings" to mapOf(
            "en" to "General Settings", "ru" to "Общие настройки", "zh-CN" to "通用设置", "zh-TW" to "通用設定", "fa" to "تنظیمات عمومی",
            "es" to "Generales", "fr" to "Général", "de" to "Allgemeine Einstellungen", "ja" to "一般設定", "ko" to "일반 설정",
            "ar" to "الإعدادات العامة", "tr" to "Genel Ayarlar", "vi" to "Cài đặt chung", "id" to "Pengaturan Umum", "hi" to "सामान्य सेटिंग्स",
            "pt-BR" to "Configurações Gerais", "it" to "Impostazioni generali", "uk" to "Загальні налаштування", "kk" to "Жалпы баптаулар", "he" to "הגדרות כלליות"
        ),
        "kill_switch" to mapOf(
            "en" to "Kill Switch", "ru" to "Kill Switch", "zh-CN" to "安全开关 (Kill Switch)", "zh-TW" to "安全開關 (Kill Switch)", "fa" to "سوئیچ قطع اضطراری",
            "es" to "Kill Switch", "fr" to "Coupe-circuit", "de" to "Kill Switch", "ja" to "キルスイッチ", "ko" to "킬 스위치",
            "ar" to "مفتاح قطع الاتصال", "tr" to "Acil Kapatma", "vi" to "Công tắc ngắt", "id" to "Kill Switch", "hi" to "किल स्विच",
            "pt-BR" to "Kill Switch", "it" to "Kill Switch", "uk" to "Аварійне вимкнення", "kk" to "Kill Switch", "he" to "מתג ניתוק חירום"
        ),
        "language" to mapOf(
            "en" to "App Language", "ru" to "Язык приложения", "zh-CN" to "应用语言", "zh-TW" to "應用語言", "fa" to "زبان برنامه",
            "es" to "Idioma de la app", "fr" to "Langue de l'application", "de" to "App-Sprache", "ja" to "アプリの言語", "ko" to "앱 언어",
            "ar" to "لغة التطبيق", "tr" to "Uygulama Dili", "vi" to "Ngôn ngữ ứng dụng", "id" to "Bahasa Aplikasi", "hi" to "ऐप की भाषा",
            "pt-BR" to "Idioma do App", "it" to "Lingua dell'app", "uk" to "Мова програми", "kk" to "Қосымша тілі", "he" to "שפת האפליקציה"
        ),
        "follow_system" to mapOf(
            "en" to "Follow System", "ru" to "Как в системе", "zh-CN" to "跟随系统", "zh-TW" to "跟随系統", "fa" to "هماهنگ با سیستم",
            "es" to "Seguir sistema", "fr" to "Suivre le système", "de" to "System folgen", "ja" to "システムに従う", "ko" to "시스템 설정 준수",
            "ar" to "اتبع النظام", "tr" to "Sistemi Takip Et", "vi" to "Theo hệ thống", "id" to "Ikuti Sistem", "hi" to "सिस्टम का पालन करें",
            "pt-BR" to "Seguir o Sistema", "it" to "Segui il sistema", "uk" to "Як у системі", "kk" to "Жүйеге сәйкес", "he" to "עקוב אחר המערכת"
        ),
        "check_updates" to mapOf(
            "en" to "Check for Updates", "ru" to "Проверить обновления", "zh-CN" to "检查更新", "zh-TW" to "檢查更新", "fa" to "بررسی برای بروزرسانی",
            "es" to "Buscar actualizaciones", "fr" to "Vérifier les mises à jour", "de" to "Nach Updates suchen", "ja" to "アップデートを確認", "ko" to "업데이트 확인",
            "ar" to "التحقق من التحديثات", "tr" to "Güncellemeleri Denetle", "vi" to "Kiểm tra cập nhật", "id" to "Periksa Pembaruan", "hi" to "अपडेट की जाँच करें",
            "pt-BR" to "Verificar Atualizações", "it" to "Controlla aggiornamenti", "uk" to "Перевірити оновлення", "kk" to "Жаңартуларды тексеру", "he" to "בדוק עדכונים"
        ),
        "onboard_1_title" to mapOf(
            "en" to "See the Truth", "ru" to "Увидь правду", "zh-CN" to "看清真相", "zh-TW" to "看清真相", "fa" to "حقیقت را ببینید",
            "es" to "Mira la verdad", "fr" to "Voyez la vérité", "de" to "Sieh die Wahrheit", "ja" to "真実を見よ", "ko" to "진실을 보라",
            "ar" to "شاهد الحقيقة", "tr" to "Gerçeği Gör", "vi" to "Thấy sự thật", "id" to "Lihat Kebenaran", "hi" to "सच्चाई देखें",
            "pt-BR" to "Veja a Verdade", "it" to "Guarda la verità", "uk" to "Побач правду", "kk" to "Шындықты көр", "he" to "ראה את האמת"
        ),
        "onboard_1_desc" to mapOf(
            "en" to "Bypass censorship. Access the free, open internet securely without borders.",
            "ru" to "Обходи цензуру. Безопасный доступ к свободному интернету без границ.",
            "zh-CN" to "绕过审查。安全地访问自由、开放的互联网，不受国界限制。",
            "zh-TW" to "繞過審查。安全地存取自由、開放的網際網路，不受國界限制。",
            "fa" to "از سانسور عبور کنید. دسترسی امن و بدون مرز به اینترنت آزاد.",
            "es" to "Evita la censura. Accede a internet libre y abierto de forma segura y sin límites.",
            "fr" to "Contournez la censure. Accédez à l'internet libre et ouvert en toute sécurité.",
            "de" to "Zensur umgehen. Sicherer Zugang zum freien, offenen Internet ohne Grenzen.",
            "ja" to "検閲を回避。境界のない自由で開かれたインターネットに安全にアクセス。",
            "ko" to "검열을 우회하세요. 경계 없는 자유롭고 개방된 인터넷에 안전하게 접속하세요.",
            "ar" to "تجاوز الرقابة. وصول آمن إلى الإنترنت الحر والمفتوح دون حدود.",
            "tr" to "Sansürü aş. Sınırlar olmadan özgür ve açık internete güvenle eriş.",
            "vi" to "Vượt qua kiểm duyệt. Truy cập internet tự do, mở an toàn không giới hạn.",
            "id" to "Lewati sensor. Akses internet bebas dan terbuka dengan aman tanpa batas.",
            "hi" to "सेंसरशिप को बायपास करें। सीमाओं के बिना सुरक्षित रूप से मुफ्त, खुले इंटरनेट तक पहुंचें।",
            "pt-BR" to "Bypass de censura. Acesse a internet livre de forma segura e sem fronteiras.",
            "it" to "Supera la censura. Accedi a internet libero in modo sicuro e senza confini.",
            "uk" to "Обходь цензуру. Безпечний доступ до вільного інтернету без кордонів.",
            "kk" to "Цензураны айналып өт. Еркін әрі ашық интернетке шекарасыз қауіпсіз қол жеткіз.",
            "he" to "עקוף את הצנזורה. גישה בטוחה לאינטרנט החופשי והפתוח ללא גבולות."
        ),
        "onboard_2_title" to mapOf(
            "en" to "Full Control", "ru" to "Полный контроль", "zh-CN" to "完全控制", "zh-TW" to "完全控制", "fa" to "کنترل کامل",
            "es" to "Control total", "fr" to "Contrôle total", "de" to "Volle Kontrolle", "ja" to "完全な制御", "ko" to "완전한 제어",
            "ar" to "التحكم الكامل", "tr" to "Tam Kontrol", "vi" to "Kiểm soát toàn diện", "id" to "Kontrol Penuh", "hi" to "पूर्ण नियंत्रण",
            "pt-BR" to "Controle Total", "it" to "Controllo totale", "uk" to "Повний контроль", "kk" to "Толық бақылау", "he" to "שליטה מלאה"
        ),
        "onboard_2_desc" to mapOf(
            "en" to "Smart routing rules. Decide precisely which apps or domains go through RedShift.",
            "ru" to "Умные правила маршрутизации. Решай сам, какие сайты идут через прокси.",
            "zh-CN" to "智能路由规则。精确决定哪些应用或域名通过 RedShift 代理。",
            "zh-TW" to "智能路由規則。精確決定哪些應用或網域通過 RedShift 代理。",
            "fa" to "قوانین هوشمند مسیریابی. دقیقا تصمیم بگیرید کدام دامنه‌ها از ردشیفت عبور کنند.",
            "es" to "Reglas de enrutamiento inteligente. Elige exactamente qué va por el proxy.",
            "fr" to "Règles de routage intelligentes. Décidez précisément quels domaines passent par RedShift.",
            "de" to "Intelligente Routing-Regeln. Entscheide präzise, was über RedShift läuft.",
            "ja" to "スマートルーティングルール。RedShiftを通すアプリやドメインを厳密に決定。",
            "ko" to "스마트 라우팅 규칙. 어떤 앱이나 도메인이 RedShift를 거칠지 정확히 결정하세요.",
            "ar" to "قواعد توجيه ذكية. قرر بدقة أي المواقع تمر عبر الشبكة الوهمية.",
            "tr" to "Akıllı yönlendirme kuralları. Hangi sitelerin proxy'den geçeceğine kendin karar ver.",
            "vi" to "Quy tắc định tuyến thông minh. Quyết định chính xác ứng dụng/tên miền nào qua RedShift.",
            "id" to "Aturan rute cerdas. Tentukan dengan tepat domain mana yang melewati RedShift.",
            "hi" to "स्मार्ट रूटिंग नियम। तय करें कि कौन से ऐप या डोमेन रेडशिफ्ट से होकर गुजरते हैं।",
            "pt-BR" to "Regras inteligentes. Decida exatamente qual tráfego passa pelo RedShift.",
            "it" to "Regole di instradamento intelligenti. Decidi con precisione cosa passa da RedShift.",
            "uk" to "Розумні правила маршрутизації. Вирішуй сам, які сайти йдуть через прокси.",
            "kk" to "Ақылды бағыттау ережелері. Трафиктің қай бөлігі прокси арқылы өтетінін өзіңіз шешіңіз.",
            "he" to "חוקי ניתוב חכמים. קבע בדיוק אילו אפליקציות או דומיינים יעברו דרך RedShift."
        ),
        "onboard_3_title" to mapOf(
            "en" to "Stay Private", "ru" to "Полная приватность", "zh-CN" to "保持私密", "zh-TW" to "保持私密", "fa" to "خصوصی بمانید",
            "es" to "Privacidad absoluta", "fr" to "Restez privé", "de" to "Privat bleiben", "ja" to "プライバシーを保護", "ko" to "개인정보 보호",
            "ar" to "حافظ على خصوصيتك", "tr" to "Gizli Kal", "vi" to "Giữ riêng tư", "id" to "Tetap Privat", "hi" to "निजी रहें",
            "pt-BR" to "Privacidade Total", "it" to "Resta privato", "uk" to "Повна приватність", "kk" to "Құпиялылықты сақтау", "he" to "שמור על פרטיות"
        ),
        "onboard_3_desc" to mapOf(
            "en" to "Zero logs. Your keys, rules, and network configs are stored safely on-device.",
            "ru" to "Ноль логов. Твои ключи, правила и трафик хранятся только на твоем устройстве.",
            "zh-CN" to "无日志。您的密钥、规则和网络配置全部安全地保存在本地设备上。",
            "zh-TW" to "無日誌。您的金鑰、規則和網路配置全部安全地保存在本地設備上。",
            "fa" to "بدون ذخیره هیچگونه لاگ. کلیدها و پیکربندی‌های شما به صورت امن روی دستگاه ذخیره می‌شوند.",
            "es" to "Cero registros. Tus llaves y configuraciones de red están seguras en tu dispositivo.",
            "fr" to "Zéro journal. Vos clés et configurations réseau sont stockées sur votre appareil.",
            "de" to "Keine Protokolle. Deine Schlüssel und Regeln bleiben sicher auf deinem Gerät.",
            "ja" to "ログ保存なし。キー、ルール、ネットワーク設定はデバイスに安全に保存されます。",
            "ko" to "로그 저장 제로. 키와 네트워크 구성은 기기에만 안전하게 저장됩니다.",
            "ar" to "سجل تصفح خالٍ تماما. مفاتيحك وإعدادات شبكتك محفوظة بأمان على جهازك.",
            "tr" to "Sıfır log. Anahtarların ve kuralların sadece kendi cihazında güvenle saklanır.",
            "vi" to "Không lưu nhật ký. Khóa, quy tắc, cấu hình mạng được lưu an toàn trên thiết bị.",
            "id" to "Tanpa log. Kunci dan konfigurasi Anda disimpan dengan aman di perangkat.",
            "hi" to "शून्य लॉग। आपकी कुंजियाँ, नियम और नेटवर्क कॉन्फ़िगरेशन डिवाइस पर सुरक्षित रूप से संग्रहीत हैं।",
            "pt-BR" to "Zero logs. Suas chaves e configurações de rede ficam seguras em seu dispositivo.",
            "it" to "Zero registri. Le tue chiavi e configurazioni rimangono sicure sul tuo dispositivo.",
            "uk" to "Без логів. Твої ключі, правила та конфігурації зберігаються лише на твоєму пристрої.",
            "kk" to "Журналдарсыз. Сіздің кілттеріңіз бен баптауларыңыз тек құрылғыңызда сақталады.",
            "he" to "אפס יומנים. המפתחות, החוקים והגדרות הרשת שלך נשמרים בבטחה על המכשיר."
        )
    )

    fun get(key: String): String {
        val entry = strings[key] ?: return key
        return entry[LocalizationState.currentLanguage.code] ?: entry["en"] ?: key
    }
}
