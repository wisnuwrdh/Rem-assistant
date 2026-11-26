package com.ecosystemku.assistant.data.backup

import android.content.Context
import com.ecosystemku.assistant.data.kg.KgBackupWrapperDto
import com.ecosystemku.assistant.data.kg.KnowledgeGraphHelpers
import com.ecosystemku.assistant.data.local.AssistantDatabase
import com.ecosystemku.assistant.data.local.ChatMessageEntity
import com.ecosystemku.assistant.data.local.ChatSessionEntity
import com.ecosystemku.assistant.data.local.ConversationSummaryEntity
import com.ecosystemku.assistant.data.local.CuciAccountEntity
import com.ecosystemku.assistant.data.local.CuciBookEntity
import com.ecosystemku.assistant.data.local.CuciCategoryEntity
import com.ecosystemku.assistant.data.local.CuciMasterBudgetEntity
import com.ecosystemku.assistant.data.local.CuciMemberEntity
import com.ecosystemku.assistant.data.local.CuciTransactionEntity
import com.ecosystemku.assistant.data.local.CuciWishlistEntity
import com.ecosystemku.assistant.data.local.IdekuIdeaEntity
import com.ecosystemku.assistant.data.local.KgEdgeEntity
import com.ecosystemku.assistant.data.local.KgNodeEntity
import com.ecosystemku.assistant.data.local.LinkkuBackupWrapperDto
import com.ecosystemku.assistant.data.local.LinkkuCategoryEntity
import com.ecosystemku.assistant.data.local.LinkkuEncryptedPrivatePayloadDto
import com.ecosystemku.assistant.data.network.DailyOsCoachMemoryDto
import com.ecosystemku.assistant.ui.apps.cucimoney.CuciMoneyHelpers
import com.ecosystemku.assistant.ui.apps.cucimoney.CuciMoneyUiState
import com.ecosystemku.assistant.ui.apps.dailyos.DailyOsHelpers
import com.ecosystemku.assistant.ui.apps.ideku.IdekuHelpers
import com.ecosystemku.assistant.ui.apps.kronik.KronikHelpers
import com.ecosystemku.assistant.ui.apps.linkku.LinkkuCrypto
import com.ecosystemku.assistant.ui.apps.linkku.LinkkuHelpers
import com.ecosystemku.assistant.ui.apps.linkku.LinkkuHelpers.toEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
private data class EcosystemManifestDto(
    val version: Int = 3,
    val app: String = "asisten-rem",
    val exportedAt: String = "",
    val profileName: String = "Mahfud"
)

@Serializable
private data class AsistenBackupChatDto(
    val role: String,
    val content: String,
    val expressionTag: String? = null,
    val createdAt: Long = 0L,
    val choicesJson: String? = null,
    val sessionId: Long = 1L
)

@Serializable
private data class AsistenBackupSessionDto(
    val id: Long = 0L,
    val title: String = "Obrolan Baru",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val rollingSummary: String? = null,
    val summarizedCount: Int = 0
)

@Serializable
private data class AsistenBackupMemoryDto(
    val category: String,
    val factKey: String,
    val factValue: String,
    val confidence: Float = 0.8f,
    val status: String = "active",
    val createdAt: Long = 0L,
    val deprecatedAt: Long? = null,
    val supersededBy: String? = null
)

@Serializable
private data class AsistenBackupSummaryDto(
    val summaryText: String,
    val turnCount: Int = 0,
    val createdAt: Long = 0L
)

@Serializable
private data class AsistenBackupWrapperDto(
    val profileName: String = "Mahfud",
    val autoLearnMemory: Boolean = true,
    val messages: List<AsistenBackupChatDto> = emptyList(),
    val sessions: List<AsistenBackupSessionDto> = emptyList(),
    val memories: List<AsistenBackupMemoryDto> = emptyList(),
    val summaries: List<AsistenBackupSummaryDto> = emptyList(),
    val memoryProfileJson: String? = null
)

sealed interface EcosystemZipImportResult {
    data class Success(val summary: String, val latestSessionId: Long? = null) : EcosystemZipImportResult
    data class NeedsPin(val zipBytes: ByteArray, val replaceAll: Boolean) : EcosystemZipImportResult
    data class Error(val message: String) : EcosystemZipImportResult
}

/**
 * Mengelola ekspor dan impor arsip Ecosystem ZIP v3 secara mandiri dari Room SQLite & SharedPreferences,
 * sehingga dapat dipanggil oleh UI maupun WorkManager di latar belakang.
 */
