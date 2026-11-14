package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.chess.MoveExplainer

@Composable
fun EvalBar(
    evalPawns: Double,
    flipped: Boolean,
    modifier: Modifier = Modifier
) {
    val winPct = (MoveExplainer.winProbability(evalPawns) / 100.0).toFloat().coerceIn(0.03f, 0.97f)
    val animatedFrac by animateFloatAsState(
        targetValue = winPct,
        animationSpec = tween(durationMillis = 250),
        label = "eval_bar_fraction"
    )

    Box(
        modifier = modifier
            .width(14.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF262626))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedFrac)
                .align(if (flipped) Alignment.TopCenter else Alignment.BottomCenter)
                .background(Color(0xFFF4F4F2))
        )
    }
}
