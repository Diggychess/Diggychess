package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.example.data.UserProfile
import com.example.service.ContactSender
import com.example.service.ContactSubmission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

data class SelfTestRow(
    val name: String,
    val passed: Boolean,
    val detail: String = ""
)

data class AboutUiState(
    val isTesting: Boolean = false,
    val testResults: List<SelfTestRow> = emptyList(),
    val backupMessage: String? = null,
    val userProfile: UserProfile = UserProfile(),
    val bestPuzzleStreak: Int = 0,
    val contactEmail: String = "",
    val isSendingContact: Boolean = false,
    val contactStatus: String? = null,
    val contactIsError: Boolean = false,
    val contactConfigured: Boolean = ContactSender.hasAnyProvider()
)

class AboutViewModel(
    private val repo: KnightScopeRepository,
    private val engine: ChessEngine = ChessEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.userProfileFlow.collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
        viewModelScope.launch {
            repo.bestPuzzleStreakFlow.distinctUntilChanged().collect { best ->
                _uiState.update { it.copy(bestPuzzleStreak = best) }
            }
        }
        viewModelScope.launch {
            repo.contactEmailFlow.collect { email ->
                _uiState.update { it.copy(contactEmail = email) }
            }
        }
    }

    fun sendContactMessage(
        name: String,
        email: String,
        subject: String,
        message: String,
        onClearMessage: () -> Unit
    ) {
        if (!ContactSender.hasAnyProvider()) {
            _uiState.update {
                it.copy(
                    contactStatus = "Contact delivery is unavailable because no email provider is configured.",
                    contactIsError = true
                )
            }
            return
        }

        val cleanName = name.trim()
        val cleanEmail = email.trim()
        val cleanSubject = subject.trim().ifBlank { "Chess Analyzer: AI Coach Feedback" }
        val cleanMessage = message.trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            _uiState.update {
                it.copy(
                    contactStatus = "Please enter a valid email address.",
                    contactIsError = true
                )
            }
            return
        }
        if (cleanMessage.length < 5) {
            _uiState.update {
                it.copy(
                    contactStatus = "Please enter a short message (at least 5 characters).",
                    contactIsError = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSendingContact = true,
                    contactStatus = null,
                    contactIsError = false
                )
            }
            repo.updateContactEmail(cleanEmail)
            val result = ContactSender.send(
                ContactSubmission(
                    senderName = cleanName.ifBlank { "Chess Analyzer Player" },
                    senderEmail = cleanEmail,
                    subject = cleanSubject,
                    message = cleanMessage
                )
            )
            result.fold(
                onSuccess = { msg ->
                    onClearMessage()
                    _uiState.update {
                        it.copy(
                            isSendingContact = false,
                            contactStatus = msg,
                            contactIsError = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSendingContact = false,
                            contactStatus = err.message ?: "Failed to send message.",
                            contactIsError = true
                        )
                    }
                }
            )
        }
    }

    fun clearContactNotice() {
        _uiState.update {
            it.copy(
                contactStatus = null,
                contactIsError = false,
                backupMessage = null
            )
        }
    }

    fun runSelfTest() {
        if (_uiState.value.isTesting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResults = emptyList()) }
            val rows = mutableListOf<SelfTestRow>()

            fun check(name: String, block: () -> Pair<Boolean, String>) {
                val (ok, msg) = runCatching(block).getOrElse { false to (it.message ?: "error") }
                rows.add(SelfTestRow(name, ok, msg))
            }

            fun play(b: ChessBoard, line: String): Move? {
                var last: Move? = null
                for (tok in line.split(' ')) {
                    last = b.makeSanMove(tok) ?: error("illegal $tok")
                }
                return last
            }

            check("Chess rules engine loaded") { true to "" }
            check("Captures") {
                val m = play(ChessBoard(), "e4 d5 exd5")
                (m?.captured == PieceType.PAWN) to "no capture flag"
            }
            check("Castling") {
                val b = ChessBoard()
                play(b, "e4 e5 Nf3 Nc6 Bc4 Bc5 O-O")
                b.toFen().split(' ')[0].contains("RNBQ1RK1") to "king not on g1"
            }
            check("En passant") {
                val m = play(ChessBoard(), "e4 a6 e5 d5 exd6")
                (m?.isEnPassant == true && m.captured == PieceType.PAWN) to "not an en passant capture"
            }
            check("Promotion") {
                val b = ChessBoard("8/P7/8/8/8/8/7k/K7 w - - 0 1")
                val m = b.makeUciMove("a7a8q")
                (m?.promotion == PieceType.QUEEN) to "promotion failed"
            }
            check("Checkmate detection") {
                val b = ChessBoard()
                play(b, "f3 e5 g4 Qh4#")
                b.inCheckmate() to "not detected"
            }
            check("Illegal moves are rejected") {
                (ChessBoard().makeSanMove("e5") == null) to "accepted e5 from start"
            }
            check("PGN import") {
                val parsed = PgnParser.parse("[White \"A\"]\n[Black \"B\"]\n\n1. e4 e5 2. Nf3 Nc6 *")
                (parsed.plies.size == 4) to "wrong move count"
            }
            check("Opening list") {
                val size = OpeningBook.fenMap.size
                (size > 0 && OpeningBook.invalidLineCount == 0) to "${OpeningBook.invalidLineCount} invalid lines"
            }
            check("Opening transposition") {
                fun key(line: String): String {
                    val b = ChessBoard()
                    play(b, line)
                    return b.openingKey()
                }
                (key("d4 d5 Nf3 Nf6 Bf4") == key("d4 Nf6 Nf3 d5 Bf4")) to "different keys"
            }

            _uiState.update { it.copy(testResults = rows.toList()) }

            // Engine checks
            val mateRes = runCatching {
                engine.evaluatePosition("6k1/5ppp/8/8/8/8/5PPP/4R1K1 w - - 0 1", depth = 4, timeLimitMs = 1500, allowCloud = false)
            }.getOrNull()
            rows.add(
                SelfTestRow(
                    "Engine finds mate in 1 (Re8#)",
                    mateRes?.bestUci == "e1e8" && mateRes.mate == 1,
                    "got ${mateRes?.bestUci} mate=${mateRes?.mate}"
                )
            )

            val startRes = runCatching {
                engine.evaluatePosition(ChessBoard.START_FEN, depth = 4, timeLimitMs = 1500, allowCloud = false)
            }.getOrNull()
            rows.add(
                SelfTestRow(
                    "Engine evaluates start position",
                    startRes != null && abs(startRes.cp) < 1.5 && startRes.depth > 0,
                    "cp=${startRes?.cp}"
                )
            )

            _uiState.update { it.copy(isTesting = false, testResults = rows.toList()) }
        }
    }

    suspend fun generateBackupJson(): String = repo.exportBackupJson()

    fun restoreBackupJson(json: String) {
        viewModelScope.launch {
            try {
                repo.importBackupJson(json)
                _uiState.update { it.copy(backupMessage = "Backup restored.") }
            } catch (_: Exception) {
                _uiState.update { it.copy(backupMessage = "This is not a valid backup file.") }
            }
        }
    }

    fun setBackupMessage(msg: String) {
        _uiState.update { it.copy(backupMessage = msg) }
    }
}
