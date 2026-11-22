package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import com.example.R
import com.example.ui.components.DiggyButton
import com.example.ui.components.DiggyButtonVariant
import com.example.ui.components.DiggyCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.HomeUiState

private data class HomeQuickTile(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    hasActivePlayGame: Boolean,
    onNavigate: (String) -> Unit,
    onStartDailyPuzzle: () -> Unit,
    onContinuePlay: () -> Unit,
    onAnalysePgn: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickTiles = listOf(
        HomeQuickTile("play", "Play Bot", "Stockfish & timed modes", Icons.Rounded.SmartToy, "home_tile_play"),
        HomeQuickTile("analyse", "Analyse Game", "Chess.com & Lichess import", Icons.Rounded.Analytics, "home_tile_analyse"),
        HomeQuickTile("puzzles", "Puzzles", "Tactics & hint trainer", Icons.Rounded.Extension, "home_tile_puzzles"),
        HomeQuickTile("friends", "Friendly Match", "Invite links & Wi-Fi", Icons.Rounded.Group, "home_tile_friends"),
        HomeQuickTile("explorer", "Opening Explorer", "Live DB win rates", Icons.Rounded.Explore, "home_tile_explorer"),
        HomeQuickTile("trainer", "Opening Trainer", "ECO repertoire drills", Icons.Rounded.FitnessCenter, "home_tile_trainer"),
        HomeQuickTile("lessons", "Chess Lessons", "Interactive masterclasses", Icons.Rounded.School, "home_tile_lessons"),
        HomeQuickTile("history", "Game History", "Local reports & games", Icons.Rounded.History, "home_tile_history")
    )

    val totalGames = state.userProfile.gamesPlayedBot + state.recentGamesCount
    val bestStreak = maxOf(state.userProfile.winStreak, state.openingStreak)
    val latestPlay = state.recentPlayGames.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DiggySpacing.s2, vertical = DiggySpacing.s2),
        verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
    ) {
        // Greeting Header
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Welcome back, ${state.userProfile.displayName}",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Ready for your next game, review, or tactical warm-up.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // Stats Row (Rating, Streak, Games Played)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DiggySpacing.s1)
        ) {
            HomeStatCard(
                label = "Rating",
                value = state.puzzleRating.toString(),
                icon = Icons.Rounded.EmojiEvents,
                accentColor = AccentGreen,
                modifier = Modifier.weight(1f)
            )
            HomeStatCard(
                label = "Streak",
                value = "$bestStreak",
                icon = Icons.Rounded.LocalFireDepartment,
                accentColor = ClassMistake,
                modifier = Modifier.weight(1f)
            )
            HomeStatCard(
                label = "Games",
                value = "$totalGames",
                icon = Icons.Rounded.SportsEsports,
                accentColor = StudioCyan,
                modifier = Modifier.weight(1f)
            )
        }

        // Daily Puzzle Hero Card
        Surface(
            color = CardSurfaceElevated,
            shape = RoundedCornerShape(DiggySpacing.cardRadius),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.45f)),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_diggy_hero),
                    contentDescription = "Daily tactical board",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xF2121212),
                                    Color(0xDA121212),
                                    Color(0x88121212)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Daily Puzzle",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentGreen
                        )
                    }
                    Text(
                        text = "Sharpen your tactical vision with today's hand-picked challenge.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFFFFF)
                    )
                    DiggyButton(
                        text = "Solve Daily Puzzle",
                        onClick = onStartDailyPuzzle,
                        variant = DiggyButtonVariant.PRIMARY,
                        icon = Icons.Rounded.PlayArrow,
                        testTag = "home_daily_puzzle",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Continue Game Card
        DiggyCard(
            elevated = false,
            borderColor = if (hasActivePlayGame) AccentGreen.copy(alpha = 0.55f) else BorderSubtle,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = StudioCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Continue Game",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = if (hasActivePlayGame) "In progress" else if (latestPlay != null) latestPlay.result else "New match",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasActivePlayGame) AccentGreen else TextMuted
                    )
                }

                when {
                    hasActivePlayGame -> {
                        Text(
                            text = "Your match against Stockfish is waiting on the board.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        DiggyButton(
                            text = "Resume Match",
                            onClick = onContinuePlay,
                            variant = DiggyButtonVariant.PRIMARY,
                            icon = Icons.Rounded.SmartToy,
                            testTag = "home_continue_active",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    latestPlay != null -> {
                        Text(
                            text = "${latestPlay.title} · ${latestPlay.timeControlLabel} · ${latestPlay.movesCount} moves",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DiggyButton(
                                text = "New Game",
                                onClick = { onNavigate("play") },
                                variant = DiggyButtonVariant.PRIMARY,
                                icon = Icons.Rounded.SmartToy,
                                testTag = "home_quick_play",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyButton(
                                text = "Analyse Last",
                                onClick = { onAnalysePgn(latestPlay.pgn) },
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.Analytics,
                                testTag = "home_analyse_last",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    state.lastAnalysedTitle != null -> {
                        Text(
                            text = "Last reviewed: ${state.lastAnalysedTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DiggyButton(
                                text = "Play Computer",
                                onClick = { onNavigate("play") },
                                variant = DiggyButtonVariant.PRIMARY,
                                icon = Icons.Rounded.SmartToy,
                                testTag = "home_quick_play",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyButton(
                                text = "History",
                                onClick = { onNavigate("history") },
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.History,
                                testTag = "home_open_history",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "Challenge Stockfish across Easy, Medium, or Master strengths, or import games by username.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        DiggyButton(
                            text = "Play vs Computer",
                            onClick = { onNavigate("play") },
                            variant = DiggyButtonVariant.PRIMARY,
                            icon = Icons.Rounded.SmartToy,
                            testTag = "home_quick_play",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Quick Actions Grid
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Column(verticalArrangement = Arrangement.spacedBy(DiggySpacing.s1)) {
            quickTiles.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DiggySpacing.s1)
                ) {
                    pair.forEach { tile ->
                        DiggyCard(
                            onClick = { onNavigate(tile.route) },
                            contentPadding = PaddingValues(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 104.dp)
                                .testTag(tile.testTag)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(AccentGreen.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tile.icon,
                                        contentDescription = tile.title,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Text(
                                    text = tile.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tile.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
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
private fun HomeStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    DiggyCard(
        elevated = false,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                color = TextPrimary
            )
        }
    }
}