class EcosystemBackupRepository(private val appContext: Context) {

    private val db = AssistantDatabase.getInstance(appContext)
    private val chatDao = db.chatDao()
    private val userMemoryDao = db.userMemoryDao()
    private val kronikDao = db.kronikDao()
    private val linkkuDao = db.linkkuDao()
    private val cuciMoneyDao = db.cuciMoneyDao()
    private val idekuDao = db.idekuDao()
    private val dailyOsDao = db.dailyOsDao()
    private val kgDao = db.knowledgeGraphDao()

    private val asistenPrefs = appContext.getSharedPreferences("asisten_prefs", Context.MODE_PRIVATE)
    private val linkkuPrefs = appContext.getSharedPreferences("linkku_prefs", Context.MODE_PRIVATE)
    private val cuciPrefs = appContext.getSharedPreferences("cucimoney_prefs", Context.MODE_PRIVATE)
    private val dailyOsPrefs = appContext.getSharedPreferences("dailyos_prefs", Context.MODE_PRIVATE)

    suspend fun buildEcosystemZipBytes(updateLocalStamp: Boolean = true): ByteArray = withContext(Dispatchers.IO) {
        val nowIso = Instant.now().toString()
        val profileName = asistenPrefs.getString("asisten_profile_name", "Mahfud")
            ?.trim()
            ?.ifBlank { "Mahfud" }
            ?: "Mahfud"
        val autoLearnMemory = asistenPrefs.getBoolean("asisten_auto_learn", true)

        val manifestJson = DailyOsHelpers.json.encodeToString(
            EcosystemManifestDto(
                version = 3,
                app = "asisten-rem",
                exportedAt = nowIso,
                profileName = profileName
            )
        )

        val coachMemRaw = dailyOsPrefs.getString("do_coach_memory", null)
        val coachMemory = if (!coachMemRaw.isNullOrBlank()) {
            runCatching { DailyOsHelpers.json.decodeFromString<DailyOsCoachMemoryDto>(coachMemRaw) }.getOrNull()
        } else null
        val dailyJson = DailyOsHelpers.buildBackupJson(
            habits = dailyOsDao.getAllHabits(),
            todos = dailyOsDao.getAllTodos(),
            coachMemory = coachMemory
        )

        val idekuJson = IdekuHelpers.buildExportJson(idekuDao.getAll())
        val kronikJson = KronikHelpers.buildExportJson(kronikDao.getAllEntries())

        val cuciLastAccRaw = cuciPrefs.getString("cm_last_acc", "{}").orEmpty()
        val cuciLastAcc = runCatching {
            CuciMoneyHelpers.json.decodeFromString<Map<Long, Long>>(cuciLastAccRaw)
        }.getOrDefault(emptyMap())
        val cuciState = CuciMoneyUiState(
            books = cuciMoneyDao.getAllBooks(),
            accounts = cuciMoneyDao.getAllAccounts(),
            categories = cuciMoneyDao.getAllCategories(),
            members = cuciMoneyDao.getAllMembers(),
            transactions = cuciMoneyDao.getAllTransactions(),
            masterBudgets = cuciMoneyDao.getAllMasterBudgets(),
            wishlists = cuciMoneyDao.getAllWishlists(),
            currentBookId = cuciPrefs.getLong("cm_book_id", 1L),
            visibility = cuciPrefs.getBoolean("cm_vis", true),
            budgetCollapsed = cuciPrefs.getBoolean("cm_budget_collapsed", false),
            lastAccByBook = cuciLastAcc
        )
        val cuciJson = CuciMoneyHelpers.buildBackupJson(cuciState)

        val linkkuJson = LinkkuHelpers.buildExportJson(
            links = linkkuDao.getAllLinks(),
            categories = linkkuDao.getAllCategories(),
            pinHash = linkkuPrefs.getString("lk_pin", null)
        )

        val asistenWrapper = AsistenBackupWrapperDto(
            profileName = profileName,
            autoLearnMemory = autoLearnMemory,
            sessions = chatDao.getAllSessions().map {
                AsistenBackupSessionDto(it.id, it.title, it.createdAt, it.updatedAt, it.rollingSummary, it.summarizedCount)
            },
            messages = chatDao.getAll().map {
                AsistenBackupChatDto(it.role, it.content, it.expressionTag, it.createdAt, it.choicesJson, it.sessionId)
            },
            memories = userMemoryDao.getAllMemories().map {
                AsistenBackupMemoryDto(
                    category = it.category,
                    factKey = it.factKey,
                    factValue = it.factValue,
                    confidence = it.confidence,
                    status = it.status,
                    createdAt = it.createdAt,
                    deprecatedAt = it.deprecatedAt,
                    supersededBy = it.supersededBy
                )
            },
            summaries = userMemoryDao.getAllSummaries().map {
                AsistenBackupSummaryDto(it.summaryText, it.turnCount, it.createdAt)
            },
            memoryProfileJson = asistenPrefs.getString("asisten_memory_profile_json", null)
        )
        val asistenJson = DailyOsHelpers.json.encodeToString(asistenWrapper)
        val kgJson = DailyOsHelpers.json.encodeToString(
            KnowledgeGraphHelpers.buildBackupDto(kgDao.getAllNodes(), kgDao.getAllEdges())
        )

        val entries = mapOf(
            "manifest.json" to manifestJson,
            "dailyos.json" to dailyJson,
            "ideku.json" to idekuJson,
            "kronik.json" to kronikJson,
            "cucimoney.json" to cuciJson,
            "linkku.json" to linkkuJson,
            "asisten.json" to asistenJson,
            "knowledge_graph.json" to kgJson
        )

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (filename, content) ->
                zos.putNextEntry(ZipEntry(filename))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        if (updateLocalStamp) {
            val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID")).format(Date())
            asistenPrefs.edit().putString("asisten_last_backup", stamp).apply()
        }
        baos.toByteArray()
    }

