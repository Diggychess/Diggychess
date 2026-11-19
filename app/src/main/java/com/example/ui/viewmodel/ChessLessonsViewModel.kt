package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.example.ui.theme.BoardTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LessonStep(
    val title: String,
    val prompt: String,
    val fen: String,
    val solutionUci: String,
    val hintText: String,
    val successExplanation: String
)

data class ChessLesson(
    val id: String,
    val title: String,
    val levelTag: String,
    val summary: String,
    val steps: List<LessonStep>
)

data class ChessLessonsUiState(
    val lessons: List<ChessLesson> = ChessLessonsViewModel.CURRICULUM,
    val completedLessonIds: Set<String> = emptySet(),
    val activeLessonId: String? = null,
    val stepIndex: Int = 0,
    val fen: String = ChessBoard.START_FEN,
    val selectedSquare: String? = null,
    val legalTargets: Map<String, Boolean> = emptyMap(),
    val lastMoveSquares: List<String> = emptyList(),
    val hintSquares: List<String> = emptyList(),
    val wrongMoveSquares: List<String> = emptyList(),
    val stepSolved: Boolean = false,
    val lessonFinished: Boolean = false,
    val feedbackText: String = "",
    val feedbackOk: Boolean? = null,
    val boardTheme: BoardTheme = BoardTheme.GREEN,
    val pendingPromotion: Pair<String, String>? = null
)

class ChessLessonsViewModel(private val repo: KnightScopeRepository) : ViewModel() {

