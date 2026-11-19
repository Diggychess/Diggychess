package com.example.service

import com.example.chess.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object PlatformGameFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8000, TimeUnit.MILLISECONDS)
        .readTimeout(15000, TimeUnit.MILLISECONDS)
        .build()

    private const val CHESS_COM_USER_AGENT = "KnightScopeAndroid/1.0 (contact@knightscope.app)"

    suspend fun fetchUserGamesAndStats(
        platform: ChessPlatform,
        rawUsername: String,
        maxGames: Int = 30
    ): Result<PlatformFetchResult> = withContext(Dispatchers.IO) {
        val username = rawUsername.trim()
        if (username.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Enter a ${platform.label} username."))
        }
        runCatching {
            when (platform) {
                ChessPlatform.CHESS_COM -> fetchChessCom(username, maxGames)
                ChessPlatform.LICHESS -> fetchLichess(username, maxGames)
            }
        }
    }

    private fun fetchChessCom(username: String, maxGames: Int): PlatformFetchResult {
        val lowerUser = username.lowercase()
        val encoded = URLEncoder.encode(lowerUser, "UTF-8")

        // 1. Fetch player profile (for title & display handle)
        val profileReq = Request.Builder()
            .url("https://api.chess.com/pub/player/$encoded")
            .header("User-Agent", CHESS_COM_USER_AGENT)
            .header("Accept", "application/json")
            .build()

        var title: String? = null
        var resolvedName = username
        var statusNote: String? = null
        client.newCall(profileReq).execute().use { resp ->
            if (resp.code == 404) {
                throw IllegalStateException("Player \"$username\" not found on Chess.com.")
            }
            if (resp.code == 429) {
                throw IllegalStateException("Chess.com rate limit reached. Wait a few seconds and retry.")
            }
            if (!resp.isSuccessful) {
                throw IllegalStateException("Chess.com error (${resp.code}). Please try again.")
            }
            val body = resp.body?.string().orEmpty()
            val pJson = JSONObject(body)
            resolvedName = pJson.optString("username", username)
            title = pJson.optString("title").takeIf { it.isNotBlank() }
            statusNote = pJson.optString("status").takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
        }

        // 2. Fetch player rating stats
        val statsList = mutableListOf<PlatformRatingStat>()
        val statsReq = Request.Builder()
            .url("https://api.chess.com/pub/player/$encoded/stats")
            .header("User-Agent", CHESS_COM_USER_AGENT)
            .header("Accept", "application/json")
            .build()

        runCatching {
            client.newCall(statsReq).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val sJson = JSONObject(resp.body?.string().orEmpty())
                val modes = listOf(
                    "chess_bullet" to "Bullet",
                    "chess_blitz" to "Blitz",
                    "chess_rapid" to "Rapid",
                    "chess_daily" to "Daily"
                )
                for ((key, label) in modes) {
                    val obj = sJson.optJSONObject(key) ?: continue
                    val last = obj.optJSONObject("last") ?: continue
                    val rating = last.optInt("rating", 0)
                    if (rating <= 0) continue
                    val best = obj.optJSONObject("best")?.optInt("rating", 0)?.takeIf { it > 0 }
                    val rec = obj.optJSONObject("record")
                    statsList.add(
                        PlatformRatingStat(
                            mode = label,
                            rating = rating,
                            bestRating = best,
                            wins = rec?.optInt("win", 0) ?: 0,
                            losses = rec?.optInt("loss", 0) ?: 0,
                            draws = rec?.optInt("draw", 0) ?: 0
                        )
                    )
                }
            }
        }

        // 3. Fetch monthly game archives (newest first)
        val archivesReq = Request.Builder()
            .url("https://api.chess.com/pub/player/$encoded/games/archives")
            .header("User-Agent", CHESS_COM_USER_AGENT)
            .header("Accept", "application/json")
            .build()

        val archiveUrls = mutableListOf<String>()
        client.newCall(archivesReq).execute().use { resp ->
            if (resp.isSuccessful) {
                val aJson = JSONObject(resp.body?.string().orEmpty())
                val arr = aJson.optJSONArray("archives")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val u = arr.optString(i)
                        if (u.isNotBlank()) archiveUrls.add(u)
                    }
                }
            }
        }

        val games = mutableListOf<ImportedGameSummary>()
        for (monthUrl in archiveUrls.asReversed().take(3)) {
            if (games.size >= maxGames) break
            val mReq = Request.Builder()
                .url(monthUrl)
                .header("User-Agent", CHESS_COM_USER_AGENT)
                .header("Accept", "application/json")
                .build()
            client.newCall(mReq).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val gJson = JSONObject(resp.body?.string().orEmpty())
                val arr = gJson.optJSONArray("games") ?: return@use
                for (i in arr.length() - 1 downTo 0) {
                    if (games.size >= maxGames) break
                    val g = arr.optJSONObject(i) ?: continue
                    if (g.optString("rules", "chess") != "chess") continue
                    val pgn = g.optString("pgn", "").trim()
                    if (pgn.isEmpty()) continue

                    val wObj = g.optJSONObject("white") ?: continue
                    val bObj = g.optJSONObject("black") ?: continue
                    val wName = wObj.optString("username", "White")
                    val bName = bObj.optString("username", "Black")
                    val wRating = wObj.optInt("rating", 0).takeIf { it > 0 }
                    val bRating = bObj.optInt("rating", 0).takeIf { it > 0 }
                    val wRes = wObj.optString("result", "")
                    val bRes = bObj.optString("result", "")

                    val isUserWhite = wName.equals(lowerUser, ignoreCase = true)
                    val userColor = if (isUserWhite) "white" else "black"
                    val (pgnResult, userOutcome) = resolveChessComResult(wRes, bRes, isUserWhite)
                    val timeClass = g.optString("time_class", "blitz").lowercase()
                    val timeControl = g.optString("time_control", "")
                    val endTimeMs = g.optLong("end_time", 0L) * 1000L

                    val eco = extractPgnTag(pgn, "ECO")
                    val ecoUrl = extractPgnTag(pgn, "ECOUrl")
                    val openingName = ecoUrl
                        ?.substringAfterLast("/")
                        ?.replace("-", " ")
                        ?.takeIf { it.isNotBlank() }
                        ?: extractPgnTag(pgn, "Opening")
                        ?: (if (eco != null) "ECO $eco" else "Chess.com Game")

                    val moveCount = countMovesFromPgn(pgn)
                    val id = g.optString("uuid").ifBlank { g.optString("url").ifBlank { "cc_${endTimeMs}_$i" } }

                    games.add(
                        ImportedGameSummary(
                            id = id,
                            platform = ChessPlatform.CHESS_COM,
                            whiteName = wName,
                            blackName = bName,
                            whiteRating = wRating,
                            blackRating = bRating,
                            timeClass = timeClass,
                            timeControl = timeControl,
                            result = pgnResult,
                            userOutcome = userOutcome,
                            userColor = userColor,
                            openingName = openingName,
                            eco = eco,
                            moveCount = moveCount,
                            playedAtMillis = endTimeMs,
                            pgn = pgn
                        )
                    )
                }
            }
        }

        return PlatformFetchResult(
            profile = PlatformPlayerProfile(
                platform = ChessPlatform.CHESS_COM,
                username = resolvedName,
                title = title,
                subtitle = statusNote,
                stats = statsList
            ),
            games = games
        )
    }

    private fun fetchLichess(username: String, maxGames: Int): PlatformFetchResult {
        val encoded = URLEncoder.encode(username, "UTF-8")

        // 1. Fetch user profile + perfs
        val profileReq = Request.Builder()
            .url("https://lichess.org/api/user/$encoded")
            .header("Accept", "application/json")
            .build()

        var resolvedName = username
        var title: String? = null
        var subtitle: String? = null
        val statsList = mutableListOf<PlatformRatingStat>()

        client.newCall(profileReq).execute().use { resp ->
            if (resp.code == 404) {
                throw IllegalStateException("Player \"$username\" not found on Lichess.")
            }
            if (resp.code == 429) {
                throw IllegalStateException("Lichess rate limit reached. Wait a moment and retry.")
            }
            if (!resp.isSuccessful) {
                throw IllegalStateException("Lichess error (${resp.code}). Please try again.")
            }
            val uJson = JSONObject(resp.body?.string().orEmpty())
            resolvedName = uJson.optString("username", username)
            title = uJson.optString("title").takeIf { it.isNotBlank() }
            val countObj = uJson.optJSONObject("count")
            if (countObj != null) {
                val allGames = countObj.optInt("all", 0)
                if (allGames > 0) subtitle = "$allGames total games"
            }
            val perfs = uJson.optJSONObject("perfs")
            if (perfs != null) {
                val modes = listOf(
                    "bullet" to "Bullet",
                    "blitz" to "Blitz",
                    "rapid" to "Rapid",
                    "classical" to "Classical"
                )
                for ((key, label) in modes) {
                    val pObj = perfs.optJSONObject(key) ?: continue
                    val gamesCount = pObj.optInt("games", 0)
                    val rating = pObj.optInt("rating", 0)
                    if (gamesCount <= 0 && rating <= 0) continue
                    statsList.add(
                        PlatformRatingStat(
                            mode = label,
                            rating = rating,
                            bestRating = null,
                            wins = gamesCount,
                            losses = 0,
                            draws = 0
                        )
                    )
                }
            }
        }

        // 2. Fetch recent games via NDJSON stream with embedded PGN + opening metadata
        val gamesReq = Request.Builder()
            .url("https://lichess.org/api/games/user/$encoded?max=${maxGames.coerceIn(5, 50)}&pgnInJson=true&opening=true&moves=true&clocks=false&evals=false")
            .header("Accept", "application/x-ndjson")
            .build()

        val games = mutableListOf<ImportedGameSummary>()
        client.newCall(gamesReq).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Could not load Lichess games (${resp.code}).")
            }
            val source = resp.body?.charStream() ?: return@use
            source.buffered().useLines { lines ->
                lines.forEach { rawLine ->
                    val line = rawLine.trim()
                    if (line.isEmpty()) return@forEach
                    val g = runCatching { JSONObject(line) }.getOrNull() ?: return@forEach
                    if (g.optString("variant", "standard") != "standard") return@forEach
                    val pgn = g.optString("pgn", "").trim()
                    if (pgn.isEmpty()) return@forEach

                    val players = g.optJSONObject("players") ?: return@forEach
                    val wObj = players.optJSONObject("white")
                    val bObj = players.optJSONObject("black")
                    val wName = wObj?.optJSONObject("user")?.optString("name")
                        ?: wObj?.optString("name")
                        ?: "White"
                    val bName = bObj?.optJSONObject("user")?.optString("name")
                        ?: bObj?.optString("name")
                        ?: "Black"
                    val wRating = wObj?.optInt("rating", 0)?.takeIf { it > 0 }
                    val bRating = bObj?.optInt("rating", 0)?.takeIf { it > 0 }

                    val isUserWhite = wName.equals(resolvedName, ignoreCase = true)
                    val userColor = if (isUserWhite) "white" else "black"
                    val winner = g.optString("winner", "")
                    val (pgnResult, userOutcome) = when (winner) {
                        "white" -> "1-0" to if (isUserWhite) "win" else "loss"
                        "black" -> "0-1" to if (!isUserWhite) "win" else "loss"
                        else -> "1/2-1/2" to "draw"
                    }

                    val speed = g.optString("speed", "blitz").lowercase()
                    val clock = g.optJSONObject("clock")
                    val timeControl = if (clock != null) {
                        "${clock.optInt("initial", 0) / 60}+${clock.optInt("increment", 0)}"
                    } else speed

                    val openingObj = g.optJSONObject("opening")
                    val eco = openingObj?.optString("eco")?.takeIf { it.isNotBlank() }
                    val openingName = openingObj?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: "Lichess Game"

                    val movesCount = g.optString("moves", "")
                        .split(' ')
                        .count { it.isNotBlank() }
                        .let { (it + 1) / 2 }

                    val playedAt = g.optLong("lastMoveAt", g.optLong("createdAt", System.currentTimeMillis()))
                    val id = g.optString("id", "li_${playedAt}_${games.size}")

                    games.add(
                        ImportedGameSummary(
                            id = id,
                            platform = ChessPlatform.LICHESS,
                            whiteName = wName,
                            blackName = bName,
                            whiteRating = wRating,
                            blackRating = bRating,
                            timeClass = speed,
                            timeControl = timeControl,
                            result = pgnResult,
                            userOutcome = userOutcome,
                            userColor = userColor,
                            openingName = openingName,
                            eco = eco,
                            moveCount = movesCount.coerceAtLeast(1),
                            playedAtMillis = playedAt,
                            pgn = pgn
                        )
                    )
                }
            }
        }

        return PlatformFetchResult(
            profile = PlatformPlayerProfile(
                platform = ChessPlatform.LICHESS,
                username = resolvedName,
                title = title,
                subtitle = subtitle,
                stats = statsList
            ),
            games = games
        )
    }

    private fun resolveChessComResult(
        whiteResult: String,
        blackResult: String,
        isUserWhite: Boolean
    ): Pair<String, String> {
        val drawSet = setOf("agreed", "repetition", "stalemate", "insufficient", "50move", "timevsinsufficient")
        return when {
            whiteResult == "win" -> "1-0" to if (isUserWhite) "win" else "loss"
            blackResult == "win" -> "0-1" to if (!isUserWhite) "win" else "loss"
            whiteResult in drawSet || blackResult in drawSet -> "1/2-1/2" to "draw"
            else -> "1/2-1/2" to "draw"
        }
    }

    private fun extractPgnTag(pgn: String, tagName: String): String? {
        val regex = Regex("""\[$tagName\s+"([^"]*)"\]""")
        return regex.find(pgn)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun countMovesFromPgn(pgn: String): Int {
        val matches = Regex("""\b\d+\.""").findAll(pgn).count()
        return matches.coerceAtLeast(1)
    }
}
