package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.example.service.PlatformGameFetcher
import com.example.ui.theme.BoardTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AnalyzeViewModel(
    private val repo: KnightScopeRepository,
    private val analyzer: GameAnalyzer = GameAnalyzer(),
    private val engine: ChessEngine = ChessEngine()
) : ViewModel() {

    companion object {
        const val SAMPLE_PGN = "[Event \"Sample\"]\n[White \"White\"]\n[Black \"Black\"]\n[Result \"1-0\"]\n\n1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0"
    }

    private val _uiState = MutableStateFlow(AnalyzeUiState())
    val uiState: StateFlow<AnalyzeUiState> = _uiState.asStateFlow()

    private var plyEvalJob: Job? = null
    private var variationEvalJob: Job? = null
    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            repo.settingsFlow.collect { s ->
                _uiState.update { it.copy(settings = s) }
            }
        }
        viewModelScope.launch {
            repo.boardThemeFlow.collect { t ->
                _uiState.update { it.copy(boardTheme = t) }
            }
        }
        viewModelScope.launch {
            repo.historyFlow.collect { hist ->
                _uiState.update { it.copy(recentGames = hist.take(4)) }
            }
        }
    }

    fun updatePgn(text: String) {
        _uiState.update { it.copy(pgnText = text, errorMessage = null, statusMessage = null) }
    }

    fun trySample() {
        _uiState.update { it.copy(pgnText = SAMPLE_PGN, errorMessage = null, statusMessage = null) }
        analyzeGame()
    }

    fun updateSettings(newSettings: ReviewSettings) {
        _uiState.update { it.copy(settings = newSettings) }
        viewModelScope.launch {
            repo.saveSettings(newSettings)
        }
    }

    fun setBoardTheme(theme: BoardTheme) {
        _uiState.update { it.copy(boardTheme = theme) }
        viewModelScope.launch {
            repo.setBoardTheme(theme)
        }
    }

    // ===== Chess.com & Lichess Username Importer =====

    fun openImporterSheet() {
        _uiState.update { it.copy(showImporterSheet = true, platformError = null) }
    }

    fun closeImporterSheet() {
        _uiState.update { it.copy(showImporterSheet = false) }
    }

    fun setImportPlatform(platform: ChessPlatform) {
        _uiState.update {
            it.copy(
                importPlatform = platform,
                platformError = null
            )
        }
    }

    fun updateImportUsername(username: String) {
        _uiState.update { it.copy(importUsername = username, platformError = null) }
    }

    fun fetchPlatformGames() {
        val s = _uiState.value
        val name = s.importUsername.trim()
        if (name.isEmpty()) {
            _uiState.update { it.copy(platformError = "Enter a ${s.importPlatform.label} username.") }
            return
        }
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isFetchingPlatform = true,
                    platformError = null
                )
            }
            val result = PlatformGameFetcher.fetchUserGamesAndStats(
                platform = s.importPlatform,
                rawUsername = name,
                maxGames = 30
            )
            result.fold(
                onSuccess = { fetched ->
                    _uiState.update { cur ->
                        val filtered = applyImportFilters(
                            games = fetched.games,
                            speed = cur.speedFilter,
                            resultFilter = cur.resultFilter,
                            color = cur.colorFilter
                        )
                        cur.copy(
                            isFetchingPlatform = false,
                            platformError = null,
                            platformProfile = fetched.profile,
                            allImportedGames = fetched.games,
                            filteredImportedGames = filtered
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isFetchingPlatform = false,
                            platformError = err.localizedMessage ?: "Failed to load games."
                        )
                    }
                }
            )
        }
    }

    fun setImportSpeedFilter(filter: ImportSpeedFilter) {
        _uiState.update { cur ->
            cur.copy(
                speedFilter = filter,
                filteredImportedGames = applyImportFilters(cur.allImportedGames, filter, cur.resultFilter, cur.colorFilter)
            )
        }
    }

    fun setImportResultFilter(filter: ImportResultFilter) {
        _uiState.update { cur ->
            cur.copy(
                resultFilter = filter,
                filteredImportedGames = applyImportFilters(cur.allImportedGames, cur.speedFilter, filter, cur.colorFilter)
            )
        }
    }

    fun setImportColorFilter(filter: ImportColorFilter) {
        _uiState.update { cur ->
            cur.copy(
                colorFilter = filter,
                filteredImportedGames = applyImportFilters(cur.allImportedGames, cur.speedFilter, cur.resultFilter, filter)
            )
        }
    }

    fun importAndAnalyzeGame(game: ImportedGameSummary) {
        val shouldFlip = game.userColor == "black"
        _uiState.update {
            it.copy(
                showImporterSheet = false,
                pgnText = game.pgn,
                errorMessage = null,
                statusMessage = null
            )
        }
        analyzeGame(flipOnFinish = shouldFlip)
    }

    private fun applyImportFilters(
        games: List<ImportedGameSummary>,
        speed: ImportSpeedFilter,
        resultFilter: ImportResultFilter,
        color: ImportColorFilter
    ): List<ImportedGameSummary> {
        return games.filter { g ->
            val speedOk = when (speed) {
                ImportSpeedFilter.ALL -> true
                ImportSpeedFilter.BULLET -> g.timeClass == "bullet"
                ImportSpeedFilter.BLITZ -> g.timeClass == "blitz"
                ImportSpeedFilter.RAPID -> g.timeClass == "rapid"
                ImportSpeedFilter.CLASSICAL -> g.timeClass == "classical" || g.timeClass == "daily" || g.timeClass == "correspondence"
            }
            val resOk = when (resultFilter) {
                ImportResultFilter.ALL -> true
                ImportResultFilter.WIN -> g.userOutcome == "win"
                ImportResultFilter.LOSS -> g.userOutcome == "loss"
                ImportResultFilter.DRAW -> g.userOutcome == "draw"
            }
            val colorOk = when (color) {
                ImportColorFilter.BOTH -> true
                ImportColorFilter.WHITE -> g.userColor == "white"
                ImportColorFilter.BLACK -> g.userColor == "black"
            }
            speedOk && resOk && colorOk
        }
    }

    // ===== Core PGN Game Analysis =====

    fun analyzeGame(
        savedEntry: SavedGameEntry? = null,
        fromHistory: Boolean = false,
        flipOnFinish: Boolean = false
    ) {
        val text = savedEntry?.pgn ?: _uiState.value.pgnText
        if (text.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Could not read this PGN. Paste the full PGN, including the headers.")
            }
            return
        }

        if (_uiState.value.isAnalyzing) return

        plyEvalJob?.cancel()
        variationEvalJob?.cancel()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pgnText = text,
                    isAnalyzing = true,
                    progressDone = 0,
                    progressTotal = 0,
                    errorMessage = null,
                    statusMessage = null,
                    variation = null,
                    boardSelectedSquare = null,
                    boardLegalTargets = emptyMap(),
                    boardPendingPromotion = null
                )
            }
            try {
                val res = analyzer.analyze(
                    pgnText = text,
                    settings = _uiState.value.settings,
                    savedEntry = savedEntry,
                    onProgress = { done, total ->
                        _uiState.update { it.copy(progressDone = done, progressTotal = total) }
                    }
                )
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        result = res,
                        currentPly = 0,
                        showBestMove = false,
                        flipped = flipOnFinish,
                        errorMessage = null,
                        plyLiveLines = emptyList()
                    )
                }
                loadPlyMultiPvIfMissing(res, 0)
                if (!fromHistory) {
                    val saved = analyzer.toSavedGameEntry(res)
                    repo.saveGameToHistory(saved)
                    repo.incrementGamesAnalysed()
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = e.message ?: "Could not analyse this PGN."
                    )
                }
            }
        }
    }

    fun openFromHistory(entry: SavedGameEntry) {
        if (entry.evals.size == entry.plies && entry.plies > 0) {
            analyzeGame(savedEntry = entry, fromHistory = true)
        } else {
            _uiState.update {
                it.copy(
                    pgnText = entry.pgn,
                    errorMessage = null,
                    statusMessage = "Saved analysis is not available for this game. Tap Analyse game to run it again."
                )
            }
        }
    }

    fun loadAndAnalyzePgn(pgn: String) {
        _uiState.update { it.copy(pgnText = pgn, errorMessage = null, statusMessage = null) }
        analyzeGame()
    }

    fun goToPly(ply: Int, showBest: Boolean = false) {
        val res = _uiState.value.result ?: return
        val clamped = ply.coerceIn(0, res.plies.size)
        val canShowBest = showBest && clamped > 0 && res.plies[clamped - 1].bestFrom != null
        variationEvalJob?.cancel()
        _uiState.update {
            it.copy(
                currentPly = clamped,
                showBestMove = canShowBest,
                variation = null,
                boardSelectedSquare = null,
                boardLegalTargets = emptyMap(),
                boardPendingPromotion = null
            )
        }
        loadPlyMultiPvIfMissing(res, clamped)
    }

    private fun loadPlyMultiPvIfMissing(res: GameAnalysisResult, ply: Int) {
        plyEvalJob?.cancel()
        val precomputed = if (ply in 1..res.plies.size) res.plies[ply - 1].topLines else emptyList()
        if (precomputed.isNotEmpty()) {
            _uiState.update { it.copy(plyLiveLines = precomputed, isLoadingPlyLines = false) }
            return
        }
        val fen = currentMainFen(res, ply)
        _uiState.update { it.copy(plyLiveLines = emptyList(), isLoadingPlyLines = true) }
        plyEvalJob = viewModelScope.launch {
            delay(80)
            val ev = engine.evaluateMultiPv(fen, depth = 8, multiPv = 3, timeLimitMs = 600L)
            if (_uiState.value.result?.id == res.id && _uiState.value.currentPly == ply) {
                _uiState.update { it.copy(plyLiveLines = ev.topLines, isLoadingPlyLines = false) }
            }
        }
    }

    fun toggleBestMove() {
        val s = _uiState.value
        val ply = s.result?.plies?.getOrNull(s.currentPly - 1)
        if (ply?.bestFrom != null) {
            _uiState.update { it.copy(showBestMove = !it.showBestMove) }
        }
    }

    fun toggleFlip() {
        _uiState.update { it.copy(flipped = !it.flipped, showBestMove = false) }
    }

    // ===== Interactive Variation Explorer =====

    fun onBoardSquareClick(sqName: String) {
        val s = _uiState.value
        val res = s.result ?: return
        val branch = s.variation
        val activeFen = branch?.activeFen ?: currentMainFen(res, s.currentPly)
        val board = ChessBoard(activeFen)

        val currentSelected = branch?.selectedSquare ?: s.boardSelectedSquare
        if (currentSelected != null) {
            val fromIdx = parseSquare(currentSelected)
            val toIdx = parseSquare(sqName)
            if (fromIdx != null && toIdx != null) {
                val matching = board.legalMoves(fromIdx).filter { it.to == toIdx }
                if (matching.isNotEmpty()) {
                    if (matching.any { it.promotion != null }) {
                        if (branch != null) {
                            _uiState.update {
                                it.copy(variation = branch.copy(pendingPromotion = currentSelected to sqName))
                            }
                        } else {
                            _uiState.update { it.copy(boardPendingPromotion = currentSelected to sqName) }
                        }
                    } else {
                        playVariationMove(currentSelected, sqName, null)
                    }
                    return
                }
            }
        }

        val sqIdx = parseSquare(sqName) ?: return
        val piece = board.pieceAt(sqIdx)
        if (piece != null && piece.color == board.turn && sqName != currentSelected) {
            val targets = board.legalMoves(sqIdx).associate { it.toName to (it.captured != null) }
            if (branch != null) {
                _uiState.update {
                    it.copy(variation = branch.copy(selectedSquare = sqName, legalTargets = targets))
                }
            } else {
                _uiState.update {
                    it.copy(boardSelectedSquare = sqName, boardLegalTargets = targets)
                }
            }
        } else {
            if (branch != null) {
                _uiState.update {
                    it.copy(variation = branch.copy(selectedSquare = null, legalTargets = emptyMap()))
                }
            } else {
                _uiState.update {
                    it.copy(boardSelectedSquare = null, boardLegalTargets = emptyMap())
                }
            }
        }
    }

    fun selectVariationPromotion(type: PieceType) {
        val s = _uiState.value
        val pending = s.variation?.pendingPromotion ?: s.boardPendingPromotion ?: return
        _uiState.update {
            it.copy(
                boardPendingPromotion = null,
                variation = it.variation?.copy(pendingPromotion = null)
            )
        }
        playVariationMove(pending.first, pending.second, type)
    }

    fun dismissVariationPromotion() {
        _uiState.update {
            it.copy(
                boardPendingPromotion = null,
                variation = it.variation?.copy(pendingPromotion = null)
            )
        }
    }

    fun playEngineLineMoves(line: EngineVariation, upToIndex: Int = 0) {
        val s = _uiState.value
        val res = s.result ?: return
        val existingBranch = s.variation
        val anchorPly = existingBranch?.anchorPly ?: s.currentPly
        val baseFen = existingBranch?.baseFen ?: currentMainFen(res, anchorPly)
        val prefix = existingBranch?.visibleSteps.orEmpty().toMutableList()

        val board = ChessBoard(existingBranch?.activeFen ?: baseFen)
        val uciList = line.pvUci.take((upToIndex + 1).coerceAtLeast(1))
        for (uci in uciList) {
            val m = board.makeUciMove(uci) ?: break
            val san = board.sanHistory().lastOrNull() ?: uci
            prefix.add(
                VariationStep(
                    san = san,
                    uci = m.uci,
                    from = m.fromName,
                    to = m.toName,
                    fenAfter = board.toFen()
                )
            )
        }
        if (prefix.isEmpty()) return

        val updated = VariationBranch(
            anchorPly = anchorPly,
            baseFen = baseFen,
            steps = prefix,
            activeStepIndex = prefix.lastIndex,
            liveEvalCp = line.cp,
            liveEvalMate = line.mate,
            isEvaluating = true
        )
        _uiState.update {
            it.copy(
                variation = updated,
                showBestMove = false,
                boardSelectedSquare = null,
                boardLegalTargets = emptyMap()
            )
        }
        evaluateVariationPosition(updated.activeFen)
    }

    fun stepToVariationIndex(index: Int) {
        val branch = _uiState.value.variation ?: return
        if (index < 0) {
            exitVariation()
            return
        }
        val clamped = index.coerceIn(0, branch.steps.lastIndex)
        val updated = branch.copy(
            activeStepIndex = clamped,
            selectedSquare = null,
            legalTargets = emptyMap(),
            isEvaluating = true
        )
        _uiState.update { it.copy(variation = updated) }
        evaluateVariationPosition(updated.activeFen)
    }

    fun undoVariationMove() {
        val branch = _uiState.value.variation ?: return
        if (branch.activeStepIndex <= 0) {
            exitVariation()
        } else {
            stepToVariationIndex(branch.activeStepIndex - 1)
        }
    }

    fun exitVariation() {
        variationEvalJob?.cancel()
        _uiState.update {
            it.copy(
                variation = null,
                boardSelectedSquare = null,
                boardLegalTargets = emptyMap(),
                boardPendingPromotion = null
            )
        }
    }

    private fun playVariationMove(from: String, to: String, promo: PieceType?) {
        val s = _uiState.value
        val res = s.result ?: return
        val existing = s.variation
        val anchorPly = existing?.anchorPly ?: s.currentPly
        val baseFen = existing?.baseFen ?: currentMainFen(res, anchorPly)
        val currentFen = existing?.activeFen ?: baseFen

        val board = ChessBoard(currentFen)
        val uci = from + to + (promo?.symbol?.toString() ?: "")
        val move = board.makeUciMove(uci) ?: return
        val san = board.sanHistory().lastOrNull() ?: uci
        val newStep = VariationStep(
            san = san,
            uci = move.uci,
            from = move.fromName,
            to = move.toName,
            fenAfter = board.toFen()
        )
        val nextSteps = existing?.visibleSteps.orEmpty() + newStep
        val updated = VariationBranch(
            anchorPly = anchorPly,
            baseFen = baseFen,
            steps = nextSteps,
            activeStepIndex = nextSteps.lastIndex,
            isEvaluating = true
        )
        _uiState.update {
            it.copy(
                variation = updated,
                showBestMove = false,
                boardSelectedSquare = null,
                boardLegalTargets = emptyMap(),
                boardPendingPromotion = null
            )
        }
        evaluateVariationPosition(updated.activeFen)
    }

    private fun evaluateVariationPosition(fen: String) {
        variationEvalJob?.cancel()
        variationEvalJob = viewModelScope.launch {
            delay(140)
            val ev = engine.evaluateMultiPv(fen, depth = 8, multiPv = 3, timeLimitMs = 650L)
            _uiState.update { cur ->
                val br = cur.variation
                if (br != null && br.activeFen == fen) {
                    cur.copy(
                        variation = br.copy(
                            liveLines = ev.topLines,
                            liveEvalCp = ev.cp,
                            liveEvalMate = ev.mate,
                            isEvaluating = false
                        )
                    )
                } else cur
            }
        }
    }

    private fun currentMainFen(res: GameAnalysisResult, ply: Int): String {
        return if (ply <= 0) {
            res.headers["FEN"]?.takeIf { it.isNotBlank() } ?: ChessBoard.START_FEN
        } else {
            res.plies.getOrNull(ply - 1)?.fenAfter
                ?: res.headers["FEN"]?.takeIf { it.isNotBlank() }
                ?: ChessBoard.START_FEN
        }
    }

    // ===== One-Tap Practice My Mistakes Retry Drill =====

    fun startMistakeDrill(targetPlyIndex: Int? = null) {
        val res = _uiState.value.result ?: return
        val candidates = res.plies.filter { p ->
            (p.moveClass == MoveClass.MISTAKE || p.moveClass == MoveClass.BLUNDER || p.moveClass == MoveClass.INACCURACY) &&
                p.bestFrom != null && p.bestTo != null
        }.sortedByDescending {
            when (it.moveClass) {
                MoveClass.BLUNDER -> 3
                MoveClass.MISTAKE -> 2
                else -> 1
            } * 100.0 + it.loss
        }

        if (candidates.isEmpty()) {
            _uiState.update {
                it.copy(statusMessage = "No mistakes or inaccuracies found in this game — clean play!")
            }
            return
        }

        val ordered = if (targetPlyIndex != null) {
            val picked = candidates.find { it.index == targetPlyIndex }
            if (picked != null) listOf(picked) + candidates.filter { it.index != targetPlyIndex } else candidates
        } else candidates

        val first = ordered.first()
        val pColor = if (first.index % 2 == 0) PieceColor.WHITE else PieceColor.BLACK
        _uiState.update {
            it.copy(
                mistakeDrill = MistakeDrillState(
                    mistakes = ordered,
                    index = 0,
                    playerColor = pColor,
                    flipped = (pColor == PieceColor.BLACK),
                    boardFen = first.fenBefore,
                    feedback = "You played ${first.san} (${first.badgeLabel}). Find the stronger move!"
                )
            )
        }
    }

    fun onDrillSquareClick(sqName: String) {
        val drill = _uiState.value.mistakeDrill ?: return
        val cur = drill.currentMistake ?: return
        if (drill.solved) return

        val board = ChessBoard(drill.boardFen)
        val sel = drill.selectedSquare
        if (sel != null) {
            val fromIdx = parseSquare(sel)
            val toIdx = parseSquare(sqName)
            if (fromIdx != null && toIdx != null) {
                val matching = board.legalMoves(fromIdx).filter { it.to == toIdx }
                if (matching.isNotEmpty()) {
                    if (matching.any { it.promotion != null }) {
                        _uiState.update {
                            it.copy(mistakeDrill = drill.copy(pendingPromotion = sel to sqName))
                        }
                    } else {
                        executeDrillMove(sel, sqName, null)
                    }
                    return
                }
            }
        }

        val sqIdx = parseSquare(sqName) ?: return
        val piece = board.pieceAt(sqIdx)
        if (piece != null && piece.color == board.turn && sqName != sel) {
            val targets = board.legalMoves(sqIdx).associate { it.toName to (it.captured != null) }
            _uiState.update {
                it.copy(mistakeDrill = drill.copy(selectedSquare = sqName, legalTargets = targets, wrongAttempt = false))
            }
        } else {
            _uiState.update {
                it.copy(mistakeDrill = drill.copy(selectedSquare = null, legalTargets = emptyMap()))
            }
        }
    }

    fun selectDrillPromotion(type: PieceType) {
        val drill = _uiState.value.mistakeDrill ?: return
        val pending = drill.pendingPromotion ?: return
        _uiState.update { it.copy(mistakeDrill = drill.copy(pendingPromotion = null)) }
        executeDrillMove(pending.first, pending.second, type)
    }

    fun dismissDrillPromotion() {
        val drill = _uiState.value.mistakeDrill ?: return
        _uiState.update { it.copy(mistakeDrill = drill.copy(pendingPromotion = null)) }
    }

    private fun executeDrillMove(from: String, to: String, promo: PieceType?) {
        val drill = _uiState.value.mistakeDrill ?: return
        val cur = drill.currentMistake ?: return
        val board = ChessBoard(drill.boardFen)
        val uci = from + to + (promo?.symbol?.toString() ?: "")
        val move = board.makeUciMove(uci) ?: return
        val playedSan = board.sanHistory().lastOrNull() ?: uci

        val expectedUci = cur.pvUci.firstOrNull()
        val expectedBase = (cur.bestFrom ?: "") + (cur.bestTo ?: "")
        val isCorrect = (expectedUci != null && move.uci == expectedUci) ||
            (expectedBase.isNotEmpty() && (move.fromName + move.toName) == expectedBase) ||
            (cur.bestSan != null && playedSan == cur.bestSan) ||
            cur.topLines.firstOrNull()?.let { top ->
                top.firstSan == playedSan || top.firstUci == move.uci
            } == true

        if (isCorrect) {
            _uiState.update {
                it.copy(
                    mistakeDrill = drill.copy(
                        boardFen = board.toFen(),
                        selectedSquare = null,
                        legalTargets = emptyMap(),
                        lastMoveSquares = listOf(move.fromName, move.toName),
                        wrongAttempt = false,
                        solved = true,
                        solvedCount = drill.solvedCount + 1,
                        feedback = "Nice find! ${cur.bestSan ?: playedSan} is the engine's best move."
                    )
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    mistakeDrill = drill.copy(
                        boardFen = board.toFen(),
                        selectedSquare = null,
                        legalTargets = emptyMap(),
                        lastMoveSquares = listOf(move.fromName, move.toName),
                        wrongAttempt = true,
                        solved = false,
                        feedback = "$playedSan doesn't solve the position — tap Retry to try again."
                    )
                )
            }
        }
    }

    fun retryCurrentMistake() {
        val drill = _uiState.value.mistakeDrill ?: return
        val cur = drill.currentMistake ?: return
        _uiState.update {
            it.copy(
                mistakeDrill = drill.copy(
                    boardFen = cur.fenBefore,
                    selectedSquare = null,
                    legalTargets = emptyMap(),
                    lastMoveSquares = emptyList(),
                    wrongAttempt = false,
                    solved = false,
                    feedback = "Reset! Try finding a stronger move than ${cur.san}."
                )
            )
        }
    }

    fun revealDrillHint() {
        val drill = _uiState.value.mistakeDrill ?: return
        val cur = drill.currentMistake ?: return
        _uiState.update {
            it.copy(
                mistakeDrill = drill.copy(
                    showHint = true,
                    feedback = "Hint: Look at moving the piece on ${cur.bestFrom ?: "a key square"}."
                )
            )
        }
    }

    fun nextMistakeDrill() {
        val drill = _uiState.value.mistakeDrill ?: return
        val nextIdx = drill.index + 1
        if (nextIdx >= drill.mistakes.size) {
            _uiState.update { it.copy(mistakeDrill = null, statusMessage = "Mistake drill complete! Solved ${drill.solvedCount} / ${drill.mistakes.size}.") }
            return
        }
        val next = drill.mistakes[nextIdx]
        val pColor = if (next.index % 2 == 0) PieceColor.WHITE else PieceColor.BLACK
        _uiState.update {
            it.copy(
                mistakeDrill = drill.copy(
                    index = nextIdx,
                    playerColor = pColor,
                    flipped = (pColor == PieceColor.BLACK),
                    boardFen = next.fenBefore,
                    selectedSquare = null,
                    legalTargets = emptyMap(),
                    pendingPromotion = null,
                    lastMoveSquares = emptyList(),
                    wrongAttempt = false,
                    solved = false,
                    showHint = false,
                    feedback = "You played ${next.san} (${next.badgeLabel}). Find the stronger move!"
                )
            )
        }
    }

    fun closeMistakeDrill() {
        _uiState.update { it.copy(mistakeDrill = null) }
    }

    // ===== Share =====

    fun openShare(forceAnalysis: Boolean) {
        val s = _uiState.value
        val res = s.result
        val text = if (forceAnalysis && res != null) {
            MoveExplainer.buildShareReport(res)
        } else if (!forceAnalysis && res != null && s.pgnText.trim() == res.pgn.trim()) {
            MoveExplainer.buildShareReport(res)
        } else {
            s.pgnText.trim()
        }

        if (text.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Paste a PGN, import a game, or analyse first, then share it.")
            }
            return
        }
        _uiState.update { it.copy(shareText = text) }
    }

    fun closeShare() {
        _uiState.update { it.copy(shareText = null) }
    }
}
