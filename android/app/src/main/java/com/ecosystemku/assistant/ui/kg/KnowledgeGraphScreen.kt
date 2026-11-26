package com.ecosystemku.assistant.ui.kg

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.ecosystemku.assistant.ui.components.RemAlertDialog
import com.ecosystemku.assistant.ui.components.RemAlertTone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystemku.assistant.data.kg.KnowledgeGraphHelpers
import com.ecosystemku.assistant.data.kg.SimilarityRunReport
import com.ecosystemku.assistant.data.local.KgEdgeEntity
import com.ecosystemku.assistant.data.local.KgNodeEntity
import com.ecosystemku.assistant.ui.theme.AccentBlue
import com.ecosystemku.assistant.ui.theme.AppCuciMoneyLight
import com.ecosystemku.assistant.ui.theme.AppDailyOs
import com.ecosystemku.assistant.ui.theme.AppIdeKu
import com.ecosystemku.assistant.ui.theme.AppKronik
import com.ecosystemku.assistant.ui.theme.AppLinkKu
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.DominantBg
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PositionedKgNode(
    val node: KgNodeEntity,
    val worldX: Float,
    val worldY: Float,
    val radiusPx: Float
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KnowledgeGraphScreen(
    profileName: String,
    nodes: List<KgNodeEntity>,
    edges: List<KgEdgeEntity>,
    isSyncing: Boolean,
    onEnsureSyncedOnce: () -> Unit,
    onSyncSignals: () -> Unit,
    onStartKenaliAku: () -> Unit,
    onAddManualEdge: (fromType: String, fromLabel: String, relation: String, toType: String, toLabel: String, evidence: String, replacesLabel: String) -> Unit,
    onMarkEdgeHistorical: (edgeId: Long) -> Unit = {},
    onUpdateNode: (nodeId: Long, label: String, summary: String) -> Unit,
    onDeleteNode: (nodeId: Long) -> Unit,
    onDeleteEdge: (edgeId: Long) -> Unit,
    onClearGraph: () -> Unit,
    onExportDigitalTwinJson: suspend () -> String,
    onValidateNodeCorrect: (nodeId: Long) -> Unit = {},
    onValidateNodeWrong: (nodeId: Long) -> Unit = {},
    onReplaceNodeEvolution: (oldNodeId: Long, newType: String, newLabel: String, relationToSelf: String, evidence: String) -> Unit = { _, _, _, _, _ -> },
    onCompactOldNodes: () -> Unit = {},
    onRunOfflineSimilarity: suspend () -> SimilarityRunReport? = { null },
    onRunLiveSimilarity: suspend () -> Result<SimilarityRunReport> = { Result.failure(IllegalStateException("Belum tersedia")) },
    lastSimilarityLabel: String = "Belum pernah diuji",
    onShowBanner: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        onEnsureSyncedOnce()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("SEMUA") }
    var showHistorical by remember { mutableStateOf(false) }
    var selectedNodeId by remember { mutableStateOf<Long?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var showAddEdgeDialog by remember { mutableStateOf(false) }
    var editingNode by remember { mutableStateOf<KgNodeEntity?>(null) }
    var replacingNode by remember { mutableStateOf<KgNodeEntity?>(null) }
    var confirmClearGraph by remember { mutableStateOf(false) }
    var showBenchmarkDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val twinExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                try {
                    val jsonText = onExportDigitalTwinJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(jsonText.toByteArray(Charsets.UTF_8))
                        } ?: error("Gagal membuka file tujuan")
                    }
                    onShowBanner("Ekspor Digital Twin JSON berhasil disimpan")
                } catch (err: Exception) {
                    onShowBanner("Gagal ekspor: ${err.message}")
                } finally {
                    isExporting = false
                }
            }
        }
    }

    val degreeMap = remember(edges) {
        val counts = mutableMapOf<Long, Int>()
        edges.forEach { e ->
            counts[e.fromNodeId] = (counts[e.fromNodeId] ?: 0) + 1
            counts[e.toNodeId] = (counts[e.toNodeId] ?: 0) + 1
        }
        counts
    }

    val (visibleNodes, visibleEdges, isCapped) = remember(nodes, edges, searchQuery, selectedTypeFilter, selectedNodeId, showHistorical) {
        filterVisibleGraph(
            nodes = nodes,
            edges = edges,
            degreeMap = degreeMap,
            query = searchQuery,
            typeFilter = selectedTypeFilter,
            selectedNodeId = selectedNodeId,
            includeHistorical = showHistorical
        )
    }

    val positionedNodes by produceState<List<PositionedKgNode>>(
        initialValue = emptyList(),
        key1 = visibleNodes
    ) {
        value = withContext(Dispatchers.Default) {
            computeWorldLayout(visibleNodes)
        }
    }
    val posById = remember(positionedNodes) {
        positionedNodes.associateBy { it.node.id }
    }
    val selectedNeighborIds = remember(visibleEdges, selectedNodeId) {
        if (selectedNodeId == null) {
            emptySet()
        } else {
            buildSet {
                visibleEdges.forEach { e ->
                    if (e.fromNodeId == selectedNodeId) add(e.toNodeId)
                    if (e.toNodeId == selectedNodeId) add(e.fromNodeId)
                }
            }
        }
    }

    val selectedNode = remember(nodes, selectedNodeId) {
        nodes.firstOrNull { it.id == selectedNodeId }
    }

    val typeFilters = remember {
        listOf("SEMUA") + KnowledgeGraphHelpers.VALID_NODE_TYPES.toList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DominantBg)
            .statusBarsPadding()
    ) {
        // Header
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
                    .clickable(onClick = onClose),
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
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Otak Digital • ${profileName.ifBlank { "Tuan" }}",
                    fontFamily = UiSans,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${nodes.size} simpul • ${edges.size} sinapsis",
                    fontFamily = UiSans,
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                        .border(1.dp, BorderLight, CircleShape)
                        .clickable(enabled = !isSyncing) { onSyncSignals() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AccentBlue
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Sinkronkan Sinyal Modul",
                            tint = AccentBlue,
                            modifier = Modifier.size(19.dp)
                        )
                    }
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
                            contentDescription = "Menu Otak Digital",
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
                            text = { Text("Tambah Koneksi Manual", fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Add, null, tint = AccentBlue, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                menuOpen = false
                                showAddEdgeDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Uji Kemiripan (25 Skenario)", fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary)
                                    Text("Skor: $lastSimilarityLabel", fontFamily = UiSans, fontSize = 10.5.sp, color = TextMuted)
                                }
                            },
                            leadingIcon = { Icon(Icons.Outlined.Psychology, null, tint = AccentBlue, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                menuOpen = false
                                showBenchmarkDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Padatkan Riwayat Lama (>90h)", fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Schedule, null, tint = SemanticWarning, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                menuOpen = false
                                onCompactOldNodes()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ekspor Digital Twin (JSON)", fontFamily = UiSans, fontSize = 13.sp, color = TextPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Download, null, tint = SemanticSuccess, modifier = Modifier.size(18.dp)) },
                            enabled = !isExporting,
                            onClick = {
                                menuOpen = false
                                val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                                twinExportLauncher.launch("rem-digital-twin-$stamp.json")
                            }
                        )
                        if (nodes.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Bersihkan Graph", fontFamily = UiSans, fontSize = 13.sp, color = SemanticDanger) },
                                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = SemanticDanger, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuOpen = false
                                    confirmClearGraph = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // Top controls: Sesi Kenali Aku CTA + Search + Type filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp, fontFamily = UiSans),
                        cursorBrush = SolidColor(AccentBlue),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Cari simpul, kebiasaan, prinsip…", color = TextSubtle, fontSize = 12.5.sp, fontFamily = UiSans)
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Hapus pencarian",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentBlue.copy(alpha = 0.18f))
                        .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { onStartKenaliAku() }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Psychology, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(17.dp))
                    Text(
                        text = "Kenali Aku",
                        fontFamily = UiSans,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlue
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                typeFilters.forEach { t ->
                    val active = t == selectedTypeFilter
                    val tint = if (t == "SEMUA") AccentBlue else nodeTypeColor(t)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) tint.copy(alpha = 0.2f) else ElevatedSurface)
                            .border(
                                1.dp,
                                if (active) tint.copy(alpha = 0.6f) else BorderSubtle,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTypeFilter = t }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = formatNodeTypeLabel(t),
                            fontFamily = UiSans,
                            fontSize = 11.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) tint else TextMuted
                        )
                    }
                }
                val histTint = SemanticWarning
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (showHistorical) histTint.copy(alpha = 0.2f) else ElevatedSurface)
                        .border(
                            1.dp,
                            if (showHistorical) histTint.copy(alpha = 0.6f) else BorderSubtle,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { showHistorical = !showHistorical }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (showHistorical) "Riwayat: Tampil" else "Riwayat: Sembunyi",
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        fontWeight = if (showHistorical) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (showHistorical) histTint else TextMuted
                    )
                }
            }
        }

        // Interactive Canvas Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                .clipToBounds()
                .onSizeChanged { canvasSize = it }
        ) {
            if (positionedNodes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = AccentBlue.copy(alpha = 0.7f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (nodes.isEmpty()) "Otak Digital masih kosong" else "Tidak ada simpul yang cocok",
                        fontFamily = UiSans,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tekan Refresh untuk menyerap pola dari Daily OS, Kronik, IdeKu, CuciMoney+, & LinkKu Publik, atau mulai Sesi Kenali Aku.",
                        fontFamily = UiSans,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = TextMuted
                    )
                }
            } else {
                val textMeasurer = rememberTextMeasurer()
                val surfaceBg = SecondarySurface
                val labelColor = TextPrimary
                val labelCache = remember(positionedNodes, textMeasurer) {
                    val map = HashMap<Long, TextLayoutResult>(positionedNodes.size.coerceAtLeast(1))
                    positionedNodes.forEach { p ->
                        val shortLabel = if (p.node.label.length > 18) {
                            p.node.label.take(16) + "…"
                        } else {
                            p.node.label
                        }
                        val isSelfNode = p.node.nodeType == "SELF"
                        map[p.node.id] = textMeasurer.measure(
                            text = shortLabel,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = if (isSelfNode) 12.sp else 10.5.sp,
                                fontWeight = if (isSelfNode) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = UiSans
                            )
                        )
                    }
                    map
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(0.45f, 2.6f)
                                scale = nextScale
                                panOffset += pan
                            }
                        }
                        .pointerInput(positionedNodes, scale, panOffset, canvasSize) {
                            detectTapGestures { tap ->
                                val cx = canvasSize.width / 2f + panOffset.x
                                val cy = canvasSize.height / 2f + panOffset.y
                                var hitId: Long? = null
                                var bestDist = Float.MAX_VALUE
                                positionedNodes.forEach { p ->
                                    val sx = cx + p.worldX * scale
                                    val sy = cy + p.worldY * scale
                                    val d = hypot((tap.x - sx).toDouble(), (tap.y - sy).toDouble()).toFloat()
                                    val hitRadius = (p.radiusPx * scale + 26f).coerceAtLeast(36f)
                                    if (d <= hitRadius && d < bestDist) {
                                        bestDist = d
                                        hitId = p.node.id
                                    }
                                }
                                selectedNodeId = hitId
                            }
                        }
                ) {
                    val cx = size.width / 2f + panOffset.x
                    val cy = size.height / 2f + panOffset.y

                    // Edges first
                    val historicalDash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    visibleEdges.forEach { edge ->
                        val from = posById[edge.fromNodeId] ?: return@forEach
                        val to = posById[edge.toNodeId] ?: return@forEach
                        val p1 = Offset(cx + from.worldX * scale, cy + from.worldY * scale)
                        val p2 = Offset(cx + to.worldX * scale, cy + to.worldY * scale)
                        val isHist = KnowledgeGraphHelpers.isHistoricalEdge(edge)
                        val isHighlighted = selectedNodeId != null &&
                            (edge.fromNodeId == selectedNodeId || edge.toNodeId == selectedNodeId)
                        val baseColor = if (isHist) {
                            TextMuted
                        } else if (isHighlighted) {
                            AccentBlue
                        } else {
                            nodeTypeColor(to.node.nodeType)
                        }
                        val alpha = if (selectedNodeId != null && !isHighlighted) {
                            0.10f
                        } else if (isHighlighted) {
                            if (isHist) 0.55f else 0.90f
                        } else if (isHist) {
                            0.24f
                        } else {
                            (0.24f + edge.weight * 0.14f).coerceAtMost(0.75f)
                        }
                        val strokeW = ((1.2f + edge.weight * 0.7f) * scale.coerceIn(0.7f, 1.5f))
                            .coerceIn(1.2f, 6.5f)
                        drawLine(
                            color = baseColor.copy(alpha = alpha),
                            start = p1,
                            end = p2,
                            strokeWidth = strokeW,
                            pathEffect = if (isHist) historicalDash else null
                        )
                    }

                    // Nodes & labels
                    positionedNodes.forEach { p ->
                        val sx = cx + p.worldX * scale
                        val sy = cy + p.worldY * scale
                        if (sx < -80f || sx > size.width + 80f || sy < -80f || sy > size.height + 80f) {
                            return@forEach
                        }
                        val color = nodeTypeColor(p.node.nodeType)
                        val isSel = p.node.id == selectedNodeId
                        val isNeighbor = selectedNodeId != null && p.node.id in selectedNeighborIds
                        val dimmed = selectedNodeId != null && !isSel && !isNeighbor
                        val r = p.radiusPx * scale.coerceIn(0.65f, 1.55f)
                        val alpha = if (dimmed) 0.25f else 1f

                        if (isSel || p.node.nodeType == "SELF") {
                            drawCircle(
                                color = color.copy(alpha = 0.22f * alpha),
                                radius = r + 10f,
                                center = Offset(sx, sy)
                            )
                        }
                        drawCircle(
                            color = surfaceBg,
                            radius = r,
                            center = Offset(sx, sy)
                        )
                        drawCircle(
                            color = color.copy(alpha = 0.26f * alpha),
                            radius = r,
                            center = Offset(sx, sy)
                        )
                        drawCircle(
                            color = color.copy(alpha = alpha),
                            radius = r,
                            center = Offset(sx, sy),
                            style = Stroke(width = if (isSel) 3.dp.toPx() else 1.8.dp.toPx())
                        )

                        val measured = labelCache[p.node.id] ?: return@forEach
                        val labelX = (sx - measured.size.width / 2f).coerceIn(4f, (size.width - measured.size.width - 4f).coerceAtLeast(4f))
                        val labelY = (sy + r + 5f).coerceIn(4f, (size.height - measured.size.height - 4f).coerceAtLeast(4f))
                        drawText(
                            textLayoutResult = measured,
                            color = labelColor,
                            alpha = if (dimmed) 0.35f else 0.94f,
                            topLeft = Offset(labelX, labelY)
                        )
                    }
                }
            }

            if (isCapped) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DominantBg.copy(alpha = 0.85f))
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Top ${visibleNodes.size} dari ${nodes.size} simpul",
                        fontFamily = UiSans,
                        fontSize = 10.5.sp,
                        color = TextMuted
                    )
                }
            }

            // Zoom / Reset controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DominantBg.copy(alpha = 0.88f))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "−",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { scale = (scale * 0.82f).coerceIn(0.45f, 2.6f) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    text = "Reset",
                    color = AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = UiSans,
                    modifier = Modifier
                        .clickable {
                            scale = 1f
                            panOffset = Offset.Zero
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
                Text(
                    text = "+",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { scale = (scale * 1.22f).coerceIn(0.45f, 2.6f) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Bottom inspector drawer when a node is selected
        if (selectedNode != null) {
            val outgoing = remember(edges, selectedNode.id) { edges.filter { it.fromNodeId == selectedNode.id } }
            val incoming = remember(edges, selectedNode.id) { edges.filter { it.toNodeId == selectedNode.id } }
            val nodeLookup = remember(nodes) { nodes.associateBy { it.id } }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .background(SecondarySurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val tColor = nodeTypeColor(selectedNode.nodeType)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = formatNodeTypeLabel(selectedNode.nodeType),
                                    fontFamily = UiSans,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = tColor
                                )
                            }
                            if (!KnowledgeGraphHelpers.isActiveNode(selectedNode)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SemanticWarning.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (selectedNode.status == "compacted") "RINGKASAN" else "USANG",
                                        fontFamily = UiSans,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SemanticWarning
                                    )
                                }
                            }
                            if (selectedNode.validationState == "confirmed" || selectedNode.validationState == "verified") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SemanticSuccess.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "TERVERIFIKASI",
                                        fontFamily = UiSans,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SemanticSuccess
                                    )
                                }
                            } else if (selectedNode.validationState == "rejected" || selectedNode.validationState == "wrong") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SemanticDanger.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "KELIRU",
                                        fontFamily = UiSans,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SemanticDanger
                                    )
                                }
                            }
                            Text(
                                text = "sumber: ${selectedNode.sourceType} • yakin ${(selectedNode.confidence * 100).toInt()}%",
                                fontFamily = UiSans,
                                fontSize = 10.5.sp,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selectedNode.label,
                            fontFamily = UiSans,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (selectedNode.summary.isNotBlank()) {
                            Text(
                                text = selectedNode.summary,
                                fontFamily = UiSans,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                        val metaDateFmt = remember { SimpleDateFormat("dd MMM yy", Locale("in", "ID")) }
                        val firstLbl = if (selectedNode.firstSeenAt > 0L) metaDateFmt.format(Date(selectedNode.firstSeenAt)) else "-"
                        val confLbl = if (selectedNode.lastConfirmedAt > 0L) metaDateFmt.format(Date(selectedNode.lastConfirmedAt)) else "belum"
                        val refPart = if (selectedNode.sourceRef.isNotBlank()) " • asal: ${selectedNode.sourceRef}" else ""
                        Text(
                            text = "Dibuat: $firstLbl • Dikonfirmasi: $confLbl$refPart",
                            fontFamily = UiSans,
                            fontSize = 10.sp,
                            color = TextSubtle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (selectedNode.nodeType != "SELF") {
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                KgValidationPill(
                                    label = "Benar",
                                    accent = SemanticSuccess,
                                    onClick = { onValidateNodeCorrect(selectedNode.id) }
                                )
                                KgValidationPill(
                                    label = "Salah",
                                    accent = SemanticDanger,
                                    onClick = { onValidateNodeWrong(selectedNode.id) }
                                )
                                KgValidationPill(
                                    label = "Sudah Berubah",
                                    accent = AccentBlue,
                                    onClick = { replacingNode = selectedNode }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Simpul",
                            tint = AccentBlue,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { editingNode = selectedNode }
                        )
                        if (selectedNode.nodeType != "SELF") {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Hapus Simpul",
                                tint = SemanticDanger,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable {
                                        onDeleteNode(selectedNode.id)
                                        selectedNodeId = null
                                    }
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup detail",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { selectedNodeId = null }
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (outgoing.isEmpty() && incoming.isEmpty()) {
                        Text(
                            text = "Belum ada koneksi terhubung ke simpul ini.",
                            fontFamily = UiSans,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    } else {
                        outgoing.forEach { edge ->
                            val target = nodeLookup[edge.toNodeId]
                            EdgeDetailRow(
                                text = "→ ${edge.relation} → ${target?.label ?: "#${edge.toNodeId}"}",
                                evidence = edge.evidence,
                                weight = edge.weight,
                                isHistorical = KnowledgeGraphHelpers.isHistoricalEdge(edge),
                                onMarkHistorical = { onMarkEdgeHistorical(edge.id) },
                                onDelete = { onDeleteEdge(edge.id) }
                            )
                        }
                        incoming.forEach { edge ->
                            val source = nodeLookup[edge.fromNodeId]
                            EdgeDetailRow(
                                text = "${source?.label ?: "#${edge.fromNodeId}"} → ${edge.relation} →",
                                evidence = edge.evidence,
                                weight = edge.weight,
                                isHistorical = KnowledgeGraphHelpers.isHistoricalEdge(edge),
                                onMarkHistorical = { onMarkEdgeHistorical(edge.id) },
                                onDelete = { onDeleteEdge(edge.id) }
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Ketuk simpul untuk detail, cubit/seret untuk zoom & geser peta pikiranmu.",
                    fontFamily = UiSans,
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }
        }
    }

    if (showAddEdgeDialog) {
        AddKgConnectionDialog(
            defaultFromLabel = profileName.ifBlank { "Tuan" },
            onSave = { fromType, fromLabel, rel, toType, toLabel, ev, replacesLabel ->
                onAddManualEdge(fromType, fromLabel, rel, toType, toLabel, ev, replacesLabel)
                showAddEdgeDialog = false
            },
            onDismiss = { showAddEdgeDialog = false }
        )
    }

    editingNode?.let { node ->
        EditKgNodeDialog(
            node = node,
            onSave = { newLabel, newSummary ->
                onUpdateNode(node.id, newLabel, newSummary)
                editingNode = null
            },
            onDismiss = { editingNode = null }
        )
    }

    replacingNode?.let { oldNode ->
        ReplaceKgNodeDialog(
            oldNode = oldNode,
            onSave = { newType, newLabel, relToSelf, evidence ->
                onReplaceNodeEvolution(oldNode.id, newType, newLabel, relToSelf, evidence)
                replacingNode = null
            },
            onDismiss = { replacingNode = null }
        )
    }

    if (showBenchmarkDialog) {
        SimilarityBenchmarkDialog(
            lastSimilarityLabel = lastSimilarityLabel,
            onRunOffline = onRunOfflineSimilarity,
            onRunLive = onRunLiveSimilarity,
            onDismiss = { showBenchmarkDialog = false }
        )
    }

    if (confirmClearGraph) {
        RemAlertDialog(
            title = "Bersihkan Otak Digital?",
            description = "Seluruh simpul dan relasi di Knowledge Graph akan dihapus permanen. Memori flat dan catatan modul lain tetap aman, $profileName.",
            confirmText = "Bersihkan",
            dismissText = "Batal",
            tone = RemAlertTone.DANGER,
            onConfirm = {
                confirmClearGraph = false
                selectedNodeId = null
                onClearGraph()
            },
            onDismiss = { confirmClearGraph = false }
        )
    }
}

@Composable
private fun KgValidationPill(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontFamily = UiSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReplaceKgNodeDialog(
    oldNode: KgNodeEntity,
    onSave: (newType: String, newLabel: String, relationToSelf: String, evidence: String) -> Unit,
    onDismiss: () -> Unit
) {
    var newType by remember { mutableStateOf(oldNode.nodeType) }
    var newLabel by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    val types = remember { KnowledgeGraphHelpers.VALID_NODE_TYPES.filter { it != "SELF" } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SecondarySurface,
        title = {
            Text(
                "Catat Perubahan / Evolusi",
                color = TextPrimary,
                fontFamily = UiSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Simpul lama \"${oldNode.label}\" akan ditandai usang dan dihubungkan dengan relasi MENGGANTIKAN.",
                    color = TextMuted,
                    fontFamily = UiSans,
                    fontSize = 11.5.sp
                )
                Text("Tipe Baru", color = TextMuted, fontSize = 11.sp, fontFamily = UiSans)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    types.forEach { t ->
                        val active = newType == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface)
                                .border(1.dp, if (active) AccentBlue else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { newType = t }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = formatNodeTypeLabel(t),
                                color = if (active) AccentBlue else TextMuted,
                                fontSize = 10.5.sp,
                                fontFamily = UiSans
                            )
                        }
                    }
                }
                KgDialogField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    placeholder = "Kondisi/pandangan baru sekarang"
                )
                KgDialogField(
                    value = evidence,
                    onValueChange = { evidence = it },
                    placeholder = "Alasan perubahan (opsional)",
                    singleLine = false
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = newLabel.isNotBlank(),
                onClick = { onSave(newType, newLabel.trim(), "MENJALANKAN", evidence.trim()) }
            ) {
                Text(
                    "Simpan Evolusi",
                    color = if (newLabel.isNotBlank()) AccentBlue else TextSubtle,
                    fontFamily = UiSans,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextMuted, fontFamily = UiSans)
            }
        }
    )
}

@Composable
private fun SimilarityBenchmarkDialog(
    lastSimilarityLabel: String,
    onRunOffline: suspend () -> SimilarityRunReport?,
    onRunLive: suspend () -> Result<SimilarityRunReport>,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<SimilarityRunReport?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isRunning = true
        report = onRunOffline()
        isRunning = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SecondarySurface,
        title = {
            Column {
                Text(
                    "Uji Kemiripan Kembaran Digital",
                    color = TextPrimary,
                    fontFamily = UiSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    "Riwayat terakhir: $lastSimilarityLabel",
                    color = TextMuted,
                    fontFamily = UiSans,
                    fontSize = 11.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 390.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            .clickable(enabled = !isRunning) {
                                scope.launch {
                                    isRunning = true
                                    errorMsg = null
                                    report = onRunOffline()
                                    isRunning = false
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Cek Graph (Offline)",
                            color = TextPrimary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = UiSans
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentBlue.copy(alpha = 0.18f))
                            .border(1.dp, AccentBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .clickable(enabled = !isRunning) {
                                scope.launch {
                                    isRunning = true
                                    errorMsg = null
                                    onRunLive()
                                        .onSuccess { report = it }
                                        .onFailure { errorMsg = it.message ?: "Gagal menjalankan uji LLM" }
                                    isRunning = false
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Uji LLM (25 Skenario)",
                            color = AccentBlue,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = UiSans
                        )
                    }
                }

                if (isRunning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentBlue)
                        Text("Menjalankan evaluasi 25 skenario...", color = TextMuted, fontSize = 12.sp, fontFamily = UiSans)
                    }
                }

                errorMsg?.let { err ->
                    Text(err, color = SemanticDanger, fontSize = 11.5.sp, fontFamily = UiSans)
                }

                report?.let { rep ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElevatedSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                rep.summaryText,
                                color = TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = UiSans
                            )
                            rep.domainScores.forEach { (domain, score) ->
                                val barColor = when {
                                    score >= 70 -> SemanticSuccess
                                    score >= 45 -> SemanticWarning
                                    else -> SemanticDanger
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(domain, color = TextMuted, fontSize = 11.sp, fontFamily = UiSans)
                                    Text("$score%", color = barColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = UiSans)
                                }
                            }
                        }
                    }

                    rep.results.forEach { item ->
                        val itemScore = if (rep.mode == "live_llm") {
                            item.llmConfidencePercent ?: item.offlineCoveragePercent
                        } else {
                            item.offlineCoveragePercent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElevatedSurface.copy(alpha = 0.65f))
                                .padding(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        item.scenario.domain,
                                        color = AccentBlue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = UiSans
                                    )
                                    Text(
                                        "$itemScore%",
                                        color = if (itemScore >= 70) SemanticSuccess else if (itemScore >= 45) SemanticWarning else SemanticDanger,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = UiSans
                                    )
                                }
                                Text(item.scenario.situation, color = TextPrimary, fontSize = 11.5.sp, fontFamily = UiSans)
                                Text(item.predictedBehavior, color = TextMuted, fontSize = 11.sp, fontFamily = UiSans)
                                if (item.rationale.isNotBlank()) {
                                    Text(item.rationale, color = TextSubtle, fontSize = 10.sp, fontFamily = UiSans)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", color = AccentBlue, fontFamily = UiSans, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun EdgeDetailRow(
    text: String,
    evidence: String,
    weight: Float,
    isHistorical: Boolean,
    onMarkHistorical: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElevatedSurface)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isHistorical) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(SemanticWarning.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "HISTORIS",
                            fontFamily = UiSans,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SemanticWarning
                        )
                    }
                }
                Text(
                    text = text,
                    fontFamily = UiSans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isHistorical) TextMuted else TextPrimary
                )
            }
            val meta = buildString {
                append("bobot ${"%.1f".format(Locale.US, weight)}")
                if (evidence.isNotBlank()) append(" • $evidence")
            }
            Text(
                text = meta,
                fontFamily = UiSans,
                fontSize = 11.sp,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!isHistorical) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = "Tandai historis (pola lama)",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(onClick = onMarkHistorical)
                )
            }
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Hapus relasi",
                tint = TextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onDelete)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddKgConnectionDialog(
    defaultFromLabel: String,
    onSave: (fromType: String, fromLabel: String, relation: String, toType: String, toLabel: String, evidence: String, replacesLabel: String) -> Unit,
    onDismiss: () -> Unit
) {
    val nodeTypes = remember { KnowledgeGraphHelpers.VALID_NODE_TYPES.toList() }
    val relations = remember { KnowledgeGraphHelpers.VALID_RELATIONS.toList() }

    var fromType by remember { mutableStateOf("SELF") }
    var fromLabel by remember { mutableStateOf(defaultFromLabel) }
    var relation by remember { mutableStateOf("MENGUTAMAKAN") }
    var toType by remember { mutableStateOf("NILAI") }
    var toLabel by remember { mutableStateOf("") }
    var evidence by remember { mutableStateOf("") }
    var replacesLabel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SecondarySurface,
        title = { Text("Tambah Koneksi Otak Digital", color = TextPrimary, fontFamily = UiSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Dari Simpul (${formatNodeTypeLabel(fromType)})", color = TextMuted, fontSize = 11.sp, fontFamily = UiSans)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    nodeTypes.forEach { t ->
                        val active = t == fromType
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface)
                                .border(1.dp, if (active) AccentBlue else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable {
                                    fromType = t
                                    if (t == "SELF" && fromLabel.isBlank()) fromLabel = defaultFromLabel
                                }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(formatNodeTypeLabel(t), fontSize = 10.sp, color = if (active) AccentBlue else TextMuted, fontFamily = UiSans)
                        }
                    }
                }
                KgDialogField(value = fromLabel, onValueChange = { fromLabel = it }, placeholder = "Label sumber (mis. $defaultFromLabel)")

                Text("Relasi", color = TextMuted, fontSize = 11.sp, fontFamily = UiSans)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    relations.forEach { rel ->
                        val active = rel == relation
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface)
                                .border(1.dp, if (active) AccentBlue else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { relation = rel }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(rel, fontSize = 10.sp, color = if (active) AccentBlue else TextMuted, fontFamily = UiSans)
                        }
                    }
                }

                Text("Ke Simpul (${formatNodeTypeLabel(toType)})", color = TextMuted, fontSize = 11.sp, fontFamily = UiSans)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    nodeTypes.filter { it != "SELF" }.forEach { t ->
                        val active = t == toType
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) AccentBlue.copy(alpha = 0.2f) else ElevatedSurface)
                                .border(1.dp, if (active) AccentBlue else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { toType = t }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(formatNodeTypeLabel(t), fontSize = 10.sp, color = if (active) AccentBlue else TextMuted, fontFamily = UiSans)
                        }
                    }
                }
                KgDialogField(value = toLabel, onValueChange = { toLabel = it }, placeholder = "Label tujuan (mis. Kemandirian Data)")
                KgDialogField(value = evidence, onValueChange = { evidence = it }, placeholder = "Bukti / konteks singkat (opsional)")
                KgDialogField(value = replacesLabel, onValueChange = { replacesLabel = it }, placeholder = "Menggantikan pola lama (opsional, mis. Begadang)")
            }
        },
        confirmButton = {
            TextButton(
                enabled = fromLabel.isNotBlank() && toLabel.isNotBlank(),
                onClick = { onSave(fromType, fromLabel, relation, toType, toLabel, evidence, replacesLabel) }
            ) {
                Text("Simpan", color = AccentBlue, fontFamily = UiSans, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextMuted, fontFamily = UiSans)
            }
        }
    )
}

