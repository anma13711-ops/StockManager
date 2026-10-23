package com.anmaoff.stockmanager.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anmaoff.stockmanager.data.Movimiento
import com.anmaoff.stockmanager.databinding.ItemMovimientoBinding
import java.text.SimpleDateFormat
import java.util.Locale

/** Adaptador del RecyclerView. DiffUtil actualiza solo las filas que cambian. */
class MovimientoAdapter(
    private val onEditar: (Movimiento) -> Unit,
    private val onEliminar: (Movimiento) -> Unit
) : ListAdapter<Movimiento, MovimientoAdapter.VH>(Diff) {

    private val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class VH(val b: ItemMovimientoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMovimientoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = getItem(position)
        holder.b.tvTitulo.text = "${m.tipo} · ${m.codigoProducto} · ${m.cantidad} uds"
        holder.b.tvDetalle.text = "${m.almacen} · ${m.responsable}\n" +
                formato.format(m.fecha) + if (m.sincronizado) " · sincronizado" else " · pendiente"
        holder.b.btnEditar.setOnClickListener { onEditar(m) }
        holder.b.btnEliminar.setOnClickListener { onEliminar(m) }
    }

    private object Diff : DiffUtil.ItemCallback<Movimiento>() {
        override fun areItemsTheSame(a: Movimiento, b: Movimiento) = a.id == b.id
        override fun areContentsTheSame(a: Movimiento, b: Movimiento) = a == b
    }
}