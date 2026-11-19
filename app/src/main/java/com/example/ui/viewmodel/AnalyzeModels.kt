package com.example.ui.viewmodel

import com.example.chess.*
import com.example.ui.theme.BoardTheme

data class VariationStep(
    val san: String,
    val uci: String,
    val from: String,
    val to: String,
    val fenAfter: String
)

data class VariationBranch(
    val anchorPly: Int,
    val baseFen: String,
    val steps: List<VariationStep> = emptyList(),
    val activeStepIndex: Int = -1,
    val selectedSquare: String? = null,
    val legalTargets: Map<String, Boolean> = emptyMap(),
    val pendingPromotion: Pair<String, String>? = null,
    val liveLines: List<EngineVariation> = emptyList(),
    val liveEvalCp: Double = 0.0,
    val liveEvalMate: Int? = null,
    val isEvaluating: Boolean = false
) {
    val activeFen: String
        get() = if (activeStepIndex in steps.indices) steps[activeStepIndex].fenAfter else baseFen

    val visibleSteps: List<VariationStep>
        get() = if (activeStepIndex >= 0) steps.take(activeStepIndex + 1) else emptyList()

    val lastMoveSquares: List<String>
        get() = if (activeStepIndex in steps.indices) {
            val s = steps[activeStepIndex]
            listOf(s.from, s.to)
        } else emptyList()
}

data class MistakeDrillState(
    val mistakes: List<AnalyzedPly>,
    val index: Int = 0,
    val playerColor: PieceColor = PieceColor.WHITE,
    val flipped: Boolean = false,
    val boardFen: String = ChessBoard.START_FEN,
    val selectedSquare: String? = null,
    val legalTargets: Map<String, Boolean> = emptyMap(),
    val pendingPromotion: Pair<String, String>? = null,
    val lastMoveSquares: List<String> = emptyList(),
    val wrongAttempt: Boolean = false,
    val solved: Boolean = false,
    val showHint: Boolean = false,
    val feedback: String? = null,
    val solvedCount: Int = 0
) {
    val currentMistake: AnalyzedPly? get() = mistakes.getOrNull(index)
}

data class AnalyzeUiState(
    val pgnText: String = "",
    val settings: ReviewSettings = ReviewSettings(),
    val boardTheme: BoardTheme = BoardTheme.GREEN,
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
    val shareText: String? = null,

    // Multi-PV & Variation Explorer state
    val plyLiveLines: List<EngineVariation> = emptyList(),
    val isLoadingPlyLines: Boolean = false,
    val boardSelectedSquare: String? = null,
    val boardLegalTargets: Map<String, Boolean> = emptyMap(),
    val boardPendingPromotion: Pair<String, String>? = null,
    val variation: VariationBranch? = null,

    // One-tap Practice My Mistakes retry drill
    val mistakeDrill: MistakeDrillState? = null,

    // Chess.com & Lichess Username Game & Stats Importer
    val showImporterSheet: Boolean = false,
    val importPlatform: ChessPlatform = ChessPlatform.CHESS_COM,
    val importUsername: String = "",
    val isFetchingPlatform: Boolean = false,
    val platformError: String? = null,
    val platformProfile: PlatformPlayerProfile? = null,
    val allImportedGames: List<ImportedGameSummary> = emptyList(),
    val filteredImportedGames: List<ImportedGameSummary> = emptyList(),
    val speedFilter: ImportSpeedFilter = ImportSpeedFilter.ALL,
    val resultFilter: ImportResultFilter = ImportResultFilter.ALL,
    val colorFilter: ImportColorFilter = ImportColorFilter.BOTH
)
