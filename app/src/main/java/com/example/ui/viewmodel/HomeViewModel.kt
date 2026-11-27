package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.PuzzleAttempt
import com.example.chess.PuzzleDifficultyFilter
import com.example.data.KnightScopeRepository
import com.example.data.RecentPlayGameSummary
import com.example.data.UserProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class DailyGoalDayStatus(
    val dayLabel: String,
    val solvedCount: Int,
    val targetMet: Boolean,
    val isToday: Boolean
)

data class HomeUiState(
    val userProfile: UserProfile = UserProfile(),
    val puzzleRating: Int = 1200,
    val openingStreak: Int = 0,
    val bestPuzzleStreak: Int = 0,
    val dailyGoal: Int = 5,
    val dailySolvedToday: Int = 0,
    val dailyGoalDayStreak: Int = 0,
    val dailyDifficultyFilter: PuzzleDifficultyFilter = PuzzleDifficultyFilter.ALL,
    val weeklyDailyGoalDays: List<DailyGoalDayStatus> = emptyList(),
    val completedLessonsCount: Int = 0,
    val totalLessonsCount: Int = 6,
    val recentGamesCount: Int = 0,
    val lastAnalysedTitle: String? = null,
    val recentPlayGames: List<RecentPlayGameSummary> = emptyList()
)

class HomeViewModel(private val repo: KnightScopeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var pendingDifficultyFilter: PuzzleDifficultyFilter? = null
    private var difficultySaveJob: kotlinx.coroutines.Job? = null

    companion object {
        val DAILY_GOAL_PRESETS = listOf(3, 5, 10, 15, 20)

        fun computeDailyGoalMetrics(
            attempts: List<PuzzleAttempt>,
            dailyGoal: Int,
            nowMs: Long = System.currentTimeMillis()
        ): Triple<Int, Int, List<DailyGoalDayStatus>> {
            val safeGoal = dailyGoal.coerceIn(1, 100)
            val cal = Calendar.getInstance()
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

            fun dayKey(year: Int, dayOfYear: Int): String = "$year-$dayOfYear"

            val solvedByDay = HashMap<String, Int>()
            for (attempt in attempts) {
                if (!attempt.ok) continue
                cal.timeInMillis = attempt.timestamp
                val key = dayKey(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
                solvedByDay[key] = (solvedByDay[key] ?: 0) + 1
            }

            // Build 7-day rolling window ending today
            val weekDays = (6 downTo 0).map { daysAgo ->
                cal.timeInMillis = nowMs
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                val key = dayKey(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
                val solved = solvedByDay[key] ?: 0
                val label = if (daysAgo == 0) "Today" else dayFormat.format(cal.time).take(3)
                DailyGoalDayStatus(
                    dayLabel = label,
                    solvedCount = solved,
                    targetMet = solved >= safeGoal,
                    isToday = daysAgo == 0
                )
            }

            cal.timeInMillis = nowMs
            val todayKey = dayKey(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
            val solvedToday = solvedByDay[todayKey] ?: 0

            // Compute consecutive daily-goal streak: includes today if met, or starts from yesterday if today is still in progress
            var streak = 0
            val startOffset = if (solvedToday >= safeGoal) 0 else 1
            for (daysAgo in startOffset..365) {
                cal.timeInMillis = nowMs
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                val key = dayKey(cal.get(Calendar.YEAR), cal.get(Calendar.DAY_OF_YEAR))
                if ((solvedByDay[key] ?: 0) >= safeGoal) {
                    streak++
                } else {
                    break
                }
            }

            return Triple(solvedToday, streak, weekDays)
        }
    }

    init {
        viewModelScope.launch {
            repo.userProfileFlow.collect { prof ->
                _uiState.update { it.copy(userProfile = prof) }
            }
        }
        viewModelScope.launch {
            repo.puzzleRatingFlow.collect { pr ->
                _uiState.update { it.copy(puzzleRating = pr.rating.roundToInt()) }
            }
        }
        viewModelScope.launch {
            repo.openingStreakFlow.collect { st ->
                _uiState.update { it.copy(openingStreak = st) }
            }
        }
        viewModelScope.launch {
            repo.bestPuzzleStreakFlow.distinctUntilChanged().collect { best ->
                _uiState.update { it.copy(bestPuzzleStreak = best) }
            }
        }
        viewModelScope.launch {
            combine(
                repo.dailyGoalFlow.distinctUntilChanged(),
                repo.puzzleAttemptsFlow.distinctUntilChanged()
            ) { goal, attempts ->
                goal to computeDailyGoalMetrics(attempts, goal)
            }.collect { (goal, metrics) ->
                val (solvedToday, goalStreak, weekDays) = metrics
                _uiState.update {
                    it.copy(
                        dailyGoal = goal,
                        dailySolvedToday = solvedToday,
                        dailyGoalDayStreak = goalStreak,
                        weeklyDailyGoalDays = weekDays
                    )
                }
            }
        }
        viewModelScope.launch {
            repo.completedLessonsFlow.collect { done ->
                _uiState.update { it.copy(completedLessonsCount = done.size) }
            }
        }
        viewModelScope.launch {
            repo.historyFlow.collect { hist ->
                _uiState.update {
                    it.copy(
                        recentGamesCount = hist.size,
                        lastAnalysedTitle = hist.firstOrNull()?.title
                    )
                }
            }
        }
        viewModelScope.launch {
            repo.recentPlayGamesFlow.collect { played ->
                _uiState.update { it.copy(recentPlayGames = played) }
            }
        }
        viewModelScope.launch {
            repo.puzzleDifficultyFlow.distinctUntilChanged().collect { filter ->
                val pending = pendingDifficultyFilter
                if (pending != null && filter != pending) {
                    return@collect
                }
                pendingDifficultyFilter = null
                _uiState.update { it.copy(dailyDifficultyFilter = filter) }
            }
        }
    }

    fun setDailyGoal(goal: Int) {
        val safeGoal = goal.coerceIn(1, 100)
        viewModelScope.launch {
            repo.setDailyGoal(safeGoal)
        }
    }

    fun setDailyDifficultyFilter(filter: PuzzleDifficultyFilter) {
        pendingDifficultyFilter = filter
        _uiState.update { it.copy(dailyDifficultyFilter = filter) }
        difficultySaveJob?.cancel()
        difficultySaveJob = viewModelScope.launch {
            repo.setPuzzleDifficulty(filter)
        }
    }
}
