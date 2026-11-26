package com.ecosystemku.assistant.ui.setelan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystemku.assistant.ui.theme.AccentBlue
import com.ecosystemku.assistant.ui.theme.AppCuciMoney
import com.ecosystemku.assistant.ui.theme.AppDailyOs
import com.ecosystemku.assistant.ui.theme.AppIdeKu
import com.ecosystemku.assistant.ui.theme.AppKronik
import com.ecosystemku.assistant.ui.theme.AppLinkKu
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.DominantBg
import com.ecosystemku.assistant.ui.theme.EditorialSerif
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.SemanticDanger
import com.ecosystemku.assistant.ui.theme.SemanticSuccess
import com.ecosystemku.assistant.ui.theme.SemanticWarning
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.TextSubtle
import com.ecosystemku.assistant.ui.theme.UiSans

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ecosystemku.assistant.ui.components.RemAlertDialog
import com.ecosystemku.assistant.ui.components.RemAlertTone
import com.ecosystemku.assistant.ui.components.RemInputDialog
import com.ecosystemku.assistant.ui.components.RemTimePickerDialog
import com.ecosystemku.assistant.BuildConfig
import com.ecosystemku.assistant.data.backup.EcosystemZipImportResult
import com.ecosystemku.assistant.data.cloud.CloudAuthResult
import com.ecosystemku.assistant.data.cloud.CloudBackupUiState
import com.ecosystemku.assistant.data.cloud.CloudRestoreFetchResult
import com.ecosystemku.assistant.data.local.AiConfigField
import com.ecosystemku.assistant.data.local.AiConfigSnapshot
import com.ecosystemku.assistant.data.local.ConversationSummaryEntity
import com.ecosystemku.assistant.data.local.UserMemoryEntity
import com.ecosystemku.assistant.data.network.MemoryProfileData
import com.ecosystemku.assistant.device.AndroidDeviceBridge
import com.ecosystemku.assistant.notifications.DailyOsNotificationHelper
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tab 3 — Setelan (Fase 1 & Fase 2 Aktif Penuh).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetelanScreen(
    profileName: String = "Mahfud",
    lastBackupLabel: String = "Belum pernah",
    themeMode: String = "dark",
    stageWallpaperFile: String? = null,
    stageWallpaperDim: Float = 0.35f,
    aiConfig: AiConfigSnapshot = AiConfigSnapshot(),
    apiConnectionOk: Boolean = false,
    apiConnectionLabel: String = "LLM belum diatur",
    isTestingApi: Boolean = false,
    notifHabitEnabled: Boolean = true,
    notifTodoEnabled: Boolean = true,
    notifHabitTime: String = "07:00",
    notifTodoTime: String = "15:00",
    memories: List<UserMemoryEntity> = emptyList(),
    summaries: List<ConversationSummaryEntity> = emptyList(),
    memoryProfile: MemoryProfileData = MemoryProfileData(),
    isSynthesizingMemoryProfile: Boolean = false,
    autoLearnMemory: Boolean = true,
    isLearningMemory: Boolean = false,
    kgNodeCount: Int = 0,
    kgEdgeCount: Int = 0,
    kgMicroQuestionsEnabled: Boolean = true,
    kgWeeklyReflectionEnabled: Boolean = false,
    kgWeeklyReflectionDay: Int = Calendar.SUNDAY,
    kgWeeklyReflectionTime: String = "20:00",
    deviceLocationOptIn: Boolean = false,
    onUpdateProfileName: (String) -> Unit = {},
    onSetThemeMode: (String) -> Unit = {},
    onPickStageWallpaper: (Uri) -> Unit = {},
    onChangeStageWallpaperDim: (Float) -> Unit = {},
    onResetStageWallpaper: () -> Unit = {},
    onGetAiConfigValue: (AiConfigField) -> String = { "" },
    onSaveAiConfigValue: (AiConfigField, String) -> Unit = { _, _ -> },
    onTestApiConnection: () -> Unit = {},
    onSetNotifHabitEnabled: (Boolean) -> Unit = {},
    onSetNotifTodoEnabled: (Boolean) -> Unit = {},
    onSetNotifTimes: (String, String) -> Unit = { _, _ -> },
    onTestNotification: () -> Unit = {},
    onToggleAutoLearn: (Boolean) -> Unit = {},
    onOpenKnowledgeGraph: () -> Unit = {},
    onToggleKgMicroQuestions: (Boolean) -> Unit = {},
    onToggleKgWeeklyReflection: (Boolean) -> Unit = {},
    onSetKgWeeklyReflectionSchedule: (Int, String) -> Unit = { _, _ -> },
    onSetDeviceLocationOptIn: (Boolean) -> Unit = {},
    onAddManualMemory: (category: String, label: String, value: String) -> Unit = { _, _, _ -> },
    onDeleteMemory: (Long) -> Unit = {},
    onDeleteSummary: (Long) -> Unit = {},
    onClearMemories: () -> Unit = {},
    onRefreshMemoryProfile: () -> Unit = {},
    onSaveMemoryProfileSection: (index: Int?, title: String, body: String) -> Unit = { _, _, _ -> },
    onDeleteMemoryProfileSection: (Int) -> Unit = {},
    onClearMemoryProfile: () -> Unit = {},
    onExportZip: suspend () -> ByteArray = { ByteArray(0) },
    onImportZip: suspend (ByteArray, Boolean, String?) -> EcosystemZipImportResult = { _, _, _ ->
        EcosystemZipImportResult.Error("Belum terhubung")
    },
    cloudState: CloudBackupUiState = CloudBackupUiState(),
    onConnectCloud: suspend () -> CloudAuthResult = { CloudAuthResult.Failure("Belum terhubung") },
    onHandleCloudAuthIntent: suspend (Intent?) -> CloudAuthResult = { CloudAuthResult.Failure("Belum terhubung") },
    onDisconnectCloud: () -> Unit = {},
    onRefreshCloudInfo: suspend () -> Unit = {},
    onCloudBackupNow: suspend () -> String = { "Belum terhubung" },
    onPrepareCloudRestore: suspend () -> CloudRestoreFetchResult = { CloudRestoreFetchResult.Error("Belum terhubung") },
    onDecryptCloudBytes: (ByteArray, String) -> Result<ByteArray> = { _, _ -> Result.failure(IllegalStateException("Belum terhubung")) },
    onSetCloudAutoSchedule: (String, Boolean) -> Unit = { _, _ -> },
    onSetCloudPassphrase: (String) -> Unit = {},
    onShowBanner: (String) -> Unit = {},
    onResetKronik: () -> Unit = {},
    onResetLinkku: () -> Unit = {},
    onResetCuciMoney: () -> Unit = {},
    onResetIdeku: () -> Unit = {},
    onResetDailyOs: () -> Unit = {},
    onResetAssistantData: () -> Unit = {},
    onResetAllData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) onPickStageWallpaper(uri)
    }

    val cloudAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            scope.launch {
                when (val authRes = onHandleCloudAuthIntent(res.data)) {
                    is CloudAuthResult.Authorized -> onShowBanner("Google Drive berhasil dihubungkan")
                    is CloudAuthResult.Failure -> onShowBanner(authRes.message)
                    is CloudAuthResult.NeedsResolution -> onShowBanner("Izin Google Drive belum selesai")
                }
            }
        } else {
            onShowBanner("Otorisasi Google Drive dibatalkan")
        }
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    var profileInput by remember(profileName) { mutableStateOf(profileName) }
    var showMemoryProfileDialog by remember { mutableStateOf(false) }
    var showMemoryManagerDialog by remember { mutableStateOf(false) }
    var showCloudBackupDialog by remember { mutableStateOf(false) }

    var editingAiField by remember { mutableStateOf<AiConfigField?>(null) }
    var aiFieldInput by remember { mutableStateOf("") }
    var revealKeyInput by remember { mutableStateOf(false) }

    var showImportModeDialog by remember { mutableStateOf(false) }
    var importReplaceMode by remember { mutableStateOf(false) }
    var pendingPinPayload by remember { mutableStateOf<EcosystemZipImportResult.NeedsPin?>(null) }
    var pinInput by remember { mutableStateOf("") }

    var pendingResetTarget by remember { mutableStateOf<String?>(null) }
    var hasNotifPerm by remember {
        mutableStateOf(DailyOsNotificationHelper.hasNotificationPermission(context))
    }
    var hasCalPerm by remember {
        mutableStateOf(AndroidDeviceBridge.hasCalendarPermission(context))
    }
    var hasLocPerm by remember {
        mutableStateOf(AndroidDeviceBridge.hasLocationPermission(context))
    }
    var pendingTestAfterPerm by remember { mutableStateOf(false) }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
        if (granted && pendingTestAfterPerm) {
            pendingTestAfterPerm = false
            onTestNotification()
        } else if (!granted) {
            pendingTestAfterPerm = false
            onShowBanner("Izin notifikasi ditolak di pengaturan perangkat")
        }
    }

    val calPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCalPerm = granted
        onShowBanner(if (granted) "Izin Kalender aktif untuk Rem" else "Izin Kalender ditolak")
    }

    val locPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocPerm = granted
        if (granted) {
            onSetDeviceLocationOptIn(true)
            onShowBanner("Akses lokasi perkiraan aktif untuk Rem")
        } else {
            onSetDeviceLocationOptIn(false)
            onShowBanner("Izin lokasi ditolak di perangkat")
        }
    }

    var activeTimePicker by remember { mutableStateOf<Triple<String, String, (String) -> Unit>?>(null) }

    fun pickTime(
        current: String,
        subtitle: String = "Rem akan mengingatkan $profileName setiap hari",
        onPicked: (String) -> Unit
    ) {
        activeTimePicker = Triple(current, subtitle, onPicked)
    }

    activeTimePicker?.let { (initialTime, pickerSubtitle, onPicked) ->
        RemTimePickerDialog(
            initialTime = initialTime,
            subtitle = pickerSubtitle,
            onDismiss = { activeTimePicker = null },
            onConfirm = { formatted ->
                activeTimePicker = null
                onPicked(formatted)
            }
        )
    }

    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = runCatching { onExportZip() }.getOrNull()
                if (bytes == null || bytes.isEmpty()) {
                    onShowBanner("Gagal membuat file backup ZIP")
                    return@launch
                }
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(bytes)
                        } != null
                    }.getOrDefault(false)
                }
                if (ok) onShowBanner("Backup ZIP v3 berhasil disimpan")
                else onShowBanner("Gagal menulis file ZIP ke penyimpanan")
            }
        }
    }

    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }.getOrNull()
                }
                if (bytes == null || bytes.isEmpty()) {
                    onShowBanner("Gagal membaca file ZIP yang dipilih")
                    return@launch
                }
                when (val res = onImportZip(bytes, importReplaceMode, null)) {
                    is EcosystemZipImportResult.Success -> onShowBanner(res.summary)
                    is EcosystemZipImportResult.Error -> onShowBanner(res.message)
                    is EcosystemZipImportResult.NeedsPin -> {
                        pinInput = ""
                        pendingPinPayload = res
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        RemInputDialog(
            subtitle = "Ubah Nama Profil",
            value = profileInput,
            onValueChange = { profileInput = it },
            placeholder = "Nama panggilan...",
            helperText = "Nama ini digunakan pada sapaan dashboard Apps dan panggilan Rem kepadamu.",
            maxLength = 40,
            confirmText = "Simpan",
            dismissText = "Batal",
            onConfirm = {
                onUpdateProfileName(profileInput)
                showProfileDialog = false
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    editingAiField?.let { field ->
        RemInputDialog(
            subtitle = field.title,
            value = aiFieldInput,
            onValueChange = { aiFieldInput = it },
            placeholder = field.placeholder,
            helperText = field.helper,
            isSecret = field.isSecret,
            confirmText = "Simpan",
            dismissText = "Batal",
            destructiveActionText = if (aiFieldInput.isNotBlank()) "Hapus" else null,
            onDestructiveAction = if (aiFieldInput.isNotBlank()) {
                {
                    onSaveAiConfigValue(field, "")
                    editingAiField = null
                }
            } else null,
            onConfirm = {
                onSaveAiConfigValue(field, aiFieldInput)
                editingAiField = null
            },
            onDismiss = { editingAiField = null }
        )
    }

    if (showImportModeDialog) {
        RemAlertDialog(
            title = "Mode Import ZIP v3",
            description = "Pilih 'Merge' untuk menggabungkan data baru tanpa menghapus data lama, atau 'Replace' untuk mengganti seluruh data dengan isi file ZIP.",
            confirmText = "Merge",
            secondaryConfirmText = "Replace",
            dismissText = "Batal",
            tone = RemAlertTone.INFO,
            badgeIcon = Icons.Outlined.Settings,
            onConfirm = {
                importReplaceMode = false
                showImportModeDialog = false
                importZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            },
            onSecondaryConfirm = {
                importReplaceMode = true
                showImportModeDialog = false
                importZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            },
            onDismiss = { showImportModeDialog = false }
        )
    }

    pendingPinPayload?.let { payload ->
        RemInputDialog(
            subtitle = "PIN Tautan Privat LinkKu",
            value = pinInput,
            onValueChange = { pinInput = it.filter { ch -> ch.isDigit() }.take(8) },
            placeholder = "PIN 4-digit...",
            helperText = "Backup ZIP ini berisi tautan privat LinkKu terenkripsi AES-GCM. Masukkan PIN untuk mendekripsi, atau lewati untuk data publik saja.",
            keyboardType = KeyboardType.NumberPassword,
            maxLength = 8,
            confirmText = "Dekripsi",
            dismissText = "Lewati",
            confirmEnabled = pinInput.isNotBlank(),
            onConfirm = {
                val pin = pinInput
                pendingPinPayload = null
                scope.launch {
                    when (val res = onImportZip(payload.zipBytes, payload.replaceAll, pin)) {
                        is EcosystemZipImportResult.Success -> onShowBanner(res.summary)
                        is EcosystemZipImportResult.Error -> onShowBanner(res.message)
                        is EcosystemZipImportResult.NeedsPin -> onShowBanner("PIN diperlukan")
                    }
                }
            },
            onDismiss = {
                pendingPinPayload = null
                scope.launch {
                    when (val res = onImportZip(payload.zipBytes, payload.replaceAll, "")) {
                        is EcosystemZipImportResult.Success -> onShowBanner("${res.summary} (tanpa link privat)")
                        is EcosystemZipImportResult.Error -> onShowBanner(res.message)
                        is EcosystemZipImportResult.NeedsPin -> Unit
                    }
                }
            }
        )
    }

    pendingResetTarget?.let { target ->
        val isAll = target == "ALL"
        val label = if (isAll) "Seluruh Data Asisten Rem" else "Data $target"
        RemAlertDialog(
            title = "Reset $label?",
            description = if (isAll) {
                "Tindakan ini akan menghapus semua data Daily OS, IdeKu, Kronik, CuciMoney+, LinkKu, serta riwayat obrolan & memori Asisten Rem, $profileName."
            } else {
                "Semua data lokal untuk modul $target akan dikosongkan dan dikembalikan ke kondisi awal, $profileName."
            },
            confirmText = "Ya, Reset",
            dismissText = "Batal",
            tone = RemAlertTone.DANGER,
            onConfirm = {
                when (target) {
                    "Daily OS" -> onResetDailyOs()
                    "IdeKu" -> onResetIdeku()
                    "Kronik" -> onResetKronik()
                    "CuciMoney+" -> onResetCuciMoney()
                    "LinkKu" -> onResetLinkku()
                    "Asisten" -> onResetAssistantData()
                    "ALL" -> onResetAllData()
                }
                pendingResetTarget = null
            },
            onDismiss = { pendingResetTarget = null }
        )
    }

    if (showMemoryProfileDialog) {
        MemoryProfileSummaryDialog(
            memoryProfile = memoryProfile,
            hasSourceFacts = memories.isNotEmpty() || summaries.isNotEmpty(),
            isSynthesizing = isSynthesizingMemoryProfile,
            onRefresh = onRefreshMemoryProfile,
            onSaveSection = onSaveMemoryProfileSection,
            onDeleteSection = onDeleteMemoryProfileSection,
            onClear = {
                onClearMemoryProfile()
                showMemoryProfileDialog = false
            },
            onDismiss = { showMemoryProfileDialog = false }
        )
    }

    if (showMemoryManagerDialog) {
        MemoryManagerDialog(
            memories = memories,
            summaries = summaries,
            onDeleteMemory = onDeleteMemory,
            onDeleteSummary = onDeleteSummary,
            onClearAll = onClearMemories,
            onDismiss = { showMemoryManagerDialog = false }
        )
    }

    if (showCloudBackupDialog) {
        CloudBackupDialog(
            cloudState = cloudState,
            onConnect = {
                scope.launch {
                    when (val authRes = onConnectCloud()) {
                        is CloudAuthResult.Authorized -> onShowBanner("Google Drive berhasil dihubungkan")
                        is CloudAuthResult.NeedsResolution -> cloudAuthLauncher.launch(
                            IntentSenderRequest.Builder(authRes.intentSender).build()
                        )
                        is CloudAuthResult.Failure -> onShowBanner(authRes.message)
                    }
                }
            },
            onDisconnect = onDisconnectCloud,
            onRefreshInfo = { scope.launch { onRefreshCloudInfo() } },
            onBackupNow = {
                scope.launch {
                    val msg = onCloudBackupNow()
                    onShowBanner(msg)
                }
            },
            onPrepareRestore = onPrepareCloudRestore,
            onDecryptBytes = onDecryptCloudBytes,
            onImportPlainZip = { zipBytes, replaceAll ->
                val res = onImportZip(zipBytes, replaceAll, null)
                when (res) {
                    is EcosystemZipImportResult.Success -> {
                        showCloudBackupDialog = false
                        onShowBanner(res.summary)
                    }
                    is EcosystemZipImportResult.NeedsPin -> {
                        pinInput = ""
                        pendingPinPayload = res
                        showCloudBackupDialog = false
                    }
                    is EcosystemZipImportResult.Error -> onShowBanner(res.message)
                }
            },
            onSetAutoSchedule = onSetCloudAutoSchedule,
            onSetPassphrase = onSetCloudPassphrase,
            onShowBanner = onShowBanner,
            onDismiss = { showCloudBackupDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DominantBg)
            .statusBarsPadding()
    ) {
        // Sticky Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DominantBg.copy(alpha = 0.94f))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Setelan",
                fontFamily = EditorialSerif,
                fontSize = 28.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Preferensi, AI, & Backup Data",
                fontFamily = UiSans,
                fontSize = 12.5.sp,
                color = TextMuted
            )
        }
        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Profil
            item {
                SettingsSection(title = "PROFIL") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = profileName,
                                    fontFamily = UiSans,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Profil Lokal • Tanpa Cloud Sync",
                                    fontFamily = UiSans,
                                    fontSize = 11.5.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    profileInput = profileName
                                    showProfileDialog = true
                                }
                        ) {
                            NestedPill {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("Ubah", fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // 1B. Memori AI & Personalisasi
            item {
                var selectedCategory by remember { mutableStateOf("preferensi") }
                var labelInput by remember { mutableStateOf("") }
                var valueInput by remember { mutableStateOf("") }
                val categories = listOf("identitas", "preferensi", "kebiasaan", "tujuan", "catatan")

                SettingsSection(
                    title = "MEMORI AI & PERSONALISASI",
                    trailing = if (isLearningMemory) "Menganalisis…" else "${memories.size} fakta • ${summaries.size} rangkuman",
                    badgeText = if (autoLearnMemory) "Aktif" else null
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Toggle Auto-Belajar dari log chat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleAutoLearn(!autoLearnMemory) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Psychology,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Belajar dari Percakapan",
                                        fontFamily = UiSans,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Ekstrak fakta personal & rangkuman sesi ke SQLite",
                                        fontFamily = UiSans,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (autoLearnMemory) AccentBlue else ElevatedSurface)
                                    .border(1.dp, if (autoLearnMemory) AccentBlue else BorderLight, RoundedCornerShape(50))
                                    .padding(3.dp),
                                contentAlignment = if (autoLearnMemory) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (autoLearnMemory) DominantBg else TextMuted)
                                )
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        val profileStatusSub = when {
                            isSynthesizingMemoryProfile -> "Membuat ringkasan…"
                            memoryProfile.sections.isNotEmpty() -> {
                                val stamp = if (memoryProfile.updatedAt > 0L) {
                                    SimpleDateFormat("dd MMM, HH:mm", Locale("in", "ID")).format(Date(memoryProfile.updatedAt))
                                } else ""
                                val manualTag = if (memoryProfile.manuallyEdited) " • Manual" else ""
                                "${memoryProfile.sections.size} bagian" +
                                    (if (stamp.isNotEmpty()) " • $stamp" else "") +
                                    manualTag
                            }
                            else -> "Lihat profil naratif yang diingat Rem"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable {
                                    showMemoryProfileDialog = true
                                    if (memoryProfile.sections.isEmpty() && (memories.isNotEmpty() || summaries.isNotEmpty())) {
                                        onRefreshMemoryProfile()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ringkasan Memori",
                                    fontFamily = UiSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = profileStatusSub,
                                    fontFamily = UiSans,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = "Buka Ringkasan Memori",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Peta Otak Digital (Knowledge Graph)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, AccentBlue.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                                .clickable { onOpenKnowledgeGraph() }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Peta Otak Digital (Knowledge Graph)",
                                    fontFamily = UiSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (kgNodeCount > 0 || kgEdgeCount > 0) {
                                        "$kgNodeCount simpul • $kgEdgeCount sinapsis"
                                    } else {
                                        "Peta kebiasaan, prinsip, pola pikir & tujuan"
                                    },
                                    fontFamily = UiSans,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = "Buka Peta Otak Digital",
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Toggle Pertanyaan Refleksi Ringan
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleKgMicroQuestions(!kgMicroQuestionsEnabled) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pertanyaan Refleksi Ringan",
                                    fontFamily = UiSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Sesekali ajukan 1 pertanyaan ringan untuk memperkaya Otak Digital",
                                    fontFamily = UiSans,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (kgMicroQuestionsEnabled) AccentBlue else ElevatedSurface)
                                    .border(1.dp, if (kgMicroQuestionsEnabled) AccentBlue else BorderLight, RoundedCornerShape(50))
                                    .padding(3.dp),
                                contentAlignment = if (kgMicroQuestionsEnabled) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (kgMicroQuestionsEnabled) DominantBg else TextMuted)
                                )
                            }
                        }

                        // Toggle Refleksi Mingguan Otomatis ("Kenali Aku")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleKgWeeklyReflection(!kgWeeklyReflectionEnabled) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Check-In Refleksi Mingguan",
                                    fontFamily = UiSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Notifikasi mingguan untuk Sesi Kenali Aku terjadwal",
                                    fontFamily = UiSans,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (kgWeeklyReflectionEnabled) AccentBlue else ElevatedSurface)
                                    .border(1.dp, if (kgWeeklyReflectionEnabled) AccentBlue else BorderLight, RoundedCornerShape(50))
                                    .padding(3.dp),
                                contentAlignment = if (kgWeeklyReflectionEnabled) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (kgWeeklyReflectionEnabled) DominantBg else TextMuted)
                                )
                            }
                        }

                        if (kgWeeklyReflectionEnabled) {
                            val weeklyDays = remember {
                                listOf(
                                    Calendar.SUNDAY to "Min",
                                    Calendar.MONDAY to "Sen",
                                    Calendar.WEDNESDAY to "Rab",
                                    Calendar.FRIDAY to "Jum",
                                    Calendar.SATURDAY to "Sab"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    weeklyDays.forEach { (dayConst, shortLabel) ->
                                        val active = dayConst == kgWeeklyReflectionDay
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (active) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface)
                                                .border(
                                                    1.dp,
                                                    if (active) AccentBlue.copy(alpha = 0.6f) else BorderSubtle,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { onSetKgWeeklyReflectionSchedule(dayConst, kgWeeklyReflectionTime) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = shortLabel,
                                                fontFamily = UiSans,
                                                fontSize = 11.sp,
                                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (active) AccentBlue else TextMuted
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ElevatedSurface)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                        .clickable {
                                            pickTime(
                                                current = kgWeeklyReflectionTime,
                                                subtitle = "Rem akan mengundang $profileName refleksi mingguan"
                                            ) { formatted ->
                                                onSetKgWeeklyReflectionSchedule(kgWeeklyReflectionDay, formatted)
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = kgWeeklyReflectionTime,
                                        fontFamily = UiSans,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentBlue
                                    )
                                }
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        // Form tambah fakta manual
                        Text(
                            text = "Tambah Fakta Manual",
                            fontFamily = UiSans,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEach { cat ->
                                val active = cat == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface)
                                        .border(
                                            1.dp,
                                            if (active) AccentBlue.copy(alpha = 0.55f) else BorderSubtle,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 9.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = cat.replaceFirstChar { it.uppercase() },
                                        fontFamily = UiSans,
                                        fontSize = 11.sp,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (active) AccentBlue else TextMuted
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(0.38f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 9.dp)
                            ) {
                                if (labelInput.isEmpty()) {
                                    Text("Label (mis. kopi)", fontFamily = UiSans, fontSize = 11.5.sp, color = TextSubtle, maxLines = 1)
                                }
                                BasicTextField(
                                    value = labelInput,
                                    onValueChange = { labelInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = UiSans, fontSize = 12.sp, color = TextPrimary),
                                    cursorBrush = SolidColor(AccentBlue),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(0.62f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 9.dp)
                            ) {
                                if (valueInput.isEmpty()) {
                                    Text("Nilai (mis. Americano tanpa gula)", fontFamily = UiSans, fontSize = 11.5.sp, color = TextSubtle, maxLines = 1)
                                }
                                BasicTextField(
                                    value = valueInput,
                                    onValueChange = { valueInput = it },
                                    singleLine = true,
                                    textStyle = TextStyle(fontFamily = UiSans, fontSize = 12.sp, color = TextPrimary),
                                    cursorBrush = SolidColor(AccentBlue),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (valueInput.isNotBlank()) AccentBlue else ElevatedSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .clickable(enabled = valueInput.isNotBlank()) {
                                        onAddManualMemory(selectedCategory, labelInput, valueInput)
                                        labelInput = ""
                                        valueInput = ""
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "Simpan Fakta",
                                    tint = if (valueInput.isNotBlank()) DominantBg else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        val memoryManagerSub = if (memories.isEmpty() && summaries.isEmpty()) {
                            "Belum ada fakta personal atau rangkuman sesi"
                        } else {
                            "${memories.size} fakta personal • ${summaries.size} rangkuman sesi"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { showMemoryManagerDialog = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kelola Fakta & Rangkuman",
                                    fontFamily = UiSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = memoryManagerSub,
                                    fontFamily = UiSans,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.ChevronRight,
                                contentDescription = "Buka Kelola Fakta & Rangkuman",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 2. AI & Suara (Full Custom LLM + TTS — Android Keystore)
            item {
                SettingsSection(title = "AI & SUARA", trailing = "Full Custom") {
                    val openFieldEditor: (AiConfigField) -> Unit = { field ->
                        aiFieldInput = onGetAiConfigValue(field)
                        revealKeyInput = false
                        editingAiField = field
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "LLM (Chat & Semua Fitur AI)",
                            fontFamily = UiSans,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AiConfigField.LLM_BASE_URL,
                                AiConfigField.LLM_MODEL,
                                AiConfigField.LLM_API_KEY
                            ).forEach { field ->
                                AiConfigRow(
                                    label = field.title,
                                    displayValue = aiConfig.displayFor(field),
                                    filled = aiConfig.isFilled(field),
                                    isSecret = field.isSecret,
                                    onClick = { openFieldEditor(field) }
                                )
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        Text(
                            text = "TTS (Suara Rem — Fish Audio)",
                            fontFamily = UiSans,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AiConfigField.TTS_API_KEY,
                                AiConfigField.TTS_VOICE_ID,
                                AiConfigField.TTS_MODEL,
                                AiConfigField.TTS_BASE_URL
                            ).forEach { field ->
                                AiConfigRow(
                                    label = field.title,
                                    displayValue = aiConfig.displayFor(field),
                                    filled = aiConfig.isFilled(field),
                                    isSecret = field.isSecret,
                                    onClick = { openFieldEditor(field) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isTestingApi) { onTestApiConnection() }
                            ) {
                                NestedPill {
                                    Icon(
                                        imageVector = Icons.Outlined.MonitorHeart,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isTestingApi) "Menguji…" else "Test Koneksi",
                                        fontFamily = UiSans,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            val statusColor = when {
                                isTestingApi -> SemanticWarning
                                apiConnectionOk -> SemanticSuccess
                                else -> SemanticDanger
                            }
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(statusColor.copy(alpha = 0.1f))
                                        .border(1.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                                    Text(
                                        text = apiConnectionLabel,
                                        fontFamily = UiSans,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = statusColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Backup & Restore
            item {
                SettingsSection(title = "BACKUP & RESTORE", trailing = "Format ZIP v3") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = TextMuted)) { append("Backup terakhir: ") }
                                    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Medium)) { append(lastBackupLabel) }
                                },
                                fontFamily = UiSans,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentBlue)
                                .clickable {
                                    exportZipLauncher.launch("asisten_rem_backup_${LocalDate.now()}.zip")
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Download, contentDescription = null, tint = DominantBg, modifier = Modifier.size(16.dp))
                                Text("Export ZIP Sekarang", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DominantBg)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { showImportModeDialog = true }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Upload, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                Text("Import File ZIP (Merge / Replace)", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SemanticWarning.copy(alpha = 0.1f))
                                .border(1.dp, SemanticWarning.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = SemanticWarning, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Ekspor & impor tautan privat LinkKu memerlukan PIN 4-digit untuk enkripsi AES-GCM.",
                                fontFamily = UiSans,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = SemanticWarning.copy(alpha = 0.95f)
                            )
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .clickable { showCloudBackupDialog = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (cloudState.connected) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                                    contentDescription = null,
                                    tint = if (cloudState.connected) SemanticSuccess else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Cloud Backup (Google Drive)",
                                        fontFamily = UiSans,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    val cloudSub = when {
                                        cloudState.isSyncing -> "Menyinkronkan…"
                                        cloudState.connected -> {
                                            val autoTag = when (cloudState.autoSchedule) {
                                                "daily" -> "Auto Harian"
                                                "weekly" -> "Auto Mingguan"
                                                else -> "Manual"
                                            }
                                            "$autoTag • ${cloudState.lastBackupLabel}"
                                        }
                                        else -> "Ketuk untuk hubungkan Google Drive"
                                    }
                                    Text(
                                        text = cloudSub,
                                        fontFamily = UiSans,
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                NestedPill {
                                    Text(
                                        text = if (cloudState.connected) "Terhubung" else "Atur",
                                        fontFamily = UiSans,
                                        fontSize = 10.5.sp,
                                        color = if (cloudState.connected) SemanticSuccess else AccentBlue
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = "Buka Cloud Backup",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Notifikasi (AKTIF — AlarmManager & Runtime Permission Fase 2)
            item {
                SettingsSection(
                    title = "NOTIFIKASI",
                    badgeText = if (hasNotifPerm && (notifHabitEnabled || notifTodoEnabled)) "Izin Aktif" else null
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        NotificationRow(
                            title = "Pengingat Habit",
                            subtitle = "Sesuai hari aktif Daily OS",
                            time = notifHabitTime,
                            enabled = notifHabitEnabled,
                            onTimeClick = {
                                pickTime(notifHabitTime) { picked ->
                                    onSetNotifTimes(picked, notifTodoTime)
                                }
                            },
                            onToggle = { next ->
                                if (next && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onSetNotifHabitEnabled(next)
                            }
                        )
                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                        NotificationRow(
                            title = "Pengingat To-Do",
                            subtitle = "Hanya jika ada tugas pending",
                            time = notifTodoTime,
                            enabled = notifTodoEnabled,
                            onTimeClick = {
                                pickTime(notifTodoTime) { picked ->
                                    onSetNotifTimes(notifHabitTime, picked)
                                }
                            },
                            onToggle = { next ->
                                if (next && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPerm) {
                                    notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onSetNotifTodoEnabled(next)
                            }
                        )
                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Jadwal lokal perangkat", fontFamily = UiSans, fontSize = 11.sp, color = TextSubtle)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        hasNotifPerm = DailyOsNotificationHelper.hasNotificationPermission(context)
                                        if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            pendingTestAfterPerm = true
                                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            onTestNotification()
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                                Text("Test Notifikasi", fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AccentBlue)
                            }
                        }
                    }
                }
            }

            // 4b. Akses Perangkat (Kalender & Lokasi Opsional)
            item {
                SettingsSection(
                    title = "AKSES PERANGKAT",
                    badgeText = if (hasCalPerm || (deviceLocationOptIn && hasLocPerm)) "Aktif" else null
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Baca Kalender", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text(
                                    if (hasCalPerm) "Rem dapat mengecek jadwal kalender perangkat"
                                    else "Perlu izin baca kalender agar Rem bisa cek agenda",
                                    fontFamily = UiSans,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            if (hasCalPerm) {
                                NestedPill {
                                    Text("Diizinkan", fontFamily = UiSans, fontSize = 11.sp, color = SemanticSuccess, fontWeight = FontWeight.Medium)
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ElevatedSurface)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                        .clickable {
                                            calPermLauncher.launch(Manifest.permission.READ_CALENDAR)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Izinkan", fontFamily = UiSans, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = AccentBlue)
                                }
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text("Izinkan Rem Baca Lokasi", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text(
                                    "Opsional • Lokasi perkiraan hanya dibaca saat ditanya",
                                    fontFamily = UiSans,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            val locOn = deviceLocationOptIn && hasLocPerm
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (locOn) AccentBlue else ElevatedSurface)
                                    .border(1.dp, if (locOn) AccentBlue else BorderLight, RoundedCornerShape(50))
                                    .clickable {
                                        if (locOn) {
                                            onSetDeviceLocationOptIn(false)
                                        } else {
                                            hasLocPerm = AndroidDeviceBridge.hasLocationPermission(context)
                                            if (hasLocPerm) {
                                                onSetDeviceLocationOptIn(true)
                                            } else {
                                                locPermLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                            }
                                        }
                                    }
                                    .padding(3.dp),
                                contentAlignment = if (locOn) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (locOn) DominantBg else TextMuted)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Tampilan (AKTIF — Mode Warna Sistem / Terang / Gelap Fase 2)
            item {
                SettingsSection(title = "TAMPILAN") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mode Warna", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text("Skala teks mengikuti sistem", fontFamily = UiSans, fontSize = 11.sp, color = TextMuted)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isSys = themeMode == "system"
                            val isLight = themeMode == "light"
                            val isDark = themeMode != "system" && themeMode != "light"

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isSys) AccentBlue else Color.Transparent)
                                    .clickable { onSetThemeMode("system") }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sistem",
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSys) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSys) DominantBg else TextMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isLight) AccentBlue else Color.Transparent)
                                    .clickable { onSetThemeMode("light") }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Terang",
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    fontWeight = if (isLight) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isLight) DominantBg else TextMuted
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (isDark) AccentBlue else Color.Transparent)
                                    .clickable { onSetThemeMode("dark") }
                                    .padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DarkMode,
                                    contentDescription = null,
                                    tint = if (isDark) DominantBg else TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Gelap",
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    fontWeight = if (isDark) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isDark) DominantBg else TextMuted
                                )
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        val previewBmp = remember(stageWallpaperFile) {
                            stageWallpaperFile?.let { name ->
                                val file = File(File(context.filesDir, "stage_wallpaper"), name)
                                if (file.exists()) {
                                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                                    BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()
                                } else null
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ElevatedSurface)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (previewBmp != null) {
                                        Image(
                                            bitmap = previewBmp,
                                            contentDescription = "Preview Wallpaper Rem",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Image,
                                            contentDescription = null,
                                            tint = AccentBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Wallpaper Asisten Rem",
                                        fontFamily = UiSans,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (stageWallpaperFile != null) "Gambar kustom aktif" else "Default (Aura Biru)",
                                        fontFamily = UiSans,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { wallpaperPickerLauncher.launch("image/*") }
                            ) {
                                NestedPill {
                                    Icon(
                                        imageVector = Icons.Outlined.Image,
                                        contentDescription = null,
                                        tint = AccentBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (stageWallpaperFile != null) "Ganti Gambar" else "Pilih Gambar",
                                        fontFamily = UiSans,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary
                                    )
                                }
                            }

                            if (stageWallpaperFile != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onResetStageWallpaper() }
                                ) {
                                    NestedPill {
                                        Icon(
                                            imageVector = Icons.Outlined.RestartAlt,
                                            contentDescription = null,
                                            tint = SemanticDanger,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Reset",
                                            fontFamily = UiSans,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = SemanticDanger
                                        )
                                    }
                                }
                            }
                        }

                        if (stageWallpaperFile != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Redupkan Background",
                                        fontFamily = UiSans,
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "${(stageWallpaperDim * 100).roundToInt()}%",
                                        fontFamily = UiSans,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AccentBlue
                                    )
                                }
                                Slider(
                                    value = stageWallpaperDim.coerceIn(0f, 0.8f),
                                    onValueChange = { onChangeStageWallpaperDim(it) },
                                    valueRange = 0f..0.8f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentBlue,
                                        activeTrackColor = AccentBlue,
                                        inactiveTrackColor = ElevatedSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 6. Data per App
            item {
                val appDataRows = listOf(
                    "Daily OS" to AppDailyOs,
                    "IdeKu" to AppIdeKu,
                    "Kronik" to AppKronik,
                    "CuciMoney+" to AppCuciMoney,
                    "LinkKu" to AppLinkKu,
                    "Asisten" to AccentBlue
                )

                SettingsSection(title = "DATA PER APP", trailing = "Database SQLite") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        appDataRows.forEachIndexed { idx, (name, dotColor) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
                                    Text(name, fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElevatedSurface)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                        .clickable { pendingResetTarget = name }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Reset", fontFamily = UiSans, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AccentBlue)
                                }
                            }
                            if (idx < appDataRows.lastIndex) {
                                HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SemanticDanger.copy(alpha = 0.08f))
                                .border(1.dp, SemanticDanger.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable { pendingResetTarget = "ALL" }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = SemanticDanger, modifier = Modifier.size(16.dp))
                                Text("Hapus Semua Data", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = SemanticDanger)
                            }
                        }
                    }
                }
            }

            // 7. Tentang
            item {
                SettingsSection(title = "TENTANG") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Asisten Rem v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                    fontFamily = UiSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text("Android Native Super App • Kotlin Compose", fontFamily = UiSans, fontSize = 11.sp, color = TextMuted)
                            }
                            NestedPill {
                                Text("v${BuildConfig.VERSION_NAME}", fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted)
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                NestedPill { Text("Live2D Cubism", fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted) }
                                NestedPill { Text("OFL Fonts", fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted) }
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        runCatching {
                                            uriHandler.openUri("https://github.com/wisnuwrdh/Rem-assistant")
                                        }
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Repositori GitHub", fontFamily = UiSans, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = AccentBlue)
                                Icon(Icons.Outlined.NorthEast, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    trailing: String? = null,
    badgeText: String? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = UiSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.9.sp,
                    color = TextMuted
                )
                if (trailing != null && badgeText != null) {
                    Text(
                        text = trailing,
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        color = TextSubtle
                    )
                }
            }
            if (trailing != null && badgeText == null) {
                Text(
                    text = trailing,
                    fontFamily = UiSans,
                    fontSize = 11.sp,
                    color = TextSubtle,
                    maxLines = 1,
                    softWrap = false
                )
            }
            if (badgeText != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SemanticSuccess.copy(alpha = 0.1f))
                        .border(1.dp, SemanticSuccess.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SemanticSuccess))
                    Text(
                        text = badgeText,
                        fontFamily = UiSans,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = SemanticSuccess,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun NestedPill(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        content()
    }
}

@Composable
private fun AiConfigRow(
    label: String,
    displayValue: String,
    filled: Boolean,
    isSecret: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (filled) AccentBlue else TextSubtle)
            )
            Text(
                text = label,
                fontFamily = UiSans,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                softWrap = false
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            Text(
                text = displayValue,
                fontFamily = UiSans,
                fontSize = 12.sp,
                color = if (filled) TextMuted else TextSubtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                imageVector = if (filled && isSecret) Icons.Outlined.VisibilityOff else Icons.Outlined.Edit,
                contentDescription = "Ubah $label",
                tint = TextSubtle,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NotificationRow(
    title: String,
    subtitle: String,
    time: String,
    enabled: Boolean,
    onTimeClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontFamily = UiSans, fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onTimeClick)
            ) {
                NestedPill {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    Text(time, fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                }
            }
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (enabled) AccentBlue else ElevatedSurface)
                    .border(1.dp, if (enabled) AccentBlue else BorderLight, RoundedCornerShape(50))
                    .clickable { onToggle(!enabled) }
                    .padding(3.dp),
                contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (enabled) DominantBg else TextMuted)
                )
            }
        }
    }
}

