package com.example

import com.example.chess.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ChessCoreTest {

    @Test
    fun rulesEngine_handlesCapturesCastlingEnPassantPromotionAndMate() {
        val b1 = ChessBoard()
        assertNotNull(b1.makeSanMove("e4"))
        assertNotNull(b1.makeSanMove("d5"))
        val cap = b1.makeSanMove("exd5")
        assertNotNull(cap)
        assertEquals(PieceType.PAWN, cap?.captured)

        val b2 = ChessBoard()
        listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "O-O").forEach {
            assertNotNull("Failed move $it", b2.makeSanMove(it))
        }
        assertTrue(b2.toFen().split(' ')[0].contains("RNBQ1RK1"))

        val b3 = ChessBoard()
        listOf("e4", "a6", "e5", "d5").forEach { assertNotNull(b3.makeSanMove(it)) }
        val ep = b3.makeSanMove("exd6")
        assertNotNull(ep)
        assertTrue(ep!!.isEnPassant)
        assertEquals(PieceType.PAWN, ep.captured)

        val b4 = ChessBoard("8/P7/8/8/8/8/7k/K7 w - - 0 1")
        val promo = b4.makeUciMove("a7a8q")
        assertNotNull(promo)
        assertEquals(PieceType.QUEEN, promo?.promotion)

        val b5 = ChessBoard()
        listOf("f3", "e5", "g4", "Qh4#").forEach { assertNotNull(b5.makeSanMove(it)) }
        assertTrue(b5.inCheckmate())
        assertTrue(b5.isGameOver())

        assertNull(ChessBoard().makeSanMove("e5"))
    }

    @Test
    fun openingBook_isValidAndHandlesTranspositions() {
        assertTrue(OpeningBook.fenMap.isNotEmpty())
        assertEquals(0, OpeningBook.invalidLineCount)

        fun key(line: String): String {
            val b = ChessBoard()
            line.split(' ').forEach { assertNotNull(b.makeSanMove(it)) }
            return b.openingKey()
        }

        val k1 = key("d4 d5 Nf3 Nf6 Bf4")
        val k2 = key("d4 Nf6 Nf3 d5 Bf4")
        assertEquals(k1, k2)
        assertNotNull(OpeningBook.fenMap[k1])
    }

    @Test
    fun pgnParser_parsesSampleGameWithHeadersAndMoves() {
        val pgn = """
            [Event "Sample"]
            [White "Alice"]
            [Black "Bob"]
            [Result "1-0"]

            1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0
        """.trimIndent()
        val parsed = PgnParser.parse(pgn)
        assertEquals("Alice", parsed.headers["White"])
        assertEquals("Bob", parsed.headers["Black"])
        assertEquals(7, parsed.plies.size)
        assertTrue(ChessBoard(parsed.plies.last().fenAfter).inCheckmate())
    }

    @Test
    fun engine_findsMateInOne_andEvaluatesStartPosition() = runBlocking {
        val engine = ChessEngine()
        val mate = engine.evaluatePosition(
            fen = "6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1",
            depth = 4,
            timeLimitMs = 2000,
            allowCloud = false
        )
        assertEquals("e1e8", mate.bestUci)
        assertEquals(1, mate.mate)

        val start = engine.evaluatePosition(
            fen = ChessBoard.START_FEN,
            depth = 4,
            timeLimitMs = 2000,
            allowCloud = false
        )
        assertTrue(start.depth > 0)
        assertTrue(kotlin.math.abs(start.cp) < 1.5)
    }

    @Test
    fun engine_multiPvReturnsUpToThreeDistinctLinesWithValidSan() = runBlocking {
        val engine = ChessEngine()
        val ev = engine.evaluateMultiPv(
            fen = ChessBoard.START_FEN,
            depth = 2,
            multiPv = 3,
            timeLimitMs = 1500,
            allowCloud = false
        )
        assertEquals(3, ev.topLines.size)
        assertEquals(listOf(1, 2, 3), ev.topLines.map { it.rank })
        val firstMoves = ev.topLines.map { it.firstUci }.toSet()
        assertEquals(3, firstMoves.size)
        ev.topLines.forEach { line ->
            assertTrue(line.firstSan.isNotBlank())
            assertTrue(line.pvUci.isNotEmpty())
            assertTrue(line.pvSan.isNotEmpty())
        }
    }

    @Test
    fun moveExplainer_buildsCoachInsightsAndTacticalTags() {
        val fenBefore = "r1bqkbnr/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4"
        val b = ChessBoard(fenBefore)
        val move = b.makeSanMove("Qxf7#")!!
        val fenAfter = b.toFen()
        val insights = MoveExplainer.buildCoachInsights(
            plyIdx = 6,
            san = "Qxf7#",
            move = move,
            fenBefore = fenBefore,
            fenAfter = fenAfter,
            moveClass = MoveClass.BEST,
            isBook = false,
            isGreatMove = false,
            isMissedWin = false,
            opening = null,
            bestSan = "Qxf7#",
            topLines = listOf(EngineVariation(1, 99.0, 1, "h5f7", "Qxf7#", listOf("h5f7"), listOf("Qxf7#"), 4))
        )
        assertTrue(insights.headline.contains("Qxf7#"))
        assertTrue("Checkmate" in insights.tacticalTags)
        assertTrue("Capture" in insights.tacticalTags)
    }

    @Test
    fun gameAnalyzer_producesMultiPvAndCoachReviewFields() = runBlocking {
        val pgn = """
            [Event "Studio Test"]
            [White "Player1"]
            [Black "Player2"]
            [Result "1-0"]

            1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# 1-0
        """.trimIndent()
        val analyzer = GameAnalyzer(ChessEngine())
        val res = analyzer.analyze(
            pgnText = pgn,
            settings = ReviewSettings(depth = 4, forceReanalyze = false),
            savedEntry = SavedGameEntry(
                id = "test_saved",
                title = "Player1 vs Player2",
                timestamp = 0L,
                plies = 7,
                moves = 4,
                pgn = pgn,
                evals = listOf(0.2, 0.3, -0.5, -0.4, 0.0, 99.0, 99.0),
                bestMoves = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "g6", "Qxf7#"),
                report = null,
                mistakes = emptyList()
            )
        )
        assertEquals(7, res.plies.size)
        assertTrue(res.plies.all { it.coachHeadline.isNotBlank() && it.badgeGlyph.isNotBlank() })
        assertTrue(res.keyMistakes.any { it.san == "Nf6" })
    }

    @Test
    fun chessLessons_allCurriculumStepsHaveLegalFenAndValidSolutionMoves() {
        val curriculum = com.example.ui.viewmodel.ChessLessonsViewModel.CURRICULUM
        assertEquals(6, curriculum.size)
        curriculum.forEach { lesson ->
            assertTrue("${lesson.id} has empty steps", lesson.steps.isNotEmpty())
            lesson.steps.forEach { step ->
                val board = ChessBoard(step.fen)
                val move = board.makeUciMove(step.solutionUci)
                assertNotNull("Invalid solution ${step.solutionUci} in lesson ${lesson.id} / ${step.title}", move)
            }
        }
    }

    @Test
    fun openingTrainer_allRepertoireLinesReplayCleanlyAndMatchMoveNotes() {
        val repertoires = com.example.ui.viewmodel.OpeningTrainerViewModel.REPERTOIRES
        assertTrue(repertoires.size >= 8)
        repertoires.forEach { rep ->
            assertEquals("Mismatch notes vs moves in ${rep.id}", rep.sanMoves.size, rep.moveNotes.size)
            val board = ChessBoard()
            rep.sanMoves.forEach { san ->
                assertNotNull("Illegal book move $san in ${rep.id}", board.makeSanMove(san))
            }
        }
    }

    @Test
    fun lichessOpenServices_offlineExplorerFallbackProvidesOpeningMovesAndEco() {
        val board = ChessBoard()
        board.makeSanMove("e4")
        board.makeSanMove("e5")
        val entry = OpeningBook.lookup(board.toFen())
        assertNotNull(entry)
        assertEquals("C20", entry?.eco)
    }

    @Test
    fun contactSender_isOptionalWithoutSecrets() {
        assertFalse(com.example.service.ContactSender.hasAnyProvider())
    }

    @Test
    fun diggyDesignTokensAndPlayHelpers_workAsExpected() {
        assertEquals(com.example.ui.theme.BoardTheme.GREEN, com.example.ui.theme.BoardTheme.fromId("unknown"))
        assertEquals(com.example.ui.theme.BoardTheme.GREEN, com.example.ui.theme.BoardTheme.fromId("green"))
        assertEquals(com.example.ui.theme.BoardTheme.GOLD, com.example.ui.theme.BoardTheme.fromId("gold"))
        assertEquals("Gold/Classic", com.example.ui.theme.BoardTheme.GOLD.label)
        assertEquals("Green/Cream", com.example.ui.theme.BoardTheme.GREEN.label)
        assertEquals("Blue/Steel", com.example.ui.theme.BoardTheme.BLUE.label)
        assertEquals("Brown/Wood", com.example.ui.theme.BoardTheme.BROWN.label)
        assertEquals(com.example.ui.theme.PieceStyle.NEO, com.example.ui.theme.PieceStyle.fromId(null))
        assertEquals(com.example.ui.theme.PieceStyle.CLASSIC_2D, com.example.ui.theme.PieceStyle.fromId("classic_2d"))
        assertEquals(com.example.ui.theme.PieceStyle.THREED, com.example.ui.theme.PieceStyle.fromId("3d"))

        val cap = com.example.ui.components.computeCapturedMaterial("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertTrue(cap.capturedWhitePieces.isEmpty())
        assertTrue(cap.capturedBlackPieces.isEmpty())
        assertEquals(0, cap.whiteAdvantage)

        val capMissingQueen = com.example.ui.components.computeCapturedMaterial("rnb1kbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
        assertEquals(listOf(PieceType.QUEEN), capMissingQueen.capturedBlackPieces)
        assertEquals(9, capMissingQueen.whiteAdvantage)

        assertEquals("3:00", com.example.ui.components.formatClockMillis(180_000L))
        assertEquals("0:01", com.example.ui.components.formatClockMillis(50L))
        assertEquals(180_000L, com.example.ui.viewmodel.QuickPlayTimeControl.BLITZ_3.baseMillis)
        assertNull(com.example.ui.viewmodel.QuickPlayTimeControl.UNTIMED.baseMillis)
    }

    @Test
    fun chessAudioAndBoardTransitions_produceValidWoodWavsAndDetectCastling() {
        val buffers = listOf(
            com.example.audio.ChessSoundPlayer.moveBuffer,
            com.example.audio.ChessSoundPlayer.captureBuffer,
            com.example.audio.ChessSoundPlayer.captureCheckBuffer,
            com.example.audio.ChessSoundPlayer.castleBuffer,
            com.example.audio.ChessSoundPlayer.checkBuffer,
            com.example.audio.ChessSoundPlayer.checkmateBuffer,
            com.example.audio.ChessSoundPlayer.drawBuffer,
            com.example.audio.ChessSoundPlayer.pickupBuffer,
            com.example.audio.ChessSoundPlayer.wrongMoveBuffer,
            com.example.audio.ChessSoundPlayer.gameEndBuffer
        )
        buffers.forEach { buf ->
            assertTrue("Sound buffer should not be empty", buf.size > 500)
            assertTrue("Sound buffer should have audible waveform", buf.any { it != 0.toShort() })
            val wav = com.example.audio.ChessSoundPlayer.buildWavBytes(buf)
            assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
            assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
            assertEquals("data", String(wav, 36, 4, Charsets.US_ASCII))
            assertEquals(44 + buf.size * 2, wav.size)
        }

        // Capture double-clack and checkmate fanfare should be distinctly longer/richer than regular move & draw
        assertTrue(com.example.audio.ChessSoundPlayer.captureBuffer.size > com.example.audio.ChessSoundPlayer.moveBuffer.size)
        assertTrue(com.example.audio.ChessSoundPlayer.checkmateBuffer.size > com.example.audio.ChessSoundPlayer.drawBuffer.size)
        assertFalse(com.example.audio.ChessSoundPlayer.checkmateBuffer.contentEquals(com.example.audio.ChessSoundPlayer.drawBuffer))
        assertFalse(com.example.audio.ChessSoundPlayer.captureBuffer.contentEquals(com.example.audio.ChessSoundPlayer.moveBuffer))

        val beforeCastle = ChessBoard("r1bqk1nr/pppp1ppp/2n5/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4")
        val afterCastle = ChessBoard(beforeCastle.toFen()).apply { makeSanMove("O-O") }
        assertTrue(com.example.ui.components.isCastlingTransition(beforeCastle, afterCastle))

        val beforeKingStep = ChessBoard("4k3/8/8/8/8/8/8/4K3 w - - 0 1")
        val afterKingStep = ChessBoard(beforeKingStep.toFen()).apply { makeSanMove("Kf1") }
        assertFalse(com.example.ui.components.isCastlingTransition(beforeKingStep, afterKingStep))
    }

    @Test
    fun gameAnalyzer_parallelBatchedReview_reportsMonotonicProgressAndUnifiedEngineLabel() = runBlocking {
        val pgn = """
            [Event "Batch Review"]
            [White "Alice"]
            [Black "Stockfish"]
            [Result "1-0"]

            1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. O-O 1-0
        """.trimIndent()
        val analyzer = GameAnalyzer(ChessEngine())
        val progressEvents = mutableListOf<Pair<Int, Int>>()
        val res = analyzer.analyze(
            pgnText = pgn,
            settings = ReviewSettings(depth = 4, forceReanalyze = true),
            savedEntry = null,
            onProgress = { done, total -> synchronized(progressEvents) { progressEvents.add(done to total) } }
        )
        assertEquals("Chess Analyzer Stockfish Engine", res.engineLabel)
        assertEquals(7, res.plies.size)
        assertTrue(progressEvents.isNotEmpty())
        assertEquals(0, progressEvents.first().first)
        assertEquals(progressEvents.last().second, progressEvents.last().first)
        assertEquals("C50", res.openingSummary.entry?.eco)
        assertTrue(res.summaryStats.whiteAccuracy in 0.0..100.0)
        assertTrue(res.summaryStats.blackAccuracy in 0.0..100.0)
    }

    @Test
    fun uciParser_normalizesBlackPovScores_andBuildsThreeMultiPvLines() {
        val whiteLine = UciParser.parseInfoLine(
            "info depth 18 seldepth 24 multipv 1 score cp 145 lowerbound nodes 12000 pv e2e4 e7e5 g1f3",
            sideToMoveIsBlack = false
        )
        assertNotNull(whiteLine)
        assertEquals(18, whiteLine!!.depth)
        assertEquals(1, whiteLine.multiPvRank)
        assertEquals(1.45, whiteLine.cpWhitePov, 0.001)
        assertNull(whiteLine.mateWhitePov)
        assertEquals(listOf("e2e4", "e7e5", "g1f3"), whiteLine.pvUci)

        val blackLine = UciParser.parseInfoLine(
            "info depth 16 multipv 2 score cp -230 pv e7e5",
            sideToMoveIsBlack = true
        )
        assertNotNull(blackLine)
        assertEquals(2, blackLine!!.multiPvRank)
        assertEquals(2.30, blackLine.cpWhitePov, 0.001)

        val blackMate = UciParser.parseInfoLine(
            "info depth 12 multipv 1 score mate 2 pv d8h4",
            sideToMoveIsBlack = true
        )
        assertNotNull(blackMate)
        assertEquals(-2, blackMate!!.mateWhitePov)
        assertEquals(-99.0, blackMate.cpWhitePov, 0.001)

        assertEquals("e2e4", UciParser.parseBestMoveLine("bestmove e2e4 ponder e7e5"))
        assertNull(UciParser.parseBestMoveLine("bestmove (none)"))

        val lines = mapOf(
            3 to UciParser.ParsedInfoLine(18, 3, 0.15, null, listOf("g1f3", "d7d5")),
            1 to UciParser.ParsedInfoLine(18, 1, 0.32, null, listOf("e2e4", "e7e5")),
            2 to UciParser.ParsedInfoLine(18, 2, 0.28, null, listOf("d2d4", "d7d5"))
        )
        val ev = UciParser.buildEngineEval(
            fen = ChessBoard.START_FEN,
            collectedLines = lines,
            bestMoveUci = "e2e4",
            multiPv = 3
        )
        assertEquals("e2e4", ev.bestUci)
        assertEquals(3, ev.topLines.size)
        assertEquals(listOf(1, 2, 3), ev.topLines.map { it.rank })
        assertEquals("e4", ev.topLines[0].firstSan)
        assertEquals(listOf("e4", "e5"), ev.topLines[0].pvSan)
        assertEquals(listOf("d4", "d5"), ev.topLines[1].pvSan)
        assertEquals(listOf("Nf3", "d5"), ev.topLines[2].pvSan)
    }

    @Test
    fun gameAnalyzer_withStockfishWasmEngine_classifiesExcellentAndRespectsDepth22() = runBlocking {
        // Position off-book: Queen's Gambit style irregular line starting from custom middlegame FEN
        val customFen = "r1bq1rk1/pp2ppbp/2np1np1/8/2BNP3/2N1BP2/PPPQ2PP/R3K2R w KQ - 0 10"
        val b = ChessBoard(customFen)
        val moves = listOf("Bb3", "Na5", "O-O-O", "Nxb3+")
        val fenKeys = mutableListOf(customFen.split(' ').take(4).joinToString(" "))
        for (san in moves) {
            assertNotNull("Illegal move $san", b.makeSanMove(san))
            fenKeys.add(b.toFen().split(' ').take(4).joinToString(" "))
        }
        val pgn = """
            [Event "Stockfish 19 Depth 22 Test"]
            [White "Alice"]
            [Black "Stockfish 19"]
            [FEN "$customFen"]
            [SetUp "1"]
            [Result "*"]

            10. Bb3 Na5 11. O-O-O Nxb3+ *
        """.trimIndent()

        // Controlled evals (White POV):
        // pos0: +0.50 -> pos1 after Bb3: +0.44 (loss ~ 1.4 win% => MoveClass.EXCELLENT)
        // pos1: +0.44 -> pos2 after Na5: +3.20 (Black loses > 20 win% => Blunder)
        // pos2: +3.20 -> pos3 after O-O-O: +3.25 (Best, and if 2nd line is +0.20 => Great!)
        // pos3: +3.25 -> pos4 after Nxb3+: +3.25 (Best)
        val evalByFenKey = mapOf(
            fenKeys[0] to EngineEval(
                cp = 0.50,
                depth = 22,
                bestUci = "e1c1",
                sourceLabel = "Stockfish 19 Lite WASM",
                topLines = listOf(
                    EngineVariation(1, 0.50, null, "e1c1", "O-O-O", listOf("e1c1"), listOf("O-O-O"), 22),
                    EngineVariation(2, 0.34, null, "c4b3", "Bb3", listOf("c4b3"), listOf("Bb3"), 22)
                )
            ),
            fenKeys[1] to EngineEval(
                cp = 0.34,
                depth = 22,
                bestUci = "c8d7",
                sourceLabel = "Stockfish 19 Lite WASM",
                topLines = listOf(
                    EngineVariation(1, 0.34, null, "c8d7", "Bd7", listOf("c8d7"), listOf("Bd7"), 22),
                    EngineVariation(2, 0.60, null, "b7b6", "b6", listOf("b7b6"), listOf("b6"), 22)
                )
            ),
            fenKeys[2] to EngineEval(
                cp = 3.20,
                depth = 22,
                bestUci = "e1c1",
                sourceLabel = "Stockfish 19 Lite WASM",
                topLines = listOf(
                    EngineVariation(1, 3.20, null, "e1c1", "O-O-O", listOf("e1c1"), listOf("O-O-O"), 22),
                    EngineVariation(2, 0.20, null, "h2h4", "h4", listOf("h2h4"), listOf("h4"), 22)
                )
            ),
            fenKeys[3] to EngineEval(
                cp = 3.25,
                depth = 22,
                bestUci = "a5b3",
                sourceLabel = "Stockfish 19 Lite WASM",
                topLines = listOf(
                    EngineVariation(1, 3.25, null, "a5b3", "Nxb3+", listOf("a5b3"), listOf("Nxb3+"), 22)
                )
            ),
            fenKeys[4] to EngineEval(
                cp = 3.25,
                depth = 22,
                bestUci = "a2b3",
                sourceLabel = "Stockfish 19 Lite WASM",
                topLines = listOf(
                    EngineVariation(1, 3.25, null, "a2b3", "axb3", listOf("a2b3"), listOf("axb3"), 22)
                )
            )
        )

        val mockStockfish = object : StockfishAnalyzerEngine {
            override val statusFlow = kotlinx.coroutines.flow.MutableStateFlow<StockfishEngineStatus>(
                StockfishEngineStatus.Ready()
            )
            override suspend fun ensureReady(): Boolean = true
            override suspend fun evaluateMultiPv(fen: String, depth: Int, multiPv: Int, timeLimitMs: Long?): EngineEval? {
                assertEquals(22, depth)
                val key = fen.split(' ').take(4).joinToString(" ")
                return evalByFenKey[key]
            }
        }

        val analyzer = GameAnalyzer(ChessEngine(), mockStockfish)
        val res = analyzer.analyze(
            pgnText = pgn,
            settings = ReviewSettings(depth = 22, forceReanalyze = true)
        )
        assertEquals(22, res.depth)
        assertEquals(4, res.plies.size)
        assertEquals(MoveClass.EXCELLENT, res.plies[0].moveClass)
        assertEquals(MoveClass.BLUNDER, res.plies[1].moveClass)
        assertEquals(MoveClass.BEST, res.plies[2].moveClass)
        assertTrue("Expected Great move on ply 2", res.plies[2].isGreatMove)

        val saved = analyzer.toSavedGameEntry(res)
        assertEquals(22, saved.depth)
        assertEquals(MoveClass.entries.size, saved.report?.whiteCounts?.size)
        assertEquals(MoveClass.entries.size, saved.report?.blackCounts?.size)
        assertNotNull(saved.whiteAccuracy)
        assertNotNull(saved.blackAccuracy)
    }

    @Test
    fun gameAnalyzer_throwsExplicitError_whenStockfishWasmFailsToLoad() = runBlocking {
        val pgn = """
            [Event "Error Test"]
            [White "Alice"]
            [Black "Bob"]
            [Result "*"]

            1. e4 e5 *
        """.trimIndent()

        val failingStockfish = object : StockfishAnalyzerEngine {
            override val statusFlow = kotlinx.coroutines.flow.MutableStateFlow<StockfishEngineStatus>(
                StockfishEngineStatus.Error("WASM load timeout")
            )
            override suspend fun ensureReady(): Boolean = false
            override suspend fun evaluateMultiPv(fen: String, depth: Int, multiPv: Int, timeLimitMs: Long?): EngineEval? = null
        }

        val analyzer = GameAnalyzer(ChessEngine(), failingStockfish)
        try {
            analyzer.analyze(pgnText = pgn, settings = ReviewSettings(depth = 12, forceReanalyze = true))
            fail("Expected IllegalStateException when Stockfish WASM engine fails to load")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("WASM load timeout") == true)
        }
    }

    @Test
    fun rulesEngine_handlesStalemateThreefoldFiftyMoveInsufficientMaterialAndAllPromotions() {
        // 1. Stalemate detection (Qa6, Kc6 vs Ka8)
        val stale = ChessBoard("k7/8/1QK5/8/8/8/8/8 b - - 0 1")
        assertTrue(stale.inStalemate())
        assertFalse(stale.inCheckmate())
        assertTrue(stale.isGameOver())

        // 2. Threefold repetition
        val rep = ChessBoard()
        val repeatCycle = listOf("Nf3", "Nf6", "Ng1", "Ng8", "Nf3", "Nf6", "Ng1", "Ng8")
        repeatCycle.forEach { assertNotNull(rep.makeSanMove(it)) }
        assertTrue("Expected threefold repetition", rep.inThreefoldRepetition())
        assertTrue(rep.isGameOver())

        // 3. 50-move rule (halfMoveClock == 99 -> move makes it 100)
        val fifty = ChessBoard("4k3/8/8/8/8/8/8/4K2R w - - 99 60")
        assertFalse(fifty.isFiftyMoveRule())
        assertNotNull(fifty.makeSanMove("Rh2"))
        assertTrue(fifty.isFiftyMoveRule())
        assertTrue(fifty.isGameOver())

        // 4. Insufficient material checks
        assertTrue(ChessBoard("4k3/8/8/8/8/8/8/4K3 w - - 0 1").insufficientMaterial())
        assertTrue(ChessBoard("4k3/8/8/8/8/8/4B3/4K3 w - - 0 1").insufficientMaterial())
        assertTrue(ChessBoard("4k3/8/8/8/8/8/4N3/4K3 w - - 0 1").insufficientMaterial())
        // Two same-colored bishops -> draw; opposite-colored bishops -> playable
        assertTrue(ChessBoard("4k2b/8/8/8/8/8/8/B3K3 w - - 0 1").insufficientMaterial())
        assertFalse(ChessBoard("4k1b1/8/8/8/8/8/8/B3K3 w - - 0 1").insufficientMaterial())
        assertFalse(ChessBoard("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1").insufficientMaterial())

        // 5. All 4 promotions + Queenside castling + SAN history formatting
        for (promoPiece in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
            val pb = ChessBoard("8/P7/8/8/8/8/7k/K7 w - - 0 1")
            val mv = pb.makeUciMove("a7a8${promoPiece.symbol}")
            assertNotNull("Failed promotion to $promoPiece", mv)
            assertEquals(promoPiece, mv?.promotion)
            assertEquals(promoPiece, pb.pieceAt("a8")?.type)
        }
        val castleQ = ChessBoard("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        assertNotNull(castleQ.makeSanMove("O-O-O"))
        assertEquals(PieceType.KING, castleQ.pieceAt("c1")?.type)
        assertEquals(PieceType.ROOK, castleQ.pieceAt("d1")?.type)

        // Illegal move blocked (moving through check on f1)
        val blockedCastle = ChessBoard("5r2/8/8/8/8/8/8/R3K2R w KQ - 0 1")
        assertNull(blockedCastle.makeSanMove("O-O"))
        assertEquals("5r2/8/8/8/8/8/8/R3K2R w KQ - 0 1", blockedCastle.toFen())

        // SAN move history generation
        val sanGame = ChessBoard()
        listOf("e4", "e5", "Nf3", "Nc6", "Bb5").forEach { assertNotNull(sanGame.makeSanMove(it)) }
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6", "Bb5"), sanGame.sanHistory())
    }

    @Test
    fun engineDifficulty_mapsSkillLevels3_8_14_20_andResolvesExpert() {
        assertEquals(3, EngineDifficulty.EASY.skillLevel)
        assertEquals(8, EngineDifficulty.MEDIUM.skillLevel)
        assertEquals(14, EngineDifficulty.HARD.skillLevel)
        assertEquals(20, EngineDifficulty.EXPERT.skillLevel)
        assertEquals(EngineDifficulty.EXPERT, EngineDifficulty.fromId("expert"))
        assertEquals(EngineDifficulty.MEDIUM, EngineDifficulty.fromId("invalid_tier"))
        assertEquals(4, EngineDifficulty.entries.size)
    }

    @Test
    fun playClockTimeControls_increments_andCapturedMaterialAdvantage_workCorrectly() {
        assertEquals(60_000L, com.example.ui.viewmodel.TimeControlBase.MIN_1.baseMillis)
        assertEquals(180_000L, com.example.ui.viewmodel.TimeControlBase.MIN_3.baseMillis)
        assertEquals(300_000L, com.example.ui.viewmodel.TimeControlBase.MIN_5.baseMillis)
        assertEquals(600_000L, com.example.ui.viewmodel.TimeControlBase.MIN_10.baseMillis)
        assertNull(com.example.ui.viewmodel.TimeControlBase.UNLIMITED.baseMillis)

        val unlimitedState = com.example.ui.viewmodel.PlayUiState(
            selectedBaseTime = com.example.ui.viewmodel.TimeControlBase.UNLIMITED,
            selectedIncrementSec = 2
        )
        assertEquals("Unlimited", unlimitedState.timeControlLabel)
        assertEquals(0L, unlimitedState.effectiveIncrementMillis)

        val blitzState = com.example.ui.viewmodel.PlayUiState(
            selectedBaseTime = com.example.ui.viewmodel.TimeControlBase.MIN_3,
            selectedIncrementSec = 2
        )
        assertEquals("3+2", blitzState.timeControlLabel)
        assertEquals(180_000L, blitzState.effectiveBaseMillis)
        assertEquals(2_000L, blitzState.effectiveIncrementMillis)

        // Missing Black Rook (5) and White Knight (3) -> +2 for White
        val cap = com.example.ui.components.computeCapturedMaterial("rnbqkbn1/pppppppp/8/8/8/8/PPPPPPPP/R1BQKBNR w KQq - 0 1")
        assertEquals(listOf(PieceType.ROOK), cap.capturedBlackPieces)
        assertEquals(listOf(PieceType.KNIGHT), cap.capturedWhitePieces)
        assertEquals(2, cap.whiteAdvantage)
    }
}



