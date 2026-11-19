package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.HomeUiState

private data class HomeHubTile(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    onNavigate: (String) -> Unit,
    onStartDailyPuzzle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hubTiles = listOf(
        HomeHubTile("play", "Play Bot", "Engine levels & live eval", Icons.Filled.SmartToy, "home_tile_play"),
        HomeHubTile("analyse", "Analyse Game", "3-line review & importer", Icons.Filled.Analytics, "home_tile_analyse"),
        HomeHubTile("puzzles", "Puzzles", "Rated tactics & rush", Icons.Filled.Extension, "home_tile_puzzles"),
        HomeHubTile("friends", "Friendly Match", "Invite links, Wi-Fi & Pass", Icons.Filled.Group, "home_tile_friends"),
        HomeHubTile("explorer", "Opening Explorer", "Live DB stats & lines", Icons.Filled.Explore, "home_tile_explorer"),
        HomeHubTile("trainer", "Opening Trainer", "ECO repertoire drills", Icons.Filled.FitnessCenter, "home_tile_trainer"),
        HomeHubTile("lessons", "Chess Lessons", "Interactive masterclasses", Icons.Filled.School, "home_tile_lessons"),
        HomeHubTile("history", "Game History", "Local report & archives", Icons.Filled.History, "home_tile_history"),
        HomeHubTile("settings", "Settings", "Theme, board & backups", Icons.Filled.Settings, "home_tile_settings")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome Hero Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated),
            border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "KNIGHTSCOPE CHESS STUDIO",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentGreenHover,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Play sharper. Study deeper.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Train against Stockfish, review games with 3-line Coach explanations, drill openings, and challenge friends with zero login.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onNavigate("play") },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("home_quick_play")
                    ) {
                        Icon(Icons.Filled.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play Bot", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onStartDailyPuzzle,
                        border = BorderStroke(1.dp, AccentGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("home_daily_puzzle")
                    ) {
                        Icon(Icons.Outlined.Bolt, contentDescription = null, modifier = Modifier.size(18.dp), tint = AccentGreenHover)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Daily Puzzle", color = AccentGreenHover, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Session Overview Metrics
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("YOUR TRAINING PROGRESS", style = MaterialTheme.typography.titleSmall, color = TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeMetricChip("Puzzle ELO", state.puzzleRating.toString(), Modifier.weight(1f))
                    HomeMetricChip("Bot Games", state.userProfile.gamesPlayedBot.toString(), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeMetricChip("Opening Streak", state.openingStreak.toString(), Modifier.weight(1f))
                    HomeMetricChip("Lessons Done", "${state.completedLessonsCount}/${state.totalLessonsCount}", Modifier.weight(1f))
                }
                state.lastAnalysedTitle?.let { lastTitle ->
                    Surface(
                        onClick = { onNavigate("history") },
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LAST ANALYSED GAME", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text(
                                    text = lastTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${state.recentGamesCount} saved",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentGreenHover
                            )
                        }
                    }
                }
            }
        }

        // Full Studio Grid
        Text("ALL CHESS TOOLS", style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            hubTiles.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { tile ->
                        Surface(
                            onClick = { onNavigate(tile.route) },
                            color = CardDark.copy(alpha = 0.90f),
                            border = BorderStroke(1.dp, LineSubtle),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 84.dp)
                                .testTag(tile.testTag)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tile.icon,
                                    contentDescription = tile.title,
                                    tint = AccentGreenHover,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = tile.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tile.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeMetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentGreenHover)
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
