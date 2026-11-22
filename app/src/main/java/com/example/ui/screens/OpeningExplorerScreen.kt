package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.OpeningExplorerUiState
import kotlin.math.roundToInt

@Composable
fun OpeningExplorerScreen(
    state: OpeningExplorerUiState,
    onSquareClick: (String) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onPlayMove: (String) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onToggleFlip: () -> Unit,
    onDatabaseChange: (Boolean) -> Unit,
    onAnalysePosition: () -> Unit,
    modifier: Modifier = Modifier
) {
    val turnColor = if (state.fen.split(' ').getOrNull(1) == "b") PieceColor.BLACK else PieceColor.WHITE

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DiggySpacing.s2, vertical = DiggySpacing.s2),
        verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Opening Explorer", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                text = "Explore opening theory, Lichess master & club win rates, and candidate lines.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // Database Toggle & Interactive Board Card
        DiggyCard(contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DiggySegmentedControl(
                    options = listOf(false, true),
                    selectedOption = state.useMastersDb,
                    onSelect = onDatabaseChange,
                    labelFor = { if (it) "Masters DB" else "Club DB (Lichess)" },
                    tagFor = { if (it) "explorer_masters_db" else "explorer_club_db" }
                )

                val openingLabel = state.stats?.openingName?.let { name ->
                    val ecoPrefix = state.stats.openingEco?.let { "$it · " }.orEmpty()
                    "$ecoPrefix$name"
                } ?: "Starting Position"

                Text(
                    text = openingLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )

                if (state.sanHistory.isNotEmpty()) {
                    val pgnPreview = state.sanHistory.mapIndexed { idx, san ->
                        if (idx % 2 == 0) "${idx / 2 + 1}. $san" else san
                    }.joinToString(" ")
                    Text(
                        text = pgnPreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }

                ChessBoardView(
                    fen = state.fen,
                    boardTheme = state.boardTheme,
                    flipped = state.flipped,
                    selectedSquare = state.selectedSquare,
                    lastMoveSquares = state.lastMoveSquares,
                    legalTargets = state.legalTargets,
                    onSquareClick = onSquareClick,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiggyButton(
                        text = "Back",
                        onClick = onUndo,
                        enabled = state.canUndo,
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.AutoMirrored.Rounded.Undo,
                        testTag = "explorer_undo",
                        modifier = Modifier.weight(1f)
                    )
                    DiggyButton(
                        text = "Reset",
                        onClick = onReset,
                        enabled = state.canUndo,
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.Refresh,
                        testTag = "explorer_reset",
                        modifier = Modifier.weight(1f)
                    )
                    DiggyButton(
                        text = "Flip",
                        onClick = onToggleFlip,
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.SwapVert,
                        testTag = "explorer_flip",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (state.sanHistory.isNotEmpty()) {
                    DiggyButton(
                        text = "Send Line to Analyse Game",
                        onClick = onAnalysePosition,
                        variant = DiggyButtonVariant.PRIMARY,
                        icon = Icons.Rounded.Analytics,
                        testTag = "explorer_analyse",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Popular Continuations Table
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Popular Continuations", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    val src = state.stats?.sourceLabel.orEmpty()
                    if (src.isNotEmpty()) {
                        Text(src, style = MaterialTheme.typography.labelSmall, color = StudioCyan)
                    }
                }

                if (state.isLoadingStats) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) {
                            DiggySkeletonBox(modifier = Modifier.fillMaxWidth().height(54.dp))
                        }
                    }
                } else {
                    val moves = state.stats?.moves.orEmpty()
                    if (moves.isEmpty()) {
                        DiggyEmptyState(
                            title = "No database games found",
                            subtitle = "Try switching between Masters DB and Club DB or stepping back a move.",
                            icon = Icons.Rounded.Explore
                        )
                    } else {
                        moves.forEach { mv ->
                            Surface(
                                onClick = { onPlayMove(mv.uci) },
                                color = CardSurfaceElevated,
                                shape = RoundedCornerShape(DiggySpacing.controlRadius),
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("explorer_move_${mv.uci}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = mv.san,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        val meta = if (mv.isOfflineBook) {
                                            "Built-in book move"
                                        } else {
                                            val wPct = (mv.whitePct * 100).roundToInt()
                                            val dPct = (mv.drawPct * 100).roundToInt()
                                            val bPct = (mv.blackPct * 100).roundToInt()
                                            "${mv.totalGames} games · W $wPct% / D $dPct% / B $bPct%"
                                        }
                                        Text(
                                            text = meta,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (!mv.isOfflineBook) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                        ) {
                                            if (mv.whitePct > 0f) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(mv.whitePct.coerceAtLeast(0.02f))
                                                        .fillMaxHeight()
                                                        .background(Color(0xFFECEFF1))
                                                )
                                            }
                                            if (mv.drawPct > 0f) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(mv.drawPct.coerceAtLeast(0.02f))
                                                        .fillMaxHeight()
                                                        .background(Color(0xFF78909C))
                                                )
                                            }
                                            if (mv.blackPct > 0f) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(mv.blackPct.coerceAtLeast(0.02f))
                                                        .fillMaxHeight()
                                                        .background(Color(0xFF263238))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.pendingPromotion?.let {
        PromotionPickerDialog(
            color = turnColor,
            onSelect = onSelectPromotion,
            onDismiss = onDismissPromotion
        )
    }
}
