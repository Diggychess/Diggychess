package com.example.service

import com.example.chess.ChessBoard
import com.example.chess.OpeningBook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class LichessOpenInvite(
    val id: String,
    val url: String,
    val whiteUrl: String,
    val blackUrl: String,
    val clockLimitSec: Int,
    val clockIncrementSec: Int
)

data class ExplorerMoveStat(
    val uci: String,
    val san: String,
    val white: Int,
    val draws: Int,
    val black: Int,
    val averageRating: Int,
    val isOfflineBook: Boolean = false
) {
    val totalGames: Int get() = (white + draws + black).coerceAtLeast(1)
    val whitePct: Float get() = white.toFloat() / totalGames
    val drawPct: Float get() = draws.toFloat() / totalGames
    val blackPct: Float get() = black.toFloat() / totalGames
}

data class ExplorerPositionResult(
    val openingEco: String?,
    val openingName: String?,
    val whiteWins: Int,
    val draws: Int,
    val blackWins: Int,
    val moves: List<ExplorerMoveStat>,
    val sourceLabel: String
)

object LichessOpenServices {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Calls Lichess's free, zero-auth Open Challenge API (CC0/public service)
     * to generate real shareable White & Black play links usable in any browser
     * with no login required.
     */
    suspend fun createOpenInvite(
        clockLimitSec: Int = 300,
        clockIncrementSec: Int = 3,
        rated: Boolean = false,
        name: String = "Chess Analyzer Friendly Match"
    ): Result<LichessOpenInvite> = withContext(Dispatchers.IO) {
        try {
            val form = FormBody.Builder()
                .add("rated", rated.toString())
                .add("clock.limit", clockLimitSec.toString())
                .add("clock.increment", clockIncrementSec.toString())
                .add("variant", "standard")
                .add("name", name.take(60))
                .build()

            val req = Request.Builder()
                .url("https://lichess.org/api/challenge/open")
                .post(form)
                .header("Accept", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Lichess invite service returned HTTP ${resp.code}.")
                    )
                }
                val json = JSONObject(raw)
                val ch = json.optJSONObject("challenge") ?: json
                val id = ch.optString("id").ifBlank { return@withContext Result.failure(IllegalStateException("Missing challenge id")) }
                val baseUrl = ch.optString("url", "https://lichess.org/$id").ifBlank { "https://lichess.org/$id" }
                val whiteUrl = json.optString("urlWhite").ifBlank { "$baseUrl?color=white" }
                val blackUrl = json.optString("urlBlack").ifBlank { "$baseUrl?color=black" }

                Result.success(
                    LichessOpenInvite(
                        id = id,
                        url = baseUrl,
                        whiteUrl = whiteUrl,
                        blackUrl = blackUrl,
                        clockLimitSec = clockLimitSec,
                        clockIncrementSec = clockIncrementSec
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("Online invite requires internet access (${e.localizedMessage ?: "offline"}). Use Pass & Play or Wi-Fi offline.")
            )
        }
    }

    /**
     * Queries the free Lichess Opening Explorer database (CC0) and automatically
     * falls back to KnightScope's built-in ECO OpeningBook if offline.
     */
    suspend fun fetchExplorerStats(
        fen: String,
        useMasters: Boolean = false
    ): ExplorerPositionResult = withContext(Dispatchers.IO) {
        val localEntry = OpeningBook.lookup(fen)
        try {
            val encodedFen = URLEncoder.encode(fen, "UTF-8")
            val url = if (useMasters) {
                "https://explorer.lichess.ovh/masters?fen=$encodedFen"
            } else {
                "https://explorer.lichess.ovh/lichess?variant=standard&speeds=blitz,rapid,classical&ratings=1600,1800,2000,2200,2500&fen=$encodedFen"
            }
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext buildOfflineFallback(fen, localEntry)
                }
                val raw = resp.body?.string().orEmpty()
                val json = JSONObject(raw)
                val openingObj = json.optJSONObject("opening")
                val eco = openingObj?.optString("eco")?.takeIf { it.isNotBlank() } ?: localEntry?.eco
                val opName = openingObj?.optString("name")?.takeIf { it.isNotBlank() } ?: localEntry?.name

                val movesArr = json.optJSONArray("moves")
                val moves = mutableListOf<ExplorerMoveStat>()
                if (movesArr != null) {
                    for (i in 0 until movesArr.length().coerceAtMost(10)) {
                        val m = movesArr.getJSONObject(i)
                        moves.add(
                            ExplorerMoveStat(
                                uci = m.optString("uci"),
                                san = m.optString("san"),
                                white = m.optInt("white", 0),
                                draws = m.optInt("draws", 0),
                                black = m.optInt("black", 0),
                                averageRating = m.optInt("averageRating", 0),
                                isOfflineBook = false
                            )
                        )
                    }
                }

                ExplorerPositionResult(
                    openingEco = eco,
                    openingName = opName,
                    whiteWins = json.optInt("white", 0),
                    draws = json.optInt("draws", 0),
                    blackWins = json.optInt("black", 0),
                    moves = moves,
                    sourceLabel = if (useMasters) "Lichess Masters DB" else "Lichess Club DB"
                )
            }
        } catch (_: Exception) {
            buildOfflineFallback(fen, localEntry)
        }
    }

    private fun buildOfflineFallback(fen: String, localEntry: com.example.chess.OpeningEntry?): ExplorerPositionResult {
        val board = ChessBoard(fen)
        val curKey = board.openingKey()
        val seenUci = HashSet<String>()
        val bookMoves = mutableListOf<ExplorerMoveStat>()

        // Match indexed continuations from Lichess ECO OpeningBook (supports transpositions)
        for (cont in OpeningBook.lookupContinuations(fen)) {
            if (seenUci.add(cont.uci)) {
                bookMoves.add(
                    ExplorerMoveStat(
                        uci = cont.uci,
                        san = cont.san,
                        white = 40,
                        draws = 25,
                        black = 35,
                        averageRating = 1850,
                        isOfflineBook = true
                    )
                )
            }
            if (bookMoves.size >= 8) break
        }

        if (bookMoves.isEmpty()) {
            // Provide top legal moves cleanly when beyond book
            for (mv in board.legalMoves().take(6)) {
                val copy = board.copy()
                copy.makeMove(mv)
                val san = copy.sanHistory().lastOrNull() ?: mv.uci
                bookMoves.add(
                    ExplorerMoveStat(
                        uci = mv.uci,
                        san = san,
                        white = 35,
                        draws = 30,
                        black = 35,
                        averageRating = 0,
                        isOfflineBook = true
                    )
                )
            }
        }

        return ExplorerPositionResult(
            openingEco = localEntry?.eco,
            openingName = localEntry?.name,
            whiteWins = bookMoves.sumOf { it.white },
            draws = bookMoves.sumOf { it.draws },
            blackWins = bookMoves.sumOf { it.black },
            moves = bookMoves.take(8),
            sourceLabel = "Offline ECO Book"
        )
    }
}
