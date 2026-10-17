package co.edu.unal.directorioempresas.ui

import android.os.Bundle
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import co.edu.unal.directorioempresas.R
import co.edu.unal.directorioempresas.data.Empresa
import co.edu.unal.directorioempresas.data.EmpresaRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Actividad para la creación y edición de registros de empresas.
 */
class EmpresaFormActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMPRESA_ID = "co.edu.unal.directorioempresas.EXTRA_EMPRESA_ID"
    }

    private lateinit var repository: EmpresaRepository

    private lateinit var tilNombre: TextInputLayout
    private lateinit var etNombre: TextInputEditText
    private lateinit var tilUrl: TextInputLayout
    private lateinit var etUrl: TextInputEditText
    private lateinit var tilTelefono: TextInputLayout
    private lateinit var etTelefono: TextInputEditText
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilProductosServicios: TextInputLayout
    private lateinit var etProductosServicios: TextInputEditText

    private lateinit var cbConsultoria: CheckBox
    private lateinit var cbDesarrolloMedida: CheckBox
    private lateinit var cbFabricaSoftware: CheckBox
    private lateinit var tvErrorClasificacion: TextView

    private lateinit var btnGuardar: Button

    private var empresaId: Long = -1L
    private var esModoEdicion: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_empresa_form)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        repository = EmpresaRepository(this)

        // Inicialización de vistas
        tilNombre = findViewById(R.id.til_nombre)
        etNombre = findViewById(R.id.et_nombre)
        tilUrl = findViewById(R.id.til_url)
        etUrl = findViewById(R.id.et_url)
        tilTelefono = findViewById(R.id.til_telefono)
        etTelefono = findViewById(R.id.et_telefono)
        tilEmail = findViewById(R.id.til_email)
        etEmail = findViewById(R.id.et_email)
        tilProductosServicios = findViewById(R.id.til_productos_servicios)
        etProductosServicios = findViewById(R.id.et_productos_servicios)

        cbConsultoria = findViewById(R.id.cb_consultoria)
        cbDesarrolloMedida = findViewById(R.id.cb_desarrollo_medida)
        cbFabricaSoftware = findViewById(R.id.cb_fabrica_software)
        tvErrorClasificacion = findViewById(R.id.tv_error_clasificacion)

        btnGuardar = findViewById(R.id.btn_guardar)

        if (intent.hasExtra(EXTRA_EMPRESA_ID)) {
            empresaId = intent.getLongExtra(EXTRA_EMPRESA_ID, -1L)
        }

        esModoEdicion = empresaId != -1L

        if (esModoEdicion) {
            supportActionBar?.title = getString(R.string.editar_empresa)
            // Se carga de la BD únicamente si no proviene de un cambio de configuración (savedInstanceState == null)
            if (savedInstanceState == null) {
                val empresa = repository.obtenerPorId(empresaId)
                if (empresa != null) {
                    cargarDatosEmpresa(empresa)
                } else {
                    Toast.makeText(this, getString(R.string.empresa_no_encontrada), Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
            }
        } else {
            supportActionBar?.title = getString(R.string.nueva_empresa)
        }

        btnGuardar.setOnClickListener {
            guardarEmpresa()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (esModoEdicion) {
            menuInflater.inflate(R.menu.menu_empresa_form, menu)
            return true
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                confirmarYEliminarEmpresa()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    /**
     * Muestra el diálogo de confirmación y elimina la empresa si el usuario confirma.
     */
    private fun confirmarYEliminarEmpresa() {
        val empresa = repository.obtenerPorId(empresaId) ?: return
        mostrarConfirmacionEliminar(this, empresa) {
            val filasAfectadas = repository.eliminar(empresaId)
            if (filasAfectadas > 0) {
                Toast.makeText(this, getString(R.string.msg_empresa_eliminada), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, getString(R.string.error_eliminar_empresa), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Llena los campos del formulario con los datos de la empresa recibida.
     */
    private fun cargarDatosEmpresa(empresa: Empresa) {
        etNombre.setText(empresa.nombre)
        etUrl.setText(empresa.url)
        etTelefono.setText(empresa.telefono)
        etEmail.setText(empresa.email)
        etProductosServicios.setText(empresa.productosServicios)
        cbConsultoria.isChecked = empresa.esConsultoria
        cbDesarrolloMedida.isChecked = empresa.esDesarrolloMedida
        cbFabricaSoftware.isChecked = empresa.esFabricaSoftware
    }

    /**
     * Valida los campos ingresados y guarda o actualiza la empresa en la base de datos.
     */
    private fun guardarEmpresa() {
        // Limpiar errores previos
        tilNombre.error = null
        tilUrl.error = null
        tilEmail.error = null
        tvErrorClasificacion.visibility = View.GONE

        // Obtener textos limpios con trim()
        val nombre = etNombre.text?.toString()?.trim().orEmpty()
        val url = etUrl.text?.toString()?.trim().orEmpty()
        val telefono = etTelefono.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val productosServicios = etProductosServicios.text?.toString()?.trim().orEmpty()

        val esConsultoria = cbConsultoria.isChecked
        val esDesarrolloMedida = cbDesarrolloMedida.isChecked
        val esFabricaSoftware = cbFabricaSoftware.isChecked

        var esValido = true

        // Validación: Nombre no vacío
        if (nombre.isEmpty()) {
            tilNombre.error = getString(R.string.error_nombre_obligatorio)
            esValido = false
        }

        // Validación: URL válida si no está vacía
        if (url.isNotEmpty() && !Patterns.WEB_URL.matcher(url).matches()) {
            tilUrl.error = getString(R.string.error_url_invalida)
            esValido = false
        }

        // Validación: Email válido si no está vacío
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(R.string.error_email_invalido)
            esValido = false
        }

        // Validación: Al menos una clasificación seleccionada
        if (!esConsultoria && !esDesarrolloMedida && !esFabricaSoftware) {
            tvErrorClasificacion.visibility = View.VISIBLE
            esValido = false
        }

        if (!esValido) {
            return
        }

        val empresa = Empresa(
            id = if (esModoEdicion) empresaId else 0L,
            nombre = nombre,
            url = url,
            telefono = telefono,
            email = email,
            productosServicios = productosServicios,
            esConsultoria = esConsultoria,
            esDesarrolloMedida = esDesarrolloMedida,
            esFabricaSoftware = esFabricaSoftware
        )

        if (esModoEdicion) {
            repository.actualizar(empresa)
            Toast.makeText(this, getString(R.string.msg_empresa_actualizada), Toast.LENGTH_SHORT).show()
        } else {
            repository.insertar(empresa)
            Toast.makeText(this, getString(R.string.msg_empresa_creada), Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}
