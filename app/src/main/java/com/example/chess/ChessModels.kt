package com.example.chess

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
    fun fenChar(): Char = if (this == WHITE) 'w' else 'b'

    companion object {
        fun fromChar(c: Char): PieceColor = if (c == 'w') WHITE else BLACK
    }
}

enum class PieceType(val symbol: Char, val pieceName: String, val value: Int) {
    PAWN('p', "pawn", 1),
    KNIGHT('n', "knight", 3),
    BISHOP('b', "bishop", 3),
    ROOK('r', "rook", 5),
    QUEEN('q', "queen", 9),
    KING('k', "king", 0);

    companion object {
        fun fromChar(c: Char): PieceType? = entries.find { it.symbol == c.lowercaseChar() }
    }
}

data class ChessPiece(
    val color: PieceColor,
    val type: PieceType
) {
    fun fenChar(): Char = if (color == PieceColor.WHITE) type.symbol.uppercaseChar() else type.symbol
}

data class Move(
    val from: Int, // 0..63 (0 = a8, 7 = h8, 56 = a1, 63 = h1)
    val to: Int,
    val piece: PieceType,
    val color: PieceColor,
    val captured: PieceType? = null,
    val promotion: PieceType? = null,
    val isKingSideCastle: Boolean = false,
    val isQueenSideCastle: Boolean = false,
    val isEnPassant: Boolean = false
) {
    val fromName: String get() = squareName(from)
    val toName: String get() = squareName(to)
    val uci: String get() = fromName + toName + (promotion?.symbol?.toString() ?: "")
}

fun squareName(sq: Int): String {
    val file = 'a' + (sq % 8)
    val rank = 8 - (sq / 8)
    return "$file$rank"
}

fun parseSquare(name: String): Int? {
    if (name.length != 2) return null
    val f = name[0] - 'a'
    val r = 8 - (name[1] - '0')
    if (f !in 0..7 || r !in 0..7) return null
    return r * 8 + f
}

enum class MoveClass(val label: String, val color: Color, val glyph: String) {
    BEST("Best", ClassBest, "★"),
    GOOD("Good", ClassGood, "✓"),
    INACCURACY("Inaccuracy", ClassInaccuracy, "?!"),
    MISTAKE("Mistake", ClassMistake, "?"),
    BLUNDER("Blunder", ClassBlunder, "??"),
    BRILLIANT("Brilliant", ClassBrilliant, "!!"),
    EXCELLENT("Excellent", ClassExcellent, "✔");

    val isSuboptimal: Boolean
        get() = this == INACCURACY || this == MISTAKE || this == BLUNDER
}

data class ParsedPly(
    val san: String,
    val move: Move,
    val fenBefore: String,
    val fenAfter: String,
    val eval: Double? = null,
    val mate: Int? = null,
    val bestSan: String? = null
)

data class ParsedGame(
    val headers: Map<String, String>,
    val plies: List<ParsedPly>
)

data class EngineVariation(
    val rank: Int,
    val cp: Double, // In pawns from White's POV
    val mate: Int? = null,
    val firstUci: String,
    val firstSan: String,
    val pvUci: List<String> = emptyList(),
    val pvSan: List<String> = emptyList(),
    val depth: Int = 0
)

data class EngineEval(
    val cp: Double, // In pawns from White's POV, clamped [-99.0, 99.0]
    val mate: Int? = null, // Mate in N moves from White's POV (+ White mates, - Black mates)
    val pvUci: List<String> = emptyList(),
    val depth: Int = 0,
    val bestUci: String? = null,
    val sourceLabel: String = "On-device Engine",
    val candidateMoves: List<String> = emptyList(),
    val topLines: List<EngineVariation> = emptyList()
)

enum class EngineDifficulty(
    val id: String,
    val label: String,
    val eloLabel: String,
    val description: String,
    val skillLevel: Int,
    val apiDepth: Int,
    val localDepth: Int,
    val thinkingTimeMs: Int
) {
    EASY("easy", "Easy", "900 ELO", "Skill Level 3 · Relaxed play", 3, 2, 2, 15),
    MEDIUM("medium", "Medium", "1400 ELO", "Skill Level 8 · Balanced club play", 8, 6, 4, 45),
    HARD("hard", "Hard", "1950 ELO", "Skill Level 14 · Strong tactical play", 14, 10, 5, 75),
    EXPERT("expert", "Expert", "2400+ ELO", "Skill Level 20 · Full Stockfish strength", 20, 14, 6, 95);

    companion object {
        fun fromId(id: String?): EngineDifficulty = entries.find { it.id == id } ?: MEDIUM
    }
}