    companion object {
        val CURRICULUM = listOf(
            ChessLesson(
                id = "lesson_forks",
                title = "1. Mastering Knight Forks",
                levelTag = "Tactics · Beginner",
                summary = "Attack two valuable pieces at once with a single knight jump.",
                steps = listOf(
                    LessonStep(
                        title = "Royal Knight Fork",
                        prompt = "Find the knight move that checks the Black king on e7 AND attacks the queen on c7.",
                        fen = "8/2q1k3/8/8/8/2N5/8/4K3 w - - 0 1",
                        solutionUci = "c3d5",
                        hintText = "Look at d5 — it lands with check and aims at c7.",
                        successExplanation = "1. Nd5+! The king must evade check while the undefended queen on c7 falls next move."
                    ),
                    LessonStep(
                        title = "King & Rook Fork",
                        prompt = "Win Black's rook on e8 with a forcing knight check.",
                        fen = "4r1k1/7p/8/3N4/8/8/8/6K1 w - - 0 1",
                        solutionUci = "d5f6",
                        hintText = "Jump toward f6 — neither king nor pawn can take there.",
                        successExplanation = "1. Nf6+ forks g8 and e8 cleanly because the g7 pawn is gone."
                    ),
                    LessonStep(
                        title = "The Family Fork",
                        prompt = "Fork Black's King, Queen, and Rook in a single move!",
                        fen = "r3k3/8/4q3/1N6/8/8/8/6K1 w - - 0 1",
                        solutionUci = "b5c7",
                        hintText = "Jump to c7 to hit a8, e8, and e6 simultaneously.",
                        successExplanation = "1. Nc7+! That rare triple 'family fork' wins at least the queen."
                    )
                )
            ),
            ChessLesson(
                id = "lesson_pins",
                title = "2. Weaponizing Absolute Pins",
                levelTag = "Tactics · Beginner",
                summary = "Freeze defenders in front of the king, then pile up pressure.",
                steps = listOf(
                    LessonStep(
                        title = "Create an Absolute Pin",
                        prompt = "Pin Black's rook on f7 to the king on g8.",
                        fen = "6k1/5r2/8/1B6/8/8/8/6K1 w - - 0 1",
                        solutionUci = "b5c4",
                        hintText = "Step onto the a2–g8 diagonal with your bishop.",
                        successExplanation = "1. Bc4! Because moving the rook exposes the king, the rook is frozen and will be captured."
                    ),
                    LessonStep(
                        title = "Attack the Pinned Piece",
                        prompt = "Your bishop on g2 already pins c6 to a8. Attack the pinned knight with a pawn!",
                        fen = "k7/8/2n5/8/1P6/8/6B1/6K1 w - - 0 1",
                        solutionUci = "b4b5",
                        hintText = "Push your b-pawn so a minor pawn attacks the frozen knight.",
                        successExplanation = "1. b5! Putting a low-value attacker against an immovable pinned piece wins material cleanly."
                    ),
                    LessonStep(
                        title = "Supported Rook Pin",
                        prompt = "Pin Black's queen on e7 to the king on e8 using your bishop-backed rook.",
                        fen = "4k3/4q3/8/8/8/2B5/8/3R2K1 w - - 0 1",
                        solutionUci = "d1e1",
                        hintText = "Slide your rook to e1 where your c3 bishop defends it.",
                        successExplanation = "1. Re1! Since c3 backs up e1, Black cannot take the rook without losing the queen."
                    )
                )
            ),
            ChessLesson(
                id = "lesson_skewers",
                title = "3. Skewers & X-Ray Attacks",
                levelTag = "Tactics · Intermediate",
                summary = "Force a high-value piece to step aside and win what stands behind it.",
                steps = listOf(
                    LessonStep(
                        title = "Supported Bishop Skewer",
                        prompt = "Check the Black king on c6 so it must expose the rook on e8.",
                        fen = "4r3/8/2k5/8/P1B5/8/8/6K1 w - - 0 1",
                        solutionUci = "c4b5",
                        hintText = "Move your bishop to b5 — your a4 pawn protects it.",
                        successExplanation = "1. Bb5+! The king cannot capture on b5 and must flee, dropping the e8 rook."
                    ),
                    LessonStep(
                        title = "Back-Rank Skewer",
                        prompt = "Skewer the Black king on c8 and the queen on h8 along the 8th rank!",
                        fen = "2k4q/8/8/8/8/8/8/R5K1 w - - 0 1",
                        solutionUci = "a1a8",
                        hintText = "Infiltrate all the way to a8 with check.",
                        successExplanation = "1. Ra8+! The king blocks the queen from covering a8, forcing the king off the rank."
                    )
                )
            ),
            ChessLesson(
                id = "lesson_backrank",
                title = "4. Back-Rank Checkmate Patterns",
                levelTag = "Checkmate · Beginner",
                summary = "Spot trapped kings behind unmoved pawns and punish weak back ranks.",
                steps = listOf(
                    LessonStep(
                        title = "The Classic Trap",
                        prompt = "Black's pawns keep their own king boxed in. Deliver checkmate in one move.",
                        fen = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
                        solutionUci = "d1d8",
                        hintText = "Send your rook all the way to d8.",
                        successExplanation = "1. Rd8#! Always watch out for back-rank weakness when pawns stay on f7-g7-h7."
                    ),
                    LessonStep(
                        title = "Battery Crash-Through",
                        prompt = "Use your Queen + Rook battery on the e-file to force back-rank mate.",
                        fen = "4r1k1/5ppp/8/8/8/4Q3/5PPP/4R1K1 w - - 0 1",
                        solutionUci = "e3e8",
                        hintText = "Capture on e8 with the queen because your e1 rook backs it up!",
                        successExplanation = "1. Qxe8#! With the battery supporting e8, the king has zero escape squares."
                    )
                )
            ),
            ChessLesson(
                id = "lesson_opening_principles",
                title = "5. Three Golden Opening Rules",
                levelTag = "Strategy · Beginner",
                summary = "Control the center, develop knights before bishops, and castle early.",
                steps = listOf(
                    LessonStep(
                        title = "Rule 1: Develop with Tempo",
                        prompt = "After 1. e4 e5, bring out a knight toward the center while attacking Black's e5 pawn.",
                        fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                        solutionUci = "g1f3",
                        hintText = "Develop your kingside knight to f3.",
                        successExplanation = "2. Nf3 controls d4 & e5 and prepares kingside castling."
                    ),
                    LessonStep(
                        title = "Rule 2: Aim at Weak Squares",
                        prompt = "Develop your light-squared bishop to target f7 while clearing the kingside.",
                        fen = "r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 2 3",
                        solutionUci = "f1c4",
                        hintText = "Place your bishop on c4.",
                        successExplanation = "3. Bc4 targets f7 and empties f1 so you can castle next!"
                    ),
                    LessonStep(
                        title = "Rule 3: King Safety First",
                        prompt = "Tuck your king away and activate your h1 rook in a single move.",
                        fen = "r1bqk1nr/pppp1ppp/2n5/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4",
                        solutionUci = "e1g1",
                        hintText = "Castle kingside (move your king from e1 to g1).",
                        successExplanation = "4. O-O! Rapid castling shields your king before the center opens."
                    )
                )
            ),
            ChessLesson(
                id = "lesson_endgame",
                title = "6. King & Pawn Opposition",
                levelTag = "Endgame · Intermediate",
                summary = "Lead with your king in pawn endgames to win the promotion race.",
                steps = listOf(
                    LessonStep(
                        title = "Lead with the King",
                        prompt = "Step directly in front of your pawn to seize opposition against the Black king!",
                        fen = "4k3/8/8/3K4/4P3/8/8/8 w - - 0 1",
                        solutionUci = "d5e6",
                        hintText = "Move your king to e6, facing Black's king on e8.",
                        successExplanation = "1. Ke6! With direct opposition in front of the pawn, Black must give way so your pawn promotes."
                    ),
                    LessonStep(
                        title = "Promote to Victory",
                        prompt = "Escort the passed pawn to the 8th rank and crown a new Queen!",
                        fen = "7k/4P3/5K2/8/8/8/8/8 w - - 0 1",
                        solutionUci = "e7e8q",
                        hintText = "Advance the e7 pawn to e8 and promote to a Queen.",
                        successExplanation = "1. e8=Q+! Your king defended the key f7 and g7 squares to guarantee promotion."
                    )
                )
            )
        )
    }

