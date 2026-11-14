package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.*
import com.example.data.KnightScopeRepository
import com.example.ui.theme.BoardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

enum class ColorChoice(val code: String, val label: String) {
    WHITE("w", "White"),
    RANDOM("r", "Random"),
    BLACK("b", "Black")
}

enum class FriendMode {
    WIFI_HOST,
    PASS_AND_PLAY
}

data class FriendsUiState(
    val myName: String = "",
    val opponentNameInput: String = "Friend",
    val colorChoice: ColorChoice = ColorChoice.WHITE,
    val friendMode: FriendMode = FriendMode.WIFI_HOST,
    val joinAddressInput: String = "",
    val inviteAddress: String? = null,
    val statusMessage: String? = null,
    val isError: Boolean = false,
    val isConnecting: Boolean = false,

    // Active Game State
    val inGame: Boolean = false,
    val isPassAndPlay: Boolean = false,
    val isHost: Boolean = false,
    val myColor: PieceColor = PieceColor.WHITE,
    val whitePlayerName: String = "White",
    val blackPlayerName: String = "Black",
    val fen: String = ChessBoard.START_FEN,
    val turn: PieceColor = PieceColor.WHITE,
    val flipped: Boolean = false,
    val selectedSquare: String? = null,
    val legalTargets: Map<String, Boolean> = emptyMap(),
    val lastMoveSquares: List<String> = emptyList(),
    val lastMoveSan: String = "",
    val isGameOver: Boolean = false,
    val resultString: String = "*",
    val endMessage: String = "",
    val drawState: String = "", // "", "in", "out"
    val wantRematch: Boolean = false,
    val opponentWantsRematch: Boolean = false,
    val linkStatus: String = "",
    val canReconnect: Boolean = false,
    val boardTheme: BoardTheme = BoardTheme.WOOD,
    val pendingPromotion: Pair<String, String>? = null
)

class FriendsViewModel(private val repo: KnightScopeRepository) : ViewModel() {

    companion object {
        const val DEFAULT_PORT = 8766
    }

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private var board = ChessBoard()
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var writer: PrintWriter? = null
    private var networkJob: Job? = null

    init {
        viewModelScope.launch {
            repo.friendNameFlow.collect { n ->
                if (_uiState.value.myName.isEmpty() && n.isNotEmpty()) {
                    _uiState.update { it.copy(myName = n) }
                }
            }
        }
        viewModelScope.launch {
            repo.boardThemeFlow.collect { t ->
                _uiState.update { it.copy(boardTheme = t) }
            }
        }
    }

    fun updateMyName(name: String) {
        _uiState.update { it.copy(myName = name.take(20), statusMessage = null, isError = false) }
    }

    fun updateOpponentNameInput(name: String) {
        _uiState.update { it.copy(opponentNameInput = name.take(20)) }
    }

    fun selectColorChoice(choice: ColorChoice) {
        _uiState.update { it.copy(colorChoice = choice) }
    }

    fun updateJoinAddress(addr: String) {
        _uiState.update { it.copy(joinAddressInput = addr, statusMessage = null, isError = false) }
    }

    fun startPassAndPlay() {
        val rawName = _uiState.value.myName.trim().ifEmpty { "Player 1" }
        val oppName = _uiState.value.opponentNameInput.trim().ifEmpty { "Player 2" }
        viewModelScope.launch { repo.setFriendName(rawName) }

        val myCol = when (_uiState.value.colorChoice) {
            ColorChoice.WHITE -> PieceColor.WHITE
            ColorChoice.BLACK -> PieceColor.BLACK
            ColorChoice.RANDOM -> if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
        }
        val wName = if (myCol == PieceColor.WHITE) rawName else oppName
        val bName = if (myCol == PieceColor.WHITE) oppName else rawName

        board = ChessBoard()
        _uiState.update {
            it.copy(
                inGame = true,
                isPassAndPlay = true,
                isHost = true,
                myColor = myCol,
                whitePlayerName = wName,
                blackPlayerName = bName,
                fen = board.toFen(),
                turn = PieceColor.WHITE,
                flipped = (myCol == PieceColor.BLACK),
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = emptyList(),
                lastMoveSan = "",
                isGameOver = false,
                resultString = "*",
                endMessage = "",
                drawState = "",
                wantRematch = false,
                opponentWantsRematch = false,
                linkStatus = "Side-by-side Pass & Play",
                canReconnect = false,
                pendingPromotion = null
            )
        }
    }

