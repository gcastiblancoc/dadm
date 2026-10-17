package co.edu.unal.directorioempresas.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Helper para la creación, apertura y actualización de la base de datos SQLite.
 */
class EmpresaDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "directorio_empresas.db"
        const val DATABASE_VERSION = 2

        const val TABLE_NAME = "empresas"
        const val COLUMN_ID = "_id"
        const val COLUMN_NOMBRE = "nombre"
        const val COLUMN_URL = "url"
        const val COLUMN_TELEFONO = "telefono"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PRODUCTOS_SERVICIOS = "productos_servicios"
        const val COLUMN_ES_CONSULTORIA = "es_consultoria"
        const val COLUMN_ES_DESARROLLO_MEDIDA = "es_desarrollo_medida"
        const val COLUMN_ES_FABRICA_SOFTWARE = "es_fabrica_software"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSql = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOMBRE TEXT NOT NULL,
                $COLUMN_URL TEXT,
                $COLUMN_TELEFONO TEXT,
                $COLUMN_EMAIL TEXT,
                $COLUMN_PRODUCTOS_SERVICIOS TEXT,
                $COLUMN_ES_CONSULTORIA INTEGER NOT NULL DEFAULT 0,
                $COLUMN_ES_DESARROLLO_MEDIDA INTEGER NOT NULL DEFAULT 0,
                $COLUMN_ES_FABRICA_SOFTWARE INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(createTableSql)

        // Inserción de 5 empresas ficticias colombianas de ejemplo
        insertarEmpresasIniciales(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    /**
     * Inserta empresas iniciales de prueba directamente utilizando ContentValues.
     */
    private fun insertarEmpresasIniciales(db: SQLiteDatabase) {
        val empresas = listOf(
            ContentValues().apply {
                put(COLUMN_NOMBRE, "Andes Tech Solutions")
                put(COLUMN_URL, "https://andestech.co")
                put(COLUMN_TELEFONO, "+57 601 555 1234")
                put(COLUMN_EMAIL, "contacto@andestech.co")
                put(COLUMN_PRODUCTOS_SERVICIOS, "Sistemas ERP, migración y consultoría en la nube")
                put(COLUMN_ES_CONSULTORIA, 1)
                put(COLUMN_ES_DESARROLLO_MEDIDA, 1)
                put(COLUMN_ES_FABRICA_SOFTWARE, 0)
            },
            ContentValues().apply {
                put(COLUMN_NOMBRE, "Bogotá Software Labs")
                put(COLUMN_URL, "https://bogotasoftlabs.com")
                put(COLUMN_TELEFONO, "+57 310 456 7890")
                put(COLUMN_EMAIL, "info@bogotasoftlabs.com")
                put(COLUMN_PRODUCTOS_SERVICIOS, "Fábrica de software y aplicaciones móviles")
                put(COLUMN_ES_CONSULTORIA, 0)
                put(COLUMN_ES_DESARROLLO_MEDIDA, 1)
                put(COLUMN_ES_FABRICA_SOFTWARE, 1)
            },
            ContentValues().apply {
                put(COLUMN_NOMBRE, "Caribe Digital Dev")
                put(COLUMN_URL, "https://caribedigital.co")
                put(COLUMN_TELEFONO, "+57 605 333 4455")
                put(COLUMN_EMAIL, "ventas@caribedigital.co")
                put(COLUMN_PRODUCTOS_SERVICIOS, "Plataformas e-commerce e integración de pagos")
                put(COLUMN_ES_CONSULTORIA, 0)
                put(COLUMN_ES_DESARROLLO_MEDIDA, 1)
                put(COLUMN_ES_FABRICA_SOFTWARE, 0)
            },
            ContentValues().apply {
                put(COLUMN_NOMBRE, "Pacífico Consultores IT")
                put(COLUMN_URL, "https://pacificoconsultores.com")
                put(COLUMN_TELEFONO, "+57 602 888 9900")
                put(COLUMN_EMAIL, "hola@pacificoconsultores.com")
                put(COLUMN_PRODUCTOS_SERVICIOS, "Auditoría de código, ciberseguridad y consultoría IT")
                put(COLUMN_ES_CONSULTORIA, 1)
                put(COLUMN_ES_DESARROLLO_MEDIDA, 0)
                put(COLUMN_ES_FABRICA_SOFTWARE, 0)
            },
            ContentValues().apply {
                put(COLUMN_NOMBRE, "Cafetera Software House")
                put(COLUMN_URL, "https://cafeterasoft.co")
                put(COLUMN_TELEFONO, "+57 315 777 6611")
                put(COLUMN_EMAIL, "soporte@cafeterasoft.co")
                put(COLUMN_PRODUCTOS_SERVICIOS, "Desarrollo a medida y fábrica de componentes de software")
                put(COLUMN_ES_CONSULTORIA, 0)
                put(COLUMN_ES_DESARROLLO_MEDIDA, 1)
                put(COLUMN_ES_FABRICA_SOFTWARE, 1)
            }
        )

        for (values in empresas) {
            db.insert(TABLE_NAME, null, values)
        }
    }
}
