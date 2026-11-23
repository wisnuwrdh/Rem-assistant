package com.ecosystemku.assistant.ui.apps.cucimoney

import androidx.compose.ui.graphics.Color
import com.ecosystemku.assistant.data.local.CuciAccountEntity
import com.ecosystemku.assistant.data.local.CuciBackupAccountDto
import com.ecosystemku.assistant.data.local.CuciBackupBookDto
import com.ecosystemku.assistant.data.local.CuciBackupCategoryDto
import com.ecosystemku.assistant.data.local.CuciBackupDbDto
import com.ecosystemku.assistant.data.local.CuciBackupMasterBudgetDto
import com.ecosystemku.assistant.data.local.CuciBackupMemberDto
import com.ecosystemku.assistant.data.local.CuciBackupPotDto
import com.ecosystemku.assistant.data.local.CuciBackupPotSnapDto
import com.ecosystemku.assistant.data.local.CuciBackupSavingsDto
import com.ecosystemku.assistant.data.local.CuciBackupTransactionDto
import com.ecosystemku.assistant.data.local.CuciBackupWrapperDto
import com.ecosystemku.assistant.data.local.CuciBookEntity
import com.ecosystemku.assistant.data.local.CuciCategoryEntity
import com.ecosystemku.assistant.data.local.CuciMasterBudgetEntity
import com.ecosystemku.assistant.data.local.CuciMemberEntity
import com.ecosystemku.assistant.data.local.CuciSavingsHistoryDto
import com.ecosystemku.assistant.data.local.CuciTransactionEntity
import com.ecosystemku.assistant.data.local.CuciWishlistEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.random.Random

enum class CuciTab { BUKU, DOMPET, ANALISIS, LAINNYA, TABUNGAN }
enum class CuciAnalysisPeriod { MONTH, YEAR, ALL }
enum class CuciAnalysisTab { EXPENSE, INCOME, BUDGET, TREN }

data class CuciAccountTypeDef(
    val id: String,
    val label: String,
    val icon: String,
    val group: String
)

data class CuciEmojiGroup(
    val label: String,
    val emojis: List<String>
)

data class CuciSvIconItem(
    val key: String,
    val emoji: String
)

data class CuciMoneyUiState(
    val books: List<CuciBookEntity> = emptyList(),
    val accounts: List<CuciAccountEntity> = emptyList(),
    val categories: List<CuciCategoryEntity> = emptyList(),
    val members: List<CuciMemberEntity> = emptyList(),
    val transactions: List<CuciTransactionEntity> = emptyList(),
    val masterBudgets: List<CuciMasterBudgetEntity> = emptyList(),
    val wishlists: List<CuciWishlistEntity> = emptyList(),
    val currentBookId: Long = 1L,
    val visibility: Boolean = true,
    val budgetCollapsed: Boolean = false,
    val lastAccByBook: Map<Long, Long> = emptyMap()
)

data class CuciMoneyActions(
    val onToggleVisibility: () -> Unit,
    val onSelectBook: (Long) -> Unit,
    val onCreateBook: suspend (String) -> Boolean,
    val onDeleteBook: suspend (Long) -> String?,
    val onToggleBudgetCollapsed: () -> Unit,
    val onSaveTransaction: suspend (
        editId: Long?,
        type: String,
        amount: Long,
        title: String,
        categoryId: Long,
        accountId: Long,
        toAccountId: Long?,
        date: String,
        memberId: String
    ) -> String?,
    val onDeleteTransaction: suspend (Long) -> String?,
    val onSaveAccount: suspend (
        editId: Long?,
        name: String,
        type: String,
        icon: String?,
        balance: Long,
        notes: String,
        useInBook: Boolean,
        includeAsset: Boolean
    ) -> String?,
    val onDeleteAccount: suspend (Long) -> Unit,
    val onSaveCategory: suspend (
        editId: Long?,
        name: String,
        emoji: String,
        type: String
    ) -> Long?,
    val onDeleteCategory: suspend (Long) -> Unit,
    val onMoveCategory: suspend (id: Long, delta: Int, filterType: String) -> Unit,
    val onSaveMember: suspend (editId: String?, name: String, emoji: String) -> Unit,
    val onDeleteMember: suspend (String) -> Unit,
    val onMoveMember: suspend (id: String, delta: Int) -> Unit,
    val onSaveMasterBudget: suspend (
        month: Int,
        year: Int,
        limitAmount: Long,
        startDate: String,
        endDate: String,
        note: String
    ) -> String?,
    val onDeleteMasterBudget: suspend (month: Int, year: Int) -> Unit,
    val onSaveWishlist: suspend (
        editId: String?,
        title: String,
        price: Long,
        priority: String,
        image: String
    ) -> Unit,
    val onDeleteWishlist: suspend (String) -> String?,
    val onDepositWishlist: suspend (
        wishId: String,
        accountId: Long,
        amount: Long
    ) -> String?,
    val onWithdrawWishlist: suspend (
        wishId: String,
        accountId: Long,
        realize: Boolean,
        markDone: Boolean
    ) -> String?,
    val onExportCsv: suspend () -> String,
    val onExportBackupJson: suspend () -> String,
    val onImportBackupJson: suspend (String) -> Result<Int>,
    val onResetAllData: suspend () -> Unit
)

