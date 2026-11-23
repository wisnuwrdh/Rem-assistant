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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
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
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import com.ecosystemku.assistant.data.local.ConversationSummaryEntity
import com.ecosystemku.assistant.data.local.UserMemoryEntity
import com.ecosystemku.assistant.notifications.DailyOsNotificationHelper
import com.ecosystemku.assistant.ui.asisten.EcosystemZipImportResult
import java.time.LocalDate
import java.util.Locale
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
    usePersonalKeys: Boolean = false,
    sumopodMaskedKey: String = "Belum diisi",
    groqMaskedKey: String = "Belum diisi",
    fishMaskedKey: String = "Belum diisi",
    apiConnectionOk: Boolean = true,
    apiConnectionLabel: String = "Terhubung",
    isTestingApi: Boolean = false,
    notifHabitEnabled: Boolean = true,
    notifTodoEnabled: Boolean = true,
    notifHabitTime: String = "07:00",
    notifTodoTime: String = "15:00",
    memories: List<UserMemoryEntity> = emptyList(),
    summaries: List<ConversationSummaryEntity> = emptyList(),
    autoLearnMemory: Boolean = true,
    isLearningMemory: Boolean = false,
    onUpdateProfileName: (String) -> Unit = {},
    onSetThemeMode: (String) -> Unit = {},
    onSetUsingPersonalKeys: (Boolean) -> Unit = {},
    onGetRawApiKey: (String) -> String = { "" },
    onSaveApiKey: (String, String) -> Unit = { _, _ -> },
    onTestApiConnection: () -> Unit = {},
    onSetNotifHabitEnabled: (Boolean) -> Unit = {},
    onSetNotifTodoEnabled: (Boolean) -> Unit = {},
    onSetNotifTimes: (String, String) -> Unit = { _, _ -> },
    onTestNotification: () -> Unit = {},
    onToggleAutoLearn: (Boolean) -> Unit = {},
    onAddManualMemory: (category: String, label: String, value: String) -> Unit = { _, _, _ -> },
    onDeleteMemory: (Long) -> Unit = {},
    onDeleteSummary: (Long) -> Unit = {},
    onClearMemories: () -> Unit = {},
    onExportZip: suspend () -> ByteArray = { ByteArray(0) },
    onImportZip: suspend (ByteArray, Boolean, String?) -> EcosystemZipImportResult = { _, _, _ ->
        EcosystemZipImportResult.Error("Belum terhubung")
    },
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

    var showProfileDialog by remember { mutableStateOf(false) }
    var profileInput by remember(profileName) { mutableStateOf(profileName) }

    var editingKeyProvider by remember { mutableStateOf<String?>(null) }
    var apiKeyInput by remember { mutableStateOf("") }
    var revealKeyInput by remember { mutableStateOf(false) }

    var showImportModeDialog by remember { mutableStateOf(false) }
    var importReplaceMode by remember { mutableStateOf(false) }
    var pendingPinPayload by remember { mutableStateOf<EcosystemZipImportResult.NeedsPin?>(null) }
    var pinInput by remember { mutableStateOf("") }

    var pendingResetTarget by remember { mutableStateOf<String?>(null) }
    var hasNotifPerm by remember {
        mutableStateOf(DailyOsNotificationHelper.hasNotificationPermission(context))
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

    fun pickTime(current: String, onPicked: (String) -> Unit) {
        val parts = current.split(":")
        val initH = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
        val initM = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        TimePickerDialog(
            context,
            { _, h, m ->
                onPicked(String.format(Locale.US, "%02d:%02d", h, m))
            },
            initH,
            initM,
            true
        ).show()
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
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            containerColor = SecondarySurface,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
            title = {
                Text("Ubah Nama Profil", fontFamily = EditorialSerif, fontSize = 20.sp, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Nama ini digunakan pada sapaan dashboard Apps dan panggilan Rem kepada Tuan.",
                        fontFamily = UiSans,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (profileInput.isEmpty()) {
                            Text("Nama panggilan...", fontFamily = UiSans, fontSize = 13.sp, color = TextSubtle)
                        }
                        BasicTextField(
                            value = profileInput,
                            onValueChange = { profileInput = it },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = UiSans, fontSize = 13.5.sp, color = TextPrimary),
                            cursorBrush = SolidColor(AccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateProfileName(profileInput)
                        showProfileDialog = false
                    }
                ) {
                    Text("Simpan", color = AccentBlue, fontFamily = UiSans, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Batal", color = TextMuted, fontFamily = UiSans)
                }
            }
        )
    }

    editingKeyProvider?.let { provider ->
        AlertDialog(
            onDismissRequest = { editingKeyProvider = null },
            containerColor = SecondarySurface,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
            title = {
                Text("API Key $provider", fontFamily = EditorialSerif, fontSize = 20.sp, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Disimpan terenkripsi dengan AES-256-GCM di Android Keystore perangkat ini. Kosongkan lalu simpan untuk menghapus key.",
                        fontFamily = UiSans,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = TextMuted
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (apiKeyInput.isEmpty()) {
                            Text("Masukkan API Key $provider...", fontFamily = UiSans, fontSize = 13.sp, color = TextSubtle)
                        }
                        BasicTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary),
                            cursorBrush = SolidColor(AccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveApiKey(provider, apiKeyInput)
                        editingKeyProvider = null
                    }
                ) {
                    Text("Simpan Key", color = AccentBlue, fontFamily = UiSans, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Row {
                    if (apiKeyInput.isNotBlank()) {
                        TextButton(
                            onClick = {
                                onSaveApiKey(provider, "")
                                editingKeyProvider = null
                            }
                        ) {
                            Text("Hapus", color = SemanticDanger, fontFamily = UiSans)
                        }
                    }
                    TextButton(onClick = { editingKeyProvider = null }) {
                        Text("Batal", color = TextMuted, fontFamily = UiSans)
                    }
                }
            }
        )
    }

    if (showImportModeDialog) {
        AlertDialog(
            onDismissRequest = { showImportModeDialog = false },
            containerColor = SecondarySurface,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
            title = {
                Text("Mode Import ZIP v3", fontFamily = EditorialSerif, fontSize = 20.sp, color = TextPrimary)
            },
            text = {
                Text(
                    "Pilih 'Merge' untuk menggabungkan data baru tanpa menghapus data lama, atau 'Replace' untuk mengganti seluruh data dengan isi file ZIP.",
                    fontFamily = UiSans,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        importReplaceMode = false
                        showImportModeDialog = false
                        importZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    }
                ) {
                    Text("Merge (Gabung)", color = AccentBlue, fontFamily = UiSans, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showImportModeDialog = false }) {
                        Text("Batal", color = TextMuted, fontFamily = UiSans)
                    }
                    TextButton(
                        onClick = {
                            importReplaceMode = true
                            showImportModeDialog = false
                            importZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                        }
                    ) {
                        Text("Replace (Ganti)", color = SemanticWarning, fontFamily = UiSans, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )
    }

    pendingPinPayload?.let { payload ->
        AlertDialog(
            onDismissRequest = { pendingPinPayload = null },
            containerColor = SecondarySurface,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
            title = {
                Text("PIN Tautan Privat LinkKu", fontFamily = EditorialSerif, fontSize = 20.sp, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Backup ZIP ini berisi tautan privat LinkKu terenkripsi AES-GCM. Masukkan PIN 4-digit untuk mendekripsi, atau lewati jika hanya ingin memulihkan data publik.",
                        fontFamily = UiSans,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = TextMuted
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        if (pinInput.isEmpty()) {
                            Text("PIN 4-digit...", fontFamily = UiSans, fontSize = 13.sp, color = TextSubtle)
                        }
                        BasicTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it.filter { ch -> ch.isDigit() }.take(8) },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = UiSans, fontSize = 14.sp, color = TextPrimary),
                            cursorBrush = SolidColor(AccentBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pinInput.isNotBlank(),
                    onClick = {
                        val pin = pinInput
                        pendingPinPayload = null
                        scope.launch {
                            when (val res = onImportZip(payload.zipBytes, payload.replaceAll, pin)) {
                                is EcosystemZipImportResult.Success -> onShowBanner(res.summary)
                                is EcosystemZipImportResult.Error -> onShowBanner(res.message)
                                is EcosystemZipImportResult.NeedsPin -> onShowBanner("PIN diperlukan")
                            }
                        }
                    }
                ) {
                    Text("Dekripsi & Import", color = AccentBlue, fontFamily = UiSans, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingPinPayload = null
                        scope.launch {
                            when (val res = onImportZip(payload.zipBytes, payload.replaceAll, "")) {
                                is EcosystemZipImportResult.Success -> onShowBanner("${res.summary} (tanpa link privat)")
                                is EcosystemZipImportResult.Error -> onShowBanner(res.message)
                                is EcosystemZipImportResult.NeedsPin -> Unit
                            }
                        }
                    }
                ) {
                    Text("Lewati Privat", color = TextMuted, fontFamily = UiSans)
                }
            }
        )
    }

    pendingResetTarget?.let { target ->
        val isAll = target == "ALL"
        val label = if (isAll) "Seluruh Data Asisten Rem" else "Data $target"
        AlertDialog(
            onDismissRequest = { pendingResetTarget = null },
            containerColor = SecondarySurface,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
            title = {
                Text("Reset $label?", fontFamily = EditorialSerif, fontSize = 20.sp, color = TextPrimary)
            },
            text = {
                Text(
                    if (isAll) {
                        "Tindakan ini akan menghapus semua data Daily OS, IdeKu, Kronik, CuciMoney+, LinkKu, serta riwayat obrolan & memori Asisten Rem di perangkat ini."
                    } else {
                        "Semua data lokal untuk modul $target akan dikosongkan dan dikembalikan ke kondisi awal."
                    },
                    fontFamily = UiSans,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
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
                    }
                ) {
                    Text("Ya, Reset", color = SemanticDanger, fontFamily = UiSans, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingResetTarget = null }) {
                    Text("Batal", color = TextMuted, fontFamily = UiSans)
                }
            }
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
                text = "Preferensi, AI, & Backup Ekosistem",
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

                        // Daftar fakta tersimpan
                        if (memories.isEmpty()) {
                            Text(
                                text = "Belum ada fakta personal tersimpan. Ngobrol dengan Rem atau tambahkan manual di atas.",
                                fontFamily = UiSans,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                color = TextSubtle
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                memories.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(ElevatedSurface)
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(11.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = item.category.uppercase(),
                                                    fontFamily = UiSans,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AccentBlue
                                                )
                                                Text(
                                                    text = "• ${item.factKey}",
                                                    fontFamily = UiSans,
                                                    fontSize = 10.5.sp,
                                                    color = TextSubtle,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.factValue,
                                                fontFamily = UiSans,
                                                fontSize = 12.5.sp,
                                                color = TextPrimary
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Hapus Memori",
                                            tint = TextMuted,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .size(16.dp)
                                                .clickable { onDeleteMemory(item.id) }
                                        )
                                    }
                                }
                            }
                        }

                        // Daftar rangkuman sesi
                        if (summaries.isNotEmpty()) {
                            HorizontalDivider(thickness = 1.dp, color = BorderSubtle)
                            Text(
                                text = "Rangkuman Sesi Percakapan",
                                fontFamily = UiSans,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                summaries.forEach { s ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(ElevatedSurface)
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(11.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = s.summaryText,
                                            fontFamily = UiSans,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Hapus Rangkuman",
                                            tint = TextMuted,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .size(16.dp)
                                                .clickable { onDeleteSummary(s.id) }
                                        )
                                    }
                                }
                            }
                        }

                        if (memories.isNotEmpty() || summaries.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SemanticDanger.copy(alpha = 0.08f))
                                    .border(1.dp, SemanticDanger.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .clickable { onClearMemories() }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Bersihkan Semua Memori & Rangkuman",
                                    fontFamily = UiSans,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SemanticDanger
                                )
                            }
                        }
                    }
                }
            }

            // 2. AI & API Key (AKTIF — Android Keystore Fase 2)
            item {
                SettingsSection(title = "AI & API KEY", trailing = "Android Keystore") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Segmented Control
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (!usePersonalKeys) AccentBlue else Color.Transparent)
                                    .clickable { onSetUsingPersonalKeys(false) }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Server (Default)",
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    fontWeight = if (!usePersonalKeys) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (!usePersonalKeys) DominantBg else TextMuted
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (usePersonalKeys) AccentBlue else Color.Transparent)
                                    .clickable { onSetUsingPersonalKeys(true) }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Personal Key",
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    fontWeight = if (usePersonalKeys) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (usePersonalKeys) DominantBg else TextMuted
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ApiKeyRow(
                                label = "Sumopod",
                                maskedValue = sumopodMaskedKey,
                                onClick = {
                                    apiKeyInput = onGetRawApiKey("Sumopod")
                                    revealKeyInput = false
                                    editingKeyProvider = "Sumopod"
                                }
                            )
                            ApiKeyRow(
                                label = "Groq",
                                maskedValue = groqMaskedKey,
                                onClick = {
                                    apiKeyInput = onGetRawApiKey("Groq")
                                    revealKeyInput = false
                                    editingKeyProvider = "Groq"
                                }
                            )
                            ApiKeyRow(
                                label = "Fish Audio",
                                maskedValue = fishMaskedKey,
                                onClick = {
                                    apiKeyInput = onGetRawApiKey("Fish Audio")
                                    revealKeyInput = false
                                    editingKeyProvider = "Fish Audio"
                                }
                            )
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
                                .clickable {
                                    onShowBanner("Cloud Backup Google Drive dijadwalkan pada v1.1. Gunakan Export ZIP v3 untuk backup lokal.")
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(16.dp))
                                Text("Cloud Backup (Google Drive)", fontFamily = UiSans, fontSize = 12.sp, color = TextMuted)
                            }
                            NestedPill {
                                Text("Segera Hadir (v1.1)", fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted)
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
                                Text("Asisten Rem v1.0.0 (Build 1)", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Android Native Super App • Kotlin Compose", fontFamily = UiSans, fontSize = 11.sp, color = TextMuted)
                            }
                            NestedPill {
                                Text("Android v1", fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted)
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
private fun ApiKeyRow(
    label: String,
    maskedValue: String,
    onClick: () -> Unit
) {
    val hasKey = maskedValue != "Belum diisi"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (hasKey) AccentBlue else TextSubtle)
            )
            Text(label, fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = maskedValue,
                fontFamily = UiSans,
                fontSize = 12.sp,
                color = if (hasKey) TextMuted else TextSubtle
            )
            Icon(
                imageVector = if (hasKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Edit,
                contentDescription = "Ubah API Key",
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
