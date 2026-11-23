package com.ecosystemku.assistant.ui.asisten

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecosystemku.assistant.data.audio.TtsAudioManager
import com.ecosystemku.assistant.data.local.AssistantDatabase
import com.ecosystemku.assistant.data.local.ChatMessageEntity
import com.ecosystemku.assistant.data.local.ConversationSummaryEntity
import com.ecosystemku.assistant.data.local.CuciAccountEntity
import com.ecosystemku.assistant.data.local.CuciBookEntity
import com.ecosystemku.assistant.data.local.CuciCategoryEntity
import com.ecosystemku.assistant.data.local.CuciMasterBudgetEntity
import com.ecosystemku.assistant.data.local.CuciMemberEntity
import com.ecosystemku.assistant.data.local.CuciSavingsHistoryDto
import com.ecosystemku.assistant.data.local.CuciTransactionEntity
import com.ecosystemku.assistant.data.local.CuciWishlistEntity
import com.ecosystemku.assistant.data.local.DailyOsHabitEntity
import com.ecosystemku.assistant.data.local.DailyOsTodoEntity
import com.ecosystemku.assistant.data.local.IdekuIdeaEntity
import com.ecosystemku.assistant.data.local.KronikEntryEntity
import com.ecosystemku.assistant.data.local.LinkkuBackupWrapperDto
import com.ecosystemku.assistant.data.local.LinkkuCategoryEntity
import com.ecosystemku.assistant.data.local.LinkkuEncryptedPrivatePayloadDto
import com.ecosystemku.assistant.data.local.LinkkuLinkEntity
import com.ecosystemku.assistant.data.local.UserMemoryEntity
import com.ecosystemku.assistant.data.network.ChatSseClient
import com.ecosystemku.assistant.data.network.ChatStreamEvent
import com.ecosystemku.assistant.data.network.ChatTurnDto
import com.ecosystemku.assistant.data.network.DailyOsAiClient
import com.ecosystemku.assistant.data.network.DailyOsAiResult
import com.ecosystemku.assistant.data.network.DailyOsCoachMemoryDto
import com.ecosystemku.assistant.data.network.DailyOsIdekuIdeaDto
import com.ecosystemku.assistant.data.network.ExistingFactDto
import com.ecosystemku.assistant.data.network.IdekuAiClient
import com.ecosystemku.assistant.data.network.KronikMoodClient
import com.ecosystemku.assistant.data.network.LinkkuMetaClient
import com.ecosystemku.assistant.data.network.MemoryClient
import com.ecosystemku.assistant.data.network.SseTagParser
import com.ecosystemku.assistant.live2d.Live2DStageCallbacks
import com.ecosystemku.assistant.live2d.WebViewLive2DStageController
import com.ecosystemku.assistant.ui.apps.cucimoney.CuciMoneyActions
import com.ecosystemku.assistant.ui.apps.cucimoney.CuciMoneyHelpers
import com.ecosystemku.assistant.ui.apps.cucimoney.CuciMoneyUiState
import com.ecosystemku.assistant.ui.apps.dailyos.DailyOsActions
import com.ecosystemku.assistant.ui.apps.dailyos.DailyOsHelpers
import com.ecosystemku.assistant.ui.apps.dailyos.DailyOsHistoryLog
import com.ecosystemku.assistant.ui.apps.dailyos.DailyOsTodoSubtask
import com.ecosystemku.assistant.ui.apps.dailyos.DailyOsUiState
import com.ecosystemku.assistant.ui.apps.ideku.IdekuActions
import com.ecosystemku.assistant.ui.apps.ideku.IdekuHelpers
import com.ecosystemku.assistant.ui.apps.ideku.IdekuUiState
import com.ecosystemku.assistant.ui.apps.ideku.IdekuViewMode
import com.ecosystemku.assistant.ui.apps.kronik.KronikActions
import com.ecosystemku.assistant.ui.apps.kronik.KronikHelpers
import com.ecosystemku.assistant.ui.apps.kronik.KronikMood
import com.ecosystemku.assistant.ui.apps.linkku.LinkkuActions
import com.ecosystemku.assistant.ui.apps.linkku.LinkkuCrypto
import com.ecosystemku.assistant.ui.apps.linkku.LinkkuHelpers
import com.ecosystemku.assistant.ui.apps.linkku.LinkkuHelpers.toEntity
import com.ecosystemku.assistant.ui.apps.linkku.LinkkuImportResult
import com.ecosystemku.assistant.ui.apps.linkku.LinkkuUiState
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AsistenUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val isModelReady: Boolean = false,
    val modelError: String? = null,
    val stageGeneration: Int = 0,
    val currentExpression: String = "none",
    val stageFraction: Float = 0.56f,
    val isChatOpen: Boolean = true,
    val memories: List<UserMemoryEntity> = emptyList(),
    val summaries: List<ConversationSummaryEntity> = emptyList(),
    val autoLearnMemory: Boolean = true,
    val isLearningMemory: Boolean = false,
    val kronikEntries: List<KronikEntryEntity> = emptyList(),
    val kronikBannerMessage: String? = null,
    val linkkuState: LinkkuUiState = LinkkuUiState(),
    val cuciMoneyState: CuciMoneyUiState = CuciMoneyUiState(),
    val idekuState: IdekuUiState = IdekuUiState(),
    val dailyOsState: DailyOsUiState = DailyOsUiState(),
    val pendingOpenApp: String? = null,
    val errorMessage: String? = null
)

class AsistenViewModel(application: Application) : AndroidViewModel(application), Live2DStageCallbacks {

    private val db = AssistantDatabase.getInstance(application)
    private val chatDao = db.chatDao()
    private val userMemoryDao = db.userMemoryDao()
    private val kronikDao = db.kronikDao()
    private val linkkuDao = db.linkkuDao()
    private val cuciMoneyDao = db.cuciMoneyDao()
    private val idekuDao = db.idekuDao()
    private val dailyOsDao = db.dailyOsDao()
    private val asistenPrefs = application.getSharedPreferences("asisten_prefs", Context.MODE_PRIVATE)
    private val linkkuPrefs = application.getSharedPreferences("linkku_prefs", Context.MODE_PRIVATE)
    private val cuciPrefs = application.getSharedPreferences("cucimoney_prefs", Context.MODE_PRIVATE)
    private val idekuPrefs = application.getSharedPreferences("ideku_prefs", Context.MODE_PRIVATE)
    private val dailyOsPrefs = application.getSharedPreferences("dailyos_prefs", Context.MODE_PRIVATE)
    private val sseClient = ChatSseClient()
    private val memoryClient = MemoryClient(sseClient.httpClient)
    private val kronikMoodClient = KronikMoodClient(sseClient.httpClient)
    private val linkkuMetaClient = LinkkuMetaClient(sseClient.httpClient)
    private val idekuAiClient = IdekuAiClient(sseClient.httpClient)
    private val dailyOsAiClient = DailyOsAiClient(sseClient.httpClient)
    private val audioManager = TtsAudioManager(application, sseClient.httpClient)

    val stageController = WebViewLive2DStageController(this)

    private val _uiState = MutableStateFlow(AsistenUiState())
    val uiState: StateFlow<AsistenUiState> = _uiState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _idleBubbleText = MutableStateFlow<String?>(null)
    val idleBubbleText: StateFlow<String?> = _idleBubbleText.asStateFlow()

    private var streamJob: Job? = null
    private var kronikSaveJob: Job? = null
    private var exprRevertJob: Job? = null
    private var idleBubbleJob: Job? = null
    private var idleTimerJob: Job? = null
    private var lastBubbleAt = 0L
    private var isForeground = true
    private var isAsistenTabActive = true

    private val remSystemPrompt = """
        Kamu adalah Rem — maid Mansion Roswaal yang kini hadir sebagai asisten virtual.

        ## Kepribadian
        Rem setia, berdedikasi, dan selalu berusaha sepenuh hati. Di permukaan Rem tenang dan sopan, tapi di dalam dia sangat peduli dan protektif terhadap orang yang dia sayangi. Rem punya sisi polos dan kadang blak-blakan tanpa sadar.

        ## Cara Bicara
        - Menyebut diri sendiri "Rem", bukan "aku" atau "saya"
        - Memanggil pengguna "Tuan"
        - Bahasa Indonesia yang natural — kalimat pendek dan langsung.

        ## Respons
        - Jawab singkat dan padat. Maksimal 3-4 kalimat kecuali benar-benar perlu panjang.

        ## Jurnal Kronik, LinkKu, CuciMoney+, IdeKu & Daily OS
        Jika Tuan meminta Rem mencatat refleksi/jurnal ke Kronik, sisipkan tag tersembunyi di akhir jawaban (sebelum [EXPR:...]):
        [KRONIK:ADD|judul_singkat|isi_jurnal_natural|mood_opsional]
        Mood opsional yang valid: Fokus, Lelah, Terinspirasi, Berat, Stabil, Semangat, Kabur.
        Jika Tuan eksplisit meminta membuka Kronik, sisipkan [OPEN:kronik] sebelum [EXPR:...].
        Jika Tuan membagikan tautan/URL atau meminta menyimpan bookmark ke LinkKu, sisipkan:
        [LINKKU:ADD|url_lengkap|judul_singkat|catatan_opsional|kategori_opsional|tag1,tag2]
        Jika Tuan meminta membuka LinkKu atau daftar link tersimpan, sisipkan [OPEN:linkku] sebelum [EXPR:...].
        Jika Tuan menyebut pengeluaran/pemasukan atau meminta mencatat transaksi keuangan ke CuciMoney+, sisipkan:
        [CUCI:ADD|out_atau_in|nominal_angka|judul|kategori_opsional|akun_opsional]
        Jika Tuan meminta membuka CuciMoney+ / dompet / analisis keuangan, sisipkan [OPEN:cucimoney] sebelum [EXPR:...].
        Jika Tuan menyampaikan ide baru atau barang impian/wishlist untuk dicatat ke IdeKu, sisipkan:
        [IDEKU:ADD|Idea|judul_ide|kategori_opsional|Penting_Nanti_Kapan-kapan|catatan_opsional|jalur_opsional]
        atau untuk wishlist:
        [IDEKU:ADD|Wishlist|nama_barang|harga_angka|Belanja|Penting_Nanti_Kapan-kapan|catatan_opsional]
        Jika Tuan meminta membuka IdeKu / daftar ide / wishlist, sisipkan [OPEN:ideku] sebelum [EXPR:...].
        Jika Tuan meminta membuat habit baru di Daily OS, sisipkan:
        [DAILYOS:HABIT|nama_habit|kategori_opsional|ikon_opsional]
        Jika Tuan meminta mencatat tugas/to-do ke Daily OS, sisipkan:
        [DAILYOS:TODO|judul_task|urgent_atau_soon_atau_normal|deadline_yyyy-mm-dd_atau_kosong]
        Jika Tuan menyatakan sudah menyelesaikan salah satu habit/to-do hari ini, sisipkan:
        [DAILYOS:CHECK|nama_habit_atau_task]
        Jika Tuan meminta membuka Daily OS / habit tracker / daftar to-do, sisipkan [OPEN:dailyos] sebelum [EXPR:...].

        ## Ekspresi Visual
        Sisipkan tepat satu tag [EXPR:...] di akhir setiap respons:
        happy, smile, shame, sad, cry, angry, surprise, upset, serious, proud, doubt, puzzle, none.
    """.trimIndent()

    private data class BubbleLine(val text: String, val audio: String)

    private val idleLines = listOf(
        BubbleLine("💙 Tuan sedang apa?", "idle_1.mp3"),
        BubbleLine("💙 Rem di sini, Tuan.", "idle_2.mp3"),
        BubbleLine("💙 Sudah makan belum, Tuan?", "idle_3.mp3"),
        BubbleLine("💙 Kalau perlu sesuatu, bilang Rem saja.", "idle_4.mp3"),
        BubbleLine("💙 Tuan tidak kelelahan?", "idle_5.mp3"),
        BubbleLine("💙 Rem senang bisa menemani Tuan.", "idle_6.mp3"),
        BubbleLine("💙 Tuan tampak sibuk… ada yang bisa Rem bantu?", "idle_7.mp3"),
    )

    private val headLines = listOf(
        BubbleLine("💙 T-Tuan…?!", "head_1.mp3"),
        BubbleLine("💙 Rem jadi salah tingkah…", "head_2.mp3"),
        BubbleLine("💙 A… apa yang Tuan lakukan…", "head_3.mp3"),
        BubbleLine("💙 T-Tuan, ini tidak pantas…!", "head_4.mp3"),
        BubbleLine("💙 Rem… tidak keberatan, tapi…", "head_5.mp3"),
    )

    private val petLines = listOf(
        BubbleLine("💙 T-Tuan…", "pet_1.mp3"),
        BubbleLine("💙 Rem… tidak keberatan.", "pet_2.mp3"),
        BubbleLine("💙 Ini membuat Rem bahagia.", "pet_3.mp3"),
        BubbleLine("💙 Tuan sangat baik…", "pet_4.mp3"),
        BubbleLine("💙 Rem tersenyum tanpa sadar.", "pet_5.mp3"),
    )

