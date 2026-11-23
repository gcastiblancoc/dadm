package co.edu.unal.asistenteia.data

/** Quién escribió cada burbuja del chat. */
enum class Rol { USUARIO, MODELO, ERROR, ESCRIBIENDO }

/**
 * Un mensaje de la conversación.
 * @param detalle información extra que se muestra en pequeño (modelo y tokens usados).
 */
data class Mensaje(
    val id: Long,
    val rol: Rol,
    val texto: String,
    val detalle: String? = null,
)

/** Respuesta ya procesada de la API de Gemini. */
data class RespuestaGemini(
    val texto: String,
    val tokens: Int?,
    val modeloUsado: String?,
)

/** Error con un mensaje listo para mostrar al usuario. */
class GeminiException(val codigoHttp: Int, mensaje: String) : Exception(mensaje)
