package com.example.chess

import kotlin.math.abs
import kotlin.math.exp

object MoveExplainer {

    fun winProbability(evalPawns: Double): Double {
        return 50.0 + 50.0 * (2.0 / (1.0 + exp(-0.00368208 * evalPawns * 100.0)) - 1.0)
    }

    fun moveAccuracy(loss: Double): Double {
        return (103.1668 * exp(-0.04354 * loss) - 3.1669).coerceIn(0.0, 100.0)
    }

    fun formatEval(e: Double): String {
        if (abs(e) >= 90.0) return (if (e > 0) "+" else "-") + "M"
        return (if (e > 0) "+" else "") + String.format(java.util.Locale.US, "%.1f", e)
    }

    fun formatPlyEval(eval: Double, mate: Int?): String {
        if (mate != null) {
            return if (mate == 0) "Checkmate" else (if (mate > 0) "+" else "-") + "M" + abs(mate)
        }
        return formatEval(eval)
    }

    data class HangingTarget(val move: Move, val recapturable: Boolean, val netGain: Int)

    fun hangs(fen: String): HangingTarget? {
        val b = ChessBoard(fen)
        var top: HangingTarget? = null
        var topScore = 0
        for (m in b.legalMoves()) {
            val cap = m.captured ?: continue
            b.makeMove(m)
            val rec = b.legalMoves().any { it.to == m.to && it.captured != null }
            b.undo()
            val net = cap.value - (if (rec) m.piece.value else 0)
            if (net > topScore) {
                topScore = net
                top = HangingTarget(m, rec, net)
            }
        }
        return if (topScore >= 1) top else null
    }

    private data class Threat(
        val sq: String,
        val target: PieceType,
        var net: Int,
        val by: MutableList<String>
    )

    private fun threatsAgainst(fen: String, color: PieceColor): List<Threat> {
        val parts = fen.split(' ').toMutableList()
        if (parts.size < 4) return emptyList()
        parts[1] = color.opposite().fenChar().toString()
        parts[3] = "-"
        val b = ChessBoard()
        if (!b.loadFen(parts.joinToString(" "))) return emptyList()
        if (b.inCheck(color)) return emptyList() // If own king already in check, side switch is invalid

        val map = linkedMapOf<String, Threat>()
        for (m in b.legalMoves()) {
            val cap = m.captured ?: continue
            b.makeMove(m)
            val rec = b.legalMoves().any { it.to == m.to && it.captured != null }
            b.undo()
            val net = cap.value - (if (rec) m.piece.value else 0)
            if (net >= 1) {
                val t = map.getOrPut(m.toName) { Threat(m.toName, cap, 0, mutableListOf()) }
                t.net = maxOf(t.net, net)
                t.by.add(m.fromName)
            }
        }
        return map.values.toList()
    }

    private data class PawnPos(val file: Int, val rank: Int)
    private data class PawnStruct(val isolated: List<String>, val doubled: List<String>, val passed: List<String>)

    private fun pawnsOf(fen: String, color: PieceColor): List<PawnPos> {
        val b = ChessBoard(fen)
        val out = mutableListOf<PawnPos>()
        for (sq in 0 until 64) {
            val p = b.board[sq]
            if (p != null && p.type == PieceType.PAWN && p.color == color) {
                out.add(PawnPos(sq % 8, 8 - (sq / 8)))
            }
        }
        return out
    }

    private fun pawnStructure(fen: String, color: PieceColor): PawnStruct {
        val mine = pawnsOf(fen, color)
        val opp = pawnsOf(fen, color.opposite())
        val iso = mutableListOf<String>()
        val dbl = mutableListOf<String>()
        val pas = mutableListOf<String>()
        for (p in mine) {
            val fileChar = 'a' + p.file
            if (mine.none { abs(it.file - p.file) == 1 }) {
                iso.add("$fileChar${p.rank}")
            }
            if (mine.count { it.file == p.file } > 1 && fileChar.toString() !in dbl) {
                dbl.add(fileChar.toString())
            }
            val blockedByOpp = opp.any { q ->
                abs(q.file - p.file) <= 1 && (if (color == PieceColor.WHITE) q.rank > p.rank else q.rank < p.rank)
            }
            if (!blockedByOpp) {
                pas.add("$fileChar${p.rank}")
            }
        }
        return PawnStruct(iso, dbl, pas)
    }

