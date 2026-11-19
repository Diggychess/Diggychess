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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.ChessBoardView
import com.example.ui.components.PromotionPickerDialog
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated),
                border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = "Online invite link",
                            tint = StudioCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "ONLINE INVITE LINK (NO LOGIN)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StudioCyan
                        )
                    }
                    Text(
                        text = "Generate a free real-time Lichess Open Challenge link and send it to a friend to play live over any network without creating an account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FriendsViewModel.TIME_CONTROLS.forEach { tc ->
                            val selected = tc.limitSec == state.inviteTimeLimitSec && tc.incrementSec == state.inviteIncrementSec
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectTimeControl(tc.limitSec, tc.incrementSec) },
                                label = { Text(tc.label, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioCyan.copy(alpha = 0.18f),
                                    selectedLabelColor = StudioCyan
                                ),
                                modifier = Modifier.testTag("invite_tc_${tc.limitSec}")
                            )
                        }
                    }

                    Button(
                        onClick = onCreateOnlineInvite,
                        enabled = !state.isCreatingOnlineInvite,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = OnAccentDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("create_online_invite")
                    ) {
                        if (state.isCreatingOnlineInvite) {
                            CircularProgressIndicator(
                                color = OnAccentDark,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Creating link…", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create shareable online invite", fontWeight = FontWeight.Bold)
                        }
                    }

                    state.onlineInviteError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = ClassBlunder
                        )
                    }

                    state.onlineInvite?.let { invite ->
                        Surface(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("online_invite_card")
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Live Invite Ready",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = AccentGreenHover,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(onClick = onClearOnlineInvite) {
                                        Text("Dismiss", color = TextMuted)
                                    }
                                }
                                Text(
                                    text = invite.url,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = TextPrimary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(invite.url))
                                            copiedNotice = "Invite link copied."
                                        },
                                        border = BorderStroke(1.dp, BorderSubtle),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("copy_online_invite")
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Play chess with me on KnightScope! Join live match: ${invite.url}"
                                                )
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(send, "Share online match"))
                                        },
                                        border = BorderStroke(1.dp, BorderSubtle),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("share_online_invite")
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share")
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invite.whiteUrl)))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("open_white_invite")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Join as White", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(invite.blackUrl)))
                                        },
                                        border = BorderStroke(1.dp, BorderSubtle),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .testTag("open_black_invite")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Join as Black")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Pass & Play & Local Wi-Fi Match
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, LineSubtle),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = "Local match",
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("PASS & PLAY / LOCAL WI-FI", style = MaterialTheme.typography.titleSmall, color = TextMuted)
                    }

                    OutlinedTextField(
                        value = state.myName,
                        onValueChange = onMyNameChange,
                        label = { Text("Your name") },
                        placeholder = { Text("Enter your name", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("friend_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )

                    Text("You play", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorChoice.entries.forEach { choice ->
                            val selected = choice == state.colorChoice
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectColor(choice) },
                                label = { Text(choice.label, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGreen.copy(alpha = 0.18f),
                                    selectedLabelColor = AccentGreenHover
                                ),
                                modifier = Modifier.testTag("friend_color_${choice.code}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.opponentNameInput,
                        onValueChange = onOpponentNameChange,
                        label = { Text("Friend's name (for Pass & Play)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = BorderSubtle
                        )
                    )

                    Button(
                        onClick = onStartPassAndPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("start_pass_and_play")
                    ) {
                        Text("Play on this device (Pass & Play)", fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = BorderSubtle)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wifi,
                            contentDescription = "Wi-Fi match",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("LIVE WI-FI MATCH (SAME NETWORK)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }

                    OutlinedButton(
                        onClick = onHostWifiMatch,
                        border = BorderStroke(1.dp, AccentGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("host_wifi_match")
                    ) {
                        Text("Create Wi-Fi room invite", color = AccentGreenHover, fontWeight = FontWeight.SemiBold)
                    }

                    state.inviteAddress?.let { addr ->
                        val inviteLink = "knightscope://join?host=$addr"
                        Surface(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Share this address or link with your friend:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                Text(addr, style = MaterialTheme.typography.titleMedium, color = AccentGreenHover)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(addr))
                                            copiedNotice = "Wi-Fi room address copied."
                                        },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy address")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                putExtra(Intent.EXTRA_TEXT, "Play chess with me on KnightScope! Join Wi-Fi room: $addr ($inviteLink)")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Invite friend"))
                                        },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Share")
                                    }
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
                            placeholder = { Text("Host IP:8766", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("join_address_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceDark,
                                unfocusedContainerColor = SurfaceDark,
                                focusedBorderColor = AccentGreen,
                                unfocusedBorderColor = BorderSubtle
                            )
                        )
                        Button(
                            onClick = onJoinWifiMatch,
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark, contentColor = AccentGreenHover),
                            border = BorderStroke(1.dp, BorderSubtle),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 52.dp)
                                .testTag("join_wifi_match")
                        ) {
                            Text("Join")
                        }
                    }

                    state.statusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.isError) Color(0xFFFF8A80) else TextMuted
                        )
                    }

                    copiedNotice?.let { notice ->
                        Text(notice, style = MaterialTheme.typography.bodySmall, color = AccentGreenHover)
                    }
                }
            }
        } else {
            // Active Friendly Board
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
                border = BorderStroke(1.dp, LineSubtle),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val topIsBlack = !state.flipped
                    val topName = if (topIsBlack) "${state.blackPlayerName} (Black)" else "${state.whitePlayerName} (White)"
                    val botName = if (topIsBlack) "${state.whitePlayerName} (White)" else "${state.blackPlayerName} (Black)"
                    val turnStatus = when {
                        state.isGameOver -> state.endMessage
                        state.isPassAndPlay -> if (state.turn == PieceColor.WHITE) "${state.whitePlayerName}'s turn (White)" else "${state.blackPlayerName}'s turn (Black)"
                        state.turn == state.myColor -> "Your turn"
                        else -> "Opponent's turn"
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(topName, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(turnStatus, style = MaterialTheme.typography.bodySmall, color = AccentGreenHover)
                    }

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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(botName, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        if (state.lastMoveSan.isNotEmpty()) {
                            Text("Last: ${state.lastMoveSan}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }

                    if (state.linkStatus.isNotEmpty()) {
                        Text(state.linkStatus, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onToggleFlip,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.Flip, contentDescription = "Flip board", modifier = Modifier.size(16.dp), tint = TextPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flip", color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = onOfferDraw,
                            enabled = !state.isGameOver,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text("Draw", color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = onResign,
                            enabled = !state.isGameOver,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text("Resign", color = TextPrimary)
                        }
                    }

                    if (state.drawState == "in") {
                        Surface(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AccentGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Draw offered. Accept draw?", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onAcceptDraw,
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Text("Accept", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(onClick = onDeclineDraw, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                                        Text("Decline", color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    if (state.moveCount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val pgn = onBuildPgn()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_TEXT, pgn)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share PGN"))
                                },
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("export_friendly_pgn")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share PGN", color = TextPrimary)
                            }
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(onBuildPgn()))
                                    copiedNotice = "PGN copied to clipboard."
                                },
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("copy_friendly_pgn")
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy PGN", color = TextPrimary)
                            }
                        }
                    }

                    copiedNotice?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = AccentGreenHover)
                    }

                    if (state.isGameOver) {
                        HorizontalDivider(color = BorderSubtle)
                        Text(state.endMessage, style = MaterialTheme.typography.titleMedium, color = AccentGreenHover)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onAnalyseGame,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("analyse_friendly_game")
                            ) {
                                Text("Analyse game", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onRematch,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                val label = when {
                                    state.wantRematch -> "Waiting…"
                                    state.opponentWantsRematch -> "Accept rematch"
                                    else -> "Rematch"
                                }
                                Text(label, color = TextPrimary)
                            }
                        }
                    }

                    TextButton(onClick = onLeaveGame, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
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
