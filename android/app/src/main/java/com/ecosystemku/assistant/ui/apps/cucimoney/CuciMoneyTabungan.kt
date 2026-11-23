package com.ecosystemku.assistant.ui.apps.cucimoney

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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystemku.assistant.data.local.CuciWishlistEntity
import kotlin.math.roundToInt

@Composable
internal fun TabunganViewContent(
    uiState: CuciMoneyUiState,
    onBack: () -> Unit,
    onToggleVisibility: () -> Unit,
    onOpenNewWishlist: () -> Unit,
    onOpenDetail: (CuciWishlistEntity) -> Unit,
    onOpenDeposit: (CuciWishlistEntity) -> Unit,
    onOpenWithdraw: (CuciWishlistEntity, Boolean) -> Unit,
    onOpenEditWishlist: (CuciWishlistEntity) -> Unit
) {
    val active = remember(uiState.wishlists) { uiState.wishlists.filter { it.status != "Selesai" } }
    val done = remember(uiState.wishlists) { uiState.wishlists.filter { it.status == "Selesai" } }

    val readyList = remember(active) {
        active.filter { it.price > 0L && it.saved >= it.price }
            .sortedBy { CuciMoneyHelpers.svPriorityRank(it.priority) }
    }
    val savingList = remember(active) {
        active.filter { it.saved > 0L && (it.price <= 0L || it.saved < it.price) }
            .sortedBy { CuciMoneyHelpers.svPriorityRank(it.priority) }
    }
    val idleList = remember(active) {
        active.filter { it.saved == 0L }
            .sortedBy { CuciMoneyHelpers.svPriorityRank(it.priority) }
    }

    val totalSaved = remember(uiState.accounts, uiState.wishlists) {
        uiState.accounts.firstOrNull { it.isSavings }?.balance ?: active.sumOf { it.saved }
    }
    val totalTarget = remember(active) { active.filter { it.price > 0L }.sumOf { it.price } }
    val overallPct = if (totalTarget > 0L) ((totalSaved * 100.0) / totalTarget).roundToInt().coerceIn(0, 100) else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CuciHeaderIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(18.dp))
                    }
                    Text("Tabungan Wishlist", color = CuciMoneyColors.TextMain, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CuciHeaderIconButton(onClick = onToggleVisibility) {
                        Icon(
                            if (uiState.visibility) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            null,
                            tint = CuciMoneyColors.TextMain,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    CuciHeaderIconButton(onClick = onOpenNewWishlist) {
                        Icon(Icons.Outlined.Add, null, tint = CuciMoneyColors.Primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Hero Card
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1A2340), Color(0xFF0D2828))))
                    .border(1.dp, Color(0x3800BFA5), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🐷", fontSize = 20.sp)
                    Text("Total Tabungan Terkumpul", color = CuciMoneyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = CuciMoneyHelpers.formatRp(totalSaved, uiState.visibility),
                    color = CuciMoneyColors.TextMain,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (totalTarget > 0L) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Target aktif: ${CuciMoneyHelpers.formatCompact(totalTarget, uiState.visibility)}", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                        Text("$overallPct%", color = CuciMoneyColors.Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x26FFFFFF))) {
                        Box(modifier = Modifier.fillMaxWidth(overallPct / 100f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(CuciMoneyColors.Primary))
                    }
                }
            }
        }

        if (uiState.wishlists.isEmpty()) {
            item {
                CuciEmptyState(icon = "🐷", text = "Belum ada target tabungan.\nTekan + untuk membuat wishlist tabungan.")
            }
        }

        if (readyList.isNotEmpty()) {
            item { WishlistSectionTitle("🎉 Siap dicairkan (${readyList.size})") }
            items(readyList, key = { "ready_${it.id}" }) { w ->
                WishlistCard(
                    item = w,
                    visible = uiState.visibility,
                    actionLabel = "Cairkan",
                    actionPrimary = true,
                    onClick = { onOpenDetail(w) },
                    onAction = { onOpenWithdraw(w, true) }
                )
            }
        }

        if (savingList.isNotEmpty()) {
            item { WishlistSectionTitle("Sedang menabung (${savingList.size})") }
            items(savingList, key = { "saving_${it.id}" }) { w ->
                WishlistCard(
                    item = w,
                    visible = uiState.visibility,
                    actionLabel = "+ Nabung",
                    actionPrimary = true,
                    onClick = { onOpenDetail(w) },
                    onAction = { onOpenDeposit(w) }
                )
            }
        }

        if (idleList.isNotEmpty()) {
            item { WishlistSectionTitle("Belum mulai (${idleList.size})") }
            items(idleList, key = { "idle_${it.id}" }) { w ->
                val hasPrice = w.price > 0L
                WishlistCard(
                    item = w,
                    visible = uiState.visibility,
                    actionLabel = if (hasPrice) "Mulai nabung" else "Atur target",
                    actionPrimary = hasPrice,
                    onClick = { onOpenDetail(w) },
                    onAction = { if (hasPrice) onOpenDeposit(w) else onOpenEditWishlist(w) }
                )
            }
        }

        if (done.isNotEmpty()) {
            item { WishlistSectionTitle("Sudah terbeli (${done.size})") }
            items(done, key = { "done_${it.id}" }) { w ->
                WishlistCard(
                    item = w,
                    visible = uiState.visibility,
                    actionLabel = "Selesai",
                    actionPrimary = false,
                    onClick = { onOpenDetail(w) },
                    onAction = { onOpenDetail(w) }
                )
            }
        }
    }
}

