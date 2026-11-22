package com.ecosystemku.assistant.ui.apps.dailyos

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ecosystemku.assistant.data.local.DailyOsHabitEntity
import com.ecosystemku.assistant.data.local.DailyOsTodoEntity
import com.ecosystemku.assistant.data.network.DailyOsAiResult
import com.ecosystemku.assistant.data.network.DailyOsParsedTodoDto
import com.ecosystemku.assistant.data.network.DailyOsRekomTodoDto
import com.ecosystemku.assistant.ui.theme.UiSans
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private sealed interface ConfirmRequest {
    data class DeleteHabit(val habit: DailyOsHabitEntity) : ConfirmRequest
    data class DeleteTodo(val todo: DailyOsTodoEntity) : ConfirmRequest
    data class ClearDayHistory(val dateStr: String) : ConfirmRequest
    data class RestoreJson(val raw: String) : ConfirmRequest
    data object ResetAll : ConfirmRequest
}

@Composable
fun DailyOsScreen(
    uiState: DailyOsUiState,
    actions: DailyOsActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pal = remember(uiState.isDarkTheme) { DailyOsHelpers.palette(uiState.isDarkTheme) }

    var activeTab by remember { mutableStateOf(DailyOsTab.HABIT) }
    var editingHabit by remember { mutableStateOf<DailyOsHabitEntity?>(null) }
    var showHabitModal by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<DailyOsTodoEntity?>(null) }
    var showTodoModal by remember { mutableStateOf(false) }
    var confirmReq by remember { mutableStateOf<ConfirmRequest?>(null) }

    // Stats state
    var chartRangeDays by remember { mutableIntStateOf(7) }
    var calMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedCalDate by remember { mutableStateOf(DailyOsHelpers.todayStr()) }

    // AI tab state
    var coachLoading by remember { mutableStateOf(false) }
    var coachText by remember { mutableStateOf<String?>(null) }
    var coachError by remember { mutableStateOf<String?>(null) }
    var smartInput by remember { mutableStateOf("") }
    var parseLoading by remember { mutableStateOf(false) }
    var parsedPreview by remember { mutableStateOf<List<DailyOsParsedTodoDto>?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var rekomLoading by remember { mutableStateOf(false) }
    var rekomPreview by remember { mutableStateOf<List<DailyOsRekomTodoDto>?>(null) }
    var rekomError by remember { mutableStateOf<String?>(null) }

    // Settings state
    var notifHabitInput by remember(uiState.notifHabitTime) { mutableStateOf(uiState.notifHabitTime) }
    var notifTodoInput by remember(uiState.notifTodoTime) { mutableStateOf(uiState.notifTodoTime) }

    BackHandler { onClose() }

    fun fetchCoach() {
        if (coachLoading) return
        coachLoading = true
        coachError = null
        scope.launch {
            when (val res = actions.onGetAiCoach()) {
                is DailyOsAiResult.Success -> coachText = res.data
                is DailyOsAiResult.Failure -> coachError = res.message
            }
            coachLoading = false
        }
    }

    LaunchedEffect(activeTab) {
        if (activeTab == DailyOsTab.AI && coachText == null && !coachLoading) {
            fetchCoach()
        }
    }

    fun shareExportFile(fileName: String, content: String, mimeType: String, title: String) {
        runCatching {
            val file = File(context.cacheDir, fileName)
            file.writeText(content)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        }.onFailure {
            Toast.makeText(context, "Gagal mengekspor file", Toast.LENGTH_SHORT).show()
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val raw = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (raw.isNullOrBlank()) {
                Toast.makeText(context, "Gagal membaca file JSON", Toast.LENGTH_SHORT).show()
            } else {
                confirmReq = ConfirmRequest.RestoreJson(raw)
            }
        }
    }

    val today = DailyOsHelpers.todayStr()
    val activeHabits = remember(uiState.habits) { uiState.habits.filter { !it.archived } }
    val totalXp = remember(uiState.habits) { DailyOsHelpers.recalculateXp(uiState.habits) }
    val level = (totalXp / 100) + 1
    val xpFill = (totalXp % 100) / 100f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pal.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pal.bg)
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
                            .background(pal.surface2)
                            .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali", tint = pal.text, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text(
                            text = "Daily OS",
                            fontFamily = UiSans,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.text,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = DailyOsHelpers.formatTopDate(),
                            fontFamily = UiSans,
                            fontSize = 11.5.sp,
                            color = pal.text2
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pal.surface2)
                        .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                        .clickable { actions.onToggleTheme() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Ganti Tema",
                        tint = pal.text2,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            HorizontalDivider(thickness = 1.dp, color = pal.border)

            // Main Content Area
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (activeTab) {
                    DailyOsTab.HABIT -> HabitTabContent(
                        pal = pal,
                        habits = activeHabits,
                        totalXp = totalXp,
                        level = level,
                        xpFill = xpFill,
                        today = today,
                        onToggleCheck = { hid, sid -> scope.launch { actions.onToggleHabitCheck(hid, sid) } },
                        onToggleCollapse = { hid -> scope.launch { actions.onToggleHabitCollapse(hid) } },
                        onEditHabit = { h ->
                            editingHabit = h
                            showHabitModal = true
                        },
                        onMoveHabit = { hid, delta -> scope.launch { actions.onMoveHabit(hid, delta) } },
                        onDeleteHabit = { h -> confirmReq = ConfirmRequest.DeleteHabit(h) }
                    )

                    DailyOsTab.TODO -> TodoTabContent(
                        pal = pal,
                        todos = uiState.todos,
                        doneSectionOpen = uiState.doneSectionOpen,
                        today = today,
                        onToggleTodo = { tid -> scope.launch { actions.onToggleTodo(tid) } },
                        onToggleSubtask = { tid, sid -> scope.launch { actions.onToggleTodoSubtask(tid, sid) } },
                        onToggleDoneOpen = actions.onToggleDoneSectionOpen,
                        onEditTodo = { t ->
                            editingTodo = t
                            showTodoModal = true
                        },
                        onMoveTodo = { tid, delta -> scope.launch { actions.onMoveTodo(tid, delta) } },
                        onDeleteTodo = { t -> confirmReq = ConfirmRequest.DeleteTodo(t) }
                    )

                    DailyOsTab.STATS -> StatsTabContent(
                        pal = pal,
                        allHabits = uiState.habits,
                        activeHabits = activeHabits,
                        todos = uiState.todos,
                        today = today,
                        chartRangeDays = chartRangeDays,
                        onSelectRange = { chartRangeDays = it },
                        calMonth = calMonth,
                        onChangeMonth = { delta -> calMonth = calMonth.plusMonths(delta.toLong()) },
                        selectedCalDate = selectedCalDate,
                        onSelectCalDate = { selectedCalDate = it },
                        onRequestClearDay = { ds -> confirmReq = ConfirmRequest.ClearDayHistory(ds) }
                    )

                    DailyOsTab.AI -> AiTabContent(
                        pal = pal,
                        activeHabits = activeHabits,
                        today = today,
                        coachLoading = coachLoading,
                        coachText = coachText,
                        coachError = coachError,
                        onRefreshCoach = { fetchCoach() },
                        smartInput = smartInput,
                        onSmartInputChange = { smartInput = it },
                        parseLoading = parseLoading,
                        parsedPreview = parsedPreview,
                        parseError = parseError,
                        onParseSmart = {
                            val clean = smartInput.trim()
                            if (clean.isEmpty()) {
                                Toast.makeText(context, "Ketik task terlebih dahulu", Toast.LENGTH_SHORT).show()
                            } else {
                                parseLoading = true
                                parseError = null
                                parsedPreview = null
                                scope.launch {
                                    when (val res = actions.onParseSmartTodo(clean)) {
                                        is DailyOsAiResult.Success -> parsedPreview = res.data
                                        is DailyOsAiResult.Failure -> parseError = res.message
                                    }
                                    parseLoading = false
                                }
                            }
                        },
                        onConfirmParsed = { tasks ->
                            scope.launch {
                                actions.onAddParsedTodos(tasks)
                                parsedPreview = null
                                smartInput = ""
                                Toast.makeText(context, "${tasks.size} task ditambahkan ke To-Do", Toast.LENGTH_SHORT).show()
                            }
                        },
                        rekomLoading = rekomLoading,
                        rekomPreview = rekomPreview,
                        rekomError = rekomError,
                        onRequestRekom = {
                            rekomLoading = true
                            rekomError = null
                            rekomPreview = null
                            scope.launch {
                                when (val res = actions.onGetAiRecommendations()) {
                                    is DailyOsAiResult.Success -> rekomPreview = res.data
                                    is DailyOsAiResult.Failure -> rekomError = res.message
                                }
                                rekomLoading = false
                            }
                        },
                        onDismissRekomItem = { idx ->
                            rekomPreview = rekomPreview?.filterIndexed { i, _ -> i != idx }?.takeIf { it.isNotEmpty() }
                        },
                        onConfirmRekom = { tasks ->
                            scope.launch {
                                actions.onAddRecommendedTodos(tasks)
                                rekomPreview = null
                                Toast.makeText(context, "${tasks.size} rekomendasi ditambahkan ke To-Do", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    DailyOsTab.SETTINGS -> SettingsTabContent(
                        pal = pal,
                        notifHabitTime = notifHabitInput,
                        notifTodoTime = notifTodoInput,
                        onNotifHabitChange = { notifHabitInput = it },
                        onNotifTodoChange = { notifTodoInput = it },
                        onSaveNotif = {
                            actions.onSaveNotifTimes(notifHabitInput.ifBlank { "07:00" }, notifTodoInput.ifBlank { "15:00" })
                            Toast.makeText(context, "Jadwal pengingat tersimpan", Toast.LENGTH_SHORT).show()
                        },
                        onExportSummary = { kind, offset ->
                            scope.launch {
                                val (fileName, md) = actions.onBuildSummaryMarkdown(kind, offset)
                                shareExportFile(fileName, md, "text/markdown", "Export Summary")
                            }
                        },
                        onBackupJson = {
                            scope.launch {
                                val json = actions.onBuildExportJson()
                                shareExportFile("dailyos-backup.json", json, "application/json", "Backup Daily OS")
                            }
                        },
                        onRestoreJson = { importJsonLauncher.launch(arrayOf("application/json", "*/*")) },
                        onExportCsv = {
                            scope.launch {
                                val csv = actions.onExportCsv()
                                if (csv == null) {
                                    Toast.makeText(context, "Belum ada habit untuk diekspor", Toast.LENGTH_SHORT).show()
                                } else {
                                    shareExportFile("dailyos-habits.csv", csv, "text/csv", "Export CSV Daily OS")
                                }
                            }
                        },
                        onRequestReset = { confirmReq = ConfirmRequest.ResetAll }
                    )
                }

                // FAB for Habit & To-Do tabs
                if (activeTab == DailyOsTab.HABIT || activeTab == DailyOsTab.TODO) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(pal.accent)
                            .clickable {
                                if (activeTab == DailyOsTab.HABIT) {
                                    editingHabit = null
                                    showHabitModal = true
                                } else {
                                    editingTodo = null
                                    showTodoModal = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Tambah", tint = pal.accentFg, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Internal Bottom Nav Bar (5 tabs)
            HorizontalDivider(thickness = 1.dp, color = pal.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pal.surface)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val navItems = listOf(
                    Triple(DailyOsTab.HABIT, "Habit", Icons.Outlined.CheckBox),
                    Triple(DailyOsTab.TODO, "To-Do", Icons.AutoMirrored.Outlined.FormatListBulleted),
                    Triple(DailyOsTab.STATS, "Statistik", Icons.Outlined.BarChart),
                    Triple(DailyOsTab.AI, "AI", Icons.Outlined.AutoAwesome),
                    Triple(DailyOsTab.SETTINGS, "Setting", Icons.Outlined.Settings)
                )
                navItems.forEach { (tab, label, icon) ->
                    val active = activeTab == tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activeTab = tab }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (active) pal.text else pal.text3,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = label,
                            fontFamily = UiSans,
                            fontSize = 10.5.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (active) pal.text else pal.text3
                        )
                    }
                }
            }
        }
    }

    // Modals
    if (showHabitModal) {
        HabitEditorModal(
            pal = pal,
            editHabit = editingHabit,
            onDismiss = { showHabitModal = false },
            onSave = { editId, name, category, color, icon, activeDays, subTasks ->
                scope.launch {
                    actions.onSaveHabit(editId, name, category, color, icon, activeDays, subTasks)
                    showHabitModal = false
                }
            }
        )
    }

    if (showTodoModal) {
        TodoEditorModal(
            pal = pal,
            editTodo = editingTodo,
            onDismiss = { showTodoModal = false },
            onSave = { editId, title, priority, deadline, subtaskTexts ->
                scope.launch {
                    actions.onSaveTodo(editId, title, priority, deadline, subtaskTexts)
                    showTodoModal = false
                }
            }
        )
    }

    confirmReq?.let { req ->
        when (req) {
            is ConfirmRequest.DeleteHabit -> DailyOsConfirmDialog(
                pal = pal,
                title = "Hapus Habit",
                message = "Yakin ingin menghapus \"${req.habit.name}\"? XP yang sudah didapat dan riwayatnya akan tetap tersimpan.",
                confirmLabel = "Hapus",
                isDanger = true,
                onDismiss = { confirmReq = null },
                onConfirm = {
                    scope.launch {
                        actions.onDeleteHabit(req.habit.id)
                        confirmReq = null
                    }
                }
            )
            is ConfirmRequest.DeleteTodo -> DailyOsConfirmDialog(
                pal = pal,
                title = "Hapus To-Do",
                message = "Yakin ingin menghapus task \"${req.todo.title}\"?",
                confirmLabel = "Hapus",
                isDanger = true,
                onDismiss = { confirmReq = null },
                onConfirm = {
                    scope.launch {
                        actions.onDeleteTodo(req.todo.id)
                        confirmReq = null
                    }
                }
            )
            is ConfirmRequest.ClearDayHistory -> DailyOsConfirmDialog(
                pal = pal,
                title = "Hapus Data Hari Ini",
                message = "Hapus semua catatan penyelesaian habit pada tanggal ${DailyOsHelpers.formatFullDate(req.dateStr)}? XP akan dihitung ulang secara otomatis.",
                confirmLabel = "Hapus",
                isDanger = true,
                onDismiss = { confirmReq = null },
                onConfirm = {
                    scope.launch {
                        val removed = actions.onClearDayHistory(req.dateStr)
                        confirmReq = null
                        Toast.makeText(context, "$removed aktivitas habit dihapus", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            is ConfirmRequest.RestoreJson -> DailyOsConfirmDialog(
                pal = pal,
                title = "Restore Backup JSON",
                message = "Data habit dan to-do saat ini akan diganti dengan data dari file backup. Lanjutkan?",
                confirmLabel = "Restore",
                isDanger = false,
                onDismiss = { confirmReq = null },
                onConfirm = {
                    scope.launch {
                        val res = actions.onImportJson(req.raw)
                        confirmReq = null
                        res.onSuccess { (hCount, tCount) ->
                            Toast.makeText(context, "Restore berhasil ($hCount habit, $tCount task)", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(context, "Format file backup tidak valid", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
            is ConfirmRequest.ResetAll -> DailyOsConfirmDialog(
                pal = pal,
                title = "Reset Aplikasi",
                message = "Semua data habit, to-do, riwayat, dan XP akan dihapus permanen. Yakin?",
                confirmLabel = "Reset Semua",
                isDanger = true,
                onDismiss = { confirmReq = null },
                onConfirm = {
                    scope.launch {
                        actions.onResetAll()
                        coachText = null
                        parsedPreview = null
                        rekomPreview = null
                        confirmReq = null
                        Toast.makeText(context, "Semua data Daily OS direset", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

// ── 1. HABIT TAB ─────────────────────────────────────────────────────────────

@Composable
private fun HabitTabContent(
    pal: DailyOsThemePalette,
    habits: List<DailyOsHabitEntity>,
    totalXp: Int,
    level: Int,
    xpFill: Float,
    today: String,
    onToggleCheck: (String, String) -> Unit,
    onToggleCollapse: (String) -> Unit,
    onEditHabit: (DailyOsHabitEntity) -> Unit,
    onMoveHabit: (String, Int) -> Unit,
    onDeleteHabit: (DailyOsHabitEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.xpBg)
                    .border(1.dp, pal.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Consistency XP · Lvl $level",
                        fontFamily = UiSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.text
                    )
                    Text(
                        text = "$totalXp pts",
                        fontFamily = UiSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.text2
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(pal.border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(xpFill.coerceIn(0f, 1f))
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(pal.xp)
                    )
                }
            }
        }

        if (habits.isEmpty()) {
            item {
                DailyOsEmptyState(
                    pal = pal,
                    title = "Belum ada habit.",
                    subtitle = "Tap + untuk mulai."
                )
            }
        } else {
            items(habits, key = { it.id }) { habit ->
                HabitCard(
                    pal = pal,
                    habit = habit,
                    today = today,
                    onToggleCheck = onToggleCheck,
                    onToggleCollapse = onToggleCollapse,
                    onEdit = { onEditHabit(habit) },
                    onMoveUp = { onMoveHabit(habit.id, -1) },
                    onMoveDown = { onMoveHabit(habit.id, 1) },
                    onDelete = { onDeleteHabit(habit) }
                )
            }
        }
    }
}

@Composable
private fun HabitCard(
    pal: DailyOsThemePalette,
    habit: DailyOsHabitEntity,
    today: String,
    onToggleCheck: (String, String) -> Unit,
    onToggleCollapse: (String) -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val scheduled = DailyOsHelpers.isHabitScheduled(habit, today)
    val subTasks = remember(habit.subTasksJson) { DailyOsHelpers.parseSubTasks(habit.subTasksJson) }
    val history = remember(habit.historyJson) { DailyOsHelpers.parseHistory(habit.historyJson) }
    val todayLogs = history[today].orEmpty().map { it.id }.toSet()
    val activeDays = remember(habit.activeDaysJson) { DailyOsHelpers.parseActiveDays(habit.activeDaysJson) }
    var menuOpen by remember { mutableStateOf(false) }

    val badgeBgRaw = DailyOsHelpers.parseColor(habit.color.ifBlank { "#1A1917" })
    val invertWhiteInLight = !pal.isDark && DailyOsHelpers.isLightColor(habit.color)
    val invertBlackInDark = pal.isDark && DailyOsHelpers.isDarkColor(habit.color)
    val badgeBg = when {
        invertWhiteInLight -> pal.surface2
        invertBlackInDark -> Color(0xFFF0F0F0)
        else -> badgeBgRaw
    }
    val badgeTint = when {
        invertWhiteInLight -> pal.text
        invertBlackInDark -> Color(0xFF0F0F0F)
        DailyOsHelpers.isLightColor(habit.color) -> Color(0xFF1A1917)
        else -> Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (scheduled) 1f else 0.48f)
            .clip(RoundedCornerShape(12.dp))
            .background(pal.surface)
            .border(1.dp, pal.border, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleCollapse(habit.id) }
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeBg)
                    .border(if (invertWhiteInLight) 1.dp else 0.dp, pal.border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DailyOsHelpers.iconFor(habit.icon),
                    contentDescription = null,
                    tint = badgeTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = habit.name,
                    fontFamily = UiSans,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val catTxt = habit.category.ifBlank { "General" }
                val schedTxt = if (activeDays.isEmpty()) "Setiap hari" else activeDays.sorted().joinToString(", ") { DailyOsHelpers.dayShortLabel(it) }
                Text(
                    text = "$catTxt · $schedTxt",
                    fontFamily = UiSans,
                    fontSize = 11.5.sp,
                    color = pal.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!scheduled) {
                    Text(
                        text = "Tidak dijadwalkan hari ini",
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        color = pal.text3
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = if (habit.collapsed) Icons.Outlined.ChevronRight else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = pal.text3,
                    modifier = Modifier.size(18.dp)
                )
                Box {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .clickable { menuOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.MoreHoriz, contentDescription = "Menu", tint = pal.text2, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        containerColor = pal.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit", color = pal.text, fontSize = 13.sp) },
                            onClick = { menuOpen = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Geser Naik", color = pal.text, fontSize = 13.sp) },
                            onClick = { menuOpen = false; onMoveUp() }
                        )
                        DropdownMenuItem(
                            text = { Text("Geser Turun", color = pal.text, fontSize = 13.sp) },
                            onClick = { menuOpen = false; onMoveDown() }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus", color = pal.urgent, fontSize = 13.sp) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
        }

        if (!habit.collapsed) {
            HorizontalDivider(thickness = 1.dp, color = pal.border)
            val rows = if (subTasks.isEmpty()) listOf(DailyOsSubTask("main", "Selesaikan Habit Ini")) else subTasks
            Column {
                rows.forEachIndexed { idx, sub ->
                    val checked = sub.id in todayLogs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (scheduled) Modifier.clickable { onToggleCheck(habit.id, sub.id) }
                                else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(19.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (checked) pal.accent else Color.Transparent)
                                .border(1.5.dp, if (checked) pal.accent else pal.border, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (checked) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = pal.accentFg, modifier = Modifier.size(12.dp))
                            }
                        }
                        Text(
                            text = sub.text,
                            fontFamily = UiSans,
                            fontSize = 13.5.sp,
                            color = if (checked) pal.text3 else pal.text,
                            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                    if (idx < rows.lastIndex) {
                        HorizontalDivider(thickness = 1.dp, color = pal.border)
                    }
                }
            }
        }
    }
}

// ── 2. TO-DO TAB ─────────────────────────────────────────────────────────────

@Composable
private fun TodoTabContent(
    pal: DailyOsThemePalette,
    todos: List<DailyOsTodoEntity>,
    doneSectionOpen: Boolean,
    today: String,
    onToggleTodo: (String) -> Unit,
    onToggleSubtask: (String, String) -> Unit,
    onToggleDoneOpen: () -> Unit,
    onEditTodo: (DailyOsTodoEntity) -> Unit,
    onMoveTodo: (String, Int) -> Unit,
    onDeleteTodo: (DailyOsTodoEntity) -> Unit
) {
    val openMap = remember { mutableMapOf<String, Boolean>() }
    val pending = remember(todos) { todos.filter { !it.done } }
    val completed = remember(todos) { todos.filter { it.done } }
    val groups = remember(pending) {
        listOf(
            "urgent" to "Hari Ini",
            "soon" to "Minggu Ini",
            "normal" to "Nanti"
        ).mapNotNull { (p, label) ->
            val items = pending.filter { it.priority == p }
            if (items.isNotEmpty()) Triple(p, label, items) else null
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 84.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (todos.isEmpty()) {
            item {
                DailyOsEmptyState(
                    pal = pal,
                    title = "Belum ada task.",
                    subtitle = "Tap + atau gunakan AI Smart To-Do."
                )
            }
        } else {
            groups.forEach { (_, groupLabel, groupItems) ->
                item(key = "hdr_$groupLabel") {
                    Text(
                        text = groupLabel.uppercase(),
                        fontFamily = UiSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pal.text3,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 2.dp)
                    )
                }
                items(groupItems, key = { it.id }) { todo ->
                    TodoCard(
                        pal = pal,
                        todo = todo,
                        today = today,
                        subOpenState = openMap[todo.id] ?: false,
                        onToggleSubOpen = { openMap[todo.id] = !(openMap[todo.id] ?: false) },
                        onToggleTodo = { onToggleTodo(todo.id) },
                        onToggleSubtask = { sid -> onToggleSubtask(todo.id, sid) },
                        onEdit = { onEditTodo(todo) },
                        onMoveUp = { onMoveTodo(todo.id, -1) },
                        onMoveDown = { onMoveTodo(todo.id, 1) },
                        onDelete = { onDeleteTodo(todo) }
                    )
                }
            }

            if (completed.isNotEmpty()) {
                item(key = "done_toggle") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleDoneOpen)
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (doneSectionOpen) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = pal.text3,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SELESAI (${completed.size})",
                            fontFamily = UiSans,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = pal.text3,
                            letterSpacing = 0.7.sp
                        )
                    }
                }
                if (doneSectionOpen) {
                    items(completed, key = { "done_${it.id}" }) { todo ->
                        TodoCard(
                            pal = pal,
                            todo = todo,
                            today = today,
                            subOpenState = openMap[todo.id] ?: false,
                            onToggleSubOpen = { openMap[todo.id] = !(openMap[todo.id] ?: false) },
                            onToggleTodo = { onToggleTodo(todo.id) },
                            onToggleSubtask = { sid -> onToggleSubtask(todo.id, sid) },
                            onEdit = { onEditTodo(todo) },
                            onMoveUp = { onMoveTodo(todo.id, -1) },
                            onMoveDown = { onMoveTodo(todo.id, 1) },
                            onDelete = { onDeleteTodo(todo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoCard(
    pal: DailyOsThemePalette,
    todo: DailyOsTodoEntity,
    today: String,
    subOpenState: Boolean,
    onToggleSubOpen: () -> Unit,
    onToggleTodo: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val subs = remember(todo.subtasksJson) { DailyOsHelpers.parseTodoSubtasks(todo.subtasksJson) }
    val doneSubs = subs.count { it.done }
    val overdue = !todo.done && DailyOsHelpers.isOverdue(todo.deadline, today)
    var subOpen by remember(todo.id) { mutableStateOf(subOpenState) }
    var menuOpen by remember { mutableStateOf(false) }

    val (priLabel, priFg, priBg) = when (todo.priority) {
        "urgent" -> Triple("Hari Ini", pal.urgent, pal.urgentBg)
        "soon" -> Triple("Minggu Ini", pal.soon, pal.soonBg)
        else -> Triple("Nanti", pal.normal, pal.normalBg)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (todo.done) 0.58f else 1f)
            .clip(RoundedCornerShape(11.dp))
            .background(pal.surface)
            .border(1.dp, pal.border, RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (todo.done) pal.accent else Color.Transparent)
                    .border(1.5.dp, if (todo.done) pal.accent else pal.border, RoundedCornerShape(6.dp))
                    .clickable(onClick = onToggleTodo),
                contentAlignment = Alignment.Center
            ) {
                if (todo.done) {
                    Icon(Icons.Outlined.Check, contentDescription = null, tint = pal.accentFg, modifier = Modifier.size(13.dp))
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = todo.title,
                    fontFamily = UiSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (todo.done) pal.text3 else pal.text,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(priBg)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(priLabel, fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = priFg)
                    }
                    if (todo.deadline.isNotBlank()) {
                        Text(
                            text = (if (overdue) "! " else "") + DailyOsHelpers.formatShortDate(todo.deadline),
                            fontFamily = UiSans,
                            fontSize = 11.sp,
                            fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (overdue) pal.urgent else pal.text2
                        )
                    }
                    if (subs.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .clickable {
                                    subOpen = !subOpen
                                    onToggleSubOpen()
                                }
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "$doneSubs/${subs.size} subtask",
                                fontFamily = UiSans,
                                fontSize = 11.sp,
                                color = pal.text2
                            )
                            Icon(
                                imageVector = if (subOpen) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = pal.text3,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Box {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { menuOpen = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.MoreHoriz, contentDescription = "Menu", tint = pal.text2, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = pal.surface
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit", color = pal.text, fontSize = 13.sp) },
                        onClick = { menuOpen = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Geser Naik", color = pal.text, fontSize = 13.sp) },
                        onClick = { menuOpen = false; onMoveUp() }
                    )
                    DropdownMenuItem(
                        text = { Text("Geser Turun", color = pal.text, fontSize = 13.sp) },
                        onClick = { menuOpen = false; onMoveDown() }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus", color = pal.urgent, fontSize = 13.sp) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }

        if (subs.isNotEmpty() && subOpen) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = pal.border)
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier.padding(start = 28.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                subs.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSubtask(s.id) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (s.done) pal.accent else Color.Transparent)
                                .border(1.5.dp, if (s.done) pal.accent else pal.border, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (s.done) {
                                Icon(Icons.Outlined.Check, contentDescription = null, tint = pal.accentFg, modifier = Modifier.size(10.dp))
                            }
                        }
                        Text(
                            text = s.text,
                            fontFamily = UiSans,
                            fontSize = 12.5.sp,
                            color = if (s.done) pal.text3 else pal.text2,
                            textDecoration = if (s.done) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
            }
        }
    }
}

// ── 3. STATISTIK TAB ─────────────────────────────────────────────────────────

@Composable
private fun StatsTabContent(
    pal: DailyOsThemePalette,
    allHabits: List<DailyOsHabitEntity>,
    activeHabits: List<DailyOsHabitEntity>,
    todos: List<DailyOsTodoEntity>,
    today: String,
    chartRangeDays: Int,
    onSelectRange: (Int) -> Unit,
    calMonth: YearMonth,
    onChangeMonth: (Int) -> Unit,
    selectedCalDate: String,
    onSelectCalDate: (String) -> Unit,
    onRequestClearDay: (String) -> Unit
) {
    val streak = remember(allHabits, today) { DailyOsHelpers.calculateOverallStreak(allHabits, today) }
    val todayProg = remember(allHabits, today) { DailyOsHelpers.calculateTodayProgress(allHabits, today) }
    val avg7 = remember(allHabits, today) { DailyOsHelpers.calculate7DayAvgPct(allHabits, today) }
    val trendPoints = remember(allHabits, chartRangeDays, today) {
        DailyOsHelpers.calculateTrendPoints(allHabits, chartRangeDays, today)
    }

    val doneTodos = todos.count { it.done }
    val totalTodos = todos.size
    val todoPct = if (totalTodos == 0) 0 else ((doneTodos.toDouble() / totalTodos) * 100.0).roundToInt()
    val overdueCount = todos.count { !it.done && DailyOsHelpers.isOverdue(it.deadline, today) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Habit Stats
        item { StatsSectionLabel("HABIT", pal) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBoxCard("${activeHabits.size}", "Total Habit", pal, Modifier.weight(1f))
                    StatBoxCard("$streak", "Streak Hari", pal, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBoxCard("${todayProg[2]}%", "Hari Ini", pal, Modifier.weight(1f))
                    StatBoxCard("$avg7%", "Rata-rata 7H", pal, Modifier.weight(1f))
                }
            }
        }

        // Konsistensi Trend Chart
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.surface)
                    .border(1.dp, pal.border, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Konsistensi", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pal.text)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(7 to "7 Hari", 30 to "30 Hari", 90 to "3 Bulan").forEach { (days, lbl) ->
                            val sel = chartRangeDays == days
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (sel) pal.accent else pal.surface2)
                                    .clickable { onSelectRange(days) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(lbl, fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = if (sel) pal.accentFg else pal.text2)
                            }
                        }
                    }
                }

                ConsistencyTrendCanvas(points = trendPoints, pal = pal)
            }
        }

        // Kalender Bulanan
        item {
            CalendarCard(
                pal = pal,
                allHabits = allHabits,
                calMonth = calMonth,
                today = today,
                selectedDate = selectedCalDate,
                onChangeMonth = onChangeMonth,
                onSelectDate = onSelectCalDate,
                onRequestClearDay = onRequestClearDay
            )
        }

        // Section To-Do Stats
        item { StatsSectionLabel("TO-DO", pal, modifier = Modifier.padding(top = 4.dp)) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBoxCard("$totalTodos", "Total Task", pal, Modifier.weight(1f))
                    StatBoxCard("$doneTodos", "Selesai", pal, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBoxCard("$todoPct%", "Completion", pal, Modifier.weight(1f))
                    StatBoxCard("$overdueCount", "Overdue", pal, Modifier.weight(1f), isDanger = overdueCount > 0)
                }
            }
        }

        // Prioritas Breakdown
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.surface)
                    .border(1.dp, pal.border, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Prioritas", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pal.text)
                val rows = listOf(
                    Triple("urgent", "Hari Ini (Urgent)", pal.urgent),
                    Triple("soon", "Minggu Ini (Soon)", pal.soon),
                    Triple("normal", "Nanti (Normal)", pal.normal)
                )
                rows.forEach { (key, label, barColor) ->
                    val tot = todos.count { it.priority == key }
                    val dn = todos.count { it.priority == key && it.done }
                    val ratio = if (tot == 0) 0f else (dn.toFloat() / tot.toFloat()).coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, fontFamily = UiSans, fontSize = 12.sp, color = pal.text2)
                            Text("$dn/$tot", fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.text)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(pal.surface2)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(barColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsistencyTrendCanvas(
    points: List<Pair<String, Int>>,
    pal: DailyOsThemePalette
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .padding(top = 6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val leftPad = 6.dp.toPx()
                val rightPad = 6.dp.toPx()
                val topPad = 8.dp.toPx()
                val bottomPad = 8.dp.toPx()
                val w = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                val h = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                // Horizontal grid lines (0%, 50%, 100%)
                listOf(0f, 0.5f, 1f).forEach { frac ->
                    val y = topPad + h * (1f - frac)
                    drawLine(
                        color = pal.border,
                        start = Offset(leftPad, y),
                        end = Offset(leftPad + w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (points.size >= 2) {
                    val stepX = w / (points.size - 1).coerceAtLeast(1)
                    val coords = points.mapIndexed { idx, (_, pct) ->
                        val x = leftPad + stepX * idx
                        val y = topPad + h * (1f - (pct.coerceIn(0, 100) / 100f))
                        Offset(x, y)
                    }

                    val linePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) {
                            val p0 = coords[i - 1]
                            val p1 = coords[i]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(coords.last().x, topPad + h)
                        lineTo(coords.first().x, topPad + h)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(pal.accent.copy(alpha = 0.14f), pal.accent.copy(alpha = 0.0f)),
                            startY = topPad,
                            endY = topPad + h
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = pal.accent,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    if (points.size <= 14) {
                        coords.forEach { c ->
                            drawCircle(color = pal.accent, radius = 3.dp.toPx(), center = c)
                        }
                    }
                }
            }
        }
        if (points.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(points.first().first, fontFamily = UiSans, fontSize = 10.sp, color = pal.text3)
                Text(points[points.size / 2].first, fontFamily = UiSans, fontSize = 10.sp, color = pal.text3)
                Text("${points.last().first} (${points.last().second}%)", fontFamily = UiSans, fontSize = 10.sp, color = pal.text2)
            }
        }
    }
}

@Composable
private fun CalendarCard(
    pal: DailyOsThemePalette,
    allHabits: List<DailyOsHabitEntity>,
    calMonth: YearMonth,
    today: String,
    selectedDate: String,
    onChangeMonth: (Int) -> Unit,
    onSelectDate: (String) -> Unit,
    onRequestClearDay: (String) -> Unit
) {
    val idLocale = remember { Locale("in", "ID") }
    val monthTitle = remember(calMonth) {
        calMonth.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", idLocale))
            .replaceFirstChar { it.uppercase(idLocale) }
    }
    val parsedHistories = remember(allHabits) {
        allHabits.map { h -> h to DailyOsHelpers.parseHistory(h.historyJson) }
    }

    val firstDay = calMonth.atDay(1)
    // Mon = 0 .. Sun = 6
    val leadingBlanks = firstDay.dayOfWeek.value - 1
    val daysInMonth = calMonth.lengthOfMonth()
    val totalCells = leadingBlanks + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pal.surface)
            .border(1.dp, pal.border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Kalender Bulanan", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pal.text)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(pal.surface2)
                        .border(1.dp, pal.border, RoundedCornerShape(7.dp))
                        .clickable { onChangeMonth(-1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Bulan Lalu", tint = pal.text, modifier = Modifier.size(16.dp))
                }
                Text(monthTitle, fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = pal.text)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(pal.surface2)
                        .border(1.dp, pal.border, RoundedCornerShape(7.dp))
                        .clickable { onChangeMonth(1) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Bulan Depan", tint = pal.text, modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "S", "R", "K", "J", "S", "M").forEach { d ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(d, fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = pal.text3)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until 7) {
                        val cellIdx = r * 7 + c
                        val dayNum = cellIdx - leadingBlanks + 1
                        if (dayNum < 1 || dayNum > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f).height(38.dp))
                        } else {
                            val ds = calMonth.atDay(dayNum).toString()
                            val isToday = ds == today
                            val isSel = ds == selectedDate
                            val activeColors = parsedHistories.mapNotNull { (h, hist) ->
                                if (hist[ds].orEmpty().isNotEmpty()) h.color.ifBlank { "#1A1917" } else null
                            }.take(3)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) pal.accent else Color.Transparent)
                                    .border(
                                        width = if (isToday && !isSel) 1.dp else 0.dp,
                                        color = if (isToday && !isSel) pal.accent else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectDate(ds) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$dayNum",
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    fontWeight = if (isToday || isSel) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSel) pal.accentFg else pal.text
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    activeColors.forEach { hex ->
                                        val dotColor = if (isSel) pal.accentFg else DailyOsHelpers.parseColor(hex)
                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(dotColor))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Detail
        val dayActivities = parsedHistories.mapNotNull { (h, hist) ->
            val logs = hist[selectedDate].orEmpty()
            if (logs.isEmpty()) null else Triple(h, logs, DailyOsHelpers.parseSubTasks(h.subTasksJson))
        }

        HorizontalDivider(thickness = 1.dp, color = pal.border)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DailyOsHelpers.formatFullDate(selectedDate),
                    fontFamily = UiSans,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.text
                )
                if (dayActivities.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(pal.urgentBg)
                            .border(1.dp, pal.urgent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .clickable { onRequestClearDay(selectedDate) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Hapus Data Hari Ini", fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = pal.urgent)
                    }
                }
            }

            if (dayActivities.isEmpty()) {
                Text("Tidak ada aktivitas.", fontFamily = UiSans, fontSize = 12.sp, color = pal.text3)
            } else {
                dayActivities.forEach { (habit, logs, curSubs) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val dotCol = if (!pal.isDark && DailyOsHelpers.isLightColor(habit.color)) pal.text else DailyOsHelpers.parseColor(habit.color)
                        Box(modifier = Modifier.padding(top = 5.dp).size(8.dp).clip(CircleShape).background(dotCol))
                        Column(modifier = Modifier.weight(1f)) {
                            val title = if (habit.archived) "${habit.name} (dihapus)" else habit.name
                            Text(title, fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = pal.text)
                            if (logs.any { it.id != "main" }) {
                                logs.forEach { l ->
                                    val label = l.text.ifBlank { curSubs.firstOrNull { it.id == l.id }?.text ?: habit.name }
                                    Text("· $label", fontFamily = UiSans, fontSize = 11.sp, color = pal.text2)
                                }
                            } else {
                                Text("Selesai", fontFamily = UiSans, fontSize = 11.sp, color = pal.text2)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 4. AI TAB ────────────────────────────────────────────────────────────────

@Composable
private fun AiTabContent(
    pal: DailyOsThemePalette,
    activeHabits: List<DailyOsHabitEntity>,
    today: String,
    coachLoading: Boolean,
    coachText: String?,
    coachError: String?,
    onRefreshCoach: () -> Unit,
    smartInput: String,
    onSmartInputChange: (String) -> Unit,
    parseLoading: Boolean,
    parsedPreview: List<DailyOsParsedTodoDto>?,
    parseError: String?,
    onParseSmart: () -> Unit,
    onConfirmParsed: (List<DailyOsParsedTodoDto>) -> Unit,
    rekomLoading: Boolean,
    rekomPreview: List<DailyOsRekomTodoDto>?,
    rekomError: String?,
    onRequestRekom: () -> Unit,
    onDismissRekomItem: (Int) -> Unit,
    onConfirmRekom: (List<DailyOsRekomTodoDto>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI Coach Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.surface)
                    .border(1.dp, pal.border, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text("AI Coach", fontFamily = UiSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = pal.text)
                    Text("Briefing harian berdasarkan progres habit, to-do & fokus IdeKu.", fontFamily = UiSans, fontSize = 12.sp, color = pal.text2)
                }

                // Today's Habit Preview Rows
                if (activeHabits.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(pal.surface2)
                            .border(1.dp, pal.border, RoundedCornerShape(9.dp))
                    ) {
                        activeHabits.forEachIndexed { idx, h ->
                            val scheduled = DailyOsHelpers.isHabitScheduled(h, today)
                            val subs = DailyOsHelpers.parseSubTasks(h.subTasksJson)
                            val tc = if (subs.isNotEmpty()) subs.size else 1
                            val done = DailyOsHelpers.parseHistory(h.historyJson)[today].orEmpty().size
                            val streak = DailyOsHelpers.calculateHabitStreak(h, today)
                            val statusText = when {
                                !scheduled -> "—"
                                done >= tc -> "Done"
                                done > 0 -> "$done/$tc"
                                else -> "Belum"
                            }
                            val statusColor = when {
                                !scheduled -> pal.text3
                                done >= tc -> pal.aiAccent
                                else -> pal.text2
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 11.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = h.name,
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = pal.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (streak > 0) {
                                        Text("${streak}h", fontFamily = UiSans, fontSize = 11.sp, color = pal.soon)
                                    }
                                    Text(statusText, fontFamily = UiSans, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                                }
                            }
                            if (idx < activeHabits.lastIndex) {
                                HorizontalDivider(thickness = 1.dp, color = pal.border)
                            }
                        }
                    }
                }

                // Coach Green Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(pal.aiBg)
                        .border(1.dp, pal.aiBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI COACH BRIEFING",
                            fontFamily = UiSans,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = pal.aiAccent,
                            letterSpacing = 0.7.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(enabled = !coachLoading, onClick = onRefreshCoach),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = pal.aiAccent, modifier = Modifier.size(16.dp))
                        }
                    }

                    when {
                        coachLoading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = pal.aiAccent)
                            Text("Menganalisis data...", fontFamily = UiSans, fontSize = 12.5.sp, color = pal.aiAccent)
                        }
                        coachError != null -> Text("Gagal memuat briefing: $coachError", fontFamily = UiSans, fontSize = 12.5.sp, color = pal.urgent)
                        coachText != null -> Text(coachText, fontFamily = UiSans, fontSize = 13.sp, lineHeight = 20.sp, color = pal.text)
                        else -> Text("Ketuk refresh untuk meminta briefing dari AI Coach.", fontFamily = UiSans, fontSize = 12.5.sp, color = pal.text2)
                    }
                }
            }
        }

        // Smart To-Do Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.surface)
                    .border(1.dp, pal.border, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text("Smart To-Do", fontFamily = UiSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = pal.text)
                    Text("Tulis natural — AI ekstrak menjadi daftar task lengkap.", fontFamily = UiSans, fontSize = 12.sp, color = pal.text2)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pal.surface2)
                        .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    if (smartInput.isEmpty()) {
                        Text(
                            text = "Contoh: besok kerjakan laporan bulanan urgent sama beli kopi...",
                            fontFamily = UiSans,
                            fontSize = 13.sp,
                            color = pal.text3
                        )
                    }
                    BasicTextField(
                        value = smartInput,
                        onValueChange = onSmartInputChange,
                        textStyle = TextStyle(fontFamily = UiSans, fontSize = 13.sp, lineHeight = 19.sp, color = pal.text),
                        cursorBrush = SolidColor(pal.accent),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(pal.accent)
                            .clickable(enabled = !parseLoading, onClick = onParseSmart)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (parseLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = pal.accentFg)
                        } else {
                            Text("Parse dengan AI", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = pal.accentFg)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(pal.surface2)
                            .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                            .clickable(enabled = !rekomLoading, onClick = onRequestRekom)
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rekomLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = pal.text)
                        } else {
                            Text("Rekomendasi AI", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = pal.text)
                        }
                    }
                }

                if (parseError != null) {
                    Text("Gagal memproses input: $parseError", fontFamily = UiSans, fontSize = 12.sp, color = pal.urgent)
                }
                if (rekomError != null) {
                    Text("Gagal membuat rekomendasi: $rekomError", fontFamily = UiSans, fontSize = 12.sp, color = pal.urgent)
                }

                // Parse Preview
                parsedPreview?.let { list ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(pal.surface2)
                            .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Ditemukan ${list.size} task:", fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.text2)
                        list.forEach { t ->
                            val (pLbl, pCol) = when (t.priority) {
                                "urgent" -> "Hari Ini" to pal.urgent
                                "soon" -> "Minggu Ini" to pal.soon
                                else -> "Nanti" to pal.normal
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pal.surface)
                                    .border(1.dp, pal.border, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(t.judul, fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pal.text)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(pLbl, fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = pCol)
                                    if (!t.deadline.isNullOrBlank()) {
                                        Text(t.deadline, fontFamily = UiSans, fontSize = 10.5.sp, color = pal.text2)
                                    }
                                }
                                if (t.subtasks.isNotEmpty()) {
                                    Text("Subtasks: ${t.subtasks.joinToString(", ")}", fontFamily = UiSans, fontSize = 11.sp, color = pal.text2)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(pal.accent)
                                .clickable { onConfirmParsed(list) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+ Tambahkan ${list.size} Task ke To-Do", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = pal.accentFg)
                        }
                    }
                }

                // Rekomendasi Preview
                rekomPreview?.let { list ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(pal.surface2)
                            .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("${list.size} Rekomendasi To-Do:", fontFamily = UiSans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pal.text2)
                        list.forEachIndexed { idx, r ->
                            val (pLbl, pCol) = when (r.priority) {
                                "urgent" -> "Hari Ini" to pal.urgent
                                "soon" -> "Minggu Ini" to pal.soon
                                else -> "Nanti" to pal.normal
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pal.surface)
                                    .border(1.dp, pal.border, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(r.judul, fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pal.text)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(pLbl, fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = pCol)
                                        if (!r.deadline.isNullOrBlank()) {
                                            Text(r.deadline, fontFamily = UiSans, fontSize = 10.5.sp, color = pal.text2)
                                        }
                                    }
                                    if (!r.alasan.isNullOrBlank()) {
                                        Text("↳ ${r.alasan}", fontFamily = UiSans, fontSize = 11.sp, color = pal.text2)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(pal.surface2)
                                        .clickable { onDismissRekomItem(idx) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Hapus item", tint = pal.text3, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(pal.accent)
                                .clickable { onConfirmRekom(list) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+ Tambahkan ${list.size} Rekomendasi ke To-Do", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = pal.accentFg)
                        }
                    }
                }
            }
        }
    }
}

// ── 5. SETTINGS TAB ──────────────────────────────────────────────────────────

@Composable
private fun SettingsTabContent(
    pal: DailyOsThemePalette,
    notifHabitTime: String,
    notifTodoTime: String,
    onNotifHabitChange: (String) -> Unit,
    onNotifTodoChange: (String) -> Unit,
    onSaveNotif: () -> Unit,
    onExportSummary: (kind: String, offset: Int) -> Unit,
    onBackupJson: () -> Unit,
    onRestoreJson: () -> Unit,
    onExportCsv: () -> Unit,
    onRequestReset: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Notifikasi Harian
        item {
            SettingsCardBlock("Notifikasi Harian", pal) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Jam Pengingat Habit", fontFamily = UiSans, fontSize = 11.5.sp, color = pal.text2)
                        TimeField(value = notifHabitTime, onValueChange = onNotifHabitChange, pal = pal)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Jam Pengingat To-Do", fontFamily = UiSans, fontSize = 11.5.sp, color = pal.text2)
                        TimeField(value = notifTodoTime, onValueChange = onNotifTodoChange, pal = pal)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(pal.accent)
                        .clickable(onClick = onSaveNotif)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Simpan Pengaturan Notifikasi", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = pal.accentFg)
                }
            }
        }

        // Summary & Evaluasi (.md)
        item {
            SettingsCardBlock("Summary & Evaluasi", pal) {
                Text(
                    text = "Unduh laporan capaian habit & to-do sebagai file Markdown (.md).",
                    fontFamily = UiSans,
                    fontSize = 12.sp,
                    color = pal.text2
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SUMMARY MINGGUAN", fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = pal.text3, letterSpacing = 0.6.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryActionBtn("Minggu ini", pal, Modifier.weight(1f)) { onExportSummary("week", 0) }
                        SecondaryActionBtn("Minggu lalu", pal, Modifier.weight(1f)) { onExportSummary("week", 1) }
                    }
                    Text("SUMMARY BULANAN", fontFamily = UiSans, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = pal.text3, letterSpacing = 0.6.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryActionBtn("Bulan ini", pal, Modifier.weight(1f)) { onExportSummary("month", 0) }
                        SecondaryActionBtn("Bulan lalu", pal, Modifier.weight(1f)) { onExportSummary("month", 1) }
                    }
                }
            }
        }

        // Data Habit (Backup / Restore / CSV)
        item {
            SettingsCardBlock("Data Habit", pal) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryActionBtn("Backup Data (JSON v3)", pal, Modifier.fillMaxWidth(), onBackupJson)
                    SecondaryActionBtn("Restore Data (JSON)", pal, Modifier.fillMaxWidth(), onRestoreJson)
                    SecondaryActionBtn("Ekspor CSV", pal, Modifier.fillMaxWidth(), onExportCsv)
                }
            }
        }

        // Danger Zone
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(pal.surface)
                    .border(1.dp, pal.urgent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Danger Zone", fontFamily = UiSans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pal.urgent)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(pal.urgentBg)
                        .border(1.dp, pal.urgent.copy(alpha = 0.45f), RoundedCornerShape(9.dp))
                        .clickable(onClick = onRequestReset)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Reset Aplikasi", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = pal.urgent)
                }
            }
        }
    }
}