    private val _uiState = MutableStateFlow(ChessLessonsUiState())
    val uiState: StateFlow<ChessLessonsUiState> = _uiState.asStateFlow()

    private var board = ChessBoard()

    init {
        viewModelScope.launch {
            repo.completedLessonsFlow.collect { done ->
                _uiState.update { it.copy(completedLessonIds = done) }
            }
        }
        viewModelScope.launch {
            repo.boardThemeFlow.collect { theme ->
                _uiState.update { it.copy(boardTheme = theme) }
            }
        }
    }

    fun openLesson(lessonId: String) {
        val lesson = CURRICULUM.find { it.id == lessonId } ?: return
        loadStep(lesson, 0)
    }

    fun closeLesson() {
        _uiState.update { it.copy(activeLessonId = null, lessonFinished = false) }
    }

    fun nextStep() {
        val s = _uiState.value
        val lesson = CURRICULUM.find { it.id == s.activeLessonId } ?: return
        val next = s.stepIndex + 1
        if (next < lesson.steps.size) {
            loadStep(lesson, next)
        } else {
            closeLesson()
        }
    }

    fun retryStep() {
        val s = _uiState.value
        val lesson = CURRICULUM.find { it.id == s.activeLessonId } ?: return
        loadStep(lesson, s.stepIndex)
    }

    fun showHint() {
        val s = _uiState.value
        if (s.stepSolved) return
        val lesson = CURRICULUM.find { it.id == s.activeLessonId } ?: return
        val step = lesson.steps.getOrNull(s.stepIndex) ?: return
        _uiState.update {
            it.copy(
                hintSquares = listOf(step.solutionUci.take(2), step.solutionUci.substring(2, 4)),
                feedbackOk = null,
                feedbackText = "Hint: ${step.hintText}"
            )
        }
    }

