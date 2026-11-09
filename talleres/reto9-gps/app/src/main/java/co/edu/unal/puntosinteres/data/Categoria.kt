package co.edu.unal.puntosinteres.data

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import co.edu.unal.puntosinteres.R

/**
 * Tipos de puntos de interés que la app sabe buscar.
 *
 * Cada categoría se traduce a etiquetas (tags) de OpenStreetMap: por ejemplo, un hospital
 * es un elemento con `amenity=hospital`. La [clave] es el valor que se guarda en las
 * preferencias (ver `res/values/arrays.xml`).
 */
enum class Categoria(
    val clave: String,
    @StringRes val nombre: Int,
    @ColorRes val color: Int,
    val etiquetas: Map<String, List<String>>
) {
    HOSPITALES(
        "hospitales", R.string.cat_hospitales, R.color.cat_hospitales,
        mapOf(
            "amenity" to listOf("hospital", "clinic"),
            "healthcare" to listOf("hospital", "clinic")
        )
    ),
    TURISMO(
        "turismo", R.string.cat_turismo, R.color.cat_turismo,
        mapOf(
            "tourism" to listOf("attraction", "museum", "gallery", "viewpoint", "zoo", "theme_park"),
            "historic" to listOf("monument", "castle", "ruins")
        )
    ),
    FARMACIAS(
        "farmacias", R.string.cat_farmacias, R.color.cat_farmacias,
        mapOf(
            "amenity" to listOf("pharmacy"),
            "healthcare" to listOf("pharmacy")
        )
    ),
    RESTAURANTES(
        "restaurantes", R.string.cat_restaurantes, R.color.cat_restaurantes,
        mapOf("amenity" to listOf("restaurant", "cafe", "fast_food"))
    ),
    POLICIA(
        "policia", R.string.cat_policia, R.color.cat_policia,
        mapOf("amenity" to listOf("police"))
    ),
    PARQUES(
        "parques", R.string.cat_parques, R.color.cat_parques,
        mapOf("leisure" to listOf("park"))
    ),
    EDUCACION(
        "educacion", R.string.cat_educacion, R.color.cat_educacion,
        mapOf("amenity" to listOf("university", "college", "school"))
    );

    /** ¿Las etiquetas de un elemento de OSM corresponden a esta categoría? */
    fun coincide(tags: Map<String, String>): Boolean =
        etiquetas.any { (clave, valores) -> tags[clave] in valores }

    companion object {
        fun desdeClave(clave: String): Categoria? = entries.firstOrNull { it.clave == clave }

        /** Primera categoría (entre las buscadas) a la que pertenece el elemento. */
        fun clasificar(tags: Map<String, String>, buscadas: List<Categoria>): Categoria? =
            buscadas.firstOrNull { it.coincide(tags) }
    }
}
