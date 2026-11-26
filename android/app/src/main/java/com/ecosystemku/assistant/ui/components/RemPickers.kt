package com.ecosystemku.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ecosystemku.assistant.ui.theme.AccentBlue
import com.ecosystemku.assistant.ui.theme.BorderLight
import com.ecosystemku.assistant.ui.theme.BorderSubtle
import com.ecosystemku.assistant.ui.theme.EditorialSerif
import com.ecosystemku.assistant.ui.theme.ElevatedSurface
import com.ecosystemku.assistant.ui.theme.SecondarySurface
import com.ecosystemku.assistant.ui.theme.TextMuted
import com.ecosystemku.assistant.ui.theme.TextPrimary
import com.ecosystemku.assistant.ui.theme.UiSans
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Custom Time Picker Modal Rem dengan dual scroll-wheel (jam & menit),
 * gradient mask atas-bawah, pita highlight biru #5B9CF6, dan chip preset cepat.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = "Atur Jam Pengingat",
    subtitle: String = "Rem akan mengingatkan Tuan setiap hari",
    presets: List<String> = listOf("07:00", "12:00", "20:00")
) {
    val parts = initialTime.split(":")
    val initHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 7
    val initMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

    val hourBase = remember(initHour) {
        val mid = Int.MAX_VALUE / 2
        mid - (mid % 24) + initHour
    }
    val minuteBase = remember(initMinute) {
        val mid = Int.MAX_VALUE / 2
        mid - (mid % 60) + initMinute
    }

    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = hourBase)
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = minuteBase)
    val scope = rememberCoroutineScope()

    val centeredHourIndex by remember {
        derivedStateOf {
            findCenteredIndex(hourState.layoutInfo, hourState.firstVisibleItemIndex)
        }
    }
    val centeredMinuteIndex by remember {
        derivedStateOf {
            findCenteredIndex(minuteState.layoutInfo, minuteState.firstVisibleItemIndex)
        }
    }

    val selectedHour = ((centeredHourIndex % 24) + 24) % 24
    val selectedMinute = ((centeredMinuteIndex % 60) + 60) % 60

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 356.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            // Editorial Header
            Text(
                text = title,
                fontFamily = EditorialSerif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 28.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = subtitle,
                fontFamily = UiSans,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Elevated Scroll-Wheel Box with Translucent Blue Highlight Band & Fade Masks
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ElevatedSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
                    .padding(vertical = 8.dp)
            ) {
                // Center highlight band
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentBlue.copy(alpha = 0.14f))
                        .border(1.dp, AccentBlue.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                )

                // Hour | Colon | Minute Wheels
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelColumn(
                        count = 24,
                        listState = hourState,
                        centeredIndex = centeredHourIndex,
                        onSelectIndex = { idx ->
                            scope.launch { hourState.animateScrollToItem(idx) }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = ":",
                        fontFamily = UiSans,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(24.dp)
                    )

                    WheelColumn(
                        count = 60,
                        listState = minuteState,
                        centeredIndex = centeredMinuteIndex,
                        onSelectIndex = { idx ->
                            scope.launch { minuteState.animateScrollToItem(idx) }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Top & bottom gradient fade masks
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ElevatedSurface.copy(alpha = 0.96f),
                                    ElevatedSurface.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ElevatedSurface.copy(alpha = 0.55f),
                                    ElevatedSurface.copy(alpha = 0.96f)
                                )
                            )
                        )
                )
            }

            if (presets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { preset ->
                        val pParts = preset.split(":")
                        val pH = pParts.getOrNull(0)?.toIntOrNull()
                        val pM = pParts.getOrNull(1)?.toIntOrNull()
                        val isActive = pH == selectedHour && pM == selectedMinute
                        Box(
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isActive) AccentBlue.copy(alpha = 0.14f) else ElevatedSurface)
                                .border(
                                    1.dp,
                                    if (isActive) AccentBlue.copy(alpha = 0.45f) else BorderSubtle,
                                    RoundedCornerShape(999.dp)
                                )
                                .clickable(role = Role.Button) {
                                    if (pH != null && pM != null) {
                                        val hTarget = nearestCyclicIndex(centeredHourIndex, pH, 24)
                                        val mTarget = nearestCyclicIndex(centeredMinuteIndex, pM, 60)
                                        scope.launch { hourState.animateScrollToItem(hTarget) }
                                        scope.launch { minuteState.animateScrollToItem(mTarget) }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset,
                                fontFamily = UiSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isActive) AccentBlue else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Batal",
                        fontFamily = UiSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(999.dp),
                            ambientColor = AccentBlue.copy(alpha = 0.28f),
                            spotColor = AccentBlue.copy(alpha = 0.28f)
                        )
                        .clip(RoundedCornerShape(999.dp))
                        .background(AccentBlue)
                        .clickable(role = Role.Button) {
                            val formatted = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
                            onConfirm(formatted)
                        }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Simpan",
                        fontFamily = UiSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F0E0C)
                    )
                }
            }
        }
    }
}

@Composable
private fun WheelColumn(
    count: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    centeredIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    LazyColumn(
        state = listState,
        flingBehavior = snapBehavior,
        contentPadding = PaddingValues(vertical = 52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(156.dp)
    ) {
        items(count = Int.MAX_VALUE, key = { it }) { index ->
            val value = ((index % count) + count) % count
            val isSelected = index == centeredIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onSelectIndex(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format(Locale.US, "%02d", value),
                    fontFamily = UiSans,
                    fontSize = if (isSelected) 38.sp else 23.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) TextPrimary else TextMuted.copy(alpha = 0.55f)
                )
            }
        }
    }
}

