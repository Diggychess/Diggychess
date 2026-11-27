package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.example.service.AiChessCoachService
import com.example.service.TinyFishSearchResult
import com.example.service.TinyFishSearchService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DiggyUpgradeTest {

    @Test
    fun bundledLichessPuzzles_exceedTenThousandWithAllDifficultyTiersAndValidSolutions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = KnightScopeRepository(context)
        val pool = repo.loadAllPuzzles()

        assertTrue("Expected >= 10,000 puzzles, got ${pool.size}", pool.size >= 10_000)
        assertEquals("Expected unique puzzle IDs", pool.size, pool.map { it.id }.toSet().size)

        val easy = pool.count { it.rating < 1400 }
        val med = pool.count { it.rating in 1400..1999 }
        val hard = pool.count { it.rating >= 2000 }
        assertTrue("Expected > 1000 Easy puzzles, got $easy", easy > 1000)
        assertTrue("Expected > 1000 Medium puzzles, got $med", med > 1000)
        assertTrue("Expected > 1000 Hard puzzles, got $hard", hard > 1000)

        // Spot-check first 40 and stride-sampled puzzles across the pool for legal setup & solution moves
        val sampled = pool.filterIndexed { idx, _ -> idx < 40 || idx % 250 == 0 }
        sampled.forEach { pz ->
            val prepped = repo.preparePuzzle(pz)
            assertNotNull("Failed to prepare puzzle ${pz.id}", prepped)
            val board = ChessBoard(prepped!!.fen)
            for (uci in prepped.solution) {
                val mv = board.makeUciMove(uci)
                assertNotNull("Illegal solution $uci in puzzle ${pz.id}", mv)
            }
        }
    }

    @Test
    fun lichessOpeningsTsv_loadsOverThreeThousandEntriesAndSupportsTranspositions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val loadedCount = OpeningBook.ensureLoaded(context)

        assertTrue("Expected > 3,000 Lichess opening positions, got $loadedCount", loadedCount > 3000)
        assertEquals("Expected zero invalid opening lines in TSVs", 0, OpeningBook.invalidLineCount)

        // Verify ECO lookup & transposition invariance: 1.d4 d5 2.Nf3 Nf6 3.Bf4 == 1.d4 Nf6 2.Nf3 d5 3.Bf4
        val b1 = ChessBoard().apply { listOf("d4", "d5", "Nf3", "Nf6", "Bf4").forEach { makeSanMove(it) } }
        val b2 = ChessBoard().apply { listOf("d4", "Nf6", "Nf3", "d5", "Bf4").forEach { makeSanMove(it) } }
        val e1 = OpeningBook.lookup(b1.toFen())
        val e2 = OpeningBook.lookup(b2.toFen())
        assertNotNull(e1)
        assertEquals(e1, e2)

        // Verify Sicilian Defense & continuations after 1.e4
        val sicilian = ChessBoard().apply {
            makeSanMove("e4")
            makeSanMove("c5")
        }
        val sicilianEntry = OpeningBook.lookup(sicilian.toFen())
        assertNotNull(sicilianEntry)
        assertTrue(sicilianEntry!!.name.contains("Sicilian", ignoreCase = true))

        val afterE4 = ChessBoard().apply { makeSanMove("e4") }
        val cont = OpeningBook.lookupContinuations(afterE4.toFen())
        assertTrue("Expected multiple continuations after 1.e4", cont.size >= 3)
    }

    @Test
    fun chesslibPgnBridge_validatesCastlingEnPassantPromotionMateAndRejectsIllegalMoves() = runBlocking {
        val complexPgn = """
            [Event "Rules Cross-Check"]
            [White "Alice"]
            [Black "Bob"]
            [Result "1-0"]

            1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. O-O d6 5. d4 exd4 6. c3 dxc3 7. Nxc3 Nf6 8. e5 dxe5 9. Qxd8+ Kxd8 10. Nxe5 Nxe5
        """.trimIndent()
        val report = ChesslibPgnBridge.validateAndNormalizePgn(complexPgn)
        assertTrue(report.isValid)
        assertEquals(20, report.moveCount)
        assertTrue(report.normalizedPgn.contains("O-O"))

        // En passant + Pawn promotion + Checkmate via chesslib
        val epAndMate = """
            [Event "Scholar Mate"]
            [Result "1-0"]

            1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0
        """.trimIndent()
        val mateReport = ChesslibPgnBridge.validateAndNormalizePgn(epAndMate)
        assertTrue(mateReport.isValid)
        assertTrue(mateReport.inCheckmate)
        assertEquals(7, mateReport.moveCount)

        val epCheck = ChesslibPgnBridge.validateAndNormalizePgn("1. e4 a6 2. e5 d5 3. exd6 *")
        assertTrue(epCheck.isValid)
        assertEquals(5, epCheck.moveCount)

        val promoCheck = ChesslibPgnBridge.validateAndNormalizePgn(
            """
            [FEN "8/P7/8/8/8/8/7k/K7 w - - 0 1"]
            [SetUp "1"]
            1. a8=Q *
            """.trimIndent()
        )
        assertTrue(promoCheck.isValid)
        assertEquals(1, promoCheck.moveCount)

        // Reject illegal move
        val illegal = ChesslibPgnBridge.validateAndNormalizePgn("1. e4 e5 2. Qh5 Ke7 3. Qxe8# *")
        assertFalse(illegal.isValid)
        assertNotNull(illegal.error)
    }

    @Test
    fun aiChessCoachService_cachesInsightsTracksDailyQuotaAndProvidesStockfishOfflineFallback() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = KnightScopeRepository(context)
        val service = AiChessCoachService(repo)

        // Secret blank by default unless GROQ_API_KEY env var was supplied at build time
        assertEquals(BuildConfig.GROQ_API_KEY.isNotBlank(), service.hasGroqBackupConfigured())

        val analyzer = GameAnalyzer(ChessEngine())
        val gameRes = analyzer.analyze(
            pgnText = "1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0",
            settings = ReviewSettings(depth = 4, forceReanalyze = true)
        )
        val blunderPly = gameRes.plies[5] // 3... Nf6 allow mate in 1

        // Offline Stockfish-grounded fallback produces structured coaching anchored to Stockfish data
        val moveInsightRes = service.getMoveInsight(
            ply = blunderPly,
            openingName = gameRes.openingSummary.entry?.name,
            depth = 4,
            allowCloud = false
        )
        assertTrue(moveInsightRes.isSuccess)
        val insight = moveInsightRes.getOrThrow()
        assertTrue(insight.headline.isNotBlank())
        assertTrue(insight.whyGoodOrBad.isNotBlank())
        assertTrue(insight.whyBestIsStronger.isNotBlank())
        assertEquals("Offline Stockfish Coach", insight.providerLabel)

        val summaryRes = service.getGameSummaryReport(gameRes, allowCloud = false)
        assertTrue(summaryRes.isSuccess)
        val report = summaryRes.getOrThrow()
        assertTrue(report.openingInsight.isNotBlank())
        assertTrue(report.strengths.isNotEmpty())
        assertTrue(report.trainingFocus.isNotBlank())

        // Persistent cache roundtrip
        val cacheKey = service.buildMoveCacheKey(blunderPly, depth = 12)
        repo.putCachedAiMoveInsight(cacheKey, insight)
        val loadedMove = repo.getCachedAiMoveInsight(cacheKey)
        assertNotNull(loadedMove)
        assertEquals(insight.headline, loadedMove?.headline)
        assertTrue(loadedMove?.isFromCache == true)

        repo.putCachedAiGameReport(gameRes.id, report)
        val loadedReport = repo.getCachedAiGameReport(gameRes.id)
        assertNotNull(loadedReport)
        assertEquals(report.trainingFocus, loadedReport?.trainingFocus)
        assertTrue(loadedReport?.isFromCache == true)

        // Daily quota decrements cleanly
        val before = repo.getDailyAiRemaining()
        assertTrue(before > 0)
        assertTrue(repo.tryConsumeDailyAiQuota())
        assertEquals(before - 1, repo.getDailyAiRemaining())
    }

    @Test
    fun gameAnalyzer_usesParallelPoolShallowBookAndFastMultiPv1Pass() = runBlocking {
        val activeConcurrent = java.util.concurrent.atomic.AtomicInteger(0)
        val peakConcurrent = java.util.concurrent.atomic.AtomicInteger(0)
        val pass1Depths = java.util.Collections.synchronizedList(mutableListOf<Int>())
        val pass2Calls = java.util.concurrent.atomic.AtomicInteger(0)

        val parallelMock = object : StockfishAnalyzerEngine {
            override val statusFlow = kotlinx.coroutines.flow.MutableStateFlow<StockfishEngineStatus>(
                StockfishEngineStatus.Ready()
            )
            override val maxParallelism: Int = 4
            override suspend fun ensureReady(): Boolean = true
            override suspend fun evaluateMultiPv(
                fen: String,
                depth: Int,
                multiPv: Int,
                timeLimitMs: Long?
            ): EngineEval? = evaluateBatchPosition(fen, depth, multiPv, timeLimitMs)

            override suspend fun evaluateBatchPosition(
                fen: String,
                depth: Int,
                multiPv: Int,
                movetimeMs: Long?
            ): EngineEval {
                val cur = activeConcurrent.incrementAndGet()
                peakConcurrent.updateAndGet { prev -> maxOf(prev, cur) }
                kotlinx.coroutines.delay(35)
                activeConcurrent.decrementAndGet()

                val b = ChessBoard(fen)
                val firstUci = b.legalMoves().firstOrNull()?.uci
                val line1 = if (firstUci != null) {
                    EngineVariation(1, 0.25, null, firstUci, firstUci, listOf(firstUci), listOf(firstUci), depth)
                } else null
                if (multiPv >= 2) {
                    pass2Calls.incrementAndGet()
                    val altUci = b.legalMoves().getOrNull(1)?.uci ?: firstUci
                    val lines = listOfNotNull(
                        line1,
                        altUci?.let { EngineVariation(2, -1.5, null, it, it, listOf(it), listOf(it), depth) }
                    )
                    return EngineEval(cp = 0.25, depth = depth, bestUci = firstUci, topLines = lines)
                }
                pass1Depths.add(depth)
                return EngineEval(
                    cp = 0.25,
                    depth = depth,
                    bestUci = firstUci,
                    topLines = listOfNotNull(line1)
                )
            }
        }

        val pgn = """
            [Event "Parallel Speed Test"]
            [White "W"]
            [Black "B"]
            [Result "*"]

            1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0
        """.trimIndent()
        val analyzer = GameAnalyzer(ChessEngine(), parallelMock)
        val res = analyzer.analyze(
            pgnText = pgn,
            settings = ReviewSettings(depth = 12, forceReanalyze = true)
        )
        assertEquals(7, res.plies.size)
        assertTrue("Expected concurrent pool execution (peak=${peakConcurrent.get()})", peakConcurrent.get() >= 2)
        assertTrue("Expected interior book positions to use shallow depth <= 8", pass1Depths.any { it <= 8 })
        assertTrue("Expected non-book positions to use full requested depth 12", pass1Depths.any { it == 12 })
        assertTrue("Expected Pass 2 MultiPV-2 check on candidate BEST moves", pass2Calls.get() >= 1)
    }

    @Test
    fun tinyFishSearchService_parsesResultsAndHonorsRuntimeSettingsKey() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = KnightScopeRepository(context)
        val tinyFish = TinyFishSearchService(repo)

        val json = """
            {
              "results": [
                {
                  "title": "Latest FIFA World Cup news today",
                  "url": "https://example.org/fifa-news",
                  "snippet": "Match schedule and qualification updates."
                },
                {
                  "name": "Sicilian Najdorf Opening Theory",
                  "link": "https://example.org/najdorf",
                  "description": "Key pawn breaks and tactical plans."
                },
                {
                  "title": "Missing URL should be skipped"
                }
              ]
            }
        """.trimIndent()
        val parsed = tinyFish.parseResults(json, maxResults = 4)
        assertEquals(2, parsed.size)
        assertEquals("Latest FIFA World Cup news today", parsed[0].title)
        assertEquals("https://example.org/fifa-news", parsed[0].url)
        assertEquals("Sicilian Najdorf Opening Theory", parsed[1].title)
        assertEquals("https://example.org/najdorf", parsed[1].url)

        assertTrue(tinyFish.shouldEnrichQuestion("search: Sicilian Defense theory"))
        assertTrue(tinyFish.shouldEnrichQuestion("latest FIFA World Cup news today"))
        assertFalse(tinyFish.shouldEnrichQuestion("e4"))

        // Runtime Settings key override works without rebuild and supports quota refund
        repo.setGroqApiKey("  test-groq-key  ")
        repo.setTinyFishApiKey("  test-tinyfish-key  ")
        assertEquals("test-groq-key", repo.getEffectiveGroqKey())
        assertEquals("test-tinyfish-key", repo.getEffectiveTinyFishKey())
        assertTrue(tinyFish.isConfigured())

        val qBefore = repo.getDailyAiRemaining()
        assertTrue(repo.tryConsumeDailyAiQuota())
        assertEquals(qBefore - 1, repo.getDailyAiRemaining())
        repo.refundDailyAiQuota()
        assertEquals(qBefore, repo.getDailyAiRemaining())

        // Clean up local test keys
        repo.setGroqApiKey("")
        repo.setTinyFishApiKey("")
    }

    @Test
    fun aiCoachChat_alwaysProducesWorkableGroundedReplyIncludingWebReferences() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = KnightScopeRepository(context)
        val service = AiChessCoachService(repo)

        val analyzer = GameAnalyzer(ChessEngine())
        val gameRes = analyzer.analyze(
            pgnText = "1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0",
            settings = ReviewSettings(depth = 4, forceReanalyze = true)
        )
        val plyIdx = 6 // 3... Nf6
        val activeFen = gameRes.plies[plyIdx - 1].fenAfter

        val q1 = service.buildOfflineChatAnswer(
            question = "Why is Stockfish's best move stronger here?",
            result = gameRes,
            currentPly = plyIdx,
            activeFen = activeFen,
            liveLines = gameRes.plies[plyIdx - 1].topLines
        )
        assertTrue(q1.isNotBlank())
        assertTrue(q1.contains("Stockfish", ignoreCase = true))

        val q2 = service.buildOfflineChatAnswer(
            question = "Where was the turning point of this game?",
            result = gameRes,
            currentPly = 0,
            activeFen = ChessBoard.START_FEN,
            liveLines = emptyList()
        )
        assertTrue(q2.contains("Turning Point", ignoreCase = true))

        val qWeb = service.buildOfflineChatAnswer(
            question = "latest FIFA World Cup news today",
            result = gameRes,
            currentPly = 0,
            activeFen = ChessBoard.START_FEN,
            liveLines = emptyList(),
            webResults = listOf(
                TinyFishSearchResult(
                    title = "FIFA World Cup Today",
                    url = "https://example.org/wc",
                    snippet = "Updates from today"
                )
            )
        )
        assertTrue(qWeb.contains("TinyFish", ignoreCase = true))
        assertTrue(qWeb.contains("FIFA World Cup Today"))

        // Exhaust quota so streamCoachAnswer exercises its deterministic fallback directly
        while (repo.getDailyAiRemaining() > 0) {
            repo.tryConsumeDailyAiQuota()
        }
        val updates = service.streamCoachAnswer(
            question = "What tactical motif did I miss?",
            result = gameRes,
            currentPly = plyIdx,
            activeFen = activeFen,
            liveLines = gameRes.plies[plyIdx - 1].topLines,
            recentHistory = emptyList()
        ).toList()
        assertTrue("Expected at least 1 stream update from workable coach", updates.isNotEmpty())
        assertTrue(updates.last().text.isNotBlank())
        assertEquals("Stockfish Tactical Coach", updates.last().providerLabel)
    }
}
