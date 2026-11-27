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
    private val DEVICE_REGEX = Regex("""\[DEVICE:([^\]\r\n]+)]""", RegexOption.IGNORE_CASE)
    private val KG_REGEX = Regex("""\[KG:([^\]\r\n]+)]""", RegexOption.IGNORE_CASE)

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

    sealed class KgChatAction {
        data class UpdateNode(
            val target: String,
            val newLabel: String = "",
            val newSummary: String = "",
            val newType: String = "",
            val reason: String = ""
        ) : KgChatAction()

        data class ArchiveNode(
            val target: String,
            val reason: String = ""
        ) : KgChatAction()

        data class MergeNodes(
            val sourceTarget: String,
            val destinationTarget: String,
            val reason: String = ""
        ) : KgChatAction()
    }

    sealed class KronikChatAction {
        data class AddEntry(
            val title: String,
            val content: String,
            val mood: String? = null
        ) : KronikChatAction()

        data class EditEntry(
            val target: String,
            val title: String = "",
            val content: String = "",
            val mood: String? = null,
            val date: String = ""
        ) : KronikChatAction()

        data class DeleteEntry(
            val target: String,
            val archiveOnly: Boolean = false
        ) : KronikChatAction()

        data class MergeEntries(
            val sourceTarget: String,
            val destinationTarget: String
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

        data class EditTransaction(
            val target: String,
            val amount: Long? = null,
            val title: String = "",
            val categoryName: String = "",
            val accountName: String = "",
            val date: String = ""
        ) : CuciChatAction()

        data class DeleteTransaction(
            val target: String
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

        data class SetStatus(
            val target: String,
            val status: String
        ) : IdekuChatAction()

        data class EditItem(
            val target: String,
            val title: String = "",
            val category: String = "",
            val priority: String = "",
            val notes: String = "",
            val jalur: String = "",
            val price: Long? = null
        ) : IdekuChatAction()

        data class DeleteItem(
            val target: String
        ) : IdekuChatAction()

        data class ArchiveItem(
            val target: String
        ) : IdekuChatAction()

        data class MergeItems(
            val sourceTarget: String,
            val destinationTarget: String
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

        data class UncheckItem(
            val query: String
        ) : DailyOsChatAction()

        data class EditTodo(
            val target: String,
            val title: String = "",
            val priority: String = "",
            val deadline: String = "" // "" = keep, "__CLEAR__" = clear, "YYYY-MM-DD" = set
        ) : DailyOsChatAction()

        data class DeleteTodo(
            val target: String
        ) : DailyOsChatAction()

        data class ArchiveHabit(
            val target: String,
            val deletePermanently: Boolean = false
        ) : DailyOsChatAction()
    }

    sealed class DeviceChatAction {
        data class OpenUrl(val url: String) : DeviceChatAction()
        data class ShareText(val text: String) : DeviceChatAction()
        data class SetAlarm(val hour: Int, val minute: Int, val label: String = "") : DeviceChatAction()
        data class SetTimer(val seconds: Int, val label: String = "") : DeviceChatAction()
        data class SetReminder(
            val dateStr: String,
            val hour: Int,
            val minute: Int,
            val message: String
        ) : DeviceChatAction()
        data class CancelReminder(val target: String) : DeviceChatAction()
        data class AddCalendarEvent(
            val title: String,
            val dateStr: String,
            val startHour: Int,
            val startMinute: Int,
            val endHour: Int? = null,
            val endMinute: Int? = null,
            val location: String = "",
            val description: String = ""
        ) : DeviceChatAction()
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
            .replace(KG_REGEX, "")
            .replace(KRONIK_REGEX, "")
            .replace(LINKKU_REGEX, "")
            .replace(CUCI_REGEX, "")
            .replace(IDEKU_REGEX, "")
            .replace(DAILYOS_REGEX, "")
            .replace(DEVICE_REGEX, "")
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
        val searchActions: List<SearchQueryAction> = emptyList(),
        val memoryActions: List<MemoryChatAction> = emptyList(),
        val kgActions: List<KgChatAction> = emptyList(),
        val kronikActions: List<KronikChatAction> = emptyList(),
        val linkkuActions: List<LinkkuChatAction> = emptyList(),
        val cuciActions: List<CuciChatAction> = emptyList(),
        val idekuActions: List<IdekuChatAction> = emptyList(),
        val dailyOsActions: List<DailyOsChatAction> = emptyList(),
        val deviceActions: List<DeviceChatAction> = emptyList()
    ) {
        fun hasAnyWriteAction(): Boolean =
            memoryActions.isNotEmpty() ||
                kgActions.isNotEmpty() ||
                kronikActions.isNotEmpty() ||
                linkkuActions.isNotEmpty() ||
                cuciActions.isNotEmpty() ||
                idekuActions.isNotEmpty() ||
                dailyOsActions.isNotEmpty() ||
                deviceActions.isNotEmpty()
    }

    fun parseFinal(rawText: String): ParsedFinal {
        val exprMatch = EXPR_REGEX.find(rawText)
        val rawExpr = exprMatch?.groupValues?.getOrNull(1)?.lowercase()
        val expression = if (rawExpr != null && rawExpr in VALID_EXPRS) rawExpr else null

        val openApp = OPEN_REGEX.find(rawText)?.groupValues?.getOrNull(1)?.lowercase()
        val choices = CHOICES_REGEX.find(rawText)?.groupValues?.getOrNull(1)?.let { parseChoicesPayload(it) }
        val searchActions = SEARCH_REGEX.findAll(rawText).mapNotNull { match ->
            parseSearchPayload(match.groupValues.getOrNull(1).orEmpty())
        }.take(5).toList()
        val searchAction = searchActions.firstOrNull()
        val memoryActions = MEMORY_REGEX.findAll(rawText).mapNotNull { match ->
            parseMemoryPayload(match.groupValues.getOrNull(1).orEmpty())
        }.toList()
        val kgActions = KG_REGEX.findAll(rawText).mapNotNull { match ->
            parseKgPayload(match.groupValues.getOrNull(1).orEmpty())
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
        val deviceActions = DEVICE_REGEX.findAll(rawText).mapNotNull { match ->
            parseDevicePayload(match.groupValues.getOrNull(1).orEmpty())
        }.toList()

        val clean = cleanChatFormatting(
            rawText
                .replace(JA_REGEX, "")
                .replace(EXPR_REGEX, "")
                .replace(OPEN_REGEX, "")
                .replace(CHOICES_REGEX, "")
                .replace(SEARCH_REGEX, "")
                .replace(MEMORY_REGEX, "")
                .replace(KG_REGEX, "")
                .replace(KRONIK_REGEX, "")
                .replace(LINKKU_REGEX, "")
                .replace(CUCI_REGEX, "")
                .replace(IDEKU_REGEX, "")
                .replace(DAILYOS_REGEX, "")
                .replace(DEVICE_REGEX, "")
        ).trim()

        return ParsedFinal(
            cleanText = clean,
            expression = expression,
            openApp = openApp,
            choices = choices,
            searchAction = searchAction,
            searchActions = searchActions,
            memoryActions = memoryActions,
            kgActions = kgActions,
            kronikActions = kronikActions,
            linkkuActions = linkkuActions,
            cuciActions = cuciActions,
            idekuActions = idekuActions,
            dailyOsActions = dailyOsActions,
            deviceActions = deviceActions
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
            "kg", "graph", "knowledge_graph", "otak", "kepribadian", "pola_pikir" -> "kg"
            "device", "perangkat", "hp", "android", "calendar", "kalender", "battery", "baterai", "location", "lokasi", "reminder", "reminders", "pengingat" -> "device"
            "chat", "obrolan", "sessions", "sesi", "log" -> "chat"
            "all", "semua" -> "all"
            else -> null
        }
        return if (normalizedScope != null) {
            val tail = parts.drop(1).joinToString("|").trim().take(160)
            val q = if (normalizedScope == "device" && rawScope in setOf("calendar", "kalender", "battery", "baterai", "location", "lokasi", "reminder", "reminders", "pengingat")) {
                listOf(rawScope, tail).filter { it.isNotBlank() }.joinToString(" ").take(160)
            } else {
                tail
            }
            SearchQueryAction(scope = normalizedScope, query = q)
        } else {
            SearchQueryAction(scope = "all", query = parts.joinToString(" ").trim().take(160))
        }
    }

    private fun parseKgPayload(payload: String): KgChatAction? {
        val parts = payload.split("|").map { it.trim() }
        if (parts.isEmpty()) return null
        val cmd = parts[0].uppercase()
        return when (cmd) {
            "UPDATE", "EDIT", "UBAH" -> {
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) return null
                val newLabel = parts.getOrNull(2).orEmpty().trim()
                val newSummary = parts.getOrNull(3).orEmpty().trim()
                val newType = parts.getOrNull(4).orEmpty().trim().uppercase()
                val reason = parts.getOrNull(5).orEmpty().trim()
                if (newLabel.isBlank() && newSummary.isBlank() && newType.isBlank()) {
                    null
                } else {
                    KgChatAction.UpdateNode(
                        target = target,
                        newLabel = newLabel,
                        newSummary = newSummary,
                        newType = newType,
                        reason = reason
                    )
                }
            }
            "ARCHIVE", "ARSIP", "USANG", "DEPRECATE" -> {
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) null
                else KgChatAction.ArchiveNode(
                    target = target,
                    reason = parts.getOrNull(2).orEmpty().trim()
                )
            }
            "MERGE", "GABUNG" -> {
                val source = parts.getOrNull(1).orEmpty().trim()
                val dest = parts.getOrNull(2).orEmpty().trim()
                if (source.isBlank() || dest.isBlank()) null
                else KgChatAction.MergeNodes(
                    sourceTarget = source,
                    destinationTarget = dest,
                    reason = parts.getOrNull(3).orEmpty().trim()
                )
            }
            else -> null
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
            "EDIT", "UPDATE", "FIX" -> {
                // [KRONIK:EDIT|id_atau_query|judul_baru|isi_baru|mood_baru|tanggal_yyyy-mm-dd]
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) return null
                val title = parts.getOrNull(2).orEmpty().trim()
                val content = parts.getOrNull(3).orEmpty().trim()
                val rawMood = parts.getOrNull(4).orEmpty().trim()
                val mood = when {
                    rawMood.isBlank() -> null
                    rawMood.lowercase() in setOf("-", "none", "hapus", "clear") -> "__CLEAR__"
                    else -> rawMood
                }
                val date = normalizeCuciDate(parts.getOrNull(5).orEmpty())
                if (title.isBlank() && content.isBlank() && mood == null && date.isBlank()) {
                    return null
                }
                KronikChatAction.EditEntry(
                    target = target,
                    title = title,
                    content = content,
                    mood = mood,
                    date = date
                )
            }
            "DEL", "DELETE", "REMOVE", "HAPUS", "ARCHIVE", "ARSIP" -> {
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) null
                else KronikChatAction.DeleteEntry(
                    target = target,
                    archiveOnly = cmd == "ARCHIVE" || cmd == "ARSIP"
                )
            }
            "MERGE", "GABUNG" -> {
                val source = parts.getOrNull(1).orEmpty().trim()
                val dest = parts.getOrNull(2).orEmpty().trim()
                if (source.isBlank() || dest.isBlank()) null
                else KronikChatAction.MergeEntries(
                    sourceTarget = source,
                    destinationTarget = dest
                )
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
                val inDirections = setOf("in", "income", "masuk", "pemasukan", "duit masuk")
                val outDirections = setOf("out", "expense", "keluar", "pengeluaran", "duit keluar")
                val explicitDir = p1Lower in inDirections || p1Lower in outDirections
                val direction = if (explicitDir) {
                    if (p1Lower in inDirections) "in" else "out"
                } else "out"
                val offset = if (explicitDir) 1 else 0
                val rawAmt = parts.getOrNull(1 + offset).orEmpty()
                val amount = parseAmountRupiah(rawAmt) ?: return null
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
                val p1Amt = parseAmountRupiah(p1)
                val p2Amt = parseAmountRupiah(p2)
                val flipped = p1Amt != null && p2Amt == null && p2.any { it.isLetter() }
                val wishName = (if (flipped) p2 else p1).trim()
                val amount = (if (flipped) p1Amt else p2Amt) ?: return null
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
                val p2Amt = parseAmountRupiah(p2)
                val p2IsNum = p2Amt != null
                val accountName = if (p2IsNum) p3 else p2
                val paidAmount = (if (p2IsNum) p2Amt else parseAmountRupiah(p3))?.coerceAtLeast(0L) ?: 0L
                val bookName = parts.getOrNull(4).orEmpty().trim()
                CuciChatAction.RealizeWishlist(
                    wishlistQuery = wishName,
                    accountName = accountName,
                    paidAmount = paidAmount,
                    bookName = bookName
                )
            }
            "EDIT", "UPDATE", "FIX" -> {
                // [CUCI:EDIT|id_atau_query|nominal_baru_atau_kosong|judul_baru_atau_kosong|kategori_baru_atau_kosong|akun_baru_atau_kosong|tanggal_yyyy-mm-dd_atau_kosong]
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) return null
                val rawAmt = parts.getOrNull(2).orEmpty().trim()
                val parsedAmt = if (rawAmt.isNotBlank()) {
                    parseAmountRupiah(rawAmt)?.takeIf { it > 0L }
                } else null
                val newTitle = parts.getOrNull(3).orEmpty().trim()
                val newCategory = parts.getOrNull(4).orEmpty().trim()
                val newAccount = parts.getOrNull(5).orEmpty().trim()
                val newDate = normalizeCuciDate(parts.getOrNull(6).orEmpty())
                if (parsedAmt == null && newTitle.isBlank() && newCategory.isBlank() && newAccount.isBlank() && newDate.isBlank()) {
                    return null
                }
                CuciChatAction.EditTransaction(
                    target = target,
                    amount = parsedAmt,
                    title = newTitle,
                    categoryName = newCategory,
                    accountName = newAccount,
                    date = newDate
                )
            }
            "DEL", "DELETE", "REMOVE", "HAPUS" -> {
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) null else CuciChatAction.DeleteTransaction(target = target)
            }
            else -> null
        }
    }

    internal fun parseAmountRupiah(raw: String): Long? {
        val cleaned = raw.trim().lowercase()
            .replace(Regex("""^rp\.?\s*"""), "")
            .replace("idr", "")
            .trim()
        if (cleaned.isEmpty()) return null

        val suffixMatch = Regex("""^([0-9][0-9.,]*)\s*(jt|juta|rb|ribu|k)$""").matchEntire(cleaned)
        if (suffixMatch != null) {
            val numToken = suffixMatch.groupValues[1]
            val suffix = suffixMatch.groupValues[2]
            val multiplier = if (suffix == "jt" || suffix == "juta") 1_000_000.0 else 1_000.0
            val normalizedNum = when {
                numToken.contains(',') && numToken.contains('.') ->
                    numToken.replace(".", "").replace(',', '.')
                numToken.contains(',') -> {
                    val afterComma = numToken.substringAfterLast(',')
                    if (afterComma.length == 3 && numToken.count { it == ',' } >= 1) {
                        numToken.replace(",", "")
                    } else {
                        numToken.replace(',', '.')
                    }
                }
                numToken.contains('.') -> {
                    val afterDot = numToken.substringAfterLast('.')
                    if (afterDot.length == 3 && numToken.count { it == '.' } >= 1) {
                        numToken.replace(".", "")
                    } else {
                        numToken
                    }
                }
                else -> numToken
            }
            val d = normalizedNum.toDoubleOrNull() ?: return null
            val result = kotlin.math.round(d * multiplier).toLong()
            return result.takeIf { it > 0L }
        }

        if (cleaned.any { it.isLetter() }) return null
        val withoutCents = cleaned.replace(Regex("""[.,]00$"""), "")
        val digits = withoutCents.replace(Regex("""[^0-9]"""), "")
        return digits.toLongOrNull()?.takeIf { it > 0L }
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
            "STATUS", "SET_STATUS", "DONE", "SELESAI" -> {
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) return null
                val rawStatus = parts.getOrNull(2).orEmpty().trim()
                val status = if (rawStatus.isBlank() && (cmd == "DONE" || cmd == "SELESAI")) {
                    "Selesai"
                } else {
                    normalizeIdekuStatus(rawStatus) ?: return null
                }
                IdekuChatAction.SetStatus(target = target, status = status)
            }
            "EDIT", "UPDATE" -> {
                // [IDEKU:EDIT|id_atau_query|judul_baru|kategori_baru|prioritas_baru|catatan_baru|jalur_baru|harga_baru_wishlist]
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) return null
                val title = parts.getOrNull(2).orEmpty().trim()
                val cat = parts.getOrNull(3).orEmpty().trim()
                val priRaw = parts.getOrNull(4).orEmpty().trim()
                val pri = if (priRaw.isNotBlank()) normalizeIdekuPriority(priRaw) else ""
                val notes = parts.getOrNull(5).orEmpty().trim()
                val jalur = parts.getOrNull(6).orEmpty().trim()
                val priceRaw = parts.getOrNull(7).orEmpty().trim()
                val price = if (priceRaw.isNotBlank()) {
                    priceRaw.replace(Regex("""[^0-9]"""), "").toLongOrNull()
                } else null
                if (title.isBlank() && cat.isBlank() && pri.isBlank() && notes.isBlank() && jalur.isBlank() && price == null) {
                    return null
                }
                IdekuChatAction.EditItem(
                    target = target,
                    title = title,
                    category = cat,
                    priority = pri,
                    notes = notes,
                    jalur = jalur,
                    price = price
                )
            }
            "ARCHIVE", "ARSIP" -> {
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) null else IdekuChatAction.ArchiveItem(target = target)
            }
            "MERGE", "GABUNG" -> {
                val source = parts.getOrNull(1).orEmpty().trim()
                val dest = parts.getOrNull(2).orEmpty().trim()
                if (source.isBlank() || dest.isBlank()) null
                else IdekuChatAction.MergeItems(
                    sourceTarget = source,
                    destinationTarget = dest
                )
            }
            "DEL", "DELETE", "REMOVE", "HAPUS" -> {
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) null else IdekuChatAction.DeleteItem(target = target)
            }
            else -> null
        }
    }

    private fun normalizeIdekuStatus(raw: String): String? {
        return when (raw.trim().lowercase()) {
            "selesai", "done", "finish", "finished", "complete", "completed" -> "Selesai"
            "proses", "process", "aktif", "fokus", "in progress" -> "Proses"
            "parkir", "park", "hold", "tunda" -> "Parkir"
            "belum", "todo", "pending", "open" -> "Belum"
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
        return when (val cmd = parts[0].uppercase()) {
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
            "UNCHECK", "REOPEN", "BATAL" -> {
                val q = parts.getOrNull(1).orEmpty()
                if (q.isBlank()) null else DailyOsChatAction.UncheckItem(query = q)
            }
            "EDIT_TODO", "UPDATE_TODO", "EDIT", "UPDATE" -> {
                // [DAILYOS:EDIT_TODO|id_atau_query|judul_baru|priority_baru|deadline_baru]
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) return null
                val title = parts.getOrNull(2).orEmpty().trim()
                val priRaw = parts.getOrNull(3).orEmpty().trim().lowercase()
                val pri = when (priRaw) {
                    "urgent", "hari ini", "today", "high" -> "urgent"
                    "soon", "minggu ini", "week", "medium" -> "soon"
                    "normal", "nanti", "later", "low" -> "normal"
                    else -> ""
                }
                val dlRaw = parts.getOrNull(4).orEmpty().trim()
                val dl = when {
                    dlRaw.lowercase() in setOf("-", "none", "hapus", "clear") -> "__CLEAR__"
                    Regex("""^\d{4}-\d{2}-\d{2}$""").matches(dlRaw) -> dlRaw
                    else -> ""
                }
                if (title.isBlank() && pri.isBlank() && dl.isBlank()) return null
                DailyOsChatAction.EditTodo(target = target, title = title, priority = pri, deadline = dl)
            }
            "DEL_TODO", "DELETE_TODO", "REMOVE_TODO", "DEL", "DELETE", "HAPUS" -> {
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) null else DailyOsChatAction.DeleteTodo(target = target)
            }
            "ARCHIVE_HABIT", "DEL_HABIT", "DELETE_HABIT" -> {
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) null
                else DailyOsChatAction.ArchiveHabit(
                    target = target,
                    deletePermanently = cmd == "DEL_HABIT" || cmd == "DELETE_HABIT"
                )
            }
            else -> null
        }
    }

    data class CuciSearchFilter(
        val book: String = "",
        val month: String = "",
        val fromDate: String = "",
        val toDate: String = "",
        val category: String = "",
        val account: String = "",
        val direction: String = "", // "in" | "out" | ""
        val query: String = "",
        val isStructured: Boolean = false
    )

    fun parseCuciSearchQuery(rawQuery: String, defaultYear: Int = java.time.LocalDate.now().year): CuciSearchFilter {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return CuciSearchFilter()
        val keyRegex = Regex("""(?:^|[;|\s])(book|buku|month|bulan|from|dari|to|sampai|cat|kategori|acc|akun|dir|arah|q|query)=""", RegexOption.IGNORE_CASE)
        if (!keyRegex.containsMatchIn(trimmed)) {
            return parseNaturalCuciSearch(trimmed, defaultYear)
        }
        var book = ""
        var month = ""
        var fromDate = ""
        var toDate = ""
        var category = ""
        var account = ""
        var direction = ""
        val freeQueries = mutableListOf<String>()

        val segments = trimmed.split(Regex("""[;|]""")).map { it.trim() }.filter { it.isNotEmpty() }
        for (seg in segments) {
            val eqIdx = seg.indexOf('=')
            if (eqIdx <= 0) {
                freeQueries.add(seg)
                continue
            }
            val k = seg.substring(0, eqIdx).trim().lowercase()
            val v = seg.substring(eqIdx + 1).trim()
            if (v.isEmpty()) continue
            when (k) {
                "book", "buku" -> book = v
                "month", "bulan" -> {
                    val rangeParts = splitCuciRangeValue(v)
                    if (rangeParts != null) {
                        fromDate = normalizeCuciRangeBound(rangeParts.first, isEnd = false, defaultYear)
                        toDate = normalizeCuciRangeBound(rangeParts.second, isEnd = true, defaultYear)
                    } else {
                        val ym = resolveYearMonthToken(v, defaultYear)
                        if (ym != null) month = ym.toString()
                    }
                }
                "from", "dari" -> fromDate = normalizeCuciRangeBound(v, isEnd = false, defaultYear)
                "to", "sampai" -> toDate = normalizeCuciRangeBound(v, isEnd = true, defaultYear)
                "cat", "kategori" -> category = v
                "acc", "akun" -> account = v
                "dir", "arah" -> {
                    val vl = v.lowercase()
                    direction = when {
                        vl in setOf("in", "income", "masuk", "pemasukan") -> "in"
                        vl in setOf("out", "expense", "keluar", "pengeluaran") -> "out"
                        else -> ""
                    }
                }
                "q", "query" -> freeQueries.add(v)
                else -> freeQueries.add(seg)
            }
        }
        return CuciSearchFilter(
            book = book,
            month = month,
            fromDate = fromDate,
            toDate = toDate,
            category = category,
            account = account,
            direction = direction,
            query = freeQueries.joinToString(" ").trim(),
            isStructured = true
        )
    }

    private fun parseNaturalCuciSearch(trimmed: String, defaultYear: Int): CuciSearchFilter {
        val lower = trimmed.lowercase()
        val yearInText = Regex("""\b(20\d{2})\b""").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val activeYear = yearInText ?: defaultYear

        val monthMatches = mutableListOf<Pair<IntRange, java.time.YearMonth>>()
        Regex("""\b(20\d{2})-(\d{2})\b""").findAll(lower).forEach { m ->
            val y = m.groupValues[1].toIntOrNull() ?: return@forEach
            val mo = m.groupValues[2].toIntOrNull() ?: return@forEach
            if (mo in 1..12) {
                monthMatches.add(m.range to java.time.YearMonth.of(y, mo))
            }
        }
        MONTH_NAME_REGEX.findAll(lower).forEach { m ->
            val mo = monthNumberFromName(m.value) ?: return@forEach
            val hasYearAttached = monthMatches.any { it.first.contains(m.range.first) }
            if (!hasYearAttached) {
                monthMatches.add(m.range to java.time.YearMonth.of(activeYear, mo))
            }
        }

        val hasIn = Regex("""\b(pemasukan|income|duit masuk|masuk)\b""").containsMatchIn(lower)
        val hasOut = Regex("""\b(pengeluaran|expense|duit keluar|keluar)\b""").containsMatchIn(lower)
        val direction = when {
            hasIn && !hasOut -> "in"
            hasOut && !hasIn -> "out"
            else -> ""
        }

        if (monthMatches.isEmpty() && direction.isEmpty()) {
            return CuciSearchFilter(query = trimmed, isStructured = false)
        }

        val sortedMonths = monthMatches.sortedBy { it.first.first }.map { it.second }
        val (month, fromDate, toDate) = when {
            sortedMonths.size >= 2 -> {
                val minYm = sortedMonths.minOrNull()!!
                val maxYm = sortedMonths.maxOrNull()!!
                Triple("", minYm.atDay(1).toString(), maxYm.atEndOfMonth().toString())
            }
            sortedMonths.size == 1 -> Triple(sortedMonths.first().toString(), "", "")
            else -> Triple("", "", "")
        }

        val leftover = lower
            .replace(Regex("""\b20\d{2}-\d{2}\b"""), " ")
            .replace(MONTH_NAME_REGEX, " ")
            .replace(Regex("""\b20\d{2}\b"""), " ")
            .replace(Regex("""\b(pemasukan|pengeluaran|income|expense|masuk|keluar|bulan|dari|sampai|hingga|ke|s/d|sd|transaksi|uang|data|semua|total|berapa)\b"""), " ")
            .replace(Regex("""[-–—.,;|]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        return CuciSearchFilter(
            month = month,
            fromDate = fromDate,
            toDate = toDate,
            direction = direction,
            query = leftover,
            isStructured = true
        )
    }

    private fun splitCuciRangeValue(v: String): Pair<String, String>? {
        val m = Regex("""^(.+?)\s*(?:\.\.|sampai|hingga|s/d|sd|\u2013|\u2014)\s*(.+)$""", RegexOption.IGNORE_CASE).matchEntire(v.trim())
            ?: Regex("""^(20\d{2}-\d{2}|[a-z]+(?:\s+20\d{2})?)\s*-\s*(20\d{2}-\d{2}|[a-z]+(?:\s+20\d{2})?)$""", RegexOption.IGNORE_CASE).matchEntire(v.trim())
            ?: return null
        return m.groupValues[1].trim() to m.groupValues[2].trim()
    }

    private fun normalizeCuciRangeBound(raw: String, isEnd: Boolean, defaultYear: Int): String {
        val exact = normalizeCuciDate(raw)
        if (exact.isNotEmpty()) return exact
        val ym = resolveYearMonthToken(raw, defaultYear) ?: return ""
        return if (isEnd) ym.atEndOfMonth().toString() else ym.atDay(1).toString()
    }

    private fun resolveYearMonthToken(raw: String, defaultYear: Int): java.time.YearMonth? {
        val clean = raw.trim().lowercase()
        val ymMatch = Regex("""^(20\d{2})-(\d{1,2})$""").matchEntire(clean)
        if (ymMatch != null) {
            val y = ymMatch.groupValues[1].toIntOrNull() ?: return null
            val m = ymMatch.groupValues[2].toIntOrNull() ?: return null
            return if (m in 1..12) java.time.YearMonth.of(y, m) else null
        }
        val nameMatch = MONTH_NAME_REGEX.find(clean) ?: return null
        val mo = monthNumberFromName(nameMatch.value) ?: return null
        val y = Regex("""\b(20\d{2})\b""").find(clean)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: defaultYear
        return java.time.YearMonth.of(y, mo)
    }

    private val MONTH_NAME_REGEX = Regex(
        """\b(januari|jan|februari|feb|maret|mar|april|apr|mei|may|juni|jun|juli|jul|agustus|agu|ags|aug|september|sept|sep|oktober|okt|oct|november|nov|desember|des|dec)\b""",
        RegexOption.IGNORE_CASE
    )

    private fun monthNumberFromName(name: String): Int? = when (name.trim().lowercase()) {
        "januari", "jan" -> 1
        "februari", "feb" -> 2
        "maret", "mar" -> 3
        "april", "apr" -> 4
        "mei", "may" -> 5
        "juni", "jun" -> 6
        "juli", "jul" -> 7
        "agustus", "agu", "ags", "aug" -> 8
        "september", "sept", "sep" -> 9
        "oktober", "okt", "oct" -> 10
        "november", "nov" -> 11
        "desember", "des", "dec" -> 12
        else -> null
    }

    data class IdekuSearchFilter(
        val type: String = "",     // "Idea" | "Wishlist" | ""
        val status: String = "",   // "Belum" | "Proses" | "Parkir" | "Selesai" | ""
        val priority: String = "", // "Penting" | "Nanti" | "Kapan-kapan" | ""
        val category: String = "",
        val query: String = ""
    )

    fun parseIdekuSearchQuery(rawQuery: String): IdekuSearchFilter {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return IdekuSearchFilter()

        var type = ""
        var status = ""
        var priority = ""
        var category = ""
        val freeQueries = mutableListOf<String>()

        val keyRegex = Regex("""(?:^|[;|\s])(type|tipe|jenis|status|pri|priority|prioritas|cat|kategori|q|query)=""", RegexOption.IGNORE_CASE)
        if (keyRegex.containsMatchIn(trimmed)) {
            val segments = trimmed.split(Regex("""[;|]""")).map { it.trim() }.filter { it.isNotEmpty() }
            for (seg in segments) {
                val eqIdx = seg.indexOf('=')
                if (eqIdx <= 0) {
                    freeQueries.add(seg)
                    continue
                }
                val k = seg.substring(0, eqIdx).trim().lowercase()
                val v = seg.substring(eqIdx + 1).trim()
                if (v.isEmpty()) continue
                when (k) {
                    "type", "tipe", "jenis" -> {
                        val vl = v.lowercase()
                        type = when {
                            vl in setOf("idea", "ide") -> "Idea"
                            vl in setOf("wishlist", "wish", "barang") -> "Wishlist"
                            else -> ""
                        }
                    }
                    "status" -> status = normalizeIdekuStatus(v).orEmpty()
                    "pri", "priority", "prioritas" -> {
                        val vl = v.lowercase()
                        priority = when {
                            vl in setOf("penting", "tinggi", "urgent", "high") -> "Penting"
                            vl in setOf("nanti", "sedang", "medium") -> "Nanti"
                            vl in setOf("kapan-kapan", "kapan2", "rendah", "low") -> "Kapan-kapan"
                            else -> ""
                        }
                    }
                    "cat", "kategori" -> category = v
                    "q", "query" -> freeQueries.add(v)
                    else -> freeQueries.add(seg)
                }
            }
            return IdekuSearchFilter(
                type = type,
                status = status,
                priority = priority,
                category = category,
                query = freeQueries.joinToString(" ").trim()
            )
        }

        val tokens = trimmed.split(Regex("""\s+""")).filter { it.isNotBlank() }
        for (tok in tokens) {
            val tl = tok.lowercase().trim(',', '.', ';', ':')
            when {
                tl in setOf("daftar", "list", "semua", "all", "yang", "masih", "status") -> Unit
                type.isEmpty() && tl in setOf("idea", "ide", "ideku") -> type = "Idea"
                type.isEmpty() && tl in setOf("wishlist", "wish") -> type = "Wishlist"
                status.isEmpty() && tl in setOf("belum", "proses", "parkir", "selesai") -> {
                    status = normalizeIdekuStatus(tl).orEmpty()
                }
                priority.isEmpty() && tl in setOf("penting", "nanti", "kapan-kapan", "kapan2") -> {
                    priority = normalizeIdekuPriority(tl)
                }
                else -> freeQueries.add(tok)
            }
        }
        return IdekuSearchFilter(
            type = type,
            status = status,
            priority = priority,
            category = category,
            query = freeQueries.joinToString(" ").trim()
        )
    }

    private fun parseDevicePayload(payload: String): DeviceChatAction? {
        val parts = payload.split("|").map { it.trim() }
        if (parts.isEmpty()) return null
        return when (val cmd = parts[0].uppercase()) {
            "OPEN_URL", "URL", "BROWSE" -> {
                val raw = parts.getOrNull(1).orEmpty().trim()
                if (raw.isBlank()) return null
                val hasScheme = Regex("""^[a-zA-Z][a-zA-Z0-9+.-]*:""").containsMatchIn(raw)
                val url = when {
                    raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true) -> raw
                    hasScheme -> return null // reject javascript:, file:, intent:, content:, etc.
                    else -> "https://$raw"
                }
                DeviceChatAction.OpenUrl(url = url)
            }
            "SHARE", "SHARE_TEXT", "BAGIKAN" -> {
                val txt = parts.drop(1).joinToString(" | ").trim()
                if (txt.isBlank()) null else DeviceChatAction.ShareText(text = txt)
            }
            "ALARM", "SET_ALARM" -> {
                val hm = parseHourMinute(parts.getOrNull(1).orEmpty()) ?: return null
                val label = parts.getOrNull(2).orEmpty().trim()
                DeviceChatAction.SetAlarm(hour = hm.first, minute = hm.second, label = label)
            }
            "TIMER", "SET_TIMER", "POMODORO" -> {
                val secs = parseTimerSeconds(parts.getOrNull(1).orEmpty()) ?: return null
                val defaultLbl = if (cmd == "POMODORO") "Pomodoro" else ""
                val label = parts.getOrNull(2).orEmpty().trim().ifBlank { defaultLbl }
                DeviceChatAction.SetTimer(seconds = secs, label = label)
            }
            "REMINDER", "SET_REMINDER", "INGATKAN" -> {
                // Supports:
                // [DEVICE:REMINDER|YYYY-MM-DD_or_hari_ini|HH:mm|pesan]
                // [DEVICE:REMINDER|HH:mm|pesan]
                val p1 = parts.getOrNull(1).orEmpty().trim()
                val p2 = parts.getOrNull(2).orEmpty().trim()
                val hm1 = parseHourMinute(p1)
                if (hm1 != null) {
                    val msg = parts.drop(2).joinToString(" ").trim().ifBlank { "Pengingat dari Rem" }
                    DeviceChatAction.SetReminder(
                        dateStr = java.time.LocalDate.now().toString(),
                        hour = hm1.first,
                        minute = hm1.second,
                        message = msg
                    )
                } else {
                    val hm2 = parseHourMinute(p2) ?: return null
                    val dateStr = normalizeDeviceDate(p1).ifBlank { java.time.LocalDate.now().toString() }
                    val msg = parts.drop(3).joinToString(" ").trim().ifBlank { "Pengingat dari Rem" }
                    DeviceChatAction.SetReminder(
                        dateStr = dateStr,
                        hour = hm2.first,
                        minute = hm2.second,
                        message = msg
                    )
                }
            }
            "DEL_REMINDER", "CANCEL_REMINDER", "HAPUS_REMINDER" -> {
                val target = parts.getOrNull(1).orEmpty().trim()
                if (target.isBlank()) null else DeviceChatAction.CancelReminder(target = target)
            }
            "CAL_ADD", "CALENDAR_ADD", "ADD_EVENT" -> {
                // [DEVICE:CAL_ADD|judul|YYYY-MM-DD|HH:mm_mulai|HH:mm_selesai_opsional|lokasi_opsional|catatan_opsional]
                val title = parts.getOrNull(1).orEmpty().trim()
                if (title.isBlank()) return null
                val dateStr = normalizeDeviceDate(parts.getOrNull(2).orEmpty())
                    .ifBlank { java.time.LocalDate.now().toString() }
                val startHm = parseHourMinute(parts.getOrNull(3).orEmpty()) ?: (9 to 0)
                val endHm = parseHourMinute(parts.getOrNull(4).orEmpty())
                val location = parts.getOrNull(5).orEmpty().trim()
                val description = parts.getOrNull(6).orEmpty().trim()
                DeviceChatAction.AddCalendarEvent(
                    title = title,
                    dateStr = dateStr,
                    startHour = startHm.first,
                    startMinute = startHm.second,
                    endHour = endHm?.first,
                    endMinute = endHm?.second,
                    location = location,
                    description = description
                )
            }
            else -> null
        }
    }

    private fun parseHourMinute(raw: String): Pair<Int, Int>? {
        val m = Regex("""^(\d{1,2})[:.](\d{2})$""").matchEntire(raw.trim()) ?: return null
        val h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || min !in 0..59) return null
        return h to min
    }

    private fun parseTimerSeconds(raw: String): Int? {
        val clean = raw.trim().lowercase()
        if (clean.isEmpty()) return null
        val m = Regex("""^(\d+)\s*(s|sec|detik|m|min|menit|h|jam)?$""").matchEntire(clean) ?: return null
        val num = m.groupValues[1].toIntOrNull() ?: return null
        if (num <= 0) return null
        val unit = m.groupValues[2]
        val secs = when (unit) {
            "s", "sec", "detik" -> num
            "m", "min", "menit" -> num * 60
            "h", "jam" -> num * 3600
            else -> if (num <= 180) num * 60 else num
        }
        return secs.coerceIn(1, 86_400)
    }

    private fun normalizeDeviceDate(raw: String): String {
        val clean = raw.trim().lowercase()
        if (clean.isEmpty()) return ""
        return when (clean) {
            "hari ini", "today" -> java.time.LocalDate.now().toString()
            "besok", "tomorrow" -> java.time.LocalDate.now().plusDays(1).toString()
            "lusa" -> java.time.LocalDate.now().plusDays(2).toString()
            else -> normalizeCuciDate(raw)
        }
    }

    /**
     * Memastikan tag [OPEN:app] dari LLM hanya dieksekusi bila pesan pengguna benar-benar
     * berisi perintah eksplisit untuk membuka/berpindah ke layar sub-app terkait.
     */
    fun shouldAllowOpenApp(userMessage: String, appKey: String): Boolean {
        val cleanApp = appKey.trim().lowercase()
        val msg = userMessage.trim().lowercase()
        if (cleanApp.isEmpty() || msg.isEmpty()) return false

        val negationRegex = Regex(
            """\b(jangan|jgn|tidak|tak|nggak|gak|gk|usah|tanpa|bukan)\s+(?:perlu\s+|usah\s+)?(buka|bukakan|bukain|open|launch|masuk|pindah|pergi|tampilkan|tampilin|lihat)\b"""
        )
        if (negationRegex.containsMatchIn(msg)) return false

        val openIntentRegex = Regex(
            """\b(buka|bukakan|bukain|open|launch)\b|\b(masuk|pindah|pergi|arahkan)\s+ke\b|\b(tampilkan|tampilin|lihat|ke)\s+(halaman|layar|menu|tab|aplikasi|app|modul)\b"""
        )
        if (!openIntentRegex.containsMatchIn(msg)) return false

        val appTerms: List<String> = when (cleanApp) {
            "kronik" -> listOf("kronik", "jurnal", "diary", "catatan harian")
            "linkku" -> listOf("linkku", "link ku", "bookmark", "tautan", "daftar link")
            "cucimoney" -> listOf("cucimoney", "cuci money", "cuci", "dompet", "keuangan", "buku kas")
            "ideku" -> listOf("ideku", "ide ku", "wishlist", "daftar ide", "ide")
            "dailyos" -> listOf("dailyos", "daily os", "habit", "todo", "to-do", "daftar tugas")
            else -> return false
        }
        return appTerms.any { term ->
            Regex("""\b${Regex.escape(term)}\b""").containsMatchIn(msg)
        }
    }

    private val NEGATED_OR_OFFER_ACTION_REGEX = Regex(
        """\b(?:belum|gagal|tidak|tak|nggak|gak|gk|jangan|mau|ingin|perlu|boleh|akan)\s+(?:rem\s+|ku\s+)?(?:catat|mencatat|dicatat|tercatat|simpan|menyimpan|disimpan|tersimpan|masukkan|memasukkan|dimasukkan|tambahkan|menambahkan|ditambahkan|ubah|mengubah|diubah|perbarui|memperbarui|diperbarui|hapus|menghapus|dihapus|centang|mencentang|dicentang|arsipkan|mengarsipkan|diarsipkan|setor|menyetor|disetor|pasang|memasang|dipasang)\b"""
    )

    private val CLAIMED_ACTION_REGEX = Regex(
        """\b(?:sudah|udah|telah|berhasil)\s+(?:rem\s+)?(?:catat|catatkan|mencatat|dicatat|tercatat|simpan|simpankan|menyimpan|disimpan|tersimpan|masukkan|memasukkan|dimasukkan|tambahkan|menambahkan|ditambahkan|perbarui|memperbarui|diperbarui|update|diupdate|ubah|mengubah|diubah|edit|diedit|hapus|menghapus|dihapus|centang|mencentang|dicentang|tandai\s+selesai|ditandai\s+selesai|arsipkan|mengarsipkan|diarsipkan|setor|menyetor|disetor|tabung|menabung|ditabung|realisasikan|merealisasikan|direalisasikan|pasang|memasang|dipasang|jadwalkan|menjadwalkan|dijadwalkan)\b|\b(?:sudah|udah|telah)\s+masuk\s+ke\s+(?:buku|akun|jurnal|kronik|linkku|ideku|daily\s*os|cucimoney|wishlist|kalender)\b"""
    )

    /**
     * Mendeteksi apakah teks respons asisten mengklaim sudah mencatat/menyimpan/mengubah data.
     */
    fun claimsActionPerformed(cleanText: String): Boolean {
        val lower = cleanText.trim().lowercase()
        if (lower.isEmpty()) return false
        val stripped = lower.replace(NEGATED_OR_OFFER_ACTION_REGEX, " ")
        return CLAIMED_ACTION_REGEX.containsMatchIn(stripped)
    }

    private val PENDING_SEARCH_REGEX = Regex(
        """\b(?:bentar|sebentar|tunggu|tunggu\s+sebentar|tunggu\s+dulu)\b.*\b(?:rem\s+)?(?:cari|carikan|mencari|cek|ngecek|periksa|memeriksa|lihat|melihat)\b|\b(?:rem|aku|saya)\s+(?:cari|carikan|cek|periksa|lihat)\s+dulu\b|\bsedang\s+(?:rem\s+)?(?:cari|mencari|cek|mengecek|memeriksa)\b"""
    )

    /**
     * Mendeteksi respons menggantung seperti "Bentar Tuan, Rem cari dulu" tanpa hasil/tag [SEARCH:...].
     */
    fun claimsPendingSearch(cleanText: String): Boolean {
        val lower = cleanText.trim().lowercase()
        if (lower.isEmpty() || lower.length > 220) return false
        if (lower.contains("rp") || Regex("""\b\d{3,}\b""").containsMatchIn(lower)) return false
        return PENDING_SEARCH_REGEX.containsMatchIn(lower)
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
