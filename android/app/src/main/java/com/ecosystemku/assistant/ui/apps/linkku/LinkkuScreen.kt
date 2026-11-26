package com.ecosystemku.assistant.ui.apps.linkku

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import com.ecosystemku.assistant.ui.components.RemFloatingToastPill
import com.ecosystemku.assistant.ui.components.RemToastData
import com.ecosystemku.assistant.ui.components.RemToastVariant
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ecosystemku.assistant.data.local.LinkkuCategoryEntity
import com.ecosystemku.assistant.data.local.LinkkuLinkEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LkBg = Color(0xFF0C0C0E)
private val LkBg2 = Color(0xFF111114)
private val LkBg3 = Color(0xFF18181C)
private val LkBg4 = Color(0xFF1E1E24)
private val LkBorder = Color(0xFF26262E)
private val LkBorder2 = Color(0xFF32323C)
private val LkText = Color(0xFFF0F0F2)
private val LkText2 = Color(0xFF8A8A9A)
private val LkText3 = Color(0xFF5A5A6C)
private val LkGreen = Color(0xFF4ADE80)
private val LkRed = Color(0xFFF87171)

private val LkPrBg = Color(0xFF0D0A13)
private val LkPrBorder = Color(0xFF2A1A38)
private val LkPrAccent = Color(0xFFC084FC)
private val LkPrDim = Color(0x1AC084FC)

private sealed class PinDialogRequest {
    data class EnterPrivateMode(val onSuccess: () -> Unit) : PinDialogRequest()
    data class VerifyOldPinForChange(val onVerified: () -> Unit) : PinDialogRequest()
    data class DecryptImport(val rawJson: String) : PinDialogRequest()
}

