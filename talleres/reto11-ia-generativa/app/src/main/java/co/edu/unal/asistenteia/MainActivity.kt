package co.edu.unal.asistenteia

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.edu.unal.asistenteia.data.Ajustes
import co.edu.unal.asistenteia.data.GeminiClient
import co.edu.unal.asistenteia.data.GeminiException
import co.edu.unal.asistenteia.data.Mensaje
import co.edu.unal.asistenteia.data.Personalidad
import co.edu.unal.asistenteia.data.Rol
import co.edu.unal.asistenteia.ui.MensajeAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Chatbot con IA generativa: el usuario conversa con Gemini. La "personalidad" elegida es un
 * prompt de sistema predefinido que cambia el rol del modelo (tutor, guía ciclista, cuentacuentos…).
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var lista: RecyclerView
    private lateinit var vacio: View
    private lateinit var vacioEmoji: TextView
    private lateinit var vacioTitulo: TextView
    private lateinit var vacioDescripcion: TextView
    private lateinit var vacioModelo: TextView
    private lateinit var sugerencias: LinearLayout
    private lateinit var inputMensaje: TextInputEditText
    private lateinit var btnEnviar: MaterialButton

    private val adapter = MensajeAdapter(
        alCopiar = { copiar(it) },
        alReintentar = { viewModel.reintentar() },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this) // colores del fondo de pantalla (Android 12+)
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
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        lista = findViewById(R.id.lista)
        vacio = findViewById(R.id.vacio)
        vacioEmoji = findViewById(R.id.vacio_emoji)
        vacioTitulo = findViewById(R.id.vacio_titulo)
        vacioDescripcion = findViewById(R.id.vacio_descripcion)
        vacioModelo = findViewById(R.id.vacio_modelo)
        sugerencias = findViewById(R.id.sugerencias)
        inputMensaje = findViewById(R.id.input_mensaje)
        btnEnviar = findViewById(R.id.btn_enviar)

        lista.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        lista.adapter = adapter

        btnEnviar.setOnClickListener { enviar(inputMensaje.text?.toString().orEmpty()) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.estado.collect { mostrar(it) }
            }
        }

        // Primera vez sin ninguna clave: abrir los ajustes directamente
        if (savedInstanceState == null && viewModel.ajustes.apiKey.isBlank()) {
            mostrarAjustes()
        }
    }

    private fun mostrar(estado: EstadoChat) {
        val p = estado.personalidad
        toolbar.subtitle = "${p.emoji} ${p.titulo}"

        val elementos = if (estado.esperando) {
            estado.mensajes + Mensaje(-1, Rol.ESCRIBIENDO, "")
        } else {
            estado.mensajes
        }
        adapter.submitList(elementos) {
            if (elementos.isNotEmpty()) lista.scrollToPosition(elementos.size - 1)
        }

        val sinMensajes = estado.mensajes.isEmpty()
        vacio.isVisible = sinMensajes
        lista.isVisible = !sinMensajes
        if (sinMensajes) mostrarBienvenida(p)

        btnEnviar.isEnabled = !estado.esperando
    }

    private fun mostrarBienvenida(p: Personalidad) {
        vacioEmoji.text = p.emoji
        vacioTitulo.text = getString(R.string.vacio_saludo, p.titulo)
        vacioDescripcion.text = p.descripcion
        vacioModelo.text = getString(R.string.powered_by, viewModel.ajustes.modelo)
        sugerencias.removeAllViews()
        for (sugerencia in p.sugerencias) {
            val boton = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = sugerencia
                isAllCaps = false
                setOnClickListener { enviar(sugerencia) }
            }
            sugerencias.addView(
                boton,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
        }
    }

    private fun enviar(texto: String) {
        if (texto.isBlank()) return
        if (viewModel.ajustes.apiKey.isBlank()) {
            Toast.makeText(this, R.string.falta_api_key, Toast.LENGTH_SHORT).show()
            mostrarAjustes()
            return
        }
        viewModel.enviar(texto)
        inputMensaje.text?.clear()
    }

    private fun copiar(texto: String) {
        if (texto.isBlank()) return
        val portapapeles = getSystemService(ClipboardManager::class.java)
        portapapeles.setPrimaryClip(ClipData.newPlainText("mensaje", texto))
        Toast.makeText(this, R.string.copiado, Toast.LENGTH_SHORT).show()
    }

    // ----------------------------------------------------------------- menú

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_nuevo_chat -> { viewModel.nuevoChat(); true }
        R.id.action_personalidad -> { elegirPersonalidad(); true }
        R.id.action_ajustes -> { mostrarAjustes(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun elegirPersonalidad() {
        val opciones = Personalidad.entries
        val etiquetas = opciones.map { "${it.emoji}  ${it.titulo}\n${it.descripcion}" }.toTypedArray()
        val actual = opciones.indexOf(viewModel.estado.value.personalidad)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.titulo_personalidad)
            .setSingleChoiceItems(etiquetas, actual) { dialogo, indice ->
                if (indice != actual) {
                    viewModel.cambiarPersonalidad(opciones[indice])
                    Toast.makeText(this, R.string.aviso_personalidad, Toast.LENGTH_SHORT).show()
                }
                dialogo.dismiss()
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    // --------------------------------------------------------------- ajustes

    private fun mostrarAjustes() {
        val ajustes = viewModel.ajustes
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_ajustes, null)
        val inputClave = vista.findViewById<TextInputEditText>(R.id.input_api_key)
        val inputModelo = vista.findViewById<MaterialAutoCompleteTextView>(R.id.input_modelo)
        val btnCargar = vista.findViewById<MaterialButton>(R.id.btn_cargar_modelos)
        val estadoModelos = vista.findViewById<TextView>(R.id.texto_estado_modelos)
        val ayudaClave = vista.findViewById<TextView>(R.id.texto_ayuda_clave)

        inputClave.setText(ajustes.apiKeyPropia)
        if (ajustes.hayClaveDeCompilacion) {
            ayudaClave.text = getString(R.string.ayuda_api_key) + "\n" +
                getString(R.string.ayuda_api_key_compilacion)
        }
        inputModelo.setText(ajustes.modelo)
        inputModelo.setSimpleItems(Ajustes.MODELOS_SUGERIDOS.toTypedArray())

        btnCargar.setOnClickListener {
            val clave = inputClave.text?.toString()?.trim().orEmpty().ifBlank { ajustes.apiKey }
            estadoModelos.isVisible = true
            if (clave.isBlank()) {
                estadoModelos.setText(R.string.falta_clave_para_modelos)
                return@setOnClickListener
            }
            estadoModelos.setText(R.string.cargando_modelos)
            btnCargar.isEnabled = false
            lifecycleScope.launch {
                try {
                    val modelos = GeminiClient.listarModelos(clave)
                    inputModelo.setSimpleItems(modelos.toTypedArray())
                    estadoModelos.text = getString(R.string.modelos_cargados, modelos.size)
                } catch (e: GeminiException) {
                    estadoModelos.text = e.message
                } catch (e: IOException) {
                    estadoModelos.text = "Error de red: ${e.message}"
                } catch (e: org.json.JSONException) {
                    estadoModelos.text = "Respuesta inesperada: ${e.message}"
                } finally {
                    btnCargar.isEnabled = true
                }
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.titulo_ajustes)
            .setView(vista)
            .setPositiveButton(R.string.guardar) { _, _ ->
                ajustes.apiKeyPropia = inputClave.text?.toString().orEmpty()
                ajustes.modelo = inputModelo.text?.toString().orEmpty()
                Toast.makeText(this, R.string.ajustes_guardados, Toast.LENGTH_SHORT).show()
                // refrescar el texto del modelo en la pantalla de bienvenida
                if (viewModel.estado.value.mensajes.isEmpty()) mostrarBienvenida(viewModel.estado.value.personalidad)
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }
}