private fun findCenteredIndex(
    layoutInfo: androidx.compose.foundation.lazy.LazyListLayoutInfo,
    fallback: Int
): Int {
    val visible = layoutInfo.visibleItemsInfo
    if (visible.isEmpty()) return fallback
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    var bestIndex = fallback
    var bestDistance = Int.MAX_VALUE
    for (item in visible) {
        val itemCenter = item.offset + item.size / 2
        val dist = abs(itemCenter - viewportCenter)
        if (dist < bestDistance) {
            bestDistance = dist
            bestIndex = item.index
        }
    }
    return bestIndex
}

private fun nearestCyclicIndex(currentIndex: Int, targetMod: Int, modulo: Int): Int {
    val currentMod = ((currentIndex % modulo) + modulo) % modulo
    var delta = targetMod - currentMod
    if (delta > modulo / 2) delta -= modulo
    if (delta < -modulo / 2) delta += modulo
    return currentIndex + delta
}

private data class CalendarCell(
    val date: LocalDate,
    val isCurrentMonth: Boolean
)

/**
 * Custom Date Picker Modal Rem dengan navigasi bulan dan grid kalender 7 kolom minimalis.
 */
@Composable
fun RemDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    title: String = "Pilih Tanggal"
) {
    val today = remember { LocalDate.now() }
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    var displayedMonth by remember(initialDate) { mutableStateOf(YearMonth.from(initialDate)) }

    val monthNames = remember {
        listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
    }
    val weekdays = remember { listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab") }

    val cells = remember(displayedMonth) {
        val firstOfMonth = displayedMonth.atDay(1)
        val leadingCount = firstOfMonth.dayOfWeek.value % 7 // Sun = 0 .. Sat = 6
        val daysInMonth = displayedMonth.lengthOfMonth()
        val prevMonth = displayedMonth.minusMonths(1)
        val prevLength = prevMonth.lengthOfMonth()

        val result = ArrayList<CalendarCell>(42)
        for (i in leadingCount downTo 1) {
            result.add(CalendarCell(prevMonth.atDay(prevLength - i + 1), isCurrentMonth = false))
        }
        for (day in 1..daysInMonth) {
            result.add(CalendarCell(displayedMonth.atDay(day), isCurrentMonth = true))
        }
        val nextMonth = displayedMonth.plusMonths(1)
        var nextDay = 1
        while (result.size % 7 != 0) {
            result.add(CalendarCell(nextMonth.atDay(nextDay++), isCurrentMonth = false))
        }
        result
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(SecondarySurface)
                .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Text(
                text = title,
                fontFamily = EditorialSerif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Month Navigation Bar: <  Bulan Tahun  >
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, CircleShape)
                        .clickable(role = Role.Button) {
                            displayedMonth = displayedMonth.minusMonths(1)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = "Bulan sebelumnya",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${monthNames[displayedMonth.monthValue - 1]} ${displayedMonth.year}",
                    fontFamily = UiSans,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ElevatedSurface)
                        .border(1.dp, BorderSubtle, CircleShape)
                        .clickable(role = Role.Button) {
                            displayedMonth = displayedMonth.plusMonths(1)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Bulan berikutnya",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekday Header Row
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        fontFamily = UiSans,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid Rows
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        week.forEach { cell ->
                            val isSelected = cell.date == selectedDate
                            val isToday = cell.date == today
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .then(
                                            if (isSelected) {
                                                Modifier.shadow(
                                                    elevation = 8.dp,
                                                    shape = CircleShape,
                                                    ambientColor = AccentBlue.copy(alpha = 0.3f),
                                                    spotColor = AccentBlue.copy(alpha = 0.3f)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clip(CircleShape)
                                        .background(if (isSelected) AccentBlue else Color.Transparent)
                                        .then(
                                            if (!isSelected && isToday) {
                                                Modifier.border(1.5.dp, AccentBlue, CircleShape)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable(role = Role.Button) {
                                            selectedDate = cell.date
                                            if (!cell.isCurrentMonth) {
                                                displayedMonth = YearMonth.from(cell.date)
                                            }
                                        }
                                        .alpha(if (cell.isCurrentMonth || isSelected) 1f else 0.30f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cell.date.dayOfMonth.toString(),
                                        fontFamily = UiSans,
                                        fontSize = 13.5.sp,
                                        fontWeight = when {
                                            isSelected -> FontWeight.Bold
                                            isToday -> FontWeight.SemiBold
                                            else -> FontWeight.Medium
                                        },
                                        color = if (isSelected) Color(0xFF0F0E0C) else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button, onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Batal",
                        fontFamily = UiSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(999.dp),
                            ambientColor = AccentBlue.copy(alpha = 0.28f),
                            spotColor = AccentBlue.copy(alpha = 0.28f)
                        )
                        .clip(RoundedCornerShape(999.dp))
                        .background(AccentBlue)
                        .clickable(role = Role.Button) { onConfirm(selectedDate) }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pilih",
                        fontFamily = UiSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F0E0C)
                    )
                }
            }
        }
    }
}
