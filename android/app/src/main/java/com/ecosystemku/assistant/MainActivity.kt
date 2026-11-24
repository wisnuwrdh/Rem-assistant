package com.ecosystemku.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecosystemku.assistant.device.AndroidDeviceBridge
import com.ecosystemku.assistant.device.DeviceActionEvent
import com.ecosystemku.assistant.notifications.DailyOsNotificationHelper
import com.ecosystemku.assistant.ui.apps.AppsHubScreen
import com.ecosystemku.assistant.ui.apps.cucimoney.CuciMoneyScreen
import com.ecosystemku.assistant.ui.apps.dailyos.DailyOsScreen
import com.ecosystemku.assistant.ui.apps.ideku.IdekuScreen
import com.ecosystemku.assistant.ui.apps.kronik.KronikScreen
import com.ecosystemku.assistant.ui.apps.linkku.LinkkuScreen
import com.ecosystemku.assistant.ui.asisten.AsistenScreen
import com.ecosystemku.assistant.ui.asisten.AsistenViewModel
import com.ecosystemku.assistant.ui.kg.KnowledgeGraphScreen
import com.ecosystemku.assistant.ui.navigation.AppTab
import com.ecosystemku.assistant.ui.navigation.EcosystemBottomNavBar
import com.ecosystemku.assistant.ui.setelan.SetelanScreen
import com.ecosystemku.assistant.ui.theme.DominantBg
import com.ecosystemku.assistant.ui.theme.EcosystemKuTheme

