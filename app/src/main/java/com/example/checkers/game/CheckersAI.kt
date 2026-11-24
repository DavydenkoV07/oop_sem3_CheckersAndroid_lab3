package com.example.checkers.game

import timber.log.Timber
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class CheckersAI(private val maxDepth: Int = 4) {

    fun chooseMove(engine: CheckersEngine): Move? {
        val validMoves = engine.getValidMoves()
        if (validMoves.isEmpty()) return null
        if (validMoves.size == 1) {
            Timber.d("AI has only one legal move: %s -> %s", validMoves[0].from.toNotation(), validMoves[0].to.toNotation())
            return validMoves[0]
        }

        val startTime = System.currentTimeMillis()
        var bestScore = Int.MIN_VALUE
        val bestMoves = mutableListOf<Move>()

        for (move in validMoves) {
            val nextState = engine.copy()
            nextState.applyMove(move, logEvents = false)

            val score = minimax(
                state = nextState,
                depth = maxDepth - 1,
                alpha = Int.MIN_VALUE + 1,
                beta = Int.MAX_VALUE - 1
            )

            if (score > bestScore) {
                bestScore = score
                bestMoves.clear()
                bestMoves.add(move)
            } else if (score == bestScore) {
                bestMoves.add(move)
            }
        }

        val chosen = bestMoves[Random.nextInt(bestMoves.size)]
        val elapsedMs = System.currentTimeMillis() - startTime
        Timber.i(
            "AI chose %s -> %s (score=%d, candidates=%d, time=%dms)",
            chosen.from.toNotation(),
            chosen.to.toNotation(),
            bestScore,
            validMoves.size,
            elapsedMs
        )
        return chosen
    }

    private fun minimax(
        state: CheckersEngine,
        depth: Int,
        alpha: Int,
        beta: Int
    ): Int {
        if (depth == 0 || state.status != GameStatus.PLAYING) {
            return evaluate(state, depth)
        }

        val moves = state.getValidMoves()
        if (moves.isEmpty()) {
            return evaluate(state, depth)
        }

        val isMaximizing = state.currentPlayer == PieceColor.BLACK
        var currentAlpha = alpha
        var currentBeta = beta

        return if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in moves) {
                val next = state.copy()
                next.applyMove(move, logEvents = false)
                val eval = minimax(next, depth - 1, currentAlpha, currentBeta)
                maxEval = max(maxEval, eval)
                currentAlpha = max(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in moves) {
                val next = state.copy()
                next.applyMove(move, logEvents = false)
                val eval = minimax(next, depth - 1, currentAlpha, currentBeta)
                minEval = min(minEval, eval)
                currentBeta = min(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            minEval
        }
    }

    private fun evaluate(state: CheckersEngine, remainingDepth: Int): Int {
        when (state.status) {
            GameStatus.BLACK_WINS -> return 10_000 + remainingDepth
            GameStatus.WHITE_WINS -> return -10_000 - remainingDepth
            GameStatus.DRAW -> return 0
            GameStatus.PLAYING -> Unit
        }

        var score = 0
        for (r in 0 until CheckersEngine.BOARD_SIZE) {
            for (c in 0 until CheckersEngine.BOARD_SIZE) {
                val piece = state.board[r][c] ?: continue
                var value = if (piece.isKing) 320 else 100

                // Бонус за просування вперед
                if (!piece.isKing) {
                    value += if (piece.color == PieceColor.BLACK) r * 6 else (7 - r) * 6
                }
                // Бонус за контроль центру
                if (r in 2..5 && c in 2..5) {
                    value += 12
                }
                // Захист власної задньої лінії
                if ((piece.color == PieceColor.BLACK && r == 0) ||
                    (piece.color == PieceColor.WHITE && r == 7)
                ) {
                    value += 10
                }

                score += if (piece.color == PieceColor.BLACK) value else -value
            }
        }
        return score
    }
}