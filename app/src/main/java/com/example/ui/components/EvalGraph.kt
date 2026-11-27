package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun EvalGraph(
    evals: List<Double>,
    currentPly: Int,
    onSelectPly: (Int) -> Unit,
    criticalPlies: Map<Int, Color> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val totalPlies = (evals.size - 1).coerceAtLeast(1)
    val mutedColor = TextMuted
    val lineColor = StudioCyan
    val emeraldColor = AccentGreen

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(DiggySpacing.controlRadius))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(DiggySpacing.controlRadius))
            .testTag("eval_graph")
            .pointerInput(totalPlies) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSelectPly((frac * totalPlies).roundToInt())
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        fun yForEval(e: Double): Float {
            val clamped = e.coerceIn(-5.0, 5.0).toFloat()
            return midY - (clamped / 5f) * (midY * 0.88f)
        }

        drawLine(
            color = mutedColor.copy(alpha = 0.35f),
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.dp.toPx()
        )

        if (evals.size >= 2) {
            val linePath = Path()
            val fillPath = Path()
            fillPath.moveTo(0f, midY)

            evals.forEachIndexed { idx, e ->
                val x = (idx.toFloat() / totalPlies) * w
                val y = yForEval(e)
                if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            fillPath.lineTo(w, midY)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.28f),
                        emeraldColor.copy(alpha = 0.08f)
                    )
                )
            )
            drawPath(
                linePath,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
            )

            criticalPlies.forEach { (plyOneBased, dotColor) ->
                val e = evals.getOrNull(plyOneBased) ?: return@forEach
                val x = (plyOneBased.toFloat() / totalPlies) * w
                val y = yForEval(e)
                drawCircle(
                    color = dotColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        val clampedCur = currentPly.coerceIn(0, totalPlies)
        val cx = (clampedCur.toFloat() / totalPlies) * w
        val cy = yForEval(evals.getOrElse(clampedCur) { 0.0 })
        drawLine(
            color = emeraldColor.copy(alpha = 0.90f),
            start = Offset(cx, 0f),
            end = Offset(cx, h),
            strokeWidth = 2.dp.toPx()
        )
        drawCircle(
            color = emeraldColor.copy(alpha = 0.28f),
            radius = 7.5.dp.toPx(),
            center = Offset(cx, cy)
        )
        drawCircle(
            color = emeraldColor,
            radius = 4.5.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}
