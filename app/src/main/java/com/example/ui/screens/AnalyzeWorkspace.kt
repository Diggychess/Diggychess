package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.LastPage
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.*
import com.example.service.AiGameSummaryReport
import com.example.service.AiInsightState
import com.example.service.AiMoveInsight
import com.example.service.CoachChatMessage
import com.example.service.CoachChatStreamState
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.VariationBranch
import java.util.Locale

@Composable
fun AnalyzeWorkspace(
    result: GameAnalysisResult,
    currentPly: Int,
    showBestMove: Boolean,
    flipped: Boolean,
    boardTheme: BoardTheme,
    variation: VariationBranch?,
    plyLiveLines: List<EngineVariation>,
    isLoadingPlyLines: Boolean,
    boardSelectedSquare: String?,
    boardLegalTargets: Map<String, Boolean>,
    boardPendingPromotion: Pair<String, String>?,
    moveInsightsByPly: Map<Int, AiInsightState<AiMoveInsight>> = emptyMap(),
    gameReportState: AiInsightState<AiGameSummaryReport> = AiInsightState.Idle,
    coachChatMessages: List<CoachChatMessage> = emptyList(),
    coachChatStreamState: CoachChatStreamState = CoachChatStreamState.Idle,
    dailyAiRemaining: Int = 35,
    onGoToPly: (Int, Boolean) -> Unit,
    onToggleBestMove: () -> Unit,
    onToggleFlip: () -> Unit,
    onShareAnalysis: () -> Unit,
    onBoardThemeChange: (BoardTheme) -> Unit,
    onBoardSquareClick: (String) -> Unit,
    onSelectVariationPromotion: (PieceType) -> Unit,
    onDismissVariationPromotion: () -> Unit,
    onPlayEngineLineMove: (EngineVariation, Int) -> Unit,
    onStepVariation: (Int) -> Unit,
    onUndoVariation: () -> Unit,
    onExitVariation: () -> Unit,
    onPracticeMistakes: (Int?) -> Unit,
    onRequestMoveInsight: (Int) -> Unit = {},
    onUseOfflineMoveInsight: (Int) -> Unit = {},
    onRequestGameReport: () -> Unit = {},
    onUseOfflineGameReport: () -> Unit = {},
    onSendCoachMessage: (String) -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 820.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DiggySpacing.s2),
                verticalAlignment = Alignment.Top
            ) {
                Box(modifier = Modifier.weight(1.05f)) {
                    BoardAndMoveCard(
                        result = result,
                        currentPly = currentPly,
                        showBestMove = showBestMove,
                        flipped = flipped,
                        boardTheme = boardTheme,
                        variation = variation,
                        plyLiveLines = plyLiveLines,
                        isLoadingPlyLines = isLoadingPlyLines,
                        boardSelectedSquare = boardSelectedSquare,
                        boardLegalTargets = boardLegalTargets,
                        boardPendingPromotion = boardPendingPromotion,
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
                        moveInsightsByPly = moveInsightsByPly,
                        onRequestMoveInsight = onRequestMoveInsight,
                        onUseOfflineMoveInsight = onUseOfflineMoveInsight
                    )
                }
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
                ) {
                    AnalysisRightCards(
                        result = result,
                        currentPly = currentPly,
                        gameReportState = gameReportState,
                        coachChatMessages = coachChatMessages,
                        coachChatStreamState = coachChatStreamState,
                        dailyAiRemaining = dailyAiRemaining,
                        onGoToPly = onGoToPly,
                        onPracticeMistakes = onPracticeMistakes,
                        onRequestGameReport = onRequestGameReport,
                        onUseOfflineGameReport = onUseOfflineGameReport,
                        onSendCoachMessage = onSendCoachMessage
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DiggySpacing.s2)
            ) {
                BoardAndMoveCard(
                    result = result,
                    currentPly = currentPly,
                    showBestMove = showBestMove,
                    flipped = flipped,
                    boardTheme = boardTheme,
                    variation = variation,
                    plyLiveLines = plyLiveLines,
                    isLoadingPlyLines = isLoadingPlyLines,
                    boardSelectedSquare = boardSelectedSquare,
                    boardLegalTargets = boardLegalTargets,
                    boardPendingPromotion = boardPendingPromotion,
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
                    moveInsightsByPly = moveInsightsByPly,
                    onRequestMoveInsight = onRequestMoveInsight,
                    onUseOfflineMoveInsight = onUseOfflineMoveInsight
                )
                AnalysisRightCards(
                    result = result,
                    currentPly = currentPly,
                    gameReportState = gameReportState,
                    coachChatMessages = coachChatMessages,
                    coachChatStreamState = coachChatStreamState,
                    dailyAiRemaining = dailyAiRemaining,
                    onGoToPly = onGoToPly,
                    onPracticeMistakes = onPracticeMistakes,
                    onRequestGameReport = onRequestGameReport,
                    onUseOfflineGameReport = onUseOfflineGameReport,
                    onSendCoachMessage = onSendCoachMessage
                )
            }
        }
    }
}

