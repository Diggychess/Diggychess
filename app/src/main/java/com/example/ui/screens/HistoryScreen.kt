package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.HistoryUiState
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onSelectSideFilter: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenGame: (SavedGameEntry) -> Unit,
    onRequestDelete: (String) -> Unit,
    onConfirmDelete: (String) -> Unit,
    onCancelDelete: () -> Unit,
    onGoToAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = DiggySpacing.s2, vertical = DiggySpacing.s2),
            verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
        ) {
            item(key = "history_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    DiggyButton(
                        text = "Analyse a New Game",
                        onClick = onGoToAnalyze,
                        icon = Icons.Rounded.Analytics,
                        testTag = "history_analyze_new_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Your Report Card
            state.report?.let { rep ->
                item(key = "history_report") {
                    DiggyCard(
                        modifier = Modifier.animateItem(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Performance Report",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("both" to "Both", "w" to "White", "b" to "Black").forEach { (key, label) ->
                                    DiggyChip(
                                        label = label,
                                        selected = state.sideFilter == key,
                                        onClick = { onSelectSideFilter(key) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ReportMetric(
                                    value = String.format(Locale.US, "%.1f", rep.blundersPerGame),
                                    label = "Blunders / game",
                                    accent = FeedbackDangerRed,
                                    modifier = Modifier.weight(1f)
                                )
                                ReportMetric(
                                    value = String.format(Locale.US, "%.1f", rep.mistakesPerGame),
                                    label = "Mistakes / game",
                                    accent = ClassMistake,
                                    modifier = Modifier.weight(1f)
                                )
                                ReportMetric(
                                    value = String.format(Locale.US, "%.1f", rep.inaccuraciesPerGame),
                                    label = "Inaccuracies",
                                    accent = AccentGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            val phaseName = when (rep.worstPhaseIndex) {
                                0 -> "opening (first 10 moves)"
                                1 -> "middlegame"
                                else -> "endgame"
                            }
                            val tip = if (!rep.hasAnyMistake) {
                                "No mistakes or blunders recorded yet — great play!"
                            } else {
                                "Most mistakes happen in the $phaseName. Drill \"My game mistakes\" in Puzzles to sharpen up."
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
                    DiggyEmptyState(
                        title = "No analysed games yet",
                        subtitle = "Games you analyse appear here and are stored locally on your device.",
                        icon = Icons.Rounded.History,
                        actionLabel = "Analyse Your First Game",
                        onAction = onGoToAnalyze
                    )
                }
            } else {
                items(state.games, key = { "game_${it.id}" }) { game ->
                    val formattedDate = remember(game.timestamp) {
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                            .format(Date(game.timestamp))
                    }
                    DiggyCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
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
                                }
                            }

                            if (state.pendingDeleteId == game.id) {
                                Text(
                                    text = "Delete this game from your history?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FeedbackDangerRed
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    DiggyButton(
                                        text = "Delete",
                                        onClick = { onConfirmDelete(game.id) },
                                        variant = DiggyButtonVariant.DANGER,
                                        icon = Icons.Rounded.Delete,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DiggyButton(
                                        text = "Cancel",
                                        onClick = onCancelDelete,
                                        variant = DiggyButtonVariant.OUTLINED,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    DiggyButton(
                                        text = "Open Analysis",
                                        onClick = { onOpenGame(game) },
                                        variant = DiggyButtonVariant.PRIMARY,
                                        icon = Icons.Rounded.Analytics,
                                        testTag = "open_history_${game.id}",
                                        modifier = Modifier.weight(1f)
                                    )
                                    DiggyButton(
                                        text = "Delete",
                                        onClick = { onRequestDelete(game.id) },
                                        variant = DiggyButtonVariant.OUTLINED,
                                        icon = Icons.Rounded.DeleteOutline,
                                        testTag = "delete_history_${game.id}"
                                    )
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
private fun ReportMetric(
    value: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = BgDark,
        shape = RoundedCornerShape(DiggySpacing.controlRadius),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
