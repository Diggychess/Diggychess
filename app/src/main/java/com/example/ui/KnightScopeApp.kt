package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch

private enum class KnightScopeDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val primaryTab: Boolean,
    val sectionLabel: String
) {
    HOME("home", R.string.tab_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_home", true, "CORE"),
    PLAY("play", R.string.tab_play, Icons.Filled.SmartToy, Icons.Outlined.SmartToy, "nav_play", true, "CORE"),
    ANALYSE("analyse", R.string.tab_analyse, Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_analyse", true, "CORE"),
    PUZZLES("puzzles", R.string.tab_puzzles, Icons.Filled.Extension, Icons.Outlined.Extension, "nav_puzzles", true, "CORE"),
    HISTORY("history", R.string.tab_history, Icons.Filled.History, Icons.Outlined.History, "nav_history", true, "CORE"),
    FRIENDS("friends", R.string.tab_friends, Icons.Filled.Group, Icons.Outlined.Group, "nav_friends", false, "PLAY & STUDY"),
    EXPLORER("explorer", R.string.tab_explorer, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explorer", false, "PLAY & STUDY"),
    TRAINER("trainer", R.string.tab_trainer, Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter, "nav_trainer", false, "PLAY & STUDY"),
    LESSONS("lessons", R.string.tab_lessons, Icons.Filled.School, Icons.Outlined.School, "nav_lessons", false, "PLAY & STUDY"),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings", false, "PREFERENCES")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnightScopeApp(
    repo: KnightScopeRepository
) {
    val factory = remember(repo) { KnightScopeViewModelFactory(repo = repo) }

    val homeVm: HomeViewModel = viewModel(factory = factory)
    val playVm: PlayViewModel = viewModel(factory = factory)
    val analyzeVm: AnalyzeViewModel = viewModel(factory = factory)
    val puzzlesVm: PuzzlesViewModel = viewModel(factory = factory)
    val friendsVm: FriendsViewModel = viewModel(factory = factory)
    val historyVm: HistoryViewModel = viewModel(factory = factory)
    val explorerVm: OpeningExplorerViewModel = viewModel(factory = factory)
    val trainerVm: OpeningTrainerViewModel = viewModel(factory = factory)
    val lessonsVm: ChessLessonsViewModel = viewModel(factory = factory)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)

    val homeState by homeVm.uiState.collectAsStateWithLifecycle()
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
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DiggyBrandBadge(size = 36)
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "ROYAL GOLD STUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentGreenHover,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    HorizontalDivider(color = LineSubtle, modifier = Modifier.padding(vertical = 8.dp))

                    val sections = listOf("CORE", "PLAY & STUDY", "PREFERENCES")
                    sections.forEach { sec ->
                        Text(
                            text = sec,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                        KnightScopeDestination.entries
                            .filter { it.sectionLabel == sec }
                            .forEach { dest ->
                                val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                                val label = stringResource(dest.labelRes)
                                NavigationDrawerItem(
                                    selected = selected,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = AccentGreen.copy(alpha = 0.16f),
                                        selectedIconColor = AccentGreenHover,
                                        selectedTextColor = TextPrimary,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    ),
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                        .testTag("drawer_${dest.route}")
                                )
                            }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(BgRadialTop, BgDark),
                        center = Offset(400f, 0f),
                        radius = 1200f
                    )
                )
        ) {
            val useRail = maxWidth >= 840.dp
            val primaryTabs = remember { KnightScopeDestination.entries.filter { it.primaryTab } }

            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    NavigationRail(
                        containerColor = SurfaceDark.copy(alpha = 0.94f),
                        contentColor = TextPrimary,
                        modifier = Modifier.border(width = 1.dp, color = LineSubtle)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(48.dp).testTag("rail_menu_button")
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open menu", tint = TextPrimary)
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
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = AccentGreen,
                                    selectedTextColor = TextPrimary,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = AccentGreen.copy(alpha = 0.16f)
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
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
                                .background(BgDark.copy(alpha = 0.88f))
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
                                            .size(48.dp)
                                            .testTag("drawer_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = "Open navigation menu",
                                            tint = AccentGreenHover
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
                                        Column {
                                            Text(
                                                text = stringResource(R.string.app_name),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "ROYAL STUDIO",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AccentGreenHover,
                                                fontSize = 9.sp,
                                                letterSpacing = 1.2.sp
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = { navigateTo(KnightScopeDestination.SETTINGS.route) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Settings,
                                            contentDescription = stringResource(R.string.tab_settings),
                                            tint = AccentGreenHover
                                        )
                                    }
                                }
                            )
                            HorizontalDivider(color = LineSubtle)
                        }
                    },
                    bottomBar = {
                        if (!useRail) {
                            DiggyFloatingBottomDock(
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
                        enterTransition = { fadeIn(animationSpec = tween(200)) },
                        exitTransition = { fadeOut(animationSpec = tween(200)) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable(KnightScopeDestination.HOME.route) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                HomeScreen(
                                    state = homeState,
                                    onNavigate = { route -> navigateTo(route) },
                                    onStartDailyPuzzle = {
                                        puzzlesVm.startDaily()
                                        navigateTo(KnightScopeDestination.PUZZLES.route)
                                    },
                                    modifier = Modifier.widthIn(max = 860.dp)
                                )
                            }
                        }

                        composable(KnightScopeDestination.PLAY.route) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                PlayScreen(
                                    state = playState,
                                    onSelectDifficulty = playVm::setDifficulty,
                                    onSelectColor = playVm::selectColorChoice,
                                    onToggleLiveEval = playVm::setShowLiveEval,
                                    onStartGame = { playVm.startGame() },
                                    onSquareClick = playVm::onSquareClick,
                                    onSelectPromotion = playVm::selectPromotion,
                                    onDismissPromotion = playVm::dismissPromotion,
                                    onHint = playVm::requestHint,
                                    onTakeback = playVm::takeback,
                                    onFlip = playVm::toggleFlip,
                                    onResign = playVm::resign,
                                    onRematch = playVm::rematch,
                                    onAnalyseGame = {
                                        val pgn = playVm.buildFinishedPgn()
                                        analyzeVm.loadAndAnalyzePgn(pgn)
                                        navigateTo(KnightScopeDestination.ANALYSE.route)
                                    },
                                    onLeaveGame = playVm::leaveGame,
                                    modifier = Modifier.widthIn(max = 760.dp)
                                )
                            }
                        }

                        composable(KnightScopeDestination.ANALYSE.route) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                AnalyzeScreen(
                                    state = analyzeState,
                                    onPgnChange = analyzeVm::updatePgn,
                                    onAnalyze = { analyzeVm.analyzeGame() },
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
                                    onSetPieceAnimations = settingsVm::setPieceAnimations,
                                    onSelectAnalysisPreset = settingsVm::setAnalysisPreset,
                                    onSelectDailyGoal = settingsVm::setDailyGoal,
                                    onRunSelfTest = settingsVm::runSelfTest,
                                    onGenerateBackup = settingsVm::generateBackupJson,
                                    onRestoreBackup = { json ->
                                        settingsVm.restoreBackupJson(json) { puzzlesVm.reloadPool() }
                                    },
                                    onBackupMessage = settingsVm::setBackupMessage,
                                    onSendContact = settingsVm::sendContactMessage,
                                    modifier = Modifier.widthIn(max = 780.dp)
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
private fun DiggyFloatingBottomDock(
    tabs: List<KnightScopeDestination>,
    currentDestination: androidx.navigation.NavDestination?,
    onSelectTab: (KnightScopeDestination) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = SurfaceDark.copy(alpha = 0.96f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.34f)),
            shadowElevation = 14.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    val label = stringResource(tab.labelRes)
                    val pillBg by animateColorAsState(
                        targetValue = if (selected) AccentGreen.copy(alpha = 0.18f) else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "dockPillBg"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (selected) AccentGreenHover else TextMuted,
                        animationSpec = tween(durationMillis = 200),
                        label = "dockIconTint"
                    )

                    Surface(
                        onClick = { onSelectTab(tab) },
                        color = pillBg,
                        shape = RoundedCornerShape(20.dp),
                        border = if (selected) BorderStroke(1.dp, AccentGreen.copy(alpha = 0.42f)) else null,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .minimumInteractiveComponentSize()
                            .testTag(tab.testTag)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = label,
                                tint = iconTint,
                                modifier = Modifier.size(21.dp)
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) TextPrimary else TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
        contentDescription = "Diggy Chess crest",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(9.dp))
            .border(1.dp, AccentGreen.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
    )
}
