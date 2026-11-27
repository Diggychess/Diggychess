package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.PieceType
import com.example.chess.PuzzleAttempt
import com.example.chess.PuzzleDifficultyFilter
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PuzzleCategory
import com.example.ui.viewmodel.PuzzlesUiState
import com.example.ui.viewmodel.PuzzlesViewModel
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun PuzzlesScreen(
    state: PuzzlesUiState,
    levelSolvedCount: (Int) -> Int,
    isLevelUnlocked: (Int) -> Boolean,
    levelPuzzleCount: (Int) -> Int,
    onStartRated: () -> Unit,
    onSetStartingRating: (Int) -> Unit,
    onStartLevel: (Int) -> Unit,
    onToggleCategory: (PuzzleCategory) -> Unit,
    onSelectTheme: (String) -> Unit,
    onStartPractice: () -> Unit,
    onStartQuickDifficulty: (String) -> Unit,
    onStartDaily: () -> Unit,
    onStartRandom: () -> Unit,
    onStartTimed: () -> Unit,
    onStartReviewMistakes: () -> Unit,
    onStartMyGameMistakes: () -> Unit,
    onSetDailyGoal: (Int) -> Unit,
    onToggleDifficultyFilter: (PuzzleDifficultyFilter) -> Unit,
    onSetDifficultyFilter: (PuzzleDifficultyFilter) -> Unit,
    onImportCsv: (String) -> Unit,
    onSquareClick: (String) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onRestartPuzzle: () -> Unit,
    onUndoPuzzle: () -> Unit,
    onHintPuzzle: () -> Unit,
    onRevealHintDestination: () -> Unit,
    onDismissHintModal: () -> Unit,
    onNextPuzzle: () -> Unit,
    onConfirmSkip: () -> Unit,
    onDismissSkip: () -> Unit,
    onToggleFlip: () -> Unit,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.inPlay) {
        PuzzlePlayView(
            state = state,
            onSelectDifficultyFilter = onSetDifficultyFilter,
            onSquareClick = onSquareClick,
            onSelectPromotion = onSelectPromotion,
            onDismissPromotion = onDismissPromotion,
            onRestart = onRestartPuzzle,
            onUndo = onUndoPuzzle,
            onHint = onHintPuzzle,
            onRevealHintDestination = onRevealHintDestination,
            onDismissHintModal = onDismissHintModal,
            onNext = onNextPuzzle,
            onConfirmSkip = onConfirmSkip,
            onDismissSkip = onDismissSkip,
            onToggleFlip = onToggleFlip,
            onBack = onBackToMenu,
            modifier = modifier
        )
        return
    }

    var themeMenuOpen by remember { mutableStateOf(false) }
    var startRatingOpen by remember { mutableStateOf(false) }
    var goalMenuOpen by remember { mutableStateOf(false) }
    var importExpanded by remember { mutableStateOf(false) }
    var csvText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DiggySpacing.s2, vertical = DiggySpacing.s2),
        verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.puzzles_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.puzzles_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // Daily Challenge Header Card
        val dailyGoal = state.dailyGoal.coerceAtLeast(1)
        val solvedToday = state.dailySolvedToday.coerceAtMost(dailyGoal)
        val dailyDone = state.dailySolvedToday >= dailyGoal
        val dailyFrac = (solvedToday.toFloat() / dailyGoal).coerceIn(0f, 1f)
        val dailyAccent = if (dailyDone) FeedbackCorrectGreen else AccentGreen

        DiggyCard(
            elevated = true,
            borderColor = dailyAccent.copy(alpha = 0.55f),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("daily_challenge_card")
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (dailyDone) Icons.Rounded.EmojiEvents else Icons.Rounded.Today,
                            contentDescription = "Daily Challenge",
                            tint = if (dailyDone) TrophyGold else dailyAccent,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Daily Challenge",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = dailyAccent
                            )
                            Text(
                                text = if (dailyDone) {
                                    "Today's goal completed!"
                                } else {
                                    "${dailyGoal - solvedToday} puzzle${if (dailyGoal - solvedToday == 1) "" else "s"} left today"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                    Surface(
                        color = dailyAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, dailyAccent.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "$solvedToday / $dailyGoal",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = dailyAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { dailyFrac },
                    color = dailyAccent,
                    trackColor = BorderSubtle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .testTag("puzzles_daily_progress_bar")
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 330.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Daily target:",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PuzzlesViewModel.DAILY_GOAL_OPTIONS.forEach { goalOpt ->
                                    DiggyChip(
                                        label = "$goalOpt",
                                        selected = state.dailyGoal == goalOpt,
                                        onClick = { onSetDailyGoal(goalOpt) },
                                        accentColor = dailyAccent,
                                        testTag = "daily_goal_chip_$goalOpt",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (state.dailyGoal !in PuzzlesViewModel.DAILY_GOAL_OPTIONS) {
                                    DiggyChip(
                                        label = "${state.dailyGoal}",
                                        selected = true,
                                        onClick = {},
                                        accentColor = dailyAccent,
                                        testTag = "daily_goal_chip_custom",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Daily target:",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                            PuzzlesViewModel.DAILY_GOAL_OPTIONS.forEach { goalOpt ->
                                DiggyChip(
                                    label = "$goalOpt",
                                    selected = state.dailyGoal == goalOpt,
                                    onClick = { onSetDailyGoal(goalOpt) },
                                    accentColor = dailyAccent,
                                    testTag = "daily_goal_chip_$goalOpt",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (state.dailyGoal !in PuzzlesViewModel.DAILY_GOAL_OPTIONS) {
                                DiggyChip(
                                    label = "${state.dailyGoal}",
                                    selected = true,
                                    onClick = {},
                                    accentColor = dailyAccent,
                                    testTag = "daily_goal_chip_custom",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Difficulty Filter System for Daily Goal & Practice
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Difficulty filter",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        val allSelected = state.difficultyFilter == PuzzleDifficultyFilter.ALL
                        Surface(
                            onClick = { onSetDifficultyFilter(PuzzleDifficultyFilter.ALL) },
                            shape = RoundedCornerShape(999.dp),
                            color = if (allSelected) AccentGreen.copy(alpha = 0.16f) else CardSurfaceElevated,
                            border = BorderStroke(1.dp, if (allSelected) AccentGreen else BorderSubtle),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("puzzle_diff_all")
                        ) {
                            Text(
                                text = "All (${state.difficultyCounts[PuzzleDifficultyFilter.ALL] ?: state.totalPoolSize})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (allSelected) AccentGreen else TextMuted,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PuzzleDifficultyFilter.TIERS.forEach { tier ->
                            val isSelected = state.difficultyFilter == tier
                            val tierAccent = when (tier) {
                                PuzzleDifficultyFilter.BEGINNER -> FeedbackCorrectGreen
                                PuzzleDifficultyFilter.INTERMEDIATE -> StudioCyan
                                PuzzleDifficultyFilter.ADVANCED -> TrophyGold
                                PuzzleDifficultyFilter.ALL -> AccentGreen
                            }
                            val tierCount = state.difficultyCounts[tier] ?: 0
                            Surface(
                                onClick = { onToggleDifficultyFilter(tier) },
                                shape = RoundedCornerShape(DiggySpacing.controlRadius),
                                color = if (isSelected) tierAccent.copy(alpha = 0.16f) else CardSurfaceElevated,
                                border = BorderStroke(1.dp, if (isSelected) tierAccent else BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = DiggySpacing.touchTargetMin)
                                    .testTag("puzzle_diff_${tier.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = tier.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) tierAccent else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tier.ratingLabel,
                                        fontSize = 10.sp,
                                        color = if (isSelected) tierAccent.copy(alpha = 0.9f) else TextMuted,
                                        maxLines = 1
                                    )
                                    if (tierCount > 0) {
                                        Text(
                                            text = "$tierCount",
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DiggyButton(
                    text = when {
                        dailyDone -> "Challenge Complete · Play Bonus"
                        solvedToday > 0 -> "Continue Daily Challenge ($solvedToday/$dailyGoal)"
                        else -> if (state.difficultyFilter == PuzzleDifficultyFilter.ALL) {
                            "Start Daily Challenge"
                        } else {
                            "Start ${state.difficultyFilter.label} Daily Challenge"
                        }
                    },
                    onClick = onStartDaily,
                    enabled = !state.isLoadingPool,
                    icon = if (dailyDone) Icons.Rounded.Bolt else Icons.Rounded.PlayArrow,
                    testTag = "start_daily_challenge_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Rated Training Card
        DiggyCard(elevated = true, contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Rated Training", style = MaterialTheme.typography.titleMedium, color = AccentGreen)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatBox(
                        value = "${state.ratingState.rating.roundToInt()}",
                        label = "Puzzle rating",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        value = "${state.ratingState.count}",
                        label = "Rated puzzles",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (state.ratingState.history.size > 1) {
                    RatingSparkline(history = state.ratingState.history)
                }

                if (state.ratingState.count == 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Starting rating:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Box {
                            OutlinedButton(
                                onClick = { startRatingOpen = true },
                                border = BorderStroke(1.dp, BorderSubtle),
                                shape = RoundedCornerShape(DiggySpacing.chipRadius),
                                modifier = Modifier.heightIn(min = DiggySpacing.touchTargetMin)
                            ) {
                                Text("${state.ratingState.rating.roundToInt()}", color = TextPrimary)
                            }
                            DropdownMenu(
                                expanded = startRatingOpen,
                                onDismissRequest = { startRatingOpen = false }
                            ) {
                                listOf(600, 800, 1000, 1200, 1500, 1800, 2100).forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text("$r") },
                                        onClick = {
                                            onSetStartingRating(r)
                                            startRatingOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Adaptive tactical puzzles matched to your current ELO rating.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                DiggyButton(
                    text = "Start Rated Puzzles",
                    onClick = onStartRated,
                    enabled = !state.isLoadingPool,
                    icon = Icons.Rounded.PlayArrow,
                    testTag = "start_rated_puzzles",
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Quick Difficulty Drills",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiggyChip(
                        label = "Easy",
                        selected = false,
                        onClick = { if (!state.isLoadingPool) onStartQuickDifficulty("easy") },
                        accentColor = FeedbackCorrectGreen,
                        testTag = "puzzle_quick_easy",
                        modifier = Modifier.weight(1f)
                    )
                    DiggyChip(
                        label = "Medium",
                        selected = false,
                        onClick = { if (!state.isLoadingPool) onStartQuickDifficulty("medium") },
                        accentColor = StudioCyan,
                        testTag = "puzzle_quick_medium",
                        modifier = Modifier.weight(1f)
                    )
                    DiggyChip(
                        label = "Hard",
                        selected = false,
                        onClick = { if (!state.isLoadingPool) onStartQuickDifficulty("hard") },
                        accentColor = TrophyGold,
                        testTag = "puzzle_quick_hard",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Levels Card
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Levels", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                val levels = PuzzlesViewModel.LEVELS
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    levels.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { lv ->
                                val unlocked = isLevelUnlocked(lv.index)
                                val solved = levelSolvedCount(lv.index)
                                val totalInLv = levelPuzzleCount(lv.index)
                                val rangeStr = if (lv.maxRating >= 4000) "${lv.minRating}+" else "${lv.minRating}–${lv.maxRating - 1}"
                                Surface(
                                    onClick = { if (unlocked) onStartLevel(lv.index) },
                                    enabled = unlocked,
                                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                                    color = if (unlocked) CardSurfaceElevated else CardSurfaceElevated.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, if (unlocked) BorderSubtle else LineSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("puzzle_level_${lv.index}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "L${lv.index + 1} · ${lv.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (unlocked) TextPrimary else TextDisabled,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (!unlocked) {
                                                Icon(
                                                    Icons.Rounded.Lock,
                                                    contentDescription = "Locked",
                                                    tint = TextDisabled,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "$rangeStr · $totalInLv",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                        LinearProgressIndicator(
                                            progress = { (solved.toFloat() / PuzzlesViewModel.LEVEL_GOAL).coerceIn(0f, 1f) },
                                            color = AccentGreen,
                                            trackColor = BorderSubtle,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                        )
                                        Text(
                                            text = if (unlocked) {
                                                "${minOf(solved, PuzzlesViewModel.LEVEL_GOAL)}/${PuzzlesViewModel.LEVEL_GOAL} solved"
                                            } else {
                                                "Reach ${lv.minRating} ELO"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
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

        // Practice Card
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Custom Practice", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PuzzleCategory.entries.chunked(2).forEach { rowCats ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCats.forEach { cat ->
                                val selected = cat in state.selectedCategories
                                val count = state.categoryCounts[cat] ?: 0
                                Surface(
                                    onClick = { onToggleCategory(cat) },
                                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                                    color = if (selected) AccentGreen.copy(alpha = 0.16f) else CardSurfaceElevated,
                                    border = BorderStroke(1.dp, if (selected) AccentGreen else BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("puzzle_cat_${cat.id}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = cat.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) AccentGreen else TextPrimary
                                        )
                                        Text(
                                            text = "${cat.rangeLabel} · $count",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                            if (rowCats.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { themeMenuOpen = true },
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(DiggySpacing.controlRadius),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = DiggySpacing.touchTargetMin)
                            .testTag("puzzle_theme_dropdown")
                    ) {
                        Text(
                            text = if (state.selectedTheme.isEmpty()) "Theme: All themes"
                            else "Theme: ${PuzzlesViewModel.formatThemeLabel(state.selectedTheme)}",
                            color = TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = themeMenuOpen,
                        onDismissRequest = { themeMenuOpen = false },
                        modifier = Modifier.heightIn(max = 320.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("All themes") },
                            onClick = {
                                onSelectTheme("")
                                themeMenuOpen = false
                            }
                        )
                        state.availableThemes.forEach { th ->
                            DropdownMenuItem(
                                text = { Text(PuzzlesViewModel.formatThemeLabel(th)) },
                                onClick = {
                                    onSelectTheme(th)
                                    themeMenuOpen = false
                                }
                            )
                        }
                    }
                }

                DiggyButton(
                    text = "Start Practice",
                    onClick = onStartPractice,
                    enabled = !state.isLoadingPool,
                    icon = Icons.Rounded.FitnessCenter,
                    testTag = "start_practice_button",
                    modifier = Modifier.fillMaxWidth()
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 340.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DiggyChip(
                                    label = "Daily",
                                    selected = false,
                                    onClick = onStartDaily,
                                    icon = Icons.Rounded.Today,
                                    testTag = "daily_puzzle_button",
                                    modifier = Modifier.weight(1f)
                                )
                                DiggyChip(
                                    label = "Random",
                                    selected = false,
                                    onClick = onStartRandom,
                                    icon = Icons.Rounded.Shuffle,
                                    testTag = "random_puzzle_button",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            DiggyChip(
                                label = "Timed 3:00",
                                selected = false,
                                onClick = onStartTimed,
                                icon = Icons.Rounded.Timer,
                                testTag = "timed_puzzle_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiggyChip(
                                label = "Daily",
                                selected = false,
                                onClick = onStartDaily,
                                icon = Icons.Rounded.Today,
                                testTag = "daily_puzzle_button",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyChip(
                                label = "Random",
                                selected = false,
                                onClick = onStartRandom,
                                icon = Icons.Rounded.Shuffle,
                                testTag = "random_puzzle_button",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyChip(
                                label = "Timed 3:00",
                                selected = false,
                                onClick = onStartTimed,
                                icon = Icons.Rounded.Timer,
                                testTag = "timed_puzzle_button",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 310.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DiggyButton(
                                text = "Mistakes (${state.reviewMistakesCount})",
                                onClick = onStartReviewMistakes,
                                variant = DiggyButtonVariant.OUTLINED,
                                testTag = "review_mistakes_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                            DiggyButton(
                                text = "My Game Fixes",
                                onClick = onStartMyGameMistakes,
                                variant = DiggyButtonVariant.OUTLINED,
                                testTag = "my_game_mistakes_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiggyButton(
                                text = "Mistakes (${state.reviewMistakesCount})",
                                onClick = onStartReviewMistakes,
                                variant = DiggyButtonVariant.OUTLINED,
                                testTag = "review_mistakes_button",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyButton(
                                text = "My Game Fixes",
                                onClick = onStartMyGameMistakes,
                                variant = DiggyButtonVariant.OUTLINED,
                                testTag = "my_game_mistakes_button",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                state.menuError?.let { err ->
                    DiggyErrorState(message = err)
                }
            }
        }

        // Progress Card
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your Progress", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                if (state.attempts.isEmpty()) {
                    DiggyEmptyState(
                        title = "No puzzle attempts yet",
                        subtitle = "Solve puzzles to track your ELO rating, streak, and 14-day chart.",
                        icon = Icons.Rounded.Extension
                    )
                } else {
                    val total = state.attempts.size
                    val solved = state.attempts.count { it.ok }
                    val accPct = ((solved.toDouble() / total) * 100.0).roundToInt()
                    var curStreak = 0
                    var bestStreak = state.bestPuzzleStreak
                    state.attempts.forEach { a ->
                        curStreak = if (a.ok) curStreak + 1 else 0
                        bestStreak = maxOf(bestStreak, curStreak)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatBox("${state.ratingState.rating.roundToInt()}", "Puzzle rating", Modifier.weight(1f))
                        StatBox("$solved / $total", "Solved / tried", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        StatBox("$accPct%", "Accuracy", Modifier.weight(1f))
                        StatBox("$curStreak", "Streak (best $bestStreak)", Modifier.weight(1f))
                    }

                    val dailyCounts = remember(state.attempts) { computeLast14DaysCounts(state.attempts) }
                    val todayCount = dailyCounts.lastOrNull() ?: 0

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Daily goal: $todayCount /",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Box {
                            TextButton(onClick = { goalMenuOpen = true }) {
                                Text("${state.dailyGoal} solved today", color = AccentGreen)
                            }
                            DropdownMenu(expanded = goalMenuOpen, onDismissRequest = { goalMenuOpen = false }) {
                                listOf(3, 5, 10, 20).forEach { g ->
                                    DropdownMenuItem(
                                        text = { Text("$g puzzles / day") },
                                        onClick = {
                                            onSetDailyGoal(g)
                                            goalMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (todayCount.toFloat() / state.dailyGoal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        color = AccentGreen,
                        trackColor = BorderSubtle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                    )

                    DailyBarChart(counts = dailyCounts)

                    Text(
                        text = "Solved per day over the last 14 days (unassisted clean solves).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Import Lichess CSV Puzzles Card
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { importExpanded = !importExpanded },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.heightIn(min = DiggySpacing.touchTargetMin)
                ) {
                    Icon(
                        imageVector = if (importExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (importExpanded) "Hide CSV Puzzle Import" else "Import Lichess CSV Puzzles",
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (importExpanded) {
                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        placeholder = { Text("Paste Lichess puzzle CSV rows here", color = InputHint) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = InputText),
                        colors = diggyInputColors(),
                        shape = RoundedCornerShape(DiggySpacing.controlRadius)
                    )
                    DiggyButton(
                        text = "Import CSV Rows",
                        onClick = { onImportCsv(csvText) },
                        icon = Icons.Rounded.UploadFile,
                        modifier = Modifier.fillMaxWidth()
                    )
                    state.importStatus?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = AccentGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = CardSurfaceElevated,
        shape = RoundedCornerShape(DiggySpacing.controlRadius),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun RatingSparkline(history: List<Pair<Long, Int>>) {
    val lineColor = AccentGreen
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val vals = history.map { it.second }
        val minV = vals.minOrNull() ?: 1200
        val maxV = vals.maxOrNull() ?: 1200
        val span = maxOf(40, maxV - minV).toFloat()
        val path = Path()
        vals.forEachIndexed { idx, v ->
            val x = if (vals.size == 1) 0f else (idx.toFloat() / (vals.size - 1)) * size.width
            val y = size.height - 6f - ((v - minV) / span) * (size.height - 12f)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round))
    }
}

@Composable
private fun DailyBarChart(counts: List<Int>) {
    val maxC = maxOf(1, counts.maxOrNull() ?: 1)
    val barColor = AccentGreen
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val barSpace = size.width / counts.size.coerceAtLeast(1)
        val barW = barSpace * 0.65f
        counts.forEachIndexed { i, c ->
            val h = (c.toFloat() / maxC) * (size.height - 6f)
            val x = i * barSpace + (barSpace - barW) / 2f
            val y = size.height - h
            if (h > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
    }
}

private fun computeLast14DaysCounts(attempts: List<PuzzleAttempt>): List<Int> {
    val cal = Calendar.getInstance()
    fun dayKey(ms: Long): String {
        cal.timeInMillis = ms
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }
    val okByDay = attempts.filter { it.ok }.groupBy { dayKey(it.timestamp) }
    val now = System.currentTimeMillis()
    return (13 downTo 0).map { d ->
        val k = dayKey(now - d * 86_400_000L)
        okByDay[k]?.size ?: 0
    }
}
