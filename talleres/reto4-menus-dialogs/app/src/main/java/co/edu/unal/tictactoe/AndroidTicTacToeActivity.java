package co.edu.unal.tictactoe;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class AndroidTicTacToeActivity extends AppCompatActivity {

    // Lógica del juego (separada de la UI)
    private TicTacToeGame mGame;

    // Botones que forman el tablero
    private Button mBoardButtons[];

    // Texto de estado (turno / resultado)
    private TextView mInfoTextView;

    // Indica si la partida actual ya terminó
    // private boolean mGameOver;

    private int mHumanWins = 0;
    private int mComputerWins = 0;
    private int mTies = 0;

    private boolean mHumanGoesFirst = true;  // la 1.ª partida empieza el humano
    private boolean mGameOver = false;

    private TextView mHumanScoreTextView;
    private TextView mTieScoreTextView;
    private TextView mComputerScoreTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBoardButtons = new Button[TicTacToeGame.BOARD_SIZE];
        mBoardButtons[0] = findViewById(R.id.one);
        mBoardButtons[1] = findViewById(R.id.two);
        mBoardButtons[2] = findViewById(R.id.three);
        mBoardButtons[3] = findViewById(R.id.four);
        mBoardButtons[4] = findViewById(R.id.five);
        mBoardButtons[5] = findViewById(R.id.six);
        mBoardButtons[6] = findViewById(R.id.seven);
        mBoardButtons[7] = findViewById(R.id.eight);
        mBoardButtons[8] = findViewById(R.id.nine);

        mInfoTextView = findViewById(R.id.information);

        mHumanScoreTextView = findViewById(R.id.human_score);
        mTieScoreTextView = findViewById(R.id.tie_score);
        mComputerScoreTextView = findViewById(R.id.computer_score);

        mGame = new TicTacToeGame();

        startNewGame();
    }

    // Prepara el tablero para una nueva partida
    private void startNewGame() {
        mGame.clearBoard();
        mGameOver = false;

        for (int i = 0; i < mBoardButtons.length; i++) {
            mBoardButtons[i].setText("");
            mBoardButtons[i].setEnabled(true);
            mBoardButtons[i].setOnClickListener(new ButtonClickListener(i));
        }

        if (mHumanGoesFirst) {
            mInfoTextView.setText(R.string.first_human);
        } else {
            int move = mGame.getComputerMove();
            setMove(TicTacToeGame.COMPUTER_PLAYER, move);
            mInfoTextView.setText(R.string.first_computer);
        }

        mHumanGoesFirst = !mHumanGoesFirst;  // la próxima partida empieza el otro
    }

    // Coloca la jugada en el modelo y actualiza el botón correspondiente
    private void setMove(char player, int location) {
        mGame.setMove(player, location);
        mBoardButtons[location].setEnabled(false);
        mBoardButtons[location].setText(String.valueOf(player));

        if (player == TicTacToeGame.HUMAN_PLAYER) {
            mBoardButtons[location].setTextColor(Color.rgb(0, 200, 0)); // verde
        } else {
            mBoardButtons[location].setTextColor(Color.rgb(200, 0, 0)); // rojo
        }
    }

    // Deshabilita todos los botones restantes (partida terminada)
    private void disableRemainingButtons() {
        for (Button button : mBoardButtons) {
            if (button.isEnabled()) {
                button.setEnabled(false);
            }
        }
    }

    // Maneja los clics sobre las casillas del tablero
    private class ButtonClickListener implements View.OnClickListener {
        int location;

        public ButtonClickListener(int location) {
            this.location = location;
        }

        @Override
        public void onClick(View view) {
            if (mGameOver || !mBoardButtons[location].isEnabled()) return;

            setMove(TicTacToeGame.HUMAN_PLAYER, location);
            int winner = mGame.checkForWinner();

            if (winner == 0) {
                mInfoTextView.setText(R.string.turn_computer);
                int move = mGame.getComputerMove();
                setMove(TicTacToeGame.COMPUTER_PLAYER, move);
                winner = mGame.checkForWinner();
            }

            if (winner == 0) {
                mInfoTextView.setText(R.string.turn_human);
            } else {
                endGame(winner);
            }
        }
    }

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

        // TODO 1: el radio button seleccionado inicialmente = nivel actual
        int selected = mGame.getDifficultyLevel().ordinal();

        new AlertDialog.Builder(this)
                .setTitle(R.string.difficulty_choose)
                .setSingleChoiceItems(levels, selected, (dialog, item) -> {
                    dialog.dismiss();

                    // TODO 2: fijar el nivel segun el item elegido (0, 1, 2)
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