    private data class StructureDelta(val bad: List<String>, val good: List<String>)

    private fun structureChanges(fenBefore: String, fenAfter: String, color: PieceColor): StructureDelta {
        val opp = color.opposite()
        val a = pawnStructure(fenBefore, color)
        val b = pawnStructure(fenAfter, color)
        val x = pawnStructure(fenBefore, opp)
        val y = pawnStructure(fenAfter, opp)

        fun newlyAdded(old: List<String>, cur: List<String>) =
            cur.filter { s -> old.none { it[0] == s[0] } }

        val bad = buildList {
            newlyAdded(a.isolated, b.isolated).forEach { add("creates an isolated pawn on the ${it[0]}-file") }
            b.doubled.filter { it !in a.doubled }.forEach { add("creates doubled pawns on the $it-file") }
            newlyAdded(x.passed, y.passed).forEach { add("gives the opponent a passed pawn on $it") }
        }
        val good = newlyAdded(a.passed, b.passed).map { "creates a passed pawn on $it" }
        return StructureDelta(bad, good)
    }

    private fun attackedTargetsFrom(fen: String, sqName: String): List<PieceType> {
        val parts = fen.split(' ').toMutableList()
        if (parts.size < 4) return emptyList()
        parts[1] = if (parts[1] == "w") "b" else "w"
        parts[3] = "-"
        val b = ChessBoard()
        if (!b.loadFen(parts.joinToString(" "))) return emptyList()
        val sq = parseSquare(sqName) ?: return emptyList()
        return b.legalMoves(sq).mapNotNull { it.captured }
    }

    private fun describeMove(san: String, fenBefore: String, plyIdx: Int): String {
        val b = ChessBoard(fenBefore)
        val m = b.makeSanMove(san) ?: return ""
        if (b.inCheckmate()) return "delivers checkmate"
        val after = b.toFen()
        val parts = mutableListOf<String>()
        if (m.isKingSideCastle || m.isQueenSideCastle) parts.add("castles, bringing the king to safety")
        if (m.captured != null) parts.add("captures the ${m.captured.pieceName}")
        if (m.promotion != null) parts.add("promotes the pawn")

        val forked = attackedTargetsFrom(after, m.toName).filter { it.value >= 3 || it == PieceType.KING }
        if (forked.size >= 2) {
            parts.add("forks the ${forked.joinToString(" and the ") { it.pieceName }}")
        } else if (b.inCheck()) {
            parts.add("gives check")
        }

        if (m.piece == PieceType.ROOK) {
            val fi = m.to % 8
            val allPawns = pawnsOf(after, PieceColor.WHITE) + pawnsOf(after, PieceColor.BLACK)
            if (allPawns.none { it.file == fi }) {
                parts.add("places the rook on the open ${m.toName[0]}-file")
            }
        }
        parts.addAll(structureChanges(fenBefore, after, m.color).good)
        if (parts.isEmpty() && (m.piece == PieceType.KNIGHT || m.piece == PieceType.BISHOP) && plyIdx < 20) {
            parts.add("develops the ${m.piece.pieceName}")
        }
        if (parts.isEmpty() && m.piece == PieceType.PAWN && plyIdx < 12 && Regex("[de][45]").matches(m.toName)) {
            parts.add("claims the centre")
        }
        return parts.joinToString(" and ")
    }

    private fun pieceMobility(fen: String, color: PieceColor): Map<String, Int> {
        val parts = fen.split(' ').toMutableList()
        if (parts.size < 4) return emptyMap()
        parts[1] = color.fenChar().toString()
        parts[3] = "-"
        val b = ChessBoard()
        if (!b.loadFen(parts.joinToString(" "))) return emptyMap()
        val map = mutableMapOf<String, Int>()
        for (m in b.legalMoves()) {
            map[m.fromName] = (map[m.fromName] ?: 0) + 1
        }
        return map
    }

