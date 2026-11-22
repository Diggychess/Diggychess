package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chess.EngineDifficulty
import com.example.chess.MoveExplainer
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.data.RecentPlayGameSummary
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ColorChoice
import com.example.ui.viewmodel.PlayUiState
import com.example.ui.viewmodel.QuickPlayTimeControl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayScreen(
    state: PlayUiState,
    onSelectDifficulty: (EngineDifficulty) -> Unit,
    onSelectColor: (ColorChoice) -> Unit,
    onSelectTimeControl: (QuickPlayTimeControl) -> Unit,
    onStartQuickPlay: (QuickPlayTimeControl) -> Unit,
    onToggleLiveEval: (Boolean) -> Unit,
    onStartGame: () -> Unit,
    onSquareClick: (String) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onHint: () -> Unit,
    onTakeback: () -> Unit,
    onFlip: () -> Unit,
    onResign: () -> Unit,
    onRematch: () -> Unit,
    onAnalyseGame: () -> Unit,
    onAnalyseRecentPgn: (String) -> Unit,
    onLeaveGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.inGame) {
        BackHandler { onLeaveGame() }
    }

    if (!state.inGame) {
        PlayLobbyLayout(
            state = state,
            onSelectDifficulty = onSelectDifficulty,
            onSelectColor = onSelectColor,
            onSelectTimeControl = onSelectTimeControl,
            onStartQuickPlay = onStartQuickPlay,
            onToggleLiveEval = onToggleLiveEval,
            onStartGame = onStartGame,
            onAnalyseRecentPgn = onAnalyseRecentPgn,
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DiggySpacing.s2, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActiveBotMatchView(
                state = state,
                onSquareClick = onSquareClick,
                onHint = onHint,
                onTakeback = onTakeback,
                onFlip = onFlip,
                onResign = onResign,
                onRematch = onRematch,
                onAnalyseGame = onAnalyseGame,
                onLeaveGame = onLeaveGame
            )
        }
    }

    state.pendingPromotion?.let {
        PromotionPickerDialog(
            color = state.playerColor,
            onSelect = onSelectPromotion,
            onDismiss = onDismissPromotion
        )
    }
}

