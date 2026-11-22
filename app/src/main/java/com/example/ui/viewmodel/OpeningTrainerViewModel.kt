package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.example.ui.theme.BoardTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RepertoireDrillLine(
    val id: String,
    val eco: String,
    val title: String,
    val subtitle: String,
    val playerColor: PieceColor,
    val sanMoves: List<String>,
    val moveNotes: List<String>
)

data class OpeningTrainerUiState(
    val repertoires: List<RepertoireDrillLine> = OpeningTrainerViewModel.REPERTOIRES,
    val selectedId: String = OpeningTrainerViewModel.REPERTOIRES.first().id,
    val fen: String = ChessBoard.START_FEN,
    val playerColor: PieceColor = PieceColor.WHITE,
    val flipped: Boolean = false,
    val stepIndex: Int = 0,
    val totalSteps: Int = 0,
    val selectedSquare: String? = null,
    val legalTargets: Map<String, Boolean> = emptyMap(),
    val lastMoveSquares: List<String> = emptyList(),
    val hintSquares: List<String> = emptyList(),
    val wrongMoveSquares: List<String> = emptyList(),
    val streak: Int = 0,
    val feedbackText: String = "",
    val feedbackOk: Boolean? = null,
    val coachNote: String = "",
    val isLineComplete: Boolean = false,
    val boardTheme: BoardTheme = BoardTheme.GOLD
)

class OpeningTrainerViewModel(private val repo: KnightScopeRepository) : ViewModel() {

    companion object {
        val REPERTOIRES = listOf(
            RepertoireDrillLine(
                id = "italian_giuoco",
                eco = "C50",
                title = "Italian Game: Giuoco Piano",
                subtitle = "Classic central control & rapid kingside castling",
                playerColor = PieceColor.WHITE,
                sanMoves = listOf("e4", "e5", "Nf3", "Nc6", "Bc4", "Bc5", "c3", "Nf6", "d4"),
                moveNotes = listOf(
                    "Claim central space and open lines for your bishop and queen.",
                    "Black mirrors the center.",
                    "Develop your knight toward the center while attacking e5.",
                    "Black defends e5 with a knight.",
                    "Aim your bishop at Black's vulnerable f7 square.",
                    "Black develops symmetrically.",
                    "Prepare a supported pawn break with d4.",
                    "Black attacks your e4 pawn.",
                    "Strike the center with tempo against Black's bishop!"
                )
            ),
            RepertoireDrillLine(
                id = "ruy_lopez",
                eco = "C70",
                title = "Ruy Lopez: Morphy Defense",
                subtitle = "Put immediate pressure on Black's e5 defender",
                playerColor = PieceColor.WHITE,
                sanMoves = listOf("e4", "e5", "Nf3", "Nc6", "Bb5", "a6", "Ba4", "Nf6", "O-O"),
                moveNotes = listOf(
                    "Stake your claim in the center.",
                    "Black challenges the e-file.",
                    "Develop and attack e5.",
                    "Black guards e5.",
                    "The Spanish Bishop pressures the knight defending e5.",
                    "Black questions the bishop.",
                    "Maintain the diagonal pressure without trading prematurely.",
                    "Black attacks e4.",
                    "Castle early to safeguard your king and activate the rook."
                )
            ),
            RepertoireDrillLine(
                id = "queens_gambit",
                eco = "D30",
                title = "Queen's Gambit Declined",
                subtitle = "Challenge Black's d5 outpost with the c-pawn",
                playerColor = PieceColor.WHITE,
                sanMoves = listOf("d4", "d5", "c4", "e6", "Nc3", "Nf6", "Bg5"),
                moveNotes = listOf(
                    "Establish a rock-solid d4 anchor.",
                    "Black blocks the d4 advance.",
                    "Undermine Black's center with a flank pawn.",
                    "Black bolsters d5 with e6.",
                    "Add pressure to d5 and control e4.",
                    "Black develops the kingside knight.",
                    "Pin the knight defending d5 to Black's queen!"
                )
            ),
            RepertoireDrillLine(
                id = "london_system",
                eco = "D02",
                title = "London System",
                subtitle = "Solid, easy-to-learn dark-square pyramid setup",
                playerColor = PieceColor.WHITE,
                sanMoves = listOf("d4", "d5", "Bf4", "Nf6", "e3", "e6", "Nf3"),
                moveNotes = listOf(
                    "Open with the queen's pawn.",
                    "Black stakes out d5.",
                    "Develop the dark-squared bishop BEFORE locking it in with e3.",
                    "Black develops normally.",
                    "Build the trademark e3-d4-c3 pawn triangle.",
                    "Black prepares bishop development.",
                    "Complete kingside development and guard e5."
                )
            ),
            RepertoireDrillLine(
                id = "sicilian_najdorf",
                eco = "B90",
                title = "Sicilian Defense: Najdorf",
                subtitle = "Sharp, ambitious counterplay against 1. e4",
                playerColor = PieceColor.BLACK,
                sanMoves = listOf("e4", "c5", "Nf3", "d6", "d4", "cxd4", "Nxd4", "Nf6", "Nc3", "a6"),
                moveNotes = listOf(
                    "White opens with 1. e4.",
                    "Fight asymmetrically for the d4 square right away.",
                    "White develops the knight.",
                    "Control e5 and keep your pieces flexible.",
                    "White opens the center.",
                    "Trade a flank c-pawn for White's central d-pawn!",
                    "White recaptures.",
                    "Attack e4 immediately with development.",
                    "White defends e4.",
                    "The Najdorf signature: deny Bb5/Nb5 and prepare ...e5 or ...b5!"
                )
            ),
            RepertoireDrillLine(
                id = "caro_kann",
                eco = "B18",
                title = "Caro-Kann: Classical",
                subtitle = "Solid pawn structure with active light-squared bishop",
                playerColor = PieceColor.BLACK,
                sanMoves = listOf("e4", "c6", "d4", "d5", "Nc3", "dxe4", "Nxe4", "Bf5"),
                moveNotes = listOf(
                    "White opens e4.",
                    "Prepare ...d5 with solid pawn support.",
                    "White takes the full center.",
                    "Challenge White's e4 pawn right away.",
                    "White protects e4.",
                    "Release central tension cleanly.",
                    "White recaptures in the center.",
                    "Develop the bishop outside the pawn chain with tempo on the knight!"
                )
            ),
            RepertoireDrillLine(
                id = "french_classical",
                eco = "C11",
                title = "French Defense: Classical",
                subtitle = "Counterattack White's center from behind a sturdy pawn chain",
                playerColor = PieceColor.BLACK,
                sanMoves = listOf("e4", "e6", "d4", "d5", "Nc3", "Nf6"),
                moveNotes = listOf(
                    "White pushes e4.",
                    "Prepare a backed-up d5 strike.",
                    "White builds a duo.",
                    "Counterattack e4 directly.",
                    "White defends e4.",
                    "Double down on e4 pressure while developing!"
                )
            ),
            RepertoireDrillLine(
                id = "kings_indian",
                eco = "E70",
                title = "King's Indian Defense",
                subtitle = "Hypermodern fianchetto setup ready to strike the center",
                playerColor = PieceColor.BLACK,
                sanMoves = listOf("d4", "Nf6", "c4", "g6", "Nc3", "Bg7", "e4", "d6"),
                moveNotes = listOf(
                    "White opens 1. d4.",
                    "Stop 2. e4 with a flexible knight move.",
                    "White grabs queenside space.",
                    "Prepare to fianchetto your dark-squared bishop onto the long diagonal.",
                    "White develops the queenside knight.",
                    "Place the monster bishop on g7 eyeing d4 and b2.",
                    "White occupies the big center.",
                    "Clamp down on e5 before striking back with ...e5 or ...c5."
                )
            )
        )
    }

