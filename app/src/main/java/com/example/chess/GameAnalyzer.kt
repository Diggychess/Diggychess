package com.example.chess

import com.example.ui.theme.ClassBook
import com.example.ui.theme.ClassGreat
import com.example.ui.theme.ClassMiss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

class GameAnalyzer(
    private val engine: ChessEngine = ChessEngine(),
    private val stockfishWasm: StockfishAnalyzerEngine? = null
) {

    suspend fun analyze(
        pgnText: String,
        settings: ReviewSettings,
        savedEntry: SavedGameEntry? = null,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): GameAnalysisResult = withContext(Dispatchers.Default) {
        val parsed = PgnParser.parse(pgnText)
        require(parsed.plies.isNotEmpty()) {
            "Could not read this PGN. Paste the full PGN, including the headers."
        }

        val plies = parsed.plies.toMutableList()
        var usingSavedOrPgn = false
        val evalPerFen = arrayOfNulls<EngineEval>(plies.size + 1)
        var engineLabel = "PGN evaluations"

        if (savedEntry != null && savedEntry.pgn == pgnText && savedEntry.evals.size == plies.size) {
            usingSavedOrPgn = true
            engineLabel = "Saved analysis"
            for (i in plies.indices) {
                plies[i] = plies[i].copy(
                    eval = savedEntry.evals[i],
                    bestSan = savedEntry.bestMoves.getOrNull(i)
                )
            }
        } else {
            val hasEnoughPgnEvals = plies.count { it.eval != null } >= plies.size * 0.5
            if (!settings.forceReanalyze && hasEnoughPgnEvals) {
                usingSavedOrPgn = true
                engineLabel = "PGN evaluations"
            }
        }

        val plyBookEntries = Array<OpeningEntry?>(plies.size) { i ->
            if (i < 30) OpeningBook.lookup(plies[i].fenAfter) else null
        }

        val requestedDepth = settings.depth.coerceIn(4, 24)
        var effectiveDepth = savedEntry?.depth ?: requestedDepth

        if (!usingSavedOrPgn) {
            engineLabel = "Chess Analyzer Stockfish Engine"
            effectiveDepth = requestedDepth
            val startFen = parsed.headers["FEN"]?.takeIf { it.isNotBlank() } ?: ChessBoard.START_FEN
            val fens = listOf(startFen) + plies.map { it.fenAfter }
            val cache = ConcurrentHashMap<String, EngineEval>()

            // Deduplicate identical positions (transpositions / repetitions) before dispatching
            val uniqueFens = LinkedHashMap<String, String>()
            val uniqueNeedsFull = HashMap<String, Boolean>()
            val fenKeys = fens.mapIndexed { idx, fen ->
                val key = fen.split(' ').take(4).joinToString(" ")
                uniqueFens.putIfAbsent(key, fen)
                val needsFull = (idx > 0 && plyBookEntries[idx - 1] == null) ||
                    (idx < plies.size && plyBookEntries[idx] == null)
                if (needsFull || !uniqueNeedsFull.containsKey(key)) {
                    uniqueNeedsFull[key] = needsFull || (uniqueNeedsFull[key] == true)
                }
                key
            }

            val total = uniqueFens.size
            onProgress(0, total)

            val wasm = stockfishWasm
            if (wasm != null) {
                if (!wasm.ensureReady()) {
                    val errMsg = (wasm.statusFlow.value as? StockfishEngineStatus.Error)?.message
                        ?: "Stockfish 19 WASM engine failed to load."
                    throw IllegalStateException(errMsg)
                }
                wasm.prepareNewGame()

                val fullMovetimeMs = when {
                    requestedDepth >= 22 -> 1500L
                    requestedDepth >= 18 -> 850L
                    requestedDepth >= 12 -> 420L
                    else -> 260L
                }
                val parallelism = wasm.maxParallelism.coerceIn(1, 4)
                val semaphore = Semaphore(permits = parallelism)
                val completed = AtomicInteger(0)

                coroutineScope {
                    uniqueFens.entries.map { (key, fen) ->
                        async {
                            val isFull = uniqueNeedsFull[key] ?: true
                            val targetPosDepth = if (isFull) requestedDepth else minOf(requestedDepth, 8)
                            val targetBudgetMs = if (isFull) fullMovetimeMs else 80L
                            val ev = semaphore.withPermit {
                                currentCoroutineContext().ensureActive()
                                wasm.evaluateBatchPosition(
                                    fen = fen,
                                    depth = targetPosDepth,
                                    multiPv = 1,
                                    movetimeMs = targetBudgetMs
                                )
                            } ?: run {
                                val errMsg = (wasm.statusFlow.value as? StockfishEngineStatus.Error)?.message
                                    ?: "Stockfish engine stopped unexpectedly while analysing."
                                throw IllegalStateException(errMsg)
                            }
                            cache[key] = ev
                            onProgress(completed.incrementAndGet(), total)
                        }
                    }.awaitAll()
                }

                // Fast targeted Pass 2 (MultiPV 2) on non-book BEST-move candidates for Great (!) badges
                val greatCandidateKeys = LinkedHashMap<String, String>()
                for (i in plies.indices) {
                    if (plyBookEntries[i] != null) continue
                    val evBefore = cache[fenKeys[i]] ?: continue
                    if (evBefore.topLines.size >= 2) continue
                    val evAfter = cache[fenKeys[i + 1]] ?: continue
                    val isW = i % 2 == 0
                    var wpB = MoveExplainer.winProbability(evBefore.cp)
                    var wpA = MoveExplainer.winProbability(evAfter.cp)
                    if (!isW) {
                        wpB = 100.0 - wpB
                        wpA = 100.0 - wpA
                    }
                    if (maxOf(0.0, wpB - wpA) <= 0.5) {
                        greatCandidateKeys.putIfAbsent(fenKeys[i], fens[i])
                        if (greatCandidateKeys.size >= 12) break
                    }
                }
                if (greatCandidateKeys.isNotEmpty()) {
                    coroutineScope {
                        greatCandidateKeys.entries.map { (key, fen) ->
                            async {
                                val refined = semaphore.withPermit {
                                    currentCoroutineContext().ensureActive()
                                    wasm.evaluateBatchPosition(
                                        fen = fen,
                                        depth = requestedDepth,
                                        multiPv = 2,
                                        movetimeMs = 220L
                                    )
                                }
                                if (refined != null && refined.topLines.size >= 2) {
                                    val base = cache[key]
                                    if (base != null) {
                                        cache[key] = base.copy(topLines = refined.topLines)
                                    }
                                }
                            }
                        }.awaitAll()
                    }
                }
            } else {
                val timePerPos = (4500L / total.coerceAtLeast(1)).coerceIn(120L, 450L)
                val fallbackDepth = requestedDepth.coerceIn(4, 12)
                val semaphore = Semaphore(permits = 6)
                val completed = AtomicInteger(0)

                coroutineScope {
                    uniqueFens.entries.map { (key, fen) ->
                        async {
                            val ev = semaphore.withPermit {
                                engine.evaluateMultiPv(
                                    fen = fen,
                                    depth = fallbackDepth,
                                    multiPv = 3,
                                    timeLimitMs = timePerPos,
                                    allowCloud = true,
                                    quickBatch = true
                                )
                            }
                            cache[key] = ev
                            onProgress(completed.incrementAndGet(), total)
                        }
                    }.awaitAll()
                }
            }

            for (i in fens.indices) {
                evalPerFen[i] = cache[fenKeys[i]]
            }

            for (k in plies.indices) {
                val rAfter = evalPerFen[k + 1]!!
                val rBefore = evalPerFen[k]!!
                val bestUci = rBefore.bestUci
                var bestSan: String? = null
                if (!bestUci.isNullOrBlank() && bestUci != "(none)") {
                    val b = ChessBoard(fens[k])
                    val bm = b.makeUciMove(bestUci)
                    if (bm != null) {
                        val s = b.sanHistory().last()
                        if (s != plies[k].san) bestSan = s
                    }
                }
                plies[k] = plies[k].copy(
                    eval = rAfter.cp,
                    mate = rAfter.mate,
                    bestSan = bestSan
                )
            }
        }

        val evals = mutableListOf(evalPerFen[0]?.cp ?: 0.2)
        for (i in plies.indices) {
            evals.add(plies[i].eval ?: evals[i])
        }

        val inacTh = settings.inaccuracyThreshold.toDouble()
        val misTh = settings.mistakeThreshold.toDouble()
        val blunTh = settings.blunderThreshold.toDouble()

        val analyzedPlies = mutableListOf<AnalyzedPly>()
        val usedPhrases = mutableSetOf<String>()
        val whiteAccList = mutableListOf<Double>()
        val blackAccList = mutableListOf<Double>()
        val whiteCounts = MoveClass.entries.associateWith { 0 }.toMutableMap()
        val blackCounts = MoveClass.entries.associateWith { 0 }.toMutableMap()

        for (i in plies.indices) {
            val p = plies[i]
            val isWhite = i % 2 == 0
            var wpBefore = MoveExplainer.winProbability(evals[i])
            var wpAfter = MoveExplainer.winProbability(evals[i + 1])
            if (!isWhite) {
                wpBefore = 100.0 - wpBefore
                wpAfter = 100.0 - wpAfter
            }

            var loss = maxOf(0.0, wpBefore - wpAfter)
            if (!usingSavedOrPgn && p.bestSan == null && p.eval != null) {
                loss = 0.0
            }

            val opening = plyBookEntries[i]
            val isBook = opening != null
            if (isBook) {
                loss = 0.0
            }

            var cls = when {
                loss <= 0.5 -> MoveClass.BEST
                loss <= 2.5 -> MoveClass.EXCELLENT
                loss <= inacTh -> MoveClass.GOOD
                loss <= misTh -> MoveClass.INACCURACY
                loss <= blunTh -> MoveClass.MISTAKE
                else -> MoveClass.BLUNDER
            }

            if (cls == MoveClass.BEST && !isBook && p.move.piece != PieceType.PAWN) {
                val bh = MoveExplainer.hangs(p.fenAfter)
                val sideEvalAfter = if (isWhite) evals[i + 1] else -evals[i + 1]
                if (bh != null && bh.move.to == p.move.to && bh.netGain >= 3 && sideEvalAfter > -1.0) {
                    cls = MoveClass.BRILLIANT
                }
            }

            val topLines = evalPerFen[i]?.topLines ?: emptyList()

            // Great Move (!): Best non-book move where 2nd-best line drops >= 12 win%
            var isGreatMove = false
            if (cls == MoveClass.BEST && !isBook && topLines.size >= 2) {
                var wp1 = MoveExplainer.winProbability(topLines[0].cp)
                var wp2 = MoveExplainer.winProbability(topLines[1].cp)
                if (!isWhite) {
                    wp1 = 100.0 - wp1
                    wp2 = 100.0 - wp2
                }
                if (wp1 - wp2 >= 12.0) {
                    isGreatMove = true
                }
            }

            // Miss (✖): Had strong winning edge (>= 72 win%) and threw >= 15 win% (or missed forced mate)
            val isMissedWin = !isBook &&
                (cls == MoveClass.MISTAKE || cls == MoveClass.BLUNDER) &&
                wpBefore >= 72.0 && loss >= 15.0

            val acc = MoveExplainer.moveAccuracy(loss)
            if (isWhite) {
                whiteAccList.add(acc)
                whiteCounts[cls] = (whiteCounts[cls] ?: 0) + 1
            } else {
                blackAccList.add(acc)
                blackCounts[cls] = (blackCounts[cls] ?: 0) + 1
            }

            var bestFrom: String? = null
            var bestTo: String? = null
            if (p.bestSan != null) {
                val b0 = ChessBoard(p.fenBefore)
                val r0 = b0.makeSanMove(p.bestSan)
                if (r0 != null) {
                    bestFrom = r0.fromName
                    bestTo = r0.toName
                }
            }

            val pvUci = evalPerFen[i]?.pvUci ?: emptyList()
            val nextPvUci = evalPerFen.getOrNull(i + 1)?.pvUci ?: emptyList()
            val prevSideMove = if (i >= 2) plies[i - 2].move else null

            val explanation = MoveExplainer.explainMove(
                plyIdx = i,
                san = p.san,
                move = p.move,
                fenBefore = p.fenBefore,
                fenAfter = p.fenAfter,
                evalBefore = evals[i],
                evalAfter = evals[i + 1],
                mateAfter = p.mate,
                moveClass = cls,
                isBook = isBook,
                opening = opening,
                bestSan = p.bestSan,
                pvUci = pvUci,
                nextPvUci = nextPvUci,
                prevSideMove = prevSideMove,
                usedPhrases = usedPhrases
            )

            val bestExplanation = MoveExplainer.explainBestAlternative(
                plyIdx = i,
                san = p.san,
                move = p.move,
                moveClass = cls,
                fenBefore = p.fenBefore,
                bestSan = p.bestSan,
                pvUci = pvUci,
                prevSideMove = prevSideMove
            )

            val coachInsights = MoveExplainer.buildCoachInsights(
                plyIdx = i,
                san = p.san,
                move = p.move,
                fenBefore = p.fenBefore,
                fenAfter = p.fenAfter,
                moveClass = cls,
                isBook = isBook,
                isGreatMove = isGreatMove,
                isMissedWin = isMissedWin,
                opening = opening,
                bestSan = p.bestSan,
                topLines = topLines
            )

            val (badgeLabel, badgeGlyph, badgeColor) = when {
                isBook -> Triple("Book", "B", ClassBook)
                cls == MoveClass.BRILLIANT -> Triple("Brilliant", "!!", cls.color)
                isGreatMove -> Triple("Great", "!", ClassGreat)
                isMissedWin -> Triple("Miss", "✖", ClassMiss)
                else -> Triple(cls.label, cls.glyph, cls.color)
            }

            analyzedPlies.add(
                AnalyzedPly(
                    index = i,
                    san = p.san,
                    move = p.move,
                    fenBefore = p.fenBefore,
                    fenAfter = p.fenAfter,
                    evalBefore = evals[i],
                    evalAfter = evals[i + 1],
                    mate = p.mate,
                    loss = loss,
                    accuracy = acc,
                    moveClass = cls,
                    isBook = isBook,
                    opening = opening,
                    bestSan = p.bestSan,
                    bestFrom = bestFrom,
                    bestTo = bestTo,
                    pvUci = pvUci,
                    explanation = explanation,
                    bestExplanation = bestExplanation,
                    topLines = topLines,
                    coachHeadline = coachInsights.headline,
                    badgeLabel = badgeLabel,
                    badgeGlyph = badgeGlyph,
                    badgeColor = badgeColor,
                    tacticalTags = coachInsights.tacticalTags,
                    winProbSwing = wpAfter - wpBefore,
                    isGreatMove = isGreatMove,
                    isMissedWin = isMissedWin
                )
            )
        }

        val openingSummary = OpeningBook.summarize(analyzedPlies, evals)

        fun avg(list: List<Double>): Double = if (list.isEmpty()) 100.0 else list.average()
        val phaseTags = IntArray(analyzedPlies.size) { idx ->
            val ply = analyzedPlies[idx]
            if (ply.index < 20) 0 else {
                val placement = ply.fenAfter.substringBefore(' ')
                var nonPawns = 0
                for (ch in placement) {
                    if (ch in "qrbnQRBN") nonPawns++
                }
                if (nonPawns <= 6) 2 else 1
            }
        }

        fun phaseAccuracy(isW: Boolean, phase: Int): String {
            val accs = analyzedPlies.filterIndexed { idx, it ->
                (it.index % 2 == 0) == isW && phaseTags[idx] == phase
            }.map { it.accuracy }
            return if (accs.isEmpty()) "-" else String.format(java.util.Locale.US, "%.1f%%", accs.average())
        }

        val whiteName = parsed.headers["White"]?.takeIf { it.isNotBlank() } ?: "White"
        val blackName = parsed.headers["Black"]?.takeIf { it.isNotBlank() } ?: "Black"

        val summaryStats = GameSummaryStats(
            whiteName = whiteName,
            blackName = blackName,
            whiteElo = parsed.headers["WhiteElo"]?.takeIf { it.isNotBlank() },
            blackElo = parsed.headers["BlackElo"]?.takeIf { it.isNotBlank() },
            result = parsed.headers["Result"]?.takeIf { it.isNotBlank() },
            whiteAccuracy = avg(whiteAccList),
            blackAccuracy = avg(blackAccList),
            whiteCounts = whiteCounts,
            blackCounts = blackCounts,
            openingStats = PhaseStats(phaseAccuracy(true, 0), phaseAccuracy(false, 0)),
            middlegameStats = PhaseStats(phaseAccuracy(true, 1), phaseAccuracy(false, 1)),
            endgameStats = PhaseStats(phaseAccuracy(true, 2), phaseAccuracy(false, 2))
        )

        val keyMistakes = analyzedPlies
            .filter { it.moveClass == MoveClass.MISTAKE || it.moveClass == MoveClass.BLUNDER }
            .sortedByDescending { it.loss }
            .take(5)

        val id = computeGameId(analyzedPlies)
        val hasNames = !parsed.headers["White"].isNullOrBlank() || !parsed.headers["Black"].isNullOrBlank()
        val title = if (hasNames) "$whiteName vs $blackName" else {
            "Game " + analyzedPlies.take(3).joinToString(" ") { it.san }
        }

        GameAnalysisResult(
            id = id,
            title = title,
            pgn = pgnText,
            headers = parsed.headers,
            plies = analyzedPlies,
            evals = evals,
            openingSummary = openingSummary,
            summaryStats = summaryStats,
            keyMistakes = keyMistakes,
            engineLabel = engineLabel,
            depth = effectiveDepth
        )
    }

    fun toSavedGameEntry(result: GameAnalysisResult): SavedGameEntry {
        val wCounts = IntArray(MoveClass.entries.size)
        val bCounts = IntArray(MoveClass.entries.size)
        val wPh = intArrayOf(0, 0, 0)
        val bPh = intArrayOf(0, 0, 0)
        val mistakes = mutableListOf<Puzzle>()

        for (p in result.plies) {
            val isWhite = p.index % 2 == 0
            val counts = if (isWhite) wCounts else bCounts
            val ph = if (isWhite) wPh else bPh
            counts[p.moveClass.ordinal]++
            if (p.moveClass == MoveClass.MISTAKE || p.moveClass == MoveClass.BLUNDER) {
                val phIdx = when {
                    p.index < 20 -> 0
                    p.index < 60 -> 1
                    else -> 2
                }
                ph[phIdx]++
                val bestUci = p.pvUci.firstOrNull() ?: if (p.bestFrom != null && p.bestTo != null) {
                    p.bestFrom + p.bestTo
                } else null
                if (mistakes.size < 8 && bestUci != null) {
                    mistakes.add(
                        Puzzle(
                            id = "my-${result.id}-${p.index}",
                            fen0 = p.fenBefore,
                            setupUci = null,
                            fen = p.fenBefore,
                            solution = listOf(bestUci),
                            rating = 0,
                            themes = listOf("yourMistake"),
                            isMate = false,
                            source = "Your game, move ${p.index / 2 + 1} (${p.san})"
                        )
                    )
                }
            }
        }

        val gameResult = result.headers["Result"]?.takeIf {
            it in setOf("1-0", "0-1", "1/2-1/2", "*")
        } ?: "*"
        val opponentName = result.summaryStats.blackName.takeIf { it.isNotBlank() && it != "Black" } ?: ""

        return SavedGameEntry(
            id = result.id,
            title = result.title,
            timestamp = System.currentTimeMillis(),
            plies = result.plies.size,
            moves = ceil(result.plies.size / 2.0).toInt(),
            pgn = result.pgn,
            evals = result.evals.drop(1),
            bestMoves = result.plies.map { it.bestSan },
            report = PhaseReport(
                whiteCounts = wCounts.toList(),
                blackCounts = bCounts.toList(),
                whitePhaseMistakes = wPh.toList(),
                blackPhaseMistakes = bPh.toList()
            ),
            mistakes = mistakes,
            result = gameResult,
            opponent = opponentName,
            source = "analysed",
            depth = result.depth,
            whiteAccuracy = result.summaryStats.whiteAccuracy,
            blackAccuracy = result.summaryStats.blackAccuracy
        )
    }

    private fun computeGameId(plies: List<AnalyzedPly>): String {
        val t = plies.joinToString(" ") { it.san }
        var h = 5381
        for (ch in t) {
            h = ((h shl 5) + h + ch.code)
        }
        return "g" + java.lang.Integer.toUnsignedString(h, 36) + "-" + plies.size
    }
}
