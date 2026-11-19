package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType

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
    contentDescription: String? = null
) {
    Image(
        painter = painterResource(id = chessPieceDrawableRes(piece)),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
