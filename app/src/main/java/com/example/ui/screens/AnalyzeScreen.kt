package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.chess.*
import com.example.ui.components.*
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
    onSelectDepthPreset: (AnalysisDepthPreset) -> Unit,
    onRetryEngine: () -> Unit,
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
    onUploadPgn: (String, String?) -> Unit = { _, _ -> },
    onRequestMoveInsight: (Int) -> Unit = {},
    onUseOfflineMoveInsight: (Int) -> Unit = {},
    onRequestGameReport: () -> Unit = {},
    onUseOfflineGameReport: () -> Unit = {},
    onSendCoachMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pgnFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }
            if (!content.isNullOrBlank()) {
                val fileName = uri.lastPathSegment?.substringAfterLast('/')
                onUploadPgn(content, fileName)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = DiggySpacing.s2, vertical = DiggySpacing.s2),
        verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
    ) {
        // Clean Page Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
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
            Surface(
                color = AccentGreen.copy(alpha = 0.14f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "SF 19 + AI",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // Chess.com & Lichess Username Importer Hero Card
        Surface(
            color = MatteDarkCard,
            shape = RoundedCornerShape(DiggySpacing.cardRadius),
            border = BorderStroke(1.dp, BorderSubtle),
            shadowElevation = 5.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_diggy_hero),
                    contentDescription = "Chess Analyzer spotlight",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xEE1D1B18),
                                    Color(0xD4211F1C),
                                    Color(0x78211F1C)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Import from Chess.com & Lichess",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val prof = state.platformProfile
                        val sub = if (prof != null) {
                            val topStat = prof.stats.firstOrNull()
                            "${prof.username} (${prof.platform.label})" +
                                (topStat?.let { " · ${it.mode} ${it.rating}" } ?: "")
                        } else {
                            "Fetch live ratings & recent games by username."
                        }
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD0CDC7),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DiggyButton(
                        text = "Import",
                        onClick = onOpenImporter,
                        variant = DiggyButtonVariant.PRIMARY,
                        icon = Icons.Rounded.CloudDownload,
                        testTag = "open_importer_button"
                    )
                }
            }
        }

        // PGN Input Light Card (#FFFFFF)
        DiggyCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Game PGN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Paste moves or upload .pgn",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                OutlinedTextField(
                    value = state.pgnText,
                    onValueChange = onPgnChange,
                    placeholder = {
                        Text(
                            "Paste PGN here or import a game above",
                            color = InputHint
                        )
                    },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        color = InputText,
                        fontSize = 13.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 112.dp, max = 176.dp)
                        .testTag("pgn_input"),
                    colors = diggyInputColors(),
                    shape = RoundedCornerShape(DiggySpacing.controlRadius)
                )

                // Stockfish 19 WASM Engine Status Row
                when (val st = state.engineStatus) {
                    is StockfishEngineStatus.Loading -> {
                        Surface(
                            color = BgDark,
                            shape = RoundedCornerShape(DiggySpacing.chipRadius),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("engine_status_loading")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = StudioCyan,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Engine loading… (Stockfish 19 WASM)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                    is StockfishEngineStatus.Ready -> {
                        Surface(
                            color = AccentGreen.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(DiggySpacing.chipRadius),
                            border = BorderStroke(1.dp, AccentGreen.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("engine_status_ready")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Engine ready",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Engine ready · ${st.engineName} (v${st.version})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    is StockfishEngineStatus.Error -> {
                        Surface(
                            color = FeedbackDangerRed.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(DiggySpacing.chipRadius),
                            border = BorderStroke(1.dp, FeedbackDangerRed.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("engine_status_error")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = "Engine error",
                                    tint = FeedbackDangerRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = st.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FeedbackDangerRed,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = onRetryEngine,
                                    modifier = Modifier
                                        .heightIn(min = DiggySpacing.touchTargetMin)
                                        .testTag("retry_engine_button")
                                ) {
                                    Text("Retry", color = AccentGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Selectable Stockfish Depth Presets (Fast 12, Normal 18, Deep 22)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalysisDepthPreset.entries.forEach { preset ->
                        DiggyChip(
                            label = preset.label,
                            selected = state.selectedDepthPreset == preset,
                            onClick = { onSelectDepthPreset(preset) },
                            testTag = "depth_preset_${preset.id}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                DiggyButton(
                    text = if (state.isAnalyzing) "Analysing…" else "Analyse Game",
                    onClick = onAnalyze,
                    loading = state.isAnalyzing,
                    icon = Icons.Rounded.Analytics,
                    testTag = "analyze_button",
                    modifier = Modifier.fillMaxWidth()
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth < 400.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DiggyButton(
                                    text = "Upload .pgn",
                                    onClick = { pgnFileLauncher.launch(arrayOf("application/x-chess-pgn", "text/plain", "*/*")) },
                                    variant = DiggyButtonVariant.OUTLINED,
                                    icon = Icons.Rounded.UploadFile,
                                    testTag = "upload_pgn_button",
                                    modifier = Modifier.weight(1f)
                                )
                                DiggyButton(
                                    text = "Share PGN",
                                    onClick = onSharePgn,
                                    variant = DiggyButtonVariant.OUTLINED,
                                    icon = Icons.Rounded.Share,
                                    testTag = "share_pgn_button",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            DiggyButton(
                                text = "Try Sample",
                                onClick = onTrySample,
                                variant = DiggyButtonVariant.SECONDARY,
                                icon = Icons.Rounded.PlayArrow,
                                testTag = "try_sample_button",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiggyButton(
                                text = "Upload .pgn",
                                onClick = { pgnFileLauncher.launch(arrayOf("application/x-chess-pgn", "text/plain", "*/*")) },
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.UploadFile,
                                testTag = "upload_pgn_button",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyButton(
                                text = "Share PGN",
                                onClick = onSharePgn,
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.Share,
                                testTag = "share_pgn_button",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyButton(
                                text = "Try Sample",
                                onClick = onTrySample,
                                variant = DiggyButtonVariant.SECONDARY,
                                icon = Icons.Rounded.PlayArrow,
                                testTag = "try_sample_button",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (state.isAnalyzing) {
                    Surface(
                        color = BgDark,
                        shape = RoundedCornerShape(DiggySpacing.controlRadius),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val total = state.progressTotal.coerceAtLeast(1)
                            val frac = (state.progressDone.toFloat() / total).coerceIn(0f, 1f)
                            Text(
                                text = "Stockfish 19 (Depth ${state.selectedDepthPreset.depth}): ${state.progressDone} / $total positions",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                            LinearProgressIndicator(
                                progress = { frac },
                                color = AccentGreen,
                                trackColor = BorderSubtle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                            )
                        }
                    }
                }

                state.errorMessage?.let { err ->
                    DiggyErrorState(message = err)
                }

                state.pgnValidationWarning?.let { warn ->
                    Surface(
                        color = TrophyGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(DiggySpacing.chipRadius),
                        border = BorderStroke(1.dp, TrophyGold.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = warn,
                            style = MaterialTheme.typography.bodySmall,
                            color = TrophyGold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                state.statusMessage?.let { msg ->
                    Surface(
                        color = CardSurfaceElevated,
                        shape = RoundedCornerShape(DiggySpacing.chipRadius),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Recent Games or Full Analysis Workspace
        if (state.result == null) {
            if (state.recentGames.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Recent Games",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    state.recentGames.forEach { entry ->
                        val dateStr = remember(entry.timestamp) {
                            SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(entry.timestamp))
                        }
                        DiggyCard(
                            onClick = { onOpenRecent(entry) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recent_game_${entry.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
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
                DiggyEmptyState(
                    title = "No analysed games yet",
                    subtitle = "Paste a PGN, try a sample game, or import from Chess.com / Lichess.",
                    icon = Icons.Rounded.Analytics,
                    actionLabel = "Try Sample Game",
                    onAction = onTrySample
                )
            }
        } else {
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
                moveInsightsByPly = state.moveInsightsByPly,
                gameReportState = state.gameReportState,
                coachChatMessages = state.coachChatMessages,
                coachChatStreamState = state.coachChatStreamState,
                dailyAiRemaining = state.dailyAiRemaining,
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
                onPracticeMistakes = onPracticeMistakes,
                onRequestMoveInsight = onRequestMoveInsight,
                onUseOfflineMoveInsight = onUseOfflineMoveInsight,
                onRequestGameReport = onRequestGameReport,
                onUseOfflineGameReport = onUseOfflineGameReport,
                onSendCoachMessage = onSendCoachMessage
            )
        }
    }

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

    state.shareText?.let { shareContent ->
        ShareTextDialog(
            text = shareContent,
            onDismiss = onDismissShare
        )
    }
}

@Composable
fun ShareTextDialog(
    text: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        DiggyCard(
            elevated = true,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Share Game Analysis",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = InputText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    colors = diggyInputColors(),
                    shape = RoundedCornerShape(DiggySpacing.controlRadius)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiggyButton(
                        text = "Share",
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Chess Analyzer analysis"))
                        },
                        variant = DiggyButtonVariant.PRIMARY,
                        icon = Icons.Rounded.Share,
                        modifier = Modifier.weight(1f)
                    )
                    DiggyButton(
                        text = "Copy",
                        onClick = {
                            clipboard.setText(AnnotatedString(text))
                            copiedNotice = "Copied to clipboard."
                        },
                        variant = DiggyButtonVariant.OUTLINED,
                        icon = Icons.Rounded.ContentCopy,
                        modifier = Modifier.weight(1f)
                    )
                }
                copiedNotice?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AccentGreen)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = DiggySpacing.touchTargetMin)
                    ) {
                        Text("Close", color = AccentGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
