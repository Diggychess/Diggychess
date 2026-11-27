package com.example.chess

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

enum class AnalysisDepthPreset(
    val id: String,
    val label: String,
    val depth: Int,
    val subtitle: String
) {
    FAST("fast", "Fast 12", 12, "Default mobile speed"),
    NORMAL("normal", "Normal 18", 18, "Strong tactical precision"),
    DEEP("deep", "Deep 22", 22, "Deep master analysis");

    companion object {
        fun fromDepth(depth: Int): AnalysisDepthPreset = when {
            depth >= 21 -> DEEP
            depth >= 15 -> NORMAL
            else -> FAST
        }
    }
}

sealed interface StockfishEngineStatus {
    data object Loading : StockfishEngineStatus
    data class Ready(
        val version: String = "19.0.1",
        val engineName: String = "Stockfish 19 Lite WASM"
    ) : StockfishEngineStatus
    data class Error(val message: String) : StockfishEngineStatus
}

interface StockfishAnalyzerEngine {
    val statusFlow: StateFlow<StockfishEngineStatus>
    val maxParallelism: Int get() = 1
    suspend fun ensureReady(): Boolean
    suspend fun evaluateMultiPv(
        fen: String,
        depth: Int,
        multiPv: Int = 3,
        timeLimitMs: Long? = null
    ): EngineEval?
    suspend fun evaluateBatchPosition(
        fen: String,
        depth: Int,
        multiPv: Int = 1,
        movetimeMs: Long? = null
    ): EngineEval? = evaluateMultiPv(fen, depth, multiPv, movetimeMs)
    suspend fun playBotMoveWithSkill(
        fen: String,
        difficulty: EngineDifficulty
    ): EngineEval? = null
    fun prepareNewGame() {}
    fun stopSearch() {}
    fun retryInit() {}
    fun release() {}
}

/**
 * Pure-Kotlin UCI `info` / `bestmove` parser with White-POV score normalization.
 */
object UciParser {

    data class ParsedInfoLine(
        val depth: Int,
        val multiPvRank: Int,
        val cpWhitePov: Double,
        val mateWhitePov: Int?,
        val pvUci: List<String>
    )

    fun parseInfoLine(line: String, sideToMoveIsBlack: Boolean): ParsedInfoLine? {
        if (!line.startsWith("info ")) return null
        val tokens = line.trim().split(Regex("\\s+"))
        if ("pv" !in tokens || "score" !in tokens) return null

        var depth = 0
        var multiPv = 1
        var cpCentipawns: Int? = null
        var mateVal: Int? = null
        var pvStart = -1

        var i = 1
        while (i < tokens.size) {
            when (tokens[i]) {
                "depth" -> {
                    depth = tokens.getOrNull(i + 1)?.toIntOrNull() ?: depth
                    i += 2
                }
                "multipv" -> {
                    multiPv = tokens.getOrNull(i + 1)?.toIntOrNull() ?: 1
                    i += 2
                }
                "score" -> {
                    val kind = tokens.getOrNull(i + 1)
                    val rawNum = tokens.getOrNull(i + 2)?.toIntOrNull()
                    if (kind == "cp" && rawNum != null) {
                        cpCentipawns = rawNum
                        i += 3
                    } else if (kind == "mate" && rawNum != null) {
                        mateVal = rawNum
                        i += 3
                    } else {
                        i += 2
                    }
                    // Skip lowerbound / upperbound markers
                    val nextTok = tokens.getOrNull(i)
                    if (nextTok == "lowerbound" || nextTok == "upperbound") {
                        i += 1
                    }
                }
                "pv" -> {
                    pvStart = i + 1
                    break
                }
                else -> i++
            }
        }

        if (pvStart < 0 || pvStart >= tokens.size) return null
        val pv = tokens.subList(pvStart, tokens.size).filter { it.length >= 4 }
        if (pv.isEmpty()) return null

        val sign = if (sideToMoveIsBlack) -1 else 1
        val mateWhite = mateVal?.let { it * sign }
        val cpWhite = when {
            mateWhite != null -> if (mateWhite > 0) 99.0 else -99.0
            cpCentipawns != null -> ((cpCentipawns * sign) / 100.0).coerceIn(-99.0, 99.0)
            else -> return null
        }

        return ParsedInfoLine(
            depth = depth,
            multiPvRank = multiPv.coerceAtLeast(1),
            cpWhitePov = cpWhite,
            mateWhitePov = mateWhite,
            pvUci = pv
        )
    }

    fun parseBestMoveLine(line: String): String? {
        if (!line.startsWith("bestmove ")) return null
        val tok = line.trim().split(Regex("\\s+")).getOrNull(1)
        return tok?.takeIf { it.isNotBlank() && it != "(none)" }
    }

