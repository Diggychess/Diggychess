package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.example.ui.theme.BoardTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt

enum class PuzzleCategory(val id: String, val label: String, val rangeLabel: String) {
    EASY("easy", "Easy", "0–1399"),
    MEDIUM("med", "Medium", "1400–1999"),
    HARD("hard", "Hard", "2000+"),
    GM("gm", "Grandmaster", "2500+"),
    UNRATED("unr", "Starter (unrated)", "Chess Analyzer");

    fun matches(p: Puzzle): Boolean = when (this) {
        EASY -> p.rating in 1..1399
        MEDIUM -> p.rating in 1400..1999
        HARD -> p.rating >= 2000
        GM -> p.rating >= 2500
        UNRATED -> p.rating == 0
    }
}

data class PuzzleLevel(
    val index: Int,
    val name: String,
    val minRating: Int,
    val maxRating: Int
)

enum class PuzzleMode { SET, RATED, LEVEL, DAILY, RAND, TIMED, REVIEW }

data class PuzzlesUiState(
    val isLoadingPool: Boolean = true,
    val totalPoolSize: Int = 0,
    val categoryCounts: Map<PuzzleCategory, Int> = emptyMap(),
    val selectedCategories: Set<PuzzleCategory> = setOf(
        PuzzleCategory.EASY, PuzzleCategory.MEDIUM, PuzzleCategory.HARD, PuzzleCategory.UNRATED
    ),
    val availableThemes: List<String> = emptyList(),
    val selectedTheme: String = "",
    val reviewMistakesCount: Int = 0,
    val ratingState: PuzzleRatingState = PuzzleRatingState(),
    val attempts: List<PuzzleAttempt> = emptyList(),
    val dailyGoal: Int = 5,
    val dailySolvedToday: Int = 0,
    val justCompletedDailyChallenge: Boolean = false,
    val difficultyFilter: PuzzleDifficultyFilter = PuzzleDifficultyFilter.ALL,
    val difficultyCounts: Map<PuzzleDifficultyFilter, Int> = emptyMap(),
    val boardTheme: BoardTheme = BoardTheme.GREEN,
    val animateMoves: Boolean = true,
    val menuError: String? = null,
    val importStatus: String? = null,

    // Play state
    val inPlay: Boolean = false,
    val mode: PuzzleMode = PuzzleMode.SET,
    val queue: List<Puzzle> = emptyList(),
    val queueIndex: Int = 0,
    val levelIndex: Int = 0,
    val currentPuzzle: Puzzle? = null,
    val currentFen: String = ChessBoard.START_FEN,
    val playerColor: PieceColor = PieceColor.WHITE,
    val flipped: Boolean = false,
    val selectedSquare: String? = null,
    val legalTargets: Map<String, Boolean> = emptyMap(),
    val lastMoveSquares: List<String> = emptyList(),
    val correctMoveSquares: List<String> = emptyList(),
    val isWrongMove: Boolean = false,
    val hintLevel: Int = 0,
    val hintSquares: List<String> = emptyList(),
    val showHintModal: Boolean = false,
    val hintPieceName: String = "",
    val hintFromSquare: String = "",
    val hintToSquare: String = "",
    val hintSanMove: String = "",
    val hintThemeLabel: String = "",
    val hintTacticalCue: String = "",
    val solStepIndex: Int = 0,
    val isFinished: Boolean = false,
    val isTimedExpired: Boolean = false,
    val timedRemainingSec: Int = 180,
    val timedSolvedCount: Int = 0,
    val feedbackText: String = "",
    val feedbackOk: Boolean? = null,
    val solutionExplanation: String = "",
    val lastResultOk: Boolean? = null,
    val lastEloDelta: Int? = null,
    val currentPuzzleStreak: Int = 0,
    val bestPuzzleStreak: Int = 0,
    val isNewBestStreak: Boolean = false,
    val streakMilestoneReached: Int? = null,
    val showSkipDialog: Boolean = false,
    val pendingPromotion: Pair<String, String>? = null
)

