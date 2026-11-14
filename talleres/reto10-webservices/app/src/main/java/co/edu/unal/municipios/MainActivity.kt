package co.edu.unal.municipios

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.edu.unal.municipios.data.ConsultaMunicipios
import co.edu.unal.municipios.data.Formato
import co.edu.unal.municipios.data.Municipio
import co.edu.unal.municipios.data.Orden
import co.edu.unal.municipios.data.TipoMunicipio
import co.edu.unal.municipios.ui.ListaSinFiltro
import co.edu.unal.municipios.ui.MunicipioAdapter
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

/**
 * Pantalla única de la app: el usuario elige los parámetros (nombre, departamento, tipo y
 * orden), la app llama al servicio web de datos.gov.co con esos parámetros y muestra la
 * respuesta como una lista de municipios.
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var inputNombre: TextInputEditText
    private lateinit var layoutDepartamento: TextInputLayout
    private lateinit var inputDepartamento: MaterialAutoCompleteTextView
    private lateinit var chipsTipo: ChipGroup
    private lateinit var toggleOrden: MaterialButtonToggleGroup
    private lateinit var btnConsultar: MaterialButton
    private lateinit var labelUrl: TextView
    private lateinit var textUrl: TextView
    private lateinit var progreso: LinearProgressIndicator
    private lateinit var textEstado: TextView
    private lateinit var lista: RecyclerView
    private lateinit var appBar: AppBarLayout

    private val adapter = MunicipioAdapter { abrirEnMapa(it) }
    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        inputNombre = findViewById(R.id.input_nombre)
        layoutDepartamento = findViewById(R.id.layout_departamento)
        inputDepartamento = findViewById(R.id.input_departamento)
        chipsTipo = findViewById(R.id.chips_tipo)
        toggleOrden = findViewById(R.id.toggle_orden)
        btnConsultar = findViewById(R.id.btn_consultar)
        labelUrl = findViewById(R.id.label_url)
        textUrl = findViewById(R.id.text_url)
        progreso = findViewById(R.id.progreso)
        textEstado = findViewById(R.id.text_estado)
        lista = findViewById(R.id.lista_municipios)
        appBar = findViewById(R.id.app_bar)

        lista.layoutManager = LinearLayoutManager(this)
        lista.adapter = adapter

        btnConsultar.setOnClickListener { consultar() }
        inputNombre.setOnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_SEARCH) {
                consultar()
                true
            } else {
                false
            }
        }

        observarEstado()
    }

    /** Lee los parámetros de la interfaz y le pide al ViewModel que llame al servicio. */
    private fun consultar() {
        ocultarTeclado()
        // Si la lista de departamentos falló al abrir la app, se aprovecha para reintentarla
        if (viewModel.departamentos.value == MainViewModel.EstadoDepartamentos.Error) {
            viewModel.cargarDepartamentos()
        }

        val tipo = when (chipsTipo.checkedChipId) {
            R.id.chip_tipo_municipio -> TipoMunicipio.MUNICIPIO
            R.id.chip_tipo_isla -> TipoMunicipio.ISLA
            R.id.chip_tipo_anm -> TipoMunicipio.AREA_NO_MUNICIPALIZADA
            else -> TipoMunicipio.TODOS
        }
        val orden = if (toggleOrden.checkedButtonId == R.id.btn_orden_codigo) Orden.CODIGO else Orden.NOMBRE

        viewModel.consultar(
            ConsultaMunicipios(
                nombre = inputNombre.text?.toString().orEmpty().trim(),
                departamento = viewModel.departamentoSeleccionado,
                tipo = tipo,
                orden = orden
            )
        )
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.estado.collect { mostrarEstado(it) } }
                launch { viewModel.departamentos.collect { mostrarDepartamentos(it) } }
            }
        }
    }

    private fun mostrarEstado(estado: MainViewModel.Estado) {
        when (estado) {
            MainViewModel.Estado.Inicial -> {
                progreso.visibility = View.GONE
                textEstado.setText(R.string.instrucciones)
                adapter.submitList(emptyList())
            }

            is MainViewModel.Estado.Cargando -> {
                snackbar?.dismiss()
                mostrarUrl(estado.url)
                progreso.visibility = View.VISIBLE
                btnConsultar.isEnabled = false
                textEstado.setText(R.string.consultando)
            }

            is MainViewModel.Estado.Exito -> {
                mostrarUrl(estado.url)
                progreso.visibility = View.GONE
                btnConsultar.isEnabled = true
                val total = estado.municipios.size
                textEstado.text = if (total == 0) {
                    getString(R.string.sin_resultados)
                } else {
                    resources.getQuantityString(R.plurals.resultados_count, total, total)
                }
                adapter.submitList(estado.municipios) { lista.scrollToPosition(0) }
                appBar.setExpanded(true, true)
            }

            is MainViewModel.Estado.Error -> {
                mostrarUrl(estado.url)
                progreso.visibility = View.GONE
                btnConsultar.isEnabled = true
                textEstado.text = getString(R.string.error_consulta, estado.mensaje)
                adapter.submitList(emptyList())
                snackbar = Snackbar.make(lista, R.string.error_consulta_corto, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.reintentar) { viewModel.reintentar() }
                    .also { it.show() }
            }
        }
    }

    private fun mostrarDepartamentos(estado: MainViewModel.EstadoDepartamentos) {
        when (estado) {
            MainViewModel.EstadoDepartamentos.Cargando -> {
                layoutDepartamento.isEnabled = false
                layoutDepartamento.helperText = getString(R.string.cargando_departamentos)
            }

            MainViewModel.EstadoDepartamentos.Error -> {
                layoutDepartamento.isEnabled = true
                layoutDepartamento.helperText = getString(R.string.error_departamentos)
                inputDepartamento.setAdapter(
                    ListaSinFiltro(this, listOf(getString(R.string.todos_los_departamentos)))
                )
            }

            is MainViewModel.EstadoDepartamentos.Listos -> {
                layoutDepartamento.isEnabled = true
                layoutDepartamento.helperText = null
                val departamentos = estado.departamentos
                val nombres = listOf(getString(R.string.todos_los_departamentos)) +
                    departamentos.map { Formato.titulo(it.nombre) }
                inputDepartamento.setAdapter(ListaSinFiltro(this, nombres))
                inputDepartamento.setOnItemClickListener { _, _, posicion, _ ->
                    viewModel.departamentoSeleccionado =
                        if (posicion == 0) null else departamentos[posicion - 1]
                }
                // Mantiene el texto correcto tras rotar la pantalla
                val elegido = viewModel.departamentoSeleccionado
                inputDepartamento.setText(
                    if (elegido == null) nombres[0] else Formato.titulo(elegido.nombre),
                    false
                )
            }
        }
    }

    private fun mostrarUrl(url: String) {
        labelUrl.visibility = View.VISIBLE
        textUrl.visibility = View.VISIBLE
        textUrl.text = url
    }

    /** Abre el municipio en la app de mapas que tenga instalada el usuario. */
    private fun abrirEnMapa(municipio: Municipio) {
        val lat = municipio.latitud
        val lon = municipio.longitud
        if (lat == null || lon == null) {
            Toast.makeText(this, R.string.sin_coordenadas, Toast.LENGTH_SHORT).show()
            return
        }
        val etiqueta = Uri.encode(Formato.titulo(municipio.nombre))
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon($etiqueta)")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.sin_app_mapas, Toast.LENGTH_SHORT).show()
        }
    }

    private fun ocultarTeclado() {
        inputNombre.clearFocus()
        val imm = getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(inputNombre.windowToken, 0)
    }
}
