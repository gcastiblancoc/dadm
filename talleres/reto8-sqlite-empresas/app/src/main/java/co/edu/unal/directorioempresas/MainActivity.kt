package co.edu.unal.directorioempresas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import co.edu.unal.directorioempresas.data.EmpresaRepository
import co.edu.unal.directorioempresas.ui.EmpresaAdapter
import co.edu.unal.directorioempresas.ui.EmpresaFormActivity
import co.edu.unal.directorioempresas.ui.mostrarConfirmacionEliminar
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

/**
 * Pantalla principal que muestra el directorio de empresas con soporte para búsquedas y filtros por clasificación.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var repository: EmpresaRepository
    private lateinit var adapter: EmpresaAdapter

    private lateinit var searchInput: TextInputEditText
    private lateinit var chipConsultoria: Chip
    private lateinit var chipDesarrollo: Chip
    private lateinit var chipFabrica: Chip
    private lateinit var resultsCount: TextView
    private lateinit var emptyText: TextView
    private lateinit var empresasList: RecyclerView
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        repository = EmpresaRepository(this)

        // Inicialización de componentes de la interfaz
        searchInput = findViewById(R.id.search_input)
        chipConsultoria = findViewById(R.id.chip_consultoria)
        chipDesarrollo = findViewById(R.id.chip_desarrollo)
        chipFabrica = findViewById(R.id.chip_fabrica)
        resultsCount = findViewById(R.id.results_count)
        empresasList = findViewById(R.id.empresas_list)
        emptyText = findViewById(R.id.empty_text)
        fabAdd = findViewById(R.id.fab_add)

        adapter = EmpresaAdapter(
            onClick = { empresa ->
                val intent = Intent(this, EmpresaFormActivity::class.java).apply {
                    putExtra(EmpresaFormActivity.EXTRA_EMPRESA_ID, empresa.id)
                }
                startActivity(intent)
            },
            onLongClick = { empresa ->
                mostrarConfirmacionEliminar(this, empresa) {
                    val filasAfectadas = repository.eliminar(empresa.id)
                    if (filasAfectadas > 0) {
                        Toast.makeText(this, getString(R.string.msg_empresa_eliminada), Toast.LENGTH_SHORT).show()
                        cargarEmpresas()
                    } else {
                        Toast.makeText(this, getString(R.string.error_eliminar_empresa), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        empresasList.adapter = adapter

        // Listeners para actualización automática de filtros
        searchInput.doAfterTextChanged {
            cargarEmpresas()
        }

        val onChipCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, _ ->
            cargarEmpresas()
        }
        chipConsultoria.setOnCheckedChangeListener(onChipCheckedChangeListener)
        chipDesarrollo.setOnCheckedChangeListener(onChipCheckedChangeListener)
        chipFabrica.setOnCheckedChangeListener(onChipCheckedChangeListener)

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                ocultarTeclado()
                true
            } else {
                false
            }
        }

        fabAdd.setOnClickListener {
            val intent = Intent(this, EmpresaFormActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarEmpresas()
    }

    /**
     * Consulta las empresas en la base de datos según el texto de búsqueda y los chips de clasificación activos.
     * Actualiza el adaptador, el contador de resultados y el mensaje de lista vacía.
     */
    private fun cargarEmpresas() {
        val textoBusqueda = searchInput.text?.toString()?.trim()
        val consultoria = chipConsultoria.isChecked
        val desarrollo = chipDesarrollo.isChecked
        val fabrica = chipFabrica.isChecked

        val empresas = repository.listar(
            nombre = textoBusqueda,
            consultoria = consultoria,
            desarrolloMedida = desarrollo,
            fabricaSoftware = fabrica
        )

        adapter.submitList(empresas)

        resultsCount.text = resources.getQuantityString(
            R.plurals.empresas_encontradas_count,
            empresas.size,
            empresas.size
        )

        if (empresas.isEmpty()) {
            val hayFiltroActivo = !textoBusqueda.isNullOrEmpty() || consultoria || desarrollo || fabrica
            emptyText.text = if (hayFiltroActivo) {
                getString(R.string.sin_resultados_filtros)
            } else {
                getString(R.string.no_empresas_registradas)
            }
            emptyText.visibility = View.VISIBLE
        } else {
            emptyText.visibility = View.GONE
        }
    }

    /**
     * Oculta el teclado suave.
     */
    private fun ocultarTeclado() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }
}
