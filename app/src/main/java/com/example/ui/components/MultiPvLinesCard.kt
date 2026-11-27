package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Memory
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

    DiggyCard(
        elevated = false,
        borderColor = if (variation != null) StudioCyan.copy(alpha = 0.55f) else BorderSubtle,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        imageVector = Icons.Rounded.Memory,
                        contentDescription = null,
                        tint = StudioCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (variation != null) "Variation · Top 3 Lines" else "Engine · Top 3 Lines",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (variation != null) StudioCyan else TextPrimary
                    )
                }

                if (evalBusy) {
                    CircularProgressIndicator(
                        color = StudioCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    val depth = activeLines.firstOrNull()?.depth ?: 0
                    if (depth > 0) {
                        Surface(
                            color = CardSurfaceElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = "Depth $depth",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = variation != null) {
                if (variation != null) {
                    Surface(
                        color = StudioCyan.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(12.dp),
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
                                    text = "Branch (${variation.visibleSteps.size} move${if (variation.visibleSteps.size == 1) "" else "s"})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = StudioCyan
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = onUndoVariation,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .heightIn(min = DiggySpacing.touchTargetMin)
                                            .testTag("variation_undo_button")
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.Undo,
                                            contentDescription = "Undo variation move",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Undo", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                                    }
                                    TextButton(
                                        onClick = onExitVariation,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .heightIn(min = DiggySpacing.touchTargetMin)
                                            .testTag("variation_exit_button")
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Return to game",
                                            tint = StudioCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Exit", style = MaterialTheme.typography.labelMedium, color = StudioCyan)
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
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) StudioCyan else SurfaceDark,
                                        border = BorderStroke(1.dp, if (isCurrent) StudioCyan else BorderSubtle)
                                    ) {
                                        Text(
                                            text = step.san,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCurrent) OnAccentDark else TextPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                    text = if (evalBusy) "Calculating top 3 engine lines…" else "Tap any piece on the board to explore lines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeLines.take(3).forEach { line ->
                        EngineVariationRow(
                            line = line,
                            onPlayMove = { moveIdx -> onPlayLineMove(line, moveIdx) }
                        )
                    }
                }
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
        isWhiteFav -> Color(0xFFF5F4F1)
        isBlackFav -> Color(0xFF1D1B18)
        else -> CardSurfaceElevated
    }
    val evalFg = when {
        isWhiteFav -> Color(0xFF1D1B18)
        else -> Color(0xFFF5F4F1)
    }

    Surface(
        color = CardSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
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
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "#${line.rank}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = FontWeight.Bold,
                color = if (line.rank == 1) StudioCyan else TextMuted
            )

            Surface(
                color = evalBg,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.widthIn(min = 52.dp)
            ) {
                Text(
                    text = evalStr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = evalFg,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontFeatureSettings = "tnum"
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val moves = line.pvSan.ifEmpty { listOf(line.firstSan) }
                moves.forEachIndexed { idx, san ->
                    Surface(
                        onClick = { onPlayMove(idx) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (idx == 0 && line.rank == 1) AccentGreen.copy(alpha = 0.16f) else SurfaceDark,
                        border = BorderStroke(
                            1.dp,
                            if (idx == 0 && line.rank == 1) AccentGreen.copy(alpha = 0.55f) else BorderSubtle
                        ),
                        modifier = Modifier.testTag("pv_move_${line.rank}_$idx")
                    ) {
                        Text(
                            text = san,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontFeatureSettings = "tnum"
                            ),
                            fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (idx == 0 && line.rank == 1) AccentGreen else TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
