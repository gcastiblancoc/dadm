package co.edu.unal.asistenteia.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.edu.unal.asistenteia.R
import co.edu.unal.asistenteia.data.Mensaje
import co.edu.unal.asistenteia.data.Rol

/** Burbujas del chat: usuario (derecha), Gemini (izquierda), error y "escribiendo…". */
class MensajeAdapter(
    private val alCopiar: (String) -> Unit,
    private val alReintentar: () -> Unit,
) : ListAdapter<Mensaje, MensajeAdapter.Burbuja>(Diferencia) {

    override fun getItemViewType(position: Int) = getItem(position).rol.ordinal

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Burbuja {
        val layout = when (Rol.entries[viewType]) {
            Rol.USUARIO -> R.layout.item_mensaje_usuario
            Rol.MODELO -> R.layout.item_mensaje_modelo
            Rol.ERROR -> R.layout.item_mensaje_error
            Rol.ESCRIBIENDO -> R.layout.item_escribiendo
        }
        return Burbuja(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: Burbuja, position: Int) = holder.mostrar(getItem(position))

    inner class Burbuja(vista: View) : RecyclerView.ViewHolder(vista) {
        private val texto: TextView? = vista.findViewById(R.id.texto)
        private val detalle: TextView? = vista.findViewById(R.id.detalle)
        private val tarjeta: View? = vista.findViewById(R.id.tarjeta)
        private val botonReintentar: View? = vista.findViewById(R.id.btn_reintentar)

        fun mostrar(mensaje: Mensaje) {
            texto?.text = if (mensaje.rol == Rol.MODELO) Markdown.aTextoConEstilo(mensaje.texto) else mensaje.texto
            detalle?.text = mensaje.detalle
            detalle?.visibility = if (mensaje.detalle.isNullOrBlank()) View.GONE else View.VISIBLE
            tarjeta?.setOnLongClickListener {
                alCopiar(texto?.text?.toString().orEmpty())
                true
            }
            botonReintentar?.setOnClickListener { alReintentar() }
        }
    }

    private object Diferencia : DiffUtil.ItemCallback<Mensaje>() {
        override fun areItemsTheSame(a: Mensaje, b: Mensaje) = a.id == b.id
        override fun areContentsTheSame(a: Mensaje, b: Mensaje) = a == b
    }
}
