package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PuzzleMode
import com.example.ui.viewmodel.PuzzlesUiState
import com.example.ui.viewmodel.PuzzlesViewModel
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun PuzzlePlayView(
    state: PuzzlesUiState,
    onSquareClick: (String) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onRestart: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onRevealHintDestination: () -> Unit,
    onDismissHintModal: () -> Unit,
    onNext: () -> Unit,
    onConfirmSkip: () -> Unit,
    onDismissSkip: () -> Unit,
    onToggleFlip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val cur = state.currentPuzzle ?: return
    val turnLabel = when {
        state.isFinished || state.correctMoveSquares.isNotEmpty() -> "Correct"
        state.isWrongMove -> "Wrong Move"
        state.lastResultOk == true -> "Correct"
        state.lastResultOk == false -> "Wrong"
        state.playerColor == PieceColor.WHITE -> "White to play"
        else -> "Black to play"
    }
    val turnColor = when {
        state.isFinished || state.correctMoveSquares.isNotEmpty() || state.lastResultOk == true -> FeedbackCorrectGreen
        state.isWrongMove || state.lastResultOk == false -> FeedbackDangerRed
        else -> TextPrimary
    }

    val counterLabel = if (state.mode == PuzzleMode.RATED) {
        "Rated puzzle"
    } else {
        "Puzzle ${state.queueIndex + 1} / ${state.queue.size}"
    }
    val progressFrac = if (state.queue.size > 1) {
        ((state.queueIndex + 1).toFloat() / state.queue.size).coerceIn(0f, 1f)
    } else 1f

    val isCorrectFeedback = (state.correctMoveSquares.isNotEmpty() || state.isFinished) && !state.isTimedExpired
    val isWrongFeedback = state.isWrongMove && !state.isTimedExpired

    val boardFrameColor by animateColorAsState(
        targetValue = when {
            isCorrectFeedback -> FeedbackCorrectGreen
            isWrongFeedback -> FeedbackDangerRed
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "puzzleBoardFeedbackBorder"
    )

    val moveBadge = when {
        isWrongFeedback && state.lastMoveSquares.size >= 2 ->
            SquareMoveBadge(state.lastMoveSquares[1], "✖", FeedbackDangerRed)
        state.correctMoveSquares.size >= 2 ->
            SquareMoveBadge(state.correctMoveSquares[1], "✔", FeedbackCorrectGreen)
        else -> null
    }

    val hintArrows = if (state.hintLevel >= 2 && state.hintFromSquare.isNotEmpty() && state.hintToSquare.isNotEmpty()) {
        listOf(BoardArrow(state.hintFromSquare, state.hintToSquare, ArrowSecondaryCyan))
    } else emptyList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DiggySpacing.s2, vertical = DiggySpacing.s2),
        verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
    ) {
        DiggyCard(elevated = false, contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Header: Side to play + ELO
                Surface(
                    color = BgDark,
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!isCorrectFeedback && !isWrongFeedback && state.lastResultOk == null) {
                                ChessPieceIcon(
                                    piece = ChessPiece(state.playerColor, PieceType.KING),
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isWrongFeedback || state.lastResultOk == false) Icons.Rounded.Cancel else Icons.Rounded.CheckCircle,
                                    contentDescription = turnLabel,
                                    tint = turnColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = turnLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = turnColor
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ELO ",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = "${state.ratingState.rating.roundToInt()}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            state.lastEloDelta?.let { delta ->
                                val dText = (if (delta >= 0) "+" else "") + delta
                                Text(
                                    text = " $dText",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (delta >= 0) FeedbackCorrectGreen else FeedbackDangerRed
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = counterLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PuzzleStreakHeaderBadge(
                            currentStreak = state.currentPuzzleStreak,
                            bestStreak = state.bestPuzzleStreak,
                            isNewBest = state.isNewBestStreak,
                            milestoneReached = state.streakMilestoneReached
                        )
                        IconButton(
                            onClick = onToggleFlip,
                            modifier = Modifier.size(DiggySpacing.touchTargetMin)
                        ) {
                            Icon(
                                Icons.Rounded.SwapVert,
                                contentDescription = "Flip board",
                                tint = TextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { progressFrac },
                    color = AccentGreen,
                    trackColor = BorderSubtle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                )

                // Interactive Board + Immediate Visual Feedback Overlay Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.5.dp, boardFrameColor, RoundedCornerShape(14.dp))
                        .padding(2.dp)
                ) {
                    ChessBoardView(
                        fen = state.currentFen,
                        boardTheme = state.boardTheme,
                        flipped = state.flipped,
                        selectedSquare = state.selectedSquare,
                        lastMoveSquares = if (!isWrongFeedback && state.correctMoveSquares.isEmpty()) {
                            state.lastMoveSquares
                        } else emptyList(),
                        correctMoveSquares = state.correctMoveSquares,
                        wrongMoveSquares = if (isWrongFeedback) state.lastMoveSquares else emptyList(),
                        hintSquares = state.hintSquares,
                        legalTargets = state.legalTargets,
                        arrows = hintArrows,
                        moveBadge = moveBadge,
                        animateMoves = state.animateMoves,
                        onSquareClick = onSquareClick,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Floating Green / Red Feedback Pill Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isCorrectFeedback || isWrongFeedback,
                        enter = fadeIn(tween(180)) + slideInVertically(tween(200)) { it / 2 } + scaleIn(tween(180), initialScale = 0.94f),
                        exit = fadeOut(tween(140)) + slideOutVertically(tween(140)) { it / 2 } + scaleOut(tween(140), targetScale = 0.94f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        PuzzleMoveFeedbackOverlay(
                            isWrong = isWrongFeedback,
                            isSolved = state.isFinished,
                            eloDelta = if (state.isFinished) state.lastEloDelta else null,
                            currentStreak = state.currentPuzzleStreak,
                            isNewBest = state.isNewBestStreak,
                            milestoneReached = state.streakMilestoneReached,
                            onUndo = onUndo
                        )
                    }

                    // Timed Mode Expiration Overlay
                    if (state.isTimedExpired) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xD0121212))
                                .padding(16.dp)
                                .testTag("puzzle_timed_overlay"),
                            contentAlignment = Alignment.Center
                        ) {
                            DiggyCard(
                                elevated = true,
                                borderColor = AccentGreen.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth(0.92f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Timer,
                                        contentDescription = "Time expired",
                                        tint = AccentGreen,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Time's Up!",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Solved ${state.timedSolvedCount} puzzle${if (state.timedSolvedCount == 1) "" else "s"} in 3 minutes.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        DiggyButton(
                                            text = "Menu",
                                            onClick = onBack,
                                            variant = DiggyButtonVariant.OUTLINED,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DiggyButton(
                                            text = "Retry",
                                            onClick = onRestart,
                                            variant = DiggyButtonVariant.PRIMARY,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4-button Puzzle Control Bar
                Surface(
                    color = CardSurfaceElevated,
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onRestart,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("puzzle_restart")
                        ) {
                            Icon(Icons.Rounded.Replay, contentDescription = "Restart puzzle", tint = AccentGreen, modifier = Modifier.size(24.dp))
                        }
                        VerticalDivider(color = LineSubtle, modifier = Modifier.height(52.dp))
                        IconButton(
                            onClick = onUndo,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("puzzle_undo")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo move", tint = AccentGreen, modifier = Modifier.size(24.dp))
                        }
                        VerticalDivider(color = LineSubtle, modifier = Modifier.height(52.dp))
                        IconButton(
                            onClick = onHint,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("puzzle_hint")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.HelpOutline,
                                contentDescription = "Open hint modal",
                                tint = if (state.hintLevel > 0) StudioCyan else AccentGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        VerticalDivider(color = LineSubtle, modifier = Modifier.height(52.dp))
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("puzzle_next")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next puzzle", tint = AccentGreen, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        // Feedback & Details Card
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val mateCount = ceil(cur.solution.size / 2.0).toInt()
                Text(
                    text = "Find the best move." + if (cur.isMate) " Mate in $mateCount." else "",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                if (state.feedbackText.isNotEmpty()) {
                    val fbColor = when (state.feedbackOk) {
                        true -> FeedbackCorrectGreen
                        false -> FeedbackDangerRed
                        null -> TextPrimary
                    }
                    Text(
                        text = state.feedbackText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = fbColor,
                        modifier = Modifier.testTag("puzzle_feedback")
                    )
                }

                if (state.solutionExplanation.isNotEmpty() && (state.isFinished || state.hintLevel >= 2)) {
                    Surface(
                        color = CardSurfaceElevated,
                        shape = RoundedCornerShape(DiggySpacing.controlRadius),
                        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("puzzle_solution_explanation")
                    ) {
                        Text(
                            text = state.solutionExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (state.mode == PuzzleMode.TIMED) {
                    val mins = state.timedRemainingSec / 60
                    val secs = state.timedRemainingSec % 60
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "$mins:${secs.toString().padStart(2, '0')} · solved ${state.timedSolvedCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentGreen
                        )
                    }
                }

                val filteredThemes = cur.themes
                    .filter { !Regex("^(opening|middlegame|endgame|short|long|veryLong|oneMove)$").matches(it) }
                    .take(4)
                    .joinToString(" · ") { PuzzlesViewModel.formatThemeLabel(it) }

                Text(
                    text = (if (cur.rating > 0) "Rating ${cur.rating}" else "Unrated") +
                        if (filteredThemes.isNotEmpty()) " · $filteredThemes" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
                Text(
                    text = "Source: ${cur.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        DiggyButton(
            text = "Back to Puzzles",
            onClick = onBack,
            variant = DiggyButtonVariant.OUTLINED,
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            testTag = "puzzle_back_button",
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (state.showHintModal) {
        PuzzleHintDialog(
            state = state,
            onRevealDestination = onRevealHintDestination,
            onDismiss = onDismissHintModal
        )
    }

    if (state.showSkipDialog) {
        AlertDialog(
            onDismissRequest = onDismissSkip,
            containerColor = CardDark,
            title = { Text("Skip Puzzle?", color = TextPrimary) },
            text = {
                Text(
                    "Do you want to skip this puzzle? It will be counted as a loss.",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmSkip, modifier = Modifier.testTag("confirm_skip_puzzle")) {
                    Text("Skip", color = AccentGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSkip) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
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
private fun PuzzleStreakHeaderBadge(
    currentStreak: Int,
    bestStreak: Int,
    isNewBest: Boolean,
    milestoneReached: Int?,
    modifier: Modifier = Modifier
) {
    val pulseScale = remember { Animatable(1f) }
    var prevStreak by remember { mutableIntStateOf(currentStreak) }

    LaunchedEffect(currentStreak) {
        if (currentStreak > prevStreak && currentStreak > 0) {
            pulseScale.snapTo(1f)
            pulseScale.animateTo(
                targetValue = if (milestoneReached != null || isNewBest) 1.22f else 1.14f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 520f)
            )
            pulseScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = 360f)
            )
        }
        prevStreak = currentStreak
    }

    val isHotTier = currentStreak >= 5 || milestoneReached != null || isNewBest
    val isActive = currentStreak > 0

    val containerColor by animateColorAsState(
        targetValue = when {
            isHotTier -> ClassMistake.copy(alpha = 0.20f)
            isActive -> AccentGreen.copy(alpha = 0.16f)
            else -> BgDark
        },
        animationSpec = tween(220),
        label = "streakBadgeBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isHotTier -> ClassMistake.copy(alpha = 0.72f)
            isActive -> AccentGreen.copy(alpha = 0.50f)
            else -> BorderSubtle
        },
        animationSpec = tween(220),
        label = "streakBadgeBorder"
    )
    val flameTint by animateColorAsState(
        targetValue = when {
            isHotTier -> ClassMistake
            isActive -> AccentGreen
            else -> TextMuted
        },
        animationSpec = tween(220),
        label = "streakBadgeFlame"
    )
    val countColor by animateColorAsState(
        targetValue = when {
            isHotTier -> AccentGreen
            isActive -> AccentGreen
            else -> TextMuted
        },
        animationSpec = tween(220),
        label = "streakBadgeCount"
    )

    val a11yLabel = buildString {
        append("Puzzle streak $currentStreak")
        if (bestStreak > 0) append(", best streak $bestStreak")
        if (isNewBest) append(", new personal best")
        if (milestoneReached != null) append(", milestone $milestoneReached")
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .graphicsLayer {
                scaleX = pulseScale.value
                scaleY = pulseScale.value
            }
            .semantics(mergeDescendants = true) {
                contentDescription = a11yLabel
            }
            .testTag("puzzle_streak_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = flameTint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$currentStreak",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = countColor,
                maxLines = 1
            )
            when {
                isNewBest && milestoneReached != null -> {
                    Text(
                        text = "★ Best!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClassMistake,
                        maxLines = 1
                    )
                }
                isNewBest -> {
                    Text(
                        text = "Best!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClassMistake,
                        maxLines = 1
                    )
                }
                milestoneReached != null -> {
                    Text(
                        text = "★ · Best $bestStreak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary.copy(alpha = 0.85f),
                        maxLines = 1
                    )
                }
                bestStreak > 0 -> {
                    Text(
                        text = "· Best $bestStreak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PuzzleMoveFeedbackOverlay(
    isWrong: Boolean,
    isSolved: Boolean,
    eloDelta: Int?,
    currentStreak: Int,
    isNewBest: Boolean,
    milestoneReached: Int?,
    onUndo: () -> Unit
) {
    val containerColor = if (isWrong) FeedbackDangerRed.copy(alpha = 0.96f) else FeedbackCorrectGreen.copy(alpha = 0.96f)
    val borderColor = Color.White.copy(alpha = 0.3f)

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("puzzle_feedback_overlay")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isWrong) Icons.Rounded.Cancel else Icons.Rounded.CheckCircle,
                    contentDescription = if (isWrong) "Incorrect move" else "Correct move",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = when {
                        isWrong -> "Wrong Move — Try Again"
                        isSolved -> "Solved! Next puzzle…"
                        else -> "Correct Move!"
                    },
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isWrong) {
                Surface(
                    onClick = onUndo,
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .defaultMinSize(minWidth = DiggySpacing.touchTargetMin, minHeight = DiggySpacing.touchTargetMin)
                        .testTag("puzzle_overlay_undo")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = "Undo wrong move",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Undo",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (isSolved) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (currentStreak > 0) {
                        val isHighlightedStreak = milestoneReached != null || isNewBest
                        val streakPillBg = if (isHighlightedStreak) AccentGreen else Color.White.copy(alpha = 0.22f)
                        val streakPillInk = if (isHighlightedStreak) OnAccentDark else Color.White
                        val streakText = when {
                            milestoneReached != null && eloDelta == null -> "$currentStreak Milestone!"
                            milestoneReached != null -> "$currentStreak ★"
                            isNewBest && eloDelta == null -> "$currentStreak Best!"
                            isNewBest -> "$currentStreak Best"
                            eloDelta == null -> "$currentStreak Streak"
                            else -> "$currentStreak"
                        }
                        Surface(
                            color = streakPillBg,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.testTag("puzzle_overlay_streak")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = "Puzzle streak $currentStreak",
                                    tint = streakPillInk,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = streakText,
                                    color = streakPillInk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    if (eloDelta != null) {
                        val deltaStr = (if (eloDelta >= 0) "+" else "") + eloDelta + " ELO"
                        Surface(
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = deltaStr,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleHintDialog(
    state: PuzzlesUiState,
    onRevealDestination: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        DiggyCard(
            elevated = true,
            contentPadding = PaddingValues(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("puzzle_hint_modal")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(StudioCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = "Tactical Hint",
                                tint = StudioCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Tactical Hint",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (state.hintThemeLabel.isNotEmpty()) {
                                Text(
                                    text = state.hintThemeLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StudioCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(DiggySpacing.touchTargetMin)
                            .testTag("hint_modal_close")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close hint modal",
                            tint = TextMuted
                        )
                    }
                }

                Text(
                    text = "Assisted puzzles are excluded from ELO gains and daily streak credit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                // Stage 1: Piece Clue Card
                Surface(
                    color = BgDark,
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "STAGE 1 · PIECE CLUE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = StudioCyan
                        )
                        Text(
                            text = if (state.hintFromSquare.isNotEmpty()) {
                                "Move the ${state.hintPieceName} on ${state.hintFromSquare.uppercase()}"
                            } else {
                                "Inspect your active piece highlighted on the board."
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (state.hintTacticalCue.isNotEmpty()) {
                            Text(
                                text = state.hintTacticalCue,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Stage 2: Target Square & Line
                if (state.hintLevel < 2) {
                    Surface(
                        color = BgDark.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(DiggySpacing.controlRadius),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Still stuck? Reveal the exact target square and full tactical line.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            DiggyButton(
                                text = "Reveal Target Square & Line",
                                onClick = onRevealDestination,
                                variant = DiggyButtonVariant.OUTLINED,
                                testTag = "hint_reveal_destination",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Surface(
                        color = FeedbackCorrectGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(DiggySpacing.controlRadius),
                        border = BorderStroke(1.dp, FeedbackCorrectGreen.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("hint_stage2_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "STAGE 2 · TARGET & LINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FeedbackCorrectGreen
                            )
                            if (state.hintFromSquare.isNotEmpty() && state.hintToSquare.isNotEmpty()) {
                                val sanTag = if (state.hintSanMove.isNotEmpty()) " (${state.hintSanMove})" else ""
                                Text(
                                    text = "Play ${state.hintFromSquare} → ${state.hintToSquare}$sanTag",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            if (state.solutionExplanation.isNotEmpty()) {
                                Text(
                                    text = state.solutionExplanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                DiggyButton(
                    text = "Try on Board",
                    onClick = onDismiss,
                    variant = DiggyButtonVariant.PRIMARY,
                    testTag = "hint_dismiss_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
