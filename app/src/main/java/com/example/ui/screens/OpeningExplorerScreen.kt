package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Refresh
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
import com.example.ui.components.ChessBoardView
import com.example.ui.components.PromotionPickerDialog
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Opening Explorer", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                text = "Tap or drag moves to explore theory, Lichess master/club win rates, and candidate continuations.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // Database Toggle & Opening Header
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !state.useMastersDb,
                        onClick = { onDatabaseChange(false) },
                        label = { Text("Club DB (Lichess)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGreen.copy(alpha = 0.18f),
                            selectedLabelColor = AccentGreenHover
                        ),
                        modifier = Modifier.testTag("explorer_club_db")
                    )
                    FilterChip(
                        selected = state.useMastersDb,
                        onClick = { onDatabaseChange(true) },
                        label = { Text("Masters DB") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StudioCyan.copy(alpha = 0.18f),
                            selectedLabelColor = StudioCyan
                        ),
                        modifier = Modifier.testTag("explorer_masters_db")
                    )
                }

                val openingLabel = state.stats?.openingName?.let { name ->
                    val ecoPrefix = state.stats.openingEco?.let { "$it · " }.orEmpty()
                    "$ecoPrefix$name"
                } ?: "Starting Position"

                Text(
                    text = openingLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreenHover
                )

                if (state.sanHistory.isNotEmpty()) {
                    val pgnPreview = state.sanHistory.mapIndexed { idx, san ->
                        if (idx % 2 == 0) "${idx / 2 + 1}. $san" else san
                    }.joinToString(" ")
                    Text(
                        text = pgnPreview,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
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

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onUndo,
                        enabled = state.canUndo,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("explorer_undo")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo move", modifier = Modifier.size(16.dp), tint = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back", color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = onReset,
                        enabled = state.canUndo,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("explorer_reset")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset board", modifier = Modifier.size(16.dp), tint = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = onToggleFlip,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("explorer_flip")
                    ) {
                        Icon(Icons.Filled.Flip, contentDescription = "Flip board", modifier = Modifier.size(16.dp), tint = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Flip", color = TextPrimary)
                    }
                }

                if (state.sanHistory.isNotEmpty()) {
                    Button(
                        onClick = onAnalysePosition,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("explorer_analyse")
                    ) {
                        Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send line to Analyse Game", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Candidate Moves Table
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("POPULAR CONTINUATIONS", style = MaterialTheme.typography.titleSmall, color = TextMuted)
                    val src = state.stats?.sourceLabel.orEmpty()
                    if (src.isNotEmpty()) {
                        Text(src, style = MaterialTheme.typography.labelSmall, color = StudioCyan)
                    }
                }

                if (state.isLoadingStats) {
                    LinearProgressIndicator(
                        color = StudioCyan,
                        trackColor = BorderSubtle,
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(4.dp))
                    )
                }

                val moves = state.stats?.moves.orEmpty()
                if (moves.isEmpty() && !state.isLoadingStats) {
                    Text(
                        text = "No database games recorded from this position.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                } else {
                    moves.forEach { mv ->
                        Surface(
                            onClick = { onPlayMove(mv.uci) },
                            color = SurfaceDark,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("explorer_move_${mv.uci}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mv.san,
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
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

    state.pendingPromotion?.let {
        PromotionPickerDialog(
            color = turnColor,
            onSelect = onSelectPromotion,
            onDismiss = onDismissPromotion
        )
    }
}
