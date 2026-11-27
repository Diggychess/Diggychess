package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.chess.GameAnalysisResult
import com.example.chess.SavedGameEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Syncs completed game analysis accuracy and move counts to Cloud Firestore under
 * `/users/{uid}/games/{gameId}` when a Firebase Auth user is signed in.
 * When no user is signed in, returns silently so local DataStore remains active.
 */
class FirestoreGameSync(context: Context) {

    private val appContext = context.applicationContext ?: context

    private val databaseId: String by lazy {
        appContext.getString(R.string.firestore_database_id)
    }

    suspend fun syncAnalysedGame(
        result: GameAnalysisResult,
        saved: SavedGameEntry
    ) {
        val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() ?: return
        val uid = user.uid
        val docPath = "users/$uid/games/${saved.id}"

        try {
            val db = FirebaseFirestore.getInstance(databaseId)
            val docRef = db.collection("users")
                .document(uid)
                .collection("games")
                .document(saved.id)

            val validResult = saved.result.takeIf {
                it in setOf("1-0", "0-1", "1/2-1/2", "*")
            } ?: "*"
            val safeTitle = saved.title.trim().ifBlank { "Analysed Game" }.take(160)
            val safeOpponent = saved.opponent.trim().take(100)
            val safePgn = saved.pgn.take(30000).ifBlank { "*" }
            val safeEvals = saved.evals.take(500)
            val safeBestMoves = saved.bestMoves.take(500).map { it.orEmpty() }
            val whiteCounts = (saved.report?.whiteCounts ?: emptyList()).take(10)
            val blackCounts = (saved.report?.blackCounts ?: emptyList()).take(10)
            val whitePhase = (saved.report?.whitePhaseMistakes ?: emptyList()).take(5)
            val blackPhase = (saved.report?.blackPhaseMistakes ?: emptyList()).take(5)
            val safeMistakes = saved.mistakes.take(10).map { p ->
                mapOf(
                    "id" to p.id,
                    "fen" to p.fen,
                    "solution" to p.solution,
                    "source" to p.source
                )
            }
            val whiteAccuracy = result.summaryStats.whiteAccuracy.coerceIn(0.0, 100.0)
            val blackAccuracy = result.summaryStats.blackAccuracy.coerceIn(0.0, 100.0)

            val exists = docRef.get().await().exists()
            if (exists) {
                val updatePayload = hashMapOf<String, Any>(
                    "title" to safeTitle,
                    "opponent" to safeOpponent,
                    "result" to validResult,
                    "source" to "analysed",
                    "timestampMs" to saved.timestamp.coerceAtLeast(0L),
                    "plies" to saved.plies.coerceIn(0, 1000),
                    "moves" to saved.moves.coerceIn(0, 500),
                    "pgn" to safePgn,
                    "evals" to safeEvals,
                    "bestMoves" to safeBestMoves,
                    "whiteCounts" to whiteCounts,
                    "blackCounts" to blackCounts,
                    "whitePhaseMistakes" to whitePhase,
                    "blackPhaseMistakes" to blackPhase,
                    "mistakes" to safeMistakes,
                    "whiteAccuracy" to whiteAccuracy,
                    "blackAccuracy" to blackAccuracy,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val createPayload = hashMapOf<String, Any>(
                    "userId" to uid,
                    "title" to safeTitle,
                    "opponent" to safeOpponent,
                    "result" to validResult,
                    "source" to "analysed",
                    "timestampMs" to saved.timestamp.coerceAtLeast(0L),
                    "plies" to saved.plies.coerceIn(0, 1000),
                    "moves" to saved.moves.coerceIn(0, 500),
                    "pgn" to safePgn,
                    "evals" to safeEvals,
                    "bestMoves" to safeBestMoves,
                    "whiteCounts" to whiteCounts,
                    "blackCounts" to blackCounts,
                    "whitePhaseMistakes" to whitePhase,
                    "blackPhaseMistakes" to blackPhase,
                    "mistakes" to safeMistakes,
                    "whiteAccuracy" to whiteAccuracy,
                    "blackAccuracy" to blackAccuracy,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, "WRITE", docPath, uid)
        }
    }

    suspend fun syncBotGame(
        gameId: String,
        title: String,
        opponent: String,
        result: String,
        timestampMs: Long,
        plies: Int,
        moves: Int,
        pgn: String,
        evals: List<Double>
    ) {
        val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull() ?: return
        val uid = user.uid
        val safeGameId = gameId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "play_$timestampMs" }
        val docPath = "users/$uid/games/$safeGameId"

        try {
            val db = FirebaseFirestore.getInstance(databaseId)
            val docRef = db.collection("users")
                .document(uid)
                .collection("games")
                .document(safeGameId)

            val validResult = result.takeIf { it in setOf("1-0", "0-1", "1/2-1/2", "*") } ?: "*"
            val safeTitle = title.trim().ifBlank { "Match vs Stockfish" }.take(160)
            val safeOpponent = opponent.trim().take(100)
            val safePgn = pgn.take(30000).ifBlank { "*" }
            val safeEvals = evals.take(500).map { if (it.isFinite()) it else 0.0 }

            val createPayload = hashMapOf<String, Any>(
                "userId" to uid,
                "title" to safeTitle,
                "opponent" to safeOpponent,
                "result" to validResult,
                "source" to "bot",
                "timestampMs" to timestampMs.coerceAtLeast(0L),
                "plies" to plies.coerceIn(0, 1000),
                "moves" to moves.coerceIn(0, 500),
                "pgn" to safePgn,
                "evals" to safeEvals,
                "bestMoves" to emptyList<String>(),
                "whiteCounts" to emptyList<Int>(),
                "blackCounts" to emptyList<Int>(),
                "whitePhaseMistakes" to emptyList<Int>(),
                "blackPhaseMistakes" to emptyList<Int>(),
                "mistakes" to emptyList<Map<String, Any>>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(createPayload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logFirestoreError(e, "WRITE", docPath, uid)
        }
    }

    private fun logFirestoreError(
        error: Throwable,
        operationType: String,
        path: String,
        uid: String?
    ) {
        val errJson = JSONObject().apply {
            put("error", error.message ?: error.javaClass.simpleName)
            put("operationType", operationType)
            put("path", path)
            put("userId", uid ?: "")
        }
        Log.e("FirestoreGameSync", "Firestore Error: $errJson", error)
    }
}