@Composable
private fun MemoryProfileSummaryDialog(
    memoryProfile: MemoryProfileData,
    hasSourceFacts: Boolean,
    isSynthesizing: Boolean,
    onRefresh: () -> Unit,
    onSaveSection: (index: Int?, title: String, body: String) -> Unit,
    onDeleteSection: (Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var showSectionEditor by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editBody by remember { mutableStateOf("") }
    var confirmOverwriteRefresh by remember { mutableStateOf(false) }

    val statusText = when {
        isSynthesizing -> "Membuat ringkasan…"
        memoryProfile.updatedAt > 0L -> {
            val stamp = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID")).format(Date(memoryProfile.updatedAt))
            if (memoryProfile.manuallyEdited) "Diedit manual • $stamp" else "Diperbarui $stamp"
        }
        else -> "Belum dirangkum"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DominantBg)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DominantBg.copy(alpha = 0.96f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                        .border(1.dp, BorderLight, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ringkasan memori",
                        fontFamily = UiSans,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = statusText,
                        fontFamily = UiSans,
                        fontSize = 11.5.sp,
                        color = if (memoryProfile.manuallyEdited) AccentBlue else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, CircleShape)
                            .clickable { menuOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Opsi Ringkasan Memori",
                            tint = TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        modifier = Modifier.background(SecondarySurface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("Tambah Bagian", fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary)
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Add, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                menuOpen = false
                                editingIndex = null
                                editTitle = ""
                                editBody = ""
                                showSectionEditor = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text("Perbarui Ringkasan", fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary)
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            },
                            enabled = !isSynthesizing && hasSourceFacts,
                            onClick = {
                                menuOpen = false
                                if (memoryProfile.manuallyEdited && memoryProfile.sections.isNotEmpty()) {
                                    confirmOverwriteRefresh = true
                                } else {
                                    onRefresh()
                                }
                            }
                        )
                        if (memoryProfile.sections.isNotEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text("Hapus Ringkasan", fontFamily = UiSans, fontSize = 13.sp, color = SemanticDanger)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = SemanticDanger, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    menuOpen = false
                                    onClear()
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (memoryProfile.sections.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isSynthesizing) {
                                "Rem sedang menyusun ringkasan memori tentangmu…"
                            } else if (hasSourceFacts) {
                                "Ringkasan memori belum disusun. Ketuk menu ⋮ di kanan atas lalu pilih Perbarui Ringkasan atau Tambah Bagian."
                            } else {
                                "Belum ada fakta personal tersimpan. Ngobrol dengan Rem, tambahkan fakta di Setelan, atau tulis bagian ringkasan secara manual."
                            },
                            fontFamily = UiSans,
                            fontSize = 13.5.sp,
                            lineHeight = 21.sp,
                            color = TextMuted
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentBlue.copy(alpha = 0.16f))
                                .border(1.dp, AccentBlue.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .clickable {
                                    editingIndex = null
                                    editTitle = ""
                                    editBody = ""
                                    showSectionEditor = true
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                            Text("Tambah Bagian Manual", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue)
                        }
                    }
                } else {
                    memoryProfile.sections.forEachIndexed { idx, sec ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sec.title,
                                    fontFamily = UiSans,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit bagian",
                                        tint = AccentBlue,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                editingIndex = idx
                                                editTitle = sec.title
                                                editBody = sec.body
                                                showSectionEditor = true
                                            }
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Hapus bagian",
                                        tint = TextMuted,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { onDeleteSection(idx) }
                                    )
                                }
                            }
                            Text(
                                text = sec.body,
                                fontFamily = UiSans,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = TextPrimary.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSectionEditor) {
        AlertDialog(
            onDismissRequest = { showSectionEditor = false },
            containerColor = SecondarySurface,
            title = {
                Text(
                    text = if (editingIndex == null) "Tambah Bagian Ringkasan" else "Edit Bagian Ringkasan",
                    color = TextPrimary,
                    fontFamily = UiSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Judul Bagian", color = TextMuted, fontSize = 11.sp, fontFamily = UiSans)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                    ) {
                        BasicTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            singleLine = true,
                            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp, fontFamily = UiSans),
                            cursorBrush = SolidColor(AccentBlue),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (editTitle.isEmpty()) {
                                    Text("Misal: Pola Pikir & Prinsip", color = TextSubtle, fontSize = 12.5.sp, fontFamily = UiSans)
                                }
                                inner()
                            }
                        )
                    }
                    Text("Isi Ringkasan", color = TextMuted, fontSize = 11.sp, fontFamily = UiSans)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                    ) {
                        BasicTextField(
                            value = editBody,
                            onValueChange = { editBody = it },
                            singleLine = false,
                            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp, fontFamily = UiSans),
                            cursorBrush = SolidColor(AccentBlue),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (editBody.isEmpty()) {
                                    Text("Tulis narasi tentang dirimu agar Rem mengingatnya…", color = TextSubtle, fontSize = 12.5.sp, fontFamily = UiSans)
                                }
                                inner()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editTitle.isNotBlank() && editBody.isNotBlank(),
                    onClick = {
                        onSaveSection(editingIndex, editTitle, editBody)
                        showSectionEditor = false
                    }
                ) {
                    Text("Simpan", color = AccentBlue, fontFamily = UiSans, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSectionEditor = false }) {
                    Text("Batal", color = TextMuted, fontFamily = UiSans)
                }
            }
        )
    }

    if (confirmOverwriteRefresh) {
        RemAlertDialog(
            title = "Timpa Editan Manual?",
            description = "Ringkasan Memori memiliki editan manual. Memperbarui ringkasan lewat AI akan menggantikan seluruh isi ringkasan saat ini. Lanjutkan?",
            confirmText = "Perbarui",
            dismissText = "Batal",
            tone = RemAlertTone.WARNING,
            onConfirm = {
                confirmOverwriteRefresh = false
                onRefresh()
            },
            onDismiss = { confirmOverwriteRefresh = false }
        )
    }
}

