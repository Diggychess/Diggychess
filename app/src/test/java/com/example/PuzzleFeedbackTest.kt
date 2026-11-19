package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.chess.ChessBoard
import com.example.chess.parseSquare
import com.example.data.KnightScopeRepository
import com.example.ui.viewmodel.PuzzlesViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PuzzleFeedbackTest {

    private fun createReadyViewModel(): PuzzlesViewModel {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = KnightScopeRepository(context)
        val vm = PuzzlesViewModel(repo)
        val deadline = System.currentTimeMillis() + 5000L
        while (vm.uiState.value.isLoadingPool && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        ShadowLooper.idleMainLooper()
        return vm
    }

    @Test
    fun wrongMove_showsRedFeedbackState_andUndoRestoresPosition() {
        val vm = createReadyViewModel()
        vm.startDaily()
        ShadowLooper.idleMainLooper()

        val state = vm.uiState.value
        assertTrue(state.inPlay)
        val cur = state.currentPuzzle!!
        val expectedUci = cur.solution.first()
        val expectedFrom = expectedUci.substring(0, 2)
        val expectedTo = expectedUci.substring(2, 4)

        // Find any non-promotion legal move that differs from the expected puzzle move
        val board = ChessBoard(state.currentFen)
        val wrongMove = board.legalMoves().firstOrNull { mv ->
            mv.promotion == null && (mv.fromName != expectedFrom || mv.toName != expectedTo)
        } ?: board.legalMoves().firstOrNull { mv ->
            mv.fromName != expectedFrom || mv.toName != expectedTo
        }
        assertNotNull("Expected an alternative legal move for wrong-move test", wrongMove)

        vm.onSquareClick(wrongMove!!.fromName)
        vm.onSquareClick(wrongMove.toName)
        ShadowLooper.idleMainLooper()

        val afterWrong = vm.uiState.value
        assertTrue("Expected isWrongMove=true on incorrect move", afterWrong.isWrongMove)
        assertEquals(listOf(wrongMove.fromName, wrongMove.toName), afterWrong.lastMoveSquares)
        assertTrue(afterWrong.correctMoveSquares.isEmpty())
        assertEquals(false, afterWrong.feedbackOk)

        // Undo restores clean puzzle state
        vm.undoPuzzleMove()
        ShadowLooper.idleMainLooper()

        val afterUndo = vm.uiState.value
        assertFalse(afterUndo.isWrongMove)
        assertEquals(state.currentFen, afterUndo.currentFen)
        assertTrue(afterUndo.correctMoveSquares.isEmpty())
    }

    @Test
    fun correctMove_populatesGreenCorrectMoveSquares_andFinishesSingleMoveOrAdvances() {
        val vm = createReadyViewModel()
        vm.startDaily()
        ShadowLooper.idleMainLooper()

        val initial = vm.uiState.value
        val cur = initial.currentPuzzle!!
        val expectedUci = cur.solution.first()
        val from = expectedUci.substring(0, 2)
        val to = expectedUci.substring(2, 4)

        vm.onSquareClick(from)
        vm.onSquareClick(to)
        ShadowLooper.idleMainLooper()

        val afterCorrect = vm.uiState.value
        assertFalse(afterCorrect.isWrongMove)
        assertEquals(listOf(from, to), afterCorrect.correctMoveSquares)
        assertEquals(true, afterCorrect.feedbackOk)
        if (cur.solution.size == 1) {
            assertTrue(afterCorrect.isFinished)
            assertTrue(afterCorrect.solutionExplanation.isNotEmpty())
        }
    }

    @Test
    fun hintModal_opensStage1PieceClue_preservesBoardHighlightOnDismiss_andRevealsStage2Target() {
        val vm = createReadyViewModel()
        vm.startDaily()
        ShadowLooper.idleMainLooper()

        val initial = vm.uiState.value
        val cur = initial.currentPuzzle!!
        val expectedUci = cur.solution.first()
        val fromSq = expectedUci.substring(0, 2)
        val toSq = expectedUci.substring(2, 4)
        val expectedPieceName = ChessBoard(initial.currentFen)
            .pieceAt(parseSquare(fromSq)!!)!!
            .type.pieceName.replaceFirstChar { it.uppercaseChar() }

        // Open Stage 1 Hint Modal
        vm.showHint()
        ShadowLooper.idleMainLooper()

        val stage1 = vm.uiState.value
        assertTrue(stage1.showHintModal)
        assertEquals(1, stage1.hintLevel)
        assertEquals(listOf(fromSq), stage1.hintSquares)
        assertEquals(fromSq, stage1.hintFromSquare)
        assertEquals(toSq, stage1.hintToSquare)
        assertEquals(expectedPieceName, stage1.hintPieceName)
        assertTrue(stage1.hintSanMove.isNotBlank())
        assertTrue(stage1.hintThemeLabel.isNotBlank())
        assertTrue(stage1.hintTacticalCue.isNotBlank())

        // Dismiss modal: keeps Stage 1 highlight on board
        vm.dismissHintModal()
        val dismissed = vm.uiState.value
        assertFalse(dismissed.showHintModal)
        assertEquals(1, dismissed.hintLevel)
        assertEquals(listOf(fromSq), dismissed.hintSquares)

        // Re-opening hint shows Stage 1 without jumping prematurely to Stage 2
        vm.showHint()
        val reopened = vm.uiState.value
        assertTrue(reopened.showHintModal)
        assertEquals(1, reopened.hintLevel)

        // Explicit Stage 2 reveal highlights destination & explains line
        vm.revealDestinationHint()
        ShadowLooper.idleMainLooper()

        val stage2 = vm.uiState.value
        assertTrue(stage2.showHintModal)
        assertEquals(2, stage2.hintLevel)
        assertEquals(listOf(fromSq, toSq), stage2.hintSquares)
        assertTrue(stage2.solutionExplanation.isNotBlank())
    }
}
