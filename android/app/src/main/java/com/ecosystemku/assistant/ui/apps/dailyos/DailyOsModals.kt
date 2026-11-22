package com.ecosystemku.assistant.ui.apps.dailyos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ecosystemku.assistant.data.local.DailyOsHabitEntity
import com.ecosystemku.assistant.data.local.DailyOsTodoEntity
import com.ecosystemku.assistant.ui.theme.UiSans
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitEditorModal(
    pal: DailyOsThemePalette,
    editHabit: DailyOsHabitEntity?,
    onDismiss: () -> Unit,
    onSave: (
        editId: String?,
        name: String,
        category: String,
        color: String,
        icon: String,
        activeDays: List<Int>,
        subTasks: List<DailyOsSubTask>
    ) -> Unit
) {
    val initCat = editHabit?.category?.ifBlank { "General" } ?: "General"
    val isPresetCat = initCat in DailyOsHelpers.CATEGORY_PRESETS

    var name by remember(editHabit) { mutableStateOf(editHabit?.name ?: "") }
    var selectedCat by remember(editHabit) { mutableStateOf(if (isPresetCat) initCat else "__other__") }
    var customCat by remember(editHabit) { mutableStateOf(if (isPresetCat) "" else initCat) }
    var selectedColor by remember(editHabit) { mutableStateOf(editHabit?.color?.ifBlank { "#1A1917" } ?: "#1A1917") }
    var selectedIcon by remember(editHabit) { mutableStateOf(editHabit?.icon?.ifBlank { "star" } ?: "star") }
    var iconGridOpen by remember { mutableStateOf(false) }
    var activeDays by remember(editHabit) {
        mutableStateOf(
            if (editHabit != null) DailyOsHelpers.parseActiveDays(editHabit.activeDaysJson) else emptyList()
        )
    }
    val subTasks = remember(editHabit) {
        mutableStateListOf<DailyOsSubTask>().apply {
            if (editHabit != null) addAll(DailyOsHelpers.parseSubTasks(editHabit.subTasksJson))
        }
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(pal.surface)
                .border(1.dp, pal.border, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editHabit == null) "Tambah Habit" else "Edit Habit",
                    fontFamily = UiSans,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.text
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(pal.surface2)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = pal.text2, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nama Habit
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    ModalFieldLabel("Nama Habit", pal)
                    DailyOsTextInput(
                        value = name,
                        onValueChange = {
                            name = it
                            if (errorMsg != null) errorMsg = null
                        },
                        placeholder = "Contoh: Olahraga Pagi",
                        pal = pal
                    )
                    if (errorMsg != null) {
                        Text(errorMsg!!, fontFamily = UiSans, fontSize = 11.sp, color = pal.urgent)
                    }
                }

                // Kategori
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModalFieldLabel("Kategori", pal)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DailyOsHelpers.CATEGORY_PRESETS.forEach { cat ->
                            val active = selectedCat == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) pal.accent else pal.surface2)
                                    .border(1.dp, if (active) pal.accent else pal.border, RoundedCornerShape(8.dp))
                                    .clickable { selectedCat = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (active) pal.accentFg else pal.text2
                                )
                            }
                        }
                        val otherActive = selectedCat == "__other__"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (otherActive) pal.accent else pal.surface2)
                                .border(1.dp, if (otherActive) pal.accent else pal.border, RoundedCornerShape(8.dp))
                                .clickable { selectedCat = "__other__" }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Lainnya...",
                                fontFamily = UiSans,
                                fontSize = 12.sp,
                                fontWeight = if (otherActive) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (otherActive) pal.accentFg else pal.text2
                            )
                        }
                    }
                    if (selectedCat == "__other__") {
                        DailyOsTextInput(
                            value = customCat,
                            onValueChange = { customCat = it },
                            placeholder = "Ketik nama kategori custom...",
                            pal = pal
                        )
                    }
                }

                // Warna Badge
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModalFieldLabel("Warna Badge", pal)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DailyOsHelpers.HABIT_COLORS.forEach { hex ->
                            val isSel = selectedColor.equals(hex, ignoreCase = true)
                            val dotColor = DailyOsHelpers.parseColor(hex)
                            val borderColor = when {
                                isSel -> pal.accent
                                DailyOsHelpers.isLightColor(hex) && !pal.isDark -> Color(0xFFD0CFC9)
                                DailyOsHelpers.isDarkColor(hex) && pal.isDark -> Color(0xFF444444)
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .border(if (isSel) 2.5.dp else 1.dp, borderColor, CircleShape)
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    val checkTint = if (DailyOsHelpers.isLightColor(hex)) Color(0xFF1A1917) else Color.White
                                    Icon(Icons.Outlined.Check, contentDescription = null, tint = checkTint, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // Ikon
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModalFieldLabel("Ikon", pal)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(pal.surface2)
                            .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                            .clickable { iconGridOpen = !iconGridOpen }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = DailyOsHelpers.iconFor(selectedIcon),
                                contentDescription = null,
                                tint = pal.text,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Pilih Ikon (${DailyOsHelpers.iconLabel(selectedIcon)})",
                                fontFamily = UiSans,
                                fontSize = 13.sp,
                                color = pal.text
                            )
                        }
                        Icon(
                            imageVector = if (iconGridOpen) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = pal.text2,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (iconGridOpen) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(pal.surface2)
                                .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DailyOsHelpers.HABIT_ICONS.forEach { def ->
                                val isSel = selectedIcon == def.key
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) pal.accent else pal.surface)
                                        .border(1.dp, if (isSel) pal.accent else pal.border, RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedIcon = def.key
                                            iconGridOpen = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = def.icon,
                                        contentDescription = def.label,
                                        tint = if (isSel) pal.accentFg else pal.text,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Jadwal Hari Aktif
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModalFieldLabel("Jadwal Hari Aktif", pal)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        DailyOsHelpers.DAY_LABELS.forEach { (dayNum, label) ->
                            val isActive = dayNum in activeDays
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) pal.accent else pal.surface2)
                                    .border(1.dp, if (isActive) pal.accent else pal.border, RoundedCornerShape(8.dp))
                                    .clickable {
                                        activeDays = if (isActive) activeDays - dayNum else activeDays + dayNum
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = UiSans,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isActive) pal.accentFg else pal.text2
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            "Setiap Hari" to emptyList(),
                            "Senin–Jumat" to listOf(1, 2, 3, 4, 5),
                            "Sen, Rab, Jum" to listOf(1, 3, 5),
                            "Akhir Pekan" to listOf(0, 6)
                        )
                        presets.forEach { (label, days) ->
                            val matches = activeDays.sorted() == days.sorted()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (matches) pal.surface2 else Color.Transparent)
                                    .border(1.dp, if (matches) pal.accent else pal.border, RoundedCornerShape(7.dp))
                                    .clickable { activeDays = days }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = UiSans,
                                    fontSize = 11.sp,
                                    color = if (matches) pal.text else pal.text2
                                )
                            }
                        }
                    }

                    val scheduleHint = if (activeDays.isEmpty()) {
                        "Semua hari aktif (default: setiap hari)"
                    } else {
                        "Aktif: ${activeDays.sorted().joinToString(", ") { DailyOsHelpers.dayShortLabel(it) }}"
                    }
                    Text(scheduleHint, fontFamily = UiSans, fontSize = 11.sp, color = pal.text3)
                }

                // Sub-Checklist
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModalFieldLabel("Checklist (opsional — kosong = 1 aktivitas)", pal)
                    subTasks.forEachIndexed { idx, sub ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                DailyOsTextInput(
                                    value = sub.text,
                                    onValueChange = { txt -> subTasks[idx] = sub.copy(text = txt) },
                                    placeholder = "Sub-aktivitas...",
                                    pal = pal
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pal.surface2)
                                    .border(1.dp, pal.border, RoundedCornerShape(8.dp))
                                    .clickable { subTasks.removeAt(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Hapus", tint = pal.text2, modifier = Modifier.size(15.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .border(1.dp, pal.border, RoundedCornerShape(9.dp))
                            .clickable {
                                subTasks.add(
                                    DailyOsSubTask(
                                        id = "s_${System.currentTimeMillis()}_${subTasks.size}",
                                        text = ""
                                    )
                                )
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = pal.text2, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Tambah Checklist", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = pal.text2)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = pal.border)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pal.surface2)
                        .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Batal", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = pal.text2)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pal.accent)
                        .clickable {
                            val cleanName = name.trim()
                            if (cleanName.isEmpty()) {
                                errorMsg = "Nama habit tidak boleh kosong"
                                return@clickable
                            }
                            val finalCat = if (selectedCat == "__other__") {
                                customCat.trim().ifBlank { "General" }
                            } else {
                                selectedCat.ifBlank { "General" }
                            }
                            val cleanSubs = subTasks
                                .map { it.copy(text = it.text.trim()) }
                                .filter { it.text.isNotEmpty() }
                            onSave(
                                editHabit?.id,
                                cleanName,
                                finalCat,
                                selectedColor,
                                selectedIcon,
                                activeDays.distinct(),
                                cleanSubs
                            )
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Simpan", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pal.accentFg)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoEditorModal(
    pal: DailyOsThemePalette,
    editTodo: DailyOsTodoEntity?,
    onDismiss: () -> Unit,
    onSave: (
        editId: String?,
        title: String,
        priority: String,
        deadline: String,
        subtaskTexts: List<String>
    ) -> Unit
) {
    var title by remember(editTodo) { mutableStateOf(editTodo?.title ?: "") }
    var priority by remember(editTodo) { mutableStateOf(editTodo?.priority?.ifBlank { "normal" } ?: "normal") }
    var deadline by remember(editTodo) { mutableStateOf(editTodo?.deadline ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    val subtaskTexts = remember(editTodo) {
        mutableStateListOf<String>().apply {
            if (editTodo != null) {
                addAll(DailyOsHelpers.parseTodoSubtasks(editTodo.subtasksJson).map { it.text })
            }
        }
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .background(pal.surface)
                .border(1.dp, pal.border, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editTodo == null) "Tambah To-Do" else "Edit To-Do",
                    fontFamily = UiSans,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.text
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(pal.surface2)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = pal.text2, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Judul Task
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    ModalFieldLabel("Task", pal)
                    DailyOsTextInput(
                        value = title,
                        onValueChange = {
                            title = it
                            if (errorMsg != null) errorMsg = null
                        },
                        placeholder = "Apa yang perlu diselesaikan?",
                        pal = pal
                    )
                    if (errorMsg != null) {
                        Text(errorMsg!!, fontFamily = UiSans, fontSize = 11.sp, color = pal.urgent)
                    }
                }

                // Prioritas
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModalFieldLabel("Prioritas", pal)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val opts = listOf(
                            Triple("urgent", "Hari Ini", pal.urgent to pal.urgentBg),
                            Triple("soon", "Minggu Ini", pal.soon to pal.soonBg),
                            Triple("normal", "Nanti", pal.normal to pal.normalBg)
                        )
                        opts.forEach { (key, label, colors) ->
                            val active = priority == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(if (active) colors.second else pal.surface2)
                                    .border(1.dp, if (active) colors.first else pal.border, RoundedCornerShape(9.dp))
                                    .clickable { priority = key }
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = UiSans,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) colors.first else pal.text2
                                )
                            }
                        }
                    }
                }

                // Deadline
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModalFieldLabel("Deadline (opsional)", pal)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(pal.surface2)
                                .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                                .clickable { showDatePicker = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = pal.text2, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (deadline.isNotBlank()) DailyOsHelpers.formatFullDate(deadline) else "Pilih tanggal deadline...",
                                fontFamily = UiSans,
                                fontSize = 13.sp,
                                color = if (deadline.isNotBlank()) pal.text else pal.text3
                            )
                        }
                        if (deadline.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(pal.surface2)
                                    .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                                    .clickable { deadline = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Hapus tanggal", tint = pal.text2, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val today = DailyOsHelpers.todayStr()
                        val tomorrow = DailyOsHelpers.tomorrowStr()
                        QuickChip("Hari Ini", deadline == today, pal) { deadline = today }
                        QuickChip("Besok", deadline == tomorrow, pal) { deadline = tomorrow }
                    }
                }

                // Subtasks
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModalFieldLabel("Subtask (opsional)", pal)
                    subtaskTexts.forEachIndexed { idx, txt ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                DailyOsTextInput(
                                    value = txt,
                                    onValueChange = { subtaskTexts[idx] = it },
                                    placeholder = "Subtask...",
                                    pal = pal
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(pal.surface2)
                                    .border(1.dp, pal.border, RoundedCornerShape(8.dp))
                                    .clickable { subtaskTexts.removeAt(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Hapus", tint = pal.text2, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .border(1.dp, pal.border, RoundedCornerShape(9.dp))
                            .clickable { subtaskTexts.add("") }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = pal.text2, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Tambah Subtask", fontFamily = UiSans, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = pal.text2)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 1.dp, color = pal.border)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pal.surface2)
                        .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Batal", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = pal.text2)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pal.accent)
                        .clickable {
                            val cleanTitle = title.trim()
                            if (cleanTitle.isEmpty()) {
                                errorMsg = "Judul task tidak boleh kosong"
                                return@clickable
                            }
                            onSave(
                                editTodo?.id,
                                cleanTitle,
                                priority,
                                deadline.trim(),
                                subtaskTexts.map { it.trim() }.filter { it.isNotEmpty() }
                            )
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Simpan", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pal.accentFg)
                }
            }
        }
    }

    if (showDatePicker) {
        val initMillis = runCatching {
            LocalDate.parse(deadline.ifBlank { DailyOsHelpers.todayStr() })
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            deadline = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun DailyOsConfirmDialog(
    pal: DailyOsThemePalette,
    title: String,
    message: String,
    confirmLabel: String = "Lanjut",
    isDanger: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(pal.surface)
                .border(1.dp, pal.border, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, fontFamily = UiSans, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.text)
            Text(message, fontFamily = UiSans, fontSize = 13.sp, lineHeight = 19.sp, color = pal.text2)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(pal.surface2)
                        .border(1.dp, pal.border, RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Batal", fontFamily = UiSans, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = pal.text2)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDanger) pal.urgent else pal.accent)
                        .clickable(onClick = onConfirm)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = confirmLabel,
                        fontFamily = UiSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDanger) Color.White else pal.accentFg
                    )
                }
            }
        }
    }
}

@Composable
private fun ModalFieldLabel(text: String, pal: DailyOsThemePalette) {
    Text(
        text = text,
        fontFamily = UiSans,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = pal.text2
    )
}

@Composable
private fun DailyOsTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    pal: DailyOsThemePalette
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(pal.surface2)
            .border(1.dp, pal.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontFamily = UiSans, fontSize = 13.sp, color = pal.text3)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = UiSans, fontSize = 13.sp, color = pal.text),
            cursorBrush = SolidColor(pal.accent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuickChip(
    label: String,
    active: Boolean,
    pal: DailyOsThemePalette,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (active) pal.accent else pal.surface2)
            .border(1.dp, if (active) pal.accent else pal.border, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontFamily = UiSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (active) pal.accentFg else pal.text2
        )
    }
}
