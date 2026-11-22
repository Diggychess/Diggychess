package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.rememberChessFeedback
import com.example.chess.*
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

data class BoardArrow(
    val from: String,
    val to: String,
    val color: Color
)

data class SquareMoveBadge(
    val square: String,
    val glyph: String,
    val color: Color
)

private data class SlidingPiece(
    val piece: ChessPiece,
    val fromSq: Int,
    val toSq: Int
)

internal fun isCastlingTransition(prev: ChessBoard, curr: ChessBoard): Boolean {
    for (color in PieceColor.entries) {
        val prevK = prev.kingSquare(color)
        val currK = curr.kingSquare(color)
        if (prevK >= 0 && currK >= 0 && prevK / 8 == currK / 8 && abs((prevK % 8) - (currK % 8)) == 2) {
            return true
        }
    }
    return false
}

private fun computeSlidingPieces(
    prevFen: String,
    currFen: String,
    lastMoveSquares: List<String>
): List<SlidingPiece> {
    if (prevFen == currFen) return emptyList()
    val prev = ChessBoard(prevFen)
    val curr = ChessBoard(currFen)
    val goneSquares = mutableListOf<Int>()
    val arrivedSquares = mutableListOf<Int>()
    for (i in 0 until 64) {
        val p = prev.board[i]
        val c = curr.board[i]
        if (p != c) {
            if (p != null && c == null) goneSquares.add(i)
            else if (c != null) arrivedSquares.add(i)
        }
    }
    if (goneSquares.isEmpty() || arrivedSquares.isEmpty() || goneSquares.size + arrivedSquares.size > 5) {
        return emptyList()
    }

    val slides = mutableListOf<SlidingPiece>()
    val usedFrom = mutableSetOf<Int>()
    val usedTo = mutableSetOf<Int>()

    if (lastMoveSquares.size >= 2) {
        val fromIdx = parseSquare(lastMoveSquares[0])
        val toIdx = parseSquare(lastMoveSquares[1])
        if (fromIdx != null && toIdx != null && fromIdx in goneSquares && toIdx in arrivedSquares) {
            val movedPiece = curr.board[toIdx]
            if (movedPiece != null) {
                slides.add(SlidingPiece(movedPiece, fromIdx, toIdx))
                usedFrom.add(fromIdx)
                usedTo.add(toIdx)
            }
        }
    }

    for (toIdx in arrivedSquares) {
        if (toIdx in usedTo) continue
        val targetPiece = curr.board[toIdx] ?: continue
        val matchingFrom = goneSquares.firstOrNull { fromIdx ->
            fromIdx !in usedFrom && prev.board[fromIdx] == targetPiece
        } ?: goneSquares.firstOrNull { fromIdx ->
            fromIdx !in usedFrom && prev.board[fromIdx]?.color == targetPiece.color
        }
        if (matchingFrom != null) {
            slides.add(SlidingPiece(targetPiece, matchingFrom, toIdx))
            usedFrom.add(matchingFrom)
            usedTo.add(toIdx)
        }
    }

    return slides
}

private data class CabinetColors(
    val topRim: Color,
    val bodyTop: Color,
    val bodyBottom: Color,
    val apronTop: Color,
    val apronBottom: Color,
    val goldInlay: Color
)

