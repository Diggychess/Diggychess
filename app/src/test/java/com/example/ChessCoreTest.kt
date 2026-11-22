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
        assertEquals(com.example.ui.theme.BoardTheme.GOLD, com.example.ui.theme.BoardTheme.fromId("unknown"))
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
        assertEquals("Diggy Chess Stockfish Engine", res.engineLabel)
        assertEquals(7, res.plies.size)
        assertTrue(progressEvents.isNotEmpty())
        assertEquals(0, progressEvents.first().first)
        assertEquals(progressEvents.last().second, progressEvents.last().first)
        assertEquals("C50", res.openingSummary.entry?.eco)
        assertTrue(res.summaryStats.whiteAccuracy in 0.0..100.0)
        assertTrue(res.summaryStats.blackAccuracy in 0.0..100.0)
    }
}


