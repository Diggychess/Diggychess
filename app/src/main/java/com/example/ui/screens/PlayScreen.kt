package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.chess.EngineDifficulty
import com.example.chess.MoveExplainer
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.StockfishEngineStatus
import com.example.data.RecentPlayGameSummary
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ColorChoice
import com.example.ui.viewmodel.PlayUiState
import com.example.ui.viewmodel.QuickPlayTimeControl
import com.example.ui.viewmodel.TimeControlBase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlayScreen(
    state: PlayUiState,
    onSelectDifficulty: (EngineDifficulty) -> Unit,
    onSelectColor: (ColorChoice) -> Unit,
    onSelectBaseTime: (TimeControlBase) -> Unit = {},
    onSelectIncrement: (Int) -> Unit = {},
    onSelectTimeControl: (QuickPlayTimeControl) -> Unit = {},
    onStartQuickPlay: (QuickPlayTimeControl) -> Unit = {},
    onStartGame: () -> Unit,
    onSquareClick: (String) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onHint: () -> Unit,
    onTakeback: () -> Unit,
    onFlip: () -> Unit,
    onOfferDraw: () -> Unit = {},
    onResign: () -> Unit,
    onRematch: () -> Unit,
    onAnalyseGame: () -> Unit,
    onAnalyseRecentPgn: (String) -> Unit,
    onLeaveGame: () -> Unit,
    onRetryEngine: () -> Unit = {},
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
            onSelectBaseTime = onSelectBaseTime,
            onSelectIncrement = onSelectIncrement,
            onStartGame = onStartGame,
            onAnalyseRecentPgn = onAnalyseRecentPgn,
            onRetryEngine = onRetryEngine,
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
            if (state.engineStatus is StockfishEngineStatus.Error) {
                EngineWorkerErrorBanner(
                    message = state.engineStatus.message,
                    onRetry = onRetryEngine
                )
            }
            ActiveBotMatchView(
                state = state,
                onSquareClick = onSquareClick,
                onHint = onHint,
                onTakeback = onTakeback,
                onFlip = onFlip,
                onOfferDraw = onOfferDraw,
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
    onSelectBaseTime: (TimeControlBase) -> Unit,
    onSelectIncrement: (Int) -> Unit,
    onStartGame: () -> Unit,
    onAnalyseRecentPgn: (String) -> Unit,
    onRetryEngine: () -> Unit,
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
            if (state.engineStatus is StockfishEngineStatus.Error) {
                EngineWorkerErrorBanner(
                    message = state.engineStatus.message,
                    onRetry = onRetryEngine
                )
            }
            // Chess.com-style Featured Arena & Selected Bot Banner
            val selectedAccent = when (state.difficulty) {
                EngineDifficulty.EASY -> ClassBest
                EngineDifficulty.MEDIUM -> AccentGreen
                EngineDifficulty.HARD -> StudioCyan
                EngineDifficulty.EXPERT -> TrophyGold
            }
            Surface(
                color = MatteDarkCard,
                shape = RoundedCornerShape(DiggySpacing.cardRadius),
                border = BorderStroke(1.dp, BorderSubtle),
                shadowElevation = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_play_arena),
                        contentDescription = "Play Stockfish arena",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xEB1D1B18),
                                        Color(0xC9211F1C),
                                        Color(0x77211F1C)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(selectedAccent.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SmartToy,
                                contentDescription = null,
                                tint = selectedAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Play Bots",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Surface(
                                    color = selectedAccent.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(999.dp),
                                    border = BorderStroke(1.dp, selectedAccent.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "${state.difficulty.label} · ${state.difficulty.eloLabel}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = selectedAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = state.difficulty.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD0CDC7),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 2x2 Chess.com Bot Picker Grid + Play As Color Card
            DiggyCard(elevated = false, contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose Opponent",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Stockfish 19",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGreen
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EngineDifficulty.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { diff ->
                                    DifficultyLevelCard(
                                        difficulty = diff,
                                        selected = state.difficulty == diff,
                                        onClick = { onSelectDifficulty(diff) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
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
                }
            }

            // Time Control Card (1 min, 3 min, 5 min, 10 min, Unlimited + 0s/1s/2s increment)
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
                                tint = AccentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Time Control",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Surface(
                            color = CardSurfaceElevated,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = "${state.selectedBaseTime.category} · ${state.timeControlLabel}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = AccentGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            TimeControlBase.MIN_1,
                            TimeControlBase.MIN_3,
                            TimeControlBase.MIN_5,
                            TimeControlBase.MIN_10
                        ).forEach { base ->
                            DiggyChip(
                                label = base.label,
                                selected = state.selectedBaseTime == base,
                                onClick = { onSelectBaseTime(base) },
                                testTag = "play_base_time_${base.id}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DiggyChip(
                            label = TimeControlBase.UNLIMITED.label,
                            selected = state.selectedBaseTime == TimeControlBase.UNLIMITED,
                            onClick = { onSelectBaseTime(TimeControlBase.UNLIMITED) },
                            accentColor = StudioCyan,
                            testTag = "play_base_time_unlimited",
                            modifier = Modifier.weight(1.3f)
                        )
                        if (state.selectedBaseTime != TimeControlBase.UNLIMITED) {
                            listOf(0, 1, 2).forEach { inc ->
                                DiggyChip(
                                    label = if (inc == 0) "0s inc" else "+${inc}s",
                                    selected = state.selectedIncrementSec == inc,
                                    onClick = { onSelectIncrement(inc) },
                                    testTag = "play_increment_$inc",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
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

        // Fixed Emerald Start Game Dock
        Surface(
            color = SurfaceDark,
            border = BorderStroke(1.dp, LineSubtle),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DiggySpacing.s2, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DiggyButton(
                    text = "Start Game · ${state.difficulty.label} · ${state.timeControlLabel}",
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val botAccent = when (difficulty) {
        EngineDifficulty.EASY -> ClassBest
        EngineDifficulty.MEDIUM -> AccentGreen
        EngineDifficulty.HARD -> StudioCyan
        EngineDifficulty.EXPERT -> TrophyGold
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) botAccent.copy(alpha = 0.16f) else CardSurfaceElevated,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) botAccent else BorderSubtle),
        modifier = modifier
            .heightIn(min = 82.dp)
            .testTag("play_difficulty_${difficulty.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(botAccent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SmartToy,
                        contentDescription = "${difficulty.label} robot",
                        tint = botAccent,
                        modifier = Modifier.size(19.dp)
                    )
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = botAccent,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = difficulty.eloLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontFeatureSettings = "tnum"
                            ),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = difficulty.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) botAccent else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = difficulty.eloLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontFeatureSettings = "tnum"
                    ),
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }
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
    onOfferDraw: () -> Unit,
    onResign: () -> Unit,
    onRematch: () -> Unit,
    onAnalyseGame: () -> Unit,
    onLeaveGame: () -> Unit
) {
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
    var annotationDrawMode by remember { mutableStateOf(false) }
    LaunchedEffect(state.isGameOver) {
        showGameOverModal = state.isGameOver
    }

    val topPlayerCard: @Composable () -> Unit = {
        PlayerBar(
            name = topName,
            pieceColor = topColor,
            isActiveTurn = !state.isGameOver && state.turn == topColor,
            fen = state.fen,
            rating = if (!topIsPlayer) state.difficulty.eloLabel else null,
            isBot = !topIsPlayer,
            avatarUrl = if (topIsPlayer) state.playerAvatarUrl else null,
            statusNote = if (!topIsPlayer && state.isEngineThinking) "thinking…" else null,
            clockMillis = topClock
        )
    }

    val bottomPlayerCard: @Composable () -> Unit = {
        PlayerBar(
            name = bottomName,
            pieceColor = bottomColor,
            isActiveTurn = !state.isGameOver && state.turn == bottomColor,
            fen = state.fen,
            rating = if (topIsPlayer) state.difficulty.eloLabel else null,
            isBot = topIsPlayer,
            avatarUrl = if (!topIsPlayer) state.playerAvatarUrl else null,
            statusNote = when {
                state.isGameOver -> state.endMessage
                topIsPlayer && state.isEngineThinking -> "thinking…"
                !topIsPlayer && state.turn == state.playerColor -> "Your turn"
                else -> null
            },
            clockMillis = bottomClock
        )
    }

    val sideOrBottomControls: @Composable (Boolean) -> Unit = { isWidePanel ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AnimatedVisibility(visible = state.openingName != null) {
                Surface(
                    color = CardSurfaceElevated,
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
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

            LiveSanMoveList(
                sanMoves = state.sanMoves,
                maxHeight = if (isWidePanel) 320.dp else 180.dp
            )

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

            // Primary Game Controls: Resign, Offer Draw, New Game, Flip Board (2x2 on narrow phones)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 340.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MatchIconButton(
                                icon = Icons.Rounded.Flag,
                                label = "Resign",
                                enabled = !state.isGameOver,
                                tint = FeedbackDangerRed,
                                onClick = onResign,
                                testTag = "play_resign_button",
                                modifier = Modifier.weight(1f)
                            )
                            MatchIconButton(
                                icon = Icons.Rounded.Handshake,
                                label = "Draw",
                                enabled = !state.isGameOver && !state.isEngineThinking,
                                tint = TextPrimary,
                                onClick = onOfferDraw,
                                testTag = "play_offer_draw_button",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MatchIconButton(
                                icon = Icons.Rounded.AddCircleOutline,
                                label = "New",
                                enabled = true,
                                tint = AccentGreen,
                                onClick = onLeaveGame,
                                testTag = "play_new_game_button",
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
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MatchIconButton(
                            icon = Icons.Rounded.Flag,
                            label = "Resign",
                            enabled = !state.isGameOver,
                            tint = FeedbackDangerRed,
                            onClick = onResign,
                            testTag = "play_resign_button",
                            modifier = Modifier.weight(1f)
                        )
                        MatchIconButton(
                            icon = Icons.Rounded.Handshake,
                            label = "Draw",
                            enabled = !state.isGameOver && !state.isEngineThinking,
                            tint = TextPrimary,
                            onClick = onOfferDraw,
                            testTag = "play_offer_draw_button",
                            modifier = Modifier.weight(1f)
                        )
                        MatchIconButton(
                            icon = Icons.Rounded.AddCircleOutline,
                            label = "New",
                            enabled = true,
                            tint = AccentGreen,
                            onClick = onLeaveGame,
                            testTag = "play_new_game_button",
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
                    }
                }
            }

            // Helpful Training Row: Hint, Undo, Draw Arrows/Marks toggle
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
                    icon = Icons.Rounded.Edit,
                    label = if (annotationDrawMode) "Marking" else "Mark",
                    enabled = true,
                    tint = if (annotationDrawMode) AccentGreen else TextMuted,
                    onClick = { annotationDrawMode = !annotationDrawMode },
                    testTag = "play_draw_mode_button",
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
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 768.dp
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                DiggyCard(
                    elevated = false,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        topPlayerCard()
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val boardSize = maxWidth.coerceIn(220.dp, 540.dp)
                            ChessBoardView(
                                fen = state.fen,
                                boardTheme = state.boardTheme,
                                flipped = state.flipped,
                                selectedSquare = state.selectedSquare,
                                lastMoveSquares = state.lastMoveSquares,
                                legalTargets = state.legalTargets,
                                arrows = hintArrows,
                                annotationDrawMode = annotationDrawMode,
                                onSquareClick = onSquareClick,
                                modifier = Modifier
                                    .size(boardSize)
                                    .align(Alignment.Center)
                            )
                        }
                        bottomPlayerCard()
                    }
                }

                DiggyCard(
                    elevated = false,
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.widthIn(min = 280.dp, max = 340.dp)
                ) {
                    sideOrBottomControls(true)
                }
            }
        } else {
            DiggyCard(elevated = false, contentPadding = PaddingValues(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topPlayerCard()
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val boardSize = maxWidth.coerceAtLeast(200.dp)
                        ChessBoardView(
                            fen = state.fen,
                            boardTheme = state.boardTheme,
                            flipped = state.flipped,
                            selectedSquare = state.selectedSquare,
                            lastMoveSquares = state.lastMoveSquares,
                            legalTargets = state.legalTargets,
                            arrows = hintArrows,
                            annotationDrawMode = annotationDrawMode,
                            onSquareClick = onSquareClick,
                            modifier = Modifier
                                .size(boardSize)
                                .align(Alignment.Center)
                        )
                    }
                    bottomPlayerCard()
                    sideOrBottomControls(false)
                }
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
            onNewGame = {
                showGameOverModal = false
                onLeaveGame()
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
    onNewGame: () -> Unit,
    onDismiss: () -> Unit
) {
    val outcomeColor = when (state.isPlayerWin) {
        true -> TrophyGold
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
                                text = "${state.timeControlLabel} · $totalMoves moves",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiggyButton(
                        text = "Rematch",
                        onClick = onRematch,
                        variant = DiggyButtonVariant.PRIMARY,
                        icon = Icons.Rounded.Refresh,
                        testTag = "play_modal_rematch",
                        modifier = Modifier.weight(1f)
                    )
                    DiggyButton(
                        text = "New Game",
                        onClick = onNewGame,
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.AddCircleOutline,
                        testTag = "play_modal_new_game",
                        modifier = Modifier.weight(1f)
                    )
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 300.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DiggyButton(
                                text = "Analyse",
                                onClick = onAnalyseGame,
                                enabled = state.sanMoves.isNotEmpty(),
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.Analytics,
                                testTag = "play_modal_analyse",
                                modifier = Modifier.fillMaxWidth()
                            )
                            DiggyButton(
                                text = "Review later",
                                onClick = onDismiss,
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.Schedule,
                                testTag = "play_modal_review_later",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiggyButton(
                                text = "Analyse",
                                onClick = onAnalyseGame,
                                enabled = state.sanMoves.isNotEmpty(),
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.Analytics,
                                testTag = "play_modal_analyse",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyButton(
                                text = "Review later",
                                onClick = onDismiss,
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.Schedule,
                                testTag = "play_modal_review_later",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineWorkerErrorBanner(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        color = FeedbackDangerRed.copy(alpha = 0.14f),
        shape = RoundedCornerShape(DiggySpacing.cardRadius),
        border = BorderStroke(1.dp, FeedbackDangerRed.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("play_engine_error_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = "Engine warning",
                tint = FeedbackDangerRed,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Stockfish worker unavailable",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "$message Using backup on-device search.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DiggyButton(
                text = "Retry",
                onClick = onRetry,
                variant = DiggyButtonVariant.OUTLINED,
                icon = Icons.Rounded.Refresh,
                testTag = "play_retry_engine_button"
            )
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
