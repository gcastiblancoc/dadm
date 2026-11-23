package co.edu.unal.asistenteia.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

/**
 * Cliente mínimo de la API REST de Gemini (Google AI Studio):
 *   POST https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent
 *
 * Cuerpo de la petición:
 * {
 *   "systemInstruction": { "parts": [ { "text": "Eres un tutor…" } ] },
 *   "contents": [
 *     { "role": "user",  "parts": [ { "text": "Hola" } ] },
 *     { "role": "model", "parts": [ { "text": "¡Hola! ¿En qué te ayudo?" } ] },
 *     { "role": "user",  "parts": [ { "text": "…" } ] }
 *   ]
 * }
 *
 * La API no guarda estado: para que el chat "recuerde", en cada turno se reenvía el historial.
 */
object GeminiClient {

    private const val URL_BASE = "https://generativelanguage.googleapis.com/v1beta"

    /** Cuántos mensajes recientes se reenvían como contexto (limita tokens y cuota). */
    private const val MAX_MENSAJES_CONTEXTO = 30

    suspend fun generar(
        apiKey: String,
        modelo: String,
        instruccionSistema: String,
        historial: List<Mensaje>,
    ): RespuestaGemini = withContext(Dispatchers.IO) {
        val cuerpo = construirCuerpo(instruccionSistema, historial)
        val nombreModelo = URLEncoder.encode(modelo.removePrefix("models/"), "UTF-8")
        val respuesta = peticion("$URL_BASE/models/$nombreModelo:generateContent", apiKey, cuerpo.toString(), modelo)
        leerRespuesta(respuesta)
    }

    /** Lista los modelos de texto disponibles para esta API key (GET /v1beta/models). */
    suspend fun listarModelos(apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val modelos = mutableListOf<String>()
        var token: String? = null
        do {
            var url = "$URL_BASE/models?pageSize=100"
            if (token != null) url += "&pageToken=" + URLEncoder.encode(token, "UTF-8")
            val json = JSONObject(peticion(url, apiKey, cuerpo = null, modelo = null))
            val lista = json.optJSONArray("models") ?: JSONArray()
            for (i in 0 until lista.length()) {
                val m = lista.getJSONObject(i)
                val nombre = m.optString("name").removePrefix("models/")
                val metodos = m.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                val generaTexto = (0 until metodos.length()).any { metodos.optString(it) == "generateContent" }
                if (generaTexto && esModeloDeChat(nombre)) modelos += nombre
            }
            token = json.optString("nextPageToken").ifBlank { null }
        } while (token != null)
        modelos.sorted()
    }

    private fun esModeloDeChat(nombre: String): Boolean {
        if (!nombre.startsWith("gemini")) return false
        val excluidos = listOf("tts", "image", "embedding", "live", "audio", "robotics", "computer-use", "veo")
        return excluidos.none { nombre.contains(it) }
    }

    // ----------------------------------------------------------------- petición

