package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChessLessonsUiState

@Composable
fun ChessLessonsScreen(
    state: ChessLessonsUiState,
    onOpenLesson: (String) -> Unit,
    onCloseLesson: () -> Unit,
    onSquareClick: (String) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onHint: () -> Unit,
    onRetry: () -> Unit,
    onNextStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLesson = state.lessons.find { it.id == state.activeLessonId }
    if (activeLesson != null) {
        BackHandler { onCloseLesson() }
        val step = activeLesson.steps.getOrNull(state.stepIndex) ?: return
        val progressFrac = ((state.stepIndex + if (state.stepSolved) 1 else 0).toFloat() / activeLesson.steps.size).coerceIn(0f, 1f)

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DiggySpacing.s2, vertical = DiggySpacing.s2),
            verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCloseLesson,
                    modifier = Modifier
                        .heightIn(min = DiggySpacing.touchTargetMin)
                        .testTag("lesson_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to lessons", tint = AccentGreen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("All Lessons", color = AccentGreen, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "Step ${state.stepIndex + 1} / ${activeLesson.steps.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
            }

            DiggyCard(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(activeLesson.title, style = MaterialTheme.typography.labelMedium, color = StudioCyan)
                    Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(step.prompt, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)

                    LinearProgressIndicator(
                        progress = { progressFrac },
                        color = AccentGreen,
                        trackColor = BorderSubtle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                    )

                    ChessBoardView(
                        fen = state.fen,
                        boardTheme = state.boardTheme,
                        flipped = false,
                        selectedSquare = state.selectedSquare,
                        lastMoveSquares = state.lastMoveSquares,
                        wrongMoveSquares = state.wrongMoveSquares,
                        hintSquares = state.hintSquares,
                        legalTargets = state.legalTargets,
                        onSquareClick = onSquareClick,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.feedbackText.isNotEmpty()) {
                        val color = when (state.feedbackOk) {
                            true -> FeedbackCorrectGreen
                            false -> FeedbackDangerRed
                            null -> StudioCyan
                        }
                        Surface(
                            color = BgDark,
                            shape = RoundedCornerShape(DiggySpacing.controlRadius),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lesson_feedback")
                        ) {
                            Text(
                                text = state.feedbackText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiggyButton(
                            text = "Hint",
                            onClick = onHint,
                            enabled = !state.stepSolved,
                            variant = DiggyButtonVariant.OUTLINED,
                            icon = Icons.AutoMirrored.Rounded.HelpOutline,
                            testTag = "lesson_hint",
                            modifier = Modifier.weight(1f)
                        )
                        DiggyButton(
                            text = "Reset",
                            onClick = onRetry,
                            variant = DiggyButtonVariant.OUTLINED,
                            icon = Icons.Rounded.Replay,
                            testTag = "lesson_retry",
                            modifier = Modifier.weight(1f)
                        )
                        DiggyButton(
                            text = if (state.lessonFinished) "Finish" else "Next",
                            onClick = onNextStep,
                            enabled = state.stepSolved,
                            variant = DiggyButtonVariant.PRIMARY,
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            testTag = "lesson_next_step",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        state.pendingPromotion?.let {
            PromotionPickerDialog(
                color = PieceColor.WHITE,
                onSelect = onSelectPromotion,
                onDismiss = onDismissPromotion
            )
        }
        return
    }

    // Lesson Curriculum List
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DiggySpacing.s2, vertical = DiggySpacing.s2),
        verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Chess Lessons", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                text = "Interactive guided board exercises covering forks, pins, skewers, checkmates, opening strategy, and endgames.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        DiggyCard(
            elevated = true,
            borderColor = AccentGreen.copy(alpha = 0.4f),
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Curriculum Progress: ${state.completedLessonIds.size} / ${state.lessons.size} completed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
                LinearProgressIndicator(
                    progress = { (state.completedLessonIds.size.toFloat() / state.lessons.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    color = AccentGreen,
                    trackColor = BorderSubtle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                )
            }
        }

        state.lessons.forEach { lesson ->
            val completed = lesson.id in state.completedLessonIds
            DiggyCard(
                onClick = { onOpenLesson(lesson.id) },
                borderColor = if (completed) FeedbackCorrectGreen.copy(alpha = 0.55f) else BorderSubtle,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("lesson_card_${lesson.id}")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.School,
                        contentDescription = if (completed) "Completed" else "Start lesson",
                        tint = if (completed) FeedbackCorrectGreen else StudioCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(lesson.levelTag, style = MaterialTheme.typography.labelSmall, color = StudioCyan)
                        Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(lesson.summary, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text(
                            text = "${lesson.steps.size} interactive challenges",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (completed) FeedbackCorrectGreen else TextMuted
                        )
                    }
                }
            }
        }
    }
}
