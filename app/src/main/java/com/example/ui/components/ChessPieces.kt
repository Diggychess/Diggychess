package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.theme.LocalPieceStyle
import com.example.ui.theme.PieceStyle

@DrawableRes
fun chessPieceDrawableRes(piece: ChessPiece): Int = when (piece.color) {
    PieceColor.WHITE -> when (piece.type) {
        PieceType.PAWN -> R.drawable.ic_piece_wp
        PieceType.ROOK -> R.drawable.ic_piece_wr
        PieceType.KNIGHT -> R.drawable.ic_piece_wn
        PieceType.BISHOP -> R.drawable.ic_piece_wb
        PieceType.QUEEN -> R.drawable.ic_piece_wq
        PieceType.KING -> R.drawable.ic_piece_wk
    }
    PieceColor.BLACK -> when (piece.type) {
        PieceType.PAWN -> R.drawable.ic_piece_bp
        PieceType.ROOK -> R.drawable.ic_piece_br
        PieceType.KNIGHT -> R.drawable.ic_piece_bn
        PieceType.BISHOP -> R.drawable.ic_piece_bb
        PieceType.QUEEN -> R.drawable.ic_piece_bq
        PieceType.KING -> R.drawable.ic_piece_bk
    }
}

@Composable
fun ChessPieceIcon(
    piece: ChessPiece,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    pieceStyle: PieceStyle = LocalPieceStyle.current,
    uprightTiltDeg: Float = 0f,
    shadowScale: Float = 1f,
    shadowAlpha: Float = 1f
) {
    val painter = painterResource(id = chessPieceDrawableRes(piece))

    val uprightModifier = if (uprightTiltDeg != 0f) {
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationX = uprightTiltDeg
                transformOrigin = TransformOrigin(0.5f, 0.86f)
                cameraDistance = 14f * density
            }
    } else {
        Modifier.fillMaxSize()
    }

    when (pieceStyle) {
        PieceStyle.CLASSIC_2D -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                if (uprightTiltDeg != 0f && shadowAlpha > 0f) {
                    GroundContactShadow(
                        shadowScale = shadowScale,
                        shadowAlpha = shadowAlpha * 0.7f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = uprightModifier
                )
            }
        }

        PieceStyle.NEO -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                if (shadowAlpha > 0f) {
                    GroundContactShadow(
                        shadowScale = if (uprightTiltDeg != 0f) shadowScale * 0.92f else shadowScale * 0.78f,
                        shadowAlpha = if (uprightTiltDeg != 0f) shadowAlpha * 0.8f else shadowAlpha * 0.30f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(modifier = uprightModifier, contentAlignment = Alignment.Center) {
                    // Crisp subtle drop shadow
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color(0x38000000), BlendMode.SrcIn),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 0.6.dp, y = 1.4.dp)
                    )
                    // Primary vector silhouette
                    Image(
                        painter = painter,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        PieceStyle.THREED -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                if (shadowAlpha > 0f) {
                    GroundContactShadow(
                        shadowScale = shadowScale,
                        shadowAlpha = shadowAlpha,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(modifier = uprightModifier, contentAlignment = Alignment.Center) {
                    // Turned 3D pedestal base disc under foot of piece
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val pedW = w * 0.66f
                        val pedH = h * 0.18f
                        val pedLeft = (w - pedW) / 2f
                        val pedTop = h * 0.77f

                        val rimBase = if (piece.color == PieceColor.WHITE) Color(0xFFBFA980) else Color(0xFF0A0B0F)
                        val rimHighlight = if (piece.color == PieceColor.WHITE) Color(0xFFFFF3D8) else Color(0xFF424854)

                        drawOval(
                            color = rimBase,
                            topLeft = Offset(pedLeft, pedTop + h * 0.032f),
                            size = Size(pedW, pedH)
                        )
                        drawOval(
                            brush = Brush.verticalGradient(
                                colors = listOf(rimHighlight, rimBase),
                                startY = pedTop,
                                endY = pedTop + pedH
                            ),
                            topLeft = Offset(pedLeft, pedTop),
                            size = Size(pedW, pedH)
                        )
                    }

                    // Deep drop shadow
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color(0x7A000000), BlendMode.SrcIn),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 1.2.dp, y = 2.2.dp)
                    )
                    // Primary Staunton silhouette
                    Image(
                        painter = painter,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = (-0.8).dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GroundContactShadow(
    shadowScale: Float,
    shadowAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val ovalW = w * 0.80f * shadowScale
        val ovalH = h * 0.25f * shadowScale
        val cx = w * 0.53f
        val cy = h * 0.865f
        val left = cx - ovalW / 2f
        val top = cy - ovalH / 2f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = (0.52f * shadowAlpha).coerceIn(0f, 1f)),
                    Color.Black.copy(alpha = (0.22f * shadowAlpha).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = (ovalW * 0.58f).coerceAtLeast(1f)
            ),
            topLeft = Offset(left, top),
            size = Size(ovalW, ovalH)
        )
    }
}
