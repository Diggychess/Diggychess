package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.EngineVariation
import com.example.ui.theme.*
import com.example.ui.viewmodel.VariationBranch

/**
 * Compact Variation Explorer Bar shown only when the user explores a board variation.
 * Note: The Top 3 Engine Lines card has been removed as requested.
 */
@Composable
fun CompactVariationBar(
    variation: VariationBranch,
    onStepVariation: (Int) -> Unit,
    onUndoVariation: () -> Unit,
    onExitVariation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioCyan.copy(alpha = 0.10f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth()
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

/**
 * Replaces the former Top 3 Engine Moves card.
 * When [variation] is active, renders [CompactVariationBar].
 * When [variation] is null, renders nothing (zero blank space).
 */
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
    if (variation != null) {
        CompactVariationBar(
            variation = variation,
            onStepVariation = onStepVariation,
            onUndoVariation = onUndoVariation,
            onExitVariation = onExitVariation,
            modifier = modifier
        )
    }
}
