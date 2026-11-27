package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.KnightScopeRepository
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class KnightScopeDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String,
    val primaryTab: Boolean,
    val sectionLabel: String
) {
    HOME("home", R.string.tab_home, Icons.Rounded.Home, "nav_home", true, "Core"),
    PLAY("play", R.string.tab_play, Icons.Rounded.SmartToy, "nav_play", true, "Core"),
    ANALYSE("analyse", R.string.tab_analyse, Icons.Rounded.Analytics, "nav_analyse", true, "Core"),
    PUZZLES("puzzles", R.string.tab_puzzles, Icons.Rounded.Extension, "nav_puzzles", true, "Core"),
    HISTORY("history", R.string.tab_history, Icons.Rounded.History, "nav_history", true, "Core"),
    FRIENDS("friends", R.string.tab_friends, Icons.Rounded.Group, "nav_friends", false, "Play & Study"),
    EXPLORER("explorer", R.string.tab_explorer, Icons.Rounded.Explore, "nav_explorer", false, "Play & Study"),
    TRAINER("trainer", R.string.tab_trainer, Icons.Rounded.FitnessCenter, "nav_trainer", false, "Play & Study"),
    LESSONS("lessons", R.string.tab_lessons, Icons.Rounded.School, "nav_lessons", false, "Play & Study"),
    SETTINGS("settings", R.string.tab_settings, Icons.Rounded.Settings, "nav_settings", false, "Preferences")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnightScopeApp(
    repo: KnightScopeRepository
) {
    val factory = remember(repo) { KnightScopeViewModelFactory(repo = repo) }

    val playVm: PlayViewModel = viewModel(factory = factory)
    val analyzeVm: AnalyzeViewModel = viewModel(factory = factory)
    val puzzlesVm: PuzzlesViewModel = viewModel(factory = factory)
    val friendsVm: FriendsViewModel = viewModel(factory = factory)
    val historyVm: HistoryViewModel = viewModel(factory = factory)
    val explorerVm: OpeningExplorerViewModel = viewModel(factory = factory)
    val trainerVm: OpeningTrainerViewModel = viewModel(factory = factory)
    val lessonsVm: ChessLessonsViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)

    val playState by playVm.uiState.collectAsStateWithLifecycle()
    val analyzeState by analyzeVm.uiState.collectAsStateWithLifecycle()
    val puzzlesState by puzzlesVm.uiState.collectAsStateWithLifecycle()
    val friendsState by friendsVm.uiState.collectAsStateWithLifecycle()
    val historyState by historyVm.uiState.collectAsStateWithLifecycle()
    val explorerState by explorerVm.uiState.collectAsStateWithLifecycle()
    val trainerState by trainerVm.uiState.collectAsStateWithLifecycle()
    val lessonsState by lessonsVm.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showSplash by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(showSplash) {
        if (showSplash) {
            delay(950)
            showSplash = false
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        scope.launch {
            if (drawerState.isOpen) drawerState.close()
        }
    }

    if (drawerState.isOpen) {
        BackHandler {
            scope.launch { drawerState.close() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = SurfaceDark,
                    drawerContentColor = TextPrimary,
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DiggyBrandBadge(size = 44)
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = 21.sp,
                                color = TextPrimary
                            )
                        }

                        HorizontalDivider(color = LineSubtle, modifier = Modifier.padding(vertical = 12.dp))

                        val sections = listOf("Core", "Play & Study", "Preferences")
                        sections.forEach { sec ->
                            Text(
                                text = sec.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                            KnightScopeDestination.entries
                                .filter { it.sectionLabel == sec }
                                .forEach { dest ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                                    val label = stringResource(dest.labelRes)
                                    DiggyDrawerItem(
                                        label = label,
                                        icon = dest.icon,
                                        selected = selected,
                                        onClick = { navigateTo(dest.route) },
                                        testTag = "drawer_${dest.route}"
                                    )
                                }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(BgRadialTop, BgDark, BgDark)
                        )
                    )
            ) {
                val useRail = maxWidth >= 768.dp
                val primaryTabs = remember { KnightScopeDestination.entries.filter { it.primaryTab } }

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = SurfaceDark,
                            contentColor = TextPrimary,
                            modifier = Modifier.border(width = 1.dp, color = LineSubtle)
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .size(DiggySpacing.touchTargetMin)
                                    .testTag("rail_menu_button")
                            ) {
                                Icon(
                                    Icons.Rounded.Menu,
                                    contentDescription = "Open menu",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            primaryTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                val label = stringResource(tab.labelRes)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = AccentGreen,
                                        selectedTextColor = AccentGreen,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = AccentGreen.copy(alpha = 0.14f)
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    val view = LocalView.current
                    val paletteIsDark = LocalKnightScopeColors.current.isDark
                    LaunchedEffect(showSplash, paletteIsDark) {
                        if (!view.isInEditMode) {
                            var ctx: Context? = view.context
                            var hostActivity: Activity? = null
                            while (ctx is ContextWrapper) {
                                if (ctx is Activity) {
                                    hostActivity = ctx
                                    break
                                }
                                ctx = ctx.baseContext
                            }
                            if (hostActivity == null && ctx is Activity) hostActivity = ctx
                            hostActivity?.window?.let { win ->
                                val useLightSystemBars = !paletteIsDark
                                WindowCompat.getInsetsController(win, view).apply {
                                    isAppearanceLightStatusBars = useLightSystemBars
                                    isAppearanceLightNavigationBars = useLightSystemBars
                                }
                            }
                        }
                    }

                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        topBar = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceDark)
                            ) {
                                TopAppBar(
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = Color.Transparent,
                                        titleContentColor = TextPrimary
                                    ),
                                    navigationIcon = {
                                        IconButton(
                                            onClick = { scope.launch { drawerState.open() } },
                                            modifier = Modifier
                                                .size(DiggySpacing.touchTargetMin)
                                                .testTag("drawer_menu_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Menu,
                                                contentDescription = "Open navigation menu",
                                                tint = TextPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.clickable { navigateTo(KnightScopeDestination.HOME.route) }
                                        ) {
                                            DiggyBrandBadge(size = 32)
                                            Text(
                                                text = stringResource(R.string.app_name),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    actions = {
                                        IconButton(
                                            onClick = { navigateTo(KnightScopeDestination.SETTINGS.route) },
                                            modifier = Modifier
                                                .size(DiggySpacing.touchTargetMin)
                                                .testTag("settings_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Settings,
                                                contentDescription = stringResource(R.string.tab_settings),
                                                tint = TextMuted,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                )
                                HorizontalDivider(color = LineSubtle)
                            }
                        },
                        bottomBar = {
                            if (!useRail) {
                                DiggyBottomNavBar(
                                    tabs = primaryTabs,
                                    currentDestination = currentDestination,
                                    onSelectTab = { navigateTo(it.route) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = KnightScopeDestination.HOME.route,
                            enterTransition = {
                                fadeIn(animationSpec = tween(200)) +
                                    slideInVertically(animationSpec = tween(200)) { it / 48 }
                            },
                            exitTransition = { fadeOut(animationSpec = tween(150)) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable(KnightScopeDestination.HOME.route) {
                                HomeScreen(
                                    latestAnalysis = analyzeState.result,
                                    onAnalysePgn = { pgn ->
                                        analyzeVm.loadAndAnalyzePgn(pgn)
                                        navigateTo(KnightScopeDestination.ANALYSE.route)
                                    },
                                    onSearchUsername = { platform, username ->
                                        analyzeVm.setImportPlatform(platform)
                                        analyzeVm.updateImportUsername(username)
                                        analyzeVm.openImporterSheet()
                                        analyzeVm.fetchPlatformGames()
                                        navigateTo(KnightScopeDestination.ANALYSE.route)
                                    },
                                    onUploadPgn = { content, fileName ->
                                        analyzeVm.importUploadedPgn(content, fileName)
                                        navigateTo(KnightScopeDestination.ANALYSE.route)
                                    },
                                    onOpenPlatformImporter = {
                                        analyzeVm.openImporterSheet()
                                        navigateTo(KnightScopeDestination.ANALYSE.route)
                                    },
                                    onTrySample = {
                                        analyzeVm.trySample()
                                        navigateTo(KnightScopeDestination.ANALYSE.route)
                                    },
                                    onOpenLatestAnalysis = {
                                        navigateTo(KnightScopeDestination.ANALYSE.route)
                                    }
                                )
                            }

                            composable(KnightScopeDestination.PLAY.route) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    PlayScreen(
                                        state = playState,
                                        onSelectDifficulty = playVm::setDifficulty,
                                        onSelectColor = playVm::selectColorChoice,
                                        onSelectBaseTime = playVm::selectBaseTime,
                                        onSelectIncrement = playVm::selectIncrement,
                                        onSelectTimeControl = playVm::selectTimeControl,
                                        onStartQuickPlay = playVm::startQuickPlay,
                                        onStartGame = { playVm.startGame() },
                                        onSquareClick = playVm::onSquareClick,
                                        onSelectPromotion = playVm::selectPromotion,
                                        onDismissPromotion = playVm::dismissPromotion,
                                        onHint = playVm::requestHint,
                                        onTakeback = playVm::takeback,
                                        onFlip = playVm::toggleFlip,
                                        onOfferDraw = playVm::offerDraw,
                                        onResign = playVm::resign,
                                        onRematch = playVm::rematch,
                                        onAnalyseGame = {
                                            val pgn = playVm.buildFinishedPgn()
                                            analyzeVm.loadAndAnalyzePgn(pgn)
                                            navigateTo(KnightScopeDestination.ANALYSE.route)
                                        },
                                        onAnalyseRecentPgn = { pgn ->
                                            analyzeVm.loadAndAnalyzePgn(pgn)
                                            navigateTo(KnightScopeDestination.ANALYSE.route)
                                        },
                                        onLeaveGame = playVm::leaveGame,
                                        onRetryEngine = playVm::retryEngine,
                                        modifier = Modifier.widthIn(max = 1120.dp)
                                    )
                                }
                            }

                            composable(KnightScopeDestination.ANALYSE.route) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    AnalyzeScreen(
                                        state = analyzeState,
                                        onPgnChange = analyzeVm::updatePgn,
                                        onAnalyze = { analyzeVm.analyzeGame() },
                                        onSelectDepthPreset = analyzeVm::selectDepthPreset,
                                        onRetryEngine = analyzeVm::retryEngineInit,
                                        onTrySample = analyzeVm::trySample,
                                        onSharePgn = { analyzeVm.openShare(forceAnalysis = false) },
                                        onShareAnalysis = { analyzeVm.openShare(forceAnalysis = true) },
                                        onDismissShare = analyzeVm::closeShare,
                                        onOpenRecent = analyzeVm::openFromHistory,
                                        onGoToPly = analyzeVm::goToPly,
                                        onToggleBestMove = analyzeVm::toggleBestMove,
                                        onToggleFlip = analyzeVm::toggleFlip,
                                        onBoardThemeChange = analyzeVm::setBoardTheme,
                                        onOpenImporter = analyzeVm::openImporterSheet,
                                        onCloseImporter = analyzeVm::closeImporterSheet,
                                        onSelectImportPlatform = analyzeVm::setImportPlatform,
                                        onImportUsernameChange = analyzeVm::updateImportUsername,
                                        onFetchPlatformGames = analyzeVm::fetchPlatformGames,
                                        onSpeedFilter = analyzeVm::setImportSpeedFilter,
                                        onResultFilter = analyzeVm::setImportResultFilter,
                                        onColorFilter = analyzeVm::setImportColorFilter,
                                        onAnalyzeImportedGame = analyzeVm::importAndAnalyzeGame,
                                        onBoardSquareClick = analyzeVm::onBoardSquareClick,
                                        onSelectVariationPromotion = analyzeVm::selectVariationPromotion,
                                        onDismissVariationPromotion = analyzeVm::dismissVariationPromotion,
                                        onPlayEngineLineMove = analyzeVm::playEngineLineMoves,
                                        onStepVariation = analyzeVm::stepToVariationIndex,
                                        onUndoVariation = analyzeVm::undoVariationMove,
                                        onExitVariation = analyzeVm::exitVariation,
                                        onPracticeMistakes = analyzeVm::startMistakeDrill,
                                        onDrillSquareClick = analyzeVm::onDrillSquareClick,
                                        onDrillPromotion = analyzeVm::selectDrillPromotion,
                                        onDismissDrillPromotion = analyzeVm::dismissDrillPromotion,
                                        onDrillHint = analyzeVm::revealDrillHint,
                                        onDrillRetry = analyzeVm::retryCurrentMistake,
                                        onDrillNext = analyzeVm::nextMistakeDrill,
                                        onCloseDrill = analyzeVm::closeMistakeDrill,
                                        onUploadPgn = analyzeVm::importUploadedPgn,
                                        onRequestMoveInsight = { plyIdx -> analyzeVm.requestMoveInsight(plyIdx, allowCloud = true) },
                                        onUseOfflineMoveInsight = analyzeVm::useOfflineMoveInsight,
                                        onRequestGameReport = { analyzeVm.requestAiGameReport(allowCloud = true) },
                                        onUseOfflineGameReport = analyzeVm::useOfflineGameReport,
                                        onSendCoachMessage = analyzeVm::sendCoachChatMessage,
                                        modifier = Modifier.widthIn(max = 1040.dp)
                                    )
                                }
                            }

                            composable(KnightScopeDestination.PUZZLES.route) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    PuzzlesScreen(
                                        state = puzzlesState,
                                        levelSolvedCount = puzzlesVm::levelSolvedCount,
                                        isLevelUnlocked = puzzlesVm::isLevelUnlocked,
                                        levelPuzzleCount = puzzlesVm::levelPuzzleCount,
                                        onStartRated = puzzlesVm::startRated,
                                        onSetStartingRating = puzzlesVm::setStartingRating,
                                        onStartLevel = puzzlesVm::startLevel,
                                        onToggleCategory = puzzlesVm::toggleCategory,
                                        onSelectTheme = puzzlesVm::setThemeFilter,
                                        onStartPractice = puzzlesVm::startPractice,
                                        onStartQuickDifficulty = puzzlesVm::startQuickDifficulty,
                                        onStartDaily = puzzlesVm::startDaily,
                                        onStartRandom = puzzlesVm::startRandom,
                                        onStartTimed = puzzlesVm::startTimed,
                                        onStartReviewMistakes = puzzlesVm::startReviewMistakes,
                                        onStartMyGameMistakes = puzzlesVm::startMyGameMistakes,
                                        onSetDailyGoal = puzzlesVm::setDailyGoal,
                                        onToggleDifficultyFilter = puzzlesVm::toggleDifficultyFilter,
                                        onSetDifficultyFilter = puzzlesVm::setDifficultyFilter,
                                        onImportCsv = puzzlesVm::importCsvText,
                                        onSquareClick = puzzlesVm::onSquareClick,
                                        onSelectPromotion = puzzlesVm::selectPromotion,
                                        onDismissPromotion = puzzlesVm::dismissPromotion,
                                        onRestartPuzzle = puzzlesVm::restartPuzzle,
                                        onUndoPuzzle = puzzlesVm::undoPuzzleMove,
                                        onHintPuzzle = puzzlesVm::showHint,
                                        onRevealHintDestination = puzzlesVm::revealDestinationHint,
                                        onDismissHintModal = puzzlesVm::dismissHintModal,
                                        onNextPuzzle = puzzlesVm::requestNextPuzzle,
                                        onConfirmSkip = puzzlesVm::confirmSkipPuzzle,
                                        onDismissSkip = puzzlesVm::dismissSkipDialog,
                                        onToggleFlip = puzzlesVm::toggleFlip,
                                        onBackToMenu = puzzlesVm::backToMenu,
                                        modifier = Modifier.widthIn(max = 920.dp)
                                    )
                                }
                            }

                            composable(KnightScopeDestination.FRIENDS.route) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    FriendsScreen(
                                        state = friendsState,
                                        onMyNameChange = friendsVm::updateMyName,
                                        onOpponentNameChange = friendsVm::updateOpponentNameInput,
                                        onSelectColor = friendsVm::selectColorChoice,
                                        onSelectTimeControl = friendsVm::selectTimeControl,
                                        onCreateOnlineInvite = friendsVm::createOnlineInvite,
                                        onClearOnlineInvite = friendsVm::clearOnlineInvite,
                                        onJoinAddressChange = friendsVm::updateJoinAddress,
                                        onStartPassAndPlay = friendsVm::startPassAndPlay,
                                        onHostWifiMatch = friendsVm::hostWifiMatch,
                                        onJoinWifiMatch = friendsVm::joinWifiMatch,
                                        onSquareClick = friendsVm::onSquareClick,
                                        onSelectPromotion = friendsVm::selectPromotion,
                                        onDismissPromotion = friendsVm::dismissPromotion,
                                        onToggleFlip = friendsVm::toggleFlip,
                                        onOfferDraw = friendsVm::offerDraw,
                                        onAcceptDraw = friendsVm::acceptDraw,
                                        onDeclineDraw = friendsVm::declineDraw,
                                        onResign = friendsVm::resign,
                                        onRematch = friendsVm::requestRematch,
                                        onBuildPgn = friendsVm::buildFinishedPgn,
                                        onAnalyseGame = {
                                            val pgn = friendsVm.buildFinishedPgn()
                                            analyzeVm.loadAndAnalyzePgn(pgn)
                                            navigateTo(KnightScopeDestination.ANALYSE.route)
                                        },
                                        onLeaveGame = friendsVm::leaveGame,
                                        modifier = Modifier.widthIn(max = 760.dp)
                                    )
                                }
                            }

                            composable(KnightScopeDestination.HISTORY.route) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    HistoryScreen(
                                        state = historyState,
                                        onSelectSideFilter = historyVm::setSideFilter,
                                        onRefresh = historyVm::refresh,
                                        onOpenGame = { entry ->
                                            analyzeVm.openFromHistory(entry)
                                            navigateTo(KnightScopeDestination.ANALYSE.route)
                                        },
                                        onRequestDelete = historyVm::requestDelete,
                                        onConfirmDelete = historyVm::confirmDelete,
                                        onCancelDelete = historyVm::cancelDelete,
                                        onGoToAnalyze = { navigateTo(KnightScopeDestination.ANALYSE.route) },
                                        modifier = Modifier.widthIn(max = 820.dp)
                                    )
                                }
                            }

                            composable(KnightScopeDestination.EXPLORER.route) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    OpeningExplorerScreen(
                                        state = explorerState,
                                        onSquareClick = explorerVm::onSquareClick,
                                        onSelectPromotion = explorerVm::selectPromotion,
                                        onDismissPromotion = explorerVm::dismissPromotion,
                                        onPlayMove = explorerVm::playUciMove,
                                        onUndo = explorerVm::undoMove,
                                        onReset = explorerVm::resetBoard,
                                        onToggleFlip = explorerVm::toggleFlip,
                                        onDatabaseChange = explorerVm::setDatabaseMode,
                                        onAnalysePosition = {
                                            analyzeVm.loadAndAnalyzePgn(explorerVm.buildCurrentPgn())
                                            navigateTo(KnightScopeDestination.ANALYSE.route)
                                        },
                                        modifier = Modifier.widthIn(max = 860.dp)
                                    )
                                }
                            }

                            composable(KnightScopeDestination.TRAINER.route) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    OpeningTrainerScreen(
                                        state = trainerState,
                                        onSelectRepertoire = trainerVm::selectRepertoire,
                                        onSquareClick = trainerVm::onSquareClick,
                                        onHint = trainerVm::requestHint,
                                        onRestart = trainerVm::restartDrill,
                                        onNextRepertoire = trainerVm::nextRepertoire,
                                        modifier = Modifier.widthIn(max = 780.dp)
                                    )
                                }
                            }

                            composable(KnightScopeDestination.LESSONS.route) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    ChessLessonsScreen(
                                        state = lessonsState,
                                        onOpenLesson = lessonsVm::openLesson,
                                        onCloseLesson = lessonsVm::closeLesson,
                                        onSquareClick = lessonsVm::onSquareClick,
                                        onSelectPromotion = lessonsVm::selectPromotion,
                                        onDismissPromotion = lessonsVm::dismissPromotion,
                                        onHint = lessonsVm::showHint,
                                        onRetry = lessonsVm::retryStep,
                                        onNextStep = lessonsVm::nextStep,
                                        modifier = Modifier.widthIn(max = 780.dp)
                                    )
                                }
                            }

                            composable(KnightScopeDestination.SETTINGS.route) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    SettingsScreen(
                                        state = settingsState,
                                        onSetDarkMode = settingsVm::setDarkMode,
                                        onSelectBoardTheme = settingsVm::setBoardTheme,
                                        onSelectPieceStyle = settingsVm::setPieceStyle,
                                        onSetBoard3dEnabled = settingsVm::setBoard3dEnabled,
                                        onSetPieceAnimations = settingsVm::setPieceAnimations,
                                        onSetSoundEnabled = settingsVm::setSoundEnabled,
                                        onSetHapticsEnabled = settingsVm::setHapticsEnabled,
                                        onSelectAnalysisPreset = settingsVm::setAnalysisPreset,
                                        onSelectDailyGoal = settingsVm::setDailyGoal,
                                        onRunSelfTest = settingsVm::runSelfTest,
                                        onGenerateBackup = settingsVm::generateBackupJson,
                                        onRestoreBackup = { json ->
                                            settingsVm.restoreBackupJson(json) { puzzlesVm.reloadPool() }
                                        },
                                        onBackupMessage = settingsVm::setBackupMessage,
                                        onSendContact = settingsVm::sendContactMessage,
                                        onSaveGroqApiKey = settingsVm::saveGroqApiKey,
                                        onSaveTinyFishApiKey = settingsVm::saveTinyFishApiKey,
                                        onGoogleSignInStarting = { settingsVm.setAuthBusy(true) },
                                        onGoogleIdTokenSignedIn = settingsVm::signInWithGoogleIdToken,
                                        onSignOutAccount = settingsVm::signOutAccount,
                                        onAuthError = settingsVm::setAuthError,
                                        modifier = Modifier.widthIn(max = 780.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Chess Analyzer: AI Coach Launch Splash Overlay (tagline shown ONLY on splash)
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(220))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(BgRadialTop, BgDark, SurfaceDark)
                        )
                    )
                    .clickable { showSplash = false }
                    .testTag("diggy_splash_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DiggyBrandBadge(size = 88)
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Play. Learn. Win.",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun DiggyDrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bg = if (selected) AccentGreen.copy(alpha = 0.14f) else Color.Transparent
    val iconColor = if (selected) AccentGreen else TextMuted
    val textColor = if (selected) TextPrimary else TextMuted

    Surface(
        onClick = onClick,
        color = bg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .heightIn(min = DiggySpacing.touchTargetMin)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selected) AccentGreen else Color.Transparent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DiggyBottomNavBar(
    tabs: List<KnightScopeDestination>,
    currentDestination: androidx.navigation.NavDestination?,
    onSelectTab: (KnightScopeDestination) -> Unit
) {
    val barBg by animateColorAsState(
        targetValue = SurfaceDark,
        animationSpec = tween(durationMillis = 220),
        label = "navBarBg"
    )
    val barBorder by animateColorAsState(
        targetValue = LineSubtle,
        animationSpec = tween(durationMillis = 220),
        label = "navBarBorder"
    )
    val activeContentColor = AccentGreen
    val inactiveContentColor = TextMuted
    val activePillColor = AccentGreen.copy(alpha = 0.14f)

    Surface(
        color = barBg,
        border = BorderStroke(1.dp, barBorder),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                val label = stringResource(tab.labelRes)
                val pillBg by animateColorAsState(
                    targetValue = if (selected) activePillColor else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "navPillBg"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (selected) activeContentColor else inactiveContentColor,
                    animationSpec = tween(durationMillis = 200),
                    label = "navIconTint"
                )

                Surface(
                    onClick = { onSelectTab(tab) },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = DiggySpacing.touchTargetMin)
                        .testTag(tab.testTag)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(pillBg)
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = label,
                                tint = iconTint,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                        Text(
                            text = label,
                            fontFamily = InterFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) activeContentColor else inactiveContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiggyBrandBadge(size: Int = 32) {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_art),
        contentDescription = "Chess Analyzer: AI Coach crest",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.2.dp, TrophyGold.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
    )
}