@Composable
private fun EditKgNodeDialog(
    node: KgNodeEntity,
    onSave: (label: String, summary: String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember(node.id) { mutableStateOf(node.label) }
    var summary by remember(node.id) { mutableStateOf(node.summary) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SecondarySurface,
        title = { Text("Edit Simpul", color = TextPrimary, fontFamily = UiSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Label (${formatNodeTypeLabel(node.nodeType)})", color = TextMuted, fontSize = 11.sp, fontFamily = UiSans)
                KgDialogField(value = label, onValueChange = { label = it }, placeholder = "Nama simpul")
                Text("Ringkasan / Catatan", color = TextMuted, fontSize = 11.sp, fontFamily = UiSans)
                KgDialogField(value = summary, onValueChange = { summary = it }, placeholder = "Konteks atau penjelasan simpul", singleLine = false)
            }
        },
        confirmButton = {
            TextButton(enabled = label.isNotBlank(), onClick = { onSave(label, summary) }) {
                Text("Simpan", color = AccentBlue, fontFamily = UiSans, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextMuted, fontFamily = UiSans)
            }
        }
    )
}

@Composable
private fun KgDialogField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp, fontFamily = UiSans),
            cursorBrush = SolidColor(AccentBlue),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = TextSubtle, fontSize = 12.5.sp, fontFamily = UiSans)
                }
                inner()
            }
        )
    }
}

