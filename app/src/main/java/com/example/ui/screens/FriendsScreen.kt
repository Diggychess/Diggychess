package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ColorChoice
import com.example.ui.viewmodel.FriendsUiState
import com.example.ui.viewmodel.FriendsViewModel

@Composable
fun FriendsScreen(
    state: FriendsUiState,
    onMyNameChange: (String) -> Unit,
    onOpponentNameChange: (String) -> Unit,
    onSelectColor: (ColorChoice) -> Unit,
    onSelectTimeControl: (Int, Int) -> Unit,
    onCreateOnlineInvite: () -> Unit,
    onClearOnlineInvite: () -> Unit,
    onJoinAddressChange: (String) -> Unit,
    onStartPassAndPlay: () -> Unit,
    onHostWifiMatch: () -> Unit,
    onJoinWifiMatch: () -> Unit,
    onSquareClick: (String) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onToggleFlip: () -> Unit,
    onOfferDraw: () -> Unit,
    onAcceptDraw: () -> Unit,
    onDeclineDraw: () -> Unit,
    onResign: () -> Unit,
    onRematch: () -> Unit,
    onBuildPgn: () -> String,
    onAnalyseGame: () -> Unit,
    onLeaveGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    if (state.inGame) {
        BackHandler { onLeaveGame() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DiggySpacing.s2, vertical = DiggySpacing.s2),
        verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.friends_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.friends_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        if (!state.inGame) {
            // Card 1: Online Invite Link (No Login via Lichess Open Challenge)
            DiggyCard(
                elevated = true,
                borderColor = StudioCyan.copy(alpha = 0.45f),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = "Online invite link",
                            tint = StudioCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Online Invite Link (No Login)",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Generate a free live Lichess Open Challenge link and send it to a friend to play over any network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val useTwoRows = FriendsViewModel.TIME_CONTROLS.size >= 4 && maxWidth < 340.dp
                        if (useTwoRows) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FriendsViewModel.TIME_CONTROLS.chunked(2).forEach { pair ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        pair.forEach { tc ->
                                            val selected = tc.limitSec == state.inviteTimeLimitSec && tc.incrementSec == state.inviteIncrementSec
                                            DiggyChip(
                                                label = tc.label,
                                                selected = selected,
                                                onClick = { onSelectTimeControl(tc.limitSec, tc.incrementSec) },
                                                accentColor = StudioCyan,
                                                testTag = "invite_tc_${tc.limitSec}",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FriendsViewModel.TIME_CONTROLS.forEach { tc ->
                                    val selected = tc.limitSec == state.inviteTimeLimitSec && tc.incrementSec == state.inviteIncrementSec
                                    DiggyChip(
                                        label = tc.label,
                                        selected = selected,
                                        onClick = { onSelectTimeControl(tc.limitSec, tc.incrementSec) },
                                        accentColor = StudioCyan,
                                        testTag = "invite_tc_${tc.limitSec}",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    DiggyButton(
                        text = if (state.isCreatingOnlineInvite) "Creating Link…" else "Create Shareable Invite",
                        onClick = onCreateOnlineInvite,
                        loading = state.isCreatingOnlineInvite,
                        icon = Icons.Rounded.Link,
                        testTag = "create_online_invite",
                        modifier = Modifier.fillMaxWidth()
                    )

                    state.onlineInviteError?.let { err ->
                        DiggyErrorState(message = err)
                    }

                    state.onlineInvite?.let { invite ->
                        Surface(
                            color = CardSurfaceElevated,
                            shape = RoundedCornerShape(DiggySpacing.controlRadius),
                            border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("online_invite_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Live Invite Ready",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = FeedbackCorrectGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(onClick = onClearOnlineInvite) {
                                        Text("Dismiss", color = TextMuted)
                                    }
                                }
                                Text(
                                    text = invite.url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DiggyButton(
                                        text = "Copy",
                                        onClick = {
                                            clipboard.setText(AnnotatedString(invite.url))
                                            copiedNotice = "Invite link copied."
                                        },
                                        variant = DiggyButtonVariant.OUTLINED,
                                        icon = Icons.Rounded.ContentCopy,
                                        testTag = "copy_online_invite",
                                        modifier = Modifier.weight(1f)
                                    )
                                    DiggyButton(
                                        text = "Share",
                                        onClick = {
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Play chess with me on Chess Analyzer: AI Coach! Join live match: ${invite.url}"
                                                )
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(send, "Share online match"))
                                        },
                                        variant = DiggyButtonVariant.OUTLINED,
                                        icon = Icons.Rounded.Share,
                                        testTag = "share_online_invite",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DiggyButton(
                                        text = "Join White",
                                        onClick = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invite.whiteUrl)))
                                        },
                                        variant = DiggyButtonVariant.PRIMARY,
                                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                                        testTag = "open_white_invite",
                                        modifier = Modifier.weight(1f)
                                    )
                                    DiggyButton(
                                        text = "Join Black",
                                        onClick = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invite.blackUrl)))
                                        },
                                        variant = DiggyButtonVariant.OUTLINED,
                                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                                        testTag = "open_black_invite",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Pass & Play & Local Wi-Fi Match
            DiggyCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Group,
                            contentDescription = "Local match",
                            tint = AccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Pass & Play / Local Wi-Fi", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }

                    OutlinedTextField(
                        value = state.myName,
                        onValueChange = onMyNameChange,
                        label = { Text("Your name") },
                        placeholder = { Text("Enter your name", color = InputHint) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("friend_name_input"),
                        colors = diggyInputColors(),
                        shape = RoundedCornerShape(DiggySpacing.controlRadius)
                    )

                    Text("You play", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    DiggySegmentedControl(
                        options = ColorChoice.entries,
                        selectedOption = state.colorChoice,
                        onSelect = onSelectColor,
                        labelFor = { it.label },
                        tagFor = { "friend_color_${it.code}" }
                    )

                    OutlinedTextField(
                        value = state.opponentNameInput,
                        onValueChange = onOpponentNameChange,
                        label = { Text("Friend's name (Pass & Play)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = diggyInputColors(),
                        shape = RoundedCornerShape(DiggySpacing.controlRadius)
                    )

                    DiggyButton(
                        text = "Play on This Device (Pass & Play)",
                        onClick = onStartPassAndPlay,
                        variant = DiggyButtonVariant.PRIMARY,
                        icon = Icons.Rounded.PlayArrow,
                        testTag = "start_pass_and_play",
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = LineSubtle)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Wifi,
                            contentDescription = "Wi-Fi match",
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Live Wi-Fi Match (Same Network)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    }

                    DiggyButton(
                        text = "Create Wi-Fi Room Invite",
                        onClick = onHostWifiMatch,
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.WifiTethering,
                        testTag = "host_wifi_match",
                        modifier = Modifier.fillMaxWidth()
                    )

                    state.inviteAddress?.let { addr ->
                        val inviteLink = "chessanalyzer://join?host=$addr"
                        Surface(
                            color = CardSurfaceElevated,
                            shape = RoundedCornerShape(DiggySpacing.controlRadius),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Share this address with your friend:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                Text(addr, style = MaterialTheme.typography.titleMedium, color = AccentGreen)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DiggyButton(
                                        text = "Copy",
                                        onClick = {
                                            clipboard.setText(AnnotatedString(addr))
                                            copiedNotice = "Wi-Fi room address copied."
                                        },
                                        variant = DiggyButtonVariant.OUTLINED,
                                        icon = Icons.Rounded.ContentCopy,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DiggyButton(
                                        text = "Share",
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                putExtra(Intent.EXTRA_TEXT, "Play chess with me on Chess Analyzer: AI Coach! Join Wi-Fi room: $addr ($inviteLink)")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Invite friend"))
                                        },
                                        variant = DiggyButtonVariant.OUTLINED,
                                        icon = Icons.Rounded.Share,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.joinAddressInput,
                            onValueChange = onJoinAddressChange,
                            placeholder = { Text("Host IP:8766", color = InputHint) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("join_address_input"),
                            colors = diggyInputColors(),
                            shape = RoundedCornerShape(DiggySpacing.controlRadius)
                        )
                        DiggyButton(
                            text = "Join",
                            onClick = onJoinWifiMatch,
                            variant = DiggyButtonVariant.SECONDARY,
                            testTag = "join_wifi_match"
                        )
                    }

                    state.statusMessage?.let { msg ->
                        if (state.isError) {
                            DiggyErrorState(message = msg)
                        } else {
                            Text(text = msg, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }

                    copiedNotice?.let { notice ->
                        Text(notice, style = MaterialTheme.typography.bodySmall, color = FeedbackCorrectGreen)
                    }
                }
            }
        } else {
            // Active Friendly Board
            DiggyCard(contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val topIsBlack = !state.flipped
                    val topColor = if (topIsBlack) PieceColor.BLACK else PieceColor.WHITE
                    val botColor = if (topIsBlack) PieceColor.WHITE else PieceColor.BLACK
                    val topName = if (topIsBlack) state.blackPlayerName else state.whitePlayerName
                    val botName = if (topIsBlack) state.whitePlayerName else state.blackPlayerName

                    val turnStatus = when {
                        state.isGameOver -> state.endMessage
                        state.isPassAndPlay -> if (state.turn == PieceColor.WHITE) "${state.whitePlayerName}'s turn" else "${state.blackPlayerName}'s turn"
                        state.turn == state.myColor -> "Your turn"
                        else -> "Opponent's turn"
                    }

                    PlayerBar(
                        name = topName,
                        pieceColor = topColor,
                        isActiveTurn = !state.isGameOver && state.turn == topColor,
                        fen = state.fen,
                        statusNote = if (!state.isGameOver && state.turn == topColor) turnStatus else null
                    )

                    ChessBoardView(
                        fen = state.fen,
                        boardTheme = state.boardTheme,
                        flipped = state.flipped,
                        selectedSquare = state.selectedSquare,
                        lastMoveSquares = state.lastMoveSquares,
                        legalTargets = state.legalTargets,
                        animateMoves = state.animateMoves,
                        onSquareClick = onSquareClick,
                        modifier = Modifier.fillMaxWidth()
                    )

                    PlayerBar(
                        name = botName,
                        pieceColor = botColor,
                        isActiveTurn = !state.isGameOver && state.turn == botColor,
                        fen = state.fen,
                        statusNote = when {
                            state.lastMoveSan.isNotEmpty() -> "Last: ${state.lastMoveSan}"
                            !state.isGameOver && state.turn == botColor -> turnStatus
                            else -> null
                        }
                    )

                    if (state.linkStatus.isNotEmpty()) {
                        Text(state.linkStatus, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        if (maxWidth < 340.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DiggyButton(
                                        text = "Flip",
                                        onClick = onToggleFlip,
                                        variant = DiggyButtonVariant.OUTLINED,
                                        icon = Icons.Rounded.SwapVert,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DiggyButton(
                                        text = "Draw",
                                        onClick = onOfferDraw,
                                        enabled = !state.isGameOver,
                                        variant = DiggyButtonVariant.OUTLINED,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                DiggyButton(
                                    text = "Resign",
                                    onClick = onResign,
                                    enabled = !state.isGameOver,
                                    variant = DiggyButtonVariant.OUTLINED,
                                    icon = Icons.Rounded.Flag,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DiggyButton(
                                    text = "Flip",
                                    onClick = onToggleFlip,
                                    variant = DiggyButtonVariant.OUTLINED,
                                    icon = Icons.Rounded.SwapVert,
                                    modifier = Modifier.weight(1f)
                                )
                                DiggyButton(
                                    text = "Draw",
                                    onClick = onOfferDraw,
                                    enabled = !state.isGameOver,
                                    variant = DiggyButtonVariant.OUTLINED,
                                    modifier = Modifier.weight(1f)
                                )
                                DiggyButton(
                                    text = "Resign",
                                    onClick = onResign,
                                    enabled = !state.isGameOver,
                                    variant = DiggyButtonVariant.OUTLINED,
                                    icon = Icons.Rounded.Flag,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (state.drawState == "in") {
                        Surface(
                            color = CardSurfaceElevated,
                            shape = RoundedCornerShape(DiggySpacing.controlRadius),
                            border = BorderStroke(1.dp, AccentGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Draw offered. Accept draw?", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DiggyButton(
                                        text = "Accept",
                                        onClick = onAcceptDraw,
                                        variant = DiggyButtonVariant.PRIMARY,
                                        modifier = Modifier.weight(1f)
                                    )
                                    DiggyButton(
                                        text = "Decline",
                                        onClick = onDeclineDraw,
                                        variant = DiggyButtonVariant.OUTLINED,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    if (state.moveCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiggyButton(
                                text = "Share PGN",
                                onClick = {
                                    val pgn = onBuildPgn()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, pgn)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share PGN"))
                                },
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.Share,
                                testTag = "export_friendly_pgn",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyButton(
                                text = "Copy PGN",
                                onClick = {
                                    clipboard.setText(AnnotatedString(onBuildPgn()))
                                    copiedNotice = "PGN copied to clipboard."
                                },
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.ContentCopy,
                                testTag = "copy_friendly_pgn",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    copiedNotice?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = FeedbackCorrectGreen)
                    }

                    if (state.isGameOver) {
                        HorizontalDivider(color = LineSubtle)
                        Text(state.endMessage, style = MaterialTheme.typography.titleMedium, color = AccentGreen)
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            if (maxWidth < 330.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DiggyButton(
                                        text = "Analyse Game",
                                        onClick = onAnalyseGame,
                                        variant = DiggyButtonVariant.PRIMARY,
                                        icon = Icons.Rounded.Analytics,
                                        testTag = "analyse_friendly_game",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    DiggyButton(
                                        text = when {
                                            state.wantRematch -> "Waiting…"
                                            state.opponentWantsRematch -> "Accept Rematch"
                                            else -> "Rematch"
                                        },
                                        onClick = onRematch,
                                        variant = DiggyButtonVariant.OUTLINED,
                                        icon = Icons.Rounded.Refresh,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DiggyButton(
                                        text = "Analyse Game",
                                        onClick = onAnalyseGame,
                                        variant = DiggyButtonVariant.PRIMARY,
                                        icon = Icons.Rounded.Analytics,
                                        testTag = "analyse_friendly_game",
                                        modifier = Modifier.weight(1f)
                                    )
                                    DiggyButton(
                                        text = when {
                                            state.wantRematch -> "Waiting…"
                                            state.opponentWantsRematch -> "Accept Rematch"
                                            else -> "Rematch"
                                        },
                                        onClick = onRematch,
                                        variant = DiggyButtonVariant.OUTLINED,
                                        icon = Icons.Rounded.Refresh,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = onLeaveGame,
                        modifier = Modifier
                            .align(Alignment.End)
                            .heightIn(min = DiggySpacing.touchTargetMin)
                    ) {
                        Text("Leave match", color = TextMuted)
                    }
                }
            }
        }
    }

    state.pendingPromotion?.let {
        PromotionPickerDialog(
            color = state.turn,
            onSelect = onSelectPromotion,
            onDismiss = onDismissPromotion
        )
    }
}
