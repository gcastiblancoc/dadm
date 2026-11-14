package co.edu.unal.municipios.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

/**
 * Cliente del servicio web del conjunto de datos «DIVIPOLA – Códigos municipios» (DANE),
 * publicado en el Catálogo de Datos Abiertos de Colombia (datos.gov.co).
 *
 * datos.gov.co usa la plataforma Socrata, cuyo API (SODA) recibe los parámetros de la
 * consulta en la URL usando el lenguaje SoQL:
 *  - `$select`  columnas a devolver
 *  - `$where`   filtros
 *  - `$order`   ordenamiento
 *  - `$group`   agrupación
 *  - `$limit`   número máximo de filas
 *
 * La respuesta es un arreglo JSON que se procesa con [JSONArray] / [JSONObject] del SDK de Android.
 * Todas las funciones públicas hacen peticiones de red bloqueantes: deben llamarse fuera del
 * hilo principal.
 */
object DatosGovClient {

    const val URL_SERVICIO = "https://www.datos.gov.co/resource/gdxc-w37w.json"

    /** Mayor que el número total de registros (~1.100), para no truncar resultados. */
    private const val LIMITE = 2000

    private const val COLUMNAS =
        "cod_dpto,dpto,cod_mpio,nom_mpio,tipo_municipio,longitud,latitud"

    private const val USER_AGENT = "MunicipiosColombia-DADM-UNAL/1.0 (Android)"

    /** Vocales y Ñ: en la fuente pueden llevar tilde, así que no sirven para filtrar con `like`. */
    private const val LETRAS_CON_TILDE = "AEIOUN"

    // ---------------------------------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------------------------------

    /** Lista de departamentos, obtenida del mismo servicio agrupando por departamento. */
    @Throws(IOException::class)
    fun obtenerDepartamentos(): List<Departamento> {
        val json = get(construirUrl(parametrosDepartamentos(), codificar = true))
        val arreglo = leerArreglo(json)
        return (0 until arreglo.length())
            .map { arreglo.getJSONObject(it) }
            .map { Departamento(it.optString("cod_dpto"), it.optString("dpto")) }
            .filter { it.codigo.isNotBlank() && it.nombre.isNotBlank() }
            .sortedBy { normalizar(it.nombre) }
    }

    @Throws(IOException::class)
    fun buscarMunicipios(consulta: ConsultaMunicipios): ResultadoConsulta {
        val parametros = parametrosMunicipios(consulta)
        val json = get(construirUrl(parametros, codificar = true))
        val arreglo = leerArreglo(json)

        val municipios = ArrayList<Municipio>(arreglo.length())
        for (i in 0 until arreglo.length()) {
            municipios += aMunicipio(arreglo.getJSONObject(i))
        }

        // El filtro por nombre del servicio es aproximado (ver filtrosNombre);
        // aquí se aplica el filtro exacto sin tener en cuenta tildes.
        val buscado = normalizar(consulta.nombre)
        val filtrados = if (buscado.isEmpty()) municipios
        else municipios.filter { normalizar(it.nombre).contains(buscado) }

        return ResultadoConsulta(urlLegible(consulta), filtrados)
    }

    /** La URL de la consulta sin codificar, para mostrársela al usuario. */
    fun urlLegible(consulta: ConsultaMunicipios): String =
        construirUrl(parametrosMunicipios(consulta), codificar = false)

    // ---------------------------------------------------------------------------------------
    // Construcción de los parámetros SoQL
    // ---------------------------------------------------------------------------------------

    fun parametrosMunicipios(consulta: ConsultaMunicipios): List<Pair<String, String>> {
        val condiciones = buildList {
            consulta.departamento?.let { add("cod_dpto = '${escapar(it.codigo)}'") }
            consulta.tipo.valorServicio?.let { add("tipo_municipio = '${escapar(it)}'") }
            addAll(filtrosNombre(consulta.nombre))
        }

        return buildList {
            add("\$select" to COLUMNAS)
            if (condiciones.isNotEmpty()) add("\$where" to condiciones.joinToString(" AND "))
            add("\$order" to "${consulta.orden.columna} ASC")
            add("\$limit" to LIMITE.toString())
        }
    }

