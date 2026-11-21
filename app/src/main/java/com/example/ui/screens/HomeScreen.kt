package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
        HomeHubTile("analyse", "Analyse Game", "Chess.com & Lichess import", Icons.Filled.Analytics, "home_tile_analyse"),
        HomeHubTile("puzzles", "Puzzles", "Rated tactics & hints", Icons.Filled.Extension, "home_tile_puzzles"),
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Diggy Chess Royal Gold & Obsidian Floating Hero Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated),
            border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.50f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(22.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_diggy_hero),
                    contentDescription = "Diggy Chess royal studio board",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xF0080A0F),
                                    Color(0xD60C0F16),
                                    Color(0x66080A0F)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = AccentGreen.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = "DIGGY CHESS STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentGreenHover,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Command the board.\nMaster every line.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFF6F2E9),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Import Chess.com & Lichess games by username, train rated tactics with instant feedback, and challenge Stockfish.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD0D5DD)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onNavigate("play") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGreen,
                                contentColor = OnAccentDark
                            ),
                            shape = RoundedCornerShape(14.dp),
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
                            border = BorderStroke(1.dp, AccentGreenHover),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0x8810131C),
                                contentColor = AccentGreenHover
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("home_daily_puzzle")
                        ) {
                            Icon(
                                Icons.Outlined.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = AccentGreenHover
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Daily Puzzle", color = AccentGreenHover, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Session Overview Floating Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.94f)),
            border = BorderStroke(1.dp, LineSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("YOUR TRAINING PROGRESS", style = MaterialTheme.typography.titleSmall, color = AccentGreenHover)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeMetricChip("Puzzle ELO", state.puzzleRating.toString(), Modifier.weight(1f))
                    HomeMetricChip("Bot Games", state.userProfile.gamesPlayedBot.toString(), Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeMetricChip("Opening Streak", state.openingStreak.toString(), Modifier.weight(1f))
                    HomeMetricChip("Lessons Done", "${state.completedLessonsCount}/${state.totalLessonsCount}", Modifier.weight(1f))
                }
                state.lastAnalysedTitle?.let { lastTitle ->
                    Surface(
                        onClick = { onNavigate("history") },
                        shape = RoundedCornerShape(14.dp),
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

        // Studio Feature Floating Cards
        Text(
            text = "ROYAL STUDIO TOOLS",
            style = MaterialTheme.typography.titleSmall,
            color = AccentGreenHover
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            hubTiles.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { tile ->
                        Surface(
                            onClick = { onNavigate(tile.route) },
                            color = CardDark.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, LineSubtle),
                            shadowElevation = 4.dp,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 92.dp)
                                .testTag(tile.testTag)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(AccentGreen.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tile.icon,
                                        contentDescription = tile.title,
                                        tint = AccentGreenHover,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Text(
                                    text = tile.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 14.sp,
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
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGreenHover
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