object CuciMoneyColors {
    val Bg = Color(0xFF0F1117)
    val Card = Color(0xFF1A1D27)
    val Card2 = Color(0xFF21253A)
    val Hover = Color(0xFF272C40)
    val Primary = Color(0xFF00BFA5)
    val PrimaryDark = Color(0xFF008E76)
    val PrimaryDim = Color(0x1F00BFA5)
    val Income = Color(0xFF00BFA5)
    val IncomeDim = Color(0x2100BFA5)
    val Expense = Color(0xFFFF5370)
    val ExpenseDim = Color(0x21FF5370)
    val Transfer = Color(0xFF7C8FF5)
    val TransferCyan = Color(0xFF63B3ED)
    val Warning = Color(0xFFFFAB00)
    val TextMain = Color(0xFFEEF0F8)
    val TextMuted = Color(0xFF8891AA)
    val TextLight = Color(0xFF545D78)
    val Border = Color(0x12FFFFFF)
    val BorderStrong = Color(0x24FFFFFF)

    val DonutPalette = listOf(
        Color(0xFF00BFA5),
        Color(0xFFFF5370),
        Color(0xFFFFAB00),
        Color(0xFF7C4DFF),
        Color(0xFF00B0FF),
        Color(0xFFFF6D00),
        Color(0xFF64DD17),
        Color(0xFFF50057),
        Color(0xFF00E5FF),
        Color(0xFFFFD600)
    )
}

object CuciMoneyHelpers {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val idLocale = Locale("in", "ID")

    val MONTH_NAMES = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    val MONTH_SHORT = listOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    val ACCOUNT_TYPES = listOf(
        CuciAccountTypeDef("cash", "Tunai", "💵", "Tunai"),
        CuciAccountTypeDef("bank", "Bank", "🏦", "Bank"),
        CuciAccountTypeDef("ewallet", "E-Wallet", "📱", "Akun Virtual"),
        CuciAccountTypeDef("digital", "Digital", "🏪", "Akun Virtual"),
        CuciAccountTypeDef("credit", "Kredit", "💳", "Kredit"),
        CuciAccountTypeDef("investment", "Investasi", "📈", "Investasi")
    )

    val ACC_GROUP_ORDER = listOf("Tunai", "Bank", "Akun Virtual", "Kredit", "Investasi", "Lainnya")

    val ACC_ICONS_MAP = mapOf(
        "cash" to "💵",
        "bank" to "🏦",
        "ewallet" to "📱",
        "credit" to "💳",
        "saving" to "🐷",
        "digital" to "🏪",
        "investment" to "📈"
    )