private data class ConfirmDialogData(
    val title: String,
    val message: String,
    val confirmLabel: String = "Hapus",
    val isDanger: Boolean = true,
    val onConfirm: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkkuScreen(
    uiState: LinkkuUiState,
    actions: LinkkuActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var privateMode by remember { mutableStateOf(false) }
    var currentCategory by remember { mutableStateOf("all") }
    var filterMode by remember { mutableStateOf("all") } // "all" | "recent"
    var searchQuery by remember { mutableStateOf("") }

    var showLinkDialog by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<LinkkuLinkEntity?>(null) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCatMgr by remember { mutableStateOf(false) }
    var showPrivCatMgr by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var pinRequest by remember { mutableStateOf<PinDialogRequest?>(null) }
    var bottomSheetLink by remember { mutableStateOf<LinkkuLinkEntity?>(null) }
    var confirmDialog by remember { mutableStateOf<ConfirmDialogData?>(null) }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastIsError by remember { mutableStateOf(false) }

    fun showToast(msg: String, isError: Boolean = false) {
        toastMessage = msg
        toastIsError = isError
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2600L)
            toastMessage = null
        }
    }

    BackHandler {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            bottomSheetLink != null -> bottomSheetLink = null
            showLinkDialog -> showLinkDialog = false
            showSettingsDialog -> showSettingsDialog = false
            showCatMgr -> showCatMgr = false
            showPrivCatMgr -> showPrivCatMgr = false
            showSetPinDialog -> showSetPinDialog = false
            pinRequest != null -> pinRequest = null
            else -> onClose()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val payload = actions.onExportJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: error("Tidak bisa menulis file")
                }.onSuccess {
                    showToast("Export berhasil")
                }.onFailure {
                    showToast("Gagal export: ${it.message}", true)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Tidak bisa membaca file")
                }.getOrElse {
                    showToast("Gagal membaca file", true)
                    return@launch
                }
                when (val res = actions.onImportJson(raw, null)) {
                    is LinkkuImportResult.Success -> showToast("${res.importedCount} link diimport")
                    is LinkkuImportResult.NeedsPin -> {
                        showSettingsDialog = false
                        pinRequest = PinDialogRequest.DecryptImport(res.rawJson)
                    }
                    is LinkkuImportResult.Error -> showToast(res.message, true)
                }
            }
        }
    }

    val activeBg = if (privateMode) LkPrBg else LkBg
    val activeBorder = if (privateMode) LkPrBorder else LkBorder
    val normalCats = remember(uiState.categories) { uiState.categories.filter { !it.isPrivate } }
    val privCats = remember(uiState.categories) { uiState.categories.filter { it.isPrivate } }

    val pubLinks = remember(uiState.links) { uiState.links.filter { !it.isPrivate } }
    val privLinks = remember(uiState.links) { uiState.links.filter { it.isPrivate } }

    val filteredLinks = remember(uiState.links, privateMode, currentCategory, searchQuery, filterMode) {
        var list = if (privateMode) privLinks else pubLinks
        if (currentCategory != "all" && currentCategory != "private-all") {
            list = if (privateMode) {
                list.filter { it.pcat == currentCategory }
            } else {
                list.filter { it.cat == currentCategory }
            }
        }
        if (searchQuery.isNotBlank()) {
            val lq = searchQuery.trim().lowercase()
            list = list.filter { item ->
                item.title.lowercase().contains(lq) ||
                    item.url.lowercase().contains(lq) ||
                    item.note.lowercase().contains(lq) ||
                    LinkkuHelpers.parseTags(item.tagsJson).any { it.lowercase().contains(lq) }
            }
        }
        if (filterMode == "recent") {
            list = list.sortedByDescending { it.ts }.take(20)
        }
        list
    }

    val pageTitle = remember(privateMode, currentCategory, normalCats, privCats) {
        when {
            privateMode && currentCategory == "private-all" -> "Semua Privat"
            privateMode -> privCats.firstOrNull { it.id == currentCategory }?.name
                ?: normalCats.firstOrNull { it.id == currentCategory }?.name
                ?: "Kategori"
            currentCategory == "all" -> "Semua Link"
            else -> normalCats.firstOrNull { it.id == currentCategory }?.name ?: "Kategori"
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = activeBg,
                drawerContentColor = LkText,
                modifier = Modifier.width(248.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    if (!privateMode) {
                        SidebarLabel("SEMUA")
                        SidebarItem(
                            label = "Semua Link",
                            count = pubLinks.size,
                            active = currentCategory == "all",
                            isPrivate = false,
                            icon = { Icon(Icons.Outlined.Link, null, modifier = Modifier.size(14.dp)) },
                            onClick = {
                                currentCategory = "all"
                                scope.launch { drawerState.close() }
                            }
                        )
                        HorizontalDivider(color = activeBorder, modifier = Modifier.padding(vertical = 8.dp))
                        SidebarLabel("KATEGORI")
                        normalCats.forEach { cat ->
                            val cnt = pubLinks.count { it.cat == cat.id }
                            SidebarItem(
                                label = cat.name,
                                count = cnt,
                                active = currentCategory == cat.id,
                                isPrivate = false,
                                icon = { Icon(Icons.Outlined.Folder, null, modifier = Modifier.size(14.dp)) },
                                onClick = {
                                    currentCategory = cat.id
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                        HorizontalDivider(color = activeBorder, modifier = Modifier.padding(vertical = 8.dp))
                        SidebarItem(
                            label = "Kelola Kategori",
                            count = null,
                            active = false,
                            isPrivate = false,
                            muted = true,
                            icon = { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(14.dp)) },
                            onClick = {
                                scope.launch { drawerState.close() }
                                showCatMgr = true
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.Lock, null, tint = LkPrAccent, modifier = Modifier.size(13.dp))
                            Text(
                                text = "Mode Privat",
                                color = LkPrAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        SidebarItem(
                            label = "Semua Privat",
                            count = privLinks.size,
                            active = currentCategory == "private-all",
                            isPrivate = true,
                            icon = { Icon(Icons.Outlined.Link, null, modifier = Modifier.size(14.dp)) },
                            onClick = {
                                currentCategory = "private-all"
                                scope.launch { drawerState.close() }
                            }
                        )
                        HorizontalDivider(color = activeBorder, modifier = Modifier.padding(vertical = 8.dp))
                        SidebarLabel("KATEGORI")
                        val populatedPrivCats = privCats.filter { c -> privLinks.any { it.pcat == c.id } }
                        if (populatedPrivCats.isEmpty()) {
                            Text(
                                text = "Belum ada",
                                fontSize = 11.sp,
                                color = LkText3,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        } else {
                            populatedPrivCats.forEach { cat ->
                                val cnt = privLinks.count { it.pcat == cat.id }
                                SidebarItem(
                                    label = cat.name,
                                    count = cnt,
                                    active = currentCategory == cat.id,
                                    isPrivate = true,
                                    icon = { Icon(Icons.Outlined.Folder, null, modifier = Modifier.size(14.dp)) },
                                    onClick = {
                                        currentCategory = cat.id
                                        scope.launch { drawerState.close() }
                                    }
                                )
                            }
                        }
                        HorizontalDivider(color = activeBorder, modifier = Modifier.padding(vertical = 8.dp))
                        SidebarItem(
                            label = "Kelola Kategori Privat",
                            count = null,
                            active = false,
                            isPrivate = true,
                            muted = true,
                            icon = { Icon(Icons.Outlined.Add, null, modifier = Modifier.size(14.dp)) },
                            onClick = {
                                scope.launch { drawerState.close() }
                                showPrivCatMgr = true
                            }
                        )
                        SidebarItem(
                            label = "Keluar Mode Privat",
                            count = null,
                            active = false,
                            isPrivate = true,
                            muted = true,
                            icon = { Icon(Icons.AutoMirrored.Outlined.ExitToApp, null, modifier = Modifier.size(14.dp)) },
                            onClick = {
                                privateMode = false
                                currentCategory = "all"
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(activeBg)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Topbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(activeBg.copy(alpha = 0.94f))
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, activeBorder, RoundedCornerShape(8.dp))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali ke Apps",
                            tint = LkText2,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Logo Linkku + pm dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(22.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .border(1.8.dp, LkText, RoundedCornerShape(5.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = LkText,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            if (privateMode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(LkPrAccent)
                                        .border(1.5.dp, activeBg, CircleShape)
                                )
                            }
                        }
                        Text(
                            text = "Linkku",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LkText,
                            letterSpacing = (-0.4).sp
                        )
                    }

                    // Search bar
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(LkBg3)
                            .border(1.dp, activeBorder, RoundedCornerShape(7.dp))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Search, null, tint = LkText3, modifier = Modifier.size(14.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text("Cari link...", fontSize = 12.sp, color = LkText3, maxLines = 1)
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 12.sp, color = LkText),
                                cursorBrush = SolidColor(if (privateMode) LkPrAccent else LkText),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Hapus cari",
                                tint = LkText3,
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }

                    // Drawer button
                    TopIconButton(
                        border = activeBorder,
                        onClick = { scope.launch { drawerState.open() } }
                    ) {
                        Icon(Icons.Outlined.Menu, "Kategori", tint = LkText2, modifier = Modifier.size(16.dp))
                    }

                    // Private mode toggle
                    TopIconButton(
                        border = if (privateMode) LkPrBorder else activeBorder,
                        onClick = {
                            if (privateMode) {
                                privateMode = false
                                currentCategory = "all"
                            } else if (!uiState.hasPin) {
                                showToast("Atur PIN dulu di Pengaturan", true)
                            } else {
                                pinRequest = PinDialogRequest.EnterPrivateMode {
                                    privateMode = true
                                    currentCategory = "private-all"
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = "Mode Privat",
                            tint = if (privateMode) LkPrAccent else LkText2,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Settings button
                    TopIconButton(
                        border = activeBorder,
                        onClick = { showSettingsDialog = true }
                    ) {
                        Icon(Icons.Outlined.Settings, "Pengaturan", tint = LkText2, modifier = Modifier.size(15.dp))
                    }
                }
                HorizontalDivider(color = activeBorder)

                // Content header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = pageTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LkText
                        )
                        Text(
                            text = "${filteredLinks.size} link",
                            fontSize = 11.sp,
                            color = LkText3
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChipPill("Semua", filterMode == "all") { filterMode = "all" }
                        FilterChipPill("Terbaru", filterMode == "recent") { filterMode = "recent" }
                    }
                }

                // Links List / Empty State
                if (filteredLinks.isEmpty()) {
                    LinkkuEmptyState(
                        isSearch = searchQuery.isNotBlank(),
                        isCategory = currentCategory != "all" && currentCategory != "private-all",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 92.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredLinks, key = { it.id }) { item ->
                            val catName = if (item.isPrivate) {
                                privCats.firstOrNull { it.id == item.pcat }?.name
                            } else {
                                normalCats.firstOrNull { it.id == item.cat }?.name
                            }
                            LinkkuCard(
                                link = item,
                                categoryName = catName,
                                actions = actions,
                                onOpenUrl = { openUrlInBrowser(context, item.url) { showToast(it, true) } },
                                onLongPress = { bottomSheetLink = item }
                            )
                        }
                    }
                }
            }

            // FAB +
            FloatingActionButton(
                onClick = {
                    editingLink = null
                    showLinkDialog = true
                },
                containerColor = if (privateMode) LkPrAccent else LkText,
                contentColor = LkBg,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 22.dp)
                    .size(54.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Tambah Link", modifier = Modifier.size(24.dp))
            }

            // Floating Rem Toast Pill
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 88.dp)
            ) {
                RemFloatingToastPill(
                    toast = RemToastData(
                        id = 1L,
                        message = toastMessage.orEmpty(),
                        variant = if (toastIsError) RemToastVariant.WARNING else RemToastVariant.SUCCESS
                    ),
                    onDismiss = { toastMessage = null }
                )
            }
        }
    }

    // Bottom Sheet (Long-press actions)
    val sheetTarget = bottomSheetLink
    if (sheetTarget != null) {
        val domain = LinkkuHelpers.extractDomain(sheetTarget.url)
        ModalBottomSheet(
            onDismissRequest = { bottomSheetLink = null },
            containerColor = LkBg2,
            contentColor = LkText
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(LkBg3),
                        contentAlignment = Alignment.Center
                    ) {
                        RemoteThumbBox(
                            thumbUrl = sheetTarget.thumb,
                            faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=32",
                            fallbackLetter = (sheetTarget.title.ifBlank { domain }).take(1).uppercase(),
                            domain = domain,
                            onLoadBitmap = actions.onLoadBitmap
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sheetTarget.title.ifBlank { domain },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = LkText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = domain,
                            fontSize = 11.sp,
                            color = LkText3,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider(color = LkBorder)
                BottomSheetActionRow(Icons.Outlined.OpenInBrowser, "Buka Link", LkText) {
                    val url = sheetTarget.url
                    bottomSheetLink = null
                    openUrlInBrowser(context, url) { showToast(it, true) }
                }
                HorizontalDivider(color = LkBorder)
                BottomSheetActionRow(Icons.Outlined.ContentCopy, "Salin URL", LkText) {
                    copyToClipboard(context, sheetTarget.url)
                    bottomSheetLink = null
                    showToast("URL disalin")
                }
                HorizontalDivider(color = LkBorder)
                BottomSheetActionRow(Icons.Outlined.Edit, "Edit", LkText) {
                    editingLink = sheetTarget
                    bottomSheetLink = null
                    showLinkDialog = true
                }
                HorizontalDivider(color = LkBorder)
                BottomSheetActionRow(Icons.Outlined.DeleteOutline, "Hapus", LkRed) {
                    val idToDelete = sheetTarget.id
                    bottomSheetLink = null
                    confirmDialog = ConfirmDialogData(
                        title = "Hapus Link?",
                        message = "Link akan dihapus permanen.",
                        onConfirm = {
                            actions.onDeleteLink(idToDelete)
                            showToast("Dihapus", true)
                        }
                    )
                }
            }
        }
    }

    // Add/Edit Link Dialog
    if (showLinkDialog) {
        AddEditLinkDialog(
            editing = editingLink,
            defaultPrivate = privateMode,
            uiState = uiState,
            actions = actions,
            onRequirePin = { showToast("Atur PIN dulu di Pengaturan", true) },
            onDeleteTagRequest = { tag, isPriv, onDone ->
                confirmDialog = ConfirmDialogData(
                    title = "Hapus tag \"$tag\"?",
                    message = "Tag akan dihapus dari semua link di mode ini.",
                    onConfirm = {
                        actions.onDeleteTagGlobally(tag, isPriv)
                        onDone()
                        showToast("Tag \"$tag\" dihapus", true)
                    }
                )
            },
            onDismiss = {
                showLinkDialog = false
                editingLink = null
            },
            onSaved = { isEdit ->
                showLinkDialog = false
                editingLink = null
                showToast(if (isEdit) "Diperbarui" else "Disimpan")
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        LinkkuSettingsDialog(
            hasPin = uiState.hasPin,
            onDismiss = { showSettingsDialog = false },
            onOpenSetPin = {
                showSettingsDialog = false
                if (uiState.hasPin) {
                    pinRequest = PinDialogRequest.VerifyOldPinForChange {
                        showSetPinDialog = true
                    }
                } else {
                    showSetPinDialog = true
                }
            },
            onExport = {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                exportLauncher.launch("linkku-$dateStr.json")
            },
            onImport = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onReset = {
                confirmDialog = ConfirmDialogData(
                    title = "Reset Semua Data?",
                    message = "Semua link, kategori, dan PIN Linkku dihapus permanen.",
                    onConfirm = {
                        actions.onResetLinkku()
                        privateMode = false
                        currentCategory = "all"
                        showSettingsDialog = false
                        showToast("Data direset", true)
                    }
                )
            }
        )
    }

    // Normal Category Manager
    if (showCatMgr) {
        CategoryManagerDialog(
            title = "Kelola Kategori",
            placeholder = "Nama kategori...",
            categories = normalCats,
            onAdd = { name ->
                scope.launch {
                    actions.onAddCategory(name, false)
                    showToast("Kategori ditambahkan")
                }
            },
            onDelete = { cat ->
                confirmDialog = ConfirmDialogData(
                    title = "Hapus Kategori?",
                    message = "Link di kategori \"${cat.name}\" tidak terhapus.",
                    onConfirm = {
                        actions.onDeleteCategory(cat.id, false)
                        if (currentCategory == cat.id) currentCategory = "all"
                        showToast("Kategori dihapus", true)
                    }
                )
            },
            onDismiss = { showCatMgr = false }
        )
    }

    // Private Category Manager
    if (showPrivCatMgr) {
        CategoryManagerDialog(
            title = "Kelola Kategori Privat",
            placeholder = "Nama kategori privat...",
            categories = privCats,
            onAdd = { name ->
                scope.launch {
                    actions.onAddCategory(name, true)
                    showToast("Kategori privat ditambahkan")
                }
            },
            onDelete = { cat ->
                confirmDialog = ConfirmDialogData(
                    title = "Hapus Kategori Privat?",
                    message = "Link di kategori \"${cat.name}\" tidak terhapus.",
                    onConfirm = {
                        actions.onDeleteCategory(cat.id, true)
                        if (currentCategory == cat.id) currentCategory = "private-all"
                        showToast("Kategori privat dihapus", true)
                    }
                )
            },
            onDismiss = { showPrivCatMgr = false }
        )
    }

    // PIN Keypad Dialog
    val activePinReq = pinRequest
    if (activePinReq != null) {
        val title = when (activePinReq) {
            is PinDialogRequest.EnterPrivateMode -> "Masukkan PIN"
            is PinDialogRequest.VerifyOldPinForChange -> "Verifikasi PIN Lama"
            is PinDialogRequest.DecryptImport -> "Masukkan PIN untuk Import"
        }
        PinEntryDialog(
            title = title,
            rawMode = activePinReq is PinDialogRequest.DecryptImport,
            onVerify = actions.onVerifyPin,
            onSuccess = { rawPin ->
                pinRequest = null
                when (activePinReq) {
                    is PinDialogRequest.EnterPrivateMode -> activePinReq.onSuccess()
                    is PinDialogRequest.VerifyOldPinForChange -> activePinReq.onVerified()
                    is PinDialogRequest.DecryptImport -> {
                        scope.launch {
                            when (val res = actions.onImportJson(activePinReq.rawJson, rawPin)) {
                                is LinkkuImportResult.Success -> showToast("${res.importedCount} link diimport")
                                is LinkkuImportResult.Error -> showToast(res.message, true)
                                else -> Unit
                            }
                        }
                    }
                }
            },
            onDismiss = { pinRequest = null }
        )
    }

    // Set New PIN Dialog
    if (showSetPinDialog) {
        SetPinDialog(
            onPinConfirmed = { newPin ->
                actions.onSetPin(newPin)
                showSetPinDialog = false
                showToast("PIN diatur")
            },
            onDismiss = { showSetPinDialog = false }
        )
    }

    // Confirm Dialog
    val cfmData = confirmDialog
    if (cfmData != null) {
        Dialog(onDismissRequest = { confirmDialog = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LkBg2)
                    .border(1.dp, LkBorder2, RoundedCornerShape(14.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(cfmData.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = LkText)
                Text(cfmData.message, fontSize = 12.sp, color = LkText2, lineHeight = 18.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { confirmDialog = null }) {
                        Text("Batal", color = LkText2, fontSize = 12.5.sp)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (cfmData.isDanger) LkRed.copy(alpha = 0.14f) else LkText)
                            .border(
                                1.dp,
                                if (cfmData.isDanger) LkRed.copy(alpha = 0.4f) else LkText,
                                RoundedCornerShape(7.dp)
                            )
                            .clickable {
                                cfmData.onConfirm()
                                confirmDialog = null
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cfmData.confirmLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cfmData.isDanger) LkRed else LkBg
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopIconButton(
    border: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(7.dp))
            .border(1.dp, border, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SidebarLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.1.sp,
        color = LkText3,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

@Composable
private fun SidebarItem(
    label: String,
    count: Int?,
    active: Boolean,
    isPrivate: Boolean,
    muted: Boolean = false,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val activeAccent = if (isPrivate) LkPrAccent else LkText
    val activeBg = if (isPrivate) LkPrDim else Color(0x12F0F0F2)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (active) activeBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(16.dp)
                    .background(if (active) activeAccent else Color.Transparent)
            )
            icon()
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                color = when {
                    active -> activeAccent
                    muted -> LkText3
                    else -> LkText2
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (count != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(LkBg4)
                    .border(1.dp, LkBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text("$count", fontSize = 10.sp, color = LkText3)
            }
        }
    }
}

@Composable
private fun FilterChipPill(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) LkBg4 else Color.Transparent)
            .border(1.dp, if (active) LkBorder2 else LkBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = if (active) LkText else LkText3,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun LinkkuCard(
    link: LinkkuLinkEntity,
    categoryName: String?,
    actions: LinkkuActions,
    onOpenUrl: () -> Unit,
    onLongPress: () -> Unit
) {
    val domain = remember(link.url) { LinkkuHelpers.extractDomain(link.url) }
    val tags = remember(link.tagsJson) { LinkkuHelpers.parseTags(link.tagsJson) }
    var noteExpanded by remember(link.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LkBg2)
            .border(1.dp, LkBorder, RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onOpenUrl,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Left square 88dp thumbnail
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(LkBg3),
                contentAlignment = Alignment.Center
            ) {
                RemoteThumbBox(
                    thumbUrl = link.thumb,
                    faviconUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=32",
                    fallbackLetter = (link.title.ifBlank { domain }).take(1).uppercase(),
                    domain = domain,
                    onLoadBitmap = actions.onLoadBitmap
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = domain,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LkText3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!categoryName.isNullOrBlank()) {
                        Text(
                            text = "· $categoryName",
                            fontSize = 10.5.sp,
                            color = LkText3,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (link.title.isNotBlank()) {
                    Text(
                        text = link.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LkText,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = domain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = LkText3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (link.note.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { noteExpanded = !noteExpanded }
                    ) {
                        Text(
                            text = link.note,
                            fontSize = 10.5.sp,
                            color = LkText2,
                            lineHeight = 15.sp,
                            maxLines = if (noteExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = if (noteExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = LkText3,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        tags.forEach { t ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(LkBg4)
                                    .border(1.dp, LkBorder, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(t, fontSize = 9.sp, color = LkText3)
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = LkBorder)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LinkkuHelpers.formatRelativeTime(link.ts),
                fontSize = 10.sp,
                color = LkText3
            )
            Text(
                text = "Tahan untuk opsi",
                fontSize = 9.5.sp,
                color = LkText3.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun RemoteThumbBox(
    thumbUrl: String,
    faviconUrl: String,
    fallbackLetter: String,
    domain: String,
    onLoadBitmap: suspend (String) -> Bitmap?
) {
    var bitmap by remember(thumbUrl, faviconUrl) { mutableStateOf<Bitmap?>(null) }
    var isRealThumb by remember(thumbUrl) { mutableStateOf(false) }

    LaunchedEffect(thumbUrl, faviconUrl) {
        bitmap = null
        isRealThumb = false
        if (thumbUrl.isNotBlank()) {
            val b = onLoadBitmap(thumbUrl)
            if (b != null) {
                bitmap = b
                isRealThumb = true
                return@LaunchedEffect
            }
        }
        val fav = onLoadBitmap(faviconUrl)
        if (fav != null) {
            bitmap = fav
            isRealThumb = false
        }
    }

    val hue = remember(domain) { LinkkuHelpers.domainHue(domain) }
    val fallbackBg = remember(hue) { Color.hsl(hue, 0.16f, 0.13f) }

    val bmp = bitmap
    if (bmp != null && isRealThumb) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(fallbackBg),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, LkBorder2, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fallbackLetter,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = LkText2
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkkuEmptyState(
    isSearch: Boolean,
    isCategory: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(LkBg3)
                .border(1.dp, LkBorder, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isSearch -> Icons.Outlined.Search
                    isCategory -> Icons.Outlined.Folder
                    else -> Icons.Outlined.Link
                },
                contentDescription = null,
                tint = LkText3,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when {
                isSearch -> "Tidak ditemukan"
                isCategory -> "Kategori kosong"
                else -> "Belum ada link"
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = LkText
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                isSearch -> "Coba kata kunci lain"
                isCategory -> "Belum ada link di kategori ini"
                else -> "Simpan link favoritmu di sini, akses kapan saja."
            },
            fontSize = 11.5.sp,
            color = LkText3
        )
        if (!isSearch) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = if (isCategory) "Tap tombol + untuk menambah" else "Tap tombol + untuk mulai",
                fontSize = 10.5.sp,
                color = LkText3
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEditLinkDialog(
    editing: LinkkuLinkEntity?,
    defaultPrivate: Boolean,
    uiState: LinkkuUiState,
    actions: LinkkuActions,
    onRequirePin: () -> Unit,
    onDeleteTagRequest: (tag: String, isPriv: Boolean, onDone: () -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSaved: (isEdit: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var urlInput by remember(editing) { mutableStateOf(editing?.url.orEmpty()) }
    var titleInput by remember(editing) { mutableStateOf(editing?.title.orEmpty()) }
    var noteInput by remember(editing) { mutableStateOf(editing?.note.orEmpty()) }
    var isPrivate by remember(editing, defaultPrivate) {
        mutableStateOf(editing?.isPrivate ?: defaultPrivate)
    }
    var selectedCatId by remember(editing) {
        mutableStateOf(
            if (editing != null) {
                if (editing.isPrivate) editing.pcat else editing.cat
            } else ""
        )
    }
    var selectedTags by remember(editing) {
        mutableStateOf(editing?.let { LinkkuHelpers.parseTags(it.tagsJson) } ?: emptyList())
    }
    var newTagInput by remember { mutableStateOf("") }
    var fetchState by remember { mutableStateOf("") }
    var catMenuExpanded by remember { mutableStateOf(false) }

    val availableCats = remember(uiState.categories, isPrivate) {
        uiState.categories.filter { it.isPrivate == isPrivate }
    }
    val allModeTags = remember(uiState.links, uiState.extraTagsNormal, uiState.extraTagsPriv, isPrivate, selectedTags) {
        val fromLinks = uiState.links
            .filter { it.isPrivate == isPrivate }
            .flatMap { LinkkuHelpers.parseTags(it.tagsJson) }
        val extra = if (isPrivate) uiState.extraTagsPriv else uiState.extraTagsNormal
        (fromLinks + extra + selectedTags).distinct().sorted()
    }

    // Auto-fetch title on URL input
    LaunchedEffect(urlInput) {
        val trimmed = urlInput.trim()
        if (trimmed.length < 8 || editing != null) return@LaunchedEffect
        fetchState = "Fetch"
        delay(850L)
        val meta = actions.onFetchMeta(trimmed)
        if (meta.title.isNotBlank() && titleInput.isBlank()) {
            titleInput = meta.title
        }
        fetchState = if (meta.title.isNotBlank() || meta.thumb.isNotBlank()) "OK" else "-"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .imePadding()
                .clip(RoundedCornerShape(14.dp))
                .background(LkBg2)
                .border(1.dp, LkBorder2, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editing != null) "Edit Link" else "Tambah Link",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LkText
                )
                TopIconButton(LkBorder, onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, "Tutup", tint = LkText2, modifier = Modifier.size(14.dp))
                }
            }

            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DialogFieldLabel("URL *")
                DialogTextInput(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = "https://..."
                )

                if (urlInput.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(LkBg3)
                            .border(1.dp, LkBorder, RoundedCornerShape(7.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = urlInput.trim(),
                            fontSize = 10.5.sp,
                            color = LkText3,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (fetchState.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(fetchState, fontSize = 10.5.sp, color = LkText2)
                        }
                    }
                }

                DialogFieldLabel("JUDUL")
                DialogTextInput(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    placeholder = "Auto-fetch dari URL..."
                )

                DialogFieldLabel("CATATAN")
                DialogTextInput(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = "Catatan opsional...",
                    singleLine = false,
                    minHeightDp = 64
                )

                DialogFieldLabel("KATEGORI")
                Box {
                    val selectedCatName = availableCats.firstOrNull { it.id == selectedCatId }?.name
                        ?: "— Tanpa Kategori —"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(7.dp))
                            .background(LkBg3)
                            .border(1.dp, LkBorder, RoundedCornerShape(7.dp))
                            .clickable { catMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedCatName, fontSize = 12.sp, color = LkText)
                        Icon(Icons.Outlined.ExpandMore, null, tint = LkText3, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false },
                        containerColor = LkBg3
                    ) {
                        DropdownMenuItem(
                            text = { Text("— Tanpa Kategori —", fontSize = 12.sp, color = LkText) },
                            onClick = {
                                selectedCatId = ""
                                catMenuExpanded = false
                            }
                        )
                        availableCats.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name, fontSize = 12.sp, color = LkText) },
                                onClick = {
                                    selectedCatId = cat.id
                                    catMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                DialogFieldLabel("TAGS")
                if (allModeTags.isEmpty()) {
                    Text(
                        text = "Belum ada tag — tambahkan di bawah",
                        fontSize = 10.5.sp,
                        color = LkText3
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allModeTags.forEach { tag ->
                            val isOn = tag in selectedTags
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isOn) LkBg4 else LkBg3)
                                    .border(1.dp, if (isOn) LkBorder2 else LkBorder, RoundedCornerShape(20.dp))
                                    .clickable {
                                        selectedTags = if (isOn) selectedTags - tag else selectedTags + tag
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isOn) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isOn) LkText else LkText3
                                )
                                Text(
                                    text = "×",
                                    fontSize = 12.sp,
                                    color = LkText3,
                                    modifier = Modifier.clickable {
                                        onDeleteTagRequest(tag, isPrivate) {
                                            selectedTags = selectedTags - tag
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DialogTextInput(
                            value = newTagInput,
                            onValueChange = { if (it.length <= 24) newTagInput = it },
                            placeholder = "Tag baru..."
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(LkBg3)
                            .border(1.dp, LkBorder, RoundedCornerShape(7.dp))
                            .clickable {
                                val t = newTagInput.trim()
                                if (t.isNotEmpty()) {
                                    actions.onAddExtraTag(t, isPrivate)
                                    if (t !in selectedTags) selectedTags = selectedTags + t
                                    newTagInput = ""
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text("+ Tambah", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = LkText2)
                    }
                }

                // Private toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(7.dp))
                        .background(LkBg3)
                        .border(1.dp, LkBorder, RoundedCornerShape(7.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Outlined.Lock, null, tint = LkText, modifier = Modifier.size(12.dp))
                            Text("Link Privat", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = LkText)
                        }
                        Text("Hanya muncul di Mode Privat", fontSize = 10.sp, color = LkText3)
                    }
                    Switch(
                        checked = isPrivate,
                        onCheckedChange = { checked ->
                            if (checked && !uiState.hasPin) {
                                onRequirePin()
                            } else {
                                isPrivate = checked
                                selectedCatId = ""
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = LkPrAccent,
                            uncheckedThumbColor = LkText2,
                            uncheckedTrackColor = LkBorder
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .border(1.dp, LkBorder, RoundedCornerShape(7.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Batal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = LkText2)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(LkText)
                        .clickable {
                            val rawUrl = urlInput.trim()
                            if (rawUrl.isEmpty()) return@clickable
                            val finalUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
                                rawUrl
                            } else {
                                "https://$rawUrl"
                            }
                            scope.launch {
                                actions.onSaveLink(
                                    editing?.id,
                                    finalUrl,
                                    titleInput.trim(),
                                    noteInput.trim(),
                                    selectedCatId,
                                    selectedTags,
                                    isPrivate
                                )
                                onSaved(editing != null)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LkBg)
                }
            }
        }
    }
}

@Composable
private fun LinkkuSettingsDialog(
    hasPin: Boolean,
    onDismiss: () -> Unit,
    onOpenSetPin: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(LkBg2)
                .border(1.dp, LkBorder2, RoundedCornerShape(14.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pengaturan", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = LkText)
                TopIconButton(LkBorder, onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, "Tutup", tint = LkText2, modifier = Modifier.size(14.dp))
                }
            }

            DialogFieldLabel("KEAMANAN")
            SettingsActionRow(
                icon = Icons.Outlined.Lock,
                title = "PIN Privat",
                subtitle = if (hasPin) "Sudah diatur" else "Belum diatur",
                danger = false,
                onClick = onOpenSetPin
            )

            DialogFieldLabel("DATA")
            SettingsActionRow(
                icon = Icons.Outlined.Download,
                title = "Export Data",
                subtitle = "Download JSON",
                danger = false,
                onClick = onExport
            )
            SettingsActionRow(
                icon = Icons.Outlined.Upload,
                title = "Import Data",
                subtitle = "Dari file JSON",
                danger = false,
                onClick = onImport
            )
            SettingsActionRow(
                icon = Icons.Outlined.DeleteOutline,
                title = "Reset Semua Data",
                subtitle = "Hapus semua link & kategori",
                danger = true,
                onClick = onReset
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(LkBg3)
                    .border(1.dp, LkBorder, RoundedCornerShape(7.dp))
                    .padding(10.dp)
            ) {
                Text("Linkku v2.0", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LkText)
                Text("Data tersimpan lokal di SQLite perangkat.", fontSize = 10.sp, color = LkText3)
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    danger: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(LkBg3)
            .border(1.dp, LkBorder, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = if (danger) LkRed else LkText, modifier = Modifier.size(16.dp))
            Column {
                Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (danger) LkRed else LkText)
                Text(subtitle, fontSize = 10.sp, color = if (danger) LkRed.copy(alpha = 0.65f) else LkText3)
            }
        }
        Icon(
            Icons.Outlined.ChevronRight,
            null,
            tint = if (danger) LkRed else LkText3,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun CategoryManagerDialog(
    title: String,
    placeholder: String,
    categories: List<LinkkuCategoryEntity>,
    onAdd: (String) -> Unit,
    onDelete: (LinkkuCategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(LkBg2)
                .border(1.dp, LkBorder2, RoundedCornerShape(14.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = LkText)
                TopIconButton(LkBorder, onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, "Tutup", tint = LkText2, modifier = Modifier.size(14.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DialogTextInput(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = placeholder
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(LkText)
                        .clickable {
                            val clean = nameInput.trim()
                            if (clean.isNotEmpty()) {
                                onAdd(clean)
                                nameInput = ""
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text("Tambah", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LkBg)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (categories.isEmpty()) {
                    Text(
                        text = "Belum ada kategori",
                        fontSize = 11.sp,
                        color = LkText3,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(7.dp))
                                .background(LkBg3)
                                .border(1.dp, LkBorder, RoundedCornerShape(7.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Folder, null, tint = LkText3, modifier = Modifier.size(14.dp))
                                Text(cat.name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = LkText)
                            }
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Hapus",
                                tint = LkRed,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onDelete(cat) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinEntryDialog(
    title: String,
    rawMode: Boolean,
    onVerify: (String) -> Boolean,
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var buffer by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    fun submit() {
        if (buffer.length < 4) {
            errorMsg = "Masukkan 4 digit PIN"
            return
        }
        if (rawMode || onVerify(buffer)) {
            onSuccess(buffer)
        } else {
            errorMsg = "PIN salah"
            buffer = ""
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        PinKeypadCard(
            title = title,
            hint = null,
            buffer = buffer,
            errorMsg = errorMsg,
            onDigit = { d ->
                if (buffer.length < 4) {
                    errorMsg = ""
                    buffer += d
                    if (buffer.length == 4) submit()
                }
            },
            onBackspace = { if (buffer.isNotEmpty()) buffer = buffer.dropLast(1) },
            onOk = { submit() },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun SetPinDialog(
    onPinConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var firstPin by remember { mutableStateOf("") }
    var buffer by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    fun submit() {
        if (buffer.length < 4) {
            errorMsg = "Masukkan 4 digit PIN"
            return
        }
        if (step == 1) {
            firstPin = buffer
            buffer = ""
            errorMsg = ""
            step = 2
        } else {
            if (buffer == firstPin) {
                onPinConfirmed(firstPin)
            } else {
                errorMsg = "PIN tidak cocok"
                step = 1
                firstPin = ""
                buffer = ""
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        PinKeypadCard(
            title = "Atur PIN Privat",
            hint = if (step == 1) "Masukkan PIN 4 digit baru" else "Konfirmasi PIN",
            buffer = buffer,
            errorMsg = errorMsg,
            onDigit = { d ->
                if (buffer.length < 4) {
                    errorMsg = ""
                    buffer += d
                    if (buffer.length == 4) submit()
                }
            },
            onBackspace = { if (buffer.isNotEmpty()) buffer = buffer.dropLast(1) },
            onOk = { submit() },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PinKeypadCard(
    title: String,
    hint: String?,
    buffer: String,
    errorMsg: String,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onOk: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(288.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LkBg2)
            .border(1.dp, LkBorder2, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold, color = LkText)
            TopIconButton(LkBorder, onClick = onDismiss) {
                Icon(Icons.Outlined.Close, "Tutup", tint = LkText2, modifier = Modifier.size(14.dp))
            }
        }

        if (hint != null) {
            Text(hint, fontSize = 11.sp, color = LkText3)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) { idx ->
                val filled = idx < buffer.length
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(if (filled) LkPrAccent else Color.Transparent)
                        .border(1.5.dp, if (filled) LkPrAccent else LkBorder2, CircleShape)
                )
            }
        }

        Text(
            text = errorMsg,
            fontSize = 11.sp,
            color = LkRed,
            modifier = Modifier.height(16.dp)
        )

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("DEL", "0", "OK")
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { key ->
                        val isOk = key == "OK"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isOk) LkPrDim else LkBg3)
                                .border(1.dp, if (isOk) LkPrBorder else LkBorder, RoundedCornerShape(7.dp))
                                .clickable {
                                    when (key) {
                                        "DEL" -> onBackspace()
                                        "OK" -> onOk()
                                        else -> onDigit(key)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "DEL" -> Icon(
                                    Icons.AutoMirrored.Outlined.Backspace,
                                    "Hapus",
                                    tint = LkText,
                                    modifier = Modifier.size(16.dp)
                                )
                                "OK" -> Text("OK", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LkPrAccent)
                                else -> Text(key, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = LkText)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
        color = LkText3
    )
}

@Composable
private fun DialogTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    minHeightDp: Int = 38
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeightDp.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(LkBg3)
            .border(1.dp, LkBorder, RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 12.sp, color = LkText3)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 12.sp, color = LkText),
            cursorBrush = SolidColor(LkText),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BottomSheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tint.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

private fun openUrlInBrowser(context: Context, rawUrl: String, onError: (String) -> Unit) {
    val clean = rawUrl.trim()
    val url = if (clean.startsWith("http://") || clean.startsWith("https://")) clean else "https://$clean"
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        onError("Tidak dapat membuka tautan")
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("Linkku URL", text))
}
