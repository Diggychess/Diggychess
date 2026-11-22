package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.ChessBoard
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.theme.*

data class CapturedMaterialInfo(
    val capturedBlackPieces: List<PieceType>, // pieces White captured
    val capturedWhitePieces: List<PieceType>, // pieces Black captured
    val whiteAdvantage: Int
)

fun computeCapturedMaterial(fen: String): CapturedMaterialInfo {
    val board = ChessBoard(fen)
    val startingCounts = mapOf(
        PieceType.QUEEN to 1,
        PieceType.ROOK to 2,
        PieceType.BISHOP to 2,
        PieceType.KNIGHT to 2,
        PieceType.PAWN to 8
    )

    val whitePresent = mutableMapOf<PieceType, Int>()
    val blackPresent = mutableMapOf<PieceType, Int>()
    var whitePts = 0
    var blackPts = 0

    for (sq in 0 until 64) {
        val p = board.board[sq] ?: continue
        if (p.type == PieceType.KING) continue
        if (p.color == PieceColor.WHITE) {
            whitePresent[p.type] = (whitePresent[p.type] ?: 0) + 1
            whitePts += p.type.value
        } else {
            blackPresent[p.type] = (blackPresent[p.type] ?: 0) + 1
            blackPts += p.type.value
        }
    }

    val capWhite = mutableListOf<PieceType>()
    val capBlack = mutableListOf<PieceType>()
    listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT, PieceType.PAWN).forEach { type ->
        val start = startingCounts[type] ?: 0
        val missingWhite = (start - (whitePresent[type] ?: 0)).coerceAtLeast(0)
        val missingBlack = (start - (blackPresent[type] ?: 0)).coerceAtLeast(0)
        repeat(missingWhite) { capWhite.add(type) }
        repeat(missingBlack) { capBlack.add(type) }
    }

    return CapturedMaterialInfo(
        capturedBlackPieces = capBlack,
        capturedWhitePieces = capWhite,
        whiteAdvantage = whitePts - blackPts
    )
}

fun formatClockMillis(ms: Long?): String {
    if (ms == null) return ""
    val clamped = ms.coerceAtLeast(0L)
    val totalSec = ((clamped + 999L) / 1000L).toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%d:%02d".format(mins, secs)
}

@Composable
fun PlayerBar(
    name: String,
    pieceColor: PieceColor,
    isActiveTurn: Boolean,
    fen: String,
    modifier: Modifier = Modifier,
    rating: String? = null,
    isBot: Boolean = false,
    statusNote: String? = null,
    clockMillis: Long? = null
) {
    val material = remember(fen) { computeCapturedMaterial(fen) }
    val capturedPieces = if (pieceColor == PieceColor.WHITE) {
        material.capturedBlackPieces
    } else {
        material.capturedWhitePieces
    }
    val adv = if (pieceColor == PieceColor.WHITE) material.whiteAdvantage else -material.whiteAdvantage
    val capturedColor = pieceColor.opposite()

    val lowTime = clockMillis != null && clockMillis in 1L..15_000L
    val avatarBg = if (pieceColor == PieceColor.WHITE) Color(0xFFF3F4F6) else Color(0xFF27272A)
    val avatarFg = if (pieceColor == PieceColor.WHITE) Color(0xFF121212) else Color(0xFFFFFFFF)

    val containerColor by animateColorAsState(
        targetValue = if (isActiveTurn) CardSurfaceElevated else SurfaceDark,
        animationSpec = tween(180),
        label = "player_bar_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActiveTurn) AccentGreen.copy(alpha = 0.72f) else BorderSubtle,
        animationSpec = tween(180),
        label = "player_bar_border"
    )

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(DiggySpacing.controlRadius),
        border = BorderStroke(width = 1.dp, color = borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Avatar badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(avatarBg)
                    .border(
                        width = if (isActiveTurn) 1.5.dp else 1.dp,
                        color = if (isActiveTurn) AccentGreen else BorderSubtle,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBot) Icons.Rounded.SmartToy else Icons.Rounded.Person,
                    contentDescription = "$name avatar",
                    tint = avatarFg,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Name, rating, captured pieces
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!rating.isNullOrBlank()) {
                        Surface(
                            color = BgDark,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = rating,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (isActiveTurn) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        )
                    }
                }

                if (capturedPieces.isNotEmpty() || adv > 0 || !statusNote.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        capturedPieces.take(9).forEach { pt ->
                            ChessPieceIcon(
                                piece = ChessPiece(capturedColor, pt),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (adv > 0) {
                            Text(
                                text = " +$adv",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }
                        if (!statusNote.isNullOrBlank()) {
                            if (capturedPieces.isNotEmpty() || adv > 0) {
                                Text(" · ", fontSize = 11.sp, color = TextMuted)
                            }
                            Text(
                                text = statusNote,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = if (isActiveTurn) AccentGreen else TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Live Clock Pill
            if (clockMillis != null) {
                val clockBg = when {
                    lowTime && isActiveTurn -> FeedbackDangerRed.copy(alpha = 0.22f)
                    isActiveTurn -> AccentGreen.copy(alpha = 0.16f)
                    else -> BgDark
                }
                val clockFg = when {
                    lowTime && isActiveTurn -> FeedbackDangerRed
                    isActiveTurn -> AccentGreen
                    else -> TextMuted
                }
                Surface(
                    color = clockBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (lowTime && isActiveTurn) FeedbackDangerRed else if (isActiveTurn) AccentGreen else BorderSubtle
                    )
                ) {
                    Text(
                        text = formatClockMillis(clockMillis),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Bold,
                        color = clockFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
