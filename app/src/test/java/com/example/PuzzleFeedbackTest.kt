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

    @Test
    fun solvingPuzzle_automaticallyShiftsToNextPuzzleInDailyAndPractice() {
        val vm = createReadyViewModel()

        fun solveAndVerifyAutoShift(startSession: () -> Unit, modeLabel: String) {
            startSession()
            ShadowLooper.idleMainLooper()

            val firstState = vm.uiState.value
            val firstPuzzle = firstState.currentPuzzle!!
            assertTrue("$modeLabel should enter play mode", firstState.inPlay)

            // Play all required player moves in the puzzle
            var safety = 0
            while (!vm.uiState.value.isFinished && safety < 10) {
                val s = vm.uiState.value
                val cur = s.currentPuzzle!!
                val step = s.solStepIndex
                val expectedUci = cur.solution.getOrNull(step) ?: break
                val from = expectedUci.substring(0, 2)
                val to = expectedUci.substring(2, 4)

                vm.onSquareClick(from)
                vm.onSquareClick(to)
                ShadowLooper.idleMainLooper()

                if (!vm.uiState.value.isFinished && expectedUci.length == 5 && vm.uiState.value.pendingPromotion != null) {
                    val promo = com.example.chess.PieceType.fromChar(expectedUci[4]) ?: com.example.chess.PieceType.QUEEN
                    vm.selectPromotion(promo)
                    ShadowLooper.idleMainLooper()
                }

                // Advance Robolectric looper clock for opponent auto-reply (500ms) if multi-move puzzle
                if (!vm.uiState.value.isFinished) {
                    var waitedMs = 0L
                    while (vm.uiState.value.solStepIndex == step + 1 && !vm.uiState.value.isFinished && waitedMs < 1500L) {
                        ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                        waitedMs += 100L
                    }
                }
                safety++
            }

            assertTrue("$modeLabel puzzle should be marked finished before auto-shift", vm.uiState.value.isFinished)

            // Advance Robolectric looper clock past the ~650ms auto-shift delay
            var shiftWaitedMs = 0L
            while (vm.uiState.value.currentPuzzle?.id == firstPuzzle.id && shiftWaitedMs < 2000L) {
                ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                shiftWaitedMs += 100L
            }

            val shifted = vm.uiState.value
            assertTrue("$modeLabel should stay in play after auto-shift", shifted.inPlay)
            assertNotNull(shifted.currentPuzzle)
            assertNotEquals(
                "Expected automatic shift to a new puzzle after solving in $modeLabel",
                firstPuzzle.id,
                shifted.currentPuzzle?.id
            )
            assertFalse("New puzzle in $modeLabel should start fresh with isFinished=false", shifted.isFinished)
            assertEquals(0, shifted.solStepIndex)
        }

        solveAndVerifyAutoShift({ vm.startDaily() }, "Daily")
        vm.backToMenu()
        ShadowLooper.idleMainLooper()
        solveAndVerifyAutoShift({ vm.startPractice() }, "Practice")
    }

    @Test
    fun puzzleStreak_incrementsOnConsecutiveSolves_persistsBest_andResetsOnFailure() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = KnightScopeRepository(context)
        val vm = PuzzlesViewModel(repo)
        val deadline = System.currentTimeMillis() + 5000L
        while (vm.uiState.value.isLoadingPool && System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        ShadowLooper.idleMainLooper()

        fun solveCurrentPuzzleCleanly() {
            var safety = 0
            while (!vm.uiState.value.isFinished && safety < 10) {
                val s = vm.uiState.value
                val cur = s.currentPuzzle!!
                val step = s.solStepIndex
                val expectedUci = cur.solution.getOrNull(step) ?: break
                vm.onSquareClick(expectedUci.substring(0, 2))
                vm.onSquareClick(expectedUci.substring(2, 4))
                ShadowLooper.idleMainLooper()

                if (!vm.uiState.value.isFinished && expectedUci.length == 5 && vm.uiState.value.pendingPromotion != null) {
                    val promo = com.example.chess.PieceType.fromChar(expectedUci[4]) ?: com.example.chess.PieceType.QUEEN
                    vm.selectPromotion(promo)
                    ShadowLooper.idleMainLooper()
                }
                if (!vm.uiState.value.isFinished) {
                    var waited = 0L
                    while (vm.uiState.value.solStepIndex == step + 1 && !vm.uiState.value.isFinished && waited < 1500L) {
                        ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                        waited += 100L
                    }
                }
                safety++
            }
        }

        fun waitForAutoShift(prevId: String) {
            var waited = 0L
            while (vm.uiState.value.currentPuzzle?.id == prevId && waited < 2000L) {
                ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                waited += 100L
            }
        }

        val initialStreak = vm.uiState.value.currentPuzzleStreak
        vm.startPractice()
        ShadowLooper.idleMainLooper()

        // Solve 3 puzzles in a row cleanly -> streak reaches +3 and triggers milestone 3
        val p1 = vm.uiState.value.currentPuzzle!!.id
        solveCurrentPuzzleCleanly()
        assertEquals(initialStreak + 1, vm.uiState.value.currentPuzzleStreak)
        waitForAutoShift(p1)

        val p2 = vm.uiState.value.currentPuzzle!!.id
        solveCurrentPuzzleCleanly()
        assertEquals(initialStreak + 2, vm.uiState.value.currentPuzzleStreak)
        assertTrue(vm.uiState.value.bestPuzzleStreak >= initialStreak + 2)
        waitForAutoShift(p2)

        val p3 = vm.uiState.value.currentPuzzle!!.id
        solveCurrentPuzzleCleanly()
        val streakAfterThree = vm.uiState.value.currentPuzzleStreak
        assertEquals(initialStreak + 3, streakAfterThree)
        if (streakAfterThree == 3) {
            assertEquals(3, vm.uiState.value.streakMilestoneReached)
        }
        val expectedBest = vm.uiState.value.bestPuzzleStreak
        assertTrue(expectedBest >= streakAfterThree)
        waitForAutoShift(p3)

        // Allow DataStore writes to settle, then verify persistence in a fresh ViewModel
        Thread.sleep(100)
        ShadowLooper.idleMainLooper(200, java.util.concurrent.TimeUnit.MILLISECONDS)
        val vmReloaded = PuzzlesViewModel(KnightScopeRepository(context))
        val reloadDeadline = System.currentTimeMillis() + 5000L
        while ((vmReloaded.uiState.value.isLoadingPool || vmReloaded.uiState.value.currentPuzzleStreak < streakAfterThree) && System.currentTimeMillis() < reloadDeadline) {
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            Thread.sleep(20)
        }
        assertEquals(streakAfterThree, vmReloaded.uiState.value.currentPuzzleStreak)
        assertTrue(vmReloaded.uiState.value.bestPuzzleStreak >= expectedBest)

        // Wrong move resets current streak to 0 while keeping bestPuzzleStreak intact
        vmReloaded.startPractice()
        ShadowLooper.idleMainLooper()
        val cur = vmReloaded.uiState.value.currentPuzzle!!
        val expectedUci = cur.solution.first()
        val expFrom = expectedUci.substring(0, 2)
        val expTo = expectedUci.substring(2, 4)
        val board = ChessBoard(vmReloaded.uiState.value.currentFen)
        val wrongMove = board.legalMoves().first { it.fromName != expFrom || it.toName != expTo }

        vmReloaded.onSquareClick(wrongMove.fromName)
        vmReloaded.onSquareClick(wrongMove.toName)
        ShadowLooper.idleMainLooper()

        assertEquals(0, vmReloaded.uiState.value.currentPuzzleStreak)
        assertFalse(vmReloaded.uiState.value.isNewBestStreak)
        assertNull(vmReloaded.uiState.value.streakMilestoneReached)
        assertTrue(vmReloaded.uiState.value.bestPuzzleStreak >= expectedBest)
    }
}
