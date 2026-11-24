package com.ecosystemku.assistant.data.network

import com.ecosystemku.assistant.data.local.ChatChoicesData

object SseTagParser {
    private val JA_REGEX = Regex("""\[JA][\s\S]*?(\[/JA]|$)""", RegexOption.IGNORE_CASE)
    private val EXPR_REGEX = Regex("""\[EXPR:([a-z]+)]""", RegexOption.IGNORE_CASE)
    private val OPEN_REGEX = Regex("""\[OPEN:([a-z0-9-]+)]""", RegexOption.IGNORE_CASE)
    private val CHOICES_REGEX = Regex("""\[(?:CHOICES|OPTIONS):([^\]\r\n]+)]""", RegexOption.IGNORE_CASE)
    private val SEARCH_REGEX = Regex("""\[SEARCH:([^\]\r\n]+)]""", RegexOption.IGNORE_CASE)
    private val MEMORY_REGEX = Regex("""\[MEMORY:([^\]\r\n]+)]""", RegexOption.IGNORE_CASE)
    private val KRONIK_REGEX = Regex("""\[KRONIK:([^\]\r\n]+)]""", RegexOption.IGNORE_CASE)
    private val LINKKU_REGEX = Regex("""\[LINKKU:([^\]\r\n]+)]""", RegexOption.IGNORE_CASE)
    private val CUCI_REGEX = Regex("""\[CUCI:([^\]\r\n]+)]""", RegexOption.IGNORE_CASE)
    private val IDEKU_REGEX = Regex("""\[IDEKU:([^\]\r\n]+)]""", RegexOption.IGNORE_CASE)
    private val DAILYOS_REGEX = Regex("""\[DAILYOS:([^\]\r\n]+)]""", RegexOption.IGNORE_CASE)

    private val VALID_EXPRS = setOf(
        "happy", "smile", "shame", "sad", "cry", "angry",
        "surprise", "serious", "proud", "doubt", "puzzle",
        "upset", "appeal", "navi", "none"
    )

    data class SearchQueryAction(
        val scope: String,
        val query: String
    )

    sealed class MemoryChatAction {
        data class SetMemory(
            val category: String,
            val factKey: String,
            val factValue: String
        ) : MemoryChatAction()

        data class DeleteMemory(
            val query: String
        ) : MemoryChatAction()
    }

    sealed class KronikChatAction {
        data class AddEntry(
            val title: String,
            val content: String,
            val mood: String? = null
        ) : KronikChatAction()
    }

    sealed class LinkkuChatAction {
        data class AddLink(
            val url: String,
            val title: String = "",
            val note: String = "",
            val category: String = "",
            val tags: List<String> = emptyList()
        ) : LinkkuChatAction()
    }

    sealed class CuciChatAction {
        data class AddTransaction(
            val direction: String, // "out" | "in"
            val amount: Long,
            val title: String,
            val categoryName: String = "",
            val accountName: String = "",
            val bookName: String = "",
            val date: String = ""
        ) : CuciChatAction()

        data class SaveWishlist(
            val wishlistQuery: String,
            val amount: Long,
            val accountName: String = "",
            val bookName: String = ""
        ) : CuciChatAction()

        data class RealizeWishlist(
            val wishlistQuery: String,
            val accountName: String = "",
            val paidAmount: Long = 0L,
            val bookName: String = ""
        ) : CuciChatAction()
    }

    sealed class IdekuChatAction {
        data class AddItem(
            val type: String, // "Idea" | "Wishlist"
            val title: String,
            val category: String = "Dev",
            val priority: String = "Nanti",
            val notes: String = "",
            val jalur: String = "",
            val price: Long = 0L
        ) : IdekuChatAction()
    }

    sealed class DailyOsChatAction {
        data class AddHabit(
            val name: String,
            val category: String = "General",
            val icon: String = "star"
        ) : DailyOsChatAction()

        data class AddTodo(
            val title: String,
            val priority: String = "urgent", // "urgent" | "soon" | "normal"
            val deadline: String = ""
        ) : DailyOsChatAction()

        data class CheckItem(
            val query: String
        ) : DailyOsChatAction()
    }

