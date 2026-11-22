package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.theme.*
import com.example.ui.viewmodel.MistakeDrillState

@Composable
fun MistakeDrillDialog(
    drill: MistakeDrillState,
    boardTheme: BoardTheme,
    onSquareClick: (String) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onHint: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val cur = drill.currentMistake ?: return
    val moveNum = cur.index / 2 + 1
    val turnStr = if (drill.playerColor == PieceColor.WHITE) "White" else "Black"
    val hintList = if (drill.showHint && cur.bestFrom != null) listOf(cur.bestFrom) else emptyList()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = BgDark,
            shape = RoundedCornerShape(DiggySpacing.cardRadius),
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 560.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FitnessCenter,
                            contentDescription = null,
                            tint = StudioCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Practice Mistakes",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Position ${drill.index + 1} / ${drill.mistakes.size} · Solved ${drill.solvedCount} · Move $moveNum ($turnStr)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(DiggySpacing.touchTargetMin)
                            .testTag("drill_close_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close drill", tint = TextMuted, modifier = Modifier.size(24.dp))
                    }
                }

                // Prompt / Feedback banner
                Surface(
                    color = when {
                        drill.solved -> ClassBest.copy(alpha = 0.16f)
                        drill.wrongAttempt -> ClassBlunder.copy(alpha = 0.16f)
                        else -> SurfaceDark
                    },
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    border = BorderStroke(
                        1.dp,
                        when {
                            drill.solved -> ClassBest
                            drill.wrongAttempt -> ClassBlunder
                            else -> BorderSubtle
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = drill.feedback.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            drill.solved -> ClassBest
                            drill.wrongAttempt -> ClassBlunder
                            else -> TextPrimary
                        },
                        modifier = Modifier
                            .padding(12.dp)
                            .testTag("drill_feedback")
                    )
                }

                ChessBoardView(
                    fen = drill.boardFen,
                    boardTheme = boardTheme,
                    flipped = drill.flipped,
                    selectedSquare = drill.selectedSquare,
                    lastMoveSquares = if (!drill.wrongAttempt) drill.lastMoveSquares else emptyList(),
                    wrongMoveSquares = if (drill.wrongAttempt) drill.lastMoveSquares else emptyList(),
                    hintSquares = hintList,
                    legalTargets = drill.legalTargets,
                    onSquareClick = onSquareClick,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiggyButton(
                        text = "Hint",
                        onClick = onHint,
                        enabled = !drill.solved,
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.Lightbulb,
                        testTag = "drill_hint_button",
                        modifier = Modifier.weight(1f)
                    )
                    DiggyButton(
                        text = "Retry",
                        onClick = onRetry,
                        enabled = drill.wrongAttempt || !drill.solved,
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.Refresh,
                        testTag = "drill_retry_button",
                        modifier = Modifier.weight(1f)
                    )
                    DiggyButton(
                        text = if (drill.index + 1 >= drill.mistakes.size) "Finish" else "Next",
                        onClick = onNext,
                        variant = if (drill.solved) DiggyButtonVariant.PRIMARY else DiggyButtonVariant.SECONDARY,
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        testTag = "drill_next_button",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    drill.pendingPromotion?.let {
        PromotionPickerDialog(
            color = drill.playerColor,
            onSelect = onSelectPromotion,
            onDismiss = onDismissPromotion
        )
    }
}
