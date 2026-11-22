package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.example.data.RecentPlayGameSummary
import com.example.ui.theme.BoardTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class QuickPlayTimeControl(
    val id: String,
    val label: String,
    val category: String,
    val baseMinutes: Int,
    val incrementSec: Int
) {
    UNTIMED("untimed", "Untimed", "Casual", 0, 0),
    BULLET_1("bullet_1", "1+0", "Bullet", 1, 0),
    BULLET_2_1("bullet_2_1", "2+1", "Bullet", 2, 1),
    BLITZ_3("blitz_3", "3+0", "Blitz", 3, 0),
    BLITZ_5("blitz_5", "5+0", "Blitz", 5, 0),
    RAPID_10("rapid_10", "10+0", "Rapid", 10, 0),
    RAPID_15_10("rapid_15_10", "15+10", "Rapid", 15, 10);

    val baseMillis: Long? get() = if (baseMinutes > 0) baseMinutes * 60_000L else null
    val incrementMillis: Long get() = incrementSec * 1_000L
}

data class PlayUiState(
    val inGame: Boolean = false,
    val difficulty: EngineDifficulty = EngineDifficulty.MEDIUM,
    val colorChoice: ColorChoice = ColorChoice.WHITE,
    val timeControl: QuickPlayTimeControl = QuickPlayTimeControl.UNTIMED,
    val whiteTimeMs: Long? = null,
    val blackTimeMs: Long? = null,
    val showLiveEval: Boolean = true,
    val boardTheme: BoardTheme = BoardTheme.GOLD,
    val playerName: String = "You",
    val playerColor: PieceColor = PieceColor.WHITE,
    val fen: String = ChessBoard.START_FEN,
    val turn: PieceColor = PieceColor.WHITE,
    val flipped: Boolean = false,
    val selectedSquare: String? = null,
    val legalTargets: Map<String, Boolean> = emptyMap(),
    val lastMoveSquares: List<String> = emptyList(),
    val lastMoveSan: String = "",
    val sanMoves: List<String> = emptyList(),
    val isEngineThinking: Boolean = false,
    val liveEvalCp: Double = 0.2,
    val liveEvalMate: Int? = null,
    val engineSourceLabel: String = "Stockfish Engine",
    val openingEco: String? = null,
    val openingName: String? = null,
    val hintFrom: String? = null,
    val hintTo: String? = null,
    val hintText: String? = null,
    val isHintLoading: Boolean = false,
    val isGameOver: Boolean = false,
    val isPlayerWin: Boolean? = null,
    val resultString: String = "*",
    val endMessage: String = "",
    val canTakeback: Boolean = false,
    val pendingPromotion: Pair<String, String>? = null,
    val recentGames: List<RecentPlayGameSummary> = emptyList()
)

