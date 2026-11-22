package com.ecosystemku.assistant.ui.apps.cucimoney

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.ecosystemku.assistant.data.local.CuciWishlistEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class CuciConfirmState(
    val title: String,
    val message: String,
    val confirmLabel: String = "Ya, Lanjutkan",
    val isDanger: Boolean = false,
    val onConfirm: () -> Unit
)

@Composable
fun CuciMoneyScreen(
    uiState: CuciMoneyUiState,
    actions: CuciMoneyActions,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (nowYear, nowMonth) = CuciMoneyHelpers.currentYearMonth()

    var activeTab by remember { mutableStateOf(CuciTab.BUKU) }
    var viewMonth by remember { mutableIntStateOf(nowMonth) }
    var viewYear by remember { mutableIntStateOf(nowYear) }

    var analysisPeriod by remember { mutableStateOf(CuciAnalysisPeriod.MONTH) }
    var analysisTab by remember { mutableStateOf(CuciAnalysisTab.EXPENSE) }
    var anaMonth by remember { mutableIntStateOf(nowMonth) }
    var anaYear by remember { mutableIntStateOf(nowYear) }

    // Modal & Sheet states
    var showBookSheet by remember { mutableStateOf(false) }
    var showCreateBookPrompt by remember { mutableStateOf(false) }
    var showSearchModal by remember { mutableStateOf(false) }
    var showAllTrxSheet by remember { mutableStateOf(false) }

    var showAddEditTrx by remember { mutableStateOf(false) }
    var editingTrx by remember { mutableStateOf<CuciTransactionEntity?>(null) }
    var detailTrx by remember { mutableStateOf<CuciTransactionEntity?>(null) }

    var detailAccount by remember { mutableStateOf<CuciAccountEntity?>(null) }
    var showAddEditAccount by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<CuciAccountEntity?>(null) }

    var catDetailPair by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var budgetDetailMonthYear by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var setupBudgetMonthYear by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var showCategoryManager by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CuciCategoryEntity?>(null) }
    var creatingCatType by remember { mutableStateOf<String?>(null) }

    var showMemberManager by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<CuciMemberEntity?>(null) }
    var creatingMember by remember { mutableStateOf(false) }

    var detailWishlistId by remember { mutableStateOf<String?>(null) }
    var editingWishlist by remember { mutableStateOf<CuciWishlistEntity?>(null) }
    var creatingWishlist by remember { mutableStateOf(false) }
    var depositWishlistId by remember { mutableStateOf<String?>(null) }
    var withdrawWishlistState by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    var confirmState by remember { mutableStateOf<CuciConfirmState?>(null) }
    var toastState by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun showToast(msg: String, type: String = "ok") {
        toastState = msg to type
    }

    LaunchedEffect(toastState) {
        if (toastState != null) {
            delay(2600)
            toastState = null
        }
    }

    // Pending export payloads for SAF writers
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    var pendingJsonContent by remember { mutableStateOf<String?>(null) }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val content = pendingCsvContent
        pendingCsvContent = null
        if (uri != null && content != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            }.onSuccess {
                showToast("CSV berhasil diekspor", "ok")
            }.onFailure {
                showToast("Gagal menulis CSV: ${it.message}", "err")
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = pendingJsonContent
        pendingJsonContent = null
        if (uri != null && content != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            }.onSuccess {
                showToast("Backup JSON tersimpan", "ok")
            }.onFailure {
                showToast("Gagal menyimpan backup: ${it.message}", "err")
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }.getOrNull()
                if (raw.isNullOrBlank()) {
                    showToast("File backup kosong atau tidak valid", "err")
                } else {
                    confirmState = CuciConfirmState(
                        title = "Restore Data?",
                        message = "Semua data CuciMoney+ saat ini akan digantikan oleh file backup ini.",
                        confirmLabel = "Restore",
                        isDanger = true
                    ) {
                        scope.launch {
                            actions.onImportBackupJson(raw)
                                .onSuccess { count -> showToast("Restore berhasil ($count transaksi)", "ok") }
                                .onFailure { showToast("Format backup tidak valid", "err") }
                        }
                    }
                }
            }
        }
    }

    // Handle back navigation through nested modals cleanly
    val hasOverlay = confirmState != null || showCreateBookPrompt || showAddEditTrx ||
        showAddEditAccount || detailAccount != null || catDetailPair != null ||
        setupBudgetMonthYear != null || budgetDetailMonthYear != null ||
        creatingCatType != null || editingCategory != null || showCategoryManager ||
        creatingMember || editingMember != null || showMemberManager ||
        depositWishlistId != null || withdrawWishlistState != null ||
        creatingWishlist || editingWishlist != null || detailWishlistId != null ||
        detailTrx != null || showSearchModal || showAllTrxSheet || showBookSheet ||
        activeTab == CuciTab.TABUNGAN

    BackHandler(enabled = hasOverlay) {
        when {
            confirmState != null -> confirmState = null
            showCreateBookPrompt -> showCreateBookPrompt = false
            showAddEditTrx -> { showAddEditTrx = false; editingTrx = null }
            showAddEditAccount -> { showAddEditAccount = false; editingAccount = null }
            detailAccount != null -> detailAccount = null
            catDetailPair != null -> catDetailPair = null
            setupBudgetMonthYear != null -> setupBudgetMonthYear = null
            budgetDetailMonthYear != null -> budgetDetailMonthYear = null
            creatingCatType != null -> creatingCatType = null
            editingCategory != null -> editingCategory = null
            showCategoryManager -> showCategoryManager = false
            creatingMember -> creatingMember = false
            editingMember != null -> editingMember = null
            showMemberManager -> showMemberManager = false
            depositWishlistId != null -> depositWishlistId = null
            withdrawWishlistState != null -> withdrawWishlistState = null
            creatingWishlist -> creatingWishlist = false
            editingWishlist != null -> editingWishlist = null
            detailWishlistId != null -> detailWishlistId = null
            detailTrx != null -> detailTrx = null
            showSearchModal -> showSearchModal = false
            showAllTrxSheet -> showAllTrxSheet = false
            showBookSheet -> showBookSheet = false
            activeTab == CuciTab.TABUNGAN -> activeTab = CuciTab.LAINNYA
        }
    }

    // Resolve fresh entity references when uiState updates
    val resolvedDetailAccount = detailAccount?.let { cur -> uiState.accounts.firstOrNull { it.id == cur.id } ?: cur }
    val resolvedDetailWishlist = detailWishlistId?.let { id -> uiState.wishlists.firstOrNull { it.id == id } }
    val resolvedDepositWishlist = depositWishlistId?.let { id -> uiState.wishlists.firstOrNull { it.id == id } }
    val resolvedWithdrawWishlist = withdrawWishlistState?.let { (id, realize) ->
        uiState.wishlists.firstOrNull { it.id == id }?.let { it to realize }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CuciMoneyColors.Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Return Bar (Subtle bar matching EcosystemKu app hub return)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(16.dp))
                    Text("Apps", color = CuciMoneyColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("CuciMoney+", color = CuciMoneyColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.width(56.dp))
            }

            // Active Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    CuciTab.BUKU -> BukuTabContent(
                        uiState = uiState,
                        viewMonth = viewMonth,
                        viewYear = viewYear,
                        onChangeMonth = { delta ->
                            var m = viewMonth + delta
                            var y = viewYear
                            if (m < 0) { m = 11; y-- }
                            if (m > 11) { m = 0; y++ }
                            if (y < nowYear || (y == nowYear && m <= nowMonth)) {
                                viewMonth = m
                                viewYear = y
                            }
                        },
                        onOpenSearch = { showSearchModal = true },
                        onOpenBookSheet = { showBookSheet = true },
                        onToggleVisibility = actions.onToggleVisibility,
                        onToggleBudgetCollapsed = actions.onToggleBudgetCollapsed,
                        onOpenBudgetDetail = { m, y -> budgetDetailMonthYear = m to y },
                        onOpenTrxDetail = { detailTrx = it }
                    )

                    CuciTab.DOMPET -> DompetTabContent(
                        uiState = uiState,
                        onToggleVisibility = actions.onToggleVisibility,
                        onOpenAccountDetail = { detailAccount = it },
                        onOpenAddAccount = {
                            editingAccount = null
                            showAddEditAccount = true
                        },
                        onOpenTabungan = { activeTab = CuciTab.TABUNGAN }
                    )

                    CuciTab.ANALISIS -> AnalisisTabContent(
                        uiState = uiState,
                        period = analysisPeriod,
                        activeTab = analysisTab,
                        viewMonth = anaMonth,
                        viewYear = anaYear,
                        onSelectPeriod = { analysisPeriod = it },
                        onSelectTab = { analysisTab = it },
                        onChangePeriodOffset = { delta ->
                            when (analysisPeriod) {
                                CuciAnalysisPeriod.MONTH -> {
                                    var m = anaMonth + delta
                                    var y = anaYear
                                    if (m < 0) { m = 11; y-- }
                                    if (m > 11) { m = 0; y++ }
                                    if (y < nowYear || (y == nowYear && m <= nowMonth)) {
                                        anaMonth = m
                                        anaYear = y
                                    }
                                }
                                CuciAnalysisPeriod.YEAR -> {
                                    val y = anaYear + delta
                                    if (y <= nowYear) anaYear = y
                                }
                                CuciAnalysisPeriod.ALL -> Unit
                            }
                        },
                        onToggleVisibility = actions.onToggleVisibility,
                        onOpenCatDetail = { cid, dir -> catDetailPair = cid to dir },
                        onOpenBudgetDetail = { m, y -> budgetDetailMonthYear = m to y },
                        onOpenSetupBudget = { m, y -> setupBudgetMonthYear = m to y }
                    )

                    CuciTab.LAINNYA -> LainnyaTabContent(
                        uiState = uiState,
                        onOpenBooks = { showBookSheet = true },
                        onOpenAccounts = { activeTab = CuciTab.DOMPET },
                        onOpenAllTransactions = { showAllTrxSheet = true },
                        onOpenSearch = { showSearchModal = true },
                        onOpenBudget = {
                            analysisTab = CuciAnalysisTab.BUDGET
                            activeTab = CuciTab.ANALISIS
                        },
                        onOpenTabungan = { activeTab = CuciTab.TABUNGAN },
                        onOpenCategories = { showCategoryManager = true },
                        onOpenMembers = { showMemberManager = true },
                        onExportCsv = {
                            scope.launch {
                                val csv = actions.onExportCsv()
                                pendingCsvContent = csv
                                csvLauncher.launch("cucimoney_transaksi_${CuciMoneyHelpers.todayDateStr()}.csv")
                            }
                        },
                        onBackupJson = {
                            scope.launch {
                                val json = actions.onExportBackupJson()
                                pendingJsonContent = json
                                backupLauncher.launch("cucimoney_backup_${CuciMoneyHelpers.todayDateStr()}.json")
                            }
                        },
                        onRestoreJson = {
                            restoreLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        onResetData = {
                            confirmState = CuciConfirmState(
                                title = "Reset Semua Data?",
                                message = "Semua buku, akun, transaksi, budget, dan tabungan akan dikembalikan ke data awal.",
                                confirmLabel = "Reset Data",
                                isDanger = true
                            ) {
                                scope.launch {
                                    actions.onResetAllData()
                                    showToast("Data berhasil direset", "ok")
                                }
                            }
                        }
                    )

                    CuciTab.TABUNGAN -> TabunganViewContent(
                        uiState = uiState,
                        onBack = { activeTab = CuciTab.LAINNYA },
                        onToggleVisibility = actions.onToggleVisibility,
                        onOpenNewWishlist = {
                            editingWishlist = null
                            creatingWishlist = true
                        },
                        onOpenDetail = { detailWishlistId = it.id },
                        onOpenDeposit = { depositWishlistId = it.id },
                        onOpenWithdraw = { w, realize -> withdrawWishlistState = w.id to realize },
                        onOpenEditWishlist = {
                            editingWishlist = it
                            creatingWishlist = true
                        }
                    )
                }
            }

            // Bottom Navigation Bar (hidden on TABUNGAN sub-view)
            if (activeTab != CuciTab.TABUNGAN) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CuciMoneyColors.Card)
                        .border(1.dp, CuciMoneyColors.Border)
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomNavItem(Icons.Outlined.MenuBook, "Buku", activeTab == CuciTab.BUKU) { activeTab = CuciTab.BUKU }
                    BottomNavItem(Icons.Outlined.AccountBalanceWallet, "Dompet", activeTab == CuciTab.DOMPET) { activeTab = CuciTab.DOMPET }
                    BottomNavItem(Icons.Outlined.BarChart, "Analisis", activeTab == CuciTab.ANALISIS) { activeTab = CuciTab.ANALISIS }
                    BottomNavItem(Icons.Outlined.GridView, "Lainnya", activeTab == CuciTab.LAINNYA) { activeTab = CuciTab.LAINNYA }
                }
            }
        }

        // FAB on Buku Tab
        if (activeTab == CuciTab.BUKU) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 78.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(CuciMoneyColors.Primary, Color(0xFF008E76))))
                    .clickable {
                        editingTrx = null
                        showAddEditTrx = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, "Tambah Transaksi", tint = Color(0xFF0A1214), modifier = Modifier.size(26.dp))
            }
        }

        // Overlays stack
        if (showBookSheet) {
            BookSelectorSheet(
                uiState = uiState,
                onClose = { showBookSheet = false },
                onSelect = { id ->
                    actions.onSelectBook(id)
                    showBookSheet = false
                },
                onAddBook = { showCreateBookPrompt = true },
                onDeleteBook = { book ->
                    confirmState = CuciConfirmState(
                        title = "Hapus Buku \"${book.name}\"?",
                        message = "Semua transaksi dan budget di buku ini akan dihapus dan saldo akun disesuaikan.",
                        confirmLabel = "Hapus Buku",
                        isDanger = true
                    ) {
                        scope.launch {
                            val err = actions.onDeleteBook(book.id)
                            if (err != null) showToast(err, "err")
                            else showToast("Buku dihapus", "ok")
                        }
                    }
                }
            )
        }

        if (showCreateBookPrompt) {
            CuciPromptDialog(
                title = "Buku Baru",
                placeholder = "Nama buku (misal: Tabungan)",
                onClose = { showCreateBookPrompt = false },
                onConfirm = { name ->
                    scope.launch {
                        val ok = actions.onCreateBook(name)
                        if (ok) {
                            showToast("Buku \"$name\" dibuat", "ok")
                            showCreateBookPrompt = false
                        } else {
                            showToast("Nama buku tidak boleh kosong", "err")
                        }
                    }
                }
            )
        }

        if (showSearchModal) {
            SearchTransactionsModal(
                uiState = uiState,
                onClose = { showSearchModal = false },
                onOpenTrxDetail = { detailTrx = it }
            )
        }

        if (showAllTrxSheet) {
            AllTransactionsSheet(
                uiState = uiState,
                onClose = { showAllTrxSheet = false },
                onOpenTrxDetail = { detailTrx = it }
            )
        }

        if (resolvedDetailAccount != null) {
            AccountDetailModal(
                uiState = uiState,
                account = resolvedDetailAccount,
                initialMonth = viewMonth,
                initialYear = viewYear,
                onClose = { detailAccount = null },
                onEdit = {
                    editingAccount = resolvedDetailAccount
                    showAddEditAccount = true
                },
                onOpenTrxDetail = { detailTrx = it }
            )
        }

        if (showAddEditAccount) {
            val activeBook = CuciMoneyHelpers.activeBook(uiState)
            val bookAccIds = activeBook?.let { CuciMoneyHelpers.parseAccountIds(it.accountIdsJson).toSet() } ?: emptySet()
            AddEditAccountModal(
                editingAccount = editingAccount,
                currentBookAccountIds = bookAccIds,
                onClose = { showAddEditAccount = false; editingAccount = null },
                onSave = { name, type, icon, bal, notes, useInBook, includeAsset ->
                    scope.launch {
                        val err = actions.onSaveAccount(editingAccount?.id, name, type, icon, bal, notes, useInBook, includeAsset)
                        if (err != null) showToast(err, "err")
                        else {
                            showToast(if (editingAccount != null) "Akun diperbarui" else "Akun ditambahkan", "ok")
                            showAddEditAccount = false
                            editingAccount = null
                        }
                    }
                },
                onDelete = editingAccount?.let { acc ->
                    {
                        confirmState = CuciConfirmState(
                            title = "Hapus Akun \"${acc.name}\"?",
                            message = "Akun akan dihapus permanen. Transaksi lama tetap ada.",
                            confirmLabel = "Hapus",
                            isDanger = true
                        ) {
                            scope.launch {
                                actions.onDeleteAccount(acc.id)
                                showAddEditAccount = false
                                editingAccount = null
                                detailAccount = null
                                showToast("Akun dihapus", "ok")
                            }
                        }
                    }
                }
            )
        }

        catDetailPair?.let { (cid, dir) ->
            CategoryTransactionsModal(
                uiState = uiState,
                categoryId = cid,
                direction = dir,
                period = analysisPeriod,
                viewMonth = anaMonth,
                viewYear = anaYear,
                onClose = { catDetailPair = null },
                onOpenTrxDetail = { detailTrx = it }
            )
        }

        budgetDetailMonthYear?.let { (bm, by) ->
            BudgetDetailSheet(
                uiState = uiState,
                month = bm,
                year = by,
                onClose = { budgetDetailMonthYear = null },
                onEdit = {
                    budgetDetailMonthYear = null
                    setupBudgetMonthYear = bm to by
                },
                onDelete = {
                    confirmState = CuciConfirmState(
                        title = "Hapus Budget?",
                        message = "Master budget bulan ${CuciMoneyHelpers.MONTH_NAMES[bm]} $by akan dihapus.",
                        confirmLabel = "Hapus",
                        isDanger = true
                    ) {
                        scope.launch {
                            actions.onDeleteMasterBudget(bm, by)
                            budgetDetailMonthYear = null
                            showToast("Budget dihapus", "ok")
                        }
                    }
                }
            )
        }

        setupBudgetMonthYear?.let { (bm, by) ->
            val bookId = CuciMoneyHelpers.activeBook(uiState)?.id ?: uiState.currentBookId
            val existing = uiState.masterBudgets.firstOrNull { it.bookId == bookId && it.month == bm && it.year == by }
            SetupMasterBudgetModal(
                existing = existing,
                month = bm,
                year = by,
                onClose = { setupBudgetMonthYear = null },
                onSave = { limit, start, end, note ->
                    scope.launch {
                        val err = actions.onSaveMasterBudget(bm, by, limit, start, end, note)
                        if (err != null) showToast(err, "err")
                        else {
                            showToast("Master budget disimpan", "ok")
                            setupBudgetMonthYear = null
                        }
                    }
                }
            )
        }

        if (showCategoryManager) {
            CategoryModal(
                categories = uiState.categories,
                transactions = uiState.transactions,
                managerMode = true,
                onSelect = {},
                onAddClick = { t ->
                    editingCategory = null
                    creatingCatType = t
                },
                onEditClick = { cat ->
                    editingCategory = cat
                    creatingCatType = cat.type
                },
                onMove = { id, delta, filterType ->
                    scope.launch { actions.onMoveCategory(id, delta, filterType) }
                },
                onDelete = { cat ->
                    val used = uiState.transactions.count { it.categoryId == cat.id }
                    confirmState = CuciConfirmState(
                        title = "Hapus Kategori \"${cat.name}\"?",
                        message = if (used > 0) "Ada $used transaksi menggunakan kategori ini." else "Kategori akan dihapus.",
                        confirmLabel = "Hapus",
                        isDanger = true
                    ) {
                        scope.launch {
                            actions.onDeleteCategory(cat.id)
                            showToast("Kategori dihapus", "ok")
                        }
                    }
                },
                onClose = { showCategoryManager = false }
            )
        }

        creatingCatType?.let { cType ->
            CategoryCreateEditModal(
                existing = editingCategory,
                defaultType = cType,
                onSave = { name, emoji ->
                    scope.launch {
                        actions.onSaveCategory(editingCategory?.id, name, emoji, cType)
                        creatingCatType = null
                        editingCategory = null
                        showToast("Kategori disimpan", "ok")
                    }
                },
                onDelete = editingCategory?.let { cat ->
                    {
                        confirmState = CuciConfirmState(
                            title = "Hapus Kategori \"${cat.name}\"?",
                            message = "Kategori akan dihapus dari daftar.",
                            confirmLabel = "Hapus",
                            isDanger = true
                        ) {
                            scope.launch {
                                actions.onDeleteCategory(cat.id)
                                creatingCatType = null
                                editingCategory = null
                                showToast("Kategori dihapus", "ok")
                            }
                        }
                    }
                },
                onClose = { creatingCatType = null; editingCategory = null }
            )
        }

        if (showMemberManager) {
            MemberModal(
                members = uiState.members,
                selectedMemberId = "",
                managerMode = true,
                onSelect = {},
                onAddClick = { editingMember = null; creatingMember = true },
                onEditClick = { m -> editingMember = m; creatingMember = true },
                onMove = { id, delta -> scope.launch { actions.onMoveMember(id, delta) } },
                onDelete = { m ->
                    confirmState = CuciConfirmState(
                        title = "Hapus Anggota \"${m.name}\"?",
                        message = "Anggota akan dihapus dari daftar.",
                        confirmLabel = "Hapus",
                        isDanger = true
                    ) {
                        scope.launch {
                            actions.onDeleteMember(m.id)
                            showToast("Anggota dihapus", "ok")
                        }
                    }
                },
                onClose = { showMemberManager = false }
            )
        }

        if (creatingMember) {
            MemberCreateEditModal(
                existing = editingMember,
                onSave = { name, emoji ->
                    scope.launch {
                        actions.onSaveMember(editingMember?.id, name, emoji)
                        creatingMember = false
                        editingMember = null
                        showToast("Anggota disimpan", "ok")
                    }
                },
                onDelete = editingMember?.takeIf { it.id != "m1" }?.let { m ->
                    {
                        scope.launch {
                            actions.onDeleteMember(m.id)
                            creatingMember = false
                            editingMember = null
                            showToast("Anggota dihapus", "ok")
                        }
                    }
                },
                onClose = { creatingMember = false; editingMember = null }
            )
        }

        if (resolvedDetailWishlist != null) {
            WishlistDetailSheet(
                item = resolvedDetailWishlist,
                visible = uiState.visibility,
                onClose = { detailWishlistId = null },
                onEdit = {
                    editingWishlist = resolvedDetailWishlist
                    creatingWishlist = true
                },
                onDeposit = { depositWishlistId = resolvedDetailWishlist.id },
                onWithdraw = { realize -> withdrawWishlistState = resolvedDetailWishlist.id to realize }
            )
        }

        if (creatingWishlist) {
            WishlistFormSheet(
                existing = editingWishlist,
                onClose = { creatingWishlist = false; editingWishlist = null },
                onSave = { title, price, priority, image ->
                    scope.launch {
                        actions.onSaveWishlist(editingWishlist?.id, title, price, priority, image)
                        creatingWishlist = false
                        editingWishlist = null
                        showToast("Wishlist disimpan", "ok")
                    }
                },
                onDelete = editingWishlist?.let { w ->
                    {
                        scope.launch {
                            val err = actions.onDeleteWishlist(w.id)
                            if (err != null) {
                                showToast(err, "err")
                            } else {
                                creatingWishlist = false
                                editingWishlist = null
                                detailWishlistId = null
                                showToast("Wishlist dihapus", "ok")
                            }
                        }
                    }
                }
            )
        }

        if (resolvedDepositWishlist != null) {
            WishlistDepositSheet(
                uiState = uiState,
                item = resolvedDepositWishlist,
                onClose = { depositWishlistId = null },
                onDeposit = { accId, amt ->
                    scope.launch {
                        val err = actions.onDepositWishlist(resolvedDepositWishlist.id, accId, amt)
                        if (err != null) showToast(err, "err")
                        else {
                            showToast("Setoran tabungan berhasil", "ok")
                            depositWishlistId = null
                        }
                    }
                }
            )
        }

        if (resolvedWithdrawWishlist != null) {
            val (wItem, defRealize) = resolvedWithdrawWishlist
            WishlistWithdrawSheet(
                uiState = uiState,
                item = wItem,
                realizeDefault = defRealize,
                onClose = { withdrawWishlistState = null },
                onConfirm = { accId, realize, markDone ->
                    scope.launch {
                        val err = actions.onWithdrawWishlist(wItem.id, accId, realize, markDone)
                        if (err != null) showToast(err, "err")
                        else {
                            showToast(if (realize) "Tabungan dicairkan!" else "Dana dikembalikan ke akun", "ok")
                            withdrawWishlistState = null
                        }
                    }
                }
            )
        }

        detailTrx?.let { trx ->
            TransactionDetailDialog(
                uiState = uiState,
                trx = trx,
                onClose = { detailTrx = null },
                onEdit = {
                    if (trx.savingsPot != null) {
                        showToast("Transaksi tabungan wishlist hanya bisa dikelola dari menu Tabungan", "err")
                    } else {
                        detailTrx = null
                        editingTrx = trx
                        showAddEditTrx = true
                    }
                },
                onDelete = {
                    if (trx.savingsPot != null) {
                        showToast("Transaksi tabungan wishlist tidak dapat dihapus langsung", "err")
                    } else {
                        confirmState = CuciConfirmState(
                            title = "Hapus Transaksi?",
                            message = "Saldo akun akan disesuaikan otomatis.",
                            confirmLabel = "Hapus",
                            isDanger = true
                        ) {
                            scope.launch {
                                val err = actions.onDeleteTransaction(trx.id)
                                if (err != null) showToast(err, "err")
                                else {
                                    detailTrx = null
                                    showToast("Transaksi dihapus", "ok")
                                }
                            }
                        }
                    }
                }
            )
        }

        if (showAddEditTrx) {
            AddEditTransactionModal(
                uiState = uiState,
                editTrx = editingTrx,
                onClose = { showAddEditTrx = false; editingTrx = null },
                onToast = ::showToast,
                onSave = actions.onSaveTransaction,
                onSaveCategory = actions.onSaveCategory,
                onSaveMember = actions.onSaveMember
            )
        }

        confirmState?.let { c ->
            CuciConfirmDialog(
                state = c,
                onDismiss = { confirmState = null }
            )
        }

        // Toast Banner
        AnimatedVisibility(
            visible = toastState != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 56.dp, start = 24.dp, end = 24.dp)
        ) {
            toastState?.let { (msg, type) ->
                val bg = if (type == "err") CuciMoneyColors.Expense else CuciMoneyColors.Primary
                val fg = if (type == "err") Color.White else Color(0xFF0A1214)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(msg, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) CuciMoneyColors.Primary else CuciMoneyColors.TextMuted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (selected) CuciMoneyColors.Primary else CuciMoneyColors.TextMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun BookSelectorSheet(
    uiState: CuciMoneyUiState,
    onClose: () -> Unit,
    onSelect: (Long) -> Unit,
    onAddBook: () -> Unit,
    onDeleteBook: (com.ecosystemku.assistant.data.local.CuciBookEntity) -> Unit
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
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(CuciMoneyColors.Bg)
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Pilih Buku Keuangan", color = CuciMoneyColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(22.dp).clickable { onClose() })
            }
            Spacer(modifier = Modifier.height(14.dp))
            LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.books, key = { it.id }) { book ->
                    val sel = book.id == uiState.currentBookId
                    val accIds = CuciMoneyHelpers.parseAccountIds(book.accountIdsJson).toSet()
                    val bal = uiState.accounts.filter { it.id in accIds }.sumOf { it.balance }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) CuciMoneyColors.PrimaryDim else CuciMoneyColors.Card)
                            .border(1.dp, if (sel) CuciMoneyColors.Primary else CuciMoneyColors.Border, RoundedCornerShape(16.dp))
                            .clickable { onSelect(book.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.MenuBook, null, tint = if (sel) CuciMoneyColors.Primary else CuciMoneyColors.TextMuted, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(book.name, color = CuciMoneyColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${accIds.size} akun · ${CuciMoneyHelpers.formatRp(bal, uiState.visibility)}",
                                color = CuciMoneyColors.TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        if (sel) {
                            Icon(Icons.Outlined.Check, null, tint = CuciMoneyColors.Primary, modifier = Modifier.size(20.dp))
                        } else if (uiState.books.size > 1) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                null,
                                tint = CuciMoneyColors.Expense,
                                modifier = Modifier.size(20.dp).clickable { onDeleteBook(book) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CuciMoneyColors.Primary)
                    .clickable { onAddBook() }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ Buku Baru", color = Color(0xFF0A1214), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun TransactionDetailDialog(
    uiState: CuciMoneyUiState,
    trx: CuciTransactionEntity,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isTransfer = trx.transferPair != null
    val cat = uiState.categories.firstOrNull { it.id == trx.categoryId }
    val acc = uiState.accounts.firstOrNull { it.id == trx.accountId }
    val member = uiState.members.firstOrNull { it.id == trx.memberId }
    val pair = if (isTransfer) uiState.transactions.firstOrNull { it.transferPair == trx.transferPair && it.id != trx.id } else null
    val pairAcc = pair?.let { p -> uiState.accounts.firstOrNull { it.id == p.accountId } }

    val color = when {
        isTransfer -> CuciMoneyColors.Transfer
        trx.direction == "in" -> CuciMoneyColors.Income
        else -> CuciMoneyColors.Expense
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA6000000))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(22.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CuciMoneyColors.Card)
                .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(22.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isTransfer) "⇄ Transfer" else "${cat?.emoji ?: "📌"} ${cat?.name ?: "Transaksi"}", color = CuciMoneyColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Icon(Icons.Outlined.Close, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(20.dp).clickable { onClose() })
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = CuciMoneyHelpers.formatRp(trx.amount, uiState.visibility),
                color = color,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CuciMoneyColors.Border)
            Spacer(modifier = Modifier.height(12.dp))

            DetailRow("Catatan", trx.title)
            if (isTransfer && pairAcc != null) {
                val fromName = if (trx.direction == "out") acc?.name else pairAcc.name
                val toName = if (trx.direction == "in") acc?.name else pairAcc.name
                DetailRow("Dari → Ke", "${fromName ?: "-"} → ${toName ?: "-"}")
            } else {
                DetailRow("Akun", "${CuciMoneyHelpers.accountIcon(acc)} ${acc?.name ?: "-"}")
            }
            DetailRow("Tanggal", CuciMoneyHelpers.formatLongDate(trx.date))
            DetailRow("Anggota", "${member?.emoji ?: "😄"} ${member?.name ?: "-"}")

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
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
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CuciMoneyColors.Primary)
                        .clickable { onEdit() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Edit, null, tint = Color(0xFF0A1214), modifier = Modifier.size(16.dp))
                        Text("Edit", color = Color(0xFF0A1214), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = CuciMoneyColors.TextMuted, fontSize = 12.sp)
        Text(value, color = CuciMoneyColors.TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(start = 12.dp))
    }
}