data class AnalyzedPly(
    val index: Int, // 0-based ply index
    val san: String,
    val move: Move,
    val fenBefore: String,
    val fenAfter: String,
    val evalBefore: Double,
    val evalAfter: Double,
    val mate: Int?,
    val loss: Double,
    val accuracy: Double,
    val moveClass: MoveClass,
    val isBook: Boolean,
    val opening: OpeningEntry?,
    val bestSan: String?,
    val bestFrom: String?,
    val bestTo: String?,
    val pvUci: List<String>,
    val explanation: String,
    val bestExplanation: String,
    val topLines: List<EngineVariation> = emptyList(),
    val coachHeadline: String = "",
    val badgeLabel: String = moveClass.label,
    val badgeGlyph: String = moveClass.glyph,
    val badgeColor: Color = moveClass.color,
    val tacticalTags: List<String> = emptyList(),
    val winProbSwing: Double = 0.0,
    val isGreatMove: Boolean = false,
    val isMissedWin: Boolean = false
)

data class OpeningEntry(
    val eco: String,
    val name: String,
    val moves: String
)

data class OpeningSummary(
    val entry: OpeningEntry?,
    val lastBookIndex: Int,
    val matchedText: String,
    val nextMoveNote: String,
    val engineFirstNote: String,
    val engineEvalNote: String,
    val phaseHeading: String,
    val whiteObs: String,
    val blackObs: String
)

data class PhaseStats(
    val whiteAccuracy: String,
    val blackAccuracy: String
)

data class GameSummaryStats(
    val whiteName: String,
    val blackName: String,
    val whiteElo: String?,
    val blackElo: String?,
    val result: String?,
    val whiteAccuracy: Double,
    val blackAccuracy: Double,
    val whiteCounts: Map<MoveClass, Int>,
    val blackCounts: Map<MoveClass, Int>,
    val openingStats: PhaseStats,
    val middlegameStats: PhaseStats,
    val endgameStats: PhaseStats
)

data class GameAnalysisResult(
    val id: String,
    val title: String,
    val pgn: String,
    val headers: Map<String, String>,
    val plies: List<AnalyzedPly>,
    val evals: List<Double>, // length = plies.size + 1
    val openingSummary: OpeningSummary,
    val summaryStats: GameSummaryStats,
    val keyMistakes: List<AnalyzedPly>,
    val engineLabel: String,
    val depth: Int = 12
)

data class ReviewSettings(
    val depth: Int = 12,
    val forceReanalyze: Boolean = false,
    val inaccuracyThreshold: Int = 5,
    val mistakeThreshold: Int = 10,
    val blunderThreshold: Int = 20
)

enum class PuzzleDifficultyFilter(
    val id: String,
    val label: String,
    val ratingLabel: String
) {
    ALL("all", "All", "All ratings"),
    BEGINNER("beginner", "Beginner", "0–1399"),
    INTERMEDIATE("intermediate", "Intermediate", "1400–1999"),
    ADVANCED("advanced", "Advanced", "2000+");

    fun matches(puzzle: Puzzle): Boolean = when (this) {
        ALL -> true
        BEGINNER -> puzzle.rating in 0..1399
        INTERMEDIATE -> puzzle.rating in 1400..1999
        ADVANCED -> puzzle.rating >= 2000
    }

    companion object {
        val TIERS = listOf(BEGINNER, INTERMEDIATE, ADVANCED)

        fun fromId(id: String?): PuzzleDifficultyFilter =
            entries.firstOrNull { it.id == id } ?: ALL

        fun tierForRating(rating: Int): PuzzleDifficultyFilter = when {
            rating in 0..1399 -> BEGINNER
            rating in 1400..1999 -> INTERMEDIATE
            else -> ADVANCED
        }
    }
}

data class Puzzle(
    val id: String,
    val fen0: String, // position before opponent's setup move (or initial fen for starter puzzles)
    val setupUci: String?, // opponent's setup move (null for pre-positioned puzzles)
    val fen: String, // actual puzzle start fen (computed lazily if needed)
    val solution: List<String>, // UCI moves starting with player's move
    val rating: Int,
    val themes: List<String>,
    val isMate: Boolean,
    val source: String
)

data class PuzzleAttempt(
    val id: String,
    val timestamp: Long,
    val ok: Boolean,
    val rating: Int
)

data class PuzzleRatingState(
    val rating: Double = 1200.0,
    val count: Int = 0,
    val history: List<Pair<Long, Int>> = emptyList(),
    val seenIds: List<String> = emptyList()
)

data class PhaseReport(
    val whiteCounts: List<Int>, // length 7 corresponding to MoveClass.entries (backward compatible with 6)
    val blackCounts: List<Int>, // length 7
    val whitePhaseMistakes: List<Int>, // [opening, middlegame, endgame]
    val blackPhaseMistakes: List<Int>
)

data class SavedGameEntry(
    val id: String,
    val title: String,
    val timestamp: Long,
    val plies: Int,
    val moves: Int,
    val pgn: String,
    val evals: List<Double>,
    val bestMoves: List<String?>,
    val report: PhaseReport?,
    val mistakes: List<Puzzle>,
    val result: String = "*",
    val opponent: String = "",
    val source: String = "analysed",
    val depth: Int = 12,
    val whiteAccuracy: Double? = null,
    val blackAccuracy: Double? = null
)
