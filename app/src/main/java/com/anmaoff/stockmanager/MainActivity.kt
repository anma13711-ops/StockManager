package com.anmaoff.stockmanager

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.anmaoff.stockmanager.data.Movimiento
import com.anmaoff.stockmanager.data.ProductoConCategoria
import com.anmaoff.stockmanager.data.StockResumen
import com.anmaoff.stockmanager.data.TipoMovimiento
import com.anmaoff.stockmanager.databinding.ActivityMainBinding
import com.anmaoff.stockmanager.ui.InventarioViewModel
import com.anmaoff.stockmanager.ui.MovimientoAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MovimientoAdapter
    private val vm: InventarioViewModel by viewModels()

    private var productos: List<ProductoConCategoria> = emptyList()
    private var stock: List<StockResumen> = emptyList()

    // Si no es null, el formulario está en modo "editar".
    private var editando: Movimiento? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        theme.applyStyle(R.style.ThemeOverlay_StockManager, true)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        adapter = MovimientoAdapter(
            onEditar = ::cargarParaEditar,
            onEliminar = ::confirmarEliminar
        )
        binding.rvMovimientos.layoutManager = LinearLayoutManager(this)
        binding.rvMovimientos.adapter = adapter
        com.anmaoff.stockmanager.sync.SyncScheduler.programar(this)
        binding.etAlmacen.setText(ALMACEN_POR_DEFECTO)
        binding.btnGuardar.setOnClickListener { guardar() }
        binding.btnCancelar.setOnClickListener { limpiar() }

        // La pantalla observa la base de datos y se actualiza sola.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.movimientos.collect { adapter.submitList(it) } }
                launch {
                    vm.productos.collect { lista ->
                        productos = lista
                        val seleccion = binding.spProducto.selectedItemPosition
                        binding.spProducto.adapter = ArrayAdapter(
                            this@MainActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            lista.map { "${it.producto.codigo} · ${it.producto.nombre}" }
                        )
                        if (seleccion in lista.indices) binding.spProducto.setSelection(seleccion)
                    }
                }
                launch {
                    vm.stock.collect { lista ->
                        stock = lista
                        binding.tvStock.text = if (lista.isEmpty()) "Sin productos" else
                            lista.joinToString("\n") {
                                val alerta = if (it.stock <= it.stockMinimo) "  ⚠ STOCK BAJO" else ""
                                "${it.codigo} (${it.categoria}): ${it.stock} uds$alerta"
                            }
                    }
                }
            }
        }
    }

    private fun guardar() {
        val producto = productos.getOrNull(binding.spProducto.selectedItemPosition)?.producto
        val cantidad = binding.etCantidad.text.toString().toIntOrNull()
        val almacen = binding.etAlmacen.text.toString().trim()
        val responsable = binding.etResponsable.text.toString().trim()

        if (producto == null) { aviso("Selecciona un producto"); return }
        if (cantidad == null || cantidad <= 0) { aviso("La cantidad debe ser mayor a cero"); return }
        if (almacen.isEmpty()) { aviso("Escribe el almacén"); return }
        if (responsable.isEmpty()) { aviso("Escribe el nombre del responsable"); return }

        val tipo = if (binding.rbEntrada.isChecked) TipoMovimiento.ENTRADA else TipoMovimiento.SALIDA
        val actual = editando
        if (actual == null) {
            // Regla de negocio: no se puede sacar más de lo que hay.
            if (tipo == TipoMovimiento.SALIDA) {
                val disponible = stock.firstOrNull { it.codigo == producto.codigo }?.stock ?: 0
                if (cantidad > disponible) {
                    aviso("Stock insuficiente: solo hay $disponible unidades"); return
                }
            }
            vm.registrar(producto.codigo, cantidad, tipo, almacen, responsable)        // CREATE
        } else {
            vm.actualizar(actual.copy(cantidad = cantidad, tipo = tipo,
                almacen = almacen, responsable = responsable))                          // UPDATE
        }
        limpiar()
    }

    private fun cargarParaEditar(m: Movimiento) {
        editando = m
        binding.spProducto.setSelection(
            productos.indexOfFirst { it.producto.codigo == m.codigoProducto }.coerceAtLeast(0)
        )
        binding.spProducto.isEnabled = false        // el producto no se cambia al editar
        binding.etCantidad.setText(m.cantidad.toString())
        binding.etAlmacen.setText(m.almacen)
        binding.etResponsable.setText(m.responsable)
        binding.rbEntrada.isChecked = m.tipo == TipoMovimiento.ENTRADA
        binding.rbSalida.isChecked = m.tipo == TipoMovimiento.SALIDA
        binding.btnGuardar.text = "Guardar cambios"
        binding.btnCancelar.visibility = View.VISIBLE
    }

    private fun confirmarEliminar(m: Movimiento) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar movimiento")
            .setMessage("¿Eliminar ${m.tipo.lowercase()} de ${m.cantidad} uds de ${m.codigoProducto}?")
            .setPositiveButton("Eliminar") { _, _ -> vm.eliminar(m) }   // DELETE
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun limpiar() {
        editando = null
        binding.spProducto.isEnabled = true
        binding.etCantidad.setText("")
        binding.etAlmacen.setText(ALMACEN_POR_DEFECTO)
        binding.etResponsable.setText("")
        binding.rbEntrada.isChecked = true
        binding.btnGuardar.text = "Registrar"
        binding.btnCancelar.visibility = View.GONE
    }

    private fun aviso(texto: String) = Toast.makeText(this, texto, Toast.LENGTH_SHORT).show()

    private companion object {
        const val ALMACEN_POR_DEFECTO = "Almacén Central"
    }
}