@Composable
private fun PlayLobbyLayout(
    state: PlayUiState,
    onSelectDifficulty: (EngineDifficulty) -> Unit,
    onSelectColor: (ColorChoice) -> Unit,
    onSelectTimeControl: (QuickPlayTimeControl) -> Unit,
    onStartQuickPlay: (QuickPlayTimeControl) -> Unit,
    onToggleLiveEval: (Boolean) -> Unit,
    onStartGame: () -> Unit,
    onAnalyseRecentPgn: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Scrollable Setup Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DiggySpacing.s2, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Play vs Computer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Pick engine strength, side, and clock mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            // Main Play vs Computer Card
            DiggyCard(elevated = false, contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Difficulty Level",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EngineDifficulty.entries.forEach { diff ->
                            DifficultyLevelCard(
                                difficulty = diff,
                                selected = state.difficulty == diff,
                                onClick = { onSelectDifficulty(diff) }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Play as",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                        DiggySegmentedControl(
                            options = ColorChoice.entries,
                            selectedOption = state.colorChoice,
                            onSelect = onSelectColor,
                            labelFor = { it.label },
                            tagFor = { "play_color_${it.code}" }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = DiggySpacing.touchTargetMin),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Evaluation",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Show real-time Stockfish advantage bar",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = state.showLiveEval,
                            onCheckedChange = onToggleLiveEval,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnAccentDark,
                                checkedTrackColor = AccentGreen
                            ),
                            modifier = Modifier.testTag("play_live_eval_switch")
                        )
                    }
                }
            }

            // Quick Play Modes (Bullet, Blitz, Rapid)
            DiggyCard(elevated = false, contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = StudioCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Quick Play Modes",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        DiggyChip(
                            label = "Untimed",
                            selected = state.timeControl == QuickPlayTimeControl.UNTIMED,
                            onClick = { onSelectTimeControl(QuickPlayTimeControl.UNTIMED) },
                            testTag = "play_tc_untimed"
                        )
                    }

                    QuickPlayRow(
                        title = "Bullet",
                        icon = Icons.Rounded.Bolt,
                        accent = ClassBlunder,
                        controls = listOf(QuickPlayTimeControl.BULLET_1, QuickPlayTimeControl.BULLET_2_1),
                        selected = state.timeControl,
                        onSelect = onSelectTimeControl,
                        onQuickLaunch = onStartQuickPlay
                    )
                    QuickPlayRow(
                        title = "Blitz",
                        icon = Icons.Rounded.LocalFireDepartment,
                        accent = AccentGreen,
                        controls = listOf(QuickPlayTimeControl.BLITZ_3, QuickPlayTimeControl.BLITZ_5),
                        selected = state.timeControl,
                        onSelect = onSelectTimeControl,
                        onQuickLaunch = onStartQuickPlay
                    )
                    QuickPlayRow(
                        title = "Rapid",
                        icon = Icons.Rounded.Speed,
                        accent = StudioCyan,
                        controls = listOf(QuickPlayTimeControl.RAPID_10, QuickPlayTimeControl.RAPID_15_10),
                        selected = state.timeControl,
                        onSelect = onSelectTimeControl,
                        onQuickLaunch = onStartQuickPlay
                    )
                }
            }

            // Recent Games Section
            DiggyCard(elevated = false, contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Recent Games",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    if (state.recentGames.isEmpty()) {
                        Text(
                            text = "Completed matches against Stockfish appear here for instant post-game review.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        state.recentGames.take(5).forEach { game ->
                            RecentPlayRow(
                                game = game,
                                onAnalyse = { onAnalyseRecentPgn(game.pgn) }
                            )
                        }
                    }
                }
            }
        }

        // Fixed Gold Start Game Dock
        Surface(
            color = SurfaceDark,
            border = BorderStroke(1.dp, LineSubtle),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiggySpacing.s2, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tcLabel = if (state.timeControl == QuickPlayTimeControl.UNTIMED) {
                    "Casual"
                } else {
                    "${state.timeControl.category} ${state.timeControl.label}"
                }
                DiggyButton(
                    text = "Start Game · ${state.difficulty.label} · $tcLabel",
                    onClick = onStartGame,
                    variant = DiggyButtonVariant.PRIMARY,
                    icon = Icons.Rounded.PlayArrow,
                    testTag = "play_start_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DifficultyLevelCard(
    difficulty: EngineDifficulty,
    selected: Boolean,
    onClick: () -> Unit
) {
    val botAccent = when (difficulty) {
        EngineDifficulty.EASY -> ClassBest
        EngineDifficulty.MEDIUM -> AccentGreen
        EngineDifficulty.HARD -> StudioCyan
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DiggySpacing.controlRadius),
        color = if (selected) AccentGreen.copy(alpha = 0.14f) else CardSurfaceElevated,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) AccentGreen else BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("play_difficulty_${difficulty.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(botAccent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SmartToy,
                    contentDescription = "${difficulty.label} robot",
                    tint = botAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = difficulty.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) AccentGreen else TextPrimary
                    )
                    Surface(
                        color = BgDark,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = difficulty.eloLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = difficulty.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = AccentGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickPlayRow(
    title: String,
    icon: ImageVector,
    accent: Color,
    controls: List<QuickPlayTimeControl>,
    selected: QuickPlayTimeControl,
    onSelect: (QuickPlayTimeControl) -> Unit,
    onQuickLaunch: (QuickPlayTimeControl) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.width(84.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        controls.forEach { tc ->
            val isSelected = tc == selected
            DiggyChip(
                label = tc.label,
                selected = isSelected,
                onClick = {
                    if (isSelected) onQuickLaunch(tc) else onSelect(tc)
                },
                accentColor = accent,
                testTag = "play_tc_${tc.id}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RecentPlayRow(
    game: RecentPlayGameSummary,
    onAnalyse: () -> Unit
) {
    val dateStr = remember(game.timestamp) {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(game.timestamp))
    }
    Surface(
        onClick = onAnalyse,
        color = CardSurfaceElevated,
        shape = RoundedCornerShape(DiggySpacing.controlRadius),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DiggySpacing.touchTargetMin)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${game.result} · ${game.timeControlLabel} · ${game.movesCount} moves · $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.Analytics,
                contentDescription = "Analyse match",
                tint = AccentGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ActiveBotMatchView(
    state: PlayUiState,
    onSquareClick: (String) -> Unit,
    onHint: () -> Unit,
    onTakeback: () -> Unit,
    onFlip: () -> Unit,
    onResign: () -> Unit,
    onRematch: () -> Unit,
    onAnalyseGame: () -> Unit,
    onLeaveGame: () -> Unit
) {
    val botColor = state.playerColor.opposite()
    val botName = "Stockfish (${state.difficulty.label})"
    val topColor = if (state.flipped) PieceColor.WHITE else PieceColor.BLACK
    val bottomColor = topColor.opposite()

    val topIsPlayer = (topColor == state.playerColor)
    val topName = if (topIsPlayer) state.playerName else botName
    val bottomName = if (topIsPlayer) botName else state.playerName
    val topClock = if (topColor == PieceColor.WHITE) state.whiteTimeMs else state.blackTimeMs
    val bottomClock = if (bottomColor == PieceColor.WHITE) state.whiteTimeMs else state.blackTimeMs

    val hintArrows = if (state.hintFrom != null && state.hintTo != null) {
        listOf(BoardArrow(state.hintFrom, state.hintTo, ArrowBestGreen))
    } else emptyList()

    var showGameOverModal by remember { mutableStateOf(false) }
    LaunchedEffect(state.isGameOver) {
        showGameOverModal = state.isGameOver
    }

    DiggyCard(elevated = false, contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayerBar(
                name = topName,
                pieceColor = topColor,
                isActiveTurn = !state.isGameOver && state.turn == topColor,
                fen = state.fen,
                rating = if (!topIsPlayer) state.difficulty.eloLabel else null,
                isBot = !topIsPlayer,
                statusNote = if (!topIsPlayer && state.isEngineThinking) "Thinking…" else null,
                clockMillis = topClock
            )

            // Board + 20dp EvalBar matched cleanly to board square height
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val barAndGap = if (state.showLiveEval) DiggySpacing.evalBarWidth + 8.dp else 0.dp
                val boardSize = (maxWidth - barAndGap).coerceAtLeast(200.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.showLiveEval) {
                        EvalBar(
                            evalPawns = state.liveEvalCp,
                            mate = state.liveEvalMate,
                            flipped = state.flipped,
                            modifier = Modifier
                                .height(boardSize)
                                .testTag("play_eval_bar")
                        )
                    }
                    ChessBoardView(
                        fen = state.fen,
                        boardTheme = state.boardTheme,
                        flipped = state.flipped,
                        selectedSquare = state.selectedSquare,
                        lastMoveSquares = state.lastMoveSquares,
                        legalTargets = state.legalTargets,
                        arrows = hintArrows,
                        onSquareClick = onSquareClick,
                        modifier = Modifier.size(boardSize)
                    )
                }
            }

            PlayerBar(
                name = bottomName,
                pieceColor = bottomColor,
                isActiveTurn = !state.isGameOver && state.turn == bottomColor,
                fen = state.fen,
                rating = if (topIsPlayer) state.difficulty.eloLabel else null,
                isBot = topIsPlayer,
                statusNote = when {
                    state.isGameOver -> state.endMessage
                    !topIsPlayer && state.turn == state.playerColor -> "Your turn"
                    else -> null
                },
                clockMillis = bottomClock
            )

            LiveMoveRibbon(sanMoves = state.sanMoves)

            AnimatedVisibility(visible = state.openingName != null) {
                Surface(
                    color = CardSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("play_opening_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MenuBook,
                            contentDescription = "Detected opening",
                            tint = AccentGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = listOfNotNull(state.openingEco, state.openingName).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            AnimatedVisibility(visible = state.hintText != null || state.isHintLoading) {
                Surface(
                    color = AccentGreen.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.isHintLoading) "Finding best move…" else state.hintText.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Control Bar (48dp touch targets)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MatchIconButton(
                    icon = Icons.Rounded.Lightbulb,
                    label = "Hint",
                    enabled = !state.isGameOver && !state.isEngineThinking && state.turn == state.playerColor,
                    tint = AccentGreen,
                    onClick = onHint,
                    testTag = "play_hint_button",
                    modifier = Modifier.weight(1f)
                )
                MatchIconButton(
                    icon = Icons.AutoMirrored.Rounded.Undo,
                    label = "Undo",
                    enabled = state.canTakeback && !state.isGameOver,
                    tint = TextPrimary,
                    onClick = onTakeback,
                    testTag = "play_takeback_button",
                    modifier = Modifier.weight(1f)
                )
                MatchIconButton(
                    icon = Icons.Rounded.Flip,
                    label = "Flip",
                    enabled = true,
                    tint = TextPrimary,
                    onClick = onFlip,
                    testTag = "play_flip_button",
                    modifier = Modifier.weight(1f)
                )
                MatchIconButton(
                    icon = Icons.Rounded.Flag,
                    label = "Resign",
                    enabled = !state.isGameOver,
                    tint = FeedbackDangerRed,
                    onClick = onResign,
                    testTag = "play_resign_button",
                    modifier = Modifier.weight(1f)
                )
            }

            // Post-Game Result Card
            AnimatedVisibility(
                visible = state.isGameOver,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut()
            ) {
                Surface(
                    color = CardSurfaceElevated,
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = state.endMessage,
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentGreen
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiggyButton(
                                text = "Analyse",
                                onClick = onAnalyseGame,
                                enabled = state.sanMoves.isNotEmpty(),
                                variant = DiggyButtonVariant.PRIMARY,
                                icon = Icons.Rounded.Analytics,
                                testTag = "play_analyse_game",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyButton(
                                text = "Rematch",
                                onClick = onRematch,
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.Refresh,
                                testTag = "play_rematch",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = onLeaveGame,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = DiggySpacing.touchTargetMin)
                    .testTag("play_new_setup_button")
            ) {
                Text("New match setup", color = TextMuted)
            }
        }
    }

    if (showGameOverModal && state.isGameOver) {
        GameOverSummaryDialog(
            state = state,
            onAnalyseGame = {
                showGameOverModal = false
                onAnalyseGame()
            },
            onRematch = {
                showGameOverModal = false
                onRematch()
            },
            onDismiss = { showGameOverModal = false }
        )
    }
}

@Composable
private fun GameOverSummaryDialog(
    state: PlayUiState,
    onAnalyseGame: () -> Unit,
    onRematch: () -> Unit,
    onDismiss: () -> Unit
) {
    val outcomeColor = when (state.isPlayerWin) {
        true -> ClassBest
        false -> FeedbackDangerRed
        null -> StudioCyan
    }
    val outcomeTitle = when (state.isPlayerWin) {
        true -> "Victory!"
        false -> "Defeat"
        null -> "Draw"
    }
    val outcomeIcon = when (state.isPlayerWin) {
        true -> Icons.Rounded.EmojiEvents
        false -> Icons.Rounded.Flag
        null -> Icons.Rounded.Balance
    }
    val totalMoves = (state.sanMoves.size + 1) / 2

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(DiggySpacing.cardRadius),
            color = CardDark,
            border = BorderStroke(1.dp, outcomeColor.copy(alpha = 0.55f)),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("play_gameover_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(outcomeColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = outcomeIcon,
                        contentDescription = outcomeTitle,
                        tint = outcomeColor,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = outcomeTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Surface(
                            color = CardSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = state.resultString,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = outcomeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = state.endMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }

                Surface(
                    color = CardSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Opponent", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Text(
                                text = "Stockfish (${state.difficulty.label} · ${state.difficulty.eloLabel})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Mode & Moves", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Text(
                                text = "${state.timeControl.label} · $totalMoves moves",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        if (!state.openingName.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Opening", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = listOfNotNull(state.openingEco, state.openingName).joinToString(" "),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                DiggyButton(
                    text = "Analyse Game",
                    onClick = onAnalyseGame,
                    enabled = state.sanMoves.isNotEmpty(),
                    variant = DiggyButtonVariant.PRIMARY,
                    icon = Icons.Rounded.Analytics,
                    testTag = "play_modal_analyse",
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiggyButton(
                        text = "Rematch",
                        onClick = onRematch,
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.Refresh,
                        testTag = "play_modal_rematch",
                        modifier = Modifier.weight(1f)
                    )
                    DiggyButton(
                        text = "View Board",
                        onClick = onDismiss,
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.Visibility,
                        testTag = "play_modal_dismiss",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchIconButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(DiggySpacing.controlRadius),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
        modifier = modifier
            .heightIn(min = DiggySpacing.touchTargetMin)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = if (enabled) tint else TextDisabled
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (enabled) TextPrimary else TextDisabled,
            maxLines = 1
        )
    }
}
