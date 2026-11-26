package com.ecosystemku.assistant.ui.apps.kronik

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.ecosystemku.assistant.ui.components.LocalRemToast
import com.ecosystemku.assistant.ui.components.RemDatePickerDialog
import java.time.LocalDate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystemku.assistant.data.local.KronikEntryEntity
import com.ecosystemku.assistant.data.network.KronikMoodDetectResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Palet Autentik Kronik (dari index_90c418be.html) ───────────────────────
private val KronikBackdropTop = Color(0xFF1C1008)
private val KronikBackdropBottom = Color(0xFF080503)
private val KronikLeatherTop = Color(0xFF3D2008)
private val KronikLeatherMid = Color(0xFF2A1405)
private val KronikLeatherDark = Color(0xFF1A0D03)
private val KronikSpineDark = Color(0xFF120701)
private val KronikSpineBorder = Color(0xFF5A3010)

private val KronikGold = Color(0xFFC8A050)
private val KronikGoldBright = Color(0xFFC9A84C)
private val KronikGoldDim = Color(0xFF7A5F28)

private val PaperLight = Color(0xFFF6EDDA)
private val PaperDark = Color(0xFFEFE3C6)
private val InkColor = Color(0xFF3A2510)
private val InkMid = Color(0x8C503214)
private val InkLight = Color(0x59503214)
private val RuleLineColor = Color(0x2EA07846)
private val MarginLineColor = Color(0x38D27878)