    private data class PvSummary(
        val san: List<String>,
        val lost: List<PieceType>,
        val won: List<PieceType>,
        val mate: Boolean
    )

    private fun summarizePv(fen: String, pvUci: List<String>, color: PieceColor, limit: Int = 4): PvSummary {
        val b = ChessBoard(fen)
        val san = mutableListOf<String>()
        val lost = mutableListOf<PieceType>()
        val won = mutableListOf<PieceType>()
        var mate = false
        for (u in pvUci.take(limit)) {
            val m = b.makeUciMove(u) ?: break
            san.add(b.sanHistory().last())
            if (m.captured != null) {
                if (m.color == color) won.add(m.captured) else lost.add(m.captured)
            }
            if (b.inCheckmate()) mate = true
        }
        return PvSummary(san, lost, won, mate)
    }

    private fun nameList(list: List<PieceType>): String = list.joinToString(" and ") { "a ${it.pieceName}" }
    private fun valSum(list: List<PieceType>): Int = list.sumOf { it.value }

    private fun pickVariant(idx: Int, options: List<String>, used: MutableSet<String>): String {
        val n = options.size
        val start = (idx * 7 + 3) % n
        for (j in 0 until n) {
            val cand = options[(start + j) % n]
            if (cand !in used) {
                used.add(cand)
                return cand
            }
        }
        return options[start]
    }

    private fun playedWhy(plyIdx: Int, m: Move, prevSideMove: Move?): List<String> {
        val out = mutableListOf<String>()
        if (m.piece == PieceType.KNIGHT && (m.toName[0] == 'a' || m.toName[0] == 'h')) {
            out.add("The knight on ${m.toName} sits on the edge, where it controls fewer squares and does little.")
        } else if (prevSideMove != null && prevSideMove.piece == m.piece && prevSideMove.to == m.from && plyIdx < 24) {
            out.add("You move the same ${m.piece.pieceName} a second time while other pieces are still undeveloped.")
        } else if (m.piece == PieceType.PAWN && plyIdx < 16 && m.toName[0] !in charArrayOf('c', 'd', 'e', 'f') && m.captured == null) {
            out.add("A flank pawn move does little for the centre or for development.")
        } else if (m.piece == PieceType.QUEEN && plyIdx < 14) {
            out.add("Bringing the queen out early lets the opponent gain time by attacking it.")
        } else if (m.piece == PieceType.KING && !m.isKingSideCastle && !m.isQueenSideCastle && plyIdx < 24) {
            out.add("Moving the king by hand costs you the right to castle.")
        }
        return out
    }

