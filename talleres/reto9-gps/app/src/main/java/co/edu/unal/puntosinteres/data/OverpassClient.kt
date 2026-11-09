package co.edu.unal.puntosinteres.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Busca puntos de interés en OpenStreetMap usando la Overpass API.
 *
 * La consulta pide todos los nodos, vías y relaciones (`nwr`) que tengan nombre y alguna
 * de las etiquetas de las categorías elegidas, dentro de un círculo de [radioM] metros
 * alrededor del usuario (`around`). Para vías y relaciones se pide su centro (`out center`).
 *
 * Debe llamarse fuera del hilo principal (hace una petición de red bloqueante).
 */
object OverpassClient {

    private val SERVIDORES = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"
    )

    /** Máximo de elementos que se piden al servidor. */
    private const val LIMITE_CONSULTA = 500

    /** Máximo de puntos que se dibujan en el mapa (los más cercanos). */
    const val MAX_PUNTOS = 150

    /** Dos elementos con el mismo nombre a menos de esta distancia se consideran el mismo lugar. */
    private const val DISTANCIA_DUPLICADO_M = 200.0

    private const val USER_AGENT = "PuntosInteres-DADM-UNAL/1.0 (Android)"

    @Throws(IOException::class)
    fun buscar(lat: Double, lon: Double, radioM: Int, categorias: List<Categoria>): List<PuntoInteres> {
        if (categorias.isEmpty()) return emptyList()
        val consulta = construirConsulta(lat, lon, radioM, categorias)

        var ultimoError: IOException? = null
        for (servidor in SERVIDORES) {
            try {
                val json = enviar(servidor, consulta)
                return interpretar(json, lat, lon, radioM, categorias)
            } catch (e: IOException) {
                ultimoError = e // Servidor caído o saturado: se intenta con el siguiente
            }
        }
        throw ultimoError ?: IOException("No hay servidores Overpass disponibles")
    }

    /** Arma la consulta en Overpass QL. */
    fun construirConsulta(lat: Double, lon: Double, radioM: Int, categorias: List<Categoria>): String {
        val alrededor = "(around:$radioM,$lat,$lon)"
        val sb = StringBuilder("[out:json][timeout:25];(")
        for (categoria in categorias) {
            for ((clave, valores) in categoria.etiquetas) {
                sb.append("nwr[\"").append(clave).append("\"~\"^(")
                    .append(valores.joinToString("|"))
                    .append(")\$\"][\"name\"]")
                    .append(alrededor)
                    .append(';')
            }
        }
        sb.append(");out center ").append(LIMITE_CONSULTA).append(';')
        return sb.toString()
    }

    private fun enviar(servidor: String, consulta: String): String {
        val conexion = URL(servidor).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "POST"
            conexion.connectTimeout = 15_000
            conexion.readTimeout = 35_000
            conexion.doOutput = true
            conexion.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            conexion.setRequestProperty("User-Agent", USER_AGENT)

            val cuerpo = "data=" + URLEncoder.encode(consulta, "UTF-8")
            conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }

            val codigo = conexion.responseCode
            if (codigo != HttpURLConnection.HTTP_OK) {
                throw IOException("Overpass respondió HTTP $codigo")
            }
            return conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }

    private fun interpretar(
        json: String,
        lat: Double,
        lon: Double,
        radioM: Int,
        categorias: List<Categoria>
    ): List<PuntoInteres> {
        val elementos = try {
            JSONObject(json).getJSONArray("elements")
        } catch (e: Exception) {
            throw IOException("Respuesta de Overpass inválida", e)
        }

        val encontrados = ArrayList<PuntoInteres>()
        for (i in 0 until elementos.length()) {
            val elemento = elementos.getJSONObject(i)
            val tagsJson = elemento.optJSONObject("tags") ?: continue
            val nombre = tagsJson.optString("name").trim()
            if (nombre.isEmpty()) continue

            // Los nodos traen lat/lon; las vías y relaciones traen "center"
            val (pLat, pLon) = when {
                elemento.has("lat") -> elemento.getDouble("lat") to elemento.getDouble("lon")
                elemento.has("center") -> elemento.getJSONObject("center").let {
                    it.getDouble("lat") to it.getDouble("lon")
                }
                else -> continue
            }

            val tags = HashMap<String, String>()
            tagsJson.keys().forEach { clave -> tags[clave] = tagsJson.optString(clave) }
            val categoria = Categoria.clasificar(tags, categorias) ?: continue

            val distancia = distanciaMetros(lat, lon, pLat, pLon)
            // El centro de un edificio grande puede quedar un poco fuera del círculo
            if (distancia > radioM) continue

            val duplicado = encontrados.any {
                it.categoria == categoria &&
                    it.nombre.equals(nombre, ignoreCase = true) &&
                    distanciaMetros(it.latitud, it.longitud, pLat, pLon) < DISTANCIA_DUPLICADO_M
            }
            if (duplicado) continue

            encontrados += PuntoInteres(
                id = elemento.optLong("id"),
                nombre = nombre,
                latitud = pLat,
                longitud = pLon,
                categoria = categoria,
                distanciaM = distancia
            )
        }
        return encontrados.sortedBy { it.distanciaM }.take(MAX_PUNTOS)
    }

    /** Distancia sobre la superficie terrestre (fórmula del haversine), en metros. */
    fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radioTierra = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * radioTierra * asin(sqrt(a))
    }
}
