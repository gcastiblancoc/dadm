package co.edu.unal.municipios.data

import java.util.Locale

/** Utilidades para mostrar los textos del servicio, que vienen en MAYÚSCULAS. */
object Formato {

    private val PALABRAS_MENORES = setOf("de", "del", "la", "las", "el", "los", "y", "e", "en")
    private val ES = Locale.forLanguageTag("es-CO")

    /** «SAN JUAN DE ARAMA» → «San Juan de Arama». Las siglas con punto se dejan igual. */
    fun titulo(texto: String): String =
        texto.trim().lowercase(ES).split(' ').filter { it.isNotEmpty() }
            .mapIndexed { i, palabra ->
                when {
                    palabra.contains('.') -> palabra.uppercase(ES)
                    i > 0 && palabra in PALABRAS_MENORES -> palabra
                    else -> palabra.replaceFirstChar { it.titlecase(ES) }
                }
            }
            .joinToString(" ")
}