    private val _uiState = MutableStateFlow(OpeningTrainerUiState())
    val uiState: StateFlow<OpeningTrainerUiState> = _uiState.asStateFlow()

    private var board = ChessBoard()
    private var autoReplyJob: Job? = null
    private var drillToken = 0
    private var hadMistakeInLine = false

    init {
        viewModelScope.launch {
            repo.openingStreakFlow.collect { s ->
                _uiState.update { it.copy(streak = s) }
            }
        }
        viewModelScope.launch {
            repo.boardThemeFlow.collect { t ->
                _uiState.update { it.copy(boardTheme = t) }
            }
        }
        selectRepertoire(REPERTOIRES.first().id)
    }

    fun selectRepertoire(id: String) {
        val rep = REPERTOIRES.find { it.id == id } ?: return
        autoReplyJob?.cancel()
        drillToken++
        hadMistakeInLine = false
        board = ChessBoard()

        _uiState.update {
            it.copy(
                selectedId = rep.id,
                fen = board.toFen(),
                playerColor = rep.playerColor,
                flipped = (rep.playerColor == PieceColor.BLACK),
                stepIndex = 0,
                totalSteps = rep.sanMoves.size,
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = emptyList(),
                hintSquares = emptyList(),
                wrongMoveSquares = emptyList(),
                feedbackText = if (rep.playerColor == PieceColor.WHITE) {
                    "Your move as White — play ${rep.title}"
                } else {
                    "Playing ${rep.title} as Black…"
                },
                feedbackOk = null,
                coachNote = rep.moveNotes.firstOrNull().orEmpty(),
                isLineComplete = false
            )
        }

        if (rep.playerColor == PieceColor.BLACK) {
            scheduleOpponentReply(rep, 0, drillToken)
        }
    }

    fun restartDrill() {
        selectRepertoire(_uiState.value.selectedId)
    }