private enum class KronikPhase {
    COVER,
    SPREAD,
    EDITOR
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun KronikScreen(
    entries: List<KronikEntryEntity>,
    actions: KronikActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remToast = LocalRemToast.current

    var phase by rememberSaveable { mutableStateOf(KronikPhase.COVER) }
    var isCoverFlipping by remember { mutableStateOf(false) }
    var currentEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var flipDirection by remember { mutableIntStateOf(1) } // 1 = forward, -1 = backward

    var showSettingsMenu by remember { mutableStateOf(false) }
    var selectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    var showMoodSheet by remember { mutableStateOf(false) }
    var isDetectingMood by remember { mutableStateOf(false) }
    var aiMoodHint by remember { mutableStateOf("") }
    var aiSuggestedMood by remember { mutableStateOf<KronikMood?>(null) }

    // Android Back button navigation inside Kronik
    BackHandler {
        when {
            showMoodSheet -> showMoodSheet = false
            phase == KronikPhase.EDITOR -> phase = KronikPhase.SPREAD
            phase == KronikPhase.SPREAD -> {
                isCoverFlipping = false
                showSettingsMenu = false
                selectMode = false
                selectedIds = emptySet()
                phase = KronikPhase.COVER
            }
            else -> onClose()
        }
    }

    // SAF Export JSON
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val jsonStr = actions.onExportJson()
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonStr.toByteArray(Charsets.UTF_8))
                    }
                    remToast?.success("Backup Kronik berhasil disimpan")
                }.onFailure {
                    remToast?.warning("Gagal mengekspor data Kronik")
                }
            }
        }
    }

    // SAF Import JSON
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("File kosong")
                    val importedCount = actions.onImportJson(raw)
                    remToast?.success("$importedCount lembaran Kronik berhasil diimpor")
                }.onFailure {
                    remToast?.warning("Gagal impor: format file tidak valid")
                }
            }
        }
    }

    fun triggerOpenBook() {
        if (isCoverFlipping || phase != KronikPhase.COVER) return
        isCoverFlipping = true
        scope.launch {
            delay(520)
            phase = KronikPhase.SPREAD
            isCoverFlipping = false
        }
    }

    fun triggerCloseBook() {
        showSettingsMenu = false
        selectMode = false
        selectedIds = emptySet()
        phase = KronikPhase.COVER
    }

    fun openEditorFor(entryId: String?) {
        scope.launch {
            val targetId = when {
                entryId != null -> entryId
                entries.isNotEmpty() -> entries.first().id
                else -> actions.onCreateEntry().id
            }
            currentEntryId = targetId
            flipDirection = 1
            phase = KronikPhase.EDITOR
        }
    }

    fun createNewEntryAndOpen() {
        scope.launch {
            val created = actions.onCreateEntry()
            currentEntryId = created.id
            flipDirection = 1
            phase = KronikPhase.EDITOR
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(KronikBackdropTop, KronikBackdropBottom)
                )
            )
            .imePadding()
    ) {
        // Ambient candlelight glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            KronikGold.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
        )

        if (phase != KronikPhase.EDITOR) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                val bookWidth = minOf(310.dp, maxWidth * 0.86f)
                val bookHeight = minOf(390.dp, (maxHeight - 130.dp).coerceAtLeast(240.dp))

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top bar tombol kembali ke Apps Hub
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onClose() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Kembali",
                                tint = KronikGold.copy(alpha = 0.72f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Apps",
                                fontFamily = FontFamily.Serif,
                                fontSize = 13.sp,
                                letterSpacing = 1.2.sp,
                                color = KronikGold.copy(alpha = 0.72f)
                            )
                        }
                        Text(
                            text = "${entries.size} LEMBARAN",
                            fontFamily = FontFamily.Serif,
                            fontSize = 10.sp,
                            letterSpacing = 2.4.sp,
                            color = KronikGold.copy(alpha = 0.45f)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Book container (Cover + Spread — proporsi min(310dp, 86vw) x min(390dp, 100vh - 130dp))
                    Box(
                        modifier = Modifier
                            .size(width = bookWidth, height = bookHeight)
                            .shadow(28.dp, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (phase == KronikPhase.COVER) {
                            KronikBookCover(
                                isFlipping = isCoverFlipping,
                                onOpen = ::triggerOpenBook,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            KronikBookSpread(
                                entries = entries,
                                showSettingsMenu = showSettingsMenu,
                                selectMode = selectMode,
                                selectedIds = selectedIds,
                                onToggleSettings = {
                                    showSettingsMenu = !showSettingsMenu
                                    if (!showSettingsMenu && selectMode) {
                                        selectMode = false
                                        selectedIds = emptySet()
                                    }
                                },
                                onToggleSelectMode = {
                                    selectMode = !selectMode
                                    selectedIds = emptySet()
                                },
                                onToggleSelectItem = { id ->
                                    selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                },
                                onDeleteSelected = {
                                    if (selectedIds.isNotEmpty()) {
                                        actions.onDeleteEntries(selectedIds)
                                        selectedIds = emptySet()
                                        selectMode = false
                                        showSettingsMenu = false
                                    }
                                },
                                onExport = {
                                    showSettingsMenu = false
                                    exportLauncher.launch("kronik-backup-${KronikHelpers.todayDateStr()}.json")
                                },
                                onImport = {
                                    showSettingsMenu = false
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                onOpenEntry = { id -> openEditorFor(id) },
                                onTogglePin = { id -> actions.onTogglePin(id) },
                                onNewEntry = { createNewEntryAndOpen() },
                                onSwipeCloseBook = ::triggerCloseBook,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (phase == KronikPhase.COVER) {
                        Text(
                            text = "— JURNAL & LEGENDA —",
                            fontFamily = FontFamily.Serif,
                            fontSize = 11.sp,
                            letterSpacing = 3.5.sp,
                            color = KronikGold.copy(alpha = 0.28f)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { triggerCloseBook() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                                tint = KronikGold.copy(alpha = 0.45f),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "KEMBALI KE COVER",
                                fontFamily = FontFamily.Serif,
                                fontSize = 10.5.sp,
                                letterSpacing = 2.6.sp,
                                color = KronikGold.copy(alpha = 0.45f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        } else {
            val activeIndex = entries.indexOfFirst { it.id == currentEntryId }.let { idx ->
                if (idx >= 0) idx else 0
            }
            val activeEntry = entries.getOrNull(activeIndex)

            if (activeEntry == null) {
                LaunchedEffect(Unit) { phase = KronikPhase.SPREAD }
            } else {
                AnimatedContent(
                    targetState = activeEntry.id,
                    transitionSpec = {
                        if (flipDirection >= 0) {
                            (slideInHorizontally(tween(300)) { it / 2 } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(tween(300)) { -it / 2 } + fadeOut(tween(220)))
                        } else {
                            (slideInHorizontally(tween(300)) { -it / 2 } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(tween(300)) { it / 2 } + fadeOut(tween(220)))
                        }
                    },
                    label = "kronikPageFlip"
                ) { targetId ->
                    val entryForPage = entries.firstOrNull { it.id == targetId } ?: activeEntry
                    val pageIdx = entries.indexOfFirst { it.id == entryForPage.id }.coerceAtLeast(0)

                    KronikJournalPageEditor(
                        entry = entryForPage,
                        entryNumber = pageIdx + 1,
                        totalEntries = entries.size.coerceAtLeast(1),
                        onBackToSpread = { phase = KronikPhase.SPREAD },
                        onArchive = {
                            val nextFallback = entries.getOrNull(pageIdx + 1)?.id
                                ?: entries.getOrNull(pageIdx - 1)?.id
                            actions.onArchiveEntry(entryForPage.id)
                            if (nextFallback != null && entries.size > 1) {
                                flipDirection = -1
                                currentEntryId = nextFallback
                            } else {
                                phase = KronikPhase.SPREAD
                            }
                        },
                        onNewEntry = ::createNewEntryAndOpen,
                        onOpenMoodPicker = {
                            aiMoodHint = ""
                            aiSuggestedMood = null
                            showMoodSheet = true
                        },
                        onUpdateLive = { t, c, d ->
                            actions.onUpdateEntry(entryForPage.id, t, c, d)
                        },
                        onFlushSave = { t, c, d ->
                            actions.onFlushEntry(entryForPage.id, t, c, d)
                        },
                        onSwipeNext = {
                            if (pageIdx < entries.lastIndex) {
                                flipDirection = 1
                                currentEntryId = entries[pageIdx + 1].id
                            }
                        },
                        onSwipePrev = {
                            if (pageIdx > 0) {
                                flipDirection = -1
                                currentEntryId = entries[pageIdx - 1].id
                            } else {
                                phase = KronikPhase.SPREAD
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (showMoodSheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    val currentMood = KronikMood.fromStatus(activeEntry.moodStatus)

                    ModalBottomSheet(
                        onDismissRequest = { showMoodSheet = false },
                        sheetState = sheetState,
                        containerColor = Color(0xFF221609),
                        contentColor = KronikGold
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "PILIH SUASANA HATI",
                                fontFamily = FontFamily.Serif,
                                fontSize = 11.sp,
                                letterSpacing = 3.2.sp,
                                color = KronikGold.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Grid 4 kolom untuk 7 mood
                            KronikMood.entries.chunked(4).forEach { rowMoods ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowMoods.forEach { mood ->
                                        val isActive = currentMood == mood || aiSuggestedMood == mood
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    if (isActive) KronikGold.copy(alpha = 0.14f)
                                                    else Color.White.copy(alpha = 0.03f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isActive) KronikGold.copy(alpha = 0.45f)
                                                    else KronikGold.copy(alpha = 0.12f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    actions.onSetMood(activeEntry.id, mood)
                                                    showMoodSheet = false
                                                }
                                                .padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            KronikMoodIcon(
                                                mood = mood,
                                                tint = KronikGoldBright,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Text(
                                                text = mood.status,
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 10.sp,
                                                color = if (isActive) KronikGold else KronikGold.copy(alpha = 0.65f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    repeat(4 - rowMoods.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            HorizontalDivider(thickness = 1.dp, color = KronikGold.copy(alpha = 0.14f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(KronikGold.copy(alpha = 0.10f))
                                        .border(1.dp, KronikGold.copy(alpha = 0.28f), RoundedCornerShape(8.dp))
                                        .clickable(enabled = !isDetectingMood) {
                                            val text = activeEntry.content.trim()
                                            if (text.length < 10) {
                                                aiMoodHint = "Tulis dulu isi jurnalnya ya..."
                                            } else {
                                                scope.launch {
                                                    isDetectingMood = true
                                                    aiMoodHint = "Menganalisis..."
                                                    when (val res = actions.onDetectMoodAi(text)) {
                                                        is KronikMoodDetectResult.Success -> {
                                                            val matched = KronikMood.fromStatus(res.mood)
                                                            if (matched != null) {
                                                                aiSuggestedMood = matched
                                                                aiMoodHint = "AI menyarankan: ${matched.status} — ketuk ikon untuk konfirmasi"
                                                            } else {
                                                                aiMoodHint = "Mood tidak terdeteksi, pilih manual ya"
                                                            }
                                                        }
                                                        is KronikMoodDetectResult.Failure -> {
                                                            aiMoodHint = res.message
                                                        }
                                                    }
                                                    isDetectingMood = false
                                                }
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDetectingMood) {
                                        CircularProgressIndicator(
                                            color = KronikGold,
                                            strokeWidth = 1.8.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AutoAwesome,
                                                contentDescription = null,
                                                tint = KronikGold.copy(alpha = 0.8f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "DETEKSI MOOD",
                                                fontFamily = FontFamily.Serif,
                                                fontSize = 10.5.sp,
                                                letterSpacing = 1.8.sp,
                                                color = KronikGold.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "hapus",
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFD87A6A).copy(alpha = 0.75f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            actions.onSetMood(activeEntry.id, null)
                                            showMoodSheet = false
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }

                            if (aiMoodHint.isNotEmpty()) {
                                Text(
                                    text = aiMoodHint,
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 11.sp,
                                    color = KronikGold.copy(alpha = 0.65f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KronikBookCover(
    isFlipping: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipping) -155f else 0f,
        animationSpec = tween(durationMillis = 520),
        label = "bookCoverRotate"
    )
    var dragAccum by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (dragAccum < -45f) onOpen()
                    }
                )
            }
            .clickable { onOpen() }
    ) {
        // Stack kertas di belakang cover (kelihatan saat cover terangkat)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp)
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(Brush.verticalGradient(listOf(PaperLight, PaperDark)))
        )

        // Tepian halaman di sebelah kanan
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(vertical = 4.dp)
                .width(12.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFC8B48A), Color(0xFFEDE0C4), Color(0xFFF5EAD4))
                    )
                )
        )

        // Spine buku di kiri
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(24.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(KronikSpineDark, KronikLeatherTop, Color(0xFF261205))
                    )
                )
                .border(1.dp, KronikSpineBorder, RoundedCornerShape(topStart = 7.dp, bottomStart = 7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "KRONIK",
                fontFamily = FontFamily.Serif,
                fontSize = 9.sp,
                letterSpacing = 2.5.sp,
                color = KronikGold.copy(alpha = 0.38f),
                modifier = Modifier.graphicsLayer { rotationZ = -90f }
            )
        }

        // Cover kulit depan dengan animasi putar 3D
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 6.dp)
                .graphicsLayer {
                    rotationY = rotation
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    cameraDistance = 14f * density
                }
                .clip(RoundedCornerShape(topEnd = 9.dp, bottomEnd = 9.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(KronikLeatherTop, KronikLeatherMid, KronikLeatherDark)
                    )
                )
                .border(1.dp, KronikSpineBorder, RoundedCornerShape(topEnd = 9.dp, bottomEnd = 9.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Emboss outer & inner frames
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .border(1.dp, Color(0x33B47832), RoundedCornerShape(6.dp))
                    .padding(7.dp)
                    .border(1.dp, Color(0x1AB47832), RoundedCornerShape(4.dp))
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                KronikFourPointStarRow(
                    color = KronikGold.copy(alpha = 0.5f),
                    starSize = 11.dp,
                    spacing = 10.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Kronik",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 27.sp,
                        letterSpacing = 1.sp,
                        color = KronikGold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, KronikGold.copy(alpha = 0.5f), Color.Transparent)
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "jurnal pribadi",
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        color = KronikGold.copy(alpha = 0.45f)
                    )
                }

                KronikFourPointStarRow(
                    color = KronikGold.copy(alpha = 0.5f),
                    starSize = 11.dp,
                    spacing = 10.dp
                )
            }

            Text(
                text = "KETUK / GESER UNTUK MEMBUKA",
                fontFamily = FontFamily.Serif,
                fontSize = 9.sp,
                letterSpacing = 2.2.sp,
                color = KronikGold.copy(alpha = 0.36f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KronikBookSpread(
    entries: List<KronikEntryEntity>,
    showSettingsMenu: Boolean,
    selectMode: Boolean,
    selectedIds: Set<String>,
    onToggleSettings: () -> Unit,
    onToggleSelectMode: () -> Unit,
    onToggleSelectItem: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onNewEntry: () -> Unit,
    onSwipeCloseBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var confirmBatchDelete by remember(selectedIds.size, selectMode) { mutableStateOf(false) }
    var dragAccum by remember { mutableStateOf(0f) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccum = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (dragAccum > 65f) onSwipeCloseBook()
                        else if (dragAccum < -65f) onNewEntry()
                    }
                )
            }
    ) {
        // Cover strip di kiri
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight()
                .background(Brush.verticalGradient(listOf(Color(0xFF3A1E07), Color(0xFF221003))))
        )

        // Kertas Daftar Isi bergaris
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Brush.verticalGradient(listOf(PaperLight, PaperDark)))
                .drawBehind {
                    val stepPx = 28.dp.toPx()
                    var y = 54.dp.toPx()
                    while (y < size.height) {
                        drawLine(RuleLineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += stepPx
                    }
                    val marginX = 34.dp.toPx()
                    drawLine(MarginLineColor, Offset(marginX, 0f), Offset(marginX, size.height), strokeWidth = 1.2f)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0x2E000000), Color.Transparent),
                            startX = 0f,
                            endX = 16.dp.toPx()
                        )
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 44.dp, end = 14.dp, top = 16.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Daftar Isi",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.8.sp,
                            color = InkColor
                        )
                        Text(
                            text = "CATATAN HARIAN",
                            fontFamily = FontFamily.Serif,
                            fontSize = 9.sp,
                            letterSpacing = 2.6.sp,
                            color = InkLight
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.MoreHoriz,
                        contentDescription = "Pengaturan Daftar Isi",
                        tint = if (showSettingsMenu) InkColor else InkLight,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onToggleSettings() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0x4D966432), Color.Transparent)
                            )
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (showSettingsMenu) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF5EDDA))
                            .border(1.dp, Color(0x33A07846), RoundedCornerShape(6.dp))
                    ) {
                        SpreadMenuItem(
                            icon = Icons.Outlined.CheckBox,
                            label = if (selectMode) "Mode Pilih: Aktif" else "Pilih Lembaran",
                            tint = if (selectMode) Color(0xFF8C501E) else InkColor,
                            onClick = onToggleSelectMode
                        )
                        if (selectMode) {
                            SpreadMenuItem(
                                icon = Icons.Outlined.Close,
                                label = if (confirmBatchDelete) "Yakin hapus ${selectedIds.size} lembaran?" else "Hapus Dipilih (${selectedIds.size})",
                                tint = Color(0xFFA0321E),
                                onClick = {
                                    if (selectedIds.isNotEmpty()) {
                                        if (!confirmBatchDelete) confirmBatchDelete = true
                                        else {
                                            confirmBatchDelete = false
                                            onDeleteSelected()
                                        }
                                    }
                                }
                            )
                        } else {
                            HorizontalDivider(thickness = 1.dp, color = Color(0x26A07846))
                            SpreadMenuItem(icon = Icons.Outlined.FileUpload, label = "Export Data (JSON)", tint = InkColor, onClick = onExport)
                            SpreadMenuItem(icon = Icons.Outlined.FileDownload, label = "Import Data (JSON)", tint = InkColor, onClick = onImport)
                        }
                    }
                }

                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KronikFourPointStar(color = InkLight, modifier = Modifier.size(12.dp))
                            Text(
                                text = "Belum ada lembaran.\nKetuk tombol di bawah untuk mulai",
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontSize = 11.5.sp,
                                lineHeight = 19.sp,
                                color = InkLight,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        itemsIndexed(entries, key = { _, item -> item.id }) { idx, entry ->
                            val mood = KronikMood.fromStatus(entry.moodStatus)
                            val isSelected = entry.id in selectedIds
                            val rowBg = when {
                                entry.pinned -> Color(0x12C8963C)
                                else -> Color.Transparent
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .then(
                                        if (mood != null) {
                                            Modifier.drawBehind {
                                                drawRect(
                                                    color = Color(mood.tocAccentHex),
                                                    size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                                                )
                                            }
                                        } else Modifier
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (selectMode) onToggleSelectItem(entry.id)
                                            else onOpenEntry(entry.id)
                                        },
                                        onLongClick = {
                                            if (!selectMode) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onTogglePin(entry.id)
                                            }
                                        }
                                    )
                                    .padding(vertical = 6.dp, horizontal = if (mood != null) 7.dp else 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectMode) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(14.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isSelected) Color(0x268C501E) else Color.Transparent)
                                            .border(1.dp, Color(0x66A07846), RoundedCornerShape(3.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = "Dipilih",
                                                tint = Color(0xFF7A5030),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "${idx + 1}",
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 9.5.sp,
                                    color = InkLight,
                                    modifier = Modifier.width(19.dp)
                                )

                                Text(
                                    text = entry.title.ifBlank { "(tanpa judul)" },
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 12.sp,
                                    color = InkColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = KronikHelpers.formatShortIdDate(entry.date),
                                    fontFamily = FontFamily.Serif,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 9.sp,
                                    color = InkLight
                                )

                                if (entry.pinned) {
                                    Text(
                                        text = "📌",
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                if (mood != null) {
                                    KronikMoodIcon(
                                        mood = mood,
                                        tint = Color(0xFF7A5030).copy(alpha = 0.65f),
                                        modifier = Modifier
                                            .padding(start = 5.dp)
                                            .size(13.dp)
                                    )
                                }
                            }
                            HorizontalDivider(thickness = 0.6.dp, color = Color(0x33A07846))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0x1A8C501E))
                        .border(1.dp, Color(0x40A06428), RoundedCornerShape(5.dp))
                        .clickable { onNewEntry() }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KronikFourPointStar(
                        color = Color(0xFF7A5030),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ Lembaran Baru",
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.5.sp,
                        color = Color(0xFF7A5030)
                    )
                }
            }
        }
    }
}

@Composable
private fun KronikFourPointStar(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val rx = size.width / 2f
        val ry = size.height / 2f
        val pinch = 0.22f
        val p = Path().apply {
            moveTo(cx, 0f)
            quadraticTo(cx + rx * pinch, cy - ry * pinch, size.width, cy)
            quadraticTo(cx + rx * pinch, cy + ry * pinch, cx, size.height)
            quadraticTo(cx - rx * pinch, cy + ry * pinch, 0f, cy)
            quadraticTo(cx - rx * pinch, cy - ry * pinch, cx, 0f)
            close()
        }
        drawPath(p, color = color)
    }
}

@Composable
private fun KronikFourPointStarRow(
    color: Color,
    starSize: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            KronikFourPointStar(color = color, modifier = Modifier.size(starSize))
        }
    }
}

@Composable
private fun SpreadMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Text(label, fontFamily = FontFamily.Serif, fontSize = 11.5.sp, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KronikJournalPageEditor(
    entry: KronikEntryEntity,
    entryNumber: Int,
    totalEntries: Int,
    onBackToSpread: () -> Unit,
    onArchive: () -> Unit,
    onNewEntry: () -> Unit,
    onOpenMoodPicker: () -> Unit,
    onUpdateLive: (title: String?, content: String?, date: String?) -> Unit,
    onFlushSave: (title: String, content: String, date: String) -> Unit,
    onSwipeNext: () -> Unit,
    onSwipePrev: () -> Unit,
    modifier: Modifier = Modifier
) {
    var titleText by remember(entry.id) { mutableStateOf(entry.title) }
    var contentText by remember(entry.id) { mutableStateOf(entry.content) }
    var dateText by remember(entry.id) { mutableStateOf(entry.date) }

    var isTypingIndicator by remember { mutableStateOf(false) }
    var confirmArchive by remember(entry.id) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSwipeHint by remember(entry.id) { mutableStateOf(true) }

    val mood = KronikMood.fromStatus(entry.moodStatus)
    val habitLogs = remember(entry.habitsLogJson) { KronikHelpers.parseHabitLogs(entry.habitsLogJson) }

    LaunchedEffect(entry.id) {
        delay(2600)
        showSwipeHint = false
    }

    LaunchedEffect(confirmArchive) {
        if (confirmArchive) {
            delay(2500)
            confirmArchive = false
        }
    }

    DisposableEffect(entry.id) {
        onDispose {
            onFlushSave(titleText, contentText, dateText)
        }
    }

    val scrollState = rememberScrollState()
    var dragAccumX by remember { mutableStateOf(0f) }
    var dragAccumY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PaperLight, PaperDark)))
            .drawBehind {
                val lineHeightPx = 32.sp.toPx()
                val topOffsetPx = 112.dp.toPx()
                var y = topOffsetPx
                while (y < size.height) {
                    drawLine(RuleLineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += lineHeightPx
                }
                val marginX = 50.dp.toPx()
                drawLine(MarginLineColor, Offset(marginX, 0f), Offset(marginX, size.height), strokeWidth = 1.2f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0x28000000), Color.Transparent),
                        startX = 0f,
                        endX = 20.dp.toPx()
                    )
                )
            }
            .pointerInput(entry.id) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragAccumX = 0f
                        dragAccumY = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        dragAccumX += dragAmount
                        dragAccumY += kotlin.math.abs(change.position.y - change.previousPosition.y)
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(dragAccumX) > 90f && kotlin.math.abs(dragAccumX) > dragAccumY * 1.4f) {
                            onFlushSave(titleText, contentText, dateText)
                            if (dragAccumX < 0f) onSwipeNext() else onSwipePrev()
                        }
                    }
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(start = 60.dp, end = 22.dp, top = 12.dp, bottom = 90.dp)
        ) {
            // Topbar editor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                onFlushSave(titleText, contentText, dateText)
                                onBackToSpread()
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Tutup",
                            tint = InkLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "TUTUP",
                            fontFamily = FontFamily.Serif,
                            fontSize = 11.sp,
                            letterSpacing = 1.6.sp,
                            color = InkLight
                        )
                    }

                    Text(
                        text = if (confirmArchive) "ARSIP?" else "HAPUS",
                        fontFamily = FontFamily.Serif,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = if (confirmArchive) Color(0xCCA0321E) else Color(0x66A04632),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (!confirmArchive) confirmArchive = true
                                else onArchive()
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isTypingIndicator) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0x668C6428))
                        )
                    }

                    // Tombol pilih mood
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x148C501E))
                            .border(
                                1.dp,
                                if (mood != null) Color(0x66A06428) else Color(0x33A06428),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                onFlushSave(titleText, contentText, dateText)
                                onOpenMoodPicker()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        KronikMoodIcon(
                            mood = mood,
                            tint = if (mood != null) Color(0xFF6B421D) else InkMid,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = mood?.status ?: "mood",
                            fontFamily = FontFamily.Serif,
                            fontSize = 10.5.sp,
                            color = if (mood != null) InkColor else InkMid
                        )
                    }

                    Text(
                        text = "$entryNumber / $totalEntries",
                        fontFamily = FontFamily.Serif,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.4.sp,
                        color = InkLight
                    )

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x148C501E))
                            .border(1.dp, Color(0x33A06428), CircleShape)
                            .clickable {
                                onFlushSave(titleText, contentText, dateText)
                                onNewEntry()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 15.sp, color = Color(0xFF8B5A2B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Judul & Tanggal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (titleText.isEmpty()) {
                        Text(
                            text = "Judul lembaran...",
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 20.sp,
                            color = Color(0x38644628)
                        )
                    }
                    BasicTextField(
                        value = titleText,
                        onValueChange = {
                            titleText = it
                            isTypingIndicator = true
                            onUpdateLive(it, null, null)
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = InkColor
                        ),
                        cursorBrush = SolidColor(Color(0xFF8B3A1A)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = KronikHelpers.formatShortIdDate(dateText),
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                    color = Color(0x8C5C3D1E),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showDatePicker = true }
                        .padding(vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(thickness = 1.dp, color = Color(0x2E966432))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Area tulis isi jurnal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 360.dp)
            ) {
                if (contentText.isEmpty()) {
                    Text(
                        text = "Tulis sesuatu...",
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                        lineHeight = 32.sp,
                        color = Color(0x47644628)
                    )
                }
                BasicTextField(
                    value = contentText,
                    onValueChange = {
                        contentText = it
                        isTypingIndicator = true
                        onUpdateLive(null, it, null)
                    },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        lineHeight = 32.sp,
                        letterSpacing = 0.3.sp,
                        color = InkColor
                    ),
                    cursorBrush = SolidColor(Color(0xFF8B3A1A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 360.dp)
                )
            }

            // Quest Log (habits_log)
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(thickness = 1.dp, color = Color(0x26966432))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "QUEST LOG",
                    fontFamily = FontFamily.Serif,
                    fontSize = 9.5.sp,
                    letterSpacing = 2.6.sp,
                    color = Color(0x80966432)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = Color(0x1F966432)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (habitLogs.isEmpty()) {
                Text(
                    text = "Belum ada aktivitas tercatat hari ini.",
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 11.5.sp,
                    color = Color(0x4D644628)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    habitLogs.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KronikFourPointStar(
                                color = Color(0x80966432),
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .size(9.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.habitName,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 12.sp,
                                    color = Color(0xBF644628)
                                )
                                if (item.category.isNotBlank()) {
                                    Text(
                                        text = item.category,
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 10.sp,
                                        color = Color(0x66644628)
                                    )
                                }
                            }
                            val timeStr = KronikHelpers.formatTimeFromIso(item.completedAt)
                            if (timeStr.isNotEmpty()) {
                                Text(
                                    text = timeStr,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 10.sp,
                                    color = Color(0x59644628)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSwipeHint) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = Color(0x33785028),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "GESER HALAMAN",
                    fontFamily = FontFamily.Serif,
                    fontSize = 9.sp,
                    letterSpacing = 2.6.sp,
                    color = Color(0x33785028)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Color(0x33785028),
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }

    LaunchedEffect(isTypingIndicator) {
        if (isTypingIndicator) {
            delay(900)
            isTypingIndicator = false
        }
    }

    if (showDatePicker) {
        val initDate = runCatching { LocalDate.parse(dateText) }.getOrNull() ?: LocalDate.now()
        RemDatePickerDialog(
            initialDate = initDate,
            title = "Pilih Tanggal",
            onDismiss = { showDatePicker = false },
            onConfirm = { picked ->
                val newDate = picked.toString()
                dateText = newDate
                onUpdateLive(null, null, newDate)
                showDatePicker = false
            }
        )
    }
}
