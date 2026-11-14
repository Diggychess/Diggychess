package com.example.chess

import kotlin.math.ceil

object OpeningBook {
    private val RAW_OPENINGS = """
        A00|Polish Opening|b4
        A00|Grob Opening|g4
        A01|Nimzowitsch-Larsen Attack|b3
        A02|Bird Opening|f4
        A04|Zukertort Opening|Nf3
        A07|King’s Indian Attack|Nf3 d5 g3
        A09|Réti Opening|Nf3 d5 c4
        A10|English Opening|c4
        A13|English Opening: Agincourt Defense|c4 e6
        A15|English Opening: Anglo-Indian Defense|c4 Nf6
        A20|English Opening: King’s English Variation|c4 e5
        A30|English Opening: Symmetrical Variation|c4 c5
        A40|Queen’s Pawn Game|d4
        A40|Modern Defense|d4 g6
        A43|Old Benoni Defense|d4 c5
        A45|Indian Defense|d4 Nf6
        A45|Trompowsky Attack|d4 Nf6 Bg5
        A46|Indian Defense: Knights Variation|d4 Nf6 Nf3
        A51|Budapest Gambit|d4 Nf6 c4 e5
        A57|Benko Gambit|d4 Nf6 c4 c5 d5 b5
        A60|Benoni Defense|d4 Nf6 c4 c5 d5 e6
        A80|Dutch Defense|d4 f5
        B00|King’s Pawn Game|e4
        B01|Scandinavian Defense|e4 d5
        B02|Alekhine’s Defense|e4 Nf6
        B06|Modern Defense|e4 g6
        B07|Pirc Defense|e4 d6 d4 Nf6 Nc3
        B08|Pirc Defense: Classical Variation|e4 d6 d4 Nf6 Nc3 g6 Nf3
        B09|Pirc Defense: Austrian Attack|e4 d6 d4 Nf6 Nc3 g6 f4
        B10|Caro-Kann Defense|e4 c6
        B12|Caro-Kann Defense: Advance Variation|e4 c6 d4 d5 e5
        B13|Caro-Kann Defense: Exchange Variation|e4 c6 d4 d5 exd5 cxd5
        B15|Caro-Kann Defense: Main Line|e4 c6 d4 d5 Nc3 dxe4 Nxe4
        B18|Caro-Kann Defense: Classical Variation|e4 c6 d4 d5 Nc3 dxe4 Nxe4 Bf5
        B20|Sicilian Defense|e4 c5
        B21|Sicilian Defense: Smith-Morra Gambit|e4 c5 d4 cxd4 c3
        B22|Sicilian Defense: Alapin Variation|e4 c5 c3
        B23|Sicilian Defense: Closed|e4 c5 Nc3
        B27|Sicilian Defense: Hyperaccelerated Dragon|e4 c5 Nf3 g6
        B30|Sicilian Defense: Old Sicilian|e4 c5 Nf3 Nc6
        B31|Sicilian Defense: Rossolimo Variation|e4 c5 Nf3 Nc6 Bb5
        B33|Sicilian Defense: Sveshnikov Variation|e4 c5 Nf3 Nc6 d4 cxd4 Nxd4 Nf6 Nc3 e5
        B40|Sicilian Defense: French Variation|e4 c5 Nf3 e6
        B50|Sicilian Defense: Modern Variations|e4 c5 Nf3 d6
        B54|Sicilian Defense: Open|e4 c5 Nf3 d6 d4 cxd4 Nxd4
        B60|Sicilian Defense: Richter-Rauzer Attack|e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 Nc6 Bg5
        B70|Sicilian Defense: Dragon Variation|e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 g6
        B80|Sicilian Defense: Scheveningen Variation|e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 e6
        B90|Sicilian Defense: Najdorf Variation|e4 c5 Nf3 d6 d4 cxd4 Nxd4 Nf6 Nc3 a6
        C00|French Defense|e4 e6
        C01|French Defense: Exchange Variation|e4 e6 d4 d5 exd5 exd5
        C02|French Defense: Advance Variation|e4 e6 d4 d5 e5
        C03|French Defense: Tarrasch Variation|e4 e6 d4 d5 Nd2
        C10|French Defense: Rubinstein Variation|e4 e6 d4 d5 Nc3 dxe4
        C11|French Defense: Classical Variation|e4 e6 d4 d5 Nc3 Nf6
        C15|French Defense: Winawer Variation|e4 e6 d4 d5 Nc3 Bb4
        C20|King’s Pawn Game|e4 e5
        C21|Danish Gambit|e4 e5 d4 exd4 c3
        C23|Bishop’s Opening|e4 e5 Bc4
        C25|Vienna Game|e4 e5 Nc3
        C30|King’s Gambit|e4 e5 f4
        C31|King’s Gambit Declined: Falkbeer Countergambit|e4 e5 f4 d5
        C33|King’s Gambit Accepted|e4 e5 f4 exf4
        C34|King’s Gambit Accepted: Knight Variation|e4 e5 f4 exf4 Nf3
        C40|King’s Knight Opening|e4 e5 Nf3
        C41|Philidor Defense|e4 e5 Nf3 d6
        C42|Russian Game (Petrov’s Defense)|e4 e5 Nf3 Nf6
        C44|King’s Knight Opening: Normal Variation|e4 e5 Nf3 Nc6
        C44|Scotch Game|e4 e5 Nf3 Nc6 d4
        C45|Scotch Game: Main Line|e4 e5 Nf3 Nc6 d4 exd4 Nxd4
        C46|Three Knights Game|e4 e5 Nf3 Nc6 Nc3
        C47|Four Knights Game|e4 e5 Nf3 Nc6 Nc3 Nf6
        C50|Italian Game|e4 e5 Nf3 Nc6 Bc4
        C50|Italian Game: Giuoco Piano|e4 e5 Nf3 Nc6 Bc4 Bc5
        C51|Italian Game: Evans Gambit|e4 e5 Nf3 Nc6 Bc4 Bc5 b4
        C54|Italian Game: Giuoco Piano, Classical Variation|e4 e5 Nf3 Nc6 Bc4 Bc5 c3 Nf6
        C55|Italian Game: Two Knights Defense|e4 e5 Nf3 Nc6 Bc4 Nf6
        C57|Italian Game: Two Knights Defense, Knight Attack|e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5
        C57|Italian Game: Two Knights Defense, Fried Liver Attack|e4 e5 Nf3 Nc6 Bc4 Nf6 Ng5 d5 exd5 Nxd5 Nxf7
        C60|Ruy Lopez|e4 e5 Nf3 Nc6 Bb5
        C63|Ruy Lopez: Schliemann Defense|e4 e5 Nf3 Nc6 Bb5 f5
        C65|Ruy Lopez: Berlin Defense|e4 e5 Nf3 Nc6 Bb5 Nf6
        C68|Ruy Lopez: Exchange Variation|e4 e5 Nf3 Nc6 Bb5 a6 Bxc6
        C70|Ruy Lopez: Morphy Defense|e4 e5 Nf3 Nc6 Bb5 a6
        C80|Ruy Lopez: Open|e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Nxe4
        C84|Ruy Lopez: Closed|e4 e5 Nf3 Nc6 Bb5 a6 Ba4 Nf6 O-O Be7
        D00|Queen’s Pawn Game|d4 d5
        D00|Queen’s Pawn Game: Accelerated London System|d4 d5 Bf4
        D01|Richter-Veresov Attack|d4 d5 Nc3 Nf6 Bg5
        D02|Queen’s Pawn Game: Zukertort Variation|d4 d5 Nf3
        D02|London System|d4 d5 Nf3 Nf6 Bf4
        D03|Torre Attack|d4 d5 Nf3 Nf6 Bg5
        D06|Queen’s Gambit|d4 d5 c4
        D10|Slav Defense|d4 d5 c4 c6
        D20|Queen’s Gambit Accepted|d4 d5 c4 dxc4
        D30|Queen’s Gambit Declined|d4 d5 c4 e6
        D31|Queen’s Gambit Declined: Queen’s Knight Variation|d4 d5 c4 e6 Nc3
        D35|Queen’s Gambit Declined: Exchange Variation|d4 d5 c4 e6 Nc3 Nf6 cxd5 exd5
        D43|Semi-Slav Defense|d4 d5 c4 c6 Nc3 Nf6 Nf3 e6
        D80|Grünfeld Defense|d4 Nf6 c4 g6 Nc3 d5
        E00|Catalan Opening|d4 Nf6 c4 e6 g3
        E11|Bogo-Indian Defense|d4 Nf6 c4 e6 Nf3 Bb4
        E12|Queen’s Indian Defense|d4 Nf6 c4 e6 Nf3 b6
        E20|Nimzo-Indian Defense|d4 Nf6 c4 e6 Nc3 Bb4
        E60|King’s Indian Defense|d4 Nf6 c4 g6
        E70|King’s Indian Defense: Normal Variation|d4 Nf6 c4 g6 Nc3 Bg7 e4 d6
    """.trimIndent()

