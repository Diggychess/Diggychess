package com.example.chess

import com.github.bhlangonijr.chesslib.Board
import com.github.bhlangonijr.chesslib.move.MoveList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PgnValidationReport(
    val isValid: Boolean,
    val normalizedPgn: String,
    val moveCount: Int,
    val headers: Map<String, String>,
    val finalFen: String,
    val inCheckmate: Boolean,
    val inDraw: Boolean,
    val warning: String? = null,
    val error: String? = null
)

/**
 * Off-UI-thread bridge to bhlangonijr/chesslib for PGN import validation,
 * SAN replay cross-checks, and clean PGN export without duplicating live UI state.
 */
object ChesslibPgnBridge {

    private val HEADER_REGEX = Regex("""^\[(\w+)\s+"([^"]*)"\]""", RegexOption.MULTILINE)
    private val COMMENT_REGEX = Regex("""\{[^}]*\}""")
    private val PAREN_REGEX = Regex("""\([^()]*\)""")
    private val TOKEN_REGEX = Regex("""[^\s{}()]+""")
    private val NAG_REGEX = Regex("""^\$\d+$""")
    private val RESULT_REGEX = Regex("""^(1-0|0-1|1/2-1/2|\*)$""")
    private val MOVE_NUM_REGEX = Regex("""^\d+\.+""")

    suspend fun validateAndNormalizePgn(rawPgn: String): PgnValidationReport = withContext(Dispatchers.Default) {
        val trimmed = rawPgn.trim()
        if (trimmed.isEmpty()) {
            return@withContext PgnValidationReport(
                isValid = false,
                normalizedPgn = "",
                moveCount = 0,
                headers = emptyMap(),
                finalFen = ChessBoard.START_FEN,
                inCheckmate = false,
                inDraw = false,
                error = "Empty PGN text."
            )
        }

        val headers = linkedMapOf<String, String>()
        HEADER_REGEX.findAll(trimmed).forEach { m ->
            headers[m.groupValues[1]] = m.groupValues[2]
        }

        val customFen = headers["FEN"]?.takeIf { it.isNotBlank() }
        val libBoard = Board()
        if (customFen != null) {
            try {
                libBoard.loadFromFen(customFen)
            } catch (e: Exception) {
                return@withContext PgnValidationReport(
                    isValid = false,
                    normalizedPgn = trimmed,
                    moveCount = 0,
                    headers = headers,
                    finalFen = ChessBoard.START_FEN,
                    inCheckmate = false,
                    inDraw = false,
                    error = "Invalid starting FEN header: ${e.message ?: "malformed FEN"}"
                )
            }
        }

        var moveText = trimmed.replace(Regex("""^\[.*\]\s*$""", RegexOption.MULTILINE), " ")
        moveText = COMMENT_REGEX.replace(moveText, " ")
        var prev: String
        do {
            prev = moveText
            moveText = PAREN_REGEX.replace(moveText, " ")
        } while (prev != moveText)

        val sanTokens = mutableListOf<String>()
        for (match in TOKEN_REGEX.findAll(moveText)) {
            var tk = match.value
            if (NAG_REGEX.matches(tk) || RESULT_REGEX.matches(tk)) continue
            tk = tk.replace(MOVE_NUM_REGEX, "").trimEnd('!', '?')
            if (tk.isNotEmpty()) sanTokens.add(tk)
        }

        if (sanTokens.isEmpty()) {
            return@withContext PgnValidationReport(
                isValid = false,
                normalizedPgn = trimmed,
                moveCount = 0,
                headers = headers,
                finalFen = libBoard.fen,
                inCheckmate = false,
                inDraw = false,
                error = "No chess moves found in this PGN."
            )
        }

        val sanJoin = sanTokens.joinToString(" ")
        return@withContext try {
            val moveList = if (customFen != null) MoveList(customFen) else MoveList()
            moveList.loadFromSan(sanJoin)
            for (mv in moveList) {
                if (!libBoard.doMove(mv)) {
                    throw IllegalStateException("Illegal move encountered: $mv")
                }
            }

            val exportedHeaders = buildString {
                val orderedKeys = listOf("Event", "Site", "Date", "Round", "White", "Black", "Result", "FEN", "SetUp")
                val used = HashSet<String>()
                orderedKeys.forEach { k ->
                    headers[k]?.let { v ->
                        append("[$k \"$v\"]\n")
                        used.add(k)
                    }
                }
                headers.forEach { (k, v) ->
                    if (used.add(k)) append("[$k \"$v\"]\n")
                }
            }
            val resultStr = headers["Result"]?.takeIf { it.isNotBlank() } ?: "*"
            val cleanBody = buildMoveBody(moveList.toSanArray().toList(), resultStr)
            val normalized = if (exportedHeaders.isNotBlank()) "$exportedHeaders\n$cleanBody" else cleanBody

            PgnValidationReport(
                isValid = true,
                normalizedPgn = normalized,
                moveCount = moveList.size,
                headers = headers,
                finalFen = libBoard.fen,
                inCheckmate = libBoard.isMated,
                inDraw = libBoard.isDraw || libBoard.isStaleMate
            )
        } catch (e: Exception) {
            // Fallback check via native Kotlin ChessBoard in case PGN uses minor non-standard SAN notation
            val fallbackParsed = PgnParser.parse(trimmed)
            if (fallbackParsed.plies.isNotEmpty() && fallbackParsed.plies.size == sanTokens.size) {
                PgnValidationReport(
                    isValid = true,
                    normalizedPgn = trimmed,
                    moveCount = fallbackParsed.plies.size,
                    headers = fallbackParsed.headers,
                    finalFen = fallbackParsed.plies.last().fenAfter,
                    inCheckmate = ChessBoard(fallbackParsed.plies.last().fenAfter).inCheckmate(),
                    inDraw = ChessBoard(fallbackParsed.plies.last().fenAfter).isGameOver(),
                    warning = "Imported with fallback parser (${e.message ?: "variant SAN"})."
                )
            } else {
                PgnValidationReport(
                    isValid = false,
                    normalizedPgn = trimmed,
                    moveCount = 0,
                    headers = headers,
                    finalFen = ChessBoard.START_FEN,
                    inCheckmate = false,
                    inDraw = false,
                    error = "Invalid PGN or illegal move: ${e.message ?: "parse error"}"
                )
            }
        }
    }

    suspend fun verifyFen(fen: String): Boolean = withContext(Dispatchers.Default) {
        try {
            val b = Board()
            b.loadFromFen(fen)
            b.legalMoves().size >= 0
        } catch (_: Exception) {
            false
        }
    }

    private fun buildMoveBody(sans: List<String>, result: String): String {
        val sb = StringBuilder()
        sans.forEachIndexed { idx, san ->
            if (idx % 2 == 0) {
                if (sb.isNotEmpty()) sb.append(' ')
                sb.append("${idx / 2 + 1}. $san")
            } else {
                sb.append(" $san")
            }
        }
        if (sb.isNotEmpty()) sb.append(' ')
        sb.append(result)
        return sb.toString()
    }
}