    private fun parametrosDepartamentos(): List<Pair<String, String>> = listOf(
        "\$select" to "cod_dpto,dpto",
        "\$group" to "cod_dpto,dpto",
        "\$order" to "dpto ASC",
        "\$limit" to "100"
    )

    /**
     * Filtros `like` para el nombre. En la fuente los nombres van en mayúsculas y con tildes
     * (p. ej. «MEDELLÍN») y `like` distingue tildes, así que «medellin» no encontraría nada.
     * Por eso solo se envían los fragmentos del texto que no tienen vocales ni Ñ:
     * «medellin» → `like '%M%' AND like '%D%' AND like '%LL%'`. El servicio devuelve así un
     * conjunto que seguro contiene a los buscados, y [buscarMunicipios] termina de filtrar.
     */
    fun filtrosNombre(nombre: String): List<String> =
        normalizar(nombre)
            .split(Regex("[$LETRAS_CON_TILDE\\s]+"))
            .map { fragmento -> fragmento.filter { it.isLetterOrDigit() } }
            .filter { it.isNotEmpty() }
            .distinct()
            .map { "upper(nom_mpio) like '%${escapar(it)}%'" }

    private fun construirUrl(parametros: List<Pair<String, String>>, codificar: Boolean): String =
        URL_SERVICIO + "?" + parametros.joinToString("&") { (clave, valor) ->
            if (codificar) "${codificarUrl(clave)}=${codificarUrl(valor)}" else "$clave=$valor"
        }

    private fun codificarUrl(texto: String): String =
        URLEncoder.encode(texto, "UTF-8").replace("+", "%20")

    /** En SoQL las comillas simples dentro de un texto se escriben dobles. */
    private fun escapar(texto: String): String = texto.replace("'", "''")

    /** Mayúsculas y sin tildes: «Bogotá» → «BOGOTA», «Nariño» → «NARINO». */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .uppercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .trim()

    // ---------------------------------------------------------------------------------------
    // Red y JSON
    // ---------------------------------------------------------------------------------------

    private fun get(url: String): String {
        val conexion = URL(url).openConnection() as HttpURLConnection
        try {
            conexion.requestMethod = "GET"
            conexion.connectTimeout = 15_000
            conexion.readTimeout = 20_000
            conexion.setRequestProperty("Accept", "application/json")
            conexion.setRequestProperty("User-Agent", USER_AGENT)

            val codigo = conexion.responseCode
            if (codigo != HttpURLConnection.HTTP_OK) {
                val detalle = conexion.errorStream?.bufferedReader()?.use { it.readText() }
                throw IOException("HTTP $codigo${mensajeDeError(detalle)}")
            }
            return conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }

    /** Socrata responde los errores como {"message": "..."}. */
    private fun mensajeDeError(cuerpo: String?): String {
        if (cuerpo.isNullOrBlank()) return ""
        return try {
            JSONObject(cuerpo).optString("message").takeIf { it.isNotBlank() }?.let { " – $it" } ?: ""
        } catch (e: JSONException) {
            ""
        }
    }

    private fun leerArreglo(json: String): JSONArray =
        try {
            JSONArray(json)
        } catch (e: JSONException) {
            throw IOException("La respuesta del servicio no es un arreglo JSON", e)
        }

    fun aMunicipio(objeto: JSONObject): Municipio = Municipio(
        codigoDepartamento = objeto.optString("cod_dpto"),
        departamento = objeto.optString("dpto"),
        codigo = objeto.optString("cod_mpio"),
        nombre = objeto.optString("nom_mpio"),
        tipo = objeto.optString("tipo_municipio"),
        latitud = aNumero(objeto.optString("latitud")),
        longitud = aNumero(objeto.optString("longitud"))
    )

    /** Las coordenadas llegan como texto con coma decimal: "-75,581775". */
    private fun aNumero(texto: String): Double? =
        texto.trim().replace(',', '.').toDoubleOrNull()
}