    fun hostWifiMatch() {
        val myName = _uiState.value.myName.trim().replace(Regex("\\s+"), " ")
        if (myName.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Please enter your name first.", isError = true) }
            return
        }
        viewModelScope.launch { repo.setFriendName(myName) }

        closeSockets()
        val ip = getLocalIpAddress()
        if (ip == null) {
            _uiState.update {
                it.copy(
                    statusMessage = "No Wi-Fi/LAN address detected. Use Pass & Play or connect both devices to the same Wi-Fi network.",
                    isError = true
                )
            }
            return
        }

        val address = "$ip:$DEFAULT_PORT"
        _uiState.update {
            it.copy(
                inviteAddress = address,
                isConnecting = true,
                isError = false,
                statusMessage = "Room ready at $address. Share this address with your friend on the same Wi-Fi network and keep this screen open."
            )
        }

        networkJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val srv = ServerSocket(DEFAULT_PORT)
                serverSocket = srv
                val sock = srv.accept()
                clientSocket = sock
                writer = PrintWriter(sock.getOutputStream(), true)
                listenSocket(sock, asHost = true, myName = myName)
            } catch (e: Exception) {
                if (_uiState.value.inGame) {
                    _uiState.update { it.copy(linkStatus = "Friend disconnected.") }
                } else if (_uiState.value.inviteAddress != null) {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            statusMessage = "Could not start room: ${e.localizedMessage ?: "port busy"}",
                            isError = true
                        )
                    }
                }
            }
        }
    }

    fun joinWifiMatch() {
        val myName = _uiState.value.myName.trim().replace(Regex("\\s+"), " ")
        if (myName.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Please enter your name first.", isError = true) }
            return
        }
        val rawAddr = _uiState.value.joinAddressInput.trim()
            .removePrefix("knightscope://join?host=")
            .trim()
        if (rawAddr.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Enter the host IP address (e.g. 192.168.1.12:8766).", isError = true) }
            return
        }
        val parts = rawAddr.split(':')
        val host = parts[0].trim()
        val port = parts.getOrNull(1)?.toIntOrNull() ?: DEFAULT_PORT

        viewModelScope.launch { repo.setFriendName(myName) }
        closeSockets()

        _uiState.update {
            it.copy(
                isConnecting = true,
                isError = false,
                statusMessage = "Connecting to $host:$port…"
            )
        }

        networkJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val sock = Socket(host, port)
                clientSocket = sock
                writer = PrintWriter(sock.getOutputStream(), true)
                sendJson(JSONObject().apply {
                    put("t", "hi")
                    put("name", myName)
                })
                listenSocket(sock, asHost = false, myName = myName)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isError = true,
                        statusMessage = "Could not connect to $host:$port. Check that both devices are on the same Wi-Fi and the host screen is open."
                    )
                }
            }
        }
    }

    private suspend fun listenSocket(sock: Socket, asHost: Boolean, myName: String) {
        try {
            val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
            while (true) {
                val line = reader.readLine() ?: break
                val msg = runCatching { JSONObject(line) }.getOrNull() ?: continue
                withContext(Dispatchers.Main) {
                    handleNetworkMessage(msg, asHost, myName)
                }
            }
        } catch (_: Exception) {
        } finally {
            withContext(Dispatchers.Main) {
                if (_uiState.value.inGame && !_uiState.value.isGameOver) {
                    _uiState.update {
                        it.copy(
                            linkStatus = "Opponent disconnected.",
                            canReconnect = !asHost
                        )
                    }
                }
            }
        }
    }

    private fun handleNetworkMessage(d: JSONObject, asHost: Boolean, myName: String) {
        val type = d.optString("t")
        when (type) {
            "hi" -> {
                if (!asHost) return
                val opp = d.optString("name", "Friend").take(20)
                val hc = when (_uiState.value.colorChoice) {
                    ColorChoice.WHITE -> PieceColor.WHITE
                    ColorChoice.BLACK -> PieceColor.BLACK
                    ColorChoice.RANDOM -> if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
                }
                sendJson(JSONObject().apply {
                    put("t", "start")
                    put("host", myName)
                    put("hostCol", hc.fenChar().toString())
                })
                startWifiBoard(myColor = hc, myName = myName, oppName = opp, isHost = true)
            }
            "start" -> {
                if (asHost) return
                val hostName = d.optString("host", "Friend").take(20)
                val hostCol = if (d.optString("hostCol") == "w") PieceColor.WHITE else PieceColor.BLACK
                startWifiBoard(myColor = hostCol.opposite(), myName = myName, oppName = hostName, isHost = false)
            }
            "mv" -> {
                if (_uiState.value.isGameOver) return
                val from = d.optString("from")
                val to = d.optString("to")
                val promo = d.optString("p", "q")
                val m = board.makeUciMove(from + to + promo) ?: return
                syncBoardAfterMove(m)
            }
            "resign" -> {
                if (_uiState.value.isGameOver) return
                val s = _uiState.value
                val opp = if (s.myColor == PieceColor.WHITE) s.blackPlayerName else s.whitePlayerName
                _uiState.update {
                    it.copy(
                        isGameOver = true,
                        resultString = if (s.myColor == PieceColor.WHITE) "1-0" else "0-1",
                        endMessage = "$opp resigned — you win!",
                        drawState = ""
                    )
                }
            }
            "draw" -> {
                if (!_uiState.value.isGameOver) {
                    _uiState.update { it.copy(drawState = "in") }
                }
            }
            "drawyes" -> {
                if (!_uiState.value.isGameOver && _uiState.value.drawState == "out") {
                    _uiState.update {
                        it.copy(
                            isGameOver = true,
                            resultString = "1/2-1/2",
                            endMessage = "Draw agreed.",
                            drawState = ""
                        )
                    }
                }
            }
            "drawno" -> {
                if (_uiState.value.drawState == "out") {
                    _uiState.update { it.copy(drawState = "", linkStatus = "Opponent declined the draw.") }
                }
            }
            "rematch" -> {
                val s = _uiState.value
                if (asHost && s.wantRematch) {
                    val nextHostCol = s.myColor.opposite()
                    sendJson(JSONObject().apply {
                        put("t", "newgame")
                        put("hostCol", nextHostCol.fenChar().toString())
                    })
                    val opp = if (s.myColor == PieceColor.WHITE) s.blackPlayerName else s.whitePlayerName
                    startWifiBoard(nextHostCol, myName, opp, true)
                } else {
                    _uiState.update { it.copy(opponentWantsRematch = true) }
                }
            }
            "newgame" -> {
                if (asHost) return
                val s = _uiState.value
                val hostCol = if (d.optString("hostCol") == "w") PieceColor.WHITE else PieceColor.BLACK
                val opp = if (s.myColor == PieceColor.WHITE) s.blackPlayerName else s.whitePlayerName
                startWifiBoard(hostCol.opposite(), myName, opp, false)
            }
        }
    }

    private fun startWifiBoard(myColor: PieceColor, myName: String, oppName: String, isHost: Boolean) {
        board = ChessBoard()
        val wName = if (myColor == PieceColor.WHITE) myName else oppName
        val bName = if (myColor == PieceColor.WHITE) oppName else myName
        _uiState.update {
            it.copy(
                inGame = true,
                isPassAndPlay = false,
                isHost = isHost,
                isConnecting = false,
                myColor = myColor,
                whitePlayerName = wName,
                blackPlayerName = bName,
                fen = board.toFen(),
                turn = PieceColor.WHITE,
                flipped = (myColor == PieceColor.BLACK),
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = emptyList(),
                lastMoveSan = "",
                isGameOver = false,
                resultString = "*",
                endMessage = "",
                drawState = "",
                wantRematch = false,
                opponentWantsRematch = false,
                linkStatus = "Connected",
                canReconnect = false,
                pendingPromotion = null
            )
        }
    }

    private fun sendJson(obj: JSONObject) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { writer?.println(obj.toString()) }
        }
    }

    fun onSquareClick(sqName: String) {
        val s = _uiState.value
        if (!s.inGame || s.isGameOver) return
        if (!s.isPassAndPlay && board.turn != s.myColor) return

        val sel = s.selectedSquare
        if (sel != null) {
            val fromIdx = parseSquare(sel)
            val toIdx = parseSquare(sqName)
            if (fromIdx != null && toIdx != null) {
                val matching = board.legalMoves(fromIdx).filter { it.to == toIdx }
                if (matching.isNotEmpty()) {
                    if (matching.any { it.promotion != null }) {
                        _uiState.update { it.copy(pendingPromotion = sel to sqName) }
                    } else {
                        playMove(sel, sqName, null)
                    }
                    return
                }
            }
        }

        val sqIdx = parseSquare(sqName) ?: return
        val p = board.pieceAt(sqIdx)
        if (p != null && p.color == board.turn && sqName != sel) {
            val targets = board.legalMoves(sqIdx).associate { it.toName to (it.captured != null) }
            _uiState.update { it.copy(selectedSquare = sqName, legalTargets = targets) }
        } else {
            _uiState.update { it.copy(selectedSquare = null, legalTargets = emptyMap()) }
        }
    }

    fun selectPromotion(type: PieceType) {
        val pending = _uiState.value.pendingPromotion ?: return
        _uiState.update { it.copy(pendingPromotion = null) }
        playMove(pending.first, pending.second, type)
    }

    fun dismissPromotion() {
        _uiState.update { it.copy(pendingPromotion = null) }
    }

    private fun playMove(from: String, to: String, promo: PieceType?) {
        val uci = from + to + (promo?.symbol?.toString() ?: "")
        val m = board.makeUciMove(uci) ?: return
        if (!_uiState.value.isPassAndPlay) {
            sendJson(JSONObject().apply {
                put("t", "mv")
                put("from", m.fromName)
                put("to", m.toName)
                if (m.promotion != null) put("p", m.promotion.symbol.toString())
                put("n", board.moveHistoryCount())
            })
        }
        syncBoardAfterMove(m)
    }

    private fun syncBoardAfterMove(lastMove: Move) {
        val s = _uiState.value
        val nextTurn = board.turn
        var over = false
        var res = "*"
        var endMsg = ""

        if (board.isGameOver()) {
            over = true
            when {
                board.inCheckmate() -> {
                    val winner = nextTurn.opposite()
                    res = if (winner == PieceColor.WHITE) "1-0" else "0-1"
                    val winName = if (winner == PieceColor.WHITE) s.whitePlayerName else s.blackPlayerName
                    endMsg = "Checkmate — $winName wins!"
                }
                board.inStalemate() -> {
                    res = "1/2-1/2"
                    endMsg = "Draw by stalemate"
                }
                board.inThreefoldRepetition() -> {
                    res = "1/2-1/2"
                    endMsg = "Draw by repetition"
                }
                board.insufficientMaterial() -> {
                    res = "1/2-1/2"
                    endMsg = "Draw — insufficient material"
                }
                else -> {
                    res = "1/2-1/2"
                    endMsg = "Draw by the 50-move rule"
                }
            }
        }

        _uiState.update {
            it.copy(
                fen = board.toFen(),
                turn = nextTurn,
                selectedSquare = null,
                legalTargets = emptyMap(),
                lastMoveSquares = listOf(lastMove.fromName, lastMove.toName),
                lastMoveSan = board.sanHistory().lastOrNull() ?: "",
                drawState = "",
                isGameOver = over,
                resultString = res,
                endMessage = endMsg,
                flipped = if (it.isPassAndPlay && !over) (nextTurn == PieceColor.BLACK) else it.flipped
            )
        }
    }

    fun toggleFlip() {
        _uiState.update { it.copy(flipped = !it.flipped) }
    }

    fun offerDraw() {
        val s = _uiState.value
        if (!s.inGame || s.isGameOver) return
        if (s.isPassAndPlay) {
            _uiState.update { it.copy(drawState = "in") }
        } else {
            sendJson(JSONObject().put("t", "draw"))
            _uiState.update { it.copy(drawState = "out", linkStatus = "Draw offered. Waiting for opponent…") }
        }
    }

    fun acceptDraw() {
        val s = _uiState.value
        if (s.drawState != "in") return
        if (!s.isPassAndPlay) sendJson(JSONObject().put("t", "drawyes"))
        _uiState.update {
            it.copy(
                isGameOver = true,
                resultString = "1/2-1/2",
                endMessage = "Draw agreed.",
                drawState = ""
            )
        }
    }

    fun declineDraw() {
        val s = _uiState.value
        if (s.drawState != "in") return
        if (!s.isPassAndPlay) sendJson(JSONObject().put("t", "drawno"))
        _uiState.update { it.copy(drawState = "") }
    }

    fun resign() {
        val s = _uiState.value
        if (!s.inGame || s.isGameOver) return
        if (s.isPassAndPlay) {
            val loser = board.turn
            val loserName = if (loser == PieceColor.WHITE) s.whitePlayerName else s.blackPlayerName
            _uiState.update {
                it.copy(
                    isGameOver = true,
                    resultString = if (loser == PieceColor.WHITE) "0-1" else "1-0",
                    endMessage = "$loserName resigned.",
                    drawState = ""
                )
            }
        } else {
            sendJson(JSONObject().put("t", "resign"))
            _uiState.update {
                it.copy(
                    isGameOver = true,
                    resultString = if (s.myColor == PieceColor.WHITE) "0-1" else "1-0",
                    endMessage = "You resigned.",
                    drawState = ""
                )
            }
        }
    }

    fun requestRematch() {
        val s = _uiState.value
        if (!s.isGameOver) return
        if (s.isPassAndPlay) {
            val nextCol = s.myColor.opposite()
            val prevW = s.whitePlayerName
            val prevB = s.blackPlayerName
            board = ChessBoard()
            _uiState.update {
                it.copy(
                    myColor = nextCol,
                    whitePlayerName = prevB,
                    blackPlayerName = prevW,
                    fen = board.toFen(),
                    turn = PieceColor.WHITE,
                    flipped = (nextCol == PieceColor.BLACK),
                    selectedSquare = null,
                    legalTargets = emptyMap(),
                    lastMoveSquares = emptyList(),
                    lastMoveSan = "",
                    isGameOver = false,
                    resultString = "*",
                    endMessage = "",
                    drawState = "",
                    wantRematch = false,
                    opponentWantsRematch = false
                )
            }
            return
        }

        if (s.isHost && s.opponentWantsRematch) {
            val nextHostCol = s.myColor.opposite()
            sendJson(JSONObject().apply {
                put("t", "newgame")
                put("hostCol", nextHostCol.fenChar().toString())
            })
            val opp = if (s.myColor == PieceColor.WHITE) s.blackPlayerName else s.whitePlayerName
            startWifiBoard(nextHostCol, s.myName, opp, true)
        } else {
            sendJson(JSONObject().put("t", "rematch"))
            _uiState.update { it.copy(wantRematch = true) }
        }
    }

    fun buildFinishedPgn(): String {
        val s = _uiState.value
        val copy = board.copy()
        copy.setHeader("Event", if (s.isPassAndPlay) "Pass & Play match" else "Friendly match")
        copy.setHeader("White", s.whitePlayerName)
        copy.setHeader("Black", s.blackPlayerName)
        copy.setHeader("Result", s.resultString)
        return copy.toPgn(s.resultString)
    }

    fun leaveGame() {
        closeSockets()
        _uiState.update {
            it.copy(
                inGame = false,
                inviteAddress = null,
                isConnecting = false,
                statusMessage = null
            )
        }
    }

    private fun closeSockets() {
        networkJob?.cancel()
        runCatching { writer?.close() }
        runCatching { clientSocket?.close() }
        runCatching { serverSocket?.close() }
        writer = null
        clientSocket = null
        serverSocket = null
    }

    private fun getLocalIpAddress(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        closeSockets()
    }
}
