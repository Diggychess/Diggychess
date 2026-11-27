package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.example.service.ExplorerPositionResult
import com.example.service.LichessOpenServices
import com.example.ui.theme.BoardTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OpeningExplorerUiState(
    val fen: String = ChessBoard.START_FEN,
    val flipped: Boolean = false,
    val selectedSquare: String? = null,
    val legalTargets: Map<String, Boolean> = emptyMap(),
    val lastMoveSquares: List<String> = emptyList(),
    val sanHistory: List<String> = emptyList(),
    val canUndo: Boolean = false,
    val useMastersDb: Boolean = false,
    val isLoadingStats: Boolean = false,
    val stats: ExplorerPositionResult? = null,
    val boardTheme: BoardTheme = BoardTheme.GREEN,
    val pendingPromotion: Pair<String, String>? = null
)

class OpeningExplorerViewModel(private val repo: KnightScopeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OpeningExplorerUiState())
    val uiState: StateFlow<OpeningExplorerUiState> = _uiState.asStateFlow()

    private var board = ChessBoard()
    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            repo.boardThemeFlow.collect { theme ->
                _uiState.update { it.copy(boardTheme = theme) }
            }
        }
        refreshStats()
    }

    fun onSquareClick(sqName: String) {
        val s = _uiState.value
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
                        playUciMove(sel + sqName)
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
        playUciMove(pending.first + pending.second + type.symbol)
    }

    fun dismissPromotion() {
        _uiState.update { it.copy(pendingPromotion = null) }
    }

    fun playUciMove(uci: String) {
        val mv = board.makeUciMove(uci) ?: return
        _uiState.update {
            it.copy(
                fen = board.toFen(),
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = listOf(mv.fromName, mv.toName),
                sanHistory = board.sanHistory(),
                canUndo = board.moveHistoryCount() > 0,
                pendingPromotion = null
            )
        }
        refreshStats()
    }

    fun undoMove() {
        if (board.moveHistoryCount() == 0) return
        board.undo()
        val last = board.moveHistory().lastOrNull()
        _uiState.update {
            it.copy(
                fen = board.toFen(),
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = if (last != null) listOf(last.fromName, last.toName) else emptyList(),
                sanHistory = board.sanHistory(),
                canUndo = board.moveHistoryCount() > 0
            )
        }
        refreshStats()
    }

    fun resetBoard() {
        board = ChessBoard()
        _uiState.update {
            it.copy(
                fen = board.toFen(),
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = emptyList(),
                sanHistory = emptyList(),
                canUndo = false
            )
        }
        refreshStats()
    }

    fun toggleFlip() {
        _uiState.update { it.copy(flipped = !it.flipped) }
    }

    fun setDatabaseMode(useMasters: Boolean) {
        if (_uiState.value.useMastersDb == useMasters) return
        _uiState.update { it.copy(useMastersDb = useMasters) }
        refreshStats()
    }

    fun buildCurrentPgn(): String = board.toPgn("*")

    private fun refreshStats() {
        fetchJob?.cancel()
        val fen = board.toFen()
        val masters = _uiState.value.useMastersDb
        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStats = true) }
            repo.ensureOpeningsLoaded()
            val result = LichessOpenServices.fetchExplorerStats(fen = fen, useMasters = masters)
            _uiState.update {
                it.copy(
                    isLoadingStats = false,
                    stats = result
                )
            }
        }
    }
}
