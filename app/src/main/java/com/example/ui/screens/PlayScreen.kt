package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.EngineDifficulty
import com.example.chess.MoveExplainer
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.components.BoardArrow
import com.example.ui.components.ChessBoardView
import com.example.ui.components.EvalBar
import com.example.ui.components.PromotionPickerDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.ColorChoice
import com.example.ui.viewmodel.PlayUiState

@Composable
fun PlayScreen(
    state: PlayUiState,
    onSelectDifficulty: (EngineDifficulty) -> Unit,
    onSelectColor: (ColorChoice) -> Unit,
    onToggleLiveEval: (Boolean) -> Unit,
    onStartGame: () -> Unit,
    onSquareClick: (String) -> Unit,
    onSelectPromotion: (PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onHint: () -> Unit,
    onTakeback: () -> Unit,
    onFlip: () -> Unit,
    onResign: () -> Unit,
    onRematch: () -> Unit,
    onAnalyseGame: () -> Unit,
    onLeaveGame: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = stringResource(R.string.play_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.play_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        if (!state.inGame) {
            PlayLobbyCard(
                state = state,
                onSelectDifficulty = onSelectDifficulty,
                onSelectColor = onSelectColor,
                onToggleLiveEval = onToggleLiveEval,
                onStartGame = onStartGame
            )
        } else {
            ActiveBotMatchCard(
                state = state,
                onSquareClick = onSquareClick,
                onHint = onHint,
                onTakeback = onTakeback,
                onFlip = onFlip,
                onResign = onResign,
                onRematch = onRematch,
                onAnalyseGame = onAnalyseGame,
                onLeaveGame = onLeaveGame
            )
        }
    }

    state.pendingPromotion?.let {
        PromotionPickerDialog(
            color = state.playerColor,
            onSelect = onSelectPromotion,
            onDismiss = onDismissPromotion
        )
    }
}

@Composable
private fun PlayLobbyCard(
    state: PlayUiState,
    onSelectDifficulty: (EngineDifficulty) -> Unit,
    onSelectColor: (ColorChoice) -> Unit,
    onToggleLiveEval: (Boolean) -> Unit,
    onStartGame: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, LineSubtle),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    tint = AccentGreenHover,
                    modifier = Modifier.size(20.dp)
                )
                Text("ENGINE STRENGTH", style = MaterialTheme.typography.titleSmall, color = TextMuted)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EngineDifficulty.entries.forEach { diff ->
                    val selected = state.difficulty == diff
                    Surface(
                        onClick = { onSelectDifficulty(diff) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) AccentGreen.copy(alpha = 0.16f) else SurfaceDark,
                        border = BorderStroke(1.dp, if (selected) AccentGreen else BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("play_difficulty_${diff.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = diff.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) AccentGreenHover else TextPrimary
                            )
                            Text(
                                text = diff.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("PLAY AS", style = MaterialTheme.typography.labelMedium, color = TextMuted)
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
                            modifier = Modifier.testTag("play_color_${choice.code}")
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Live evaluation bar",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Show live Stockfish advantage beside the board",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = state.showLiveEval,
                    onCheckedChange = onToggleLiveEval,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnAccentDark,
                        checkedTrackColor = AccentGreen
                    ),
                    modifier = Modifier.testTag("play_live_eval_switch")
                )
            }

            Button(
                onClick = onStartGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = OnAccentDark
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("play_start_button")
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start game vs Stockfish", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActiveBotMatchCard(
    state: PlayUiState,
    onSquareClick: (String) -> Unit,
    onHint: () -> Unit,
    onTakeback: () -> Unit,
    onFlip: () -> Unit,
    onResign: () -> Unit,
    onRematch: () -> Unit,
    onAnalyseGame: () -> Unit,
    onLeaveGame: () -> Unit
) {
    val botLabel = "Stockfish · ${state.difficulty.label}"
    val topIsBlack = !state.flipped
    val topLabel = if (topIsBlack) {
        if (state.playerColor == PieceColor.BLACK) "${state.playerName} (Black)" else "$botLabel (Black)"
    } else {
        if (state.playerColor == PieceColor.WHITE) "${state.playerName} (White)" else "$botLabel (White)"
    }
    val bottomLabel = if (topIsBlack) {
        if (state.playerColor == PieceColor.WHITE) "${state.playerName} (White)" else "$botLabel (White)"
    } else {
        if (state.playerColor == PieceColor.BLACK) "${state.playerName} (Black)" else "$botLabel (Black)"
    }

    val hintArrows = if (state.hintFrom != null && state.hintTo != null) {
        listOf(BoardArrow(state.hintFrom, state.hintTo, ArrowBestGreen))
    } else emptyList()

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
        border = BorderStroke(1.dp, LineSubtle),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top player strip + eval badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(topLabel, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        text = when {
                            state.isGameOver -> state.endMessage
                            state.isEngineThinking -> "Thinking (${state.engineSourceLabel})…"
                            state.turn == state.playerColor -> "Your turn · ${state.engineSourceLabel}"
                            else -> "Engine turn"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isEngineThinking) AccentGreenHover else TextMuted
                    )
                }

                if (state.showLiveEval) {
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = MoveExplainer.formatPlyEval(state.liveEvalCp, state.liveEvalMate),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreenHover,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("play_eval_pill")
                        )
                    }
                }
            }

            // Board + Live Evaluation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.showLiveEval) {
                    EvalBar(
                        evalPawns = state.liveEvalCp,
                        flipped = state.flipped,
                        modifier = Modifier
                            .height(260.dp)
                            .testTag("play_eval_bar")
                    )
                }
                ChessBoardView(
                    fen = state.fen,
                    boardTheme = state.boardTheme,
                    flipped = state.flipped,
                    selectedSquare = state.selectedSquare,
                    lastMoveSquares = state.lastMoveSquares,
                    legalTargets = state.legalTargets,
                    arrows = hintArrows,
                    onSquareClick = onSquareClick,
                    modifier = Modifier.weight(1f)
                )
            }

            // Bottom player + move counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(bottomLabel, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                if (state.lastMoveSan.isNotEmpty()) {
                    Text(
                        text = "Last: ${state.lastMoveSan}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Compact horizontal SAN strip
            if (state.sanMoves.isNotEmpty()) {
                val pgnSnippet = state.sanMoves.chunked(2).mapIndexed { idx, pair ->
                    val w = pair[0]
                    val b = pair.getOrNull(1)
                    if (b != null) "${idx + 1}. $w $b" else "${idx + 1}. $w"
                }.joinToString("  ")

                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = pgnSnippet,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = TextPrimary,
                        maxLines = 1,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            // Hint feedback banner
            AnimatedVisibility(visible = state.hintText != null || state.isHintLoading) {
                Surface(
                    color = AccentGreen.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.isHintLoading) "Finding best move…" else state.hintText.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreenHover,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Control Bar (Hint, Takeback, Flip, Resign)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onHint,
                    enabled = !state.isGameOver && !state.isEngineThinking && state.turn == state.playerColor,
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("play_hint_button")
                ) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = "Hint", modifier = Modifier.size(18.dp), tint = AccentGreenHover)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hint", color = TextPrimary, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onTakeback,
                    enabled = state.canTakeback && !state.isGameOver,
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("play_takeback_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Takeback", modifier = Modifier.size(18.dp), tint = TextPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Undo", color = TextPrimary, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onFlip,
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("play_flip_button")
                ) {
                    Icon(Icons.Filled.Flip, contentDescription = "Flip board", modifier = Modifier.size(18.dp), tint = TextPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flip", color = TextPrimary, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onResign,
                    enabled = !state.isGameOver,
                    border = BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("play_resign_button")
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = "Resign", modifier = Modifier.size(18.dp), tint = Color(0xFFFF8A80))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resign", color = TextPrimary, fontSize = 12.sp)
                }
            }

            // Post-game actions with spring animation
            AnimatedVisibility(
                visible = state.isGameOver,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut()
            ) {
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = state.endMessage,
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentGreenHover
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onAnalyseGame,
                                enabled = state.sanMoves.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentGreen,
                                    contentColor = OnAccentDark
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("play_analyse_game")
                            ) {
                                Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyse game", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onRematch,
                                border = BorderStroke(1.dp, BorderSubtle),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("play_rematch")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rematch", color = TextPrimary)
                            }
                        }
                    }
                }
            }

            TextButton(
                onClick = onLeaveGame,
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("play_new_setup_button")
            ) {
                Text("New match setup", color = TextMuted)
            }
        }
    }
}
