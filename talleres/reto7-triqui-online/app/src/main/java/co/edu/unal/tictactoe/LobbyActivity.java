package co.edu.unal.tictactoe;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LobbyActivity extends AppCompatActivity {

    private static final String TAG = "Lobby";
    private static final String EMPTY_BOARD = "         ";  // 9 espacios

    // Referencias a la base de datos
    private DatabaseReference mGamesRef;
    private Query mWaitingQuery;
    private ValueEventListener mGamesListener;

    // Datos de la lista: ids de los juegos y el texto que se muestra
    private final List<String> mGameIds = new ArrayList<>();
    private final List<String> mGameLabels = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;

    private TextView mStatusText;
    private Button mCreateButton;

    // UID anónimo de este dispositivo
    private String mUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        mGamesRef = FirebaseDatabase.getInstance().getReference("games");
        // Solo los juegos que esperan rival
        mWaitingQuery = mGamesRef.orderByChild("status").equalTo("waiting");

        mStatusText = findViewById(R.id.status_text);

        ListView gamesList = findViewById(R.id.games_list);
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mGameLabels);
        gamesList.setAdapter(mAdapter);
        gamesList.setEmptyView(findViewById(R.id.empty_text));
        gamesList.setOnItemClickListener((parent, view, position, id) ->
                onGameSelected(mGameIds.get(position)));

        mCreateButton = findViewById(R.id.create_game_button);
        mCreateButton.setEnabled(false);  // hasta tener sesión
        mCreateButton.setOnClickListener(v -> createGame());

        Button vsComputer = findViewById(R.id.vs_computer_button);
        vsComputer.setOnClickListener(v ->
                startActivity(new Intent(this, AndroidTicTacToeActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        signIn();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Dejar de escuchar cuando la pantalla no está visible
        if (mGamesListener != null) {
            mWaitingQuery.removeEventListener(mGamesListener);
            mGamesListener = null;
        }
    }

    // Inicia sesión anónima (o reutiliza la existente)
    private void signIn() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            onSignedIn(user.getUid());
            return;
        }
        FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().getUser() != null) {
                onSignedIn(task.getResult().getUser().getUid());
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "";
                Log.e(TAG, "Error de autenticación", task.getException());
                mStatusText.setText(getString(R.string.lobby_error, msg));
            }
        });
    }

    private void onSignedIn(String uid) {
        mUid = uid;
        mStatusText.setText(R.string.lobby_connected);
        mCreateButton.setEnabled(true);
        listenForGames();
    }

    // Escucha en tiempo real los juegos en espera
    private void listenForGames() {
        if (mGamesListener != null) return;

        mGamesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mGameIds.clear();
                mGameLabels.clear();

                for (DataSnapshot game : snapshot.getChildren()) {
                    String host = game.child("host").getValue(String.class);
                    String hostName = game.child("hostName").getValue(String.class);
                    Long createdAt = game.child("createdAt").getValue(Long.class);

                    String label = (hostName != null ? hostName : "Jugador");
                    if (createdAt != null) {
                        label += " · " + DateUtils.getRelativeTimeSpanString(createdAt);
                    }
                    if (mUid != null && mUid.equals(host)) {
                        label += " " + getString(R.string.your_game);
                    }

                    // Insertar al inicio: los más recientes primero
                    mGameIds.add(0, game.getKey());
                    mGameLabels.add(0, label);
                }
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Lectura cancelada", error.toException());
                mStatusText.setText(getString(R.string.lobby_error, error.getMessage()));
            }
        };
        mWaitingQuery.addValueEventListener(mGamesListener);
    }

    // Crea un juego nuevo en Firebase con este dispositivo como anfitrión (X)
    private void createGame() {
        DatabaseReference newGame = mGamesRef.push();

        Map<String, Object> game = new HashMap<>();
        game.put("host", mUid);
        game.put("hostName", Build.MODEL);
        game.put("board", EMPTY_BOARD);
        game.put("turn", "X");
        game.put("status", "waiting");
        game.put("winner", 0);
        game.put("starter", "X");   // quién empieza la partida actual
        game.put("scoreX", 0);
        game.put("scoreO", 0);
        game.put("ties", 0);
        game.put("createdAt", ServerValue.TIMESTAMP);

        newGame.setValue(game)
                .addOnSuccessListener(unused -> openGame(newGame.getKey()))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "No se pudo crear el juego", e);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Por ahora solo un aviso; unirse se implementa en el paso 5
    // Intenta unirse al juego elegido con una transacción
    private void onGameSelected(String gameId) {
        mGamesRef.child(gameId).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData current) {
                // Puede llegar vacío la primera vez (sin caché local):
                // devolverlo tal cual hace que Firebase reintente con el dato real
                if (current.getValue() == null) {
                    return Transaction.success(current);
                }

                String host = current.child("host").getValue(String.class);
                String guest = current.child("guest").getValue(String.class);
                String status = current.child("status").getValue(String.class);

                // No unirse a un juego propio, ya ocupado o que no está en espera
                if (mUid.equals(host) || guest != null || !"waiting".equals(status)) {
                    return Transaction.abort();
                }

                current.child("guest").setValue(mUid);
                current.child("guestName").setValue(Build.MODEL);
                current.child("status").setValue("playing");
                return Transaction.success(current);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (error != null) {
                    Log.e(TAG, "Error al unirse", error.toException());
                    Toast.makeText(LobbyActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
                // Entrar si soy el anfitrión (juego propio) o si quedé como invitado
                String host = snapshot != null ? snapshot.child("host").getValue(String.class) : null;
                String guest = snapshot != null ? snapshot.child("guest").getValue(String.class) : null;
                if (mUid.equals(host) || mUid.equals(guest)) {
                    openGame(gameId);
                } else {
                    Toast.makeText(LobbyActivity.this, R.string.game_unavailable,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openGame(String gameId) {
        Intent intent = new Intent(this, OnlineGameActivity.class);
        intent.putExtra(OnlineGameActivity.EXTRA_GAME_ID, gameId);
        startActivity(intent);
    }
}