class PlayViewModel(
    private val repo: KnightScopeRepository,
    private val engine: ChessEngine = ChessEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayUiState())
    val uiState: StateFlow<PlayUiState> = _uiState.asStateFlow()

    private var board = ChessBoard()
    private val evalHistory = mutableListOf(0.2)
    private var gameToken = 0
    private var recordedFinish = false

    private var botJob: Job? = null
    private var liveEvalJob: Job? = null
    private var hintJob: Job? = null
    private var clockJob: Job? = null

    init {
        viewModelScope.launch {
            repo.boardThemeFlow.collect { theme ->
                _uiState.update { it.copy(boardTheme = theme) }
            }
        }
        viewModelScope.launch {
            repo.playDifficultyFlow.collect { diff ->
                _uiState.update { it.copy(difficulty = diff) }
            }
        }
        viewModelScope.launch {
            repo.playShowLiveEvalFlow.collect { show ->
                _uiState.update { it.copy(showLiveEval = show) }
            }
        }
        viewModelScope.launch {
            repo.userProfileFlow.collect { profile ->
                _uiState.update { it.copy(playerName = profile.displayName) }
            }
        }
        viewModelScope.launch {
            repo.recentPlayGamesFlow.collect { recents ->
                _uiState.update { it.copy(recentGames = recents) }
            }
        }
    }

    fun setDifficulty(difficulty: EngineDifficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
        viewModelScope.launch {
            repo.setPlayDifficulty(difficulty)
        }
    }

    fun selectColorChoice(choice: ColorChoice) {
        _uiState.update { it.copy(colorChoice = choice) }
    }

    fun selectTimeControl(tc: QuickPlayTimeControl) {
        _uiState.update { it.copy(timeControl = tc) }
    }

    fun startQuickPlay(tc: QuickPlayTimeControl) {
        _uiState.update { it.copy(timeControl = tc) }
        startGame()
    }

    fun setShowLiveEval(show: Boolean) {
        _uiState.update { it.copy(showLiveEval = show) }
        viewModelScope.launch {
            repo.setPlayShowLiveEval(show)
        }
        if (show && _uiState.value.inGame && !_uiState.value.isGameOver) {
            refreshLiveEval(gameToken)
        }
    }

    fun startGame(playerColorOverride: PieceColor? = null) {
        cancelEngineJobs()
        gameToken++
        recordedFinish = false
        board = ChessBoard()
        evalHistory.clear()
        evalHistory.add(0.2)

        val s = _uiState.value
        val pColor = playerColorOverride ?: when (s.colorChoice) {
            ColorChoice.WHITE -> PieceColor.WHITE
            ColorChoice.BLACK -> PieceColor.BLACK
            ColorChoice.RANDOM -> if (kotlin.random.Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
        }
        val initialTimeMs = s.timeControl.baseMillis

        _uiState.update {
            it.copy(
                inGame = true,
                playerColor = pColor,
                whiteTimeMs = initialTimeMs,
                blackTimeMs = initialTimeMs,
                fen = board.toFen(),
                turn = PieceColor.WHITE,
                flipped = (pColor == PieceColor.BLACK),
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = emptyList(),
                lastMoveSan = "",
                sanMoves = emptyList(),
                isEngineThinking = (pColor == PieceColor.BLACK),
                liveEvalCp = 0.2,
                liveEvalMate = null,
                openingEco = null,
                openingName = null,
                hintFrom = null,
                hintTo = null,
                hintText = null,
                isHintLoading = false,
                isGameOver = false,
                isPlayerWin = null,
                resultString = "*",
                endMessage = "",
                canTakeback = false,
                pendingPromotion = null
            )
        }

        if (initialTimeMs != null) {
            startClockTicker(gameToken)
        }
        if (pColor == PieceColor.BLACK) {
            triggerBotTurn(gameToken)
        }
    }

    fun rematch() {
        val nextColor = _uiState.value.playerColor.opposite()
        startGame(playerColorOverride = nextColor)
    }

    fun leaveGame() {
        cancelEngineJobs()
        gameToken++
        _uiState.update {
            it.copy(
                inGame = false,
                isEngineThinking = false,
                pendingPromotion = null
            )
        }
    }

    fun toggleFlip() {
        _uiState.update { it.copy(flipped = !it.flipped) }
    }

    fun onSquareClick(sqName: String) {
        val s = _uiState.value
        if (!s.inGame || s.isGameOver || s.isEngineThinking) return
        if (board.turn != s.playerColor) return

        val sel = s.selectedSquare
        if (sel != null) {
            val fromIdx = parseSquare(sel)
            val toIdx = parseSquare(sqName)
            if (fromIdx != null && toIdx != null) {
                val matching = board.legalMoves(fromIdx).filter { it.to == toIdx }
                if (matching.isNotEmpty()) {
                    if (matching.any { it.promotion != null }) {
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
        val uci = from + to + (promo?.symbol?.toString() ?: "")
        val move = board.makeUciMove(uci) ?: return
        val curTok = gameToken
        hintJob?.cancel()

        syncAfterMove(move)
        if (!board.isGameOver()) {
            triggerBotTurn(curTok)
        }
    }

    private fun triggerBotTurn(tok: Int) {
        botJob?.cancel()
        val fenBeforeBot = board.toFen()
        val difficulty = _uiState.value.difficulty

        _uiState.update { it.copy(isEngineThinking = true, hintFrom = null, hintTo = null, hintText = null) }

        botJob = viewModelScope.launch {
            delay(220)
            if (tok != gameToken) return@launch

            val evalRes = engine.playBotMove(fenBeforeBot, difficulty)
            if (tok != gameToken) return@launch

            val legal = board.legalMoves()
            val chosenUci = evalRes.bestUci?.takeIf { u -> legal.any { it.uci == u } }
                ?: legal.firstOrNull()?.uci

            if (chosenUci == null) {
                _uiState.update { it.copy(isEngineThinking = false) }
                return@launch
            }

            val move = board.makeUciMove(chosenUci)
            if (move != null) {
                evalHistory.add(evalRes.cp)
                syncAfterMove(move, precomputedEval = evalRes)
            } else {
                _uiState.update { it.copy(isEngineThinking = false) }
            }
        }
    }

    private fun syncAfterMove(lastMove: Move, precomputedEval: EngineEval? = null) {
        val s = _uiState.value
        val nextTurn = board.turn
        val sans = board.sanHistory()
        var over = false
        var resStr = "*"
        var endMsg = ""
        var playerWon = false

        if (board.isGameOver()) {
            over = true
            when {
                board.inCheckmate() -> {
                    val winner = nextTurn.opposite()
                    resStr = if (winner == PieceColor.WHITE) "1-0" else "0-1"
                    playerWon = (winner == s.playerColor)
                    endMsg = if (playerWon) {
                        "Checkmate — you beat Stockfish (${s.difficulty.label})!"
                    } else {
                        "Checkmate — Stockfish wins."
                    }
                }
                board.inStalemate() -> {
                    resStr = "1/2-1/2"
                    endMsg = "Draw by stalemate"
                }
                board.inThreefoldRepetition() -> {
                    resStr = "1/2-1/2"
                    endMsg = "Draw by repetition"
                }
                board.insufficientMaterial() -> {
                    resStr = "1/2-1/2"
                    endMsg = "Draw — insufficient material"
                }
                else -> {
                    resStr = "1/2-1/2"
                    endMsg = "Draw by the 50-move rule"
                }
            }
        }

        val cp = precomputedEval?.cp ?: evalHistory.lastOrNull() ?: 0.0
        if (precomputedEval == null) {
            evalHistory.add(cp)
        }

        val canTakebackNow = !over && (
            (board.turn == s.playerColor && board.moveHistoryCount() >= 2) ||
                (board.turn != s.playerColor && board.moveHistoryCount() >= 1)
            )

        // Apply increment to the side that just moved when playing a timed mode
        val incMs = s.timeControl.incrementMillis
        val nextWhiteMs = if (!over && incMs > 0L && lastMove.color == PieceColor.WHITE && s.whiteTimeMs != null) {
            s.whiteTimeMs + incMs
        } else s.whiteTimeMs
        val nextBlackMs = if (!over && incMs > 0L && lastMove.color == PieceColor.BLACK && s.blackTimeMs != null) {
            s.blackTimeMs + incMs
        } else s.blackTimeMs

        val recognizedOpening = if (sans.size <= 30) OpeningBook.lookup(board.toFen()) else null
        val nextOpeningEco = recognizedOpening?.eco ?: s.openingEco
        val nextOpeningName = recognizedOpening?.name ?: s.openingName
        val playerWinOutcome: Boolean? = if (over) {
            if (board.inCheckmate()) playerWon else null
        } else null

        _uiState.update {
            it.copy(
                fen = board.toFen(),
                turn = nextTurn,
                whiteTimeMs = nextWhiteMs,
                blackTimeMs = nextBlackMs,
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = listOf(lastMove.fromName, lastMove.toName),
                lastMoveSan = sans.lastOrNull().orEmpty(),
                sanMoves = sans,
                isEngineThinking = false,
                liveEvalCp = cp,
                liveEvalMate = precomputedEval?.mate,
                engineSourceLabel = precomputedEval?.sourceLabel ?: it.engineSourceLabel,
                openingEco = nextOpeningEco,
                openingName = nextOpeningName,
                hintFrom = null,
                hintTo = null,
                hintText = null,
                isGameOver = over,
                isPlayerWin = playerWinOutcome,
                resultString = resStr,
                endMessage = endMsg,
                canTakeback = canTakebackNow
            )
        }

        if (over) {
            clockJob?.cancel()
            recordMatchOnce(playerWon)
        } else if (precomputedEval == null && _uiState.value.showLiveEval) {
            refreshLiveEval(gameToken)
        }
    }

    private fun startClockTicker(tok: Int) {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (tok == gameToken) {
                delay(200)
                if (tok != gameToken) break
                val now = System.currentTimeMillis()
                val elapsed = (now - lastTick).coerceAtLeast(0L)
                lastTick = now

                val s = _uiState.value
                if (!s.inGame || s.isGameOver) break
                if (s.sanMoves.isEmpty()) continue // clocks begin on first move

                if (s.turn == PieceColor.WHITE && s.whiteTimeMs != null) {
                    val remaining = (s.whiteTimeMs - elapsed).coerceAtLeast(0L)
                    if (remaining == 0L) {
                        onFlagged(PieceColor.WHITE)
                        break
                    } else {
                        _uiState.update { it.copy(whiteTimeMs = remaining) }
                    }
                } else if (s.turn == PieceColor.BLACK && s.blackTimeMs != null) {
                    val remaining = (s.blackTimeMs - elapsed).coerceAtLeast(0L)
                    if (remaining == 0L) {
                        onFlagged(PieceColor.BLACK)
                        break
                    } else {
                        _uiState.update { it.copy(blackTimeMs = remaining) }
                    }
                }
            }
        }
    }

    private fun onFlagged(flaggedSide: PieceColor) {
        val s = _uiState.value
        if (!s.inGame || s.isGameOver) return
        cancelEngineJobs()
        gameToken++
        val winner = flaggedSide.opposite()
        val playerWon = (winner == s.playerColor)
        val resStr = if (winner == PieceColor.WHITE) "1-0" else "0-1"
        val endMsg = if (playerWon) {
            "Stockfish flagged on time — you win!"
        } else {
            "Time expired — Stockfish wins."
        }
        _uiState.update {
            it.copy(
                whiteTimeMs = if (flaggedSide == PieceColor.WHITE) 0L else it.whiteTimeMs,
                blackTimeMs = if (flaggedSide == PieceColor.BLACK) 0L else it.blackTimeMs,
                isEngineThinking = false,
                isGameOver = true,
                isPlayerWin = playerWon,
                resultString = resStr,
                endMessage = endMsg,
                canTakeback = false
            )
        }
        recordMatchOnce(playerWon)
    }

    private fun refreshLiveEval(tok: Int) {
        liveEvalJob?.cancel()
        val targetFen = board.toFen()
        liveEvalJob = viewModelScope.launch {
            val ev = engine.evaluateForLiveBar(targetFen)
            if (tok != gameToken) return@launch
            if (evalHistory.isNotEmpty()) {
                evalHistory[evalHistory.lastIndex] = ev.cp
            }
            _uiState.update {
                it.copy(
                    liveEvalCp = ev.cp,
                    liveEvalMate = ev.mate,
                    engineSourceLabel = ev.sourceLabel
                )
            }
        }
    }

    fun requestHint() {
        val s = _uiState.value
        if (!s.inGame || s.isGameOver || s.isEngineThinking || s.isHintLoading) return
        if (board.turn != s.playerColor) return

        val tok = gameToken
        val targetFen = board.toFen()
        _uiState.update { it.copy(isHintLoading = true, hintText = null) }

        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            val ev = engine.getHint(targetFen)
            if (tok != gameToken) return@launch
            val bestUci = ev.bestUci
            if (bestUci == null || bestUci.length < 4) {
                _uiState.update { it.copy(isHintLoading = false, hintText = "No hint available.") }
                return@launch
            }
            val from = bestUci.substring(0, 2)
            val to = bestUci.substring(2, 4)
            val temp = ChessBoard(targetFen)
            val san = if (temp.makeUciMove(bestUci) != null) temp.sanHistory().lastOrNull() ?: bestUci else bestUci
            _uiState.update {
                it.copy(
                    isHintLoading = false,
                    hintFrom = from,
                    hintTo = to,
                    hintText = "Hint: try $san (${ev.sourceLabel})",
                    liveEvalCp = ev.cp,
                    liveEvalMate = ev.mate
                )
            }
        }
    }

    fun takeback() {
        val s = _uiState.value
        if (!s.inGame || s.isGameOver) return

        cancelEngineJobs()
        gameToken++

        val pliesToUndo = if (board.turn == s.playerColor) 2 else 1
        if (board.moveHistoryCount() < pliesToUndo) return

        repeat(pliesToUndo) {
            board.undo()
            if (evalHistory.size > 1) {
                evalHistory.removeAt(evalHistory.lastIndex)
            }
        }

        val sans = board.sanHistory()
        val lastMove = board.moveHistory().lastOrNull()
        val lastSq = if (lastMove != null) {
            listOf(lastMove.fromName, lastMove.toName)
        } else emptyList()

        val canTakebackAgain = board.turn == s.playerColor && board.moveHistoryCount() >= 2
        val updatedOpening = findDeepestOpening(sans)

        _uiState.update {
            it.copy(
                fen = board.toFen(),
                turn = board.turn,
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = lastSq,
                lastMoveSan = sans.lastOrNull().orEmpty(),
                sanMoves = sans,
                isEngineThinking = false,
                liveEvalCp = evalHistory.lastOrNull() ?: 0.2,
                liveEvalMate = null,
                openingEco = updatedOpening?.eco,
                openingName = updatedOpening?.name,
                hintFrom = null,
                hintTo = null,
                hintText = null,
                isHintLoading = false,
                canTakeback = canTakebackAgain
            )
        }

        if (_uiState.value.showLiveEval) {
            refreshLiveEval(gameToken)
        }
    }

    private fun findDeepestOpening(sans: List<String>): OpeningEntry? {
        if (sans.isEmpty()) return null
        val replay = ChessBoard()
        var latest: OpeningEntry? = null
        for (san in sans.take(30)) {
            if (replay.makeSanMove(san) == null) break
            OpeningBook.lookup(replay.toFen())?.let { latest = it }
        }
        return latest
    }

    fun resign() {
        val s = _uiState.value
        if (!s.inGame || s.isGameOver) return
        cancelEngineJobs()
        gameToken++
        val resStr = if (s.playerColor == PieceColor.WHITE) "0-1" else "1-0"
        _uiState.update {
            it.copy(
                isEngineThinking = false,
                isGameOver = true,
                isPlayerWin = false,
                resultString = resStr,
                endMessage = "You resigned — Stockfish wins.",
                canTakeback = false
            )
        }
        recordMatchOnce(playerWon = false)
    }

    fun buildFinishedPgn(): String {
        val s = _uiState.value
        val copy = board.copy()
        val botLabel = "Stockfish (${s.difficulty.label})"
        val white = if (s.playerColor == PieceColor.WHITE) s.playerName else botLabel
        val black = if (s.playerColor == PieceColor.BLACK) s.playerName else botLabel
        copy.setHeader("Event", "Diggy Chess vs Stockfish (${s.difficulty.label})")
        copy.setHeader("White", white)
        copy.setHeader("Black", black)
        copy.setHeader("Result", s.resultString)
        return copy.toPgn(s.resultString)
    }

    private fun recordMatchOnce(playerWon: Boolean) {
        if (recordedFinish) return
        recordedFinish = true
        val s = _uiState.value
        val pgn = buildFinishedPgn()
        val now = System.currentTimeMillis()
        val summary = RecentPlayGameSummary(
            id = "play_$now",
            title = "${s.playerName} vs Stockfish (${s.difficulty.label})",
            result = s.resultString,
            timeControlLabel = s.timeControl.label,
            difficultyLabel = s.difficulty.label,
            movesCount = (s.sanMoves.size + 1) / 2,
            timestamp = now,
            pgn = pgn
        )
        viewModelScope.launch {
            repo.recordBotGameResult(playerWon)
            repo.recordPlayGameSummary(summary)
        }
    }

    private fun cancelEngineJobs() {
        botJob?.cancel()
        liveEvalJob?.cancel()
        hintJob?.cancel()
        clockJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        cancelEngineJobs()
    }
}
