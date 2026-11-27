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
import com.example.service.AiInsightState
import com.example.service.AiMoveInsight
import com.example.ui.theme.*
import com.example.ui.viewmodel.VariationBranch

private fun classificationIcon(cls: MoveClass, isGreatMove: Boolean): ImageVector {
    if (isGreatMove && cls == MoveClass.BEST) return Icons.Rounded.Bolt
    return when (cls) {
        MoveClass.BRILLIANT -> Icons.Rounded.AutoAwesome
        MoveClass.BEST -> Icons.Rounded.Star
        MoveClass.EXCELLENT -> Icons.Rounded.DoneAll
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
    moveInsightState: AiInsightState<AiMoveInsight> = AiInsightState.Idle,
    onRequestMoveInsight: (Int) -> Unit = {},
    onUseOfflineMoveInsight: (Int) -> Unit = {},
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
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = FontWeight.Bold,
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
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontFeatureSettings = "tnum"
                                    ),
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
                                text = "Try again",
                                onClick = { onPracticeMistake(activePly.index) },
                                variant = DiggyButtonVariant.PRIMARY,
                                icon = Icons.Rounded.FitnessCenter,
                                testTag = "coach_practice_mistake",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // AI Coach Deep Explanation (grounded in Stockfish evaluations & top lines)
                HorizontalDivider(color = LineSubtle)
                AiMoveInsightSection(
                    plyIndex = activePly.index,
                    state = moveInsightState,
                    onRequest = onRequestMoveInsight,
                    onUseOffline = onUseOfflineMoveInsight
                )
            }
        }
    }
}

@Composable
private fun AiMoveInsightSection(
    plyIndex: Int,
    state: AiInsightState<AiMoveInsight>,
    onRequest: (Int) -> Unit,
    onUseOffline: (Int) -> Unit
) {
    when (state) {
        is AiInsightState.Idle -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Need a deeper human explanation?",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                DiggyChip(
                    label = "Ask AI Coach",
                    selected = false,
                    onClick = { onRequest(plyIndex) },
                    icon = Icons.Rounded.AutoAwesome,
                    accentColor = StudioCyan,
                    testTag = "ask_ai_move_insight_$plyIndex"
                )
            }
        }
        is AiInsightState.Loading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_move_insight_loading")
            ) {
                CircularProgressIndicator(
                    color = StudioCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "AI Chess Coach is reviewing Stockfish lines…",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioCyan
                )
            }
        }
        is AiInsightState.Success -> {
            val data = state.data
            Surface(
                color = CardSurfaceElevated,
                shape = RoundedCornerShape(DiggySpacing.controlRadius),
                border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_move_insight_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "AI Coach Insight",
                                tint = StudioCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "AI Coach Insight",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = StudioCyan
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = StudioCyan.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = if (data.isFromCache) "${data.providerLabel} · Cached" else data.providerLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioCyan,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            TextButton(
                                onClick = { onRequest(plyIndex) },
                                modifier = Modifier
                                    .heightIn(min = DiggySpacing.touchTargetMin)
                                    .testTag("refresh_ai_move_insight")
                            ) {
                                Text("Refresh", color = AccentGreen, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Text(
                        text = data.headline,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    AiCoachBullet("Why", data.whyGoodOrBad, TextPrimary)
                    AiCoachBullet("Stronger idea", data.whyBestIsStronger, AccentGreenHover)
                    AiCoachBullet("Missed motif", data.whatWasMissed, ClassInaccuracy)
                    AiCoachBullet("Coach tip", data.improvementTip, TextMuted)
                }
            }
        }
        is AiInsightState.Error -> {
            Surface(
                color = CardSurfaceElevated,
                shape = RoundedCornerShape(DiggySpacing.controlRadius),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ClassMistake
                    )
                    state.fallbackMessage?.let { fb ->
                        Text(
                            text = fb,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiggyChip(
                            label = "Retry AI",
                            selected = false,
                            onClick = { onRequest(plyIndex) },
                            icon = Icons.Rounded.Refresh,
                            accentColor = StudioCyan,
                            testTag = "retry_ai_move_insight"
                        )
                        DiggyChip(
                            label = "Offline Coach",
                            selected = false,
                            onClick = { onUseOffline(plyIndex) },
                            icon = Icons.Rounded.MenuBook,
                            accentColor = AccentGreen,
                            testTag = "offline_ai_move_insight"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiCoachBullet(label: String, body: String, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent,
            letterSpacing = 0.6.sp
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary
        )
    }
}
