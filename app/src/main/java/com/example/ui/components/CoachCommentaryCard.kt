package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.School
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
import com.example.chess.AnalyzedPly
import com.example.chess.MoveClass
import com.example.chess.MoveExplainer
import com.example.ui.theme.*
import com.example.ui.viewmodel.VariationBranch

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
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                variation != null -> StudioCyan.copy(alpha = 0.55f)
                activePly != null -> activePly.badgeColor.copy(alpha = 0.55f)
                else -> BorderSubtle
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (variation != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(StudioCyan.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "Coach variation view",
                            tint = StudioCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "What-If Variation Explorer",
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioCyan
                        )
                        val evalStr = MoveExplainer.formatPlyEval(variation.liveEvalCp, variation.liveEvalMate)
                        Text(
                            text = if (variation.isEvaluating) {
                                "Evaluating variation…"
                            } else {
                                "Live Stockfish eval: $evalStr · Keep playing moves or tap Back to game."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }
            } else if (activePly == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AccentGreen.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "Coach",
                            tint = AccentGreenHover,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Start of game. Step through moves, tap any engine line, or move pieces on the board to explore variations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            } else {
                val moveNum = (currentPly - 1) / 2 + 1
                val dots = if (currentPly % 2 == 1) ". " else "... "

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(activePly.badgeColor.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activePly.badgeGlyph,
                            color = activePly.badgeColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
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
                                color = activePly.badgeColor,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = activePly.badgeLabel,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = MoveExplainer.formatPlyEval(activePly.evalAfter, activePly.mate),
                                fontSize = 12.sp,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = TextMuted
                            )
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

                // Tactical tags row
                if (activePly.tacticalTags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        activePly.tacticalTags.forEach { tag ->
                            Surface(
                                color = CardDark,
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                if (isShowingBest) {
                    Text(
                        text = "Green arrow = best move ${activePly.bestSan}, red arrow = move played.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGreenHover
                    )
                    Text(
                        text = activePly.bestExplanation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
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
                        OutlinedButton(
                            onClick = onToggleBestMove,
                            border = BorderStroke(1.dp, AccentGreen),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("toggle_best_move")
                        ) {
                            Icon(
                                Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = AccentGreenHover,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isShowingBest) "Back" else "Show best",
                                color = AccentGreenHover,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isSuboptimal) {
                            Button(
                                onClick = { onPracticeMistake(activePly.index) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StudioCyan,
                                    contentColor = OnAccentDark
                                ),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("coach_practice_mistake")
                            ) {
                                Icon(
                                    Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Practice retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
