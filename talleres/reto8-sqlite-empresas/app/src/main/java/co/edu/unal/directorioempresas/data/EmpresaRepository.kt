package co.edu.unal.directorioempresas.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

/**
 * Repositorio para gestionar las operaciones CRUD de empresas sobre la base de datos SQLite.
 */
class EmpresaRepository(context: Context) {

    private val dbHelper = EmpresaDbHelper(context)

    /**
     * Inserta una nueva empresa en la base de datos y retorna el ID asignado.
     */
    fun insertar(empresa: Empresa): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EmpresaDbHelper.COLUMN_NOMBRE, empresa.nombre)
            put(EmpresaDbHelper.COLUMN_URL, empresa.url)
            put(EmpresaDbHelper.COLUMN_TELEFONO, empresa.telefono)
            put(EmpresaDbHelper.COLUMN_EMAIL, empresa.email)
            put(EmpresaDbHelper.COLUMN_PRODUCTOS_SERVICIOS, empresa.productosServicios)
            put(EmpresaDbHelper.COLUMN_ES_CONSULTORIA, if (empresa.esConsultoria) 1 else 0)
            put(EmpresaDbHelper.COLUMN_ES_DESARROLLO_MEDIDA, if (empresa.esDesarrolloMedida) 1 else 0)
            put(EmpresaDbHelper.COLUMN_ES_FABRICA_SOFTWARE, if (empresa.esFabricaSoftware) 1 else 0)
        }
        return db.insert(EmpresaDbHelper.TABLE_NAME, null, values)
    }

    /**
     * Obtiene una empresa por su ID. Retorna null si no se encuentra.
     */
    fun obtenerPorId(id: Long): Empresa? {
        val db = dbHelper.readableDatabase
        val selection = "${EmpresaDbHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())

        return db.query(
            EmpresaDbHelper.TABLE_NAME,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                mapCursorToEmpresa(cursor)
            } else {
                null
            }
        }
    }

    /**
     * Obtiene el listado de empresas aplicando filtros opcionales de nombre y clasificación.
     * Permite filtrar por nombre usando un patrón LIKE y por clasificación si una o más
     * opciones están seleccionadas (con lógica OR entre clasificaciones). Los resultados
     * se ordenan de forma ascendente por nombre.
     */
    fun listar(
        nombre: String?,
        consultoria: Boolean,
        desarrolloMedida: Boolean,
        fabricaSoftware: Boolean
    ): List<Empresa> {
        val db = dbHelper.readableDatabase
        val selectionClauses = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        if (!nombre.isNullOrBlank()) {
            selectionClauses.add("${EmpresaDbHelper.COLUMN_NOMBRE} LIKE ?")
            selectionArgs.add("%${nombre.trim()}%")
        }

        val classificationClauses = mutableListOf<String>()
        if (consultoria) {
            classificationClauses.add("${EmpresaDbHelper.COLUMN_ES_CONSULTORIA} = 1")
        }
        if (desarrolloMedida) {
            classificationClauses.add("${EmpresaDbHelper.COLUMN_ES_DESARROLLO_MEDIDA} = 1")
        }
        if (fabricaSoftware) {
            classificationClauses.add("${EmpresaDbHelper.COLUMN_ES_FABRICA_SOFTWARE} = 1")
        }

        if (classificationClauses.isNotEmpty()) {
            selectionClauses.add("(${classificationClauses.joinToString(" OR ")})")
        }

        val selection = if (selectionClauses.isNotEmpty()) selectionClauses.joinToString(" AND ") else null
        val args = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null

        val empresas = mutableListOf<Empresa>()
        db.query(
            EmpresaDbHelper.TABLE_NAME,
            null,
            selection,
            args,
            null,
            null,
            "${EmpresaDbHelper.COLUMN_NOMBRE} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                empresas.add(mapCursorToEmpresa(cursor))
            }
        }
        return empresas
    }

    /**
     * Actualiza la información de una empresa existente y retorna la cantidad de filas afectadas.
     */
    fun actualizar(empresa: Empresa): Int {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(EmpresaDbHelper.COLUMN_NOMBRE, empresa.nombre)
            put(EmpresaDbHelper.COLUMN_URL, empresa.url)
            put(EmpresaDbHelper.COLUMN_TELEFONO, empresa.telefono)
            put(EmpresaDbHelper.COLUMN_EMAIL, empresa.email)
            put(EmpresaDbHelper.COLUMN_PRODUCTOS_SERVICIOS, empresa.productosServicios)
            put(EmpresaDbHelper.COLUMN_ES_CONSULTORIA, if (empresa.esConsultoria) 1 else 0)
            put(EmpresaDbHelper.COLUMN_ES_DESARROLLO_MEDIDA, if (empresa.esDesarrolloMedida) 1 else 0)
            put(EmpresaDbHelper.COLUMN_ES_FABRICA_SOFTWARE, if (empresa.esFabricaSoftware) 1 else 0)
        }
        val selection = "${EmpresaDbHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(empresa.id.toString())
        return db.update(EmpresaDbHelper.TABLE_NAME, values, selection, selectionArgs)
    }

    /**
     * Elimina una empresa por su ID y retorna la cantidad de filas afectadas.
     */
    fun eliminar(id: Long): Int {
        val db = dbHelper.writableDatabase
        val selection = "${EmpresaDbHelper.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(id.toString())
        return db.delete(EmpresaDbHelper.TABLE_NAME, selection, selectionArgs)
    }

    /**
     * Función auxiliar para mapear el registro actual del Cursor a un objeto Empresa.
     */
    private fun mapCursorToEmpresa(cursor: Cursor): Empresa {
        val idIndex = cursor.getColumnIndexOrThrow(EmpresaDbHelper.COLUMN_ID)
        val nombreIndex = cursor.getColumnIndexOrThrow(EmpresaDbHelper.COLUMN_NOMBRE)
        val urlIndex = cursor.getColumnIndexOrThrow(EmpresaDbHelper.COLUMN_URL)
        val telefonoIndex = cursor.getColumnIndexOrThrow(EmpresaDbHelper.COLUMN_TELEFONO)
        val emailIndex = cursor.getColumnIndexOrThrow(EmpresaDbHelper.COLUMN_EMAIL)
        val prodServIndex = cursor.getColumnIndexOrThrow(EmpresaDbHelper.COLUMN_PRODUCTOS_SERVICIOS)
        val consultoriaIndex = cursor.getColumnIndexOrThrow(EmpresaDbHelper.COLUMN_ES_CONSULTORIA)
        val desarrolloIndex = cursor.getColumnIndexOrThrow(EmpresaDbHelper.COLUMN_ES_DESARROLLO_MEDIDA)
        val fabricaIndex = cursor.getColumnIndexOrThrow(EmpresaDbHelper.COLUMN_ES_FABRICA_SOFTWARE)

        return Empresa(
            id = cursor.getLong(idIndex),
            nombre = cursor.getString(nombreIndex) ?: "",
            url = cursor.getString(urlIndex) ?: "",
            telefono = cursor.getString(telefonoIndex) ?: "",
            email = cursor.getString(emailIndex) ?: "",
            productosServicios = cursor.getString(prodServIndex) ?: "",
            esConsultoria = cursor.getInt(consultoriaIndex) == 1,
            esDesarrolloMedida = cursor.getInt(desarrolloIndex) == 1,
            esFabricaSoftware = cursor.getInt(fabricaIndex) == 1
        )
    }
}
