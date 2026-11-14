package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

private enum class KnightScopeTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    ANALYSE("analyse", R.string.tab_analyse, Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_analyse"),
    PUZZLES("puzzles", R.string.tab_puzzles, Icons.Filled.Extension, Icons.Outlined.Extension, "nav_puzzles"),
    FRIENDS("friends", R.string.tab_friends, Icons.Filled.Group, Icons.Outlined.Group, "nav_friends"),
    HISTORY("history", R.string.tab_history, Icons.Filled.History, Icons.Outlined.History, "nav_history")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnightScopeApp(repo: KnightScopeRepository) {
    val factory = remember(repo) { KnightScopeViewModelFactory(repo) }
    val analyzeVm: AnalyzeViewModel = viewModel(factory = factory)
    val puzzlesVm: PuzzlesViewModel = viewModel(factory = factory)
    val friendsVm: FriendsViewModel = viewModel(factory = factory)
    val historyVm: HistoryViewModel = viewModel(factory = factory)
    val aboutVm: AboutViewModel = viewModel(factory = factory)

    val analyzeState by analyzeVm.uiState.collectAsStateWithLifecycle()
    val puzzlesState by puzzlesVm.uiState.collectAsStateWithLifecycle()
    val friendsState by friendsVm.uiState.collectAsStateWithLifecycle()
    val historyState by historyVm.uiState.collectAsStateWithLifecycle()
    val aboutState by aboutVm.uiState.collectAsStateWithLifecycle()

    var showAbout by remember { mutableStateOf(false) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

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

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    containerColor = SurfaceDark.copy(alpha = 0.94f),
                    contentColor = TextPrimary,
                    modifier = Modifier.border(width = 1.dp, color = LineSubtle)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    KnightScopeTab.entries.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        val label = stringResource(tab.labelRes)
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
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
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = BgDark.copy(alpha = 0.90f),
                            titleContentColor = TextPrimary
                        ),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KnightScopeBrandBadge()
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showAbout = true },
                                modifier = Modifier.testTag("about_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = stringResource(R.string.about_title),
                                    tint = TextMuted
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = SurfaceDark.copy(alpha = 0.96f),
                            contentColor = TextPrimary,
                            tonalElevation = 0.dp
                        ) {
                            KnightScopeTab.entries.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                val label = stringResource(tab.labelRes)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
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
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = KnightScopeTab.ANALYSE.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(KnightScopeTab.ANALYSE.route) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            AnalyzeScreen(
                                state = analyzeState,
                                onPgnChange = analyzeVm::updatePgn,
                                onAnalyze = { analyzeVm.analyzeGame() },
                                onTrySample = analyzeVm::trySample,
                                onSharePgn = { analyzeVm.openShare(forceAnalysis = false) },
                                onShareAnalysis = { analyzeVm.openShare(forceAnalysis = true) },
                                onDismissShare = analyzeVm::closeShare,
                                onUpdateSettings = analyzeVm::updateSettings,
                                onOpenRecent = analyzeVm::openFromHistory,
                                onGoToPly = analyzeVm::goToPly,
                                onToggleBestMove = analyzeVm::toggleBestMove,
                                onToggleFlip = analyzeVm::toggleFlip,
                                onBoardThemeChange = analyzeVm::setBoardTheme,
                                modifier = Modifier.widthIn(max = 1040.dp)
                            )
                        }
                    }

                    composable(KnightScopeTab.PUZZLES.route) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
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
                                onNextPuzzle = puzzlesVm::requestNextPuzzle,
                                onConfirmSkip = puzzlesVm::confirmSkipPuzzle,
                                onDismissSkip = puzzlesVm::dismissSkipDialog,
                                onToggleFlip = puzzlesVm::toggleFlip,
                                onBackToMenu = puzzlesVm::backToMenu,
                                modifier = Modifier.widthIn(max = 920.dp)
                            )
                        }
                    }

                    composable(KnightScopeTab.FRIENDS.route) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            FriendsScreen(
                                state = friendsState,
                                onMyNameChange = friendsVm::updateMyName,
                                onOpponentNameChange = friendsVm::updateOpponentNameInput,
                                onSelectColor = friendsVm::selectColorChoice,
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
                                onAnalyseGame = {
                                    val pgn = friendsVm.buildFinishedPgn()
                                    analyzeVm.loadAndAnalyzePgn(pgn)
                                    navigateToTab(KnightScopeTab.ANALYSE.route)
                                },
                                onLeaveGame = friendsVm::leaveGame,
                                modifier = Modifier.widthIn(max = 720.dp)
                            )
                        }
                    }

                    composable(KnightScopeTab.HISTORY.route) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            HistoryScreen(
                                state = historyState,
                                onSelectSideFilter = historyVm::setSideFilter,
                                onOpenGame = { entry ->
                                    analyzeVm.openFromHistory(entry)
                                    navigateToTab(KnightScopeTab.ANALYSE.route)
                                },
                                onRequestDelete = historyVm::requestDelete,
                                onConfirmDelete = historyVm::confirmDelete,
                                onCancelDelete = historyVm::cancelDelete,
                                onGoToAnalyze = { navigateToTab(KnightScopeTab.ANALYSE.route) },
                                modifier = Modifier.widthIn(max = 820.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAbout) {
        AboutDialog(
            state = aboutState,
            onRunSelfTest = aboutVm::runSelfTest,
            onGenerateBackup = aboutVm::generateBackupJson,
            onRestoreBackup = { json ->
                aboutVm.restoreBackupJson(json)
                puzzlesVm.reloadPool()
            },
            onBackupMessage = aboutVm::setBackupMessage,
            onDismiss = { showAbout = false }
        )
    }
}

@Composable
private fun KnightScopeBrandBadge() {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AccentGreen),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val knight = Path().apply {
                // Stylised chess knight silhouette matching the KnightScope emblem
                moveTo(w * 0.24f, h * 0.86f)
                lineTo(w * 0.78f, h * 0.86f)
                lineTo(w * 0.74f, h * 0.74f)
                cubicTo(w * 0.73f, h * 0.44f, w * 0.68f, h * 0.24f, w * 0.50f, h * 0.16f)
                lineTo(w * 0.44f, h * 0.10f)
                lineTo(w * 0.40f, h * 0.19f)
                lineTo(w * 0.22f, h * 0.34f)
                lineTo(w * 0.25f, h * 0.44f)
                lineTo(w * 0.40f, h * 0.40f)
                cubicTo(w * 0.43f, h * 0.50f, w * 0.32f, h * 0.62f, w * 0.28f, h * 0.74f)
                close()
            }
            drawPath(knight, color = OnAccentDark)
        }
    }
}
