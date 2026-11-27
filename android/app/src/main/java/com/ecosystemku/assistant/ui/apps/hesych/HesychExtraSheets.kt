package com.ecosystemku.assistant.ui.apps.hesych

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.ecosystemku.assistant.data.hesych.HesychBackupCodec
import com.ecosystemku.assistant.data.hesych.HesychBackupNeedsPasswordException
import com.ecosystemku.assistant.data.hesych.HesychHealthLevel
import com.ecosystemku.assistant.data.hesych.HesychHealthReport
import com.ecosystemku.assistant.data.local.HesychVaultItem
import com.ecosystemku.assistant.ui.components.RemAlertDialog
import com.ecosystemku.assistant.ui.components.RemAlertTone
import com.ecosystemku.assistant.ui.components.RemInputDialog
import com.ecosystemku.assistant.ui.theme.AppHesych
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
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun HesychHealthDialog(
    items: List<HesychVaultItem>,
    report: HesychHealthReport?,
    isCheckingBreach: Boolean,
    breachProgressLabel: String?,
    onScan: () -> Unit,
    onRunBreachCheck: () -> Unit,
    onOpenItem: (HesychVaultItem) -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        onScan()
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
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali", tint = TextPrimary, modifier = Modifier.size(19.dp))
                }
                Text("Kesehatan Vault", fontFamily = EditorialSerif, fontSize = 20.sp, color = TextPrimary)
                Spacer(modifier = Modifier.size(40.dp))
            }
            HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val score = report?.score ?: 100
                val level = report?.level ?: HesychHealthLevel.GREAT
                val scoreColor = when (level) {
                    HesychHealthLevel.GREAT -> SemanticSuccess
                    HesychHealthLevel.GOOD -> AppHesych
                    HesychHealthLevel.FAIR -> SemanticWarning
                    HesychHealthLevel.POOR -> SemanticDanger
                }

                // Score Hero Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(SecondarySurface)
                        .border(1.dp, scoreColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("SKOR KEAMANAN", fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle, letterSpacing = 0.8.sp)
                            Text("$score / 100", fontFamily = EditorialSerif, fontSize = 34.sp, color = TextPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(scoreColor.copy(alpha = 0.16f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(level.label, fontFamily = UiSans, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = scoreColor)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)),
                        color = scoreColor,
                        trackColor = ElevatedSurface
                    )
                    Text(
                        text = "${report?.checked ?: 0}/${report?.total ?: items.size} akun sudah dicek kebocoran",
                        fontFamily = UiSans,
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                // 4 Metric Grid
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HealthMetricCard("Bocor", "${report?.breached ?: 0}", SemanticDanger, Modifier.weight(1f))
                    HealthMetricCard("Lemah", "${report?.weak ?: 0}", SemanticWarning, Modifier.weight(1f))
                    HealthMetricCard("Duplikat", "${report?.dupExtra ?: 0}", Color(0xFF8B5CF6), Modifier.weight(1f))
                    HealthMetricCard(">90 Hari", "${report?.oldCount ?: 0}", Color(0xFF3B82F6), Modifier.weight(1f))
                }

                // HIBP Breach Check Action
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SecondarySurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppHesych)
                            .clickable(enabled = !isCheckingBreach) { onRunBreachCheck() }
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCheckingBreach) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF0A0F0D))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Memeriksa HIBP (${breachProgressLabel ?: "..."})",
                                fontFamily = UiSans,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0A0F0D)
                            )
                        } else {
                            Icon(Icons.Outlined.MonitorHeart, contentDescription = null, tint = Color(0xFF0A0F0D), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Cek Kebocoran HIBP (k-Anonymity)",
                                fontFamily = UiSans,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0A0F0D)
                            )
                        }
                    }
                    Text(
                        text = "Privasi terjaga: hanya 5 karakter awal hash SHA-1 yang dikirim ke Have I Been Pwned.",
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                // Problem Items List
                if (report != null) {
                    val issueRows = items.mapNotNull { item ->
                        val tags = buildList {
                            if (item.id in report.breachedIds) add("Bocor" to SemanticDanger)
                            if (item.id in report.weakIds) add("Lemah" to SemanticWarning)
                            if (item.id in report.dupIds) add("Duplikat" to Color(0xFF8B5CF6))
                            if (item.id in report.oldIds) add(">90 Hari" to Color(0xFF3B82F6))
                        }
                        if (tags.isEmpty()) null else item to tags
                    }

                    if (issueRows.isNotEmpty()) {
                        Text(
                            text = "AKUN PERLU PERHATIAN (${issueRows.size})",
                            fontFamily = UiSans,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSubtle,
                            letterSpacing = 0.8.sp
                        )
                        issueRows.forEach { (item, issueTags) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SecondarySurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                    .clickable { onOpenItem(item) }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontFamily = UiSans, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (item.username.isNotBlank()) {
                                        Text(item.username, fontFamily = UiSans, fontSize = 11.5.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    issueTags.forEach { (lbl, col) ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(col.copy(alpha = 0.16f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(lbl, fontFamily = UiSans, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = col)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SemanticSuccess.copy(alpha = 0.1f))
                                .border(1.dp, SemanticSuccess.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, tint = SemanticSuccess, modifier = Modifier.size(18.dp))
                            Text("Semua akun terlihat kuat dan tidak terdeteksi masalah.", fontFamily = UiSans, fontSize = 12.5.sp, color = SemanticSuccess)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthMetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SecondarySurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = EditorialSerif, fontSize = 20.sp, color = color)
        Text(label, fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted, maxLines = 1)
    }
}

@Composable
internal fun HesychBackupBioDialog(
    biometricAvailable: Boolean,
    biometricEnrolled: Boolean,
    onEnableBiometric: (FragmentActivity) -> Unit,
    onDisableBiometric: () -> Unit,
    onExportBackupJson: suspend (customPassword: String?) -> String,
    onImportBackupJson: suspend (rawJson: String, replaceAll: Boolean, customPassword: String?) -> Result<Int>,
    onShowToast: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    var pendingExportCustom by remember { mutableStateOf(false) }
    var customExportPassword by remember { mutableStateOf("") }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var pendingImportReplace by remember { mutableStateOf(false) }
    var showImportModeDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var importPasswordInput by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = pendingExportJson
        pendingExportJson = null
        if (uri != null && !payload.isNullOrBlank()) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(payload.toByteArray(Charsets.UTF_8))
                        } != null
                    }.getOrDefault(false)
                }
                onShowToast(if (ok) "Backup Hesych berhasil disimpan" else "Gagal menulis file backup")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                if (raw.isNullOrBlank()) {
                    onShowToast("Gagal membaca file backup")
                    return@launch
                }
                pendingImportJson = raw
                showImportModeDialog = true
            }
        }
    }

    if (pendingExportCustom) {
        RemInputDialog(
            subtitle = "Password Backup Khusus",
            value = customExportPassword,
            onValueChange = { customExportPassword = it },
            placeholder = "Minimal 6 karakter…",
            helperText = "Gunakan password khusus ini saat impor ke perangkat lain atau Hesych Web.",
            isSecret = true,
            confirmText = "Ekspor",
            dismissText = "Batal",
            confirmEnabled = customExportPassword.length >= 6,
            onConfirm = {
                val pw = customExportPassword
                customExportPassword = ""
                pendingExportCustom = false
                scope.launch {
                    runCatching { onExportBackupJson(pw) }
                        .onSuccess { jsonStr ->
                            pendingExportJson = jsonStr
                            exportLauncher.launch("hesych_custom_backup_${LocalDate.now()}.json")
                        }
                        .onFailure { err -> onShowToast("Gagal ekspor: ${err.message}") }
                }
            },
            onDismiss = {
                pendingExportCustom = false
                customExportPassword = ""
            }
        )
    }

    var importNeedsMasterHint by remember { mutableStateOf(false) }

    fun attemptImport(replaceAll: Boolean) {
        showImportModeDialog = false
        pendingImportReplace = replaceAll
        val raw = pendingImportJson ?: return
        scope.launch {
            onImportBackupJson(raw, replaceAll, null)
                .onSuccess { count ->
                    pendingImportJson = null
                    onShowToast(if (replaceAll) "$count akun berhasil di-restore" else "$count akun berhasil diimpor")
                }
                .onFailure { err ->
                    if (err is HesychBackupNeedsPasswordException) {
                        importNeedsMasterHint = err.isMasterBackup
                        importPasswordInput = ""
                        showImportPasswordDialog = true
                    } else {
                        pendingImportJson = null
                        onShowToast(err.message ?: "Gagal impor")
                    }
                }
        }
    }

    if (showImportModeDialog && pendingImportJson != null) {
        RemAlertDialog(
            title = "Mode Impor Hesych",
            description = "Merge: tambahkan akun dari file backup ke vault saat ini.\nReplace: ganti seluruh isi vault saat ini.",
            confirmText = "Merge",
            secondaryConfirmText = "Replace",
            dismissText = "Batal",
            tone = RemAlertTone.INFO,
            onConfirm = { attemptImport(replaceAll = false) },
            onSecondaryConfirm = { attemptImport(replaceAll = true) },
            onDismiss = {
                showImportModeDialog = false
                pendingImportJson = null
            }
        )
    }

    if (showImportPasswordDialog && pendingImportJson != null) {
        RemInputDialog(
            subtitle = if (importNeedsMasterHint) "Master Password Backup Lama" else "Password File Backup",
            value = importPasswordInput,
            onValueChange = { importPasswordInput = it },
            placeholder = if (importNeedsMasterHint) "Masukkan master password lama…" else "Masukkan password backup…",
            helperText = if (importNeedsMasterHint) {
                "File backup Hesych Web ini dibuat dengan kunci vault lama. Masukkan master password lama untuk mendekripsi dan memindahkan ke vault ini."
            } else {
                ""
            },
            isSecret = true,
            confirmText = "Dekripsi & Impor",
            dismissText = "Batal",
            confirmEnabled = importPasswordInput.isNotEmpty(),
            onConfirm = {
                val pw = importPasswordInput
                val raw = pendingImportJson!!
                val replaceAll = pendingImportReplace
                showImportPasswordDialog = false
                pendingImportJson = null
                importPasswordInput = ""
                scope.launch {
                    onImportBackupJson(raw, replaceAll, pw)
                        .onSuccess { count -> onShowToast("$count akun berhasil diimpor") }
                        .onFailure { err -> onShowToast(err.message ?: "Password salah") }
                }
            },
            onDismiss = {
                showImportPasswordDialog = false
                pendingImportJson = null
                importPasswordInput = ""
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keamanan & Backup", fontFamily = EditorialSerif, fontSize = 19.sp, color = TextPrimary)
                Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = TextMuted, modifier = Modifier.size(20.dp).clickable(onClick = onDismiss))
            }

            // Biometric Toggle
            if (biometricAvailable && activity != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .clickable {
                            if (biometricEnrolled) onDisableBiometric()
                            else onEnableBiometric(activity)
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = AppHesych, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Buka Kunci Biometrik", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(
                                text = if (biometricEnrolled) "Aktif (AndroidKeyStore)" else "Ketuk untuk aktifkan sidik jari",
                                fontFamily = UiSans,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                    Text(
                        text = if (biometricEnrolled) "Matikan" else "Aktifkan",
                        fontFamily = UiSans,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (biometricEnrolled) SemanticWarning else AppHesych
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

            Text("BACKUP FORMAT HESYCH (.JSON)", fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle, letterSpacing = 0.8.sp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppHesych)
                    .clickable {
                        scope.launch {
                            runCatching { onExportBackupJson(null) }
                                .onSuccess { jsonStr ->
                                    pendingExportJson = jsonStr
                                    exportLauncher.launch("hesych_master_backup_${LocalDate.now()}.json")
                                }
                                .onFailure { err -> onShowToast("Gagal ekspor: ${err.message}") }
                        }
                    }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null, tint = Color(0xFF0A0F0D), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Export Backup (Master Key)", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0A0F0D))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .clickable { pendingExportCustom = true }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Export Backup (Custom Password)", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .clickable { importLauncher.launch(arrayOf("application/json", "*/*")) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Upload, contentDescription = null, tint = AppHesych, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Import Backup Hesych (.json)", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = AppHesych)
            }

            Text(
                text = "Vault terenkripsi juga otomatis ikut dalam Ecosystem ZIP v3 & Google Drive Backup.",
                fontFamily = UiSans,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}