@Composable
private fun WishlistSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = CuciMoneyColors.TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun WishlistCard(
    item: CuciWishlistEntity,
    visible: Boolean,
    actionLabel: String,
    actionPrimary: Boolean,
    onClick: () -> Unit,
    onAction: () -> Unit
) {
    val pct = if (item.price > 0L) ((item.saved * 100.0) / item.price).roundToInt().coerceIn(0, 100) else 0
    val pColor = CuciMoneyHelpers.svPriorityColor(item.priority)

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CuciMoneyColors.Card)
            .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CuciMoneyColors.PrimaryDim),
                contentAlignment = Alignment.Center
            ) {
                Text(CuciMoneyHelpers.svIconEmoji(item.image), fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = CuciMoneyColors.TextMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(pColor.copy(alpha = 0.16f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.priority, color = pColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    if (item.price > 0L) {
                        Text("Target ${CuciMoneyHelpers.formatCompact(item.price, visible)}", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                    } else {
                        Text("Belum ada target harga", color = CuciMoneyColors.TextLight, fontSize = 11.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (actionPrimary) CuciMoneyColors.Primary else CuciMoneyColors.Card2)
                    .clickable { onAction() }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = actionLabel,
                    color = if (actionPrimary) Color(0xFF0A1214) else CuciMoneyColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        if (item.price > 0L && item.status != "Selesai") {
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(CuciMoneyHelpers.formatRp(item.saved, visible), color = CuciMoneyColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("$pct%", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(5.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x14FFFFFF))) {
                Box(modifier = Modifier.fillMaxWidth(pct / 100f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(CuciMoneyColors.Primary))
            }
        }
    }
}

@Composable
internal fun WishlistDetailSheet(
    item: CuciWishlistEntity,
    visible: Boolean,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDeposit: () -> Unit,
    onWithdraw: (realize: Boolean) -> Unit
) {
    val history = remember(item.historyJson) { CuciMoneyHelpers.parseWishlistHistory(item.historyJson).sortedByDescending { it.ts } }
    val pct = if (item.price > 0L) ((item.saved * 100.0) / item.price).roundToInt().coerceIn(0, 100) else 0

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
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CuciMoneyColors.PrimaryDim),
                    contentAlignment = Alignment.Center
                ) {
                    Text(CuciMoneyHelpers.svIconEmoji(item.image), fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, color = CuciMoneyColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${item.priority} · ${item.status}", color = CuciMoneyColors.TextMuted, fontSize = 12.sp)
                }
                Icon(Icons.Outlined.Edit, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(20.dp).clickable { onEdit() })
                Spacer(modifier = Modifier.width(10.dp))
                Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(22.dp).clickable { onClose() })
            }

            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CuciMoneyColors.Card)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Terkumpul", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                        Text(CuciMoneyHelpers.formatRp(item.saved, visible), color = CuciMoneyColors.Primary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                        Text(
                            if (item.price > 0L) CuciMoneyHelpers.formatRp(item.price, visible) else "Belum diatur",
                            color = CuciMoneyColors.TextMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (item.price > 0L) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x14FFFFFF))) {
                        Box(modifier = Modifier.fillMaxWidth(pct / 100f).height(7.dp).clip(RoundedCornerShape(4.dp)).background(CuciMoneyColors.Primary))
                    }
                }
            }

            if (item.status != "Selesai") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CuciMoneyColors.Primary)
                            .clickable { onDeposit() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ Nabung", color = Color(0xFF0A1214), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    if (item.saved > 0L) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CuciMoneyColors.Card2)
                                .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(12.dp))
                                .clickable { onWithdraw(true) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Cairkan", color = CuciMoneyColors.TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("RIWAYAT TABUNGAN (${history.size})", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (history.isEmpty()) {
                Text("Belum ada riwayat setoran", color = CuciMoneyColors.TextLight, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(history, key = { h -> "${h.ts}_${h.type}_${h.amount}_${h.accountName}" }) { h ->
                        val isIn = h.type == "in"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CuciMoneyColors.Card)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isIn) "Setoran dari ${h.accountName}" else if (h.kind == "realize") "Dicairkan ke ${h.accountName}" else "Dikembalikan ke ${h.accountName}",
                                    color = CuciMoneyColors.TextMain,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(CuciMoneyHelpers.formatDateShort(h.date), color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                            }
                            Text(
                                text = "${if (isIn) "+" else "−"}${CuciMoneyHelpers.formatRp(h.amount, visible)}",
                                color = if (isIn) CuciMoneyColors.Income else CuciMoneyColors.Expense,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WishlistFormSheet(
    existing: CuciWishlistEntity?,
    onClose: () -> Unit,
    onSave: (title: String, price: Long, priority: String, image: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var priceText by remember(existing) { mutableStateOf(existing?.price?.takeIf { it > 0L }?.toString() ?: "") }
    var priority by remember(existing) { mutableStateOf(existing?.priority ?: "Nanti") }
    var image by remember(existing) { mutableStateOf(existing?.image ?: "bag") }

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
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CuciMoneyColors.Bg)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (existing == null) "Wishlist Baru" else "Edit Wishlist",
                    color = CuciMoneyColors.TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (existing != null && onDelete != null) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = CuciMoneyColors.Expense, modifier = Modifier.size(20.dp).clickable { onDelete() })
                    }
                    Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(22.dp).clickable { onClose() })
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("NAMA TARGET", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CuciMoneyColors.Card)
                    .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(CuciMoneyColors.Primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (title.isEmpty()) Text("Contoh: Laptop baru, Liburan...", color = CuciMoneyColors.TextLight, fontSize = 14.sp)
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("HARGA TARGET (RP)", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CuciMoneyColors.Card)
                    .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 11.dp)
            ) {
                BasicTextField(
                    value = priceText,
                    onValueChange = { v -> priceText = v.filter { it.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(CuciMoneyColors.Primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (priceText.isEmpty()) Text("0", color = CuciMoneyColors.TextLight, fontSize = 14.sp)
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("PRIORITAS", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CuciMoneyHelpers.SV_PRIORITIES.forEach { p ->
                    val sel = priority == p
                    val col = CuciMoneyHelpers.svPriorityColor(p)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) col.copy(alpha = 0.2f) else CuciMoneyColors.Card)
                            .border(1.dp, if (sel) col else CuciMoneyColors.Border, RoundedCornerShape(10.dp))
                            .clickable { priority = p }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(p, color = if (sel) col else CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("IKON", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CuciMoneyHelpers.SV_ICONS.forEach { ic ->
                    val sel = image == ic.key
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (sel) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card)
                            .border(1.5.dp, if (sel) CuciMoneyColors.Primary else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { image = ic.key },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ic.emoji, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CuciMoneyColors.Primary)
                    .clickable {
                        if (title.isNotBlank()) {
                            onSave(title.trim(), priceText.toLongOrNull() ?: 0L, priority, image)
                        }
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Simpan Wishlist", color = Color(0xFF0A1214), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun WishlistDepositSheet(
    uiState: CuciMoneyUiState,
    item: CuciWishlistEntity,
    onClose: () -> Unit,
    onDeposit: (accountId: Long, amount: Long) -> Unit
) {
    val activeBook = CuciMoneyHelpers.activeBook(uiState)
    val bookAccIds = remember(activeBook) {
        activeBook?.let { CuciMoneyHelpers.parseAccountIds(it.accountIdsJson).toSet() } ?: emptySet()
    }
    val sourceAccounts = remember(uiState.accounts, bookAccIds) {
        val list = uiState.accounts.filter { !it.isSavings && it.id in bookAccIds }
        list.ifEmpty { uiState.accounts.filter { !it.isSavings } }
    }
    var selectedAccId by remember { mutableLongStateOf(sourceAccounts.firstOrNull()?.id ?: 1L) }
    var amountText by remember { mutableStateOf("") }
    val remainingNeeded = (item.price - item.saved).coerceAtLeast(0L)

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
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CuciMoneyColors.Bg)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Nabung: ${item.title}", color = CuciMoneyColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(22.dp).clickable { onClose() })
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("NOMINAL SETORAN (RP)", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CuciMoneyColors.Card)
                    .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
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
                        if (amountText.isEmpty()) Text("0", color = CuciMoneyColors.TextLight, fontSize = 16.sp)
                        inner()
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(50_000L to "50rb", 100_000L to "100rb", 500_000L to "500rb").forEach { (amt, lbl) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CuciMoneyColors.Card2)
                            .clickable { amountText = amt.toString() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(lbl, color = CuciMoneyColors.TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (remainingNeeded > 0L) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CuciMoneyColors.PrimaryDim)
                            .clickable { amountText = remainingNeeded.toString() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Pas sisa", color = CuciMoneyColors.Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text("SUMBER AKUN", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 190.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(sourceAccounts, key = { it.id }) { acc ->
                    val sel = acc.id == selectedAccId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card)
                            .border(1.dp, if (sel) CuciMoneyColors.Primary else CuciMoneyColors.Border, RoundedCornerShape(12.dp))
                            .clickable { selectedAccId = acc.id }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(CuciMoneyHelpers.accountIcon(acc), fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(acc.name, color = CuciMoneyColors.TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(CuciMoneyHelpers.formatRp(acc.balance, true), color = CuciMoneyColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CuciMoneyColors.Primary)
                    .clickable {
                        val amt = amountText.toLongOrNull() ?: 0L
                        if (amt > 0L) onDeposit(selectedAccId, amt)
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Transfer ke Tabungan", color = Color(0xFF0A1214), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun WishlistWithdrawSheet(
    uiState: CuciMoneyUiState,
    item: CuciWishlistEntity,
    realizeDefault: Boolean,
    onClose: () -> Unit,
    onConfirm: (accountId: Long, realize: Boolean, markDone: Boolean) -> Unit
) {
    val activeBook = CuciMoneyHelpers.activeBook(uiState)
    val bookAccIds = remember(activeBook) {
        activeBook?.let { CuciMoneyHelpers.parseAccountIds(it.accountIdsJson).toSet() } ?: emptySet()
    }
    val targetAccounts = remember(uiState.accounts, bookAccIds) {
        val list = uiState.accounts.filter { !it.isSavings && it.id in bookAccIds }
        list.ifEmpty { uiState.accounts.filter { !it.isSavings } }
    }
    var selectedAccId by remember { mutableLongStateOf(targetAccounts.firstOrNull()?.id ?: 1L) }
    var realize by remember(realizeDefault) { mutableStateOf(realizeDefault) }
    var markDone by remember(realizeDefault) { mutableStateOf(realizeDefault) }

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
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CuciMoneyColors.Bg)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Cairkan Tabungan", color = CuciMoneyColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(22.dp).clickable { onClose() })
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Saldo tabungan ${item.title}: ${CuciMoneyHelpers.formatRp(item.saved, true)}",
                color = CuciMoneyColors.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (realize) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card)
                        .border(1.dp, if (realize) CuciMoneyColors.Primary else CuciMoneyColors.Border, RoundedCornerShape(10.dp))
                        .clickable { realize = true; markDone = true }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cairkan ke Akun", color = if (realize) CuciMoneyColors.Primary else CuciMoneyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!realize) CuciMoneyColors.ExpenseDim else CuciMoneyColors.Card)
                        .border(1.dp, if (!realize) CuciMoneyColors.Expense else CuciMoneyColors.Border, RoundedCornerShape(10.dp))
                        .clickable { realize = false; markDone = false }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Kembalikan Semua", color = if (!realize) CuciMoneyColors.Expense else CuciMoneyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (realize) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { markDone = !markDone }
                ) {
                    Checkbox(
                        checked = markDone,
                        onCheckedChange = { markDone = it },
                        colors = CheckboxDefaults.colors(checkedColor = CuciMoneyColors.Primary)
                    )
                    Text("Tandai wishlist Selesai", color = CuciMoneyColors.TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("AKUN TUJUAN", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 190.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(targetAccounts, key = { it.id }) { acc ->
                    val sel = acc.id == selectedAccId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card)
                            .border(1.dp, if (sel) CuciMoneyColors.Primary else CuciMoneyColors.Border, RoundedCornerShape(12.dp))
                            .clickable { selectedAccId = acc.id }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(CuciMoneyHelpers.accountIcon(acc), fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(acc.name, color = CuciMoneyColors.TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(CuciMoneyHelpers.formatRp(acc.balance, true), color = CuciMoneyColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (realize) CuciMoneyColors.Primary else CuciMoneyColors.Expense)
                    .clickable { onConfirm(selectedAccId, realize, markDone) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (realize) "Cairkan Sekarang" else "Kembalikan Dana",
                    color = if (realize) Color(0xFF0A1214) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