private fun cabinetColorsFor(theme: BoardTheme): CabinetColors = when (theme) {
    BoardTheme.GREEN -> CabinetColors(
        topRim = Color(0xFF4A3121),
        bodyTop = Color(0xFF332014),
        bodyBottom = Color(0xFF1F130B),
        apronTop = Color(0xFF2B1B10),
        apronBottom = Color(0xFF120B06),
        goldInlay = Color(0x99F5C542)
    )
    BoardTheme.BROWN -> CabinetColors(
        topRim = Color(0xFF563722),
        bodyTop = Color(0xFF3C2515),
        bodyBottom = Color(0xFF23140A),
        apronTop = Color(0xFF311E11),
        apronBottom = Color(0xFF140B06),
        goldInlay = Color(0x99F5C542)
    )
    BoardTheme.BLUE -> CabinetColors(
        topRim = Color(0xFF364152),
        bodyTop = Color(0xFF222A36),
        bodyBottom = Color(0xFF141820),
        apronTop = Color(0xFF1D242E),
        apronBottom = Color(0xFF0C0F14),
        goldInlay = Color(0x884C9AFF)
    )
    BoardTheme.GOLD -> CabinetColors(
        topRim = Color(0xFF43331A),
        bodyTop = Color(0xFF2A2010),
        bodyBottom = Color(0xFF171209),
        apronTop = Color(0xFF231A0D),
        apronBottom = Color(0xFF0E0A05),
        goldInlay = Color(0xCCF5C542)
    )
}

