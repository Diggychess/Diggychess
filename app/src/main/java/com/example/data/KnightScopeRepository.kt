package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chess.*
import com.example.ui.theme.BoardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.knightScopeDataStore: DataStore<Preferences> by preferencesDataStore(name = "knightscope_prefs")

data class UserProfile(
    val id: String = "local",
    val userId: String = "local",
    val displayName: String = "KnightScope Player",
    val avatarUrl: String? = null,
    val preferredDifficulty: String = "medium",
    val gamesPlayedBot: Int = 0,
    val gamesAnalysed: Int = 0,
    val puzzlesSolved: Int = 0,
    val winStreak: Int = 0
)

class KnightScopeRepository(private val context: Context) {

    companion object {
        private val KEY_HISTORY = stringPreferencesKey("ks_hist")
        private val KEY_STATS = stringPreferencesKey("ks_st")
        private val KEY_RATING = stringPreferencesKey("ks_rate")
        private val KEY_IMPORTED_PUZZLES = stringPreferencesKey("ks_pz")
        private val KEY_SETTINGS = stringPreferencesKey("ks_set")
        private val KEY_BOARD_THEME = stringPreferencesKey("ks_bt")
        private val KEY_DAILY_GOAL = stringPreferencesKey("ks_goal")
        private val KEY_FRIEND_NAME = stringPreferencesKey("ks_name")
        private val KEY_REPORT_SIDE = stringPreferencesKey("ks_side")
        private val KEY_PLAY_DIFFICULTY = stringPreferencesKey("ks_play_diff")
        private val KEY_PLAY_SHOW_EVAL = booleanPreferencesKey("ks_play_eval")
        private val KEY_USER_PROFILE = stringPreferencesKey("ks_profile")
        private val KEY_CONTACT_EMAIL = stringPreferencesKey("ks_contact_email")

        val STARTER_PUZZLES = listOf(
            Puzzle("ks-01", "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1", null, "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1", listOf("e1e8"), 0, listOf("Back-rank mate", "mate"), true, "KnightScope"),
            Puzzle("ks-02", "4r1k1/5ppp/8/8/8/8/5PPP/6K1 b - - 0 1", null, "4r1k1/5ppp/8/8/8/8/5PPP/6K1 b - - 0 1", listOf("e8e1"), 0, listOf("Back-rank mate", "mate"), true, "KnightScope"),
            Puzzle("ks-03", "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4", null, "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4", listOf("h5f7"), 0, listOf("Scholar’s mate", "mate"), true, "KnightScope"),
            Puzzle("ks-04", "7k/8/6K1/8/8/8/8/R7 w - - 0 1", null, "7k/8/6K1/8/8/8/8/R7 w - - 0 1", listOf("a1a8"), 0, listOf("Rook mate", "mate"), true, "KnightScope"),
            Puzzle("ks-05", "7k/Q7/5K2/8/8/8/8/8 w - - 0 1", null, "7k/Q7/5K2/8/8/8/8/8 w - - 0 1", listOf("a7g7"), 0, listOf("Queen mate", "mate"), true, "KnightScope"),
            Puzzle("ks-06", "8/8/8/8/8/5k2/q7/7K b - - 0 1", null, "8/8/8/8/8/5k2/q7/7K b - - 0 1", listOf("a2g2"), 0, listOf("Queen mate", "mate"), true, "KnightScope"),
            Puzzle("ks-07", "6k1/R7/8/8/8/8/8/1R4K1 w - - 0 1", null, "6k1/R7/8/8/8/8/8/1R4K1 w - - 0 1", listOf("b1b8"), 0, listOf("Two-rook mate", "mate"), true, "KnightScope"),
            Puzzle("ks-08", "1r4k1/8/8/8/8/8/r7/6K1 b - - 0 1", null, "1r4k1/8/8/8/8/8/r7/6K1 b - - 0 1", listOf("b8b1"), 0, listOf("Two-rook mate", "mate"), true, "KnightScope"),
            Puzzle("ks-09", "6rk/6pp/8/4N3/8/8/8/6K1 w - - 0 1", null, "6rk/6pp/8/4N3/8/8/8/6K1 w - - 0 1", listOf("e5f7"), 0, listOf("Smothered mate", "mate"), true, "KnightScope"),
            Puzzle("ks-10", "6k1/8/8/8/4n3/8/6PP/6RK b - - 0 1", null, "6k1/8/8/8/4n3/8/6PP/6RK b - - 0 1", listOf("e4f2"), 0, listOf("Smothered mate", "mate"), true, "KnightScope"),
            Puzzle("ks-11", "rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2", null, "rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2", listOf("d8h4"), 0, listOf("Fool’s mate", "mate"), true, "KnightScope")
        )
    }