@Composable
private fun StatsSectionLabel(title: String, pal: DailyOsThemePalette, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, fontFamily = UiSans, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = pal.text3, letterSpacing = 1.sp)
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = pal.border)
    }
}

@Composable
private fun StatBoxCard(
    value: String,
    label: String,
    pal: DailyOsThemePalette,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(pal.surface)
            .border(1.dp, pal.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            fontFamily = UiSans,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDanger) pal.urgent else pal.text
        )
        Text(label, fontFamily = UiSans, fontSize = 11.5.sp, color = pal.text2)
    }
}

@Composable
private fun DailyOsEmptyState(pal: DailyOsThemePalette, title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, fontFamily = UiSans, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = pal.text2)
        Text(subtitle, fontFamily = UiSans, fontSize = 12.5.sp, color = pal.text3)
    }
}

@Composable
private fun SettingsCardBlock(
    title: String,
    pal: DailyOsThemePalette,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pal.surface)
            .border(1.dp, pal.border, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, fontFamily = UiSans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = pal.text)
        content()
    }
}

@Composable
private fun SecondaryActionBtn(
    label: String,
    pal: DailyOsThemePalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(pal.surface2)
            .border(1.dp, pal.border, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = pal.text)
    }
}

@Composable
private fun TimeField(value: String, onValueChange: (String) -> Unit, pal: DailyOsThemePalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(pal.surface2)
            .border(1.dp, pal.border, RoundedCornerShape(9.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 5) onValueChange(it) },
            singleLine = true,
            textStyle = TextStyle(fontFamily = UiSans, fontSize = 13.sp, color = pal.text),
            cursorBrush = SolidColor(pal.accent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
