package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType

private val WhiteFill = Color(0xFFF8F4E9)
private val WhiteStroke = Color(0xFF1D1D1D)
private val BlackFill = Color(0xFF2E3136)
private val BlackStroke = Color(0xFF0A0A0A)
private val BlackDetail = Color(0xFFDADAD2)

@Composable
fun ChessPieceIcon(
    piece: ChessPiece,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val s = minOf(size.width, size.height) / 100f
        val fill = if (piece.color == PieceColor.WHITE) WhiteFill else BlackFill
        val strokeColor = if (piece.color == PieceColor.WHITE) WhiteStroke else BlackStroke
        val detailColor = if (piece.color == PieceColor.WHITE) WhiteStroke else BlackDetail
        val stroke = Stroke(width = 3.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val detailStroke = Stroke(width = 3.0f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        scale(scale = s, pivot = Offset.Zero) {
            when (piece.type) {
                PieceType.PAWN -> drawPawn(fill, strokeColor, stroke)
                PieceType.ROOK -> drawRook(fill, strokeColor, detailColor, stroke, detailStroke)
                PieceType.KNIGHT -> drawKnight(fill, strokeColor, detailColor, stroke, detailStroke)
                PieceType.BISHOP -> drawBishop(fill, strokeColor, detailColor, stroke, detailStroke)
                PieceType.QUEEN -> drawQueen(fill, strokeColor, detailColor, stroke, detailStroke)
                PieceType.KING -> drawKing(fill, strokeColor, detailColor, stroke, detailStroke)
            }
        }
    }
}

private fun DrawScope.drawPawn(fill: Color, strokeColor: Color, stroke: Stroke) {
    val body = Path().apply {
        moveTo(26f, 84f)
        lineTo(74f, 84f)
        cubicTo(74f, 67f, 63f, 57f, 56f, 53f)
        cubicTo(61f, 49f, 64f, 44f, 64f, 38f)
        cubicTo(64f, 31f, 59f, 26f, 55f, 24f)
        cubicTo(58f, 21f, 59f, 17f, 59f, 14f)
        cubicTo(59f, 9f, 55f, 6f, 50f, 6f)
        cubicTo(45f, 6f, 41f, 9f, 41f, 14f)
        cubicTo(41f, 17f, 42f, 21f, 45f, 24f)
        cubicTo(41f, 26f, 36f, 31f, 36f, 38f)
        cubicTo(36f, 44f, 39f, 49f, 44f, 53f)
        cubicTo(37f, 57f, 26f, 67f, 26f, 84f)
        close()
    }
    drawPath(body, fill, style = Fill)
    drawPath(body, strokeColor, style = stroke)
    drawRoundRect(
        color = fill,
        topLeft = Offset(22f, 82f),
        size = Size(56f, 8f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = strokeColor,
        topLeft = Offset(22f, 82f),
        size = Size(56f, 8f),
        cornerRadius = CornerRadius(3f, 3f),
        style = stroke
    )
}

private fun DrawScope.drawRook(
    fill: Color,
    strokeColor: Color,
    detailColor: Color,
    stroke: Stroke,
    detailStroke: Stroke
) {
    val body = Path().apply {
        // Battlements
        moveTo(24f, 16f)
        lineTo(35f, 16f)
        lineTo(35f, 24f)
        lineTo(44f, 24f)
        lineTo(44f, 16f)
        lineTo(56f, 16f)
        lineTo(56f, 24f)
        lineTo(65f, 24f)
        lineTo(65f, 16f)
        lineTo(76f, 16f)
        lineTo(76f, 31f)
        lineTo(69f, 38f)
        lineTo(69f, 69f)
        lineTo(76f, 77f)
        lineTo(76f, 88f)
        lineTo(24f, 88f)
        lineTo(24f, 77f)
        lineTo(31f, 69f)
        lineTo(31f, 38f)
        lineTo(24f, 31f)
        close()
    }
    drawPath(body, fill, style = Fill)
    drawPath(body, strokeColor, style = stroke)
    drawLine(detailColor, Offset(31f, 38f), Offset(69f, 38f), strokeWidth = detailStroke.width)
    drawLine(detailColor, Offset(31f, 69f), Offset(69f, 69f), strokeWidth = detailStroke.width)
    drawLine(detailColor, Offset(24f, 77f), Offset(76f, 77f), strokeWidth = detailStroke.width)
}

private fun DrawScope.drawBishop(
    fill: Color,
    strokeColor: Color,
    detailColor: Color,
    stroke: Stroke,
    detailStroke: Stroke
) {
    val base = Path().apply {
        moveTo(18f, 86f)
        cubicTo(30f, 80f, 44f, 86f, 50f, 80f)
        cubicTo(56f, 86f, 70f, 80f, 82f, 86f)
        lineTo(78f, 92f)
        cubicTo(68f, 86f, 56f, 92f, 50f, 88f)
        cubicTo(44f, 92f, 32f, 86f, 22f, 92f)
        close()
    }
    val mitre = Path().apply {
        moveTo(31f, 75f)
        cubicTo(36f, 81f, 64f, 81f, 69f, 75f)
        cubicTo(71f, 69f, 66f, 61f, 60f, 57f)
        cubicTo(74f, 51f, 73f, 27f, 50f, 18f)
        cubicTo(27f, 27f, 26f, 51f, 40f, 57f)
        cubicTo(34f, 61f, 29f, 69f, 31f, 75f)
        close()
    }
    drawPath(mitre, fill, style = Fill)
    drawPath(mitre, strokeColor, style = stroke)
    drawCircle(fill, radius = 6.5f, center = Offset(50f, 12f))
    drawCircle(strokeColor, radius = 6.5f, center = Offset(50f, 12f), style = stroke)
    drawPath(base, fill, style = Fill)
    drawPath(base, strokeColor, style = stroke)

    // Cross on mitre + collar lines
    drawLine(detailColor, Offset(50f, 31f), Offset(50f, 47f), strokeWidth = 3.2f, cap = StrokeCap.Round)
    drawLine(detailColor, Offset(42f, 39f), Offset(58f, 39f), strokeWidth = 3.2f, cap = StrokeCap.Round)
    drawLine(detailColor, Offset(38f, 59f), Offset(62f, 59f), strokeWidth = detailStroke.width, cap = StrokeCap.Round)
    drawLine(detailColor, Offset(33f, 69f), Offset(67f, 69f), strokeWidth = detailStroke.width, cap = StrokeCap.Round)
}

private fun DrawScope.drawKnight(
    fill: Color,
    strokeColor: Color,
    detailColor: Color,
    stroke: Stroke,
    detailStroke: Stroke
) {
    val horse = Path().apply {
        moveTo(24f, 88f)
        lineTo(80f, 88f)
        cubicTo(81f, 48f, 70f, 24f, 49f, 18f)
        lineTo(44f, 9f)
        lineTo(37f, 18f)
        cubicTo(30f, 22f, 22f, 33f, 18f, 42f)
        cubicTo(14f, 49f, 14f, 58f, 21f, 60f)
        cubicTo(25f, 61f, 31f, 57f, 36f, 51f)
        lineTo(43f, 45f)
        cubicTo(45f, 59f, 24f, 65f, 24f, 88f)
        close()
    }
    drawPath(horse, fill, style = Fill)
    drawPath(horse, strokeColor, style = stroke)

    // Mane accent
    val mane = Path().apply {
        moveTo(54f, 24f)
        cubicTo(65f, 33f, 72f, 53f, 70f, 82f)
    }
    drawPath(mane, detailColor, style = detailStroke)
    // Eye
    drawCircle(detailColor, radius = 2.8f, center = Offset(33f, 33f))
}

private fun DrawScope.drawQueen(
    fill: Color,
    strokeColor: Color,
    detailColor: Color,
    stroke: Stroke,
    detailStroke: Stroke
) {
    val crown = Path().apply {
        moveTo(22f, 62f)
        lineTo(14f, 29f)
        lineTo(31f, 52f)
        lineTo(33f, 21f)
        lineTo(44f, 50f)
        lineTo(50f, 18f)
        lineTo(56f, 50f)
        lineTo(67f, 21f)
        lineTo(69f, 52f)
        lineTo(86f, 29f)
        lineTo(78f, 62f)
        cubicTo(64f, 58f, 36f, 58f, 22f, 62f)
        close()
    }
    val base = Path().apply {
        moveTo(22f, 62f)
        cubicTo(22f, 69f, 27f, 72f, 26f, 79f)
        lineTo(24f, 88f)
        cubicTo(40f, 92f, 60f, 92f, 76f, 88f)
        lineTo(74f, 79f)
        cubicTo(73f, 72f, 78f, 69f, 78f, 62f)
        cubicTo(64f, 58f, 36f, 58f, 22f, 62f)
        close()
    }
    drawPath(crown, fill, style = Fill)
    drawPath(crown, strokeColor, style = stroke)
    drawPath(base, fill, style = Fill)
    drawPath(base, strokeColor, style = stroke)

    // Crown tips
    val tips = listOf(
        Offset(13f, 27f), Offset(33f, 19f), Offset(50f, 15f),
        Offset(67f, 19f), Offset(87f, 27f)
    )
    for (pt in tips) {
        drawCircle(fill, radius = 4.2f, center = pt)
        drawCircle(strokeColor, radius = 4.2f, center = pt, style = Stroke(width = 2.6f))
    }
    drawLine(detailColor, Offset(26f, 72f), Offset(74f, 72f), strokeWidth = detailStroke.width)
    drawLine(detailColor, Offset(25f, 81f), Offset(75f, 81f), strokeWidth = detailStroke.width)
}

private fun DrawScope.drawKing(
    fill: Color,
    strokeColor: Color,
    detailColor: Color,
    stroke: Stroke,
    detailStroke: Stroke
) {
    // Cross
    drawLine(strokeColor, Offset(50f, 7f), Offset(50f, 26f), strokeWidth = 5.5f, cap = StrokeCap.Round)
    drawLine(strokeColor, Offset(42f, 14f), Offset(58f, 14f), strokeWidth = 5.5f, cap = StrokeCap.Round)
    drawLine(fill, Offset(50f, 8.5f), Offset(50f, 24f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    drawLine(fill, Offset(43.5f, 14f), Offset(56.5f, 14f), strokeWidth = 2.5f, cap = StrokeCap.Round)

    val arches = Path().apply {
        moveTo(25f, 68f)
        cubicTo(10f, 55f, 13f, 34f, 30f, 32f)
        cubicTo(40f, 31f, 47f, 40f, 50f, 49f)
        cubicTo(53f, 40f, 60f, 31f, 70f, 32f)
        cubicTo(87f, 34f, 90f, 55f, 75f, 68f)
        lineTo(75f, 86f)
        cubicTo(60f, 93f, 40f, 93f, 25f, 86f)
        close()
    }
    val centerMitre = Path().apply {
        moveTo(50f, 58f)
        cubicTo(61f, 42f, 58f, 25f, 50f, 25f)
        cubicTo(42f, 25f, 39f, 42f, 50f, 58f)
        close()
    }
    drawPath(arches, fill, style = Fill)
    drawPath(arches, strokeColor, style = stroke)
    drawPath(centerMitre, fill, style = Fill)
    drawPath(centerMitre, strokeColor, style = stroke)

    // Base bands
    drawLine(detailColor, Offset(25f, 70f), Offset(75f, 70f), strokeWidth = detailStroke.width)
    drawLine(detailColor, Offset(25f, 78f), Offset(75f, 78f), strokeWidth = detailStroke.width)
    drawLine(detailColor, Offset(25f, 86f), Offset(75f, 86f), strokeWidth = detailStroke.width)
}
