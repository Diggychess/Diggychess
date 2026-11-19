package com.example.chess

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random

class ChessEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3500, TimeUnit.MILLISECONDS)
        .readTimeout(4500, TimeUnit.MILLISECONDS)
        .build()

    @Volatile
    private var lichessCloudAvailable = true

    @Volatile
    private var chessApiAvailableUntilMs = 0L

    @Volatile
    private var stockfishOnlineAvailableUntilMs = 0L

    suspend fun evaluatePosition(
        fen: String,
        depth: Int,
        timeLimitMs: Long = 1000L,
        allowCloud: Boolean = true
    ): EngineEval = evaluateMultiPv(
        fen = fen,
        depth = depth,
        multiPv = 3,
        timeLimitMs = timeLimitMs,
        allowCloud = allowCloud
    )

    suspend fun evaluateMultiPv(
        fen: String,
        depth: Int,
        multiPv: Int = 3,
        timeLimitMs: Long = 1000L,
        allowCloud: Boolean = true
    ): EngineEval = withContext(Dispatchers.Default) {
        val board = ChessBoard(fen)
        terminalEvalIfOver(board)?.let { return@withContext it }

        val targetVariants = multiPv.coerceIn(1, 3)

        if (allowCloud && lichessCloudAvailable) {
            fetchLichessCloudEval(fen, multiPv = targetVariants)?.let { ev ->
                if (ev.topLines.isNotEmpty()) return@withContext ev
            }
        }

        if (allowCloud) {
            fetchChessApiEval(
                fen = fen,
                depth = depth.coerceIn(4, 12),
                maxThinkingMs = 75,
                variants = targetVariants
            )?.let { ev ->
                if (ev.topLines.size >= 2 || board.legalMoves().size <= 1) {
                    return@withContext ev
                }
                // Enrich single-line API response with secondary on-device alternatives
                val localMulti = searchMultiPvOnDevice(board, depth.coerceIn(2, 5), (timeLimitMs / 2).coerceIn(180L, 450L), targetVariants)
                val merged = mergeEngineLines(ev.topLines, localMulti.topLines, targetVariants)
                return@withContext ev.copy(topLines = merged)
            }
        }

        searchMultiPvOnDevice(board, depth.coerceIn(2, 7), timeLimitMs, targetVariants)
    }

    suspend fun evaluateForLiveBar(fen: String): EngineEval = withContext(Dispatchers.Default) {
        val board = ChessBoard(fen)
        terminalEvalIfOver(board)?.let { return@withContext it }

        fetchChessApiEval(
            fen = fen,
            depth = 8,
            maxThinkingMs = 55,
            variants = 3
        )?.let { return@withContext it }

        fetchStockfishOnlineEval(
            fen = fen,
            boardTurn = board.turn,
            depth = 8
        )?.let { return@withContext it }

        searchMultiPvOnDevice(board, maxDepth = 4, timeLimitMs = 450L, multiPv = 3)
    }

    suspend fun getHint(fen: String): EngineEval = withContext(Dispatchers.Default) {
        val board = ChessBoard(fen)
        terminalEvalIfOver(board)?.let { return@withContext it }

        fetchChessApiEval(
            fen = fen,
            depth = 12,
            maxThinkingMs = 85,
            variants = 3
        )?.let { return@withContext it }

        fetchStockfishOnlineEval(
            fen = fen,
            boardTurn = board.turn,
            depth = 10
        )?.let { return@withContext it }

        searchMultiPvOnDevice(board, maxDepth = 6, timeLimitMs = 1100L, multiPv = 3)
    }

    suspend fun playBotMove(
        fen: String,
        difficulty: EngineDifficulty
    ): EngineEval = withContext(Dispatchers.Default) {
        val board = ChessBoard(fen)
        terminalEvalIfOver(board)?.let { return@withContext it }

        val legalMoves = board.legalMoves()
        if (legalMoves.isEmpty()) {
            return@withContext EngineEval(cp = 0.0, sourceLabel = "On-device Engine")
        }

        if (difficulty == EngineDifficulty.EASY) {
            val apiEval = fetchChessApiEval(
                fen = fen,
                depth = difficulty.apiDepth,
                maxThinkingMs = difficulty.thinkingTimeMs,
                variants = 3
            ) ?: fetchStockfishOnlineEval(
                fen = fen,
                boardTurn = board.turn,
                depth = 3
            )

            if (apiEval != null && apiEval.bestUci != null) {
                val candidates = apiEval.candidateMoves.filter { uci -> legalMoves.any { it.uci == uci } }
                if (candidates.size >= 2 && Random.nextFloat() < 0.40f) {
                    val altUci = candidates[Random.nextInt(1, candidates.size)]
                    return@withContext apiEval.copy(bestUci = altUci)
                }
                if (Random.nextFloat() < 0.25f && legalMoves.size > 1) {
                    val topLocal = topLocalMoves(board, depth = 2, timeLimitMs = 250L, count = 3)
                    val alt = topLocal.drop(1).randomOrNull()
                    if (alt != null) {
                        return@withContext apiEval.copy(bestUci = alt.uci)
                    }
                }
                return@withContext apiEval
            }

            val topLocal = topLocalMoves(board, depth = difficulty.localDepth, timeLimitMs = 350L, count = 3)
            val chosen = if (topLocal.size > 1 && Random.nextFloat() < 0.35f) {
                topLocal[Random.nextInt(1, topLocal.size)]
            } else {
                topLocal.firstOrNull() ?: legalMoves.first()
            }
            val localEval = searchOnDevice(board, difficulty.localDepth, 350L)
            return@withContext localEval.copy(bestUci = chosen.uci)
        }

        val apiEval = fetchChessApiEval(
            fen = fen,
            depth = difficulty.apiDepth,
            maxThinkingMs = difficulty.thinkingTimeMs,
            variants = 1
        )
        if (apiEval != null && !apiEval.bestUci.isNullOrBlank()) {
            return@withContext apiEval
        }

        val onlineEval = fetchStockfishOnlineEval(
            fen = fen,
            boardTurn = board.turn,
            depth = difficulty.apiDepth.coerceAtMost(12)
        )
        if (onlineEval != null && !onlineEval.bestUci.isNullOrBlank()) {
            return@withContext onlineEval
        }

        val fallbackMs = if (difficulty == EngineDifficulty.HARD) 1400L else 700L
        searchOnDevice(board, difficulty.localDepth, fallbackMs)
    }

    private fun terminalEvalIfOver(board: ChessBoard): EngineEval? {
        if (!board.isGameOver()) return null
        return if (board.inCheckmate()) {
            val cp = if (board.turn == PieceColor.WHITE) -99.0 else 99.0
            EngineEval(cp = cp, mate = 0, sourceLabel = "Terminal")
        } else {
            EngineEval(cp = 0.0, mate = null, sourceLabel = "Terminal")
        }
    }

    private fun mergeEngineLines(
        primary: List<EngineVariation>,
        secondary: List<EngineVariation>,
        maxLines: Int
    ): List<EngineVariation> {
        val merged = mutableListOf<EngineVariation>()
        val seenFirstMoves = HashSet<String>()
        for (line in primary + secondary) {
            if (line.firstUci.isNotBlank() && seenFirstMoves.add(line.firstUci)) {
                merged.add(line.copy(rank = merged.size + 1))
                if (merged.size >= maxLines) break
            }
        }
        return merged
    }

    private suspend fun fetchLichessCloudEval(fen: String, multiPv: Int = 3): EngineEval? = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(fen, "UTF-8")
            val req = Request.Builder()
                .url("https://lichess.org/api/cloud-eval?multiPv=${multiPv.coerceIn(1, 3)}&fen=$encoded")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 404) return@withContext null
                if (!resp.isSuccessful) {
                    if (resp.code == 429 || resp.code >= 500) lichessCloudAvailable = false
                    return@withContext null
                }
                val body = resp.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val pvs = json.optJSONArray("pvs") ?: return@withContext null
                if (pvs.length() == 0) return@withContext null
                val depth = json.optInt("depth", 0)

                val lines = mutableListOf<EngineVariation>()
                for (i in 0 until pvs.length().coerceAtMost(multiPv)) {
                    val v = pvs.getJSONObject(i)
                    val movesStr = v.optString("moves", "")
                    val pv = movesStr.split(' ').filter { it.isNotBlank() }
                    val firstUci = pv.firstOrNull() ?: continue
                    val pvSan = uciListToSan(fen, pv)
                    val firstSan = pvSan.firstOrNull() ?: firstUci
                    val mate = if (!v.isNull("mate")) v.optInt("mate") else null
                    val cp = if (mate != null) {
                        if (mate > 0) 99.0 else -99.0
                    } else {
                        (v.optInt("cp", 0) / 100.0).coerceIn(-99.0, 99.0)
                    }
                    lines.add(
                        EngineVariation(
                            rank = lines.size + 1,
                            cp = cp,
                            mate = mate,
                            firstUci = firstUci,
                            firstSan = firstSan,
                            pvUci = pv,
                            pvSan = pvSan,
                            depth = depth
                        )
                    )
                }

                val top = lines.firstOrNull() ?: return@withContext null
                EngineEval(
                    cp = top.cp,
                    mate = top.mate,
                    pvUci = top.pvUci,
                    depth = depth,
                    bestUci = top.firstUci,
                    sourceLabel = "Lichess Cloud",
                    candidateMoves = lines.map { it.firstUci },
                    topLines = lines
                )
            }
        } catch (_: Exception) {
            lichessCloudAvailable = false
            null
        }
    }

    private suspend fun fetchChessApiEval(
        fen: String,
        depth: Int,
        maxThinkingMs: Int,
        variants: Int
    ): EngineEval? = withContext(Dispatchers.IO) {
        if (System.currentTimeMillis() < chessApiAvailableUntilMs) return@withContext null
        try {
            val payload = JSONObject().apply {
                put("fen", fen)
                put("depth", depth.coerceIn(1, 18))
                put("maxThinkingTime", maxThinkingMs.coerceIn(10, 100))
                put("variants", variants.coerceIn(1, 5))
            }
            val req = Request.Builder()
                .url("https://chess-api.com/v1")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    if (resp.code == 429 || resp.code >= 500) {
                        chessApiAvailableUntilMs = System.currentTimeMillis() + 30_000L
                    }
                    return@withContext null
                }
                val body = resp.body?.string()?.trim() ?: return@withContext null
                val json = JSONObject(body)
                if (json.optString("type") == "error") return@withContext null

                val moveUci = json.optString("move").ifBlank { json.optString("lan") }
                    .takeIf { it.length >= 4 }
                val contArr = json.optJSONArray("continuationArr")
                val pv = buildList {
                    if (contArr != null) {
                        for (i in 0 until contArr.length()) {
                            val uci = contArr.optString(i)
                            if (uci.isNotBlank()) add(uci)
                        }
                    }
                    if (isEmpty() && moveUci != null) add(moveUci)
                }
                val bestUci = moveUci ?: pv.firstOrNull()
                if (bestUci.isNullOrBlank()) return@withContext null

                val respDepth = json.optInt("depth", depth)
                val mateVal = if (!json.isNull("mate")) json.optInt("mate") else null
                val cpPawns = if (mateVal != null) {
                    if (mateVal > 0) 99.0 else -99.0
                } else {
                    json.optDouble("eval", 0.0).coerceIn(-99.0, 99.0)
                }

                val primarySan = uciListToSan(fen, pv)
                val lines = mutableListOf(
                    EngineVariation(
                        rank = 1,
                        cp = cpPawns,
                        mate = mateVal,
                        firstUci = bestUci,
                        firstSan = primarySan.firstOrNull() ?: bestUci,
                        pvUci = pv,
                        pvSan = primarySan,
                        depth = respDepth
                    )
                )

                val candidates = mutableListOf(bestUci)
                val variantsArr = json.optJSONArray("variants")
                if (variantsArr != null) {
                    for (i in 0 until variantsArr.length()) {
                        val item = variantsArr.optJSONObject(i) ?: continue
                        val vm = item.optString("move").ifBlank { item.optString("lan") }
                            .takeIf { it.length >= 4 } ?: continue
                        if (vm !in candidates) {
                            candidates.add(vm)
                        }
                        if (lines.none { it.firstUci == vm } && lines.size < variants.coerceIn(1, 3)) {
                            val vContArr = item.optJSONArray("continuationArr")
                            val vPv = buildList {
                                if (vContArr != null) {
                                    for (j in 0 until vContArr.length()) {
                                        val u = vContArr.optString(j)
                                        if (u.isNotBlank()) add(u)
                                    }
                                }
                                if (isEmpty()) add(vm)
                            }
                            val vSan = uciListToSan(fen, vPv)
                            val vMate = if (!item.isNull("mate")) item.optInt("mate") else null
                            val vCp = if (vMate != null) {
                                if (vMate > 0) 99.0 else -99.0
                            } else {
                                item.optDouble("eval", cpPawns).coerceIn(-99.0, 99.0)
                            }
                            lines.add(
                                EngineVariation(
                                    rank = lines.size + 1,
                                    cp = vCp,
                                    mate = vMate,
                                    firstUci = vm,
                                    firstSan = vSan.firstOrNull() ?: vm,
                                    pvUci = vPv,
                                    pvSan = vSan,
                                    depth = item.optInt("depth", respDepth)
                                )
                            )
                        }
                    }
                }

                EngineEval(
                    cp = cpPawns,
                    mate = mateVal,
                    pvUci = pv,
                    depth = respDepth,
                    bestUci = bestUci,
                    sourceLabel = "Stockfish API",
                    candidateMoves = candidates,
                    topLines = lines
                )
            }
        } catch (_: Exception) {
            chessApiAvailableUntilMs = System.currentTimeMillis() + 15_000L
            null
        }
    }

    private suspend fun fetchStockfishOnlineEval(
        fen: String,
        boardTurn: PieceColor,
        depth: Int
    ): EngineEval? = withContext(Dispatchers.IO) {
        if (System.currentTimeMillis() < stockfishOnlineAvailableUntilMs) return@withContext null
        try {
            val encoded = URLEncoder.encode(fen, "UTF-8")
            val d = depth.coerceIn(1, 15)
            val req = Request.Builder()
                .url("https://stockfish.online/api/s/v2.php?fen=$encoded&depth=$d")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    if (resp.code == 429 || resp.code >= 500) {
                        stockfishOnlineAvailableUntilMs = System.currentTimeMillis() + 30_000L
                    }
                    return@withContext null
                }
                val body = resp.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (!json.optBoolean("success", false)) return@withContext null

                val bestRaw = json.optString("bestmove", "")
                val bestUci = bestRaw.split(' ')
                    .firstOrNull { it.length in 4..5 && it != "bestmove" && it != "ponder" }
                val contRaw = json.optString("continuation", "")
                val pv = contRaw.split(' ').filter { it.isNotBlank() }
                val resolvedBest = bestUci ?: pv.firstOrNull() ?: return@withContext null
                val fullPv = pv.ifEmpty { listOf(resolvedBest) }
                val pvSan = uciListToSan(fen, fullPv)

                val sign = if (boardTurn == PieceColor.WHITE) 1 else -1
                val mateVal = if (!json.isNull("mate")) json.optInt("mate") * sign else null
                val cpPawns = if (mateVal != null) {
                    if (mateVal > 0) 99.0 else -99.0
                } else {
                    (json.optDouble("evaluation", 0.0) * sign).coerceIn(-99.0, 99.0)
                }

                val line = EngineVariation(
                    rank = 1,
                    cp = cpPawns,
                    mate = mateVal,
                    firstUci = resolvedBest,
                    firstSan = pvSan.firstOrNull() ?: resolvedBest,
                    pvUci = fullPv,
                    pvSan = pvSan,
                    depth = d
                )

                EngineEval(
                    cp = cpPawns,
                    mate = mateVal,
                    pvUci = fullPv,
                    depth = d,
                    bestUci = resolvedBest,
                    sourceLabel = "Stockfish Online",
                    topLines = listOf(line)
                )
            }
        } catch (_: Exception) {
            stockfishOnlineAvailableUntilMs = System.currentTimeMillis() + 15_000L
            null
        }
    }

    // ===== On-Device Negamax Alpha-Beta Engine (per-search isolated state for thread safety) =====

    private data class TTEntry(val depth: Int, val score: Int, val flag: Int, val bestUci: String?)

    private fun searchOnDevice(board: ChessBoard, maxDepth: Int, timeLimitMs: Long): EngineEval {
        return OnDeviceSearchSession().search(board.copy(), maxDepth, timeLimitMs)
    }

    private fun searchMultiPvOnDevice(
        board: ChessBoard,
        maxDepth: Int,
        timeLimitMs: Long,
        multiPv: Int
    ): EngineEval {
        return OnDeviceSearchSession().searchMultiPv(board.copy(), maxDepth, timeLimitMs, multiPv)
    }

    private fun topLocalMoves(board: ChessBoard, depth: Int, timeLimitMs: Long, count: Int): List<Move> {
        return OnDeviceSearchSession().topMoves(board.copy(), depth, timeLimitMs, count)
    }

    private inner class OnDeviceSearchSession {
        private val tt = HashMap<String, TTEntry>(8192)
        private var deadlineMs = 0L
        private var aborted = false
        private val killers = Array(32) { arrayOfNulls<String>(2) }

        fun topMoves(board: ChessBoard, depth: Int, timeLimitMs: Long, count: Int): List<Move> {
            deadlineMs = System.currentTimeMillis() + timeLimitMs
            aborted = false
            val legal = orderMoves(board, board.legalMoves(), 0, null)
            if (legal.size <= 1) return legal
            val scored = mutableListOf<Pair<Move, Int>>()
            for (m in legal) {
                board.makeMove(m)
                val sc = -negamax(board, (depth - 1).coerceAtLeast(0), -MATE_SCORE - 100, MATE_SCORE + 100, 1)
                board.undo()
                if (aborted) break
                scored.add(m to sc)
            }
            if (scored.isEmpty()) return legal.take(count)
            return scored.sortedByDescending { it.second }.map { it.first }.take(count)
        }

        fun searchMultiPv(
            board: ChessBoard,
            maxDepth: Int,
            timeLimitMs: Long,
            multiPv: Int
        ): EngineEval {
            val rootMoves = orderMoves(board, board.legalMoves(), 0, null)
            if (rootMoves.isEmpty()) {
                return if (board.inCheck()) {
                    val cp = if (board.turn == PieceColor.WHITE) -99.0 else 99.0
                    EngineEval(cp, 0, sourceLabel = "On-device Engine")
                } else {
                    EngineEval(0.0, null, sourceLabel = "On-device Engine")
                }
            }

            deadlineMs = System.currentTimeMillis() + timeLimitMs
            aborted = false

            val scoresByUci = linkedMapOf<String, Int>()
            val moveByUci = rootMoves.associateBy { it.uci }
            var reachedDepth = 1

            for (d in 1..maxDepth) {
                aborted = false
                val iterScores = mutableListOf<Pair<Move, Int>>()
                val ordered = if (scoresByUci.isNotEmpty()) {
                    rootMoves.sortedByDescending { scoresByUci[it.uci] ?: -INF }
                } else rootMoves

                for (m in ordered) {
                    board.makeMove(m)
                    val sc = -negamax(board, d - 1, -MATE_SCORE - 100, MATE_SCORE + 100, 1)
                    board.undo()
                    if (aborted) break
                    iterScores.add(m to sc)
                }

                if (!aborted && iterScores.isNotEmpty()) {
                    reachedDepth = d
                    for ((m, sc) in iterScores) {
                        scoresByUci[m.uci] = sc
                    }
                    val topScore = iterScores.maxOf { it.second }
                    if (abs(topScore) > MATE_SCORE - 100) break
                } else {
                    // Merge partial scores on timeout if none recorded yet
                    if (scoresByUci.isEmpty()) {
                        for ((m, sc) in iterScores) scoresByUci[m.uci] = sc
                    }
                    break
                }
            }

            val ranked = scoresByUci.entries
                .sortedByDescending { it.value }
                .take(multiPv.coerceIn(1, rootMoves.size))

            val fen = board.toFen()
            val whiteSign = if (board.turn == PieceColor.WHITE) 1 else -1

            val lines = if (ranked.isEmpty()) {
                val first = rootMoves.first()
                val san = board.toSan(first)
                listOf(
                    EngineVariation(
                        rank = 1,
                        cp = 0.0,
                        mate = null,
                        firstUci = first.uci,
                        firstSan = san,
                        pvUci = listOf(first.uci),
                        pvSan = listOf(san),
                        depth = reachedDepth
                    )
                )
            } else {
                ranked.mapIndexed { idx, (uci, rawScore) ->
                    val moveObj = moveByUci[uci]!!
                    val whiteScore = rawScore * whiteSign
                    val mateMoves = if (abs(whiteScore) > MATE_SCORE - 200) {
                        val pliesToMate = MATE_SCORE - abs(whiteScore)
                        val moves = (pliesToMate + 1) / 2
                        if (whiteScore > 0) moves else -moves
                    } else null
                    val cpPawns = if (mateMoves != null) {
                        if (mateMoves > 0) 99.0 else -99.0
                    } else {
                        (whiteScore / 100.0).coerceIn(-99.0, 99.0)
                    }
                    val pvUci = extractPv(board, moveObj, maxLen = 5)
                    val pvSan = uciListToSan(fen, pvUci)
                    EngineVariation(
                        rank = idx + 1,
                        cp = cpPawns,
                        mate = mateMoves,
                        firstUci = uci,
                        firstSan = pvSan.firstOrNull() ?: board.toSan(moveObj),
                        pvUci = pvUci,
                        pvSan = pvSan,
                        depth = reachedDepth
                    )
                }
            }

            val top = lines.first()
            return EngineEval(
                cp = top.cp,
                mate = top.mate,
                pvUci = top.pvUci,
                depth = reachedDepth,
                bestUci = top.firstUci,
                sourceLabel = "On-device Engine",
                candidateMoves = lines.map { it.firstUci },
                topLines = lines
            )
        }

        fun search(board: ChessBoard, maxDepth: Int, timeLimitMs: Long): EngineEval {
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
                    EngineEval(cp, 0, sourceLabel = "On-device Engine")
                } else {
                    EngineEval(0.0, null, sourceLabel = "On-device Engine")
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

            val pvSan = uciListToSan(board.toFen(), bestPv)
            val line = bestMove?.let {
                EngineVariation(
                    rank = 1,
                    cp = cpPawns,
                    mate = mateInMoves,
                    firstUci = it.uci,
                    firstSan = pvSan.firstOrNull() ?: board.toSan(it),
                    pvUci = bestPv,
                    pvSan = pvSan,
                    depth = reachedDepth
                )
            }

            return EngineEval(
                cp = cpPawns,
                mate = mateInMoves,
                pvUci = bestPv,
                depth = reachedDepth,
                bestUci = bestMove?.uci,
                sourceLabel = "On-device Engine",
                topLines = listOfNotNull(line)
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
            var flag = 2

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
                    flag = 0
                }
                if (alpha >= beta) {
                    if (m.captured == null && ply < killers.size) {
                        if (killers[ply][0] != m.uci) {
                            killers[ply][1] = killers[ply][0]
                            killers[ply][0] = m.uci
                        }
                    }
                    flag = 1
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
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val INF = 1_000_000
        private const val MATE_SCORE = 50_000

        fun uciListToSan(fen: String, pvUci: List<String>, maxMoves: Int = 6): List<String> {
            val copy = ChessBoard(fen)
            val out = mutableListOf<String>()
            for (u in pvUci.take(maxMoves)) {
                val m = copy.makeUciMove(u) ?: break
                out.add(copy.sanHistory().lastOrNull() ?: m.uci)
            }
            return out
        }

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
