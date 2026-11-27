package com.ecosystemku.assistant.ui.apps.hesych

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecosystemku.assistant.data.local.HesychCategory
import com.ecosystemku.assistant.data.local.HesychVaultItem
import com.ecosystemku.assistant.ui.components.LocalRemToast
import com.ecosystemku.assistant.ui.theme.AppHesych
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.DominantBg
import com.ecosystemku.assistant.ui.theme.EditorialSerif
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.SemanticDanger
import com.ecosystemku.assistant.ui.theme.SemanticWarning
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.TextSubtle
import com.ecosystemku.assistant.ui.theme.UiSans

@Composable
fun HesychScreen(
    viewModel: HesychViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toast = LocalRemToast.current

    var detailItem by remember { mutableStateOf<HesychVaultItem?>(null) }
    var editingItem by remember { mutableStateOf<Pair<Boolean, HesychVaultItem?>?>(null) }
    var showGenerator by remember { mutableStateOf(false) }
    var showSecretLock by remember { mutableStateOf(false) }
    var showHealthDialog by remember { mutableStateOf(false) }
    var showBackupBioDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) { onClose() }

    fun showBannerToast(msg: String) {
        if (msg.contains("gagal", ignoreCase = true) ||
            msg.contains("salah", ignoreCase = true) ||
            msg.contains("tidak", ignoreCase = true)
        ) {
            toast?.warning(msg)
        } else {
            toast?.success(msg)
        }
    }

    LaunchedEffect(uiState.bannerMessage) {
        val msg = uiState.bannerMessage ?: return@LaunchedEffect
        viewModel.dismissBanner()
        showBannerToast(msg)
    }

    fun copySensitive(label: String, text: String, sensitive: Boolean = true) {
        if (text.isEmpty()) return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text).apply {
            if (sensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
        }
        cm.setPrimaryClip(clip)
        toast?.success("$label disalin")
    }

    val isSecretLocked = viewModel.isSecretLocked()

    detailItem?.let { current ->
        val latest = uiState.items.firstOrNull { it.id == current.id } ?: current
        HesychItemDetailDialog(
            item = latest,
            isSecretLocked = isSecretLocked,
            revealSecret = viewModel::revealSecret,
            generateTotp = viewModel::generateTotpCode,
            loadHistory = viewModel::getPasswordHistory,
            onCopy = { lbl, txt -> copySensitive(lbl, txt, sensitive = lbl != "Username") },
            onEdit = {
                detailItem = null
                editingItem = true to latest
            },
            onDelete = {
                detailItem = null
                viewModel.deleteItem(latest.id)
            },
            onDismiss = { detailItem = null }
        )
    }

    editingItem?.let { (_, itemToEdit) ->
        HesychEditItemDialog(
            initialItem = itemToEdit,
            revealSecret = viewModel::revealSecret,
            onSave = { id, title, username, pw, notes, color, fav, cat, tags, totp, fields ->
                viewModel.saveItem(id, title, username, pw, notes, color, fav, cat, tags, totp, fields)
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }

    if (showGenerator) {
        HesychGeneratorDialog(
            onCopy = { pw ->
                copySensitive("Password", pw, sensitive = true)
                showGenerator = false
            },
            onDismiss = { showGenerator = false }
        )
    }

    if (showSecretLock) {
        HesychSecretLockDialog(
            isSecretLocked = isSecretLocked,
            onSelectMinutes = viewModel::setSecretLockMinutes,
            onDismiss = { showSecretLock = false }
        )
    }

    if (showHealthDialog) {
        HesychHealthDialog(
            items = uiState.items,
            report = uiState.healthReport,
            isCheckingBreach = uiState.isCheckingBreach,
            breachProgressLabel = uiState.breachProgressLabel,
            onScan = viewModel::runHealthScan,
            onRunBreachCheck = viewModel::runBreachCheck,
            onOpenItem = { item ->
                showHealthDialog = false
                editingItem = true to item
            },
            onDismiss = { showHealthDialog = false }
        )
    }

    if (showBackupBioDialog) {
        HesychBackupBioDialog(
            biometricAvailable = uiState.biometricAvailable,
            biometricEnrolled = uiState.biometricEnrolled,
            onEnableBiometric = viewModel::enableBiometric,
            onDisableBiometric = viewModel::disableBiometric,
            onExportBackupJson = viewModel::exportBackupJson,
            onImportBackupJson = viewModel::importBackupJson,
            onShowToast = { msg -> showBannerToast(msg) },
            onDismiss = { showBackupBioDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DominantBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                        .border(1.dp, BorderLight, CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali", tint = TextPrimary, modifier = Modifier.size(19.dp))
                }
                Column {
                    Text("Hesych", fontFamily = EditorialSerif, fontSize = 22.sp, color = TextPrimary)
                    Text(
                        text = if (uiState.isUnlocked) "${uiState.items.size} akun terenkripsi" else "Zero-knowledge Vault",
                        fontFamily = UiSans,
                        fontSize = 11.5.sp,
                        color = AppHesych
                    )
                }
            }

            if (uiState.isUnlocked) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconCircleButton(Icons.Outlined.MonitorHeart, "Kesehatan Vault") { showHealthDialog = true }
                    IconCircleButton(Icons.Outlined.Key, "Generator Password") { showGenerator = true }
                    IconCircleButton(
                        icon = Icons.Outlined.Timer,
                        contentDescription = "Secret Lock",
                        tint = if (isSecretLocked) SemanticWarning else TextPrimary
                    ) { showSecretLock = true }
                    IconCircleButton(Icons.Outlined.Settings, "Keamanan & Backup") { showBackupBioDialog = true }
                    IconCircleButton(Icons.Outlined.Lock, "Kunci Vault") { viewModel.lock() }
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

        if (!uiState.isUnlocked) {
            val hostActivity = context as? FragmentActivity
            HesychLockView(
                isInitialized = uiState.isInitialized,
                isBusy = uiState.isBusy,
                lockError = uiState.lockError,
                showBiometricButton = uiState.isInitialized && uiState.biometricAvailable && uiState.biometricEnrolled && hostActivity != null,
                onSetup = viewModel::setupMasterPassword,
                onUnlock = viewModel::unlock,
                onBiometricUnlock = { hostActivity?.let(viewModel::unlockWithBiometric) }
            )
        } else {
            val filtered = remember(
                uiState.items,
                uiState.selectedCategory,
                uiState.favoritesOnly,
                uiState.searchQuery,
                uiState.selectedTag
            ) {
                val q = uiState.searchQuery.trim().lowercase()
                uiState.items.filter { item ->
                    val catOk = uiState.selectedCategory == null || item.category == uiState.selectedCategory
                    val favOk = !uiState.favoritesOnly || item.favorite
                    val tagOk = uiState.selectedTag == null || item.tags.any { it.equals(uiState.selectedTag, ignoreCase = true) }
                    val qOk = q.isEmpty() ||
                        item.title.lowercase().contains(q) ||
                        item.username.lowercase().contains(q) ||
                        item.tags.any { it.lowercase().contains(q) }
                    catOk && favOk && tagOk && qOk
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search + Filter Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(SecondarySurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(18.dp))
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = viewModel::setSearchQuery,
                                singleLine = true,
                                textStyle = TextStyle(fontFamily = UiSans, fontSize = 13.5.sp, color = TextPrimary),
                                cursorBrush = SolidColor(AppHesych),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text("Cari akun, username, atau tag…", fontFamily = UiSans, fontSize = 13.sp, color = TextSubtle)
                                    }
                                    inner()
                                }
                            )
                            if (uiState.searchQuery.isNotEmpty()) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Hapus cari",
                                    tint = TextMuted,
                                    modifier = Modifier.size(17.dp).clickable { viewModel.setSearchQuery("") }
                                )
                            }
                            Icon(
                                imageVector = if (uiState.favoritesOnly) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Filter Favorit",
                                tint = if (uiState.favoritesOnly) SemanticWarning else TextMuted,
                                modifier = Modifier.size(19.dp).clickable { viewModel.toggleFavoritesOnly() }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterPill("Semua", uiState.selectedCategory == null) { viewModel.setCategoryFilter(null) }
                            HesychCategory.entries.forEach { cat ->
                                FilterPill(cat.label, uiState.selectedCategory == cat) {
                                    viewModel.setCategoryFilter(if (uiState.selectedCategory == cat) null else cat)
                                }
                            }
                        }

                        if (uiState.selectedTag != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AppHesych.copy(alpha = 0.14f))
                                    .clickable { viewModel.setTagFilter(null) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Tag: #${uiState.selectedTag}", fontFamily = UiSans, fontSize = 11.sp, color = AppHesych, fontWeight = FontWeight.Medium)
                                Icon(Icons.Outlined.Close, contentDescription = null, tint = AppHesych, modifier = Modifier.size(13.dp))
                            }
                        }

                        if (isSecretLocked) {
                            val remainMins = ((uiState.secretLockedUntilMs - System.currentTimeMillis()) / 60_000L).coerceAtLeast(1L)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SemanticWarning.copy(alpha = 0.12f))
                                    .border(1.dp, SemanticWarning.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .clickable { showSecretLock = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Secret Lock aktif (~$remainMins mnt)", fontFamily = UiSans, fontSize = 12.sp, color = SemanticWarning, fontWeight = FontWeight.Medium)
                                Text("Atur", fontFamily = UiSans, fontSize = 11.5.sp, color = SemanticWarning, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Shield, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(36.dp))
                                Text(
                                    text = when {
                                        uiState.items.isEmpty() && uiState.storedItemCount > 0 ->
                                            "${uiState.storedItemCount} akun gagal didekripsi"
                                        uiState.items.isEmpty() -> "Vault masih kosong"
                                        else -> "Tidak ada akun yang cocok"
                                    },
                                    fontFamily = UiSans,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = when {
                                        uiState.items.isEmpty() && uiState.storedItemCount > 0 ->
                                            "Akun tersimpan di database, tetapi tidak cocok dengan kunci vault yang aktif."
                                        uiState.items.isEmpty() ->
                                            "Ketuk tombol + untuk menambah akun terenkripsi pertama."
                                        else ->
                                            "Coba ubah pencarian, kategori, atau filter favorit."
                                    },
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filtered, key = { it.id }) { item ->
                                HesychItemCard(
                                    item = item,
                                    isSecretLocked = isSecretLocked,
                                    onClick = { detailItem = item },
                                    onToggleFavorite = { viewModel.toggleFavorite(item) },
                                    onCopyPassword = {
                                        val plain = viewModel.revealSecret(item.password)
                                        copySensitive("Password", plain, sensitive = true)
                                    },
                                    onTagClick = { tag -> viewModel.setTagFilter(tag) }
                                )
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { editingItem = false to null },
                    containerColor = AppHesych,
                    contentColor = Color(0xFF0A0F0D),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Tambah Akun")
                }
            }
        }
    }
}