    fun explainMove(
        plyIdx: Int,
        san: String,
        move: Move,
        fenBefore: String,
        fenAfter: String,
        evalBefore: Double,
        evalAfter: Double,
        mateAfter: Int?,
        moveClass: MoveClass,
        isBook: Boolean,
        opening: OpeningEntry?,
        bestSan: String?,
        pvUci: List<String>,
        nextPvUci: List<String>,
        prevSideMove: Move?,
        usedPhrases: MutableSet<String>
    ): String {
        val isWhite = plyIdx % 2 == 0
        val color = if (isWhite) PieceColor.WHITE else PieceColor.BLACK
        val opp = color.opposite()
        val sideEvalBefore = if (isWhite) evalBefore else -evalBefore

        val boardAfter = ChessBoard(fenAfter)
        val isMateAfter = boardAfter.inCheckmate()
        val nonPawnPieces = boardAfter.board.count { it != null && it.type != PieceType.KING && it.type != PieceType.PAWN }
        val phase = when {
            plyIdx < 20 -> 'o'
            nonPawnPieces <= 6 -> 'e'
            else -> 'm'
        }

        val t0 = runCatching { threatsAgainst(fenBefore, color) }.getOrDefault(emptyList())
        val t1 = runCatching { threatsAgainst(fenAfter, color) }.getOrDefault(emptyList())
        val n0 = runCatching { threatsAgainst(fenBefore, opp) }.getOrDefault(emptyList())
        val n1 = runCatching { threatsAgainst(fenAfter, opp) }.getOrDefault(emptyList())

        val savedThreat = t0.find { t ->
            if (t.sq == move.fromName) t1.none { it.sq == move.toName }
            else t1.none { it.sq == t.sq }
        }
        val newThreat = n1.find { t ->
            move.toName in t.by && n0.none { it.sq == t.sq && move.fromName in it.by }
        }

        val ex = mutableListOf<String>()
        if (isBook && opening != null) {
            val d = describeMove(san, fenBefore, plyIdx)
            val intro = pickVariant(
                plyIdx,
                listOf(
                    "This is standard theory in the ${opening.name} (${opening.eco}).",
                    "A known opening move: it belongs to the ${opening.name} (${opening.eco}).",
                    "Opening book: ${opening.name} (${opening.eco})."
                ),
                usedPhrases
            )
            ex.add(intro + if (d.isNotEmpty()) " The move $d." else " Strong players have used it for years.")
        } else if (moveClass == MoveClass.BEST || moveClass == MoveClass.GOOD || moveClass == MoveClass.BRILLIANT) {
            if (isMateAfter) {
                ex.add("Checkmate: the king has no escape.")
            } else {
                if (moveClass == MoveClass.BRILLIANT) {
                    ex.add("A real sacrifice: you give up material, yet the engine confirms the position is stronger afterwards.")
                }
                if (savedThreat != null) {
                    ex.add(
                        when {
                            move.fromName == savedThreat.sq -> pickVariant(
                                plyIdx,
                                listOf(
                                    "The ${savedThreat.target.pieceName} was under attack, and this move takes it to safety.",
                                    "This moves the attacked ${savedThreat.target.pieceName} out of danger."
                                ),
                                usedPhrases
                            )
                            move.toName in savedThreat.by -> "It removes the piece that was attacking the ${savedThreat.target.pieceName} on ${savedThreat.sq}."
                            else -> pickVariant(
                                plyIdx,
                                listOf(
                                    "This defends the ${savedThreat.target.pieceName} on ${savedThreat.sq}, which was under attack.",
                                    "The ${savedThreat.target.pieceName} on ${savedThreat.sq} was attacked; now it is protected."
                                ),
                                usedPhrases
                            )
                        }
                    )
                } else if (move.captured != null) {
                    ex.add(
                        pickVariant(
                            plyIdx,
                            listOf(
                                "Captures the ${move.captured.pieceName} on ${move.toName}.",
                                "Wins the ${move.captured.pieceName} on ${move.toName}."
                            ),
                            usedPhrases
                        )
                    )
                } else if (newThreat != null) {
                    ex.add("Now the ${move.piece.pieceName} attacks the ${newThreat.target.pieceName} on ${newThreat.sq}, so the opponent has to react.")
                } else if (move.isKingSideCastle || move.isQueenSideCastle) {
                    ex.add(
                        pickVariant(
                            plyIdx,
                            listOf(
                                "Castling tucks the king away and brings a rook into play.",
                                "You castle: the king is safer and the rook joins the game."
                            ),
                            usedPhrases
                        )
                    )
                } else if (boardAfter.inCheck()) {
                    ex.add(
                        pickVariant(
                            plyIdx,
                            listOf(
                                "Check: the opponent must answer it first.",
                                "This check forces the opponent to react and gains you a tempo.",
                                "You give check, so the opponent has no time for their own plans."
                            ),
                            usedPhrases
                        )
                    )
                } else {
                    val d = describeMove(san, fenBefore, plyIdx)
                    if (d.isNotEmpty()) {
                        ex.add("This move $d.")
                    } else {
                        val quietOpts = when (phase) {
                            'o' -> listOf(
                                "A sound developing move that keeps the position healthy.",
                                "Nothing is hanging, and your pieces keep working together.",
                                "Natural and safe: the move fits your plan and gives the opponent nothing to attack.",
                                "A calm move that helps your pieces coordinate."
                            )
                            'e' -> listOf(
                                "In the endgame, this improves the activity of your pieces.",
                                "A precise endgame move that keeps your plans on track.",
                                "A useful waiting move that keeps your structure intact.",
                                "Good technique: you improve your worst-placed piece."
                            )
                            else -> listOf(
                                "A quiet move that improves your piece placement without allowing tactics.",
                                "A solid move: it keeps the balance and avoids weaknesses.",
                                "You tidy up the position and leave no targets for the opponent.",
                                "A patient move that keeps all your pieces protected."
                            )
                        }
                        ex.add(pickVariant(plyIdx, quietOpts, usedPhrases))
                    }
                }
            }
            if (moveClass == MoveClass.GOOD && bestSan != null && bestSan != san) {
                ex.add("The engine slightly prefers $bestSan.")
            }
        } else {
            val h = hangs(fenAfter)
            if (sideEvalBefore >= 90.0) {
                ex.add("You had a forced checkmate here and missed it.")
            }
            if (h != null) {
                ex.add(
                    if (h.recapturable) {
                        "Your ${h.move.captured?.pieceName} on ${h.move.toName} can be taken by a ${h.move.piece.pieceName}, and you lose material in the exchange."
                    } else {
                        pickVariant(
                            plyIdx,
                            listOf(
                                "Your ${h.move.captured?.pieceName} on ${h.move.toName} is left hanging: it is attacked and has no defender.",
                                "This leaves the ${h.move.captured?.pieceName} on ${h.move.toName} without protection, so it can simply be captured."
                            ),
                            usedPhrases
                        )
                    }
                )
            } else {
                val u = t1.find { it.net >= 3 }
                if (u != null) ex.add("This leaves your ${u.target.pieceName} on ${u.sq} under attack.")
            }

            var pm = 0
            ex.addAll(playedWhy(plyIdx, move, prevSideMove))
            if (mateAfter != null && mateAfter != 0 && (if (isWhite) mateAfter < 0 else mateAfter > 0)) {
                ex.add("This allows a forced checkmate in ${abs(mateAfter)}.")
            }
            if (nextPvUci.isNotEmpty()) {
                val rl = summarizePv(fenAfter, nextPvUci, color, 4)
                if (rl.san.isNotEmpty()) {
                    val d = describeMove(rl.san[0], fenAfter, plyIdx + 1)
                    val net = valSum(rl.lost) - valSum(rl.won)
                    ex.add("After $san, the opponent can reply ${rl.san[0]}${if (d.isNotEmpty()) ", which $d" else ""}.")
                    if (rl.mate) {
                        ex.add("The line ${rl.san.joinToString(" ")} ends in checkmate against you.")
                    } else if (rl.lost.isNotEmpty() && net >= 1) {
                        pm = -net
                        val wonPart = if (rl.won.isNotEmpty()) " and win back ${nameList(rl.won)}" else ""
                        ex.add("Following ${rl.san.joinToString(" ")}, you lose ${nameList(rl.lost)}$wonPart, ending $net point${if (net > 1) "s" else ""} down.")
                    } else if (rl.san.size > 1) {
                        ex.add("The likely continuation is ${rl.san.joinToString(" ")}, after which your opponent has the initiative.")
                    }
                }
            }

            structureChanges(fenBefore, fenAfter, color).bad.forEach { ex.add("The move $it.") }
            if (ex.isEmpty()) ex.add("This move does not improve your position as much as the alternative.")

            if (bestSan != null) {
                val d2 = describeMove(bestSan, fenBefore, plyIdx)
                var t2 = ""
                if (pvUci.isNotEmpty()) {
                    val bl = summarizePv(fenBefore, pvUci, color, 4)
                    if (bl.san.isNotEmpty()) {
                        val n2 = valSum(bl.won) - valSum(bl.lost)
                        t2 = when {
                            bl.mate -> " ${bl.san.joinToString(" ")} leads to checkmate."
                            n2 >= 1 -> " After ${bl.san.joinToString(" ")} you win ${nameList(bl.won)}."
                            else -> " After ${bl.san.joinToString(" ")} everything of yours stays safe."
                        }
                    }
                }
                val clause = if (d2.isNotEmpty()) ", which $d2" else ", the more accurate way to keep your position together"
                ex.add("You should have played $bestSan$clause.$t2")
            }

            val lesson = when {
                h != null || pm <= -2 -> pickVariant(
                    plyIdx,
                    listOf(
                        "Lesson: before each move, ask which of your pieces are undefended.",
                        "Lesson: check what the opponent can capture after your move.",
                        "Lesson: if a piece is attacked, defend it or move it before doing anything else.",
                        "Lesson: a free piece for the opponent usually decides the game - check twice."
                    ),
                    usedPhrases
                )
                phase == 'o' -> pickVariant(
                    plyIdx,
                    listOf(
                        "Lesson: in the opening, develop pieces, castle early and fight for the centre.",
                        "Lesson: avoid moving the same piece twice before your army is developed.",
                        "Lesson: do not grab pawns early if it leaves your king or pieces behind.",
                        "Lesson: stick to opening ideas - development first, tactics second."
                    ),
                    usedPhrases
                )
                phase == 'e' -> "Lesson: in endgames, activate your king and count the pawn races."
                else -> pickVariant(
                    plyIdx,
                    listOf(
                        "Lesson: before moving, look at checks, captures and threats on both sides.",
                        "Lesson: a quiet move still needs a safety check - what does it allow?",
                        "Lesson: ask what your opponent wants to do next, then stop it.",
                        "Lesson: compare two or three candidate moves before you choose."
                    ),
                    usedPhrases
                )
            }
            ex.add(lesson)
        }

        return ex.joinToString("\n")
    }

