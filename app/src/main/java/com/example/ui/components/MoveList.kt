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
                            fontFamily = JetBrainsMonoFontFamily,
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isLast) AccentGreen else TextPrimary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveSanMoveList(
    sanMoves: List<String>,
    modifier: Modifier = Modifier,
    maxHeight: androidx.compose.ui.unit.Dp = 260.dp
) {
    val pairs = sanMoves.chunked(2)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(sanMoves.size) {
        if (pairs.isNotEmpty()) {
            listState.animateScrollToItem(pairs.lastIndex)
        }
    }

    Surface(
        color = SurfaceDark,
        shape = RoundedCornerShape(DiggySpacing.controlRadius),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .testTag("play_san_move_list")
    ) {
        if (sanMoves.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Moves will appear here (1. e4 e5 ...)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp, max = maxHeight)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(pairs.size, key = { it }) { idx ->
                    val whiteSan = pairs[idx][0]
                    val blackSan = pairs[idx].getOrNull(1)
                    val whiteIndex = idx * 2
                    val blackIndex = idx * 2 + 1
                    val whiteActive = whiteIndex == sanMoves.lastIndex
                    val blackActive = blackSan != null && blackIndex == sanMoves.lastIndex

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${idx + 1}.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontFeatureSettings = "tnum"
                            ),
                            color = TextMuted,
                            modifier = Modifier.width(30.dp)
                        )
                        Surface(
                            color = if (whiteActive) AccentGreen.copy(alpha = 0.22f) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            border = if (whiteActive) BorderStroke(1.dp, AccentGreen.copy(alpha = 0.6f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("play_san_move_$whiteIndex")
                        ) {
                            Text(
                                text = whiteSan,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontFeatureSettings = "tnum"
                                ),
                                fontWeight = if (whiteActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (whiteActive) AccentGreen else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                        if (blackSan != null) {
                            Surface(
                                color = if (blackActive) AccentGreen.copy(alpha = 0.22f) else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                border = if (blackActive) BorderStroke(1.dp, AccentGreen.copy(alpha = 0.6f)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("play_san_move_$blackIndex")
                            ) {
                                Text(
                                    text = blackSan,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    fontWeight = if (blackActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (blackActive) AccentGreen else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
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
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontFeatureSettings = "tnum"
                    ),
                    color = TextMuted,
                    modifier = Modifier.width(32.dp)
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
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(ply.badgeColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ply.badgeGlyph,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ply.badgeColor
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = ply.san,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontFeatureSettings = "tnum"
                    ),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary
                )
            }
            Text(
                text = MoveExplainer.formatPlyEval(ply.evalAfter, ply.mate),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontFeatureSettings = "tnum"
                ),
                color = TextMuted
            )
        }
    }
}