@Composable
private fun HesychLockView(
    isInitialized: Boolean,
    isBusy: Boolean,
    lockError: String?,
    showBiometricButton: Boolean,
    onSetup: (String, String) -> Unit,
    onUnlock: (String) -> Unit,
    onBiometricUnlock: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var reveal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AppHesych.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = AppHesych, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = if (isInitialized) "Buka Vault Hesych" else "Buat Master Password",
                        fontFamily = EditorialSerif,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Argon2id + AES-256-GCM • Zero-knowledge",
                        fontFamily = UiSans,
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DominantBg)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = TextPrimary),
                    cursorBrush = SolidColor(AppHesych),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (password.isEmpty()) Text("Master password…", fontFamily = UiSans, fontSize = 13.sp, color = TextSubtle)
                        inner()
                    }
                )
                Icon(
                    imageVector = if (reveal) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp).clickable { reveal = !reveal }
                )
            }

            if (!isInitialized) {
                PasswordStrengthBar(password)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DominantBg)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        singleLine = true,
                        visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = TextPrimary),
                        cursorBrush = SolidColor(AppHesych),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (confirm.isEmpty()) Text("Ulangi master password…", fontFamily = UiSans, fontSize = 13.sp, color = TextSubtle)
                            inner()
                        }
                    )
                }
            }

            if (!lockError.isNullOrBlank()) {
                Text(lockError, fontFamily = UiSans, fontSize = 12.sp, color = SemanticDanger)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppHesych)
                    .clickable(enabled = !isBusy) {
                        if (isInitialized) onUnlock(password) else onSetup(password, confirm)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF0A0F0D))
                } else {
                    Text(
                        text = if (isInitialized) "Buka Vault" else "Buat & Enkripsi Vault",
                        fontFamily = UiSans,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0A0F0D)
                    )
                }
            }

            if (showBiometricButton) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                        .clickable(enabled = !isBusy, onClick = onBiometricUnlock)
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = AppHesych, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka dengan Sidik Jari", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun HesychItemCard(
    item: HesychVaultItem,
    isSecretLocked: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopyPassword: () -> Unit,
    onTagClick: (String) -> Unit
) {
    val accent = HESYCH_ITEM_COLORS[item.color.coerceIn(0, HESYCH_ITEM_COLORS.lastIndex)]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SecondarySurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(accent))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontFamily = UiSans,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.username.isNotBlank()) {
                        Text(
                            text = item.username,
                            fontFamily = UiSans,
                            fontSize = 12.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (item.totpSecret.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppHesych.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("2FA", fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppHesych)
                    }
                }
                Icon(
                    imageVector = if (item.favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorit",
                    tint = if (item.favorite) SemanticWarning else TextSubtle,
                    modifier = Modifier.size(18.dp).clickable(onClick = onToggleFavorite)
                )
                if (!isSecretLocked) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Salin Password",
                        tint = AppHesych,
                        modifier = Modifier.size(18.dp).clickable(onClick = onCopyPassword)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ElevatedSurface)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(item.category.label, fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted)
            }
            item.tags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(ElevatedSurface)
                        .clickable { onTagClick(tag) }
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text("#$tag", fontFamily = UiSans, fontSize = 10.5.sp, color = TextSubtle)
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) AppHesych.copy(alpha = 0.18f) else ElevatedSurface)
            .border(1.dp, if (selected) AppHesych.copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontFamily = UiSans,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) AppHesych else TextMuted
        )
    }
}

@Composable
private fun IconCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(ElevatedSurface)
            .border(1.dp, BorderLight, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}