@Composable
private fun SearchTransactionsModal(
    uiState: CuciMoneyUiState,
    onClose: () -> Unit,
    onOpenTrxDetail: (CuciTransactionEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val bookId = CuciMoneyHelpers.activeBook(uiState)?.id ?: uiState.currentBookId
    val catById = remember(uiState.categories) { uiState.categories.associateBy { it.id } }
    val accById = remember(uiState.accounts) { uiState.accounts.associateBy { it.id } }

    val results = remember(uiState.transactions, bookId, query) {
        val q = query.trim().lowercase()
        uiState.transactions
            .filter { t ->
                t.bookId == bookId && (q.isEmpty() ||
                    t.title.lowercase().contains(q) ||
                    (catById[t.categoryId]?.name?.lowercase()?.contains(q) == true) ||
                    (accById[t.accountId]?.name?.lowercase()?.contains(q) == true))
            }
            .sortedByDescending { it.date }
            .take(100)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CuciMoneyColors.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CuciHeaderIconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(18.dp))
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CuciMoneyColors.Card)
                    .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, null, tint = CuciMoneyColors.TextMuted, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 14.sp),
                    cursorBrush = SolidColor(CuciMoneyColors.Primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Cari catatan, kategori, atau akun...", color = CuciMoneyColors.TextLight, fontSize = 13.sp)
                        inner()
                    }
                )
            }
        }
        if (results.isEmpty()) {
            CuciEmptyState(icon = "🔍", text = "Tidak ada transaksi ditemukan")
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.id }) { t ->
                    val cat = catById[t.categoryId]
                    val acc = accById[t.accountId]
                    val isIn = t.direction == "in"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CuciMoneyColors.Card)
                    ) {
                        CuciTransactionRow(
                            emoji = if (t.transferPair != null) "⇄" else cat?.emoji ?: "📌",
                            iconBg = if (t.transferPair != null) Color(0x247C8FF5) else if (isIn) CuciMoneyColors.IncomeDim else CuciMoneyColors.ExpenseDim,
                            title = t.title,
                            subtitle = "${CuciMoneyHelpers.formatDateShort(t.date)} · ${acc?.name ?: ""}",
                            amountText = "${if (t.transferPair != null) "" else if (isIn) "+ " else "− "}${CuciMoneyHelpers.formatRp(t.amount, uiState.visibility)}",
                            amountColor = if (t.transferPair != null) CuciMoneyColors.Transfer else if (isIn) CuciMoneyColors.Income else CuciMoneyColors.Expense,
                            onClick = { onOpenTrxDetail(t) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllTransactionsSheet(
    uiState: CuciMoneyUiState,
    onClose: () -> Unit,
    onOpenTrxDetail: (CuciTransactionEntity) -> Unit
) {
    val bookId = CuciMoneyHelpers.activeBook(uiState)?.id ?: uiState.currentBookId
    val catById = remember(uiState.categories) { uiState.categories.associateBy { it.id } }
    val accById = remember(uiState.accounts) { uiState.accounts.associateBy { it.id } }
    val allTrxs = remember(uiState.transactions, bookId) {
        uiState.transactions.filter { it.bookId == bookId }.sortedByDescending { it.date }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CuciMoneyColors.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CuciHeaderIconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = CuciMoneyColors.TextMain, modifier = Modifier.size(18.dp))
            }
            Text("Semua Transaksi (${allTrxs.size})", color = CuciMoneyColors.TextMain, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (allTrxs.isEmpty()) {
            CuciEmptyState(icon = "🧾", text = "Belum ada transaksi di buku ini")
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allTrxs, key = { it.id }) { t ->
                    val cat = catById[t.categoryId]
                    val acc = accById[t.accountId]
                    val isIn = t.direction == "in"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CuciMoneyColors.Card)
                    ) {
                        CuciTransactionRow(
                            emoji = if (t.transferPair != null) "⇄" else cat?.emoji ?: "📌",
                            iconBg = if (t.transferPair != null) Color(0x247C8FF5) else if (isIn) CuciMoneyColors.IncomeDim else CuciMoneyColors.ExpenseDim,
                            title = t.title,
                            subtitle = "${CuciMoneyHelpers.formatDateShort(t.date)} · ${acc?.name ?: ""}",
                            amountText = "${if (t.transferPair != null) "" else if (isIn) "+ " else "− "}${CuciMoneyHelpers.formatRp(t.amount, uiState.visibility)}",
                            amountColor = if (t.transferPair != null) CuciMoneyColors.Transfer else if (isIn) CuciMoneyColors.Income else CuciMoneyColors.Expense,
                            onClick = { onOpenTrxDetail(t) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CuciConfirmDialog(
    state: CuciConfirmState,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA6000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CuciMoneyColors.Card)
                .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(22.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Text(state.title, color = CuciMoneyColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(state.message, color = CuciMoneyColors.TextMuted, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CuciMoneyColors.Card2)
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Batal", color = CuciMoneyColors.TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.isDanger) CuciMoneyColors.Expense else CuciMoneyColors.Primary)
                        .clickable {
                            state.onConfirm()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.confirmLabel,
                        color = if (state.isDanger) Color.White else Color(0xFF0A1214),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CuciPromptDialog(
    title: String,
    placeholder: String,
    onClose: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xA6000000))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(CuciMoneyColors.Card)
                .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(22.dp))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Text(title, color = CuciMoneyColors.TextMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CuciMoneyColors.Card2)
                    .border(1.dp, CuciMoneyColors.BorderStrong, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = TextStyle(color = CuciMoneyColors.TextMain, fontSize = 14.sp),
                    cursorBrush = SolidColor(CuciMoneyColors.Primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (text.isEmpty()) Text(placeholder, color = CuciMoneyColors.TextLight, fontSize = 13.sp)
                        inner()
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
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
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CuciMoneyColors.Primary)
                        .clickable { onConfirm(text.trim()) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Buat", color = Color(0xFF0A1214), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
