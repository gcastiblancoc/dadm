package co.edu.unal.municipios.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.edu.unal.municipios.R
import co.edu.unal.municipios.data.Formato
import co.edu.unal.municipios.data.Municipio

class MunicipioAdapter(
    private val alTocar: (Municipio) -> Unit
) : ListAdapter<Municipio, MunicipioAdapter.MunicipioViewHolder>(Diferencias) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MunicipioViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_municipio, parent, false)
        return MunicipioViewHolder(vista)
    }

    override fun onBindViewHolder(holder: MunicipioViewHolder, position: Int) {
        holder.mostrar(getItem(position))
    }

    inner class MunicipioViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        private val nombre: TextView = vista.findViewById(R.id.text_nombre)
        private val tipo: TextView = vista.findViewById(R.id.text_tipo)
        private val detalle: TextView = vista.findViewById(R.id.text_detalle)
        private val coordenadas: TextView = vista.findViewById(R.id.text_coordenadas)

        fun mostrar(municipio: Municipio) {
            val contexto = itemView.context
            nombre.text = Formato.titulo(municipio.nombre)
            tipo.text = municipio.tipo
            tipo.visibility = if (municipio.tipo.isBlank()) View.GONE else View.VISIBLE
            detalle.text = contexto.getString(
                R.string.item_detalle, Formato.titulo(municipio.departamento), municipio.codigo
            )

            val lat = municipio.latitud
            val lon = municipio.longitud
            coordenadas.text = if (lat != null && lon != null) {
                contexto.getString(R.string.item_coordenadas, lat, lon)
            } else {
                contexto.getString(R.string.sin_coordenadas)
            }

            itemView.setOnClickListener { alTocar(municipio) }
        }
    }

    private object Diferencias : DiffUtil.ItemCallback<Municipio>() {
        override fun areItemsTheSame(a: Municipio, b: Municipio) = a.codigo == b.codigo
        override fun areContentsTheSame(a: Municipio, b: Municipio) = a == b
    }
}
