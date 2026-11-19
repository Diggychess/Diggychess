package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.chess.*
import com.example.ui.components.MistakeDrillDialog
import com.example.ui.components.PlatformImporterSheet
import com.example.ui.theme.*
import com.example.ui.viewmodel.AnalyzeUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyzeScreen(
    state: AnalyzeUiState,
    onPgnChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onTrySample: () -> Unit,
    onSharePgn: () -> Unit,
    onShareAnalysis: () -> Unit,
    onDismissShare: () -> Unit,
    onOpenRecent: (SavedGameEntry) -> Unit,
    onGoToPly: (Int, Boolean) -> Unit,
    onToggleBestMove: () -> Unit,
    onToggleFlip: () -> Unit,
    onBoardThemeChange: (BoardTheme) -> Unit,
    onOpenImporter: () -> Unit,
    onCloseImporter: () -> Unit,
    onSelectImportPlatform: (ChessPlatform) -> Unit,
    onImportUsernameChange: (String) -> Unit,
    onFetchPlatformGames: () -> Unit,
    onSpeedFilter: (ImportSpeedFilter) -> Unit,
    onResultFilter: (ImportResultFilter) -> Unit,
    onColorFilter: (ImportColorFilter) -> Unit,
    onAnalyzeImportedGame: (ImportedGameSummary) -> Unit,
    onBoardSquareClick: (String) -> Unit,
    onSelectVariationPromotion: (PieceType) -> Unit,
    onDismissVariationPromotion: () -> Unit,
    onPlayEngineLineMove: (EngineVariation, Int) -> Unit,
    onStepVariation: (Int) -> Unit,
    onUndoVariation: () -> Unit,
    onExitVariation: () -> Unit,
    onPracticeMistakes: (Int?) -> Unit,
    onDrillSquareClick: (String) -> Unit,
    onDrillPromotion: (PieceType) -> Unit,
    onDismissDrillPromotion: () -> Unit,
    onDrillHint: () -> Unit,
    onDrillRetry: () -> Unit,
    onDrillNext: () -> Unit,
    onCloseDrill: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Page Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.analyse_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.analyse_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        // UnCappio Studio Username Importer CTA Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurfaceElevated),
            border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Chess.com & Lichess Studio Import",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StudioCyan
                    )
                    val prof = state.platformProfile
                    val sub = if (prof != null) {
                        val topStat = prof.stats.firstOrNull()
                        "${prof.username} (${prof.platform.label})" +
                            (topStat?.let { " · ${it.mode} ${it.rating}" } ?: "")
                    } else {
                        "Fetch ratings & recent games by username, filter & review in 1 tap."
                    }
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onOpenImporter,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioCyan,
                        contentColor = OnAccentDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("open_importer_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import", fontWeight = FontWeight.Bold)
                }
            }
        }

        // PGN Input Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.88f)),
            border = BorderStroke(1.dp, LineSubtle),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Game PGN",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.pgnText,
                    onValueChange = onPgnChange,
                    placeholder = { Text("Paste PGN here or import by username above", color = TextMuted) },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = TextPrimary,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp, max = 180.dp)
                        .testTag("pgn_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onAnalyze,
                        enabled = !state.isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = OnAccentDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("analyze_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (state.isAnalyzing) "Analysing…" else "Analyse game",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onSharePgn,
                        border = BorderStroke(1.dp, LineSubtle),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("share_pgn_button")
                    ) {
                        Text("Share PGN", color = TextPrimary)
                    }

                    TextButton(
                        onClick = onTrySample,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("try_sample_button")
                    ) {
                        Text("Try a sample", color = AccentGreen)
                    }
                }

                // Progress / Error / Status
                if (state.isAnalyzing) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            val total = state.progressTotal.coerceAtLeast(1)
                            val frac = (state.progressDone.toFloat() / total).coerceIn(0f, 1f)
                            Text(
                                text = "Analysing game… position ${state.progressDone} / $total (3-line eval)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { frac },
                                color = StudioCyan,
                                trackColor = BorderSubtle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                state.errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorErrorText
                    )
                }

                state.statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = SurfaceDark,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Recent Games (when no analysis result is open)
        if (state.result == null) {
            if (state.recentGames.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "RECENT GAMES",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    state.recentGames.forEach { entry ->
                        val dateStr = remember(entry.timestamp) {
                            SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(entry.timestamp))
                        }
                        Surface(
                            onClick = { onOpenRecent(entry) },
                            shape = RoundedCornerShape(14.dp),
                            color = CardDark,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("recent_game_${entry.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$dateStr · ${entry.moves} moves",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Games you analyse are saved here, on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        } else {
            // Full Analysis Workspace
            AnalyzeWorkspace(
                result = state.result,
                currentPly = state.currentPly,
                showBestMove = state.showBestMove,
                flipped = state.flipped,
                boardTheme = state.boardTheme,
                variation = state.variation,
                plyLiveLines = state.plyLiveLines,
                isLoadingPlyLines = state.isLoadingPlyLines,
                boardSelectedSquare = state.boardSelectedSquare,
                boardLegalTargets = state.boardLegalTargets,
                boardPendingPromotion = state.boardPendingPromotion,
                onGoToPly = onGoToPly,
                onToggleBestMove = onToggleBestMove,
                onToggleFlip = onToggleFlip,
                onShareAnalysis = onShareAnalysis,
                onBoardThemeChange = onBoardThemeChange,
                onBoardSquareClick = onBoardSquareClick,
                onSelectVariationPromotion = onSelectVariationPromotion,
                onDismissVariationPromotion = onDismissVariationPromotion,
                onPlayEngineLineMove = onPlayEngineLineMove,
                onStepVariation = onStepVariation,
                onUndoVariation = onUndoVariation,
                onExitVariation = onExitVariation,
                onPracticeMistakes = onPracticeMistakes
            )
        }
    }

    // Username Importer Bottom Sheet
    if (state.showImporterSheet) {
        PlatformImporterSheet(
            state = state,
            onSelectPlatform = onSelectImportPlatform,
            onUsernameChange = onImportUsernameChange,
            onFetch = onFetchPlatformGames,
            onSpeedFilter = onSpeedFilter,
            onResultFilter = onResultFilter,
            onColorFilter = onColorFilter,
            onAnalyzeImportedGame = onAnalyzeImportedGame,
            onDismiss = onCloseImporter
        )
    }

    // One-Tap Practice My Mistakes Drill Dialog
    state.mistakeDrill?.let { drill ->
        MistakeDrillDialog(
            drill = drill,
            boardTheme = state.boardTheme,
            onSquareClick = onDrillSquareClick,
            onSelectPromotion = onDrillPromotion,
            onDismissPromotion = onDismissDrillPromotion,
            onHint = onDrillHint,
            onRetry = onDrillRetry,
            onNext = onDrillNext,
            onDismiss = onCloseDrill
        )
    }

    // Share Analysis / PGN Dialog
    state.shareText?.let { shareContent ->
        ShareTextDialog(
            text = shareContent,
            onDismiss = onDismissShare
        )
    }
}

private val ColorErrorText = Color(0xFFFF8A80)

@Composable
fun ShareTextDialog(
    text: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardDark,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Share game analysis",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = BorderSubtle,
                        unfocusedBorderColor = BorderSubtle
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share KnightScope analysis"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = OnAccentDark),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(text))
                            copiedNotice = "Copied to clipboard."
                        },
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", color = TextPrimary)
                    }
                }
                copiedNotice?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AccentGreen)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = AccentGreen)
                    }
                }
            }
        }
    }
}
