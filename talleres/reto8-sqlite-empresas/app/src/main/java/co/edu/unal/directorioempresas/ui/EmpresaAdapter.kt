package co.edu.unal.directorioempresas.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.edu.unal.directorioempresas.R
import co.edu.unal.directorioempresas.data.Empresa

/**
 * Adaptador para mostrar la lista de empresas en un RecyclerView usando ListAdapter y DiffUtil.
 */
class EmpresaAdapter(
    private val onClick: (Empresa) -> Unit,
    private val onLongClick: (Empresa) -> Unit
) : ListAdapter<Empresa, EmpresaAdapter.EmpresaViewHolder>(EmpresaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmpresaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_empresa, parent, false)
        return EmpresaViewHolder(view, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: EmpresaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder que gestiona las vistas de cada elemento de la lista.
     */
    class EmpresaViewHolder(
        itemView: View,
        private val onClick: (Empresa) -> Unit,
        private val onLongClick: (Empresa) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val textNombre: TextView = itemView.findViewById(R.id.text_nombre)
        private val textClasificaciones: TextView = itemView.findViewById(R.id.text_clasificaciones)
        private val textContacto: TextView = itemView.findViewById(R.id.text_contacto)

        private var currentEmpresa: Empresa? = null

        init {
            itemView.setOnClickListener {
                currentEmpresa?.let { empresa -> onClick(empresa) }
            }
            itemView.setOnLongClickListener {
                currentEmpresa?.let { empresa -> onLongClick(empresa) }
                true
            }
        }

        fun bind(empresa: Empresa) {
            currentEmpresa = empresa
            textNombre.text = empresa.nombre
            textClasificaciones.text = empresa.obtenerClasificacionesTexto()

            val contactoTexto = when {
                empresa.telefono.isNotBlank() && empresa.email.isNotBlank() -> "${empresa.telefono} · ${empresa.email}"
                empresa.telefono.isNotBlank() -> empresa.telefono
                empresa.email.isNotBlank() -> empresa.email
                else -> ""
            }
            textContacto.text = contactoTexto
            textContacto.visibility = if (contactoTexto.isNotBlank()) View.VISIBLE else View.GONE
        }
    }

    /**
     * Callback de DiffUtil para comparar elementos por ID y por contenido.
     */
    private class EmpresaDiffCallback : DiffUtil.ItemCallback<Empresa>() {
        override fun areItemsTheSame(oldItem: Empresa, newItem: Empresa): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Empresa, newItem: Empresa): Boolean {
            return oldItem == newItem
        }
    }
}
