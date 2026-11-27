package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.MoveExplainer
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DiggySpacing
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun EvalBar(
    evalPawns: Double,
    flipped: Boolean,
    modifier: Modifier = Modifier,
    mate: Int? = null
) {
    val effectiveEval = when {
        mate != null && mate > 0 -> 15.0
        mate != null && mate < 0 -> -15.0
        else -> evalPawns
    }
    val winPct = (MoveExplainer.winProbability(effectiveEval) / 100.0).toFloat().coerceIn(0.05f, 0.95f)
    val animatedFrac by animateFloatAsState(
        targetValue = winPct,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "eval_bar_fraction"
    )

    val scoreLabel = MoveExplainer.formatPlyEval(evalPawns, mate)
        .replace("+", "")
        .let { formatted ->
            if (mate == null && evalPawns >= 0.0 && !formatted.startsWith("-")) "+$formatted" else formatted
        }
        .let { s ->
            // Compact fit inside 20dp bar
            if (s.length > 4 && !s.startsWith("#")) s.take(4) else s
        }

    val whiteLeading = winPct >= 0.5f
    val labelAlign = when {
        whiteLeading && !flipped -> Alignment.BottomCenter
        whiteLeading && flipped -> Alignment.TopCenter
        !whiteLeading && !flipped -> Alignment.TopCenter
        else -> Alignment.BottomCenter
    }
    val labelColor = if (whiteLeading) Color(0xFF1D1B18) else Color(0xFFF5F4F1)

    Box(
        modifier = modifier
            .width(DiggySpacing.evalBarWidth)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1D1B18))
            .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedFrac)
                .align(if (flipped) Alignment.TopCenter else Alignment.BottomCenter)
                .background(Color(0xFFF5F4F1))
        )

        // Subtle 0.0 equal-position midpoint tick line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.Center)
                .background(Color(0x668E8E93))
        )

        Text(
            text = scoreLabel,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor,
            maxLines = 1,
            modifier = Modifier
                .align(labelAlign)
                .padding(vertical = 4.dp)
        )
    }
}
