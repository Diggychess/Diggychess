package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.AnalyzedPly
import com.example.chess.MoveClass
import com.example.chess.MoveExplainer
import com.example.ui.theme.*

@Composable
fun LiveMoveRibbon(
    sanMoves: List<String>,
    modifier: Modifier = Modifier
) {
    if (sanMoves.isEmpty()) return

    val pairs = sanMoves.chunked(2)
    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(DiggySpacing.controlRadius),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            pairs.forEachIndexed { idx, pair ->
                val whiteSan = pair[0]
                val blackSan = pair.getOrNull(1)
                val moveLabel = if (blackSan != null) "${idx + 1}. $whiteSan $blackSan" else "${idx + 1}. $whiteSan"
                val isLast = idx == pairs.lastIndex
                Surface(
                    color = if (isLast) AccentGreen.copy(alpha = 0.16f) else Color.Transparent,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = moveLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isLast) AccentGreen else TextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyzedMoveList(
    plies: List<AnalyzedPly>,
    currentPly: Int,
    onSelectPly: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pairCount = (plies.size + 1) / 2
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
    ) {
        items(pairCount, key = { it }) { pairIdx ->
            val wPly = plies.getOrNull(pairIdx * 2)
            val bPly = plies.getOrNull(pairIdx * 2 + 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${pairIdx + 1}.",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = TextMuted,
                    modifier = Modifier.width(30.dp)
                )
                if (wPly != null) {
                    AnalyzedMoveChip(
                        ply = wPly,
                        isSelected = currentPly == wPly.index + 1,
                        onClick = { onSelectPly(wPly.index + 1) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (bPly != null) {
                    AnalyzedMoveChip(
                        ply = bPly,
                        isSelected = currentPly == bPly.index + 1,
                        onClick = { onSelectPly(bPly.index + 1) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AnalyzedMoveChip(
    ply: AnalyzedPly,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) CardSurfaceElevated else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) AccentGreen else Color.Transparent),
        modifier = modifier
            .heightIn(min = 42.dp)
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
                        .background(ply.badgeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = ply.san,
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary
                )
                if (ply.moveClass != MoveClass.GOOD && ply.moveClass != MoveClass.BEST) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ply.badgeGlyph,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ply.badgeColor
                    )
                }
            }
            Text(
                text = MoveExplainer.formatPlyEval(ply.evalAfter, ply.mate),
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = TextMuted
            )
        }
    }
}
