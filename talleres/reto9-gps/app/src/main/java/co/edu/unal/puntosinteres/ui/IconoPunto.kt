package co.edu.unal.puntosinteres.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.annotation.ColorInt
import kotlin.math.ceil
import kotlin.math.max

/**
 * Dibuja el ícono de un punto de interés: un círculo del color de su categoría con el
 * nombre del lugar escrito debajo, para que el nombre se vea en el mapa sin tocar el punto.
 */
object IconoPunto {

    private const val MAX_CARACTERES = 24

    /** Ícono y posición vertical (0..1) del centro del círculo, que es el punto anclado al mapa. */
    data class Resultado(val icono: BitmapDrawable, val anclaY: Float)

    fun crear(context: Context, nombre: String, @ColorInt color: Int): Resultado {
        val d = context.resources.displayMetrics.density
        val texto = if (nombre.length > MAX_CARACTERES) nombre.take(MAX_CARACTERES - 1) + "…" else nombre

        val radio = 7f * d
        val borde = 2f * d
        val separacion = 2f * d

        val pinturaTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f * d
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            this.color = Color.rgb(33, 33, 33)
        }
        // Contorno blanco alrededor de las letras para que se lean sobre cualquier fondo
        val pinturaHalo = Paint(pinturaTexto).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * d
            strokeJoin = Paint.Join.ROUND
            this.color = Color.WHITE
        }

        val metricas = pinturaTexto.fontMetrics
        val altoTexto = metricas.descent - metricas.ascent
        val diametro = 2 * (radio + borde)
        val ancho = ceil(max(pinturaTexto.measureText(texto) + 6f * d, diametro)).toInt()
        val alto = ceil(diametro + separacion + altoTexto + 2f * d).toInt()

        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = ancho / 2f
        val cy = radio + borde

        canvas.drawCircle(cx, cy, radio + borde, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.WHITE })
        canvas.drawCircle(cx, cy, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })

        val lineaBase = diametro + separacion - metricas.ascent
        canvas.drawText(texto, cx, lineaBase, pinturaHalo)
        canvas.drawText(texto, cx, lineaBase, pinturaTexto)

        return Resultado(BitmapDrawable(context.resources, bitmap), cy / alto)
    }
}
