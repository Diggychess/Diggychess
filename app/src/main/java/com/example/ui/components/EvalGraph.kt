package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ClassBlunder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import kotlin.math.roundToInt

@Composable
fun EvalGraph(
    evals: List<Double>,
    currentPly: Int,
    onSelectPly: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPlies = (evals.size - 1).coerceAtLeast(1)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
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
            return midY - (clamped / 5f) * (midY * 0.92f)
        }

        // Baseline
        drawLine(
            color = TextMuted.copy(alpha = 0.5f),
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

            drawPath(fillPath, color = AccentGreen.copy(alpha = 0.32f))
            drawPath(
                linePath,
                color = AccentGreen,
                style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
            )
        }

        // Cursor line
        val cx = (currentPly.toFloat() / totalPlies).coerceIn(0f, 1f) * w
        drawLine(
            color = ClassBlunder,
            start = Offset(cx, 0f),
            end = Offset(cx, h),
            strokeWidth = 2.dp.toPx()
        )
    }
}