    fun explainBestAlternative(
        plyIdx: Int,
        san: String,
        move: Move,
        moveClass: MoveClass,
        fenBefore: String,
        bestSan: String?,
        pvUci: List<String>,
        prevSideMove: Move?
    ): String {
        if (bestSan == null) return ""
        val isWhite = plyIdx % 2 == 0
        val color = if (isWhite) PieceColor.WHITE else PieceColor.BLACK
        val b = ChessBoard(fenBefore)
        val rb = b.makeSanMove(bestSan) ?: return ""
        val fa = b.toFen()
        val ex = mutableListOf<String>()

        val d = describeMove(bestSan, fenBefore, plyIdx)
        ex.add("$bestSan ${if (d.isNotEmpty()) d else "is the most accurate move here"}.")

        val t0 = runCatching { threatsAgainst(fenBefore, color) }.getOrDefault(emptyList())
        val t1 = runCatching { threatsAgainst(fa, color) }.getOrDefault(emptyList())
        val sv = t0.find { t ->
            if (t.sq == rb.fromName) t1.none { it.sq == rb.toName }
            else t1.none { it.sq == t.sq }
        }
        if (sv != null && !d.contains("safety") && !d.contains("defend")) {
            ex.add("It also deals with the threat to your ${sv.target.pieceName} on ${sv.sq}.")
        }

        val m0 = pieceMobility(fenBefore, color)
        val m1 = pieceMobility(fa, color)
        var bestSq: String? = null
        var bestGain = 0
        for ((sq, cnt) in m1) {
            if (sq == rb.fromName || sq == rb.toName) continue
            val gain = cnt - (m0[sq] ?: 0)
            val piece = b.pieceAt(sq)
            if (piece != null && (piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN || piece.type == PieceType.ROOK) && gain > bestGain) {
                bestGain = gain
                bestSq = sq
            }
        }
        if (bestSq != null && bestGain >= 2) {
            val pt = b.pieceAt(bestSq)?.type?.pieceName ?: "piece"
            ex.add("It frees your $pt on $bestSq, which can now reach ${m1[bestSq]} squares instead of ${m0[bestSq] ?: 0}.")
        }

        if (rb.piece == PieceType.PAWN && Regex("^[cdef][45]$").matches(rb.toName) && rb.captured == null) {
            ex.add("It also claims the central square ${rb.toName}.")
        }

        if (pvUci.isNotEmpty()) {
            val bl = summarizePv(fenBefore, pvUci, color, 4)
            if (bl.san.size > 1) {
                val n2 = valSum(bl.won) - valSum(bl.lost)
                val suffix = when {
                    bl.mate -> " - checkmate."
                    n2 >= 1 -> " - you win ${nameList(bl.won)}."
                    else -> "."
                }
                ex.add("Likely continuation: ${bl.san.joinToString(" ")}$suffix")
            }
        }

        val pw = playedWhy(plyIdx, move, prevSideMove)
        if (pw.isNotEmpty()) ex.add("Your move: ${pw[0]}")
        else if (moveClass.ordinal >= MoveClass.INACCURACY.ordinal && moveClass != MoveClass.BRILLIANT) {
            ex.add("$san does less for your position than $bestSan.")
        }

        return ex.joinToString(" ")
    }

