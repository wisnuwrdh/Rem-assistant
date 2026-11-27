package com.ecosystemku.assistant.ui.apps.hesych

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ecosystemku.assistant.data.hesych.HesychGenMode
import com.ecosystemku.assistant.data.hesych.HesychGenOptions
import com.ecosystemku.assistant.data.hesych.HesychPasswordGenerator
import com.ecosystemku.assistant.data.hesych.HesychTotp
import com.ecosystemku.assistant.data.local.HesychCategory
import com.ecosystemku.assistant.data.local.HesychHistoryEntity
import com.ecosystemku.assistant.data.local.HesychVaultItem
import com.ecosystemku.assistant.ui.components.RemAlertDialog
import com.ecosystemku.assistant.ui.components.RemAlertTone
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

internal val HESYCH_ITEM_COLORS = listOf(
    Color(0xFF10B981),
    Color(0xFF3B82F6),
    Color(0xFF8B5CF6),
    Color(0xFFF59E0B),
    Color(0xFFEC4899),
    Color(0xFFEF4444)
)

internal fun strengthColor(score: Int): Color = when (score) {
    1 -> SemanticDanger
    2 -> SemanticWarning
    3 -> Color(0xFF3B82F6)
    4 -> SemanticSuccess
    else -> TextSubtle
}