@Composable
private fun MemoryManagerDialog(
    memories: List<UserMemoryEntity>,
    summaries: List<ConversationSummaryEntity>,
    onDeleteMemory: (Long) -> Unit,
    onDeleteSummary: (Long) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("semua") }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmClearAll by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("in", "ID")) }
    val categories = remember { listOf("semua", "identitas", "preferensi", "kebiasaan", "tujuan", "catatan") }

    val query = searchQuery.trim()
    val filteredMemories = remember(memories, selectedCategory, query) {
        memories.filter { item ->
            val catOk = selectedCategory == "semua" || item.category.equals(selectedCategory, ignoreCase = true)
            val queryOk = query.isEmpty() ||
                item.factKey.contains(query, ignoreCase = true) ||
                item.factValue.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true)
            catOk && queryOk
        }
    }
    val filteredSummaries = remember(summaries, query) {
        summaries.filter { item ->
            query.isEmpty() || item.summaryText.contains(query, ignoreCase = true)
        }
    }

    if (confirmClearAll) {
        RemAlertDialog(
            title = "Hapus Semua Memori?",
            description = "Seluruh fakta personal (${memories.size}) dan rangkuman sesi (${summaries.size}) akan dihapus permanen dari SQLite.",
            confirmText = "Hapus Semua",
            dismissText = "Batal",
            tone = RemAlertTone.DANGER,
            onConfirm = {
                confirmClearAll = false
                onClearAll()
            },
            onDismiss = { confirmClearAll = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DominantBg)
                .statusBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DominantBg.copy(alpha = 0.96f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                        .border(1.dp, BorderLight, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fakta & Rangkuman",
                        fontFamily = UiSans,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${memories.size} fakta • ${summaries.size} rangkuman",
                        fontFamily = UiSans,
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, CircleShape)
                            .clickable { menuOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Opsi Memori",
                            tint = TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        modifier = Modifier.background(SecondarySurface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Bersihkan Semua",
                                    fontFamily = UiSans,
                                    fontSize = 13.sp,
                                    color = if (memories.isNotEmpty() || summaries.isNotEmpty()) SemanticDanger else TextSubtle
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = if (memories.isNotEmpty() || summaries.isNotEmpty()) SemanticDanger else TextSubtle,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            enabled = memories.isNotEmpty() || summaries.isNotEmpty(),
                            onClick = {
                                menuOpen = false
                                confirmClearAll = true
                            }
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

            // Tab & Search Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondarySurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "Fakta (${memories.size})",
                        "Rangkuman (${summaries.size})"
                    ).forEachIndexed { idx, label ->
                        val active = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (active) AccentBlue.copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (active) AccentBlue.copy(alpha = 0.55f) else Color.Transparent,
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .clickable { selectedTab = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = UiSans,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (active) AccentBlue else TextMuted,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Search bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = if (selectedTab == 0) "Cari label, nilai, atau kategori…" else "Cari isi rangkuman sesi…",
                                fontFamily = UiSans,
                                fontSize = 12.sp,
                                color = TextSubtle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = UiSans, fontSize = 12.5.sp, color = TextPrimary),
                            cursorBrush = SolidColor(AccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Hapus Pencarian",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                if (selectedTab == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val active = cat == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface)
                                    .border(
                                        1.dp,
                                        if (active) AccentBlue.copy(alpha = 0.55f) else BorderSubtle,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = cat.replaceFirstChar { it.uppercase() },
                                    fontFamily = UiSans,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (active) AccentBlue else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTab == 0) {
                    if (filteredMemories.isEmpty()) {
                        item {
                            Text(
                                text = if (memories.isEmpty()) {
                                    "Belum ada fakta personal tersimpan. Ngobrol dengan Rem atau tambahkan fakta manual di Setelan."
                                } else {
                                    "Tidak ada fakta yang cocok dengan pencarian/filter saat ini."
                                },
                                fontFamily = UiSans,
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp,
                                color = TextSubtle,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        items(filteredMemories, key = { "mem_${it.id}" }) { item ->
                            MemoryFactCard(
                                item = item,
                                dateFormatter = dateFormatter,
                                onDelete = { onDeleteMemory(item.id) }
                            )
                        }
                    }
                } else {
                    if (filteredSummaries.isEmpty()) {
                        item {
                            Text(
                                text = if (summaries.isEmpty()) {
                                    "Belum ada rangkuman sesi percakapan tersimpan."
                                } else {
                                    "Tidak ada rangkuman sesi yang cocok dengan pencarian."
                                },
                                fontFamily = UiSans,
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp,
                                color = TextSubtle,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    } else {
                        items(filteredSummaries, key = { "sum_${it.id}" }) { summary ->
                            MemorySummaryCard(
                                summary = summary,
                                dateFormatter = dateFormatter,
                                onDelete = { onDeleteSummary(summary.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryFactCard(
    item: UserMemoryEntity,
    dateFormatter: SimpleDateFormat,
    onDelete: () -> Unit
) {
    val isDeprecated = item.status != "active"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SecondarySurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDeprecated) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SemanticWarning.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "USANG",
                            fontFamily = UiSans,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SemanticWarning
                        )
                    }
                }
                Text(
                    text = item.category.uppercase(),
                    fontFamily = UiSans,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDeprecated) TextMuted else AccentBlue
                )
                Text(
                    text = "• ${item.factKey}",
                    fontFamily = UiSans,
                    fontSize = 10.5.sp,
                    color = TextSubtle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.updatedAt > 0L) {
                    Text(
                        text = "• ${dateFormatter.format(Date(item.updatedAt))}",
                        fontFamily = UiSans,
                        fontSize = 10.sp,
                        color = TextSubtle,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = item.factValue,
                fontFamily = UiSans,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = if (isDeprecated) TextMuted else TextPrimary
            )
        }
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Hapus Memori",
            tint = TextMuted,
            modifier = Modifier
                .padding(start = 10.dp)
                .size(17.dp)
                .clickable(onClick = onDelete)
        )
    }
}

@Composable
private fun MemorySummaryCard(
    summary: ConversationSummaryEntity,
    dateFormatter: SimpleDateFormat,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SecondarySurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (summary.createdAt > 0L) {
                Text(
                    text = "${dateFormatter.format(Date(summary.createdAt))} • ${summary.turnCount} pesan",
                    fontFamily = UiSans,
                    fontSize = 10.sp,
                    color = TextSubtle
                )
            }
            Text(
                text = summary.summaryText,
                fontFamily = UiSans,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = TextPrimary
            )
        }
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Hapus Rangkuman",
            tint = TextMuted,
            modifier = Modifier
                .padding(start = 10.dp)
                .size(17.dp)
                .clickable(onClick = onDelete)
        )
    }
}

@Composable
private fun CloudBackupDialog(
    cloudState: CloudBackupUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRefreshInfo: () -> Unit,
    onBackupNow: () -> Unit,
    onPrepareRestore: suspend () -> CloudRestoreFetchResult,
    onDecryptBytes: (ByteArray, String) -> Result<ByteArray>,
    onImportPlainZip: suspend (ByteArray, Boolean) -> Unit,
    onSetAutoSchedule: (String, Boolean) -> Unit,
    onSetPassphrase: (String) -> Unit,
    onShowBanner: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID")) }

    var showRestoreModeDialog by remember { mutableStateOf(false) }
    var pendingRestoreReplace by remember { mutableStateOf(false) }
    var pendingEncryptedPayload by remember { mutableStateOf<ByteArray?>(null) }
    var restorePassInput by remember { mutableStateOf("") }
    var restorePassError by remember { mutableStateOf<String?>(null) }

    var showPassphraseDialog by remember { mutableStateOf(false) }
    var passphraseInput by remember { mutableStateOf("") }
    var passphraseConfirm by remember { mutableStateOf("") }
    var passphraseReveal by remember { mutableStateOf(false) }
    var passphraseError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(cloudState.connected) {
        if (cloudState.connected) {
            onRefreshInfo()
        }
    }

    fun launchCloudRestore(replaceAll: Boolean) {
        showRestoreModeDialog = false
        pendingRestoreReplace = replaceAll
        scope.launch {
            when (val res = onPrepareRestore()) {
                is CloudRestoreFetchResult.Ready -> onImportPlainZip(res.zipBytes, replaceAll)
                is CloudRestoreFetchResult.NeedsPassphrase -> {
                    restorePassInput = ""
                    restorePassError = if (res.wrongSavedPassphrase) "Passphrase tersimpan tidak cocok." else null
                    pendingEncryptedPayload = res.encryptedBytes
                }
                CloudRestoreFetchResult.NeedsReauth -> onShowBanner("Sesi Google Drive berakhir, silakan hubungkan ulang.")
                is CloudRestoreFetchResult.Error -> onShowBanner(res.message)
            }
        }
    }

    if (showRestoreModeDialog) {
        RemAlertDialog(
            title = "Mode Restore Cloud",
            description = "Merge: tambahkan data cloud yang belum ada di HP.\nReplace: ganti total data modul saat ini dengan backup cloud.",
            confirmText = "Merge",
            secondaryConfirmText = "Replace",
            dismissText = "Batal",
            tone = RemAlertTone.INFO,
            badgeIcon = Icons.Outlined.Settings,
            onConfirm = { launchCloudRestore(replaceAll = false) },
            onSecondaryConfirm = { launchCloudRestore(replaceAll = true) },
            onDismiss = { showRestoreModeDialog = false }
        )
    }

    pendingEncryptedPayload?.let { encBytes ->
        RemInputDialog(
            subtitle = "Passphrase Cadangan Cloud",
            value = restorePassInput,
            onValueChange = {
                restorePassInput = it
                restorePassError = null
            },
            placeholder = "Passphrase…",
            helperText = "File backup di Google Drive terenkripsi AES-256-GCM. Masukkan passphrase untuk mendekripsi.",
            errorText = restorePassError,
            isSecret = true,
            confirmText = "Buka & Restore",
            dismissText = "Batal",
            confirmEnabled = restorePassInput.isNotBlank(),
            onConfirm = {
                val decRes = onDecryptBytes(encBytes, restorePassInput)
                decRes.onSuccess { plainZip ->
                    pendingEncryptedPayload = null
                    scope.launch {
                        onImportPlainZip(plainZip, pendingRestoreReplace)
                    }
                }.onFailure {
                    restorePassError = "Passphrase salah atau file rusak."
                }
            },
            onDismiss = { pendingEncryptedPayload = null }
        )
    }

    if (showPassphraseDialog) {
        AlertDialog(
            onDismissRequest = { showPassphraseDialog = false },
            containerColor = SecondarySurface,
            title = {
                Text("Enkripsi Passphrase Cloud", fontFamily = EditorialSerif, fontSize = 20.sp, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Mengunci seluruh arsip ZIP v3 dengan AES-256-GCM sebelum diunggah ke Google Drive. Penting: jika lupa passphrase, cadangan cloud tidak bisa didekripsi.",
                        fontFamily = UiSans,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = SemanticWarning.copy(alpha = 0.95f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (passphraseInput.isEmpty()) {
                                Text("Passphrase baru (min. 4 karakter)", fontFamily = UiSans, fontSize = 12.5.sp, color = TextSubtle)
                            }
                            BasicTextField(
                                value = passphraseInput,
                                onValueChange = {
                                    passphraseInput = it
                                    passphraseError = null
                                },
                                singleLine = true,
                                visualTransformation = if (passphraseReveal) VisualTransformation.None else PasswordVisualTransformation(),
                                textStyle = TextStyle(fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary),
                                cursorBrush = SolidColor(AccentBlue),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Icon(
                            imageVector = if (passphraseReveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Tampilkan",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { passphraseReveal = !passphraseReveal }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (passphraseConfirm.isEmpty()) {
                            Text("Ulangi passphrase", fontFamily = UiSans, fontSize = 12.5.sp, color = TextSubtle)
                        }
                        BasicTextField(
                            value = passphraseConfirm,
                            onValueChange = {
                                passphraseConfirm = it
                                passphraseError = null
                            },
                            singleLine = true,
                            visualTransformation = if (passphraseReveal) VisualTransformation.None else PasswordVisualTransformation(),
                            textStyle = TextStyle(fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary),
                            cursorBrush = SolidColor(AccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (!passphraseError.isNullOrBlank()) {
                        Text(passphraseError!!, fontFamily = UiSans, fontSize = 11.sp, color = SemanticDanger)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val p = passphraseInput.trim()
                        when {
                            p.length < 4 -> passphraseError = "Passphrase minimal 4 karakter."
                            p != passphraseConfirm.trim() -> passphraseError = "Konfirmasi passphrase tidak cocok."
                            else -> {
                                onSetPassphrase(p)
                                showPassphraseDialog = false
                                onShowBanner("Enkripsi passphrase cloud diaktifkan")
                            }
                        }
                    }
                ) {
                    Text("Simpan", color = AccentBlue, fontFamily = UiSans, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPassphraseDialog = false }) {
                    Text("Batal", color = TextMuted, fontFamily = UiSans)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DominantBg)
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DominantBg.copy(alpha = 0.96f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                        .border(1.dp, BorderLight, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Kembali",
                        tint = TextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Cloud Backup",
                        fontFamily = UiSans,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Google Drive • appDataFolder",
                        fontFamily = UiSans,
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                        .border(1.dp, BorderLight, CircleShape)
                        .clickable(enabled = cloudState.connected && !cloudState.isSyncing) { onRefreshInfo() },
                    contentAlignment = Alignment.Center
                ) {
                    if (cloudState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AccentBlue
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Segarkan Info Cloud",
                            tint = if (cloudState.connected) TextPrimary else TextSubtle,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Koneksi & Remote Info
                SettingsSection(
                    title = "KONEKSI GOOGLE DRIVE",
                    badgeText = if (cloudState.connected) "Terhubung" else null
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = cloudState.lastStatusLabel,
                            fontFamily = UiSans,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Backup cloud terakhir: ${cloudState.lastBackupLabel}",
                            fontFamily = UiSans,
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )

                        cloudState.remoteBackupInfo?.let { info ->
                            val remoteStamp = if (info.modifiedAtMs > 0L) dateFmt.format(Date(info.modifiedAtMs)) else "-"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${info.fileName} (${formatCloudSize(info.sizeBytes)})",
                                        fontFamily = UiSans,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Di Drive: $remoteStamp",
                                        fontFamily = UiSans,
                                        fontSize = 10.5.sp,
                                        color = TextMuted
                                    )
                                }
                                if (info.encrypted) {
                                    NestedPill {
                                        Text("REMC1", fontFamily = UiSans, fontSize = 10.sp, color = AccentBlue)
                                    }
                                }
                            }
                        }

                        if (!cloudState.connected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentBlue)
                                    .clickable(enabled = !cloudState.isSyncing, onClick = onConnect)
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Hubungkan Google Drive",
                                    fontFamily = UiSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DominantBg
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SemanticDanger.copy(alpha = 0.08f))
                                    .border(1.dp, SemanticDanger.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .clickable(enabled = !cloudState.isSyncing, onClick = onDisconnect)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Putuskan Koneksi Drive",
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SemanticDanger
                                )
                            }
                        }
                    }
                }

                // Aksi Manual
                SettingsSection(title = "SINKRONISASI MANUAL") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (cloudState.connected) ElevatedSurface else ElevatedSurface.copy(alpha = 0.5f))
                                    .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                                    .clickable(enabled = cloudState.connected && !cloudState.isSyncing, onClick = onBackupNow)
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                                    Text("Cadangkan", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (cloudState.connected) TextPrimary else TextSubtle)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (cloudState.connected) ElevatedSurface else ElevatedSurface.copy(alpha = 0.5f))
                                    .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                                    .clickable(enabled = cloudState.connected && !cloudState.isSyncing) {
                                        showRestoreModeDialog = true
                                    }
                                    .padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    Text("Pulihkan", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (cloudState.connected) TextPrimary else TextSubtle)
                                }
                            }
                        }
                        Text(
                            text = "Mencadangkan seluruh memori, obrolan, dan 5 modul produktivitas (ZIP v3). API Key LLM/TTS tidak ikut diunggah.",
                            fontFamily = UiSans,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = TextMuted
                        )
                    }
                }

                // Auto-Backup WorkManager
                SettingsSection(title = "AUTO-BACKUP TERJADWAL") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("off" to "Mati", "daily" to "Harian", "weekly" to "Mingguan").forEach { (key, label) ->
                                val active = cloudState.autoSchedule == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (active) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface)
                                        .border(1.dp, if (active) AccentBlue.copy(alpha = 0.6f) else BorderSubtle, RoundedCornerShape(10.dp))
                                        .clickable {
                                            onSetAutoSchedule(key, cloudState.wifiOnly)
                                        }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = UiSans,
                                        fontSize = 12.sp,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (active) AccentBlue else TextMuted
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetAutoSchedule(cloudState.autoSchedule, !cloudState.wifiOnly) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hanya via Wi-Fi", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Text("Hemat kuota seluler saat auto-backup berjalan", fontFamily = UiSans, fontSize = 11.sp, color = TextMuted)
                            }
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (cloudState.wifiOnly) AccentBlue else ElevatedSurface)
                                    .border(1.dp, if (cloudState.wifiOnly) AccentBlue else BorderLight, RoundedCornerShape(50))
                                    .padding(3.dp),
                                contentAlignment = if (cloudState.wifiOnly) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (cloudState.wifiOnly) DominantBg else TextMuted)
                                )
                            }
                        }
                    }
                }

                // Enkripsi Passphrase
                SettingsSection(
                    title = "ENKRIPSI PASSPHRASE (OPSIONAL)",
                    badgeText = if (cloudState.encryptionEnabled) "AES-256" else null
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (cloudState.encryptionEnabled) {
                                "Aktif: Seluruh file ZIP dikunci AES-256-GCM sebelum diunggah."
                            } else {
                                "Nonaktif: ZIP v3 diunggah tanpa password tambahan (hanya link privat LinkKu yang terenkripsi PIN)."
                            },
                            fontFamily = UiSans,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = TextMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                                    .clickable {
                                        passphraseInput = ""
                                        passphraseConfirm = ""
                                        passphraseError = null
                                        showPassphraseDialog = true
                                    }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (cloudState.encryptionEnabled) "Ubah Passphrase" else "Aktifkan Passphrase",
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentBlue
                                )
                            }
                            if (cloudState.encryptionEnabled) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SemanticDanger.copy(alpha = 0.08f))
                                        .border(1.dp, SemanticDanger.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            onSetPassphrase("")
                                            onShowBanner("Enkripsi passphrase cloud dinonaktifkan")
                                        }
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Matikan",
                                        fontFamily = UiSans,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SemanticDanger
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCloudSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.2f MB", kb / 1024.0)
}


