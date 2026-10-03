package co.edu.unal.tictactoe;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class AndroidTicTacToeActivity extends AppCompatActivity {

    // Tiempo que "piensa" el computador antes de jugar
    private static final long COMPUTER_DELAY_MS = 1000;

    // Lógica del juego (separada de la UI)
    private TicTacToeGame mGame;

    // Vista personalizada que dibuja el tablero
    private BoardView mBoardView;

    // Texto de estado (turno / resultado)
    private TextView mInfoTextView;

    private int mHumanWins = 0;
    private int mComputerWins = 0;
    private int mTies = 0;

    private boolean mHumanGoesFirst = true;  // la 1.ª partida empieza el humano
    private boolean mGameOver = false;

    // true mientras el computador está esperando para jugar
    private boolean mComputerThinking = false;

    // Handler del hilo principal para programar la jugada del computador
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private TextView mHumanScoreTextView;
    private TextView mTieScoreTextView;
    private TextView mComputerScoreTextView;

    // Reproductores de los efectos de sonido
    private MediaPlayer mHumanMediaPlayer;
    private MediaPlayer mComputerMediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInfoTextView = findViewById(R.id.information);

        mHumanScoreTextView = findViewById(R.id.human_score);
        mTieScoreTextView = findViewById(R.id.tie_score);
        mComputerScoreTextView = findViewById(R.id.computer_score);

        mGame = new TicTacToeGame();

        mBoardView = findViewById(R.id.board);
        mBoardView.setGame(mGame);

        // Escuchar toques sobre el tablero
        mBoardView.setOnTouchListener(mTouchListener);

        startNewGame();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHumanMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.human_move);
        mComputerMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.computer_move);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHumanMediaPlayer != null) {
            mHumanMediaPlayer.release();
            mHumanMediaPlayer = null;
        }
        if (mComputerMediaPlayer != null) {
            mComputerMediaPlayer.release();
            mComputerMediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);  // cancelar jugadas pendientes
    }

    // Prepara el tablero para una nueva partida
    private void startNewGame() {
        // Si el computador tenía una jugada programada de la partida anterior, cancelarla
        mHandler.removeCallbacksAndMessages(null);
        mComputerThinking = false;

        mGame.clearBoard();
        mBoardView.invalidate();   // Redibujar el tablero vacío
        mGameOver = false;

        if (mHumanGoesFirst) {
            mInfoTextView.setText(R.string.first_human);
        } else {
            mInfoTextView.setText(R.string.first_computer);
            scheduleComputerMove();
        }

        mHumanGoesFirst = !mHumanGoesFirst;  // la próxima partida empieza el otro
    }

    // Coloca la jugada, redibuja el tablero y reproduce el sonido si fue válida
    private boolean setMove(char player, int location) {
        if (mGame.setMove(player, location)) {
            mBoardView.invalidate();   // Redibujar el tablero

            MediaPlayer sound = (player == TicTacToeGame.HUMAN_PLAYER)
                    ? mHumanMediaPlayer : mComputerMediaPlayer;
            if (sound != null) {
                sound.start();  // Reproducir el efecto
            }
            return true;
        }
        return false;
    }

    // Programa la jugada del computador para dentro de COMPUTER_DELAY_MS
    private void scheduleComputerMove() {
        mComputerThinking = true;

        mHandler.postDelayed(() -> {
            int move = mGame.getComputerMove();
            setMove(TicTacToeGame.COMPUTER_PLAYER, move);
            mComputerThinking = false;

            int winner = mGame.checkForWinner();
            if (winner == 0) {
                mInfoTextView.setText(R.string.turn_human);
            } else {
                endGame(winner);
            }
        }, COMPUTER_DELAY_MS);
    }

    // Maneja los toques sobre el tablero
    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
            v.performClick();  // accesibilidad

            // Ignorar toques si la partida terminó o si es turno del computador
            if (mGameOver || mComputerThinking) return false;

            // Determinar qué celda se tocó
            int col = (int) event.getX() / mBoardView.getBoardCellWidth();
            int row = (int) event.getY() / mBoardView.getBoardCellHeight();
            if (col < 0 || col > 2 || row < 0 || row > 2) return false;
            int pos = row * 3 + col;

            if (setMove(TicTacToeGame.HUMAN_PLAYER, pos)) {
                int winner = mGame.checkForWinner();

                if (winner == 0) {
                    mInfoTextView.setText(R.string.turn_computer);
                    scheduleComputerMove();
                } else {
                    endGame(winner);
                }
            }

            // Así no nos notifican los eventos siguientes (mover / levantar el dedo)
            return false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.new_game) {
            startNewGame();
            return true;
        } else if (id == R.id.ai_difficulty) {
            showDifficultyDialog();
            return true;
        } else if (id == R.id.quit) {
            showQuitDialog();
            return true;
        } else if (id == R.id.about) {
            showAboutDialog();
            return true;
        }

        return false;
    }

    private void showAboutDialog() {
        View layout = getLayoutInflater().inflate(R.layout.about_dialog, null);

        new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    private void showDifficultyDialog() {
        final CharSequence[] levels = {
                getString(R.string.difficulty_easy),
                getString(R.string.difficulty_harder),
                getString(R.string.difficulty_expert)
        };

        int selected = mGame.getDifficultyLevel().ordinal();

        new AlertDialog.Builder(this)
                .setTitle(R.string.difficulty_choose)
                .setSingleChoiceItems(levels, selected, (dialog, item) -> {
                    dialog.dismiss();
                    mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[item]);
                    Toast.makeText(getApplicationContext(), levels[item], Toast.LENGTH_SHORT).show();
                })
                .create()
                .show();
    }

    private void showQuitDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.quit_question)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, id) -> AndroidTicTacToeActivity.this.finish())
                .setNegativeButton(R.string.no, null)
                .create()
                .show();
    }

    private void updateScores() {
        mHumanScoreTextView.setText(getString(R.string.score_human, mHumanWins));
        mTieScoreTextView.setText(getString(R.string.score_ties, mTies));
        mComputerScoreTextView.setText(getString(R.string.score_computer, mComputerWins));
    }

    private void endGame(int winner) {
        mGameOver = true;  // evita contar dos veces la misma partida

        if (winner == 1) {
            mTies++;
            mInfoTextView.setText(R.string.result_tie);
        } else if (winner == 2) {
            mHumanWins++;
            mInfoTextView.setText(R.string.result_human_wins);
        } else {
            mComputerWins++;
            mInfoTextView.setText(R.string.result_computer_wins);
        }
        updateScores();
    }
}