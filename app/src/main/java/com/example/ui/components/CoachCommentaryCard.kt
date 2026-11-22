package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.AnalyzedPly
import com.example.chess.MoveClass
import com.example.chess.MoveExplainer
import com.example.ui.theme.*
import com.example.ui.viewmodel.VariationBranch

private fun classificationIcon(cls: MoveClass, isGreatMove: Boolean): ImageVector {
    if (isGreatMove && cls == MoveClass.BEST) return Icons.Rounded.Bolt
    return when (cls) {
        MoveClass.BRILLIANT -> Icons.Rounded.AutoAwesome
        MoveClass.BEST -> Icons.Rounded.Star
        MoveClass.GOOD -> Icons.Rounded.CheckCircle
        MoveClass.INACCURACY -> Icons.Rounded.ReportProblem
        MoveClass.MISTAKE -> Icons.Rounded.ErrorOutline
        MoveClass.BLUNDER -> Icons.Rounded.Dangerous
    }
}

@Composable
fun CoachCommentaryCard(
    activePly: AnalyzedPly?,
    currentPly: Int,
    isShowingBest: Boolean,
    variation: VariationBranch?,
    onToggleBestMove: () -> Unit,
    onPracticeMistake: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        variation != null -> StudioCyan.copy(alpha = 0.55f)
        activePly != null -> activePly.badgeColor.copy(alpha = 0.60f)
        else -> BorderSubtle
    }

    DiggyCard(
        elevated = false,
        borderColor = borderColor,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (variation != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StudioCyan.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = "Variation explorer",
                            tint = StudioCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "What-If Variation",
                            style = MaterialTheme.typography.titleMedium,
                            color = StudioCyan
                        )
                        val evalStr = MoveExplainer.formatPlyEval(variation.liveEvalCp, variation.liveEvalMate)
                        Text(
                            text = if (variation.isEvaluating) {
                                "Evaluating variation…"
                            } else {
                                "Live eval $evalStr · Play moves on board or return to game."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            } else if (activePly == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentGreen.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = "Coach review",
                            tint = AccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Starting position. Step through moves, tap an engine line, or move pieces on the board to explore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            } else {
                val moveNum = (currentPly - 1) / 2 + 1
                val dots = if (currentPly % 2 == 1) ". " else "... "
                val classIcon = classificationIcon(activePly.moveClass, activePly.isGreatMove)

                // Move classification header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(activePly.badgeColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = classIcon,
                            contentDescription = activePly.badgeLabel,
                            tint = activePly.badgeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$moveNum$dots${activePly.san}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Surface(
                                color = activePly.badgeColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, activePly.badgeColor.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = "${activePly.badgeGlyph} ${activePly.badgeLabel}",
                                    color = activePly.badgeColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                color = CardSurfaceElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    text = MoveExplainer.formatPlyEval(activePly.evalAfter, activePly.mate),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (activePly.coachHeadline.isNotBlank()) {
                            Text(
                                text = activePly.coachHeadline,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = activePly.badgeColor
                            )
                        }
                    }
                }

                if (activePly.tacticalTags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        activePly.tacticalTags.forEach { tag ->
                            Surface(
                                color = CardSurfaceElevated,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (isShowingBest) {
                    Surface(
                        color = ClassBest.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ClassBest.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Best move: ${activePly.bestSan} (green arrow) vs played (red arrow)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ClassBest
                            )
                            Text(
                                text = activePly.bestExplanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                } else {
                    Text(
                        text = activePly.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }

                val isSuboptimal = activePly.moveClass == MoveClass.INACCURACY ||
                    activePly.moveClass == MoveClass.MISTAKE ||
                    activePly.moveClass == MoveClass.BLUNDER

                if (activePly.bestFrom != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiggyButton(
                            text = if (isShowingBest) "Back to Move" else "Show Best",
                            onClick = onToggleBestMove,
                            variant = DiggyButtonVariant.OUTLINED,
                            icon = Icons.Rounded.Lightbulb,
                            testTag = "toggle_best_move",
                            modifier = Modifier.weight(1f)
                        )
                        if (isSuboptimal) {
                            DiggyButton(
                                text = "Retry Move",
                                onClick = { onPracticeMistake(activePly.index) },
                                variant = DiggyButtonVariant.PRIMARY,
                                icon = Icons.Rounded.FitnessCenter,
                                testTag = "coach_practice_mistake",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
