package com.example.checkers

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.checkers.game.CheckersAI
import com.example.checkers.game.CheckersEngine
import com.example.checkers.game.GameStatus
import com.example.checkers.game.PieceColor
import com.example.checkers.game.Position
import com.example.checkers.ui.CheckersBoardView
import timber.log.Timber
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var engine: CheckersEngine
    private val ai = CheckersAI(maxDepth = 4)

    private lateinit var boardView: CheckersBoardView
    private lateinit var tvStatus: TextView
    private lateinit var tvScore: TextView
    private lateinit var btnNewGame: Button
    private lateinit var btnLanguage: Button

    private val aiExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isAiThinking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        engine = (application as CheckersApp).engine

        boardView = findViewById(R.id.boardView)
        tvStatus = findViewById(R.id.tvStatus)
        tvScore = findViewById(R.id.tvScore)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnLanguage = findViewById(R.id.btnLanguage)

        boardView.engine = engine
        boardView.onCellTapped = { pos -> handleCellTap(pos) }

        btnNewGame.setOnClickListener {
            mainHandler.removeCallbacksAndMessages(null)
            isAiThinking = false
            engine.reset()
            clearSelection()
            refreshUi()
        }

        btnLanguage.setOnClickListener { toggleLanguage() }

        Timber.d("MainActivity created. Current locale: %s", resources.configuration.locales[0].toLanguageTag())
        refreshUi()

        if (engine.status == GameStatus.PLAYING && engine.currentPlayer == PieceColor.BLACK) {
            scheduleAiMove()
        }
    }

    private fun handleCellTap(pos: Position) {
        if (isAiThinking || engine.status != GameStatus.PLAYING || engine.currentPlayer != PieceColor.WHITE) {
            return
        }

        // Якщо клікнули по підсвіченій клітинці — виконуємо хід
        val matchingMove = boardView.highlightedMoves.firstOrNull { it.to == pos }
        if (matchingMove != null) {
            engine.applyMove(matchingMove)
            if (engine.activeChainPiece != null) {
                boardView.selectedPos = engine.activeChainPiece
                boardView.highlightedMoves = engine.getValidMovesForPiece(engine.activeChainPiece!!)
            } else {
                clearSelection()
            }
            refreshUi()

            if (engine.status == GameStatus.PLAYING && engine.currentPlayer == PieceColor.BLACK) {
                scheduleAiMove()
            }
            return
        }

        // Інакше намагаємось вибрати шашку людини
        val piece = engine.board[pos.row][pos.col]
        if (piece != null && piece.color == PieceColor.WHITE) {
            val movesForPiece = engine.getValidMovesForPiece(pos)
            if (movesForPiece.isNotEmpty()) {
                boardView.selectedPos = pos
                boardView.highlightedMoves = movesForPiece
                Timber.d("Selected piece at %s (%d valid moves)", pos.toNotation(), movesForPiece.size)
            } else if (engine.hasMandatoryCapture()) {
                Timber.d("Piece at %s cannot move because another capture is mandatory", pos.toNotation())
                Toast.makeText(this, R.string.mandatory_capture_hint, Toast.LENGTH_SHORT).show()
            }
            boardView.invalidate()
        } else {
            clearSelection()
            boardView.invalidate()
        }
    }

    private fun scheduleAiMove() {
        isAiThinking = true
        refreshUi()

        mainHandler.postDelayed({
            val snapshot = engine.copy()
            aiExecutor.execute {
                val move = ai.chooseMove(snapshot)
                mainHandler.post {
                    if (isFinishing || isDestroyed) return@post
                    if (move != null && engine.currentPlayer == PieceColor.BLACK) {
                        engine.applyMove(move)
                    }
                    isAiThinking = false
                    refreshUi()

                    // Якщо у комп'ютера серійне взяття — робимо наступний крок із короткою паузою
                    if (engine.status == GameStatus.PLAYING && engine.currentPlayer == PieceColor.BLACK) {
                        scheduleAiMove()
                    }
                }
            }
        }, 350L)
    }

    private fun toggleLanguage() {
        val currentLang = resources.configuration.locales[0].language
        val targetLang = if (currentLang == "uk") "en" else "uk"
        Timber.i("Switching app language: %s -> %s", currentLang, targetLang)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(targetLang))
    }

    private fun clearSelection() {
        boardView.selectedPos = null
        boardView.highlightedMoves = emptyList()
    }

    private fun refreshUi() {
        val whiteCount = engine.countPieces(PieceColor.WHITE)
        val blackCount = engine.countPieces(PieceColor.BLACK)
        tvScore.text = getString(R.string.pieces_score, whiteCount, blackCount)

        tvStatus.text = when (engine.status) {
            GameStatus.WHITE_WINS -> getString(R.string.status_human_wins)
            GameStatus.BLACK_WINS -> getString(R.string.status_ai_wins)
            GameStatus.DRAW -> getString(R.string.status_draw)
            GameStatus.PLAYING -> when {
                engine.currentPlayer == PieceColor.BLACK -> getString(R.string.turn_ai)
                engine.activeChainPiece != null -> getString(R.string.turn_continue_capture)
                else -> getString(R.string.turn_human)
            }
        }
        boardView.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        aiExecutor.shutdownNow()
    }
}