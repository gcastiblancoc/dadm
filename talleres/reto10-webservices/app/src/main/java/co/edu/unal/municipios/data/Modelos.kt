package co.edu.unal.municipios.data

/** Un registro del conjunto de datos DIVIPOLA. */
data class Municipio(
    val codigoDepartamento: String,
    val departamento: String,
    val codigo: String,
    val nombre: String,
    val tipo: String,
    val latitud: Double?,
    val longitud: Double?
)

data class Departamento(val codigo: String, val nombre: String)

/** Valores posibles de la columna `tipo_municipio`. */
enum class TipoMunicipio(val valorServicio: String?) {
    TODOS(null),
    MUNICIPIO("Municipio"),
    ISLA("Isla"),
    AREA_NO_MUNICIPALIZADA("Área no municipalizada")
}

/** Columna por la que se ordenan los resultados (`$order`). */
enum class Orden(val columna: String) {
    NOMBRE("nom_mpio"),
    CODIGO("cod_mpio")
}

/** Parámetros que el usuario elige en la interfaz y que se envían al servicio web. */
data class ConsultaMunicipios(
    val nombre: String = "",
    val departamento: Departamento? = null,
    val tipo: TipoMunicipio = TipoMunicipio.TODOS,
    val orden: Orden = Orden.NOMBRE
)

/** Respuesta ya procesada, junto con la URL que se usó para pedirla. */
data class ResultadoConsulta(val url: String, val municipios: List<Municipio>)
