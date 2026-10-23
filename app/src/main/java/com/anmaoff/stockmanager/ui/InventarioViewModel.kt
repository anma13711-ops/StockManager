package com.anmaoff.stockmanager.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anmaoff.stockmanager.data.InventarioRepository
import com.anmaoff.stockmanager.data.Movimiento
import com.anmaoff.stockmanager.data.StockDatabase
import com.anmaoff.stockmanager.sync.SyncScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Conserva el estado de la pantalla aunque se gire el teléfono y
 * ejecuta las operaciones de base de datos fuera del hilo principal.
 */
class InventarioViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = InventarioRepository(StockDatabase.get(app))

    val movimientos = repo.observarMovimientos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val stock = repo.observarStock()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val productos = repo.observarProductos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categorias = repo.observarCategorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun registrar(codigo: String, cantidad: Int, tipo: String, almacen: String, responsable: String) =
        viewModelScope.launch {
            repo.registrarMovimiento(codigo, cantidad, tipo, almacen, responsable)
            SyncScheduler.programar(getApplication())
        }

    fun actualizar(m: Movimiento) = viewModelScope.launch {
        repo.actualizarMovimiento(m)
        SyncScheduler.programar(getApplication())
    }

    fun eliminar(m: Movimiento) = viewModelScope.launch { repo.eliminarMovimiento(m) }
}