    private val ITALIC_STAR_REGEX = Regex("""\*([^*\r\n]+)\*""")
    private val BACKTICK_REGEX = Regex("""`([^`\r\n]+)`""")
    private val HEADING_REGEX = Regex("""(?m)^#{1,6}\s+""")
    private val EM_DASH_START_REGEX = Regex("""(^|\n)\s*\u2014\s*""")
    private val EM_DASH_AFTER_PUNCT_REGEX = Regex("""([.!?,:;(\[])\s*\u2014\s*""")
    private val EM_DASH_MID_REGEX = Regex("""\s*\u2014\s*""")

    private fun cleanChatFormatting(text: String): String {
        return text
            .replace("**", "")
            .replace("__", "")
            .replace(ITALIC_STAR_REGEX, "$1")
            .replace(BACKTICK_REGEX, "$1")
            .replace(HEADING_REGEX, "")
            .replace(EM_DASH_START_REGEX, "$1")
            .replace(EM_DASH_AFTER_PUNCT_REGEX, "$1 ")
            .replace(EM_DASH_MID_REGEX, ", ")
    }

    fun visibleTextDuringStream(raw: String): String {
        var v = raw
            .replace(JA_REGEX, "")
            .replace(EXPR_REGEX, "")
            .replace(OPEN_REGEX, "")
            .replace(CHOICES_REGEX, "")
            .replace(SEARCH_REGEX, "")
            .replace(MEMORY_REGEX, "")
            .replace(KRONIK_REGEX, "")
            .replace(LINKKU_REGEX, "")
            .replace(CUCI_REGEX, "")
            .replace(IDEKU_REGEX, "")
            .replace(DAILYOS_REGEX, "")
        val openBracket = v.lastIndexOf('[')
        if (openBracket != -1 && !v.substring(openBracket).contains(']')) {
            v = v.substring(0, openBracket)
        }
        return cleanChatFormatting(v)
    }

    data class ParsedFinal(
        val cleanText: String,
        val expression: String?,
        val openApp: String? = null,
        val choices: ChatChoicesData? = null,
        val searchAction: SearchQueryAction? = null,
        val memoryActions: List<MemoryChatAction> = emptyList(),
        val kronikActions: List<KronikChatAction> = emptyList(),
        val linkkuActions: List<LinkkuChatAction> = emptyList(),
        val cuciActions: List<CuciChatAction> = emptyList(),
        val idekuActions: List<IdekuChatAction> = emptyList(),
        val dailyOsActions: List<DailyOsChatAction> = emptyList()
    )

    fun parseFinal(rawText: String): ParsedFinal {
        val exprMatch = EXPR_REGEX.find(rawText)
        val rawExpr = exprMatch?.groupValues?.getOrNull(1)?.lowercase()
        val expression = if (rawExpr != null && rawExpr in VALID_EXPRS) rawExpr else null

        val openApp = OPEN_REGEX.find(rawText)?.groupValues?.getOrNull(1)?.lowercase()
        val choices = CHOICES_REGEX.find(rawText)?.groupValues?.getOrNull(1)?.let { parseChoicesPayload(it) }
        val searchAction = SEARCH_REGEX.find(rawText)?.groupValues?.getOrNull(1)?.let { parseSearchPayload(it) }
        val memoryActions = MEMORY_REGEX.findAll(rawText).mapNotNull { match ->
            parseMemoryPayload(match.groupValues.getOrNull(1).orEmpty())
        }.toList()
        val kronikActions = KRONIK_REGEX.findAll(rawText).mapNotNull { match ->
            parseKronikPayload(match.groupValues.getOrNull(1).orEmpty())
        }.toList()
        val linkkuActions = LINKKU_REGEX.findAll(rawText).mapNotNull { match ->
            parseLinkkuPayload(match.groupValues.getOrNull(1).orEmpty())
        }.toList()
        val cuciActions = CUCI_REGEX.findAll(rawText).mapNotNull { match ->
            parseCuciPayload(match.groupValues.getOrNull(1).orEmpty())
        }.toList()
        val idekuActions = IDEKU_REGEX.findAll(rawText).mapNotNull { match ->
            parseIdekuPayload(match.groupValues.getOrNull(1).orEmpty())
        }.toList()
        val dailyOsActions = DAILYOS_REGEX.findAll(rawText).mapNotNull { match ->
            parseDailyOsPayload(match.groupValues.getOrNull(1).orEmpty())
        }.toList()

        val clean = cleanChatFormatting(
            rawText
                .replace(JA_REGEX, "")
                .replace(EXPR_REGEX, "")
                .replace(OPEN_REGEX, "")
                .replace(CHOICES_REGEX, "")
                .replace(SEARCH_REGEX, "")
                .replace(MEMORY_REGEX, "")
                .replace(KRONIK_REGEX, "")
                .replace(LINKKU_REGEX, "")
                .replace(CUCI_REGEX, "")
                .replace(IDEKU_REGEX, "")
                .replace(DAILYOS_REGEX, "")
        ).trim()

        return ParsedFinal(
            cleanText = clean,
            expression = expression,
            openApp = openApp,
            choices = choices,
            searchAction = searchAction,
            memoryActions = memoryActions,
            kronikActions = kronikActions,
            linkkuActions = linkkuActions,
            cuciActions = cuciActions,
            idekuActions = idekuActions,
            dailyOsActions = dailyOsActions
        )
    }

