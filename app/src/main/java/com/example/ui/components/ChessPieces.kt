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
                        shadowAlpha = shadowAlpha * 0.65f,
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
                if (uprightTiltDeg != 0f && shadowAlpha > 0f) {
                    GroundContactShadow(
                        shadowScale = shadowScale,
                        shadowAlpha = shadowAlpha * 0.8f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(modifier = uprightModifier, contentAlignment = Alignment.Center) {
                    // Soft directional shadow underneath Neo pieces
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color(0x5A000000), BlendMode.SrcIn),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 0.6.dp, y = 1.6.dp)
                    )
                    Image(
                        painter = painter,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Slight glossy highlight on top-left edge
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color.White, BlendMode.SrcIn),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = (-0.6).dp, y = (-0.8).dp)
                            .alpha(if (piece.color == PieceColor.WHITE) 0.11f else 0.08f)
                    )
                }
            }
        }

        PieceStyle.THREED -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                // Grounded directional elliptical cast shadow flat on the square surface
                if (shadowAlpha > 0f) {
                    GroundContactShadow(
                        shadowScale = shadowScale,
                        shadowAlpha = shadowAlpha,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Counter-tilted upright Staunton silhouette + turned cylindrical base
                Box(modifier = uprightModifier, contentAlignment = Alignment.Center) {
                    // Turned 3D pedestal base disc under foot of piece
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val pedW = w * 0.66f
                        val pedH = h * 0.18f
                        val pedLeft = (w - pedW) / 2f
                        val pedTop = h * 0.77f

                        val rimBase = if (piece.color == PieceColor.WHITE) Color(0xFFB5A17D) else Color(0xFF0A0B0E)
                        val rimSide = if (piece.color == PieceColor.WHITE) Color(0xFF8C7959) else Color(0xFF1D1F24)
                        val rimHighlight = if (piece.color == PieceColor.WHITE) Color(0xFFF6EBD4) else Color(0xFF424651)

                        // Extruded lower cylinder lip
                        drawOval(
                            color = rimSide,
                            topLeft = Offset(pedLeft, pedTop + h * 0.032f),
                            size = Size(pedW, pedH)
                        )
                        // Top bevel ring
                        drawOval(
                            brush = Brush.verticalGradient(
                                colors = listOf(rimHighlight, rimBase),
                                startY = pedTop,
                                endY = pedTop + pedH
                            ),
                            topLeft = Offset(pedLeft, pedTop),
                            size = Size(pedW, pedH)
                        )
                        drawOval(
                            color = rimHighlight.copy(alpha = 0.45f),
                            topLeft = Offset(pedLeft, pedTop),
                            size = Size(pedW, pedH),
                            style = Stroke(width = (w * 0.022f).coerceAtLeast(1f))
                        )
                    }

                    // Deep ambient contact silhouette
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color(0x88000000), BlendMode.SrcIn),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 1.4.dp, y = 2.6.dp)
                    )
                    // 3D base rim extrusion behind piece
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(
                            if (piece.color == PieceColor.WHITE) Color(0xFFCBB897) else Color(0xFF101115),
                            BlendMode.SrcIn
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 0.2.dp, y = 1.1.dp)
                    )
                    // Primary carved Staunton piece
                    Image(
                        painter = painter,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = (-0.9).dp)
                    )
                    // Warm key-light rim highlight from top-left
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(
                            if (piece.color == PieceColor.WHITE) Color(0xFFFFF8E8) else Color(0xFFDDE4F0),
                            BlendMode.SrcIn
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = (-1.0).dp, y = (-1.5).dp)
                            .alpha(if (piece.color == PieceColor.WHITE) 0.18f else 0.13f)
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
        val ovalW = w * 0.78f * shadowScale
        val ovalH = h * 0.25f * shadowScale
        val cx = w * 0.54f
        val cy = h * 0.87f
        val left = cx - ovalW / 2f
        val top = cy - ovalH / 2f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = (0.48f * shadowAlpha).coerceIn(0f, 1f)),
                    Color.Black.copy(alpha = (0.20f * shadowAlpha).coerceIn(0f, 1f)),
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