    private var cachedBundledPuzzles: List<Puzzle>? = null

    suspend fun loadAllPuzzles(): List<Puzzle> = withContext(Dispatchers.IO) {
        val bundled = cachedBundledPuzzles ?: run {
            val list = mutableListOf<Puzzle>()
            try {
                val jsonStr = context.assets.open("puzzles.json").bufferedReader().use { it.readText() }
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val row = arr.getJSONArray(i)
                    val id = row.getString(0)
                    val fen0 = row.getString(1)
                    val moves = row.getString(2).split(' ').filter { it.isNotBlank() }
                    val rating = row.getInt(3)
                    val themesStr = row.getString(4)
                    val themes = themesStr.split(' ').filter { it.isNotBlank() }
                    if (moves.size >= 2) {
                        list.add(
                            Puzzle(
                                id = id,
                                fen0 = fen0,
                                setupUci = moves[0],
                                fen = "",
                                solution = moves.drop(1),
                                rating = rating,
                                themes = themes,
                                isMate = themes.any { it.contains("mate", ignoreCase = true) },
                                source = "Lichess puzzle database (CC0)"
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
            cachedBundledPuzzles = list
            list
        }

        val imported = getImportedPuzzles()
        val seen = HashSet<String>()
        val result = ArrayList<Puzzle>(STARTER_PUZZLES.size + imported.size + bundled.size)
        for (p in STARTER_PUZZLES + imported + bundled) {
            if (seen.add(p.id)) {
                result.add(p)
            }
        }
        result
    }

    fun preparePuzzle(p: Puzzle): Puzzle? {
        if (p.fen.isNotEmpty()) return p
        val setup = p.setupUci ?: return null
        val b = ChessBoard(p.fen0)
        if (b.makeUciMove(setup) == null) return null
        val startFen = b.toFen()
        for (u in p.solution) {
            if (b.makeUciMove(u) == null) return null
        }
        if (p.isMate && !b.inCheckmate()) return null
        return p.copy(fen = startFen)
    }

    val boardThemeFlow: Flow<BoardTheme> = context.knightScopeDataStore.data.map { prefs ->
        BoardTheme.fromId(prefs[KEY_BOARD_THEME] ?: "green")
    }

    suspend fun setBoardTheme(theme: BoardTheme) {
        context.knightScopeDataStore.edit { it[KEY_BOARD_THEME] = theme.id }
    }

    val settingsFlow: Flow<ReviewSettings> = context.knightScopeDataStore.data.map { prefs ->
        parseSettings(prefs[KEY_SETTINGS])
    }

    suspend fun saveSettings(s: ReviewSettings) {
        val obj = JSONObject().apply {
            put("depth", s.depth)
            put("force", s.forceReanalyze)
            put("th", JSONArray(listOf(s.inaccuracyThreshold, s.mistakeThreshold, s.blunderThreshold)))
        }
        context.knightScopeDataStore.edit { it[KEY_SETTINGS] = obj.toString() }
    }

    val historyFlow: Flow<List<SavedGameEntry>> = context.knightScopeDataStore.data.map { prefs ->
        parseHistory(prefs[KEY_HISTORY])
    }

    suspend fun saveGameToHistory(entry: SavedGameEntry) {
        context.knightScopeDataStore.edit { prefs ->
            val current = parseHistory(prefs[KEY_HISTORY]).filter { it.id != entry.id }
            val updated = (listOf(entry) + current).take(100)
            prefs[KEY_HISTORY] = serializeHistory(updated).toString()
        }
    }

    suspend fun deleteGameFromHistory(id: String) {
        context.knightScopeDataStore.edit { prefs ->
            val current = parseHistory(prefs[KEY_HISTORY]).filter { it.id != id }
            prefs[KEY_HISTORY] = serializeHistory(current).toString()
        }
    }

    val puzzleAttemptsFlow: Flow<List<PuzzleAttempt>> = context.knightScopeDataStore.data.map { prefs ->
        parseAttempts(prefs[KEY_STATS])
    }

    suspend fun recordPuzzleAttempt(attempt: PuzzleAttempt) {
        context.knightScopeDataStore.edit { prefs ->
            val current = parseAttempts(prefs[KEY_STATS]).toMutableList()
            current.add(attempt)
            val trimmed = if (current.size > 2000) current.takeLast(2000) else current
            prefs[KEY_STATS] = serializeAttempts(trimmed).toString()
        }
    }

    val puzzleRatingFlow: Flow<PuzzleRatingState> = context.knightScopeDataStore.data.map { prefs ->
        parseRatingState(prefs[KEY_RATING])
    }

    suspend fun savePuzzleRating(state: PuzzleRatingState) {
        context.knightScopeDataStore.edit { prefs ->
            prefs[KEY_RATING] = serializeRatingState(state).toString()
        }
    }

    val dailyGoalFlow: Flow<Int> = context.knightScopeDataStore.data.map { prefs ->
        prefs[KEY_DAILY_GOAL]?.toIntOrNull() ?: 5
    }

    suspend fun setDailyGoal(goal: Int) {
        context.knightScopeDataStore.edit { it[KEY_DAILY_GOAL] = goal.toString() }
    }

    val friendNameFlow: Flow<String> = context.knightScopeDataStore.data.map { prefs ->
        prefs[KEY_FRIEND_NAME] ?: ""
    }

    suspend fun setFriendName(name: String) {
        context.knightScopeDataStore.edit { it[KEY_FRIEND_NAME] = name }
    }

    val reportSideFlow: Flow<String> = context.knightScopeDataStore.data.map { prefs ->
        prefs[KEY_REPORT_SIDE] ?: "both"
    }

    suspend fun setReportSide(side: String) {
        context.knightScopeDataStore.edit { it[KEY_REPORT_SIDE] = side }
    }

    val playDifficultyFlow: Flow<EngineDifficulty> = context.knightScopeDataStore.data.map { prefs ->
        EngineDifficulty.fromId(prefs[KEY_PLAY_DIFFICULTY])
    }

    suspend fun setPlayDifficulty(difficulty: EngineDifficulty) {
        context.knightScopeDataStore.edit { it[KEY_PLAY_DIFFICULTY] = difficulty.id }
    }

    val playShowLiveEvalFlow: Flow<Boolean> = context.knightScopeDataStore.data.map { prefs ->
        prefs[KEY_PLAY_SHOW_EVAL] ?: true
    }

    suspend fun setPlayShowLiveEval(show: Boolean) {
        context.knightScopeDataStore.edit { it[KEY_PLAY_SHOW_EVAL] = show }
    }

    val userProfileFlow: Flow<UserProfile> = context.knightScopeDataStore.data.map { prefs ->
        parseUserProfile(prefs[KEY_USER_PROFILE])
    }

    val contactEmailFlow: Flow<String> = context.knightScopeDataStore.data.map { prefs ->
        prefs[KEY_CONTACT_EMAIL] ?: ""
    }

    suspend fun updateProfileDisplayName(displayName: String) {
        val safeName = displayName.trim().ifBlank { "KnightScope Player" }.take(60)
        context.knightScopeDataStore.edit { prefs ->
            val current = parseUserProfile(prefs[KEY_USER_PROFILE])
            prefs[KEY_USER_PROFILE] = serializeUserProfile(current.copy(displayName = safeName)).toString()
        }
    }

    suspend fun updateProfileAvatarUrl(avatarUrl: String) {
        val safeUrl = avatarUrl.trim().take(800)
        context.knightScopeDataStore.edit { prefs ->
            val current = parseUserProfile(prefs[KEY_USER_PROFILE])
            prefs[KEY_USER_PROFILE] = serializeUserProfile(current.copy(avatarUrl = safeUrl.ifBlank { null })).toString()
        }
    }

    suspend fun updateContactEmail(email: String) {
        context.knightScopeDataStore.edit { it[KEY_CONTACT_EMAIL] = email.trim().take(120) }
    }

    suspend fun recordBotGameResult(playerWon: Boolean) {
        context.knightScopeDataStore.edit { prefs ->
            val cur = parseUserProfile(prefs[KEY_USER_PROFILE])
            val nextStreak = if (playerWon) (cur.winStreak + 1).coerceAtMost(1_000_000) else 0
            val updated = cur.copy(
                gamesPlayedBot = (cur.gamesPlayedBot + 1).coerceAtMost(1_000_000),
                winStreak = nextStreak
            )
            prefs[KEY_USER_PROFILE] = serializeUserProfile(updated).toString()
        }
    }

    suspend fun incrementGamesAnalysed() {
        context.knightScopeDataStore.edit { prefs ->
            val cur = parseUserProfile(prefs[KEY_USER_PROFILE])
            val updated = cur.copy(gamesAnalysed = (cur.gamesAnalysed + 1).coerceAtMost(1_000_000))
            prefs[KEY_USER_PROFILE] = serializeUserProfile(updated).toString()
        }
    }

    suspend fun incrementPuzzlesSolved() {
        context.knightScopeDataStore.edit { prefs ->
            val cur = parseUserProfile(prefs[KEY_USER_PROFILE])
            val updated = cur.copy(puzzlesSolved = (cur.puzzlesSolved + 1).coerceAtMost(1_000_000))
            prefs[KEY_USER_PROFILE] = serializeUserProfile(updated).toString()
        }
    }

    suspend fun getImportedPuzzles(): List<Puzzle> {
        val raw = context.knightScopeDataStore.data.first()[KEY_IMPORTED_PUZZLES] ?: return emptyList()
        return parseImportedPuzzles(raw)
    }

    suspend fun addImportedPuzzles(newOnes: List<Puzzle>) {
        context.knightScopeDataStore.edit { prefs ->
            val existing = parseImportedPuzzles(prefs[KEY_IMPORTED_PUZZLES])
            val ids = existing.map { it.id }.toSet()
            val merged = (existing + newOnes.filter { it.id !in ids }).takeLast(1500)
            prefs[KEY_IMPORTED_PUZZLES] = serializePuzzles(merged).toString()
        }
    }

    suspend fun exportBackupJson(): String {
        val prefs = context.knightScopeDataStore.data.first()
        val games = parseHistory(prefs[KEY_HISTORY])
        val stats = parseAttempts(prefs[KEY_STATS])
        val rate = parseRatingState(prefs[KEY_RATING])
        return JSONObject().apply {
            put("games", serializeHistory(games))
            put("stats", serializeAttempts(stats))
            put("rate", serializeRatingState(rate))
        }.toString(2)
    }

    suspend fun importBackupJson(jsonStr: String) {
        val root = JSONObject(jsonStr)
        val inGames = if (root.has("games")) parseHistory(root.getJSONArray("games").toString()) else emptyList()
        val inStats = if (root.has("stats")) parseAttempts(root.getJSONArray("stats").toString()) else emptyList()
        val inRate = if (root.has("rate")) parseRatingState(root.getJSONObject("rate").toString()) else null

        context.knightScopeDataStore.edit { prefs ->
            // Merge games
            val existingGames = parseHistory(prefs[KEY_HISTORY])
            val seenGame = HashSet<String>()
            val mergedGames = (inGames + existingGames)
                .sortedByDescending { it.timestamp }
                .filter { seenGame.add(it.id) }
                .take(100)
            prefs[KEY_HISTORY] = serializeHistory(mergedGames).toString()

            // Merge stats
            val existingStats = parseAttempts(prefs[KEY_STATS])
            val statMap = LinkedHashMap<String, PuzzleAttempt>()
            for (s in inStats + existingStats) {
                statMap["${s.id}|${s.timestamp}"] = s
            }
            val mergedStats = statMap.values.sortedBy { it.timestamp }.takeLast(1000)
            prefs[KEY_STATS] = serializeAttempts(mergedStats).toString()

            // Merge rating if higher sample count
            if (inRate != null) {
                val curRate = parseRatingState(prefs[KEY_RATING])
                if (inRate.count > curRate.count) {
                    prefs[KEY_RATING] = serializeRatingState(inRate).toString()
                }
            }
        }
    }

    // ===== JSON Serialization Helpers =====

    private fun parseSettings(raw: String?): ReviewSettings {
        if (raw.isNullOrBlank()) return ReviewSettings()
        return try {
            val o = JSONObject(raw)
            val th = o.optJSONArray("th")
            val a = th?.optInt(0, 5) ?: 5
            val b = th?.optInt(1, 10) ?: 10
            val c = th?.optInt(2, 20) ?: 20
            val valid = a in 2..97 && a < b && b < c && c < 100
            ReviewSettings(
                depth = o.optInt("depth", 6).coerceIn(4, 12),
                forceReanalyze = o.optBoolean("force", false),
                inaccuracyThreshold = if (valid) a else 5,
                mistakeThreshold = if (valid) b else 10,
                blunderThreshold = if (valid) c else 20
            )
        } catch (_: Exception) {
            ReviewSettings()
        }
    }

    private fun parseHistory(raw: String?): List<SavedGameEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<SavedGameEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val evArr = o.optJSONArray("ev") ?: JSONArray()
                val evs = List(evArr.length()) { idx -> evArr.optDouble(idx, 0.0) }
                val bestArr = o.optJSONArray("best") ?: JSONArray()
                val bests = List(bestArr.length()) { idx -> if (bestArr.isNull(idx)) null else bestArr.optString(idx) }

                val repObj = o.optJSONObject("rep")
                val report = if (repObj != null) {
                    val wArr = repObj.optJSONArray("w") ?: JSONArray()
                    val bArr = repObj.optJSONArray("b") ?: JSONArray()
                    val phObj = repObj.optJSONObject("ph")
                    val wPhArr = phObj?.optJSONArray("w") ?: JSONArray()
                    val bPhArr = phObj?.optJSONArray("b") ?: JSONArray()
                    PhaseReport(
                        whiteCounts = List(6) { idx -> wArr.optInt(idx, 0) },
                        blackCounts = List(6) { idx -> bArr.optInt(idx, 0) },
                        whitePhaseMistakes = List(3) { idx -> wPhArr.optInt(idx, 0) },
                        blackPhaseMistakes = List(3) { idx -> bPhArr.optInt(idx, 0) }
                    )
                } else null

                val misArr = o.optJSONArray("mis")
                val mistakes = if (misArr != null) parseImportedPuzzles(misArr.toString()) else emptyList()

                out.add(
                    SavedGameEntry(
                        id = o.optString("id"),
                        title = o.optString("title", "Game"),
                        timestamp = o.optLong("ts", 0L),
                        plies = o.optInt("plies", 0),
                        moves = o.optInt("moves", 0),
                        pgn = o.optString("pgn", ""),
                        evals = evs,
                        bestMoves = bests,
                        report = report,
                        mistakes = mistakes
                    )
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeHistory(list: List<SavedGameEntry>): JSONArray {
        val arr = JSONArray()
        for (g in list) {
            val o = JSONObject().apply {
                put("id", g.id)
                put("title", g.title)
                put("ts", g.timestamp)
                put("plies", g.plies)
                put("moves", g.moves)
                put("pgn", g.pgn)
                put("ev", JSONArray(g.evals))
                put("best", JSONArray().apply { g.bestMoves.forEach { put(it ?: JSONObject.NULL) } })
                if (g.report != null) {
                    put("rep", JSONObject().apply {
                        put("w", JSONArray(g.report.whiteCounts))
                        put("b", JSONArray(g.report.blackCounts))
                        put("ph", JSONObject().apply {
                            put("w", JSONArray(g.report.whitePhaseMistakes))
                            put("b", JSONArray(g.report.blackPhaseMistakes))
                        })
                    })
                }
                put("mis", serializePuzzles(g.mistakes))
            }
            arr.put(o)
        }
        return arr
    }

    private fun parseAttempts(raw: String?): List<PuzzleAttempt> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { idx ->
                val o = arr.getJSONObject(idx)
                PuzzleAttempt(
                    id = o.optString("id"),
                    timestamp = o.optLong("t", 0L),
                    ok = o.optBoolean("ok", false),
                    rating = o.optInt("r", 0)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeAttempts(list: List<PuzzleAttempt>): JSONArray {
        val arr = JSONArray()
        for (a in list) {
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("t", a.timestamp)
                put("ok", a.ok)
                put("r", a.rating)
            })
        }
        return arr
    }

    private fun parseRatingState(raw: String?): PuzzleRatingState {
        if (raw.isNullOrBlank()) return PuzzleRatingState()
        return try {
            val o = JSONObject(raw)
            val hArr = o.optJSONArray("h") ?: JSONArray()
            val hist = List(hArr.length()) { idx ->
                val pair = hArr.getJSONArray(idx)
                pair.optLong(0, 0L) to pair.optInt(1, 1200)
            }
            val sArr = o.optJSONArray("seen") ?: JSONArray()
            val seen = List(sArr.length()) { idx -> sArr.optString(idx) }
            PuzzleRatingState(
                rating = o.optDouble("r", 1200.0),
                count = o.optInt("n", 0),
                history = hist,
                seenIds = seen
            )
        } catch (_: Exception) {
            PuzzleRatingState()
        }
    }

    private fun serializeRatingState(s: PuzzleRatingState): JSONObject {
        val hArr = JSONArray()
        s.history.forEach { (ts, r) -> hArr.put(JSONArray().apply { put(ts); put(r) }) }
        return JSONObject().apply {
            put("r", s.rating)
            put("n", s.count)
            put("h", hArr)
            put("seen", JSONArray(s.seenIds))
        }
    }

    private fun parseImportedPuzzles(raw: String?): List<Puzzle> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { idx ->
                val o = arr.getJSONObject(idx)
                val solArr = o.optJSONArray("sol") ?: JSONArray()
                val thArr = o.optJSONArray("themes") ?: JSONArray()
                val fen = o.optString("fen", "")
                Puzzle(
                    id = o.optString("id"),
                    fen0 = fen,
                    setupUci = null,
                    fen = fen,
                    solution = List(solArr.length()) { solArr.getString(it) },
                    rating = o.optInt("rating", 0),
                    themes = List(thArr.length()) { thArr.getString(it) },
                    isMate = o.optBoolean("mate", false),
                    source = o.optString("src", "Lichess puzzle database (CC0)")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializePuzzles(list: List<Puzzle>): JSONArray {
        val arr = JSONArray()
        for (p in list) {
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("fen", p.fen.ifEmpty { p.fen0 })
                put("sol", JSONArray(p.solution))
                put("themes", JSONArray(p.themes))
                put("rating", p.rating)
                put("mate", p.isMate)
                put("src", p.source)
            })
        }
        return arr
    }

    private fun parseUserProfile(raw: String?): UserProfile {
        if (raw.isNullOrBlank()) return UserProfile(displayName = "KnightScope Player")
        return try {
            val o = JSONObject(raw)
            val avatar = o.optString("avatarUrl", "").takeIf { it.isNotBlank() }
            UserProfile(
                id = "local",
                userId = "local",
                displayName = o.optString("displayName", "KnightScope Player").ifBlank { "KnightScope Player" },
                avatarUrl = avatar,
                preferredDifficulty = o.optString("preferredDifficulty", "medium"),
                gamesPlayedBot = o.optInt("gamesPlayedBot", 0),
                gamesAnalysed = o.optInt("gamesAnalysed", 0),
                puzzlesSolved = o.optInt("puzzlesSolved", 0),
                winStreak = o.optInt("winStreak", 0)
            )
        } catch (_: Exception) {
            UserProfile(displayName = "KnightScope Player")
        }
    }

    private fun serializeUserProfile(p: UserProfile): JSONObject {
        return JSONObject().apply {
            put("displayName", p.displayName)
            if (!p.avatarUrl.isNullOrBlank()) put("avatarUrl", p.avatarUrl)
            put("preferredDifficulty", p.preferredDifficulty)
            put("gamesPlayedBot", p.gamesPlayedBot)
            put("gamesAnalysed", p.gamesAnalysed)
            put("puzzlesSolved", p.puzzlesSolved)
            put("winStreak", p.winStreak)
        }
    }
}
