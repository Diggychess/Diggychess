package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.EngineVariation
import com.example.chess.MoveExplainer
import com.example.ui.theme.*
import com.example.ui.viewmodel.VariationBranch

@Composable
fun MultiPvLinesCard(
    lines: List<EngineVariation>,
    isLoading: Boolean,
    variation: VariationBranch?,
    onPlayLineMove: (EngineVariation, Int) -> Unit,
    onStepVariation: (Int) -> Unit,
    onUndoVariation: () -> Unit,
    onExitVariation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLines = variation?.liveLines ?: lines
    val evalBusy = variation?.isEvaluating ?: isLoading

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, if (variation != null) StudioCyan.copy(alpha = 0.55f) else BorderSubtle),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = null,
                        tint = StudioCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (variation != null) "VARIATION · TOP 3 LINES" else "ENGINE LINES · TOP 3",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (variation != null) StudioCyan else TextMuted
                    )
                }

                if (evalBusy) {
                    CircularProgressIndicator(
                        color = StudioCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    val depth = activeLines.firstOrNull()?.depth ?: 0
                    if (depth > 0) {
                        Surface(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = "depth $depth",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Variation explorer breadcrumb strip
            AnimatedVisibility(visible = variation != null) {
                if (variation != null) {
                    Surface(
                        color = StudioCyan.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Exploring branch (${variation.visibleSteps.size} move${if (variation.visibleSteps.size == 1) "" else "s"})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StudioCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = onUndoVariation,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .heightIn(min = 36.dp)
                                            .testTag("variation_undo_button")
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo variation move",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Step back", fontSize = 12.sp, color = TextPrimary)
                                    }
                                    TextButton(
                                        onClick = onExitVariation,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .heightIn(min = 36.dp)
                                            .testTag("variation_exit_button")
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Return to main game",
                                            tint = StudioCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Back to game", fontSize = 12.sp, color = StudioCyan)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                variation.steps.forEachIndexed { idx, step ->
                                    val isCurrent = idx == variation.activeStepIndex
                                    Surface(
                                        onClick = { onStepVariation(idx) },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCurrent) StudioCyan else SurfaceDark,
                                        border = BorderStroke(1.dp, if (isCurrent) StudioCyan else BorderSubtle)
                                    ) {
                                        Text(
                                            text = step.san,
                                            fontSize = 12.sp,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCurrent) OnAccentDark else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeLines.isEmpty()) {
                Text(
                    text = if (evalBusy) "Calculating top 3 Stockfish lines…" else "Tap any square on the board to explore a variation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeLines.take(3).forEach { line ->
                        EngineVariationRow(
                            line = line,
                            onPlayMove = { moveIdx -> onPlayLineMove(line, moveIdx) }
                        )
                    }
                }
                Text(
                    text = "Tap any move above or tap pieces on the board to explore lines.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun EngineVariationRow(
    line: EngineVariation,
    onPlayMove: (Int) -> Unit
) {
    val evalStr = MoveExplainer.formatPlyEval(line.cp, line.mate)
    val isWhiteFav = (line.mate != null && line.mate > 0) || (line.mate == null && line.cp >= 0.25)
    val isBlackFav = (line.mate != null && line.mate < 0) || (line.mate == null && line.cp <= -0.25)

    val evalBg = when {
        isWhiteFav -> Color(0xFFF3F6FA)
        isBlackFav -> Color(0xFF1F2730)
        else -> SurfaceDark
    }
    val evalFg = when {
        isWhiteFav -> Color(0xFF0C1014)
        else -> TextPrimary
    }

    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (line.rank == 1) StudioCyan.copy(alpha = 0.45f) else BorderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("multipv_line_${line.rank}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "#${line.rank}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (line.rank == 1) StudioCyan else TextMuted
            )

            Surface(
                color = evalBg,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.widthIn(min = 52.dp)
            ) {
                Text(
                    text = evalStr,
                    fontSize = 12.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = evalFg,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val moves = line.pvSan.ifEmpty { listOf(line.firstSan) }
                moves.forEachIndexed { idx, san ->
                    Surface(
                        onClick = { onPlayMove(idx) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (idx == 0 && line.rank == 1) AccentGreen.copy(alpha = 0.16f) else CardDark,
                        border = BorderStroke(
                            1.dp,
                            if (idx == 0 && line.rank == 1) AccentGreen.copy(alpha = 0.5f) else BorderSubtle
                        ),
                        modifier = Modifier.testTag("pv_move_${line.rank}_$idx")
                    ) {
                        Text(
                            text = san,
                            fontSize = 12.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (idx == 0 && line.rank == 1) AccentGreenHover else TextPrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