    val entries: List<OpeningEntry> by lazy {
        RAW_OPENINGS.lines().filter { it.isNotBlank() }.map { line ->
            val parts = line.trim().split('|')
            OpeningEntry(parts[0], parts[1], parts[2])
        }
    }

    var invalidLineCount = 0
        private set

    val fenMap: Map<String, OpeningEntry> by lazy {
        val map = LinkedHashMap<String, OpeningEntry>()
        var bad = 0
        for (entry in entries) {
            val board = ChessBoard()
            var ok = true
            for (token in entry.moves.split(' ')) {
                if (board.makeSanMove(token) == null) {
                    ok = false
                    break
                }
            }
            if (!ok) {
                bad++
            } else {
                val key = board.openingKey()
                if (!map.containsKey(key)) map[key] = entry
            }
        }
        invalidLineCount = bad
        map
    }

    fun fenKey(fen: String): String = fen.split(' ').take(3).joinToString(" ")

    fun lookup(fen: String): OpeningEntry? = fenMap[fenKey(fen)]

    fun observeSide(fen: String, color: PieceColor): String {
        val board = ChessBoard(fen)
        val homeSquares = if (color == PieceColor.WHITE) {
            listOf("b1" to PieceType.KNIGHT, "g1" to PieceType.KNIGHT, "c1" to PieceType.BISHOP, "f1" to PieceType.BISHOP)
        } else {
            listOf("b8" to PieceType.KNIGHT, "g8" to PieceType.KNIGHT, "c8" to PieceType.BISHOP, "f8" to PieceType.BISHOP)
        }
        val undeveloped = homeSquares.count { (sq, pt) ->
            val p = board.pieceAt(sq)
            p != null && p.color == color && p.type == pt
        }
        val dev = 4 - undeveloped
        val ksIdx = board.kingSquare(color)
        val ks = if (ksIdx >= 0) squareName(ksIdx) else ""
        val castlingStatus = when (ks) {
            if (color == PieceColor.WHITE) "g1" else "g8" -> "castled kingside"
            if (color == PieceColor.WHITE) "c1" else "c8" -> "castled queenside"
            if (color == PieceColor.WHITE) "e1" else "e8" -> "not castled yet"
            else -> "king has moved"
        }
        val centerSq = if (color == PieceColor.WHITE) listOf("d4", "e4") else listOf("d5", "e5")
        val centerPawns = centerSq.filter { sq ->
            val p = board.pieceAt(sq)
            p != null && p.color == color && p.type == PieceType.PAWN
        }
        val centerText = if (centerPawns.isNotEmpty()) {
            "pawns on ${centerPawns.joinToString(" ")}"
        } else {
            "no pawns on the d/e centre squares"
        }
        return "$dev of 4 minor pieces developed · $castlingStatus · $centerText"
    }