    fun buildEngineEval(
        fen: String,
        collectedLines: Map<Int, ParsedInfoLine>,
        bestMoveUci: String?,
        multiPv: Int,
        engineName: String = "Stockfish 19 Lite WASM"
    ): EngineEval {
        val variations = collectedLines.values
            .sortedBy { it.multiPvRank }
            .take(multiPv.coerceIn(1, 3))
            .mapIndexed { idx, parsed ->
                val board = ChessBoard(fen)
                val pvSan = mutableListOf<String>()
                for (uci in parsed.pvUci.take(8)) {
                    if (board.makeUciMove(uci) == null) break
                    pvSan.add(board.sanHistory().last())
                }
                val firstUci = parsed.pvUci.first()
                EngineVariation(
                    rank = idx + 1,
                    cp = parsed.cpWhitePov,
                    mate = parsed.mateWhitePov,
                    firstUci = firstUci,
                    firstSan = pvSan.firstOrNull() ?: firstUci,
                    pvUci = parsed.pvUci.take(8),
                    pvSan = pvSan,
                    depth = parsed.depth
                )
            }

        val top = variations.firstOrNull()
        val resolvedBest = bestMoveUci ?: top?.firstUci
        return EngineEval(
            cp = top?.cp ?: 0.0,
            mate = top?.mate,
            pvUci = top?.pvUci ?: listOfNotNull(resolvedBest),
            depth = top?.depth ?: 0,
            bestUci = resolvedBest,
            sourceLabel = engineName,
            candidateMoves = variations.map { it.firstUci },
            topLines = variations
        )
    }
}

/**
 * Executes a parallel pool of single-threaded Stockfish 19 Lite WASM Web Workers
 * inside a headless Android WebView served from cached RAM assets via
 * `https://appassets.androidplatform.net/assets/...`.
 */