@Composable
fun ChessBoardView(
    fen: String,
    boardTheme: BoardTheme,
    flipped: Boolean = false,
    selectedSquare: String? = null,
    lastMoveSquares: List<String> = emptyList(),
    correctMoveSquares: List<String> = emptyList(),
    wrongMoveSquares: List<String> = emptyList(),
    hintSquares: List<String> = emptyList(),
    legalTargets: Map<String, Boolean> = emptyMap(), // square -> isCapture
    arrows: List<BoardArrow> = emptyList(),
    moveBadge: SquareMoveBadge? = null,
    animateMoves: Boolean = LocalPieceAnimations.current,
    pieceStyle: PieceStyle = LocalPieceStyle.current,
    is3dBoard: Boolean = LocalBoard3dEnabled.current,
    playMoveSounds: Boolean = true,
    onSquareClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val chess = remember(fen) { ChessBoard(fen) }
    val checkSquare = remember(fen) {
        if (chess.inCheck()) {
            val ks = chess.kingSquare(chess.turn)
            if (ks >= 0) squareName(ks) else null
        } else null
    }
    val isCheckmate = remember(fen) { chess.inCheckmate() }
    val isDraw = remember(fen) { chess.isGameOver() && !chess.inCheckmate() }
    val feedback = rememberChessFeedback()

    var lastRenderedFen by remember { mutableStateOf(fen) }
    var lastSoundFen by remember { mutableStateOf(fen) }
    var lastSelectedForPickup by remember { mutableStateOf(selectedSquare) }
    var activeSlides by remember { mutableStateOf<List<SlidingPiece>>(emptyList()) }
    val slideProgress = remember { Animatable(1f) }

    // Drag-and-drop state
    var draggedFromSq by remember { mutableStateOf<String?>(null) }
    var draggedPiece by remember { mutableStateOf<ChessPiece?>(null) }
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var hoveredDropSq by remember { mutableStateOf<String?>(null) }
    var suppressNextAnimation by remember { mutableStateOf(false) }

    val currentOnSquareClick by rememberUpdatedState(onSquareClick)
    val currentSelectedSquare by rememberUpdatedState(selectedSquare)

    // Soft pickup audio + haptic cue when user selects one of their own pieces
    LaunchedEffect(selectedSquare) {
        if (playMoveSounds &&
            selectedSquare != null &&
            selectedSquare != lastSelectedForPickup &&
            draggedFromSq == null
        ) {
            val idx = parseSquare(selectedSquare)
            val p = if (idx != null) chess.board[idx] else null
            if (p != null && p.color == chess.turn) {
                feedback.onPickup()
            }
        }
        lastSelectedForPickup = selectedSquare
    }

    // Move, capture, castling, check, game-end, and wrong-move sounds
    LaunchedEffect(fen, wrongMoveSquares) {
        if (playMoveSounds && fen != lastSoundFen) {
            val prevBoard = ChessBoard(lastSoundFen)
            val changedSquares = (0 until 64).count { prevBoard.board[it] != chess.board[it] }
            val isMoveLikeChange = changedSquares in 2..4
            val isCapture = chess.board.count { it != null } < prevBoard.board.count { it != null }
            val isCastle = isCastlingTransition(prevBoard, chess)

            if (wrongMoveSquares.isNotEmpty()) {
                feedback.onWrongMove()
            } else if (lastMoveSquares.isNotEmpty() || correctMoveSquares.isNotEmpty() || isMoveLikeChange) {
                feedback.onMove(
                    isCapture = isCapture,
                    isCheck = checkSquare != null,
                    isCastle = isCastle,
                    isCheckmate = isCheckmate,
                    isDraw = isDraw,
                    isGameOver = isCheckmate || isDraw
                )
            }
            lastSoundFen = fen
        } else if (playMoveSounds && wrongMoveSquares.isNotEmpty() && fen == lastSoundFen) {
            feedback.onWrongMove()
        }
    }

    LaunchedEffect(fen, animateMoves) {
        val slides = if (animateMoves && !suppressNextAnimation) {
            computeSlidingPieces(lastRenderedFen, fen, lastMoveSquares)
        } else emptyList()
        suppressNextAnimation = false
        lastRenderedFen = fen
        if (slides.isNotEmpty()) {
            activeSlides = slides
            slideProgress.snapTo(0f)
            slideProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = if (is3dBoard) 200 else 170,
                    easing = FastOutSlowInEasing
                )
            )
            activeSlides = emptyList()
        } else {
            activeSlides = emptyList()
            slideProgress.snapTo(1f)
        }
    }

    val animatingTargets = remember(activeSlides) {
        activeSlides.map { it.toSq }.toSet()
    }

    val cabinet = remember(boardTheme) { cabinetColorsFor(boardTheme) }
    val tiltDeg = if (is3dBoard) 19f else 0f
    val pieceUprightTilt = if (is3dBoard) -14f else 0f

    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val outerSize = maxWidth
        val cabinetSidePadding = if (is3dBoard) 8.dp else 0.dp
        val cabinetBottomInset = if (is3dBoard) 10.dp else 0.dp
        val framePadding = if (is3dBoard) 7.dp else 0.dp
        val cornerRadius = if (is3dBoard) 10.dp else 8.dp

        // Tabletop ambient shadow + 3D beveled front apron underneath tilted board
        if (is3dBoard) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val sidePad = cabinetSidePadding.toPx()
                val apronH = cabinetBottomInset.toPx()

                // Deep ambient drop shadow beneath wooden cabinet
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.56f),
                            Color.Black.copy(alpha = 0.24f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.94f),
                        radius = w * 0.54f
                    ),
                    topLeft = Offset(sidePad * 0.2f, h * 0.85f),
                    size = Size(w - sidePad * 0.4f, h * 0.15f)
                )

                // Extruded front wooden thickness lip under rank 1/8
                val lipLeft = sidePad * 0.55f
                val lipWidth = w - sidePad * 1.1f
                val lipTop = h - apronH * 1.65f
                val lipHeight = apronH * 1.45f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(cabinet.apronTop, cabinet.apronBottom),
                        startY = lipTop,
                        endY = lipTop + lipHeight
                    ),
                    topLeft = Offset(lipLeft, lipTop),
                    size = Size(lipWidth, lipHeight),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                // Specular edge bevel along front lip crease
                drawLine(
                    color = cabinet.goldInlay.copy(alpha = 0.26f),
                    start = Offset(lipLeft + 10.dp.toPx(), lipTop + 1.dp.toPx()),
                    end = Offset(lipLeft + lipWidth - 10.dp.toPx(), lipTop + 1.dp.toPx()),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
        }

        // Cabinet outer frame + tilted 3D playing surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = cabinetSidePadding,
                    end = cabinetSidePadding,
                    top = if (is3dBoard) 2.dp else 0.dp,
                    bottom = cabinetBottomInset
                )
                .graphicsLayer {
                    if (is3dBoard) {
                        rotationX = tiltDeg
                        cameraDistance = 15f * density
                        transformOrigin = TransformOrigin(0.5f, 0.52f)
                    }
                }
                .clip(RoundedCornerShape(cornerRadius))
                .then(
                    if (is3dBoard) {
                        Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(cabinet.topRim, cabinet.bodyTop, cabinet.bodyBottom)
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(cornerRadius))
                    } else {
                        Modifier.border(1.dp, BorderSubtle, RoundedCornerShape(cornerRadius))
                    }
                )
                .padding(framePadding)
        ) {
            // Gold tournament inlay line framing the 64 squares in optional 3D mode
            val innerShape = RoundedCornerShape(if (is3dBoard) 4.dp else cornerRadius)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(innerShape)
                    .then(
                        if (is3dBoard) {
                            Modifier.border(1.dp, cabinet.goldInlay, innerShape)
                        } else Modifier
                    )
            ) {
                val gridSize = maxWidth
                val cellDp = gridSize / 8f
                val density = LocalDensity.current
                val gridPx = with(density) { gridSize.toPx() }
                val cellPx = gridPx / 8f

                fun sqNameAt(pos: Offset): String? {
                    if (cellPx <= 0f) return null
                    val col = (pos.x / cellPx).toInt()
                    val row = (pos.y / cellPx).toInt()
                    if (col !in 0..7 || row !in 0..7) return null
                    val f = if (flipped) 7 - col else col
                    val r = if (flipped) 7 - row else row
                    return squareName(r * 8 + f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (onSquareClick != null) {
                                Modifier.pointerInput(fen, flipped) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val startSq = sqNameAt(down.position) ?: return@awaitEachGesture
                                        val sqIdx = parseSquare(startSq) ?: return@awaitEachGesture
                                        val piece = chess.board[sqIdx] ?: return@awaitEachGesture
                                        if (piece.color != chess.turn) return@awaitEachGesture

                                        val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                                            change.consume()
                                        } ?: return@awaitEachGesture

                                        draggedFromSq = startSq
                                        draggedPiece = piece
                                        dragOffsetPx = dragStart.position
                                        hoveredDropSq = sqNameAt(dragStart.position)
                                        feedback.onPickup()
                                        if (currentSelectedSquare != startSq) {
                                            currentOnSquareClick?.invoke(startSq)
                                        }

                                        try {
                                            drag(dragStart.id) { change ->
                                                change.consume()
                                                dragOffsetPx = change.position
                                                hoveredDropSq = sqNameAt(change.position)
                                            }
                                            val dropSq = hoveredDropSq
                                            if (dropSq != null && dropSq != startSq) {
                                                suppressNextAnimation = true
                                                currentOnSquareClick?.invoke(dropSq)
                                            }
                                        } finally {
                                            draggedFromSq = null
                                            draggedPiece = null
                                            hoveredDropSq = null
                                        }
                                    }
                                }
                            } else Modifier
                        )
                ) {
                    // 8x8 Board Squares & Highlights
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (dr in 0 until 8) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                for (df in 0 until 8) {
                                    val r = if (flipped) 7 - dr else dr
                                    val f = if (flipped) 7 - df else df
                                    val sqIdx = r * 8 + f
                                    val sqName = squareName(sqIdx)
                                    val isLight = (r + f) % 2 == 0
                                    val baseColor = if (isLight) boardTheme.lightSquare else boardTheme.darkSquare
                                    val labelColor = if (isLight) boardTheme.darkSquare else boardTheme.lightSquare
                                    val piece = chess.board[sqIdx]
                                    val isSelected = sqName == selectedSquare
                                    val isLast = sqName in lastMoveSquares
                                    val isCorrect = sqName in correctMoveSquares
                                    val isWrong = sqName in wrongMoveSquares
                                    val isHint = sqName in hintSquares
                                    val isCheck = sqName == checkSquare
                                    val isDropHover = sqName == hoveredDropSq && draggedFromSq != null && sqName != draggedFromSq
                                    val captureTarget = legalTargets[sqName]

                                    val squareDesc = buildString {
                                        append(sqName)
                                        if (piece != null) {
                                            append(" ${piece.color.name.lowercase()} ${piece.type.pieceName}")
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(baseColor)
                                            .then(
                                                if (onSquareClick != null) {
                                                    Modifier
                                                        .clickable { onSquareClick(sqName) }
                                                        .testTag("sq_$sqName")
                                                } else Modifier
                                            )
                                            .semantics { contentDescription = squareDesc },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected && !isCorrect && !isWrong) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(HighlightSelected)
                                            )
                                        } else if (isLast && !isCorrect && !isWrong) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(HighlightLastMove)
                                            )
                                        }
                                        if (isCorrect) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(HighlightCorrect)
                                                    .border(2.dp, ClassBest)
                                            )
                                        }
                                        if (isWrong) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(HighlightWrong)
                                                    .border(2.dp, ClassBlunder)
                                            )
                                        }
                                        if (isHint && !isCorrect && !isWrong) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(HighlightHint)
                                            )
                                        }
                                        if (isCheck) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.radialGradient(
                                                            colors = listOf(CheckGlow, Color.Transparent)
                                                        )
                                                    )
                                            )
                                        }
                                        if (isDropHover) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.White.copy(alpha = 0.20f))
                                                    .border(2.5.dp, HighlightDragTarget)
                                            )
                                        }

                                        // Tactile micro-bevel on individual 3D square tile
                                        if (is3dBoard) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                val sW = size.width
                                                val sH = size.height
                                                drawLine(
                                                    color = Color.White.copy(alpha = if (isLight) 0.16f else 0.08f),
                                                    start = Offset(0f, 0.5f),
                                                    end = Offset(sW, 0.5f),
                                                    strokeWidth = 1f
                                                )
                                                drawLine(
                                                    color = Color.Black.copy(alpha = if (isLight) 0.08f else 0.14f),
                                                    start = Offset(0f, sH - 0.5f),
                                                    end = Offset(sW, sH - 0.5f),
                                                    strokeWidth = 1f
                                                )
                                            }
                                        }

                                        // Coordinates inside squares: ranks 1–8 left column, files a–h bottom row
                                        if (df == 0) {
                                            Text(
                                                text = "${8 - r}",
                                                color = labelColor.copy(alpha = 0.90f),
                                                fontFamily = InterFontFamily,
                                                fontSize = if (outerSize < 260.dp) 8.sp else 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(start = 3.dp, top = 1.dp)
                                                    .testTag("coord_rank_${8 - r}")
                                            )
                                        }
                                        if (dr == 7) {
                                            Text(
                                                text = "${'a' + f}",
                                                color = labelColor.copy(alpha = 0.90f),
                                                fontFamily = InterFontFamily,
                                                fontSize = if (outerSize < 260.dp) 8.sp else 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(end = 3.dp, bottom = 1.dp)
                                                    .testTag("coord_file_${'a' + f}")
                                            )
                                        }

                                        // Static piece (hidden while sliding in or currently dragged)
                                        if (piece != null && sqIdx !in animatingTargets && sqName != draggedFromSq) {
                                            ChessPieceIcon(
                                                piece = piece,
                                                pieceStyle = pieceStyle,
                                                uprightTiltDeg = pieceUprightTilt,
                                                modifier = Modifier.fillMaxSize(0.88f)
                                            )
                                        }

                                        // Legal move indicator (Chess.com / Lichess dot & capture ring)
                                        if (captureTarget != null) {
                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                if (captureTarget) {
                                                    drawCircle(
                                                        color = Color(0x2E000000),
                                                        radius = size.minDimension * 0.43f,
                                                        style = Stroke(width = size.minDimension * 0.088f)
                                                    )
                                                } else {
                                                    drawCircle(
                                                        color = Color(0x28000000),
                                                        radius = size.minDimension * 0.165f
                                                    )
                                                }
                                            }
                                        }

                                        // Checkmate badge on defeated king square
                                        if (isCheck && isCheckmate) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(2.dp)
                                                    .size(if (outerSize < 260.dp) 15.dp else 18.dp)
                                                    .clip(CircleShape)
                                                    .background(ClassBlunder)
                                                    .border(1.dp, Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "#",
                                                    color = Color.White,
                                                    fontSize = if (outerSize < 260.dp) 8.sp else 10.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }

                                        // Move classification badge
                                        if (moveBadge != null && moveBadge.square == sqName) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(1.dp)
                                                    .size(if (outerSize < 260.dp) 15.dp else 18.dp)
                                                    .clip(CircleShape)
                                                    .background(moveBadge.color)
                                                    .border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = moveBadge.glyph,
                                                    color = Color.White,
                                                    fontSize = if (outerSize < 260.dp) 8.sp else 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    lineHeight = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Subtle studio surface grain + directional overhead light wash in 3D mode
                    if (is3dBoard) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Soft overhead spotlight -> lower shadow gradient across playing field
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.055f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.075f)
                                    )
                                )
                            )
                            // Ultra-subtle inlaid horizontal wood grain striations
                            val step = size.height / 28f
                            for (i in 1 until 28) {
                                val y = i * step
                                drawLine(
                                    color = if (i % 2 == 0) Color.Black.copy(alpha = 0.022f) else Color.White.copy(alpha = 0.018f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f
                                )
                            }
                        }
                    }

                    // Smooth 200ms 3D arc-lifted sliding piece overlay
                    if (activeSlides.isNotEmpty()) {
                        val t = slideProgress.value
                        val arcLift = sin(t * Math.PI).toFloat()
                        val flightScale = 1f + (if (is3dBoard) 0.14f else 0.06f) * arcLift
                        val liftYDp = if (is3dBoard) -(cellDp * 0.22f * arcLift) else 0.dp
                        for (slide in activeSlides) {
                            val fr = slide.fromSq / 8
                            val ff = slide.fromSq % 8
                            val tr = slide.toSq / 8
                            val tf = slide.toSq % 8

                            val fromCol = if (flipped) 7 - ff else ff
                            val fromRow = if (flipped) 7 - fr else fr
                            val toCol = if (flipped) 7 - tf else tf
                            val toRow = if (flipped) 7 - tr else tr

                            val curX = lerp(cellDp * fromCol, cellDp * toCol, t)
                            val curY = lerp(cellDp * fromRow, cellDp * toRow, t)

                            // Detached ground shadow tracking below the airborne piece
                            if (is3dBoard && arcLift > 0.02f) {
                                Canvas(
                                    modifier = Modifier
                                        .offset(x = curX, y = curY)
                                        .size(cellDp)
                                ) {
                                    val w = size.width
                                    val h = size.height
                                    val s = 0.65f * (1f - 0.15f * arcLift)
                                    drawOval(
                                        color = Color.Black.copy(alpha = 0.28f * arcLift),
                                        topLeft = Offset(w * (0.52f - s * 0.5f), h * 0.78f),
                                        size = Size(w * s, h * 0.18f)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .offset(x = curX, y = curY + liftYDp)
                                    .size(cellDp),
                                contentAlignment = Alignment.Center
                            ) {
                                ChessPieceIcon(
                                    piece = slide.piece,
                                    pieceStyle = pieceStyle,
                                    uprightTiltDeg = pieceUprightTilt,
                                    shadowAlpha = if (is3dBoard) 1f - 0.65f * arcLift else 1f,
                                    modifier = Modifier
                                        .fillMaxSize(0.88f)
                                        .graphicsLayer {
                                            scaleX = flightScale
                                            scaleY = flightScale
                                        }
                                )
                            }
                        }
                    }

                    // Floating lifted dragged piece (peeks visibly above thumb while touch maps accurately)
                    val activeDragPiece = draggedPiece
                    if (activeDragPiece != null) {
                        val halfCell = cellPx / 2f
                        val maxOffset = (gridPx - cellPx).coerceAtLeast(0f)
                        val pxX = (dragOffsetPx.x - halfCell).coerceIn(0f, maxOffset)
                        val pxY = (dragOffsetPx.y - halfCell).coerceIn(0f, maxOffset)
                        val liftPx = if (is3dBoard) with(density) { 14.dp.toPx() } else (cellPx * 0.16f).coerceAtLeast(6f)

                        if (is3dBoard) {
                            Canvas(
                                modifier = Modifier
                                    .offset { IntOffset(pxX.roundToInt(), pxY.roundToInt()) }
                                    .size(cellDp)
                            ) {
                                drawOval(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.42f),
                                            Color.Transparent
                                        )
                                    ),
                                    topLeft = Offset(size.width * 0.16f, size.height * 0.74f),
                                    size = Size(size.width * 0.72f, size.height * 0.22f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(pxX.roundToInt(), (pxY - liftPx).coerceAtLeast(0f).roundToInt()) }
                                .size(cellDp),
                            contentAlignment = Alignment.Center
                        ) {
                            ChessPieceIcon(
                                piece = activeDragPiece,
                                pieceStyle = pieceStyle,
                                uprightTiltDeg = pieceUprightTilt,
                                shadowAlpha = if (is3dBoard) 0.25f else 1f,
                                modifier = Modifier
                                    .fillMaxSize(0.94f)
                                    .graphicsLayer {
                                        val s = 1.28f
                                        scaleX = s
                                        scaleY = s
                                    }
                            )
                        }
                    }

                    // Arrow overlay
                    if (arrows.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cell = size.width / 8f
                            for (arrow in arrows) {
                                val fromSq = parseSquare(arrow.from) ?: continue
                                val toSq = parseSquare(arrow.to) ?: continue
                                val fr = fromSq / 8
                                val ff = fromSq % 8
                                val tr = toSq / 8
                                val tf = toSq % 8
                                val x1 = ((if (flipped) 7 - ff else ff) + 0.5f) * cell
                                val y1 = ((if (flipped) 7 - fr else fr) + 0.5f) * cell
                                val x2 = ((if (flipped) 7 - tf else tf) + 0.5f) * cell
                                val y2 = ((if (flipped) 7 - tr else tr) + 0.5f) * cell

                                val dx = x2 - x1
                                val dy = y2 - y1
                                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                                if (len < 1f) continue
                                val ux = dx / len
                                val uy = dy / len
                                val headLen = cell * 0.42f
                                val bx = x2 - ux * headLen
                                val by = y2 - uy * headLen

                                drawLine(
                                    color = arrow.color,
                                    start = Offset(x1, y1),
                                    end = Offset(bx, by),
                                    strokeWidth = cell * 0.17f,
                                    cap = StrokeCap.Round
                                )

                                val headWidth = cell * 0.24f
                                val path = Path().apply {
                                    moveTo(x2, y2)
                                    lineTo(bx - uy * headWidth, by + ux * headWidth)
                                    lineTo(bx + uy * headWidth, by - ux * headWidth)
                                    close()
                                }
                                drawPath(path, arrow.color)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionPickerDialog(
    color: PieceColor,
    onSelect: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(DiggySpacing.cardRadius),
            color = CardDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Promote pawn",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        PieceType.QUEEN,
                        PieceType.ROOK,
                        PieceType.BISHOP,
                        PieceType.KNIGHT
                    ).forEach { pt ->
                        Surface(
                            onClick = { onSelect(pt) },
                            shape = RoundedCornerShape(12.dp),
                            color = CardSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("promote_${pt.symbol}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(6.dp)) {
                                ChessPieceIcon(
                                    piece = ChessPiece(color, pt),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