    internal fun construirCuerpo(instruccionSistema: String, historial: List<Mensaje>): JSONObject {
        val conversacion = historial
            .filter { it.rol == Rol.USUARIO || it.rol == Rol.MODELO }
            .takeLast(MAX_MENSAJES_CONTEXTO)
            .dropWhile { it.rol != Rol.USUARIO } // la conversación debe empezar con el usuario

        val contents = JSONArray()
        var rolAnterior: String? = null
        var partesActuales = JSONArray()
        for (mensaje in conversacion) {
            val rol = if (mensaje.rol == Rol.USUARIO) "user" else "model"
            // Si hay dos mensajes seguidos del mismo rol (p. ej. tras un error), se unen en un turno.
            if (rol != rolAnterior) {
                partesActuales = JSONArray()
                contents.put(JSONObject().put("role", rol).put("parts", partesActuales))
                rolAnterior = rol
            }
            partesActuales.put(JSONObject().put("text", mensaje.texto))
        }

        return JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instruccionSistema))))
            .put("contents", contents)
    }

    private fun peticion(url: String, apiKey: String, cuerpo: String?, modelo: String?): String {
        val conexion = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            conexion.connectTimeout = 15_000
            conexion.readTimeout = 90_000 // los modelos con "razonamiento" pueden tardar
            conexion.setRequestProperty("x-goog-api-key", apiKey)
            conexion.setRequestProperty("Accept", "application/json")
            if (cuerpo != null) {
                conexion.requestMethod = "POST"
                conexion.doOutput = true
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
            }
            val codigo = conexion.responseCode
            val flujo = if (codigo in 200..299) conexion.inputStream else conexion.errorStream
            val texto = flujo?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (codigo !in 200..299) throw GeminiException(codigo, mensajeDeError(codigo, texto, modelo))
            return texto
        } finally {
            conexion.disconnect()
        }
    }

    /** Traduce los errores de la API ({"error": {"code", "message", "status"}}) a algo entendible. */
    private fun mensajeDeError(codigo: Int, cuerpo: String, modelo: String?): String {
        val detalle = try {
            JSONObject(cuerpo).optJSONObject("error")?.optString("message").orEmpty()
        } catch (e: JSONException) {
            ""
        }
        val explicacion = when {
            codigo == 400 && detalle.contains("API key", ignoreCase = true) ->
                "La API key no es válida. Revísala en Ajustes."
            codigo == 400 -> "Gemini rechazó la petición (400)."
            codigo == 401 || codigo == 403 ->
                "La API key no tiene permiso para usar Gemini ($codigo). Revísala en Ajustes."
            codigo == 404 ->
                "El modelo «${modelo ?: "?"}» no está disponible para tu clave. " +
                    "Elige otro en Ajustes (botón «Cargar modelos de mi clave»)."
            codigo == 429 ->
                "Se alcanzó el límite de uso de la capa gratuita. Espera un minuto o prueba otro modelo."
            codigo >= 500 -> "Los servidores de Gemini están ocupados ($codigo). Intenta de nuevo."
            else -> "Error HTTP $codigo."
        }
        return if (detalle.isBlank()) explicacion else "$explicacion\n\nDetalle: $detalle"
    }

    // ---------------------------------------------------------------- respuesta

    /**
     * Respuesta típica:
     * { "candidates": [ { "content": { "parts": [ { "text": "…" } ], "role": "model" },
     *                     "finishReason": "STOP" } ],
     *   "usageMetadata": { "totalTokenCount": 123 }, "modelVersion": "gemini-…" }
     */
    internal fun leerRespuesta(texto: String): RespuestaGemini {
        val json = JSONObject(texto)
        val candidatos = json.optJSONArray("candidates")
        if (candidatos == null || candidatos.length() == 0) {
            val bloqueo = json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            throw GeminiException(
                200,
                if (bloqueo.isNotBlank()) "Gemini bloqueó el mensaje por sus filtros de seguridad ($bloqueo)."
                else "Gemini no devolvió ninguna respuesta.",
            )
        }
        val candidato = candidatos.getJSONObject(0)
        val partes = candidato.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
        val respuesta = StringBuilder()
        for (i in 0 until partes.length()) {
            val parte = partes.getJSONObject(i)
            if (parte.optBoolean("thought", false)) continue // partes de "razonamiento" interno
            respuesta.append(parte.optString("text"))
        }

        val motivoFin = candidato.optString("finishReason")
        if (respuesta.isBlank()) {
            throw GeminiException(
                200,
                when (motivoFin) {
                    "SAFETY" -> "La respuesta se bloqueó por los filtros de seguridad de Gemini."
                    "MAX_TOKENS" -> "La respuesta se cortó antes de generar texto. Intenta de nuevo."
                    else -> "Gemini devolvió una respuesta vacía ($motivoFin)."
                },
            )
        }
        var final = respuesta.toString().trim()
        if (motivoFin == "MAX_TOKENS") final += "\n\n[Respuesta cortada por longitud]"

        val tokens = json.optJSONObject("usageMetadata")?.optInt("totalTokenCount", -1)?.takeIf { it >= 0 }
        val modelo = json.optString("modelVersion").ifBlank { null }
        return RespuestaGemini(final, tokens, modelo)
    }
}