@Composable
private fun BoardAndMoveCard(
    result: GameAnalysisResult,
    currentPly: Int,
    showBestMove: Boolean,
    flipped: Boolean,
    boardTheme: BoardTheme,
    variation: VariationBranch?,
    plyLiveLines: List<EngineVariation>,
    isLoadingPlyLines: Boolean,
    boardSelectedSquare: String?,
    boardLegalTargets: Map<String, Boolean>,
    boardPendingPromotion: Pair<String, String>?,
    onGoToPly: (Int, Boolean) -> Unit,
    onToggleBestMove: () -> Unit,
    onToggleFlip: () -> Unit,
    onShareAnalysis: () -> Unit,
    onBoardThemeChange: (BoardTheme) -> Unit,
    onBoardSquareClick: (String) -> Unit,
    onSelectVariationPromotion: (PieceType) -> Unit,
    onDismissVariationPromotion: () -> Unit,
    onPlayEngineLineMove: (EngineVariation, Int) -> Unit,
    onStepVariation: (Int) -> Unit,
    onUndoVariation: () -> Unit,
    onExitVariation: () -> Unit,
    onPracticeMistakes: (Int?) -> Unit,
    moveInsightsByPly: Map<Int, AiInsightState<AiMoveInsight>>,
    onRequestMoveInsight: (Int) -> Unit,
    onUseOfflineMoveInsight: (Int) -> Unit
) {
    val activePly = if (currentPly > 0) result.plies.getOrNull(currentPly - 1) else null
    val isShowingBest = variation == null && showBestMove && activePly?.bestFrom != null && activePly.bestTo != null
    val displayFen = when {
        variation != null -> variation.activeFen
        activePly == null -> result.headers["FEN"]?.takeIf { it.isNotBlank() } ?: ChessBoard.START_FEN
        isShowingBest -> activePly.fenBefore
        else -> activePly.fenAfter
    }

    val turnColor = remember(displayFen) { ChessBoard(displayFen).turn }

    val highlightSquares = when {
        variation != null -> variation.lastMoveSquares
        activePly != null && !isShowingBest -> listOf(activePly.move.fromName, activePly.move.toName)
        else -> emptyList()
    }

    val selectedSq = variation?.selectedSquare ?: boardSelectedSquare
    val legalTargets = variation?.legalTargets ?: boardLegalTargets

    val moveBadge = if (variation == null && activePly != null && !isShowingBest) {
        SquareMoveBadge(
            square = activePly.move.toName,
            glyph = activePly.badgeGlyph,
            color = activePly.badgeColor
        )
    } else null

    val arrows = remember(variation, isShowingBest, activePly, plyLiveLines) {
        when {
            variation != null -> {
                val uci = variation.liveLines.firstOrNull()?.firstUci
                if (uci != null && uci.length >= 4) {
                    listOf(BoardArrow(uci.substring(0, 2), uci.substring(2, 4), ArrowSecondaryCyan))
                } else emptyList()
            }
            isShowingBest && activePly != null && activePly.bestFrom != null && activePly.bestTo != null -> {
                listOf(
                    BoardArrow(activePly.bestFrom, activePly.bestTo, ArrowBestGreen),
                    BoardArrow(activePly.move.fromName, activePly.move.toName, ArrowPlayedRed)
                )
            }
            else -> {
                val uci = plyLiveLines.firstOrNull()?.firstUci
                if (uci != null && uci.length >= 4) {
                    listOf(BoardArrow(uci.substring(0, 2), uci.substring(2, 4), ArrowSecondaryCyan))
                } else emptyList()
            }
        }
    }

    val opLabelText = when {
        result.plies.isEmpty() -> ""
        currentPly == 0 -> "Starting position"
        activePly?.opening != null -> {
            val late = activePly.index > result.openingSummary.lastBookIndex && result.openingSummary.lastBookIndex >= 0
            "${activePly.opening.name} · ${activePly.opening.eco}" +
                if (late) " (book ends after move ${(result.openingSummary.lastBookIndex + 1) / 2 + 1})" else ""
        }
        result.openingSummary.entry != null && currentPly - 1 > result.openingSummary.lastBookIndex -> {
            val op = result.openingSummary.entry
            "${op.name} · ${op.eco} (book ends after move ${(result.openingSummary.lastBookIndex + 2) / 2})"
        }
        else -> ""
    }

    val topIsBlack = !flipped
    val topColor = if (topIsBlack) PieceColor.BLACK else PieceColor.WHITE
    val botColor = if (topIsBlack) PieceColor.WHITE else PieceColor.BLACK
    val topName = if (topIsBlack) result.summaryStats.blackName else result.summaryStats.whiteName
    val botName = if (topIsBlack) result.summaryStats.whiteName else result.summaryStats.blackName
    val topElo = (if (topIsBlack) result.summaryStats.blackElo else result.summaryStats.whiteElo)?.toString()
    val botElo = (if (topIsBlack) result.summaryStats.whiteElo else result.summaryStats.blackElo)?.toString()

    DiggyCard(
        elevated = false,
        borderColor = if (variation != null) StudioCyan.copy(alpha = 0.5f) else BorderSubtle,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PlayerBar(
                name = topName,
                pieceColor = topColor,
                isActiveTurn = turnColor == topColor,
                fen = displayFen,
                rating = topElo
            )

            if (opLabelText.isNotEmpty()) {
                Text(
                    text = opLabelText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            // Eval bar height-matched pixel-for-pixel to ChessBoardView
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val boardSize = (maxWidth - DiggySpacing.evalBarWidth - 8.dp).coerceAtLeast(200.dp)
                val curEval = variation?.liveEvalCp ?: result.evals.getOrElse(currentPly) { 0.0 }
                val curMate = variation?.liveEvalMate ?: activePly?.mate

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EvalBar(
                        evalPawns = curEval,
                        flipped = flipped,
                        mate = curMate,
                        modifier = Modifier.height(boardSize)
                    )
                    ChessBoardView(
                        fen = displayFen,
                        boardTheme = boardTheme,
                        flipped = flipped,
                        selectedSquare = selectedSq,
                        lastMoveSquares = highlightSquares,
                        legalTargets = legalTargets,
                        arrows = arrows,
                        moveBadge = moveBadge,
                        onSquareClick = onBoardSquareClick,
                        modifier = Modifier.size(boardSize)
                    )
                }
            }

            PlayerBar(
                name = botName,
                pieceColor = botColor,
                isActiveTurn = turnColor == botColor,
                fen = displayFen,
                rating = botElo
            )

            // Large 48dp Move Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val navModifier = Modifier
                    .weight(1f)
                    .height(DiggySpacing.touchTargetMin)
                OutlinedButton(
                    onClick = { onGoToPly(0, false) },
                    enabled = currentPly > 0,
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CardSurfaceElevated,
                        disabledContainerColor = SurfaceDark
                    ),
                    modifier = navModifier.testTag("nav_first")
                ) {
                    Icon(
                        Icons.Rounded.FirstPage,
                        contentDescription = "First move",
                        tint = if (currentPly > 0) TextPrimary else TextDisabled,
                        modifier = Modifier.size(24.dp)
                    )
                }
                OutlinedButton(
                    onClick = { onGoToPly(currentPly - 1, false) },
                    enabled = currentPly > 0,
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CardSurfaceElevated,
                        disabledContainerColor = SurfaceDark
                    ),
                    modifier = navModifier.testTag("nav_prev")
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous move",
                        tint = if (currentPly > 0) TextPrimary else TextDisabled,
                        modifier = Modifier.size(24.dp)
                    )
                }
                OutlinedButton(
                    onClick = { onGoToPly(currentPly + 1, false) },
                    enabled = currentPly < result.plies.size,
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CardSurfaceElevated,
                        disabledContainerColor = SurfaceDark
                    ),
                    modifier = navModifier.testTag("nav_next")
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Next move",
                        tint = if (currentPly < result.plies.size) TextPrimary else TextDisabled,
                        modifier = Modifier.size(24.dp)
                    )
                }
                OutlinedButton(
                    onClick = { onGoToPly(result.plies.size, false) },
                    enabled = currentPly < result.plies.size,
                    shape = RoundedCornerShape(DiggySpacing.controlRadius),
                    border = BorderStroke(1.dp, BorderSubtle),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CardSurfaceElevated,
                        disabledContainerColor = SurfaceDark
                    ),
                    modifier = navModifier.testTag("nav_last")
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.LastPage,
                        contentDescription = "Last move",
                        tint = if (currentPly < result.plies.size) TextPrimary else TextDisabled,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Coach Commentary & Move Classification Badges + AI Insight
            CoachCommentaryCard(
                activePly = activePly,
                currentPly = currentPly,
                isShowingBest = isShowingBest,
                variation = variation,
                onToggleBestMove = onToggleBestMove,
                onPracticeMistake = { idx -> onPracticeMistakes(idx) },
                moveInsightState = activePly?.let { moveInsightsByPly[it.index] } ?: AiInsightState.Idle,
                onRequestMoveInsight = onRequestMoveInsight,
                onUseOfflineMoveInsight = onUseOfflineMoveInsight
            )

            // Multi-PV Top 3 Engine Lines & Variation Explorer
            MultiPvLinesCard(
                lines = plyLiveLines,
                isLoading = isLoadingPlyLines,
                variation = variation,
                onPlayLineMove = onPlayEngineLineMove,
                onStepVariation = onStepVariation,
                onUndoVariation = onUndoVariation,
                onExitVariation = onExitVariation
            )

            // Flip, Share, Quick Board Themes (responsive for 360dp phones)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackThemes = maxWidth < 360.dp
                if (stackThemes) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DiggyChip(
                                label = "Flip",
                                selected = flipped,
                                onClick = onToggleFlip,
                                icon = Icons.Rounded.SwapVert,
                                testTag = "flip_board_button",
                                modifier = Modifier.weight(1f)
                            )
                            DiggyChip(
                                label = "Share",
                                selected = false,
                                onClick = onShareAnalysis,
                                icon = Icons.Rounded.Share,
                                testTag = "share_analysis_button",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Board style",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BoardTheme.entries.forEach { theme ->
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(theme.darkSquare)
                                            .border(
                                                width = if (theme == boardTheme) 2.5.dp else 1.dp,
                                                color = if (theme == boardTheme) AccentGreen else BorderSubtle,
                                                shape = CircleShape
                                            )
                                            .clickable { onBoardThemeChange(theme) }
                                            .testTag("theme_${theme.id}")
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DiggyChip(
                                label = "Flip",
                                selected = flipped,
                                onClick = onToggleFlip,
                                icon = Icons.Rounded.SwapVert,
                                testTag = "flip_board_button"
                            )
                            DiggyChip(
                                label = "Share",
                                selected = false,
                                onClick = onShareAnalysis,
                                icon = Icons.Rounded.Share,
                                testTag = "share_analysis_button"
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BoardTheme.entries.forEach { theme ->
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(theme.darkSquare)
                                        .border(
                                            width = if (theme == boardTheme) 2.5.dp else 1.dp,
                                            color = if (theme == boardTheme) AccentGreen else BorderSubtle,
                                            shape = CircleShape
                                        )
                                        .clickable { onBoardThemeChange(theme) }
                                        .testTag("theme_${theme.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val pendingPromo = variation?.pendingPromotion ?: boardPendingPromotion
    if (pendingPromo != null) {
        PromotionPickerDialog(
            color = turnColor,
            onSelect = onSelectVariationPromotion,
            onDismiss = onDismissVariationPromotion
        )
    }
}

private fun summaryClassIcon(cls: MoveClass): ImageVector = when (cls) {
    MoveClass.BRILLIANT -> Icons.Rounded.AutoAwesome
    MoveClass.BEST -> Icons.Rounded.Star
    MoveClass.EXCELLENT -> Icons.Rounded.DoneAll
    MoveClass.GOOD -> Icons.Rounded.CheckCircle
    MoveClass.INACCURACY -> Icons.Rounded.ReportProblem
    MoveClass.MISTAKE -> Icons.Rounded.ErrorOutline
    MoveClass.BLUNDER -> Icons.Rounded.Dangerous
}

@Composable
private fun AnalysisRightCards(
    result: GameAnalysisResult,
    currentPly: Int,
    gameReportState: AiInsightState<AiGameSummaryReport>,
    coachChatMessages: List<CoachChatMessage>,
    coachChatStreamState: CoachChatStreamState,
    dailyAiRemaining: Int,
    onGoToPly: (Int, Boolean) -> Unit,
    onPracticeMistakes: (Int?) -> Unit,
    onRequestGameReport: () -> Unit,
    onUseOfflineGameReport: () -> Unit,
    onSendCoachMessage: (String) -> Unit
) {
    val criticalPlies = remember(result) {
        result.plies
            .filter {
                it.moveClass == MoveClass.MISTAKE ||
                    it.moveClass == MoveClass.BLUNDER ||
                    it.moveClass == MoveClass.BRILLIANT ||
                    it.isGreatMove
            }
            .associate { (it.index + 1) to it.badgeColor }
    }

    val hasMistakesToPractice = remember(result) {
        result.plies.any {
            (it.moveClass == MoveClass.INACCURACY ||
                it.moveClass == MoveClass.MISTAKE ||
                it.moveClass == MoveClass.BLUNDER) &&
                it.bestFrom != null && it.bestTo != null
        }
    }

    // Evaluation Graph Card
    DiggyCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Evaluation Graph",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            EvalGraph(
                evals = result.evals,
                currentPly = currentPly,
                onSelectPly = { onGoToPly(it, false) },
                criticalPlies = criticalPlies
            )
            Text(
                text = "Tap any point on the chart to jump directly to that move.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }

    // Opening Summary Card
    val op = result.openingSummary
    DiggyCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Opening",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
                op.entry?.let {
                    Surface(
                        color = AccentGreen,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = it.eco,
                            color = OnAccentDark,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = op.entry?.name ?: "Opening not identified",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            val bookLine = listOf(op.matchedText, op.nextMoveNote).filter { it.isNotEmpty() }.joinToString(" ")
            if (bookLine.isNotEmpty()) {
                Text(bookLine, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            HorizontalDivider(color = LineSubtle)
            Text("Engine Perspective", style = MaterialTheme.typography.labelMedium, color = StudioCyan)
            Text(op.engineFirstNote, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(op.engineEvalNote, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            HorizontalDivider(color = LineSubtle)
            Text(op.phaseHeading, style = MaterialTheme.typography.labelMedium, color = AccentGreen)
            Text("White: ${op.whiteObs}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text("Black: ${op.blackObs}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
    }

    // Accuracy & Game Summary Card
    val st = result.summaryStats
    val classRows = remember(result) {
        data class ClassSummaryRow(val label: String, val icon: ImageVector, val color: Color, val wCount: Int, val bCount: Int)
        val wMap = mutableMapOf<String, Int>()
        val bMap = mutableMapOf<String, Int>()
        result.plies.forEach { ply ->
            val target = if (ply.index % 2 == 0) wMap else bMap
            target[ply.badgeLabel] = (target[ply.badgeLabel] ?: 0) + 1
        }
        listOf(
            ClassSummaryRow("Brilliant", Icons.Rounded.AutoAwesome, ClassBrilliant, wMap["Brilliant"] ?: 0, bMap["Brilliant"] ?: 0),
            ClassSummaryRow("Great", Icons.Rounded.Bolt, ClassGreat, wMap["Great"] ?: 0, bMap["Great"] ?: 0),
            ClassSummaryRow("Best", Icons.Rounded.Star, ClassBest, wMap["Best"] ?: 0, bMap["Best"] ?: 0),
            ClassSummaryRow("Excellent", Icons.Rounded.DoneAll, ClassExcellent, wMap["Excellent"] ?: 0, bMap["Excellent"] ?: 0),
            ClassSummaryRow("Good", Icons.Rounded.CheckCircle, ClassGood, wMap["Good"] ?: 0, bMap["Good"] ?: 0),
            ClassSummaryRow("Book", Icons.Rounded.MenuBook, ClassBook, wMap["Book"] ?: 0, bMap["Book"] ?: 0),
            ClassSummaryRow("Inaccuracy", Icons.Rounded.ReportProblem, ClassInaccuracy, wMap["Inaccuracy"] ?: 0, bMap["Inaccuracy"] ?: 0),
            ClassSummaryRow("Mistake", Icons.Rounded.ErrorOutline, ClassMistake, wMap["Mistake"] ?: 0, bMap["Mistake"] ?: 0),
            ClassSummaryRow("Miss", Icons.Rounded.HighlightOff, ClassMiss, wMap["Miss"] ?: 0, bMap["Miss"] ?: 0),
            ClassSummaryRow("Blunder", Icons.Rounded.Dangerous, ClassBlunder, wMap["Blunder"] ?: 0, bMap["Blunder"] ?: 0)
        )
    }
    DiggyCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Accuracy & Summary", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            SummaryRow("", "White", "Black", isHeader = true)
            SummaryRow(
                label = "Accuracy",
                whiteVal = String.format(Locale.US, "%.1f%%", st.whiteAccuracy),
                blackVal = String.format(Locale.US, "%.1f%%", st.blackAccuracy),
                boldValues = true
            )
            classRows.forEach { row ->
                SummaryRow(
                    label = row.label,
                    whiteVal = row.wCount.toString(),
                    blackVal = row.bCount.toString(),
                    icon = row.icon,
                    badgeColor = row.color
                )
            }
            SummaryRow("Opening", st.openingStats.whiteAccuracy, st.openingStats.blackAccuracy)
            SummaryRow("Middlegame", st.middlegameStats.whiteAccuracy, st.middlegameStats.blackAccuracy)
            SummaryRow("Endgame", st.endgameStats.whiteAccuracy, st.endgameStats.blackAccuracy)
        }
    }

    // Key Mistakes & Practice CTA Card
    DiggyCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Key Mistakes", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                if (hasMistakesToPractice) {
                    DiggyChip(
                        label = "Practice All",
                        selected = true,
                        onClick = { onPracticeMistakes(null) },
                        icon = Icons.Rounded.FitnessCenter,
                        accentColor = AccentGreen,
                        testTag = "practice_all_mistakes_button"
                    )
                }
            }

            if (result.keyMistakes.isEmpty()) {
                Text(
                    text = "No major mistakes detected. Clean game!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            } else {
                result.keyMistakes.forEach { p ->
                    val moveLabel = "${p.index / 2 + 1}${if (p.index % 2 == 1) "..." else "."} ${p.san}"
                    Surface(
                        onClick = { onGoToPly(p.index + 1, true) },
                        color = CardSurfaceElevated,
                        shape = RoundedCornerShape(DiggySpacing.chipRadius),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(p.badgeColor.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = summaryClassIcon(p.moveClass),
                                    contentDescription = p.badgeLabel,
                                    tint = p.badgeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = moveLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${p.badgeLabel}${p.bestSan?.let { " · Best $it" } ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Jump to mistake",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // AI Game Coaching Report Card
    AiGameCoachingReportCard(
        reportState = gameReportState,
        dailyAiRemaining = dailyAiRemaining,
        onRefreshReport = onRequestGameReport,
        onUseOfflineReport = onUseOfflineGameReport
    )

    // Interactive AI Chess Coach Q&A Card
    AskAiCoachChatCard(
        messages = coachChatMessages,
        streamState = coachChatStreamState,
        currentPly = currentPly,
        onSendQuestion = onSendCoachMessage
    )

    // Move List Card
    DiggyCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Moves", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            AnalyzedMoveList(
                plies = result.plies,
                currentPly = currentPly,
                onSelectPly = { ply -> onGoToPly(ply, false) }
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    whiteVal: String,
    blackVal: String,
    isHeader: Boolean = false,
    boldValues: Boolean = false,
    icon: ImageVector? = null,
    badgeColor: Color? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1.35f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null && badgeColor != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isHeader) TextMuted else TextPrimary
                )
            }
            Text(
                text = whiteVal,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = if (isHeader) PlusJakartaSansFontFamily else JetBrainsMonoFontFamily,
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = if (isHeader || boldValues) FontWeight.Bold else FontWeight.Medium,
                color = if (boldValues) AccentGreen else TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = blackVal,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = if (isHeader) PlusJakartaSansFontFamily else JetBrainsMonoFontFamily,
                    fontFeatureSettings = "tnum"
                ),
                fontWeight = if (isHeader || boldValues) FontWeight.Bold else FontWeight.Medium,
                color = if (boldValues) AccentGreen else TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = LineSubtle, thickness = 0.5.dp)
    }
}
