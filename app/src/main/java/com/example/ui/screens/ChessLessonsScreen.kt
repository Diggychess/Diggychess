package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
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
import com.example.ui.components.ChessBoardView
import com.example.ui.components.PromotionPickerDialog
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCloseLesson,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("lesson_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to lessons", tint = AccentGreenHover)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("All Lessons", color = AccentGreenHover, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = "Step ${state.stepIndex + 1} / ${activeLesson.steps.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, LineSubtle),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(activeLesson.title, style = MaterialTheme.typography.labelMedium, color = StudioCyan)
                    Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(step.prompt, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)

                    LinearProgressIndicator(
                        progress = { progressFrac },
                        color = AccentGreen,
                        trackColor = BorderSubtle,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
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
                            true -> AccentGreenHover
                            false -> ClassBlunder
                            null -> StudioCyan
                        }
                        Surface(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth().testTag("lesson_feedback")
                        ) {
                            Text(
                                text = state.feedbackText,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onHint,
                            enabled = !state.stepSolved,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("lesson_hint")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hint", color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = onRetry,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("lesson_retry")
                        ) {
                            Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", color = TextPrimary)
                        }
                        Button(
                            onClick = onNextStep,
                            enabled = state.stepSolved,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("lesson_next_step")
                        ) {
                            Text(if (state.lessonFinished) "Finish" else "Next", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Chess Lessons", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                text = "Interactive guided board exercises covering forks, pins, skewers, checkmates, opening strategy, and endgames.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Surface(
            color = CardSurfaceElevated,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Curriculum Progress: ${state.completedLessonIds.size} / ${state.lessons.size} completed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreenHover
                )
                LinearProgressIndicator(
                    progress = { (state.completedLessonIds.size.toFloat() / state.lessons.size.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    color = AccentGreen,
                    trackColor = BorderSubtle,
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp))
                )
            }
        }

        state.lessons.forEach { lesson ->
            val completed = lesson.id in state.completedLessonIds
            Card(
                onClick = { onOpenLesson(lesson.id) },
                colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.90f)),
                border = BorderStroke(1.dp, if (completed) AccentGreen.copy(alpha = 0.5f) else LineSubtle),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("lesson_card_${lesson.id}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Filled.School,
                        contentDescription = if (completed) "Completed" else "Start lesson",
                        tint = if (completed) AccentGreenHover else StudioCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(lesson.levelTag, style = MaterialTheme.typography.labelSmall, color = StudioCyan)
                        Text(lesson.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(lesson.summary, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text(
                            text = "${lesson.steps.size} interactive challenges",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (completed) AccentGreenHover else TextMuted
                        )
                    }
                }
            }
        }
    }
}