    fun summarize(plies: List<AnalyzedPly>, evals: List<Double>): OpeningSummary {
        var lastEntry: OpeningEntry? = null
        var lastIdx = -1
        for (i in plies.indices) {
            if (i < 40) {
                val found = lookup(plies[i].fenAfter)
                if (found != null) {
                    lastEntry = found
                    lastIdx = i
                }
            }
        }
        fun moveSeq(n: Int): String = plies.take(n).mapIndexed { i, p ->
            (if (i % 2 == 0) "${i / 2 + 1}. " else "") + p.san
        }.joinToString(" ")

        fun moveNum(i: Int): String = "${i / 2 + 1}${if (i % 2 == 1) "..." else "."}"

        val lim = minOf(plies.size, 20)
        val firstBadIdx = plies.indexOfFirst { it.index < 20 && it.moveClass.ordinal >= MoveClass.INACCURACY.ordinal && it.moveClass != MoveClass.BRILLIANT }
        val whiteObs = if (lim > 0) observeSide(plies[lim - 1].fenAfter, PieceColor.WHITE) else ""
        val blackObs = if (lim > 0) observeSide(plies[lim - 1].fenAfter, PieceColor.BLACK) else ""

        val matched = if (lastEntry != null) {
            "Matched the first ${ceil((lastIdx + 1) / 2.0).toInt()} moves in the built-in opening list: ${moveSeq(lastIdx + 1)}."
        } else {
            "These moves are not in the built-in opening list."
        }
        val nextNote = if (lastEntry != null && lastIdx + 1 < plies.size) {
            val nextPly = plies[lastIdx + 1]
            "The next move, ${moveNum(lastIdx + 1)} ${nextPly.san}, is outside the list (this does not mean it is a mistake)."
        } else ""

        val e1 = if (firstBadIdx >= 0) {
            val p = plies[firstBadIdx]
            "First inaccuracy or worse in the opening: ${moveNum(firstBadIdx)} ${p.san} (${p.moveClass.label})" +
                (p.bestSan?.let { ". Engine prefers $it" } ?: "") + "."
        } else {
            "No inaccuracies in the first 10 moves."
        }
        val evalAtLim = evals.getOrElse(lim) { 0.0 }
        val e2 = "Evaluation after ${if (lim >= 20) "move 10" else "the last move"}: ${MoveExplainer.formatEval(evalAtLim)} (White’s view)."
        val phaseHd = "Position after ${if (lim >= 20) "move 10" else "the last move"}"

        return OpeningSummary(
            entry = lastEntry,
            lastBookIndex = lastIdx,
            matchedText = matched,
            nextMoveNote = nextNote,
            engineFirstNote = e1,
            engineEvalNote = e2,
            phaseHeading = phaseHd,
            whiteObs = whiteObs,
            blackObs = blackObs
        )
    }
}
