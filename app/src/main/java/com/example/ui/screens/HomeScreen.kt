package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.ChessBoard
import com.example.chess.ChessPlatform
import com.example.chess.GameAnalysisResult
import com.example.chess.MoveClass
import com.example.ui.components.ChessPieceIcon
import com.example.ui.components.DiggyButton
import com.example.ui.components.DiggyButtonVariant
import com.example.ui.components.DiggyCard
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val PreviewBoardLight = Color(0xFFEEEED2)
private val PreviewBoardDark = Color(0xFF769656)
private val PreviewHighlightYellow = Color(0x8CF5C542)
private val PreviewArrowYellow = Color(0xFFF5C542)
private val PreviewDotBest = Color(0xFF7FA650)
private val PreviewDotMistake = Color(0xFFF5A623)
private val PreviewDotBlunder = Color(0xFFCA3431)

private data class HomePreviewData(
    val fen: String,
    val fromSquare: String?,
    val toSquare: String?,
    val evalWhiteFraction: Float,
    val accuracyText: String,
    val evalText: String,
    val coachTip: String,
    val bestCount: Int,
    val mistakeCount: Int,
    val blunderCount: Int,
    val isLiveGameData: Boolean
) {
    companion object {
        private const val STATIC_PREVIEW_FEN =
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"

        fun fromAnalysis(result: GameAnalysisResult?): HomePreviewData {
            if (result == null || result.plies.isEmpty()) {
                return HomePreviewData(
                    fen = STATIC_PREVIEW_FEN,
                    fromSquare = "g1",
                    toSquare = "f3",
                    evalWhiteFraction = 0.64f,
                    accuracyText = "87%",
                    evalText = "+1.4",
                    coachTip = "Coach: Nf3 was the best move here. Qh5 early gave your opponent a free tempo.",
                    bestCount = 12,
                    mistakeCount = 2,
                    blunderCount = 1,
                    isLiveGameData = false
                )
            }

            val focus = result.keyMistakes.firstOrNull() ?: result.plies.last()
            val stats = result.summaryStats
            val accuracy = stats.whiteAccuracy.roundToInt().coerceIn(0, 100)
            val evalCp = focus.evalAfter
            val mate = focus.mate
            val evalLabel = when {
                mate != null && mate > 0 -> "+M${abs(mate)}"
                mate != null -> "-M${abs(mate)}"
                else -> String.format(Locale.US, "%+.1f", evalCp)
            }
            val barFill = when {
                mate != null && mate > 0 -> 0.92f
                mate != null -> 0.08f
                else -> (0.5f + (evalCp / 8.0).toFloat()).coerceIn(0.10f, 0.90f)
            }
            val rawCoach = focus.coachHeadline.takeIf { it.isNotBlank() }
                ?: focus.explanation.takeIf { it.isNotBlank() }
                ?: result.openingSummary.engineEvalNote
            val formattedCoach = if (rawCoach.startsWith("Coach", ignoreCase = true)) {
                rawCoach
            } else {
                "Coach: $rawCoach"
            }

            val bestTotal =
                (stats.whiteCounts[MoveClass.BEST] ?: 0) + (stats.blackCounts[MoveClass.BEST] ?: 0) +
                    (stats.whiteCounts[MoveClass.EXCELLENT] ?: 0) + (stats.blackCounts[MoveClass.EXCELLENT] ?: 0) +
                    (stats.whiteCounts[MoveClass.BRILLIANT] ?: 0) + (stats.blackCounts[MoveClass.BRILLIANT] ?: 0)
            val mistakeTotal =
                (stats.whiteCounts[MoveClass.MISTAKE] ?: 0) + (stats.blackCounts[MoveClass.MISTAKE] ?: 0)
            val blunderTotal =
                (stats.whiteCounts[MoveClass.BLUNDER] ?: 0) + (stats.blackCounts[MoveClass.BLUNDER] ?: 0)

            return HomePreviewData(
                fen = focus.fenBefore,
                fromSquare = focus.bestFrom ?: focus.move.fromName,
                toSquare = focus.bestTo ?: focus.move.toName,
                evalWhiteFraction = barFill,
                accuracyText = "$accuracy%",
                evalText = evalLabel,
                coachTip = formattedCoach,
                bestCount = bestTotal,
                mistakeCount = mistakeTotal,
                blunderCount = blunderTotal,
                isLiveGameData = true
            )
        }
    }
}

