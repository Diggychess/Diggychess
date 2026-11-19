package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.PieceColor
import com.example.ui.components.ChessBoardView
import com.example.ui.theme.*
import com.example.ui.viewmodel.OpeningTrainerUiState

@Composable
fun OpeningTrainerScreen(
    state: OpeningTrainerUiState,
    onSelectRepertoire: (String) -> Unit,
    onSquareClick: (String) -> Unit,
    onHint: () -> Unit,
    onRestart: () -> Unit,
    onNextRepertoire: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeRep = state.repertoires.find { it.id == state.selectedId } ?: state.repertoires.first()
    val progressFrac = if (state.totalSteps > 0) (state.stepIndex.toFloat() / state.totalSteps).coerceIn(0f, 1f) else 0f

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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Opening Trainer", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Text(
                    text = "Drill essential White and Black repertoires with instant move coaching.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Surface(
                color = AccentGreen.copy(alpha = 0.16f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Clean drill streak", tint = AccentGreenHover, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${state.streak}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreenHover
                    )
                }
            }
        }

        // Repertoire Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.repertoires.forEach { rep ->
                val selected = rep.id == state.selectedId
                FilterChip(
                    selected = selected,
                    onClick = { onSelectRepertoire(rep.id) },
                    label = {
                        val side = if (rep.playerColor == PieceColor.WHITE) "White" else "Black"
                        Text("${rep.eco} ${rep.title} ($side)", fontWeight = FontWeight.SemiBold)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                        selectedLabelColor = AccentGreenHover
                    ),
                    modifier = Modifier.testTag("trainer_rep_${rep.id}")
                )
            }
        }

        // Interactive Drill Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(activeRep.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(activeRep.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Text(
                        text = "${state.stepIndex} / ${state.totalSteps}",
                        style = MaterialTheme.typography.labelLarge,
                        color = AccentGreenHover,
                        fontWeight = FontWeight.Bold
                    )
                }

                LinearProgressIndicator(
                    progress = { progressFrac },
                    color = AccentGreen,
                    trackColor = BorderSubtle,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp))
                )

                ChessBoardView(
                    fen = state.fen,
                    boardTheme = state.boardTheme,
                    flipped = state.flipped,
                    selectedSquare = state.selectedSquare,
                    lastMoveSquares = state.lastMoveSquares,
                    wrongMoveSquares = state.wrongMoveSquares,
                    hintSquares = state.hintSquares,
                    legalTargets = state.legalTargets,
                    onSquareClick = onSquareClick,
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.feedbackText.isNotEmpty()) {
                    val fbColor = when (state.feedbackOk) {
                        true -> AccentGreenHover
                        false -> ClassBlunder
                        null -> StudioCyan
                    }
                    Text(
                        text = state.feedbackText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = fbColor,
                        modifier = Modifier.testTag("trainer_feedback")
                    )
                }

                if (state.coachNote.isNotEmpty()) {
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Idea: ${state.coachNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onHint,
                        enabled = !state.isLineComplete,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("trainer_hint")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hint", color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = onRestart,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("trainer_restart")
                    ) {
                        Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart", color = TextPrimary)
                    }
                    Button(
                        onClick = onNextRepertoire,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("trainer_next")
                    ) {
                        Text("Next line", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
