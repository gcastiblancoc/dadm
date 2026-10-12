package co.edu.unal.tictactoe;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class OnlineGameActivity extends AppCompatActivity {

    public static final String EXTRA_GAME_ID = "gameId";
    private static final String TAG = "OnlineGame";
    private static final String EMPTY_BOARD = "         ";  // 9 espacios

    private DatabaseReference mGameRef;
    private ValueEventListener mGameListener;

    private TicTacToeGame mGame;
    private BoardView mBoardView;
    private TextView mRoleText;
    private TextView mScoreText;
    private TextView mInfoText;
    private Button mNewRoundButton;
    private Button mBackButton;

    private String mUid;
    private char mMySymbol = ' ';    // 'X' si soy anfitrión, 'O' si soy invitado

    // Último estado recibido de Firebase
    private String mStatus = "waiting";
    private char mTurn = 'X';
    private String mLastBoard = null;  // para detectar jugadas nuevas y sonar

    // true mientras el servidor tiene programado borrar el juego si me desconecto
    private boolean mDisconnectArmed = false;

    // Sonidos: mi jugada / jugada del rival
    private MediaPlayer mMySound;
    private MediaPlayer mRivalSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_game);

        String gameId = getIntent().getStringExtra(EXTRA_GAME_ID);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (gameId == null || user == null) {
            finish();  // no se puede jugar sin juego o sin sesión
            return;
        }
        mUid = user.getUid();
        mGameRef = FirebaseDatabase.getInstance().getReference("games").child(gameId);

        mRoleText = findViewById(R.id.role_text);
        mScoreText = findViewById(R.id.score_text);
        mInfoText = findViewById(R.id.information);

        mNewRoundButton = findViewById(R.id.new_round_button);
        mNewRoundButton.setOnClickListener(v -> startNewRound());
        mBackButton = findViewById(R.id.back_button);
        mBackButton.setOnClickListener(v -> finish());

        mGame = new TicTacToeGame();
        mBoardView = findViewById(R.id.board);
        mBoardView.setGame(mGame);
        mBoardView.setOnTouchListener(mTouchListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGameRef != null) listenToGame();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGameListener != null) {
            mGameRef.removeEventListener(mGameListener);
            mGameListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMySound = MediaPlayer.create(getApplicationContext(), R.raw.human_move);
        mRivalSound = MediaPlayer.create(getApplicationContext(), R.raw.computer_move);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMySound != null) {
            mMySound.release();
            mMySound = null;
        }
        if (mRivalSound != null) {
            mRivalSound.release();
            mRivalSound = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // isFinishing() distingue "el usuario salió" de "Android recrea por rotación"
        if (isFinishing() && mGameRef != null) {
            leaveGame();
        }
    }

    // Lee un entero de una transacción (0 si no existe)
    private static int intOf(MutableData data, String key) {
        Integer value = data.child(key).getValue(Integer.class);
        return value != null ? value : 0;
    }

    // ---------- Salir del juego ----------

    private void leaveGame() {
        if ("deleted".equals(mStatus)) return;
        final String me = mUid;
        final char mySymbol = mMySymbol;

        mGameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData current) {
                if (current.getValue() == null) {
                    return Transaction.success(current);
                }
                String status = current.child("status").getValue(String.class);
                String host = current.child("host").getValue(String.class);

                if ("waiting".equals(status) && me.equals(host)) {
                    // El anfitrión se va antes de que llegue rival: borrar el juego
                    current.setValue(null);
                } else if ("playing".equals(status)) {
                    // Abandono a mitad de partida: el rival gana el punto
                    String rivalScoreKey = (mySymbol == TicTacToeGame.HUMAN_PLAYER) ? "scoreO" : "scoreX";
                    current.child(rivalScoreKey).setValue(intOf(current, rivalScoreKey) + 1);
                    current.child("status").setValue("abandoned");
                    current.child("leftBy").setValue(me);
                    current.child("abandonedDuringPlay").setValue(true);
                } else if ("finished".equals(status)) {
                    // Sale entre partidas: ya no hay revancha posible
                    current.child("status").setValue("abandoned");
                    current.child("leftBy").setValue(me);
                    current.child("abandonedDuringPlay").setValue(false);
                } else {
                    return Transaction.abort();
                }
                return Transaction.success(current);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) Log.e(TAG, "Error al salir", error.toException());
            }
        });
    }

    // Mientras espero rival, el servidor borra el juego si me desconecto
    private void updateDisconnectHandler() {
        boolean shouldArm = mMySymbol == TicTacToeGame.HUMAN_PLAYER && "waiting".equals(mStatus);
        if (shouldArm && !mDisconnectArmed) {
            mGameRef.onDisconnect().removeValue();
            mDisconnectArmed = true;
        } else if (!shouldArm && mDisconnectArmed) {
            mGameRef.onDisconnect().cancel();
            mDisconnectArmed = false;
        }
    }

    // ---------- Lectura en tiempo real ----------

    private void listenToGame() {
        if (mGameListener != null) return;

        mGameListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(OnlineGameActivity.this, R.string.game_deleted,
                            Toast.LENGTH_LONG).show();
                    mStatus = "deleted";  // para que leaveGame no intente escribir
                    finish();
                    return;
                }
                updateFromSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lectura cancelada", error.toException());
                mInfoText.setText(error.getMessage());
            }
        };
        mGameRef.addValueEventListener(mGameListener);
    }

    // Refleja en pantalla el estado que llegó de Firebase
    private void updateFromSnapshot(DataSnapshot snapshot) {
        String host = snapshot.child("host").getValue(String.class);
        String guest = snapshot.child("guest").getValue(String.class);
        String hostName = snapshot.child("hostName").getValue(String.class);
        String guestName = snapshot.child("guestName").getValue(String.class);
        String board = snapshot.child("board").getValue(String.class);
        String status = snapshot.child("status").getValue(String.class);
        String turn = snapshot.child("turn").getValue(String.class);
        String leftBy = snapshot.child("leftBy").getValue(String.class);
        Boolean duringPlay = snapshot.child("abandonedDuringPlay").getValue(Boolean.class);
        Integer winner = snapshot.child("winner").getValue(Integer.class);
        Integer scoreX = snapshot.child("scoreX").getValue(Integer.class);
        Integer scoreO = snapshot.child("scoreO").getValue(Integer.class);
        Integer ties = snapshot.child("ties").getValue(Integer.class);

        // ¿Quién soy en este juego?
        if (mUid.equals(host)) {
            mMySymbol = TicTacToeGame.HUMAN_PLAYER;      // 'X'
        } else if (mUid.equals(guest)) {
            mMySymbol = TicTacToeGame.COMPUTER_PLAYER;   // 'O'
        }
        mStatus = (status != null) ? status : "waiting";
        mTurn = (turn != null && !turn.isEmpty()) ? turn.charAt(0) : 'X';

        updateDisconnectHandler();

        // Encabezado: mi símbolo y el rival
        String rival = (mMySymbol == TicTacToeGame.HUMAN_PLAYER) ? guestName : hostName;
        String role = getString(R.string.you_are, String.valueOf(mMySymbol));
        if (rival != null) {
            role += " · " + getString(R.string.playing_against, rival);
        }
        mRoleText.setText(role);

        // Marcador desde mi punto de vista
        int x = scoreX != null ? scoreX : 0;
        int o = scoreO != null ? scoreO : 0;
        int t = ties != null ? ties : 0;
        boolean iAmX = mMySymbol == TicTacToeGame.HUMAN_PLAYER;
        mScoreText.setText(getString(R.string.online_score, iAmX ? x : o, t, iAmX ? o : x));

        // Tablero (+ sonido si hay una ficha nueva)
        if (board != null && board.length() == TicTacToeGame.BOARD_SIZE) {
            playSoundForNewMove(board);
            mGame.setBoardState(board.toCharArray());
            mBoardView.invalidate();
        }

        // Mensaje de estado y botones
        boolean showNewRound = false;
        boolean showBack = false;

        if ("waiting".equals(mStatus)) {
            String key = snapshot.getKey();
            String code = (key != null && key.length() >= 4) ? key.substring(key.length() - 4) : "";
            mInfoText.setText(getString(R.string.waiting_rival, code));
        } else if ("playing".equals(mStatus)) {
            if (mTurn == mMySymbol) {
                mInfoText.setText(R.string.your_turn);
            } else {
                mInfoText.setText(getString(R.string.rival_turn, rival != null ? rival : "rival"));
            }
        } else if ("finished".equals(mStatus)) {
            showResult(winner != null ? winner : 0);
            showNewRound = true;
            showBack = true;
        } else if ("abandoned".equals(mStatus)) {
            if (!mUid.equals(leftBy)) {
                mInfoText.setText(Boolean.TRUE.equals(duringPlay)
                        ? R.string.rival_left : R.string.rival_exited);
            }
            showBack = true;
        }
        mNewRoundButton.setVisibility(showNewRound ? View.VISIBLE : View.GONE);
        mBackButton.setVisibility(showBack ? View.VISIBLE : View.GONE);
    }

    private void showResult(int winner) {
        if (winner == 1) {
            mInfoText.setText(R.string.result_tie);
        } else {
            char winnerSymbol = (winner == 2) ? TicTacToeGame.HUMAN_PLAYER : TicTacToeGame.COMPUTER_PLAYER;
            mInfoText.setText(winnerSymbol == mMySymbol
                    ? R.string.result_you_win : R.string.result_you_lose);
        }
    }

    // Compara con el tablero anterior para saber quién acaba de jugar
    private void playSoundForNewMove(String board) {
        if (mLastBoard != null && !board.equals(mLastBoard)) {
            for (int i = 0; i < board.length(); i++) {
                if (mLastBoard.charAt(i) == TicTacToeGame.OPEN_SPOT
                        && board.charAt(i) != TicTacToeGame.OPEN_SPOT) {
                    MediaPlayer sound = (board.charAt(i) == mMySymbol) ? mMySound : mRivalSound;
                    if (sound != null) sound.start();
                    break;
                }
            }
        }
        mLastBoard = board;
    }

    // ---------- Jugadas ----------

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
            v.performClick();  // accesibilidad

            if (!"playing".equals(mStatus)) return false;
            if (mTurn != mMySymbol) {
                Toast.makeText(OnlineGameActivity.this, R.string.not_your_turn,
                        Toast.LENGTH_SHORT).show();
                return false;
            }

            int col = (int) event.getX() / mBoardView.getBoardCellWidth();
            int row = (int) event.getY() / mBoardView.getBoardCellHeight();
            if (col < 0 || col > 2 || row < 0 || row > 2) return false;

            makeMove(row * 3 + col);
            return false;
        }
    };

    // Escribe la jugada con una transacción: valida, pone la ficha,
    // calcula ganador, actualiza el marcador y pasa el turno, todo de una vez
    private void makeMove(final int pos) {
        final char me = mMySymbol;

        mGameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData current) {
                if (current.getValue() == null) {
                    return Transaction.success(current);  // reintentar con el dato real
                }

                String status = current.child("status").getValue(String.class);
                String turn = current.child("turn").getValue(String.class);
                String board = current.child("board").getValue(String.class);

                // Validaciones: juego en curso, mi turno, casilla libre
                if (!"playing".equals(status)
                        || turn == null || turn.isEmpty() || turn.charAt(0) != me
                        || board == null || board.length() != TicTacToeGame.BOARD_SIZE
                        || board.charAt(pos) != TicTacToeGame.OPEN_SPOT) {
                    return Transaction.abort();
                }

                // Poner la ficha
                char[] cells = board.toCharArray();
                cells[pos] = me;

                // Reutilizar la lógica de siempre para saber si hay ganador
                TicTacToeGame checker = new TicTacToeGame();
                checker.setBoardState(cells);
                int winner = checker.checkForWinner();

                current.child("board").setValue(new String(cells));
                current.child("winner").setValue(winner);

                if (winner == 0) {
                    char next = (me == TicTacToeGame.HUMAN_PLAYER)
                            ? TicTacToeGame.COMPUTER_PLAYER : TicTacToeGame.HUMAN_PLAYER;
                    current.child("turn").setValue(String.valueOf(next));
                } else {
                    current.child("status").setValue("finished");
                    // Marcador: se actualiza en la misma operación que la jugada final
                    String key = (winner == 1) ? "ties" : (winner == 2) ? "scoreX" : "scoreO";
                    current.child(key).setValue(intOf(current, key) + 1);
                }
                return Transaction.success(current);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) {
                    Log.e(TAG, "Error al jugar", error.toException());
                    Toast.makeText(OnlineGameActivity.this, error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
                // Si se hizo, la pantalla se actualiza sola por el listener
            }
        });
    }

    // Revancha: limpia el tablero, conserva el marcador y alterna quién empieza
    private void startNewRound() {
        mGameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData current) {
                if (current.getValue() == null) {
                    return Transaction.success(current);
                }
                String status = current.child("status").getValue(String.class);
                if (!"finished".equals(status)) {
                    // Ya la inició el otro jugador, o alguien salió
                    return Transaction.abort();
                }

                String starter = current.child("starter").getValue(String.class);
                String next = "O".equals(starter) ? "X" : "O";

                current.child("board").setValue(EMPTY_BOARD);
                current.child("winner").setValue(0);
                current.child("starter").setValue(next);
                current.child("turn").setValue(next);
                current.child("status").setValue("playing");
                return Transaction.success(current);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) Log.e(TAG, "Error en revancha", error.toException());
            }
        });
    }
}