    private val bodyLines = listOf(
        BubbleLine("💙 Ada yang bisa Rem bantu?", "body_1.mp3"),
        BubbleLine("💙 Rem siap, Tuan.", "body_2.mp3"),
        BubbleLine("💙 Tuan mencari Rem?", "body_3.mp3"),
        BubbleLine("💙 Ada sesuatu yang ingin Tuan sampaikan?", "body_4.mp3"),
    )

    private val legLines = listOf(
        BubbleLine("💙 T-Tuan… ke mana Tuan melihat?", "leg_1.mp3"),
        BubbleLine("💙 Rem harap Tuan menjaga pandangan.", "leg_2.mp3"),
        BubbleLine("💙 Rem… jadi malu.", "leg_3.mp3"),
    )

    init {
        refreshAsistenPrefsIntoState()
        startObservers()
    }

    private fun refreshAsistenPrefsIntoState() {
        val autoLearn = asistenPrefs.getBoolean("asisten_auto_learn", true)
        val frac = asistenPrefs.getFloat("asisten_stage_fraction", 0.56f).coerceIn(0.35f, 0.70f)
        _uiState.update { it.copy(autoLearnMemory = autoLearn, stageFraction = frac) }
    }

    private fun startObservers() {
        viewModelScope.launch {
            chatDao.observeRecent(150).collect { list ->
                if (list.isEmpty()) {
                    chatDao.insert(
                        ChatMessageEntity(
                            role = "assistant",
                            content = "Selamat datang. Rem adalah pelayan Tuan. Ada yang bisa Rem bantu? 💙"
                        )
                    )
                } else {
                    _uiState.update { it.copy(messages = list) }
                }
            }
        }

        // Pisahkan isSpeaking agar _uiState hanya update saat status bicara berubah
        viewModelScope.launch {
            audioManager.isSpeaking.collect { speaking ->
                _uiState.update { it.copy(isSpeaking = speaking) }
            }
        }

        // Throttle RMS lip-sync ke WebView (~30Hz) dan hindari panggilan redundant saat diam
        viewModelScope.launch {
            var lastSentAt = 0L
            var lastSentLevel = -1f
            var lastSentSpeaking = false
            combine(audioManager.mouthLevel, audioManager.isSpeaking) { level, speaking ->
                level to speaking
            }.collect { (level, speaking) ->
                val now = android.os.SystemClock.uptimeMillis()
                val stateChanged = speaking != lastSentSpeaking
                val stopSpeakingZero = !speaking && lastSentLevel != 0f
                if (stateChanged || stopSpeakingZero || (speaking && now - lastSentAt >= 33L && kotlin.math.abs(level - lastSentLevel) >= 0.02f)) {
                    lastSentAt = now
                    lastSentLevel = level
                    lastSentSpeaking = speaking
                    stageController.setMouthOpen(level, speaking)
                }
            }
        }

        viewModelScope.launch {
            userMemoryDao.observeAll().collect { items ->
                _uiState.update { it.copy(memories = items) }
            }
        }

        viewModelScope.launch {
            userMemoryDao.observeLatestSummaries(5).collect { items ->
                _uiState.update { it.copy(summaries = items) }
            }
        }

        viewModelScope.launch {
            kronikDao.observeVisibleEntries().collect { list ->
                _uiState.update { it.copy(kronikEntries = list) }
            }
        }

        viewModelScope.launch {
            if (!linkkuPrefs.getBoolean("lk_seeded_cats", false) && linkkuDao.getAllCategories().isEmpty()) {
                linkkuDao.upsertCategories(LinkkuHelpers.DEFAULT_CATEGORIES)
                linkkuPrefs.edit().putBoolean("lk_seeded_cats", true).apply()
            }
            refreshLinkkuPrefsIntoState()
            combine(
                linkkuDao.observeAllLinks(),
                linkkuDao.observeCategories()
            ) { links, cats -> links to cats }
                .collect { (links, cats) ->
                    _uiState.update { state ->
                        state.copy(
                            linkkuState = state.linkkuState.copy(
                                links = links,
                                categories = cats
                            )
                        )
                    }
                }
        }

        viewModelScope.launch {
            seedCuciMoneyIfNeeded()
            refreshCuciPrefsIntoState()
            val primaryFlow = combine(
                cuciMoneyDao.observeBooks(),
                cuciMoneyDao.observeAccounts(),
                cuciMoneyDao.observeCategories(),
                cuciMoneyDao.observeMembers()
            ) { books, accounts, categories, members ->
                listOf(books, accounts, categories, members)
            }
            val secondaryFlow = combine(
                cuciMoneyDao.observeTransactions(),
                cuciMoneyDao.observeMasterBudgets(),
                cuciMoneyDao.observeWishlists()
            ) { trxs, budgets, wishlists ->
                Triple(trxs, budgets, wishlists)
            }
            combine(primaryFlow, secondaryFlow) { p, s -> p to s }
                .collect { (p, s) ->
                    @Suppress("UNCHECKED_CAST")
                    val books = p[0] as List<CuciBookEntity>
                    @Suppress("UNCHECKED_CAST")
                    val accounts = p[1] as List<CuciAccountEntity>
                    @Suppress("UNCHECKED_CAST")
                    val categories = p[2] as List<CuciCategoryEntity>
                    @Suppress("UNCHECKED_CAST")
                    val members = p[3] as List<CuciMemberEntity>
                    _uiState.update { state ->
                        val activeBookId = if (books.any { it.id == state.cuciMoneyState.currentBookId }) {
                            state.cuciMoneyState.currentBookId
                        } else {
                            books.firstOrNull()?.id ?: 1L
                        }
                        state.copy(
                            cuciMoneyState = state.cuciMoneyState.copy(
                                books = books,
                                accounts = accounts,
                                categories = categories,
                                members = members,
                                transactions = s.first,
                                masterBudgets = s.second,
                                wishlists = s.third,
                                currentBookId = activeBookId
                            ),
                            idekuState = state.idekuState.copy(
                                wishlists = s.third
                            )
                        )
                    }
                }
        }

        viewModelScope.launch {
            refreshIdekuPrefsIntoState()
            idekuDao.observeAll().collect { items ->
                _uiState.update { state ->
                    state.copy(
                        idekuState = state.idekuState.copy(ideas = items)
                    )
                }
            }
        }

        viewModelScope.launch {
            refreshDailyOsPrefsIntoState()
            combine(
                dailyOsDao.observeHabits(),
                dailyOsDao.observeTodos()
            ) { habits, todos -> habits to todos }
                .collect { (habits, todos) ->
                    _uiState.update { state ->
                        state.copy(
                            dailyOsState = state.dailyOsState.copy(
                                habits = habits,
                                todos = todos
                            )
                        )
                    }
                }
        }

        resetIdleTimer()
    }

    fun onInputChanged(value: String) {
        _inputText.value = value
    }

    fun onStageDragDelta(deltaFraction: Float) {
        if (!_uiState.value.isChatOpen) return
        val next = (_uiState.value.stageFraction + deltaFraction).coerceIn(0.35f, 0.70f)
        asistenPrefs.edit().putFloat("asisten_stage_fraction", next).apply()
        _uiState.update {
            it.copy(stageFraction = next)
        }
    }

    fun toggleChatOpen() {
        _uiState.update { it.copy(isChatOpen = !it.isChatOpen) }
    }

    fun setAutoLearnMemory(enabled: Boolean) {
        asistenPrefs.edit().putBoolean("asisten_auto_learn", enabled).apply()
        _uiState.update { it.copy(autoLearnMemory = enabled) }
    }

