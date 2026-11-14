package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chess.*
import com.example.ui.theme.*
import kotlin.math.hypot

data class BoardArrow(
    val from: String,
    val to: String,
    val color: Color
)

@Composable
fun ChessBoardView(
    fen: String,
    boardTheme: BoardTheme,
    flipped: Boolean = false,
    selectedSquare: String? = null,
    lastMoveSquares: List<String> = emptyList(),
    wrongMoveSquares: List<String> = emptyList(),
    hintSquares: List<String> = emptyList(),
    legalTargets: Map<String, Boolean> = emptyMap(), // square -> isCapture
    arrows: List<BoardArrow> = emptyList(),
    onSquareClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val chess = ChessBoard(fen)
    val checkSquare = if (chess.inCheck()) {
        val ks = chess.kingSquare(chess.turn)
        if (ks >= 0) squareName(ks) else null
    } else null

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
    ) {
        val boardSize = maxWidth
        Column(modifier = Modifier.fillMaxSize()) {
            for (dr in 0 until 8) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (df in 0 until 8) {
                        val r = if (flipped) 7 - dr else dr
                        val f = if (flipped) 7 - df else df
                        val sqIdx = r * 8 + f
                        val sqName = squareName(sqIdx)
                        val isLight = (r + f) % 2 == 0
                        val baseColor = if (isLight) boardTheme.lightSquare else boardTheme.darkSquare
                        val labelColor = if (isLight) boardTheme.darkSquare else boardTheme.lightSquare
                        val piece = chess.board[sqIdx]
                        val isSelected = sqName == selectedSquare
                        val isLast = sqName in lastMoveSquares
                        val isWrong = sqName in wrongMoveSquares
                        val isHint = sqName in hintSquares
                        val isCheck = sqName == checkSquare
                        val captureTarget = legalTargets[sqName]

                        val squareDesc = buildString {
                            append(sqName)
                            if (piece != null) {
                                append(" ${piece.color.name.lowercase()} ${piece.type.pieceName}")
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isSelected) HighlightSelected else baseColor)
                                .then(
                                    if (onSquareClick != null) {
                                        Modifier
                                            .clickable { onSquareClick(sqName) }
                                            .testTag("sq_$sqName")
                                    } else Modifier
                                )
                                .semantics { contentDescription = squareDesc },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLast && !isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(HighlightLastMove)
                                )
                            }
                            if (isWrong) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(HighlightWrong)
                                )
                            }
                            if (isHint) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(HighlightHint)
                                )
                            }
                            if (isCheck) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(CheckGlow, Color.Transparent)
                                            )
                                        )
                                )
                            }

                            // Coordinate labels
                            if (df == 7) {
                                Text(
                                    text = "${8 - r}",
                                    color = labelColor.copy(alpha = 0.9f),
                                    fontSize = if (boardSize < 260.dp) 8.sp else 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 1.dp, end = 3.dp)
                                )
                            }
                            if (dr == 7) {
                                Text(
                                    text = "${'a' + f}",
                                    color = labelColor.copy(alpha = 0.9f),
                                    fontSize = if (boardSize < 260.dp) 8.sp else 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 3.dp, bottom = 1.dp)
                                )
                            }

                            // Piece
                            if (piece != null) {
                                ChessPieceIcon(
                                    piece = piece,
                                    modifier = Modifier.fillMaxSize(0.88f)
                                )
                            }

                            // Legal move indicator
                            if (captureTarget != null) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (captureTarget) {
                                        drawCircle(
                                            color = Color(0x59141E0A),
                                            radius = size.minDimension * 0.44f,
                                            style = Stroke(width = size.minDimension * 0.09f)
                                        )
                                    } else {
                                        drawCircle(
                                            color = Color(0x59141E0A),
                                            radius = size.minDimension * 0.16f
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Arrow overlay
        if (arrows.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cell = size.width / 8f
                for (arrow in arrows) {
                    val fromSq = parseSquare(arrow.from) ?: continue
                    val toSq = parseSquare(arrow.to) ?: continue
                    val fr = fromSq / 8
                    val ff = fromSq % 8
                    val tr = toSq / 8
                    val tf = toSq % 8
                    val x1 = ((if (flipped) 7 - ff else ff) + 0.5f) * cell
                    val y1 = ((if (flipped) 7 - fr else fr) + 0.5f) * cell
                    val x2 = ((if (flipped) 7 - tf else tf) + 0.5f) * cell
                    val y2 = ((if (flipped) 7 - tr else tr) + 0.5f) * cell

                    val dx = x2 - x1
                    val dy = y2 - y1
                    val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    if (len < 1f) continue
                    val ux = dx / len
                    val uy = dy / len
                    val headLen = cell * 0.42f
                    val bx = x2 - ux * headLen
                    val by = y2 - uy * headLen

                    drawLine(
                        color = arrow.color,
                        start = Offset(x1, y1),
                        end = Offset(bx, by),
                        strokeWidth = cell * 0.17f,
                        cap = StrokeCap.Round
                    )

                    val headWidth = cell * 0.24f
                    val path = Path().apply {
                        moveTo(x2, y2)
                        lineTo(bx - uy * headWidth, by + ux * headWidth)
                        lineTo(bx + uy * headWidth, by - ux * headWidth)
                        close()
                    }
                    drawPath(path, arrow.color)
                }
            }
        }
    }
}

@Composable
fun PromotionPickerDialog(
    color: PieceColor,
    onSelect: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CardDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Promote pawn",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        PieceType.QUEEN,
                        PieceType.ROOK,
                        PieceType.BISHOP,
                        PieceType.KNIGHT
                    ).forEach { pt ->
                        Surface(
                            onClick = { onSelect(pt) },
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceDark,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("promote_${pt.symbol}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                                ChessPieceIcon(
                                    piece = ChessPiece(color, pt),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
