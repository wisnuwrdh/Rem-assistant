package com.ecosystemku.assistant.ui.apps.cucimoney

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystemku.assistant.data.local.CuciMasterBudgetEntity
import com.ecosystemku.assistant.data.local.CuciTransactionEntity
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

private data class CatAggregate(
    val categoryId: Long,
    val name: String,
    val emoji: String,
    val total: Long,
    val color: Color
)

@Composable
internal fun AnalisisTabContent(
    uiState: CuciMoneyUiState,
    period: CuciAnalysisPeriod,
    activeTab: CuciAnalysisTab,
    viewMonth: Int,
    viewYear: Int,
    onSelectPeriod: (CuciAnalysisPeriod) -> Unit,
    onSelectTab: (CuciAnalysisTab) -> Unit,
    onChangePeriodOffset: (Int) -> Unit,
    onToggleVisibility: () -> Unit,
    onOpenCatDetail: (categoryId: Long, direction: String) -> Unit,
    onOpenBudgetDetail: (month: Int, year: Int) -> Unit,
    onOpenSetupBudget: (month: Int, year: Int) -> Unit
) {
    val activeBook = CuciMoneyHelpers.activeBook(uiState)
    val bookId = activeBook?.id ?: uiState.currentBookId
    val bookTrxs = remember(uiState.transactions, bookId) {
        uiState.transactions.filter { it.bookId == bookId && it.transferPair == null }
    }

    val (nowYear, nowMonth) = CuciMoneyHelpers.currentYearMonth()
    val canAdvance = when (period) {
        CuciAnalysisPeriod.MONTH -> !(viewYear == nowYear && viewMonth == nowMonth)
        CuciAnalysisPeriod.YEAR -> viewYear < nowYear
        CuciAnalysisPeriod.ALL -> false
    }

    val periodLabel = when (period) {
        CuciAnalysisPeriod.MONTH -> "${CuciMoneyHelpers.MONTH_NAMES[viewMonth]} $viewYear"
        CuciAnalysisPeriod.YEAR -> "Tahun $viewYear"
        CuciAnalysisPeriod.ALL -> "Semua Waktu"
    }

    val curPrefix = remember(period, viewYear, viewMonth) {
        when (period) {
            CuciAnalysisPeriod.MONTH -> String.format("%04d-%02d", viewYear, viewMonth + 1)
            CuciAnalysisPeriod.YEAR -> String.format("%04d-", viewYear)
            CuciAnalysisPeriod.ALL -> ""
        }
    }

    val prevPrefix = remember(period, viewYear, viewMonth) {
        when (period) {
            CuciAnalysisPeriod.MONTH -> {
                var pm = viewMonth - 1
                var py = viewYear
                if (pm < 0) {
                    pm = 11; py -= 1
                }
                String.format("%04d-%02d", py, pm + 1)
            }
            CuciAnalysisPeriod.YEAR -> String.format("%04d-", viewYear - 1)
            CuciAnalysisPeriod.ALL -> null
        }
    }

    val filteredTrxs = remember(bookTrxs, curPrefix) {
        if (curPrefix.isEmpty()) bookTrxs else bookTrxs.filter { it.date.startsWith(curPrefix) }
    }
    val prevTrxs = remember(bookTrxs, prevPrefix) {
        if (prevPrefix == null) emptyList() else bookTrxs.filter { it.date.startsWith(prevPrefix) }
    }

    val totalIn = remember(filteredTrxs) { filteredTrxs.filter { it.direction == "in" }.sumOf { it.amount } }
    val totalOut = remember(filteredTrxs) { filteredTrxs.filter { it.direction == "out" }.sumOf { it.amount } }
    val net = totalIn - totalOut

    val prevIn = remember(prevTrxs) { prevTrxs.filter { it.direction == "in" }.sumOf { it.amount } }
    val prevOut = remember(prevTrxs) { prevTrxs.filter { it.direction == "out" }.sumOf { it.amount } }
    val prevNet = prevIn - prevOut

    val catById = remember(uiState.categories) { uiState.categories.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.BarChart, null, tint = CuciMoneyColors.Primary, modifier = Modifier.size(22.dp))
                    Text("Analisis", color = CuciMoneyColors.TextMain, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                CuciHeaderIconButton(onClick = onToggleVisibility) {
                    Icon(
                        if (uiState.visibility) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        null,
                        tint = CuciMoneyColors.TextMain,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Period switcher + Navigator
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CuciMoneyColors.Card)
                        .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        CuciAnalysisPeriod.MONTH to "Bulan",
                        CuciAnalysisPeriod.YEAR to "Tahun",
                        CuciAnalysisPeriod.ALL to "Semua"
                    ).forEach { (p, lbl) ->
                        val sel = period == p
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) CuciMoneyColors.Primary else Color.Transparent)
                                .clickable { onSelectPeriod(p) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lbl,
                                color = if (sel) Color(0xFF0A1214) else CuciMoneyColors.TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CuciMoneyColors.Card)
                        .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (period != CuciAnalysisPeriod.ALL) {
                        CuciCircleNavButton(onClick = { onChangePeriodOffset(-1) }) {
                            Icon(Icons.Outlined.ChevronLeft, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.size(28.dp))
                    }
                    Text(
                        text = periodLabel,
                        color = CuciMoneyColors.TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (canAdvance) {
                        CuciCircleNavButton(onClick = { onChangePeriodOffset(1) }) {
                            Icon(Icons.Outlined.ChevronRight, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        // 3 Summary cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Masuk",
                    valueText = CuciMoneyHelpers.formatCompact(totalIn, uiState.visibility),
                    valueColor = CuciMoneyColors.Income,
                    deltaPct = if (period != CuciAnalysisPeriod.ALL && prevIn > 0) (((totalIn - prevIn) * 100.0) / prevIn).roundToInt() else null,
                    positiveIsGood = true
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Keluar",
                    valueText = CuciMoneyHelpers.formatCompact(totalOut, uiState.visibility),
                    valueColor = CuciMoneyColors.Expense,
                    deltaPct = if (period != CuciAnalysisPeriod.ALL && prevOut > 0) (((totalOut - prevOut) * 100.0) / prevOut).roundToInt() else null,
                    positiveIsGood = false
                )
                SummaryStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Selisih",
                    valueText = (if (net < 0 && uiState.visibility) "-" else "") + CuciMoneyHelpers.formatCompact(abs(net), uiState.visibility),
                    valueColor = if (net >= 0) CuciMoneyColors.Income else CuciMoneyColors.Expense,
                    deltaPct = if (period != CuciAnalysisPeriod.ALL && prevNet != 0L) (((net - prevNet) * 100.0) / abs(prevNet)).roundToInt() else null,
                    positiveIsGood = true
                )
            }
        }

        // 4 Analysis Sub-Tabs
        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CuciMoneyColors.Card)
                    .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    CuciAnalysisTab.EXPENSE to "Pengeluaran",
                    CuciAnalysisTab.INCOME to "Pemasukan",
                    CuciAnalysisTab.BUDGET to "Budget",
                    CuciAnalysisTab.TREN to "Tren"
                ).forEach { (tab, lbl) ->
                    val sel = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) CuciMoneyColors.Card2 else Color.Transparent)
                            .border(
                                1.dp,
                                if (sel) CuciMoneyColors.BorderStrong else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectTab(tab) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lbl,
                            color = if (sel) CuciMoneyColors.Primary else CuciMoneyColors.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        when (activeTab) {
            CuciAnalysisTab.EXPENSE, CuciAnalysisTab.INCOME -> {
                val dir = if (activeTab == CuciAnalysisTab.EXPENSE) "out" else "in"
                val dirTrxs = filteredTrxs.filter { it.direction == dir }
                val totalDir = dirTrxs.sumOf { it.amount }
                val groupedCats = dirTrxs.groupBy { it.categoryId }
                    .entries
                    .map { (cid, list) ->
                        val cat = catById[cid]
                        CatAggregate(
                            categoryId = cid,
                            name = cat?.name ?: "Lainnya",
                            emoji = cat?.emoji ?: "📌",
                            total = list.sumOf { it.amount },
                            color = Color.Transparent
                        )
                    }
                    .sortedByDescending { it.total }
                    .mapIndexed { idx, item ->
                        item.copy(color = CuciMoneyColors.DonutPalette[idx % CuciMoneyColors.DonutPalette.size])
                    }

                if (groupedCats.isEmpty() || totalDir == 0L) {
                    item {
                        CuciEmptyState(
                            icon = if (dir == "out") "💸" else "💰",
                            text = "Belum ada ${if (dir == "out") "pengeluaran" else "pemasukan"} pada periode ini"
                        )
                    }
                } else {
                    item {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(CuciMoneyColors.Card)
                                .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(20.dp))
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 26.dp.toPx()
                                    var startAngle = -90f
                                    groupedCats.forEach { cat ->
                                        val sweep = (cat.total.toFloat() / totalDir.toFloat()) * 360f
                                        drawArc(
                                            color = cat.color,
                                            startAngle = startAngle,
                                            sweepAngle = (sweep - 1.5f).coerceAtLeast(1f),
                                            useCenter = false,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (dir == "out") "Total Keluar" else "Total Masuk",
                                        color = CuciMoneyColors.TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = CuciMoneyHelpers.formatCompact(totalDir, uiState.visibility),
                                        color = CuciMoneyColors.TextMain,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            groupedCats.take(6).forEach { cat ->
                                val pct = ((cat.total * 100.0) / totalDir).roundToInt()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(cat.color))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${cat.emoji} ${cat.name}", color = CuciMoneyColors.TextMain, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                    Text("$pct%", color = CuciMoneyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    items(groupedCats, key = { "cat_${dir}_${it.categoryId}" }) { cat ->
                        val ratio = (cat.total.toFloat() / totalDir.toFloat()).coerceIn(0f, 1f)
                        val pct = ((cat.total * 100.0) / totalDir).roundToInt()
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CuciMoneyColors.Card)
                                .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(16.dp))
                                .clickable { onOpenCatDetail(cat.categoryId, dir) }
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(cat.name, color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CuciMoneyHelpers.formatRp(cat.total, uiState.visibility),
                                        color = CuciMoneyColors.TextMain,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("$pct%", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0x14FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cat.color)
                                )
                            }
                        }
                    }
                }
            }

            CuciAnalysisTab.BUDGET -> {
                val targetMonth = if (period == CuciAnalysisPeriod.MONTH) viewMonth else nowMonth
                val targetYear = if (period == CuciAnalysisPeriod.MONTH) viewYear else nowYear
                val budget = uiState.masterBudgets.firstOrNull {
                    it.bookId == bookId && it.month == targetMonth && it.year == targetYear
                }
                item {
                    if (budget == null) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(CuciMoneyColors.Card)
                                .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(20.dp))
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎯", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Belum Ada Master Budget",
                                color = CuciMoneyColors.TextMain,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Atur batas pengeluaran untuk ${CuciMoneyHelpers.MONTH_NAMES[targetMonth]} $targetYear agar keuangan lebih terkontrol.",
                                color = CuciMoneyColors.TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CuciMoneyColors.Primary)
                                    .clickable { onOpenSetupBudget(targetMonth, targetYear) }
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Text("Atur Master Budget", color = Color(0xFF0A1214), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    } else {
                        val spent = computeBudgetSpent(budget, uiState.transactions)
                        val pct = if (budget.limitAmount > 0) ((spent * 100.0) / budget.limitAmount).toInt() else 0
                        val clamped = pct.coerceIn(0, 100)
                        val remaining = budget.limitAmount - spent
                        val (ringColor, statusLabel) = when {
                            pct < 70 -> CuciMoneyColors.Primary to "Aman"
                            pct <= 100 -> CuciMoneyColors.Warning to "Waspada"
                            else -> CuciMoneyColors.Expense to "Over Budget"
                        }
                        val daysLeft = if (budget.endDate.isNotBlank()) CuciMoneyHelpers.daysLeftUntil(budget.endDate) else {
                            val cal = Calendar.getInstance()
                            (CuciMoneyHelpers.daysInMonth(targetYear, targetMonth) - cal.get(Calendar.DAY_OF_MONTH)).coerceAtLeast(1)
                        }
                        val idealPerDay = if (remaining > 0 && (daysLeft ?: 0) > 0) remaining / (daysLeft ?: 1) else 0L

                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(CuciMoneyColors.Card)
                                .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(20.dp))
                                .clickable { onOpenBudgetDetail(targetMonth, targetYear) }
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Budget ${CuciMoneyHelpers.MONTH_NAMES[targetMonth]} $targetYear",
                                        color = CuciMoneyColors.TextMain,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (budget.note.isNotBlank()) {
                                        Text(budget.note, color = CuciMoneyColors.TextMuted, fontSize = 12.sp)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ringColor.copy(alpha = 0.16f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(statusLabel, color = ringColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(color = Color(0x14FFFFFF), style = Stroke(width = 9.dp.toPx()))
                                        drawArc(
                                            color = ringColor,
                                            startAngle = -90f,
                                            sweepAngle = (clamped / 100f) * 360f,
                                            useCenter = false,
                                            style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    Text("$pct%", color = CuciMoneyColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Spacer(modifier = Modifier.width(18.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BudgetMetricLine("Terpakai", CuciMoneyHelpers.formatRp(spent, uiState.visibility), CuciMoneyColors.TextMain)
                                    BudgetMetricLine("Batas", CuciMoneyHelpers.formatRp(budget.limitAmount, uiState.visibility), CuciMoneyColors.TextMuted)
                                    BudgetMetricLine(
                                        label = if (remaining >= 0) "Sisa" else "Lebih",
                                        value = CuciMoneyHelpers.formatRp(abs(remaining), uiState.visibility),
                                        color = if (remaining >= 0) CuciMoneyColors.Income else CuciMoneyColors.Expense
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = CuciMoneyColors.Border)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Ideal/hari", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                                    Text(CuciMoneyHelpers.formatRp(idealPerDay, uiState.visibility), color = CuciMoneyColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Periode", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                                    val pStr = if (budget.startDate.isNotBlank() && budget.endDate.isNotBlank()) {
                                        "${CuciMoneyHelpers.formatDateShort(budget.startDate)} – ${CuciMoneyHelpers.formatDateShort(budget.endDate)}"
                                    } else {
                                        "${CuciMoneyHelpers.MONTH_SHORT[targetMonth]} $targetYear"
                                    }
                                    Text(pStr, color = CuciMoneyColors.TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ketuk untuk rincian kategori & edit budget →",
                                color = CuciMoneyColors.Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            CuciAnalysisTab.TREN -> {
                item {
                    val monthsData = remember(bookTrxs, viewYear, viewMonth) {
                        (5 downTo 0).map { offset ->
                            val cal = Calendar.getInstance()
                            cal.set(viewYear, viewMonth, 1)
                            cal.add(Calendar.MONTH, -offset)
                            val y = cal.get(Calendar.YEAR)
                            val m = cal.get(Calendar.MONTH)
                            val prefix = String.format("%04d-%02d", y, m + 1)
                            val trxs = bookTrxs.filter { it.date.startsWith(prefix) }
                            Triple(
                                CuciMoneyHelpers.MONTH_SHORT[m],
                                trxs.filter { it.direction == "in" }.sumOf { it.amount },
                                trxs.filter { it.direction == "out" }.sumOf { it.amount }
                            )
                        }
                    }
                    val maxVal = monthsData.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1L) ?: 1L

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CuciMoneyColors.Card)
                            .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Text("Arus Kas 6 Bulan", color = CuciMoneyColors.TextMain, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CuciMoneyColors.Income))
                                Text("Pemasukan", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(CuciMoneyColors.Expense))
                                Text("Pengeluaran", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            val groupW = size.width / monthsData.size
                            val barW = (groupW * 0.28f).coerceAtMost(22.dp.toPx())
                            val gap = 4.dp.toPx()
                            monthsData.forEachIndexed { idx, (_, inc, exp) ->
                                val cx = idx * groupW + groupW / 2f
                                val hIn = (inc.toFloat() / maxVal.toFloat()) * (size.height - 8.dp.toPx())
                                val hOut = (exp.toFloat() / maxVal.toFloat()) * (size.height - 8.dp.toPx())
                                if (hIn > 0f) {
                                    drawRoundRect(
                                        color = CuciMoneyColors.Income,
                                        topLeft = Offset(cx - barW - gap / 2f, size.height - hIn),
                                        size = Size(barW, hIn),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                                if (hOut > 0f) {
                                    drawRoundRect(
                                        color = CuciMoneyColors.Expense,
                                        topLeft = Offset(cx + gap / 2f, size.height - hOut),
                                        size = Size(barW, hOut),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            monthsData.forEach { (lbl, _, _) ->
                                Text(
                                    text = lbl,
                                    color = CuciMoneyColors.TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
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
private fun SummaryStatCard(
    modifier: Modifier,
    label: String,
    valueText: String,
    valueColor: Color,
    deltaPct: Int?,
    positiveIsGood: Boolean
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CuciMoneyColors.Card)
            .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(label, color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = valueText,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (deltaPct != null) {
            Spacer(modifier = Modifier.height(4.dp))
            val isGood = if (positiveIsGood) deltaPct >= 0 else deltaPct <= 0
            val c = if (isGood) CuciMoneyColors.Income else CuciMoneyColors.Expense
            val arrow = if (deltaPct >= 0) "▲" else "▼"
            val sign = if (deltaPct > 0) "+" else ""
            Text(
                text = "$arrow $sign$deltaPct%",
                color = c,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BudgetMetricLine(label: String, value: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = CuciMoneyColors.TextMuted, fontSize = 12.sp, modifier = Modifier.width(64.dp))
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

internal fun computeBudgetSpent(
    budget: CuciMasterBudgetEntity,
    transactions: List<CuciTransactionEntity>
): Long {
    val monthPrefix = String.format("%04d-%02d", budget.year, budget.month + 1)
    return transactions
        .filter { t ->
            t.bookId == budget.bookId &&
                t.direction == "out" &&
                t.transferPair == null &&
                if (budget.startDate.isNotBlank() && budget.endDate.isNotBlank()) {
                    t.date >= budget.startDate && t.date <= budget.endDate
                } else {
                    t.date.startsWith(monthPrefix)
                }
        }
        .sumOf { it.amount }
}

@Composable
internal fun CategoryTransactionsModal(
    uiState: CuciMoneyUiState,
    categoryId: Long,
    direction: String,
    period: CuciAnalysisPeriod,
    viewMonth: Int,
    viewYear: Int,
    onClose: () -> Unit,
    onOpenTrxDetail: (CuciTransactionEntity) -> Unit
) {
    val bookId = CuciMoneyHelpers.activeBook(uiState)?.id ?: uiState.currentBookId
    val cat = uiState.categories.firstOrNull { it.id == categoryId }
    val accById = remember(uiState.accounts) { uiState.accounts.associateBy { it.id } }

    val prefix = when (period) {
        CuciAnalysisPeriod.MONTH -> String.format("%04d-%02d", viewYear, viewMonth + 1)
        CuciAnalysisPeriod.YEAR -> String.format("%04d-", viewYear)
        CuciAnalysisPeriod.ALL -> ""
    }
    val trxs = remember(uiState.transactions, bookId, categoryId, direction, prefix) {
        uiState.transactions.filter { t ->
            t.bookId == bookId &&
                t.transferPair == null &&
                t.categoryId == categoryId &&
                t.direction == direction &&
                (prefix.isEmpty() || t.date.startsWith(prefix))
        }.sortedByDescending { it.date }
    }
    val total = remember(trxs) { trxs.sumOf { it.amount } }
    val grouped = remember(trxs) { trxs.groupBy { it.date }.toSortedMap(compareByDescending { it }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CuciMoneyColors.Bg)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF00695C), Color(0xFF00BFA5))))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${cat?.emoji ?: "📌"} ${cat?.name ?: "Kategori"}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (direction == "out") "Total Pengeluaran" else "Total Pemasukan",
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp
            )
            Text(
                text = CuciMoneyHelpers.formatRp(total, uiState.visibility),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (grouped.isEmpty()) {
            CuciEmptyState(icon = "📭", text = "Tidak ada transaksi")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                grouped.forEach { (dateStr, itemsList) ->
                    item(key = "cat_grp_$dateStr") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CuciMoneyColors.Card)
                                .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(16.dp))
                        ) {
                            Text(
                                text = CuciMoneyHelpers.formatGroupDayLabel(dateStr),
                                color = CuciMoneyColors.TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                            itemsList.forEach { t ->
                                HorizontalDivider(color = CuciMoneyColors.Border)
                                val isIn = t.direction == "in"
                                CuciTransactionRow(
                                    emoji = cat?.emoji ?: "📌",
                                    iconBg = if (isIn) CuciMoneyColors.IncomeDim else CuciMoneyColors.ExpenseDim,
                                    title = t.title,
                                    subtitle = accById[t.accountId]?.name ?: "",
                                    amountText = "${if (isIn) "+" else "−"} ${CuciMoneyHelpers.formatRp(t.amount, uiState.visibility)}",
                                    amountColor = if (isIn) CuciMoneyColors.Income else CuciMoneyColors.Expense,
                                    onClick = { onOpenTrxDetail(t) }
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
internal fun BudgetDetailSheet(
    uiState: CuciMoneyUiState,
    month: Int,
    year: Int,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bookId = CuciMoneyHelpers.activeBook(uiState)?.id ?: uiState.currentBookId
    val budget = uiState.masterBudgets.firstOrNull {
        it.bookId == bookId && it.month == month && it.year == year
    }
    if (budget == null) {
        onClose()
        return
    }
    val spent = computeBudgetSpent(budget, uiState.transactions)
    val pct = if (budget.limitAmount > 0) ((spent * 100.0) / budget.limitAmount).toInt() else 0
    val clamped = pct.coerceIn(0, 100)
    val ringColor = when {
        pct < 70 -> CuciMoneyColors.Primary
        pct <= 100 -> CuciMoneyColors.Warning
        else -> CuciMoneyColors.Expense
    }
    val monthPrefix = String.format("%04d-%02d", year, month + 1)
    val catById = uiState.categories.associateBy { it.id }
    val expByCat = uiState.transactions
        .filter { t ->
            t.bookId == bookId && t.direction == "out" && t.transferPair == null &&
                if (budget.startDate.isNotBlank() && budget.endDate.isNotBlank()) t.date >= budget.startDate && t.date <= budget.endDate
                else t.date.startsWith(monthPrefix)
        }
        .groupBy { it.categoryId }
        .map { (cid, list) -> Triple(catById[cid]?.emoji ?: "📌", catById[cid]?.name ?: "Lainnya", list.sumOf { it.amount }) }
        .sortedByDescending { it.third }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA6000000))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CuciMoneyColors.Bg)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rincian Budget ${CuciMoneyHelpers.MONTH_NAMES[month]} $year",
                    color = CuciMoneyColors.TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(22.dp).clickable { onClose() })
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CuciMoneyColors.Card)
                    .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color(0x14FFFFFF), style = Stroke(width = 7.dp.toPx()))
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = (clamped / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text("$pct%", color = CuciMoneyColors.TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(CuciMoneyHelpers.formatRp(spent, uiState.visibility), color = CuciMoneyColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("dari ${CuciMoneyHelpers.formatRp(budget.limitAmount, uiState.visibility)}", color = CuciMoneyColors.TextMuted, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("RINCIAN PER KATEGORI", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(expByCat, key = { (emoji, name, _) -> "${emoji}_${name}" }) { (emoji, name, amt) ->
                    val share = if (budget.limitAmount > 0) (amt.toFloat() / budget.limitAmount.toFloat()).coerceIn(0f, 1f) else 0f
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CuciMoneyColors.Card)
                            .padding(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$emoji $name", color = CuciMoneyColors.TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(CuciMoneyHelpers.formatRp(amt, uiState.visibility), color = CuciMoneyColors.Expense, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x14FFFFFF))) {
                            Box(modifier = Modifier.fillMaxWidth(share).height(5.dp).clip(RoundedCornerShape(3.dp)).background(ringColor))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CuciMoneyColors.ExpenseDim)
                        .clickable { onDelete() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = CuciMoneyColors.Expense, modifier = Modifier.size(16.dp))
                        Text("Hapus", color = CuciMoneyColors.Expense, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CuciMoneyColors.Primary)
                        .clickable { onEdit() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Edit, null, tint = Color(0xFF0A1214), modifier = Modifier.size(16.dp))
                        Text("Edit Budget", color = Color(0xFF0A1214), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SetupMasterBudgetModal(
    existing: CuciMasterBudgetEntity?,
    month: Int,
    year: Int,
    onClose: () -> Unit,
    onSave: (limitAmount: Long, startDate: String, endDate: String, note: String) -> Unit
) {
    val defaultStart = remember(month, year) { String.format("%04d-%02d-01", year, month + 1) }
    val defaultEnd = remember(month, year) {
        String.format("%04d-%02d-%02d", year, month + 1, CuciMoneyHelpers.daysInMonth(year, month))
    }

    var amountText by remember(existing) { mutableStateOf(existing?.limitAmount?.toString() ?: "") }
    var startDate by remember(existing) { mutableStateOf(existing?.startDate?.ifBlank { defaultStart } ?: defaultStart) }
    var endDate by remember(existing) { mutableStateOf(existing?.endDate?.ifBlank { defaultEnd } ?: defaultEnd) }
    var note by remember(existing) { mutableStateOf(existing?.note ?: "") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA6000000))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CuciMoneyColors.Card)
                .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(22.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (existing == null) "Atur Master Budget" else "Edit Master Budget",
                color = CuciMoneyColors.TextMain,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "${CuciMoneyHelpers.MONTH_NAMES[month]} $year",
                color = CuciMoneyColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("BATAS PENGELUARAN (RP)", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CuciMoneyColors.Card2)
                    .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = amountText,
                    onValueChange = { v -> amountText = v.filter { it.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(CuciMoneyColors.Primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (amountText.isEmpty()) Text("Contoh: 3000000", color = CuciMoneyColors.TextLight, fontSize = 14.sp)
                        inner()
                    }
                )
            }
            val parsedAmt = amountText.toLongOrNull() ?: 0L
            if (parsedAmt > 0) {
                Text(CuciMoneyHelpers.formatRp(parsedAmt, true), color = CuciMoneyColors.Primary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TANGGAL MULAI", color = CuciMoneyColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CuciMoneyColors.Card2)
                            .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            singleLine = true,
                            textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 12.sp),
                            cursorBrush = SolidColor(CuciMoneyColors.Primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("TANGGAL AKHIR", color = CuciMoneyColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CuciMoneyColors.Card2)
                            .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            singleLine = true,
                            textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 12.sp),
                            cursorBrush = SolidColor(CuciMoneyColors.Primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("CATATAN (OPSIONAL)", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CuciMoneyColors.Card2)
                    .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = true,
                    textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 13.sp),
                    cursorBrush = SolidColor(CuciMoneyColors.Primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (note.isEmpty()) Text("Misal: Hemat bulan ini", color = CuciMoneyColors.TextLight, fontSize = 13.sp)
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CuciMoneyColors.Card2)
                        .clickable { onClose() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Batal", color = CuciMoneyColors.TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CuciMoneyColors.Primary)
                        .clickable { onSave(parsedAmt, startDate.trim(), endDate.trim(), note.trim()) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Simpan", color = Color(0xFF0A1214), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

private data class LainnyaTileItem(
    val icon: String,
    val label: String,
    val count: String? = null,
    val danger: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LainnyaTabContent(
    uiState: CuciMoneyUiState,
    onOpenBooks: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenAllTransactions: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenTabungan: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenMembers: () -> Unit,
    onExportCsv: () -> Unit,
    onBackupJson: () -> Unit,
    onRestoreJson: () -> Unit,
    onResetData: () -> Unit
) {
    val activeBook = CuciMoneyHelpers.activeBook(uiState)
    val bookId = activeBook?.id ?: uiState.currentBookId
    val nonSavingsAccCount = uiState.accounts.count { !it.isSavings }
    val bookTrxCount = uiState.transactions.count { it.bookId == bookId }

    val tiles = listOf(
        LainnyaTileItem("📖", "Buku", "${uiState.books.size}", onClick = onOpenBooks),
        LainnyaTileItem("💳", "Akun", "$nonSavingsAccCount", onClick = onOpenAccounts),
        LainnyaTileItem("🧾", "Transaksi", "$bookTrxCount", onClick = onOpenAllTransactions),
        LainnyaTileItem("🔍", "Cari", onClick = onOpenSearch),
        LainnyaTileItem("🎯", "Budget", onClick = onOpenBudget),
        LainnyaTileItem("🐷", "Tabungan", onClick = onOpenTabungan),
        LainnyaTileItem("🏷️", "Kategori", "${uiState.categories.size}", onClick = onOpenCategories),
        LainnyaTileItem("👥", "Anggota", "${uiState.members.size}", onClick = onOpenMembers),
        LainnyaTileItem("📊", "Export CSV", onClick = onExportCsv),
        LainnyaTileItem("💾", "Backup", onClick = onBackupJson),
        LainnyaTileItem("📥", "Restore", onClick = onRestoreJson),
        LainnyaTileItem("🗑️", "Reset Data", danger = true, onClick = onResetData)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.GridView, null, tint = CuciMoneyColors.Primary, modifier = Modifier.size(22.dp))
                Text("Lainnya", color = CuciMoneyColors.TextMain, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        item {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tiles.forEach { tile ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (tile.danger) CuciMoneyColors.ExpenseDim else CuciMoneyColors.Card)
                            .border(
                                1.dp,
                                if (tile.danger) Color(0x40FF5370) else CuciMoneyColors.Border,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { tile.onClick() }
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(tile.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tile.label,
                            color = if (tile.danger) CuciMoneyColors.Expense else CuciMoneyColors.TextMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tile.count != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tile.count,
                                color = CuciMoneyColors.Primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