    val DEFAULT_CATEGORIES = listOf(
        CuciCategoryEntity(1L, "Makanan", "expense", "🍔", 0),
        CuciCategoryEntity(2L, "Minuman", "expense", "☕", 1),
        CuciCategoryEntity(3L, "Transport", "expense", "🚗", 2),
        CuciCategoryEntity(4L, "Belanja", "expense", "🛍️", 3),
        CuciCategoryEntity(5L, "Hiburan", "expense", "🎮", 4),
        CuciCategoryEntity(6L, "Kesehatan", "expense", "💊", 5),
        CuciCategoryEntity(7L, "Pendidikan", "expense", "📚", 6),
        CuciCategoryEntity(8L, "Tagihan", "expense", "⚡", 7),
        CuciCategoryEntity(9L, "Gaji", "income", "💼", 8),
        CuciCategoryEntity(10L, "Freelance", "income", "💻", 9),
        CuciCategoryEntity(11L, "Investasi", "income", "📈", 10),
        CuciCategoryEntity(12L, "Lainnya", "both", "📌", 11)
    )

    val DEFAULT_BOOKS = listOf(
        CuciBookEntity(1L, "Personal", "[1,2]"),
        CuciBookEntity(2L, "Bisnis", "[]")
    )

    val DEFAULT_ACCOUNTS = listOf(
        CuciAccountEntity(1L, "Dompet", "cash", null, 0L, "", true, true, false),
        CuciAccountEntity(2L, "BCA", "bank", null, 0L, "", true, true, false)
    )

    val DEFAULT_MEMBERS = listOf(
        CuciMemberEntity("m1", "diri sendiri", "😄", 0)
    )

    val ACC_ICON_GROUPS = listOf(
        CuciEmojiGroup("💰 Uang & Bank", listOf("💵", "💴", "💶", "💷", "💰", "💸", "🪙", "💳", "🏧", "🏦", "💹", "📈", "📉", "📊", "🤑", "💎", "🏆")),
        CuciEmojiGroup("📱 Digital & E-Wallet", listOf("📱", "💻", "🖥️", "📲", "📡", "🔌", "🔋", "💾", "🌐", "⚡", "🛒", "🏪", "🏬")),
        CuciEmojiGroup("👜 Dompet & Tas", listOf("👛", "👜", "🎒", "🧳", "💼", "🗃️", "🗂️", "📦", "📁", "🗄️")),
        CuciEmojiGroup("🏠 Properti & Aset", listOf("🏠", "🏡", "🏢", "🏣", "🏨", "🏫", "🏭", "🏯", "🏰", "🏗️", "🏘️")),
        CuciEmojiGroup("🚗 Kendaraan", listOf("🚗", "🚕", "🚙", "🏎️", "🛻", "🚌", "🛵", "🏍️", "🚲", "✈️", "🚢", "⛽")),
        CuciEmojiGroup("🎯 Tabungan & Tujuan", listOf("🎯", "🐷", "🏆", "🥇", "🎖️", "🌟", "⭐", "💫", "✨", "🎁", "🎀", "🏅"))
    )