    fun nextRepertoire() {
        val idx = REPERTOIRES.indexOfFirst { it.id == _uiState.value.selectedId }
        val next = REPERTOIRES[(idx + 1) % REPERTOIRES.size]
        selectRepertoire(next.id)
    }

    fun requestHint() {
        val s = _uiState.value
        if (s.isLineComplete) return
        val rep = REPERTOIRES.find { it.id == s.selectedId } ?: return
        if (board.turn != rep.playerColor) return
        val expectedSan = rep.sanMoves.getOrNull(s.stepIndex) ?: return
        val copy = board.copy()
        val mv = copy.makeSanMove(expectedSan) ?: return
        _uiState.update {
            it.copy(
                hintSquares = listOf(mv.fromName, mv.toName),
                feedbackText = "Hint: play $expectedSan (${rep.moveNotes.getOrNull(s.stepIndex).orEmpty()})",
                feedbackOk = null
            )
        }
    }

    fun onSquareClick(sqName: String) {
        val s = _uiState.value
        val rep = REPERTOIRES.find { it.id == s.selectedId } ?: return
        if (s.isLineComplete || board.turn != rep.playerColor) return

        val sel = s.selectedSquare
        if (sel != null) {
            val fromIdx = parseSquare(sel)
            val toIdx = parseSquare(sqName)
            if (fromIdx != null && toIdx != null) {
                val matching = board.legalMoves(fromIdx).filter { it.to == toIdx }
                if (matching.isNotEmpty()) {
                    tryUserMove(rep, sel, sqName)
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

    private fun tryUserMove(rep: RepertoireDrillLine, from: String, to: String) {
        val step = _uiState.value.stepIndex
        val expectedSan = rep.sanMoves.getOrNull(step) ?: return
        val expectedBoard = board.copy()
        val expectedMove = expectedBoard.makeSanMove(expectedSan) ?: return

        if (expectedMove.fromName == from && expectedMove.toName == to) {
            if (!board.makeMove(expectedMove)) return
            val note = rep.moveNotes.getOrNull(step).orEmpty()
            val nextStep = step + 1
            val done = nextStep >= rep.sanMoves.size

            if (done) {
                if (!hadMistakeInLine) {
                    viewModelScope.launch { repo.updateOpeningStreak(true) }
                }
                _uiState.update {
                    it.copy(
                        fen = board.toFen(),
                        stepIndex = nextStep,
                        selectedSquare = null,
                        legalTargets = emptyMap(),
                        lastMoveSquares = listOf(expectedMove.fromName, expectedMove.toName),
                        hintSquares = emptyList(),
                        wrongMoveSquares = emptyList(),
                        isLineComplete = true,
                        feedbackOk = true,
                        feedbackText = "Line mastered! ${rep.eco} ${rep.title} complete.",
                        coachNote = note
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        fen = board.toFen(),
                        stepIndex = nextStep,
                        selectedSquare = null,
                        legalTargets = emptyMap(),
                        lastMoveSquares = listOf(expectedMove.fromName, expectedMove.toName),
                        hintSquares = emptyList(),
                        wrongMoveSquares = emptyList(),
                        feedbackOk = true,
                        feedbackText = "✔ Correct ($expectedSan)!",
                        coachNote = note
                    )
                }
                scheduleOpponentReply(rep, nextStep, drillToken)
            }
        } else {
            hadMistakeInLine = true
            viewModelScope.launch { repo.updateOpeningStreak(false) }
            _uiState.update {
                it.copy(
                    selectedSquare = null,
                    legalTargets = emptyMap(),
                    wrongMoveSquares = listOf(from, to),
                    feedbackOk = false,
                    feedbackText = "Not the book move for ${rep.title}. Try again or tap Hint.",
                    coachNote = rep.moveNotes.getOrNull(step).orEmpty()
                )
            }
        }
    }

    private fun scheduleOpponentReply(rep: RepertoireDrillLine, replyIndex: Int, token: Int) {
        val replySan = rep.sanMoves.getOrNull(replyIndex) ?: return
        autoReplyJob?.cancel()
        autoReplyJob = viewModelScope.launch {
            delay(380)
            if (drillToken != token) return@launch
            val mv = board.makeSanMove(replySan) ?: return@launch
            val nextStep = replyIndex + 1
            val done = nextStep >= rep.sanMoves.size
            if (done && !hadMistakeInLine) {
                repo.updateOpeningStreak(true)
            }
            _uiState.update {
                it.copy(
                    fen = board.toFen(),
                    stepIndex = nextStep,
                    lastMoveSquares = listOf(mv.fromName, mv.toName),
                    isLineComplete = done,
                    coachNote = rep.moveNotes.getOrNull(replyIndex).orEmpty(),
                    feedbackText = if (done) {
                        "Line mastered! ${rep.eco} ${rep.title} complete."
                    } else {
                        "Opponent played $replySan — your move!"
                    },
                    feedbackOk = if (done) true else null
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoReplyJob?.cancel()
    }
}
