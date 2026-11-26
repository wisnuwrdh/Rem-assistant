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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ecosystemku.assistant.data.local.CuciAccountEntity
import com.ecosystemku.assistant.data.local.CuciCategoryEntity
import com.ecosystemku.assistant.data.local.CuciMemberEntity
import com.ecosystemku.assistant.data.local.CuciTransactionEntity
import com.ecosystemku.assistant.ui.components.RemDatePickerDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddEditTransactionModal(
    uiState: CuciMoneyUiState,
    editTrx: CuciTransactionEntity?,
    onClose: () -> Unit,
    onToast: (String, String) -> Unit,
    onSave: suspend (
        editId: Long?,
        type: String,
        amount: Long,
        title: String,
        categoryId: Long,
        accountId: Long,
        toAccountId: Long?,
        date: String,
        memberId: String
    ) -> String?,
    onSaveCategory: suspend (editId: Long?, name: String, emoji: String, type: String) -> Long?,
    onSaveMember: suspend (editId: String?, name: String, emoji: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val activeBook = CuciMoneyHelpers.activeBook(uiState)
    val bookAccIds = remember(activeBook) {
        activeBook?.let { CuciMoneyHelpers.parseAccountIds(it.accountIdsJson).toSet() } ?: emptySet()
    }
    val bookAccounts = remember(uiState.accounts, bookAccIds) {
        val filtered = uiState.accounts.filter { !it.isSavings && it.id in bookAccIds }
        filtered.ifEmpty { uiState.accounts.filter { !it.isSavings } }
    }

    val initialPair = remember(editTrx, uiState.transactions) {
        if (editTrx?.transferPair != null) {
            uiState.transactions.firstOrNull { it.transferPair == editTrx.transferPair && it.id != editTrx.id }
        } else null
    }

    val initialType = when {
        editTrx == null -> "expense"
        editTrx.transferPair != null -> "transfer"
        editTrx.direction == "in" -> "income"
        else -> "expense"
    }

    var trxType by remember(editTrx) { mutableStateOf(initialType) }
    var expr by remember(editTrx) { mutableStateOf(editTrx?.amount?.toString() ?: "") }
    var note by remember(editTrx) {
        val catName = uiState.categories.firstOrNull { it.id == editTrx?.categoryId }?.name
        val prefill = if (editTrx != null && editTrx.transferPair == null && editTrx.title != catName) editTrx.title else ""
        mutableStateOf(prefill)
    }

    val defaultCatId = remember(uiState.categories, initialType) {
        uiState.categories.firstOrNull { it.type == initialType || it.type == "both" }?.id ?: 1L
    }
    var selectedCatId by remember(editTrx) {
        mutableLongStateOf(editTrx?.categoryId ?: defaultCatId)
    }

    val lastAcc = uiState.lastAccByBook[uiState.currentBookId]
    val fallbackAccId = bookAccounts.firstOrNull { it.id == lastAcc }?.id ?: bookAccounts.firstOrNull()?.id ?: 1L
    var fromAccId by remember(editTrx) {
        mutableLongStateOf(
            when {
                editTrx == null -> fallbackAccId
                editTrx.transferPair != null && editTrx.direction == "in" -> initialPair?.accountId ?: fallbackAccId
                else -> editTrx.accountId
            }
        )
    }
    var toAccId by remember(editTrx) {
        val secondAcc = bookAccounts.firstOrNull { it.id != fromAccId }?.id ?: fallbackAccId
        mutableLongStateOf(
            when {
                editTrx?.transferPair != null && editTrx.direction == "out" -> initialPair?.accountId ?: secondAcc
                editTrx?.transferPair != null && editTrx.direction == "in" -> editTrx.accountId
                else -> secondAcc
            }
        )
    }

    var selectedDate by remember(editTrx) { mutableStateOf(editTrx?.date ?: CuciMoneyHelpers.todayDateStr()) }
    var selectedMemberId by remember(editTrx) {
        mutableStateOf(editTrx?.memberId ?: uiState.members.firstOrNull()?.id ?: "m1")
    }

    var showWalletFor by remember { mutableStateOf<String?>(null) } // "from" or "to"
    var showDatePicker by remember { mutableStateOf(false) }
    var showMemberPicker by remember { mutableStateOf(false) }
    var showNewCatModal by remember { mutableStateOf(false) }
    var showNewMemberModal by remember { mutableStateOf(false) }

    val filteredCats = remember(uiState.categories, trxType) {
        if (trxType == "transfer") emptyList()
        else uiState.categories.filter { it.type == trxType || it.type == "both" }.sortedBy { it.sortOrder }
    }
    val selectedCat = uiState.categories.firstOrNull { it.id == selectedCatId }
    val fromAcc = bookAccounts.firstOrNull { it.id == fromAccId } ?: bookAccounts.firstOrNull()
    val toAcc = bookAccounts.firstOrNull { it.id == toAccId } ?: bookAccounts.firstOrNull()
    val member = uiState.members.firstOrNull { it.id == selectedMemberId } ?: uiState.members.firstOrNull()

    val computedAmount = remember(expr) { CuciMoneyHelpers.evalCalcExpression(expr) }
    val modeColor = when (trxType) {
        "income" -> CuciMoneyColors.Income
        "transfer" -> CuciMoneyColors.Transfer
        else -> CuciMoneyColors.Expense
    }

    fun submit(keepOpen: Boolean) {
        val amt = CuciMoneyHelpers.evalCalcExpression(expr)
        if (amt <= 0L) {
            onToast("Masukkan nominal lebih dari 0", "err")
            return
        }
        if (trxType == "transfer" && fromAccId == toAccId) {
            onToast("Akun asal dan tujuan harus berbeda", "err")
            return
        }
        val finalCatId = if (trxType == "transfer") 12L else (filteredCats.firstOrNull { it.id == selectedCatId }?.id ?: filteredCats.firstOrNull()?.id ?: 12L)
        val finalTitle = when {
            note.isNotBlank() -> note.trim()
            trxType == "transfer" -> "Transfer ${fromAcc?.name ?: ""} → ${toAcc?.name ?: ""}"
            else -> uiState.categories.firstOrNull { it.id == finalCatId }?.name ?: "Transaksi"
        }
        scope.launch {
            val err = onSave(
                editTrx?.id,
                trxType,
                amt,
                finalTitle,
                finalCatId,
                fromAccId,
                if (trxType == "transfer") toAccId else null,
                selectedDate,
                selectedMemberId
            )
            if (err != null) {
                onToast(err, "err")
            } else {
                onToast(if (editTrx != null) "Transaksi diperbarui" else "Transaksi disimpan", "ok")
                if (keepOpen && editTrx == null) {
                    expr = ""
                    note = ""
                } else {
                    onClose()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CuciMoneyColors.Bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar with mode tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CuciHeaderIconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(18.dp))
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CuciMoneyColors.Card)
                        .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "expense" to "Pengeluaran",
                        "income" to "Pemasukan",
                        "transfer" to "Transfer"
                    ).forEach { (id, lbl) ->
                        val sel = trxType == id
                        val activeCol = when (id) {
                            "income" -> CuciMoneyColors.Income
                            "transfer" -> CuciMoneyColors.Transfer
                            else -> CuciMoneyColors.Expense
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sel) activeCol.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (sel) activeCol.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable {
                                    trxType = id
                                    if (id != "transfer") {
                                        val first = uiState.categories.firstOrNull { it.type == id || it.type == "both" }
                                        if (first != null && filteredCats.none { it.id == selectedCatId }) {
                                            selectedCatId = first.id
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lbl,
                                color = if (sel) activeCol else CuciMoneyColors.TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Middle scrollable area: Category grid or Transfer From -> To
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                if (trxType == "transfer") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("TRANSFER ANTAR AKUN", color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TransferAccountCard(
                            modifier = Modifier.weight(1f),
                            label = "Dari Akun",
                            acc = fromAcc,
                            onClick = { showWalletFor = "from" }
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = CuciMoneyColors.Transfer,
                            modifier = Modifier.size(22.dp)
                        )
                        TransferAccountCard(
                            modifier = Modifier.weight(1f),
                            label = "Ke Akun",
                            acc = toAcc,
                            onClick = { showWalletFor = "to" }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        maxItemsInEachRow = 4,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCats.forEach { cat ->
                            val isSel = cat.id == selectedCatId
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSel) modeColor.copy(alpha = 0.16f) else CuciMoneyColors.Card)
                                    .border(
                                        1.5.dp,
                                        if (isSel) modeColor else CuciMoneyColors.Border,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedCatId = cat.id }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(cat.emoji, fontSize = 21.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.name,
                                    color = if (isSel) CuciMoneyColors.TextMain else CuciMoneyColors.TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CuciMoneyColors.Card2)
                                .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(14.dp))
                                .clickable { showNewCatModal = true }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Tambah",
                                tint = CuciMoneyColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tambah", color = CuciMoneyColors.Primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Bottom Calculator Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(CuciMoneyColors.Card)
                    .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Note input
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CuciMoneyColors.Card2)
                        .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    BasicTextField(
                        value = note,
                        onValueChange = { note = it },
                        singleLine = true,
                        textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 13.sp),
                        cursorBrush = SolidColor(CuciMoneyColors.Primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (note.isEmpty()) {
                                Text(
                                    text = if (trxType == "transfer") "Catatan transfer (opsional)" else "${selectedCat?.name ?: "Catatan"} (opsional)",
                                    color = CuciMoneyColors.TextLight,
                                    fontSize = 13.sp
                                )
                            }
                            inner()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Wallet pill + Expression/Amount display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CuciMoneyColors.Card2)
                            .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(12.dp))
                            .clickable { showWalletFor = "from" }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(CuciMoneyHelpers.accountIcon(fromAcc), fontSize = 15.sp)
                        Text(fromAcc?.name ?: "Akun", color = CuciMoneyColors.TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (expr.any { it in listOf('+', '−', '×', '÷') }) {
                            Text(expr, color = CuciMoneyColors.TextMuted, fontSize = 12.sp)
                        }
                        Text(
                            text = CuciMoneyHelpers.formatRp(computedAmount, true),
                            color = modeColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action chip row: Member, Date, Save+Next, Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CuciMoneyColors.Card2)
                            .clickable { showMemberPicker = true }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(member?.emoji ?: "😄", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = member?.name ?: "Saya",
                            color = CuciMoneyColors.TextMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CuciMoneyColors.Card2)
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.CalendarToday, null, tint = CuciMoneyColors.Primary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = CuciMoneyHelpers.formatDayMonthShort(selectedDate),
                            color = CuciMoneyColors.TextMain,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    if (editTrx == null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(CuciMoneyColors.PrimaryDim)
                                .border(1.dp, CuciMoneyColors.Primary, RoundedCornerShape(10.dp))
                                .clickable { submit(keepOpen = true) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Add, "Simpan & Lanjut", tint = CuciMoneyColors.Primary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CuciMoneyColors.Primary)
                            .clickable { submit(keepOpen = false) }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Check, "Simpan", tint = Color(0xFF0A1214), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4x4 Calculator Keypad
                val rows = listOf(
                    listOf("7", "8", "9", "÷"),
                    listOf("4", "5", "6", "×"),
                    listOf("1", "2", "3", "−"),
                    listOf("000", "0", "⌫", "+")
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    rows.forEach { r ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            r.forEach { key ->
                                val isOp = key in listOf("÷", "×", "−", "+")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isOp) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card2)
                                        .clickable {
                                            when (key) {
                                                "⌫" -> if (expr.isNotEmpty()) expr = expr.dropLast(1)
                                                "÷", "×", "−", "+" -> {
                                                    if (expr.isNotEmpty() && expr.last() !in listOf('÷', '×', '−', '+')) {
                                                        expr += key
                                                    } else if (expr.isNotEmpty()) {
                                                        expr = expr.dropLast(1) + key
                                                    }
                                                }
                                                "000" -> if (expr.isNotEmpty() && expr.last().isDigit()) expr += "000"
                                                else -> if (expr.length < 24) expr += key
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (key == "⌫") {
                                        Icon(Icons.AutoMirrored.Outlined.Backspace, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(18.dp))
                                    } else {
                                        Text(
                                            text = key,
                                            color = if (isOp) CuciMoneyColors.Primary else CuciMoneyColors.TextMain,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showWalletFor != null) {
            WalletPickerModal(
                accounts = bookAccounts,
                selectedId = if (showWalletFor == "to") toAccId else fromAccId,
                onSelect = { accId ->
                    if (showWalletFor == "to") toAccId = accId else fromAccId = accId
                    showWalletFor = null
                },
                onClose = { showWalletFor = null }
            )
        }

        if (showDatePicker) {
            CuciDatePickerDialog(
                initialDate = selectedDate,
                onSelect = { selectedDate = it; showDatePicker = false },
                onClose = { showDatePicker = false }
            )
        }

        if (showMemberPicker) {
            MemberModal(
                members = uiState.members,
                selectedMemberId = selectedMemberId,
                managerMode = false,
                onSelect = { selectedMemberId = it; showMemberPicker = false },
                onAddClick = { showMemberPicker = false; showNewMemberModal = true },
                onEditClick = {},
                onMove = { _, _ -> },
                onDelete = {},
                onClose = { showMemberPicker = false }
            )
        }

        if (showNewCatModal) {
            val catType = if (trxType == "income") "income" else "expense"
            CategoryCreateEditModal(
                existing = null,
                defaultType = catType,
                onSave = { name, emoji ->
                    scope.launch {
                        val newId = onSaveCategory(null, name, emoji, catType)
                        if (newId != null) selectedCatId = newId
                        showNewCatModal = false
                    }
                },
                onDelete = null,
                onClose = { showNewCatModal = false }
            )
        }

        if (showNewMemberModal) {
            MemberCreateEditModal(
                existing = null,
                onSave = { name, emoji ->
                    scope.launch {
                        onSaveMember(null, name, emoji)
                        showNewMemberModal = false
                    }
                },
                onDelete = null,
                onClose = { showNewMemberModal = false }
            )
        }
    }
}

@Composable
private fun TransferAccountCard(
    modifier: Modifier,
    label: String,
    acc: CuciAccountEntity?,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CuciMoneyColors.Card)
            .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(label, color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(CuciMoneyHelpers.accountIcon(acc), fontSize = 20.sp)
            Text(
                text = acc?.name ?: "Pilih Akun",
                color = CuciMoneyColors.TextMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = CuciMoneyHelpers.formatRp(acc?.balance ?: 0L, true),
            color = CuciMoneyColors.Primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun CuciDatePickerDialog(
    initialDate: String,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val initDate = remember(initialDate) {
        runCatching { LocalDate.parse(initialDate) }.getOrNull() ?: LocalDate.now()
    }
    RemDatePickerDialog(
        initialDate = initDate,
        title = "Pilih Tanggal",
        onDismiss = onClose,
        onConfirm = { picked -> onSelect(picked.toString()) }
    )
}

@Composable
internal fun WalletPickerModal(
    accounts: List<CuciAccountEntity>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                .background(CuciMoneyColors.Card)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PILIH AKUN", color = CuciMoneyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(20.dp).clickable { onClose() })
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts, key = { it.id }) { acc ->
                    val sel = acc.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card2)
                            .border(1.dp, if (sel) CuciMoneyColors.Primary else CuciMoneyColors.Border, RoundedCornerShape(14.dp))
                            .clickable { onSelect(acc.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(CuciMoneyHelpers.accountIcon(acc), fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(acc.name, color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(CuciMoneyHelpers.accountTypeLabel(acc.type), color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                        }
                        Text(CuciMoneyHelpers.formatRp(acc.balance, true), color = CuciMoneyColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun CategoryModal(
    categories: List<CuciCategoryEntity>,
    transactions: List<CuciTransactionEntity>,
    managerMode: Boolean,
    onSelect: (CuciCategoryEntity) -> Unit,
    onAddClick: (type: String) -> Unit,
    onEditClick: (CuciCategoryEntity) -> Unit,
    onMove: (id: Long, delta: Int, filterType: String) -> Unit,
    onDelete: (CuciCategoryEntity) -> Unit,
    onClose: () -> Unit
) {
    var filterType by remember { mutableStateOf("expense") }
    val filtered = remember(categories, filterType) {
        categories.filter { it.type == filterType || it.type == "both" }.sortedBy { it.sortOrder }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CuciMoneyColors.Bg)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (managerMode) "Kelola Kategori" else "Pilih Kategori",
                    color = CuciMoneyColors.TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CuciMoneyColors.Primary)
                            .clickable { onAddClick(filterType) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ Tambah", color = Color(0xFF0A1214), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(22.dp).clickable { onClose() })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CuciMoneyColors.Card)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("expense" to "Pengeluaran", "income" to "Pemasukan").forEach { (id, lbl) ->
                    val sel = filterType == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (sel) CuciMoneyColors.Primary else Color.Transparent)
                            .clickable { filterType = id }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(lbl, color = if (sel) Color(0xFF0A1214) else CuciMoneyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { cat ->
                    val usedCount = transactions.count { it.categoryId == cat.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CuciMoneyColors.Card)
                            .border(1.dp, CuciMoneyColors.Border, RoundedCornerShape(14.dp))
                            .clickable { if (managerMode) onEditClick(cat) else onSelect(cat) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name, color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (usedCount > 0) {
                                Text("$usedCount transaksi", color = CuciMoneyColors.TextMuted, fontSize = 11.sp)
                            }
                        }
                        if (managerMode) {
                            Icon(
                                Icons.Outlined.ArrowUpward,
                                null,
                                tint = CuciMoneyColors.TextMuted,
                                modifier = Modifier.size(20.dp).clickable { onMove(cat.id, -1, filterType) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Outlined.ArrowDownward,
                                null,
                                tint = CuciMoneyColors.TextMuted,
                                modifier = Modifier.size(20.dp).clickable { onMove(cat.id, 1, filterType) }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                null,
                                tint = CuciMoneyColors.Expense,
                                modifier = Modifier.size(20.dp).clickable { onDelete(cat) }
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
internal fun CategoryCreateEditModal(
    existing: CuciCategoryEntity?,
    defaultType: String,
    onSave: (name: String, emoji: String) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit
) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var selectedEmoji by remember(existing) { mutableStateOf(existing?.emoji ?: "📌") }

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
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CuciMoneyColors.Bg)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) "Kategori Baru" else "Edit Kategori",
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
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CuciMoneyColors.PrimaryDim)
                        .border(1.dp, CuciMoneyColors.Primary, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(selectedEmoji, fontSize = 24.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CuciMoneyColors.Card)
                        .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(CuciMoneyColors.Primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (name.isEmpty()) Text("Nama kategori...", color = CuciMoneyColors.TextLight, fontSize = 14.sp)
                            inner()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CuciMoneyHelpers.CAT_EMOJI_GROUPS.forEach { grp ->
                    item {
                        Text(grp.label, color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            grp.emojis.forEach { e ->
                                val sel = selectedEmoji == e
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card)
                                        .border(1.5.dp, if (sel) CuciMoneyColors.Primary else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable { selectedEmoji = e },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(e, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CuciMoneyColors.Primary)
                    .clickable { if (name.isNotBlank()) onSave(name.trim(), selectedEmoji) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Simpan Kategori", color = Color(0xFF0A1214), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun MemberModal(
    members: List<CuciMemberEntity>,
    selectedMemberId: String,
    managerMode: Boolean,
    onSelect: (String) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (CuciMemberEntity) -> Unit,
    onMove: (id: String, delta: Int) -> Unit,
    onDelete: (CuciMemberEntity) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CuciMoneyColors.Bg)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (managerMode) "Kelola Anggota" else "Pilih Anggota",
                    color = CuciMoneyColors.TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(CuciMoneyColors.Primary)
                            .clickable { onAddClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ Tambah", color = Color(0xFF0A1214), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(22.dp).clickable { onClose() })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(members.sortedBy { it.sortOrder }, key = { it.id }) { m ->
                    val sel = !managerMode && m.id == selectedMemberId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (sel) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card)
                            .border(1.dp, if (sel) CuciMoneyColors.Primary else CuciMoneyColors.Border, RoundedCornerShape(14.dp))
                            .clickable { if (managerMode) onEditClick(m) else onSelect(m.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(m.emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(m.name, color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (managerMode) {
                            Icon(Icons.Outlined.ArrowUpward, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(20.dp).clickable { onMove(m.id, -1) })
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Outlined.ArrowDownward, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(20.dp).clickable { onMove(m.id, 1) })
                            if (m.id != "m1") {
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(Icons.Outlined.DeleteOutline, null, tint = CuciMoneyColors.Expense, modifier = Modifier.size(20.dp).clickable { onDelete(m) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MemberCreateEditModal(
    existing: CuciMemberEntity?,
    onSave: (name: String, emoji: String) -> Unit,
    onDelete: (() -> Unit)?,
    onClose: () -> Unit
) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var selectedEmoji by remember(existing) { mutableStateOf(existing?.emoji ?: "😄") }

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
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CuciMoneyColors.Bg)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) "Anggota Baru" else "Edit Anggota",
                    color = CuciMoneyColors.TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (existing != null && existing.id != "m1" && onDelete != null) {
                        Icon(Icons.Outlined.DeleteOutline, null, tint = CuciMoneyColors.Expense, modifier = Modifier.size(20.dp).clickable { onDelete() })
                    }
                    Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(22.dp).clickable { onClose() })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CuciMoneyColors.PrimaryDim)
                        .border(1.dp, CuciMoneyColors.Primary, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(selectedEmoji, fontSize = 24.sp)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CuciMoneyColors.Card)
                        .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        cursorBrush = SolidColor(CuciMoneyColors.Primary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (name.isEmpty()) Text("Nama anggota...", color = CuciMoneyColors.TextLight, fontSize = 14.sp)
                            inner()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CuciMoneyHelpers.MEMBER_EMOJI_GROUPS.forEach { grp ->
                    item {
                        Text(grp.label, color = CuciMoneyColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            grp.emojis.forEach { e ->
                                val sel = selectedEmoji == e
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card)
                                        .border(1.5.dp, if (sel) CuciMoneyColors.Primary else Color.Transparent, RoundedCornerShape(10.dp))
                                        .clickable { selectedEmoji = e },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(e, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CuciMoneyColors.Primary)
                    .clickable { if (name.isNotBlank()) onSave(name.trim(), selectedEmoji) }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Simpan Anggota", color = Color(0xFF0A1214), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
