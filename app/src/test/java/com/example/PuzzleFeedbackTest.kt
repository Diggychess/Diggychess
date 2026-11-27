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

    @Test
    fun dailyChallenge_tracksDailyGoalProgress_andMarksCompletionOnTarget() {
        val vm = createReadyViewModel()
        vm.setDailyGoal(3)
        val goalDeadline = System.currentTimeMillis() + 3000L
        while (vm.uiState.value.dailyGoal != 3 && System.currentTimeMillis() < goalDeadline) {
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            Thread.sleep(20)
        }
        assertEquals(3, vm.uiState.value.dailyGoal)
        assertEquals(0, vm.uiState.value.dailySolvedToday)

        vm.startDaily()
        ShadowLooper.idleMainLooper()
        assertEquals(com.example.ui.viewmodel.PuzzleMode.DAILY, vm.uiState.value.mode)

        fun solveOneAndWaitShift(expectDailyDoneNow: Boolean, expectedTodayCount: Int) {
            val prevId = vm.uiState.value.currentPuzzle!!.id
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
            assertEquals(expectedTodayCount, vm.uiState.value.dailySolvedToday)
            assertEquals(expectDailyDoneNow, vm.uiState.value.justCompletedDailyChallenge)

            var waited = 0L
            while (vm.uiState.value.currentPuzzle?.id == prevId && waited < 2000L) {
                ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                waited += 100L
            }
        }

        solveOneAndWaitShift(expectDailyDoneNow = false, expectedTodayCount = 1)
        solveOneAndWaitShift(expectDailyDoneNow = false, expectedTodayCount = 2)
        solveOneAndWaitShift(expectDailyDoneNow = true, expectedTodayCount = 3)
        assertTrue(vm.uiState.value.dailySolvedToday >= vm.uiState.value.dailyGoal)
    }

    @Test
    fun bestStreakIndicator_syncsAcrossHomeAndSessionStatsViewModels_andSurvivesBackup() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = KnightScopeRepository(context)
        val homeVm = com.example.ui.viewmodel.HomeViewModel(repo)
        val aboutVm = com.example.ui.viewmodel.AboutViewModel(repo)

        kotlinx.coroutines.runBlocking {
            repo.updateBestPuzzleStreak(7)
        }

        val deadline = System.currentTimeMillis() + 3000L
        while ((homeVm.uiState.value.bestPuzzleStreak < 7 || aboutVm.uiState.value.bestPuzzleStreak < 7) &&
            System.currentTimeMillis() < deadline
        ) {
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            Thread.sleep(20)
        }

        assertEquals(7, homeVm.uiState.value.bestPuzzleStreak)
        assertEquals(7, aboutVm.uiState.value.bestPuzzleStreak)

        val backupJson = kotlinx.coroutines.runBlocking {
            val exported = repo.exportBackupJson()
            assertTrue(exported.contains("\"bestPuzzleStreak\": 7"))
            repo.updateBestPuzzleStreak(9)
            exported
        }
        // Restoring an older backup (7) never downgrades higher record (9), and restoring a higher record upgrades it
        kotlinx.coroutines.runBlocking {
            repo.importBackupJson(backupJson.replace("\"bestPuzzleStreak\": 7", "\"bestPuzzleStreak\": 12"))
        }
        val upDeadline = System.currentTimeMillis() + 3000L
        while (homeVm.uiState.value.bestPuzzleStreak < 12 && System.currentTimeMillis() < upDeadline) {
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            Thread.sleep(20)
        }
        assertEquals(12, homeVm.uiState.value.bestPuzzleStreak)
        assertEquals(12, aboutVm.uiState.value.bestPuzzleStreak)
    }

    @Test
    fun homeDailyGoal_persistsCustomTargets_clampsBounds_andSyncsOnSolve() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = KnightScopeRepository(context)
        val homeVm = com.example.ui.viewmodel.HomeViewModel(repo)
        val puzzlesVm = PuzzlesViewModel(repo)

        val readyDeadline = System.currentTimeMillis() + 5000L
        while (puzzlesVm.uiState.value.isLoadingPool && System.currentTimeMillis() < readyDeadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }

        // Custom target (7) persists and syncs across HomeViewModel & PuzzlesViewModel
        homeVm.setDailyGoal(7)
        val customDeadline = System.currentTimeMillis() + 3000L
        while ((homeVm.uiState.value.dailyGoal != 7 || puzzlesVm.uiState.value.dailyGoal != 7) &&
            System.currentTimeMillis() < customDeadline
        ) {
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            Thread.sleep(20)
        }
        assertEquals(7, homeVm.uiState.value.dailyGoal)
        assertEquals(7, puzzlesVm.uiState.value.dailyGoal)

        // Lower bound clamps 0 -> 1
        homeVm.setDailyGoal(0)
        val minDeadline = System.currentTimeMillis() + 3000L
        while (homeVm.uiState.value.dailyGoal != 1 && System.currentTimeMillis() < minDeadline) {
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            Thread.sleep(20)
        }
        assertEquals(1, homeVm.uiState.value.dailyGoal)

        // Upper bound clamps 250 -> 100
        homeVm.setDailyGoal(250)
        val maxDeadline = System.currentTimeMillis() + 3000L
        while (homeVm.uiState.value.dailyGoal != 100 && System.currentTimeMillis() < maxDeadline) {
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            Thread.sleep(20)
        }
        assertEquals(100, homeVm.uiState.value.dailyGoal)

        // Set practical goal of 2 and solve a puzzle -> Home dailySolvedToday and 7-day strip update reactively
        homeVm.setDailyGoal(2)
        val setTwoDeadline = System.currentTimeMillis() + 3000L
        while ((homeVm.uiState.value.dailyGoal != 2 || puzzlesVm.uiState.value.dailyGoal != 2) &&
            System.currentTimeMillis() < setTwoDeadline
        ) {
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            Thread.sleep(20)
        }
        val beforeToday = homeVm.uiState.value.dailySolvedToday

        puzzlesVm.startDaily()
        ShadowLooper.idleMainLooper()
        var safety = 0
        while (!puzzlesVm.uiState.value.isFinished && safety < 10) {
            val s = puzzlesVm.uiState.value
            val cur = s.currentPuzzle!!
            val step = s.solStepIndex
            val expectedUci = cur.solution.getOrNull(step) ?: break
            puzzlesVm.onSquareClick(expectedUci.substring(0, 2))
            puzzlesVm.onSquareClick(expectedUci.substring(2, 4))
            ShadowLooper.idleMainLooper()
            if (!puzzlesVm.uiState.value.isFinished && expectedUci.length == 5 && puzzlesVm.uiState.value.pendingPromotion != null) {
                val promo = com.example.chess.PieceType.fromChar(expectedUci[4]) ?: com.example.chess.PieceType.QUEEN
                puzzlesVm.selectPromotion(promo)
                ShadowLooper.idleMainLooper()
            }
            if (!puzzlesVm.uiState.value.isFinished) {
                var waited = 0L
                while (puzzlesVm.uiState.value.solStepIndex == step + 1 && !puzzlesVm.uiState.value.isFinished && waited < 1500L) {
                    ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
                    waited += 100L
                }
            }
            safety++
        }

        val syncDeadline = System.currentTimeMillis() + 3000L
        while (homeVm.uiState.value.dailySolvedToday <= beforeToday && System.currentTimeMillis() < syncDeadline) {
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            Thread.sleep(20)
        }
        assertEquals(beforeToday + 1, homeVm.uiState.value.dailySolvedToday)
        assertEquals(7, homeVm.uiState.value.weeklyDailyGoalDays.size)
        val todayEntry = homeVm.uiState.value.weeklyDailyGoalDays.last()
        assertTrue(todayEntry.isToday)
        assertEquals(beforeToday + 1, todayEntry.solvedCount)
    }

    @Test
    fun computeDailyGoalMetrics_calculatesStreakAndRollingSevenDays() {
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 24, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val nowMs = cal.timeInMillis
        val dayMs = 24L * 60L * 60L * 1000L

        val attempts = listOf(
            // 2 days ago: 3 solved (target 3 met)
            com.example.chess.PuzzleAttempt("a1", nowMs - 2 * dayMs, ok = true, rating = 1200),
            com.example.chess.PuzzleAttempt("a2", nowMs - 2 * dayMs + 1000, ok = true, rating = 1210),
            com.example.chess.PuzzleAttempt("a3", nowMs - 2 * dayMs + 2000, ok = true, rating = 1220),
            // Yesterday: 3 solved + 1 failed (target 3 met)
            com.example.chess.PuzzleAttempt("b1", nowMs - dayMs, ok = true, rating = 1230),
            com.example.chess.PuzzleAttempt("b2", nowMs - dayMs + 1000, ok = true, rating = 1240),
            com.example.chess.PuzzleAttempt("b3", nowMs - dayMs + 2000, ok = false, rating = 1240),
            com.example.chess.PuzzleAttempt("b4", nowMs - dayMs + 3000, ok = true, rating = 1250),
            // Today: 1 solved so far (in progress -> yesterday's 2-day streak remains active)
            com.example.chess.PuzzleAttempt("c1", nowMs, ok = true, rating = 1260)
        )

        val (solvedInProgress, streakInProgress, weekInProgress) =
            com.example.ui.viewmodel.HomeViewModel.computeDailyGoalMetrics(attempts, dailyGoal = 3, nowMs = nowMs)
        assertEquals(1, solvedInProgress)
        assertEquals(2, streakInProgress)
        assertEquals(7, weekInProgress.size)
        assertTrue(weekInProgress.last().isToday)
        assertFalse(weekInProgress.last().targetMet)
        assertTrue(weekInProgress[5].targetMet)
        assertTrue(weekInProgress[4].targetMet)

        // Complete today's 3rd puzzle -> streak advances to 3 and today's chip marks targetMet = true
        val completedAttempts = attempts + listOf(
            com.example.chess.PuzzleAttempt("c2", nowMs + 1000, ok = true, rating = 1270),
            com.example.chess.PuzzleAttempt("c3", nowMs + 2000, ok = true, rating = 1280)
        )
        val (solvedDone, streakDone, weekDone) =
            com.example.ui.viewmodel.HomeViewModel.computeDailyGoalMetrics(completedAttempts, dailyGoal = 3, nowMs = nowMs)
        assertEquals(3, solvedDone)
        assertEquals(3, streakDone)
        assertTrue(weekDone.last().targetMet)
    }

    @Test
    fun difficultyFilter_togglesBetweenBeginnerIntermediateAdvanced_persistsAndFiltersDailyChallenge() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = KnightScopeRepository(context)
        val puzzlesVm = PuzzlesViewModel(repo)
        val homeVm = com.example.ui.viewmodel.HomeViewModel(repo)

        val readyDeadline = System.currentTimeMillis() + 5000L
        while (puzzlesVm.uiState.value.isLoadingPool && System.currentTimeMillis() < readyDeadline) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(20)
        }
        ShadowLooper.idleMainLooper()

        // Ensure counts are populated for all three difficulty tiers
        val counts = puzzlesVm.uiState.value.difficultyCounts
        assertTrue((counts[com.example.chess.PuzzleDifficultyFilter.BEGINNER] ?: 0) > 0)
        assertTrue((counts[com.example.chess.PuzzleDifficultyFilter.INTERMEDIATE] ?: 0) > 0)
        assertTrue((counts[com.example.chess.PuzzleDifficultyFilter.ADVANCED] ?: 0) > 0)

        // Toggle Beginner on, then start Daily Challenge -> puzzle must match Beginner (0..1399)
        puzzlesVm.toggleDifficultyFilter(com.example.chess.PuzzleDifficultyFilter.BEGINNER)
        ShadowLooper.idleMainLooper()
        assertEquals(com.example.chess.PuzzleDifficultyFilter.BEGINNER, puzzlesVm.uiState.value.difficultyFilter)

        puzzlesVm.startDaily()
        ShadowLooper.idleMainLooper()
        val beginnerPuzzle = puzzlesVm.uiState.value.currentPuzzle
        assertNotNull(beginnerPuzzle)
        assertTrue(
            "Expected Beginner puzzle rating <= 1399, got ${beginnerPuzzle!!.rating}",
            beginnerPuzzle.rating in 0..1399
        )

        // Switch mid-session to Intermediate -> immediately loads Intermediate (1400..1999)
        puzzlesVm.setDifficultyFilter(com.example.chess.PuzzleDifficultyFilter.INTERMEDIATE)
        ShadowLooper.idleMainLooper()
        assertEquals(com.example.chess.PuzzleDifficultyFilter.INTERMEDIATE, puzzlesVm.uiState.value.difficultyFilter)
        val intermediatePuzzle = puzzlesVm.uiState.value.currentPuzzle
        assertNotNull(intermediatePuzzle)
        assertTrue(
            "Expected Intermediate puzzle rating in 1400..1999, got ${intermediatePuzzle!!.rating}",
            intermediatePuzzle.rating in 1400..1999
        )

        // Switch mid-session to Advanced -> immediately loads Advanced (2000+)
        puzzlesVm.setDifficultyFilter(com.example.chess.PuzzleDifficultyFilter.ADVANCED)
        ShadowLooper.idleMainLooper()
        assertEquals(com.example.chess.PuzzleDifficultyFilter.ADVANCED, puzzlesVm.uiState.value.difficultyFilter)
        val advancedPuzzle = puzzlesVm.uiState.value.currentPuzzle
        assertNotNull(advancedPuzzle)
        assertTrue(
            "Expected Advanced puzzle rating >= 2000, got ${advancedPuzzle!!.rating}",
            advancedPuzzle.rating >= 2000
        )

        // Sync check: HomeViewModel reflects persisted Advanced difficulty filter
        val syncDeadline = System.currentTimeMillis() + 3000L
        while (homeVm.uiState.value.dailyDifficultyFilter != com.example.chess.PuzzleDifficultyFilter.ADVANCED &&
            System.currentTimeMillis() < syncDeadline
        ) {
            ShadowLooper.idleMainLooper(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            Thread.sleep(20)
        }
        assertEquals(com.example.chess.PuzzleDifficultyFilter.ADVANCED, homeVm.uiState.value.dailyDifficultyFilter)

        // Toggling already-active tier resets filter back to ALL
        puzzlesVm.toggleDifficultyFilter(com.example.chess.PuzzleDifficultyFilter.ADVANCED)
        ShadowLooper.idleMainLooper()
        assertEquals(com.example.chess.PuzzleDifficultyFilter.ALL, puzzlesVm.uiState.value.difficultyFilter)
    }
}
