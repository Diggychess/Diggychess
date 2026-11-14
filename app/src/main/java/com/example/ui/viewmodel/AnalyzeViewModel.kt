package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.example.ui.theme.BoardTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AnalyzeUiState(
    val pgnText: String = "",
    val settings: ReviewSettings = ReviewSettings(),
    val boardTheme: BoardTheme = BoardTheme.WOOD,
    val isAnalyzing: Boolean = false,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val result: GameAnalysisResult? = null,
    val currentPly: Int = 0,
    val showBestMove: Boolean = false,
    val flipped: Boolean = false,
    val recentGames: List<SavedGameEntry> = emptyList(),
    val shareText: String? = null
)

class AnalyzeViewModel(
    private val repo: KnightScopeRepository,
    private val analyzer: GameAnalyzer = GameAnalyzer()
) : ViewModel() {

    companion object {
        const val SAMPLE_PGN = "[Event \"Sample\"]\n[White \"White\"]\n[Black \"Black\"]\n[Result \"1-0\"]\n\n1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0"
    }

    private val _uiState = MutableStateFlow(AnalyzeUiState())
    val uiState: StateFlow<AnalyzeUiState> = _uiState.asStateFlow()

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
                _uiState.update { it.copy(recentGames = hist.take(3)) }
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

    fun analyzeGame(savedEntry: SavedGameEntry? = null, fromHistory: Boolean = false) {
        val text = savedEntry?.pgn ?: _uiState.value.pgnText
        if (text.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Could not read this PGN. Paste the full PGN, including the headers.")
            }
            return
        }

        if (_uiState.value.isAnalyzing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pgnText = text,
                    isAnalyzing = true,
                    progressDone = 0,
                    progressTotal = 0,
                    errorMessage = null,
                    statusMessage = null
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
                        flipped = false,
                        errorMessage = null
                    )
                }
                if (!fromHistory) {
                    repo.saveGameToHistory(analyzer.toSavedGameEntry(res))
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
        _uiState.update { it.copy(currentPly = clamped, showBestMove = canShowBest) }
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
                it.copy(errorMessage = "Paste a PGN or analyse a game first, then share it.")
            }
            return
        }
        _uiState.update { it.copy(shareText = text) }
    }

    fun closeShare() {
        _uiState.update { it.copy(shareText = null) }
    }
}
