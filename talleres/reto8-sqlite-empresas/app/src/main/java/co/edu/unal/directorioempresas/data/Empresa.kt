package co.edu.unal.directorioempresas.data

/**
 * Modelo de datos que representa una Empresa en el directorio.
 */
data class Empresa(
    val id: Long = 0,
    val nombre: String,
    val url: String,
    val telefono: String,
    val email: String,
    val productosServicios: String,
    val esConsultoria: Boolean,
    val esDesarrolloMedida: Boolean,
    val esFabricaSoftware: Boolean
) {
    /**
     * Retorna las clasificaciones activas formateadas como texto legible
     * separado por " · " o "Sin clasificación" si ninguna aplica.
     */
    fun obtenerClasificacionesTexto(): String {
        val clasificaciones = mutableListOf<String>()
        if (esConsultoria) clasificaciones.add("Consultoría")
        if (esDesarrolloMedida) clasificaciones.add("Desarrollo a medida")
        if (esFabricaSoftware) clasificaciones.add("Fábrica de software")

        return if (clasificaciones.isNotEmpty()) {
            clasificaciones.joinToString(" · ")
        } else {
            "Sin clasificación"
        }
    }
}
