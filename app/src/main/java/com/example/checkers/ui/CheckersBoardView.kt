package com.example.checkers.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.checkers.game.CheckersEngine
import com.example.checkers.game.Move
import com.example.checkers.game.PieceColor
import com.example.checkers.game.Position
import kotlin.math.min

class CheckersBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var engine: CheckersEngine? = null
    var selectedPos: Position? = null
    var highlightedMoves: List<Move> = emptyList()
    var onCellTapped: ((Position) -> Unit)? = null

    private val lightSquarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F0D9B5") }
    private val darkSquarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B58863") }
    private val lastMovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#552196F3") }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#88FFEB3B") }
    private val moveDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#AA4CAF50") }
    private val captureDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CCE53935") }

    private val whitePiecePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FAFAFA") }
    private val whiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDBDBD")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val blackPiecePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#263238") }
    private val blackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#607D8B")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val crownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD54F")
        style = Paint.Style.FILL
    }
    private val crownPath = Path()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = if (height == 0) width else min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentEngine = engine ?: return
        val cellSize = width / CheckersEngine.BOARD_SIZE.toFloat()

        // 1. Малюємо клітинки дошки
        for (r in 0 until CheckersEngine.BOARD_SIZE) {
            for (c in 0 until CheckersEngine.BOARD_SIZE) {
                val paint = if ((r + c) % 2 == 0) lightSquarePaint else darkSquarePaint
                canvas.drawRect(
                    c * cellSize,
                    r * cellSize,
                    (c + 1) * cellSize,
                    (r + 1) * cellSize,
                    paint
                )
            }
        }

        // 2. Підсвітка останнього ходу
        currentEngine.lastMove?.let { move ->
            highlightCell(canvas, move.from, cellSize, lastMovePaint)
            highlightCell(canvas, move.to, cellSize, lastMovePaint)
        }

        // 3. Підсвітка обраної шашки
        selectedPos?.let { pos ->
            highlightCell(canvas, pos, cellSize, selectedPaint)
        }

        // 4. Підказки можливих ходів
        for (move in highlightedMoves) {
            val cx = (move.to.col + 0.5f) * cellSize
            val cy = (move.to.row + 0.5f) * cellSize
            val paint = if (move.isCapture) captureDotPaint else moveDotPaint
            canvas.drawCircle(cx, cy, cellSize * 0.2f, paint)
        }

        // 5. Малюємо шашки
        for (r in 0 until CheckersEngine.BOARD_SIZE) {
            for (c in 0 until CheckersEngine.BOARD_SIZE) {
                val piece = currentEngine.board[r][c] ?: continue
                val cx = (c + 0.5f) * cellSize
                val cy = (r + 0.5f) * cellSize
                val radius = cellSize * 0.38f

                if (piece.color == PieceColor.WHITE) {
                    canvas.drawCircle(cx, cy, radius, whitePiecePaint)
                    canvas.drawCircle(cx, cy, radius, whiteBorderPaint)
                } else {
                    canvas.drawCircle(cx, cy, radius, blackPiecePaint)
                    canvas.drawCircle(cx, cy, radius, blackBorderPaint)
                }

                if (piece.isKing) {
                    drawCrown(canvas, cx, cy, radius * 0.55f)
                }
            }
        }
    }

    private fun highlightCell(canvas: Canvas, pos: Position, cellSize: Float, paint: Paint) {
        canvas.drawRect(
            pos.col * cellSize,
            pos.row * cellSize,
            (pos.col + 1) * cellSize,
            (pos.row + 1) * cellSize,
            paint
        )
    }

    private fun drawCrown(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        crownPath.reset()
        crownPath.moveTo(cx - r, cy + r * 0.6f)
        crownPath.lineTo(cx - r, cy - r * 0.4f)
        crownPath.lineTo(cx - r * 0.4f, cy + r * 0.1f)
        crownPath.lineTo(cx, cy - r * 0.7f)
        crownPath.lineTo(cx + r * 0.4f, cy + r * 0.1f)
        crownPath.lineTo(cx + r, cy - r * 0.4f)
        crownPath.lineTo(cx + r, cy + r * 0.6f)
        crownPath.close()
        canvas.drawPath(crownPath, crownPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val cellSize = width / CheckersEngine.BOARD_SIZE.toFloat()
            val col = (event.x / cellSize).toInt()
            val row = (event.y / cellSize).toInt()
            if (row in 0 until CheckersEngine.BOARD_SIZE && col in 0 until CheckersEngine.BOARD_SIZE) {
                onCellTapped?.invoke(Position(row, col))
                performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}