private fun filterVisibleGraph(
    nodes: List<KgNodeEntity>,
    edges: List<KgEdgeEntity>,
    degreeMap: Map<Long, Int>,
    query: String,
    typeFilter: String,
    selectedNodeId: Long?,
    includeHistorical: Boolean = false
): Triple<List<KgNodeEntity>, List<KgEdgeEntity>, Boolean> {
    if (nodes.isEmpty()) return Triple(emptyList(), emptyList(), false)
    val baseNodes = if (includeHistorical) nodes else nodes.filter { KnowledgeGraphHelpers.isActiveNode(it) || it.id == selectedNodeId }
    if (baseNodes.isEmpty()) return Triple(emptyList(), emptyList(), false)
    val baseNodeIds = baseNodes.map { it.id }.toSet()
    val baseEdges = if (includeHistorical) {
        edges
    } else {
        edges.filter {
            !KnowledgeGraphHelpers.isHistoricalEdge(it) &&
                it.fromNodeId in baseNodeIds &&
                it.toNodeId in baseNodeIds
        }
    }

    val q = query.trim().lowercase()
    val isFiltered = q.isNotBlank() || typeFilter != "SEMUA"
    val selectedSet = mutableSetOf<Long>()
    val selfNode = baseNodes.firstOrNull { it.nodeType == "SELF" }

    val matchingNodes = if (!isFiltered) {
        baseNodes.sortedByDescending {
            (if (it.nodeType == "SELF") 100f else 0f) + it.salience * 3f + (degreeMap[it.id] ?: 0) * 0.4f
        }
    } else {
        baseNodes.filter { n ->
            val typeOk = typeFilter == "SEMUA" || n.nodeType == typeFilter
            val qOk = q.isBlank() ||
                n.label.lowercase().contains(q) ||
                n.summary.lowercase().contains(q) ||
                n.nodeType.lowercase().contains(q)
            typeOk && qOk
        }.sortedByDescending {
            (if (it.nodeType == "SELF") 100f else 0f) + it.salience * 3f + (degreeMap[it.id] ?: 0) * 0.4f
        }
    }

    val effectiveCap = if (isFiltered) 120 else 35
    val hardCeiling = effectiveCap + 20
    val capped = matchingNodes.size > effectiveCap
    matchingNodes.take(effectiveCap).forEach { selectedSet.add(it.id) }

    if (selectedNodeId != null && selectedNodeId in baseNodeIds) {
        selectedSet.add(selectedNodeId)
        for (e in baseEdges) {
            if (selectedSet.size >= hardCeiling) break
            if (e.fromNodeId == selectedNodeId) selectedSet.add(e.toNodeId)
            if (e.toNodeId == selectedNodeId) selectedSet.add(e.fromNodeId)
        }
    }
    if (isFiltered && selectedSet.isNotEmpty() && selfNode != null) {
        selectedSet.add(selfNode.id)
        for (e in baseEdges) {
            if (selectedSet.size >= hardCeiling) break
            if (e.fromNodeId in selectedSet || e.toNodeId in selectedSet) {
                selectedSet.add(e.fromNodeId)
                selectedSet.add(e.toNodeId)
            }
        }
    }

    val visibleNodes = baseNodes.filter { it.id in selectedSet }
    val visibleEdges = baseEdges.filter { it.fromNodeId in selectedSet && it.toNodeId in selectedSet }
    return Triple(visibleNodes, visibleEdges, capped)
}