    fun buildShareReport(result: GameAnalysisResult): String {
        val s = result.summaryStats
        val wName = s.whiteName
        val bName = s.blackName
        val wRes = result.headers["Result"] ?: "*"
        val op = result.openingSummary
        val wp = result.plies.filterIndexed { i, _ -> i % 2 == 0 }
        val bp = result.plies.filterIndexed { i, _ -> i % 2 == 1 }
        val key = result.keyMistakes.take(3).map { p ->
            "${p.index / 2 + 1}${if (p.index % 2 == 1) "..." else "."} ${p.san} (${p.moveClass.label}${p.bestSan?.let { ", best was $it" } ?: ""})"
        }
        val moveText = result.plies.mapIndexed { i, p ->
            (if (i % 2 == 0) "${i / 2 + 1}. " else "") + p.san
        }.joinToString(" ") + " $wRes"

        val mvLines = result.plies.mapIndexed { i, p ->
            val exp = p.explanation.replace("\n", "\n      ")
            "${i / 2 + 1}${if (i % 2 == 1) "..." else "."} ${p.san} — ${p.moveClass.label} (eval ${formatPlyEval(p.evalAfter, p.mate)})\n      $exp"
        }

        return buildList {
            add("KnightScope game review")
            add("$wName vs $bName")
            if (!result.headers["Result"].isNullOrBlank()) add("Result: ${result.headers["Result"]}")
            if (op.entry != null) add("Opening: ${op.entry.name} (${op.entry.eco})")
            add("")
            add("Accuracy: $wName ${String.format(java.util.Locale.US, "%.1f", s.whiteAccuracy)}% | $bName ${String.format(java.util.Locale.US, "%.1f", s.blackAccuracy)}%")
            add("Mistakes / blunders: $wName ${s.whiteCounts[MoveClass.MISTAKE] ?: 0}/${s.whiteCounts[MoveClass.BLUNDER] ?: 0} | $bName ${s.blackCounts[MoveClass.MISTAKE] ?: 0}/${s.blackCounts[MoveClass.BLUNDER] ?: 0}")
            if (key.isNotEmpty()) add("Key mistakes: ${key.joinToString("; ")}")
            add("")
            add("OPENING SUMMARY")
            if (op.entry != null) add("Opening list: ${op.entry.name} (${op.entry.eco})")
            add(op.matchedText)
            if (op.nextMoveNote.isNotEmpty()) add(op.nextMoveNote)
            add("Engine: ${op.engineFirstNote}")
            add("Engine: ${op.engineEvalNote}")
            add(op.phaseHeading + ":")
            add("White: ${op.whiteObs}")
            add("Black: ${op.blackObs}")
            add("")
            add("MOVE-BY-MOVE ANALYSIS")
            add("(Evaluations are from White’s point of view.)")
            addAll(mvLines)
            add("")
            add("PGN:")
            add("[White \"$wName\"]")
            add("[Black \"$bName\"]")
            if (!result.headers["Date"].isNullOrBlank()) add("[Date \"${result.headers["Date"]}\"]")
            add("[Result \"$wRes\"]")
            add("")
            add(moveText)
        }.joinToString("\n")
    }

