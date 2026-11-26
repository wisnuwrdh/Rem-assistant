package com.ecosystemku.assistant.ui.apps.ideku

import android.content.Intent
import com.ecosystemku.assistant.ui.components.LocalRemToast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ecosystemku.assistant.data.local.CuciWishlistEntity
import com.ecosystemku.assistant.data.local.IdekuIdeaEntity
import com.ecosystemku.assistant.data.network.IdekuAiResult
import com.ecosystemku.assistant.ui.theme.AppIdeKu
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.DominantBg
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.SemanticDanger
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.TextSubtle
import com.ecosystemku.assistant.ui.theme.UiSans
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun IdekuScreen(
    uiState: IdekuUiState,
    actions: IdekuActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remToast = LocalRemToast.current

    var filterType by remember { mutableStateOf("Idea") }
    var filterStatus by remember { mutableStateOf<String?>(null) }
    var filterCat by remember { mutableStateOf("Semua") }
    var filterJalur by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var collapsedCats by remember { mutableStateOf(setOf<String>()) }

    var menuOpen by remember { mutableStateOf(false) }
    var showTypeSelector by remember { mutableStateOf(false) }
    var formType by remember { mutableStateOf<String?>(null) }
    var editingItem by remember { mutableStateOf<IdekuIdeaEntity?>(null) }
    var detailItemId by remember { mutableStateOf<Long?>(null) }
    var deleteCandidate by remember { mutableStateOf<IdekuIdeaEntity?>(null) }
    var showDeleteAll by remember { mutableStateOf(false) }
    var showManageCats by remember { mutableStateOf(false) }
    var focusSwapTarget by remember { mutableStateOf<IdekuIdeaEntity?>(null) }
    var aiDecisionTarget by remember { mutableStateOf<IdekuIdeaEntity?>(null) }

    var aiDialogItem by remember { mutableStateOf<IdekuIdeaEntity?>(null) }
    var aiDialogBreakdown by remember { mutableStateOf(false) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiResultText by remember { mutableStateOf<String?>(null) }
    var aiErrorText by remember { mutableStateOf<String?>(null) }

    BackHandler { onClose() }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
                if (raw.isNullOrBlank()) {
                    remToast?.warning("Gagal membaca file JSON")
                    return@launch
                }
                val res = actions.onImportJson(raw)
                res.onSuccess { added ->
                    remToast?.success("$added ide baru berhasil diimpor")
                }.onFailure {
                    remToast?.warning("Format JSON tidak valid")
                }
            }
        }
    }

    fun triggerAiModal(item: IdekuIdeaEntity, isBreakdown: Boolean) {
        aiDialogItem = item
        aiDialogBreakdown = isBreakdown
        aiLoading = true
        aiResultText = null
        aiErrorText = null
        scope.launch {
            val res = if (isBreakdown) actions.onBreakdownIdea(item) else actions.onPerkayaIdea(item)
            aiLoading = false
            when (res) {
                is IdekuAiResult.Success -> aiResultText = res.text
                is IdekuAiResult.Failure -> aiErrorText = res.message
            }
        }
    }

    fun requestStatusChange(item: IdekuIdeaEntity, newStatus: String) {
        if (newStatus == "Proses" && item.type == "Idea" && item.status != "Proses") {
            val activeFocus = uiState.ideas.filter { it.type == "Idea" && it.status == "Proses" }
            if (activeFocus.size >= IdekuHelpers.FOCUS_LIMIT) {
                focusSwapTarget = item
                return
            }
        }
        scope.launch { actions.onUpdateStatus(item.id, newStatus) }
    }

    val ideas = uiState.ideas
    val belumCount = remember(ideas) { ideas.count { it.status == "Belum" } }
    val fokusIdeas = remember(ideas) { ideas.filter { it.type == "Idea" && it.status == "Proses" } }
    val selesaiCount = remember(ideas) { ideas.count { it.status == "Selesai" } }

    val jalurEntries = remember(ideas) {
        ideas.filter { it.type == "Idea" && it.jalur.isNotBlank() }
            .groupingBy { it.jalur }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
    }
    val existingJalurNames = remember(jalurEntries) { jalurEntries.map { it.key } }
    val noJalurCount = remember(ideas) { ideas.count { it.type == "Idea" && it.jalur.isBlank() } }

    val scopedByType = remember(ideas, filterType) { ideas.filter { it.type == filterType } }
    val catDistEntries = remember(scopedByType) {
        scopedByType.groupingBy { it.category.ifBlank { "Lainnya" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
    }
    val parkirCount = remember(scopedByType) { scopedByType.count { it.status == "Parkir" } }

    val filteredIdeas = remember(scopedByType, filterStatus, filterCat, filterJalur, searchQuery) {
        val q = searchQuery.trim().lowercase()
        scopedByType.filter { item ->
            val matchStatus = if (filterStatus != null) item.status == filterStatus else item.status != "Parkir"
            val matchCat = filterCat == "Semua" || item.category == filterCat
            val matchJalur = when (filterJalur) {
                null -> true
                "__none" -> item.jalur.isBlank()
                else -> item.jalur == filterJalur
            }
            val matchSearch = q.isEmpty() || item.title.lowercase().contains(q) || item.notes.lowercase().contains(q)
            matchStatus && matchCat && matchJalur && matchSearch
        }
    }

    val detailItem = detailItemId?.let { id -> ideas.firstOrNull { it.id == id } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DominantBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sticky Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SecondarySurface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TextPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Lightbulb, null, tint = DominantBg, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("Ideku", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Catat · Kelola · Eksekusi", color = TextMuted, fontSize = 11.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .border(1.dp, BorderLight, RoundedCornerShape(9.dp))
                                .clickable { menuOpen = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.MoreHoriz, "Menu", tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = SecondarySurface
                        ) {
                            DropdownMenuItem(
                                text = { Text("Kelola Kategori", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    menuOpen = false
                                    showManageCats = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export JSON", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    menuOpen = false
                                    if (ideas.isEmpty()) {
                                        remToast?.warning("Tidak ada ide untuk diekspor")
                                        return@DropdownMenuItem
                                    }
                                    scope.launch {
                                        val jsonStr = actions.onExportJson()
                                        runCatching {
                                            val file = File(context.cacheDir, "ideku-backup.json")
                                            file.writeText(jsonStr)
                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Export IdeKu JSON"))
                                        }.onFailure {
                                            remToast?.warning("Gagal mengekspor JSON")
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import JSON", color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    menuOpen = false
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hapus Semua Data", color = SemanticDanger, fontSize = 13.sp) },
                                onClick = {
                                    menuOpen = false
                                    if (ideas.isEmpty()) {
                                        remToast?.warning("Tidak ada data untuk dihapus")
                                    } else {
                                        showDeleteAll = true
                                    }
                                }
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(TextPrimary)
                            .clickable { showTypeSelector = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Add, "Tambah", tint = DominantBg, modifier = Modifier.size(18.dp))
                    }
                }
            }
            HorizontalDivider(color = BorderSubtle)

            // Main Scrollable Body
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                // 4 Stats Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SecondarySurface)
                    ) {
                        IdekuStatCell("Total", ideas.size.toString(), isPrimary = true, modifier = Modifier.weight(1f))
                        IdekuStatCell("Belum mulai", belumCount.toString(), modifier = Modifier.weight(1f))
                        IdekuStatCell("Fokus (maks ${IdekuHelpers.FOCUS_LIMIT})", fokusIdeas.size.toString(), modifier = Modifier.weight(1f))
                        IdekuStatCell("Selesai", selesaiCount.toString(), showDivider = false, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(color = BorderSubtle)
                }

                // Search + View Mode + Type + Status Bar
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SecondarySurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Search, null, tint = TextSubtle, modifier = Modifier.size(16.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text("Cari ide...", color = TextSubtle, fontSize = 13.sp)
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary),
                                        cursorBrush = SolidColor(AppIdeKu),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(15.dp).clickable { searchQuery = "" }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SecondarySurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                    .clickable {
                                        val next = when (uiState.viewMode) {
                                            IdekuViewMode.GROUP -> IdekuViewMode.FLAT
                                            IdekuViewMode.FLAT -> IdekuViewMode.GRID
                                            IdekuViewMode.GRID -> IdekuViewMode.GROUP
                                        }
                                        actions.onSetViewMode(next)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (uiState.viewMode) {
                                        IdekuViewMode.GROUP -> Icons.Outlined.FolderOpen
                                        IdekuViewMode.FLAT -> Icons.AutoMirrored.Outlined.ViewList
                                        IdekuViewMode.GRID -> Icons.Outlined.GridView
                                    },
                                    contentDescription = "Ubah Tampilan",
                                    tint = TextMuted,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        // Compact Type + Status Filter Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SecondarySurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            listOf("Idea", "Wishlist").forEach { t ->
                                val active = filterType == t
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (active) TextPrimary else Color.Transparent)
                                        .clickable {
                                            filterType = t
                                            filterCat = "Semua"
                                            filterJalur = null
                                        }
                                        .padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (t == "Idea") Icons.Outlined.Lightbulb else Icons.Outlined.ShoppingCart,
                                        contentDescription = null,
                                        tint = if (active) DominantBg else TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(t, color = if (active) DominantBg else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Box(modifier = Modifier.width(1.dp).height(18.dp).background(BorderLight))
                            Text("STATUS", color = TextSubtle, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                            IdekuHelpers.STATUSES.forEach { st ->
                                val active = filterStatus == st
                                val label = if (st == "Parkir") "Parkir ($parkirCount)" else st
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (active) TextPrimary else Color.Transparent)
                                        .clickable { filterStatus = if (filterStatus == st) null else st }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(label, color = if (active) DominantBg else TextMuted, fontSize = 11.5.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // Fokus Sekarang Panel (only for Idea)
                if (filterType == "Idea") {
                    item {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 12.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SecondarySurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(13.dp)
                        ) {
                            Text(
                                text = "FOKUS SEKARANG · ${fokusIdeas.size}/${IdekuHelpers.FOCUS_LIMIT}",
                                color = TextSubtle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (fokusIdeas.isEmpty()) {
                                Text(
                                    "Belum ada fokus. Buka sebuah ide lalu ubah statusnya ke “Proses”.",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            } else {
                                fokusIdeas.forEach { f ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { detailItemId = f.id }
                                            .padding(vertical = 6.dp)
                                    ) {
                                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(bottom = 8.dp))
                                        Text(
                                            text = buildString {
                                                append(f.title)
                                                if (f.jalur.isNotBlank()) append(" · ${f.jalur}")
                                            },
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        if (f.nextStep.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ChevronRight,
                                                    contentDescription = null,
                                                    tint = TextPrimary.copy(alpha = 0.86f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = f.nextStep,
                                                    color = TextPrimary.copy(alpha = 0.86f),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "+ tulis langkah berikutnya",
                                                color = TextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                                val slots = (IdekuHelpers.FOCUS_LIMIT - fokusIdeas.size).coerceAtLeast(0)
                                if (slots > 0) {
                                    Text("$slots slot fokus tersisa", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                    }

                    // Jalur Bar
                    if (jalurEntries.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 12.dp)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("JALUR", color = TextSubtle, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                jalurEntries.forEach { (j, count) ->
                                    val active = filterJalur == j
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(if (active) TextPrimary else SecondarySurface)
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(7.dp))
                                            .clickable { filterJalur = if (filterJalur == j) null else j }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text("$j ($count)", color = if (active) DominantBg else TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (noJalurCount > 0) {
                                    val active = filterJalur == "__none"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(if (active) TextPrimary else SecondarySurface)
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(7.dp))
                                            .clickable { filterJalur = if (filterJalur == "__none") null else "__none" }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text("Tanpa jalur ($noJalurCount)", color = if (active) DominantBg else TextMuted, fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Category Distribution Dashboard
                if (catDistEntries.size > 1 || filterCat != "Semua") {
                    item {
                        val maxCount = catDistEntries.firstOrNull()?.value?.coerceAtLeast(1) ?: 1
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 12.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SecondarySurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                                .padding(13.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("SEBARAN KATEGORI · ${scopedByType.size} ITEM", color = TextSubtle, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                if (filterCat != "Semua") {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(ElevatedSurface)
                                            .border(1.dp, BorderLight, RoundedCornerShape(50))
                                            .clickable { filterCat = "Semua" }
                                            .padding(horizontal = 9.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(filterCat, color = TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                        Icon(Icons.Outlined.Close, contentDescription = "Reset filter", tint = TextMuted, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            catDistEntries.forEach { (cat, count) ->
                                val col = IdekuHelpers.categoryColor(cat)
                                val frac = (count.toFloat() / maxCount.toFloat()).coerceIn(0.06f, 1f)
                                val isActive = filterCat == cat
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { filterCat = if (filterCat == cat) "Semua" else cat }
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(col))
                                    Text(
                                        cat,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.width(68.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ElevatedSurface)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(frac)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(col)
                                        )
                                    }
                                    Text(count.toString(), color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(22.dp))
                                }
                            }
                        }
                    }
                }

                // Empty State or Item List
                if (filteredIdeas.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SecondarySurface)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (filterType == "Wishlist") Icons.Outlined.ShoppingCart else Icons.Outlined.Lightbulb,
                                    null,
                                    tint = TextSubtle,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = when {
                                    searchQuery.isNotBlank() -> "Tidak ditemukan"
                                    filterType == "Wishlist" && scopedByType.isEmpty() -> "Belum ada wishlist"
                                    ideas.isNotEmpty() -> "Tidak ada item di sini"
                                    else -> "Belum ada ide tersimpan"
                                },
                                color = TextPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = when {
                                    searchQuery.isNotBlank() -> "Coba kata kunci lain."
                                    filterType == "Wishlist" && scopedByType.isEmpty() -> "Simpan barang impianmu di sini. Terhubung ke Tabungan CuciMoney+."
                                    ideas.isNotEmpty() -> "Coba ganti filter atau tambah item baru."
                                    else -> "Ide bagus dimulai dari satu langkah kecil. Catat sebelum lupa!"
                                },
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            if (searchQuery.isBlank() && (ideas.isEmpty() || (filterType == "Wishlist" && scopedByType.isEmpty()))) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(TextPrimary)
                                        .clickable { formType = filterType }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = if (filterType == "Wishlist") "+ Tambah wishlist pertama" else "+ Tambah ide pertama",
                                        color = DominantBg,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    when (uiState.viewMode) {
                        IdekuViewMode.GRID -> {
                            val rows = filteredIdeas.chunked(2)
                            items(rows, key = { r -> r.joinToString("_") { it.id.toString() } }) { pair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { item ->
                                        IdekuGridCard(
                                            item = item,
                                            onOpen = { detailItemId = item.id },
                                            onEdit = { editingItem = item },
                                            onDelete = { deleteCandidate = item },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        IdekuViewMode.FLAT -> {
                            items(filteredIdeas, key = { it.id }) { item ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                    IdekuItemCard(
                                        item = item,
                                        savingPot = IdekuHelpers.matchingWishlistPot(item, uiState.wishlists),
                                        onOpen = { detailItemId = item.id },
                                        onEdit = { editingItem = item },
                                        onDelete = { deleteCandidate = item },
                                        onOpenAiScore = { aiDecisionTarget = item }
                                    )
                                }
                            }
                        }

                        IdekuViewMode.GROUP -> {
                            val grouped = filteredIdeas.groupBy { it.category.ifBlank { "Lainnya" } }
                                .entries
                                .sortedByDescending { it.value.size }
                            grouped.forEach { (cat, list) ->
                                val col = IdekuHelpers.categoryColor(cat)
                                val collapsed = cat in collapsedCats
                                val doneInCat = list.count { it.status == "Selesai" }
                                item(key = "grp_$cat") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(top = 6.dp, bottom = 6.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SecondarySurface)
                                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                            .drawBehind {
                                                drawRect(color = col, topLeft = Offset.Zero, size = Size(3.dp.toPx(), size.height))
                                            }
                                            .clickable {
                                                collapsedCats = if (collapsed) collapsedCats - cat else collapsedCats + cat
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(col))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cat, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${list.size} item" + if (doneInCat > 0) " · $doneInCat selesai" else "",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = if (collapsed) Icons.Outlined.ChevronRight else Icons.Outlined.ExpandMore,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                if (!collapsed) {
                                    items(list, key = { it.id }) { item ->
                                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                            IdekuItemCard(
                                                item = item,
                                                savingPot = IdekuHelpers.matchingWishlistPot(item, uiState.wishlists),
                                                onOpen = { detailItemId = item.id },
                                                onEdit = { editingItem = item },
                                                onDelete = { deleteCandidate = item },
                                                onOpenAiScore = { aiDecisionTarget = item }
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

        // Modals
        if (showTypeSelector) {
            IdekuTypeSelectorDialog(
                onSelect = { selected ->
                    showTypeSelector = false
                    formType = selected
                },
                onDismiss = { showTypeSelector = false }
            )
        }

        if (formType != null || editingItem != null) {
            IdekuFormDialog(
                initialType = formType ?: editingItem?.type ?: "Idea",
                editingItem = editingItem,
                categories = uiState.categories,
                existingJalur = existingJalurNames,
                onDismiss = {
                    formType = null
                    editingItem = null
                },
                onSave = { type, title, notes, cat, pri, jalur, price, image ->
                    val editId = editingItem?.id
                    val nextStep = editingItem?.nextStep.orEmpty()
                    formType = null
                    editingItem = null
                    scope.launch {
                        actions.onSaveIdea(editId, type, title, notes, cat, pri, jalur, nextStep, price, image)
                    }
                }
            )
        }

        if (detailItem != null) {
            IdekuItemDetailDialog(
                item = detailItem,
                savingPot = IdekuHelpers.matchingWishlistPot(detailItem, uiState.wishlists),
                existingJalur = existingJalurNames,
                onDismiss = { detailItemId = null },
                onEdit = {
                    detailItemId = null
                    editingItem = detailItem
                },
                onDelete = { deleteCandidate = detailItem },
                onUpdateStatus = { newSt -> requestStatusChange(detailItem, newSt) },
                onUpdatePriority = { newPri -> scope.launch { actions.onUpdatePriority(detailItem.id, newPri) } },
                onSaveMeta = { jalur, next ->
                    scope.launch {
                        actions.onUpdateMeta(detailItem.id, jalur, next)
                        remToast?.success("Tersimpan")
                    }
                },
                onPerkaya = { triggerAiModal(detailItem, isBreakdown = false) },
                onBreakdown = { triggerAiModal(detailItem, isBreakdown = true) }
            )
        }

        if (aiDialogItem != null) {
            val target = aiDialogItem!!
            IdekuAiResultDialog(
                isBreakdown = aiDialogBreakdown,
                ideaTitle = target.title,
                isLoading = aiLoading,
                resultText = aiResultText,
                errorText = aiErrorText,
                originalNotes = target.notes,
                onRetry = { triggerAiModal(target, aiDialogBreakdown) },
                onSavePerkaya = { enriched ->
                    aiDialogItem = null
                    scope.launch {
                        actions.onSaveIdea(
                            target.id,
                            target.type,
                            target.title,
                            enriched,
                            target.category,
                            target.priority,
                            target.jalur,
                            target.nextStep,
                            target.price,
                            target.image
                        )
                        remToast?.success("Ide berhasil diperkaya")
                    }
                },
                onDismiss = { aiDialogItem = null }
            )
        }

        if (aiDecisionTarget != null) {
            val target = aiDecisionTarget!!
            val decision = remember(target) { IdekuHelpers.analyzeWishlist(target) }
            IdekuAiDecisionDialog(
                item = target,
                decision = decision,
                onDismiss = { aiDecisionTarget = null }
            )
        }

        if (focusSwapTarget != null) {
            val newTarget = focusSwapTarget!!
            IdekuFocusFullDialog(
                newItem = newTarget,
                activeFocusList = fokusIdeas,
                onSwap = { parkId ->
                    focusSwapTarget = null
                    scope.launch {
                        actions.onSwapFocus(newTarget.id, parkId)
                        remToast?.success("Fokus diperbarui")
                    }
                },
                onDismiss = { focusSwapTarget = null }
            )
        }

        if (showManageCats) {
            IdekuManageCategoriesDialog(
                categories = uiState.categories,
                ideas = uiState.ideas,
                onAddCategory = actions.onAddCategory,
                onDeleteCategory = actions.onDeleteCategory,
                onDismiss = { showManageCats = false }
            )
        }

        if (deleteCandidate != null) {
            val cand = deleteCandidate!!
            val pot = IdekuHelpers.matchingWishlistPot(cand, uiState.wishlists)
            IdekuConfirmDialog(
                title = "Hapus ide ini?",
                subtitle = "“${cand.title}” akan dihapus permanen." +
                    if (pot != null) " Dana tabungan ${IdekuHelpers.formatRp(pot.saved)} di CuciMoney+ tetap aman." else "",
                onConfirm = {
                    deleteCandidate = null
                    if (detailItemId == cand.id) detailItemId = null
                    scope.launch { actions.onDeleteIdea(cand.id) }
                },
                onDismiss = { deleteCandidate = null }
            )
        }

        if (showDeleteAll) {
            IdekuConfirmDialog(
                title = "Hapus semua data?",
                subtitle = "Semua ide dan wishlist akan dihapus permanen. Tindakan ini tidak bisa dibatalkan.",
                confirmLabel = "Hapus Semua",
                onConfirm = {
                    showDeleteAll = false
                    detailItemId = null
                    scope.launch {
                        actions.onDeleteAll()
                        remToast?.success("Semua data IdeKu dihapus")
                    }
                },
                onDismiss = { showDeleteAll = false }
            )
        }
    }
}

@Composable
private fun IdekuStatCell(
    label: String,
    value: String,
    isPrimary: Boolean = false,
    showDivider: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(1f).padding(vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = TextPrimary, fontSize = if (isPrimary) 17.sp else 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = TextSubtle, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (showDivider) {
            Box(modifier = Modifier.width(1.dp).height(26.dp).background(BorderSubtle))
        }
    }
}

@Composable
private fun IdekuItemCard(
    item: IdekuIdeaEntity,
    savingPot: CuciWishlistEntity?,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenAiScore: () -> Unit
) {
    val catColor = IdekuHelpers.categoryColor(item.category)
    val done = item.status == "Selesai"
    val statusDot = IdekuHelpers.statusDotColor(item.status)
    val priColor = IdekuHelpers.priorityColor(item.priority)

    if (item.type == "Wishlist") {
        val aiScore = remember(item) { IdekuHelpers.analyzeWishlist(item) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (done) 0.5f else 1f)
                .clip(RoundedCornerShape(12.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .drawBehind {
                    drawRect(color = catColor, topLeft = Offset.Zero, size = Size(3.dp.toPx(), size.height))
                }
                .clickable(onClick = onOpen)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(13.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(IdekuHelpers.wishlistIcon(item.image), null, tint = TextMuted, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Text(
                            text = item.title,
                            color = if (done) TextMuted else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(modifier = Modifier.size(26.dp).clickable(onClick = onEdit), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Edit, "Edit", tint = TextMuted, modifier = Modifier.size(13.dp))
                            }
                            Box(modifier = Modifier.size(26.dp).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Close, "Hapus", tint = TextSubtle, modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                    Text(
                        text = if (item.price > 0L) IdekuHelpers.formatRp(item.price) else "—",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("${item.category} · ${item.priority}", color = TextMuted, fontSize = 11.sp)
                }
            }

            if (savingPot != null) {
                Box(modifier = Modifier.padding(horizontal = 13.dp).padding(bottom = 10.dp)) {
                    IdekuWishlistSavingsBox(price = item.price, saved = savingPot.saved)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ElevatedSurface.copy(alpha = 0.7f))
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusDot))
                Spacer(modifier = Modifier.width(6.dp))
                Text(item.status, color = TextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.width(1.dp).height(12.dp).background(BorderLight))
                Spacer(modifier = Modifier.width(10.dp))
                val filledDots = when (item.priority) {
                    "Penting" -> 3
                    "Nanti" -> 2
                    else -> 1
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(3) { idx ->
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (idx < filledDots) priColor else BorderLight)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(5.dp))
                Text(item.priority, color = TextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(aiScore.color.copy(alpha = 0.14f))
                        .border(1.dp, aiScore.color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenAiScore)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(aiScore.label, color = aiScore.color, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (done) 0.45f else 1f)
                .clip(RoundedCornerShape(12.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                .drawBehind {
                    drawRect(color = catColor, topLeft = Offset.Zero, size = Size(3.dp.toPx(), size.height))
                }
                .clickable(onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = AppIdeKu, modifier = Modifier.size(15.dp).padding(top = 2.dp))
                    Text(
                        text = item.title,
                        color = if (done) TextMuted else TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.size(26.dp).clickable(onClick = onEdit), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, "Edit", tint = TextMuted, modifier = Modifier.size(13.dp))
                    }
                    Box(modifier = Modifier.size(26.dp).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, "Hapus", tint = TextSubtle, modifier = Modifier.size(13.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(7.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(statusDot))
                Text(item.status, color = TextMuted, fontSize = 11.sp)
                Text("·", color = TextSubtle, fontSize = 10.sp)
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(catColor))
                Text(item.category, color = TextMuted, fontSize = 11.sp)
                Text("·", color = TextSubtle, fontSize = 10.sp)
                Text(item.priority, color = priColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                Text(IdekuHelpers.formatDate(item.id), color = TextSubtle, fontSize = 10.5.sp)
            }
        }
    }
}

@Composable
private fun IdekuGridCard(
    item: IdekuIdeaEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catColor = IdekuHelpers.categoryColor(item.category)
    val done = item.status == "Selesai"
    Column(
        modifier = modifier
            .alpha(if (done) 0.45f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(SecondarySurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .drawBehind {
                drawRect(color = catColor, topLeft = Offset.Zero, size = Size(size.width, 3.dp.toPx()))
            }
            .clickable(onClick = onOpen)
            .padding(11.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(catColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(IdekuHelpers.categoryIcon(item.category), null, tint = catColor, modifier = Modifier.size(15.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(modifier = Modifier.size(22.dp).clickable(onClick = onEdit), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Edit, "Edit", tint = TextMuted, modifier = Modifier.size(12.dp))
                }
                Box(modifier = Modifier.size(22.dp).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Close, "Hapus", tint = TextSubtle, modifier = Modifier.size(11.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            color = if (done) TextMuted else TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(item.category, color = catColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        if (item.price > 0L) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(IdekuHelpers.formatRp(item.price), color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(IdekuHelpers.statusDotColor(item.status)))
            Text(item.status, color = TextMuted, fontSize = 10.sp)
            Text("·", color = TextSubtle, fontSize = 9.sp)
            Text(item.priority, color = IdekuHelpers.priorityColor(item.priority), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
