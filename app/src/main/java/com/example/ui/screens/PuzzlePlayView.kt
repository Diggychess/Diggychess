package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.components.BoardArrow
import com.example.ui.components.ChessBoardView
import com.example.ui.components.ChessPieceIcon
import com.example.ui.components.PromotionPickerDialog
import com.example.ui.components.SquareMoveBadge
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
        state.isFinished || state.correctMoveSquares.isNotEmpty() -> "✔ Correct"
        state.isWrongMove -> "✖ Wrong Move"
        state.lastResultOk == true -> "✔ Correct"
        state.lastResultOk == false -> "✖ Wrong"
        state.playerColor == PieceColor.WHITE -> "White to play"
        else -> "Black to play"
    }
    val turnColor = when {
        state.isFinished || state.correctMoveSquares.isNotEmpty() || state.lastResultOk == true -> AccentGreenHover
        state.isWrongMove || state.lastResultOk == false -> ClassBlunder
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
            isCorrectFeedback -> ClassBest
            isWrongFeedback -> ClassBlunder
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 220),
        label = "puzzleBoardFeedbackBorder"
    )

    val moveBadge = when {
        isWrongFeedback && state.lastMoveSquares.size >= 2 ->
            SquareMoveBadge(state.lastMoveSquares[1], "✖", ClassBlunder)
        state.correctMoveSquares.size >= 2 ->
            SquareMoveBadge(state.correctMoveSquares[1], "✔", ClassBest)
        else -> null
    }

    val hintArrows = if (state.hintLevel >= 2 && state.hintFromSquare.isNotEmpty() && state.hintToSquare.isNotEmpty()) {
        listOf(BoardArrow(state.hintFromSquare, state.hintToSquare, ArrowSecondaryCyan))
    } else emptyList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header: Side to play + ELO
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LineSubtle),
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
                            }
                            Text(
                                text = turnLabel,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = turnColor
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ELO ",
                                fontSize = 12.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${state.ratingState.rating.roundToInt()}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            state.lastEloDelta?.let { delta ->
                                val dText = (if (delta >= 0) "+" else "") + delta
                                Text(
                                    text = " $dText",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (delta >= 0) AccentGreenHover else ClassBlunder
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(counterLabel, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    IconButton(onClick = onToggleFlip, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Flip, contentDescription = "Flip board", tint = TextMuted)
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

                Spacer(modifier = Modifier.height(10.dp))

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
                            onUndo = onUndo
                        )
                    }

                    // Timed Mode Expiration Overlay
                    if (state.isTimedExpired) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xC8090D12))
                                .padding(16.dp)
                                .testTag("puzzle_timed_overlay"),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = CardDark,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = "Time expired",
                                        tint = AccentGreenHover,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Time's Up!",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
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
                                        OutlinedButton(
                                            onClick = onBack,
                                            border = BorderStroke(1.dp, BorderSubtle),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                        ) {
                                            Text("Menu", color = TextPrimary)
                                        }
                                        Button(
                                            onClick = onRestart,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AccentGreen,
                                                contentColor = OnAccentDark
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 48.dp)
                                        ) {
                                            Text("Retry", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4-button Puzzle Control Bar
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LineSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = onRestart,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("puzzle_restart")
                        ) {
                            Icon(Icons.Filled.Replay, contentDescription = "Restart puzzle", tint = AccentGreenHover)
                        }
                        VerticalDivider(color = LineSubtle, modifier = Modifier.height(54.dp))
                        IconButton(
                            onClick = onUndo,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("puzzle_undo")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo move", tint = AccentGreenHover)
                        }
                        VerticalDivider(color = LineSubtle, modifier = Modifier.height(54.dp))
                        IconButton(
                            onClick = onHint,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("puzzle_hint")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "Open hint modal",
                                tint = if (state.hintLevel > 0) StudioCyan else AccentGreenHover
                            )
                        }
                        VerticalDivider(color = LineSubtle, modifier = Modifier.height(54.dp))
                        IconButton(
                            onClick = onNext,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("puzzle_next")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next puzzle", tint = AccentGreenHover)
                        }
                    }
                }
            }
        }

        // Feedback & Details Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val mateCount = ceil(cur.solution.size / 2.0).toInt()
                Text(
                    text = "Find the best move." + if (cur.isMate) " Mate in $mateCount." else "",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                if (state.feedbackText.isNotEmpty()) {
                    val fbColor = when (state.feedbackOk) {
                        true -> AccentGreen
                        false -> ClassBlunder
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
                        color = SurfaceDark,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("puzzle_solution_explanation")
                    ) {
                        Text(
                            text = state.solutionExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (state.mode == PuzzleMode.TIMED) {
                    val mins = state.timedRemainingSec / 60
                    val secs = state.timedRemainingSec % 60
                    Text(
                        text = "⏱ $mins:${secs.toString().padStart(2, '0')} · solved ${state.timedSolvedCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGreenHover
                    )
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

        OutlinedButton(
            onClick = onBack,
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("puzzle_back_button")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Back to puzzles", color = TextPrimary)
        }
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
            title = { Text("Confirm", color = TextPrimary) },
            text = {
                Text(
                    "Do you want to skip this puzzle? It will be counted as a loss.",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmSkip, modifier = Modifier.testTag("confirm_skip_puzzle")) {
                    Text("OK", color = AccentGreenHover, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSkip) {
                    Text("CANCEL", color = TextMuted)
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
private fun PuzzleMoveFeedbackOverlay(
    isWrong: Boolean,
    isSolved: Boolean,
    eloDelta: Int?,
    onUndo: () -> Unit
) {
    val containerColor = if (isWrong) ClassBlunder.copy(alpha = 0.95f) else AccentGreenDark.copy(alpha = 0.95f)
    val borderColor = if (isWrong) Color.White.copy(alpha = 0.35f) else AccentGreenHover.copy(alpha = 0.7f)

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
                    imageVector = if (isWrong) Icons.Filled.Cancel else Icons.Filled.CheckCircle,
                    contentDescription = if (isWrong) "Incorrect move" else "Correct move",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = when {
                        isWrong -> "Wrong Move — Try Again"
                        isSolved -> "Solved!"
                        else -> "Correct Move!"
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            if (isWrong) {
                Surface(
                    onClick = onUndo,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("puzzle_overlay_undo")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
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
            } else if (isSolved && eloDelta != null) {
                val deltaStr = (if (eloDelta >= 0) "+" else "") + eloDelta + " ELO"
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = deltaStr,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
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
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderSubtle),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("puzzle_hint_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(StudioCyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
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
                            .size(48.dp)
                            .testTag("hint_modal_close")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
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
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
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
                        color = SurfaceDark.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(14.dp),
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
                            OutlinedButton(
                                onClick = onRevealDestination,
                                border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.65f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("hint_reveal_destination")
                            ) {
                                Text(
                                    text = "Reveal Target Square & Line",
                                    color = StudioCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = AccentGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.55f)),
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
                                color = AccentGreenHover
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

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = OnAccentDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("hint_dismiss_button")
                ) {
                    Text("Try on Board", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
