package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

private const val TAG = "AiChessCoach"
private const val FIREBASE_MODEL_PRIMARY = "gemini-3.8-flash"
private const val FIREBASE_MODEL_LITE = "gemini-3.5-flash-lite"
private const val GROQ_MODEL_PRIMARY = "llama-3.3-70b-versatile"
private const val GROQ_MODEL_FAST = "llama-3.1-8b-instant"
private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
private const val POLLINATIONS_CHAT_URL = "https://gen.pollinations.ai/v1/chat/completions"
private const val POLLINATIONS_TEXT_URL = "https://text.pollinations.ai/openai"

data class AiMoveInsight(
    val headline: String,
    val whyGoodOrBad: String,
    val whyBestIsStronger: String,
    val whatWasMissed: String,
    val improvementTip: String,
    val providerLabel: String,
    val isFromCache: Boolean = false
)

data class AiGameSummaryReport(
    val openingInsight: String,
    val middlegameInsight: String,
    val endgameInsight: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val trainingFocus: String,
    val providerLabel: String,
    val isFromCache: Boolean = false
)

data class CoachChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val providerLabel: String? = null,
    val isError: Boolean = false,
    val webSources: List<TinyFishSearchResult> = emptyList()
)

data class CoachStreamUpdate(
    val text: String,
    val providerLabel: String,
    val webSources: List<TinyFishSearchResult> = emptyList()
)

sealed interface AiInsightState<out T> {
    data object Idle : AiInsightState<Nothing>
    data object Loading : AiInsightState<Nothing>
    data class Success<T>(val data: T) : AiInsightState<T>
    data class Error(val message: String, val fallbackMessage: String? = null) : AiInsightState<Nothing>
}

sealed interface CoachChatStreamState {
    data object Idle : CoachChatStreamState
    data object Loading : CoachChatStreamState
    data class Streaming(val partialText: String) : CoachChatStreamState
    data class Error(val message: String) : CoachChatStreamState
}

/**
 * Workable multi-provider AI Chess Coach grounded in verified Stockfish 19 data:
 * 1) Firebase AI Logic (`gemini-3.8-flash`, fallback `gemini-3.5-flash-lite` via App Check),
 * 2) Groq Cloud (`llama-3.3-70b-versatile` / `llama-3.1-8b-instant` via Secrets or Settings),
 * 3) Pollinations free keyless OpenAI-compatible backup (`awesome-freellm-apis`),
 * 4) Live TinyFish Web Search context (`TinyFishSearchService`) for theory & chess news,
 * 5) Guaranteed workable Stockfish Tactical Coach offline engine fallback (never dead-ends).
 */