    val CAT_EMOJI_GROUPS = listOf(
        CuciEmojiGroup("🍽️ Makanan", listOf("🍔", "🍟", "🌮", "🌯", "🍕", "🍣", "🍱", "🍜", "🍝", "🍛", "🍲", "🥩", "🥗", "🍗", "🥚", "🍞", "🥪", "🌭", "🍿", "🥟")),
        CuciEmojiGroup("🍰 Dessert & Snack", listOf("🍦", "🍧", "🍨", "🍩", "🍪", "🎂", "🍰", "🧁", "🍮", "🍭", "🍬", "🍫", "🍯", "🥜")),
        CuciEmojiGroup("🥤 Minuman", listOf("☕", "🍵", "🧉", "🧋", "🥤", "🍺", "🍷", "🍹", "🧊", "🧃")),
        CuciEmojiGroup("🚗 Transportasi", listOf("🚗", "🚕", "🚙", "🚌", "🛵", "🏍️", "🚲", "🛴", "🚂", "🚢", "✈️", "⛽", "🅿️")),
        CuciEmojiGroup("🛍️ Belanja & Fashion", listOf("🛍️", "🛒", "👗", "👕", "👛", "👜", "🎒", "👟", "💍", "💎", "⌚", "🕶️", "💄")),
        CuciEmojiGroup("🎮 Hiburan", listOf("🎮", "🕹️", "🎲", "🎯", "🎳", "🎭", "🎬", "📺", "🎵", "🎸", "🎧", "🎨", "🎟️")),
        CuciEmojiGroup("⚽ Olahraga", listOf("⚽", "🏀", "🎾", "🏐", "🎱", "🏓", "🏸", "🥊", "🏋️", "🧘", "🏊", "🚴")),
        CuciEmojiGroup("💊 Kesehatan", listOf("💊", "🩺", "🏥", "🩹", "💉", "🧬", "🧪", "💪", "🧴", "🪥")),
        CuciEmojiGroup("📚 Pendidikan", listOf("📚", "📖", "📝", "✏️", "🖊️", "📓", "📐", "🔬", "🎓", "🏫")),
        CuciEmojiGroup("⚡ Tagihan & Utilitas", listOf("⚡", "💡", "💧", "🌊", "🔥", "📡", "📶", "📱", "💻", "🔌", "🔋")),
        CuciEmojiGroup("🏠 Rumah & Properti", listOf("🏠", "🏡", "🏢", "🛋️", "🛁", "🚿", "🔨", "🛠️", "🔧", "🧹", "🪴")),
        CuciEmojiGroup("💰 Keuangan & Investasi", listOf("💰", "💵", "💸", "💳", "🪙", "🏧", "💹", "📈", "📉", "📊", "💼", "🏦", "💎")),
        CuciEmojiGroup("✈️ Perjalanan & Wisata", listOf("✈️", "🌍", "🗺️", "🏖️", "🏕️", "🏔️", "🏝️", "📸", "🧳")),
        CuciEmojiGroup("🎉 Sosial & Hadiah", listOf("🎉", "🎊", "🎈", "🎁", "🎀", "❤️", "💖", "🙏", "🕌", "🎂", "🥳")),
        CuciEmojiGroup("📦 Lainnya", listOf("📦", "🔑", "🔐", "🎯", "⚙️", "📌", "🗓️", "🔔", "🏷️", "🧾"))
    )

    val MEMBER_EMOJI_GROUPS = listOf(
        CuciEmojiGroup("😄 Ekspresi", listOf("😄", "😎", "🤩", "🥳", "😇", "🤗", "😍", "🤓", "😊", "🥰", "😆", "😅", "🤪", "🫡", "😌", "🤑")),
        CuciEmojiGroup("🧑 Orang & Keluarga", listOf("👦", "👧", "🧑", "👨", "👩", "🧔", "👴", "👵", "👶", "👪", "👫", "🤝")),
        CuciEmojiGroup("💼 Profesi", listOf("🧑‍💼", "👩‍💼", "👨‍💼", "🧑‍🍳", "🧑‍🎓", "🧑‍🏫", "🧑‍⚕️", "🧑‍🔧", "🧑‍💻", "🥷")),
        CuciEmojiGroup("🐶 Hewan & Simbol", listOf("🐶", "🐱", "🐰", "🦊", "🐻", "🐼", "🐯", "🦁", "🐸", "🐧", "🏠", "🌟", "🎮", "🚀", "💎", "🏆"))
    )

    val SV_ICONS = listOf(
        CuciSvIconItem("bag", "🛍️"),
        CuciSvIconItem("phone", "📱"),
        CuciSvIconItem("laptop", "💻"),
        CuciSvIconItem("headphone", "🎧"),
        CuciSvIconItem("watch", "⌚"),
        CuciSvIconItem("camera", "📷"),
        CuciSvIconItem("shirt", "👕"),
        CuciSvIconItem("shoe", "👟"),
        CuciSvIconItem("book", "📚"),
        CuciSvIconItem("gamepad", "🎮"),
        CuciSvIconItem("furniture", "🛋️"),
        CuciSvIconItem("food", "🍔"),
        CuciSvIconItem("bike", "🚲"),
        CuciSvIconItem("tool", "🔧"),
        CuciSvIconItem("gift", "🎁"),
        CuciSvIconItem("beauty", "💄"),
        CuciSvIconItem("sport", "⚽"),
        CuciSvIconItem("car", "🚗"),
        CuciSvIconItem("music", "🎵"),
        CuciSvIconItem("health", "💊"),
        CuciSvIconItem("star", "⭐"),
        CuciSvIconItem("other", "📦"),
        CuciSvIconItem("tv", "📺"),
        CuciSvIconItem("glasses", "👓"),
        CuciSvIconItem("ring", "💍"),
        CuciSvIconItem("toy", "🧸"),
        CuciSvIconItem("plant", "🪴"),
        CuciSvIconItem("palette", "🎨"),
        CuciSvIconItem("pen", "✏️")
    )