class MainActivity : ComponentActivity() {
    private var pendingLaunchAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractLaunchAction(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            EcosystemKuShell(
                pendingLaunchAction = pendingLaunchAction,
                onConsumeLaunchAction = { pendingLaunchAction = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractLaunchAction(intent)
    }

    private fun extractLaunchAction(intent: Intent?) {
        val action = intent?.getStringExtra(DailyOsNotificationHelper.EXTRA_LAUNCH_ACTION)
        if (!action.isNullOrBlank()) {
            intent.removeExtra(DailyOsNotificationHelper.EXTRA_LAUNCH_ACTION)
            pendingLaunchAction = action
        }
    }
}

@Composable
fun EcosystemKuShell(
    asistenViewModel: AsistenViewModel = viewModel(),
    pendingLaunchAction: String? = null,
    onConsumeLaunchAction: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.ASISTEN) }
    var showKronik by rememberSaveable { mutableStateOf(false) }
    var showLinkku by rememberSaveable { mutableStateOf(false) }
    var showCuciMoney by rememberSaveable { mutableStateOf(false) }
    var showIdeku by rememberSaveable { mutableStateOf(false) }
    var showDailyOs by rememberSaveable { mutableStateOf(false) }
    var showKnowledgeGraph by rememberSaveable { mutableStateOf(false) }
    val asistenState by asistenViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingPermissionLabel by remember { mutableStateOf("") }
    val devicePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val lbl = pendingPermissionLabel.ifBlank { "Perangkat" }
        pendingPermissionLabel = ""
        if (granted) {
            asistenViewModel.showBannerMessage("Izin $lbl aktif, silakan tanya Rem lagi")
        } else {
            asistenViewModel.showBannerMessage("Izin $lbl ditolak di perangkat")
        }
    }

    EcosystemKuTheme(themeMode = asistenState.themeMode) {
        val kronikActions = remember(asistenViewModel) { asistenViewModel.createKronikActions() }
        val linkkuActions = remember(asistenViewModel) { asistenViewModel.createLinkkuActions() }
        val cuciMoneyActions = remember(asistenViewModel) { asistenViewModel.createCuciMoneyActions() }
        val idekuActions = remember(asistenViewModel) { asistenViewModel.createIdekuActions() }
        val dailyOsActions = remember(asistenViewModel) { asistenViewModel.createDailyOsActions() }
        val blockerInteraction = remember { MutableInteractionSource() }
        val lifecycleOwner = LocalLifecycleOwner.current
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val isAsistenActive = selectedTab == AppTab.ASISTEN && !showKronik && !showLinkku && !showCuciMoney && !showIdeku && !showDailyOs && !showKnowledgeGraph
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        var wasImeVisible by remember { mutableStateOf(false) }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> asistenViewModel.setForegroundActive(true)
                    Lifecycle.Event.ON_STOP -> {
                        asistenViewModel.setForegroundActive(false)
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(imeVisible) {
            if (wasImeVisible && !imeVisible) {
                focusManager.clearFocus(force = true)
            }
            wasImeVisible = imeVisible
        }

        LaunchedEffect(isAsistenActive) {
            asistenViewModel.setAsistenTabActive(isAsistenActive)
            if (!isAsistenActive) {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
        }

        LaunchedEffect(pendingLaunchAction) {
            if (pendingLaunchAction == DailyOsNotificationHelper.ACTION_KENALI_AKU) {
                onConsumeLaunchAction()
                showKronik = false
                showLinkku = false
                showCuciMoney = false
                showIdeku = false
                showDailyOs = false
                showKnowledgeGraph = false
                selectedTab = AppTab.ASISTEN
                asistenViewModel.startKenaliAkuSession()
            }
        }

        LaunchedEffect(asistenViewModel) {
            asistenViewModel.deviceEvents.collect { event ->
                if (event is DeviceActionEvent.RequestPermission) {
                    pendingPermissionLabel = event.reasonLabel
                    devicePermLauncher.launch(event.permission)
                } else {
                    val err = AndroidDeviceBridge.executeAction(context, event)
                    if (!err.isNullOrBlank()) {
                        asistenViewModel.showBannerMessage(err)
                    }
                }
            }
        }

        LaunchedEffect(asistenState.pendingOpenApp) {
            when (asistenState.pendingOpenApp) {
                "kronik" -> {
                    showLinkku = false
                    showCuciMoney = false
                    showIdeku = false
                    showDailyOs = false
                    showKronik = true
                    asistenViewModel.consumeOpenApp()
                }
                "linkku" -> {
                    showKronik = false
                    showCuciMoney = false
                    showIdeku = false
                    showDailyOs = false
                    showLinkku = true
                    asistenViewModel.consumeOpenApp()
                }
                "cucimoney" -> {
                    showKronik = false
                    showLinkku = false
                    showIdeku = false
                    showDailyOs = false
                    showCuciMoney = true
                    asistenViewModel.consumeOpenApp()
                }
                "ideku" -> {
                    showKronik = false
                    showLinkku = false
                    showCuciMoney = false
                    showDailyOs = false
                    showIdeku = true
                    asistenViewModel.consumeOpenApp()
                }
                "dailyos" -> {
                    showKronik = false
                    showLinkku = false
                    showCuciMoney = false
                    showIdeku = false
                    showDailyOs = true
                    asistenViewModel.consumeOpenApp()
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DominantBg)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Pertahankan instance AsistenScreen (termasuk WebView Live2D) agar tidak reload saat pindah tab
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    AsistenScreen(
                        uiState = asistenState,
                        stageController = asistenViewModel.stageController,
                        inputTextFlow = asistenViewModel.inputText,
                        streamingTextFlow = asistenViewModel.streamingText,
                        idleBubbleTextFlow = asistenViewModel.idleBubbleText,
                        onInputChanged = asistenViewModel::onInputChanged,
                        onSend = asistenViewModel::sendMessage,
                        onStageDragDelta = asistenViewModel::onStageDragDelta,
                        onDismissError = asistenViewModel::dismissError,
                        onDismissKronikBanner = asistenViewModel::dismissKronikBanner,
                        onRetryModel = asistenViewModel::retryModel,
                        onToggleMute = asistenViewModel::toggleMute,
                        onToggleVoiceClipMute = asistenViewModel::toggleVoiceClipMute,
                        onClearChat = asistenViewModel::clearChatHistoryOnly,
                        onSelectChoices = asistenViewModel::selectMessageChoices,
                        onNewChatSession = asistenViewModel::startNewChatSession,
                        onSwitchChatSession = asistenViewModel::switchChatSession,
                        onRenameChatSession = asistenViewModel::renameChatSession,
                        onDeleteChatSession = asistenViewModel::deleteChatSession,
                        onDeleteMessage = asistenViewModel::deleteSingleMessage,
                        onRegenerateMessage = asistenViewModel::regenerateAssistantMessage,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (selectedTab == AppTab.ASISTEN) 1f else 0f)
                            .focusProperties { canFocus = isAsistenActive }
                    )

                    if (selectedTab == AppTab.APPS) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(2f)
                                .background(DominantBg)
                                .clickable(interactionSource = blockerInteraction, indication = null) {}
                        ) {
                            AppsHubScreen(
                                profileName = asistenState.profileName,
                                kronikEntries = asistenState.kronikEntries,
                                linkkuLinks = asistenState.linkkuState.links,
                                cuciMoneyState = asistenState.cuciMoneyState,
                                idekuState = asistenState.idekuState,
                                dailyOsState = asistenState.dailyOsState,
                                onOpenKronik = { showKronik = true },
                                onOpenLinkku = { showLinkku = true },
                                onOpenCuciMoney = { showCuciMoney = true },
                                onOpenIdeku = { showIdeku = true },
                                onOpenDailyOs = { showDailyOs = true },
                                onToggleCuciVisibility = cuciMoneyActions.onToggleVisibility,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else if (selectedTab == AppTab.SETELAN) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(2f)
                                .background(DominantBg)
                                .clickable(interactionSource = blockerInteraction, indication = null) {}
                        ) {
                            SetelanScreen(
                                profileName = asistenState.profileName,
                                lastBackupLabel = asistenState.lastBackupLabel,
                                themeMode = asistenState.themeMode,
                                stageWallpaperFile = asistenState.stageWallpaperFile,
                                stageWallpaperDim = asistenState.stageWallpaperDim,
                                aiConfig = asistenState.aiConfig,
                                apiConnectionOk = asistenState.apiConnectionOk,
                                apiConnectionLabel = asistenState.apiConnectionLabel,
                                isTestingApi = asistenState.isTestingApi,
                                notifHabitEnabled = asistenState.notifHabitEnabled,
                                notifTodoEnabled = asistenState.notifTodoEnabled,
                                notifHabitTime = asistenState.dailyOsState.notifHabitTime,
                                notifTodoTime = asistenState.dailyOsState.notifTodoTime,
                                memories = asistenState.memories,
                                summaries = asistenState.summaries,
                                memoryProfile = asistenState.memoryProfile,
                                isSynthesizingMemoryProfile = asistenState.isSynthesizingMemoryProfile,
                                autoLearnMemory = asistenState.autoLearnMemory,
                                isLearningMemory = asistenState.isLearningMemory,
                                kgNodeCount = asistenState.kgNodes.size,
                                kgEdgeCount = asistenState.kgEdges.size,
                                kgMicroQuestionsEnabled = asistenState.kgMicroQuestionsEnabled,
                                kgWeeklyReflectionEnabled = asistenState.kgWeeklyReflectionEnabled,
                                kgWeeklyReflectionDay = asistenState.kgWeeklyReflectionDay,
                                kgWeeklyReflectionTime = asistenState.kgWeeklyReflectionTime,
                                deviceLocationOptIn = asistenState.deviceLocationOptIn,
                                onUpdateProfileName = asistenViewModel::setProfileName,
                                onSetThemeMode = asistenViewModel::setThemeMode,
                                onPickStageWallpaper = asistenViewModel::setStageWallpaper,
                                onChangeStageWallpaperDim = asistenViewModel::setStageWallpaperDim,
                                onResetStageWallpaper = asistenViewModel::clearStageWallpaper,
                                onGetAiConfigValue = asistenViewModel::getAiConfigValue,
                                onSaveAiConfigValue = asistenViewModel::saveAiConfigValue,
                                onTestApiConnection = asistenViewModel::testApiConnection,
                                onSetNotifHabitEnabled = asistenViewModel::setNotifHabitEnabled,
                                onSetNotifTodoEnabled = asistenViewModel::setNotifTodoEnabled,
                                onSetNotifTimes = asistenViewModel::setNotifTimes,
                                onTestNotification = asistenViewModel::triggerTestNotification,
                                onToggleAutoLearn = asistenViewModel::setAutoLearnMemory,
                                onOpenKnowledgeGraph = { showKnowledgeGraph = true },
                                onToggleKgMicroQuestions = asistenViewModel::setKgMicroQuestionsEnabled,
                                onToggleKgWeeklyReflection = asistenViewModel::setKgWeeklyReflectionEnabled,
                                onSetKgWeeklyReflectionSchedule = asistenViewModel::setKgWeeklyReflectionSchedule,
                                onSetDeviceLocationOptIn = asistenViewModel::setDeviceLocationOptIn,
                                onAddManualMemory = asistenViewModel::addManualMemory,
                                onDeleteMemory = asistenViewModel::deleteMemory,
                                onDeleteSummary = asistenViewModel::deleteSummary,
                                onClearMemories = asistenViewModel::clearAllMemories,
                                onRefreshMemoryProfile = { asistenViewModel.refreshMemoryProfile(force = true) },
                                onSaveMemoryProfileSection = asistenViewModel::saveMemoryProfileSection,
                                onDeleteMemoryProfileSection = asistenViewModel::deleteMemoryProfileSection,
                                onClearMemoryProfile = { asistenViewModel.clearMemoryProfile(showBanner = true) },
                                onExportZip = asistenViewModel::buildEcosystemZipBytes,
                                onImportZip = asistenViewModel::importEcosystemZipBytes,
                                cloudState = asistenState.cloudState,
                                onConnectCloud = asistenViewModel::connectGoogleDrive,
                                onHandleCloudAuthIntent = asistenViewModel::handleGoogleDriveAuthIntent,
                                onDisconnectCloud = asistenViewModel::disconnectGoogleDrive,
                                onRefreshCloudInfo = asistenViewModel::refreshCloudRemoteInfo,
                                onCloudBackupNow = asistenViewModel::backupToCloudNow,
                                onPrepareCloudRestore = asistenViewModel::prepareCloudRestore,
                                onDecryptCloudBytes = asistenViewModel::decryptCloudBytesWithPassphrase,
                                onSetCloudAutoSchedule = asistenViewModel::setCloudAutoSchedule,
                                onSetCloudPassphrase = asistenViewModel::setCloudPassphrase,
                                onShowBanner = asistenViewModel::showBannerMessage,
                                onResetKronik = asistenViewModel::resetKronikData,
                                onResetLinkku = asistenViewModel::resetLinkkuData,
                                onResetCuciMoney = asistenViewModel::resetCuciMoneyData,
                                onResetIdeku = asistenViewModel::resetIdekuData,
                                onResetDailyOs = asistenViewModel::resetDailyOsData,
                                onResetAssistantData = asistenViewModel::resetAssistantData,
                                onResetAllData = asistenViewModel::resetAllEcosystemData,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Saat keyboard tampil, bottom nav disembunyikan: imePadding di AsistenScreen sudah
                // menghitung tinggi keyboard penuh dari dasar layar, jadi jika bottom nav tetap
                // ada, tingginya terhitung dua kali dan bar chat melayang jauh di atas keyboard.
                if (!imeVisible) {
                    EcosystemBottomNavBar(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                        onAsistenDoubleTap = asistenViewModel::toggleChatOpen
                    )
                }
            }

        if (showKronik) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f)
                    .background(DominantBg)
                    .clickable(interactionSource = blockerInteraction, indication = null) {}
            ) {
                KronikScreen(
                    entries = asistenState.kronikEntries,
                    actions = kronikActions,
                    onClose = { showKronik = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showLinkku) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(11f)
                    .background(DominantBg)
                    .clickable(interactionSource = blockerInteraction, indication = null) {}
            ) {
                LinkkuScreen(
                    uiState = asistenState.linkkuState,
                    actions = linkkuActions,
                    onClose = { showLinkku = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showCuciMoney) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(12f)
                    .background(DominantBg)
                    .clickable(interactionSource = blockerInteraction, indication = null) {}
            ) {
                CuciMoneyScreen(
                    uiState = asistenState.cuciMoneyState,
                    actions = cuciMoneyActions,
                    onBack = { showCuciMoney = false }
                )
            }
        }

        if (showIdeku) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(13f)
                    .background(DominantBg)
                    .clickable(interactionSource = blockerInteraction, indication = null) {}
            ) {
                IdekuScreen(
                    uiState = asistenState.idekuState,
                    actions = idekuActions,
                    onClose = { showIdeku = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showDailyOs) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(14f)
                    .background(DominantBg)
                    .clickable(interactionSource = blockerInteraction, indication = null) {}
            ) {
                DailyOsScreen(
                    uiState = asistenState.dailyOsState,
                    actions = dailyOsActions,
                    onClose = { showDailyOs = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showKnowledgeGraph) {
            BackHandler(enabled = true) { showKnowledgeGraph = false }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(15f)
                    .background(DominantBg)
                    .clickable(interactionSource = blockerInteraction, indication = null) {}
            ) {
                KnowledgeGraphScreen(
                    profileName = asistenState.profileName,
                    nodes = asistenState.kgNodes,
                    edges = asistenState.kgEdges,
                    isSyncing = asistenState.isSyncingKg,
                    onEnsureSyncedOnce = asistenViewModel::ensureKgSyncedOnce,
                    onSyncSignals = { asistenViewModel.syncKnowledgeGraphSignals(silent = false) },
                    onStartKenaliAku = {
                        showKnowledgeGraph = false
                        selectedTab = AppTab.ASISTEN
                        asistenViewModel.startKenaliAkuSession()
                    },
                    onAddManualEdge = asistenViewModel::addManualKgEdge,
                    onMarkEdgeHistorical = asistenViewModel::markKgEdgeHistorical,
                    onUpdateNode = asistenViewModel::updateKgNode,
                    onDeleteNode = asistenViewModel::deleteKgNode,
                    onDeleteEdge = asistenViewModel::deleteKgEdge,
                    onClearGraph = asistenViewModel::clearKnowledgeGraph,
                    onExportDigitalTwinJson = asistenViewModel::buildDigitalTwinJson,
                    onShowBanner = asistenViewModel::showBannerMessage,
                    onClose = { showKnowledgeGraph = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        }
    }
}
