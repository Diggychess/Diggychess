package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.SavedGameEntry
import com.example.data.KnightScopeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryReportSummary(
    val blundersPerGame: Double,
    val mistakesPerGame: Double,
    val inaccuraciesPerGame: Double,
    val worstPhaseIndex: Int, // 0 = opening, 1 = middlegame, 2 = endgame
    val hasAnyMistake: Boolean,
    val gameCount: Int
)

data class HistoryUiState(
    val games: List<SavedGameEntry> = emptyList(),
    val sideFilter: String = "both", // "both", "w", "b"
    val pendingDeleteId: String? = null,
    val report: HistoryReportSummary? = null
)

class HistoryViewModel(private val repo: KnightScopeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repo.historyFlow, repo.reportSideFlow) { games, side ->
                val report = computeReport(games, side)
                _uiState.value.copy(
                    games = games,
                    sideFilter = side,
                    report = report
                )
            }.collect { updated ->
                _uiState.value = updated
            }
        }
    }

    fun setSideFilter(side: String) {
        viewModelScope.launch { repo.setReportSide(side) }
    }

    fun requestDelete(id: String) {
        _uiState.update { it.copy(pendingDeleteId = id) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(pendingDeleteId = null) }
    }

    fun confirmDelete(id: String) {
        _uiState.update { it.copy(pendingDeleteId = null) }
        viewModelScope.launch { repo.deleteGameFromHistory(id) }
    }

    private fun computeReport(games: List<SavedGameEntry>, side: String): HistoryReportSummary? {
        val withReports = games.filter { it.report != null }
        if (withReports.isEmpty()) return null

        val sides = if (side == "both") listOf("w", "b") else listOf(side)
        var bl = 0
        var mi = 0
        var inac = 0
        val phases = intArrayOf(0, 0, 0)

        for (g in withReports) {
            val rep = g.report ?: continue
            for (sd in sides) {
                val counts = if (sd == "w") rep.whiteCounts else rep.blackCounts
                val ph = if (sd == "w") rep.whitePhaseMistakes else rep.blackPhaseMistakes
                inac += counts.getOrElse(2) { 0 }
                mi += counts.getOrElse(3) { 0 }
                bl += counts.getOrElse(4) { 0 }
                for (k in 0..2) phases[k] += ph.getOrElse(k) { 0 }
            }
        }

        val denom = (withReports.size * sides.size).toDouble().coerceAtLeast(1.0)
        val maxVal = phases.maxOrNull() ?: 0
        val worstIdx = phases.indexOfFirst { it == maxVal }.coerceAtLeast(0)

        return HistoryReportSummary(
            blundersPerGame = bl / denom,
            mistakesPerGame = mi / denom,
            inaccuraciesPerGame = inac / denom,
            worstPhaseIndex = worstIdx,
            hasAnyMistake = maxVal > 0,
            gameCount = withReports.size
        )
    }
}