class PuzzlesViewModel(
    private val repo: KnightScopeRepository
) : ViewModel() {

    companion object {
        const val LEVEL_GOAL = 10
        val LEVELS = listOf(
            PuzzleLevel(0, "Rookie", 0, 900),
            PuzzleLevel(1, "Learner", 900, 1200),
            PuzzleLevel(2, "Club", 1200, 1500),
            PuzzleLevel(3, "Strong club", 1500, 1800),
            PuzzleLevel(4, "Expert", 1800, 2100),
            PuzzleLevel(5, "Master", 2100, 2400),
            PuzzleLevel(6, "Grandmaster", 2400, 4000)
        )

        val STREAK_MILESTONES = setOf(3, 5, 10, 25, 50, 100)
        val DAILY_GOAL_OPTIONS = listOf(3, 5, 10, 20)

        fun milestoneForStreak(streak: Int): Int? =
            streak.takeIf { it > 0 && (it in STREAK_MILESTONES || (it >= 100 && it % 50 == 0)) }

        fun computeStreaks(attempts: List<PuzzleAttempt>): Pair<Int, Int> {
            var cur = 0
            var best = 0
            attempts.forEach { a ->
                cur = if (a.ok) cur + 1 else 0
                if (cur > best) best = cur
            }
            return cur to best
        }

        fun countSolvedToday(attempts: List<PuzzleAttempt>, nowMs: Long = System.currentTimeMillis()): Int {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
            val year = cal.get(Calendar.YEAR)
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            return attempts.count { a ->
                if (!a.ok) return@count false
                cal.timeInMillis = a.timestamp
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.DAY_OF_YEAR) == dayOfYear
            }
        }

        fun solvedIdsToday(attempts: List<PuzzleAttempt>, nowMs: Long = System.currentTimeMillis()): Set<String> {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
            val year = cal.get(Calendar.YEAR)
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            return attempts.mapNotNull { a ->
                if (!a.ok) return@mapNotNull null
                cal.timeInMillis = a.timestamp
                if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.DAY_OF_YEAR) == dayOfYear) a.id else null
            }.toSet()
        }

        fun formatThemeLabel(t: String): String =
            t.replace(Regex("([A-Z])"), " $1").replaceFirstChar { it.uppercaseChar() }.trim()
    }

    private val _uiState = MutableStateFlow(PuzzlesUiState())
    val uiState: StateFlow<PuzzlesUiState> = _uiState.asStateFlow()

    private var allPuzzles: List<Puzzle> = emptyList()
    private var savedHistory: List<SavedGameEntry> = emptyList()
    private var board: ChessBoard = ChessBoard()
    private var prevLastMoveSquares: List<String> = emptyList()
    private var recordedOutcome: Boolean? = null
    private var persistedBestPuzzleStreak: Int = 0
    private var latestAttemptSeqTs: Long = 0L
    private var latestPersistedAttempts: List<PuzzleAttempt> = emptyList()
    private val pendingLocalAttempts = mutableListOf<PuzzleAttempt>()
    private val attemptWriteMutex = Mutex()
    private var timerJob: Job? = null
    private var autoReplyJob: Job? = null
    private var solutionAnimJob: Job? = null
    private var difficultySaveJob: Job? = null
    private var pendingDifficultyFilter: PuzzleDifficultyFilter? = null
    private var playToken = 0

    init {
        reloadPool()
        viewModelScope.launch {
            repo.puzzleRatingFlow.collect { r ->
                _uiState.update { it.copy(ratingState = r) }
            }
        }
        viewModelScope.launch {
            repo.puzzleAttemptsFlow.distinctUntilChanged().collect { attempts ->
                latestPersistedAttempts = attempts
                val maxPersistedTs = attempts.maxOfOrNull { it.timestamp } ?: 0L
                if (maxPersistedTs > latestAttemptSeqTs) {
                    latestAttemptSeqTs = maxPersistedTs
                }
                pendingLocalAttempts.removeAll { pending ->
                    attempts.any { it.id == pending.id && it.timestamp == pending.timestamp }
                }
                val effectiveAttempts = attempts + pendingLocalAttempts
                val lastByPuzzle = mutableMapOf<String, Boolean>()
                effectiveAttempts.forEach { lastByPuzzle[it.id] = it.ok }
                val wrongCount = allPuzzles.count { lastByPuzzle[it.id] == false }
                val (curStreak, bestFromAttempts) = computeStreaks(effectiveAttempts)
                val mergedBest = maxOf(persistedBestPuzzleStreak, bestFromAttempts, _uiState.value.bestPuzzleStreak)
                val solvedToday = countSolvedToday(effectiveAttempts)
                _uiState.update {
                    it.copy(
                        attempts = effectiveAttempts,
                        reviewMistakesCount = wrongCount,
                        currentPuzzleStreak = curStreak,
                        bestPuzzleStreak = mergedBest,
                        dailySolvedToday = solvedToday
                    )
                }
            }
        }
        viewModelScope.launch {
            repo.bestPuzzleStreakFlow.distinctUntilChanged().collect { savedBest ->
                persistedBestPuzzleStreak = savedBest
                _uiState.update {
                    it.copy(bestPuzzleStreak = maxOf(it.bestPuzzleStreak, savedBest))
                }
            }
        }
        viewModelScope.launch {
            repo.dailyGoalFlow.collect { g ->
                _uiState.update { it.copy(dailyGoal = g) }
            }
        }
        viewModelScope.launch {
            repo.puzzleDifficultyFlow.distinctUntilChanged().collect { filter ->
                val pending = pendingDifficultyFilter
                if (pending != null && filter != pending) {
                    return@collect
                }
                pendingDifficultyFilter = null
                _uiState.update { s ->
                    s.copy(
                        difficultyFilter = filter,
                        selectedCategories = if (filter == PuzzleDifficultyFilter.ALL) {
                            s.selectedCategories
                        } else {
                            categoriesForDifficulty(filter)
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            repo.boardThemeFlow.collect { t ->
                _uiState.update { it.copy(boardTheme = t) }
            }
        }
        viewModelScope.launch {
            repo.pieceAnimationsFlow.collect { anim ->
                _uiState.update { it.copy(animateMoves = anim) }
            }
        }
        viewModelScope.launch {
            repo.historyFlow.collect { h ->
                savedHistory = h
            }
        }
    }

    fun reloadPool() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPool = true) }
            val loaded = repo.loadAllPuzzles()
            allPuzzles = loaded
            val counts = PuzzleCategory.entries.associateWith { cat -> loaded.count { cat.matches(it) } }
            val diffCounts = PuzzleDifficultyFilter.entries.associateWith { filter -> loaded.count { filter.matches(it) } }
            val themes = loaded.flatMap { it.themes }.toSet().sorted()
            val lastByPuzzle = mutableMapOf<String, Boolean>()
            _uiState.value.attempts.forEach { lastByPuzzle[it.id] = it.ok }
            val wrongCount = loaded.count { lastByPuzzle[it.id] == false }
            _uiState.update {
                it.copy(
                    isLoadingPool = false,
                    totalPoolSize = loaded.size,
                    categoryCounts = counts,
                    difficultyCounts = diffCounts,
                    availableThemes = themes,
                    reviewMistakesCount = wrongCount
                )
            }
        }
    }

    fun toggleCategory(cat: PuzzleCategory) {
        _uiState.update {
            val next = it.selectedCategories.toMutableSet()
            if (cat in next) next.remove(cat) else next.add(cat)
            it.copy(selectedCategories = next)
        }
    }

    fun setThemeFilter(theme: String) {
        _uiState.update { it.copy(selectedTheme = theme) }
    }

    fun setStartingRating(rating: Int) {
        viewModelScope.launch {
            val cur = _uiState.value.ratingState
            val nextState = cur.copy(rating = rating.toDouble(), history = emptyList())
            repo.savePuzzleRating(nextState)
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            repo.setDailyGoal(goal)
        }
    }

    private fun categoriesForDifficulty(filter: PuzzleDifficultyFilter): Set<PuzzleCategory> = when (filter) {
        PuzzleDifficultyFilter.ALL -> setOf(
            PuzzleCategory.EASY,
            PuzzleCategory.MEDIUM,
            PuzzleCategory.HARD,
            PuzzleCategory.GM,
            PuzzleCategory.UNRATED
        )
        PuzzleDifficultyFilter.BEGINNER -> setOf(PuzzleCategory.EASY, PuzzleCategory.UNRATED)
        PuzzleDifficultyFilter.INTERMEDIATE -> setOf(PuzzleCategory.MEDIUM)
        PuzzleDifficultyFilter.ADVANCED -> setOf(PuzzleCategory.HARD, PuzzleCategory.GM)
    }

    fun toggleDifficultyFilter(tier: PuzzleDifficultyFilter) {
        val next = if (_uiState.value.difficultyFilter == tier) PuzzleDifficultyFilter.ALL else tier
        setDifficultyFilter(next)
    }

    fun setDifficultyFilter(filter: PuzzleDifficultyFilter) {
        val wasInPlay = _uiState.value.inPlay
        val activeMode = _uiState.value.mode
        pendingDifficultyFilter = filter
        _uiState.update {
            it.copy(
                difficultyFilter = filter,
                selectedCategories = categoriesForDifficulty(filter),
                menuError = null
            )
        }
        difficultySaveJob?.cancel()
        difficultySaveJob = viewModelScope.launch {
            repo.setPuzzleDifficulty(filter)
        }
        if (wasInPlay && allPuzzles.isNotEmpty()) {
            when (activeMode) {
                PuzzleMode.DAILY -> startDaily()
                PuzzleMode.SET -> startPractice()
                PuzzleMode.RAND -> startRandom()
                PuzzleMode.TIMED -> startTimed()
                else -> Unit
            }
        }
    }

    fun levelSolvedCount(levelIdx: Int): Int {
        if (levelIdx !in LEVELS.indices) return 0
        val lv = LEVELS[levelIdx]
        return _uiState.value.attempts
            .filter { it.ok && it.rating >= lv.minRating && it.rating < lv.maxRating }
            .map { it.id }
            .toSet()
            .size
    }

    fun isLevelUnlocked(levelIdx: Int): Boolean {
        if (levelIdx == 0) return true
        val lv = LEVELS[levelIdx]
        return levelSolvedCount(levelIdx - 1) >= LEVEL_GOAL || _uiState.value.ratingState.rating >= lv.minRating
    }

    fun levelPuzzleCount(levelIdx: Int): Int {
        val lv = LEVELS[levelIdx]
        return allPuzzles.count { it.rating >= lv.minRating && it.rating < lv.maxRating }
    }

    private fun filteredPracticePool(): List<Puzzle> {
        val s = _uiState.value
        return allPuzzles.filter { p ->
            s.difficultyFilter.matches(p) &&
                s.selectedCategories.any { it.matches(p) } &&
                (s.selectedTheme.isEmpty() || s.selectedTheme in p.themes)
        }
    }

    fun startRated() {
        val p = pickRatedPuzzle(excludeId = null)
        if (p == null) {
            _uiState.update { it.copy(menuError = "No rated puzzles available.") }
            return
        }
        beginSession(PuzzleMode.RATED, listOf(p))
    }

    fun startLevel(levelIdx: Int) {
        if (!isLevelUnlocked(levelIdx)) return
        val lv = LEVELS[levelIdx]
        val solved = _uiState.value.attempts
            .filter { it.ok && it.rating >= lv.minRating && it.rating < lv.maxRating }
            .map { it.id }
            .toSet()
        val list = allPuzzles
            .filter { it.rating >= lv.minRating && it.rating < lv.maxRating && it.id !in solved }
            .sortedBy { it.rating + Math.random() * 60.0 }
            .take(80)
        if (list.isEmpty()) {
            _uiState.update { it.copy(menuError = "You have solved every puzzle in this level.") }
            return
        }
        _uiState.update { it.copy(levelIndex = levelIdx) }
        beginSession(PuzzleMode.LEVEL, list)
    }

    fun startPractice() {
        val list = filteredPracticePool().shuffled()
        beginSession(PuzzleMode.SET, list)
    }

    fun startQuickDifficulty(tier: String) {
        val list = when (tier) {
            "easy" -> allPuzzles.filter { it.rating in 0..1299 }
            "medium" -> allPuzzles.filter { it.rating in 1300..1850 }
            else -> allPuzzles.filter { it.rating > 1850 }
        }.shuffled()
        if (list.isEmpty()) {
            startPractice()
        } else {
            beginSession(PuzzleMode.SET, list)
        }
    }

    fun startDaily() {
        if (allPuzzles.isEmpty()) {
            _uiState.update { it.copy(menuError = "No puzzles available.") }
            return
        }
        val activeFilter = _uiState.value.difficultyFilter
        val pool = allPuzzles.filter { activeFilter.matches(it) }.ifEmpty { allPuzzles }
        val cal = Calendar.getInstance()
        val daysSinceEpoch = (cal.timeInMillis + cal.timeZone.rawOffset) / 86_400_000L
        val idx = ((daysSinceEpoch % pool.size) + pool.size).toInt() % pool.size
        val dailyPuzzle = pool[idx]
        val followUps = pool
            .filterIndexed { i, p -> i != idx && p.id != dailyPuzzle.id }
            .shuffled(java.util.Random(daysSinceEpoch))
        val seededDailyQueue = listOf(dailyPuzzle) + followUps
        val doneToday = solvedIdsToday(_uiState.value.attempts)
        val ordered = if (doneToday.isNotEmpty()) {
            val (unsolved, solved) = seededDailyQueue.partition { it.id !in doneToday }
            if (unsolved.isNotEmpty()) unsolved + solved else seededDailyQueue
        } else {
            seededDailyQueue
        }
        beginSession(PuzzleMode.DAILY, ordered)
    }

    fun startRandom() {
        val list = filteredPracticePool()
        if (list.isEmpty()) {
            _uiState.update { it.copy(menuError = "No puzzles match this choice. Change the difficulty or theme.") }
            return
        }
        beginSession(PuzzleMode.RAND, listOf(list.random()))
    }

    fun startTimed() {
        val list = filteredPracticePool().shuffled()
        beginSession(PuzzleMode.TIMED, list)
    }

    fun startReviewMistakes() {
        val last = mutableMapOf<String, Boolean>()
        _uiState.value.attempts.forEach { last[it.id] = it.ok }
        val list = allPuzzles.filter { last[it.id] == false }
        if (list.isEmpty()) {
            _uiState.update { it.copy(menuError = "Nothing to review yet. Puzzles you get wrong will appear here.") }
            return
        }
        beginSession(PuzzleMode.REVIEW, list)
    }

    fun startMyGameMistakes() {
        val seen = HashSet<String>()
        val list = mutableListOf<Puzzle>()
        for (h in savedHistory) {
            for (m in h.mistakes) {
                if (seen.add(m.id)) list.add(m)
            }
        }
        if (list.isEmpty()) {
            _uiState.update {
                it.copy(menuError = "Analyse a game first. Your own mistakes and blunders will appear here as puzzles.")
            }
            return
        }
        beginSession(PuzzleMode.REVIEW, list.take(60))
    }

    private fun pickRatedPuzzle(excludeId: String? = _uiState.value.currentPuzzle?.id): Puzzle? {
        val rState = _uiState.value.ratingState
        val seen = rState.seenIds.toSet()
        val ok = allPuzzles.filter { it.rating > 0 && it.id != excludeId }
            .ifEmpty { allPuzzles.filter { it.rating > 0 } }
        for (w in listOf(100, 200, 350, 600, 3000)) {
            val candidates = ok.filter { kotlin.math.abs(it.rating - rState.rating) <= w && it.id !in seen }
            if (candidates.isNotEmpty()) return candidates.random()
        }
        return ok.randomOrNull()
    }

    private fun beginSession(mode: PuzzleMode, rawList: List<Puzzle>) {
        if (rawList.isEmpty()) {
            _uiState.update {
                it.copy(menuError = "No puzzles match this choice. Change the difficulty or theme.")
            }
            return
        }
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                menuError = null,
                inPlay = true,
                mode = mode,
                queue = rawList,
                queueIndex = 0,
                isTimedExpired = false,
                timedRemainingSec = 180,
                timedSolvedCount = 0
            )
        }
        loadPuzzleAt(0, keepOutcome = false)
        if (mode == PuzzleMode.TIMED) {
            startTimedClock()
        }
    }

    private fun startTimedClock() {
        timerJob?.cancel()
        val deadline = System.currentTimeMillis() + 180_000L
        timerJob = viewModelScope.launch {
            while (true) {
                val remSec = ceil((deadline - System.currentTimeMillis()) / 1000.0).toInt().coerceAtLeast(0)
                _uiState.update { it.copy(timedRemainingSec = remSec) }
                if (remSec <= 0) {
                    val solved = _uiState.value.timedSolvedCount
                    _uiState.update {
                        it.copy(
                            isTimedExpired = true,
                            feedbackOk = true,
                            feedbackText = "Time is up! You solved $solved puzzle${if (solved == 1) "" else "s"}."
                        )
                    }
                    break
                }
                delay(250)
            }
        }
    }

    private fun loadPuzzleAt(idx: Int, keepOutcome: Boolean) {
        autoReplyJob?.cancel()
        solutionAnimJob?.cancel()
        playToken++

        val q = _uiState.value.queue.toMutableList()
        var curIdx = idx.coerceIn(0, (q.size - 1).coerceAtLeast(0))
        var prepped: Puzzle? = null
        while (q.isNotEmpty()) {
            val candidate = repo.preparePuzzle(q[curIdx])
            if (candidate != null) {
                prepped = candidate
                q[curIdx] = candidate
                break
            } else {
                q.removeAt(curIdx)
                if (curIdx >= q.size) curIdx = 0
            }
        }

        if (prepped == null) {
            backToMenu()
            _uiState.update { it.copy(menuError = "No valid puzzles found for this choice.") }
            return
        }

        board = ChessBoard(prepped.fen)
        prevLastMoveSquares = emptyList()
        if (!keepOutcome) recordedOutcome = null
        val pColor = board.turn

        _uiState.update {
            it.copy(
                queue = q,
                queueIndex = curIdx,
                currentPuzzle = prepped,
                currentFen = prepped.fen,
                playerColor = pColor,
                flipped = (pColor == PieceColor.BLACK),
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = emptyList(),
                correctMoveSquares = emptyList(),
                isWrongMove = false,
                hintLevel = 0,
                hintSquares = emptyList(),
                showHintModal = false,
                hintPieceName = "",
                hintFromSquare = "",
                hintToSquare = "",
                hintSanMove = "",
                hintThemeLabel = "",
                hintTacticalCue = "",
                solStepIndex = 0,
                isFinished = false,
                feedbackText = "",
                feedbackOk = null,
                solutionExplanation = "",
                lastResultOk = if (keepOutcome) it.lastResultOk else null,
                lastEloDelta = if (keepOutcome) it.lastEloDelta else null,
                isNewBestStreak = false,
                streakMilestoneReached = null,
                justCompletedDailyChallenge = false,
                pendingPromotion = null
            )
        }
    }

    fun onSquareClick(sqName: String) {
        val s = _uiState.value
        val cur = s.currentPuzzle ?: return
        if (s.isFinished || s.isTimedExpired) return

        // Clear wrong move first if tapping any square
        if (s.isWrongMove) {
            board.undo()
            _uiState.update {
                it.copy(
                    currentFen = board.toFen(),
                    isWrongMove = false,
                    correctMoveSquares = emptyList(),
                    showHintModal = false,
                    lastMoveSquares = prevLastMoveSquares,
                    feedbackText = "Back to the puzzle position.",
                    feedbackOk = null
                )
            }
        }

        val sel = _uiState.value.selectedSquare
        if (sel != null) {
            val fromIdx = parseSquare(sel)
            val toIdx = parseSquare(sqName)
            if (fromIdx != null && toIdx != null) {
                val matching = board.legalMoves(fromIdx).filter { it.to == toIdx }
                if (matching.isNotEmpty()) {
                    val isPromo = matching.any { it.promotion != null }
                    val expectedUci = cur.solution.getOrNull(s.solStepIndex)
                    if (isPromo && expectedUci != null && expectedUci.startsWith(sel + sqName)) {
                        val promoType = expectedUci.getOrNull(4)?.let { PieceType.fromChar(it) } ?: PieceType.QUEEN
                        executePlayerMove(sel, sqName, promoType)
                    } else if (isPromo) {
                        _uiState.update { it.copy(pendingPromotion = sel to sqName) }
                    } else {
                        executePlayerMove(sel, sqName, null)
                    }
                    return
                }
            }
        }

        val sqIdx = parseSquare(sqName) ?: return
        val piece = board.pieceAt(sqIdx)
        if (piece != null && piece.color == board.turn && sqName != sel) {
            val targets = board.legalMoves(sqIdx).associate { it.toName to (it.captured != null) }
            _uiState.update { it.copy(selectedSquare = sqName, legalTargets = targets) }
        } else {
            _uiState.update { it.copy(selectedSquare = null, legalTargets = emptyMap()) }
        }
    }

    fun selectPromotion(type: PieceType) {
        val pending = _uiState.value.pendingPromotion ?: return
        _uiState.update { it.copy(pendingPromotion = null) }
        executePlayerMove(pending.first, pending.second, type)
    }

    fun dismissPromotion() {
        _uiState.update { it.copy(pendingPromotion = null) }
    }

    private fun executePlayerMove(from: String, to: String, promo: PieceType?) {
        val s = _uiState.value
        val cur = s.currentPuzzle ?: return
        val step = s.solStepIndex
        val expected = cur.solution.getOrNull(step) ?: return
        val isLastStep = step == cur.solution.lastIndex
        val uciAttempt = from + to + (promo?.symbol?.toString() ?: "")
        val move = board.makeUciMove(uciAttempt) ?: return

        val matchesExpected = (from + to == expected.take(4) && (expected.length < 5 || expected[4] == (promo?.symbol ?: 'q'))) ||
            (isLastStep && cur.isMate && board.inCheckmate())

        if (!matchesExpected) {
            prevLastMoveSquares = s.lastMoveSquares
            recordOutcome(false)
            _uiState.update {
                it.copy(
                    currentFen = board.toFen(),
                    selectedSquare = null,
                    legalTargets = emptyMap(),
                    lastMoveSquares = listOf(from, to),
                    correctMoveSquares = emptyList(),
                    isWrongMove = true,
                    showHintModal = false,
                    feedbackOk = false,
                    feedbackText = "Incorrect move — try again. Tap Undo, or any square, to go back."
                )
            }
            return
        }

        val nextStep = step + 1
        if (nextStep >= cur.solution.size) {
            val clean = recordedOutcome == null
            recordOutcome(true)
            val newSolved = if (s.mode == PuzzleMode.TIMED && clean) s.timedSolvedCount + 1 else s.timedSolvedCount
            val explanation = buildPuzzleExplanation(cur)
            _uiState.update {
                it.copy(
                    currentFen = board.toFen(),
                    selectedSquare = null,
                    legalTargets = emptyMap(),
                    lastMoveSquares = listOf(from, to),
                    correctMoveSquares = listOf(from, to),
                    isWrongMove = false,
                    showHintModal = false,
                    hintLevel = 0,
                    hintSquares = emptyList(),
                    solStepIndex = nextStep,
                    isFinished = true,
                    timedSolvedCount = newSolved,
                    feedbackOk = true,
                    feedbackText = if (clean) "Correct! Solved on the first try." else "Solved! Keep practising.",
                    solutionExplanation = explanation
                )
            }
            val tok = playToken
            val autoShiftMs = if (s.mode == PuzzleMode.TIMED) 500L else 650L
            autoReplyJob = viewModelScope.launch {
                delay(autoShiftMs)
                if (playToken == tok && !_uiState.value.isTimedExpired && _uiState.value.isFinished) {
                    nextPuzzle()
                }
            }
            return
        }

        _uiState.update {
            it.copy(
                currentFen = board.toFen(),
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = listOf(from, to),
                correctMoveSquares = listOf(from, to),
                isWrongMove = false,
                showHintModal = false,
                solStepIndex = nextStep,
                hintLevel = 0,
                hintSquares = emptyList(),
                hintPieceName = "",
                hintFromSquare = "",
                hintToSquare = "",
                hintSanMove = "",
                hintThemeLabel = "",
                hintTacticalCue = "",
                feedbackOk = true,
                feedbackText = "Correct! Keep going…"
            )
        }

        val tok = playToken
        autoReplyJob = viewModelScope.launch {
            delay(500)
            if (playToken != tok || _uiState.value.isWrongMove) return@launch
            val replyUci = cur.solution.getOrNull(nextStep) ?: return@launch
            val rMove = board.makeUciMove(replyUci) ?: return@launch
            _uiState.update {
                it.copy(
                    currentFen = board.toFen(),
                    lastMoveSquares = listOf(rMove.fromName, rMove.toName),
                    correctMoveSquares = emptyList(),
                    solStepIndex = nextStep + 1
                )
            }
        }
    }

    private fun recordOutcome(ok: Boolean) {
        if (recordedOutcome != null) return
        val s = _uiState.value
        val cur = s.currentPuzzle ?: return
        recordedOutcome = ok

        val ts = maxOf(System.currentTimeMillis(), latestAttemptSeqTs + 1L)
        latestAttemptSeqTs = ts
        val newAttempt = PuzzleAttempt(
            id = cur.id,
            timestamp = ts,
            ok = ok,
            rating = cur.rating
        )
        pendingLocalAttempts.add(newAttempt)
        val effectiveAttempts = latestPersistedAttempts + pendingLocalAttempts

        val prevBest = maxOf(s.bestPuzzleStreak, persistedBestPuzzleStreak)
        val (nextStreak, bestFromAttempts) = computeStreaks(effectiveAttempts)
        val nextBest = maxOf(prevBest, bestFromAttempts, nextStreak)
        val isNewBest = ok && nextStreak > prevBest && (prevBest > 0 || nextStreak >= 2)
        val milestone = if (ok) milestoneForStreak(nextStreak) else null
        val prevSolvedToday = s.dailySolvedToday
        val nextSolvedToday = countSolvedToday(effectiveAttempts)
        val hitDailyGoalNow = ok && prevSolvedToday < s.dailyGoal && nextSolvedToday >= s.dailyGoal
        _uiState.update {
            it.copy(
                attempts = effectiveAttempts,
                currentPuzzleStreak = nextStreak,
                bestPuzzleStreak = nextBest,
                isNewBestStreak = isNewBest,
                streakMilestoneReached = milestone,
                dailySolvedToday = nextSolvedToday,
                justCompletedDailyChallenge = hitDailyGoalNow
            )
        }

        viewModelScope.launch {
            attemptWriteMutex.withLock {
                repo.recordPuzzleAttempt(newAttempt)
                if (ok) {
                    repo.incrementPuzzlesSolved()
                    if (nextBest > persistedBestPuzzleStreak) {
                        repo.updateBestPuzzleStreak(nextBest)
                    }
                }

                if (cur.rating > 0 && s.mode != PuzzleMode.REVIEW) {
                    val r = _uiState.value.ratingState
                    val k = when {
                        r.count < 10 -> 40.0
                        r.count < 30 -> 28.0
                        else -> 18.0
                    }
                    val expected = 1.0 / (1.0 + 10.0.pow((cur.rating - r.rating) / 400.0))
                    val delta = k * ((if (ok) 1.0 else 0.0) - expected)
                    val nextRating = (r.rating + delta).coerceAtLeast(100.0)
                    val nextHist = (r.history + (System.currentTimeMillis() to nextRating.roundToInt())).takeLast(100)
                    val nextSeen = (r.seenIds + cur.id).takeLast(400)
                    val nextRatingState = PuzzleRatingState(
                        rating = nextRating,
                        count = r.count + 1,
                        history = nextHist,
                        seenIds = nextSeen
                    )
                    repo.savePuzzleRating(nextRatingState)
                    _uiState.update {
                        it.copy(
                            lastResultOk = ok,
                            lastEloDelta = delta.roundToInt()
                        )
                    }
                } else {
                    _uiState.update { it.copy(lastResultOk = ok, lastEloDelta = null) }
                }
            }
        }
    }

    fun restartPuzzle() {
        loadPuzzleAt(_uiState.value.queueIndex, keepOutcome = true)
    }

    fun undoPuzzleMove() {
        val s = _uiState.value
        if (s.isTimedExpired) return
        if (s.isWrongMove) {
            board.undo()
            _uiState.update {
                it.copy(
                    currentFen = board.toFen(),
                    isWrongMove = false,
                    correctMoveSquares = emptyList(),
                    showHintModal = false,
                    lastMoveSquares = prevLastMoveSquares,
                    selectedSquare = null,
                    legalTargets = emptyMap(),
                    feedbackText = "Back to the puzzle position.",
                    feedbackOk = null
                )
            }
            return
        }
        if (s.solStepIndex > 0) {
            autoReplyJob?.cancel()
            val n = if (s.solStepIndex % 2 == 0) 2 else 1
            if (s.solStepIndex >= n) {
                repeat(n) { board.undo() }
                _uiState.update {
                    it.copy(
                        currentFen = board.toFen(),
                        solStepIndex = s.solStepIndex - n,
                        isFinished = false,
                        lastMoveSquares = emptyList(),
                        correctMoveSquares = emptyList(),
                        hintLevel = 0,
                        hintSquares = emptyList(),
                        showHintModal = false,
                        hintPieceName = "",
                        hintFromSquare = "",
                        hintToSquare = "",
                        hintSanMove = "",
                        hintThemeLabel = "",
                        hintTacticalCue = "",
                        selectedSquare = null,
                        legalTargets = emptyMap(),
                        feedbackText = "",
                        feedbackOk = null
                    )
                }
            }
        }
    }

    fun showHint() {
        val s = _uiState.value
        val cur = s.currentPuzzle ?: return
        if (s.isFinished || s.isTimedExpired) return
        if (s.isWrongMove) {
            board.undo()
        }

        if (s.hintLevel >= 1 && s.hintFromSquare.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    currentFen = board.toFen(),
                    isWrongMove = false,
                    correctMoveSquares = emptyList(),
                    lastMoveSquares = if (s.isWrongMove) prevLastMoveSquares else s.lastMoveSquares,
                    showHintModal = true
                )
            }
            return
        }

        recordOutcome(false)
        val exp = cur.solution.getOrNull(s.solStepIndex)
        val fromSq = if (exp != null && exp.length >= 4) exp.substring(0, 2) else ""
        val toSq = if (exp != null && exp.length >= 4) exp.substring(2, 4) else ""
        val pieceName = parseSquare(fromSq)
            ?.let { board.pieceAt(it)?.type?.pieceName?.replaceFirstChar { c -> c.uppercaseChar() } }
            ?: "Piece"
        val sanMove = if (exp != null) {
            val temp = ChessBoard(board.toFen())
            val mv = temp.makeUciMove(exp)
            temp.sanHistory().lastOrNull() ?: mv?.uci ?: exp
        } else ""
        val primaryTheme = cur.themes
            .firstOrNull { !Regex("^(opening|middlegame|endgame|short|long|veryLong|oneMove)$").matches(it) }
        val themeLabel = when {
            primaryTheme != null -> formatThemeLabel(primaryTheme)
            cur.isMate -> "Checkmate"
            else -> "Tactical Motif"
        }
        val tacticalCue = buildTacticalCue(cur, pieceName, fromSq)

        _uiState.update {
            it.copy(
                currentFen = board.toFen(),
                isWrongMove = false,
                correctMoveSquares = emptyList(),
                lastMoveSquares = if (s.isWrongMove) prevLastMoveSquares else s.lastMoveSquares,
                selectedSquare = null,
                legalTargets = emptyMap(),
                hintLevel = 1,
                hintSquares = if (fromSq.isNotEmpty()) listOf(fromSq) else emptyList(),
                showHintModal = true,
                hintPieceName = pieceName,
                hintFromSquare = fromSq,
                hintToSquare = toSq,
                hintSanMove = sanMove,
                hintThemeLabel = themeLabel,
                hintTacticalCue = tacticalCue,
                feedbackOk = null,
                feedbackText = if (fromSq.isNotEmpty()) "Hint: inspect your $pieceName on $fromSq." else "Hint active."
            )
        }
    }

    fun revealDestinationHint() {
        val s = _uiState.value
        val cur = s.currentPuzzle ?: return
        if (s.isFinished || s.isTimedExpired) return
        recordOutcome(false)
        val exp = cur.solution.getOrNull(s.solStepIndex)
        val fromSq = s.hintFromSquare.ifEmpty { if (exp != null && exp.length >= 4) exp.substring(0, 2) else "" }
        val toSq = s.hintToSquare.ifEmpty { if (exp != null && exp.length >= 4) exp.substring(2, 4) else "" }
        val squares = listOfNotNull(fromSq.takeIf { it.isNotEmpty() }, toSq.takeIf { it.isNotEmpty() })
        _uiState.update {
            it.copy(
                hintLevel = 2,
                hintSquares = squares,
                hintFromSquare = fromSq,
                hintToSquare = toSq,
                showHintModal = true,
                feedbackOk = null,
                feedbackText = if (fromSq.isNotEmpty() && toSq.isNotEmpty()) {
                    "Target revealed: move $fromSq → $toSq."
                } else {
                    "Hint: move the highlighted piece to the target square."
                },
                solutionExplanation = buildPuzzleExplanation(cur)
            )
        }
    }

    fun dismissHintModal() {
        _uiState.update { it.copy(showHintModal = false) }
    }

    private fun buildTacticalCue(puzzle: Puzzle, pieceName: String, fromSq: String): String {
        val sqLabel = if (fromSq.isNotEmpty()) " on ${fromSq.uppercase()}" else ""
        return when {
            puzzle.isMate || puzzle.themes.any { it.contains("mate", ignoreCase = true) } ->
                "Your $pieceName$sqLabel sets up a forcing checkmate net against the enemy king."
            puzzle.themes.any { it.equals("fork", ignoreCase = true) } ->
                "Look for how your $pieceName$sqLabel can attack two valuable targets at once."
            puzzle.themes.any { it.equals("pin", ignoreCase = true) } ->
                "Your $pieceName$sqLabel exploits an alignment along a file, rank, or diagonal."
            puzzle.themes.any { it.equals("skewer", ignoreCase = true) } ->
                "Your $pieceName$sqLabel can drive away a key defender to capture what sits behind it."
            puzzle.themes.any { it.contains("discovered", ignoreCase = true) } ->
                "Moving your $pieceName$sqLabel clears the line for a hidden threat behind it."
            puzzle.themes.any { it.equals("hangingPiece", ignoreCase = true) } ->
                "Your $pieceName$sqLabel targets an undefended piece or decisive material gain."
            puzzle.themes.any { it.equals("sacrifice", ignoreCase = true) } ->
                "Consider a bold forcing sacrifice with your $pieceName$sqLabel to crack the defense."
            else ->
                "Look for forcing checks, captures, or threats starting with your $pieceName$sqLabel."
        }
    }

    private fun buildPuzzleExplanation(puzzle: Puzzle): String {
        val temp = ChessBoard(puzzle.fen)
        val sanTokens = mutableListOf<String>()
        for ((idx, uci) in puzzle.solution.withIndex()) {
            val mv = temp.makeUciMove(uci) ?: break
            val san = temp.sanHistory().lastOrNull() ?: mv.uci
            if (idx % 2 == 0) {
                sanTokens.add("${idx / 2 + 1}. $san")
            } else {
                sanTokens.add(san)
            }
        }
        val lineStr = sanTokens.joinToString(" ")
        val themeHint = when {
            puzzle.isMate || puzzle.themes.any { it.contains("mate", ignoreCase = true) } ->
                "Forces checkmate with a precise attacking sequence."
            puzzle.themes.any { it.equals("fork", ignoreCase = true) } ->
                "Double attack (fork) — hits two valuable targets at once."
            puzzle.themes.any { it.equals("pin", ignoreCase = true) } ->
                "Exploits a pin so the defending piece cannot move safely."
            puzzle.themes.any { it.equals("skewer", ignoreCase = true) } ->
                "Skewer — attacks a high-value piece to win what sits behind it."
            puzzle.themes.any { it.contains("discovered", ignoreCase = true) } ->
                "Discovered attack — moving one piece unleashes another behind it."
            puzzle.themes.any { it.equals("hangingPiece", ignoreCase = true) } ->
                "Wins an undefended piece cleanly."
            puzzle.themes.any { it.equals("sacrifice", ignoreCase = true) } ->
                "Tactical sacrifice that breaks open the king's shelter."
            else -> "Calculates the strongest forcing continuation."
        }
        return if (lineStr.isNotEmpty()) "Line: $lineStr — $themeHint" else themeHint
    }

    fun requestNextPuzzle() {
        val s = _uiState.value
        if (s.isTimedExpired) return
        if (s.isFinished) {
            nextPuzzle()
        } else {
            _uiState.update { it.copy(showSkipDialog = true) }
        }
    }

    fun confirmSkipPuzzle() {
        _uiState.update { it.copy(showSkipDialog = false) }
        recordOutcome(false)
        nextPuzzle()
    }

    fun dismissSkipDialog() {
        _uiState.update { it.copy(showSkipDialog = false) }
    }

    private fun nextPuzzle() {
        val s = _uiState.value
        val curId = s.currentPuzzle?.id
        when (s.mode) {
            PuzzleMode.RATED -> {
                val p = pickRatedPuzzle(excludeId = curId) ?: return
                _uiState.update { it.copy(queue = listOf(p), queueIndex = 0) }
                loadPuzzleAt(0, keepOutcome = false)
            }
            PuzzleMode.RAND -> {
                val list = filteredPracticePool()
                val p = list.filter { it.id != curId }.randomOrNull()
                    ?: allPuzzles.filter { it.id != curId }.randomOrNull()
                    ?: s.currentPuzzle
                    ?: return
                _uiState.update { it.copy(queue = listOf(p), queueIndex = 0) }
                loadPuzzleAt(0, keepOutcome = false)
            }
            else -> {
                if (s.queue.size > 1) {
                    val nextIdx = (s.queueIndex + 1) % s.queue.size
                    loadPuzzleAt(nextIdx, keepOutcome = false)
                } else {
                    val fallback = filteredPracticePool().firstOrNull { it.id != curId }
                        ?: allPuzzles.firstOrNull { it.id != curId }
                    if (fallback != null) {
                        _uiState.update { it.copy(queue = listOf(fallback), queueIndex = 0) }
                    }
                    loadPuzzleAt(0, keepOutcome = false)
                }
            }
        }
    }

    fun toggleFlip() {
        _uiState.update { it.copy(flipped = !it.flipped) }
    }

    fun backToMenu() {
        timerJob?.cancel()
        autoReplyJob?.cancel()
        solutionAnimJob?.cancel()
        playToken++
        _uiState.update {
            it.copy(
                inPlay = false,
                showSkipDialog = false,
                showHintModal = false,
                correctMoveSquares = emptyList()
            )
        }
    }

    fun importCsvText(csvText: String) {
        viewModelScope.launch {
            val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("PuzzleId", ignoreCase = true) }
            val add = mutableListOf<Puzzle>()
            var bad = 0
            for (ln in lines) {
                val c = ln.split(',')
                if (c.size < 8) {
                    bad++
                    continue
                }
                val mv = c[2].trim().split(' ').filter { it.isNotBlank() }
                if (mv.size < 2) {
                    bad++
                    continue
                }
                val b = ChessBoard()
                if (!b.loadFen(c[1]) || b.makeUciMove(mv[0]) == null) {
                    bad++
                    continue
                }
                val startFen = b.toFen()
                val sol = mv.drop(1)
                var valid = true
                for (u in sol) {
                    if (b.makeUciMove(u) == null) {
                        valid = false
                        break
                    }
                }
                if (!valid) {
                    bad++
                    continue
                }
                val th = c[7].trim().split(' ').filter { it.isNotBlank() }
                val isMate = th.any { it.contains("mate", ignoreCase = true) }
                if (isMate && !b.inCheckmate()) {
                    bad++
                    continue
                }
                add.add(
                    Puzzle(
                        id = c[0],
                        fen0 = startFen,
                        setupUci = null,
                        fen = startFen,
                        solution = sol,
                        rating = c[3].toIntOrNull() ?: 0,
                        themes = th,
                        isMate = isMate,
                        source = "Lichess puzzle database (CC0)"
                    )
                )
            }
            if (add.isEmpty()) {
                _uiState.update { it.copy(importStatus = "No usable puzzles found in this input.") }
                return@launch
            }
            repo.addImportedPuzzles(add)
            reloadPool()
            _uiState.update {
                it.copy(importStatus = "Imported ${add.size} puzzles" + if (bad > 0) " (skipped $bad invalid)" else ".")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        autoReplyJob?.cancel()
        solutionAnimJob?.cancel()
    }
}
