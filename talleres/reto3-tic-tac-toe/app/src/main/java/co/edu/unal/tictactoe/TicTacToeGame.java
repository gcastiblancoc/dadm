package co.edu.unal.tictactoe;

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

    /**
     * Coloca al jugador dado en la posición indicada.
     * Si la posición no está libre, no hace nada.
     */
    public void setMove(char player, int location) {
        if (location >= 0 && location < BOARD_SIZE && mBoard[location] == OPEN_SPOT) {
            mBoard[location] = player;
        }
    }

    /**
     * Calcula la mejor jugada para el computador:
     * 1) Si puede ganar en esta jugada, gana.
     * 2) Si no, si el humano podría ganar en su próxima jugada, lo bloquea.
     * 3) Si no, hace un movimiento aleatorio disponible.
     */
    public int getComputerMove() {

        // 1. ¿Puede ganar el computador ahora mismo?
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = COMPUTER_PLAYER;
                boolean wins = (checkForWinner() == 3);
                mBoard[i] = OPEN_SPOT; // deshacer la simulación
                if (wins) {
                    return i;
                }
            }
        }

        // 2. ¿El humano ganaría en su próxima jugada? Si sí, bloquear esa casilla.
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = HUMAN_PLAYER;
                boolean humanWins = (checkForWinner() == 2);
                mBoard[i] = OPEN_SPOT; // deshacer la simulación
                if (humanWins) {
                    return i;
                }
            }
        }

        // 3. Ningún movimiento gana ni bloquea: jugar al azar.
        return getRandomMove();
    }

    private int getRandomMove() {
        int move;
        do {
            move = mRand.nextInt(BOARD_SIZE);
        } while (mBoard[move] != OPEN_SPOT);
        return move;
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
}