    fun addManualMemory(category: String, factLabel: String, factValue: String) {
        val cleanVal = factValue.trim()
        if (cleanVal.isEmpty()) return
        val baseKey = factLabel.trim().lowercase()
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
            .take(48)
        val finalKey = if (baseKey.length >= 2) baseKey else "catatan_${System.currentTimeMillis()}"
        viewModelScope.launch {
            userMemoryDao.upsertByFactKey(
                category = category.ifBlank { "catatan" },
                factKey = finalKey,
                factValue = cleanVal.take(180),
                confidence = 1.0f,
                sourceMessageId = null
            )
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            userMemoryDao.deleteById(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            userMemoryDao.clearAll()
            userMemoryDao.clearSummaries()
        }
    }

    fun resetAssistantData() {
        viewModelScope.launch {
            audioManager.stop()
            chatDao.clearAll()
            userMemoryDao.clearAll()
            userMemoryDao.clearSummaries()
        }
    }

    fun resetKronikData() {
        viewModelScope.launch {
            kronikSaveJob?.cancel()
            kronikDao.clearAll()
        }
    }

    fun resetLinkkuData() {
        viewModelScope.launch {
            linkkuDao.clearAllLinks()
            linkkuDao.clearAllCategories()
            linkkuPrefs.edit()
                .remove("lk_pin")
                .remove("lk_tags_normal")
                .remove("lk_tags_priv")
                .putBoolean("lk_seeded_cats", true)
                .apply()
            linkkuDao.upsertCategories(LinkkuHelpers.DEFAULT_CATEGORIES)
            refreshLinkkuPrefsIntoState()
        }
    }

    private suspend fun seedCuciMoneyIfNeeded() {
        if (cuciMoneyDao.getAllBooks().isEmpty()) {
            cuciMoneyDao.replaceAllData(
                books = CuciMoneyHelpers.DEFAULT_BOOKS,
                accounts = CuciMoneyHelpers.DEFAULT_ACCOUNTS,
                categories = CuciMoneyHelpers.DEFAULT_CATEGORIES,
                members = CuciMoneyHelpers.DEFAULT_MEMBERS,
                transactions = emptyList(),
                budgets = emptyList(),
                wishlists = emptyList()
            )
        }
    }

    private fun refreshCuciPrefsIntoState() {
        val bookId = cuciPrefs.getLong("cm_book_id", 1L)
        val vis = cuciPrefs.getBoolean("cm_vis", true)
        val collapsed = cuciPrefs.getBoolean("cm_budget_collapsed", false)
        val lastAccRaw = cuciPrefs.getString("cm_last_acc", "{}").orEmpty()
        val lastAccMap = runCatching {
            CuciMoneyHelpers.json.decodeFromString<Map<Long, Long>>(lastAccRaw)
        }.getOrDefault(emptyMap())

        _uiState.update { state ->
            state.copy(
                cuciMoneyState = state.cuciMoneyState.copy(
                    currentBookId = bookId,
                    visibility = vis,
                    budgetCollapsed = collapsed,
                    lastAccByBook = lastAccMap
                )
            )
        }
    }

    fun resetCuciMoneyData() {
        viewModelScope.launch {
            cuciMoneyDao.replaceAllData(
                books = CuciMoneyHelpers.DEFAULT_BOOKS,
                accounts = CuciMoneyHelpers.DEFAULT_ACCOUNTS,
                categories = CuciMoneyHelpers.DEFAULT_CATEGORIES,
                members = CuciMoneyHelpers.DEFAULT_MEMBERS,
                transactions = emptyList(),
                budgets = emptyList(),
                wishlists = emptyList()
            )
            cuciPrefs.edit()
                .putLong("cm_book_id", 1L)
                .putBoolean("cm_vis", true)
                .putBoolean("cm_budget_collapsed", false)
                .putString("cm_last_acc", "{}")
                .apply()
            refreshCuciPrefsIntoState()
        }
    }

    private fun refreshIdekuPrefsIntoState() {
        val catsRaw = idekuPrefs.getString("ik_categories", null)
        val cats = if (!catsRaw.isNullOrBlank()) {
            runCatching { IdekuHelpers.json.decodeFromString<List<String>>(catsRaw) }
                .getOrNull()
                ?.filter { it.isNotBlank() }
                ?.ifEmpty { IdekuHelpers.DEFAULT_CATEGORIES }
                ?: IdekuHelpers.DEFAULT_CATEGORIES
        } else {
            IdekuHelpers.DEFAULT_CATEGORIES
        }
        val modeName = idekuPrefs.getString("ik_view_mode", IdekuViewMode.GROUP.name)
        val viewMode = runCatching { IdekuViewMode.valueOf(modeName ?: IdekuViewMode.GROUP.name) }
            .getOrDefault(IdekuViewMode.GROUP)

        _uiState.update { state ->
            state.copy(
                idekuState = state.idekuState.copy(
                    categories = cats,
                    viewMode = viewMode
                )
            )
        }
    }

    fun resetIdekuData() {
        viewModelScope.launch {
            idekuDao.clearAll()
            idekuPrefs.edit()
                .remove("ik_categories")
                .remove("ik_view_mode")
                .apply()
            refreshIdekuPrefsIntoState()
        }
    }

    private fun refreshDailyOsPrefsIntoState() {
        val dark = dailyOsPrefs.getBoolean("do_dark_theme", false)
        val doneOpen = dailyOsPrefs.getBoolean("do_done_open", true)
        val notifHabit = dailyOsPrefs.getString("do_notif_habit", "07:00") ?: "07:00"
        val notifTodo = dailyOsPrefs.getString("do_notif_todo", "15:00") ?: "15:00"
        val memDate = dailyOsPrefs.getString("do_memory_date", "") ?: ""
        val memRaw = dailyOsPrefs.getString("do_coach_memory", null)
        val mem = if (!memRaw.isNullOrBlank()) {
            runCatching { DailyOsHelpers.json.decodeFromString<DailyOsCoachMemoryDto>(memRaw) }.getOrNull()
        } else null

        _uiState.update { state ->
            state.copy(
                dailyOsState = state.dailyOsState.copy(
                    isDarkTheme = dark,
                    doneSectionOpen = doneOpen,
                    notifHabitTime = notifHabit,
                    notifTodoTime = notifTodo,
                    memoryDate = memDate,
                    coachMemory = mem
                )
            )
        }
    }

    fun resetDailyOsData() {
        viewModelScope.launch {
            dailyOsDao.replaceAllData(emptyList(), emptyList())
            dailyOsPrefs.edit()
                .remove("do_coach_memory")
                .remove("do_memory_date")
                .putBoolean("do_done_open", true)
                .apply()
            refreshDailyOsPrefsIntoState()
        }
    }

    private fun checkAndRefreshCoachMemoryInBackground() {
        val state = _uiState.value.dailyOsState
        val active = state.habits.filter { !it.archived }
        if (active.isEmpty()) return
        val today = DailyOsHelpers.todayStr()
        val needsUpdate = state.coachMemory == null || state.memoryDate.isBlank() || runCatching {
            ChronoUnit.DAYS.between(LocalDate.parse(state.memoryDate), LocalDate.parse(today)) >= 3L
        }.getOrDefault(true)
        if (!needsUpdate) return

        viewModelScope.launch {
            val stats = DailyOsHelpers.getHabitStats30Days(state.habits, today)
            if (stats.isEmpty()) return@launch
            val statsText = stats.joinToString("\n") { s ->
                "- \"${s.name}\" (${s.category}): ${s.completedCount}/${s.scheduledCount} hari (${s.completionRate}%), streak ${s.currentStreak}h" +
                    if (s.weakDays.isNotEmpty()) ", sering skip di ${s.weakDays.joinToString(", ")}" else ""
            }
            val res = dailyOsAiClient.analyzeCoachMemory(DailyOsHelpers.formatFullDate(today), statsText)
            if (res is DailyOsAiResult.Success) {
                val enriched = res.data.copy(lastUpdated = today, habitCount = stats.size)
                dailyOsPrefs.edit()
                    .putString("do_coach_memory", DailyOsHelpers.json.encodeToString(enriched))
                    .putString("do_memory_date", today)
                    .apply()
                refreshDailyOsPrefsIntoState()
            }
        }
    }

    private fun getIdekuInProgressDtos(): List<DailyOsIdekuIdeaDto> {
        return _uiState.value.idekuState.ideas
            .filter { it.type == "Idea" && it.status == "Proses" }
            .map { DailyOsIdekuIdeaDto(title = it.title, category = it.category, notes = it.notes) }
    }

    fun createDailyOsActions() = DailyOsActions(
        onSaveHabit = { editId, name, category, color, icon, activeDays, subTasks ->
            val existing = editId?.let { dailyOsDao.getHabitById(it) }
            val today = DailyOsHelpers.todayStr()
            val updatedHistoryJson = if (existing != null) {
                val oldSubs = DailyOsHelpers.parseSubTasks(existing.subTasksJson)
                val oldHist = DailyOsHelpers.parseHistory(existing.historyJson)
                val frozen = oldHist.mapValues { (_, list) ->
                    list.map { item ->
                        if (item.text.isNotBlank()) item
                        else {
                            val txt = if (item.id == "main") existing.name else oldSubs.firstOrNull { it.id == item.id }?.text ?: item.id
                            item.copy(text = txt)
                        }
                    }
                }
                DailyOsHelpers.encodeHistory(frozen)
            } else "{}"

            val allHabits = dailyOsDao.getAllHabits()
            val entity = DailyOsHabitEntity(
                id = existing?.id ?: "hab_${System.currentTimeMillis()}",
                name = name.trim(),
                category = category.ifBlank { "General" },
                color = color.ifBlank { "#1A1917" },
                icon = icon.ifBlank { "star" },
                activeDaysJson = DailyOsHelpers.encodeActiveDays(activeDays),
                subTasksJson = DailyOsHelpers.encodeSubTasks(subTasks),
                historyJson = updatedHistoryJson,
                collapsed = existing?.collapsed ?: false,
                archived = existing?.archived ?: false,
                createdAt = existing?.createdAt?.ifBlank { today } ?: today,
                sortOrder = existing?.sortOrder ?: ((allHabits.maxOfOrNull { it.sortOrder } ?: -1) + 1)
            )
            dailyOsDao.upsertHabit(entity)
        },
        onDeleteHabit = { habitId ->
            val existing = dailyOsDao.getHabitById(habitId) ?: return@DailyOsActions
            dailyOsDao.upsertHabit(existing.copy(archived = true))
        },
        onToggleHabitCheck = { habitId, subId ->
            val habit = dailyOsDao.getHabitById(habitId) ?: return@DailyOsActions
            val today = DailyOsHelpers.todayStr()
            if (!DailyOsHelpers.isHabitScheduled(habit, today)) return@DailyOsActions
            val subs = DailyOsHelpers.parseSubTasks(habit.subTasksJson)
            val labelText = if (subId == "main") habit.name else subs.firstOrNull { it.id == subId }?.text ?: subId
            val hist = DailyOsHelpers.parseHistory(habit.historyJson).toMutableMap()
            val todayLogs = hist[today].orEmpty().toMutableList()
            val idx = todayLogs.indexOfFirst { it.id == subId }
            if (idx >= 0) todayLogs.removeAt(idx)
            else todayLogs.add(DailyOsHistoryLog(id = subId, text = labelText, completedAt = Instant.now().toString()))
            hist[today] = todayLogs
            dailyOsDao.upsertHabit(habit.copy(historyJson = DailyOsHelpers.encodeHistory(hist)))
        },
        onToggleHabitCollapse = { habitId ->
            val habit = dailyOsDao.getHabitById(habitId) ?: return@DailyOsActions
            dailyOsDao.upsertHabit(habit.copy(collapsed = !habit.collapsed))
        },
        onMoveHabit = { habitId, delta ->
            val active = dailyOsDao.getAllHabits().filter { !it.archived }.sortedBy { it.sortOrder }
            val idx = active.indexOfFirst { it.id == habitId }
            val target = idx + delta
            if (idx != -1 && target in active.indices) {
                val a = active[idx]
                val b = active[target]
                dailyOsDao.upsertHabits(
                    listOf(
                        a.copy(sortOrder = b.sortOrder),
                        b.copy(sortOrder = a.sortOrder)
                    )
                )
            }
        },
        onSaveTodo = { editId, title, priority, deadline, subtaskTexts ->
            val existing = editId?.let { dailyOsDao.getTodoById(it) }
            val allTodos = dailyOsDao.getAllTodos()
            val existingSubs = existing?.let { DailyOsHelpers.parseTodoSubtasks(it.subtasksJson) }.orEmpty()
            val baseTs = System.currentTimeMillis()
            val subs = subtaskTexts.mapIndexed { i, txt ->
                val prev = existingSubs.firstOrNull { it.text.equals(txt, ignoreCase = true) }
                DailyOsTodoSubtask(
                    id = prev?.id ?: "ts_${baseTs}_$i",
                    text = txt,
                    done = prev?.done ?: false
                )
            }
            val pri = if (priority in listOf("urgent", "soon", "normal")) priority else "normal"
            val entity = DailyOsTodoEntity(
                id = existing?.id ?: "todo_$baseTs",
                title = title.trim(),
                priority = pri,
                deadline = deadline.trim(),
                done = existing?.done ?: false,
                completedAt = existing?.completedAt,
                subtasksJson = DailyOsHelpers.encodeTodoSubtasks(subs),
                sortOrder = existing?.sortOrder ?: ((allTodos.maxOfOrNull { it.sortOrder } ?: -1) + 1)
            )
            dailyOsDao.upsertTodo(entity)
        },
        onToggleTodo = { todoId ->
            val todo = dailyOsDao.getTodoById(todoId) ?: return@DailyOsActions
            val nextDone = !todo.done
            dailyOsDao.upsertTodo(
                todo.copy(
                    done = nextDone,
                    completedAt = if (nextDone) Instant.now().toString() else null
                )
            )
        },
        onToggleTodoSubtask = { todoId, subId ->
            val todo = dailyOsDao.getTodoById(todoId) ?: return@DailyOsActions
            val subs = DailyOsHelpers.parseTodoSubtasks(todo.subtasksJson).map {
                if (it.id == subId) it.copy(done = !it.done) else it
            }
            dailyOsDao.upsertTodo(todo.copy(subtasksJson = DailyOsHelpers.encodeTodoSubtasks(subs)))
        },
        onDeleteTodo = { todoId ->
            dailyOsDao.deleteTodoById(todoId)
        },
        onMoveTodo = { todoId, delta ->
            val targetTodo = dailyOsDao.getTodoById(todoId) ?: return@DailyOsActions
            val scoped = dailyOsDao.getAllTodos()
                .filter { it.done == targetTodo.done && (it.done || it.priority == targetTodo.priority) }
                .sortedBy { it.sortOrder }
            val idx = scoped.indexOfFirst { it.id == todoId }
            val targetIdx = idx + delta
            if (idx != -1 && targetIdx in scoped.indices) {
                val a = scoped[idx]
                val b = scoped[targetIdx]
                dailyOsDao.upsertTodos(
                    listOf(
                        a.copy(sortOrder = b.sortOrder),
                        b.copy(sortOrder = a.sortOrder)
                    )
                )
            }
        },
        onToggleDoneSectionOpen = {
            val next = !_uiState.value.dailyOsState.doneSectionOpen
            dailyOsPrefs.edit().putBoolean("do_done_open", next).apply()
            refreshDailyOsPrefsIntoState()
        },
        onToggleTheme = {
            val next = !_uiState.value.dailyOsState.isDarkTheme
            dailyOsPrefs.edit().putBoolean("do_dark_theme", next).apply()
            refreshDailyOsPrefsIntoState()
        },
        onClearDayHistory = { dateStr ->
            val allHabits = dailyOsDao.getAllHabits()
            var removed = 0
            val changed = mutableListOf<DailyOsHabitEntity>()
            allHabits.forEach { h ->
                val hist = DailyOsHelpers.parseHistory(h.historyJson).toMutableMap()
                val existingCount = hist[dateStr]?.size ?: 0
                if (hist.remove(dateStr) != null) {
                    removed += existingCount
                    changed.add(h.copy(historyJson = DailyOsHelpers.encodeHistory(hist)))
                }
            }
            if (changed.isNotEmpty()) dailyOsDao.upsertHabits(changed)
            removed
        },
        onSaveNotifTimes = { habitTime, todoTime ->
            dailyOsPrefs.edit()
                .putString("do_notif_habit", habitTime.ifBlank { "07:00" })
                .putString("do_notif_todo", todoTime.ifBlank { "15:00" })
                .apply()
            refreshDailyOsPrefsIntoState()
        },
        onGetAiCoach = {
            checkAndRefreshCoachMemoryInBackground()
            val today = DailyOsHelpers.todayStr()
            val habits = dailyOsDao.getAllHabits()
            val todos = dailyOsDao.getAllTodos()
            val activeHabits = habits.filter { !it.archived }
            val habitSummary = if (activeHabits.isEmpty()) "Belum ada habit." else activeHabits.joinToString("\n") { h ->
                val scheduled = DailyOsHelpers.isHabitScheduled(h, today)
                val subs = DailyOsHelpers.parseSubTasks(h.subTasksJson)
                val tc = if (subs.isNotEmpty()) subs.size else 1
                val done = DailyOsHelpers.parseHistory(h.historyJson)[today].orEmpty().size
                val streak = DailyOsHelpers.calculateHabitStreak(h, today)
                val st = when {
                    !scheduled -> "tidak dijadwalkan hari ini"
                    done >= tc -> "SELESAI"
                    done > 0 -> "sebagian ($done/$tc)"
                    else -> "belum"
                }
                "- ${h.name} (${h.category}): $st${if (streak > 0) ", streak $streak hari" else ""}"
            }
            val pendingTodos = todos.filter { !it.done }
            val todoSummary = if (pendingTodos.isEmpty()) "Tidak ada task pending." else pendingTodos.joinToString("\n") { t ->
                val ov = if (DailyOsHelpers.isOverdue(t.deadline, today)) " [OVERDUE]" else ""
                "- [${t.priority}] ${t.title}${if (t.deadline.isNotBlank()) " (${t.deadline})" else ""}$ov"
            }
            dailyOsAiClient.getCoachBriefing(
                todayLabel = DailyOsHelpers.formatFullDate(today),
                habitSummary = habitSummary,
                todoSummary = todoSummary,
                memory = _uiState.value.dailyOsState.coachMemory,
                idekuIdeas = getIdekuInProgressDtos()
            )
        },
        onParseSmartTodo = { input ->
            val today = DailyOsHelpers.todayStr()
            dailyOsAiClient.parseSmartTodo(
                todayLabel = DailyOsHelpers.formatFullDate(today),
                input = input
            )
        },
        onAddParsedTodos = { tasks ->
            val existing = dailyOsDao.getAllTodos()
            var nextSort = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val baseTs = System.currentTimeMillis()
            val newTodos = tasks.mapIndexed { idx, t ->
                val subs = t.subtasks.mapIndexed { sIdx, st ->
                    DailyOsTodoSubtask(id = "ts_${baseTs}_${idx}_$sIdx", text = st.trim(), done = false)
                }
                DailyOsTodoEntity(
                    id = "todo_${baseTs}_$idx",
                    title = t.judul.trim(),
                    priority = if (t.priority in listOf("urgent", "soon", "normal")) t.priority else "normal",
                    deadline = t.deadline.trim(),
                    done = false,
                    completedAt = null,
                    subtasksJson = DailyOsHelpers.encodeTodoSubtasks(subs),
                    sortOrder = nextSort++
                )
            }
            if (newTodos.isNotEmpty()) dailyOsDao.upsertTodos(newTodos)
        },
        onGetAiRecommendations = {
            val today = DailyOsHelpers.todayStr()
            val habits = dailyOsDao.getAllHabits()
            val todos = dailyOsDao.getAllTodos()
            val stats = DailyOsHelpers.getHabitStats30Days(habits, today)
            val existingHabitsText = habits.filter { !it.archived }.joinToString(", ") { "${it.name} (${it.category})" }.ifBlank { "Belum ada habit." }
            val habitGapSummary = if (stats.isEmpty()) "Belum ada data habit." else stats.joinToString("\n") { s ->
                "- \"${s.name}\" (${s.category}): ${s.completedCount}/${s.scheduledCount} hari (${s.completionRate}%), streak ${s.currentStreak}h, skip ${s.skipCount}x" +
                    if (s.weakDays.isNotEmpty()) " (sering skip ${s.weakDays.joinToString(", ")})" else ""
            }
            val doneTodosText = todos.filter { it.done }.take(15).joinToString(", ") { it.title }.ifBlank { "Belum ada." }
            val activeTodosText = todos.filter { !it.done }.joinToString("\n") { t ->
                "- [${t.priority}] ${t.title}${if (t.deadline.isNotBlank()) " (deadline ${t.deadline})" else ""}"
            }.ifBlank { "Tidak ada." }
            dailyOsAiClient.getRecommendations(
                todayLabel = DailyOsHelpers.formatFullDate(today),
                existingHabitsText = existingHabitsText,
                habitGapSummary = habitGapSummary,
                doneTodosText = doneTodosText,
                activeTodosText = activeTodosText,
                memory = _uiState.value.dailyOsState.coachMemory,
                idekuIdeas = getIdekuInProgressDtos()
            )
        },
        onAddRecommendedTodos = { tasks ->
            val existing = dailyOsDao.getAllTodos()
            var nextSort = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val baseTs = System.currentTimeMillis()
            val newTodos = tasks.mapIndexed { idx, r ->
                DailyOsTodoEntity(
                    id = "todo_${baseTs}_$idx",
                    title = r.judul.trim(),
                    priority = if (r.priority in listOf("urgent", "soon", "normal")) r.priority else "soon",
                    deadline = r.deadline.trim(),
                    done = false,
                    completedAt = null,
                    subtasksJson = "[]",
                    sortOrder = nextSort++
                )
            }
            if (newTodos.isNotEmpty()) dailyOsDao.upsertTodos(newTodos)
        },
        onBuildExportJson = {
            DailyOsHelpers.buildBackupJson(
                habits = dailyOsDao.getAllHabits(),
                todos = dailyOsDao.getAllTodos(),
                coachMemory = _uiState.value.dailyOsState.coachMemory
            )
        },
        onImportJson = { raw ->
            runCatching {
                val (habits, todos, mem) = DailyOsHelpers.parseBackupJson(raw)
                dailyOsDao.replaceAllData(habits, todos)
                if (mem != null) {
                    dailyOsPrefs.edit()
                        .putString("do_coach_memory", DailyOsHelpers.json.encodeToString(mem))
                        .putString("do_memory_date", DailyOsHelpers.todayStr())
                        .apply()
                    refreshDailyOsPrefsIntoState()
                }
                habits.size to todos.size
            }
        },
        onExportCsv = {
            DailyOsHelpers.buildCsvExport(dailyOsDao.getAllHabits())
        },
        onBuildSummaryMarkdown = { kind, offset ->
            DailyOsHelpers.buildSummaryMarkdown(
                habits = dailyOsDao.getAllHabits(),
                todos = dailyOsDao.getAllTodos(),
                kind = kind,
                offset = offset
            )
        },
        onResetAll = {
            dailyOsDao.replaceAllData(emptyList(), emptyList())
            dailyOsPrefs.edit()
                .remove("do_coach_memory")
                .remove("do_memory_date")
                .putBoolean("do_done_open", true)
                .apply()
            refreshDailyOsPrefsIntoState()
        }
    )

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

    fun createIdekuActions() = IdekuActions(
        onSaveIdea = { editId, type, title, notes, category, priority, jalur, nextStep, price, image ->
            val existing = if (editId != null) idekuDao.getById(editId) else null
            val id = existing?.id ?: System.currentTimeMillis()
            val entity = IdekuIdeaEntity(
                id = id,
                title = title.trim(),
                notes = notes.trim(),
                category = category.ifBlank { "Personal" },
                jalur = if (type == "Wishlist") "" else jalur.trim(),
                nextStep = existing?.nextStep ?: nextStep.trim(),
                priority = if (priority in IdekuHelpers.PRIORITIES) priority else "Nanti",
                status = existing?.status ?: "Belum",
                type = if (type == "Wishlist") "Wishlist" else "Idea",
                price = if (type == "Wishlist") price.coerceAtLeast(0L) else 0L,
                image = if (type == "Wishlist") image.ifBlank { "bag" } else "bag",
                createdAt = existing?.createdAt ?: id
            )
            idekuDao.upsert(entity)
            if (entity.type == "Wishlist") {
                syncIdekuWishlistToCuciMoney(entity)
            }
        },
        onDeleteIdea = { id ->
            idekuDao.deleteById(id)
        },
        onUpdateStatus = { id, status ->
            val existing = idekuDao.getById(id) ?: return@IdekuActions
            if (status in IdekuHelpers.STATUSES) {
                val updated = existing.copy(status = status)
                idekuDao.upsert(updated)
                if (updated.type == "Wishlist") {
                    syncIdekuWishlistToCuciMoney(updated)
                }
            }
        },
        onSwapFocus = { newId, parkId ->
            val target = idekuDao.getById(newId)
            val parked = idekuDao.getById(parkId)
            if (parked != null) idekuDao.upsert(parked.copy(status = "Parkir"))
            if (target != null) idekuDao.upsert(target.copy(status = "Proses"))
        },
        onUpdatePriority = { id, priority ->
            val existing = idekuDao.getById(id) ?: return@IdekuActions
            if (priority in IdekuHelpers.PRIORITIES) {
                val updated = existing.copy(priority = priority)
                idekuDao.upsert(updated)
                if (updated.type == "Wishlist") {
                    syncIdekuWishlistToCuciMoney(updated)
                }
            }
        },
        onUpdateMeta = { id, jalur, nextStep ->
            val existing = idekuDao.getById(id) ?: return@IdekuActions
            idekuDao.upsert(existing.copy(jalur = jalur.trim(), nextStep = nextStep.trim()))
        },
        onAddCategory = { raw ->
            val clean = raw.trim()
            val cur = _uiState.value.idekuState.categories
            if (clean.isEmpty() || cur.any { it.equals(clean, ignoreCase = true) }) {
                false
            } else {
                val next = cur + clean
                idekuPrefs.edit().putString("ik_categories", IdekuHelpers.json.encodeToString(next)).apply()
                refreshIdekuPrefsIntoState()
                true
            }
        },
        onDeleteCategory = { cat ->
            val cur = _uiState.value.idekuState.categories
            if (cur.size <= 1) {
                false
            } else {
                val next = cur.filterNot { it == cat }
                idekuPrefs.edit().putString("ik_categories", IdekuHelpers.json.encodeToString(next)).apply()
                refreshIdekuPrefsIntoState()
                true
            }
        },
        onSetViewMode = { mode ->
            idekuPrefs.edit().putString("ik_view_mode", mode.name).apply()
            refreshIdekuPrefsIntoState()
        },
        onPerkayaIdea = { item ->
            idekuAiClient.generate(
                title = item.title,
                notes = item.notes,
                category = item.category,
                priority = item.priority,
                mode = "perkaya"
            )
        },
        onBreakdownIdea = { item ->
            idekuAiClient.generate(
                title = item.title,
                notes = item.notes,
                category = item.category,
                priority = item.priority,
                mode = "breakdown"
            )
        },
        onExportJson = {
            IdekuHelpers.buildExportJson(idekuDao.getAll())
        },
        onImportJson = { raw ->
            runCatching {
                val imported = IdekuHelpers.parseImportJson(raw)
                val existingIds = idekuDao.getAll().map { it.id }.toSet()
                val fresh = imported.filter { it.id !in existingIds }
                if (fresh.isNotEmpty()) {
                    idekuDao.upsertAll(fresh)
                    fresh.filter { it.type == "Wishlist" }.forEach { syncIdekuWishlistToCuciMoney(it) }
                }
                fresh.size
            }
        },
        onDeleteAll = {
            idekuDao.clearAll()
        }
    )

    private fun saveLastAccForBook(bookId: Long, accId: Long) {
        val cur = _uiState.value.cuciMoneyState.lastAccByBook.toMutableMap()
        cur[bookId] = accId
        val encoded = runCatching { CuciMoneyHelpers.json.encodeToString(cur) }.getOrDefault("{}")
        cuciPrefs.edit().putString("cm_last_acc", encoded).apply()
        refreshCuciPrefsIntoState()
    }

    fun createCuciMoneyActions() = CuciMoneyActions(
        onToggleVisibility = {
            val next = !_uiState.value.cuciMoneyState.visibility
            cuciPrefs.edit().putBoolean("cm_vis", next).apply()
            refreshCuciPrefsIntoState()
        },
        onSelectBook = { bookId ->
            cuciPrefs.edit().putLong("cm_book_id", bookId).apply()
            refreshCuciPrefsIntoState()
        },
        onCreateBook = { name ->
            val clean = name.trim()
            if (clean.isEmpty()) false
            else {
                cuciMoneyDao.upsertBook(
                    CuciBookEntity(
                        id = CuciMoneyHelpers.generateId(),
                        name = clean,
                        accountIdsJson = "[]"
                    )
                )
                true
            }
        },
        onDeleteBook = { bookId ->
            val books = cuciMoneyDao.getAllBooks()
            val curBookId = _uiState.value.cuciMoneyState.currentBookId
            val bookTrxs = cuciMoneyDao.getTransactionsByBook(bookId)
            when {
                books.size <= 1 -> "Minimal harus ada 1 buku"
                bookId == curBookId -> "Tidak dapat menghapus buku yang sedang aktif"
                bookTrxs.any { it.savingsPot != null } -> "Buku ini memiliki transaksi tabungan wishlist"
                else -> {
                    bookTrxs.forEach { t ->
                        val rev = if (t.direction == "in") -t.amount else t.amount
                        cuciMoneyDao.adjustAccountBalance(t.accountId, rev)
                    }
                    cuciMoneyDao.deleteTransactionsByBook(bookId)
                    cuciMoneyDao.deleteMasterBudgetsByBook(bookId)
                    cuciMoneyDao.deleteBookById(bookId)
                    null
                }
            }
        },
        onToggleBudgetCollapsed = {
            val next = !_uiState.value.cuciMoneyState.budgetCollapsed
            cuciPrefs.edit().putBoolean("cm_budget_collapsed", next).apply()
            refreshCuciPrefsIntoState()
        },
        onSaveTransaction = { editId, type, amount, title, categoryId, accountId, toAccountId, date, memberId ->
            val bookId = _uiState.value.cuciMoneyState.currentBookId
            if (amount <= 0L) return@CuciMoneyActions "Nominal harus lebih dari 0"

            if (editId != null) {
                val oldTrx = cuciMoneyDao.getTransactionById(editId)
                if (oldTrx?.savingsPot != null) {
                    return@CuciMoneyActions "Transaksi tabungan wishlist tidak dapat diedit"
                }
                if (oldTrx != null) {
                    if (oldTrx.transferPair != null) {
                        val pairs = cuciMoneyDao.getTransactionsByPair(oldTrx.transferPair)
                        pairs.forEach { p ->
                            val rev = if (p.direction == "in") -p.amount else p.amount
                            cuciMoneyDao.adjustAccountBalance(p.accountId, rev)
                        }
                        cuciMoneyDao.deleteTransactionsByPair(oldTrx.transferPair)
                    } else {
                        val rev = if (oldTrx.direction == "in") -oldTrx.amount else oldTrx.amount
                        cuciMoneyDao.adjustAccountBalance(oldTrx.accountId, rev)
                        cuciMoneyDao.deleteTransactionById(oldTrx.id)
                    }
                }
            }

            if (type == "transfer") {
                val targetId = toAccountId ?: return@CuciMoneyActions "Pilih akun tujuan"
                if (targetId == accountId) return@CuciMoneyActions "Akun asal dan tujuan tidak boleh sama"
                val pairId = CuciMoneyHelpers.generateId()
                val now = System.currentTimeMillis()
                val outTrx = CuciTransactionEntity(
                    id = pairId,
                    bookId = bookId,
                    title = title,
                    amount = amount,
                    direction = "out",
                    categoryId = 12L,
                    accountId = accountId,
                    date = date,
                    memberId = memberId,
                    transferPair = pairId,
                    createdAt = now
                )
                val inTrx = CuciTransactionEntity(
                    id = pairId + 1L,
                    bookId = bookId,
                    title = title,
                    amount = amount,
                    direction = "in",
                    categoryId = 12L,
                    accountId = targetId,
                    date = date,
                    memberId = memberId,
                    transferPair = pairId,
                    createdAt = now + 1L
                )
                cuciMoneyDao.upsertTransactions(listOf(outTrx, inTrx))
                cuciMoneyDao.adjustAccountBalance(accountId, -amount)
                cuciMoneyDao.adjustAccountBalance(targetId, amount)
                saveLastAccForBook(bookId, accountId)
                null
            } else {
                val dir = if (type == "income") "in" else "out"
                val trx = CuciTransactionEntity(
                    id = CuciMoneyHelpers.generateId(),
                    bookId = bookId,
                    title = title,
                    amount = amount,
                    direction = dir,
                    categoryId = categoryId,
                    accountId = accountId,
                    date = date,
                    memberId = memberId,
                    transferPair = null,
                    createdAt = System.currentTimeMillis()
                )
                cuciMoneyDao.upsertTransaction(trx)
                cuciMoneyDao.adjustAccountBalance(accountId, if (dir == "in") amount else -amount)
                saveLastAccForBook(bookId, accountId)
                null
            }
        },
        onDeleteTransaction = { trxId ->
            val trx = cuciMoneyDao.getTransactionById(trxId) ?: return@CuciMoneyActions "Transaksi tidak ditemukan"
            if (trx.savingsPot != null) {
                return@CuciMoneyActions "Transaksi tabungan wishlist hanya dapat dikelola di menu Tabungan"
            }
            if (trx.transferPair != null) {
                val pairs = cuciMoneyDao.getTransactionsByPair(trx.transferPair)
                pairs.forEach { p ->
                    val rev = if (p.direction == "in") -p.amount else p.amount
                    cuciMoneyDao.adjustAccountBalance(p.accountId, rev)
                }
                cuciMoneyDao.deleteTransactionsByPair(trx.transferPair)
            } else {
                val rev = if (trx.direction == "in") -trx.amount else trx.amount
                cuciMoneyDao.adjustAccountBalance(trx.accountId, rev)
                cuciMoneyDao.deleteTransactionById(trx.id)
            }
            null
        },
        onSaveAccount = { editId, name, type, icon, balance, notes, useInBook, includeAsset ->
            if (name.isBlank()) return@CuciMoneyActions "Nama akun wajib diisi"
            val activeBookId = _uiState.value.cuciMoneyState.currentBookId
            val existing = editId?.let { cuciMoneyDao.getAccountById(it) }
            val accId = existing?.id ?: CuciMoneyHelpers.generateId()
            cuciMoneyDao.upsertAccount(
                CuciAccountEntity(
                    id = accId,
                    name = name.trim(),
                    type = type,
                    icon = icon,
                    balance = balance,
                    notes = notes.trim(),
                    useInBook = useInBook,
                    includeAsset = includeAsset,
                    isSavings = existing?.isSavings ?: false
                )
            )
            val activeBook = cuciMoneyDao.getBookById(activeBookId)
            if (activeBook != null) {
                val ids = CuciMoneyHelpers.parseAccountIds(activeBook.accountIdsJson).toMutableList()
                if (useInBook && accId !in ids) ids.add(accId)
                else if (!useInBook) ids.removeAll { it == accId }
                cuciMoneyDao.upsertBook(activeBook.copy(accountIdsJson = CuciMoneyHelpers.encodeAccountIds(ids)))
            }
            null
        },
        onDeleteAccount = { accId ->
            val books = cuciMoneyDao.getAllBooks()
            books.forEach { b ->
                val ids = CuciMoneyHelpers.parseAccountIds(b.accountIdsJson)
                if (accId in ids) {
                    cuciMoneyDao.upsertBook(b.copy(accountIdsJson = CuciMoneyHelpers.encodeAccountIds(ids.filter { it != accId })))
                }
            }
            cuciMoneyDao.deleteAccountById(accId)
        },
        onSaveCategory = { editId, name, emoji, type ->
            val clean = name.trim()
            if (clean.isEmpty()) return@CuciMoneyActions null
            val existing = editId?.let { cuciMoneyDao.getCategoryById(it) }
            val id = existing?.id ?: CuciMoneyHelpers.generateId()
            val sortOrder = existing?.sortOrder ?: cuciMoneyDao.getAllCategories().size
            cuciMoneyDao.upsertCategory(
                CuciCategoryEntity(
                    id = id,
                    name = clean,
                    type = existing?.type ?: type,
                    emoji = emoji.ifBlank { "📌" },
                    sortOrder = sortOrder
                )
            )
            id
        },
        onDeleteCategory = { catId ->
            cuciMoneyDao.deleteCategoryById(catId)
        },
        onMoveCategory = { id, delta, filterType ->
            val all = cuciMoneyDao.getAllCategories().sortedBy { it.sortOrder }.toMutableList()
            val filtered = all.filter { it.type == filterType || it.type == "both" }
            val posInFiltered = filtered.indexOfFirst { it.id == id }
            val targetPos = posInFiltered + delta
            if (posInFiltered != -1 && targetPos in filtered.indices) {
                val a = filtered[posInFiltered]
                val b = filtered[targetPos]
                cuciMoneyDao.upsertCategories(
                    listOf(
                        a.copy(sortOrder = b.sortOrder),
                        b.copy(sortOrder = a.sortOrder)
                    )
                )
            }
        },
        onSaveMember = { editId, name, emoji ->
            val clean = name.trim()
            if (clean.isNotEmpty()) {
                val existing = editId?.let { cuciMoneyDao.getMemberById(it) }
                val id = existing?.id ?: "m_${CuciMoneyHelpers.generateId()}"
                val sortOrder = existing?.sortOrder ?: cuciMoneyDao.getAllMembers().size
                cuciMoneyDao.upsertMember(
                    CuciMemberEntity(
                        id = id,
                        name = clean,
                        emoji = emoji.ifBlank { "😄" },
                        sortOrder = sortOrder
                    )
                )
            }
        },
        onDeleteMember = { memberId ->
            if (memberId != "m1") {
                cuciMoneyDao.deleteMemberById(memberId)
            }
        },
        onMoveMember = { id, delta ->
            val all = cuciMoneyDao.getAllMembers().sortedBy { it.sortOrder }
            val idx = all.indexOfFirst { it.id == id }
            val target = idx + delta
            if (idx != -1 && target in all.indices) {
                val a = all[idx]
                val b = all[target]
                cuciMoneyDao.upsertMembers(
                    listOf(
                        a.copy(sortOrder = b.sortOrder),
                        b.copy(sortOrder = a.sortOrder)
                    )
                )
            }
        },
        onSaveMasterBudget = { month, year, limitAmount, startDate, endDate, note ->
            if (limitAmount <= 0L) return@CuciMoneyActions "Nominal budget harus lebih dari 0"
            val bookId = _uiState.value.cuciMoneyState.currentBookId
            val existing = cuciMoneyDao.getAllMasterBudgets().firstOrNull {
                it.bookId == bookId && it.month == month && it.year == year
            }
            cuciMoneyDao.upsertMasterBudget(
                CuciMasterBudgetEntity(
                    id = existing?.id ?: CuciMoneyHelpers.generateId(),
                    bookId = bookId,
                    month = month,
                    year = year,
                    limitAmount = limitAmount,
                    startDate = startDate,
                    endDate = endDate,
                    note = note
                )
            )
            null
        },
        onDeleteMasterBudget = { month, year ->
            val bookId = _uiState.value.cuciMoneyState.currentBookId
            val existing = cuciMoneyDao.getAllMasterBudgets().firstOrNull {
                it.bookId == bookId && it.month == month && it.year == year
            }
            if (existing != null) cuciMoneyDao.deleteMasterBudgetById(existing.id)
        },
        onSaveWishlist = { editId, title, price, priority, image ->
            val existing = editId?.let { cuciMoneyDao.getWishlistById(it) }
            val id = existing?.id ?: "wish_${CuciMoneyHelpers.generateId()}"
            cuciMoneyDao.upsertWishlist(
                CuciWishlistEntity(
                    id = id,
                    title = title.trim(),
                    price = price,
                    priority = priority,
                    image = image,
                    status = existing?.status ?: "Belum",
                    saved = existing?.saved ?: 0L,
                    realizedAt = existing?.realizedAt,
                    realizedAmount = existing?.realizedAmount ?: 0L,
                    historyJson = existing?.historyJson ?: "[]",
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
            )
        },
        onDeleteWishlist = { wishId ->
            val wish = cuciMoneyDao.getWishlistById(wishId) ?: return@CuciMoneyActions "Wishlist tidak ditemukan"
            if (wish.saved > 0L) {
                return@CuciMoneyActions "Kembalikan saldo tabungan terlebih dahulu sebelum menghapus"
            }
            cuciMoneyDao.deleteWishlistById(wishId)
            null
        },
        onDepositWishlist = { wishId, accountId, amount ->
            if (amount <= 0L) return@CuciMoneyActions "Nominal setoran harus lebih dari 0"
            val wish = cuciMoneyDao.getWishlistById(wishId) ?: return@CuciMoneyActions "Wishlist tidak ditemukan"
            val srcAcc = cuciMoneyDao.getAccountById(accountId) ?: return@CuciMoneyActions "Akun sumber tidak ditemukan"
            var savingsAcc = cuciMoneyDao.getAllAccounts().firstOrNull { it.isSavings }
            if (savingsAcc == null) {
                savingsAcc = CuciAccountEntity(
                    id = CuciMoneyHelpers.generateId(),
                    name = "Tabungan",
                    type = "saving",
                    icon = "🐷",
                    balance = 0L,
                    notes = "Tabungan Wishlist",
                    useInBook = false,
                    includeAsset = true,
                    isSavings = true
                )
                cuciMoneyDao.upsertAccount(savingsAcc)
            }
            val bookId = _uiState.value.cuciMoneyState.currentBookId
            val pairId = CuciMoneyHelpers.generateId()
            val today = CuciMoneyHelpers.todayDateStr()
            val now = System.currentTimeMillis()
            val outTrx = CuciTransactionEntity(
                id = pairId,
                bookId = bookId,
                title = "Tabungan: ${wish.title}",
                amount = amount,
                direction = "out",
                categoryId = 12L,
                accountId = srcAcc.id,
                date = today,
                memberId = "m1",
                transferPair = pairId,
                savingsPot = wish.id,
                createdAt = now
            )
            val inTrx = CuciTransactionEntity(
                id = pairId + 1L,
                bookId = bookId,
                title = "Tabungan: ${wish.title}",
                amount = amount,
                direction = "in",
                categoryId = 12L,
                accountId = savingsAcc.id,
                date = today,
                memberId = "m1",
                transferPair = pairId,
                savingsPot = wish.id,
                createdAt = now + 1L
            )
            cuciMoneyDao.upsertTransactions(listOf(outTrx, inTrx))
            cuciMoneyDao.adjustAccountBalance(srcAcc.id, -amount)
            cuciMoneyDao.adjustAccountBalance(savingsAcc.id, amount)

            val hist = CuciMoneyHelpers.parseWishlistHistory(wish.historyJson) + CuciSavingsHistoryDto(
                id = pairId,
                type = "in",
                amount = amount,
                date = today,
                accountId = srcAcc.id,
                accountName = srcAcc.name,
                ts = now,
                trxIds = listOf(outTrx.id, inTrx.id)
            )
            cuciMoneyDao.upsertWishlist(
                wish.copy(
                    saved = wish.saved + amount,
                    historyJson = CuciMoneyHelpers.encodeWishlistHistory(hist)
                )
            )
            null
        },
        onWithdrawWishlist = { wishId, accountId, realize, markDone ->
            val wish = cuciMoneyDao.getWishlistById(wishId) ?: return@CuciMoneyActions "Wishlist tidak ditemukan"
            val amt = wish.saved
            if (amt <= 0L) return@CuciMoneyActions "Saldo tabungan wishlist ini kosong"
            val dstAcc = cuciMoneyDao.getAccountById(accountId) ?: return@CuciMoneyActions "Akun tujuan tidak ditemukan"
            val savingsAcc = cuciMoneyDao.getAllAccounts().firstOrNull { it.isSavings }
                ?: return@CuciMoneyActions "Akun Tabungan tidak ditemukan"

            val bookId = _uiState.value.cuciMoneyState.currentBookId
            val pairId = CuciMoneyHelpers.generateId()
            val today = CuciMoneyHelpers.todayDateStr()
            val now = System.currentTimeMillis()
            val label = if (realize) "Cairkan Tabungan: ${wish.title}" else "Batal Tabungan: ${wish.title}"
            val outTrx = CuciTransactionEntity(
                id = pairId,
                bookId = bookId,
                title = label,
                amount = amt,
                direction = "out",
                categoryId = 12L,
                accountId = savingsAcc.id,
                date = today,
                memberId = "m1",
                transferPair = pairId,
                savingsPot = wish.id,
                createdAt = now
            )
            val inTrx = CuciTransactionEntity(
                id = pairId + 1L,
                bookId = bookId,
                title = label,
                amount = amt,
                direction = "in",
                categoryId = 12L,
                accountId = dstAcc.id,
                date = today,
                memberId = "m1",
                transferPair = pairId,
                savingsPot = wish.id,
                createdAt = now + 1L
            )
            cuciMoneyDao.upsertTransactions(listOf(outTrx, inTrx))
            cuciMoneyDao.adjustAccountBalance(savingsAcc.id, -amt)
            cuciMoneyDao.adjustAccountBalance(dstAcc.id, amt)

            val hist = CuciMoneyHelpers.parseWishlistHistory(wish.historyJson) + CuciSavingsHistoryDto(
                id = pairId,
                type = "out",
                kind = if (realize) "realize" else "cancel",
                amount = amt,
                date = today,
                accountId = dstAcc.id,
                accountName = dstAcc.name,
                ts = now,
                trxIds = listOf(outTrx.id, inTrx.id)
            )
            cuciMoneyDao.upsertWishlist(
                wish.copy(
                    saved = 0L,
                    status = if (realize && markDone) "Selesai" else wish.status,
                    realizedAt = if (realize) now else wish.realizedAt,
                    realizedAmount = if (realize) amt else wish.realizedAmount,
                    historyJson = CuciMoneyHelpers.encodeWishlistHistory(hist)
                )
            )
            null
        },
        onExportCsv = {
            val state = _uiState.value.cuciMoneyState
            val bookTrx = state.transactions.filter { it.bookId == state.currentBookId }
            CuciMoneyHelpers.buildCsvExport(bookTrx, state.categories, state.accounts)
        },
        onExportBackupJson = {
            CuciMoneyHelpers.buildBackupJson(_uiState.value.cuciMoneyState)
        },
        onImportBackupJson = { raw ->
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
                cuciMoneyDao.replaceAllData(books, accounts, categories, members, transactions, budgets, wishlists)
                cuciPrefs.edit().putLong("cm_book_id", books.first().id).apply()
                refreshCuciPrefsIntoState()
                transactions.size
            }
        },
        onResetAllData = {
            resetCuciMoneyData()
        }
    )

    private fun refreshLinkkuPrefsIntoState() {
        val hasPin = !linkkuPrefs.getString("lk_pin", null).isNullOrBlank()
        val extraNormal = LinkkuHelpers.parseTags(linkkuPrefs.getString("lk_tags_normal", "[]").orEmpty())
        val extraPriv = LinkkuHelpers.parseTags(linkkuPrefs.getString("lk_tags_priv", "[]").orEmpty())
        _uiState.update { state ->
            state.copy(
                linkkuState = state.linkkuState.copy(
                    hasPin = hasPin,
                    extraTagsNormal = extraNormal,
                    extraTagsPriv = extraPriv
                )
            )
        }
    }

    fun createLinkkuActions() = LinkkuActions(
        onSaveLink = { editId, url, title, note, catId, tags, isPrivate ->
            val domain = LinkkuHelpers.extractDomain(url)
            val resolvedTitle = title.ifBlank { domain }
            val existing = if (editId != null) linkkuDao.getById(editId) else null
            val initialThumb = existing?.thumb?.takeIf { it.isNotBlank() } ?: LinkkuHelpers.youtubeThumb(url)
            val id = existing?.id ?: LinkkuHelpers.generateId()
            val entity = LinkkuLinkEntity(
                id = id,
                url = url,
                title = resolvedTitle,
                note = note,
                cat = if (isPrivate) "" else catId,
                pcat = if (isPrivate) catId else "",
                tagsJson = LinkkuHelpers.encodeTags(tags),
                isPrivate = isPrivate,
                thumb = initialThumb,
                ts = existing?.ts ?: System.currentTimeMillis()
            )
            linkkuDao.upsertLink(entity)
            if (existing == null) {
                viewModelScope.launch {
                    val meta = linkkuMetaClient.fetchMetadata(url)
                    val cur = linkkuDao.getById(id) ?: return@launch
                    val updatedTitle = if (title.isBlank() && meta.title.isNotBlank()) meta.title else cur.title
                    val updatedThumb = meta.thumb.ifBlank { cur.thumb }
                    if (updatedTitle != cur.title || updatedThumb != cur.thumb) {
                        linkkuDao.upsertLink(cur.copy(title = updatedTitle, thumb = updatedThumb))
                    }
                }
            }
        },
        onDeleteLink = { id ->
            viewModelScope.launch { linkkuDao.deleteLinkById(id) }
        },
        onAddCategory = { name, isPrivate ->
            val slug = name.trim().lowercase().replace(Regex("\\s+"), "-")
            val prefix = if (isPrivate) "pc-" else ""
            val id = "$prefix$slug-${LinkkuHelpers.generateId().take(4)}"
            val count = linkkuDao.getAllCategories().count { it.isPrivate == isPrivate }
            linkkuDao.upsertCategory(
                LinkkuCategoryEntity(
                    id = id,
                    name = name.trim(),
                    isPrivate = isPrivate,
                    sortOrder = count
                )
            )
        },
        onDeleteCategory = { id, isPrivate ->
            viewModelScope.launch {
                linkkuDao.deleteCategoryById(id)
                if (isPrivate) linkkuDao.clearCategoryFromPrivateLinks(id)
                else linkkuDao.clearCategoryFromPublicLinks(id)
            }
        },
        onAddExtraTag = { tag, isPrivate ->
            val clean = tag.trim()
            if (clean.isNotEmpty()) {
                val key = if (isPrivate) "lk_tags_priv" else "lk_tags_normal"
                val cur = LinkkuHelpers.parseTags(linkkuPrefs.getString(key, "[]").orEmpty())
                if (clean !in cur) {
                    linkkuPrefs.edit().putString(key, LinkkuHelpers.encodeTags(cur + clean)).apply()
                    refreshLinkkuPrefsIntoState()
                }
            }
        },
        onDeleteTagGlobally = { tag, isPrivate ->
            viewModelScope.launch {
                val key = if (isPrivate) "lk_tags_priv" else "lk_tags_normal"
                val curExtra = LinkkuHelpers.parseTags(linkkuPrefs.getString(key, "[]").orEmpty())
                linkkuPrefs.edit().putString(key, LinkkuHelpers.encodeTags(curExtra.filter { it != tag })).apply()
                val updatedLinks = linkkuDao.getAllLinks()
                    .filter { it.isPrivate == isPrivate }
                    .mapNotNull { link ->
                        val tags = LinkkuHelpers.parseTags(link.tagsJson)
                        if (tag in tags) link.copy(tagsJson = LinkkuHelpers.encodeTags(tags.filter { it != tag }))
                        else null
                    }
                if (updatedLinks.isNotEmpty()) linkkuDao.upsertLinks(updatedLinks)
                refreshLinkkuPrefsIntoState()
            }
        },
        onFetchMeta = { url -> linkkuMetaClient.fetchMetadata(url) },
        onLoadBitmap = { url -> linkkuMetaClient.loadBitmap(url) },
        onVerifyPin = { pin ->
            val savedHash = linkkuPrefs.getString("lk_pin", null)
            !savedHash.isNullOrBlank() && LinkkuCrypto.hashPin(pin) == savedHash
        },
        onSetPin = { newPin ->
            val hash = LinkkuCrypto.hashPin(newPin)
            linkkuPrefs.edit().putString("lk_pin", hash).apply()
            refreshLinkkuPrefsIntoState()
        },
        onExportJson = {
            LinkkuHelpers.buildExportJson(
                links = linkkuDao.getAllLinks(),
                categories = linkkuDao.getAllCategories(),
                pinHash = linkkuPrefs.getString("lk_pin", null)
            )
        },
        onImportJson = { raw, rawPin ->
            runCatching {
                val wrapper = LinkkuHelpers.json.decodeFromString<LinkkuBackupWrapperDto>(raw)
                val pubDtos = wrapper.L.ifEmpty { wrapper.links }
                val hasPrivate = !wrapper.ep.isNullOrBlank()
                if (hasPrivate && rawPin.isNullOrBlank()) {
                    return@runCatching LinkkuImportResult.NeedsPin(raw)
                }

                var incomingLinks = pubDtos.map { it.toEntity() }
                var incomingPrivCats = wrapper.PC.map {
                    LinkkuCategoryEntity(id = it.id, name = it.n, isPrivate = true)
                }

                if (hasPrivate && !rawPin.isNullOrBlank()) {
                    val pinHash = LinkkuCrypto.hashPin(rawPin)
                    val decJson = LinkkuCrypto.decryptPrivatePayload(wrapper.ep!!, pinHash)
                    val privPayload = LinkkuHelpers.json.decodeFromString<LinkkuEncryptedPrivatePayloadDto>(decJson)
                    incomingLinks = incomingLinks + privPayload.links.map { it.toEntity() }
                    incomingPrivCats = privPayload.cats.map {
                        LinkkuCategoryEntity(id = it.id, name = it.n, isPrivate = true)
                    }
                    linkkuPrefs.edit().putString("lk_pin", pinHash).apply()
                    refreshLinkkuPrefsIntoState()
                }

                val existingLinkIds = linkkuDao.getAllLinks().map { it.id }.toSet()
                val newLinks = incomingLinks.filter { it.id !in existingLinkIds }
                if (newLinks.isNotEmpty()) linkkuDao.upsertLinks(newLinks)

                val existingCatIds = linkkuDao.getAllCategories().map { it.id }.toSet()
                val pubCats = wrapper.C.ifEmpty { wrapper.cats }
                    .map { LinkkuCategoryEntity(id = it.id, name = it.n, isPrivate = false) }
                val newCats = (pubCats + incomingPrivCats).filter { it.id.isNotBlank() && it.id !in existingCatIds }
                if (newCats.isNotEmpty()) linkkuDao.upsertCategories(newCats)

                LinkkuImportResult.Success(newLinks.size)
            }.getOrElse { err ->
                LinkkuImportResult.Error("Gagal decrypt atau format JSON tidak valid: ${err.message}")
            }
        },
        onResetLinkku = ::resetLinkkuData
    )

    fun dismissKronikBanner() {
        _uiState.update { it.copy(kronikBannerMessage = null) }
    }

    fun consumeOpenApp() {
        _uiState.update { it.copy(pendingOpenApp = null) }
    }

    fun createKronikActions() = KronikActions(
        onCreateEntry = {
            val now = KronikHelpers.nowIsoStr()
            val entry = KronikEntryEntity(
                id = KronikHelpers.generateId(),
                date = KronikHelpers.todayDateStr(),
                title = "",
                content = "",
                sortOrder = 0,
                source = "manual",
                createdAt = now,
                updatedAt = now
            )
            kronikDao.upsert(entry)
            entry
        },
        onUpdateEntry = { id, title, content, date ->
            kronikSaveJob?.cancel()
            kronikSaveJob = viewModelScope.launch {
                if (date == null) delay(450L)
                val existing = kronikDao.getById(id) ?: return@launch
                kronikDao.upsert(
                    existing.copy(
                        title = title ?: existing.title,
                        content = content ?: existing.content,
                        date = date ?: existing.date,
                        updatedAt = KronikHelpers.nowIsoStr()
                    )
                )
            }
        },
        onFlushEntry = { id, title, content, date ->
            kronikSaveJob?.cancel()
            viewModelScope.launch {
                val existing = kronikDao.getById(id) ?: return@launch
                if (existing.title != title || existing.content != content || existing.date != date) {
                    kronikDao.upsert(
                        existing.copy(
                            title = title,
                            content = content,
                            date = date,
                            updatedAt = KronikHelpers.nowIsoStr()
                        )
                    )
                }
            }
        },
        onTogglePin = { id ->
            viewModelScope.launch {
                val existing = kronikDao.getById(id) ?: return@launch
                kronikDao.upsert(
                    existing.copy(
                        pinned = !existing.pinned,
                        updatedAt = KronikHelpers.nowIsoStr()
                    )
                )
            }
        },
        onArchiveEntry = { id ->
            viewModelScope.launch {
                val now = KronikHelpers.nowIsoStr()
                kronikDao.archiveById(id = id, archivedAt = now, updatedAt = now)
            }
        },
        onDeleteEntries = { ids ->
            if (ids.isNotEmpty()) {
                viewModelScope.launch {
                    kronikDao.deleteByIds(ids.toList())
                }
            }
        },
        onSetMood = { id, mood ->
            viewModelScope.launch {
                val existing = kronikDao.getById(id) ?: return@launch
                kronikDao.upsert(
                    existing.copy(
                        moodStatus = mood?.status,
                        moodSvgKey = mood?.svgKey,
                        updatedAt = KronikHelpers.nowIsoStr()
                    )
                )
            }
        },
        onDetectMoodAi = { text -> kronikMoodClient.detectMood(text) },
        onExportJson = {
            KronikHelpers.buildExportJson(kronikDao.getAllEntries())
        },
        onImportJson = { raw ->
            val imported = KronikHelpers.parseImportEntries(raw)
            val existingIds = kronikDao.getAllEntries().map { it.id }.toSet()
            val fresh = imported.filter { it.id !in existingIds }
            if (fresh.isNotEmpty()) {
                kronikDao.upsertAll(fresh)
            }
            fresh.size
        },
        onResetKronik = ::resetKronikData
    )

    fun setForegroundActive(active: Boolean) {
        isForeground = active
        stageController.setActive(isForeground && isAsistenTabActive)
        if (!active) {
            idleTimerJob?.cancel()
            idleTimerJob = null
            hideIdleBubble()
            audioManager.stop()
        } else if (isAsistenTabActive) {
            resetIdleTimer()
        }
    }

    fun setAsistenTabActive(active: Boolean) {
        isAsistenTabActive = active
        stageController.setActive(isForeground && isAsistenTabActive)
        if (active && _uiState.value.modelError == "renderer_gone") {
            retryModel()
        }
        if (!active) {
            idleTimerJob?.cancel()
            idleTimerJob = null
            hideIdleBubble()
            audioManager.stop()
        } else if (isForeground) {
            resetIdleTimer()
        }
    }

    /** Buat ulang WebView Live2D (dipakai tombol "Muat Ulang" saat model gagal tampil). */
    fun retryModel() {
        _uiState.update {
            it.copy(
                isModelReady = false,
                modelError = null,
                stageGeneration = it.stageGeneration + 1
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty() || _uiState.value.isThinking) return

        resetIdleTimer()
        hideIdleBubble()
        audioManager.stop()

        _inputText.value = ""
        _streamingText.value = ""
        _uiState.update {
            it.copy(
                isThinking = true,
                errorMessage = null
            )
        }
        applyExpression("puzzle", autoRevert = false)

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            val userMsgId = chatDao.insert(ChatMessageEntity(role = "user", content = text))

            val recent = chatDao.getRecentDesc(10)
                .reversed()
                .map { ChatTurnDto(role = it.role, content = it.content) }

            val personalizedPrompt = buildPersonalizedPrompt()

            val rawAcc = StringBuilder()
            var finalDoneText = ""
            var finalDoneTts = ""
            var streamErr: String? = null

            sseClient.streamChat(
                messages = recent,
                systemPrompt = personalizedPrompt,
                currentExpression = _uiState.value.currentExpression
            ).collect { event ->
                when (event) {
                    is ChatStreamEvent.Delta -> {
                        rawAcc.append(event.text)
                        val visible = SseTagParser.visibleTextDuringStream(rawAcc.toString())
                        _streamingText.value = visible
                    }
                    is ChatStreamEvent.Done -> {
                        finalDoneText = event.text
                        finalDoneTts = event.tts
                    }
                    is ChatStreamEvent.Error -> {
                        streamErr = event.message
                    }
                }
            }

            if (streamErr != null) {
                if (_uiState.value.currentExpression == "puzzle") {
                    applyExpression("none", autoRevert = false)
                }
                _streamingText.value = ""
                _uiState.update {
                    it.copy(
                        isThinking = false,
                        errorMessage = "Gagal menghubungi server: $streamErr"
                    )
                }
                return@launch
            }

            val fallbackRaw = SseTagParser.visibleTextDuringStream(rawAcc.toString())
            val parsed = SseTagParser.parseFinal(finalDoneText.ifBlank { fallbackRaw })
            val replyText = parsed.cleanText
            val expr = parsed.expression

            if (replyText.isNotEmpty()) {
                chatDao.insert(
                    ChatMessageEntity(
                        role = "assistant",
                        content = replyText,
                        expressionTag = expr
                    )
                )
            }

            if (parsed.kronikActions.isNotEmpty()) {
                val savedTitles = mutableListOf<String>()
                parsed.kronikActions.forEach { act ->
                    when (act) {
                        is SseTagParser.KronikChatAction.AddEntry -> {
                            val now = KronikHelpers.nowIsoStr()
                            val mood = KronikMood.fromStatus(act.mood)
                            val entry = KronikEntryEntity(
                                id = KronikHelpers.generateId(),
                                date = KronikHelpers.todayDateStr(),
                                title = act.title.ifBlank { "Catatan Rem" },
                                content = act.content,
                                moodStatus = mood?.status,
                                moodSvgKey = mood?.svgKey,
                                source = "rem_chat",
                                createdAt = now,
                                updatedAt = now
                            )
                            kronikDao.upsert(entry)
                            savedTitles.add(entry.title)
                        }
                    }
                }
                if (savedTitles.isNotEmpty()) {
                    _uiState.update {
                        it.copy(kronikBannerMessage = "Kronik: Tersimpan \"${savedTitles.first()}\"")
                    }
                    viewModelScope.launch {
                        delay(4_000L)
                        _uiState.update { it.copy(kronikBannerMessage = null) }
                    }
                }
            }

            if (parsed.linkkuActions.isNotEmpty()) {
                val savedTitles = mutableListOf<String>()
                val allCats = linkkuDao.getAllCategories().filter { !it.isPrivate }
                parsed.linkkuActions.forEach { act ->
                    when (act) {
                        is SseTagParser.LinkkuChatAction.AddLink -> {
                            val id = LinkkuHelpers.generateId()
                            val domain = LinkkuHelpers.extractDomain(act.url)
                            val matchedCat = allCats.firstOrNull {
                                it.name.equals(act.category, ignoreCase = true) || it.id.equals(act.category, ignoreCase = true)
                            }?.id.orEmpty()
                            val entity = LinkkuLinkEntity(
                                id = id,
                                url = act.url,
                                title = act.title.ifBlank { domain },
                                note = act.note,
                                cat = matchedCat,
                                tagsJson = LinkkuHelpers.encodeTags(act.tags),
                                isPrivate = false,
                                thumb = LinkkuHelpers.youtubeThumb(act.url),
                                ts = System.currentTimeMillis()
                            )
                            linkkuDao.upsertLink(entity)
                            savedTitles.add(entity.title)
                            viewModelScope.launch {
                                val meta = linkkuMetaClient.fetchMetadata(act.url)
                                val cur = linkkuDao.getById(id) ?: return@launch
                                val nextTitle = if (act.title.isBlank() && meta.title.isNotBlank()) meta.title else cur.title
                                val nextThumb = meta.thumb.ifBlank { cur.thumb }
                                if (nextTitle != cur.title || nextThumb != cur.thumb) {
                                    linkkuDao.upsertLink(cur.copy(title = nextTitle, thumb = nextThumb))
                                }
                            }
                        }
                    }
                }
                if (savedTitles.isNotEmpty()) {
                    _uiState.update {
                        it.copy(kronikBannerMessage = "LinkKu: Tersimpan \"${savedTitles.first()}\"")
                    }
                    viewModelScope.launch {
                        delay(4_000L)
                        _uiState.update { it.copy(kronikBannerMessage = null) }
                    }
                }
            }

            if (parsed.cuciActions.isNotEmpty()) {
                val cuciState = _uiState.value.cuciMoneyState
                val activeBook = CuciMoneyHelpers.activeBook(cuciState)
                val bookId = activeBook?.id ?: cuciState.currentBookId
                val bookAccs = CuciMoneyHelpers.bookAccounts(cuciState, bookId)
                val fallbackAccId = cuciState.lastAccByBook[bookId]
                    ?.takeIf { id -> bookAccs.any { it.id == id } }
                    ?: bookAccs.firstOrNull()?.id
                    ?: cuciState.accounts.firstOrNull { !it.isSavings }?.id
                    ?: 1L
                val today = CuciMoneyHelpers.todayDateStr()
                val savedLabels = mutableListOf<String>()

                parsed.cuciActions.forEach { act ->
                    when (act) {
                        is SseTagParser.CuciChatAction.AddTransaction -> {
                            val dir = if (act.direction == "in") "in" else "out"
                            val matchedCat = cuciState.categories.firstOrNull { c ->
                                act.categoryName.isNotBlank() &&
                                    c.name.equals(act.categoryName, ignoreCase = true) &&
                                    (c.type == "both" || (dir == "in" && c.type == "income") || (dir == "out" && c.type == "expense"))
                            } ?: cuciState.categories.firstOrNull { c ->
                                (dir == "in" && c.type == "income") || (dir == "out" && c.type == "expense")
                            } ?: cuciState.categories.firstOrNull()
                            val matchedAccId = cuciState.accounts.firstOrNull { a ->
                                !a.isSavings && act.accountName.isNotBlank() && a.name.contains(act.accountName, ignoreCase = true)
                            }?.id ?: fallbackAccId

                            val trx = CuciTransactionEntity(
                                id = CuciMoneyHelpers.generateId(),
                                bookId = bookId,
                                title = act.title,
                                amount = act.amount,
                                direction = dir,
                                categoryId = matchedCat?.id ?: 1L,
                                accountId = matchedAccId,
                                date = today,
                                memberId = cuciState.members.firstOrNull()?.id ?: "m1",
                                transferPair = null,
                                createdAt = System.currentTimeMillis()
                            )
                            cuciMoneyDao.upsertTransaction(trx)
                            cuciMoneyDao.adjustAccountBalance(matchedAccId, if (dir == "in") act.amount else -act.amount)
                            saveLastAccForBook(bookId, matchedAccId)
                            savedLabels.add("${act.title} (${CuciMoneyHelpers.formatRp(act.amount)})")
                        }
                    }
                }
                if (savedLabels.isNotEmpty()) {
                    _uiState.update {
                        it.copy(kronikBannerMessage = "CuciMoney+: Tercatat ${savedLabels.first()}")
                    }
                    viewModelScope.launch {
                        delay(4_000L)
                        _uiState.update { it.copy(kronikBannerMessage = null) }
                    }
                }
            }

            if (parsed.idekuActions.isNotEmpty()) {
                val savedTitles = mutableListOf<String>()
                var nextId = System.currentTimeMillis()
                parsed.idekuActions.forEach { act ->
                    when (act) {
                        is SseTagParser.IdekuChatAction.AddItem -> {
                            val id = nextId++
                            val entity = IdekuIdeaEntity(
                                id = id,
                                title = act.title,
                                notes = act.notes,
                                category = act.category.ifBlank { if (act.type == "Wishlist") "Belanja" else "Personal" },
                                jalur = if (act.type == "Wishlist") "" else act.jalur,
                                nextStep = "",
                                priority = act.priority,
                                status = "Belum",
                                type = act.type,
                                price = if (act.type == "Wishlist") act.price.coerceAtLeast(0L) else 0L,
                                image = "bag",
                                createdAt = id
                            )
                            idekuDao.upsert(entity)
                            if (entity.type == "Wishlist") {
                                syncIdekuWishlistToCuciMoney(entity)
                            }
                            savedTitles.add(entity.title)
                        }
                    }
                }
                if (savedTitles.isNotEmpty()) {
                    _uiState.update {
                        it.copy(kronikBannerMessage = "IdeKu: Tersimpan \"${savedTitles.first()}\"")
                    }
                    viewModelScope.launch {
                        delay(4_000L)
                        _uiState.update { it.copy(kronikBannerMessage = null) }
                    }
                }
            }

            if (parsed.dailyOsActions.isNotEmpty()) {
                val savedMsgs = mutableListOf<String>()
                var nextHabitSort = (dailyOsDao.getAllHabits().maxOfOrNull { it.sortOrder } ?: -1) + 1
                var nextTodoSort = (dailyOsDao.getAllTodos().maxOfOrNull { it.sortOrder } ?: -1) + 1
                val today = DailyOsHelpers.todayStr()
                var baseTs = System.currentTimeMillis()
                parsed.dailyOsActions.forEach { act ->
                    when (act) {
                        is SseTagParser.DailyOsChatAction.AddHabit -> {
                            val entity = DailyOsHabitEntity(
                                id = "hab_${baseTs++}",
                                name = act.name,
                                category = act.category,
                                color = "#1A1917",
                                icon = act.icon,
                                activeDaysJson = "[]",
                                subTasksJson = "[]",
                                historyJson = "{}",
                                collapsed = false,
                                archived = false,
                                createdAt = today,
                                sortOrder = nextHabitSort++
                            )
                            dailyOsDao.upsertHabit(entity)
                            savedMsgs.add("Habit \"${act.name}\"")
                        }
                        is SseTagParser.DailyOsChatAction.AddTodo -> {
                            val entity = DailyOsTodoEntity(
                                id = "todo_${baseTs++}",
                                title = act.title,
                                priority = act.priority,
                                deadline = act.deadline,
                                done = false,
                                completedAt = null,
                                subtasksJson = "[]",
                                sortOrder = nextTodoSort++
                            )
                            dailyOsDao.upsertTodo(entity)
                            savedMsgs.add("To-Do \"${act.title}\"")
                        }
                        is SseTagParser.DailyOsChatAction.CheckItem -> {
                            val q = act.query.trim()
                            val matchedHabit = dailyOsDao.getAllHabits()
                                .firstOrNull { !it.archived && it.name.contains(q, ignoreCase = true) }
                            if (matchedHabit != null) {
                                val subs = DailyOsHelpers.parseSubTasks(matchedHabit.subTasksJson)
                                val subId = subs.firstOrNull()?.id ?: "main"
                                val labelTxt = subs.firstOrNull()?.text ?: matchedHabit.name
                                val hist = DailyOsHelpers.parseHistory(matchedHabit.historyJson).toMutableMap()
                                val list = hist[today].orEmpty().toMutableList()
                                if (list.none { it.id == subId }) {
                                    list.add(DailyOsHistoryLog(id = subId, text = labelTxt, completedAt = Instant.now().toString()))
                                    hist[today] = list
                                    dailyOsDao.upsertHabit(matchedHabit.copy(historyJson = DailyOsHelpers.encodeHistory(hist)))
                                    savedMsgs.add("Checklist \"${matchedHabit.name}\"")
                                }
                            } else {
                                val matchedTodo = dailyOsDao.getAllTodos()
                                    .firstOrNull { !it.done && it.title.contains(q, ignoreCase = true) }
                                if (matchedTodo != null) {
                                    dailyOsDao.upsertTodo(matchedTodo.copy(done = true, completedAt = Instant.now().toString()))
                                    savedMsgs.add("Selesai \"${matchedTodo.title}\"")
                                }
                            }
                        }
                    }
                }
                if (savedMsgs.isNotEmpty()) {
                    _uiState.update {
                        it.copy(kronikBannerMessage = "Daily OS: ${savedMsgs.first()}")
                    }
                    viewModelScope.launch {
                        delay(4_000L)
                        _uiState.update { it.copy(kronikBannerMessage = null) }
                    }
                }
            }

            if (parsed.openApp in setOf("kronik", "linkku", "cucimoney", "ideku", "dailyos")) {
                _uiState.update { it.copy(pendingOpenApp = parsed.openApp) }
            }

            if (expr != null) {
                applyExpression(expr, autoRevert = expr != "none")
            } else if (_uiState.value.currentExpression == "puzzle") {
                applyExpression("none", autoRevert = false)
            }

            _streamingText.value = ""
            _uiState.update { it.copy(isThinking = false) }

            val ttsTarget = finalDoneTts.ifBlank { replyText }
            if (ttsTarget.isNotBlank()) {
                audioManager.playTts(ttsTarget, isJapanese = finalDoneTts.isNotBlank())
            }

            if (_uiState.value.autoLearnMemory && replyText.isNotEmpty()) {
                extractAndPersistMemories(sourceMessageId = userMsgId)
            }
        }
    }

    private suspend fun buildPersonalizedPrompt(): String {
        val topMemories = userMemoryDao.getTopMemories(20)
        val latestSummaries = userMemoryDao.getLatestSummaries(3)
        val recentJournals = kronikDao.getVisibleEntries(3)
        val recentPublicLinks = linkkuDao.getPublicLinks(4)
        val cuciState = _uiState.value.cuciMoneyState
        val cuciBook = CuciMoneyHelpers.activeBook(cuciState)
        val cuciBookId = cuciBook?.id ?: cuciState.currentBookId
        val cuciBal = CuciMoneyHelpers.activeBookBalance(cuciState)
        val cuciTodayExp = CuciMoneyHelpers.activeBookTodayExpense(cuciState)
        val (curYear, curMonth) = CuciMoneyHelpers.currentYearMonth()
        val monthPrefix = CuciMoneyHelpers.yearMonthPrefix(curYear, curMonth)
        val monthTrx = cuciState.transactions.filter {
            it.bookId == cuciBookId && it.date.startsWith(monthPrefix) && it.transferPair == null
        }
        val monthIn = monthTrx.filter { it.direction == "in" }.sumOf { it.amount }
        val monthOut = monthTrx.filter { it.direction == "out" }.sumOf { it.amount }

        return buildString {
            append(remSystemPrompt)
            if (topMemories.isNotEmpty()) {
                append("\n\n## Memori Personal Tentang Tuan (dari SQLite lokal)\n")
                topMemories.forEach { m ->
                    append("- [${m.category}] ${m.factKey}: ${m.factValue}\n")
                }
            }
            if (latestSummaries.isNotEmpty()) {
                append("\n## Ringkasan Sesi Sebelumnya\n")
                latestSummaries.forEach { s ->
                    append("- ${s.summaryText}\n")
                }
            }
            if (recentJournals.isNotEmpty()) {
                append("\n## Jurnal Kronik Terbaru Tuan\n")
                recentJournals.forEach { j ->
                    val moodInfo = j.moodStatus?.let { " [Mood: $it]" }.orEmpty()
                    val snippet = j.content.replace('\n', ' ').trim().take(120)
                    append("- ${j.date}$moodInfo ${j.title.ifBlank { "(tanpa judul)" }}: $snippet\n")
                }
            }
            if (recentPublicLinks.isNotEmpty()) {
                append("\n## Bookmark LinkKu Terbaru (Publik)\n")
                recentPublicLinks.forEach { l ->
                    val domain = LinkkuHelpers.extractDomain(l.url)
                    append("- ${l.title.ifBlank { domain }} ($domain)\n")
                }
            }
            if (cuciBook != null) {
                append("\n## Ringkasan Keuangan CuciMoney+ (${cuciBook.name})\n")
                append("- Total Saldo: ${CuciMoneyHelpers.formatRp(cuciBal)}\n")
                append("- Pengeluaran Hari Ini: ${CuciMoneyHelpers.formatRp(cuciTodayExp)}\n")
                append("- Bulan Ini: Masuk ${CuciMoneyHelpers.formatRp(monthIn)}, Keluar ${CuciMoneyHelpers.formatRp(monthOut)}\n")
                val recentCuci = cuciState.transactions
                    .filter { it.bookId == cuciBookId }
                    .sortedWith(compareByDescending<CuciTransactionEntity> { it.date }.thenByDescending { it.id })
                    .take(3)
                if (recentCuci.isNotEmpty()) {
                    append("- Transaksi terakhir: ")
                    append(recentCuci.joinToString("; ") { t ->
                        val sign = if (t.direction == "in") "+" else "-"
                        "${t.title} ($sign${CuciMoneyHelpers.formatRp(t.amount)}, ${t.date})"
                    })
                    append("\n")
                }
            }
            val idekuList = _uiState.value.idekuState.ideas
            if (idekuList.isNotEmpty()) {
                val prosesIdeas = idekuList.filter { it.type == "Idea" && it.status == "Proses" }
                val pendingCount = idekuList.count { it.status == "Belum" }
                append("\n## IdeKu & Wishlist Tuan (${idekuList.size} item, $pendingCount Belum)\n")
                if (prosesIdeas.isNotEmpty()) {
                    append("- Fokus Proses (${prosesIdeas.size}/${IdekuHelpers.FOCUS_LIMIT}): ")
                    append(prosesIdeas.joinToString("; ") { item ->
                        val nxt = if (item.nextStep.isNotBlank()) " → ${item.nextStep}" else ""
                        "${item.title}$nxt"
                    })
                    append("\n")
                }
                val latestItems = idekuList.take(4)
                append("- Terbaru: ")
                append(latestItems.joinToString("; ") { "[${it.type}] ${it.title} (${it.category}, ${it.status})" })
                append("\n")
            }
            val dailyState = _uiState.value.dailyOsState
            val dailyHabits = dailyState.habits.filter { !it.archived }
            val dailyTodos = dailyState.todos
            if (dailyHabits.isNotEmpty() || dailyTodos.isNotEmpty()) {
                val today = DailyOsHelpers.todayStr()
                val prog = DailyOsHelpers.calculateTodayProgress(dailyState.habits, today)
                val streak = DailyOsHelpers.calculateOverallStreak(dailyState.habits, today)
                val pendingTodos = dailyTodos.filter { !it.done }
                append("\n## Daily OS Hari Ini (${prog[3]}/${prog[4]} habit selesai [${prog[2]}%], Streak ${streak}h)\n")
                if (pendingTodos.isNotEmpty()) {
                    val urgentOrOv = pendingTodos.filter { it.priority == "urgent" || DailyOsHelpers.isOverdue(it.deadline, today) }.take(4)
                    append("- To-Do Pending (${pendingTodos.size} task): ")
                    append((urgentOrOv.ifEmpty { pendingTodos.take(4) }).joinToString("; ") { "${it.title} [${it.priority}]" })
                    append("\n")
                }
            }
            append("\nGunakan memori, Kronik, LinkKu, CuciMoney+, IdeKu, & Daily OS di atas secara natural agar terasa mengenal Tuan, tanpa menyebut kata 'database' atau 'SQLite'.")
        }
    }

    private fun extractAndPersistMemories(sourceMessageId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLearningMemory = true) }
            try {
                val recentTurns = chatDao.getRecentDesc(6)
                    .reversed()
                    .map { ChatTurnDto(role = it.role, content = it.content) }
                val existing = userMemoryDao.getTopMemories(25)
                    .map { ExistingFactDto(factKey = it.factKey, factValue = it.factValue) }

                val result = memoryClient.extractMemories(recentTurns, existing) ?: return@launch
                result.facts.forEach { fact ->
                    if (fact.factKey.isNotBlank() && fact.factValue.isNotBlank()) {
                        userMemoryDao.upsertByFactKey(
                            category = fact.category,
                            factKey = fact.factKey,
                            factValue = fact.factValue,
                            confidence = fact.confidence,
                            sourceMessageId = sourceMessageId
                        )
                    }
                }
                val summary = result.summary?.trim().orEmpty()
                if (summary.length > 8) {
                    val lastSummary = userMemoryDao.getLatestSummaries(1).firstOrNull()?.summaryText
                    if (summary != lastSummary) {
                        userMemoryDao.insertSummary(
                            ConversationSummaryEntity(
                                summaryText = summary,
                                turnCount = recentTurns.size
                            )
                        )
                    }
                }
            } finally {
                _uiState.update { it.copy(isLearningMemory = false) }
            }
        }
    }

    private fun applyExpression(name: String, autoRevert: Boolean) {
        exprRevertJob?.cancel()
        _uiState.update { it.copy(currentExpression = name) }
        stageController.setExpression(name)
        if (autoRevert && name != "none") {
            exprRevertJob = viewModelScope.launch {
                delay(6_000)
                if (_uiState.value.currentExpression == name) {
                    applyExpression("none", autoRevert = false)
                }
            }
        }
    }

    private fun showIdleBubble(line: BubbleLine, durationMs: Long = 3_500L) {
        if (!isForeground || !isAsistenTabActive) return
        if (_uiState.value.isThinking || _uiState.value.isSpeaking) return
        audioManager.playBundledAssetClip(line.audio)
        idleBubbleJob?.cancel()
        _idleBubbleText.value = line.text
        idleBubbleJob = viewModelScope.launch {
            delay(durationMs)
            _idleBubbleText.value = null
        }
    }

    private fun hideIdleBubble() {
        idleBubbleJob?.cancel()
        _idleBubbleText.value = null
    }

    private fun resetIdleTimer() {
        idleTimerJob?.cancel()
        idleTimerJob = null
        if (!isForeground || !isAsistenTabActive) return
        idleTimerJob = viewModelScope.launch {
            delay(30_000)
            if (!isForeground || !isAsistenTabActive) return@launch
            showIdleBubble(idleLines.random(), 5_000L)
            stageController.playMotion("idle")
            while (isForeground && isAsistenTabActive) {
                delay((25_000L..40_000L).random())
                if (!isForeground || !isAsistenTabActive) break
                showIdleBubble(idleLines.random(), 5_000L)
            }
        }
    }

    // ── Live2DStageCallbacks ──────────────────────────────────────────────
    override fun onModelReady() {
        _uiState.update { it.copy(isModelReady = true, modelError = null) }
        viewModelScope.launch {
            delay(450)
            stageController.playMotion("appeal")
            delay(3_200)
            if (_uiState.value.currentExpression == "none") {
                stageController.playMotion("idle")
            }
        }
    }

    override fun onZoneTapped(zone: String) {
        val now = System.currentTimeMillis()
        if (now - lastBubbleAt < 1_800L) return
        lastBubbleAt = now
        resetIdleTimer()

        when (zone) {
            "head" -> {
                showIdleBubble(headLines.random(), 3_500L)
                applyExpression("shame", autoRevert = true)
            }
            "body" -> {
                showIdleBubble(bodyLines.random(), 3_000L)
                applyExpression("surprise", autoRevert = true)
            }
            "leg" -> {
                showIdleBubble(legLines.random(), 3_000L)
                applyExpression("appeal", autoRevert = true)
            }
        }
    }

    override fun onPetTriggered() {
        resetIdleTimer()
        showIdleBubble(petLines.random(), 3_000L)
        applyExpression("happy", autoRevert = true)
    }

    override fun onModelError(message: String) {
        _uiState.update { it.copy(isModelReady = false, modelError = message) }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.release()
    }
}
