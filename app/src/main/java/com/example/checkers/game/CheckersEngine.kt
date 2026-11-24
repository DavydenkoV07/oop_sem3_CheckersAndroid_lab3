package com.example.checkers.game

import timber.log.Timber

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class GameStatus {
    PLAYING, WHITE_WINS, BLACK_WINS, DRAW
}

data class Piece(
    val color: PieceColor,
    val isKing: Boolean = false
)

data class Position(val row: Int, val col: Int) {
    fun toNotation(): String = "${'A' + col}${8 - row}"
}

data class Move(
    val from: Position,
    val to: Position,
    val captured: Position? = null
) {
    val isCapture: Boolean get() = captured != null
}

class CheckersEngine {

    companion object {
        const val BOARD_SIZE = 8
        private const val DRAW_MOVE_LIMIT = 40
        private val ALL_DIAGONALS = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
    }

    val board: Array<Array<Piece?>> = Array(BOARD_SIZE) { arrayOfNulls<Piece>(BOARD_SIZE) }
    var currentPlayer: PieceColor = PieceColor.WHITE
        private set
    var activeChainPiece: Position? = null
        private set
    var status: GameStatus = GameStatus.PLAYING
        private set
    var lastMove: Move? = null
        private set

    private var quietMovesCount: Int = 0

    init {
        reset(logEvent = false)
    }

