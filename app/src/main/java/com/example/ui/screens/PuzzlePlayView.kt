package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.components.ChessBoardView
import com.example.ui.components.ChessPieceIcon
import com.example.ui.components.PromotionPickerDialog
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
        state.lastResultOk == true -> "✔ Correct"
        state.lastResultOk == false -> "✖ Wrong"
        state.playerColor == PieceColor.WHITE -> "White to play"
        else -> "Black to play"
    }
    val turnColor = when (state.lastResultOk) {
        true -> AccentGreenHover
        false -> ClassBlunder
        null -> TextPrimary
    }

    val counterLabel = if (state.mode == PuzzleMode.RATED) {
        "Rated puzzle"
    } else {
        "Puzzle ${state.queueIndex + 1} / ${state.queue.size}"
    }
    val progressFrac = if (state.queue.size > 1) {
        ((state.queueIndex + 1).toFloat() / state.queue.size).coerceIn(0f, 1f)
    } else 1f

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
                            if (state.lastResultOk == null) {
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
                    IconButton(onClick = onToggleFlip, modifier = Modifier.size(36.dp)) {
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

                ChessBoardView(
                    fen = state.currentFen,
                    boardTheme = state.boardTheme,
                    flipped = state.flipped,
                    selectedSquare = state.selectedSquare,
                    lastMoveSquares = if (!state.isWrongMove) state.lastMoveSquares else emptyList(),
                    wrongMoveSquares = if (state.isWrongMove) state.lastMoveSquares else emptyList(),
                    hintSquares = state.hintSquares,
                    legalTargets = state.legalTargets,
                    onSquareClick = onSquareClick,
                    modifier = Modifier.fillMaxWidth()
                )

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
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Hint", tint = AccentGreenHover)
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
                        false -> Color(0xFFFF8A80)
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