@Composable
fun HomeScreen(
    latestAnalysis: GameAnalysisResult?,
    onAnalysePgn: (String) -> Unit,
    onSearchUsername: (ChessPlatform, String) -> Unit,
    onUploadPgn: (content: String, fileName: String?) -> Unit,
    onOpenPlatformImporter: () -> Unit,
    onTrySample: () -> Unit,
    onOpenLatestAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPlatform by remember { mutableStateOf(ChessPlatform.CHESS_COM) }
    var usernameDraft by remember { mutableStateOf("") }
    var pgnDraft by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    val previewData = remember(latestAnalysis) { HomePreviewData.fromAnalysis(latestAnalysis) }

    val pgnFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (!content.isNullOrBlank()) {
                val fileName = uri.lastPathSegment?.substringAfterLast('/')
                onUploadPgn(content.trim(), fileName)
            } else {
                inputError = "Could not read selected PGN file."
            }
        }
    }

    fun submitUsernameSearch() {
        val cleanUser = usernameDraft.trim()
        if (cleanUser.isEmpty()) {
            inputError = "Enter a ${selectedPlatform.label} username to search."
        } else {
            inputError = null
            onSearchUsername(selectedPlatform, cleanUser)
        }
    }

    fun submitPrimaryAction() {
        val cleanPgn = pgnDraft.trim()
        val cleanUser = usernameDraft.trim()
        when {
            cleanPgn.isNotEmpty() -> {
                inputError = null
                onAnalysePgn(cleanPgn)
            }
            cleanUser.isNotEmpty() -> {
                inputError = null
                onSearchUsername(selectedPlatform, cleanUser)
            }
            else -> {
                inputError = "Enter a username or paste a PGN to analyse."
            }
        }
    }

    fun shareCurrentSummary() {
        val textToShare = when {
            latestAnalysis != null -> {
                val st = latestAnalysis.summaryStats
                val wAcc = String.format(Locale.US, "%.1f%%", st.whiteAccuracy)
                val bAcc = String.format(Locale.US, "%.1f%%", st.blackAccuracy)
                "Chess Analyzer: AI Coach\n${st.whiteName} ($wAcc) vs ${st.blackName} ($bAcc)\n" +
                    "Opening: ${latestAnalysis.openingSummary.entry?.name ?: "Custom Game"}\n\n" +
                    latestAnalysis.pgn
            }
            pgnDraft.trim().isNotEmpty() -> pgnDraft.trim()
            else -> "Analyse your chess games with Stockfish 19 and AI Coach in Chess Analyzer: AI Coach!"
        }
        runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, textToShare)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share via"))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(durationMillis = 260)) +
                slideInVertically(animationSpec = tween(durationMillis = 260)) { it / 28 }
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DiggySpacing.md, vertical = 14.dp)
                    .testTag("home_screen_root"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Matte Dark Hero Card (#18181B)
                HomeHeroCard()

                // 2. White Chessboard & Stats Preview Card (#FFFFFF)
                HomePreviewCard(
                    data = previewData,
                    onClick = {
                        if (previewData.isLiveGameData) {
                            onOpenLatestAnalysis()
                        } else {
                            onTrySample()
                        }
                    }
                )

                // 3. Dark Matte Combined Action Card (#18181B) with White Inputs & #7FA650 CTA
                HomeMainActionCard(
                    selectedPlatform = selectedPlatform,
                    onSelectPlatform = { selectedPlatform = it },
                    username = usernameDraft,
                    onUsernameChange = {
                        usernameDraft = it
                        inputError = null
                    },
                    onSearchClick = ::submitUsernameSearch,
                    onOpenPlatformImporter = onOpenPlatformImporter,
                    pgnText = pgnDraft,
                    onPgnChange = {
                        pgnDraft = it
                        inputError = null
                    },
                    errorText = inputError,
                    onAnalyseClick = ::submitPrimaryAction
                )

                // 4. Responsive Quick Action Buttons: Upload PGN, Share, Try Sample
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
                                    text = "Upload PGN",
                                    onClick = {
                                        pgnFileLauncher.launch(
                                            arrayOf("application/x-chess-pgn", "text/plain", "*/*")
                                        )
                                    },
                                    variant = DiggyButtonVariant.OUTLINED,
                                    icon = Icons.Rounded.UploadFile,
                                    testTag = "home_upload_pgn_card",
                                    modifier = Modifier.weight(1f)
                                )
                                DiggyButton(
                                    text = "Share",
                                    onClick = ::shareCurrentSummary,
                                    variant = DiggyButtonVariant.OUTLINED,
                                    icon = Icons.Rounded.Share,
                                    testTag = "home_share_card",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            DiggyButton(
                                text = "Try Sample",
                                onClick = onTrySample,
                                variant = DiggyButtonVariant.SECONDARY,
                                icon = Icons.Rounded.PlayArrow,
                                testTag = "home_sample_card",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiggyButton(
                                text = "Upload PGN",
                                onClick = {
                                    pgnFileLauncher.launch(
                                        arrayOf("application/x-chess-pgn", "text/plain", "*/*")
                                    )
                                },
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.UploadFile,
                                testTag = "home_upload_pgn_card",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyButton(
                                text = "Share",
                                onClick = ::shareCurrentSummary,
                                variant = DiggyButtonVariant.OUTLINED,
                                icon = Icons.Rounded.Share,
                                testTag = "home_share_card",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyButton(
                                text = "Try Sample",
                                onClick = onTrySample,
                                variant = DiggyButtonVariant.SECONDARY,
                                icon = Icons.Rounded.PlayArrow,
                                testTag = "home_sample_card",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun HomeHeroCard() {
    DiggyCard(
        darkMatte = true,
        elevated = false,
        borderColor = MatteDarkBorder,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_header")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MatteDarkSecondary,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MatteDarkBorder)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_piece_wn),
                        contentDescription = "Chess Analyzer knight emblem",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(24.dp)
                    )
                }
                Text(
                    text = "CHESS ANALYZER",
                    fontFamily = NunitoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    letterSpacing = 0.8.sp,
                    color = OnMatteDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = AccentGreen,
                shape = RoundedCornerShape(999.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("home_tagline_pill"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = OnAccentDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Analyse your chess with AI",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = OnAccentDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "Stockfish 19 Engine with an AI Coach",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                color = OnMatteDarkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("home_hero_subtitle")
            )
        }
    }
}

@Composable
private fun HomePreviewCard(
    data: HomePreviewData,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "previewPressScale"
    )

    Surface(
        color = CardDark,
        shape = RoundedCornerShape(DiggySpacing.cardRadius),
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(DiggySpacing.cardRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .testTag("home_preview_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Preview Header & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (data.isLiveGameData) "Last Analysed Game" else "Engine Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = if (data.isLiveGameData) AccentGreen.copy(alpha = 0.14f) else CardSurfaceElevated,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(
                        1.dp,
                        if (data.isLiveGameData) AccentGreen.copy(alpha = 0.35f) else BorderSubtle
                    )
                ) {
                    Text(
                        text = if (data.isLiveGameData) "Latest Analysis" else "Sample Preview",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (data.isLiveGameData) AccentGreen else TextMuted,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("home_preview_badge")
                    )
                }
            }

            // Eval bar + 8x8 mini board + Accuracy & Eval stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(118.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MatteDarkCard)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(data.evalWhiteFraction.coerceIn(0.05f, 0.95f))
                            .background(Color.White)
                    )
                }

                MiniPreviewBoard(
                    fen = data.fen,
                    fromSquare = data.fromSquare,
                    toSquare = data.toSquare,
                    modifier = Modifier
                        .size(118.dp)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Accuracy",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Text(
                            text = data.accuracyText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Eval",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Text(
                            text = data.evalText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            maxLines = 1
                        )
                    }
                }
            }

            // AI Coach tip banner
            Surface(
                color = CardSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MatteDarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = "AI Coach",
                            tint = OnMatteDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = data.coachTip,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Legend dots row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreviewLegendDot(color = PreviewDotBest, label = "Best ${data.bestCount}")
                PreviewLegendDot(color = PreviewDotMistake, label = "Mistake ${data.mistakeCount}")
                PreviewLegendDot(color = PreviewDotBlunder, label = "Blunder ${data.blunderCount}")
            }
        }
    }
}

