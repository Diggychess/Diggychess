package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun FriendsScreen(
    state: FriendsUiState,
    onMyNameChange: (String) -> Unit,
    onOpponentNameChange: (String) -> Unit,
    onSelectColor: (ColorChoice) -> Unit,
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
    onAnalyseGame: () -> Unit,
    onLeaveGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

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
            // Friendly Match Lobby
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, LineSubtle),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("FRIENDLY MATCH", style = MaterialTheme.typography.titleSmall, color = TextMuted)

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

                    // Side-by-side Pass & Play
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

                    Text("LIVE WI-FI MATCH (SAME NETWORK)", style = MaterialTheme.typography.labelSmall, color = TextMuted)

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
                                        onClick = { clipboard.setText(AnnotatedString(addr)) },
                                        modifier = Modifier.weight(1f)
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
                                        modifier = Modifier.weight(1f)
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
                }
            }
        } else {
            // Active Friendly Board
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
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
                            Icon(Icons.Filled.Flip, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flip", color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = onOfferDraw,
                            enabled = !state.isGameOver,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text("Offer draw", color = TextPrimary)
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
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Accept", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(onClick = onDeclineDraw, modifier = Modifier.weight(1f)) {
                                        Text("Decline", color = TextPrimary)
                                    }
                                }
                            }
                        }
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

                    TextButton(onClick = onLeaveGame, modifier = Modifier.align(Alignment.End)) {
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