    val SV_PRIORITIES = listOf("Penting", "Nanti", "Kapan-kapan")

    fun svPriorityColor(priority: String): Color = when (priority) {
        "Penting" -> CuciMoneyColors.Expense
        "Nanti" -> CuciMoneyColors.Warning
        else -> CuciMoneyColors.Primary
    }

    fun svPriorityRank(priority: String): Int = when (priority) {
        "Penting" -> 0
        "Nanti" -> 1
        "Kapan-kapan" -> 2
        else -> 3
    }

    fun svIconEmoji(key: String): String =
        SV_ICONS.firstOrNull { it.key == key }?.emoji ?: "🛍️"

    fun generateId(): Long = System.currentTimeMillis() * 1000L + Random.nextInt(100, 999)

    fun parseAccountIds(raw: String): List<Long> = runCatching {
        json.decodeFromString<List<Long>>(raw)
    }.getOrDefault(emptyList())

    fun encodeAccountIds(ids: List<Long>): String = runCatching {
        json.encodeToString(ids.distinct())
    }.getOrDefault("[]")

    fun parseWishlistHistory(raw: String): List<CuciSavingsHistoryDto> = runCatching {
        json.decodeFromString<List<CuciSavingsHistoryDto>>(raw)
    }.getOrDefault(emptyList())

    fun encodeWishlistHistory(items: List<CuciSavingsHistoryDto>): String = runCatching {
        json.encodeToString(items)
    }.getOrDefault("[]")

    fun accountIcon(acc: CuciAccountEntity?): String {
        if (acc == null) return "💰"
        return acc.icon?.takeIf { it.isNotBlank() } ?: ACC_ICONS_MAP[acc.type] ?: "💰"
    }

    fun accountTypeLabel(typeId: String): String =
        ACCOUNT_TYPES.firstOrNull { it.id == typeId }?.label
            ?: if (typeId == "saving") "Tabungan" else typeId

    fun accountGroup(typeId: String): String =
        ACCOUNT_TYPES.firstOrNull { it.id == typeId }?.group ?: "Lainnya"

    fun todayDateStr(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun yesterdayDateStr(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    fun formatRp(amount: Long, visible: Boolean = true): String {
        if (!visible) return "••••••"
        val nf = NumberFormat.getNumberInstance(idLocale)
        nf.maximumFractionDigits = 0
        val sign = if (amount < 0) "-" else ""
        return "${sign}Rp ${nf.format(abs(amount))}"
    }

    fun formatCompact(amount: Long, visible: Boolean = true): String {
        if (!visible) return "••••••"
        val a = abs(amount).toDouble()
        val sign = if (amount < 0) "-" else ""
        val nf1 = NumberFormat.getNumberInstance(idLocale).apply { maximumFractionDigits = 1 }
        return when {
            a >= 1_000_000_000.0 -> "${sign}Rp ${nf1.format(a / 1_000_000_000.0)}M"
            a >= 1_000_000.0 -> "${sign}Rp ${nf1.format(a / 1_000_000.0)}Jt"
            a >= 1_000.0 -> "${sign}Rp ${(a / 1_000.0).roundToLong()}rb"
            else -> formatRp(amount, true)
        }
    }

    fun formatHero(amount: Long, visible: Boolean = true): String {
        if (!visible) return "••••••"
        val a = abs(amount).toDouble()
        val sign = if (amount < 0) "-" else ""
        val nf1 = NumberFormat.getNumberInstance(idLocale).apply { maximumFractionDigits = 1 }
        val nf2 = NumberFormat.getNumberInstance(idLocale).apply { maximumFractionDigits = 2 }
        return when {
            a >= 1_000_000_000.0 -> "${sign}Rp ${nf2.format(a / 1_000_000_000.0)} M"
            a >= 100_000_000.0 -> "${sign}Rp ${nf1.format(a / 1_000_000.0)} Jt"
            else -> formatRp(amount, true)
        }
    }

    fun formatDateShort(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        return runCatching {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) ?: return dateStr
            SimpleDateFormat("d MMM yyyy", idLocale).format(d)
        }.getOrDefault(dateStr)
    }

    fun formatDayMonthShort(dateStr: String): String {
        if (dateStr == todayDateStr()) return "Today"
        if (dateStr == yesterdayDateStr()) return "Kemarin"
        return runCatching {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) ?: return dateStr
            SimpleDateFormat("d MMM", idLocale).format(d)
        }.getOrDefault(dateStr)
    }

