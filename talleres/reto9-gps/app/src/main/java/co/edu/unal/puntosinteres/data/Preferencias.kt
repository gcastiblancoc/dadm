package co.edu.unal.puntosinteres.data

import android.content.SharedPreferences

/**
 * Lectura de las preferencias que el usuario define en la pantalla de Configuración
 * (ver `res/xml/preferencias.xml`).
 */
object Preferencias {
    const val CLAVE_RADIO = "radio_km"
    const val CLAVE_CATEGORIAS = "categorias"

    private const val RADIO_POR_DEFECTO = 2
    private const val RADIO_MIN = 1
    private const val RADIO_MAX = 10
    private val CATEGORIAS_POR_DEFECTO = setOf("hospitales", "turismo", "farmacias")

    /** Radio de búsqueda en kilómetros. */
    fun radioKm(prefs: SharedPreferences): Int =
        prefs.getInt(CLAVE_RADIO, RADIO_POR_DEFECTO).coerceIn(RADIO_MIN, RADIO_MAX)

    /** Categorías seleccionadas, en el orden en que están definidas. */
    fun categorias(prefs: SharedPreferences): List<Categoria> =
        (prefs.getStringSet(CLAVE_CATEGORIAS, null) ?: CATEGORIAS_POR_DEFECTO)
            .mapNotNull { Categoria.desdeClave(it) }
            .sortedBy { it.ordinal }
}
