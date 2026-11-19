package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.PieceType
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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

        // Rated Training Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("RATED TRAINING", style = MaterialTheme.typography.titleSmall, color = TextMuted)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
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
                                shape = RoundedCornerShape(10.dp)
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
                        text = "You get puzzles close to your rating. Solving a harder puzzle raises it more; failing an easier one costs more.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Button(
                    onClick = onStartRated,
                    enabled = !state.isLoadingPool,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("start_rated_puzzles")
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start rated puzzles", fontWeight = FontWeight.Bold)
                }

                Text("QUICK DIFFICULTY DRILLS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onStartQuickDifficulty("easy") },
                        enabled = !state.isLoadingPool,
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("puzzle_quick_easy")
                    ) {
                        Text("Easy", color = AccentGreenHover, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { onStartQuickDifficulty("medium") },
                        enabled = !state.isLoadingPool,
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("puzzle_quick_medium")
                    ) {
                        Text("Medium", color = StudioCyan, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { onStartQuickDifficulty("hard") },
                        enabled = !state.isLoadingPool,
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("puzzle_quick_hard")
                    ) {
                        Text("Hard", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Levels Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("LEVELS", style = MaterialTheme.typography.titleSmall, color = TextMuted)
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
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceDark.copy(alpha = if (unlocked) 0.9f else 0.45f),
                                    border = BorderStroke(1.dp, LineSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("puzzle_level_${lv.index}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Level ${lv.index + 1} · ${lv.name}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (unlocked) TextPrimary else TextMuted
                                            )
                                            if (!unlocked) {
                                                Icon(
                                                    Icons.Filled.Lock,
                                                    contentDescription = "Locked",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "$rangeStr · $totalInLv puzzles",
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
                                                "Solved ${minOf(solved, PuzzlesViewModel.LEVEL_GOAL)} / ${PuzzlesViewModel.LEVEL_GOAL} to unlock next"
                                            } else {
                                                "Solve ${PuzzlesViewModel.LEVEL_GOAL} in Level ${lv.index} or reach ${lv.minRating}"
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
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PRACTICE", style = MaterialTheme.typography.titleSmall, color = TextMuted)

                // Category multi-select pills
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
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) AccentGreen.copy(alpha = 0.14f) else SurfaceDark,
                                    border = BorderStroke(1.dp, if (selected) AccentGreen else BorderSubtle),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("puzzle_cat_${cat.id}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = cat.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${cat.rangeLabel} · $count puzzles",
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

                // Theme Dropdown
                Box {
                    OutlinedButton(
                        onClick = { themeMenuOpen = true },
                        border = BorderStroke(1.dp, BorderSubtle),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
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

                // Practice Mode Action Buttons
                Button(
                    onClick = onStartPractice,
                    enabled = !state.isLoadingPool,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("start_practice_button")
                ) {
                    Text("Start practice", fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onStartDaily,
                        border = BorderStroke(1.dp, LineSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("daily_puzzle_button")
                    ) {
                        Text("Daily puzzle", color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = onStartRandom,
                        border = BorderStroke(1.dp, LineSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("random_puzzle_button")
                    ) {
                        Text("Random", color = TextPrimary)
                    }
                    OutlinedButton(
                        onClick = onStartTimed,
                        border = BorderStroke(1.dp, LineSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("timed_puzzle_button")
                    ) {
                        Text("Timed 3:00", color = TextPrimary)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onStartReviewMistakes,
                        border = BorderStroke(1.dp, LineSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("review_mistakes_button")
                    ) {
                        Text("Review mistakes (${state.reviewMistakesCount})", color = TextPrimary, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onStartMyGameMistakes,
                        border = BorderStroke(1.dp, LineSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("my_game_mistakes_button")
                    ) {
                        Text("My game mistakes", color = TextPrimary, fontSize = 12.sp)
                    }
                }

                state.menuError?.let { err ->
                    Text(err, style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color(0xFFFF8A80))
                }
            }
        }

        // Your Progress Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("YOUR PROGRESS", style = MaterialTheme.typography.titleSmall, color = TextMuted)
                if (state.attempts.isEmpty()) {
                    Text(
                        text = "No attempts yet. Solve a puzzle and your statistics will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                } else {
                    val total = state.attempts.size
                    val solved = state.attempts.count { it.ok }
                    val accPct = ((solved.toDouble() / total) * 100.0).roundToInt()
                    var curStreak = 0
                    var bestStreak = 0
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
                        text = "Solved per day, last 14 days. A puzzle counts as solved when you finish it without hints, wrong moves or the solution.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Import Lichess CSV Puzzles Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                TextButton(
                    onClick = { importExpanded = !importExpanded },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (importExpanded) "Hide CSV puzzle import" else "Import Lichess CSV puzzles",
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (importExpanded) {
                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        placeholder = { Text("Paste Lichess puzzle CSV rows here", color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily, color = TextPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { onImportCsv(csvText) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Import CSV rows", fontWeight = FontWeight.Bold)
                    }
                    state.importStatus?.let {
                        Spacer(modifier = Modifier.height(6.dp))
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
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
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
        drawPath(path, color = lineColor, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
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
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
    }
}

private fun computeLast14DaysCounts(attempts: List<com.example.chess.PuzzleAttempt>): List<Int> {
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