private fun computeWorldLayout(nodes: List<KgNodeEntity>): List<PositionedKgNode> {
    if (nodes.isEmpty()) return emptyList()
    val sectorOrder = listOf(
        "NILAI", "POLA_PIKIR", "KEBIASAAN", "TUJUAN",
        "EMOSI_PEMICU", "KEPUTUSAN", "MINAT", "ORANG", "KONTEKS"
    )
    val sectorCount = sectorOrder.size.toFloat()
    val xs = FloatArray(nodes.size)
    val ys = FloatArray(nodes.size)
    val radii = FloatArray(nodes.size)
    val isSelf = BooleanArray(nodes.size)

    val byType = nodes.groupBy { it.nodeType }
    val typeCounters = mutableMapOf<String, Int>()

    nodes.forEachIndexed { idx, node ->
        radii[idx] = if (node.nodeType == "SELF") {
            34f
        } else {
            (18f + node.salience * 14f).coerceIn(18f, 34f)
        }
        if (node.nodeType == "SELF") {
            isSelf[idx] = true
            xs[idx] = 0f
            ys[idx] = 0f
        } else {
            val sIdx = sectorOrder.indexOf(node.nodeType).let { if (it < 0) 8 else it }
            val countInSector = (byType[node.nodeType]?.size ?: 1).coerceAtLeast(1)
            val localPos = typeCounters[node.nodeType] ?: 0
            typeCounters[node.nodeType] = localPos + 1

            val sectorCenterAngle = (sIdx / sectorCount) * (2.0 * PI) - PI / 2.0
            val spreadAngle = ((localPos + 0.5f) / countInSector - 0.5f) * (2.0 * PI / sectorCount) * 0.85
            val angle = sectorCenterAngle + spreadAngle
            val ringRadius = 185f + (localPos % 3) * 85f
            xs[idx] = (cos(angle) * ringRadius).toFloat()
            ys[idx] = (sin(angle) * ringRadius).toFloat()
        }
    }

    // Repulsion pass to reduce overlapping labels
    val minDist = 112f
    repeat(22) {
        for (i in nodes.indices) {
            if (isSelf[i]) continue
            for (j in i + 1 until nodes.size) {
                val dx = xs[j] - xs[i]
                val dy = ys[j] - ys[i]
                val d = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                if (d < minDist) {
                    val push = (minDist - d) * 0.35f
                    val ux = dx / d
                    val uy = dy / d
                    xs[i] -= ux * push
                    ys[i] -= uy * push
                    if (!isSelf[j]) {
                        xs[j] += ux * push
                        ys[j] += uy * push
                    }
                }
            }
        }
    }

    return nodes.mapIndexed { idx, n ->
        PositionedKgNode(
            node = n,
            worldX = xs[idx],
            worldY = ys[idx],
            radiusPx = radii[idx]
        )
    }
}

private fun nodeTypeColor(nodeType: String): Color = when (nodeType) {
    "SELF" -> AccentBlue
    "NILAI" -> AppKronik
    "POLA_PIKIR" -> AppLinkKu
    "KEBIASAAN" -> AppDailyOs
    "TUJUAN" -> SemanticSuccess
    "EMOSI_PEMICU" -> SemanticWarning
    "KEPUTUSAN" -> AppCuciMoneyLight
    "MINAT" -> AppIdeKu
    "ORANG" -> Color(0xFFEC4899)
    else -> TextMuted
}

private fun formatNodeTypeLabel(nodeType: String): String = when (nodeType) {
    "SEMUA" -> "Semua"
    "SELF" -> "Diri (Self)"
    "NILAI" -> "Nilai"
    "POLA_PIKIR" -> "Pola Pikir"
    "KEBIASAAN" -> "Kebiasaan"
    "TUJUAN" -> "Tujuan"
    "EMOSI_PEMICU" -> "Pemicu Emosi"
    "KEPUTUSAN" -> "Keputusan"
    "MINAT" -> "Minat"
    "ORANG" -> "Orang"
    "KONTEKS" -> "Konteks"
    else -> nodeType
}
