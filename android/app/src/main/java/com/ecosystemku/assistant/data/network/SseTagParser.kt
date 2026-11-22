package com.ecosystemku.assistant.data.network

object SseTagParser {
    private val JA_REGEX = Regex("""\[JA][\s\S]*?(\[/JA]|$)""", RegexOption.IGNORE_CASE)
    private val EXPR_REGEX = Regex("""\[EXPR:([a-z]+)]""", RegexOption.IGNORE_CASE)
    private val OPEN_REGEX = Regex("""\[OPEN:([a-z0-9-]+)]""", RegexOption.IGNORE_CASE)
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
            val accountName: String = ""
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

    fun visibleTextDuringStream(raw: String): String {
        var v = raw
            .replace(JA_REGEX, "")
            .replace(EXPR_REGEX, "")
            .replace(OPEN_REGEX, "")
            .replace(KRONIK_REGEX, "")
            .replace(LINKKU_REGEX, "")
            .replace(CUCI_REGEX, "")
            .replace(IDEKU_REGEX, "")
            .replace(DAILYOS_REGEX, "")
        val openBracket = v.lastIndexOf('[')
        if (openBracket != -1 && !v.substring(openBracket).contains(']')) {
            v = v.substring(0, openBracket)
        }
        return v
    }

    data class ParsedFinal(
        val cleanText: String,
        val expression: String?,
        val openApp: String? = null,
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

        val clean = rawText
            .replace(JA_REGEX, "")
            .replace(EXPR_REGEX, "")
            .replace(OPEN_REGEX, "")
            .replace(KRONIK_REGEX, "")
            .replace(LINKKU_REGEX, "")
            .replace(CUCI_REGEX, "")
            .replace(IDEKU_REGEX, "")
            .replace(DAILYOS_REGEX, "")
            .trim()

        return ParsedFinal(
            cleanText = clean,
            expression = expression,
            openApp = openApp,
            kronikActions = kronikActions,
            linkkuActions = linkkuActions,
            cuciActions = cuciActions,
            idekuActions = idekuActions,
            dailyOsActions = dailyOsActions
        )
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
                // Support both [CUCI:ADD|out|25000|Kopi|Makan|Tunai] and [CUCI:ADD|25000|Kopi]
                val p1 = parts.getOrNull(1).orEmpty()
                val p1Lower = p1.lowercase()
                val explicitDir = p1Lower in setOf("out", "in", "expense", "income", "keluar", "masuk")
                val direction = if (explicitDir) {
                    if (p1Lower in setOf("in", "income", "masuk")) "in" else "out"
                } else "out"
                val rawAmt = if (explicitDir) parts.getOrNull(2).orEmpty() else p1
                val amount = rawAmt.replace(Regex("""[^0-9]"""), "").toLongOrNull() ?: return null
                if (amount <= 0L) return null
                val title = (if (explicitDir) parts.getOrNull(3) else parts.getOrNull(2)).orEmpty()
                val categoryName = (if (explicitDir) parts.getOrNull(4) else parts.getOrNull(3)).orEmpty()
                val accountName = (if (explicitDir) parts.getOrNull(5) else parts.getOrNull(4)).orEmpty()
                CuciChatAction.AddTransaction(
                    direction = direction,
                    amount = amount,
                    title = title.ifBlank { if (direction == "in") "Pemasukan" else "Pengeluaran" },
                    categoryName = categoryName,
                    accountName = accountName
                )
            }
            else -> null
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
