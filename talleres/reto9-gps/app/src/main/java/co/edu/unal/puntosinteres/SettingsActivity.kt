package co.edu.unal.puntosinteres

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import co.edu.unal.puntosinteres.data.Preferencias
import com.google.android.material.appbar.MaterialToolbar

/**
 * Pantalla de Configuración: radio de búsqueda en km y tipos de puntos de interés.
 * Los valores quedan guardados en las SharedPreferences por defecto de la app.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contenedor_preferencias, PreferenciasFragment())
                .commit()
        }
    }

    // Volver sin recrear MainActivity, para no perder el mapa ni la ubicación
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class PreferenciasFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferencias, rootKey)

            // Muestra como resumen las categorías elegidas
            findPreference<MultiSelectListPreference>(Preferencias.CLAVE_CATEGORIAS)?.summaryProvider =
                Preference.SummaryProvider<MultiSelectListPreference> { pref ->
                    val seleccion = pref.values
                    val nombres = pref.entryValues.indices
                        .filter { pref.entryValues[it].toString() in seleccion }
                        .map { pref.entries[it] }
                    if (nombres.isEmpty()) getString(R.string.pref_categorias_ninguna)
                    else nombres.joinToString(", ")
                }
        }
    }
}