    fun formatGroupDayLabel(dateStr: String): String = runCatching {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) ?: return dateStr.uppercase()
        SimpleDateFormat("EEE, d MMM", idLocale).format(d).uppercase(idLocale)
    }.getOrDefault(dateStr)

    fun formatLongDate(dateStr: String): String = runCatching {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) ?: return dateStr
        SimpleDateFormat("EEEE, d MMMM yyyy", idLocale).format(d)
    }.getOrDefault(dateStr)

    fun currentYearMonth(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
    }

    fun yearMonthPrefix(year: Int, month: Int): String =
        String.format(Locale.US, "%04d-%02d-", year, month + 1)

    fun daysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun daysLeftUntil(endDate: String): Int? = runCatching {
        val today = todayDateStr()
        if (today > endDate) return null
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val t = fmt.parse(today)?.time ?: return null
        val e = fmt.parse(endDate)?.time ?: return null
        ((e - t) / 86_400_000L).toInt().coerceAtLeast(0)
    }.getOrNull()

    fun activeBook(state: CuciMoneyUiState): CuciBookEntity? =
        state.books.firstOrNull { it.id == state.currentBookId } ?: state.books.firstOrNull()

    fun bookAccounts(state: CuciMoneyUiState, bookId: Long): List<CuciAccountEntity> {
        val book = state.books.firstOrNull { it.id == bookId } ?: activeBook(state)
        val ids = book?.let { parseAccountIds(it.accountIdsJson).toSet() } ?: emptySet()
        val filtered = state.accounts.filter { !it.isSavings && it.id in ids }
        return filtered.ifEmpty { state.accounts.filter { !it.isSavings } }
    }

    fun activeBookBalance(state: CuciMoneyUiState): Long {
        val book = activeBook(state) ?: return 0L
        val ids = parseAccountIds(book.accountIdsJson).toSet()
        return state.accounts.filter { it.id in ids }.sumOf { it.balance }
    }

    fun activeBookTodayExpense(state: CuciMoneyUiState): Long {
        val book = activeBook(state) ?: return 0L
        val today = todayDateStr()
        return state.transactions
            .filter { it.bookId == book.id && it.date == today && it.direction == "out" && it.transferPair == null }
            .sumOf { it.amount }
    }

    /**
     * Safe arithmetic parser for the calculator keyboard (supports +, -, ×, ÷, decimals).
     */
    fun evalCalcExpression(raw: String): Long {
        val normalized = raw
            .replace('×', '*')
            .replace('÷', '/')
            .replace('−', '-')
            .replace(",", "")
            .trim()
            .trimEnd('+', '-', '*', '/')
        if (normalized.isEmpty()) return 0L

        return runCatching {
            var pos = 0
            fun skipWs() {
                while (pos < normalized.length && normalized[pos].isWhitespace()) pos++
            }
            fun parseNumber(): Double {
                skipWs()
                val start = pos
                while (pos < normalized.length && (normalized[pos].isDigit() || normalized[pos] == '.')) pos++
                if (start == pos) return 0.0
                return normalized.substring(start, pos).toDoubleOrNull() ?: 0.0
            }
            fun parseFactor(): Double {
                skipWs()
                if (pos < normalized.length && normalized[pos] == '-') {
                    pos++
                    return -parseFactor()
                }
                if (pos < normalized.length && normalized[pos] == '+') {
                    pos++
                    return parseFactor()
                }
                return parseNumber()
            }
            fun parseTerm(): Double {
                var v = parseFactor()
                while (true) {
                    skipWs()
                    if (pos >= normalized.length) break
                    val op = normalized[pos]
                    if (op == '*' || op == '/') {
                        pos++
                        val rhs = parseFactor()
                        v = if (op == '*') v * rhs else if (rhs != 0.0) v / rhs else 0.0
                    } else break
                }
                return v
            }
            fun parseExpr(): Double {
                var v = parseTerm()
                while (true) {
                    skipWs()
                    if (pos >= normalized.length) break
                    val op = normalized[pos]
                    if (op == '+' || op == '-') {
                        pos++
                        val rhs = parseTerm()
                        v = if (op == '+') v + rhs else v - rhs
                    } else break
                }
                return v
            }
            val res = parseExpr()
            if (res.isNaN() || res.isInfinite()) 0L else abs(res.roundToLong())
        }.getOrDefault(0L)
    }

    fun buildCsvExport(
        bookTrx: List<CuciTransactionEntity>,
        categories: List<CuciCategoryEntity>,
        accounts: List<CuciAccountEntity>
    ): String {
        val catById = categories.associateBy { it.id }
        val accById = accounts.associateBy { it.id }
        return buildString {
            append("Tanggal,Kategori,Deskripsi,Jumlah,Tipe,Akun\n")
            bookTrx.forEach { t ->
                val catName = (catById[t.categoryId]?.name ?: "Lainnya").replace("\"", "\"\"")
                val titleEsc = t.title.replace("\"", "\"\"")
                val tipe = when {
                    t.transferPair != null -> "Transfer"
                    t.direction == "in" -> "Pemasukan"
                    else -> "Pengeluaran"
                }
                val accName = (accById[t.accountId]?.name ?: "").replace("\"", "\"\"")
                append("${t.date},\"$catName\",\"$titleEsc\",${t.amount},$tipe,\"$accName\"\n")
            }
        }
    }

    fun buildBackupJson(state: CuciMoneyUiState): String {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
        val pots = mutableMapOf<String, CuciBackupPotDto>()
        state.wishlists.forEach { w ->
            val hist = parseWishlistHistory(w.historyJson)
            if (w.saved > 0L || hist.isNotEmpty() || w.realizedAt != null) {
                pots[w.id] = CuciBackupPotDto(
                    wishId = w.id,
                    saved = w.saved,
                    history = hist,
                    snap = CuciBackupPotSnapDto(w.title, w.price, w.image),
                    realizedAt = w.realizedAt,
                    realizedAmount = w.realizedAmount
                )
            }
        }
        val wrapper = CuciBackupWrapperDto(
            version = 1,
            app = "cucimoney",
            exportedAt = nowIso,
            db = CuciBackupDbDto(
                books = state.books.map { CuciBackupBookDto(it.id, it.name, parseAccountIds(it.accountIdsJson)) },
                accounts = state.accounts.map {
                    CuciBackupAccountDto(it.id, it.name, it.type, it.icon, it.balance, it.notes, it.useInBook, it.includeAsset, it.isSavings)
                },
                categories = state.categories.map { CuciBackupCategoryDto(it.id, it.name, it.type, it.emoji) },
                transactions = state.transactions.map {
                    CuciBackupTransactionDto(it.id, it.bookId, it.title, it.amount, it.direction, it.categoryId, it.accountId, it.date, it.memberId, it.transferPair, it.savingsPot, it.createdAt)
                },
                members = state.members.map { CuciBackupMemberDto(it.id, it.name, it.emoji) }
            ),
            masterBudgets = state.masterBudgets.map {
                CuciBackupMasterBudgetDto(it.id, it.bookId, it.month, it.year, it.limitAmount, it.startDate, it.endDate, it.note)
            },
            savings = CuciBackupSavingsDto(pots = pots)
        )
        return json.encodeToString(wrapper)
    }

    fun parseBackupJson(raw: String): CuciBackupWrapperDto =
        json.decodeFromString<CuciBackupWrapperDto>(raw)
}
