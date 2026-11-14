package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.*
import com.example.ui.components.BoardArrow
import com.example.ui.components.ChessBoardView
import com.example.ui.components.EvalBar
import com.example.ui.components.EvalGraph
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun AnalyzeWorkspace(
    result: GameAnalysisResult,
    currentPly: Int,
    showBestMove: Boolean,
    flipped: Boolean,
    boardTheme: BoardTheme,
    onGoToPly: (Int, Boolean) -> Unit,
    onToggleBestMove: () -> Unit,
    onToggleFlip: () -> Unit,
    onShareAnalysis: () -> Unit,
    onBoardThemeChange: (BoardTheme) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 820.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(modifier = Modifier.weight(1.05f)) {
                    BoardAndMoveCard(
                        result = result,
                        currentPly = currentPly,
                        showBestMove = showBestMove,
                        flipped = flipped,
                        boardTheme = boardTheme,
                        onGoToPly = onGoToPly,
                        onToggleBestMove = onToggleBestMove,
                        onToggleFlip = onToggleFlip,
                        onShareAnalysis = onShareAnalysis,
                        onBoardThemeChange = onBoardThemeChange
                    )
                }
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AnalysisRightCards(
                        result = result,
                        currentPly = currentPly,
                        onGoToPly = onGoToPly
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BoardAndMoveCard(
                    result = result,
                    currentPly = currentPly,
                    showBestMove = showBestMove,
                    flipped = flipped,
                    boardTheme = boardTheme,
                    onGoToPly = onGoToPly,
                    onToggleBestMove = onToggleBestMove,
                    onToggleFlip = onToggleFlip,
                    onShareAnalysis = onShareAnalysis,
                    onBoardThemeChange = onBoardThemeChange
                )
                AnalysisRightCards(
                    result = result,
                    currentPly = currentPly,
                    onGoToPly = onGoToPly
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
    onGoToPly: (Int, Boolean) -> Unit,
    onToggleBestMove: () -> Unit,
    onToggleFlip: () -> Unit,
    onShareAnalysis: () -> Unit,
    onBoardThemeChange: (BoardTheme) -> Unit
) {
    val activePly = if (currentPly > 0) result.plies.getOrNull(currentPly - 1) else null
    val isShowingBest = showBestMove && activePly?.bestFrom != null && activePly.bestTo != null
    val displayFen = when {
        activePly == null -> result.headers["FEN"]?.takeIf { it.isNotBlank() } ?: ChessBoard.START_FEN
        isShowingBest -> activePly.fenBefore
        else -> activePly.fenAfter
    }
    val highlightSquares = if (activePly != null && !isShowingBest) {
        listOf(activePly.move.fromName, activePly.move.toName)
    } else emptyList()

    val arrows = if (isShowingBest && activePly != null && activePly.bestFrom != null && activePly.bestTo != null) {
        listOf(
            BoardArrow(activePly.bestFrom, activePly.bestTo, ArrowBestGreen),
            BoardArrow(activePly.move.fromName, activePly.move.toName, ArrowPlayedRed)
        )
    } else emptyList()

    val opLabelText = when {
        result.plies.isEmpty() -> ""
        currentPly == 0 -> "Starting position"
        activePly?.opening != null -> {
            val late = activePly.index > result.openingSummary.lastBookIndex && result.openingSummary.lastBookIndex >= 0
            "${activePly.opening.name} · ${activePly.opening.eco}" +
                if (late) " (list ends after move ${(result.openingSummary.lastBookIndex + 1) / 2 + 1})" else ""
        }
        result.openingSummary.entry != null && currentPly - 1 > result.openingSummary.lastBookIndex -> {
            val op = result.openingSummary.entry
            "${op.name} · ${op.eco} (list ends after move ${(result.openingSummary.lastBookIndex + 2) / 2})"
        }
        else -> ""
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, LineSubtle),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Player Names Header
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚪ ${result.summaryStats.whiteName}" +
                            (result.summaryStats.whiteElo?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚫ ${result.summaryStats.blackName}" +
                            (result.summaryStats.blackElo?.let { " ($it)" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (opLabelText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = opLabelText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Eval bar + Board
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val curEval = result.evals.getOrElse(currentPly) { 0.0 }
                EvalBar(
                    evalPawns = curEval,
                    flipped = flipped,
                    modifier = Modifier
                        .height(260.dp)
                        .fillMaxHeight()
                )
                ChessBoardView(
                    fen = displayFen,
                    boardTheme = boardTheme,
                    flipped = flipped,
                    lastMoveSquares = highlightSquares,
                    arrows = arrows,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Move Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val navModifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                OutlinedButton(
                    onClick = { onGoToPly(0, false) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = navModifier.testTag("nav_first")
                ) {
                    Icon(Icons.Filled.FirstPage, contentDescription = "First move", tint = TextPrimary)
                }
                OutlinedButton(
                    onClick = { onGoToPly(currentPly - 1, false) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = navModifier.testTag("nav_prev")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous move", tint = TextPrimary)
                }
                OutlinedButton(
                    onClick = { onGoToPly(currentPly + 1, false) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = navModifier.testTag("nav_next")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next move", tint = TextPrimary)
                }
                OutlinedButton(
                    onClick = { onGoToPly(result.plies.size, false) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = navModifier.testTag("nav_last")
                ) {
                    Icon(Icons.Filled.LastPage, contentDescription = "Last move", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Move Explanation Box
            Surface(
                color = SurfaceDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (activePly == null) {
                        Text(
                            text = "Start of game. Press ▶ or pick a move from the list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    } else {
                        val moveNum = (currentPly - 1) / 2 + 1
                        val dots = if (currentPly % 2 == 1) ". " else "... "
                        val badgeLabel = when {
                            activePly.isBook -> "Book"
                            activePly.moveClass == MoveClass.BEST && activePly.bestSan != null && activePly.bestSan != activePly.san -> "Excellent"
                            else -> activePly.moveClass.label
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$moveNum$dots${activePly.san}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Surface(
                                color = activePly.moveClass.color,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = badgeLabel,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "eval ${MoveExplainer.formatPlyEval(activePly.evalAfter, activePly.mate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isShowingBest) {
                            Text(
                                text = "Green arrow = best move ${activePly.bestSan}, red arrow = move played.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentGreenHover
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = activePly.bestExplanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        } else {
                            Text(
                                text = activePly.explanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }

                        if (activePly.bestFrom != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onToggleBestMove,
                                border = BorderStroke(1.dp, AccentGreen),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.testTag("toggle_best_move")
                            ) {
                                Text(
                                    text = if (isShowingBest) "Back" else "Show best move",
                                    color = AccentGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Flip, Share, Board Themes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onToggleFlip,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.testTag("flip_board_button")
                    ) {
                        Icon(Icons.Filled.Flip, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Flip", color = AccentGreen)
                    }
                    OutlinedButton(
                        onClick = onShareAnalysis,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.testTag("share_analysis_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", color = AccentGreen)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Board", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    BoardTheme.entries.forEach { theme ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
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

@Composable
private fun AnalysisRightCards(
    result: GameAnalysisResult,
    currentPly: Int,
    onGoToPly: (Int, Boolean) -> Unit
) {
    // Evaluation Graph Card
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, LineSubtle),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("EVALUATION", style = MaterialTheme.typography.titleSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            EvalGraph(
                evals = result.evals,
                currentPly = currentPly,
                onSelectPly = { onGoToPly(it, false) }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap the graph to jump to a move. Above the line favours White.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }

    // Opening Summary Card
    val op = result.openingSummary
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, LineSubtle),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("OPENING", style = MaterialTheme.typography.titleSmall, color = TextMuted)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = op.entry?.name ?: "Opening not identified",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f, fill = false)
                )
                op.entry?.let {
                    Surface(color = AccentGreen, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = it.eco,
                            color = OnAccentDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = listOf(op.matchedText, op.nextMoveNote).filter { it.isNotEmpty() }.joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text("ENGINE VIEW", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(op.engineFirstNote, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text(op.engineEvalNote, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(op.phaseHeading.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("White: ${op.whiteObs}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            Text("Black: ${op.blackObs}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
    }

    // Game Summary Table
    val st = result.summaryStats
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, LineSubtle),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("GAME SUMMARY", style = MaterialTheme.typography.titleSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            SummaryRow("", "White", "Black", isHeader = true)
            SummaryRow(
                "Accuracy",
                String.format(Locale.US, "%.1f%%", st.whiteAccuracy),
                String.format(Locale.US, "%.1f%%", st.blackAccuracy),
                boldValues = true
            )
            MoveClass.entries.forEach { cls ->
                SummaryRow(
                    label = cls.label,
                    whiteVal = (st.whiteCounts[cls] ?: 0).toString(),
                    blackVal = (st.blackCounts[cls] ?: 0).toString(),
                    dotColor = cls.color
                )
            }
            SummaryRow("Opening", st.openingStats.whiteAccuracy, st.openingStats.blackAccuracy)
            SummaryRow("Middlegame", st.middlegameStats.whiteAccuracy, st.middlegameStats.blackAccuracy)
            SummaryRow("Endgame", st.endgameStats.whiteAccuracy, st.endgameStats.blackAccuracy)
        }
    }

    // Key Mistakes Card
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, LineSubtle),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("KEY MISTAKES", style = MaterialTheme.typography.titleSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            if (result.keyMistakes.isEmpty()) {
                Text(
                    text = "No big mistakes. Well played!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            } else {
                result.keyMistakes.forEach { p ->
                    val moveLabel = "${p.index / 2 + 1}${if (p.index % 2 == 1) "..." else "."} ${p.san}"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGoToPly(p.index + 1, true) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(p.moveClass.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = moveLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${p.moveClass.label}${p.bestSan?.let { ", best was $it" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                }
            }
        }
    }

    // Move List Card
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, LineSubtle),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("MOVES", style = MaterialTheme.typography.titleSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            val pairCount = (result.plies.size + 1) / 2
            for (pairIdx in 0 until pairCount) {
                val wPly = result.plies.getOrNull(pairIdx * 2)
                val bPly = result.plies.getOrNull(pairIdx * 2 + 1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${pairIdx + 1}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.width(30.dp)
                    )
                    if (wPly != null) {
                        MoveListChip(
                            ply = wPly,
                            isSelected = currentPly == wPly.index + 1,
                            onClick = { onGoToPly(wPly.index + 1, false) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (bPly != null) {
                        MoveListChip(
                            ply = bPly,
                            isSelected = currentPly == bPly.index + 1,
                            onClick = { onGoToPly(bPly.index + 1, false) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
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
    dotColor: Color? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1.3f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                    color = TextPrimary
                )
            }
            Text(
                text = whiteVal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isHeader || boldValues) FontWeight.Bold else FontWeight.Normal,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = blackVal,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isHeader || boldValues) FontWeight.Bold else FontWeight.Normal,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
    }
}

@Composable
private fun MoveListChip(
    ply: AnalyzedPly,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) SurfaceDark else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) AccentGreen else Color.Transparent),
        modifier = modifier
            .heightIn(min = 38.dp)
            .testTag("move_ply_${ply.index + 1}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ply.moveClass.color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = ply.san,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
            Text(
                text = MoveExplainer.formatPlyEval(ply.evalAfter, ply.mate),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