    private fun loadStep(lesson: ChessLesson, idx: Int) {
        val step = lesson.steps[idx]
        board = ChessBoard(step.fen)
        _uiState.update {
            it.copy(
                activeLessonId = lesson.id,
                stepIndex = idx,
                fen = step.fen,
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = emptyList(),
                hintSquares = emptyList(),
                wrongMoveSquares = emptyList(),
                stepSolved = false,
                lessonFinished = false,
                feedbackText = "",
                feedbackOk = null,
                pendingPromotion = null
            )
        }
    }

    fun onSquareClick(sqName: String) {
        val s = _uiState.value
        if (s.stepSolved) return
        val lesson = CURRICULUM.find { it.id == s.activeLessonId } ?: return
        val step = lesson.steps.getOrNull(s.stepIndex) ?: return

        val sel = s.selectedSquare
        if (sel != null) {
            val fromIdx = parseSquare(sel)
            val toIdx = parseSquare(sqName)
            if (fromIdx != null && toIdx != null) {
                val matching = board.legalMoves(fromIdx).filter { it.to == toIdx }
                if (matching.isNotEmpty()) {
                    val isPromo = matching.any { it.promotion != null }
                    if (isPromo && step.solutionUci.startsWith(sel + sqName)) {
                        val p = step.solutionUci.getOrNull(4)?.let { PieceType.fromChar(it) } ?: PieceType.QUEEN
                        executeMove(lesson, step, sel, sqName, p)
                    } else if (isPromo) {
                        _uiState.update { it.copy(pendingPromotion = sel to sqName) }
                    } else {
                        executeMove(lesson, step, sel, sqName, null)
                    }
                    return
                }
            }
        }

        val sqIdx = parseSquare(sqName) ?: return
        val piece = board.pieceAt(sqIdx)
        if (piece != null && piece.color == board.turn && sqName != sel) {
            val targets = board.legalMoves(sqIdx).associate { it.toName to (it.captured != null) }
            _uiState.update {
                it.copy(
                    selectedSquare = sqName,
                    legalTargets = targets,
                    wrongMoveSquares = emptyList()
                )
            }
        } else {
            _uiState.update { it.copy(selectedSquare = null, legalTargets = emptyMap()) }
        }
    }

    fun selectPromotion(type: PieceType) {
        val pending = _uiState.value.pendingPromotion ?: return
        val lesson = CURRICULUM.find { it.id == _uiState.value.activeLessonId } ?: return
        val step = lesson.steps.getOrNull(_uiState.value.stepIndex) ?: return
        _uiState.update { it.copy(pendingPromotion = null) }
        executeMove(lesson, step, pending.first, pending.second, type)
    }

    fun dismissPromotion() {
        _uiState.update { it.copy(pendingPromotion = null) }
    }

    private fun executeMove(
        lesson: ChessLesson,
        step: LessonStep,
        from: String,
        to: String,
        promo: PieceType?
    ) {
        val attempted = from + to + (promo?.symbol?.toString() ?: "")
        if (attempted == step.solutionUci) {
            board.makeUciMove(attempted) ?: return
            val isLast = _uiState.value.stepIndex == lesson.steps.lastIndex
            if (isLast) {
                viewModelScope.launch { repo.markLessonCompleted(lesson.id) }
            }
            _uiState.update {
                it.copy(
                    fen = board.toFen(),
                    selectedSquare = null,
                    legalTargets = emptyMap(),
                    lastMoveSquares = listOf(from, to),
                    hintSquares = emptyList(),
                    wrongMoveSquares = emptyList(),
                    stepSolved = true,
                    lessonFinished = isLast,
                    feedbackOk = true,
                    feedbackText = step.successExplanation
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedSquare = null,
                    legalTargets = emptyMap(),
                    wrongMoveSquares = listOf(from, to),
                    feedbackOk = false,
                    feedbackText = "Not quite! Look carefully at the prompt or tap Hint."
                )
            }
        }
    }
}
