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
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt

enum class PuzzleCategory(val id: String, val label: String, val rangeLabel: String) {
    EASY("easy", "Easy", "0–1399"),
    MEDIUM("med", "Medium", "1400–1999"),
    HARD("hard", "Hard", "2000+"),
    GM("gm", "Grandmaster", "2500+"),
    UNRATED("unr", "Starter (unrated)", "KnightScope");

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
    val boardTheme: BoardTheme = BoardTheme.WOOD,
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
    val isWrongMove: Boolean = false,
    val hintLevel: Int = 0,
    val hintSquares: List<String> = emptyList(),
    val solStepIndex: Int = 0,
    val isFinished: Boolean = false,
    val isTimedExpired: Boolean = false,
    val timedRemainingSec: Int = 180,
    val timedSolvedCount: Int = 0,
    val feedbackText: String = "",
    val feedbackOk: Boolean? = null,
    val lastResultOk: Boolean? = null,
    val lastEloDelta: Int? = null,
    val showSkipDialog: Boolean = false,
    val pendingPromotion: Pair<String, String>? = null
)

class PuzzlesViewModel(private val repo: KnightScopeRepository) : ViewModel() {

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
    private var timerJob: Job? = null
    private var autoReplyJob: Job? = null
    private var solutionAnimJob: Job? = null
    private var playToken = 0

    init {
        reloadPool()
        viewModelScope.launch {
            repo.puzzleRatingFlow.collect { r ->
                _uiState.update { it.copy(ratingState = r) }
            }
        }
        viewModelScope.launch {
            repo.puzzleAttemptsFlow.collect { attempts ->
                val lastByPuzzle = mutableMapOf<String, Boolean>()
                attempts.forEach { lastByPuzzle[it.id] = it.ok }
                val wrongCount = allPuzzles.count { lastByPuzzle[it.id] == false }
                _uiState.update { it.copy(attempts = attempts, reviewMistakesCount = wrongCount) }
            }
        }
        viewModelScope.launch {
            repo.dailyGoalFlow.collect { g ->
                _uiState.update { it.copy(dailyGoal = g) }
            }
        }
        viewModelScope.launch {
            repo.boardThemeFlow.collect { t ->
                _uiState.update { it.copy(boardTheme = t) }
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
            val themes = loaded.flatMap { it.themes }.toSet().sorted()
            val lastByPuzzle = mutableMapOf<String, Boolean>()
            _uiState.value.attempts.forEach { lastByPuzzle[it.id] = it.ok }
            val wrongCount = loaded.count { lastByPuzzle[it.id] == false }
            _uiState.update {
                it.copy(
                    isLoadingPool = false,
                    totalPoolSize = loaded.size,
                    categoryCounts = counts,
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
            repo.savePuzzleRating(cur.copy(rating = rating.toDouble(), history = emptyList()))
        }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { repo.setDailyGoal(goal) }
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
            s.selectedCategories.any { it.matches(p) } &&
                (s.selectedTheme.isEmpty() || s.selectedTheme in p.themes)
        }
    }

    fun startRated() {
        val p = pickRatedPuzzle()
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

    fun startDaily() {
        if (allPuzzles.isEmpty()) {
            _uiState.update { it.copy(menuError = "No puzzles available.") }
            return
        }
        val cal = Calendar.getInstance()
        val daysSinceEpoch = (cal.timeInMillis + cal.timeZone.rawOffset) / 86_400_000L
        val idx = ((daysSinceEpoch % allPuzzles.size) + allPuzzles.size).toInt() % allPuzzles.size
        beginSession(PuzzleMode.DAILY, listOf(allPuzzles[idx]))
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

    private fun pickRatedPuzzle(): Puzzle? {
        val rState = _uiState.value.ratingState
        val seen = rState.seenIds.toSet()
        val ok = allPuzzles.filter { it.rating > 0 }
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
                isWrongMove = false,
                hintLevel = 0,
                hintSquares = emptyList(),
                solStepIndex = 0,
                isFinished = false,
                feedbackText = "",
                feedbackOk = null,
                lastResultOk = if (keepOutcome) it.lastResultOk else null,
                lastEloDelta = if (keepOutcome) it.lastEloDelta else null,
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
                    isWrongMove = true,
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
            _uiState.update {
                it.copy(
                    currentFen = board.toFen(),
                    selectedSquare = null,
                    legalTargets = emptyMap(),
                    lastMoveSquares = listOf(from, to),
                    solStepIndex = nextStep,
                    isFinished = true,
                    timedSolvedCount = newSolved,
                    feedbackOk = true,
                    feedbackText = if (clean) "Correct! Solved on the first try." else "Solved! Keep practising."
                )
            }
            if (s.mode != PuzzleMode.DAILY) {
                val tok = playToken
                autoReplyJob = viewModelScope.launch {
                    delay(1000)
                    if (playToken == tok && !_uiState.value.isTimedExpired && _uiState.value.isFinished) {
                        nextPuzzle()
                    }
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
                solStepIndex = nextStep,
                hintLevel = 0,
                hintSquares = emptyList(),
                feedbackOk = true,
                feedbackText = "Correct. Keep going…"
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
        viewModelScope.launch {
            repo.recordPuzzleAttempt(
                PuzzleAttempt(
                    id = cur.id,
                    timestamp = System.currentTimeMillis(),
                    ok = ok,
                    rating = cur.rating
                )
            )
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
                repo.savePuzzleRating(
                    PuzzleRatingState(
                        rating = nextRating,
                        count = r.count + 1,
                        history = nextHist,
                        seenIds = nextSeen
                    )
                )
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
            val n = if (s.solStepIndex % 2 == 0) 2 else 1
            if (s.solStepIndex >= n) {
                repeat(n) { board.undo() }
                _uiState.update {
                    it.copy(
                        currentFen = board.toFen(),
                        solStepIndex = s.solStepIndex - n,
                        isFinished = false,
                        lastMoveSquares = emptyList(),
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
        recordOutcome(false)
        val nextHint = (s.hintLevel + 1).coerceAtMost(2)
        val exp = cur.solution.getOrNull(s.solStepIndex)
        val squares = buildList {
            if (exp != null && exp.length >= 4) {
                add(exp.substring(0, 2))
                if (nextHint >= 2) add(exp.substring(2, 4))
            }
        }
        _uiState.update {
            it.copy(
                currentFen = board.toFen(),
                isWrongMove = false,
                hintLevel = nextHint,
                hintSquares = squares,
                feedbackOk = null,
                feedbackText = if (nextHint == 1) "Hint: look at the highlighted piece." else "Hint: this is the destination square."
            )
        }
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
        when (s.mode) {
            PuzzleMode.RATED -> {
                val p = pickRatedPuzzle() ?: return
                _uiState.update { it.copy(queue = listOf(p), queueIndex = 0) }
                loadPuzzleAt(0, keepOutcome = false)
            }
            PuzzleMode.RAND -> {
                val list = filteredPracticePool()
                val p = list.randomOrNull() ?: s.currentPuzzle ?: return
                _uiState.update { it.copy(queue = listOf(p), queueIndex = 0) }
                loadPuzzleAt(0, keepOutcome = false)
            }
            else -> {
                val nextIdx = if (s.queue.size > 1) (s.queueIndex + 1) % s.queue.size else 0
                loadPuzzleAt(nextIdx, keepOutcome = false)
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
        _uiState.update { it.copy(inPlay = false, showSkipDialog = false) }
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
