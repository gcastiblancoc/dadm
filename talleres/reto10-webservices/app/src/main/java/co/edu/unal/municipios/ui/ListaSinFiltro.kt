package co.edu.unal.municipios.ui

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

/**
 * Adaptador para el desplegable de departamentos. Un AutoCompleteTextView normal filtra las
 * opciones con el texto que ya tiene escrito, y como el campo muestra la opción elegida, la
 * lista quedaría reducida a esa sola opción. Este adaptador muestra siempre todas.
 */
class ListaSinFiltro(context: Context, private val opciones: List<String>) :
    ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, opciones) {

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
            values = opciones
            count = opciones.size
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            notifyDataSetChanged()
        }
    }
}
