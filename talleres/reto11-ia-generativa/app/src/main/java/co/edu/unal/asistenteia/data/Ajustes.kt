package co.edu.unal.asistenteia.data

import android.content.Context
import androidx.core.content.edit
import co.edu.unal.asistenteia.BuildConfig

/** Preferencias guardadas en el teléfono: API key, modelo y personalidad elegida. */
class Ajustes(context: Context) {

    private val prefs = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    /** Clave escrita por el usuario en la app (tiene prioridad sobre la de local.properties). */
    var apiKeyPropia: String
        get() = prefs.getString(CLAVE_API_KEY, "").orEmpty()
        set(valor) = prefs.edit { putString(CLAVE_API_KEY, valor.trim()) }

    /** Clave que realmente se usa en las peticiones. */
    val apiKey: String
        get() = apiKeyPropia.ifBlank { BuildConfig.GEMINI_API_KEY }

    val hayClaveDeCompilacion: Boolean
        get() = BuildConfig.GEMINI_API_KEY.isNotBlank()

    var modelo: String
        get() = prefs.getString(CLAVE_MODELO, null)?.takeIf { it.isNotBlank() } ?: MODELO_POR_DEFECTO
        set(valor) = prefs.edit { putString(CLAVE_MODELO, valor.trim()) }

    var personalidad: Personalidad
        get() = Personalidad.desdeNombre(prefs.getString(CLAVE_PERSONALIDAD, null))
        set(valor) = prefs.edit { putString(CLAVE_PERSONALIDAD, valor.name) }

    companion object {
        private const val CLAVE_API_KEY = "api_key"
        private const val CLAVE_MODELO = "modelo"
        private const val CLAVE_PERSONALIDAD = "personalidad"

        /** Alias que Google mantiene apuntando al Flash-Lite más reciente (rápido y con capa gratuita). */
        const val MODELO_POR_DEFECTO = "gemini-flash-lite-latest"

        /** Opciones que se muestran antes de consultar la lista real de modelos de la clave. */
        val MODELOS_SUGERIDOS = listOf(
            "gemini-flash-lite-latest",
            "gemini-flash-latest",
            "gemini-2.5-flash-lite",
            "gemini-2.5-flash",
        )
    }
}