class AiChessCoachService(
    private val repo: KnightScopeRepository,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val tinyFish: TinyFishSearchService = TinyFishSearchService(repo, httpClient)
) {
    private val moveInsightSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string("Concise 1-sentence coaching headline referencing played move."),
            "whyGoodOrBad" to Schema.string("Human explanation of why the played move succeeded or lost evaluation."),
            "whyBestIsStronger" to Schema.string("Concrete chess reason why the engine best move or top PV is stronger."),
            "whatWasMissed" to Schema.string("Tactical or positional threat or opportunity missed in the position."),
            "improvementTip" to Schema.string("Actionable habit or calculation tip for future games.")
        )
    )

    private val gameSummarySchema = Schema.obj(
        properties = mapOf(
            "openingInsight" to Schema.string("Coaching review of opening phase accuracy and theory."),
            "middlegameInsight" to Schema.string("Coaching review of middlegame tactics, king safety, and plans."),
            "endgameInsight" to Schema.string("Coaching review of endgame technique or conversion."),
            "strengths" to Schema.array(Schema.string(), "2-3 concrete strengths shown in this game."),
            "weaknesses" to Schema.array(Schema.string(), "2-3 key areas to sharpen based on the mistakes."),
            "trainingFocus" to Schema.string("1 focused practice drill recommendation.")
        )
    )

    private val coachSystemInstruction = content {
        text(
            "You are Chess Analyzer: AI Coach, a knowledgeable, encouraging FIDE chess coach. " +
                "Stockfish 19 calculations provided in the prompt (FEN, played move, classification, " +
                "evaluations in pawns from White POV, best move, and top PV lines) are authoritative ground truth. " +
                "NEVER invent different engine evaluations or illegal moves outside the provided Stockfish data. " +
                "Give clear, practical, human chess coaching."
        )
    }

    private val moveModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FIREBASE_MODEL_PRIMARY,
            generationConfig = generationConfig {
                temperature = 0.35f
                responseMimeType = "application/json"
                responseSchema = moveInsightSchema
            },
            systemInstruction = coachSystemInstruction
        )
    }

    private val summaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FIREBASE_MODEL_PRIMARY,
            generationConfig = generationConfig {
                temperature = 0.35f
                responseMimeType = "application/json"
                responseSchema = gameSummarySchema
            },
            systemInstruction = coachSystemInstruction
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FIREBASE_MODEL_PRIMARY,
            generationConfig = generationConfig { temperature = 0.45f },
            systemInstruction = coachSystemInstruction
        )
    }

    private val liteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FIREBASE_MODEL_LITE,
            generationConfig = generationConfig { temperature = 0.35f },
            systemInstruction = coachSystemInstruction
        )
    }

    fun hasGroqBackupConfigured(): Boolean = BuildConfig.GROQ_API_KEY.isNotBlank()

    suspend fun hasAnyGroqKey(): Boolean = repo.getEffectiveGroqKey().isNotBlank()

    suspend fun hasTinyFishKey(): Boolean = tinyFish.isConfigured()

    fun buildMoveCacheKey(ply: AnalyzedPly, depth: Int): String =
        "${ply.fenBefore}|${ply.san}|${ply.badgeLabel}|d$depth"

    suspend fun getMoveInsight(
        ply: AnalyzedPly,
        openingName: String?,
        depth: Int,
        allowCloud: Boolean = true
    ): Result<AiMoveInsight> {
        val cacheKey = buildMoveCacheKey(ply, depth)
        repo.getCachedAiMoveInsight(cacheKey)?.let { cached ->
            return Result.success(cached)
        }

        if (!allowCloud) {
            return Result.success(buildOfflineMoveInsight(ply, "Offline Stockfish Coach"))
        }

        if (repo.getDailyAiRemaining() <= 0) {
            return Result.success(buildOfflineMoveInsight(ply, "Stockfish Coach (Daily cloud limit)"))
        }

        val prompt = buildMovePrompt(ply, openingName)

        // 1. Primary: Firebase AI Logic (gemini-3.8-flash -> gemini-3.5-flash-lite)
        val geminiJson = tryFirebaseJson(
            primaryCall = {
                withTimeout(18_000L) {
                    moveModel.generateContent(prompt).text
                }
            },
            litePrompt = "$prompt\n\nRespond ONLY with valid JSON keys: headline, whyGoodOrBad, whyBestIsStronger, whatWasMissed, improvementTip."
        )
        if (geminiJson != null) {
            runCatching { parseMoveInsightJson(geminiJson, "Gemini AI Coach") }.getOrNull()?.let { insight ->
                repo.tryConsumeDailyAiQuota()
                repo.putCachedAiMoveInsight(cacheKey, insight)
                return Result.success(insight)
            }
        }

        // 2. Backup 1: Groq Cloud (Secrets or user Settings key)
        val groqKey = repo.getEffectiveGroqKey()
        if (groqKey.isNotBlank()) {
            val groqSys = "You are Chess Analyzer: AI Coach. Return ONLY valid JSON with keys: " +
                "headline, whyGoodOrBad, whyBestIsStronger, whatWasMissed, improvementTip. " +
                "Never invent Stockfish evaluations or illegal variations."
            val groqJson = tryGroqWithModelFallback(groqKey, groqSys, prompt, jsonMode = true)
            if (groqJson != null) {
                runCatching { parseMoveInsightJson(groqJson, "Groq AI Coach") }.getOrNull()?.let { insight ->
                    repo.tryConsumeDailyAiQuota()
                    repo.putCachedAiMoveInsight(cacheKey, insight)
                    return Result.success(insight)
                }
            }
        }

        // 3. Backup 2: Keyless Free LLM (Pollinations from awesome-freellm-apis)
        val freeJson = tryPollinationsCompletion(
            systemPrompt = "You are Chess Analyzer: AI Coach. Return ONLY valid JSON with keys: " +
                "headline, whyGoodOrBad, whyBestIsStronger, whatWasMissed, improvementTip. Ground strictly in Stockfish data.",
            userPrompt = prompt,
            jsonMode = true
        )
        if (freeJson != null) {
            runCatching { parseMoveInsightJson(freeJson, "Free Cloud AI Coach") }.getOrNull()?.let { insight ->
                repo.tryConsumeDailyAiQuota()
                repo.putCachedAiMoveInsight(cacheKey, insight)
                return Result.success(insight)
            }
        }

        // 4. Guaranteed Workable Fallback: Stockfish Tactical Coach (never leaves card broken)
        return Result.success(buildOfflineMoveInsight(ply, "Stockfish Tactical Coach"))
    }

    suspend fun getGameSummaryReport(
        result: GameAnalysisResult,
        allowCloud: Boolean = true
    ): Result<AiGameSummaryReport> {
        repo.getCachedAiGameReport(result.id)?.let { cached ->
            return Result.success(cached)
        }

        if (!allowCloud) {
            return Result.success(buildOfflineGameSummary(result, "Offline Stockfish Coach"))
        }

        if (repo.getDailyAiRemaining() <= 0) {
            return Result.success(buildOfflineGameSummary(result, "Stockfish Coach (Daily cloud limit)"))
        }

        val prompt = buildGameSummaryPrompt(result)

        // 1. Primary: Firebase AI Logic
        val geminiJson = tryFirebaseJson(
            primaryCall = {
                withTimeout(20_000L) {
                    summaryModel.generateContent(prompt).text
                }
            },
            litePrompt = "$prompt\n\nRespond ONLY with valid JSON keys: openingInsight, middlegameInsight, endgameInsight, strengths (string array), weaknesses (string array), trainingFocus."
        )
        if (geminiJson != null) {
            runCatching { parseGameSummaryJson(geminiJson, "Gemini AI Coach") }.getOrNull()?.let { report ->
                repo.tryConsumeDailyAiQuota()
                repo.putCachedAiGameReport(result.id, report)
                return Result.success(report)
            }
        }

        // 2. Backup 1: Groq Cloud
        val groqKey = repo.getEffectiveGroqKey()
        if (groqKey.isNotBlank()) {
            val groqSys = "You are Chess Analyzer: AI Coach. Return ONLY valid JSON with keys: " +
                "openingInsight, middlegameInsight, endgameInsight, strengths (string array), " +
                "weaknesses (string array), trainingFocus. Ground exclusively in the provided Stockfish stats."
            val groqJson = tryGroqWithModelFallback(groqKey, groqSys, prompt, jsonMode = true)
            if (groqJson != null) {
                runCatching { parseGameSummaryJson(groqJson, "Groq AI Coach") }.getOrNull()?.let { report ->
                    repo.tryConsumeDailyAiQuota()
                    repo.putCachedAiGameReport(result.id, report)
                    return Result.success(report)
                }
            }
        }

        // 3. Backup 2: Keyless Free LLM (Pollinations)
        val freeJson = tryPollinationsCompletion(
            systemPrompt = "You are Chess Analyzer: AI Coach. Return ONLY valid JSON with keys: " +
                "openingInsight, middlegameInsight, endgameInsight, strengths (array), weaknesses (array), trainingFocus.",
            userPrompt = prompt,
            jsonMode = true
        )
        if (freeJson != null) {
            runCatching { parseGameSummaryJson(freeJson, "Free Cloud AI Coach") }.getOrNull()?.let { report ->
                repo.tryConsumeDailyAiQuota()
                repo.putCachedAiGameReport(result.id, report)
                return Result.success(report)
            }
        }

        // 4. Guaranteed Workable Fallback
        return Result.success(buildOfflineGameSummary(result, "Stockfish Tactical Coach"))
    }

    fun streamCoachAnswer(
        question: String,
        result: GameAnalysisResult,
        currentPly: Int,
        activeFen: String,
        liveLines: List<EngineVariation>,
        recentHistory: List<CoachChatMessage>
    ): Flow<CoachStreamUpdate> = flow {
        val webResults = runCatching {
            tinyFish.searchChessContext(question, result.openingSummary.entry?.name)
        }.getOrElse { emptyList() }

        val hasQuota = repo.getDailyAiRemaining() > 0
        if (hasQuota) {
            val groundedPrompt = buildChatPrompt(
                question = question,
                result = result,
                currentPly = currentPly,
                activeFen = activeFen,
                liveLines = liveLines,
                recentHistory = recentHistory,
                webResults = webResults
            )

            // 1. Primary: Firebase AI Logic streaming (`gemini-3.8-flash`)
            val buffer = StringBuilder()
            var firebaseSucceeded = false
            try {
                withTimeout(22_000L) {
                    chatModel.generateContentStream(groundedPrompt).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            buffer.append(piece)
                            emit(CoachStreamUpdate(buffer.toString(), "Gemini AI Coach", webResults))
                        }
                    }
                }
                if (buffer.isNotBlank()) {
                    firebaseSucceeded = true
                }
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    Log.e(TAG, "Firebase AI chat stream timed out", e)
                } else {
                    throw e
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI chat stream failed", e)
            }

            if (firebaseSucceeded) {
                repo.tryConsumeDailyAiQuota()
                return@flow
            }

            // 1b. Quick Firebase Lite fallback if 3.8 Flash returned partial/error
            val liteText = try {
                withTimeout(15_000L) {
                    liteModel.generateContent(groundedPrompt).text?.trim().orEmpty()
                }
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) "" else throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI lite chat failed", e)
                ""
            }
            if (liteText.isNotBlank()) {
                repo.tryConsumeDailyAiQuota()
                emit(CoachStreamUpdate(liteText, "Gemini Flash-Lite Coach", webResults))
                return@flow
            }

            // 2. Backup 1: Groq Cloud (`llama-3.3-70b-versatile` / `llama-3.1-8b-instant`)
            val groqKey = repo.getEffectiveGroqKey()
            if (groqKey.isNotBlank()) {
                val groqReply = tryGroqWithModelFallback(
                    apiKey = groqKey,
                    systemPrompt = "You are Chess Analyzer: AI Coach. Answer the player's question clearly, practically, and accurately grounded in the Stockfish data and any verified web references provided.",
                    userPrompt = groundedPrompt,
                    jsonMode = false
                )
                if (!groqReply.isNullOrBlank()) {
                    repo.tryConsumeDailyAiQuota()
                    val label = if (webResults.isNotEmpty()) "Groq AI + TinyFish" else "Groq AI Coach"
                    emit(CoachStreamUpdate(groqReply, label, webResults))
                    return@flow
                }
            }

            // 3. Backup 2: Free Keyless Pollinations LLM
            val freeReply = tryPollinationsCompletion(
                systemPrompt = "You are Chess Analyzer: AI Coach. Answer concisely and clearly grounded ONLY in the Stockfish data and references provided.",
                userPrompt = groundedPrompt,
                jsonMode = false
            )
            if (!freeReply.isNullOrBlank()) {
                repo.tryConsumeDailyAiQuota()
                val label = if (webResults.isNotEmpty()) "Free Cloud AI + TinyFish" else "Free Cloud AI Coach"
                emit(CoachStreamUpdate(freeReply, label, webResults))
                return@flow
            }
        }

        // 4. Guaranteed Workable Engine Coach Fallback (plus TinyFish Web results if available)
        val offlineAnswer = buildOfflineChatAnswer(
            question = question,
            result = result,
            currentPly = currentPly,
            activeFen = activeFen,
            liveLines = liveLines,
            webResults = webResults
        )
        val label = if (webResults.isNotEmpty()) {
            "TinyFish Web + Stockfish Coach"
        } else {
            "Stockfish Tactical Coach"
        }
        emit(CoachStreamUpdate(offlineAnswer, label, webResults))
    }

    /**
     * Direct TinyFish web search pass-through usable both inside Coach Q&A and UI search actions.
     */
    suspend fun searchWebWithTinyFish(query: String, location: String = "US", language: String = "en"): Result<List<TinyFishSearchResult>> {
        return tinyFish.query(query = query, location = location, language = language)
    }

    private suspend fun tryFirebaseJson(
        primaryCall: suspend () -> String?,
        litePrompt: String
    ): String? {
        try {
            val primary = primaryCall()?.trim()
            if (!primary.isNullOrBlank()) return primary
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.e(TAG, "Firebase primary model timed out", e)
            } else {
                throw e
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase primary model failed: ${e.message}", e)
        }

        return try {
            withTimeout(14_000L) {
                liteModel.generateContent(litePrompt).text?.trim()?.takeIf { it.isNotBlank() }
            }
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) null else throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase lite fallback failed: ${e.message}", e)
            null
        }
    }

    private suspend fun tryGroqWithModelFallback(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        jsonMode: Boolean
    ): String? {
        val models = listOf(GROQ_MODEL_PRIMARY, GROQ_MODEL_FAST)
        for (model in models) {
            try {
                val res = callOpenAiCompatibleChat(
                    url = GROQ_API_URL,
                    apiKey = apiKey,
                    model = model,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    jsonMode = jsonMode,
                    timeoutMs = 18_000L
                )
                if (res.isNotBlank()) return res
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    Log.e(TAG, "Groq model $model timed out", e)
                } else {
                    throw e
                }
            } catch (e: Exception) {
                Log.e(TAG, "Groq model $model failed: ${e.message}")
            }
        }
        return null
    }

    private suspend fun tryPollinationsCompletion(
        systemPrompt: String,
        userPrompt: String,
        jsonMode: Boolean
    ): String? {
        for (url in listOf(POLLINATIONS_CHAT_URL, POLLINATIONS_TEXT_URL)) {
            try {
                val res = callOpenAiCompatibleChat(
                    url = url,
                    apiKey = null,
                    model = "openai",
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    jsonMode = jsonMode,
                    timeoutMs = 11_000L
                )
                if (res.isNotBlank()) return res
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) {
                    Log.w(TAG, "Pollinations $url timed out")
                } else {
                    throw e
                }
            } catch (e: Exception) {
                Log.w(TAG, "Pollinations $url failed: ${e.message}")
            }
        }
        return null
    }

    fun buildOfflineMoveInsight(ply: AnalyzedPly, providerLabel: String = "Offline Stockfish Coach"): AiMoveInsight {
        val mvLabel = "${ply.index / 2 + 1}${if (ply.index % 2 == 1) "..." else "."} ${ply.san}"
        val headline = ply.coachHeadline.ifBlank { "$mvLabel — ${ply.badgeLabel}" }
        val whyBest = if (!ply.bestSan.isNullOrBlank() && ply.bestSan != ply.san) {
            ply.bestExplanation
        } else {
            "Engine evaluation confirms ${ply.san} holds the strongest evaluation (${MoveExplainer.formatPlyEval(ply.evalAfter, ply.mate)})."
        }
        val missed = when {
            ply.moveClass == MoveClass.BLUNDER || ply.moveClass == MoveClass.MISTAKE ->
                "Swung win probability by ${String.format(Locale.US, "%.1f%%", abs(ply.winProbSwing))}" +
                    (ply.bestSan?.let { " compared to $it." } ?: ".")
            ply.tacticalTags.isNotEmpty() ->
                "Key motifs present: ${ply.tacticalTags.joinToString(", ")}."
            else -> "No major tactical oversight detected on this move."
        }
        val tip = when {
            ply.moveClass.isSuboptimal -> "Pause before moving to scan opponent checks, captures, and undefended pieces."
            ply.isBook -> "Understanding the pawn structure behind ${ply.opening?.name ?: "this opening"} matters more than memorizing moves."
            else -> "Keep coordinating your pieces and double-check forcing tactical replies."
        }
        return AiMoveInsight(
            headline = headline,
            whyGoodOrBad = ply.explanation,
            whyBestIsStronger = whyBest,
            whatWasMissed = missed,
            improvementTip = tip,
            providerLabel = providerLabel
        )
    }

    fun buildOfflineGameSummary(result: GameAnalysisResult, providerLabel: String = "Offline Stockfish Coach"): AiGameSummaryReport {
        val st = result.summaryStats
        val opName = result.openingSummary.entry?.name ?: "Unclassified Opening"
        val blunderCount = (st.whiteCounts[MoveClass.BLUNDER] ?: 0) + (st.blackCounts[MoveClass.BLUNDER] ?: 0)
        val mistakeCount = (st.whiteCounts[MoveClass.MISTAKE] ?: 0) + (st.blackCounts[MoveClass.MISTAKE] ?: 0)
        return AiGameSummaryReport(
            openingInsight = "$opName (${result.openingSummary.entry?.eco ?: "-"}). " +
                "Opening accuracy: White ${st.openingStats.whiteAccuracy}, Black ${st.openingStats.blackAccuracy}. ${result.openingSummary.engineFirstNote}",
            middlegameInsight = "Middlegame accuracy: White ${st.middlegameStats.whiteAccuracy}, Black ${st.middlegameStats.blackAccuracy}. " +
                if (result.keyMistakes.isNotEmpty()) "Decisive tactical swings clustered around move ${result.keyMistakes.first().index / 2 + 1}." else "Both sides navigated middlegame tactics steadily.",
            endgameInsight = "Endgame accuracy: White ${st.endgameStats.whiteAccuracy}, Black ${st.endgameStats.blackAccuracy}.",
            strengths = listOf(
                "White overall accuracy: ${String.format(Locale.US, "%.1f%%", st.whiteAccuracy)} · Black: ${String.format(Locale.US, "%.1f%%", st.blackAccuracy)}",
                "Best + Excellent moves: White ${(st.whiteCounts[MoveClass.BEST] ?: 0) + (st.whiteCounts[MoveClass.EXCELLENT] ?: 0)}, Black ${(st.blackCounts[MoveClass.BEST] ?: 0) + (st.blackCounts[MoveClass.EXCELLENT] ?: 0)}"
            ),
            weaknesses = listOf(
                "Total Blunders: $blunderCount · Mistakes: $mistakeCount",
                if (result.keyMistakes.isNotEmpty()) "Review critical moment ${result.keyMistakes.first().index / 2 + 1}. ${result.keyMistakes.first().san}" else "Focus on converting slight positional edges cleanly."
            ),
            trainingFocus = if (result.keyMistakes.isNotEmpty()) "Use 'Practice All' in Key Mistakes to drill the exact missed Stockfish lines." else "Challenge Stockfish on a higher difficulty or practice tactical calculation.",
            providerLabel = providerLabel
        )
    }

    internal fun buildOfflineChatAnswer(
        question: String,
        result: GameAnalysisResult,
        currentPly: Int,
        activeFen: String,
        liveLines: List<EngineVariation>,
        webResults: List<TinyFishSearchResult> = emptyList()
    ): String {
        val q = question.lowercase()
        val activePly = if (currentPly > 0) result.plies.getOrNull(currentPly - 1) else null
        val board = runCatching { ChessBoard(activeFen) }.getOrNull()
        val lines = if (liveLines.isNotEmpty()) liveLines else activePly?.topLines.orEmpty()
        val topLine = lines.firstOrNull()
        val hanging = runCatching { MoveExplainer.hangs(activeFen) }.getOrNull()
        val hangingSan = if (hanging != null && board != null) {
            runCatching { board.toSan(hanging.move) }.getOrDefault("${hanging.move.fromName}x${hanging.move.toName}")
        } else {
            hanging?.move?.let { "${it.fromName}x${it.toName}" }
        }
        val checksCount = if (board != null) {
            board.legalMoves().count { m ->
                board.makeMove(m)
                val chk = board.inCheck()
                board.undo()
                chk
            }
        } else 0
        val capturesCount = board?.legalMoves()?.count { it.captured != null } ?: 0

        val sb = StringBuilder()

        if (webResults.isNotEmpty() && (q.startsWith("search:") || q.startsWith("web:") || q.contains("news") || q.contains("fifa") || q.contains("latest"))) {
            sb.append("Live Web Results via TinyFish:\n")
            webResults.take(3).forEachIndexed { idx, r ->
                sb.append("${idx + 1}. ${r.title}")
                if (r.snippet.isNotBlank()) sb.append(" — ${r.snippet}")
                sb.append("\n")
            }
            return sb.toString().trim()
        }

        when {
            q.contains("turning") || q.contains("summary") || q.contains("key") -> {
                val topSwing = result.plies.maxByOrNull { abs(it.winProbSwing) }
                sb.append("Game Turning Point & Review:\n")
                if (topSwing != null) {
                    val mv = "${topSwing.index / 2 + 1}${if (topSwing.index % 2 == 1) "..." else "."} ${topSwing.san}"
                    sb.append("• Biggest swing: $mv (${topSwing.badgeLabel}), shifting eval from ${MoveExplainer.formatEval(topSwing.evalBefore)} to ${MoveExplainer.formatPlyEval(topSwing.evalAfter, topSwing.mate)} (${String.format(Locale.US, "%.1f%%", abs(topSwing.winProbSwing))} win% swing).\n")
                    if (!topSwing.bestSan.isNullOrBlank() && topSwing.bestSan != topSwing.san) {
                        sb.append("• Stockfish's stronger alternative was ${topSwing.bestSan}: ${topSwing.bestExplanation}\n")
                    }
                }
                if (result.keyMistakes.isNotEmpty()) {
                    sb.append("• Critical moments: ")
                    sb.append(result.keyMistakes.take(3).joinToString("; ") { p ->
                        "${p.index / 2 + 1}${if (p.index % 2 == 1) "..." else "."} ${p.san} (${p.badgeLabel}, best ${p.bestSan ?: "-"})"
                    })
                    sb.append(".\n")
                }
                sb.append("• Accuracy: ${result.summaryStats.whiteName} ${String.format(Locale.US, "%.1f%%", result.summaryStats.whiteAccuracy)} vs ${result.summaryStats.blackName} ${String.format(Locale.US, "%.1f%%", result.summaryStats.blackAccuracy)}.")
            }

            q.contains("opening") || q.contains("theory") || q.contains("eco") -> {
                val op = result.openingSummary.entry
                sb.append("Opening Breakdown:\n")
                sb.append("• Classified Opening: ${op?.name ?: "Unclassified"} (${op?.eco ?: "-"}).\n")
                sb.append("• ${result.openingSummary.engineFirstNote}\n")
                sb.append("• Opening accuracy: White ${result.summaryStats.openingStats.whiteAccuracy} | Black ${result.summaryStats.openingStats.blackAccuracy}.")
            }

            q.contains("why") && (q.contains("best") || q.contains("strong") || q.contains("better") || q.contains("inaccur") || q.contains("mistake") || q.contains("blunder")) && activePly != null -> {
                val mv = "${activePly.index / 2 + 1}${if (activePly.index % 2 == 1) "..." else "."} ${activePly.san}"
                sb.append("Why Stockfish evaluates $mv as ${activePly.badgeLabel}:\n")
                sb.append("• Played move impact: ${activePly.explanation}\n")
                if (!activePly.bestSan.isNullOrBlank() && activePly.bestSan != activePly.san) {
                    sb.append("• Why ${activePly.bestSan} is stronger: ${activePly.bestExplanation}\n")
                }
                if (topLine != null && topLine.pvSan.isNotEmpty()) {
                    sb.append("• Top engine continuation (${MoveExplainer.formatPlyEval(topLine.cp, topLine.mate)}): ${topLine.pvSan.take(6).joinToString(" ")}.")
                }
            }

            q.contains("tactic") || q.contains("motif") || q.contains("miss") || q.contains("threat") -> {
                sb.append("Tactical Scan of the Current Position:\n")
                if (activePly != null && activePly.tacticalTags.isNotEmpty()) {
                    sb.append("• Detected motifs: ${activePly.tacticalTags.joinToString(", ")}.\n")
                }
                if (board?.inCheck() == true) {
                    sb.append("• Immediate forcing alert: the ${if (board.turn == PieceColor.WHITE) "White" else "Black"} King is in check.\n")
                }
                if (hanging != null && hangingSan != null) {
                    sb.append("• Tactical target: $hangingSan attacks/wins material (net ${if (hanging.netGain >= 0) "+" else ""}${hanging.netGain} pawns${if (hanging.recapturable) " even after recapture" else " cleanly"}).\n")
                }
                sb.append("• Forcing moves available now: $checksCount checks and $capturesCount captures.\n")
                if (topLine != null && topLine.pvSan.isNotEmpty()) {
                    sb.append("• Stockfish's cleanest line (${MoveExplainer.formatPlyEval(topLine.cp, topLine.mate)}): ${topLine.pvSan.take(6).joinToString(" ")}.")
                }
            }

            else -> {
                val turnSide = if (board?.turn == PieceColor.BLACK) "Black" else "White"
                val evalLabel = when {
                    topLine != null -> MoveExplainer.formatPlyEval(topLine.cp, topLine.mate)
                    activePly != null -> MoveExplainer.formatPlyEval(activePly.evalAfter, activePly.mate)
                    else -> "+0.2"
                }
                sb.append("Position Plan ($turnSide to move, Eval $evalLabel):\n")
                if (activePly != null) {
                    sb.append("• Last move: ${activePly.san} (${activePly.badgeLabel}) — ${activePly.explanation}\n")
                }
                val openingHere = OpeningBook.lookup(activeFen)
                if (openingHere != null) {
                    sb.append("• Theory match: ${openingHere.eco} ${openingHere.name}.\n")
                }
                if (lines.isNotEmpty()) {
                    sb.append("• Stockfish top candidate plans:\n")
                    lines.take(3).forEach { l ->
                        sb.append("  #${l.rank} [${MoveExplainer.formatPlyEval(l.cp, l.mate)}]: ${l.pvSan.take(6).joinToString(" ")}\n")
                    }
                } else {
                    sb.append("• Focus on King safety, activating your least-active piece, and scanning the $checksCount available checks & $capturesCount captures.\n")
                }
                if (hangingSan != null) {
                    sb.append("• Watch out for $hangingSan, which challenges undefended material.")
                }
            }
        }

        if (webResults.isNotEmpty()) {
            sb.append("\n\nTinyFish Web References:\n")
            webResults.take(2).forEach { r ->
                sb.append("• ${r.title}: ${r.url}\n")
            }
        }

        return sb.toString().trim()
    }

    private fun buildMovePrompt(ply: AnalyzedPly, openingName: String?): String {
        val side = if (ply.index % 2 == 0) "White" else "Black"
        val moveNum = ply.index / 2 + 1
        val topLinesText = if (ply.topLines.isEmpty()) "None" else ply.topLines.joinToString("\n") { line ->
            "Line #${line.rank}: ${MoveExplainer.formatPlyEval(line.cp, line.mate)} -> ${line.pvSan.take(6).joinToString(" ")}"
        }
        return """
            Analyze this single chess move as a coach using ONLY the verified Stockfish data below:
            Side to move: $side (Move $moveNum)
            Position FEN before move: ${ply.fenBefore}
            Position FEN after move: ${ply.fenAfter}
            Opening: ${openingName ?: ply.opening?.name ?: "Middlegame / Endgame"}
            Played move: ${ply.san}
            Stockfish Classification: ${ply.badgeLabel} (${ply.badgeGlyph})
            Eval before: ${MoveExplainer.formatEval(ply.evalBefore)}
            Eval after: ${MoveExplainer.formatPlyEval(ply.evalAfter, ply.mate)}
            Win-probability loss: ${String.format(Locale.US, "%.1f%%", ply.loss)}
            Stockfish best move: ${ply.bestSan ?: ply.san}
            Tactical tags detected: ${ply.tacticalTags.joinToString(", ").ifEmpty { "Positional" }}
            Stockfish Top 3 PV lines from before move:
            $topLinesText
        """.trimIndent()
    }

    private fun buildGameSummaryPrompt(result: GameAnalysisResult): String {
        val st = result.summaryStats
        val keyList = if (result.keyMistakes.isEmpty()) "No major mistakes." else {
            result.keyMistakes.joinToString("\n") { p ->
                val mv = "${p.index / 2 + 1}${if (p.index % 2 == 1) "..." else "."} ${p.san}"
                "- $mv (${p.badgeLabel}, eval ${MoveExplainer.formatEval(p.evalBefore)} -> ${MoveExplainer.formatPlyEval(p.evalAfter, p.mate)}, best ${p.bestSan ?: "-"})"
            }
        }
        return """
            Summarize this analyzed chess game (${result.title}, Result: ${st.result ?: "*"}, Depth ${result.depth}):
            Opening: ${result.openingSummary.entry?.eco ?: "-"} ${result.openingSummary.entry?.name ?: "Unclassified"}
            White (${st.whiteName}): Accuracy ${String.format(Locale.US, "%.1f%%", st.whiteAccuracy)} (Op ${st.openingStats.whiteAccuracy}, Mid ${st.middlegameStats.whiteAccuracy}, End ${st.endgameStats.whiteAccuracy})
            White Counts: Brilliant=${st.whiteCounts[MoveClass.BRILLIANT] ?: 0}, Best=${st.whiteCounts[MoveClass.BEST] ?: 0}, Inacc=${st.whiteCounts[MoveClass.INACCURACY] ?: 0}, Mistake=${st.whiteCounts[MoveClass.MISTAKE] ?: 0}, Blunder=${st.whiteCounts[MoveClass.BLUNDER] ?: 0}
            Black (${st.blackName}): Accuracy ${String.format(Locale.US, "%.1f%%", st.blackAccuracy)} (Op ${st.openingStats.blackAccuracy}, Mid ${st.middlegameStats.blackAccuracy}, End ${st.endgameStats.blackAccuracy})
            Black Counts: Brilliant=${st.blackCounts[MoveClass.BRILLIANT] ?: 0}, Best=${st.blackCounts[MoveClass.BEST] ?: 0}, Inacc=${st.blackCounts[MoveClass.INACCURACY] ?: 0}, Mistake=${st.blackCounts[MoveClass.MISTAKE] ?: 0}, Blunder=${st.blackCounts[MoveClass.BLUNDER] ?: 0}
            Key Moments:
            $keyList
        """.trimIndent()
    }

    private fun buildChatPrompt(
        question: String,
        result: GameAnalysisResult,
        currentPly: Int,
        activeFen: String,
        liveLines: List<EngineVariation>,
        recentHistory: List<CoachChatMessage>,
        webResults: List<TinyFishSearchResult> = emptyList()
    ): String {
        val active = if (currentPly > 0) result.plies.getOrNull(currentPly - 1) else null
        val activeDesc = if (active != null) {
            "Current move viewed: ply $currentPly (${active.san}, ${active.badgeLabel}), eval ${MoveExplainer.formatPlyEval(active.evalAfter, active.mate)}, Stockfish best was ${active.bestSan ?: active.san}."
        } else {
            "Viewing starting position."
        }
        val linesStr = if (liveLines.isEmpty()) "None" else liveLines.joinToString("\n") { l ->
            "#${l.rank} (${MoveExplainer.formatPlyEval(l.cp, l.mate)}): ${l.pvSan.take(6).joinToString(" ")}"
        }
        val histStr = recentHistory.takeLast(4).joinToString("\n") { m ->
            "${if (m.isUser) "Student" else "Coach"}: ${m.text.take(300)}"
        }
        val webStr = if (webResults.isEmpty()) "" else {
            "\nTinyFish Live Web References:\n" + webResults.joinToString("\n") { r ->
                "- ${r.title} (${r.url}): ${r.snippet}"
            }
        }
        return """
            Game: ${result.title} (${result.openingSummary.entry?.name ?: "Chess game"})
            White Accuracy: ${String.format(Locale.US, "%.1f%%", result.summaryStats.whiteAccuracy)} | Black Accuracy: ${String.format(Locale.US, "%.1f%%", result.summaryStats.blackAccuracy)}
            $activeDesc
            Board FEN: $activeFen
            Verified Stockfish Lines:
            $linesStr$webStr
            Recent conversation:
            $histStr
            Student question: $question
        """.trimIndent()
    }

    private fun parseMoveInsightJson(jsonString: String, providerLabel: String): AiMoveInsight {
        val cleaned = extractJsonObjectString(jsonString)
        val o = JSONObject(cleaned)
        return AiMoveInsight(
            headline = o.optString("headline").ifBlank { "Move Analysis" },
            whyGoodOrBad = o.optString("whyGoodOrBad").ifBlank { "No commentary provided." },
            whyBestIsStronger = o.optString("whyBestIsStronger").ifBlank { "Best line retains optimal piece coordination." },
            whatWasMissed = o.optString("whatWasMissed").ifBlank { "No missed tactics flagged." },
            improvementTip = o.optString("improvementTip").ifBlank { "Double-check forcing moves before committing." },
            providerLabel = providerLabel
        )
    }

    private fun parseGameSummaryJson(jsonString: String, providerLabel: String): AiGameSummaryReport {
        val cleaned = extractJsonObjectString(jsonString)
        val o = JSONObject(cleaned)
        val sArr = o.optJSONArray("strengths") ?: JSONArray()
        val wArr = o.optJSONArray("weaknesses") ?: JSONArray()
        return AiGameSummaryReport(
            openingInsight = o.optString("openingInsight"),
            middlegameInsight = o.optString("middlegameInsight"),
            endgameInsight = o.optString("endgameInsight"),
            strengths = List(sArr.length()) { sArr.optString(it) }.filter { it.isNotBlank() },
            weaknesses = List(wArr.length()) { wArr.optString(it) }.filter { it.isNotBlank() },
            trainingFocus = o.optString("trainingFocus"),
            providerLabel = providerLabel
        )
    }

    private fun extractJsonObjectString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val first = trimmed.indexOf('{')
        val last = trimmed.lastIndexOf('}')
        return if (first >= 0 && last > first) trimmed.substring(first, last + 1) else trimmed
    }

    private suspend fun callOpenAiCompatibleChat(
        url: String,
        apiKey: String?,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        jsonMode: Boolean,
        timeoutMs: Long
    ): String = withContext(Dispatchers.IO) {
        withTimeout(timeoutMs) {
            val payload = JSONObject().apply {
                put("model", model)
                put("temperature", 0.35)
                if (jsonMode) {
                    put("response_format", JSONObject().apply { put("type", "json_object") })
                }
                put(
                    "messages",
                    JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        })
                    }
                )
            }.toString()

            val reqBuilder = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))

            if (!apiKey.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer $apiKey")
            }

            httpClient.newCall(reqBuilder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException("LLM API error HTTP ${response.code} at $url")
                }
                val trimmed = body.trim()
                val content = if (trimmed.startsWith("{")) {
                    val root = JSONObject(trimmed)
                    if (root.has("choices")) {
                        root.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                    } else {
                        trimmed
                    }
                } else {
                    trimmed
                }
                if (content.isBlank()) throw IllegalStateException("LLM API returned empty text")
                content
            }
        }
    }
}