@Composable
internal fun PasswordStrengthBar(password: String) {
    val score = remember(password) { HesychPasswordGenerator.scorePassword(password) }
    val label = remember(score) { HesychPasswordGenerator.strengthLabel(score) }
    val color = strengthColor(score)
    if (password.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { score / 4f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = ElevatedSurface
        )
        Text(
            text = "Kekuatan: $label",
            fontFamily = UiSans,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HesychItemDetailDialog(
    item: HesychVaultItem,
    isSecretLocked: Boolean,
    revealSecret: (String) -> String,
    generateTotp: (String, Long) -> String?,
    loadHistory: suspend (Long) -> List<HesychHistoryEntity> = { emptyList() },
    onCopy: (label: String, text: String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var revealPassword by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var totpCode by remember(item.id, isSecretLocked) { mutableStateOf<String?>(null) }
    var totpRemain by remember { mutableIntStateOf(HesychTotp.secondsRemaining()) }
    var historyEntries by remember(item.id) { mutableStateOf<List<HesychHistoryEntity>>(emptyList()) }

    val plainPassword = remember(item.id, item.password, isSecretLocked) {
        if (isSecretLocked) "" else revealSecret(item.password)
    }

    LaunchedEffect(item.id) {
        historyEntries = loadHistory(item.id)
    }

    LaunchedEffect(item.id, item.totpSecret, isSecretLocked) {
        if (item.totpSecret.isBlank() || isSecretLocked) {
            totpCode = null
            return@LaunchedEffect
        }
        while (true) {
            val nowMs = System.currentTimeMillis()
            totpCode = generateTotp(item.totpSecret, nowMs / 1000L)
            totpRemain = HesychTotp.secondsRemaining(nowMs)
            delay(1000L)
        }
    }

    if (showDeleteConfirm) {
        RemAlertDialog(
            title = "Hapus \"${item.title}\"?",
            description = "Item dan riwayat password-nya akan dihapus permanen dari vault lokal.",
            confirmText = "Hapus",
            dismissText = "Batal",
            tone = RemAlertTone.DANGER,
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            onDismiss = { showDeleteConfirm = false }
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
            // Header
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
                Text(
                    text = item.title,
                    fontFamily = EditorialSerif,
                    fontSize = 20.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, CircleShape)
                            .clickable(onClick = onEdit),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = AppHesych, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElevatedSurface)
                            .border(1.dp, BorderLight, CircleShape)
                            .clickable { showDeleteConfirm = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Hapus", tint = SemanticDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category & Tags
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val accent = HESYCH_ITEM_COLORS[item.color.coerceIn(0, HESYCH_ITEM_COLORS.lastIndex)]
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accent))
                    Text(
                        text = item.category.label,
                        fontFamily = UiSans,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                    if (item.favorite) {
                        Icon(Icons.Outlined.Star, contentDescription = "Favorit", tint = SemanticWarning, modifier = Modifier.size(15.dp))
                    }
                }

                // Username card
                DetailFieldCard(
                    label = "USERNAME / EMAIL",
                    value = item.username.ifBlank { "—" },
                    canCopy = item.username.isNotBlank(),
                    onCopy = { onCopy("Username", item.username) }
                )

                // Password card
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PASSWORD", fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle, letterSpacing = 0.8.sp)
                        if (!isSecretLocked) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    imageVector = if (revealPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Lihat Password",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp).clickable { revealPassword = !revealPassword }
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Salin Password",
                                    tint = AppHesych,
                                    modifier = Modifier.size(18.dp).clickable { onCopy("Password", plainPassword) }
                                )
                            }
                        }
                    }
                    if (isSecretLocked) {
                        Text(
                            text = "Terkunci oleh Secret Lock",
                            fontFamily = UiSans,
                            fontSize = 13.sp,
                            color = SemanticWarning
                        )
                    } else {
                        Text(
                            text = if (revealPassword) plainPassword else "••••••••••••",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        PasswordStrengthBar(plainPassword)
                    }
                }

                // TOTP Card
                if (item.totpSecret.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SecondarySurface)
                            .border(1.dp, AppHesych.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("KODE 2FA (TOTP)", fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = AppHesych, letterSpacing = 0.8.sp)
                            Text("${totpRemain}d", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextMuted)
                        }
                        if (isSecretLocked) {
                            Text("Terkunci oleh Secret Lock", fontFamily = UiSans, fontSize = 13.sp, color = SemanticWarning)
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val code = totpCode ?: "------"
                                val spaced = if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code
                                Text(
                                    text = spaced,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Salin TOTP",
                                    tint = AppHesych,
                                    modifier = Modifier.size(20.dp).clickable {
                                        if (totpCode != null) onCopy("Kode TOTP", totpCode!!)
                                    }
                                )
                            }
                            LinearProgressIndicator(
                                progress = { totpRemain / 30f },
                                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(3.dp)),
                                color = AppHesych,
                                trackColor = ElevatedSurface
                            )
                        }
                    }
                }

                // Custom Fields
                if (item.customFields.isNotEmpty()) {
                    Text("CUSTOM FIELDS", fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle, letterSpacing = 0.8.sp)
                    item.customFields.forEach { field ->
                        var revealed by remember(field.name) { mutableStateOf(field.type != "password") }
                        val plainVal = remember(field.value, isSecretLocked) {
                            if (isSecretLocked) "" else revealSecret(field.value)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SecondarySurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(field.name.ifBlank { "Field" }, fontFamily = UiSans, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                if (!isSecretLocked) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (field.type == "password") {
                                            Icon(
                                                imageVector = if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp).clickable { revealed = !revealed }
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "Salin",
                                            tint = AppHesych,
                                            modifier = Modifier.size(16.dp).clickable { onCopy(field.name, plainVal) }
                                        )
                                    }
                                }
                            }
                            Text(
                                text = when {
                                    isSecretLocked -> "Terkunci oleh Secret Lock"
                                    revealed -> plainVal.ifBlank { "—" }
                                    else -> "••••••••"
                                },
                                fontFamily = if (field.type == "password") FontFamily.Monospace else UiSans,
                                fontSize = 13.5.sp,
                                color = if (isSecretLocked) SemanticWarning else TextPrimary
                            )
                        }
                    }
                }

                // Tags
                if (item.tags.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ElevatedSurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text("#$tag", fontFamily = UiSans, fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }
                }

                // Notes
                if (item.notes.isNotBlank()) {
                    DetailFieldCard(
                        label = "CATATAN",
                        value = item.notes,
                        canCopy = true,
                        onCopy = { onCopy("Catatan", item.notes) }
                    )
                }

                // Password History
                if (historyEntries.isNotEmpty()) {
                    val histDateFmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID")) }
                    Text(
                        text = "RIWAYAT PASSWORD (${historyEntries.size})",
                        fontFamily = UiSans,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSubtle,
                        letterSpacing = 0.8.sp
                    )
                    historyEntries.forEach { entry ->
                        var showOld by remember(entry.hid) { mutableStateOf(false) }
                        val oldPlain = remember(entry.hid, isSecretLocked) {
                            if (isSecretLocked) "" else revealSecret(entry.encPassword)
                        }
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
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = when {
                                        isSecretLocked -> "Terkunci oleh Secret Lock"
                                        showOld -> oldPlain.ifBlank { "—" }
                                        else -> "••••••••••••"
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.5.sp,
                                    color = if (isSecretLocked) SemanticWarning else TextPrimary
                                )
                                Text(
                                    text = histDateFmt.format(Date(entry.changedAt)),
                                    fontFamily = UiSans,
                                    fontSize = 10.5.sp,
                                    color = TextSubtle
                                )
                            }
                            if (!isSecretLocked && oldPlain.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(
                                        imageVector = if (showOld) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Lihat Password Lama",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp).clickable { showOld = !showOld }
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Salin Password Lama",
                                        tint = AppHesych,
                                        modifier = Modifier.size(16.dp).clickable { onCopy("Password Lama", oldPlain) }
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

@Composable
private fun DetailFieldCard(
    label: String,
    value: String,
    canCopy: Boolean,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SecondarySurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle, letterSpacing = 0.8.sp)
            if (canCopy) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Salin $label",
                    tint = AppHesych,
                    modifier = Modifier.size(18.dp).clickable(onClick = onCopy)
                )
            }
        }
        Text(value, fontFamily = UiSans, fontSize = 14.sp, color = TextPrimary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HesychEditItemDialog(
    initialItem: HesychVaultItem?,
    revealSecret: (String) -> String,
    onSave: (
        id: Long,
        title: String,
        username: String,
        plainPassword: String,
        notes: String,
        color: Int,
        favorite: Boolean,
        category: HesychCategory,
        tags: List<String>,
        plainTotpSecret: String,
        plainCustomFields: List<Triple<String, String, String>>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialItem?.title.orEmpty()) }
    var username by remember { mutableStateOf(initialItem?.username.orEmpty()) }
    var password by remember { mutableStateOf(initialItem?.let { revealSecret(it.password) }.orEmpty()) }
    var showPassword by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(initialItem?.notes.orEmpty()) }
    var colorIdx by remember { mutableIntStateOf(initialItem?.color ?: 0) }
    var favorite by remember { mutableStateOf(initialItem?.favorite ?: false) }
    var category by remember { mutableStateOf(initialItem?.category ?: HesychCategory.OTHER) }
    var tagsInput by remember { mutableStateOf(initialItem?.tags?.joinToString(", ").orEmpty()) }
    var totpSecret by remember { mutableStateOf(initialItem?.let { revealSecret(it.totpSecret) }.orEmpty()) }
    var showGenerator by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val customFields = remember {
        mutableStateListOf<Triple<String, String, String>>().apply {
            initialItem?.customFields?.forEach { cf ->
                add(Triple(cf.name, revealSecret(cf.value), cf.type))
            }
        }
    }

    if (showGenerator) {
        HesychGeneratorDialog(
            onCopy = { generated ->
                password = generated
                showGenerator = false
            },
            actionLabel = "Gunakan",
            onDismiss = { showGenerator = false }
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
                .imePadding()
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
                    Icon(Icons.Outlined.Close, contentDescription = "Batal", tint = TextPrimary, modifier = Modifier.size(19.dp))
                }
                Text(
                    text = if (initialItem == null) "Tambah Akun" else "Edit Akun",
                    fontFamily = EditorialSerif,
                    fontSize = 20.sp,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppHesych)
                        .clickable {
                            if (title.isBlank()) {
                                errorMsg = "Judul akun wajib diisi"
                                return@clickable
                            }
                            if (password.isBlank()) {
                                errorMsg = "Password wajib diisi"
                                return@clickable
                            }
                            val cleanTotp = totpSecret.trim().replace(" ", "")
                            if (cleanTotp.isNotEmpty() && !HesychTotp.isValidBase32(cleanTotp)) {
                                errorMsg = "Secret TOTP harus berformat Base32 valid (A-Z, 2-7)"
                                return@clickable
                            }
                            val parsedTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                            onSave(
                                initialItem?.id ?: 0L,
                                title,
                                username,
                                password,
                                notes,
                                colorIdx,
                                favorite,
                                category,
                                parsedTags,
                                cleanTotp,
                                customFields.toList()
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Simpan", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0A0F0D))
                }
            }
            HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!errorMsg.isNullOrBlank()) {
                    Text(errorMsg!!, fontFamily = UiSans, fontSize = 12.sp, color = SemanticDanger)
                }

                LabeledInputBox("JUDUL AKUN", title, { title = it; errorMsg = null }, "Contoh: Gmail Utama")
                LabeledInputBox("USERNAME / EMAIL", username, { username = it }, "email@domain.com")

                // Password + quick generate
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PASSWORD", fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle, letterSpacing = 0.8.sp)
                        Text(
                            text = "Buka Generator",
                            fontFamily = UiSans,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppHesych,
                            modifier = Modifier.clickable { showGenerator = true }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SecondarySurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = password,
                            onValueChange = { password = it; errorMsg = null },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = TextPrimary),
                            cursorBrush = SolidColor(AppHesych),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (password.isEmpty()) Text("Masukkan password…", fontFamily = UiSans, fontSize = 13.sp, color = TextSubtle)
                                inner()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp).clickable { showPassword = !showPassword }
                        )
                    }
                    PasswordStrengthBar(password)
                }

                // Category chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("KATEGORI", fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle, letterSpacing = 0.8.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HesychCategory.entries.forEach { cat ->
                            val selected = cat == category
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) AppHesych.copy(alpha = 0.18f) else ElevatedSurface)
                                    .border(1.dp, if (selected) AppHesych.copy(alpha = 0.6f) else BorderSubtle, RoundedCornerShape(10.dp))
                                    .clickable { category = cat }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(cat.label, fontFamily = UiSans, fontSize = 11.5.sp, color = if (selected) AppHesych else TextMuted, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }

                // Color + Favorite row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HESYCH_ITEM_COLORS.forEachIndexed { idx, col ->
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(2.dp, if (idx == colorIdx) TextPrimary else Color.Transparent, CircleShape)
                                    .clickable { colorIdx = idx }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { favorite = !favorite }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (favorite) SemanticWarning else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Favorit", fontFamily = UiSans, fontSize = 11.5.sp, color = TextPrimary)
                    }
                }

                LabeledInputBox("TOTP 2FA SECRET (BASE32 OPSIONAL)", totpSecret, { totpSecret = it; errorMsg = null }, "Contoh: JBSWY3DPEHPK3PXP")
                LabeledInputBox("TAG (PISAHKAN DENGAN KOMA)", tagsInput, { tagsInput = it }, "kerja, utama, bank")
                LabeledInputBox("CATATAN", notes, { notes = it }, "Catatan tambahan…", singleLine = false)

                // Custom Fields editor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CUSTOM FIELDS", fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle, letterSpacing = 0.8.sp)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { customFields.add(Triple("", "", "text")) }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = AppHesych, modifier = Modifier.size(14.dp))
                        Text("Tambah Field", fontFamily = UiSans, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = AppHesych)
                    }
                }

                customFields.forEachIndexed { index, (fName, fVal, fType) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SecondarySurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = fName,
                                onValueChange = { customFields[index] = Triple(it, fVal, fType) },
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary),
                                cursorBrush = SolidColor(AppHesych),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (fName.isEmpty()) Text("Nama field (mis. PIN)", fontFamily = UiSans, fontSize = 12.sp, color = TextSubtle)
                                    inner()
                                }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (fType == "password") "Rahasia" else "Teks",
                                    fontFamily = UiSans,
                                    fontSize = 11.sp,
                                    color = AppHesych,
                                    modifier = Modifier.clickable {
                                        val nextType = if (fType == "password") "text" else "password"
                                        customFields[index] = Triple(fName, fVal, nextType)
                                    }
                                )
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Hapus field",
                                    tint = SemanticDanger,
                                    modifier = Modifier.size(16.dp).clickable { customFields.removeAt(index) }
                                )
                            }
                        }
                        BasicTextField(
                            value = fVal,
                            onValueChange = { customFields[index] = Triple(fName, it, fType) },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = if (fType == "password") FontFamily.Monospace else UiSans, fontSize = 13.sp, color = TextPrimary),
                            cursorBrush = SolidColor(AppHesych),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (fVal.isEmpty()) Text("Isi nilai…", fontFamily = UiSans, fontSize = 12.sp, color = TextSubtle)
                                inner()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledInputBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextSubtle, letterSpacing = 0.8.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(fontFamily = UiSans, fontSize = 14.sp, color = TextPrimary),
            cursorBrush = SolidColor(AppHesych),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, fontFamily = UiSans, fontSize = 13.sp, color = TextSubtle)
                inner()
            }
        )
    }
}

