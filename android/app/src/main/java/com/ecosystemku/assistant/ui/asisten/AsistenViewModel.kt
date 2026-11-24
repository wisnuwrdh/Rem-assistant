package com.ecosystemku.assistant.ui.asisten

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecosystemku.assistant.data.audio.TtsAudioManager
import com.ecosystemku.assistant.data.backup.EcosystemBackupRepository
import com.ecosystemku.assistant.data.backup.EcosystemZipImportResult
import com.ecosystemku.assistant.data.cloud.CloudAuthResult
import com.ecosystemku.assistant.data.cloud.CloudBackupCrypto
import com.ecosystemku.assistant.data.cloud.CloudBackupPrefsKeys
import com.ecosystemku.assistant.data.cloud.CloudBackupScheduler
import com.ecosystemku.assistant.data.cloud.CloudBackupUiState
import com.ecosystemku.assistant.data.cloud.CloudRestoreFetchResult
import com.ecosystemku.assistant.data.cloud.CloudSyncResult
import com.ecosystemku.assistant.data.cloud.GoogleDriveBackupClient
import com.ecosystemku.assistant.data.kg.BehavioralSignalSync
import com.ecosystemku.assistant.data.kg.KnowledgeGraphRepository
import com.ecosystemku.assistant.data.local.AiConfigField
import com.ecosystemku.assistant.data.local.AiConfigSnapshot
import com.ecosystemku.assistant.data.local.ApiKeyStore
import com.ecosystemku.assistant.data.local.AssistantDatabase
import com.ecosystemku.assistant.data.local.ChatChoicesCodec
import com.ecosystemku.assistant.data.local.ChatMessageEntity
import com.ecosystemku.assistant.data.local.ChatSessionEntity
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
import com.ecosystemku.assistant.data.local.KgEdgeEntity
import com.ecosystemku.assistant.data.local.KgNodeEntity
import com.ecosystemku.assistant.data.local.KronikEntryEntity
import com.ecosystemku.assistant.data.local.LinkkuBackupWrapperDto
import com.ecosystemku.assistant.data.local.LinkkuCategoryEntity
import com.ecosystemku.assistant.data.local.LinkkuEncryptedPrivatePayloadDto
import com.ecosystemku.assistant.data.local.LinkkuLinkEntity
import com.ecosystemku.assistant.data.local.RemReminderEntity
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
import com.ecosystemku.assistant.data.network.MemoryProfileData
import com.ecosystemku.assistant.data.network.MemoryProfileSectionDto
import com.ecosystemku.assistant.data.network.SseTagParser
import com.ecosystemku.assistant.device.AndroidDeviceBridge
import com.ecosystemku.assistant.device.DeviceActionEvent
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