class StockfishWasmEngine(
    context: Context
) : StockfishAnalyzerEngine {

    private val appContext = context.applicationContext ?: context
    private val mainHandler = Handler(Looper.getMainLooper())
    private val released = AtomicBoolean(false)

    private val _statusFlow = MutableStateFlow<StockfishEngineStatus>(StockfishEngineStatus.Loading)
    override val statusFlow: StateFlow<StockfishEngineStatus> = _statusFlow.asStateFlow()

    override val maxParallelism: Int
        get() = Runtime.getRuntime().availableProcessors().let { cores ->
            when {
                cores >= 6 -> 4
                cores >= 4 -> 3
                else -> 2
            }
        }

    @Volatile
    private var webView: WebView? = null

    @Volatile
    private var engineVersionName: String = "Stockfish 19 Lite WASM"

    @Volatile
    private var initDeferred = CompletableDeferred<Boolean>()

    private val nextJobId = java.util.concurrent.atomic.AtomicInteger(1)
    private val activeJobs = ConcurrentHashMap<Int, ActiveSearchJob>()
    private val registeredWorkers = ConcurrentHashMap.newKeySet<Int>()
    private val freeWorkerChannel = kotlinx.coroutines.channels.Channel<Int>(capacity = 8)
    private val assetCache = ConcurrentHashMap<String, ByteArray>()

    private data class ActiveSearchJob(
        val jobId: Int,
        val fen: String,
        val sideToMoveIsBlack: Boolean,
        val multiPv: Int,
        val completion: CompletableDeferred<EngineEval> = CompletableDeferred()
    )

    init {
        preloadAssets()
        initHost()
    }

    private fun preloadAssets() {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            listOf(
                "stockfish/engine.html",
                "stockfish/stockfish-19-lite-single.js",
                "stockfish/stockfish-19-lite-single.wasm"
            ).forEach { path ->
                runCatching {
                    appContext.assets.open(path).use { it.readBytes() }
                }.onSuccess { bytes ->
                    assetCache[path] = bytes
                }
            }
        }
    }

    override fun retryInit() {
        if (released.get()) return
        _statusFlow.value = StockfishEngineStatus.Loading
        initDeferred = CompletableDeferred()
        registeredWorkers.clear()
        while (freeWorkerChannel.tryReceive().isSuccess) { /* drain */ }
        activeJobs.values.forEach { it.completion.cancel() }
        activeJobs.clear()
        mainHandler.post {
            val wv = webView
            if (wv == null) {
                createAndLoadWebView()
            } else {
                wv.evaluateJavascript("window.restartEngine && window.restartEngine();", null)
            }
        }
    }

    private fun initHost() {
        mainHandler.post { createAndLoadWebView() }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    private fun createAndLoadWebView() {
        if (released.get()) return
        try {
            val wv = WebView(appContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        val url = request?.url ?: return null
                        if (url.host != "appassets.androidplatform.net") return null
                        val path = url.path ?: return null
                        if (!path.startsWith("/assets/")) return null

                        val assetRelative = path.removePrefix("/assets/").trimStart('/')
                        val cacheHeader = if (assetRelative.endsWith(".html", ignoreCase = true)) {
                            "no-cache"
                        } else {
                            "public, max-age=86400"
                        }
                        val corsHeaders = mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Access-Control-Allow-Methods" to "GET, OPTIONS",
                            "Cache-Control" to cacheHeader
                        )
                        if (request.method.equals("OPTIONS", ignoreCase = true)) {
                            return WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                200,
                                "OK",
                                corsHeaders,
                                ByteArrayInputStream(ByteArray(0))
                            )
                        }

                        val mime = when {
                            assetRelative.endsWith(".wasm", ignoreCase = true) -> "application/wasm"
                            assetRelative.endsWith(".js", ignoreCase = true) -> "application/javascript"
                            assetRelative.endsWith(".html", ignoreCase = true) -> "text/html"
                            else -> "application/octet-stream"
                        }
                        return try {
                            val bytes = assetCache[assetRelative] ?: appContext.assets.open(assetRelative).use {
                                it.readBytes()
                            }.also { loaded ->
                                assetCache[assetRelative] = loaded
                            }
                            WebResourceResponse(mime, "UTF-8", 200, "OK", corsHeaders, ByteArrayInputStream(bytes))
                        } catch (e: Exception) {
                            WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                404,
                                "Not Found",
                                corsHeaders,
                                ByteArrayInputStream((e.message ?: "Missing asset").toByteArray())
                            )
                        }
                    }
                }
                addJavascriptInterface(UciBridge(), "AndroidStockfish")
            }
            webView = wv
            wv.loadUrl("https://appassets.androidplatform.net/assets/stockfish/engine.html")
        } catch (e: Exception) {
            val msg = "Failed to initialize Stockfish WebView: ${e.message ?: "Unavailable"}"
            _statusFlow.value = StockfishEngineStatus.Error(msg)
            initDeferred.complete(false)
        }
    }

    private inner class UciBridge {
        @JavascriptInterface
        fun onUciLine(rawLine: String?) {
            val line = rawLine?.trim().orEmpty()
            if (line.isEmpty()) return

            when {
                line.startsWith("id name ") -> {
                    engineVersionName = line.removePrefix("id name ").trim()
                        .ifBlank { "Stockfish 19 Lite WASM" }
                }
                line == "readyok" -> {
                    _statusFlow.value = StockfishEngineStatus.Ready(
                        version = "19.0.1",
                        engineName = engineVersionName
                    )
                    initDeferred.complete(true)
                }
            }
        }

        @JavascriptInterface
        fun onWorkerReady(workerIndex: Int) {
            if (workerIndex == 0) {
                _statusFlow.value = StockfishEngineStatus.Ready(
                    version = "19.0.1",
                    engineName = engineVersionName
                )
                initDeferred.complete(true)
            }
            if (registeredWorkers.add(workerIndex)) {
                freeWorkerChannel.trySend(workerIndex)
            }
        }

        @JavascriptInterface
        fun onSearchResult(jobId: Int, @Suppress("UNUSED_PARAMETER") workerIndex: Int, bestMoveLine: String?, infoBlock: String?) {
            val job = activeJobs.remove(jobId) ?: return
            if (job.completion.isCompleted) return

            val linesByRank = HashMap<Int, UciParser.ParsedInfoLine>(4)
            infoBlock?.lineSequence()?.forEach { raw ->
                val parsed = UciParser.parseInfoLine(raw, job.sideToMoveIsBlack)
                if (parsed != null) {
                    val existing = linesByRank[parsed.multiPvRank]
                    if (existing == null || parsed.depth >= existing.depth) {
                        linesByRank[parsed.multiPvRank] = parsed
                    }
                }
            }
            val best = UciParser.parseBestMoveLine(bestMoveLine.orEmpty())
            val eval = UciParser.buildEngineEval(
                fen = job.fen,
                collectedLines = linesByRank,
                bestMoveUci = best,
                multiPv = job.multiPv,
                engineName = engineVersionName
            )
            job.completion.complete(eval)
        }

        @JavascriptInterface
        fun onSearchFailed(jobId: Int, @Suppress("UNUSED_PARAMETER") workerIndex: Int, message: String?) {
            val job = activeJobs.remove(jobId) ?: return
            if (!job.completion.isCompleted) {
                job.completion.completeExceptionally(
                    IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "Stockfish search failed.")
                )
            }
        }

        @JavascriptInterface
        fun onEngineError(message: String?) {
            val clean = message?.takeIf { it.isNotBlank() } ?: "Stockfish WASM worker failed to load."
            _statusFlow.value = StockfishEngineStatus.Error(clean)
            initDeferred.complete(false)
            activeJobs.values.forEach {
                it.completion.completeExceptionally(IllegalStateException(clean))
            }
            activeJobs.clear()
        }
    }

    private fun evalJs(script: String) {
        if (released.get()) return
        mainHandler.post {
            webView?.evaluateJavascript(script, null)
        }
    }

    override suspend fun ensureReady(): Boolean {
        if (released.get()) return false
        if (_statusFlow.value is StockfishEngineStatus.Ready) return true
        val ok = withTimeoutOrNull(25_000L) {
            initDeferred.await()
        }
        if (ok == null && _statusFlow.value is StockfishEngineStatus.Loading) {
            _statusFlow.value = StockfishEngineStatus.Error("Stockfish WASM timed out while loading.")
            initDeferred.complete(false)
            return false
        }
        return ok == true
    }

    override fun prepareNewGame() {
        if (_statusFlow.value is StockfishEngineStatus.Ready) {
            evalJs("window.preparePoolNewGame && window.preparePoolNewGame();")
        }
    }

    override fun stopSearch() {
        evalJs("window.stopAllSearches && window.stopAllSearches();")
    }

    override suspend fun evaluateMultiPv(
        fen: String,
        depth: Int,
        multiPv: Int,
        timeLimitMs: Long?
    ): EngineEval? = evaluateBatchPosition(
        fen = fen,
        depth = depth,
        multiPv = multiPv,
        movetimeMs = timeLimitMs ?: when {
            depth >= 22 -> 1800L
            depth >= 18 -> 1100L
            depth >= 12 -> 750L
            else -> 450L
        }
    )

    override suspend fun evaluateBatchPosition(
        fen: String,
        depth: Int,
        multiPv: Int,
        movetimeMs: Long?
    ): EngineEval? = runWasmSearch(
        fen = fen,
        depth = depth,
        multiPv = multiPv,
        movetimeMs = movetimeMs,
        skillLevel = 20
    )

    override suspend fun playBotMoveWithSkill(
        fen: String,
        difficulty: EngineDifficulty
    ): EngineEval? {
        val (depth, budgetMs) = when (difficulty) {
            EngineDifficulty.EASY -> 5 to 240L
            EngineDifficulty.MEDIUM -> 9 to 420L
            EngineDifficulty.HARD -> 13 to 680L
            EngineDifficulty.EXPERT -> 16 to 950L
        }
        return runWasmSearch(
            fen = fen,
            depth = depth,
            multiPv = 1,
            movetimeMs = budgetMs,
            skillLevel = difficulty.skillLevel
        )
    }

    private suspend fun runWasmSearch(
        fen: String,
        depth: Int,
        multiPv: Int,
        movetimeMs: Long?,
        skillLevel: Int
    ): EngineEval? {
        val board = ChessBoard(fen)
        if (board.isGameOver()) {
            return if (board.inCheckmate()) {
                val cp = if (board.turn == PieceColor.WHITE) -99.0 else 99.0
                EngineEval(cp = cp, mate = 0, sourceLabel = engineVersionName)
            } else {
                EngineEval(cp = 0.0, mate = null, sourceLabel = engineVersionName)
            }
        }

        if (!ensureReady()) return null

        val safeDepth = depth.coerceIn(1, 24)
        val safeMultiPv = multiPv.coerceIn(1, 3)
        val safeSkill = skillLevel.coerceIn(0, 20)
        val budgetMs = (movetimeMs ?: when {
            safeDepth >= 22 -> 1600L
            safeDepth >= 18 -> 900L
            safeDepth >= 12 -> 420L
            else -> 250L
        }).coerceIn(50L, 25_000L)

        val hardTimeout = (budgetMs + 6_000L).coerceAtLeast(8_000L)
        val workerId = freeWorkerChannel.receive()
        val jobId = nextJobId.incrementAndGet()
        val job = ActiveSearchJob(
            jobId = jobId,
            fen = fen,
            sideToMoveIsBlack = board.turn == PieceColor.BLACK,
            multiPv = safeMultiPv
        )
        activeJobs[jobId] = job

        return try {
            val quotedFen = JSONObject.quote(fen)
            evalJs("window.runSearch && window.runSearch($jobId, $workerId, $quotedFen, $safeDepth, $safeMultiPv, $budgetMs, $safeSkill);")
            withTimeoutOrNull(hardTimeout) {
                job.completion.await()
            }
        } catch (_: Exception) {
            null
        } finally {
            activeJobs.remove(jobId)
            freeWorkerChannel.trySend(workerId)
        }
    }

    override fun release() {
        if (!released.compareAndSet(false, true)) return
        activeJobs.values.forEach { it.completion.cancel() }
        activeJobs.clear()
        freeWorkerChannel.close()
        mainHandler.post {
            try {
                webView?.evaluateJavascript("window.sendUci && window.sendUci('quit');", null)
                webView?.destroy()
            } catch (_: Exception) {
            } finally {
                webView = null
            }
        }
    }
}
