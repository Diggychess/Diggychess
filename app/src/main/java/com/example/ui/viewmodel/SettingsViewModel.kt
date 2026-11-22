package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.example.service.ContactSender
import com.example.service.ContactSubmission
import com.example.ui.theme.BoardTheme
import com.example.ui.theme.PieceStyle
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class AnalysisPreset(val label: String, val depth: Int, val subtitle: String) {
    FAST("Fast", 4, "Instant review"),
    BALANCED("Balanced", 6, "Recommended accuracy"),
    DEEP("Deep", 8, "Thorough tactical check")
}

data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val boardTheme: BoardTheme = BoardTheme.GOLD,
    val pieceStyle: PieceStyle = PieceStyle.NEO,
    val board3dEnabled: Boolean = false,
    val pieceAnimations: Boolean = true,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reviewSettings: ReviewSettings = ReviewSettings(),
    val dailyGoal: Int = 5,
    val isTesting: Boolean = false,
    val testResults: List<SelfTestRow> = emptyList(),
    val backupMessage: String? = null,
    val contactEmail: String = "",
    val isSendingContact: Boolean = false,
    val contactStatus: String? = null,
    val contactIsError: Boolean = false,
    val contactConfigured: Boolean = ContactSender.hasAnyProvider()
)

class SettingsViewModel(
    private val repo: KnightScopeRepository,
    private val engine: ChessEngine = ChessEngine()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.darkModeFlow.collect { dark ->
                _uiState.update { it.copy(isDarkMode = dark) }
            }
        }
        viewModelScope.launch {
            repo.boardThemeFlow.collect { theme ->
                _uiState.update { it.copy(boardTheme = theme) }
            }
        }
        viewModelScope.launch {
            repo.pieceAnimationsFlow.collect { anim ->
                _uiState.update { it.copy(pieceAnimations = anim) }
            }
        }
        viewModelScope.launch {
            repo.pieceStyleFlow.collect { style ->
                _uiState.update { it.copy(pieceStyle = style) }
            }
        }
        viewModelScope.launch {
            repo.board3dEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(board3dEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            repo.soundEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(soundEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            repo.hapticsEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(hapticsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            repo.settingsFlow.collect { s ->
                _uiState.update { it.copy(reviewSettings = s) }
            }
        }
        viewModelScope.launch {
            repo.dailyGoalFlow.collect { goal ->
                _uiState.update { it.copy(dailyGoal = goal) }
            }
        }
        viewModelScope.launch {
            repo.contactEmailFlow.collect { email ->
                _uiState.update { it.copy(contactEmail = email) }
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { repo.setDarkMode(enabled) }
    }

    fun setBoardTheme(theme: BoardTheme) {
        viewModelScope.launch { repo.setBoardTheme(theme) }
    }

    fun setPieceStyle(style: PieceStyle) {
        viewModelScope.launch { repo.setPieceStyle(style) }
    }

    fun setBoard3dEnabled(enabled: Boolean) {
        viewModelScope.launch { repo.setBoard3dEnabled(enabled) }
    }

    fun setPieceAnimations(enabled: Boolean) {
        viewModelScope.launch { repo.setPieceAnimations(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repo.setSoundEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { repo.setHapticsEnabled(enabled) }
    }

    fun setAnalysisPreset(preset: AnalysisPreset) {
        val next = _uiState.value.reviewSettings.copy(depth = preset.depth)
        viewModelScope.launch { repo.saveSettings(next) }
    }

    fun setDailyGoal(goal: Int) {
        viewModelScope.launch { repo.setDailyGoal(goal) }
    }

    suspend fun generateBackupJson(): String = repo.exportBackupJson()

    fun restoreBackupJson(json: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repo.importBackupJson(json)
                onDone()
                _uiState.update { it.copy(backupMessage = "Backup restored successfully.") }
            } catch (_: Exception) {
                _uiState.update { it.copy(backupMessage = "This is not a valid backup file.") }
            }
        }
    }

    fun setBackupMessage(msg: String?) {
        _uiState.update { it.copy(backupMessage = msg) }
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
            check("Captures, Castling & En passant") {
                val b = ChessBoard()
                play(b, "e4 e5 Nf3 Nc6 Bc4 Bc5 O-O")
                val castled = b.toFen().split(' ')[0].contains("RNBQ1RK1")
                val ep = play(ChessBoard(), "e4 a6 e5 d5 exd6")
                (castled && ep?.isEnPassant == true) to "rule validation failed"
            }
            check("Checkmate & PGN parser") {
                val b = ChessBoard()
                play(b, "f3 e5 g4 Qh4#")
                val parsed = PgnParser.parse("1. e4 e5 2. Nf3 Nc6 *")
                (b.inCheckmate() && parsed.plies.size == 4) to "checkmate/PGN mismatch"
            }
            check("Opening list & transpositions") {
                fun key(line: String): String {
                    val b = ChessBoard()
                    play(b, line)
                    return b.openingKey()
                }
                (OpeningBook.fenMap.isNotEmpty() && OpeningBook.invalidLineCount == 0 &&
                    key("d4 d5 Nf3 Nf6 Bf4") == key("d4 Nf6 Nf3 d5 Bf4")) to "opening book error"
            }

            _uiState.update { it.copy(testResults = rows.toList()) }

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
        val cleanSubject = subject.trim().ifBlank { "Diggy Chess Feedback" }
        val cleanMessage = message.trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            _uiState.update {
                it.copy(contactStatus = "Please enter a valid email address.", contactIsError = true)
            }
            return
        }
        if (cleanMessage.length < 5) {
            _uiState.update {
                it.copy(contactStatus = "Please enter a short message (at least 5 characters).", contactIsError = true)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSendingContact = true, contactStatus = null, contactIsError = false) }
            repo.updateContactEmail(cleanEmail)
            val result = ContactSender.send(
                ContactSubmission(
                    senderName = cleanName.ifBlank { "Diggy Chess Player" },
                    senderEmail = cleanEmail,
                    subject = cleanSubject,
                    message = cleanMessage
                )
            )
            result.fold(
                onSuccess = { msg ->
                    onClearMessage()
                    _uiState.update {
                        it.copy(isSendingContact = false, contactStatus = msg, contactIsError = false)
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
        _uiState.update { it.copy(contactStatus = null, contactIsError = false, backupMessage = null) }
    }
}
