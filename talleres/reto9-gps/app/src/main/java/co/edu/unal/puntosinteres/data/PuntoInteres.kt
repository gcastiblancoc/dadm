package co.edu.unal.puntosinteres.data

/** Un punto de interés encontrado alrededor del usuario. */
data class PuntoInteres(
    val id: Long,
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val categoria: Categoria,
    /** Distancia en metros desde la ubicación del usuario. */
    val distanciaM: Double
)
