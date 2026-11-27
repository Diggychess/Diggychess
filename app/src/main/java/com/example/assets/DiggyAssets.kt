package com.example.assets

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.ui.components.chessPieceDrawableRes
import java.net.URLEncoder

/**
 * Centralized registry of all free external CDN endpoints, bundled typography,
 * synthetic offline audio cues, and fallback drawables for Chess Analyzer: AI Coach.
 * All external resources fail gracefully to offline Compose vectors / initials / synthesis.
 */
object DiggyAssets {

    // 1. Free Lichess piece SVG CDN sets (cburnett primary, merida & alpha fallbacks)
    enum class LichessPieceCdnSet(val slug: String, val label: String) {
        CBURNETT("cburnett", "Cburnett"),
        MERIDA("merida", "Merida"),
        ALPHA("alpha", "Alpha")
    }

    private const val LICHESS_PIECE_CDN_BASE =
        "https://cdn.jsdelivr.net/gh/lichess-org/lila@master/public/piece"

    fun pieceSvgUrl(
        piece: ChessPiece,
        set: LichessPieceCdnSet = LichessPieceCdnSet.CBURNETT
    ): String {
        val c = if (piece.color == PieceColor.WHITE) 'w' else 'b'
        val t = piece.type.symbol.uppercaseChar()
        return "$LICHESS_PIECE_CDN_BASE/${set.slug}/$c$t.svg"
    }

    @DrawableRes
    fun offlinePieceDrawable(piece: ChessPiece): Int = chessPieceDrawableRes(piece)

    // 2. Free DiceBear avatar URL builder (always falls back cleanly to initials/bot circle offline)
    fun diceBearAvatarUrl(seed: String, style: String = "thumbs"): String {
        val encoded = runCatching { URLEncoder.encode(seed.ifBlank { "ChessPlayer" }, "UTF-8") }
            .getOrDefault("ChessPlayer")
        return "https://api.dicebear.com/9.x/$style/svg?seed=$encoded&radius=24"
    }

    fun avatarInitials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "CA"
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        }
    }

    // 3. Bundled Fonts (res/font/*.ttf via font-util)
    object Fonts {
        val nunitoResId: Int = R.font.nunito      // weights 400, 700, 800
        val interResId: Int = R.font.inter        // weights 400, 600, 700
        val monoResId: Int = R.font.jetbrains_mono
    }

    // 4. Offline Audio Synthesis Cues (no copyrighted audio files, zero network needed)
    enum class SoundCueId {
        MOVE,
        CAPTURE,
        CHECK,
        CASTLE,
        GAME_END,
        DRAW,
        PICKUP,
        WRONG
    }

    // 5. Evaluation Graph styling constants
    object EvalGraph {
        val WhiteFillColor = Color(0xFFF3F3EE)
        val BlackFillColor = Color(0xFF1F1D1A)
        val CenterLineColor = Color(0x66C3C2C1)
        val AccentCursorColor = Color(0xFF81B64C)
    }
}