    suspend fun importEcosystemZipBytes(
        zipBytes: ByteArray,
        replaceAll: Boolean,
        linkkuPin: String? = null
    ): EcosystemZipImportResult = withContext(Dispatchers.IO) {
        runCatching {
            val files = mutableMapOf<String, String>()
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.substringAfterLast('/')
                        files[name] = zis.readBytes().toString(Charsets.UTF_8)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            if (files.isEmpty()) {
                return@withContext EcosystemZipImportResult.Error("File ZIP kosong atau tidak valid")
            }

            // Cek apakah LinkKu memiliki tautan privat terenkripsi dan PIN belum diberikan
            val linkkuRaw = files["linkku.json"]
            if (!linkkuRaw.isNullOrBlank() && linkkuPin == null) {
                val lkWrapper = runCatching {
                    LinkkuHelpers.json.decodeFromString<LinkkuBackupWrapperDto>(linkkuRaw)
                }.getOrNull()
                if (lkWrapper != null && !lkWrapper.ep.isNullOrBlank()) {
                    return@withContext EcosystemZipImportResult.NeedsPin(zipBytes, replaceAll)
                }
            }

            var restoredModules = 0
            var latestSessionId: Long? = null

            files["dailyos.json"]?.let { raw ->
                runCatching {
                    val (habits, todos, mem) = DailyOsHelpers.parseBackupJson(raw)
                    if (replaceAll) {
                        dailyOsDao.replaceAllData(habits, todos)
                    } else {
                        val existingHabits = dailyOsDao.getAllHabits().map { it.id }.toSet()
                        val newHabits = habits.filter { it.id !in existingHabits }
                        if (newHabits.isNotEmpty()) dailyOsDao.upsertHabits(newHabits)
                        val existingTodos = dailyOsDao.getAllTodos().map { it.id }.toSet()
                        val newTodos = todos.filter { it.id !in existingTodos }
                        if (newTodos.isNotEmpty()) dailyOsDao.upsertTodos(newTodos)
                    }
                    val existingCoach = dailyOsPrefs.getString("do_coach_memory", null)
                    if (mem != null && (replaceAll || existingCoach.isNullOrBlank())) {
                        dailyOsPrefs.edit()
                            .putString("do_coach_memory", DailyOsHelpers.json.encodeToString(mem))
                            .putString("do_memory_date", DailyOsHelpers.todayStr())
                            .apply()
                    }
                    restoredModules++
                }
            }

            files["ideku.json"]?.let { raw ->
                runCatching {
                    val imported = IdekuHelpers.parseImportJson(raw)
                    if (replaceAll) idekuDao.clearAll()
                    val existingIds = if (replaceAll) emptySet() else idekuDao.getAll().map { it.id }.toSet()
                    val fresh = imported.filter { it.id !in existingIds }
                    if (fresh.isNotEmpty()) {
                        idekuDao.upsertAll(fresh)
                        fresh.filter { it.type == "Wishlist" }.forEach { syncIdekuWishlistToCuciMoney(it) }
                    }
                    restoredModules++
                }
            }

            files["kronik.json"]?.let { raw ->
                runCatching {
                    val imported = KronikHelpers.parseImportEntries(raw)
                    if (replaceAll) kronikDao.clearAll()
                    val existingIds = if (replaceAll) emptySet() else kronikDao.getAllEntries().map { it.id }.toSet()
                    val fresh = imported.filter { it.id !in existingIds }
                    if (fresh.isNotEmpty()) kronikDao.upsertAll(fresh)
                    restoredModules++
                }
            }

            files["cucimoney.json"]?.let { raw ->
                runCatching {
                    val wrapper = CuciMoneyHelpers.parseBackupJson(raw)
                    val books = wrapper.db.books.map {
                        CuciBookEntity(it.id, it.name, CuciMoneyHelpers.encodeAccountIds(it.accountIds))
                    }.ifEmpty { CuciMoneyHelpers.DEFAULT_BOOKS }
                    val accounts = wrapper.db.accounts.map {
                        CuciAccountEntity(it.id, it.name, it.type, it.icon, it.balance, it.notes, it.useInBook, it.includeAsset, it.isSavings)
                    }.ifEmpty { CuciMoneyHelpers.DEFAULT_ACCOUNTS }
                    val categories = wrapper.db.categories.mapIndexed { idx, c ->
                        CuciCategoryEntity(c.id, c.name, c.type, c.emoji, idx)
                    }.ifEmpty { CuciMoneyHelpers.DEFAULT_CATEGORIES }
                    val members = wrapper.db.members.mapIndexed { idx, m ->
                        CuciMemberEntity(m.id, m.name, m.emoji, idx)
                    }.ifEmpty { CuciMoneyHelpers.DEFAULT_MEMBERS }
                    val transactions = wrapper.db.transactions.map {
                        CuciTransactionEntity(it.id, it.bookId, it.title, it.amount, it.direction, it.categoryId, it.accountId, it.date, it.memberId, it.transferPair, it.savingsPot, it.createdAt)
                    }
                    val budgets = wrapper.masterBudgets.map {
                        CuciMasterBudgetEntity(it.id, it.bookId, it.month, it.year, it.limit, it.startDate, it.endDate, it.note)
                    }
                    val wishlists = wrapper.savings.pots.values.map { p ->
                        CuciWishlistEntity(
                            id = p.wishId.ifBlank { "wish_${CuciMoneyHelpers.generateId()}" },
                            title = p.snap?.title ?: "Wishlist",
                            price = p.snap?.price ?: 0L,
                            priority = "Nanti",
                            image = p.snap?.image ?: "bag",
                            status = if (p.realizedAt != null) "Selesai" else "Belum",
                            saved = p.saved,
                            realizedAt = p.realizedAt,
                            realizedAmount = p.realizedAmount,
                            historyJson = CuciMoneyHelpers.encodeWishlistHistory(p.history)
                        )
                    }
                    if (replaceAll) {
                        cuciMoneyDao.replaceAllData(books, accounts, categories, members, transactions, budgets, wishlists)
                        cuciPrefs.edit().putLong("cm_book_id", books.first().id).apply()
                    } else {
                        val existingBookIds = cuciMoneyDao.getAllBooks().map { it.id }.toSet()
                        books.filter { it.id !in existingBookIds }.forEach { cuciMoneyDao.upsertBook(it) }
                        val existingAccIds = cuciMoneyDao.getAllAccounts().map { it.id }.toSet()
                        accounts.filter { it.id !in existingAccIds }.forEach { cuciMoneyDao.upsertAccount(it) }
                        val existingCatIds = cuciMoneyDao.getAllCategories().map { it.id }.toSet()
                        val newCats = categories.filter { it.id !in existingCatIds }
                        if (newCats.isNotEmpty()) cuciMoneyDao.upsertCategories(newCats)
                        val existingTrxIds = cuciMoneyDao.getAllTransactions().map { it.id }.toSet()
                        val newTrxs = transactions.filter { it.id !in existingTrxIds }
                        if (newTrxs.isNotEmpty()) cuciMoneyDao.upsertTransactions(newTrxs)
                        val existingWishIds = cuciMoneyDao.getAllWishlists().map { it.id }.toSet()
                        wishlists.filter { it.id !in existingWishIds }.forEach { cuciMoneyDao.upsertWishlist(it) }
                    }
                    restoredModules++
                }
            }

            if (!linkkuRaw.isNullOrBlank()) {
                val wrapper = runCatching {
                    LinkkuHelpers.json.decodeFromString<LinkkuBackupWrapperDto>(linkkuRaw)
                }.getOrNull()
                if (wrapper != null) {
                    if (replaceAll) {
                        linkkuDao.clearAllLinks()
                        linkkuDao.clearAllCategories()
                    }
                    val pubDtos = wrapper.L.ifEmpty { wrapper.links }
                    var incomingLinks = pubDtos.map { it.toEntity() }
                    var incomingPrivCats = wrapper.PC.map {
                        LinkkuCategoryEntity(id = it.id, name = it.n, isPrivate = true)
                    }
                    if (!wrapper.ep.isNullOrBlank() && !linkkuPin.isNullOrBlank()) {
                        val pinHash = LinkkuCrypto.hashPin(linkkuPin)
                        val decJson = runCatching {
                            LinkkuCrypto.decryptPrivatePayload(wrapper.ep, pinHash)
                        }.getOrElse {
                            return@withContext EcosystemZipImportResult.Error("PIN LinkKu salah atau gagal membuka tautan privat")
                        }
                        val privPayload = LinkkuHelpers.json.decodeFromString<LinkkuEncryptedPrivatePayloadDto>(decJson)
                        incomingLinks = incomingLinks + privPayload.links.map { it.toEntity() }
                        incomingPrivCats = privPayload.cats.map {
                            LinkkuCategoryEntity(id = it.id, name = it.n, isPrivate = true)
                        }
                        linkkuPrefs.edit().putString("lk_pin", pinHash).apply()
                    }
                    val existingLinkIds = linkkuDao.getAllLinks().map { it.id }.toSet()
                    val newLinks = incomingLinks.filter { it.id !in existingLinkIds }
                    if (newLinks.isNotEmpty()) linkkuDao.upsertLinks(newLinks)
                    val existingCatIds = linkkuDao.getAllCategories().map { it.id }.toSet()
                    val pubCats = wrapper.C.ifEmpty { wrapper.cats }
                        .map { LinkkuCategoryEntity(id = it.id, name = it.n, isPrivate = false) }
                    val newCats = (pubCats + incomingPrivCats).filter { it.id.isNotBlank() && it.id !in existingCatIds }
                    if (newCats.isNotEmpty()) linkkuDao.upsertCategories(newCats)
                    restoredModules++
                }
            }

            files["asisten.json"]?.let { raw ->
                runCatching {
                    val wrapper = DailyOsHelpers.json.decodeFromString<AsistenBackupWrapperDto>(raw)
                    if (wrapper.profileName.isNotBlank()) {
                        asistenPrefs.edit().putString("asisten_profile_name", wrapper.profileName.trim()).apply()
                    }
                    asistenPrefs.edit().putBoolean("asisten_auto_learn", wrapper.autoLearnMemory).apply()
                    if (!wrapper.memoryProfileJson.isNullOrBlank()) {
                        asistenPrefs.edit().putString("asisten_memory_profile_json", wrapper.memoryProfileJson).apply()
                    } else if (replaceAll) {
                        asistenPrefs.edit().remove("asisten_memory_profile_json").apply()
                    }

                    if (replaceAll) {
                        chatDao.clearAll()
                        chatDao.clearAllSessions()
                        userMemoryDao.clearAll()
                        userMemoryDao.clearSummaries()
                    }
                    wrapper.memories.forEach { m ->
                        if (m.factKey.isNotBlank() && m.factValue.isNotBlank()) {
                            if (m.status != "active") {
                                if (userMemoryDao.getByFactKey(m.factKey) == null) {
                                    val tsNow = System.currentTimeMillis()
                                    userMemoryDao.insertMemory(
                                        com.ecosystemku.assistant.data.local.UserMemoryEntity(
                                            category = m.category,
                                            factKey = m.factKey,
                                            factValue = m.factValue,
                                            confidence = m.confidence,
                                            sourceMessageId = null,
                                            updatedAt = m.deprecatedAt ?: tsNow,
                                            status = m.status,
                                            createdAt = if (m.createdAt > 0L) m.createdAt else tsNow,
                                            deprecatedAt = m.deprecatedAt ?: tsNow,
                                            supersededBy = m.supersededBy
                                        )
                                    )
                                }
                            } else {
                                userMemoryDao.upsertByFactKey(m.category, m.factKey, m.factValue, m.confidence, null)
                            }
                        }
                    }
                    val existingSummaries = userMemoryDao.getAllSummaries().map { it.summaryText }.toSet()
                    wrapper.summaries.filter { it.summaryText !in existingSummaries }.forEach { s ->
                        userMemoryDao.insertSummary(
                            ConversationSummaryEntity(
                                summaryText = s.summaryText,
                                turnCount = s.turnCount,
                                createdAt = if (s.createdAt > 0L) s.createdAt else System.currentTimeMillis()
                            )
                        )
                    }
                    val existingSessions = chatDao.getAllSessions().associateBy { it.id }
                    val sessionIdMap = mutableMapOf<Long, Long>()
                    val nowTs = System.currentTimeMillis()
                    wrapper.sessions.forEach { s ->
                        val title = s.title.ifBlank { "Obrolan" }
                        val created = if (s.createdAt > 0L) s.createdAt else nowTs
                        val updated = if (s.updatedAt > 0L) s.updatedAt else created
                        val existing = existingSessions[s.id]
                        if (s.id > 0L && existing == null) {
                            chatDao.insertSession(
                                ChatSessionEntity(
                                    id = s.id,
                                    title = title,
                                    createdAt = created,
                                    updatedAt = updated,
                                    rollingSummary = s.rollingSummary,
                                    summarizedCount = s.summarizedCount
                                )
                            )
                            sessionIdMap[s.id] = s.id
                        } else if (existing != null && existing.title == title) {
                            sessionIdMap[s.id] = existing.id
                        } else {
                            val newId = chatDao.insertSession(
                                ChatSessionEntity(
                                    title = title,
                                    createdAt = created,
                                    updatedAt = updated,
                                    rollingSummary = s.rollingSummary,
                                    summarizedCount = s.summarizedCount
                                )
                            )
                            sessionIdMap[s.id] = newId
                        }
                    }
                    var fallbackSessionId = sessionIdMap.values.firstOrNull() ?: chatDao.getAllSessions().firstOrNull()?.id
                    if (fallbackSessionId == null && wrapper.messages.isNotEmpty()) {
                        val minTs = wrapper.messages.minOfOrNull { it.createdAt }?.takeIf { it > 0L } ?: nowTs
                        val maxTs = wrapper.messages.maxOfOrNull { it.createdAt }?.takeIf { it > 0L } ?: minTs
                        fallbackSessionId = chatDao.insertSession(
                            ChatSessionEntity(title = "Obrolan Sebelumnya", createdAt = minTs, updatedAt = maxTs)
                        )
                    }
                    val existingMsgKeys = chatDao.getAll().map { "${it.createdAt}_${it.role}" }.toSet()
                    val freshMsgs = wrapper.messages
                        .filter { "${it.createdAt}_${it.role}" !in existingMsgKeys }
                        .map {
                            val resolvedSid = sessionIdMap[it.sessionId] ?: fallbackSessionId ?: 1L
                            ChatMessageEntity(
                                sessionId = resolvedSid,
                                role = it.role,
                                content = it.content,
                                expressionTag = it.expressionTag,
                                createdAt = if (it.createdAt > 0L) it.createdAt else nowTs,
                                choicesJson = it.choicesJson
                            )
                        }
                    if (freshMsgs.isNotEmpty()) {
                        chatDao.insertAll(freshMsgs)
                    }
                    latestSessionId = chatDao.getAllSessions().firstOrNull()?.id
                    restoredModules++
                }
            }

            val kgRaw = files["knowledge_graph.json"]
            if (kgRaw != null) {
                runCatching {
                    val wrapper = DailyOsHelpers.json.decodeFromString<KgBackupWrapperDto>(kgRaw)
                    if (replaceAll) {
                        kgDao.clearAll()
                    }
                    val nowMs = System.currentTimeMillis()
                    val keyToId = kgDao.getAllNodes().associate { it.nodeKey to it.id }.toMutableMap()
                    wrapper.nodes.forEach { n ->
                        if (n.nodeKey.isBlank() || n.label.isBlank()) return@forEach
                        if (!keyToId.containsKey(n.nodeKey)) {
                            val firstAt = if (n.firstSeenAt > 0L) n.firstSeenAt else nowMs
                            val lastAt = if (n.lastSeenAt > 0L) n.lastSeenAt else nowMs
                            val newId = kgDao.insertNode(
                                KgNodeEntity(
                                    nodeKey = n.nodeKey,
                                    nodeType = KnowledgeGraphHelpers.normalizeNodeType(n.nodeType),
                                    label = n.label.take(120),
                                    summary = n.summary.take(300),
                                    salience = n.salience.coerceIn(0.1f, 1.0f),
                                    sourceType = n.sourceType.ifBlank { "chat" },
                                    firstSeenAt = firstAt,
                                    lastSeenAt = lastAt,
                                    status = n.status.ifBlank { "active" },
                                    confidence = n.confidence.coerceIn(0.3f, 1.0f),
                                    lastConfirmedAt = if (n.lastConfirmedAt > 0L) n.lastConfirmedAt else lastAt,
                                    sourceRef = n.sourceRef,
                                    validationState = n.validationState.ifBlank { "unverified" }
                                )
                            )
                            keyToId[n.nodeKey] = newId
                        }
                    }
                    wrapper.edges.forEach { e ->
                        val fromId = keyToId[e.fromKey] ?: return@forEach
                        val toId = keyToId[e.toKey] ?: return@forEach
                        if (fromId == toId) return@forEach
                        val rel = KnowledgeGraphHelpers.normalizeRelation(e.relation)
                        if (kgDao.getEdgeByTriple(fromId, toId, rel) == null) {
                            val updAt = if (e.updatedAt > 0L) e.updatedAt else nowMs
                            val resolvedStatus = if (
                                e.status != "active" ||
                                rel == "MENGGANTIKAN" ||
                                e.weight < 0.4f ||
                                e.evidence.startsWith("[Historis")
                            ) "historical" else "active"
                            kgDao.insertEdge(
                                KgEdgeEntity(
                                    fromNodeId = fromId,
                                    toNodeId = toId,
                                    relation = rel,
                                    evidence = e.evidence.take(240),
                                    weight = e.weight.coerceIn(0.15f, 5.0f),
                                    confidence = e.confidence.coerceIn(0.3f, 1.0f),
                                    updatedAt = updAt,
                                    status = resolvedStatus,
                                    createdAt = if (e.createdAt > 0L) e.createdAt else updAt,
                                    lastConfirmedAt = if (e.lastConfirmedAt > 0L) e.lastConfirmedAt else updAt,
                                    sourceType = e.sourceType.ifBlank { "chat" },
                                    sourceRef = e.sourceRef
                                )
                            )
                        }
                    }
                    restoredModules++
                }
            } else if (replaceAll && files["asisten.json"] != null) {
                kgDao.clearAll()
            }

            if (restoredModules == 0) {
                EcosystemZipImportResult.Error("Tidak ada modul Asisten Rem yang dikenali di dalam ZIP")
            } else {
                val modeLabel = if (replaceAll) "Replace" else "Merge"
                EcosystemZipImportResult.Success(
                    summary = "Restore ZIP ($modeLabel) berhasil untuk $restoredModules modul",
                    latestSessionId = latestSessionId
                )
            }
        }.getOrElse { err ->
            EcosystemZipImportResult.Error("Gagal membaca ZIP: ${err.message}")
        }
    }

    private suspend fun syncIdekuWishlistToCuciMoney(item: IdekuIdeaEntity) {
        if (item.type != "Wishlist") return
        val idStr = item.id.toString()
        val existing = cuciMoneyDao.getWishlistById(idStr)
            ?: cuciMoneyDao.getWishlistById("wish_${item.id}")
            ?: cuciMoneyDao.getAllWishlists().firstOrNull { it.title.equals(item.title, ignoreCase = true) }
        val wishId = existing?.id ?: idStr
        cuciMoneyDao.upsertWishlist(
            CuciWishlistEntity(
                id = wishId,
                title = item.title,
                price = item.price,
                priority = item.priority,
                image = item.image.ifBlank { "bag" },
                status = item.status,
                saved = existing?.saved ?: 0L,
                realizedAt = existing?.realizedAt,
                realizedAmount = existing?.realizedAmount ?: 0L,
                historyJson = existing?.historyJson ?: "[]",
                createdAt = existing?.createdAt ?: item.createdAt
            )
        )
    }
}
