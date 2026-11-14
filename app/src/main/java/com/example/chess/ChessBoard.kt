package com.example.chess

class ChessBoard(fen: String = START_FEN) {

    companion object {
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        private val KNIGHT_OFFSETS = arrayOf(
            intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, -2), intArrayOf(-1, 2),
            intArrayOf(1, -2), intArrayOf(1, 2), intArrayOf(2, -1), intArrayOf(2, 1)
        )
        private val BISHOP_DIRS = arrayOf(
            intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1)
        )
        private val ROOK_DIRS = arrayOf(
            intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1)
        )
        private val KING_OFFSETS = arrayOf(
            intArrayOf(-1, -1), intArrayOf(-1, 0), intArrayOf(-1, 1),
            intArrayOf(0, -1), intArrayOf(0, 1),
            intArrayOf(1, -1), intArrayOf(1, 0), intArrayOf(1, 1)
        )
    }

    private data class UndoState(
        val move: Move,
        val capturedPiece: ChessPiece?,
        val castlingMask: Int,
        val epSquare: Int,
        val halfMoveClock: Int,
        val fullMoveNumber: Int,
        val turn: PieceColor,
        val positionKey: String,
        val san: String
    )

    val board = arrayOfNulls<ChessPiece>(64)
    var turn: PieceColor = PieceColor.WHITE
        private set

    // Castling bitmask: 1 = WK, 2 = WQ, 4 = BK, 8 = BQ
    var castlingMask: Int = 0
        private set
    var epSquare: Int = -1
        private set
    var halfMoveClock: Int = 0
        private set
    var fullMoveNumber: Int = 1
        private set

    private val history = mutableListOf<UndoState>()
    private val positionCounts = mutableMapOf<String, Int>()
    private val pgnHeaders = linkedMapOf<String, String>()

    init {
        loadFen(fen)
    }

    fun copy(): ChessBoard {
        val c = ChessBoard(toFen())
        c.pgnHeaders.putAll(pgnHeaders)
        return c
    }

    fun setHeader(key: String, value: String) {
        pgnHeaders[key] = value
    }

    fun loadFen(fen: String): Boolean {
        val parts = fen.trim().split(Regex("\\s+"))
        if (parts.size < 4) return false

        board.fill(null)
        history.clear()
        positionCounts.clear()

        val rows = parts[0].split('/')
        if (rows.size != 8) return false

        for (r in 0 until 8) {
            var f = 0
            for (ch in rows[r]) {
                if (ch.isDigit()) {
                    f += ch - '0'
                } else {
                    if (f >= 8) return false
                    val type = PieceType.fromChar(ch) ?: return false
                    val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                    board[r * 8 + f] = ChessPiece(color, type)
                    f++
                }
            }
            if (f != 8) return false
        }

        turn = if (parts[1] == "b") PieceColor.BLACK else PieceColor.WHITE

        castlingMask = 0
        val castling = parts[2]
        if (castling != "-") {
            if ('K' in castling) castlingMask = castlingMask or 1
            if ('Q' in castling) castlingMask = castlingMask or 2
            if ('k' in castling) castlingMask = castlingMask or 4
            if ('q' in castling) castlingMask = castlingMask or 8
        }

        epSquare = if (parts[3] == "-") -1 else (parseSquare(parts[3]) ?: -1)
        halfMoveClock = parts.getOrNull(4)?.toIntOrNull() ?: 0
        fullMoveNumber = parts.getOrNull(5)?.toIntOrNull() ?: 1

        val key = positionKey()
        positionCounts[key] = 1
        return true
    }

    fun toFen(): String {
        val sb = StringBuilder()
        for (r in 0 until 8) {
            var empty = 0
            for (f in 0 until 8) {
                val p = board[r * 8 + f]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.fenChar())
                }
            }
            if (empty > 0) sb.append(empty)
            if (r < 7) sb.append('/')
        }
        sb.append(' ').append(turn.fenChar()).append(' ')
        if (castlingMask == 0) {
            sb.append('-')
        } else {
            if ((castlingMask and 1) != 0) sb.append('K')
            if ((castlingMask and 2) != 0) sb.append('Q')
            if ((castlingMask and 4) != 0) sb.append('k')
            if ((castlingMask and 8) != 0) sb.append('q')
        }
        sb.append(' ').append(if (epSquare == -1) "-" else squareName(epSquare))
        sb.append(' ').append(halfMoveClock).append(' ').append(fullMoveNumber)
        return sb.toString()
    }

    fun positionKey(): String {
        val sb = StringBuilder()
        for (i in 0 until 64) {
            val p = board[i]
            sb.append(p?.fenChar() ?: '.')
        }
        sb.append(turn.fenChar()).append(castlingMask).append(epSquare)
        return sb.toString()
    }

    fun openingKey(): String = toFen().split(' ').take(3).joinToString(" ")

    fun pieceAt(sq: Int): ChessPiece? = if (sq in 0..63) board[sq] else null
    fun pieceAt(name: String): ChessPiece? = parseSquare(name)?.let { board[it] }

    fun kingSquare(color: PieceColor): Int {
        for (i in 0 until 64) {
            val p = board[i]
            if (p != null && p.color == color && p.type == PieceType.KING) return i
        }
        return -1
    }

    fun isSquareAttacked(sq: Int, byColor: PieceColor): Boolean {
        val r = sq / 8
        val f = sq % 8

        // Pawns
        val pawnRow = if (byColor == PieceColor.WHITE) r + 1 else r - 1
        if (pawnRow in 0..7) {
            for (df in intArrayOf(-1, 1)) {
                val pf = f + df
                if (pf in 0..7) {
                    val p = board[pawnRow * 8 + pf]
                    if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
                }
            }
        }

        // Knights
        for (d in KNIGHT_OFFSETS) {
            val nr = r + d[0]
            val nf = f + d[1]
            if (nr in 0..7 && nf in 0..7) {
                val p = board[nr * 8 + nf]
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // Kings
        for (d in KING_OFFSETS) {
            val kr = r + d[0]
            val kf = f + d[1]
            if (kr in 0..7 && kf in 0..7) {
                val p = board[kr * 8 + kf]
                if (p != null && p.color == byColor && p.type == PieceType.KING) return true
            }
        }

        // Diagonals (Bishop / Queen)
        for (d in BISHOP_DIRS) {
            var cr = r + d[0]
            var cf = f + d[1]
            while (cr in 0..7 && cf in 0..7) {
                val p = board[cr * 8 + cf]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                cr += d[0]
                cf += d[1]
            }
        }

        // Orthogonals (Rook / Queen)
        for (d in ROOK_DIRS) {
            var cr = r + d[0]
            var cf = f + d[1]
            while (cr in 0..7 && cf in 0..7) {
                val p = board[cr * 8 + cf]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                cr += d[0]
                cf += d[1]
            }
        }

        return false
    }

    fun inCheck(color: PieceColor = turn): Boolean {
        val ks = kingSquare(color)
        return ks != -1 && isSquareAttacked(ks, color.opposite())
    }

    fun legalMoves(fromSquare: Int? = null): List<Move> {
        val pseudo = generatePseudoLegalMoves(fromSquare)
        val legal = ArrayList<Move>(pseudo.size)
        val us = turn
        for (m in pseudo) {
            val savedCap = applyMoveInternal(m)
            if (!inCheck(us)) {
                legal.add(m)
            }
            undoMoveInternal(m, savedCap)
        }
        return legal
    }

    private fun generatePseudoLegalMoves(fromFilter: Int? = null): List<Move> {
        val moves = ArrayList<Move>(48)
        val us = turn
        val them = us.opposite()
        val startIdx = fromFilter ?: 0
        val endIdx = fromFilter ?: 63

        for (sq in startIdx..endIdx) {
            val p = board[sq] ?: continue
            if (p.color != us) continue
            val r = sq / 8
            val f = sq % 8

            when (p.type) {
                PieceType.PAWN -> {
                    val dir = if (us == PieceColor.WHITE) -1 else 1
                    val startRow = if (us == PieceColor.WHITE) 6 else 1
                    val promoRow = if (us == PieceColor.WHITE) 0 else 7
                    val oneStepR = r + dir
                    if (oneStepR in 0..7) {
                        val oneSq = oneStepR * 8 + f
                        if (board[oneSq] == null) {
                            if (oneStepR == promoRow) {
                                addPromotions(moves, sq, oneSq, us, null)
                            } else {
                                moves.add(Move(sq, oneSq, PieceType.PAWN, us))
                                if (r == startRow) {
                                    val twoSq = (r + 2 * dir) * 8 + f
                                    if (board[twoSq] == null) {
                                        moves.add(Move(sq, twoSq, PieceType.PAWN, us))
                                    }
                                }
                            }
                        }
                        for (df in intArrayOf(-1, 1)) {
                            val cf = f + df
                            if (cf in 0..7) {
                                val capSq = oneStepR * 8 + cf
                                val target = board[capSq]
                                if (target != null && target.color == them) {
                                    if (oneStepR == promoRow) {
                                        addPromotions(moves, sq, capSq, us, target.type)
                                    } else {
                                        moves.add(Move(sq, capSq, PieceType.PAWN, us, captured = target.type))
                                    }
                                } else if (capSq == epSquare) {
                                    moves.add(
                                        Move(
                                            sq, capSq, PieceType.PAWN, us,
                                            captured = PieceType.PAWN, isEnPassant = true
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                PieceType.KNIGHT -> {
                    for (d in KNIGHT_OFFSETS) {
                        val nr = r + d[0]
                        val nf = f + d[1]
                        if (nr in 0..7 && nf in 0..7) {
                            val to = nr * 8 + nf
                            val t = board[to]
                            if (t == null || t.color == them) {
                                moves.add(Move(sq, to, PieceType.KNIGHT, us, captured = t?.type))
                            }
                        }
                    }
                }

                PieceType.BISHOP -> addSlidingMoves(moves, sq, r, f, BISHOP_DIRS, PieceType.BISHOP, us, them)
                PieceType.ROOK -> addSlidingMoves(moves, sq, r, f, ROOK_DIRS, PieceType.ROOK, us, them)
                PieceType.QUEEN -> {
                    addSlidingMoves(moves, sq, r, f, BISHOP_DIRS, PieceType.QUEEN, us, them)
                    addSlidingMoves(moves, sq, r, f, ROOK_DIRS, PieceType.QUEEN, us, them)
                }

                PieceType.KING -> {
                    for (d in KING_OFFSETS) {
                        val kr = r + d[0]
                        val kf = f + d[1]
                        if (kr in 0..7 && kf in 0..7) {
                            val to = kr * 8 + kf
                            val t = board[to]
                            if (t == null || t.color == them) {
                                moves.add(Move(sq, to, PieceType.KING, us, captured = t?.type))
                            }
                        }
                    }
                    // Castling
                    if (us == PieceColor.WHITE && sq == 60) {
                        if ((castlingMask and 1) != 0 &&
                            board[61] == null && board[62] == null &&
                            board[63]?.let { it.color == us && it.type == PieceType.ROOK } == true &&
                            !isSquareAttacked(60, them) &&
                            !isSquareAttacked(61, them) &&
                            !isSquareAttacked(62, them)
                        ) {
                            moves.add(Move(60, 62, PieceType.KING, us, isKingSideCastle = true))
                        }
                        if ((castlingMask and 2) != 0 &&
                            board[59] == null && board[58] == null && board[57] == null &&
                            board[56]?.let { it.color == us && it.type == PieceType.ROOK } == true &&
                            !isSquareAttacked(60, them) &&
                            !isSquareAttacked(59, them) &&
                            !isSquareAttacked(58, them)
                        ) {
                            moves.add(Move(60, 58, PieceType.KING, us, isQueenSideCastle = true))
                        }
                    } else if (us == PieceColor.BLACK && sq == 4) {
                        if ((castlingMask and 4) != 0 &&
                            board[5] == null && board[6] == null &&
                            board[7]?.let { it.color == us && it.type == PieceType.ROOK } == true &&
                            !isSquareAttacked(4, them) &&
                            !isSquareAttacked(5, them) &&
                            !isSquareAttacked(6, them)
                        ) {
                            moves.add(Move(4, 6, PieceType.KING, us, isKingSideCastle = true))
                        }
                        if ((castlingMask and 8) != 0 &&
                            board[3] == null && board[2] == null && board[1] == null &&
                            board[0]?.let { it.color == us && it.type == PieceType.ROOK } == true &&
                            !isSquareAttacked(4, them) &&
                            !isSquareAttacked(3, them) &&
                            !isSquareAttacked(2, them)
                        ) {
                            moves.add(Move(4, 2, PieceType.KING, us, isQueenSideCastle = true))
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun addPromotions(
        moves: MutableList<Move>,
        from: Int,
        to: Int,
        us: PieceColor,
        captured: PieceType?
    ) {
        moves.add(Move(from, to, PieceType.PAWN, us, captured = captured, promotion = PieceType.QUEEN))
        moves.add(Move(from, to, PieceType.PAWN, us, captured = captured, promotion = PieceType.ROOK))
        moves.add(Move(from, to, PieceType.PAWN, us, captured = captured, promotion = PieceType.BISHOP))
        moves.add(Move(from, to, PieceType.PAWN, us, captured = captured, promotion = PieceType.KNIGHT))
    }

    private fun addSlidingMoves(
        moves: MutableList<Move>,
        from: Int,
        r: Int,
        f: Int,
        dirs: Array<IntArray>,
        type: PieceType,
        us: PieceColor,
        them: PieceColor
    ) {
        for (d in dirs) {
            var cr = r + d[0]
            var cf = f + d[1]
            while (cr in 0..7 && cf in 0..7) {
                val to = cr * 8 + cf
                val t = board[to]
                if (t == null) {
                    moves.add(Move(from, to, type, us))
                } else {
                    if (t.color == them) {
                        moves.add(Move(from, to, type, us, captured = t.type))
                    }
                    break
                }
                cr += d[0]
                cf += d[1]
            }
        }
    }

    private fun applyMoveInternal(m: Move): ChessPiece? {
        val moving = board[m.from]
        val captured = if (m.isEnPassant) {
            val capSq = if (m.color == PieceColor.WHITE) m.to + 8 else m.to - 8
            val epPiece = board[capSq]
            board[capSq] = null
            epPiece
        } else {
            board[m.to]
        }

        board[m.from] = null
        board[m.to] = if (m.promotion != null) ChessPiece(m.color, m.promotion) else moving

        if (m.isKingSideCastle) {
            if (m.color == PieceColor.WHITE) {
                board[61] = board[63]
                board[63] = null
            } else {
                board[5] = board[7]
                board[7] = null
            }
        } else if (m.isQueenSideCastle) {
            if (m.color == PieceColor.WHITE) {
                board[59] = board[56]
                board[56] = null
            } else {
                board[3] = board[0]
                board[0] = null
            }
        }
        return captured
    }

    private fun undoMoveInternal(m: Move, captured: ChessPiece?) {
        val movedPiece = if (m.promotion != null) ChessPiece(m.color, PieceType.PAWN) else board[m.to]
        board[m.from] = movedPiece
        board[m.to] = if (m.isEnPassant) null else captured

        if (m.isEnPassant) {
            val capSq = if (m.color == PieceColor.WHITE) m.to + 8 else m.to - 8
            board[capSq] = captured
        }

        if (m.isKingSideCastle) {
            if (m.color == PieceColor.WHITE) {
                board[63] = board[61]
                board[61] = null
            } else {
                board[7] = board[5]
                board[5] = null
            }
        } else if (m.isQueenSideCastle) {
            if (m.color == PieceColor.WHITE) {
                board[56] = board[59]
                board[59] = null
            } else {
                board[0] = board[3]
                board[3] = null
            }
        }
    }

    fun makeMove(m: Move, precomputedSan: String? = null): Boolean {
        val san = precomputedSan ?: toSan(m)
        val prevCastling = castlingMask
        val prevEp = epSquare
        val prevHalf = halfMoveClock
        val prevFull = fullMoveNumber
        val prevTurn = turn

        val cap = applyMoveInternal(m)

        // Update castling rights
        if (m.piece == PieceType.KING) {
            castlingMask = if (m.color == PieceColor.WHITE) castlingMask and 12 else castlingMask and 3
        }
        if (m.from == 63 || m.to == 63) castlingMask = castlingMask and 1.inv()
        if (m.from == 56 || m.to == 56) castlingMask = castlingMask and 2.inv()
        if (m.from == 7 || m.to == 7) castlingMask = castlingMask and 4.inv()
        if (m.from == 0 || m.to == 0) castlingMask = castlingMask and 8.inv()

        // Update en passant target square
        epSquare = if (m.piece == PieceType.PAWN && kotlin.math.abs(m.to - m.from) == 16) {
            (m.from + m.to) / 2
        } else {
            -1
        }

        halfMoveClock = if (m.piece == PieceType.PAWN || m.captured != null) 0 else halfMoveClock + 1
        if (turn == PieceColor.BLACK) fullMoveNumber++
        turn = turn.opposite()

        val key = positionKey()
        positionCounts[key] = (positionCounts[key] ?: 0) + 1
        history.add(UndoState(m, cap, prevCastling, prevEp, prevHalf, prevFull, prevTurn, key, san))
        return true
    }

    fun undo(): Move? {
        if (history.isEmpty()) return null
        val st = history.removeAt(history.lastIndex)
        val count = positionCounts[st.positionKey] ?: 1
        if (count <= 1) positionCounts.remove(st.positionKey) else positionCounts[st.positionKey] = count - 1

        undoMoveInternal(st.move, st.capturedPiece)
        castlingMask = st.castlingMask
        epSquare = st.epSquare
        halfMoveClock = st.halfMoveClock
        fullMoveNumber = st.fullMoveNumber
        turn = st.turn
        return st.move
    }

    fun makeUciMove(uci: String): Move? {
        if (uci.length < 4) return null
        val from = parseSquare(uci.substring(0, 2)) ?: return null
        val to = parseSquare(uci.substring(2, 4)) ?: return null
        val promo = uci.getOrNull(4)?.let { PieceType.fromChar(it) }
        val match = legalMoves(from).find { m ->
            m.to == to && (m.promotion == null || m.promotion == (promo ?: PieceType.QUEEN))
        } ?: return null
        makeMove(match)
        return match
    }

    fun makeSanMove(rawSan: String): Move? {
        val clean = rawSan.trim()
            .replace("0-0-0", "O-O-O")
            .replace("0-0", "O-O")
            .trimEnd('+', '#', '!', '?')
        if (clean.isEmpty()) return null

        val allLegal = legalMoves()

        // Castling
        if (clean == "O-O") {
            val m = allLegal.find { it.isKingSideCastle } ?: return null
            makeMove(m)
            return m
        }
        if (clean == "O-O-O") {
            val m = allLegal.find { it.isQueenSideCastle } ?: return null
            makeMove(m)
            return m
        }

        // Exact SAN match first
        for (m in allLegal) {
            if (toSan(m).trimEnd('+', '#') == clean) {
                makeMove(m)
                return m
            }
        }

        // Sloppy SAN parsing (e.g., e4, exd5, Nf3, Nbd2, R1e1, e8=Q, e8Q)
        var s = clean
        var promo: PieceType? = null
        val eqIdx = s.indexOf('=')
        if (eqIdx != -1 && eqIdx < s.length - 1) {
            promo = PieceType.fromChar(s[eqIdx + 1])
            s = s.substring(0, eqIdx)
        } else if (s.length >= 3 && s.last() in charArrayOf('Q', 'R', 'B', 'N', 'q', 'r', 'b', 'n') &&
            s[s.length - 2].isDigit()
        ) {
            promo = PieceType.fromChar(s.last())
            s = s.dropLast(1)
        }

        val pieceType = if (s.isNotEmpty() && s[0] in charArrayOf('K', 'Q', 'R', 'B', 'N')) {
            val pt = PieceType.fromChar(s[0])!!
            s = s.substring(1)
            pt
        } else {
            PieceType.PAWN
        }

        s = s.replace("x", "")
        if (s.length < 2) return null
        val destStr = s.takeLast(2)
        val destSq = parseSquare(destStr) ?: return null
        val disambig = s.dropLast(2)

        val candidates = allLegal.filter { m ->
            m.piece == pieceType && m.to == destSq &&
                (promo == null || m.promotion == promo) &&
                when (disambig.length) {
                    0 -> true
                    1 -> {
                        val c = disambig[0]
                        if (c in 'a'..'h') m.fromName[0] == c
                        else if (c in '1'..'8') m.fromName[1] == c
                        else true
                    }
                    else -> m.fromName == disambig.take(2)
                }
        }

        val chosen = candidates.firstOrNull() ?: return null
        makeMove(chosen)
        return chosen
    }

    fun toSan(m: Move, allLegalCache: List<Move>? = null): String {
        val base = when {
            m.isKingSideCastle -> "O-O"
            m.isQueenSideCastle -> "O-O-O"
            m.piece == PieceType.PAWN -> {
                val cap = if (m.captured != null) "${m.fromName[0]}x" else ""
                val promo = m.promotion?.let { "=${it.symbol.uppercaseChar()}" } ?: ""
                "$cap${m.toName}$promo"
            }
            else -> {
                val legal = allLegalCache ?: legalMoves()
                val sameDest = legal.filter {
                    it.piece == m.piece && it.to == m.to && it.from != m.from
                }
                val disambig = if (sameDest.isEmpty()) {
                    ""
                } else {
                    val sameFile = sameDest.any { (it.from % 8) == (m.from % 8) }
                    val sameRank = sameDest.any { (it.from / 8) == (m.from / 8) }
                    when {
                        !sameFile -> "${m.fromName[0]}"
                        !sameRank -> "${m.fromName[1]}"
                        else -> m.fromName
                    }
                }
                val cap = if (m.captured != null) "x" else ""
                "${m.piece.symbol.uppercaseChar()}$disambig$cap${m.toName}"
            }
        }

        // Check / Checkmate suffix
        val savedCap = applyMoveInternal(m)
        val opp = turn.opposite()
        val oppKing = kingSquare(opp)
        val givesCheck = oppKing != -1 && isSquareAttacked(oppKing, turn)
        var givesMate = false
        if (givesCheck) {
            val savedTurn = turn
            turn = opp
            givesMate = legalMoves().isEmpty()
            turn = savedTurn
        }
        undoMoveInternal(m, savedCap)

        return when {
            givesMate -> "$base#"
            givesCheck -> "$base+"
            else -> base
        }
    }

    fun inCheckmate(): Boolean = inCheck() && legalMoves().isEmpty()
    fun inStalemate(): Boolean = !inCheck() && legalMoves().isEmpty()
    fun isFiftyMoveRule(): Boolean = halfMoveClock >= 100
    fun inThreefoldRepetition(): Boolean = (positionCounts[positionKey()] ?: 0) >= 3

    fun insufficientMaterial(): Boolean {
        val whitePieces = mutableListOf<Pair<PieceType, Int>>()
        val blackPieces = mutableListOf<Pair<PieceType, Int>>()
        for (i in 0 until 64) {
            val p = board[i] ?: continue
            if (p.type == PieceType.PAWN || p.type == PieceType.ROOK || p.type == PieceType.QUEEN) {
                return false
            }
            if (p.type != PieceType.KING) {
                if (p.color == PieceColor.WHITE) whitePieces.add(p.type to i)
                else blackPieces.add(p.type to i)
            }
        }
        val total = whitePieces.size + blackPieces.size
        if (total <= 1) return true // K vs K, K+N vs K, K+B vs K
        if (whitePieces.size == 1 && blackPieces.size == 1 &&
            whitePieces[0].first == PieceType.BISHOP && blackPieces[0].first == PieceType.BISHOP
        ) {
            val sq1 = whitePieces[0].second
            val sq2 = blackPieces[0].second
            val c1 = (sq1 / 8 + sq1 % 8) % 2
            val c2 = (sq2 / 8 + sq2 % 8) % 2
            if (c1 == c2) return true
        }
        return false
    }

    fun isGameOver(): Boolean =
        legalMoves().isEmpty() || isFiftyMoveRule() || inThreefoldRepetition() || insufficientMaterial()

    fun moveHistoryCount(): Int = history.size
    fun sanHistory(): List<String> = history.map { it.san }
    fun moveHistory(): List<Move> = history.map { it.move }

    fun toPgn(resultOverride: String? = null): String {
        val sb = StringBuilder()
        pgnHeaders.forEach { (k, v) ->
            sb.append("[$k \"$v\"]\n")
        }
        if (pgnHeaders.isNotEmpty()) sb.append('\n')
        for (i in history.indices) {
            if (i % 2 == 0) {
                if (i > 0) sb.append(' ')
                sb.append("${i / 2 + 1}. ")
            } else {
                sb.append(' ')
            }
            sb.append(history[i].san)
        }
        val res = resultOverride ?: pgnHeaders["Result"]
        if (!res.isNullOrBlank()) {
            if (history.isNotEmpty()) sb.append(' ')
            sb.append(res)
        }
        return sb.toString()
    }
}
