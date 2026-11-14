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
}
