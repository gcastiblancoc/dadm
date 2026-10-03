package co.edu.unal.tictactoe;

import android.graphics.Canvas;
import android.graphics.Color;

import java.util.Random;

public class TicTacToeGame {

    // Caracteres para representar al humano, al computador y las casillas libres
    public static final char HUMAN_PLAYER = 'X';
    public static final char COMPUTER_PLAYER = 'O';
    public static final char OPEN_SPOT = ' ';
    public static final int BOARD_SIZE = 9;

    // Representación interna del tablero
    private char[] mBoard = new char[BOARD_SIZE];

    private Random mRand;

    public TicTacToeGame() {
        mRand = new Random();
        clearBoard();
    }

    /** Limpia el tablero dejando todas las casillas en OPEN_SPOT. */
    public void clearBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            mBoard[i] = OPEN_SPOT;
        }
    }

    public char getBoardOccupant(int location) {
        return mBoard[location];
    }

    /**
     * Coloca al jugador dado en la posición indicada.
     * Si la posición no está libre, no hace nada.
     */
    public boolean setMove(char player, int location) {
        if (location >= 0 && location < BOARD_SIZE && mBoard[location] == OPEN_SPOT) {
            mBoard[location] = player;
            return true;
        }
        return false;
    }

    /**
     * Calcula la mejor jugada para el computador:
     * 1) Si puede ganar en esta jugada, gana.
     * 2) Si no, si el humano podría ganar en su próxima jugada, lo bloquea.
     * 3) Si no, hace un movimiento aleatorio disponible.
     */
    public int getComputerMove() {
        int move = -1;

        if (mDifficultyLevel == DifficultyLevel.Easy) {
            move = getRandomMove();
        } else if (mDifficultyLevel == DifficultyLevel.Harder) {
            move = getWinningMove();
            if (move == -1)
                move = getRandomMove();
        } else if (mDifficultyLevel == DifficultyLevel.Expert) {
            // Ganar; si no se puede, bloquear; si no, aleatorio
            move = getWinningMove();
            if (move == -1)
                move = getBlockingMove();
            if (move == -1)
                move = getRandomMove();
        }
        return move;
    }

    private int getRandomMove() {
        // Recolecta las casillas libres y elige una
        java.util.List<Integer> open = new java.util.ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) open.add(i);
        }
        if (open.isEmpty()) return -1;
        return open.get(mRand.nextInt(open.size()));
    }

    private int getWinningMove() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = COMPUTER_PLAYER;              // prueba temporal
                boolean wins = (checkForWinner() == 3);
                mBoard[i] = OPEN_SPOT;                    // deja el tablero como estaba
                if (wins) return i;
            }
        }
        return -1;
    }

    private int getBlockingMove() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = HUMAN_PLAYER;                 // ¿ganaría el humano aquí?
                boolean humanWins = (checkForWinner() == 2);
                mBoard[i] = OPEN_SPOT;
                if (humanWins) return i;
            }
        }
        return -1;
    }

    /**
     * Revisa si hay ganador.
     * @return 0 si no hay ganador ni empate todavía, 1 si es empate,
     *         2 si ganó X (humano), 3 si ganó O (computador).
     */
    public int checkForWinner() {
        int[][] winningLines = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // filas
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // columnas
                {0, 4, 8}, {2, 4, 6}             // diagonales
        };

        for (int[] line : winningLines) {
            char a = mBoard[line[0]];
            char b = mBoard[line[1]];
            char c = mBoard[line[2]];
            if (a != OPEN_SPOT && a == b && b == c) {
                return (a == HUMAN_PLAYER) ? 2 : 3;
            }
        }

        // Sin ganador: revisar si quedan casillas libres
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) {
                return 0; // el juego sigue
            }
        }

        return 1; // tablero lleno sin ganador -> empate
    }

    // Niveles de dificultad del computador
    public enum DifficultyLevel { Easy, Harder, Expert };

    // Nivel actual
    private DifficultyLevel mDifficultyLevel = DifficultyLevel.Expert;

    public DifficultyLevel getDifficultyLevel() {
        return mDifficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        mDifficultyLevel = difficultyLevel;
    }
}