@Composable
private fun PreviewLegendDot(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun MiniPreviewBoard(
    fen: String,
    fromSquare: String?,
    toSquare: String?,
    modifier: Modifier = Modifier
) {
    val board = remember(fen) {
        runCatching { ChessBoard(fen) }.getOrElse { ChessBoard() }
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (rank in 7 downTo 0) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (file in 0..7) {
                        val sq = "${'a' + file}${rank + 1}"
                        val isLight = (file + rank) % 2 != 0
                        val baseColor = if (isLight) PreviewBoardLight else PreviewBoardDark
                        val isHighlighted = sq == fromSquare || sq == toSquare
                        val piece = board.pieceAt(sq)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(baseColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isHighlighted) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(PreviewHighlightYellow)
                                )
                            }
                            if (piece != null) {
                                ChessPieceIcon(
                                    piece = piece,
                                    pieceStyle = PieceStyle.CLASSIC_2D,
                                    modifier = Modifier.fillMaxSize(0.86f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (fromSquare != null && toSquare != null && fromSquare.length == 2 && toSquare.length == 2) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cell = size.width / 8f
                val fromFile = (fromSquare[0] - 'a').coerceIn(0, 7)
                val fromRank = (fromSquare[1] - '1').coerceIn(0, 7)
                val toFile = (toSquare[0] - 'a').coerceIn(0, 7)
                val toRank = (toSquare[1] - '1').coerceIn(0, 7)

                val start = Offset((fromFile + 0.5f) * cell, (7 - fromRank + 0.5f) * cell)
                val end = Offset((toFile + 0.5f) * cell, (7 - toRank + 0.5f) * cell)
                val dx = end.x - start.x
                val dy = end.y - start.y
                val angle = atan2(dy.toDouble(), dx.toDouble())
                val headLen = cell * 0.55f
                val shaftEnd = Offset(
                    end.x - (headLen * 0.58f * cos(angle)).toFloat(),
                    end.y - (headLen * 0.58f * sin(angle)).toFloat()
                )

                drawLine(
                    color = PreviewArrowYellow,
                    start = start,
                    end = shaftEnd,
                    strokeWidth = cell * 0.24f,
                    cap = StrokeCap.Round
                )

                val p1 = Offset(
                    end.x - (headLen * cos(angle - 0.52)).toFloat(),
                    end.y - (headLen * sin(angle - 0.52)).toFloat()
                )
                val p2 = Offset(
                    end.x - (headLen * cos(angle + 0.52)).toFloat(),
                    end.y - (headLen * sin(angle + 0.52)).toFloat()
                )
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(end.x, end.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    close()
                }
                drawPath(path = path, color = PreviewArrowYellow)
            }
        }
    }
}

@Composable
private fun HomeMainActionCard(
    selectedPlatform: ChessPlatform,
    onSelectPlatform: (ChessPlatform) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    onOpenPlatformImporter: () -> Unit,
    pgnText: String,
    onPgnChange: (String) -> Unit,
    errorText: String?,
    onAnalyseClick: () -> Unit
) {
    DiggyCard(
        darkMatte = true,
        elevated = false,
        borderColor = MatteDarkBorder,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_review_white_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Top Section: "Search by Username" + Chess.com / Lichess selector
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackSelector = maxWidth < 340.dp
                if (stackSelector) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Search by Username",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnMatteDark
                        )
                        PlatformPillToggle(
                            selectedPlatform = selectedPlatform,
                            onSelectPlatform = onSelectPlatform,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search by Username",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnMatteDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        PlatformPillToggle(
                            selectedPlatform = selectedPlatform,
                            onSelectPlatform = onSelectPlatform
                        )
                    }
                }
            }

            // Crisp White Username Input Box with Dark Text
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(DiggySpacing.controlRadius),
                border = BorderStroke(1.dp, InputBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (username.isEmpty()) {
                            Text(
                                text = "Enter ${selectedPlatform.label} username...",
                                fontFamily = InterFontFamily,
                                fontSize = 13.5.sp,
                                color = Color(0xFF6B7280),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = username,
                            onValueChange = onUsernameChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                color = Color(0xFF111827)
                            ),
                            cursorBrush = SolidColor(AccentGreen),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearchClick() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_username_input")
                        )
                    }

                    IconButton(
                        onClick = {
                            if (username.trim().isNotEmpty()) {
                                onSearchClick()
                            } else {
                                onOpenPlatformImporter()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("home_username_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search ${selectedPlatform.label} username",
                            tint = Color(0xFF111827),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Subtle "OR" divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    color = MatteDarkBorder,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "OR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = OnMatteDarkMuted,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(
                    color = MatteDarkBorder,
                    modifier = Modifier.weight(1f)
                )
            }

            // Bottom Section: "Review your game" + White PGN Textarea
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Review your game",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnMatteDark
                    )
                    Text(
                        text = "Paste a PGN to start",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnMatteDarkMuted
                    )
                }
                Surface(
                    color = MatteDarkSecondary,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, MatteDarkBorder)
                ) {
                    Text(
                        text = "SF 19 + AI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGreen,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(DiggySpacing.controlRadius),
                border = BorderStroke(1.dp, InputBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 94.dp, max = 136.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    if (pgnText.isEmpty()) {
                        Text(
                            text = "1. e4 e5 2. Nf3 Nc6 ...",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    BasicTextField(
                        value = pgnText,
                        onValueChange = onPgnChange,
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF111827)
                        ),
                        cursorBrush = SolidColor(AccentGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 66.dp)
                            .testTag("home_pgn_input")
                    )
                }
            }

            if (errorText != null) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFCA5A5)
                )
            }

            // Primary Green #7FA650 CTA Button
            DiggyButton(
                text = "Analyse Game",
                onClick = onAnalyseClick,
                variant = DiggyButtonVariant.PRIMARY,
                testTag = "home_analyse_game_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PlatformPillToggle(
    selectedPlatform: ChessPlatform,
    onSelectPlatform: (ChessPlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MatteDarkSecondary,
        shape = RoundedCornerShape(DiggySpacing.controlRadius),
        border = BorderStroke(1.dp, MatteDarkBorder),
        modifier = modifier.testTag("home_platform_toggle_row")
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChessPlatform.entries.forEach { platform ->
                val selected = selectedPlatform == platform
                Surface(
                    onClick = { onSelectPlatform(platform) },
                    color = if (selected) AccentGreen else Color.Transparent,
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier
                        .heightIn(min = 34.dp)
                        .testTag(if (platform == ChessPlatform.LICHESS) "home_platform_switch" else "home_platform_chesscom")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = platform.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) OnAccentDark else OnMatteDarkMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
