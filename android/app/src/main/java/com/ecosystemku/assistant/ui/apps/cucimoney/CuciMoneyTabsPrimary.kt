package com.ecosystemku.assistant.ui.apps.cucimoney

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.ecosystemku.assistant.data.local.CuciAccountEntity
import com.ecosystemku.assistant.data.local.CuciTransactionEntity
import java.util.Calendar
import kotlin.math.abs

@Composable
internal fun BukuTabContent(
    uiState: CuciMoneyUiState,
    viewMonth: Int,
    viewYear: Int,
    onChangeMonth: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookSheet: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleBudgetCollapsed: () -> Unit,
    onOpenBudgetDetail: (Int, Int) -> Unit,
    onOpenTrxDetail: (CuciTransactionEntity) -> Unit
) {
    val activeBook = CuciMoneyHelpers.activeBook(uiState)
    val bookId = activeBook?.id ?: uiState.currentBookId
    val bookAccIds = remember(activeBook) {
        activeBook?.let { CuciMoneyHelpers.parseAccountIds(it.accountIdsJson).toSet() } ?: emptySet()
    }
    val balance = remember(uiState.accounts, bookAccIds) {
        uiState.accounts.filter { it.id in bookAccIds }.sumOf { it.balance }
    }

    val monthPrefix = remember(viewYear, viewMonth) {
        String.format("%04d-%02d", viewYear, viewMonth + 1)
    }
    val monthTrxs = remember(uiState.transactions, bookId, monthPrefix) {
        uiState.transactions.filter { it.bookId == bookId && it.date.startsWith(monthPrefix) }
    }
    val monthIncome = remember(monthTrxs) {
        monthTrxs.filter { it.transferPair == null && it.direction == "in" }.sumOf { it.amount }
    }
    val monthExpense = remember(monthTrxs) {
        monthTrxs.filter { it.transferPair == null && it.direction == "out" }.sumOf { it.amount }
    }

    val (nowYear, nowMonth) = CuciMoneyHelpers.currentYearMonth()
    val isCurrentMonth = viewYear == nowYear && viewMonth == nowMonth

    val groupedDates = remember(monthTrxs) {
        monthTrxs.groupBy { it.date }
            .toSortedMap(compareByDescending { it })
    }
    val collapsedDates = remember { mutableStateMapOf<String, Boolean>() }

    val catById = remember(uiState.categories) { uiState.categories.associateBy { it.id } }
    val accById = remember(uiState.accounts) { uiState.accounts.associateBy { it.id } }

    val activeBudget = remember(uiState.masterBudgets, bookId, viewMonth, viewYear) {
        uiState.masterBudgets.firstOrNull {
            it.bookId == bookId && it.month == viewMonth && it.year == viewYear
        }
    }

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
                CuciHeaderIconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Cari",
                        tint = CuciMoneyColors.TextMain,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CuciMoneyColors.Card)
                        .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(12.dp))
                        .clickable { onOpenBookSheet() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = CuciMoneyColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = activeBook?.name ?: "Personal",
                        color = CuciMoneyColors.TextMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = CuciMoneyColors.TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                CuciHeaderIconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (uiState.visibility) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = "Visibilitas",
                        tint = CuciMoneyColors.TextMain,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Hero Card
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1A2340), Color(0xFF0D2828))
                        )
                    )
                    .border(1.dp, Color(0x3800BFA5), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CuciCircleNavButton(onClick = { onChangeMonth(-1) }) {
                            Icon(Icons.Outlined.ChevronLeft, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "${CuciMoneyHelpers.MONTH_NAMES[viewMonth].uppercase()} $viewYear",
                            color = CuciMoneyColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        if (!isCurrentMonth) {
                            CuciCircleNavButton(onClick = { onChangeMonth(1) }) {
                                Icon(Icons.Outlined.ChevronRight, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Total Saldo",
                        color = CuciMoneyColors.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CuciMoneyHelpers.formatHero(balance, uiState.visibility),
                        color = CuciMoneyColors.TextMain,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x3D000000))
                            .padding(vertical = 12.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(CuciMoneyColors.Income))
                                Text("Pemasukan", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = CuciMoneyHelpers.formatRp(monthIncome, uiState.visibility),
                                color = CuciMoneyColors.Income,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(CuciMoneyColors.BorderStrong))
                        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(CuciMoneyColors.Expense))
                                Text("Pengeluaran", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = CuciMoneyHelpers.formatRp(monthExpense, uiState.visibility),
                                color = CuciMoneyColors.Expense,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Collapsible Anggaran card
        if (activeBudget != null) {
            item {
                val spent = if (activeBudget.startDate.isNotBlank() && activeBudget.endDate.isNotBlank()) {
                    uiState.transactions
                        .filter { it.bookId == bookId && it.direction == "out" && it.transferPair == null && it.date >= activeBudget.startDate && it.date <= activeBudget.endDate }
                        .sumOf { it.amount }
                } else {
                    monthExpense
                }
                val pct = if (activeBudget.limitAmount > 0) ((spent * 100.0) / activeBudget.limitAmount).toInt() else 0
                val clampedPct = pct.coerceIn(0, 100)
                val remaining = activeBudget.limitAmount - spent
                val (ringColor, statusText, statusBg) = when {
                    pct < 70 -> Triple(CuciMoneyColors.Primary, "Aman", CuciMoneyColors.PrimaryDim)
                    pct <= 100 -> Triple(CuciMoneyColors.Warning, "Waspada", Color(0x26FFAB00))
                    else -> Triple(CuciMoneyColors.Expense, "Melebihi", CuciMoneyColors.ExpenseDim)
                }

                val periodLabel = if (activeBudget.startDate.isNotBlank() && activeBudget.endDate.isNotBlank()) {
                    val left = CuciMoneyHelpers.daysLeftUntil(activeBudget.endDate)
                    if (left != null) "${left}h sisa" else ""
                } else if (isCurrentMonth) {
                    val cal = Calendar.getInstance()
                    val left = CuciMoneyHelpers.daysInMonth(viewYear, viewMonth) - cal.get(Calendar.DAY_OF_MONTH)
                    "${left}h sisa"
                } else {
                    "Selesai"
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleBudgetCollapsed() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.budgetCollapsed) Icons.Outlined.ChevronRight else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = CuciMoneyColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ANGGARAN",
                            color = CuciMoneyColors.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    AnimatedVisibility(visible = !uiState.budgetCollapsed) {
                        Column(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(CuciMoneyColors.Card)
                                .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(18.dp))
                                .clickable { onOpenBudgetDetail(viewMonth, viewYear) }
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(
                                            color = Color(0x14FFFFFF),
                                            style = Stroke(width = 4.dp.toPx())
                                        )
                                        drawArc(
                                            color = ringColor,
                                            startAngle = -90f,
                                            sweepAngle = (clampedPct / 100f) * 360f,
                                            useCenter = false,
                                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    Text(
                                        text = "$pct%",
                                        color = CuciMoneyColors.TextMain,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bulanan",
                                        color = CuciMoneyColors.TextMain,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(statusBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(statusText, color = ringColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        if (periodLabel.isNotBlank()) {
                                            Text(periodLabel, color = CuciMoneyColors.TextLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CuciMoneyHelpers.formatCompact(spent, uiState.visibility),
                                        color = CuciMoneyColors.TextMain,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (remaining >= 0) CuciMoneyHelpers.formatCompact(remaining, uiState.visibility)
                                        else "−" + CuciMoneyHelpers.formatCompact(abs(remaining), uiState.visibility),
                                        color = if (remaining >= 0) CuciMoneyColors.Income else CuciMoneyColors.Expense,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0x14FFFFFF))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(clampedPct / 100f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(ringColor)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "TRANSAKSI",
                color = CuciMoneyColors.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)
            )
        }

        if (groupedDates.isEmpty()) {
            item {
                CuciEmptyState(icon = "📭", text = "Belum ada transaksi bulan ini")
            }
        } else {
            groupedDates.forEach { (dateStr, rawItems) ->
                val dayIn = rawItems.filter { it.transferPair == null && it.direction == "in" }.sumOf { it.amount }
                val dayOut = rawItems.filter { it.transferPair == null && it.direction == "out" }.sumOf { it.amount }
                val sortedItems = rawItems.sortedByDescending { it.createdAt }
                val isCollapsed = collapsedDates[dateStr] == true

                // Collapse transfer pairs
                val seenPairs = mutableSetOf<Long>()
                val displayItems = sortedItems.filter { t ->
                    val p = t.transferPair
                    if (p != null) {
                        if (p in seenPairs) false else {
                            seenPairs.add(p)
                            true
                        }
                    } else true
                }

                item(key = "grp_$dateStr") {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(CuciMoneyColors.Card)
                            .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(18.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { collapsedDates[dateStr] = !isCollapsed }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCollapsed) Icons.Outlined.ChevronRight else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = CuciMoneyColors.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = CuciMoneyHelpers.formatGroupDayLabel(dateStr),
                                color = CuciMoneyColors.TextMain,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                if (dayIn > 0L) {
                                    Text(
                                        text = "+${CuciMoneyHelpers.formatRp(dayIn, uiState.visibility)}",
                                        color = CuciMoneyColors.Income,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (dayOut > 0L) {
                                    Text(
                                        text = "-${CuciMoneyHelpers.formatRp(dayOut, uiState.visibility)}",
                                        color = CuciMoneyColors.Expense,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (!isCollapsed) {
                            displayItems.forEach { t ->
                                HorizontalDivider(color = CuciMoneyColors.Border)
                                val isTransfer = t.transferPair != null
                                if (isTransfer) {
                                    val pair = uiState.transactions.firstOrNull { it.transferPair == t.transferPair && it.id != t.id }
                                    val outTrx = if (t.direction == "out") t else pair
                                    val inTrx = if (t.direction == "in") t else pair
                                    val fromAcc = accById[outTrx?.accountId]?.name ?: "Akun"
                                    val toAcc = accById[inTrx?.accountId]?.name ?: "Akun"
                                    CuciTransactionRow(
                                        emoji = "⇄",
                                        iconBg = Color(0x247C8FF5),
                                        title = "$fromAcc → $toAcc",
                                        subtitle = "Transfer",
                                        amountText = CuciMoneyHelpers.formatRp(t.amount, uiState.visibility),
                                        amountColor = CuciMoneyColors.Transfer,
                                        onClick = { onOpenTrxDetail(outTrx ?: t) }
                                    )
                                } else {
                                    val cat = catById[t.categoryId]
                                    val acc = accById[t.accountId]
                                    val isIn = t.direction == "in"
                                    val sign = if (isIn) "+" else "−"
                                    CuciTransactionRow(
                                        emoji = cat?.emoji ?: "📌",
                                        iconBg = if (isIn) CuciMoneyColors.IncomeDim else CuciMoneyColors.ExpenseDim,
                                        title = t.title,
                                        subtitle = "${cat?.name ?: "Lainnya"} · ${acc?.name ?: ""}",
                                        amountText = "$sign ${CuciMoneyHelpers.formatRp(t.amount, uiState.visibility)}",
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
}

@Composable
internal fun DompetTabContent(
    uiState: CuciMoneyUiState,
    onToggleVisibility: () -> Unit,
    onOpenAccountDetail: (CuciAccountEntity) -> Unit,
    onOpenAddAccount: () -> Unit,
    onOpenTabungan: () -> Unit
) {
    val activeBook = CuciMoneyHelpers.activeBook(uiState)
    val bookAccIds = remember(activeBook) {
        activeBook?.let { CuciMoneyHelpers.parseAccountIds(it.accountIdsJson).toSet() } ?: emptySet()
    }
    val activeAccounts = remember(uiState.accounts, bookAccIds) {
        uiState.accounts.filter { it.id in bookAccIds }
    }
    val savingsAcc = remember(uiState.accounts) { uiState.accounts.firstOrNull { it.isSavings } }
    val savingsBal = savingsAcc?.balance ?: 0L

    var asset = 0L
    var liability = 0L
    activeAccounts.forEach { a ->
        if (a.type == "credit") liability += abs(a.balance) else asset += a.balance
    }
    asset += savingsBal
    val netWorth = asset - liability

    val grouped = remember(activeAccounts) {
        activeAccounts.groupBy { CuciMoneyHelpers.accountGroup(it.type) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AccountBalanceWallet, null, tint = CuciMoneyColors.Primary, modifier = Modifier.size(22.dp))
                    Text("Dompet", color = CuciMoneyColors.TextMain, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
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

        // Wallet Hero
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A2340), Color(0xFF0D2828))))
                    .border(1.dp, Color(0x3800BFA5), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Aset Bersih", color = CuciMoneyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CuciMoneyHelpers.formatRp(netWorth, uiState.visibility),
                        color = CuciMoneyColors.TextMain,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x3D000000))
                            .padding(vertical = 12.dp, horizontal = 14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Aset", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = CuciMoneyHelpers.formatRp(asset, uiState.visibility),
                                color = CuciMoneyColors.Income,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(modifier = Modifier.width(1.dp).height(28.dp).background(CuciMoneyColors.BorderStrong))
                        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                            Text("Hutang", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = CuciMoneyHelpers.formatRp(liability, uiState.visibility),
                                color = CuciMoneyColors.Expense,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "AKUN SAYA",
                color = CuciMoneyColors.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)
            )
        }

        if (activeAccounts.isEmpty()) {
            item {
                CuciEmptyState(icon = "💼", text = "Belum ada akun di buku ini")
            }
        } else {
            CuciMoneyHelpers.ACC_GROUP_ORDER.forEach { catGroup ->
                val list = grouped[catGroup].orEmpty()
                if (list.isNotEmpty()) {
                    item(key = "acc_grp_$catGroup") {
                        val catTotal = list.sumOf { it.balance }
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 5.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(CuciMoneyColors.Card)
                                .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(18.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(catGroup, color = CuciMoneyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Aset: ${CuciMoneyHelpers.formatRp(catTotal, uiState.visibility)}",
                                    color = CuciMoneyColors.TextMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            list.forEach { acc ->
                                HorizontalDivider(color = CuciMoneyColors.Border)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenAccountDetail(acc) }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CuciMoneyColors.PrimaryDim),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(CuciMoneyHelpers.accountIcon(acc), fontSize = 19.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(acc.name, color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        if (acc.notes.isNotBlank()) {
                                            Text(
                                                acc.notes,
                                                color = CuciMoneyColors.TextMuted,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = CuciMoneyHelpers.formatRp(acc.balance, uiState.visibility),
                                            color = CuciMoneyColors.TextMain,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("IDR(1.0)", color = CuciMoneyColors.TextLight, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (savingsAcc != null && savingsBal > 0L) {
            item(key = "acc_grp_savings") {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CuciMoneyColors.Card)
                        .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tabungan", color = CuciMoneyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Aset: ${CuciMoneyHelpers.formatRp(savingsBal, uiState.visibility)}",
                            color = CuciMoneyColors.TextMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    HorizontalDivider(color = CuciMoneyColors.Border)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTabungan() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CuciMoneyColors.PrimaryDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🐷", fontSize = 19.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tabungan Wishlist", color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Terkunci sampai target tercapai", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CuciMoneyHelpers.formatRp(savingsBal, uiState.visibility),
                                color = CuciMoneyColors.TextMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text("IDR(1.0)", color = CuciMoneyColors.TextLight, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CuciMoneyColors.Card)
                    .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(16.dp))
                    .clickable { onOpenAddAccount() }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Add, null, tint = CuciMoneyColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah Akun", color = CuciMoneyColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun AccountDetailModal(
    uiState: CuciMoneyUiState,
    account: CuciAccountEntity,
    initialMonth: Int,
    initialYear: Int,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onOpenTrxDetail: (CuciTransactionEntity) -> Unit
) {
    var detailMonth by remember(account.id) { mutableIntStateOf(initialMonth) }
    var detailYear by remember(account.id) { mutableIntStateOf(initialYear) }
    var detailTab by remember { mutableStateOf("analysis") }

    val bookId = CuciMoneyHelpers.activeBook(uiState)?.id ?: uiState.currentBookId
    val catById = remember(uiState.categories) { uiState.categories.associateBy { it.id } }

    val monthPrefix = remember(detailYear, detailMonth) {
        String.format("%04d-%02d", detailYear, detailMonth + 1)
    }
    val monthTrx = remember(uiState.transactions, bookId, account.id, monthPrefix) {
        uiState.transactions
            .filter { it.bookId == bookId && it.accountId == account.id && it.date.startsWith(monthPrefix) }
            .sortedByDescending { it.date }
    }
    val income = monthTrx.filter { it.direction == "in" }.sumOf { it.amount }
    val expense = monthTrx.filter { it.direction == "out" }.sumOf { it.amount }
    val total = income + expense

    // 6-month trend centered on selected month
    val chartData = remember(uiState.transactions, bookId, account.id, account.balance, detailMonth, detailYear) {
        val labels = mutableListOf<String>()
        val netFlows = mutableListOf<Long>()
        for (i in 5 downTo 0) {
            var m = detailMonth - i
            var y = detailYear
            while (m < 0) { m += 12; y-- }
            while (m > 11) { m -= 12; y++ }
            val prefix = String.format("%04d-%02d", y, m + 1)
            val mTrx = uiState.transactions.filter { it.bookId == bookId && it.accountId == account.id && it.date.startsWith(prefix) }
            val inc = mTrx.filter { it.direction == "in" }.sumOf { it.amount }
            val exp = mTrx.filter { it.direction == "out" }.sumOf { it.amount }
            labels.add(CuciMoneyHelpers.MONTH_SHORT[m])
            netFlows.add(inc - exp)
        }
        val running = LongArray(6)
        var bal = account.balance
        for (i in 5 downTo 0) {
            running[i] = bal.coerceAtLeast(0L)
            bal -= netFlows[i]
        }
        labels to running.toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CuciMoneyColors.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CuciHeaderIconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(18.dp))
            }
            Text("Detail Akun", color = CuciMoneyColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            CuciHeaderIconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, null, tint = CuciMoneyColors.Primary, modifier = Modifier.size(18.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Top Hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF1A2340), Color(0xFF0D1A2E))))
                    .border(1.dp, CuciMoneyColors.Border)
                    .padding(vertical = 22.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = CuciMoneyHelpers.accountTypeLabel(account.type).uppercase(),
                    color = CuciMoneyColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(CuciMoneyHelpers.accountIcon(account), fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(account.name, color = CuciMoneyColors.TextMain, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = account.notes.ifBlank { "—" },
                    color = CuciMoneyColors.TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = CuciMoneyHelpers.formatRp(account.balance, uiState.visibility),
                    color = CuciMoneyColors.Primary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("IDR", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            // Month Stat Card
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CuciMoneyColors.Card)
                    .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CuciCircleNavButton(onClick = {
                        if (detailMonth == 0) { detailMonth = 11; detailYear-- } else detailMonth--
                    }) {
                        Icon(Icons.Outlined.ChevronLeft, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${CuciMoneyHelpers.MONTH_SHORT[detailMonth].uppercase()} $detailYear",
                        color = CuciMoneyColors.TextMain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    CuciCircleNavButton(onClick = {
                        if (detailMonth == 11) { detailMonth = 0; detailYear++ } else detailMonth++
                    }) {
                        Icon(Icons.Outlined.ChevronRight, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(color = CuciMoneyColors.Border)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.weight(1f).padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("PEMASUKAN", color = CuciMoneyColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(CuciMoneyHelpers.formatRp(income, uiState.visibility), color = CuciMoneyColors.Income, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(if (total > 0) "${(income * 100 / total)}%" else "—", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                    }
                    Box(modifier = Modifier.width(1.dp).height(72.dp).background(CuciMoneyColors.Border))
                    Column(
                        modifier = Modifier.weight(1f).padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("PENGELUARAN", color = CuciMoneyColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(CuciMoneyHelpers.formatRp(expense, uiState.visibility), color = CuciMoneyColors.Expense, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(if (total > 0) "${(expense * 100 / total)}%" else "—", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                    }
                }
            }

            // Tab switcher
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CuciMoneyColors.Card)
                    .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("analysis" to "Analisis", "detail" to "Detail").forEach { (key, label) ->
                    val sel = detailTab == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (sel) CuciMoneyColors.Primary else Color.Transparent)
                            .clickable { detailTab = key }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (sel) Color(0xFF0A1214) else CuciMoneyColors.TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (detailTab == "analysis") {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CuciMoneyColors.Card)
                        .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "TREND SALDO 6 BULAN",
                        color = CuciMoneyColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    AccountBalanceLineChart(
                        labels = chartData.first,
                        values = chartData.second
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CuciMoneyColors.Card)
                        .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(18.dp))
                ) {
                    if (monthTrx.isEmpty()) {
                        CuciEmptyState(icon = "📭", text = "Belum ada transaksi bulan ini")
                    } else {
                        monthTrx.forEachIndexed { idx, t ->
                            if (idx > 0) HorizontalDivider(color = CuciMoneyColors.Border)
                            val cat = catById[t.categoryId]
                            val isIn = t.direction == "in"
                            val sign = if (isIn) "+" else "−"
                            CuciTransactionRow(
                                emoji = cat?.emoji ?: "📌",
                                iconBg = if (isIn) CuciMoneyColors.IncomeDim else CuciMoneyColors.ExpenseDim,
                                title = t.title,
                                subtitle = cat?.name ?: "Lainnya",
                                amountText = "$sign ${CuciMoneyHelpers.formatRp(t.amount, uiState.visibility)}",
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

@Composable
private fun AccountBalanceLineChart(
    labels: List<String>,
    values: List<Long>
) {
    val maxVal = (values.maxOrNull() ?: 1L).coerceAtLeast(1L)
    val minVal = (values.minOrNull() ?: 0L).coerceAtMost(0L)
    val range = (maxVal - minVal).coerceAtLeast(1L).toFloat()

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val padH = 12.dp.toPx()
            val padV = 14.dp.toPx()
            val gW = size.width - padH * 2
            val gH = size.height - padV * 2
            val n = values.size.coerceAtLeast(2)

            for (g in 0..3) {
                val y = padV + (g / 3f) * gH
                drawLine(
                    color = Color(0x14FFFFFF),
                    start = Offset(padH, y),
                    end = Offset(size.width - padH, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            fun px(i: Int) = padH + (i.toFloat() / (n - 1)) * gW
            fun py(v: Long) = padV + gH * (1f - ((v - minVal).toFloat() / range))

            val area = Path().apply {
                moveTo(px(0), py(values[0]))
                for (i in 1 until values.size) lineTo(px(i), py(values[i]))
                lineTo(px(values.lastIndex), padV + gH)
                lineTo(px(0), padV + gH)
                close()
            }
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x5500BFA5), Color(0x0000BFA5)),
                    startY = padV,
                    endY = padV + gH
                )
            )

            val line = Path().apply {
                moveTo(px(0), py(values[0]))
                for (i in 1 until values.size) lineTo(px(i), py(values[i]))
            }
            drawPath(
                path = line,
                color = CuciMoneyColors.Primary,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            for (i in values.indices) {
                drawCircle(
                    color = CuciMoneyColors.Primary,
                    radius = 4.dp.toPx(),
                    center = Offset(px(i), py(values[i]))
                )
                drawCircle(
                    color = CuciMoneyColors.Bg,
                    radius = 2.dp.toPx(),
                    center = Offset(px(i), py(values[i]))
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { lbl ->
                Text(lbl, color = CuciMoneyColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddEditAccountModal(
    editingAccount: CuciAccountEntity?,
    currentBookAccountIds: Set<Long>,
    onClose: () -> Unit,
    onSave: (
        name: String,
        type: String,
        icon: String?,
        balance: Long,
        notes: String,
        useInBook: Boolean,
        includeAsset: Boolean
    ) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember(editingAccount?.id) { mutableStateOf(editingAccount?.name ?: "") }
    var balanceText by remember(editingAccount?.id) { mutableStateOf(editingAccount?.balance?.toString() ?: "") }
    var notes by remember(editingAccount?.id) { mutableStateOf(editingAccount?.notes ?: "") }
    var selectedType by remember(editingAccount?.id) { mutableStateOf(editingAccount?.type ?: "cash") }
    var customIcon by remember(editingAccount?.id) { mutableStateOf(editingAccount?.icon) }
    var useInBook by remember(editingAccount?.id) {
        mutableStateOf(
            if (editingAccount != null) editingAccount.id in currentBookAccountIds && editingAccount.useInBook
            else true
        )
    }
    var includeAsset by remember(editingAccount?.id) { mutableStateOf(editingAccount?.includeAsset ?: true) }
    var showIconPicker by remember { mutableStateOf(false) }

    val typeDef = CuciMoneyHelpers.ACCOUNT_TYPES.firstOrNull { it.id == selectedType }
    val previewIcon = customIcon?.takeIf { it.isNotBlank() } ?: typeDef?.icon ?: "💵"
    val previewBalance = balanceText.toLongOrNull() ?: 0L

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CuciMoneyColors.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CuciHeaderIconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = if (editingAccount != null) "Edit Akun" else "Tambah Akun",
                    color = CuciMoneyColors.TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (editingAccount != null && onDelete != null) {
                    CuciHeaderIconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = CuciMoneyColors.Expense, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.size(38.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Preview Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CuciMoneyColors.Card)
                        .border(1.dp, CuciMoneyColors.Border)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CuciMoneyColors.PrimaryDim)
                            .clickable { showIconPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(previewIcon, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name.ifBlank { "Akun Baru" },
                            color = CuciMoneyColors.TextMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${typeDef?.label ?: "Tunai"} · Tap ikon untuk ubah",
                            color = CuciMoneyColors.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = CuciMoneyHelpers.formatRp(previewBalance, true),
                        color = CuciMoneyColors.TextMain,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Name Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📝", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Nama Akun", color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = CuciMoneyColors.TextMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        ),
                        cursorBrush = SolidColor(CuciMoneyColors.Primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (name.isEmpty()) {
                                    Text("Misal: BCA", color = CuciMoneyColors.TextLight, fontSize = 14.sp)
                                }
                                inner()
                            }
                        }
                    )
                }
                HorizontalDivider(color = CuciMoneyColors.Border)

                // Balance Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💵", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Saldo Awal", color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = balanceText,
                        onValueChange = { v -> balanceText = v.filter { it.isDigit() || it == '-' }.take(13) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            color = CuciMoneyColors.TextMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End
                        ),
                        cursorBrush = SolidColor(CuciMoneyColors.Primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (balanceText.isEmpty()) {
                                    Text("0", color = CuciMoneyColors.TextLight, fontSize = 14.sp)
                                }
                                inner()
                            }
                        }
                    )
                }
                HorizontalDivider(color = CuciMoneyColors.Border)

                // Account Type Grid
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                        text = "TIPE AKUN",
                        color = CuciMoneyColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        maxItemsInEachRow = 3,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CuciMoneyHelpers.ACCOUNT_TYPES.forEach { t ->
                            val sel = selectedType == t.id
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (sel) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card)
                                    .border(
                                        1.5.dp,
                                        if (sel) CuciMoneyColors.Primary else CuciMoneyColors.Border,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        selectedType = t.id
                                        customIcon = null
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(t.icon, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = t.label,
                                    color = if (sel) CuciMoneyColors.Primary else CuciMoneyColors.TextMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = CuciMoneyColors.Border)

                // Notes
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text("CATATAN KECIL", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CuciMoneyColors.Card2)
                            .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 13.sp),
                            cursorBrush = SolidColor(CuciMoneyColors.Primary),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (notes.isEmpty()) {
                                    Text("Contoh: untuk tabungan darurat", color = CuciMoneyColors.TextLight, fontSize = 13.sp)
                                }
                                inner()
                            }
                        )
                    }
                }
                HorizontalDivider(color = CuciMoneyColors.Border)

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📊", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gunakan dalam buku saat ini", color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Ditampilkan & disertakan dalam statistik", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = useInBook,
                        onCheckedChange = { useInBook = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CuciMoneyColors.Primary)
                    )
                }
                HorizontalDivider(color = CuciMoneyColors.Border)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💰", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Termasuk dalam perhitungan aset", color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Masuk dalam aset atau liabilitas", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = includeAsset,
                        onCheckedChange = { includeAsset = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CuciMoneyColors.Primary)
                    )
                }
            }

            // Footer buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CuciMoneyColors.Card)
                    .border(1.dp, CuciMoneyColors.Border)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CuciMoneyColors.Card2)
                        .clickable { onClose() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Batal", color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CuciMoneyColors.Primary)
                        .clickable {
                            onSave(
                                name.trim(),
                                selectedType,
                                customIcon,
                                balanceText.toLongOrNull() ?: 0L,
                                notes.trim(),
                                useInBook,
                                includeAsset
                            )
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Simpan", color = Color(0xFF0A1214), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        if (showIconPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable { showIconPicker = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .background(CuciMoneyColors.Card)
                        .clickable(enabled = false) {}
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PILIH IKON AKUN", color = CuciMoneyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (customIcon != null) {
                                Text(
                                    text = "Reset",
                                    color = CuciMoneyColors.Primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CuciMoneyColors.PrimaryDim)
                                        .clickable { customIcon = null; showIconPicker = false }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Icon(
                                Icons.Outlined.Close,
                                null,
                                tint = CuciMoneyColors.TextMuted,
                                modifier = Modifier.size(20.dp).clickable { showIconPicker = false }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CuciMoneyHelpers.ACC_ICON_GROUPS.forEach { grp ->
                            item {
                                Text(grp.label, color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    grp.emojis.forEach { emoji ->
                                        val isSel = customIcon == emoji
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSel) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card2)
                                                .border(
                                                    1.5.dp,
                                                    if (isSel) CuciMoneyColors.Primary else Color.Transparent,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    customIcon = emoji
                                                    showIconPicker = false
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(emoji, fontSize = 20.sp)
                                        }
                                    }
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
internal fun CuciHeaderIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CuciMoneyColors.Card)
            .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
internal fun CuciCircleNavButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color(0x14FFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
internal fun CuciTransactionRow(
    emoji: String,
    iconBg: Color,
    title: String,
    subtitle: String,
    amountText: String,
    amountColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CuciMoneyColors.TextMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = CuciMoneyColors.TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = amountText,
            color = amountColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun CuciEmptyState(icon: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 44.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 36.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = text,
            color = CuciMoneyColors.TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
