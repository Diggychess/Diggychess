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
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ClassBlunder
import com.example.ui.theme.ClassMistake
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
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

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
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
            return midY - (clamped / 5f) * (midY * 0.90f)
        }

        // Subtle baseline
        drawLine(
            color = TextMuted.copy(alpha = 0.4f),
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
                if (idx == 0) {
                    linePath.moveTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                }
                fillPath.lineTo(x, y)
            }
            fillPath.lineTo(w, midY)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        StudioCyan.copy(alpha = 0.28f),
                        AccentGreen.copy(alpha = 0.14f)
                    )
                )
            )
            drawPath(
                linePath,
                color = StudioCyan,
                style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
            )

            // Plot critical mistake / blunder dots on graph
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

        // Cursor line + node
        val clampedCur = currentPly.coerceIn(0, totalPlies)
        val cx = (clampedCur.toFloat() / totalPlies) * w
        val cy = yForEval(evals.getOrElse(clampedCur) { 0.0 })
        drawLine(
            color = ClassBlunder.copy(alpha = 0.85f),
            start = Offset(cx, 0f),
            end = Offset(cx, h),
            strokeWidth = 1.8.dp.toPx()
        )
        drawCircle(
            color = ClassMistake,
            radius = 4.dp.toPx(),
            center = Offset(cx, cy)
        )
    }
}