    private fun parseSearchPayload(payload: String): SearchQueryAction? {
        val parts = payload.split("|").map { it.trim() }
        if (parts.isEmpty()) return null
        val rawScope = parts[0].lowercase()
        val normalizedScope = when (rawScope) {
            "kronik", "jurnal" -> "kronik"
            "linkku", "bookmark", "link" -> "linkku"
            "cuci", "cucimoney", "cucimoney+", "keuangan", "transaksi" -> "cuci"
            "ideku", "ide", "wishlist" -> "ideku"
            "dailyos", "daily", "habit", "todo" -> "dailyos"
            "chat", "obrolan", "sessions", "sesi", "log" -> "chat"
            "all", "semua" -> "all"
            else -> null
        }
        return if (normalizedScope != null) {
            val q = parts.drop(1).joinToString(" ").trim().take(80)
            SearchQueryAction(scope = normalizedScope, query = q)
        } else {
            SearchQueryAction(scope = "all", query = parts.joinToString(" ").trim().take(80))
        }
    }

    private fun parseMemoryPayload(payload: String): MemoryChatAction? {
        val parts = payload.split("|").map { it.trim() }
        if (parts.isEmpty()) return null
        return when (parts[0].uppercase()) {
            "SET", "ADD", "UPDATE" -> {
                val catRaw = parts.getOrNull(1).orEmpty().lowercase()
                val cat = if (catRaw in setOf("identitas", "preferensi", "kebiasaan", "tujuan", "catatan")) catRaw else "catatan"
                val key = parts.getOrNull(2).orEmpty()
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]+"), "_")
                    .trim('_')
                    .take(48)
                val valTxt = parts.getOrNull(3).orEmpty().take(180)
                if (key.length < 2 || valTxt.isBlank()) null
                else MemoryChatAction.SetMemory(category = cat, factKey = key, factValue = valTxt)
            }
            "DEL", "DELETE", "FORGET", "REMOVE" -> {
                val q = parts.getOrNull(1).orEmpty().trim()
                if (q.isBlank()) null else MemoryChatAction.DeleteMemory(query = q)
            }
            else -> null
        }
    }

    private fun parseChoicesPayload(payload: String): ChatChoicesData? {
        val parts = payload.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) return null
        val head = parts[0].lowercase()
        val (mode, rawItems) = when (head) {
            "multi", "multiple" -> "multi" to parts.drop(1)
            "single" -> "single" to parts.drop(1)
            else -> "single" to parts
        }
        val items = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (item in rawItems) {
            val k = item.lowercase()
            if (seen.add(k)) {
                items.add(item.take(60))
            }
            if (items.size >= 8) break
        }
        if (items.isEmpty()) return null
        return ChatChoicesData(mode = mode, items = items, selected = emptyList())
    }

    private fun parseKronikPayload(payload: String): KronikChatAction? {
        val parts = payload.split("|").map { it.trim() }
        if (parts.isEmpty()) return null
        val cmd = parts[0].uppercase()
        return when (cmd) {
            "ADD", "WRITE", "NEW" -> {
                val title = parts.getOrNull(1).orEmpty()
                val content = parts.getOrNull(2).orEmpty()
                val mood = parts.getOrNull(3)?.takeIf { it.isNotBlank() }
                if (title.isBlank() && content.isBlank()) null
                else KronikChatAction.AddEntry(title = title, content = content, mood = mood)
            }
            else -> null
        }
    }

    private fun parseLinkkuPayload(payload: String): LinkkuChatAction? {
        val parts = payload.split("|").map { it.trim() }
        if (parts.isEmpty()) return null
        val cmd = parts[0].uppercase()
        return when (cmd) {
            "ADD", "SAVE", "NEW" -> {
                val rawUrl = parts.getOrNull(1).orEmpty()
                if (rawUrl.isBlank()) return null
                val url = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                    rawUrl
                } else {
                    "https://$rawUrl"
                }
                val title = parts.getOrNull(2).orEmpty()
                val note = parts.getOrNull(3).orEmpty()
                val category = parts.getOrNull(4).orEmpty()
                val tags = parts.getOrNull(5)
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()
                LinkkuChatAction.AddLink(
                    url = url,
                    title = title,
                    note = note,
                    category = category,
                    tags = tags
                )
            }
            else -> null
        }
    }

    private fun parseCuciPayload(payload: String): CuciChatAction? {
        val parts = payload.split("|").map { it.trim() }
        if (parts.isEmpty()) return null
        val cmd = parts[0].uppercase()
        return when (cmd) {
            "ADD", "NEW", "LOG" -> {
                // Format lengkap: [CUCI:ADD|out|25000|Kopi|Makan|Tunai|Personal|2026-11-22]
                // Mendukung juga tanpa direction eksplisit: [CUCI:ADD|25000|Kopi|...]
                val p1 = parts.getOrNull(1).orEmpty()
                val p1Lower = p1.lowercase()
                val explicitDir = p1Lower in setOf("out", "in", "expense", "income", "keluar", "masuk")
                val direction = if (explicitDir) {
                    if (p1Lower in setOf("in", "income", "masuk")) "in" else "out"
                } else "out"
                val offset = if (explicitDir) 1 else 0
                val rawAmt = parts.getOrNull(1 + offset).orEmpty()
                val amount = rawAmt.replace(Regex("""[^0-9]"""), "").toLongOrNull() ?: return null
                if (amount <= 0L) return null
                val title = parts.getOrNull(2 + offset).orEmpty()
                val categoryName = parts.getOrNull(3 + offset).orEmpty()
                val accountName = parts.getOrNull(4 + offset).orEmpty()
                val rawSlot5 = parts.getOrNull(5 + offset).orEmpty()
                val rawSlot6 = parts.getOrNull(6 + offset).orEmpty()
                val slot5Date = normalizeCuciDate(rawSlot5)
                val (bookName, date) = if (rawSlot6.isBlank() && slot5Date.isNotEmpty()) {
                    "" to slot5Date
                } else {
                    rawSlot5 to normalizeCuciDate(rawSlot6)
                }
                CuciChatAction.AddTransaction(
                    direction = direction,
                    amount = amount,
                    title = title.ifBlank { if (direction == "in") "Pemasukan" else "Pengeluaran" },
                    categoryName = categoryName,
                    accountName = accountName,
                    bookName = bookName,
                    date = date
                )
            }
            "SAVE", "NABUNG", "DEPOSIT", "TABUNG" -> {
                // [CUCI:SAVE|nama_wishlist|nominal|akun_sumber_opsional|buku_opsional]
                val p1 = parts.getOrNull(1).orEmpty()
                val p2 = parts.getOrNull(2).orEmpty()
                val p1Digits = p1.replace(Regex("""[^0-9]"""), "")
                val p2Digits = p2.replace(Regex("""[^0-9]"""), "")
                val flipped = p1Digits.isNotEmpty() && p1.all { it.isDigit() || it == '.' || it == ',' } && p2.any { it.isLetter() }
                val wishName = (if (flipped) p2 else p1).trim()
                val amtRaw = if (flipped) p1Digits else p2Digits
                val amount = amtRaw.toLongOrNull() ?: return null
                if (wishName.isBlank() || amount <= 0L) return null
                val accName = parts.getOrNull(3).orEmpty().trim()
                val bookName = parts.getOrNull(4).orEmpty().trim()
                CuciChatAction.SaveWishlist(
                    wishlistQuery = wishName,
                    amount = amount,
                    accountName = accName,
                    bookName = bookName
                )
            }
            "REALIZE", "BELI", "DONE", "SELESAI", "CAIRKAN" -> {
                // [CUCI:REALIZE|nama_wishlist|akun_opsional|nominal_beli_opsional|buku_opsional]
                val wishName = parts.getOrNull(1).orEmpty().trim()
                if (wishName.isBlank()) return null
                val p2 = parts.getOrNull(2).orEmpty().trim()
                val p3 = parts.getOrNull(3).orEmpty().trim()
                val p2IsNum = p2.isNotEmpty() && p2.all { it.isDigit() || it == '.' || it == ',' }
                val accountName = if (p2IsNum) p3 else p2
                val amtRaw = (if (p2IsNum) p2 else p3).replace(Regex("""[^0-9]"""), "")
                val paidAmount = amtRaw.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
                val bookName = parts.getOrNull(4).orEmpty().trim()
                CuciChatAction.RealizeWishlist(
                    wishlistQuery = wishName,
                    accountName = accountName,
                    paidAmount = paidAmount,
                    bookName = bookName
                )
            }
            else -> null
        }
    }

    private fun normalizeCuciDate(raw: String): String {
        val clean = raw.trim()
        if (clean.isEmpty()) return ""
        return when (clean.lowercase()) {
            "hari ini", "today" -> java.time.LocalDate.now().toString()
            "kemarin", "yesterday" -> java.time.LocalDate.now().minusDays(1).toString()
            "kemarin lusa" -> java.time.LocalDate.now().minusDays(2).toString()
            else -> {
                if (Regex("""^\d{4}-\d{2}-\d{2}$""").matches(clean)) {
                    runCatching { java.time.LocalDate.parse(clean).toString() }.getOrDefault("")
                } else {
                    val dmy = Regex("""^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$""").matchEntire(clean)
                    if (dmy != null) {
                        val day = dmy.groupValues[1].toIntOrNull() ?: return ""
                        val month = dmy.groupValues[2].toIntOrNull() ?: return ""
                        val year = dmy.groupValues[3].toIntOrNull() ?: return ""
                        runCatching { java.time.LocalDate.of(year, month, day).toString() }.getOrDefault("")
                    } else ""
                }
            }
        }
    }

    private fun parseIdekuPayload(payload: String): IdekuChatAction? {
        val parts = payload.split("|").map { it.trim() }
        if (parts.isEmpty()) return null
        val cmd = parts[0].uppercase()
        return when (cmd) {
            "ADD", "NEW", "SAVE" -> {
                // Formats:
                // [IDEKU:ADD|Idea|judul|kategori|prioritas|catatan|jalur]
                // [IDEKU:ADD|Wishlist|judul|harga|kategori|prioritas|catatan]
                // [IDEKU:ADD|judul|kategori|catatan]
                val p1 = parts.getOrNull(1).orEmpty()
                val p1Lower = p1.lowercase()
                val explicitType = p1Lower == "idea" || p1Lower == "ide" || p1Lower == "wishlist"
                val type = if (p1Lower == "wishlist") "Wishlist" else "Idea"
                if (explicitType) {
                    val title = parts.getOrNull(2).orEmpty()
                    if (title.isBlank()) return null
                    if (type == "Wishlist") {
                        val price = parts.getOrNull(3).orEmpty().replace(Regex("""[^0-9]"""), "").toLongOrNull() ?: 0L
                        val cat = parts.getOrNull(4).orEmpty().ifBlank { "Belanja" }
                        val pri = normalizeIdekuPriority(parts.getOrNull(5).orEmpty())
                        val notes = parts.getOrNull(6).orEmpty()
                        IdekuChatAction.AddItem(type = "Wishlist", title = title, category = cat, priority = pri, notes = notes, price = price)
                    } else {
                        val cat = parts.getOrNull(3).orEmpty().ifBlank { "Personal" }
                        val pri = normalizeIdekuPriority(parts.getOrNull(4).orEmpty())
                        val notes = parts.getOrNull(5).orEmpty()
                        val jalur = parts.getOrNull(6).orEmpty()
                        IdekuChatAction.AddItem(type = "Idea", title = title, category = cat, priority = pri, notes = notes, jalur = jalur)
                    }
                } else {
                    if (p1.isBlank()) return null
                    val cat = parts.getOrNull(2).orEmpty().ifBlank { "Personal" }
                    val notes = parts.getOrNull(3).orEmpty()
                    val pri = normalizeIdekuPriority(parts.getOrNull(4).orEmpty())
                    IdekuChatAction.AddItem(type = "Idea", title = p1, category = cat, priority = pri, notes = notes)
                }
            }
            else -> null
        }
    }

    private fun normalizeIdekuPriority(raw: String): String {
        return when (raw.trim().lowercase()) {
            "penting", "tinggi", "urgent", "high" -> "Penting"
            "kapan-kapan", "kapan2", "rendah", "low" -> "Kapan-kapan"
            else -> "Nanti"
        }
    }

    private fun parseDailyOsPayload(payload: String): DailyOsChatAction? {
        val parts = payload.split("|").map { it.trim() }
        if (parts.isEmpty()) return null
        return when (parts[0].uppercase()) {
            "HABIT" -> {
                val name = parts.getOrNull(1).orEmpty()
                if (name.isBlank()) null
                else DailyOsChatAction.AddHabit(
                    name = name,
                    category = parts.getOrNull(2).orEmpty().ifBlank { "General" },
                    icon = parts.getOrNull(3).orEmpty().ifBlank { "star" }
                )
            }
            "TODO", "TASK" -> {
                val title = parts.getOrNull(1).orEmpty()
                if (title.isBlank()) null
                else {
                    val priRaw = parts.getOrNull(2).orEmpty().trim().lowercase()
                    val pri = when (priRaw) {
                        "urgent", "hari ini", "today", "high" -> "urgent"
                        "soon", "minggu ini", "week", "medium" -> "soon"
                        "normal", "nanti", "later", "low" -> "normal"
                        else -> "urgent"
                    }
                    val dlRaw = parts.getOrNull(3).orEmpty().trim()
                    val dl = if (Regex("""^\d{4}-\d{2}-\d{2}$""").matches(dlRaw)) dlRaw else ""
                    DailyOsChatAction.AddTodo(title = title, priority = pri, deadline = dl)
                }
            }
            "CHECK", "DONE", "COMPLETE" -> {
                val q = parts.getOrNull(1).orEmpty()
                if (q.isBlank()) null else DailyOsChatAction.CheckItem(query = q)
            }
            else -> null
        }
    }

    fun splitForTts(text: String, minLen: Int = 12, maxChunks: Int = 4): List<String> {
        val trimmed = text.trim().take(500)
        if (trimmed.isEmpty()) return emptyList()

        val sentenceRegex = Regex("""[^。！？!?.]+[。！？!?.]*""")
        val parts = sentenceRegex.findAll(trimmed).map { it.value }.toList().ifEmpty { listOf(trimmed) }

        val chunks = mutableListOf<String>()
        var buf = StringBuilder()
        for (part in parts) {
            buf.append(part)
            if (buf.toString().trim().length >= minLen) {
                chunks.add(buf.toString().trim())
                buf = StringBuilder()
            }
        }
        val leftover = buf.toString().trim()
        if (leftover.isNotEmpty()) {
            if (chunks.isNotEmpty()) {
                chunks[chunks.lastIndex] = "${chunks.last()} $leftover"
            } else {
                chunks.add(leftover)
            }
        }
        while (chunks.size > maxChunks) {
            val last = chunks.removeAt(chunks.lastIndex)
            chunks[chunks.lastIndex] = "${chunks.last()} $last"
        }
        return chunks
    }
}
