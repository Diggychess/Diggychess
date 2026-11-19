package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.SavedGameEntry
import com.example.ui.theme.*
import com.example.ui.viewmodel.HistoryUiState
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onSelectSideFilter: (String) -> Unit,
    onOpenGame: (SavedGameEntry) -> Unit,
    onRequestDelete: (String) -> Unit,
    onConfirmDelete: (String) -> Unit,
    onCancelDelete: () -> Unit,
    onGoToAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "history_header") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.history_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.history_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Button(
                    onClick = onGoToAnalyze,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("history_analyze_new_button")
                ) {
                    Text("Analyse a new game", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Your Report Card
        state.report?.let { rep ->
            item(key = "history_report") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, LineSubtle),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.animateItem()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("YOUR REPORT", style = MaterialTheme.typography.titleSmall, color = TextMuted)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("both" to "Both sides", "w" to "I play White", "b" to "I play Black").forEach { (key, label) ->
                                val active = state.sideFilter == key
                                FilterChip(
                                    selected = active,
                                    onClick = { onSelectSideFilter(key) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentGreen.copy(alpha = 0.18f),
                                        selectedLabelColor = AccentGreenHover
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ReportMetric(
                                value = String.format(Locale.US, "%.1f", rep.blundersPerGame),
                                label = "blunders / game",
                                modifier = Modifier.weight(1f)
                            )
                            ReportMetric(
                                value = String.format(Locale.US, "%.1f", rep.mistakesPerGame),
                                label = "mistakes / game",
                                modifier = Modifier.weight(1f)
                            )
                            ReportMetric(
                                value = String.format(Locale.US, "%.1f", rep.inaccuraciesPerGame),
                                label = "inaccuracies / game",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val phaseName = when (rep.worstPhaseIndex) {
                            0 -> "opening (first 10 moves)"
                            1 -> "middlegame"
                            else -> "late game"
                        }
                        val tip = if (!rep.hasAnyMistake) {
                            "No mistakes or blunders recorded yet - great play!"
                        } else {
                            "Most of your mistakes and blunders happen in the $phaseName. Practise \"My game mistakes\" in Puzzles to fix them."
                        }
                        Text(
                            text = "$tip Based on ${rep.gameCount} analysed game${if (rep.gameCount > 1) "s" else ""}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Games List or Empty State
        if (state.games.isEmpty()) {
            item(key = "history_empty") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardDark.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No analysed games yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Games you analyse will appear here and sync to your account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(state.games, key = { "game_${it.id}" }) { game ->
                val formattedDate = remember(game.timestamp) {
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(game.timestamp))
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, LineSubtle),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = game.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "$formattedDate" + if (game.moves > 0) " · ${game.moves} moves" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        if (state.pendingDeleteId == game.id) {
                            Text(
                                text = "Delete this entry from your history?",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onConfirmDelete(game.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ClassBlunder, contentColor = TextPrimary),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Text("Yes, delete", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = onCancelDelete,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                ) {
                                    Text("Cancel", color = TextPrimary)
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onOpenGame(game) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("open_history_${game.id}")
                                ) {
                                    Text("Open Analysis", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onRequestDelete(game.id) },
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("delete_history_${game.id}")
                                ) {
                                    Text("Delete", color = TextPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, LineSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(label, fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)
        }
    }
}