@Composable
internal fun HesychGeneratorDialog(
    onCopy: (String) -> Unit,
    actionLabel: String = "Salin",
    onDismiss: () -> Unit
) {
    var options by remember { mutableStateOf(HesychGenOptions()) }
    var preview by remember { mutableStateOf(HesychPasswordGenerator.generateOne(options)) }

    fun regenerate(next: HesychGenOptions = options) {
        options = next
        preview = HesychPasswordGenerator.generateOne(next)
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
                Text("Generator Password", fontFamily = EditorialSerif, fontSize = 19.sp, color = TextPrimary)
                Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = TextMuted, modifier = Modifier.size(20.dp).clickable(onClick = onDismiss))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(HesychGenMode.PASSWORD to "Acak", HesychGenMode.PASSPHRASE to "Passphrase").forEach { (mode, lbl) ->
                    val active = options.mode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) AppHesych.copy(alpha = 0.18f) else ElevatedSurface)
                            .border(1.dp, if (active) AppHesych else BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable { regenerate(options.copy(mode = mode)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(lbl, fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (active) AppHesych else TextMuted)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DominantBg)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(preview, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = TextPrimary)
                PasswordStrengthBar(preview)
            }

            if (options.mode == HesychGenMode.PASSWORD) {
                Column {
                    Text("Panjang: ${options.length} karakter", fontFamily = UiSans, fontSize = 12.sp, color = TextMuted)
                    Slider(
                        value = options.length.toFloat(),
                        onValueChange = { regenerate(options.copy(length = it.toInt())) },
                        valueRange = 8f..32f,
                        colors = SliderDefaults.colors(thumbColor = AppHesych, activeTrackColor = AppHesych)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GenToggleChip("A-Z", options.upper, Modifier.weight(1f)) { regenerate(options.copy(upper = !options.upper)) }
                    GenToggleChip("a-z", options.lower, Modifier.weight(1f)) { regenerate(options.copy(lower = !options.lower)) }
                    GenToggleChip("0-9", options.numbers, Modifier.weight(1f)) { regenerate(options.copy(numbers = !options.numbers)) }
                    GenToggleChip("!@#", options.symbols, Modifier.weight(1f)) { regenerate(options.copy(symbols = !options.symbols)) }
                }
                GenToggleChip("Hindari karakter mirip (0O1lI)", options.excludeAmbiguous, Modifier.fillMaxWidth()) {
                    regenerate(options.copy(excludeAmbiguous = !options.excludeAmbiguous))
                }
            } else {
                Column {
                    Text("Jumlah kata: ${options.words}", fontFamily = UiSans, fontSize = 12.sp, color = TextMuted)
                    Slider(
                        value = options.words.toFloat(),
                        onValueChange = { regenerate(options.copy(words = it.toInt())) },
                        valueRange = 3f..8f,
                        colors = SliderDefaults.colors(thumbColor = AppHesych, activeTrackColor = AppHesych)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GenToggleChip("Kapital", options.capitalize, Modifier.weight(1f)) { regenerate(options.copy(capitalize = !options.capitalize)) }
                    GenToggleChip("+ Angka", options.includeNumber, Modifier.weight(1f)) { regenerate(options.copy(includeNumber = !options.includeNumber)) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { regenerate() }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Acak Ulang", fontFamily = UiSans, fontSize = 12.5.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppHesych)
                        .clickable { onCopy(preview) }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = Color(0xFF0A0F0D), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(actionLabel, fontFamily = UiSans, fontSize = 12.5.sp, color = Color(0xFF0A0F0D), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GenToggleChip(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (active) AppHesych.copy(alpha = 0.16f) else ElevatedSurface)
            .border(1.dp, if (active) AppHesych.copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = UiSans, fontSize = 11.5.sp, color = if (active) AppHesych else TextMuted, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun HesychSecretLockDialog(
    isSecretLocked: Boolean,
    onSelectMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppHesych, modifier = Modifier.size(20.dp))
                Text("Secret Lock", fontFamily = EditorialSerif, fontSize = 19.sp, color = TextPrimary)
            }
            Text(
                text = "Sembunyikan sementara tombol lihat & salin password (digital detox / keamanan publik).",
                fontFamily = UiSans,
                fontSize = 12.sp,
                color = TextMuted
            )
            val presets = listOf(15 to "15 Menit", 60 to "1 Jam", 240 to "4 Jam")
            presets.forEach { (mins, label) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable {
                            onSelectMinutes(mins)
                            onDismiss()
                        }
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    Text("Aktifkan $label", fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                }
            }
            if (isSecretLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SemanticWarning.copy(alpha = 0.16f))
                        .border(1.dp, SemanticWarning.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .clickable {
                            onSelectMinutes(0)
                            onDismiss()
                        }
                        .padding(vertical = 10.dp, horizontal = 12.dp)
                ) {
                    Text("Matikan Secret Lock", fontFamily = UiSans, fontSize = 13.sp, color = SemanticWarning, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