    fun reset(logEvent: Boolean = true) {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                board[r][c] = when {
                    (r + c) % 2 == 1 && r in 0..2 -> Piece(PieceColor.BLACK)
                    (r + c) % 2 == 1 && r in 5..7 -> Piece(PieceColor.WHITE)
                    else -> null
                }
            }
        }
        currentPlayer = PieceColor.WHITE
        activeChainPiece = null
        status = GameStatus.PLAYING
        lastMove = null
        quietMovesCount = 0

        if (logEvent) {
            Timber.i("New game started. Initial turn: %s", currentPlayer)
        }
    }

    fun copy(): CheckersEngine {
        val clone = CheckersEngine()
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                clone.board[r][c] = board[r][c]
            }
        }
        clone.currentPlayer = currentPlayer
        clone.activeChainPiece = activeChainPiece
        clone.status = status
        clone.lastMove = lastMove
        clone.quietMovesCount = quietMovesCount
        return clone
    }

    fun countPieces(color: PieceColor): Int {
        var count = 0
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (board[r][c]?.color == color) count++
            }
        }
        return count
    }

    fun hasMandatoryCapture(): Boolean {
        if (status != GameStatus.PLAYING) return false
        return getValidMoves().any { it.isCapture }
    }

    fun getValidMoves(): List<Move> {
        if (status != GameStatus.PLAYING) return emptyList()

        activeChainPiece?.let { pos ->
            return getCapturesForPiece(pos)
        }

        val allCaptures = mutableListOf<Move>()
        val allRegular = mutableListOf<Move>()

        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val piece = board[r][c] ?: continue
                if (piece.color != currentPlayer) continue
                val pos = Position(r, c)
                allCaptures.addAll(getCapturesForPiece(pos))
                if (allCaptures.isEmpty()) {
                    allRegular.addAll(getRegularMovesForPiece(pos))
                }
            }
        }

        return if (allCaptures.isNotEmpty()) allCaptures else allRegular
    }

    fun getValidMovesForPiece(pos: Position): List<Move> {
        return getValidMoves().filter { it.from == pos }
    }

    fun applyMove(move: Move, logEvents: Boolean = true): Boolean {
        if (status != GameStatus.PLAYING) return false
        if (move !in getValidMoves()) {
            if (logEvents) {
                Timber.w("Rejected illegal move: %s -> %s", move.from.toNotation(), move.to.toNotation())
            }
            return false
        }

        val movingPiece = board[move.from.row][move.from.col] ?: return false
        board[move.from.row][move.from.col] = null
        move.captured?.let { cap ->
            board[cap.row][cap.col] = null
        }

        val shouldPromote = !movingPiece.isKing && (
            (movingPiece.color == PieceColor.WHITE && move.to.row == 0) ||
            (movingPiece.color == PieceColor.BLACK && move.to.row == BOARD_SIZE - 1)
        )
        val finalPiece = if (shouldPromote) movingPiece.copy(isKing = true) else movingPiece
        board[move.to.row][move.to.col] = finalPiece
        lastMove = move

        if (move.isCapture || !movingPiece.isKing) {
            quietMovesCount = 0
        } else {
            quietMovesCount++
        }

        if (logEvents) {
            val action = if (move.isCapture) "captured at ${move.captured?.toNotation()}" else "moved"
            Timber.i(
                "%s %s: %s -> %s%s",
                movingPiece.color,
                action,
                move.from.toNotation(),
                move.to.toNotation(),
                if (shouldPromote) " [PROMOTED TO KING]" else ""
            )
        }

        // Перевірка на продовження серійного взяття
        if (move.isCapture && getCapturesForPiece(move.to).isNotEmpty()) {
            activeChainPiece = move.to
            if (logEvents) {
                Timber.d("Multi-jump available for %s at %s", currentPlayer, move.to.toNotation())
            }
        } else {
            activeChainPiece = null
            currentPlayer = currentPlayer.opposite()
        }

        updateGameStatus(logEvents)
        return true
    }

    private fun updateGameStatus(logEvents: Boolean) {
        if (quietMovesCount >= DRAW_MOVE_LIMIT) {
            status = GameStatus.DRAW
            if (logEvents) Timber.i("Game ended in DRAW (move limit reached)")
            return
        }

        if (getValidMoves().isEmpty()) {
            status = if (currentPlayer == PieceColor.WHITE) {
                GameStatus.BLACK_WINS
            } else {
                GameStatus.WHITE_WINS
            }
            if (logEvents) Timber.i("Game over! Result: %s", status)
        }
    }

    private fun getRegularMovesForPiece(pos: Position): List<Move> {
        val piece = board[pos.row][pos.col] ?: return emptyList()
        val moves = mutableListOf<Move>()

        if (!piece.isKing) {
            val forwardRow = if (piece.color == PieceColor.WHITE) -1 else 1
            for (dc in listOf(-1, 1)) {
                val nr = pos.row + forwardRow
                val nc = pos.col + dc
                if (isInBounds(nr, nc) && board[nr][nc] == null) {
                    moves.add(Move(pos, Position(nr, nc)))
                }
            }
        } else {
            for ((dr, dc) in ALL_DIAGONALS) {
                var nr = pos.row + dr
                var nc = pos.col + dc
                while (isInBounds(nr, nc) && board[nr][nc] == null) {
                    moves.add(Move(pos, Position(nr, nc)))
                    nr += dr
                    nc += dc
                }
            }
        }
        return moves
    }

    private fun getCapturesForPiece(pos: Position): List<Move> {
        val piece = board[pos.row][pos.col] ?: return emptyList()
        val enemyColor = piece.color.opposite()
        val captures = mutableListOf<Move>()

        if (!piece.isKing) {
            for ((dr, dc) in ALL_DIAGONALS) {
                val midR = pos.row + dr
                val midC = pos.col + dc
                val landR = pos.row + 2 * dr
                val landC = pos.col + 2 * dc
                if (isInBounds(landR, landC) &&
                    board[midR][midC]?.color == enemyColor &&
                    board[landR][landC] == null
                ) {
                    captures.add(Move(pos, Position(landR, landC), Position(midR, midC)))
                }
            }
        } else {
            for ((dr, dc) in ALL_DIAGONALS) {
                var r = pos.row + dr
                var c = pos.col + dc
                while (isInBounds(r, c) && board[r][c] == null) {
                    r += dr
                    c += dc
                }
                if (!isInBounds(r, c) || board[r][c]?.color != enemyColor) continue

                val enemyPos = Position(r, c)
                var landR = r + dr
                var landC = c + dc
                while (isInBounds(landR, landC) && board[landR][landC] == null) {
                    captures.add(Move(pos, Position(landR, landC), enemyPos))
                    landR += dr
                    landC += dc
                }
            }
        }
        return captures
    }

    private fun isInBounds(row: Int, col: Int): Boolean {
        return row in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE
    }
}