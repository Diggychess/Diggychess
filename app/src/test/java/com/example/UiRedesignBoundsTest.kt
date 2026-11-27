package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.chess.ChessBoard
import com.example.chess.ChessEngine
import com.example.chess.ChessPlatform
import com.example.chess.EngineDifficulty
import com.example.chess.GameAnalyzer
import com.example.chess.MoveClass
import com.example.chess.PhaseReport
import com.example.chess.PieceColor
import com.example.chess.Puzzle
import com.example.chess.ReviewSettings
import com.example.chess.SavedGameEntry
import com.example.chess.StockfishEngineStatus
import com.example.data.KnightScopeRepository
import com.example.ui.KnightScopeApp
import com.example.ui.screens.AnalyzeScreen
import com.example.ui.screens.ChessLessonsScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OpeningExplorerScreen
import com.example.ui.screens.OpeningTrainerScreen
import com.example.ui.screens.PlayScreen
import com.example.ui.screens.PuzzlesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BgDark
import com.example.ui.theme.KnightScopeTheme
import com.example.ui.theme.LocalKnightScopeColors
import com.example.ui.viewmodel.AnalyzeUiState
import com.example.ui.viewmodel.ChessLessonsUiState
import com.example.ui.viewmodel.ChessLessonsViewModel
import com.example.ui.viewmodel.FriendsUiState
import com.example.ui.viewmodel.HistoryReportSummary
import com.example.ui.viewmodel.HistoryUiState
import com.example.ui.viewmodel.OpeningExplorerUiState
import com.example.ui.viewmodel.OpeningTrainerUiState
import com.example.ui.viewmodel.PlayUiState
import com.example.ui.viewmodel.PuzzleMode
import com.example.ui.viewmodel.PuzzlesUiState
import com.example.ui.viewmodel.SettingsUiState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xhdpi")
class UiRedesignBoundsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun assertNodeWithinRootWidth(
        tag: String,
        minHeightDp: Dp = 0.dp,
        scrollIntoView: Boolean = true
    ): SemanticsNodeInteraction {
        val node = composeRule.onNodeWithTag(tag, useUnmergedTree = true)
        if (scrollIntoView) {
            runCatching { node.performScrollTo() }
            composeRule.waitForIdle()
        }
        val rootBounds = composeRule.onNodeWithTag("test_viewport_root", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Expected [$tag] to have positive width, got ${bounds.width}",
            bounds.width > 0f
        )
        assertTrue(
            "Expected [$tag] left (${bounds.left}) >= root left (${rootBounds.left})",
            bounds.left >= rootBounds.left - 2f
        )
        assertTrue(
            "Expected [$tag] right (${bounds.right}) <= root right (${rootBounds.right}) on 360dp viewport",
            bounds.right <= rootBounds.right + 2f
        )
        if (minHeightDp > 0.dp) {
            val minPx = with(composeRule.density) { minHeightDp.toPx() }
            assertTrue(
                "Expected [$tag] height (${bounds.height}px) >= ${minHeightDp.value}dp (${minPx}px)",
                bounds.height >= minPx - 2f
            )
        }
        node.assertIsDisplayed()
        return node
    }

    @Test
    fun homeScreen_360dp_rendersAllRedesignedElementsWithoutOverflowAndHandlesInputs() {
        var analysedPgn: String? = null
        var searchedPlatform: ChessPlatform? = null
        var searchedUser: String? = null

        composeRule.setContent {
            KnightScopeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("test_viewport_root")
                ) {
                    HomeScreen(
                        latestAnalysis = null,
                        onAnalysePgn = { analysedPgn = it },
                        onSearchUsername = { p, u ->
                            searchedPlatform = p
                            searchedUser = u
                        },
                        onUploadPgn = { _, _ -> },
                        onOpenPlatformImporter = {},
                        onTrySample = {},
                        onOpenLatestAnalysis = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        assertNodeWithinRootWidth("home_header")
        assertNodeWithinRootWidth("home_tagline_pill")
        assertNodeWithinRootWidth("home_hero_subtitle")
        assertNodeWithinRootWidth("home_preview_card")
        assertNodeWithinRootWidth("home_preview_badge").assertTextContains("Sample Preview")
        assertNodeWithinRootWidth("home_review_white_card")
        assertNodeWithinRootWidth("home_platform_toggle_row")
        assertNodeWithinRootWidth("home_username_input")
        assertNodeWithinRootWidth("home_username_search_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("home_pgn_input")
        assertNodeWithinRootWidth("home_analyse_game_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("home_upload_pgn_card", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("home_share_card", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("home_sample_card", minHeightDp = 48.dp)

        // Blank CTA shows inline validation
        composeRule.onNodeWithTag("home_analyse_game_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Enter a username or paste a PGN to analyse.").assertIsDisplayed()

        // Username-only CTA + Lichess toggle triggers username search
        composeRule.onNodeWithTag("home_platform_switch").performScrollTo().performClick()
        composeRule.onNodeWithTag("home_username_input").performScrollTo().performTextInput("MagnusCarlsen")
        composeRule.onNodeWithTag("home_analyse_game_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(ChessPlatform.LICHESS, searchedPlatform)
        assertEquals("MagnusCarlsen", searchedUser)

        // PGN pasted takes priority on Analyse Game
        composeRule.onNodeWithTag("home_pgn_input").performScrollTo().performTextInput("1. e4 e5 2. Nf3 Nc6 *")
        composeRule.onNodeWithTag("home_analyse_game_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals("1. e4 e5 2. Nf3 Nc6 *", analysedPgn)
    }

    @Test
    fun homeScreen_withLatestAnalysis_switchesPreviewBadgeToLatestAnalysis() {
        val analysis = runBlocking {
            GameAnalyzer(ChessEngine()).analyze(
                pgnText = "1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0",
                settings = ReviewSettings(depth = 4, forceReanalyze = true)
            )
        }

        composeRule.setContent {
            KnightScopeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    HomeScreen(
                        latestAnalysis = analysis,
                        onAnalysePgn = {},
                        onSearchUsername = { _, _ -> },
                        onUploadPgn = { _, _ -> },
                        onOpenPlatformImporter = {},
                        onTrySample = {},
                        onOpenLatestAnalysis = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("home_preview_badge").assertTextContains("Latest Analysis")
    }

    @Test
    fun analyzeScreen_360dp_emptyAndAnalyzedWorkspaceFitWithoutOverflow() {
        val analysis = runBlocking {
            GameAnalyzer(ChessEngine()).analyze(
                pgnText = "1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0",
                settings = ReviewSettings(depth = 4, forceReanalyze = true)
            )
        }
        var currentState by mutableStateOf(
            AnalyzeUiState(
                engineStatus = StockfishEngineStatus.Ready(),
                pgnText = "1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0",
                result = null
            )
        )

        composeRule.setContent {
            KnightScopeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    AnalyzeScreen(
                        state = currentState,
                        onPgnChange = { currentState = currentState.copy(pgnText = it) },
                        onAnalyze = {},
                        onSelectDepthPreset = {},
                        onRetryEngine = {},
                        onTrySample = {},
                        onSharePgn = {},
                        onShareAnalysis = {},
                        onDismissShare = {},
                        onOpenRecent = {},
                        onGoToPly = { _, _ -> },
                        onToggleBestMove = {},
                        onToggleFlip = {},
                        onBoardThemeChange = {},
                        onOpenImporter = {},
                        onCloseImporter = {},
                        onSelectImportPlatform = {},
                        onImportUsernameChange = {},
                        onFetchPlatformGames = {},
                        onSpeedFilter = {},
                        onResultFilter = {},
                        onColorFilter = {},
                        onAnalyzeImportedGame = {},
                        onBoardSquareClick = {},
                        onSelectVariationPromotion = {},
                        onDismissVariationPromotion = {},
                        onPlayEngineLineMove = { _, _ -> },
                        onStepVariation = {},
                        onUndoVariation = {},
                        onExitVariation = {},
                        onPracticeMistakes = {},
                        onDrillSquareClick = {},
                        onDrillPromotion = {},
                        onDismissDrillPromotion = {},
                        onDrillHint = {},
                        onDrillRetry = {},
                        onDrillNext = {},
                        onCloseDrill = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        assertNodeWithinRootWidth("open_importer_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("pgn_input")
        assertNodeWithinRootWidth("engine_status_ready")
        assertNodeWithinRootWidth("analyze_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("upload_pgn_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("share_pgn_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("try_sample_button", minHeightDp = 48.dp)

        // Switch to analyzed workspace state
        composeRule.runOnUiThread {
            currentState = currentState.copy(
                result = analysis,
                currentPly = analysis.plies.size
            )
        }
        composeRule.waitForIdle()

        assertNodeWithinRootWidth("eval_graph")
        assertNodeWithinRootWidth("ai_game_report_card")
        assertNodeWithinRootWidth("ask_ai_coach_chat_card")
    }

    @Test
    fun playScreen_360dp_lobbyAndActiveBotMatchFitWithoutOverflow() {
        var inGame by mutableStateOf(false)

        composeRule.setContent {
            KnightScopeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    PlayScreen(
                        state = if (!inGame) {
                            PlayUiState(
                                inGame = false,
                                difficulty = EngineDifficulty.MEDIUM,
                                engineStatus = StockfishEngineStatus.Ready()
                            )
                        } else {
                            PlayUiState(
                                inGame = true,
                                difficulty = EngineDifficulty.MEDIUM,
                                engineStatus = StockfishEngineStatus.Ready(),
                                sanMoves = listOf("e4", "e5", "Nf3", "Nc6"),
                                lastMoveSan = "Nc6",
                                openingEco = "C50",
                                openingName = "Italian Game"
                            )
                        },
                        onSelectDifficulty = {},
                        onSelectColor = {},
                        onStartGame = { inGame = true },
                        onSquareClick = {},
                        onSelectPromotion = {},
                        onDismissPromotion = {},
                        onHint = {},
                        onTakeback = {},
                        onFlip = {},
                        onResign = {},
                        onRematch = {},
                        onAnalyseGame = {},
                        onAnalyseRecentPgn = {},
                        onLeaveGame = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        assertNodeWithinRootWidth("play_difficulty_easy")
        assertNodeWithinRootWidth("play_difficulty_expert")
        assertNodeWithinRootWidth("play_base_time_unlimited", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("play_start_button", minHeightDp = 48.dp)

        composeRule.runOnUiThread { inGame = true }
        composeRule.waitForIdle()

        assertNodeWithinRootWidth("play_opening_badge")
        assertNodeWithinRootWidth("play_resign_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("play_offer_draw_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("play_new_game_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("play_flip_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("play_hint_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("play_takeback_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("play_draw_mode_button", minHeightDp = 48.dp)
    }

    @Test
    fun puzzlesScreen_360dp_menuAndPlayViewFitWithoutOverflow() {
        val samplePuzzle = Puzzle(
            id = "pz_test_1",
            fen0 = ChessBoard.START_FEN,
            setupUci = null,
            fen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
            solution = listOf("h5f7"),
            rating = 1450,
            themes = listOf("mateIn1"),
            isMate = true,
            source = "test"
        )
        var inPlay by mutableStateOf(false)

        composeRule.setContent {
            KnightScopeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    PuzzlesScreen(
                        state = PuzzlesUiState(
                            isLoadingPool = false,
                            totalPoolSize = 10_000,
                            inPlay = inPlay,
                            mode = PuzzleMode.DAILY,
                            queue = listOf(samplePuzzle),
                            currentPuzzle = samplePuzzle,
                            currentFen = samplePuzzle.fen,
                            currentPuzzleStreak = 3,
                            bestPuzzleStreak = 7
                        ),
                        levelSolvedCount = { 0 },
                        isLevelUnlocked = { it == 0 },
                        levelPuzzleCount = { 25 },
                        onStartRated = {},
                        onSetStartingRating = {},
                        onStartLevel = {},
                        onToggleCategory = {},
                        onSelectTheme = {},
                        onStartPractice = {},
                        onStartQuickDifficulty = {},
                        onStartDaily = { inPlay = true },
                        onStartRandom = {},
                        onStartTimed = {},
                        onStartReviewMistakes = {},
                        onStartMyGameMistakes = {},
                        onSetDailyGoal = {},
                        onToggleDifficultyFilter = {},
                        onSetDifficultyFilter = {},
                        onImportCsv = {},
                        onSquareClick = {},
                        onSelectPromotion = {},
                        onDismissPromotion = {},
                        onRestartPuzzle = {},
                        onUndoPuzzle = {},
                        onHintPuzzle = {},
                        onRevealHintDestination = {},
                        onDismissHintModal = {},
                        onNextPuzzle = {},
                        onConfirmSkip = {},
                        onDismissSkip = {},
                        onToggleFlip = {},
                        onBackToMenu = { inPlay = false }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        assertNodeWithinRootWidth("daily_challenge_card")
        assertNodeWithinRootWidth("daily_goal_chip_3")
        assertNodeWithinRootWidth("daily_goal_chip_20")
        assertNodeWithinRootWidth("start_daily_challenge_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("start_rated_puzzles", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("puzzle_quick_easy", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("puzzle_quick_hard", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("daily_puzzle_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("random_puzzle_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("timed_puzzle_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("review_mistakes_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("my_game_mistakes_button", minHeightDp = 48.dp)

        composeRule.runOnUiThread { inPlay = true }
        composeRule.waitForIdle()

        assertNodeWithinRootWidth("puzzle_back_button")
        assertNodeWithinRootWidth("puzzle_streak_badge")
        assertNodeWithinRootWidth("puzzle_best_streak_badge")
        assertNodeWithinRootWidth("puzzle_restart")
        assertNodeWithinRootWidth("puzzle_undo")
        assertNodeWithinRootWidth("puzzle_hint")
        assertNodeWithinRootWidth("puzzle_next")
    }

    @Test
    fun historyScreen_360dp_fitsWithoutOverflow() {
        val sampleEntry = SavedGameEntry(
            id = "g1",
            title = "Player vs Stockfish (Italian Game)",
            timestamp = 1_700_000_000_000L,
            plies = 32,
            moves = 16,
            pgn = "1. e4 e5 2. Nf3 Nc6 *",
            evals = listOf(0.2, 0.3, 0.1),
            bestMoves = listOf("e2e4", "e7e5", "g1f3"),
            report = PhaseReport(
                whiteCounts = List(MoveClass.entries.size) { 1 },
                blackCounts = List(MoveClass.entries.size) { 1 },
                whitePhaseMistakes = listOf(0, 1, 0),
                blackPhaseMistakes = listOf(0, 0, 1)
            ),
            mistakes = emptyList(),
            result = "1-0",
            opponent = "Stockfish 19",
            whiteAccuracy = 88.4,
            blackAccuracy = 79.2
        )

        composeRule.setContent {
            KnightScopeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    HistoryScreen(
                        state = HistoryUiState(
                            games = listOf(sampleEntry),
                            report = HistoryReportSummary(
                                blundersPerGame = 0.5,
                                mistakesPerGame = 1.0,
                                inaccuraciesPerGame = 1.5,
                                worstPhaseIndex = 1,
                                hasAnyMistake = true,
                                gameCount = 1
                            )
                        ),
                        onSelectSideFilter = {},
                        onRefresh = {},
                        onOpenGame = {},
                        onRequestDelete = {},
                        onConfirmDelete = {},
                        onCancelDelete = {},
                        onGoToAnalyze = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("history_analyze_new_button", minHeightDp = 48.dp, scrollIntoView = false)
        assertNodeWithinRootWidth("open_history_g1", minHeightDp = 48.dp, scrollIntoView = false)
        assertNodeWithinRootWidth("delete_history_g1", minHeightDp = 48.dp, scrollIntoView = false)
    }

    @Test
    fun friendsScreen_360dp_fitsWithoutOverflow() {
        composeRule.setContent {
            KnightScopeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    FriendsScreen(
                        state = FriendsUiState(),
                        onMyNameChange = {},
                        onOpponentNameChange = {},
                        onSelectColor = {},
                        onSelectTimeControl = { _, _ -> },
                        onCreateOnlineInvite = {},
                        onClearOnlineInvite = {},
                        onJoinAddressChange = {},
                        onStartPassAndPlay = {},
                        onHostWifiMatch = {},
                        onJoinWifiMatch = {},
                        onSquareClick = {},
                        onSelectPromotion = {},
                        onDismissPromotion = {},
                        onToggleFlip = {},
                        onOfferDraw = {},
                        onAcceptDraw = {},
                        onDeclineDraw = {},
                        onResign = {},
                        onRematch = {},
                        onBuildPgn = { "" },
                        onAnalyseGame = {},
                        onLeaveGame = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("invite_tc_300", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("create_online_invite", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("friend_name_input")
        assertNodeWithinRootWidth("start_pass_and_play", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("host_wifi_match", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("join_address_input")
        assertNodeWithinRootWidth("join_wifi_match", minHeightDp = 48.dp)
    }

    @Test
    fun openingExplorerScreen_360dp_fitsWithoutOverflow() {
        composeRule.setContent {
            KnightScopeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    OpeningExplorerScreen(
                        state = OpeningExplorerUiState(
                            sanHistory = listOf("e4", "e5"),
                            canUndo = true
                        ),
                        onSquareClick = {},
                        onSelectPromotion = {},
                        onDismissPromotion = {},
                        onPlayMove = {},
                        onUndo = {},
                        onReset = {},
                        onToggleFlip = {},
                        onDatabaseChange = {},
                        onAnalysePosition = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("explorer_undo", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("explorer_reset", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("explorer_flip", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("explorer_analyse", minHeightDp = 48.dp)
    }

    @Test
    fun openingTrainerScreen_360dp_fitsWithoutOverflow() {
        composeRule.setContent {
            KnightScopeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    OpeningTrainerScreen(
                        state = OpeningTrainerUiState(
                            coachNote = "Develop pieces toward the center early."
                        ),
                        onSelectRepertoire = {},
                        onSquareClick = {},
                        onHint = {},
                        onRestart = {},
                        onNextRepertoire = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("trainer_hint", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("trainer_restart", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("trainer_next", minHeightDp = 48.dp)
    }

    @Test
    fun chessLessonsScreen_360dp_fitsWithoutOverflow() {
        composeRule.setContent {
            KnightScopeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    ChessLessonsScreen(
                        state = ChessLessonsUiState(
                            activeLessonId = ChessLessonsViewModel.CURRICULUM.first().id,
                            feedbackText = "Find the strongest tactical move.",
                            feedbackOk = true
                        ),
                        onOpenLesson = {},
                        onCloseLesson = {},
                        onSquareClick = {},
                        onSelectPromotion = {},
                        onDismissPromotion = {},
                        onHint = {},
                        onRetry = {},
                        onNextStep = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("lesson_hint", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("lesson_retry", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("lesson_next_step", minHeightDp = 48.dp)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w390dp-h844dp-xxhdpi")
    fun homeScreen_390dp_rendersCleanlyWithinViewportBounds() {
        composeRule.setContent {
            KnightScopeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    HomeScreen(
                        latestAnalysis = null,
                        onAnalysePgn = {},
                        onSearchUsername = { _, _ -> },
                        onUploadPgn = { _, _ -> },
                        onOpenPlatformImporter = {},
                        onTrySample = {},
                        onOpenLatestAnalysis = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("home_header")
        assertNodeWithinRootWidth("home_preview_card")
        assertNodeWithinRootWidth("home_review_white_card")
        assertNodeWithinRootWidth("home_analyse_game_button", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("home_upload_pgn_card", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("home_share_card", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("home_sample_card", minHeightDp = 48.dp)
    }

    @Test
    fun settingsScreen_defaultsToLightClassicAndTogglesDarkThemeCleanly() {
        var darkMode by mutableStateOf(false)
        var capturedIsDark = true
        var capturedBgArgb = 0

        composeRule.setContent {
            KnightScopeTheme(darkTheme = darkMode) {
                capturedIsDark = LocalKnightScopeColors.current.isDark
                capturedBgArgb = BgDark.toArgb()
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    SettingsScreen(
                        state = SettingsUiState(isDarkMode = darkMode),
                        onSetDarkMode = { darkMode = it },
                        onSelectBoardTheme = {},
                        onSelectPieceStyle = {},
                        onSetBoard3dEnabled = {},
                        onSetPieceAnimations = {},
                        onSetSoundEnabled = {},
                        onSetHapticsEnabled = {},
                        onSelectAnalysisPreset = {},
                        onSelectDailyGoal = {},
                        onRunSelfTest = {},
                        onGenerateBackup = { "{}" },
                        onRestoreBackup = {},
                        onBackupMessage = {},
                        onSendContact = { _, _, _, _, _ -> }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        assertFalse("Default palette should be LightClassicPalette", capturedIsDark)
        assertEquals(0xFFF4F5F7.toInt(), capturedBgArgb)
        assertNodeWithinRootWidth("settings_dark_mode_switch")
        assertNodeWithinRootWidth("settings_theme_selector")
        assertNodeWithinRootWidth("settings_groq_key_input")
        assertNodeWithinRootWidth("settings_tinyfish_key_input")
        assertNodeWithinRootWidth("settings_backup_export", minHeightDp = 48.dp)
        assertNodeWithinRootWidth("settings_self_test_button", minHeightDp = 48.dp)

        // Toggle Dark Theme switch ON and ensure palette switches without error
        composeRule.onNodeWithTag("settings_dark_mode_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue("Dark Theme toggle should switch palette to DarkStudioPalette", darkMode)
        assertTrue(capturedIsDark)
        assertEquals(0xFF111114.toInt(), capturedBgArgb)
    }

    @Test
    fun knightScopeApp_fullShellNavigation_360dp_showsGlobalHeaderOnHomeAndAllTabs() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = KnightScopeRepository(context)

        composeRule.setContent {
            KnightScopeTheme {
                Box(modifier = Modifier.fillMaxSize().testTag("test_viewport_root")) {
                    KnightScopeApp(repo = repo)
                }
            }
        }
        composeRule.waitForIdle()

        // Dismiss splash overlay cleanly if still visible
        runCatching {
            composeRule.onNodeWithTag("diggy_splash_overlay").performClick()
        }
        composeRule.mainClock.advanceTimeBy(1_100L)
        composeRule.waitForIdle()

        // Global header & Home elements visible on Home
        composeRule.onNodeWithTag("drawer_menu_button").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_button").assertIsDisplayed()
        assertNodeWithinRootWidth("home_header", scrollIntoView = false)
        assertNodeWithinRootWidth("nav_home", scrollIntoView = false)
        assertNodeWithinRootWidth("nav_play", scrollIntoView = false)
        assertNodeWithinRootWidth("nav_analyse", scrollIntoView = false)
        assertNodeWithinRootWidth("nav_puzzles", scrollIntoView = false)
        assertNodeWithinRootWidth("nav_history", scrollIntoView = false)

        // Navigate across all 5 bottom bar tabs
        composeRule.onNodeWithTag("nav_play").performClick()
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("play_start_button", minHeightDp = 48.dp)

        composeRule.onNodeWithTag("nav_analyse").performClick()
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("analyze_button", minHeightDp = 48.dp)

        composeRule.onNodeWithTag("nav_puzzles").performClick()
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("daily_challenge_card")

        composeRule.onNodeWithTag("nav_history").performClick()
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("history_analyze_new_button", minHeightDp = 48.dp, scrollIntoView = false)

        // Open Settings via top bar settings icon
        composeRule.onNodeWithTag("settings_button").performClick()
        composeRule.waitForIdle()
        assertNodeWithinRootWidth("settings_dark_mode_switch")

        // Ensure zero "Diggy" branding remains anywhere in visible UI
        assertEquals(
            0,
            composeRule.onAllNodesWithText("Diggy", substring = true, ignoreCase = true)
                .fetchSemanticsNodes().size
        )
    }
}
