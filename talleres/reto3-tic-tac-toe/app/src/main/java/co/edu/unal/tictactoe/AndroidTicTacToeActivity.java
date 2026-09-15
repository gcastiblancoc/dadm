package co.edu.unal.tictactoe;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AndroidTicTacToeActivity extends AppCompatActivity {

    // Lógica del juego (separada de la UI)
    private TicTacToeGame mGame;

    // Botones que forman el tablero
    private Button mBoardButtons[];

    // Texto de estado (turno / resultado)
    private TextView mInfoTextView;

    // Indica si la partida actual ya terminó
    private boolean mGameOver;

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

        // El humano siempre empieza
        mInfoTextView.setText(R.string.first_human);
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
            if (mGameOver || !mBoardButtons[location].isEnabled()) {
                return;
            }

            // Movimiento del humano
            setMove(TicTacToeGame.HUMAN_PLAYER, location);

            int winner = mGame.checkForWinner();

            if (winner == 0) {
                // Nadie ha ganado todavía: turno del computador
                mInfoTextView.setText(R.string.turn_computer);

                int move = mGame.getComputerMove();
                setMove(TicTacToeGame.COMPUTER_PLAYER, move);
                winner = mGame.checkForWinner();
            }

            switch (winner) {
                case 0:
                    mInfoTextView.setText(R.string.turn_human);
                    break;
                case 1:
                    mInfoTextView.setText(R.string.result_tie);
                    mGameOver = true;
                    disableRemainingButtons();
                    break;
                case 2:
                    mInfoTextView.setText(R.string.result_human_wins);
                    mGameOver = true;
                    disableRemainingButtons();
                    break;
                case 3:
                    mInfoTextView.setText(R.string.result_computer_wins);
                    mGameOver = true;
                    disableRemainingButtons();
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(getString(R.string.menu_new_game));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        startNewGame();
        return true;
    }
}