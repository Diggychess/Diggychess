package com.example.chess

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class ChessEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3500, TimeUnit.MILLISECONDS)
        .readTimeout(4000, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var cloudAvailable = true

    suspend fun evaluatePosition(
        fen: String,
        depth: Int,
        timeLimitMs: Long = 1000L,
        allowCloud: Boolean = true
    ): EngineEval = withContext(Dispatchers.Default) {
        val board = ChessBoard(fen)
        if (board.isGameOver()) {
            return@withContext if (board.inCheckmate()) {
                val cp = if (board.turn == PieceColor.WHITE) -99.0 else 99.0
                EngineEval(cp = cp, mate = 0)
            } else {
                EngineEval(cp = 0.0, mate = null)
            }
        }

        if (allowCloud && cloudAvailable) {
            val cloud = fetchLichessCloudEval(fen)
            if (cloud != null) return@withContext cloud
        }

        searchOnDevice(board, depth.coerceIn(2, 8), timeLimitMs)
    }

    private suspend fun fetchLichessCloudEval(fen: String): EngineEval? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(fen, "UTF-8")
            val req = Request.Builder()
                .url("https://lichess.org/api/cloud-eval?multiPv=1&fen=$encoded")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 404) return@withContext null
                if (!resp.isSuccessful) {
                    if (resp.code == 429 || resp.code >= 500) cloudAvailable = false
                    return@withContext null
                }
                val body = resp.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val pvs = json.optJSONArray("pvs") ?: return@withContext null
                if (pvs.length() == 0) return@withContext null
                val v = pvs.getJSONObject(0)
                val movesStr = v.optString("moves", "")
                val pv = movesStr.split(' ').filter { it.isNotBlank() }
                val depth = json.optInt("depth", 0)
                if (!v.isNull("mate")) {
                    val mate = v.optInt("mate")
                    val cp = if (mate > 0) 99.0 else -99.0
                    EngineEval(cp = cp, mate = mate, pvUci = pv, depth = depth, bestUci = pv.firstOrNull())
                } else {
                    val cpRaw = v.optInt("cp", 0) / 100.0
                    EngineEval(
                        cp = cpRaw.coerceIn(-99.0, 99.0),
                        mate = null,
                        pvUci = pv,
                        depth = depth,
                        bestUci = pv.firstOrNull()
                    )
                }
            }
        } catch (_: Exception) {
            cloudAvailable = false
            null
        }
    }

    // ===== On-Device Negamax Alpha-Beta Engine with Quiescence & Transposition Table =====

    private val tt = HashMap<String, TTEntry>(16384)
    private var deadlineMs = 0L
    private var aborted = false
    private val killers = Array(32) { arrayOfNulls<String>(2) }

    private data class TTEntry(val depth: Int, val score: Int, val flag: Int, val bestUci: String?)

    private fun searchOnDevice(board: ChessBoard, maxDepth: Int, timeLimitMs: Long): EngineEval {
        tt.clear()
        for (k in killers) { k[0] = null; k[1] = null }
        deadlineMs = System.currentTimeMillis() + timeLimitMs
        aborted = false

        var bestMove: Move? = null
        var bestScore = 0
        var reachedDepth = 0
        var bestPv = emptyList<String>()

        val rootMoves = orderMoves(board, board.legalMoves(), 0, null)
        if (rootMoves.isEmpty()) {
            return if (board.inCheck()) {
                val cp = if (board.turn == PieceColor.WHITE) -99.0 else 99.0
                EngineEval(cp, 0)
            } else {
                EngineEval(0.0, null)
            }
        }
        bestMove = rootMoves.first()

        for (d in 1..maxDepth) {
            aborted = false
            var alpha = -MATE_SCORE - 100
            val beta = MATE_SCORE + 100
            var curBestMove = bestMove
            var curBestScore = -INF

            val orderedRoot = if (bestMove != null) {
                val bUci = bestMove.uci
                listOf(bestMove) + rootMoves.filter { it.uci != bUci }
            } else rootMoves

            for (m in orderedRoot) {
                board.makeMove(m)
                val score = -negamax(board, d - 1, -beta, -alpha, 1)
                board.undo()
                if (aborted) break
                if (score > curBestScore) {
                    curBestScore = score
                    curBestMove = m
                }
                if (score > alpha) alpha = score
            }

            if (!aborted && curBestMove != null) {
                bestMove = curBestMove
                bestScore = curBestScore
                reachedDepth = d
                bestPv = extractPv(board, bestMove, d)
                if (abs(bestScore) > MATE_SCORE - 100) break
            } else {
                break
            }
        }

        val whiteSign = if (board.turn == PieceColor.WHITE) 1 else -1
        val whiteScore = bestScore * whiteSign
        val mateInMoves = if (abs(whiteScore) > MATE_SCORE - 200) {
            val pliesToMate = MATE_SCORE - abs(whiteScore)
            val moves = (pliesToMate + 1) / 2
            if (whiteScore > 0) moves else -moves
        } else null

        val cpPawns = if (mateInMoves != null) {
            if (mateInMoves > 0) 99.0 else -99.0
        } else {
            (whiteScore / 100.0).coerceIn(-99.0, 99.0)
        }

        return EngineEval(
            cp = cpPawns,
            mate = mateInMoves,
            pvUci = bestPv,
            depth = reachedDepth,
            bestUci = bestMove?.uci
        )
    }

    private fun extractPv(board: ChessBoard, firstMove: Move, maxLen: Int): List<String> {
        val pv = mutableListOf<String>()
        val copy = board.copy()
        var m: Move? = firstMove
        var steps = 0
        while (m != null && steps < maxLen) {
            pv.add(m.uci)
            copy.makeMove(m)
            steps++
            val entry = tt[copy.positionKey()] ?: break
            val nextUci = entry.bestUci ?: break
            m = copy.legalMoves().find { it.uci == nextUci }
        }
        return pv
    }

    private fun negamax(board: ChessBoard, depth: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        if (System.currentTimeMillis() > deadlineMs) {
            aborted = true
            return 0
        }

        if (board.isFiftyMoveRule() || board.inThreefoldRepetition() || board.insufficientMaterial()) {
            return 0
        }

        val key = board.positionKey()
        val ttEntry = tt[key]
        if (ttEntry != null && ttEntry.depth >= depth) {
            when (ttEntry.flag) {
                0 -> return ttEntry.score
                1 -> if (ttEntry.score >= beta) return ttEntry.score
                2 -> if (ttEntry.score <= alphaIn) return ttEntry.score
            }
        }

        if (depth <= 0) {
            return quiescence(board, alphaIn, beta, 0)
        }

        val moves = board.legalMoves()
        if (moves.isEmpty()) {
            return if (board.inCheck()) -MATE_SCORE + ply else 0
        }

        val ordered = orderMoves(board, moves, ply, ttEntry?.bestUci)
        var alpha = alphaIn
        var best = -INF
        var bestMoveUci: String? = null
        var flag = 2 // upper bound

        for (m in ordered) {
            board.makeMove(m)
            val score = -negamax(board, depth - 1, -beta, -alpha, ply + 1)
            board.undo()
            if (aborted) return 0

            if (score > best) {
                best = score
                bestMoveUci = m.uci
            }
            if (score > alpha) {
                alpha = score
                flag = 0 // exact
            }
            if (alpha >= beta) {
                if (m.captured == null && ply < killers.size) {
                    if (killers[ply][0] != m.uci) {
                        killers[ply][1] = killers[ply][0]
                        killers[ply][0] = m.uci
                    }
                }
                flag = 1 // lower bound
                break
            }
        }

        if (tt.size < 40000) {
            tt[key] = TTEntry(depth, best, flag, bestMoveUci)
        }
        return best
    }

    private fun quiescence(board: ChessBoard, alphaIn: Int, beta: Int, qPly: Int): Int {
        if (System.currentTimeMillis() > deadlineMs) {
            aborted = true
            return 0
        }
        val standPat = staticEval(board)
        if (standPat >= beta) return beta
        var alpha = maxOf(alphaIn, standPat)
        if (qPly >= 6) return alpha

        val captures = board.legalMoves().filter { it.captured != null || it.promotion != null }
            .sortedByDescending { mvvLvaScore(it) }

        for (m in captures) {
            board.makeMove(m)
            val score = -quiescence(board, -beta, -alpha, qPly + 1)
            board.undo()
            if (aborted) return 0
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return alpha
    }

    private fun orderMoves(board: ChessBoard, moves: List<Move>, ply: Int, ttBestUci: String?): List<Move> {
        val k0 = if (ply < killers.size) killers[ply][0] else null
        val k1 = if (ply < killers.size) killers[ply][1] else null
        return moves.sortedByDescending { m ->
            val uci = m.uci
            when {
                uci == ttBestUci -> 1_000_000
                m.captured != null -> 100_000 + mvvLvaScore(m)
                m.promotion != null -> 90_000 + pieceValCp(m.promotion)
                uci == k0 -> 50_000
                uci == k1 -> 40_000
                m.isKingSideCastle || m.isQueenSideCastle -> 15_000
                else -> pstDelta(m)
            }
        }
    }

    private fun mvvLvaScore(m: Move): Int {
        val victim = m.captured?.let { pieceValCp(it) } ?: 0
        val attacker = pieceValCp(m.piece)
        return victim * 10 - attacker
    }

    private fun pieceValCp(type: PieceType): Int = when (type) {
        PieceType.PAWN -> 100
        PieceType.KNIGHT -> 320
        PieceType.BISHOP -> 330
        PieceType.ROOK -> 500
        PieceType.QUEEN -> 900
        PieceType.KING -> 20000
    }

    private fun pstDelta(m: Move): Int {
        return pstValue(m.piece, m.color, m.to, false) - pstValue(m.piece, m.color, m.from, false)
    }

    private fun staticEval(board: ChessBoard): Int {
        var score = 0
        var nonPawnMat = 0
        for (sq in 0 until 64) {
            val p = board.board[sq] ?: continue
            if (p.type != PieceType.PAWN && p.type != PieceType.KING) {
                nonPawnMat += pieceValCp(p.type)
            }
        }
        val isEndgame = nonPawnMat <= 2400

        for (sq in 0 until 64) {
            val p = board.board[sq] ?: continue
            val valCp = pieceValCp(p.type) + pstValue(p.type, p.color, sq, isEndgame)
            if (p.color == PieceColor.WHITE) score += valCp else score -= valCp
        }
        return if (board.turn == PieceColor.WHITE) score else -score
    }

    private fun pstValue(type: PieceType, color: PieceColor, sq: Int, endgame: Boolean): Int {
        val r = if (color == PieceColor.WHITE) sq / 8 else 7 - (sq / 8)
        val f = sq % 8
        val idx = r * 8 + f
        return when (type) {
            PieceType.PAWN -> PAWN_PST[idx]
            PieceType.KNIGHT -> KNIGHT_PST[idx]
            PieceType.BISHOP -> BISHOP_PST[idx]
            PieceType.ROOK -> ROOK_PST[idx]
            PieceType.QUEEN -> QUEEN_PST[idx]
            PieceType.KING -> if (endgame) KING_END_PST[idx] else KING_MID_PST[idx]
        }
    }

    companion object {
        private const val INF = 1_000_000
        private const val MATE_SCORE = 50_000

        private val PAWN_PST = intArrayOf(
            0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5,  5, 10, 25, 25, 10,  5,  5,
            0,  0,  0, 20, 20,  0,  0,  0,
            5, -5,-10,  0,  0,-10, -5,  5,
            5, 10, 10,-20,-20, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0
        )
        private val KNIGHT_PST = intArrayOf(
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-30,-30,-30,-30,-40,-50
        )
        private val BISHOP_PST = intArrayOf(
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-10,-10,-10,-10,-10,-20
        )
        private val ROOK_PST = intArrayOf(
            0,  0,  0,  0,  0,  0,  0,  0,
            5, 10, 10, 10, 10, 10, 10,  5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            -5,  0,  0,  0,  0,  0,  0, -5,
            0,  0,  0,  5,  5,  0,  0,  0
        )
        private val QUEEN_PST = intArrayOf(
            -20,-10,-10, -5, -5,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5,  5,  5,  5,  0,-10,
            -5,   0,  5,  5,  5,  5,  0, -5,
            0,    0,  5,  5,  5,  5,  0, -5,
            -10,  5,  5,  5,  5,  5,  0,-10,
            -10,  0,  5,  0,  0,  0,  0,-10,
            -20,-10,-10, -5, -5,-10,-10,-20
        )
        private val KING_MID_PST = intArrayOf(
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -30,-40,-40,-50,-50,-40,-40,-30,
            -20,-30,-30,-40,-40,-30,-30,-20,
            -10,-20,-20,-20,-20,-20,-20,-10,
            20,  20,  0,  0,  0,  0, 20, 20,
            20,  30, 10,  0,  0, 10, 30, 20
        )
        private val KING_END_PST = intArrayOf(
            -50,-40,-30,-20,-20,-30,-40,-50,
            -30,-20,-10,  0,  0,-10,-20,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 30, 40, 40, 30,-10,-30,
            -30,-10, 20, 30, 30, 20,-10,-30,
            -30,-30,  0,  0,  0,  0,-30,-30,
            -50,-30,-30,-30,-30,-30,-30,-50
        )
    }
}