    data class CoachInsights(
        val headline: String,
        val tacticalTags: List<String>
    )

    fun buildCoachInsights(
        plyIdx: Int,
        san: String,
        move: Move,
        fenBefore: String,
        fenAfter: String,
        moveClass: MoveClass,
        isBook: Boolean,
        isGreatMove: Boolean,
        isMissedWin: Boolean,
        opening: OpeningEntry?,
        bestSan: String?,
        topLines: List<EngineVariation>
    ): CoachInsights {
        val boardAfter = ChessBoard(fenAfter)
        val color = if (plyIdx % 2 == 0) PieceColor.WHITE else PieceColor.BLACK
        val opp = color.opposite()

        val tags = mutableListOf<String>()
        if (isBook) tags.add("Opening Theory")
        if (move.isKingSideCastle || move.isQueenSideCastle) tags.add("King Safety")
        if (boardAfter.inCheckmate()) tags.add("Checkmate")
        else if (boardAfter.inCheck()) tags.add("Check")
        if (move.captured != null) tags.add("Capture")
        if (move.promotion != null) tags.add("Promotion")

        val forked = runCatching { attackedTargetsFrom(fenAfter, move.toName) }
            .getOrDefault(emptyList())
            .filter { it.value >= 3 || it == PieceType.KING }
        if (forked.size >= 2) tags.add("Fork")

        if (moveClass == MoveClass.BRILLIANT) tags.add("Sacrifice")
        if (isGreatMove) tags.add("Only Move")
        if (isMissedWin) tags.add("Missed Win")

        if (moveClass.ordinal >= MoveClass.INACCURACY.ordinal && moveClass != MoveClass.BRILLIANT) {
            if (hangs(fenAfter) != null) tags.add("Hanging Piece")
            val threats = runCatching { threatsAgainst(fenAfter, color) }.getOrDefault(emptyList())
            if (threats.any { it.net >= 3 } && "Hanging Piece" !in tags) tags.add("Under Attack")
        } else {
            val newThreats = runCatching { threatsAgainst(fenAfter, opp) }.getOrDefault(emptyList())
            if (newThreats.any { move.toName in it.by && it.net >= 3 }) tags.add("Threat")
        }

        val bestTop = topLines.firstOrNull()
        val bestEvalText = bestTop?.let { formatPlyEval(it.cp, it.mate) }

        val headline = when {
            isBook && opening != null -> "$san is theory — ${opening.name}"
            isBook -> "$san follows opening book theory"
            moveClass == MoveClass.BRILLIANT -> "$san!! is a Brilliant sacrifice"
            isGreatMove -> "$san! is a Great move — the only accurate continuation"
            moveClass == MoveClass.BEST -> "$san is the best engine move" + (bestEvalText?.let { " ($it)" } ?: "")
            moveClass == MoveClass.GOOD -> "$san is solid" + if (bestSan != null && bestSan != san) " (engine slightly prefers $bestSan)" else ""
            moveClass == MoveClass.INACCURACY -> "$san?! is an inaccuracy" + (bestSan?.let { " — $it was sharper" } ?: "")
            isMissedWin -> "$san✖ misses a winning tactical chance" + (bestSan?.let { " ($it wins)" } ?: "")
            moveClass == MoveClass.MISTAKE -> "$san? is a mistake" + (bestSan?.let { " — $it kept control" } ?: "")
            else -> "$san?? is a blunder" + (bestSan?.let { " — $it was required" } ?: "")
        }

        return CoachInsights(
            headline = headline,
            tacticalTags = tags.distinct().take(4)
        )
    }
}