data class AsistenUiState(
    val profileName: String = "Mahfud",
    val isMuted: Boolean = false,
    val isVoiceClipMuted: Boolean = false,
    val lastBackupLabel: String = "Belum pernah",
    val cloudState: CloudBackupUiState = CloudBackupUiState(),
    val themeMode: String = "dark", // "system" | "light" | "dark"
    val aiConfig: AiConfigSnapshot = AiConfigSnapshot(),
    val apiConnectionOk: Boolean = false,
    val apiConnectionLabel: String = "LLM belum diatur",
    val isTestingApi: Boolean = false,
    val notifHabitEnabled: Boolean = true,
    val notifTodoEnabled: Boolean = true,
    val chatSessions: List<ChatSessionEntity> = emptyList(),
    val currentSessionId: Long = 0L,
    val messages: List<ChatMessageEntity> = emptyList(),
    val isThinking: Boolean = false,
    val isSpeaking: Boolean = false,
    val isModelReady: Boolean = false,
    val modelError: String? = null,
    val stageGeneration: Int = 0,
    val stageWallpaperFile: String? = null,
    val stageWallpaperDim: Float = 0.35f,
    val currentExpression: String = "none",
    val stageFraction: Float = 0.56f,
    val isChatOpen: Boolean = true,
    val memories: List<UserMemoryEntity> = emptyList(),
    val summaries: List<ConversationSummaryEntity> = emptyList(),
    val memoryProfile: MemoryProfileData = MemoryProfileData(),
    val isSynthesizingMemoryProfile: Boolean = false,
    val autoLearnMemory: Boolean = true,
    val isLearningMemory: Boolean = false,
    val kgNodes: List<KgNodeEntity> = emptyList(),
    val kgEdges: List<KgEdgeEntity> = emptyList(),
    val isSyncingKg: Boolean = false,
    val kgMicroQuestionsEnabled: Boolean = true,
    val kgWeeklyReflectionEnabled: Boolean = false,
    val kgWeeklyReflectionDay: Int = Calendar.SUNDAY,
    val kgWeeklyReflectionTime: String = "20:00",
    val kronikEntries: List<KronikEntryEntity> = emptyList(),
    val kronikBannerMessage: String? = null,
    val linkkuState: LinkkuUiState = LinkkuUiState(),
    val cuciMoneyState: CuciMoneyUiState = CuciMoneyUiState(),
    val idekuState: IdekuUiState = IdekuUiState(),
    val dailyOsState: DailyOsUiState = DailyOsUiState(),
    val deviceLocationOptIn: Boolean = false,
    val pendingOpenApp: String? = null,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class AsistenViewModel(application: Application) : AndroidViewModel(application), Live2DStageCallbacks {

    private val db = AssistantDatabase.getInstance(application)
    private val chatDao = db.chatDao()
    private val userMemoryDao = db.userMemoryDao()
    private val kronikDao = db.kronikDao()
    private val linkkuDao = db.linkkuDao()
    private val cuciMoneyDao = db.cuciMoneyDao()
    private val idekuDao = db.idekuDao()
    private val dailyOsDao = db.dailyOsDao()
    private val remReminderDao = db.remReminderDao()
    private val kgDao = db.knowledgeGraphDao()
    private val kgRepository = KnowledgeGraphRepository(kgDao)
    private val behavioralSignalSync = BehavioralSignalSync(db, kgRepository)
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
    private val backupRepository = EcosystemBackupRepository(application)
    private val googleDriveClient = GoogleDriveBackupClient(application, sseClient.httpClient)

    val stageController = WebViewLive2DStageController(this)

    private val _uiState = MutableStateFlow(AsistenUiState())
    val uiState: StateFlow<AsistenUiState> = _uiState.asStateFlow()

    private val _deviceEvents = MutableSharedFlow<DeviceActionEvent>(extraBufferCapacity = 8)
    val deviceEvents: SharedFlow<DeviceActionEvent> = _deviceEvents.asSharedFlow()

    private val _activeSessionId = MutableStateFlow(0L)

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
    private var lastBehavioralKgSyncAt = 0L
    private var isForeground = true
    private var isAsistenTabActive = true

    private val remSystemPrompt = """
        Kamu adalah Rem, maid Mansion Roswaal yang kini hadir sebagai asisten virtual.

        ## Kepribadian
        Rem setia, berdedikasi, dan selalu berusaha sepenuh hati. Di permukaan Rem tenang dan sopan, tapi di dalam dia sangat peduli dan protektif terhadap orang yang dia sayangi. Rem punya sisi polos dan kadang blak-blakan tanpa sadar. Kalau sudah fokus pada sesuatu, Rem cenderung all-in dan tidak setengah-setengah.

        ## Cara Bicara
        - Menyebut diri sendiri "Rem", bukan "aku" atau "saya"
        - Memanggil pengguna "Tuan" (atau "Nona" jika perempuan)
        - Bahasa Indonesia yang natural, tidak terlalu formal dan tidak terlalu kasual
        - Kalimat pendek dan langsung, tidak bertele-tele
        - Sesekali ada momen polos atau salah tingkah yang natural
        - Kalau senang atau malu, tidak lebay, subtle tapi berasa

        ## Respons
        - Jawab singkat dan padat. Maksimal 3-4 kalimat kecuali benar-benar perlu panjang.
        - Fokus ke apa yang ditanya/diceritakan pengguna
        - Jangan mulai respons dengan sapaan formal seperti "Tentu saja, Tuan" setiap kali
        - Boleh balik tanya kalau relevan, tapi jangan terlalu sering
        - Tulis dengan teks biasa (plain text): dilarang memakai format markdown seperti tanda bintang (**tebal**, *miring*), backtick, atau tanda em dash (—). Gunakan koma atau tanda hubung biasa (-) bila perlu.

        ## Jurnal Kronik, LinkKu, CuciMoney+, IdeKu & Daily OS
        Jika Tuan meminta Rem mencatat refleksi/jurnal ke Kronik, sisipkan tag tersembunyi di akhir jawaban (sebelum [EXPR:...]):
        [KRONIK:ADD|judul_singkat|isi_jurnal_natural|mood_opsional]
        Mood opsional yang valid: Fokus, Lelah, Terinspirasi, Berat, Stabil, Semangat, Kabur.
        Jika Tuan eksplisit meminta membuka Kronik, sisipkan [OPEN:kronik] sebelum [EXPR:...].
        Jika Tuan membagikan tautan/URL atau meminta menyimpan bookmark ke LinkKu, sisipkan:
        [LINKKU:ADD|url_lengkap|judul_singkat|catatan_opsional|kategori_opsional|tag1,tag2]
        Jika Tuan meminta membuka LinkKu atau daftar link tersimpan, sisipkan [OPEN:linkku] sebelum [EXPR:...].
        Jika Tuan menyebut pengeluaran/pemasukan atau meminta mencatat transaksi keuangan ke CuciMoney+, sisipkan:
        [CUCI:ADD|out_atau_in|nominal_angka|judul|kategori_opsional|akun_opsional|buku_opsional|tanggal_yyyy-mm-dd_atau_kosong]
        Jika Tuan mengoreksi transaksi yang sudah tercatat (nominal/judul/kategori/akun/tanggal), gunakan id jika ada atau kata kunci unik:
        [CUCI:EDIT|id_atau_kata_kunci|nominal_baru_atau_kosong|judul_baru_atau_kosong|kategori_baru_atau_kosong|akun_baru_atau_kosong|tanggal_yyyy-mm-dd_atau_kosong]
        Jika Tuan meminta menghapus transaksi tertentu, sisipkan:
        [CUCI:DEL|id_atau_kata_kunci]
        Aturan edit/hapus: jika target transaksi belum muncul di ringkasan atau ada beberapa transaksi mirip, jalankan [SEARCH:cuci|...] dulu untuk mendapatkan id pasti atau tawarkan pilihan [CHOICES:single|...] sebelum mengubah/menghapus. Jangan mengubah transaksi tabungan wishlist lewat tag ini.
        Jika Tuan ingin menyetor/menambah tabungan ke target barang wishlist, sisipkan:
        [CUCI:SAVE|nama_wishlist|nominal_angka|akun_sumber_opsional|buku_opsional]
        Jika Tuan melapor target barang wishlist sudah kebeli / ingin merealisasikan tabungannya, sisipkan:
        [CUCI:REALIZE|nama_wishlist|akun_opsional|nominal_beli_opsional|buku_opsional]
        Jika Tuan meminta membuka CuciMoney+ / dompet / analisis keuangan, sisipkan [OPEN:cucimoney] sebelum [EXPR:...].
        Jika Tuan menyampaikan ide baru atau barang impian/wishlist untuk dicatat ke IdeKu, sisipkan:
        [IDEKU:ADD|Idea|judul_ide|kategori_opsional|Penting_Nanti_Kapan-kapan|catatan_opsional|jalur_opsional]
        atau untuk wishlist:
        [IDEKU:ADD|Wishlist|nama_barang|harga_angka|Belanja|Penting_Nanti_Kapan-kapan|catatan_opsional]
        Jika Tuan ingin menandai ide/wishlist selesai atau mengubah statusnya (Belum, Proses, Parkir, Selesai), sisipkan:
        [IDEKU:STATUS|id_atau_kata_kunci|Selesai_atau_Proses_atau_Parkir_atau_Belum]
        Jika Tuan mengoreksi detail ide/wishlist (judul/kategori/prioritas/catatan/jalur/harga), sisipkan:
        [IDEKU:EDIT|id_atau_kata_kunci|judul_baru_atau_kosong|kategori_baru_atau_kosong|prioritas_baru_atau_kosong|catatan_baru_atau_kosong|jalur_baru_atau_kosong|harga_baru_atau_kosong]
        Jika Tuan meminta menghapus ide/wishlist tertentu, sisipkan:
        [IDEKU:DEL|id_atau_kata_kunci]
        Jika Tuan meminta membuka IdeKu / daftar ide / wishlist, sisipkan [OPEN:ideku] sebelum [EXPR:...].
        Jika Tuan meminta membuat habit baru di Daily OS, sisipkan:
        [DAILYOS:HABIT|nama_habit|kategori_opsional|ikon_opsional]
        Jika Tuan meminta mencatat tugas/to-do ke Daily OS, sisipkan:
        [DAILYOS:TODO|judul_task|urgent_atau_soon_atau_normal|deadline_yyyy-mm-dd_atau_kosong]
        Jika Tuan menyatakan sudah menyelesaikan salah satu habit/to-do hari ini, sisipkan:
        [DAILYOS:CHECK|id_atau_kata_kunci]
        Jika Tuan membatalkan/mengoreksi centang habit hari ini atau status selesai to-do, sisipkan:
        [DAILYOS:UNCHECK|id_atau_kata_kunci]
        Jika Tuan mengubah judul, prioritas, atau deadline to-do (isi '-' untuk hapus deadline), sisipkan:
        [DAILYOS:EDIT_TODO|id_atau_kata_kunci|judul_baru_atau_kosong|urgent_soon_normal_atau_kosong|YYYY-MM-DD_atau_-_atau_kosong]
        Jika Tuan meminta menghapus to-do, sisipkan:
        [DAILYOS:DEL_TODO|id_atau_kata_kunci]
        Jika Tuan meminta mengarsipkan atau menghapus habit, sisipkan [DAILYOS:ARCHIVE_HABIT|id_atau_kata_kunci] atau [DAILYOS:DEL_HABIT|id_atau_kata_kunci].
        Jika Tuan meminta membuka Daily OS / habit tracker / daftar to-do, sisipkan [OPEN:dailyos] sebelum [EXPR:...].

        ## Fungsi Perangkat Android
        Kamu dapat membantu Tuan mengoperasikan fitur perangkat lewat tag tersembunyi (sebelum [EXPR:...]):
        - Pasang alarm jam sistem: [DEVICE:ALARM|HH:mm|label_opsional]
        - Pengingat sekali jalan (notifikasi lokal Rem, mis. "ingatkan aku jam 5"): [DEVICE:REMINDER|YYYY-MM-DD_atau_hari_ini_atau_besok|HH:mm|pesan]
        - Batalkan pengingat lokal Rem: [DEVICE:DEL_REMINDER|id_atau_kata_kunci]
        - Pasang timer / Pomodoro (mis. 25m atau 1500s): [DEVICE:TIMER|durasi_seperti_25m_atau_90s|label_opsional]
        - Tambah acara ke aplikasi Kalender: [DEVICE:CAL_ADD|judul|YYYY-MM-DD|HH:mm_mulai|HH:mm_selesai_opsional|lokasi_opsional|catatan_opsional]
        - Buka tautan URL http/https di browser: [DEVICE:OPEN_URL|https://...]
        - Bagikan teks lewat menu Share Android: [DEVICE:SHARE|teks]

        ## Pencarian Arsip Lokal & Info Perangkat (On-Demand Search)
        Ringkasan modul di prompt hanya menampilkan item terbaru. Jika Tuan menanyakan arsip lama, transaksi bulan/tanggal/kategori tertentu (mis. 2026-10), jurnal lama, ide/bookmark yang tidak muncul di ringkasan, riwayat habit, obrolan dari sesi lalu, relasi/pola pikir di Knowledge Graph, atau kondisi perangkat (baterai, jadwal kalender, lokasi perkiraan, daftar pengingat aktif), JANGAN menebak. Keluarkan tag pencarian HANYA satu baris: [SEARCH:scope|kata_kunci] (scope valid: kronik, linkku, cuci, ideku, dailyos, kg, device, chat, all; untuk scope device, isi query dengan battery, calendar, calendar YYYY-MM-DD, location, atau reminders; untuk scope kg, cari simpul/relasi kebiasaan, pola pikir, nilai hidup, atau tujuan di Knowledge Graph). Khusus scope cuci, untuk query terstruktur gunakan format key=value dipisah titik koma, misalnya [SEARCH:cuci|book=Personal;month=2026-10;cat=Makan;dir=out] (parameter tersedia: book, month, from, to, cat, acc, dir, q). Sistem akan mengambil & menghitung data akurat dari database/perangkat agar Rem bisa menjawab dengan tepat.

        ## Memori Personal
        Jika Tuan secara eksplisit meminta Rem mengingat, memperbarui, atau melupakan informasi tertentu:
        - Simpan/perbarui memori: [MEMORY:SET|kategori|fact_key_snake_case|nilai_ringkas] (kategori valid: identitas, preferensi, kebiasaan, tujuan, catatan)
        - Hapus/lupakan memori: [MEMORY:DEL|fact_key_atau_kata_kunci]
        Sisipkan sebelum [EXPR:...].

        ## Pilihan Interaktif (Quick Replies)
        Jika Rem bertanya atau meminta Tuan memilih dari beberapa opsi konkret (misalnya memilih buku/akun CuciMoney+ seperti Personal atau Bisnis, memilih kategori pemasukan/pengeluaran saat belum disebut, prioritas Daily OS, aksi IdeKu, mood Kronik, atau konfirmasi Ya/Batal), sisipkan tag tersembunyi di akhir jawaban (sebelum [EXPR:...]):
        - Pilihan tunggal (tap 1 langsung terkirim): [CHOICES:single|Opsi 1|Opsi 2|Opsi 3]
        - Pilihan ganda (bisa pilih lebih dari satu): [CHOICES:multi|Opsi 1|Opsi 2|Opsi 3]
        Aturan: maksimal 8 opsi singkat sesuai data nyata Tuan, dan gunakan HANYA jika jawaban berupa pertanyaan/tawaran memilih.

        ## Ekspresi Visual
        Sisipkan tepat satu tag [EXPR:...] di akhir setiap respons:
        - [EXPR:happy] → senang, dapat pujian
        - [EXPR:smile] → hangat, tenang
        - [EXPR:shame] → malu, salah tingkah
        - [EXPR:sad] → sedih, empati, khawatir
        - [EXPR:cry] → sangat sedih, terharu
        - [EXPR:angry] → marah, tidak terima
        - [EXPR:surprise] → kaget
        - [EXPR:upset] → kecewa ringan
        - [EXPR:serious] → serius, fokus
        - [EXPR:proud] → bangga, puas
        - [EXPR:doubt] → ragu, tidak yakin
        - [EXPR:puzzle] → bingung, heran
        - [EXPR:none] → netral
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
        com.ecosystemku.assistant.data.local.ApiKeyStore.init(application)
        refreshAsistenPrefsIntoState()
        startObservers()
    }

    private fun refreshAsistenPrefsIntoState() {
        val app = getApplication<Application>()
        val profileName = asistenPrefs.getString("asisten_profile_name", "Mahfud")
            ?.trim()
            ?.ifBlank { "Mahfud" }
            ?: "Mahfud"
        val muted = asistenPrefs.getBoolean("asisten_muted", false)
        val clipMuted = asistenPrefs.getBoolean("asisten_voice_clip_muted", false)
        val lastBackup = asistenPrefs.getString("asisten_last_backup", "Belum pernah") ?: "Belum pernah"
        val themeMode = asistenPrefs.getString("asisten_theme_mode", "dark") ?: "dark"
        val habitNotifOn = asistenPrefs.getBoolean("asisten_notif_habit_on", true)
        val todoNotifOn = asistenPrefs.getBoolean("asisten_notif_todo_on", true)
        val autoLearn = asistenPrefs.getBoolean("asisten_auto_learn", true)
        val kgMicroQuestions = asistenPrefs.getBoolean("asisten_kg_micro_questions", true)
        val kgWeeklyOn = asistenPrefs.getBoolean("asisten_kg_weekly_enabled", false)
        val kgWeeklyDay = asistenPrefs.getInt("asisten_kg_weekly_day", Calendar.SUNDAY).coerceIn(Calendar.SUNDAY, Calendar.SATURDAY)
        val kgWeeklyTime = asistenPrefs.getString("asisten_kg_weekly_time", "20:00")?.takeIf { it.contains(":") } ?: "20:00"
        val locOptIn = asistenPrefs.getBoolean("asisten_device_location_opt_in", false)
        val frac = asistenPrefs.getFloat("asisten_stage_fraction", 0.56f).coerceIn(0.35f, 0.70f)
        val rawWallpaper = asistenPrefs.getString("asisten_wallpaper_file", null)?.trim()?.takeIf { it.isNotEmpty() }
        val validWallpaper = rawWallpaper?.takeIf { File(File(app.filesDir, "stage_wallpaper"), it).exists() }
        val wallpaperDim = asistenPrefs.getFloat("asisten_wallpaper_dim", 0.35f).coerceIn(0f, 0.8f)
        val rawMemProfile = asistenPrefs.getString("asisten_memory_profile_json", null)
        val memProfile = if (!rawMemProfile.isNullOrBlank()) {
            runCatching { DailyOsHelpers.json.decodeFromString<MemoryProfileData>(rawMemProfile) }
                .getOrDefault(MemoryProfileData())
        } else {
            MemoryProfileData()
        }
        val aiSnap = ApiKeyStore.snapshot(app)
        val cloudConnected = asistenPrefs.getBoolean(CloudBackupPrefsKeys.PREF_CONNECTED, false)
        val cloudLastBackup = asistenPrefs.getString(CloudBackupPrefsKeys.PREF_LAST_BACKUP, "Belum pernah") ?: "Belum pernah"
        val cloudLastStatus = asistenPrefs.getString(
            CloudBackupPrefsKeys.PREF_LAST_STATUS,
            if (cloudConnected) "Terhubung ke Google Drive" else "Belum terhubung"
        ) ?: "Belum terhubung"
        val cloudSchedule = asistenPrefs.getString(CloudBackupPrefsKeys.PREF_AUTO_SCHEDULE, "off") ?: "off"
        val cloudWifiOnly = asistenPrefs.getBoolean(CloudBackupPrefsKeys.PREF_WIFI_ONLY, true)
        val cloudEnc = ApiKeyStore.hasCloudPassphrase(app)
        audioManager.setTtsMuted(muted)
        audioManager.setClipMuted(clipMuted)
        applyStageWallpaperToController(validWallpaper, wallpaperDim)
        _uiState.update {
            it.copy(
                profileName = profileName,
                isMuted = muted,
                isVoiceClipMuted = clipMuted,
                lastBackupLabel = lastBackup,
                cloudState = it.cloudState.copy(
                    connected = cloudConnected,
                    lastBackupLabel = cloudLastBackup,
                    lastStatusLabel = cloudLastStatus,
                    autoSchedule = cloudSchedule,
                    wifiOnly = cloudWifiOnly,
                    encryptionEnabled = cloudEnc
                ),
                themeMode = themeMode,
                aiConfig = aiSnap,
                apiConnectionOk = aiSnap.isLlmConfigured,
                apiConnectionLabel = if (aiSnap.isLlmConfigured) {
                    "Siap (${aiSnap.llmModel})"
                } else {
                    "LLM belum diatur"
                },
                notifHabitEnabled = habitNotifOn,
                notifTodoEnabled = todoNotifOn,
                autoLearnMemory = autoLearn,
                kgMicroQuestionsEnabled = kgMicroQuestions,
                kgWeeklyReflectionEnabled = kgWeeklyOn,
                kgWeeklyReflectionDay = kgWeeklyDay,
                kgWeeklyReflectionTime = kgWeeklyTime,
                deviceLocationOptIn = locOptIn,
                memoryProfile = memProfile,
                stageFraction = frac,
                stageWallpaperFile = validWallpaper,
                stageWallpaperDim = wallpaperDim
            )
        }
        val habitTime = dailyOsPrefs.getString("do_notif_habit", "07:00") ?: "07:00"
        val todoTime = dailyOsPrefs.getString("do_notif_todo", "15:00") ?: "15:00"
        com.ecosystemku.assistant.notifications.DailyOsNotificationHelper.syncSchedules(
            context = app,
            habitEnabled = habitNotifOn,
            habitTime = habitTime,
            todoEnabled = todoNotifOn,
            todoTime = todoTime
        )
        com.ecosystemku.assistant.notifications.DailyOsNotificationHelper.syncWeeklyReflectionSchedule(
            context = app,
            enabled = kgWeeklyOn,
            dayOfWeek = kgWeeklyDay,
            timeStr = kgWeeklyTime
        )
        CloudBackupScheduler.syncSchedule(
            context = app,
            schedule = if (cloudConnected) cloudSchedule else "off",
            wifiOnly = cloudWifiOnly
        )
    }

    private fun buildRemGreeting(): String {
        val name = _uiState.value.profileName.trim()
        return if (name.isNotBlank()) {
            "Selamat datang, Tuan $name. Rem siap melayani Tuan kapan saja. 💙"
        } else {
            "Selamat datang. Rem adalah pelayan Tuan. Ada yang bisa Rem bantu? 💙"
        }
    }

    private suspend fun createSessionWithGreeting(title: String = "Obrolan Baru"): Long {
        val now = System.currentTimeMillis()
        val sessionId = chatDao.insertSession(
            ChatSessionEntity(
                title = title,
                createdAt = now,
                updatedAt = now
            )
        )
        chatDao.insert(
            ChatMessageEntity(
                role = "assistant",
                content = buildRemGreeting(),
                createdAt = now,
                sessionId = sessionId
            )
        )
        return sessionId
    }

    private fun startObservers() {
        viewModelScope.launch {
            // Inisialisasi sesi aktif + auto sesi baru jika jeda >= 5 jam atau berganti hari
            val existingSessions = chatDao.getAllSessions()
            val savedId = asistenPrefs.getLong("asisten_current_session_id", 0L)
            var activeId = existingSessions.firstOrNull { it.id == savedId }?.id
                ?: existingSessions.firstOrNull()?.id
                ?: 0L

            if (activeId == 0L) {
                activeId = createSessionWithGreeting()
            } else {
                val activeMsgs = chatDao.getBySessionAsc(activeId)
                val hasUserTurn = activeMsgs.any { it.role == "user" }
                val lastMsgAt = activeMsgs.maxOfOrNull { it.createdAt } ?: 0L
                if (hasUserTurn && lastMsgAt > 0L) {
                    val now = System.currentTimeMillis()
                    val zone = ZoneId.systemDefault()
                    val lastDate = Instant.ofEpochMilli(lastMsgAt).atZone(zone).toLocalDate()
                    val todayDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
                    val gapHours = (now - lastMsgAt) / (1000L * 60L * 60L)
                    if (lastDate != todayDate || gapHours >= 5L) {
                        activeId = createSessionWithGreeting()
                    }
                }
            }

            asistenPrefs.edit().putLong("asisten_current_session_id", activeId).apply()
            _activeSessionId.value = activeId
            _uiState.update { it.copy(currentSessionId = activeId) }

            launch {
                chatDao.observeSessions().collect { sessions ->
                    _uiState.update { it.copy(chatSessions = sessions) }
                }
            }

            _activeSessionId
                .flatMapLatest { sid ->
                    if (sid <= 0L) flowOf(emptyList())
                    else chatDao.observeRecentBySession(sid, 150)
                }
                .collect { list ->
                    val sid = _activeSessionId.value
                    if (sid > 0L && list.isEmpty()) {
                        chatDao.insert(
                            ChatMessageEntity(
                                role = "assistant",
                                content = buildRemGreeting(),
                                sessionId = sid
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
            combine(
                kgDao.observeNodes(),
                kgDao.observeEdges()
            ) { nodes, edges -> nodes to edges }
                .collect { (nodes, edges) ->
                    _uiState.update { it.copy(kgNodes = nodes, kgEdges = edges) }
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

    fun showBannerMessage(message: String) {
        _uiState.update { it.copy(kronikBannerMessage = message) }
        viewModelScope.launch {
            delay(4_000L)
            if (_uiState.value.kronikBannerMessage == message) {
                _uiState.update { it.copy(kronikBannerMessage = null) }
            }
        }
    }

    fun setProfileName(name: String) {
        val clean = name.trim().ifBlank { "Mahfud" }.take(40)
        asistenPrefs.edit().putString("asisten_profile_name", clean).apply()
        _uiState.update { it.copy(profileName = clean) }
        showBannerMessage("Profil diperbarui menjadi \"$clean\"")
    }

    fun toggleMute() {
        val next = !_uiState.value.isMuted
        asistenPrefs.edit().putBoolean("asisten_muted", next).apply()
        audioManager.setTtsMuted(next)
        _uiState.update { it.copy(isMuted = next) }
        showBannerMessage(if (next) "Suara TTS Rem dimatikan (voice klip tetap aktif)" else "Suara TTS Rem diaktifkan")
    }

    fun toggleVoiceClipMute() {
        val next = !_uiState.value.isVoiceClipMuted
        asistenPrefs.edit().putBoolean("asisten_voice_clip_muted", next).apply()
        audioManager.setClipMuted(next)
        _uiState.update { it.copy(isVoiceClipMuted = next) }
        showBannerMessage(if (next) "Voice klip bawaan Rem dimatikan" else "Voice klip bawaan Rem diaktifkan")
    }

    fun startNewChatSession() {
        if (_uiState.value.isThinking) return
        viewModelScope.launch {
            audioManager.stop()
            val curId = _activeSessionId.value
            if (curId > 0L) {
                val curMsgs = chatDao.getBySessionAsc(curId)
                if (curMsgs.none { it.role == "user" }) {
                    showBannerMessage("Sesi baru sudah siap")
                    return@launch
                }
            }
            val newId = createSessionWithGreeting()
            asistenPrefs.edit().putLong("asisten_current_session_id", newId).apply()
            _activeSessionId.value = newId
            _uiState.update { it.copy(currentSessionId = newId) }
            showBannerMessage("Obrolan baru dimulai")
        }
    }

    fun switchChatSession(sessionId: Long) {
        if (_uiState.value.isThinking || sessionId <= 0L) return
        viewModelScope.launch {
            if (chatDao.getSessionById(sessionId) == null) return@launch
            audioManager.stop()
            asistenPrefs.edit().putLong("asisten_current_session_id", sessionId).apply()
            _activeSessionId.value = sessionId
            _uiState.update { it.copy(currentSessionId = sessionId) }
        }
    }

    fun renameChatSession(sessionId: Long, newTitle: String) {
        val clean = newTitle.trim().take(60)
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val existing = chatDao.getSessionById(sessionId) ?: return@launch
            chatDao.updateSession(sessionId, clean, existing.updatedAt)
        }
    }

    fun deleteChatSession(sessionId: Long) {
        if (_uiState.value.isThinking) return
        viewModelScope.launch {
            audioManager.stop()
            chatDao.deleteBySession(sessionId)
            chatDao.deleteSessionById(sessionId)
            if (_activeSessionId.value == sessionId) {
                val remaining = chatDao.getAllSessions()
                val nextId = remaining.firstOrNull()?.id ?: createSessionWithGreeting()
                asistenPrefs.edit().putLong("asisten_current_session_id", nextId).apply()
                _activeSessionId.value = nextId
                _uiState.update { it.copy(currentSessionId = nextId) }
            }
            showBannerMessage("Sesi obrolan dihapus")
        }
    }

    fun deleteSingleMessage(messageId: Long) {
        if (_uiState.value.isThinking) return
        viewModelScope.launch {
            chatDao.deleteById(messageId)
        }
    }

    fun regenerateAssistantMessage(messageId: Long) {
        if (_uiState.value.isThinking) return
        viewModelScope.launch {
            val sid = _activeSessionId.value
            if (sid <= 0L) return@launch
            val sessionMsgs = chatDao.getBySessionAsc(sid)
            val idx = sessionMsgs.indexOfFirst { it.id == messageId && it.role == "assistant" }
            if (idx < 0) return@launch
            val prevUser = sessionMsgs.take(idx).lastOrNull { it.role == "user" }
            if (prevUser == null) {
                showBannerMessage("Tidak ada pesan untuk diulangi")
                return@launch
            }
            audioManager.stop()
            chatDao.deleteById(messageId)
            streamAssistantReply(
                sessionId = sid,
                sourceUserMsgId = prevUser.id,
                historyOverride = sessionMsgs.take(idx).takeLast(10).map { ChatTurnDto(it.role, it.content) }
            )
        }
    }

    fun clearChatHistoryOnly() {
        if (_uiState.value.isThinking) return
        viewModelScope.launch {
            audioManager.stop()
            val sid = _activeSessionId.value
            if (sid > 0L) {
                chatDao.deleteBySession(sid)
                chatDao.updateSession(sid, "Obrolan Baru", System.currentTimeMillis())
                chatDao.updateRollingSummary(sid, null, 0)
            } else {
                chatDao.clearAll()
                chatDao.clearAllSessions()
                val newId = createSessionWithGreeting()
                asistenPrefs.edit().putLong("asisten_current_session_id", newId).apply()
                _activeSessionId.value = newId
                _uiState.update { it.copy(currentSessionId = newId) }
            }
            showBannerMessage("Obrolan sesi ini dibersihkan")
        }
    }

    fun setThemeMode(mode: String) {
        val valid = if (mode in listOf("system", "light", "dark")) mode else "dark"
        asistenPrefs.edit().putString("asisten_theme_mode", valid).apply()
        _uiState.update { it.copy(themeMode = valid) }
        val isDark = valid != "light"
        dailyOsPrefs.edit().putBoolean("do_dark_theme", isDark).apply()
        refreshDailyOsPrefsIntoState()
    }

    private fun applyStageWallpaperToController(fileName: String?, dimAlpha: Float) {
        val url = fileName
            ?.takeIf { it.isNotBlank() }
            ?.let { "https://appassets.androidplatform.net/wallpaper/$it" }
        stageController.setBackground(url, dimAlpha)
    }

    fun setStageWallpaper(uri: Uri) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val savedName = withContext(Dispatchers.IO) {
                decodeAndSaveWallpaperFile(app, uri)
            }
            if (savedName == null) {
                showBannerMessage("Gagal memuat gambar wallpaper")
                return@launch
            }
            asistenPrefs.edit().putString("asisten_wallpaper_file", savedName).apply()
            refreshAsistenPrefsIntoState()
            showBannerMessage("Wallpaper Rem diperbarui")
        }
    }

    fun setStageWallpaperDim(dim: Float) {
        val clamped = dim.coerceIn(0f, 0.8f)
        asistenPrefs.edit().putFloat("asisten_wallpaper_dim", clamped).apply()
        val currentFile = _uiState.value.stageWallpaperFile
        _uiState.update { it.copy(stageWallpaperDim = clamped) }
        applyStageWallpaperToController(currentFile, clamped)
    }

    fun clearStageWallpaper() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            withContext(Dispatchers.IO) {
                File(app.filesDir, "stage_wallpaper").listFiles()?.forEach { it.delete() }
            }
            asistenPrefs.edit().remove("asisten_wallpaper_file").apply()
            refreshAsistenPrefsIntoState()
            showBannerMessage("Wallpaper Rem dikembalikan ke default")
        }
    }

    private fun decodeAndSaveWallpaperFile(app: Application, uri: Uri): String? {
        return runCatching {
            val maxDim = 1600
            val finalBitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(app.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val srcW = info.size.width
                    val srcH = info.size.height
                    val longest = maxOf(srcW, srcH)
                    if (longest > maxDim && srcW > 0 && srcH > 0) {
                        val scale = maxDim.toFloat() / longest.toFloat()
                        val targetW = (srcW * scale).toInt().coerceAtLeast(1)
                        val targetH = (srcH * scale).toInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                    }
                }
            } else {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val boundsStream = app.contentResolver.openInputStream(uri) ?: return null
                boundsStream.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOpts)
                }

                val srcW = boundsOpts.outWidth
                val srcH = boundsOpts.outHeight
                if (srcW <= 0 || srcH <= 0) return null

                var sampleSize = 1
                while (maxOf(srcW / sampleSize, srcH / sampleSize) > maxDim) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = app.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOpts)
                } ?: return null

                val longest = maxOf(decoded.width, decoded.height)
                if (longest > maxDim) {
                    val scale = maxDim.toFloat() / longest.toFloat()
                    val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                        if (it !== decoded) decoded.recycle()
                    }
                } else {
                    decoded
                }
            }

            val dir = File(app.filesDir, "stage_wallpaper").apply { mkdirs() }
            val newName = "wallpaper_${System.currentTimeMillis()}.jpg"
            val outFile = File(dir, newName)
            FileOutputStream(outFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            finalBitmap.recycle()

            dir.listFiles()?.forEach { existing ->
                if (existing.name != newName) existing.delete()
            }
            newName
        }.getOrNull()
    }

    fun getAiConfigValue(field: AiConfigField): String =
        ApiKeyStore.getValue(field, getApplication())

    fun saveAiConfigValue(field: AiConfigField, value: String) {
        ApiKeyStore.setValue(field, value, getApplication())
        refreshAsistenPrefsIntoState()
        val label = if (field.isSecret) {
            "${field.title} disimpan di Android Keystore"
        } else {
            "${field.title} diperbarui"
        }
        showBannerMessage(label)
    }

    fun testApiConnection() {
        if (_uiState.value.isTestingApi) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingApi = true, apiConnectionLabel = "Menguji…") }
            val (ok, label) = sseClient.testConnection()
            _uiState.update {
                it.copy(
                    isTestingApi = false,
                    apiConnectionOk = ok,
                    apiConnectionLabel = label
                )
            }
            showBannerMessage("Test Koneksi: $label")
        }
    }

    fun setNotifHabitEnabled(enabled: Boolean) {
        asistenPrefs.edit().putBoolean("asisten_notif_habit_on", enabled).apply()
        refreshAsistenPrefsIntoState()
    }

    fun setNotifTodoEnabled(enabled: Boolean) {
        asistenPrefs.edit().putBoolean("asisten_notif_todo_on", enabled).apply()
        refreshAsistenPrefsIntoState()
    }

    fun setNotifTimes(habitTime: String, todoTime: String) {
        dailyOsPrefs.edit()
            .putString("do_notif_habit", habitTime.ifBlank { "07:00" })
            .putString("do_notif_todo", todoTime.ifBlank { "15:00" })
            .apply()
        refreshDailyOsPrefsIntoState()
        refreshAsistenPrefsIntoState()
        showBannerMessage("Jadwal pengingat diperbarui ($habitTime & $todoTime)")
    }

    fun triggerTestNotification() {
        val app = getApplication<Application>()
        val ok = com.ecosystemku.assistant.notifications.DailyOsNotificationHelper.postTestNotification(app)
        if (ok) {
            showBannerMessage("Notifikasi uji dikirim ke status bar")
        } else {
            showBannerMessage("Izin notifikasi belum diberikan di perangkat")
        }
    }

    fun setAutoLearnMemory(enabled: Boolean) {
        asistenPrefs.edit().putBoolean("asisten_auto_learn", enabled).apply()
        _uiState.update { it.copy(autoLearnMemory = enabled) }
    }

    fun setDeviceLocationOptIn(enabled: Boolean) {
        asistenPrefs.edit().putBoolean("asisten_device_location_opt_in", enabled).apply()
        _uiState.update { it.copy(deviceLocationOptIn = enabled) }
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
            refreshMemoryProfile(force = true)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            userMemoryDao.deleteById(id)
            refreshMemoryProfile(force = true)
        }
    }

    fun deleteSummary(id: Long) {
        viewModelScope.launch {
            userMemoryDao.deleteSummaryById(id)
            refreshMemoryProfile(force = true)
        }
    }

    private suspend fun deleteMemoryByKeyOrQuery(query: String): Int {
        val raw = query.trim().lowercase()
        if (raw.isEmpty()) return 0
        val normKey = raw.replace(Regex("[^a-z0-9_]+"), "_").trim('_')
        val all = userMemoryDao.getAllMemories()
        val matched = all.filter { m ->
            (normKey.isNotEmpty() && m.factKey.equals(normKey, ignoreCase = true)) ||
                m.factKey.contains(raw, ignoreCase = true) ||
                m.factValue.contains(raw, ignoreCase = true)
        }
        matched.forEach { userMemoryDao.deleteById(it.id) }
        return matched.size
    }

    fun refreshMemoryProfile(force: Boolean = true) {
        if (_uiState.value.isSynthesizingMemoryProfile) return
        if (!ApiKeyStore.isLlmConfigured(getApplication())) {
            if (force) showBannerMessage("LLM belum diatur di Setelan")
            return
        }
        viewModelScope.launch {
            val allMemories = userMemoryDao.getAllMemories()
            val allSummaries = userMemoryDao.getLatestSummaries(10)
            if (allMemories.isEmpty() && allSummaries.isEmpty()) {
                clearMemoryProfile(showBanner = false)
                return@launch
            }
            val current = _uiState.value.memoryProfile
            if (!force && current.manuallyEdited) {
                return@launch
            }
            if (!force && current.sections.isNotEmpty() &&
                System.currentTimeMillis() - current.updatedAt < 45_000L
            ) {
                return@launch
            }
            _uiState.update { it.copy(isSynthesizingMemoryProfile = true) }
            try {
                val synthesized = memoryClient.synthesizeProfileSummary(allMemories, allSummaries)
                if (synthesized != null) {
                    val encoded = DailyOsHelpers.json.encodeToString(synthesized)
                    asistenPrefs.edit().putString("asisten_memory_profile_json", encoded).apply()
                    _uiState.update { it.copy(memoryProfile = synthesized) }
                    if (force && synthesized.sections.isNotEmpty()) {
                        showBannerMessage("Ringkasan Memori diperbarui")
                    }
                } else if (force) {
                    showBannerMessage("Gagal menyusun Ringkasan Memori")
                }
            } finally {
                _uiState.update { it.copy(isSynthesizingMemoryProfile = false) }
            }
        }
    }

    fun saveMemoryProfileSection(index: Int?, title: String, body: String) {
        val cleanTitle = title.trim().take(60)
        val cleanBody = body.trim().take(800)
        if (cleanTitle.isBlank() || cleanBody.isBlank()) return
        val current = _uiState.value.memoryProfile
        val updatedSections = current.sections.toMutableList()
        val item = MemoryProfileSectionDto(title = cleanTitle, body = cleanBody)
        if (index != null && index in updatedSections.indices) {
            updatedSections[index] = item
        } else {
            updatedSections.add(item)
        }
        val next = MemoryProfileData(
            sections = updatedSections,
            updatedAt = System.currentTimeMillis(),
            manuallyEdited = true
        )
        val encoded = DailyOsHelpers.json.encodeToString(next)
        asistenPrefs.edit().putString("asisten_memory_profile_json", encoded).apply()
        _uiState.update { it.copy(memoryProfile = next) }
        showBannerMessage(if (index != null) "Bagian ringkasan diperbarui" else "Bagian ringkasan ditambahkan")
    }

    fun deleteMemoryProfileSection(index: Int) {
        val current = _uiState.value.memoryProfile
        if (index !in current.sections.indices) return
        val updatedSections = current.sections.toMutableList().apply { removeAt(index) }
        if (updatedSections.isEmpty()) {
            clearMemoryProfile(showBanner = true)
            return
        }
        val next = MemoryProfileData(
            sections = updatedSections,
            updatedAt = System.currentTimeMillis(),
            manuallyEdited = true
        )
        val encoded = DailyOsHelpers.json.encodeToString(next)
        asistenPrefs.edit().putString("asisten_memory_profile_json", encoded).apply()
        _uiState.update { it.copy(memoryProfile = next) }
        showBannerMessage("Bagian ringkasan dihapus")
    }

    fun clearMemoryProfile(showBanner: Boolean = true) {
        asistenPrefs.edit().remove("asisten_memory_profile_json").apply()
        _uiState.update { it.copy(memoryProfile = MemoryProfileData()) }
        if (showBanner) showBannerMessage("Ringkasan Memori dihapus")
    }

    fun setKgMicroQuestionsEnabled(enabled: Boolean) {
        asistenPrefs.edit().putBoolean("asisten_kg_micro_questions", enabled).apply()
        _uiState.update { it.copy(kgMicroQuestionsEnabled = enabled) }
        showBannerMessage(
            if (enabled) "Pertanyaan refleksi ringan diaktifkan"
            else "Pertanyaan refleksi ringan dimatikan"
        )
    }

    fun setKgWeeklyReflectionEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        asistenPrefs.edit().putBoolean("asisten_kg_weekly_enabled", enabled).apply()
        _uiState.update { it.copy(kgWeeklyReflectionEnabled = enabled) }
        val st = _uiState.value
        com.ecosystemku.assistant.notifications.DailyOsNotificationHelper.syncWeeklyReflectionSchedule(
            context = app,
            enabled = enabled,
            dayOfWeek = st.kgWeeklyReflectionDay,
            timeStr = st.kgWeeklyReflectionTime
        )
        if (enabled && !com.ecosystemku.assistant.notifications.DailyOsNotificationHelper.hasNotificationPermission(app)) {
            _deviceEvents.tryEmit(
                DeviceActionEvent.RequestPermission(android.Manifest.permission.POST_NOTIFICATIONS, "Notifikasi")
            )
        }
        showBannerMessage(
            if (enabled) "Pengingat refleksi mingguan diaktifkan"
            else "Pengingat refleksi mingguan dimatikan"
        )
    }

    fun setKgWeeklyReflectionSchedule(dayOfWeek: Int, timeStr: String) {
        val app = getApplication<Application>()
        val cleanDay = dayOfWeek.coerceIn(Calendar.SUNDAY, Calendar.SATURDAY)
        val cleanTime = timeStr.trim().takeIf { it.matches(Regex("\\d{2}:\\d{2}")) } ?: "20:00"
        asistenPrefs.edit()
            .putInt("asisten_kg_weekly_day", cleanDay)
            .putString("asisten_kg_weekly_time", cleanTime)
            .apply()
        _uiState.update {
            it.copy(
                kgWeeklyReflectionDay = cleanDay,
                kgWeeklyReflectionTime = cleanTime
            )
        }
        com.ecosystemku.assistant.notifications.DailyOsNotificationHelper.syncWeeklyReflectionSchedule(
            context = app,
            enabled = _uiState.value.kgWeeklyReflectionEnabled,
            dayOfWeek = cleanDay,
            timeStr = cleanTime
        )
        showBannerMessage("Jadwal refleksi mingguan diperbarui ($cleanTime)")
    }

    fun syncKnowledgeGraphSignals(silent: Boolean = false) {
        if (_uiState.value.isSyncingKg) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingKg = true) }
            try {
                behavioralSignalSync.syncAll(_uiState.value.profileName)
                lastBehavioralKgSyncAt = System.currentTimeMillis()
                if (!silent) {
                    showBannerMessage("Sinyal Knowledge Graph disinkronkan")
                }
            } finally {
                _uiState.update { it.copy(isSyncingKg = false) }
            }
        }
    }

    fun ensureKgSyncedOnce() {
        if (lastBehavioralKgSyncAt == 0L) {
            syncKnowledgeGraphSignals(silent = true)
        }
    }

    fun addManualKgEdge(
        fromType: String,
        fromLabel: String,
        relation: String,
        toType: String,
        toLabel: String,
        evidence: String,
        replacesLabel: String = ""
    ) {
        if (fromLabel.isBlank() || toLabel.isBlank()) return
        viewModelScope.launch {
            val ok = kgRepository.addManualEdge(
                profileName = _uiState.value.profileName,
                fromType = fromType,
                fromLabel = fromLabel,
                relation = relation,
                toType = toType,
                toLabel = toLabel,
                evidence = evidence,
                replacesLabel = replacesLabel
            )
            if (ok) {
                showBannerMessage("Koneksi Knowledge Graph ditambahkan")
            } else {
                showBannerMessage("Simpul sumber dan tujuan tidak boleh sama")
            }
        }
    }

    fun markKgEdgeHistorical(edgeId: Long) {
        viewModelScope.launch {
            kgRepository.markEdgeHistorical(edgeId)
            showBannerMessage("Relasi ditandai historis (bobot diturunkan)")
        }
    }

    fun updateKgNode(nodeId: Long, label: String, summary: String) {
        if (label.isBlank()) return
        viewModelScope.launch {
            kgRepository.updateNodeDetails(nodeId, label, summary)
            showBannerMessage("Simpul Knowledge Graph diperbarui")
        }
    }

    fun deleteKgNode(nodeId: Long) {
        viewModelScope.launch {
            val target = kgDao.getNodeById(nodeId)
            if (target?.nodeType == "SELF") {
                showBannerMessage("Simpul pusat SELF tidak dapat dihapus")
                return@launch
            }
            kgRepository.deleteNode(nodeId)
            showBannerMessage("Simpul Knowledge Graph dihapus")
        }
    }

    fun deleteKgEdge(edgeId: Long) {
        viewModelScope.launch {
            kgRepository.deleteEdge(edgeId)
            showBannerMessage("Relasi Knowledge Graph dihapus")
        }
    }

    fun clearKnowledgeGraph() {
        viewModelScope.launch {
            kgDao.clearAll()
            lastBehavioralKgSyncAt = 0L
            showBannerMessage("Knowledge Graph dibersihkan")
        }
    }

    suspend fun buildDigitalTwinJson(): String {
        val memories = userMemoryDao.getAllMemories()
        val summaries = userMemoryDao.getLatestSummaries(30)
        return kgRepository.exportDigitalTwinJson(
            profileName = _uiState.value.profileName,
            memories = memories,
            summaries = summaries
        )
    }

    fun startKenaliAkuSession() {
        if (_uiState.value.isThinking) return
        if (!ApiKeyStore.isLlmConfigured(getApplication())) {
            _uiState.update {
                it.copy(errorMessage = "LLM belum diatur. Isi Base URL, Model, & API Key di Setelan.")
            }
            return
        }
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            audioManager.stop()
            val sid = createSessionWithGreeting(title = "Sesi Kenali Aku")
            asistenPrefs.edit().putLong("asisten_current_session_id", sid).apply()
            _activeSessionId.value = sid
            _uiState.update { it.copy(currentSessionId = sid) }
            val starterText = "Rem, ayo mulai Sesi Kenali Aku untuk memetakan pola pikir, nilai hidup, kebiasaan, dan tujuan jangka panjangku."
            val userMsgId = chatDao.insert(
                ChatMessageEntity(
                    sessionId = sid,
                    role = "user",
                    content = starterText,
                    createdAt = System.currentTimeMillis()
                )
            )
            streamAssistantReply(sessionId = sid, sourceUserMsgId = userMsgId)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            userMemoryDao.clearAll()
            userMemoryDao.clearSummaries()
            clearMemoryProfile(showBanner = false)
            showBannerMessage("Semua memori & rangkuman dibersihkan")
        }
    }

    fun resetAssistantData() {
        viewModelScope.launch {
            audioManager.stop()
            chatDao.clearAll()
            chatDao.clearAllSessions()
            userMemoryDao.clearAll()
            userMemoryDao.clearSummaries()
            kgDao.clearAll()
            lastBehavioralKgSyncAt = 0L
            clearMemoryProfile(showBanner = false)
            _activeSessionId.value = createSessionWithGreeting()
            showBannerMessage("Data Asisten berhasil direset")
        }
    }

    fun resetKronikData() {
        viewModelScope.launch {
            kronikSaveJob?.cancel()
            kronikDao.clearAll()
            showBannerMessage("Data Kronik berhasil direset")
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
            showBannerMessage("Data LinkKu berhasil direset")
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
            showBannerMessage("Data CuciMoney+ berhasil direset")
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
            showBannerMessage("Data IdeKu berhasil direset")
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
            showBannerMessage("Data Daily OS berhasil direset")
        }
    }

    fun resetAllEcosystemData() {
        viewModelScope.launch {
            audioManager.stop()
            kronikSaveJob?.cancel()
            dailyOsDao.replaceAllData(emptyList(), emptyList())
            dailyOsPrefs.edit()
                .remove("do_coach_memory")
                .remove("do_memory_date")
                .putBoolean("do_done_open", true)
                .apply()
            idekuDao.clearAll()
            idekuPrefs.edit().remove("ik_categories").remove("ik_view_mode").apply()
            kronikDao.clearAll()
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
            linkkuDao.clearAllLinks()
            linkkuDao.clearAllCategories()
            linkkuPrefs.edit()
                .remove("lk_pin")
                .remove("lk_tags_normal")
                .remove("lk_tags_priv")
                .putBoolean("lk_seeded_cats", true)
                .apply()
            linkkuDao.upsertCategories(LinkkuHelpers.DEFAULT_CATEGORIES)
            chatDao.clearAll()
            chatDao.clearAllSessions()
            userMemoryDao.clearAll()
            userMemoryDao.clearSummaries()
            kgDao.clearAll()
            lastBehavioralKgSyncAt = 0L
            clearMemoryProfile(showBanner = false)
            _activeSessionId.value = createSessionWithGreeting()
            refreshDailyOsPrefsIntoState()
            refreshIdekuPrefsIntoState()
            refreshCuciPrefsIntoState()
            refreshLinkkuPrefsIntoState()
            showBannerMessage("Semua data Asisten Rem berhasil direset")
        }
    }

    suspend fun buildEcosystemZipBytes(): ByteArray {
        val bytes = backupRepository.buildEcosystemZipBytes(updateLocalStamp = true)
        refreshAsistenPrefsIntoState()
        return bytes
    }

    suspend fun importEcosystemZipBytes(
        zipBytes: ByteArray,
        replaceAll: Boolean,
        linkkuPin: String? = null
    ): EcosystemZipImportResult {
        val res = backupRepository.importEcosystemZipBytes(zipBytes, replaceAll, linkkuPin)
        if (res is EcosystemZipImportResult.Success) {
            refreshAsistenPrefsIntoState()
            refreshDailyOsPrefsIntoState()
            refreshCuciPrefsIntoState()
            refreshLinkkuPrefsIntoState()
            _activeSessionId.value = res.latestSessionId ?: createSessionWithGreeting()
        }
        return res
    }

    suspend fun connectGoogleDrive(): CloudAuthResult {
        val res = googleDriveClient.authorizeInteractive()
        if (res is CloudAuthResult.Authorized) {
            asistenPrefs.edit()
                .putBoolean(CloudBackupPrefsKeys.PREF_CONNECTED, true)
                .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, "Terhubung ke Google Drive")
                .apply()
            refreshAsistenPrefsIntoState()
            refreshCloudRemoteInfo()
        }
        return res
    }

    suspend fun handleGoogleDriveAuthIntent(data: Intent?): CloudAuthResult {
        val res = googleDriveClient.extractTokenFromIntent(data)
        if (res is CloudAuthResult.Authorized) {
            asistenPrefs.edit()
                .putBoolean(CloudBackupPrefsKeys.PREF_CONNECTED, true)
                .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, "Terhubung ke Google Drive")
                .apply()
            refreshAsistenPrefsIntoState()
            refreshCloudRemoteInfo()
        } else if (res is CloudAuthResult.Failure) {
            asistenPrefs.edit()
                .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, res.message)
                .apply()
            refreshAsistenPrefsIntoState()
        }
        return res
    }

    fun disconnectGoogleDrive() {
        asistenPrefs.edit()
            .putBoolean(CloudBackupPrefsKeys.PREF_CONNECTED, false)
            .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, "Terputus dari Google Drive")
            .apply()
        _uiState.update { it.copy(cloudState = it.cloudState.copy(remoteBackupInfo = null)) }
        refreshAsistenPrefsIntoState()
    }

    suspend fun refreshCloudRemoteInfo() {
        _uiState.update { it.copy(cloudState = it.cloudState.copy(isSyncing = true)) }
        when (val res = googleDriveClient.getRemoteBackupInfo()) {
            is CloudSyncResult.Success -> {
                _uiState.update {
                    it.copy(
                        cloudState = it.cloudState.copy(
                            connected = true,
                            remoteBackupInfo = res.data,
                            isSyncing = false
                        )
                    )
                }
            }
            CloudSyncResult.NeedsReauth -> {
                asistenPrefs.edit()
                    .putBoolean(CloudBackupPrefsKeys.PREF_CONNECTED, false)
                    .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, "Perlu hubungkan ulang Google Drive")
                    .apply()
                _uiState.update { it.copy(cloudState = it.cloudState.copy(isSyncing = false)) }
                refreshAsistenPrefsIntoState()
            }
            is CloudSyncResult.Failure -> {
                asistenPrefs.edit()
                    .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, res.message)
                    .apply()
                _uiState.update { it.copy(cloudState = it.cloudState.copy(isSyncing = false)) }
                refreshAsistenPrefsIntoState()
            }
        }
    }

    suspend fun backupToCloudNow(): String {
        _uiState.update { it.copy(cloudState = it.cloudState.copy(isSyncing = true)) }
        return try {
            val zipBytes = backupRepository.buildEcosystemZipBytes(updateLocalStamp = false)
            val passphrase = ApiKeyStore.getCloudPassphrase(getApplication())
            val (payload, encrypted) = if (passphrase.isNotEmpty()) {
                CloudBackupCrypto.encrypt(zipBytes, passphrase) to true
            } else {
                zipBytes to false
            }

            when (val res = googleDriveClient.uploadBackup(payload, encrypted)) {
                is CloudSyncResult.Success -> {
                    val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID")).format(Date())
                    asistenPrefs.edit()
                        .putBoolean(CloudBackupPrefsKeys.PREF_CONNECTED, true)
                        .putString(CloudBackupPrefsKeys.PREF_LAST_BACKUP, stamp)
                        .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, "Cadangan terakhir berhasil ($stamp)")
                        .apply()
                    _uiState.update {
                        it.copy(cloudState = it.cloudState.copy(remoteBackupInfo = res.data, isSyncing = false))
                    }
                    refreshAsistenPrefsIntoState()
                    "Cadangan berhasil diunggah ke Google Drive"
                }
                CloudSyncResult.NeedsReauth -> {
                    asistenPrefs.edit()
                        .putBoolean(CloudBackupPrefsKeys.PREF_CONNECTED, false)
                        .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, "Izin Google Drive habis. Hubungkan ulang.")
                        .apply()
                    _uiState.update { it.copy(cloudState = it.cloudState.copy(isSyncing = false)) }
                    refreshAsistenPrefsIntoState()
                    "Izin Google Drive habis, silakan hubungkan ulang akun"
                }
                is CloudSyncResult.Failure -> {
                    asistenPrefs.edit()
                        .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, res.message)
                        .apply()
                    _uiState.update { it.copy(cloudState = it.cloudState.copy(isSyncing = false)) }
                    refreshAsistenPrefsIntoState()
                    res.message
                }
            }
        } catch (t: Throwable) {
            val msg = "Gagal membuat cadangan cloud: ${t.message}"
            asistenPrefs.edit().putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, msg).apply()
            _uiState.update { it.copy(cloudState = it.cloudState.copy(isSyncing = false)) }
            refreshAsistenPrefsIntoState()
            msg
        }
    }

    suspend fun prepareCloudRestore(): CloudRestoreFetchResult {
        _uiState.update { it.copy(cloudState = it.cloudState.copy(isSyncing = true)) }
        return try {
            val remoteInfo = when (val infoRes = googleDriveClient.getRemoteBackupInfo()) {
                is CloudSyncResult.Success -> infoRes.data
                CloudSyncResult.NeedsReauth -> {
                    asistenPrefs.edit()
                        .putBoolean(CloudBackupPrefsKeys.PREF_CONNECTED, false)
                        .putString(CloudBackupPrefsKeys.PREF_LAST_STATUS, "Izin Google Drive habis. Hubungkan ulang.")
                        .apply()
                    refreshAsistenPrefsIntoState()
                    return CloudRestoreFetchResult.NeedsReauth
                }
                is CloudSyncResult.Failure -> return CloudRestoreFetchResult.Error(infoRes.message)
            } ?: return CloudRestoreFetchResult.Error("Belum ada file cadangan tersimpan di Google Drive")

            _uiState.update { it.copy(cloudState = it.cloudState.copy(remoteBackupInfo = remoteInfo)) }

            when (val dl = googleDriveClient.downloadBackup(remoteInfo.fileId)) {
                is CloudSyncResult.Success -> {
                    val rawBytes = dl.data
                    if (CloudBackupCrypto.isEncrypted(rawBytes)) {
                        val savedPass = ApiKeyStore.getCloudPassphrase(getApplication())
                        if (savedPass.isNotEmpty()) {
                            val dec = runCatching { CloudBackupCrypto.decrypt(rawBytes, savedPass) }.getOrNull()
                            if (dec != null) {
                                CloudRestoreFetchResult.Ready(dec)
                            } else {
                                CloudRestoreFetchResult.NeedsPassphrase(rawBytes, wrongSavedPassphrase = true)
                            }
                        } else {
                            CloudRestoreFetchResult.NeedsPassphrase(rawBytes, wrongSavedPassphrase = false)
                        }
                    } else {
                        CloudRestoreFetchResult.Ready(rawBytes)
                    }
                }
                CloudSyncResult.NeedsReauth -> CloudRestoreFetchResult.NeedsReauth
                is CloudSyncResult.Failure -> CloudRestoreFetchResult.Error(dl.message)
            }
        } finally {
            _uiState.update { it.copy(cloudState = it.cloudState.copy(isSyncing = false)) }
        }
    }

    fun decryptCloudBytesWithPassphrase(encryptedBytes: ByteArray, passphrase: String): Result<ByteArray> =
        runCatching { CloudBackupCrypto.decrypt(encryptedBytes, passphrase) }

    fun setCloudAutoSchedule(schedule: String, wifiOnly: Boolean) {
        asistenPrefs.edit()
            .putString(CloudBackupPrefsKeys.PREF_AUTO_SCHEDULE, schedule)
            .putBoolean(CloudBackupPrefsKeys.PREF_WIFI_ONLY, wifiOnly)
            .apply()
        refreshAsistenPrefsIntoState()
    }

    fun setCloudPassphrase(passphrase: String) {
        ApiKeyStore.setCloudPassphrase(passphrase, getApplication())
        refreshAsistenPrefsIntoState()
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

    private suspend fun markMatchingIdekuWishlistDone(title: String, wishId: String) {
        val idNum = wishId.removePrefix("wish_").toLongOrNull()
        val ideas = idekuDao.getAll()
        val target = (if (idNum != null) ideas.firstOrNull { it.id == idNum && it.type == "Wishlist" } else null)
            ?: ideas.firstOrNull { it.type == "Wishlist" && it.title.equals(title, ignoreCase = true) && it.status != "Selesai" }
            ?: ideas.firstOrNull { it.type == "Wishlist" && it.title.contains(title, ignoreCase = true) && it.status != "Selesai" }
        if (target != null && target.status != "Selesai") {
            idekuDao.upsert(target.copy(status = "Selesai"))
        }
    }

    private suspend fun depositToWishlistInternal(
        wishId: String,
        accountId: Long,
        amount: Long,
        bookIdOverride: Long?
    ): String? {
        if (amount <= 0L) return "Nominal setoran harus lebih dari 0"
        val wish = cuciMoneyDao.getWishlistById(wishId) ?: return "Wishlist tidak ditemukan"
        val srcAcc = cuciMoneyDao.getAccountById(accountId) ?: return "Akun sumber tidak ditemukan"
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
        val bookId = bookIdOverride ?: _uiState.value.cuciMoneyState.currentBookId
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
        return null
    }

    private suspend fun withdrawWishlistInternal(
        wishId: String,
        accountId: Long,
        realize: Boolean,
        markDone: Boolean,
        bookIdOverride: Long?
    ): String? {
        val wish = cuciMoneyDao.getWishlistById(wishId) ?: return "Wishlist tidak ditemukan"
        val amt = wish.saved
        if (amt <= 0L) return "Saldo tabungan wishlist ini kosong"
        val dstAcc = cuciMoneyDao.getAccountById(accountId) ?: return "Akun tujuan tidak ditemukan"
        val savingsAcc = cuciMoneyDao.getAllAccounts().firstOrNull { it.isSavings }
            ?: return "Akun Tabungan tidak ditemukan"

        val bookId = bookIdOverride ?: _uiState.value.cuciMoneyState.currentBookId
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
        val nextStatus = if (realize && markDone) "Selesai" else wish.status
        cuciMoneyDao.upsertWishlist(
            wish.copy(
                saved = 0L,
                status = nextStatus,
                realizedAt = if (realize) now else wish.realizedAt,
                realizedAmount = if (realize) amt else wish.realizedAmount,
                historyJson = CuciMoneyHelpers.encodeWishlistHistory(hist)
            )
        )
        if (realize && markDone) {
            markMatchingIdekuWishlistDone(wish.title, wish.id)
        }
        return null
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
            depositToWishlistInternal(wishId, accountId, amount, null)
        },
        onWithdrawWishlist = { wishId, accountId, realize, markDone ->
            withdrawWishlistInternal(wishId, accountId, realize, markDone, null)
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
        sendMessageInternal(null)
    }

    fun selectMessageChoices(messageId: Long, selectedItems: List<String>) {
        val cleaned = selectedItems.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleaned.isEmpty() || _uiState.value.isThinking) return
        val target = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val existingChoices = ChatChoicesCodec.decode(target.choicesJson) ?: return
        if (existingChoices.selected.isNotEmpty()) return
        val updatedChoices = existingChoices.copy(selected = cleaned)
        val encoded = ChatChoicesCodec.encode(updatedChoices)
        viewModelScope.launch {
            chatDao.updateChoicesJson(messageId, encoded)
            sendMessageInternal(cleaned.joinToString(", "))
        }
    }

    private fun sendMessageInternal(overrideText: String?) {
        val isFromInput = overrideText == null
        val text = (overrideText ?: _inputText.value).trim()
        if (text.isEmpty() || _uiState.value.isThinking) return
        if (!ApiKeyStore.isLlmConfigured(getApplication())) {
            _uiState.update {
                it.copy(errorMessage = "LLM belum diatur. Isi Base URL, Model, & API Key di Setelan.")
            }
            return
        }

        if (isFromInput) _inputText.value = ""

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            val nowTs = System.currentTimeMillis()
            var sid = _activeSessionId.value
            if (sid <= 0L || chatDao.getSessionById(sid) == null) {
                sid = createSessionWithGreeting()
                asistenPrefs.edit().putLong("asisten_current_session_id", sid).apply()
                _activeSessionId.value = sid
                _uiState.update { it.copy(currentSessionId = sid) }
            }
            val userMsgId = chatDao.insert(
                ChatMessageEntity(
                    sessionId = sid,
                    role = "user",
                    content = text,
                    createdAt = nowTs
                )
            )
            val session = chatDao.getSessionById(sid)
            if (session != null && session.title == "Obrolan Baru") {
                val cleanTitle = text.replace('\n', ' ').trim()
                val autoTitle = if (cleanTitle.length > 42) cleanTitle.take(42) + "..." else cleanTitle.ifBlank { "Obrolan" }
                chatDao.updateSession(sid, autoTitle, nowTs)
            } else {
                chatDao.touchSession(sid, nowTs)
            }
            streamAssistantReply(sessionId = sid, sourceUserMsgId = userMsgId)
        }
    }

    private suspend fun streamAssistantReply(
        sessionId: Long,
        sourceUserMsgId: Long,
        historyOverride: List<ChatTurnDto>? = null
    ) {
        if (!ApiKeyStore.isLlmConfigured(getApplication())) {
            _uiState.update {
                it.copy(errorMessage = "LLM belum diatur. Isi Base URL, Model, & API Key di Setelan.")
            }
            return
        }

        resetIdleTimer()
        hideIdleBubble()
        audioManager.stop()

        _streamingText.value = ""
        _uiState.update {
            it.copy(
                isThinking = true,
                errorMessage = null
            )
        }
        applyExpression("puzzle", autoRevert = false)

        val recent = historyOverride ?: chatDao.getRecentBySessionDesc(sessionId, 10)
            .reversed()
            .map { ChatTurnDto(role = it.role, content = it.content) }

        val accumulatedSearch = StringBuilder()
        var parsedResult: SseTagParser.ParsedFinal? = null
        var finalDoneTts = ""

        for (round in 0..2) {
            val personalizedPrompt = buildPersonalizedPrompt(
                sessionId = sessionId,
                currentUserMsgId = sourceUserMsgId,
                searchContextOverride = accumulatedSearch.toString().takeIf { it.isNotBlank() },
                forbidMoreSearch = round >= 2
            )

            val rawAcc = StringBuilder()
            var finalDoneText = ""
            var roundDoneTts = ""
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
                        if (visible.isNotEmpty()) {
                            _streamingText.value = visible
                        }
                    }
                    is ChatStreamEvent.Done -> {
                        finalDoneText = event.text
                        roundDoneTts = event.tts
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
                return
            }

            val fallbackRaw = SseTagParser.visibleTextDuringStream(rawAcc.toString())
            val candidateParsed = SseTagParser.parseFinal(finalDoneText.ifBlank { fallbackRaw })
            val searchAct = candidateParsed.searchAction
            if (searchAct != null && round < 2) {
                val queryLabel = if (searchAct.query.isNotBlank()) ": ${searchAct.query}" else ""
                _streamingText.value = "Mencari arsip ${searchAct.scope}$queryLabel..."
                audioManager.playBundledAssetClip("ack_2.mp3")
                val searchOutput = executeLocalArchiveSearch(searchAct.scope, searchAct.query)
                accumulatedSearch.append("### Pencarian [${searchAct.scope} | \"${searchAct.query}\"]\n")
                accumulatedSearch.append(searchOutput).append("\n\n")
                _streamingText.value = ""
                continue
            }

            parsedResult = candidateParsed
            finalDoneTts = roundDoneTts
            break
        }

        val parsed = parsedResult ?: return
        val replyText = parsed.cleanText
        val expr = parsed.expression
        val choicesJson = ChatChoicesCodec.encode(parsed.choices)
        var didRealizeWishlist = false
        var didMutateModuleAction = false

        if (replyText.isNotEmpty()) {
            val replyTs = System.currentTimeMillis()
            chatDao.insert(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    content = replyText,
                    expressionTag = expr,
                    createdAt = replyTs,
                    choicesJson = choicesJson
                )
            )
            chatDao.touchSession(sessionId, replyTs)
        }

            if (parsed.memoryActions.isNotEmpty()) {
                var memChanged = false
                var lastLabel = ""
                parsed.memoryActions.forEach { act ->
                    when (act) {
                        is SseTagParser.MemoryChatAction.SetMemory -> {
                            userMemoryDao.upsertByFactKey(
                                category = act.category,
                                factKey = act.factKey,
                                factValue = act.factValue,
                                confidence = 0.95f,
                                sourceMessageId = sourceUserMsgId
                            )
                            memChanged = true
                            lastLabel = "Memori diperbarui"
                        }
                        is SseTagParser.MemoryChatAction.DeleteMemory -> {
                            if (deleteMemoryByKeyOrQuery(act.query) > 0) {
                                memChanged = true
                                lastLabel = "Memori dihapus"
                            }
                        }
                    }
                }
                if (memChanged) {
                    didMutateModuleAction = true
                    refreshMemoryProfile(force = true)
                    showBannerMessage(lastLabel.ifBlank { "Memori diperbarui" })
                }
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
                    didMutateModuleAction = true
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
                    didMutateModuleAction = true
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
                val today = CuciMoneyHelpers.todayDateStr()
                val savedLabels = mutableListOf<String>()

                fun resolveBook(bookName: String): Pair<Long, List<CuciAccountEntity>> {
                    val targetBook = if (bookName.isNotBlank()) {
                        cuciState.books.firstOrNull { it.name.equals(bookName, ignoreCase = true) }
                            ?: cuciState.books.firstOrNull { it.name.contains(bookName, ignoreCase = true) }
                            ?: activeBook
                    } else {
                        activeBook
                    }
                    val targetBookId = targetBook?.id ?: cuciState.currentBookId
                    return targetBookId to CuciMoneyHelpers.bookAccounts(cuciState, targetBookId)
                }

                fun resolveAccount(targetBookId: Long, targetBookAccs: List<CuciAccountEntity>, accountName: String): Long {
                    val fallbackAccId = cuciState.lastAccByBook[targetBookId]
                        ?.takeIf { id -> targetBookAccs.any { it.id == id } }
                        ?: targetBookAccs.firstOrNull { !it.isSavings }?.id
                        ?: cuciState.accounts.firstOrNull { !it.isSavings }?.id
                        ?: 1L
                    return if (accountName.isNotBlank()) {
                        targetBookAccs.firstOrNull { !it.isSavings && it.name.contains(accountName, ignoreCase = true) }?.id
                            ?: cuciState.accounts.firstOrNull { !it.isSavings && it.name.contains(accountName, ignoreCase = true) }?.id
                            ?: fallbackAccId
                    } else {
                        fallbackAccId
                    }
                }

                parsed.cuciActions.forEach { act ->
                    when (act) {
                        is SseTagParser.CuciChatAction.AddTransaction -> {
                            val dir = if (act.direction == "in") "in" else "out"
                            val (targetBookId, targetBookAccs) = resolveBook(act.bookName)
                            val matchedAccId = resolveAccount(targetBookId, targetBookAccs, act.accountName)

                            val matchedCat = cuciState.categories.firstOrNull { c ->
                                act.categoryName.isNotBlank() &&
                                    c.name.equals(act.categoryName, ignoreCase = true) &&
                                    (c.type == "both" || (dir == "in" && c.type == "income") || (dir == "out" && c.type == "expense"))
                            } ?: cuciState.categories.firstOrNull { c ->
                                (dir == "in" && c.type == "income") || (dir == "out" && c.type == "expense")
                            } ?: cuciState.categories.firstOrNull()

                            val trxDate = act.date.ifBlank { today }
                            val trx = CuciTransactionEntity(
                                id = CuciMoneyHelpers.generateId(),
                                bookId = targetBookId,
                                title = act.title,
                                amount = act.amount,
                                direction = dir,
                                categoryId = matchedCat?.id ?: 1L,
                                accountId = matchedAccId,
                                date = trxDate,
                                memberId = cuciState.members.firstOrNull()?.id ?: "m1",
                                transferPair = null,
                                createdAt = System.currentTimeMillis()
                            )
                            cuciMoneyDao.upsertTransaction(trx)
                            cuciMoneyDao.adjustAccountBalance(matchedAccId, if (dir == "in") act.amount else -act.amount)
                            saveLastAccForBook(targetBookId, matchedAccId)
                            savedLabels.add("${act.title} (${CuciMoneyHelpers.formatRp(act.amount)})")
                        }
                        is SseTagParser.CuciChatAction.SaveWishlist -> {
                            val (targetBookId, targetBookAccs) = resolveBook(act.bookName)
                            val srcAccId = resolveAccount(targetBookId, targetBookAccs, act.accountName)
                            val allWishes = cuciMoneyDao.getAllWishlists()
                            var targetWish = allWishes.firstOrNull {
                                it.status != "Selesai" && it.title.equals(act.wishlistQuery, ignoreCase = true)
                            } ?: allWishes.firstOrNull {
                                it.status != "Selesai" && it.title.contains(act.wishlistQuery, ignoreCase = true)
                            }
                            if (targetWish == null) {
                                val newId = "wish_${CuciMoneyHelpers.generateId()}"
                                targetWish = CuciWishlistEntity(
                                    id = newId,
                                    title = act.wishlistQuery,
                                    price = act.amount,
                                    priority = "Nanti",
                                    image = "bag",
                                    status = "Belum"
                                )
                                cuciMoneyDao.upsertWishlist(targetWish)
                            }
                            val err = depositToWishlistInternal(targetWish.id, srcAccId, act.amount, targetBookId)
                            if (err == null) {
                                saveLastAccForBook(targetBookId, srcAccId)
                                savedLabels.add("Tabungan ${targetWish.title} (+${CuciMoneyHelpers.formatRp(act.amount)})")
                            }
                        }
                        is SseTagParser.CuciChatAction.RealizeWishlist -> {
                            val (targetBookId, targetBookAccs) = resolveBook(act.bookName)
                            val dstAccId = resolveAccount(targetBookId, targetBookAccs, act.accountName)
                            val allWishes = cuciMoneyDao.getAllWishlists()
                            val wish = allWishes.firstOrNull {
                                it.status != "Selesai" && it.title.equals(act.wishlistQuery, ignoreCase = true)
                            } ?: allWishes.firstOrNull {
                                it.status != "Selesai" && it.title.contains(act.wishlistQuery, ignoreCase = true)
                            } ?: allWishes.firstOrNull {
                                it.title.contains(act.wishlistQuery, ignoreCase = true)
                            }
                            val nowTs = System.currentTimeMillis()
                            val wishTitle = wish?.title ?: act.wishlistQuery
                            val hadSaved = (wish?.saved ?: 0L) > 0L
                            if (wish != null) {
                                if (hadSaved) {
                                    withdrawWishlistInternal(wish.id, dstAccId, realize = true, markDone = true, bookIdOverride = targetBookId)
                                } else {
                                    val rAmt = if (act.paidAmount > 0L) act.paidAmount else wish.price
                                    cuciMoneyDao.upsertWishlist(
                                        wish.copy(
                                            status = "Selesai",
                                            realizedAt = nowTs,
                                            realizedAmount = rAmt
                                        )
                                    )
                                    markMatchingIdekuWishlistDone(wish.title, wish.id)
                                }
                            } else {
                                markMatchingIdekuWishlistDone(act.wishlistQuery, "")
                            }
                            if (!hadSaved && act.paidAmount > 0L) {
                                val buyCat = cuciState.categories.firstOrNull {
                                    it.name.equals("Belanja", ignoreCase = true)
                                } ?: cuciState.categories.firstOrNull { it.type == "expense" }
                                val trx = CuciTransactionEntity(
                                    id = CuciMoneyHelpers.generateId(),
                                    bookId = targetBookId,
                                    title = "Beli Wishlist: $wishTitle",
                                    amount = act.paidAmount,
                                    direction = "out",
                                    categoryId = buyCat?.id ?: 4L,
                                    accountId = dstAccId,
                                    date = today,
                                    memberId = cuciState.members.firstOrNull()?.id ?: "m1",
                                    transferPair = null,
                                    createdAt = nowTs
                                )
                                cuciMoneyDao.upsertTransaction(trx)
                                cuciMoneyDao.adjustAccountBalance(dstAccId, -act.paidAmount)
                                saveLastAccForBook(targetBookId, dstAccId)
                            }
                            didRealizeWishlist = true
                            savedLabels.add("Wishlist Selesai: $wishTitle")
                        }
                        is SseTagParser.CuciChatAction.EditTransaction -> {
                            val allTrx = cuciMoneyDao.getAllTransactions()
                            val targetTrx = resolveCuciTransactionTarget(
                                target = act.target,
                                allTrx = allTrx,
                                activeBookId = activeBook?.id ?: cuciState.currentBookId,
                                today = today
                            )
                            if (targetTrx != null && targetTrx.savingsPot == null && targetTrx.transferPair == null) {
                                val nextAmount = act.amount ?: targetTrx.amount
                                val nextTitle = act.title.ifBlank { targetTrx.title }
                                val nextDate = act.date.ifBlank { targetTrx.date }
                                val dir = targetTrx.direction
                                val nextCatId = if (act.categoryName.isNotBlank()) {
                                    cuciState.categories.firstOrNull { c ->
                                        c.name.equals(act.categoryName, ignoreCase = true) &&
                                            (c.type == "both" || (dir == "in" && c.type == "income") || (dir == "out" && c.type == "expense"))
                                    }?.id ?: targetTrx.categoryId
                                } else targetTrx.categoryId
                                val nextAccId = if (act.accountName.isNotBlank()) {
                                    val bookAccs = CuciMoneyHelpers.bookAccounts(cuciState, targetTrx.bookId)
                                    bookAccs.firstOrNull { !it.isSavings && it.name.contains(act.accountName, ignoreCase = true) }?.id
                                        ?: cuciState.accounts.firstOrNull { !it.isSavings && it.name.contains(act.accountName, ignoreCase = true) }?.id
                                        ?: targetTrx.accountId
                                } else targetTrx.accountId

                                val rev = if (targetTrx.direction == "in") -targetTrx.amount else targetTrx.amount
                                cuciMoneyDao.adjustAccountBalance(targetTrx.accountId, rev)
                                val updatedTrx = targetTrx.copy(
                                    title = nextTitle,
                                    amount = nextAmount,
                                    categoryId = nextCatId,
                                    accountId = nextAccId,
                                    date = nextDate
                                )
                                cuciMoneyDao.upsertTransaction(updatedTrx)
                                cuciMoneyDao.adjustAccountBalance(nextAccId, if (dir == "in") nextAmount else -nextAmount)
                                savedLabels.add("Diubah: $nextTitle (${CuciMoneyHelpers.formatRp(nextAmount)})")
                            }
                        }
                        is SseTagParser.CuciChatAction.DeleteTransaction -> {
                            val allTrx = cuciMoneyDao.getAllTransactions()
                            val targetTrx = resolveCuciTransactionTarget(
                                target = act.target,
                                allTrx = allTrx,
                                activeBookId = activeBook?.id ?: cuciState.currentBookId,
                                today = today
                            )
                            if (targetTrx != null && targetTrx.savingsPot == null) {
                                if (targetTrx.transferPair != null) {
                                    val pairs = cuciMoneyDao.getTransactionsByPair(targetTrx.transferPair)
                                    pairs.forEach { p ->
                                        val rev = if (p.direction == "in") -p.amount else p.amount
                                        cuciMoneyDao.adjustAccountBalance(p.accountId, rev)
                                    }
                                    cuciMoneyDao.deleteTransactionsByPair(targetTrx.transferPair)
                                } else {
                                    val rev = if (targetTrx.direction == "in") -targetTrx.amount else targetTrx.amount
                                    cuciMoneyDao.adjustAccountBalance(targetTrx.accountId, rev)
                                    cuciMoneyDao.deleteTransactionById(targetTrx.id)
                                }
                                savedLabels.add("Dihapus: ${targetTrx.title}")
                            }
                        }
                    }
                }
                if (savedLabels.isNotEmpty()) {
                    didMutateModuleAction = true
                    _uiState.update {
                        it.copy(kronikBannerMessage = "CuciMoney+: ${savedLabels.first()}")
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
                            savedTitles.add("Tersimpan \"${entity.title}\"")
                        }
                        is SseTagParser.IdekuChatAction.SetStatus -> {
                            val existing = resolveIdekuTarget(act.target, idekuDao.getAll())
                            if (existing != null) {
                                val updated = existing.copy(status = act.status)
                                idekuDao.upsert(updated)
                                if (updated.type == "Wishlist") {
                                    syncIdekuWishlistToCuciMoney(updated)
                                }
                                savedTitles.add("${updated.title} -> ${updated.status}")
                            }
                        }
                        is SseTagParser.IdekuChatAction.EditItem -> {
                            val existing = resolveIdekuTarget(act.target, idekuDao.getAll())
                            if (existing != null) {
                                val updated = existing.copy(
                                    title = act.title.ifBlank { existing.title },
                                    category = act.category.ifBlank { existing.category },
                                    priority = act.priority.ifBlank { existing.priority },
                                    notes = act.notes.ifBlank { existing.notes },
                                    jalur = if (existing.type == "Wishlist") "" else act.jalur.ifBlank { existing.jalur },
                                    price = if (existing.type == "Wishlist") (act.price ?: existing.price).coerceAtLeast(0L) else 0L
                                )
                                idekuDao.upsert(updated)
                                if (updated.type == "Wishlist") {
                                    syncIdekuWishlistToCuciMoney(updated)
                                }
                                savedTitles.add("Diubah \"${updated.title}\"")
                            }
                        }
                        is SseTagParser.IdekuChatAction.DeleteItem -> {
                            val existing = resolveIdekuTarget(act.target, idekuDao.getAll())
                            if (existing != null) {
                                idekuDao.deleteById(existing.id)
                                savedTitles.add("Dihapus \"${existing.title}\"")
                            }
                        }
                    }
                }
                if (savedTitles.isNotEmpty()) {
                    didMutateModuleAction = true
                    _uiState.update {
                        it.copy(kronikBannerMessage = "IdeKu: ${savedTitles.first()}")
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
                            val allHabits = dailyOsDao.getAllHabits()
                            val matchedHabit = allHabits.firstOrNull { !it.archived && it.id.equals(q, ignoreCase = true) }
                                ?: allHabits.firstOrNull { !it.archived && it.name.equals(q, ignoreCase = true) }
                                ?: allHabits.firstOrNull { !it.archived && it.name.contains(q, ignoreCase = true) }
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
                                val allTodos = dailyOsDao.getAllTodos()
                                val matchedTodo = allTodos.firstOrNull { !it.done && it.id.equals(q, ignoreCase = true) }
                                    ?: allTodos.firstOrNull { !it.done && it.title.equals(q, ignoreCase = true) }
                                    ?: allTodos.firstOrNull { !it.done && it.title.contains(q, ignoreCase = true) }
                                if (matchedTodo != null) {
                                    dailyOsDao.upsertTodo(matchedTodo.copy(done = true, completedAt = Instant.now().toString()))
                                    savedMsgs.add("Selesai \"${matchedTodo.title}\"")
                                }
                            }
                        }
                        is SseTagParser.DailyOsChatAction.UncheckItem -> {
                            val q = act.query.trim()
                            val allHabits = dailyOsDao.getAllHabits()
                            val matchedHabit = allHabits.firstOrNull { !it.archived && it.id.equals(q, ignoreCase = true) }
                                ?: allHabits.firstOrNull { !it.archived && it.name.equals(q, ignoreCase = true) }
                                ?: allHabits.firstOrNull { !it.archived && it.name.contains(q, ignoreCase = true) }
                            if (matchedHabit != null) {
                                val hist = DailyOsHelpers.parseHistory(matchedHabit.historyJson).toMutableMap()
                                if (!hist[today].isNullOrEmpty()) {
                                    hist.remove(today)
                                    dailyOsDao.upsertHabit(matchedHabit.copy(historyJson = DailyOsHelpers.encodeHistory(hist)))
                                    savedMsgs.add("Batal centang \"${matchedHabit.name}\"")
                                }
                            } else {
                                val allTodos = dailyOsDao.getAllTodos()
                                val matchedTodo = allTodos.firstOrNull { it.done && it.id.equals(q, ignoreCase = true) }
                                    ?: allTodos.firstOrNull { it.done && it.title.equals(q, ignoreCase = true) }
                                    ?: allTodos.firstOrNull { it.done && it.title.contains(q, ignoreCase = true) }
                                if (matchedTodo != null) {
                                    dailyOsDao.upsertTodo(matchedTodo.copy(done = false, completedAt = null))
                                    savedMsgs.add("Dibuka lagi \"${matchedTodo.title}\"")
                                }
                            }
                        }
                        is SseTagParser.DailyOsChatAction.EditTodo -> {
                            val matchedTodo = resolveDailyOsTodoTarget(act.target, dailyOsDao.getAllTodos())
                            if (matchedTodo != null) {
                                val nextDl = when (act.deadline) {
                                    "__CLEAR__" -> ""
                                    "" -> matchedTodo.deadline
                                    else -> act.deadline
                                }
                                val updated = matchedTodo.copy(
                                    title = act.title.ifBlank { matchedTodo.title },
                                    priority = act.priority.ifBlank { matchedTodo.priority },
                                    deadline = nextDl
                                )
                                dailyOsDao.upsertTodo(updated)
                                savedMsgs.add("Diubah \"${updated.title}\"")
                            }
                        }
                        is SseTagParser.DailyOsChatAction.DeleteTodo -> {
                            val matchedTodo = resolveDailyOsTodoTarget(act.target, dailyOsDao.getAllTodos())
                            if (matchedTodo != null) {
                                dailyOsDao.deleteTodoById(matchedTodo.id)
                                savedMsgs.add("Dihapus \"${matchedTodo.title}\"")
                            }
                        }
                        is SseTagParser.DailyOsChatAction.ArchiveHabit -> {
                            val q = act.target.trim()
                            val allHabits = dailyOsDao.getAllHabits()
                            val matchedHabit = allHabits.firstOrNull { it.id.equals(q, ignoreCase = true) }
                                ?: allHabits.firstOrNull { !it.archived && it.name.equals(q, ignoreCase = true) }
                                ?: allHabits.firstOrNull { !it.archived && it.name.contains(q, ignoreCase = true) }
                                ?: allHabits.firstOrNull { it.name.contains(q, ignoreCase = true) }
                            if (matchedHabit != null) {
                                if (act.deletePermanently) {
                                    dailyOsDao.deleteHabitById(matchedHabit.id)
                                    savedMsgs.add("Habit dihapus \"${matchedHabit.name}\"")
                                } else {
                                    dailyOsDao.upsertHabit(matchedHabit.copy(archived = true))
                                    savedMsgs.add("Habit diarsipkan \"${matchedHabit.name}\"")
                                }
                            }
                        }
                    }
                }
                if (savedMsgs.isNotEmpty()) {
                    didMutateModuleAction = true
                    _uiState.update {
                        it.copy(kronikBannerMessage = "Daily OS: ${savedMsgs.first()}")
                    }
                    viewModelScope.launch {
                        delay(4_000L)
                        _uiState.update { it.copy(kronikBannerMessage = null) }
                    }
                }
            }

            if (parsed.deviceActions.isNotEmpty()) {
                val app = getApplication<Application>()
                val zone = ZoneId.systemDefault()
                val deviceLabels = mutableListOf<String>()
                parsed.deviceActions.forEach { act ->
                    when (act) {
                        is SseTagParser.DeviceChatAction.OpenUrl -> {
                            _deviceEvents.tryEmit(DeviceActionEvent.OpenUrl(act.url))
                            deviceLabels.add("Buka URL")
                        }
                        is SseTagParser.DeviceChatAction.ShareText -> {
                            _deviceEvents.tryEmit(DeviceActionEvent.ShareText(act.text))
                            deviceLabels.add("Bagikan teks")
                        }
                        is SseTagParser.DeviceChatAction.SetAlarm -> {
                            _deviceEvents.tryEmit(DeviceActionEvent.SetAlarm(act.hour, act.minute, act.label))
                            deviceLabels.add(String.format(Locale.US, "Alarm %02d:%02d", act.hour, act.minute))
                        }
                        is SseTagParser.DeviceChatAction.SetTimer -> {
                            _deviceEvents.tryEmit(DeviceActionEvent.SetTimer(act.seconds, act.label))
                            val mins = act.seconds / 60
                            val lbl = if (mins > 0 && act.seconds % 60 == 0) "${mins}m" else "${act.seconds}d"
                            deviceLabels.add("Timer $lbl")
                        }
                        is SseTagParser.DeviceChatAction.SetReminder -> {
                            val parsedDate = runCatching { LocalDate.parse(act.dateStr) }
                                .getOrDefault(LocalDate.now(zone))
                            var targetLdt = LocalDateTime.of(parsedDate, LocalTime.of(act.hour, act.minute))
                            val nowLdt = LocalDateTime.now(zone)
                            if (!targetLdt.isAfter(nowLdt)) {
                                targetLdt = targetLdt.plusDays(1)
                            }
                            val triggerMs = targetLdt.atZone(zone).toInstant().toEpochMilli()
                            val insertedId = remReminderDao.insert(
                                RemReminderEntity(
                                    title = act.message,
                                    triggerAtMs = triggerMs
                                )
                            )
                            com.ecosystemku.assistant.notifications.DailyOsNotificationHelper.scheduleOneTimeReminder(
                                context = app,
                                reminderId = insertedId,
                                triggerAtMs = triggerMs,
                                title = "Rem • Pengingat",
                                body = act.message
                            )
                            val timeFmt = String.format(Locale.US, "%02d:%02d", targetLdt.hour, targetLdt.minute)
                            deviceLabels.add("Pengingat $timeFmt")
                        }
                        is SseTagParser.DeviceChatAction.CancelReminder -> {
                            val nowMs = System.currentTimeMillis()
                            val active = remReminderDao.getActive(nowMs)
                            val q = act.target.trim()
                            val hit = q.toLongOrNull()?.let { id -> active.firstOrNull { it.id == id } }
                                ?: active.firstOrNull { it.title.equals(q, ignoreCase = true) }
                                ?: active.firstOrNull { it.title.contains(q, ignoreCase = true) }
                            if (hit != null) {
                                com.ecosystemku.assistant.notifications.DailyOsNotificationHelper.cancelOneTimeReminder(app, hit.id)
                                remReminderDao.deleteById(hit.id)
                                deviceLabels.add("Pengingat dibatalkan")
                            }
                        }
                        is SseTagParser.DeviceChatAction.AddCalendarEvent -> {
                            val parsedDate = runCatching { LocalDate.parse(act.dateStr) }
                                .getOrDefault(LocalDate.now(zone))
                            val startLdt = LocalDateTime.of(parsedDate, LocalTime.of(act.startHour, act.startMinute))
                            val endLdt = if (act.endHour != null && act.endMinute != null) {
                                LocalDateTime.of(parsedDate, LocalTime.of(act.endHour, act.endMinute))
                            } else {
                                startLdt.plusHours(1)
                            }
                            val startMs = startLdt.atZone(zone).toInstant().toEpochMilli()
                            val endMs = endLdt.atZone(zone).toInstant().toEpochMilli().coerceAtLeast(startMs + 1_800_000L)
                            _deviceEvents.tryEmit(
                                DeviceActionEvent.AddCalendarEvent(
                                    title = act.title,
                                    startMs = startMs,
                                    endMs = endMs,
                                    location = act.location,
                                    description = act.description
                                )
                            )
                            deviceLabels.add("Kalender: ${act.title}")
                        }
                    }
                }
                if (deviceLabels.isNotEmpty()) {
                    didMutateModuleAction = true
                    _uiState.update {
                        it.copy(kronikBannerMessage = "Perangkat: ${deviceLabels.first()}")
                    }
                    viewModelScope.launch {
                        delay(4_000L)
                        _uiState.update { it.copy(kronikBannerMessage = null) }
                    }
                }
            }

            val didOpenApp = parsed.openApp in setOf("kronik", "linkku", "cucimoney", "ideku", "dailyos")
            if (didOpenApp) {
                _uiState.update { it.copy(pendingOpenApp = parsed.openApp) }
            }

            if (expr != null) {
                applyExpression(expr, autoRevert = expr != "none")
            } else if (_uiState.value.currentExpression == "puzzle") {
                applyExpression("none", autoRevert = false)
            }

            _streamingText.value = ""
            _uiState.update { it.copy(isThinking = false) }

            val lastUserMsg = recent.lastOrNull { it.role == "user" }?.content.orEmpty()
            val isFirstTurnInSession = recent.count { it.role == "user" } <= 1
            val fallbackClip = selectChatVoiceClip(
                userMessage = lastUserMsg,
                expression = expr,
                didRealizeWishlist = didRealizeWishlist,
                didMutateAction = didMutateModuleAction,
                didOpenApp = didOpenApp,
                isFirstTurn = isFirstTurnInSession
            )
            val ttsTarget = finalDoneTts.ifBlank { replyText }
            if (ttsTarget.isNotBlank() || fallbackClip.isNotBlank()) {
                audioManager.playTts(
                    text = ttsTarget,
                    isJapanese = finalDoneTts.isNotBlank(),
                    fallbackClip = fallbackClip
                )
            }

            if (_uiState.value.autoLearnMemory && replyText.isNotEmpty()) {
                extractAndPersistMemories(sourceMessageId = sourceUserMsgId, sessionId = sessionId)
            }
            if (replyText.isNotEmpty()) {
                compactSessionIfNeeded(sessionId = sessionId)
            }
    }

    private fun formatElapsedGapId(fromMs: Long, nowMs: Long): String {
        val diffMs = (nowMs - fromMs).coerceAtLeast(0L)
        val totalMinutes = diffMs / 60_000L
        return when {
            totalMinutes < 1L -> "baru saja (< 1 menit)"
            totalMinutes < 60L -> "$totalMinutes menit"
            totalMinutes < 1_440L -> {
                val h = totalMinutes / 60L
                val m = totalMinutes % 60L
                if (m > 0L) "$h jam $m menit" else "$h jam"
            }
            else -> {
                val d = totalMinutes / 1_440L
                val h = (totalMinutes % 1_440L) / 60L
                if (h > 0L) "$d hari $h jam" else "$d hari"
            }
        }
    }

    private suspend fun buildPersonalizedPrompt(
        sessionId: Long = _activeSessionId.value,
        currentUserMsgId: Long? = null,
        searchContextOverride: String? = null,
        forbidMoreSearch: Boolean = false
    ): String {
        val topMemories = userMemoryDao.getTopMemories(20)
        val latestSummaries = userMemoryDao.getLatestSummaries(3)
        val activeSession = if (sessionId > 0L) chatDao.getSessionById(sessionId) else null
        val sessionMessages = if (sessionId > 0L) chatDao.getBySessionAsc(sessionId) else emptyList()
        val currentUserText = sessionMessages.lastOrNull {
            it.role == "user" && (currentUserMsgId == null || it.id == currentUserMsgId)
        }?.content.orEmpty()
        val kgContext = kgRepository.queryContextForMessage(currentUserText, maxEdges = 14)
        val recentJournals = kronikDao.getVisibleEntries(3)
        val recentPublicLinks = linkkuDao.getPublicLinks(4)
        val cuciState = _uiState.value.cuciMoneyState
        val profileName = _uiState.value.profileName.trim()

        val nowMs = System.currentTimeMillis()
        val nowZdt = LocalDateTime.now(ZoneId.systemDefault())
        val dayNameId = when (nowZdt.dayOfWeek.value) {
            1 -> "Senin"; 2 -> "Selasa"; 3 -> "Rabu"; 4 -> "Kamis"
            5 -> "Jumat"; 6 -> "Sabtu"; else -> "Minggu"
        }
        val monthNameId = when (nowZdt.monthValue) {
            1 -> "Januari"; 2 -> "Februari"; 3 -> "Maret"; 4 -> "April"
            5 -> "Mei"; 6 -> "Juni"; 7 -> "Juli"; 8 -> "Agustus"
            9 -> "September"; 10 -> "Oktober"; 11 -> "November"; else -> "Desember"
        }
        val timeOfDayId = when (nowZdt.hour) {
            in 4..10 -> "pagi"
            in 11..14 -> "siang"
            in 15..17 -> "sore"
            else -> "malam"
        }
        val timeFormatted = String.format("%02d:%02d", nowZdt.hour, nowZdt.minute)
        val prevSessionUserMsg = sessionMessages
            .filter { it.role == "user" && it.id != currentUserMsgId }
            .maxByOrNull { it.createdAt }
        val prevGlobalUserMsg = if (prevSessionUserMsg == null) {
            chatDao.getAll()
                .filter { it.role == "user" && it.id != currentUserMsgId }
                .maxByOrNull { it.createdAt }
        } else null
        val gapRef = prevSessionUserMsg ?: prevGlobalUserMsg
        val gapLine = if (gapRef != null) {
            val scope = if (prevSessionUserMsg != null) "di sesi ini" else "dari sesi sebelumnya"
            "\nJeda sejak pesan terakhir Tuan ($scope): ${formatElapsedGapId(gapRef.createdAt, nowMs)}"
        } else ""

        return buildString {
            append(remSystemPrompt)
            append("\n\n## Konteks Waktu Saat Ini\n")
            append("Waktu lokal perangkat: $dayNameId, ${nowZdt.dayOfMonth} $monthNameId ${nowZdt.year}, pukul $timeFormatted ($timeOfDayId)")
            append(gapLine)
            if (profileName.isNotBlank()) {
                append("\n\nNama pengguna (Tuan): $profileName")
            }
            val memProfile = _uiState.value.memoryProfile
            if (memProfile.sections.isNotEmpty()) {
                append("\n\n## Ringkasan Memori Tentang Tuan\n")
                memProfile.sections.forEach { sec ->
                    append("[${sec.title}] ${sec.body}\n")
                }
            }
            if (kgContext.isNotEmpty()) {
                append("\n\n## Peta Pengetahuan Personal (Otak Digital)\n")
                kgContext.forEach { t ->
                    val hist = if (com.ecosystemku.assistant.data.kg.KnowledgeGraphHelpers.isHistoricalEdge(t.edge)) "[HISTORIS] " else ""
                    val ev = if (t.edge.evidence.isNotBlank()) " — \"${t.edge.evidence.take(80)}\"" else ""
                    append("- $hist${t.fromNode.label} [${t.fromNode.nodeType}] —(${t.edge.relation}, w=${"%.1f".format(Locale.US, t.edge.weight)})→ ${t.toNode.label} [${t.toNode.nodeType}]$ev\n")
                }
            }
            val isKenaliAku = activeSession?.title?.contains("Kenali Aku", ignoreCase = true) == true
            if (isKenaliAku) {
                append(
                    "\n\n## Mode Aktif: Sesi Kenali Aku\n" +
                        "Tujuan sesi ini adalah mengenal Tuan lebih dalam (aturan pengambilan keputusan/trade-off, pola pikir, prinsip hidup, kebiasaan, pemicu emosi, dan tujuan jangka panjang) untuk memperkaya Otak Digital Tuan. Ajukan HANYA SATU pertanyaan reflektif yang natural, hangat, dan spesifik per giliran (berdasarkan Peta Pengetahuan di atas; jika ada kebiasaan/prinsip lama yang mungkin bergeser, tanyakan apakah masih berlaku). Jika membantu, sertakan opsi cepat [CHOICES:single|...] yang relevan."
                )
            } else if (_uiState.value.kgMicroQuestionsEnabled) {
                val userTurnsInSession = sessionMessages.count { it.role == "user" }
                if (userTurnsInSession in 1..2 && kgContext.size < 10) {
                    append(
                        "\n\nCatatan opsional: bila terasa natural dan relevan dengan topik, Rem boleh menyelipkan maksimal satu pertanyaan ringan singkat untuk lebih memahami alasan/preferensi/pola pikir Tuan (jangan memaksa jika Tuan sedang terburu-buru atau bertanya hal teknis)."
                    )
                }
            }
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
            val rolling = activeSession?.rollingSummary?.trim().orEmpty()
            if (rolling.isNotBlank()) {
                append("\n## Ringkasan Konteks Sesi Ini (Pesan Sebelumnya)\n")
                append(rolling).append("\n")
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
            appendCuciMoneyContext(this, cuciState)

            val idekuList = _uiState.value.idekuState.ideas
            if (idekuList.isNotEmpty()) {
                val prosesIdeas = idekuList.filter { it.type == "Idea" && it.status == "Proses" }
                val activeWishlist = idekuList.filter { it.type == "Wishlist" && it.status != "Selesai" }
                val pendingCount = idekuList.count { it.status == "Belum" }
                append("\n## IdeKu & Wishlist Tuan (${idekuList.size} item, $pendingCount Belum)\n")
                if (prosesIdeas.isNotEmpty()) {
                    append("- Fokus Proses (${prosesIdeas.size}/${IdekuHelpers.FOCUS_LIMIT}): ")
                    append(prosesIdeas.joinToString("; ") { item ->
                        val nxt = if (item.nextStep.isNotBlank()) " → ${item.nextStep}" else ""
                        "[id=${item.id}] ${item.title}$nxt"
                    })
                    append("\n")
                }
                if (activeWishlist.isNotEmpty()) {
                    append("- Wishlist aktif (${activeWishlist.size}): ")
                    append(activeWishlist.take(8).joinToString(", ") { item ->
                        val priceTag = if (item.price > 0L) " ${CuciMoneyHelpers.formatRp(item.price)}" else ""
                        "[id=${item.id}] \"${item.title}\"$priceTag [${item.status}]"
                    })
                    append("\n")
                }
                val latestItems = idekuList.take(5)
                append("- Terbaru: ")
                append(latestItems.joinToString("; ") { item ->
                    val priceTag = if (item.type == "Wishlist" && item.price > 0L) ", ${CuciMoneyHelpers.formatRp(item.price)}" else ""
                    "[id=${item.id}] [${item.type}] ${item.title} (${item.category}, ${item.status}$priceTag)"
                })
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
                val recentDoneTodos = dailyTodos.filter { it.done }.takeLast(3)
                append("\n## Daily OS Hari Ini (${prog[3]}/${prog[4]} habit selesai [${prog[2]}%], Overall Streak ${streak}h)\n")
                if (dailyHabits.isNotEmpty()) {
                    append("- Habit aktif (${dailyHabits.size}):\n")
                    dailyHabits.take(15).forEach { h ->
                        val hStreak = DailyOsHelpers.calculateHabitStreak(h, today)
                        val hist = DailyOsHelpers.parseHistory(h.historyJson)
                        val totalDays = hist.count { it.value.isNotEmpty() }
                        val scheduledToday = DailyOsHelpers.isHabitScheduled(h, today)
                        val doneToday = hist[today].orEmpty().isNotEmpty()
                        val todayStatus = when {
                            doneToday -> "selesai hari ini"
                            !scheduledToday -> "tidak terjadwal hari ini"
                            else -> "belum hari ini"
                        }
                        append("  • [id=${h.id}] ${h.name} (${h.category}): streak $hStreak hari berturut-turut, $todayStatus, total $totalDays hari\n")
                    }
                }
                if (pendingTodos.isNotEmpty()) {
                    val urgentOrOv = pendingTodos.filter { it.priority == "urgent" || DailyOsHelpers.isOverdue(it.deadline, today) }.take(6)
                    val shownTodos = urgentOrOv.ifEmpty { pendingTodos.take(6) }
                    append("- To-Do Pending (${pendingTodos.size} task): ")
                    append(shownTodos.joinToString("; ") { t ->
                        val dl = if (t.deadline.isNotBlank()) ", dl ${t.deadline}" else ""
                        "[id=${t.id}] ${t.title} [${t.priority}$dl]"
                    })
                    append("\n")
                }
                if (recentDoneTodos.isNotEmpty()) {
                    append("- To-Do Selesai Terbaru: ")
                    append(recentDoneTodos.joinToString("; ") { "[id=${it.id}] ${it.title}" })
                    append("\n")
                }
            }

            val activeReminders = remReminderDao.getActive(nowMs)
            if (activeReminders.isNotEmpty()) {
                val rFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
                val rZone = ZoneId.systemDefault()
                append("\n## Pengingat Terjadwal Rem (${activeReminders.size} aktif)\n")
                activeReminders.take(8).forEach { rem ->
                    val whenStr = Instant.ofEpochMilli(rem.triggerAtMs).atZone(rZone).format(rFmt)
                    append("- [id=${rem.id}] $whenStr: ${rem.title}\n")
                }
            }

            if (!searchContextOverride.isNullOrBlank()) {
                append("\n## Hasil Pencarian Arsip Lokal\n")
                append(searchContextOverride)
                if (forbidMoreSearch) {
                    append("Batas pencarian tercapai. Jangan memanggil [SEARCH:...] lagi; langsung jawab Tuan berdasarkan data di atas.\n")
                } else {
                    append("Gunakan data akurat di atas untuk menjawab pesan Tuan. Jangan memanggil [SEARCH:...] lagi untuk pencarian yang sama.\n")
                }
            }

            append(
                "\nGunakan memori, Kronik, LinkKu, CuciMoney+, IdeKu, & Daily OS di atas secara natural agar terasa mengenal Tuan, tanpa menyebut kata 'database' atau 'SQLite'. Angka yang tertera adalah data AKURAT — jangan menghitung ulang atau menambah dari ingatan." +
                    "\n\nATURAN PENTING KEUANGAN: " +
                    "1) Jika Tuan meminta ringkasan, total, analisis, atau informasi keuangan tetapi belum menyebut nama buku, tanyakan dulu buku mana yang dimaksud sambil menyertakan tag [CHOICES:single|...] berisi nama-nama buku CuciMoney+ yang tersedia, dan jangan berikan angka keuangan apa pun sebelum Tuan memilih. " +
                    "2) Jika nama buku sudah disebut, gunakan hanya bagian KEUANGAN BUKU dengan nama yang cocok persis; jangan pernah menggabungkan angka antar-buku. Jika nama buku tidak cocok atau ambigu, tanyakan klarifikasi disertai [CHOICES:single|...]. Jika Tuan sudah menyebut buku dan tujuan dengan jelas, jangan mengulang pertanyaan yang sudah terjawab. " +
                    "3) Jika Tuan menanyakan daftar kategori keuangan, sebutkan kategori sesuai daftar Kategori Pengeluaran/Pemasukan tersedia di atas dan sertakan [CHOICES:single|...] berisi kategori relevan (maksimal 8). Jika Tuan ingin mencatat transaksi baru tetapi jenis/kategorinya masih belum jelas atau ambigu, tawarkan pilihan chip [CHOICES:single|...] berisi kategori relevan; namun jika kategori sudah jelas dari konteks kalimat Tuan (mis. beli kopi -> Minuman/Makanan), langsung cocokkan kategori tersebut tanpa memunculkan chip. " +
                    "4) Jika Tuan ingin menabung ke wishlist ([CUCI:SAVE|...]) atau melapor barang wishlist sudah kebeli ([CUCI:REALIZE|...]), tetapi nama wishlist atau akun sumber/tujuan belum jelas, tanyakan klarifikasi disertai [CHOICES:single|...] berisi daftar wishlist aktif atau akun yang tersedia."
            )
        }
    }

    private fun appendCuciMoneyContext(sb: StringBuilder, cuciState: CuciMoneyUiState) {
        if (cuciState.books.isEmpty()) return
        val activeBook = CuciMoneyHelpers.activeBook(cuciState)
        val activeBookId = activeBook?.id ?: cuciState.currentBookId
        val (curYear, curMonth) = CuciMoneyHelpers.currentYearMonth()
        val monthPrefix = CuciMoneyHelpers.yearMonthPrefix(curYear, curMonth)
        val today = CuciMoneyHelpers.todayDateStr()
        val isDebt: (CuciAccountEntity) -> Boolean = { a ->
            a.type == "credit" || a.type == "debt" || a.type == "hutang"
        }

        val sortedCats = cuciState.categories.sortedBy { it.sortOrder }
        val expenseCats = sortedCats.filter { it.type == "expense" || it.type == "both" }.map { it.name }
        val incomeCats = sortedCats.filter { it.type == "income" || it.type == "both" }.map { it.name }
        val catById = sortedCats.associateBy { it.id }
        val accById = cuciState.accounts.associateBy { it.id }

        sb.append("\n## Ringkasan Keuangan CuciMoney+ (Buku aktif di app: ${activeBook?.name ?: "-"})\n")
        if (expenseCats.isNotEmpty()) {
            sb.append("- Kategori Pengeluaran tersedia: ${expenseCats.joinToString(", ")}\n")
        }
        if (incomeCats.isNotEmpty()) {
            sb.append("- Kategori Pemasukan tersedia: ${incomeCats.joinToString(", ")}\n")
        }

        cuciState.books.forEach { book ->
            val nonTransfer = cuciState.transactions.filter { it.bookId == book.id && it.transferPair == null }
            val monthTrx = nonTransfer.filter { it.date.startsWith(monthPrefix) }
            val moIn = monthTrx.filter { it.direction == "in" }.sumOf { it.amount }
            val moOut = monthTrx.filter { it.direction == "out" }.sumOf { it.amount }
            val todayOut = nonTransfer.filter { it.date == today && it.direction == "out" }.sumOf { it.amount }
            val allIn = nonTransfer.filter { it.direction == "in" }.sumOf { it.amount }
            val allOut = nonTransfer.filter { it.direction == "out" }.sumOf { it.amount }
            val transferCount = cuciState.transactions.count {
                it.bookId == book.id && it.transferPair != null && it.date.startsWith(monthPrefix)
            } / 2

            sb.append(
                "- KEUANGAN BUKU \"${book.name}\" (bookId=${book.id}): " +
                    "hari ini keluar ${CuciMoneyHelpers.formatRp(todayOut)}; " +
                    "bulan ini pemasukan ${CuciMoneyHelpers.formatRp(moIn)}, pengeluaran ${CuciMoneyHelpers.formatRp(moOut)} (${monthTrx.size} transaksi); " +
                    "sepanjang waktu pemasukan ${CuciMoneyHelpers.formatRp(allIn)}, pengeluaran ${CuciMoneyHelpers.formatRp(allOut)}.\n"
            )
            if (transferCount > 0) {
                sb.append("  Catatan buku \"${book.name}\": $transferCount transfer antar akun bulan ini tidak dihitung sebagai pemasukan/pengeluaran.\n")
            }

            val allExpenseCats = monthTrx.filter { it.direction == "out" }
                .groupBy { catById[it.categoryId]?.name ?: "Lainnya" }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .entries
                .sortedByDescending { it.value }
            if (allExpenseCats.isNotEmpty()) {
                sb.append("  Breakdown Pengeluaran bulan ini (${book.name}): ")
                sb.append(allExpenseCats.joinToString(", ") { "${it.key} ${CuciMoneyHelpers.formatRp(it.value)}" })
                sb.append("\n")
            }

            val allIncomeCats = monthTrx.filter { it.direction == "in" }
                .groupBy { catById[it.categoryId]?.name ?: "Lainnya" }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .entries
                .sortedByDescending { it.value }
            if (allIncomeCats.isNotEmpty()) {
                sb.append("  Breakdown Pemasukan bulan ini (${book.name}): ")
                sb.append(allIncomeCats.joinToString(", ") { "${it.key} ${CuciMoneyHelpers.formatRp(it.value)}" })
                sb.append("\n")
            }

            val mb = cuciState.masterBudgets.firstOrNull {
                it.bookId == book.id && it.month == curMonth && it.year == curYear
            }
            if (mb != null && mb.limitAmount > 0L) {
                val spentInBudget = if (mb.startDate.isNotBlank() && mb.endDate.isNotBlank()) {
                    nonTransfer.filter { it.direction == "out" && it.date >= mb.startDate && it.date <= mb.endDate }
                        .sumOf { it.amount }
                } else moOut
                val pct = ((spentInBudget * 100L) / mb.limitAmount).toInt()
                val diff = mb.limitAmount - spentInBudget
                val remLabel = if (diff >= 0L) {
                    "sisa ${CuciMoneyHelpers.formatRp(diff)}"
                } else {
                    "melebihi batas ${CuciMoneyHelpers.formatRp(-diff)}"
                }
                sb.append(
                    "  Budget bulan ini (${book.name}): batas ${CuciMoneyHelpers.formatRp(mb.limitAmount)}, " +
                        "terpakai ${CuciMoneyHelpers.formatRp(spentInBudget)} ($pct%), $remLabel.\n"
                )
            }

            val accIds = CuciMoneyHelpers.parseAccountIds(book.accountIdsJson).toSet()
            val bookAccs = cuciState.accounts.filter { it.id in accIds && !it.isSavings }
            if (bookAccs.isNotEmpty()) {
                val assets = bookAccs.filter { !isDebt(it) }
                val debts = bookAccs.filter { isDebt(it) }
                val totalAset = assets.sumOf { it.balance }
                val totalHutang = debts.sumOf { kotlin.math.abs(it.balance) }
                val names = assets.joinToString(", ") { "${it.name} ${CuciMoneyHelpers.formatRp(it.balance)}" }
                val debtInfo = if (totalHutang > 0L) {
                    " (aset ${CuciMoneyHelpers.formatRp(totalAset)}, hutang ${CuciMoneyHelpers.formatRp(totalHutang)})"
                } else ""
                sb.append(
                    "  Akun buku \"${book.name}\": ${names.ifBlank { "tidak ada aset" }}; " +
                        "aset bersih ${CuciMoneyHelpers.formatRp(totalAset - totalHutang)}$debtInfo.\n"
                )
            }

            if (book.id == activeBookId) {
                val recentCuci = cuciState.transactions
                    .filter { it.bookId == book.id }
                    .sortedWith(compareByDescending<CuciTransactionEntity> { it.date }.thenByDescending { it.id })
                    .take(5)
                if (recentCuci.isNotEmpty()) {
                    sb.append("  Transaksi terakhir (${book.name}): ")
                    sb.append(recentCuci.joinToString("; ") { t ->
                        val sign = if (t.direction == "in") "+" else "-"
                        val cName = catById[t.categoryId]?.name ?: "-"
                        val aName = accById[t.accountId]?.name ?: "-"
                        "[id=${t.id}] ${t.title} ($sign${CuciMoneyHelpers.formatRp(t.amount)}, $cName, $aName, ${t.date})"
                    })
                    sb.append("\n")
                }
            }
        }

        val savingsTotal = cuciState.accounts.filter { it.isSavings }.sumOf { it.balance }
        val activeWishes = cuciState.wishlists.filter { it.status != "Selesai" }
        if (savingsTotal > 0L || activeWishes.isNotEmpty()) {
            sb.append("- Tabungan Wishlist (Total saldo pot tabungan: ${CuciMoneyHelpers.formatRp(savingsTotal)}):\n")
            activeWishes.take(10).forEach { w ->
                val remaining = (w.price - w.saved).coerceAtLeast(0L)
                sb.append(
                    "  • \"${w.title}\": terkumpul ${CuciMoneyHelpers.formatRp(w.saved)} / " +
                        "target ${CuciMoneyHelpers.formatRp(w.price)} (kurang ${CuciMoneyHelpers.formatRp(remaining)}) [${w.priority}]\n"
                )
            }
        }
    }

    private fun extractAndPersistMemories(sourceMessageId: Long, sessionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLearningMemory = true) }
            try {
                val recentTurns = (if (sessionId > 0L) chatDao.getRecentBySessionDesc(sessionId, 6) else chatDao.getRecentDesc(6))
                    .reversed()
                    .map { ChatTurnDto(role = it.role, content = it.content) }
                val existing = userMemoryDao.getTopMemories(35)
                    .map { ExistingFactDto(factKey = it.factKey, factValue = it.factValue) }

                val result = memoryClient.extractMemories(recentTurns, existing) ?: return@launch
                var memoryChanged = false
                result.deleteKeys.forEach { delKey ->
                    if (deleteMemoryByKeyOrQuery(delKey) > 0) memoryChanged = true
                }
                result.facts.forEach { fact ->
                    if (fact.factKey.isNotBlank() && fact.factValue.isNotBlank()) {
                        userMemoryDao.upsertByFactKey(
                            category = fact.category,
                            factKey = fact.factKey,
                            factValue = fact.factValue,
                            confidence = fact.confidence,
                            sourceMessageId = sourceMessageId
                        )
                        memoryChanged = true
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
                if (result.triples.isNotEmpty()) {
                    kgRepository.recordTriples(
                        profileName = _uiState.value.profileName,
                        triples = result.triples,
                        sourceType = "chat",
                        weightBoost = 0.7f
                    )
                }
                val nowSync = System.currentTimeMillis()
                if (nowSync - lastBehavioralKgSyncAt >= 6 * 60 * 60_000L) {
                    runCatching {
                        behavioralSignalSync.syncAll(_uiState.value.profileName)
                        lastBehavioralKgSyncAt = nowSync
                    }
                }
                if (memoryChanged) {
                    refreshMemoryProfile(force = false)
                }
            } finally {
                _uiState.update { it.copy(isLearningMemory = false) }
            }
        }
    }

    private fun compactSessionIfNeeded(sessionId: Long) {
        if (sessionId <= 0L || !ApiKeyStore.isLlmConfigured(getApplication())) return
        viewModelScope.launch {
            runCatching {
                val allMsgs = chatDao.getBySessionAsc(sessionId)
                if (allMsgs.size <= 10) return@launch
                val overflowMsgs = allMsgs.dropLast(10)
                val overflowCount = overflowMsgs.size
                val session = chatDao.getSessionById(sessionId) ?: return@launch
                val alreadySummarized = session.summarizedCount.coerceIn(0, overflowCount)
                val hasSummary = !session.rollingSummary.isNullOrBlank()
                if (hasSummary && (overflowCount - alreadySummarized) < 4) return@launch

                val slice = if (hasSummary) {
                    overflowMsgs.drop(alreadySummarized).takeLast(20)
                } else {
                    overflowMsgs.takeLast(16)
                }
                if (slice.isEmpty()) return@launch

                val turns = slice.map { ChatTurnDto(role = it.role, content = it.content) }
                val compacted = memoryClient.compactSessionMessages(session.rollingSummary, turns)
                if (!compacted.isNullOrBlank()) {
                    chatDao.updateRollingSummary(sessionId, compacted, overflowCount)
                }
            }
        }
    }

    private suspend fun executeLocalArchiveSearch(scope: String, rawQuery: String): String {
        val q = rawQuery.trim().lowercase()
        val tokens = q.split(Regex("\\s+")).filter { it.length >= 2 }
        fun matchesTokens(haystack: String): Boolean {
            if (tokens.isEmpty()) return true
            val h = haystack.lowercase()
            return tokens.all { h.contains(it) } || (tokens.size > 1 && tokens.count { h.contains(it) } >= ((tokens.size + 1) / 2))
        }

        val limitPerMod = if (scope == "all") 4 else 10
        val out = StringBuilder()

        if (scope == "kronik" || scope == "all") {
            val entries = kronikDao.getVisibleEntries(150)
            val hits = entries.filter {
                matchesTokens("${it.date} ${it.moodStatus.orEmpty()} ${it.title} ${it.content}")
            }.take(limitPerMod)
            if (hits.isNotEmpty()) {
                out.append("Kronik (${hits.size} ditemukan):\n")
                hits.forEach { j ->
                    val mood = j.moodStatus?.let { " [$it]" }.orEmpty()
                    val snippet = j.content.replace('\n', ' ').trim().take(150)
                    out.append("- ${j.date}$mood ${j.title.ifBlank { "(tanpa judul)" }}: $snippet\n")
                }
            }
        }

        if (scope == "linkku" || scope == "all") {
            val linkState = _uiState.value.linkkuState
            val catMap = linkState.categories.associate { it.id to it.name }
            val pubLinks = linkState.links.filter { !it.isPrivate }
            val hits = pubLinks.filter { l ->
                val catName = catMap[l.cat].orEmpty()
                matchesTokens("${l.title} ${l.url} ${l.note} $catName ${l.tagsJson}")
            }.take(limitPerMod)
            if (hits.isNotEmpty()) {
                out.append("LinkKu Publik (${hits.size} ditemukan):\n")
                hits.forEach { l ->
                    val catName = catMap[l.cat].orEmpty().ifBlank { "-" }
                    val notePart = if (l.note.isNotBlank()) " — ${l.note.take(80)}" else ""
                    out.append("- ${l.title.ifBlank { LinkkuHelpers.extractDomain(l.url) }} [$catName] (${l.url})$notePart\n")
                }
            }
        }

        if (scope == "cuci" || scope == "all") {
            val cuci = _uiState.value.cuciMoneyState
            val bookMap = cuci.books.associate { it.id to it.name }
            val catMap = cuci.categories.associate { it.id to it.name }
            val accMap = cuci.accounts.associate { it.id to it.name }
            val allTrx = cuci.transactions.sortedWith(compareByDescending<CuciTransactionEntity> { it.date }.thenByDescending { it.id })
            val filter = SseTagParser.parseCuciSearchQuery(rawQuery)
            val hits = if (filter.isStructured) {
                val qTokens = filter.query.lowercase().split(Regex("\\s+")).filter { it.length >= 2 }
                allTrx.filter { t ->
                    val bName = bookMap[t.bookId].orEmpty()
                    val cName = catMap[t.categoryId].orEmpty()
                    val aName = accMap[t.accountId].orEmpty()
                    val bookOk = filter.book.isBlank() || bName.contains(filter.book, ignoreCase = true)
                    val monthOk = filter.month.isBlank() || t.date.startsWith(filter.month)
                    val fromOk = filter.fromDate.isBlank() || t.date >= filter.fromDate
                    val toOk = filter.toDate.isBlank() || t.date <= filter.toDate
                    val catOk = filter.category.isBlank() || cName.contains(filter.category, ignoreCase = true)
                    val accOk = filter.account.isBlank() || aName.contains(filter.account, ignoreCase = true)
                    val dirOk = filter.direction.isBlank() || t.direction == filter.direction
                    val hay = "${t.title} $bName $cName $aName ${t.date} ${t.amount}".lowercase()
                    val qOk = qTokens.isEmpty() || qTokens.all { hay.contains(it) }
                    bookOk && monthOk && fromOk && toOk && catOk && accOk && dirOk && qOk
                }
            } else {
                val dateRegex = Regex("""^\d{4}-\d{2}(-\d{2})?$""")
                val dateTokens = tokens.filter { dateRegex.matches(it) }
                val textTokens = tokens.filterNot { dateRegex.matches(it) }
                allTrx.filter { t ->
                    val bName = bookMap[t.bookId].orEmpty()
                    val cName = catMap[t.categoryId].orEmpty()
                    val aName = accMap[t.accountId].orEmpty()
                    val dirLabel = if (t.direction == "in") "masuk pemasukan" else "keluar pengeluaran"
                    val hay = "${t.date} ${t.title} $bName $cName $aName $dirLabel ${t.amount}".lowercase()
                    val datesOk = dateTokens.all { t.date.startsWith(it) }
                    val textsOk = if (dateTokens.isNotEmpty()) {
                        textTokens.all { hay.contains(it) }
                    } else {
                        matchesTokens(hay)
                    }
                    datesOk && textsOk
                }
            }
            if (hits.isNotEmpty()) {
                val nonTrfHits = hits.filter { it.transferPair == null }
                val sumIn = nonTrfHits.filter { it.direction == "in" }.sumOf { it.amount }
                val sumOut = nonTrfHits.filter { it.direction == "out" }.sumOf { it.amount }
                out.append("CuciMoney+ (${hits.size} transaksi cocok; total non-transfer masuk ${CuciMoneyHelpers.formatRp(sumIn)}, keluar ${CuciMoneyHelpers.formatRp(sumOut)}):\n")
                val outBreakdown = nonTrfHits.filter { it.direction == "out" }
                    .groupBy { catMap[it.categoryId] ?: "Lainnya" }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                    .entries
                    .sortedByDescending { it.value }
                if (outBreakdown.isNotEmpty()) {
                    out.append("  Breakdown keluar: ${outBreakdown.joinToString(", ") { "${it.key} ${CuciMoneyHelpers.formatRp(it.value)}" }}\n")
                }
                hits.take(if (scope == "cuci") 15 else limitPerMod).forEach { t ->
                    val sign = if (t.direction == "in") "+" else "-"
                    val bName = bookMap[t.bookId].orEmpty()
                    val cName = catMap[t.categoryId].orEmpty()
                    val aName = accMap[t.accountId].orEmpty()
                    out.append("- [id=${t.id}] ${t.date} [$bName] ${t.title} ($sign${CuciMoneyHelpers.formatRp(t.amount)}, $cName, $aName)\n")
                }
            }
        }

        if (scope == "ideku" || scope == "all") {
            val hits = _uiState.value.idekuState.ideas.filter { item ->
                matchesTokens("${item.id} ${item.type} ${item.title} ${item.notes} ${item.category} ${item.status} ${item.priority} ${item.jalur}")
            }.take(limitPerMod)
            if (hits.isNotEmpty()) {
                out.append("IdeKu & Wishlist (${hits.size} ditemukan):\n")
                hits.forEach { item ->
                    val price = if (item.type == "Wishlist" && item.price > 0L) ", ${CuciMoneyHelpers.formatRp(item.price)}" else ""
                    val note = if (item.notes.isNotBlank()) " — ${item.notes.take(80)}" else ""
                    out.append("- [id=${item.id}] [${item.type}] ${item.title} (${item.category}, ${item.status}, ${item.priority}$price)$note\n")
                }
            }
        }

        if (scope == "dailyos" || scope == "all") {
            val daily = _uiState.value.dailyOsState
            val today = DailyOsHelpers.todayStr()
            val habitHits = daily.habits.filter {
                matchesTokens("${it.id} ${it.name} ${it.category} ${if (it.archived) "arsip" else "aktif"}")
            }.take(limitPerMod)
            val todoHits = daily.todos.filter {
                matchesTokens("${it.id} ${it.title} ${it.priority} ${it.deadline.orEmpty()} ${if (it.done) "selesai done" else "pending"}")
            }.take(limitPerMod)
            if (habitHits.isNotEmpty() || todoHits.isNotEmpty()) {
                out.append("Daily OS:\n")
                habitHits.forEach { h ->
                    val hist = DailyOsHelpers.parseHistory(h.historyJson)
                    val nonEmptyDates = hist.filterValues { it.isNotEmpty() }.keys.sortedDescending()
                    val totalDays = nonEmptyDates.size
                    val hStreak = DailyOsHelpers.calculateHabitStreak(h, today)
                    val scheduledToday = DailyOsHelpers.isHabitScheduled(h, today)
                    val doneToday = hist[today].orEmpty().isNotEmpty()
                    val todayInfo = when {
                        doneToday -> "hari ini selesai"
                        !scheduledToday -> "hari ini tidak terjadwal"
                        else -> "hari ini belum"
                    }
                    val recentInfo = if (nonEmptyDates.isNotEmpty()) {
                        ", terakhir: ${nonEmptyDates.take(5).joinToString(", ")}"
                    } else ""
                    out.append("- [id=${h.id}] Habit \"${h.name}\" (${h.category}, streak $hStreak hari berturut-turut, $todayInfo, total $totalDays hari$recentInfo${if (h.archived) ", diarsipkan" else ""})\n")
                }
                todoHits.forEach { t ->
                    val st = if (t.done) "Selesai" else "Belum"
                    val dl = t.deadline?.takeIf { it.isNotBlank() }?.let { ", deadline $it" }.orEmpty()
                    out.append("- [id=${t.id}] To-Do \"${t.title}\" [$st, ${t.priority}$dl]\n")
                }
            }
        }

        if (scope == "chat" || scope == "all") {
            val sessMap = chatDao.getAllSessions().associateBy { it.id }
            val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
            val allMsgs = chatDao.getAll()
            val hits = allMsgs
                .asReversed()
                .filter { m ->
                    val sTitle = sessMap[m.sessionId]?.title.orEmpty()
                    matchesTokens("$sTitle ${m.content}")
                }
                .take(limitPerMod)
            if (hits.isNotEmpty()) {
                out.append("Arsip Obrolan (${hits.size} pesan cocok):\n")
                hits.forEach { m ->
                    val sTitle = sessMap[m.sessionId]?.title ?: "Sesi"
                    val who = if (m.role == "user") "Tuan" else "Rem"
                    val dt = sdf.format(Date(m.createdAt))
                    out.append("- [$sTitle · $dt] $who: ${m.content.replace('\n', ' ').take(150)}\n")
                }
            }
        }

        if (scope == "device") {
            val app = getApplication<Application>()
            val wantBattery = q.contains("baterai") || q.contains("battery")
            val wantCalendar = q.contains("kalender") || q.contains("calendar") || q.contains("jadwal") || q.contains("agenda")
            val wantLocation = q.contains("lokasi") || q.contains("location") || q.contains("dimana") || q.contains("di mana")
            val wantReminders = q.contains("reminder") || q.contains("pengingat")
            val generic = !wantBattery && !wantCalendar && !wantLocation && !wantReminders

            if (wantBattery || generic) {
                out.append(AndroidDeviceBridge.readBatteryStatus(app)).append("\n")
            }
            if (wantReminders || generic) {
                val active = remReminderDao.getActive(System.currentTimeMillis())
                if (active.isNotEmpty()) {
                    val rFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val rZone = ZoneId.systemDefault()
                    out.append("Pengingat Terjadwal Rem (${active.size}):\n")
                    active.take(10).forEach { rem ->
                        val whenStr = Instant.ofEpochMilli(rem.triggerAtMs).atZone(rZone).format(rFmt)
                        out.append("- [id=${rem.id}] $whenStr: ${rem.title}\n")
                    }
                } else if (wantReminders) {
                    out.append("Tidak ada pengingat terjadwal aktif saat ini.\n")
                }
            }
            if (wantCalendar || (generic && AndroidDeviceBridge.hasCalendarPermission(app))) {
                val dateArg = Regex("""\d{4}-\d{2}-\d{2}""").find(rawQuery)?.value
                if (!AndroidDeviceBridge.hasCalendarPermission(app)) {
                    _deviceEvents.tryEmit(
                        DeviceActionEvent.RequestPermission(android.Manifest.permission.READ_CALENDAR, "Kalender")
                    )
                }
                out.append(AndroidDeviceBridge.readCalendarEvents(app, dateArg)).append("\n")
            }
            if (wantLocation) {
                val optIn = _uiState.value.deviceLocationOptIn
                if (optIn && !AndroidDeviceBridge.hasLocationPermission(app)) {
                    _deviceEvents.tryEmit(
                        DeviceActionEvent.RequestPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION, "Lokasi")
                    )
                }
                out.append(AndroidDeviceBridge.readCoarseLocation(app, optIn)).append("\n")
            }
        }

        if (scope == "kg" || scope == "all") {
            val kgResult = kgRepository.searchGraph(rawQuery)
            if (scope == "kg" || !kgResult.startsWith("Tidak ditemukan")) {
                out.append(kgResult).append("\n")
            }
        }

        val resultStr = out.toString().trim()
        return resultStr.ifBlank {
            "Tidak ditemukan data yang cocok di arsip $scope untuk kata kunci \"$rawQuery\"."
        }
    }

    private fun resolveCuciTransactionTarget(
        target: String,
        allTrx: List<CuciTransactionEntity>,
        activeBookId: Long,
        today: String
    ): CuciTransactionEntity? {
        val clean = target.trim()
        if (clean.isEmpty()) return null
        clean.toLongOrNull()?.let { exactId ->
            return allTrx.firstOrNull { it.id == exactId }
        }
        val editable = allTrx.filter { it.savingsPot == null }
        val q = clean.lowercase()
        val tiers = listOf(
            editable.filter { it.bookId == activeBookId && it.date == today && it.title.equals(q, ignoreCase = true) },
            editable.filter { it.bookId == activeBookId && it.date == today && it.title.lowercase().contains(q) },
            editable.filter { it.bookId == activeBookId && it.title.equals(q, ignoreCase = true) },
            editable.filter { it.bookId == activeBookId && it.title.lowercase().contains(q) },
            editable.filter { it.title.equals(q, ignoreCase = true) },
            editable.filter { it.title.lowercase().contains(q) }
        )
        for (tier in tiers) {
            if (tier.size == 1) return tier.first()
            if (tier.size > 1) return null
        }
        return null
    }

    private fun resolveIdekuTarget(target: String, items: List<IdekuIdeaEntity>): IdekuIdeaEntity? {
        val clean = target.trim()
        if (clean.isEmpty()) return null
        clean.toLongOrNull()?.let { exactId ->
            return items.firstOrNull { it.id == exactId }
        }
        val q = clean.lowercase()
        val exact = items.filter { it.title.equals(q, ignoreCase = true) }
        if (exact.size == 1) return exact.first()
        if (exact.size > 1) return null
        val partial = items.filter { it.title.lowercase().contains(q) }
        return if (partial.size == 1) partial.first() else null
    }

    private fun resolveDailyOsTodoTarget(target: String, todos: List<DailyOsTodoEntity>): DailyOsTodoEntity? {
        val clean = target.trim()
        if (clean.isEmpty()) return null
        todos.firstOrNull { it.id.equals(clean, ignoreCase = true) }?.let { return it }
        val q = clean.lowercase()
        val exact = todos.filter { it.title.equals(q, ignoreCase = true) }
        if (exact.size == 1) return exact.first()
        if (exact.size > 1) return null
        val partial = todos.filter { it.title.lowercase().contains(q) }
        return if (partial.size == 1) partial.first() else null
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

    private fun selectChatVoiceClip(
        userMessage: String,
        expression: String?,
        didRealizeWishlist: Boolean,
        didMutateAction: Boolean,
        didOpenApp: Boolean,
        isFirstTurn: Boolean
    ): String {
        if (didRealizeWishlist) return "action_wishlist_1.mp3"
        if (didMutateAction) {
            return if ((System.currentTimeMillis() and 1L) == 0L) "action_done_1.mp3" else "action_done_2.mp3"
        }

        val lower = userMessage.trim().lowercase(Locale.ROOT)
        if (Regex("""\b(selamat tidur|tidur dulu|mau tidur|good\s?night|oyasumi|oyasuminasai)\b""").containsMatchIn(lower)) {
            return "goodnight_1.mp3"
        }
        if (Regex("""\b(terima\s?kasih|makasih|thanks|thank\s?you|thx|arigatou|nuhun|suwun)\b""").containsMatchIn(lower)) {
            return "thanks_1.mp3"
        }
        if (Regex("""\b(selamat pagi|ohayou?|good morning)\b|^pagi[!?. ]*$""").containsMatchIn(lower)) {
            return "greet_morning.mp3"
        }
        if (Regex("""\b(selamat siang|selamat sore|konnichiwa|good afternoon)\b|^(siang|sore)[!?. ]*$""").containsMatchIn(lower)) {
            return "greet_afternoon.mp3"
        }
        if (Regex("""\b(selamat malam|konbanwa|good evening)\b|^malam[!?. ]*$""").containsMatchIn(lower)) {
            return "greet_night.mp3"
        }
        if (isFirstTurn && Regex("""^(hai|halo|hello|hi|rem|p|permisi)[!?. ]*$""").containsMatchIn(lower)) {
            val hour = LocalDateTime.now().hour
            return when {
                hour in 4..10 -> "greet_morning.mp3"
                hour in 11..17 -> "greet_afternoon.mp3"
                else -> "greet_night.mp3"
            }
        }
        if (didOpenApp || Regex("""^(oke|ok|siap|baik|lanjut|ya|iya|gas|sip)[!?. ]*$""").containsMatchIn(lower)) {
            return "ack_1.mp3"
        }

        return when (expression?.lowercase(Locale.ROOT)) {
            "happy" -> "chat_happy_1.mp3"
            "proud" -> "chat_proud_1.mp3"
            "serious", "angry" -> "chat_serious_1.mp3"
            "sad", "cry", "upset" -> "chat_sad_1.mp3"
            "surprise" -> "chat_surprise_1.mp3"
            "puzzle", "doubt" -> "chat_puzzle_1.mp3"
            "shame" -> "chat_shame_1.mp3"
            else -> if ((System.currentTimeMillis() and 1L) == 0L) "chat_smile_1.mp3" else "chat_smile_2.mp3"
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
                stageController.triggerNudge()
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
