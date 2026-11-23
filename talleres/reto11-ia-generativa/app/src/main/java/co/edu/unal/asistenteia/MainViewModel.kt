package co.edu.unal.asistenteia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import co.edu.unal.asistenteia.data.Ajustes
import co.edu.unal.asistenteia.data.GeminiClient
import co.edu.unal.asistenteia.data.GeminiException
import co.edu.unal.asistenteia.data.Mensaje
import co.edu.unal.asistenteia.data.Personalidad
import co.edu.unal.asistenteia.data.Rol
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class EstadoChat(
    val personalidad: Personalidad,
    val mensajes: List<Mensaje> = emptyList(),
    val esperando: Boolean = false,
)

/** Guarda la conversación (sobrevive a la rotación) y habla con Gemini. */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    val ajustes = Ajustes(app)

    private val _estado = MutableStateFlow(EstadoChat(ajustes.personalidad))
    val estado: StateFlow<EstadoChat> = _estado.asStateFlow()

    private var siguienteId = 1L
    private var trabajo: Job? = null

    fun enviar(texto: String) {
        val limpio = texto.trim()
        if (limpio.isEmpty() || _estado.value.esperando) return
        _estado.update { actual ->
            actual.copy(
                // Los errores anteriores no forman parte de la conversación
                mensajes = actual.mensajes.filter { it.rol != Rol.ERROR } +
                    Mensaje(siguienteId++, Rol.USUARIO, limpio),
                esperando = true,
            )
        }
        pedirRespuesta()
    }

    /** Vuelve a pedir la respuesta al último mensaje del usuario (tras un error). */
    fun reintentar() {
        if (_estado.value.esperando) return
        val sinErrores = _estado.value.mensajes.filter { it.rol != Rol.ERROR }
        if (sinErrores.lastOrNull()?.rol != Rol.USUARIO) return
        _estado.update { it.copy(mensajes = sinErrores, esperando = true) }
        pedirRespuesta()
    }

    fun nuevoChat() {
        trabajo?.cancel()
        _estado.value = EstadoChat(_estado.value.personalidad)
    }

    fun cambiarPersonalidad(personalidad: Personalidad) {
        ajustes.personalidad = personalidad
        trabajo?.cancel()
        _estado.value = EstadoChat(personalidad)
    }

    private fun pedirRespuesta() {
        val historial = _estado.value.mensajes
        val personalidad = _estado.value.personalidad
        val modelo = ajustes.modelo
        val apiKey = ajustes.apiKey

        trabajo = viewModelScope.launch {
            val nuevo = try {
                if (apiKey.isBlank()) throw GeminiException(0, "Falta la API key de Gemini. Ábrela en ⋮ → Ajustes.")
                val r = GeminiClient.generar(apiKey, modelo, personalidad.instruccionSistema, historial)
                val detalle = listOfNotNull(r.modeloUsado ?: modelo, r.tokens?.let { "$it tokens" })
                    .joinToString(" · ")
                Mensaje(siguienteId++, Rol.MODELO, r.texto, detalle)
            } catch (e: CancellationException) {
                throw e
            } catch (e: GeminiException) {
                Mensaje(siguienteId++, Rol.ERROR, e.message ?: "Error desconocido")
            } catch (e: UnknownHostException) {
                Mensaje(siguienteId++, Rol.ERROR, "Sin conexión a internet.")
            } catch (e: SocketTimeoutException) {
                Mensaje(siguienteId++, Rol.ERROR, "Gemini tardó demasiado en responder. Intenta de nuevo.")
            } catch (e: IOException) {
                Mensaje(siguienteId++, Rol.ERROR, "Error de red: ${e.message}")
            } catch (e: JSONException) {
                Mensaje(siguienteId++, Rol.ERROR, "No se pudo leer la respuesta de Gemini: ${e.message}")
            } catch (e: Exception) {
                Mensaje(siguienteId++, Rol.ERROR, "Error inesperado: ${e.message}")
            }
            _estado.update { it.copy(mensajes = it.mensajes + nuevo, esperando = false) }
        }
    }
}
