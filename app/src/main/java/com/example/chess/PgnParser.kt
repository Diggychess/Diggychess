package com.example.chess

object PgnParser {
    private val HEADER_REGEX = Regex("""^\[(\w+)\s+"([^"]*)"\]""", RegexOption.MULTILINE)
    private val COMMENT_REGEX = Regex("""\{[^}]*\}""")
    private val PAREN_REGEX = Regex("""\([^()]*\)""")
    private val TOKEN_REGEX = Regex("""\{\d+\}|[^\s{}]+""")
    private val EVAL_REGEX = Regex("""\[%eval\s+(#?-?\d+\.?\d*)]""")
    private val BEST_REGEX = Regex("""([A-Za-z0-9=+#\-]+)\s+was best""")
    private val NAG_REGEX = Regex("""^\$\d+$""")
    private val RESULT_REGEX = Regex("""^(1-0|0-1|1/2-1/2|\*)$""")
    private val MOVE_NUM_REGEX = Regex("""^\d+\.+""")

    fun parse(pgnText: String): ParsedGame {
        val headers = linkedMapOf<String, String>()
        HEADER_REGEX.findAll(pgnText).forEach { match ->
            headers[match.groupValues[1]] = match.groupValues[2]
        }

        var moveText = pgnText.replace(Regex("""^\[.*\]\s*$""", RegexOption.MULTILINE), "")
        val comments = mutableListOf<String>()
        moveText = COMMENT_REGEX.replace(moveText) { match ->
            comments.add(match.value)
            " {${comments.size - 1}} "
        }

        var prev: String
        do {
            prev = moveText
            moveText = PAREN_REGEX.replace(moveText, " ")
        } while (prev != moveText)

        data class RawToken(val san: String, var comment: String = "")
        val rawTokens = mutableListOf<RawToken>()

        for (match in TOKEN_REGEX.findAll(moveText)) {
            var tk = match.value
            if (tk.startsWith("{") && tk.endsWith("}")) {
                val idx = tk.substring(1, tk.length - 1).toIntOrNull()
                if (idx != null && idx in comments.indices && rawTokens.isNotEmpty()) {
                    rawTokens.last().comment = comments[idx]
                }
                continue
            }
            if (NAG_REGEX.matches(tk) || RESULT_REGEX.matches(tk)) continue
            tk = tk.replace(MOVE_NUM_REGEX, "").trimEnd('!', '?')
            if (tk.isNotEmpty()) {
                rawTokens.add(RawToken(tk))
            }
        }

        val startFen = headers["FEN"]?.takeIf { it.isNotBlank() } ?: ChessBoard.START_FEN
        val board = ChessBoard(startFen)
        val plies = mutableListOf<ParsedPly>()

        for (rt in rawTokens) {
            val fenBefore = board.toFen()
            val move = board.makeSanMove(rt.san) ?: break
            val san = board.sanHistory().last()
            val fenAfter = board.toFen()

            var eval: Double? = null
            var mate: Int? = null
            val evalMatch = EVAL_REGEX.find(rt.comment)
            if (evalMatch != null) {
                val rawEval = evalMatch.groupValues[1]
                if (rawEval.startsWith("#")) {
                    val m = rawEval.drop(1).toIntOrNull() ?: 0
                    mate = m
                    eval = if (m > 0) 99.0 else -99.0
                } else {
                    val v = rawEval.toDoubleOrNull()
                    if (v != null) eval = v.coerceIn(-99.0, 99.0)
                }
            }

            val bestMatch = BEST_REGEX.find(rt.comment)
            val bestSan = bestMatch?.groupValues?.get(1)

            plies.add(
                ParsedPly(
                    san = san,
                    move = move,
                    fenBefore = fenBefore,
                    fenAfter = fenAfter,
                    eval = eval,
                    mate = mate,
                    bestSan = bestSan
                )
            )
        }

        return ParsedGame(headers = headers, plies = plies)
    }
}
