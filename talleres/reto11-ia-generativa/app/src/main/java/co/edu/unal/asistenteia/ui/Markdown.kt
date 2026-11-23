package co.edu.unal.asistenteia.ui

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan

/**
 * Convierte el Markdown sencillo que devuelve Gemini (**negrita**, *cursiva*, `código`,
 * títulos con #, listas con - o * y bloques ```) en texto con estilos para un TextView.
 */
object Markdown {

    private val titulo = Regex("^#{1,6}\\s+")
    private val vineta = Regex("^(\\s*)[*-]\\s+")
    private val enLinea = Regex("\\*\\*(.+?)\\*\\*|`([^`]+)`|(?<![*\\w])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![*\\w])")

    fun aTextoConEstilo(markdown: String): CharSequence {
        val sb = SpannableStringBuilder()
        var enBloqueCodigo = false
        var primera = true
        for (lineaOriginal in markdown.lines()) {
            if (lineaOriginal.trimStart().startsWith("```")) {
                enBloqueCodigo = !enBloqueCodigo
                continue
            }
            if (!primera) sb.append('\n')
            primera = false

            val inicio = sb.length
            if (enBloqueCodigo) {
                sb.append(lineaOriginal)
                sb.setSpan(TypefaceSpan("monospace"), inicio, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                continue
            }

            var linea = lineaOriginal
            val esTitulo = titulo.find(linea)?.also { linea = linea.substring(it.range.last + 1) } != null
            vineta.find(linea)?.let { linea = it.groupValues[1] + "•  " + linea.substring(it.range.last + 1) }

            agregarEnLinea(sb, linea)
            if (esTitulo) {
                sb.setSpan(StyleSpan(Typeface.BOLD), inicio, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(RelativeSizeSpan(1.1f), inicio, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return sb
    }

    private fun agregarEnLinea(sb: SpannableStringBuilder, linea: String) {
        var pos = 0
        for (m in enLinea.findAll(linea)) {
            sb.append(linea, pos, m.range.first)
            val inicio = sb.length
            val estilo: Any = when {
                m.groups[1] != null -> { sb.append(m.groupValues[1]); StyleSpan(Typeface.BOLD) }
                m.groups[2] != null -> { sb.append(m.groupValues[2]); TypefaceSpan("monospace") }
                else -> { sb.append(m.groupValues[3]); StyleSpan(Typeface.ITALIC) }
            }
            sb.setSpan(estilo, inicio, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            pos = m.range.last + 1
        }
        sb.append(linea, pos, linea.length)
    }
}
