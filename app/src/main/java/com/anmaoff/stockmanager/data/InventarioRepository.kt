package com.anmaoff.stockmanager.data

import kotlinx.coroutines.flow.Flow

/**
 * Único punto de acceso a los datos para el resto de la app.
 * Aquí viven las reglas de negocio (validaciones); los DAO solo ejecutan SQL.
 */
class InventarioRepository(db: StockDatabase) {
    private val categorias = db.categoriaDao()
    private val productos = db.productoDao()
    private val movimientos = db.movimientoDao()

    fun observarMovimientos(): Flow<List<Movimiento>> = movimientos.observarTodos()
    fun observarStock(): Flow<List<StockResumen>> = movimientos.observarStock()
    fun observarProductos(): Flow<List<ProductoConCategoria>> = productos.observarConCategoria()
    fun observarCategorias(): Flow<List<Categoria>> = categorias.observarTodas()

    suspend fun registrarMovimiento(
        codigo: String, cantidad: Int, tipo: String, almacen: String, responsable: String
    ) {
        require(codigo.isNotBlank()) { "Selecciona un producto" }
        require(cantidad > 0) { "La cantidad debe ser mayor a cero" }
        require(almacen.isNotBlank()) { "El almacén es obligatorio" }
        require(responsable.isNotBlank()) { "El responsable es obligatorio" }
        movimientos.insertar(
            Movimiento(
                codigoProducto = codigo.trim(),
                cantidad = cantidad,
                tipo = tipo,
                almacen = almacen.trim(),
                responsable = responsable.trim(),
                fecha = System.currentTimeMillis()
            )
        )
    }

    /** Devuelve false si ya existía un producto con ese código. */
    suspend fun crearProducto(
        codigo: String, nombre: String, marca: String, categoriaId: Long, stockMinimo: Int
    ): Boolean {
        require(codigo.isNotBlank() && nombre.isNotBlank()) { "Código y nombre son obligatorios" }
        require(stockMinimo >= 0) { "El stock mínimo no puede ser negativo" }
        val filaNueva = productos.insertar(
            Producto(codigo.trim().uppercase(), nombre.trim(), marca.trim(), categoriaId, stockMinimo)
        )
        return filaNueva != -1L
    }

    /** Al editar, el registro vuelve a quedar pendiente de sincronizar. */
    suspend fun actualizarMovimiento(m: Movimiento) {
        require(m.cantidad > 0) { "La cantidad debe ser mayor a cero" }
        require(m.responsable.isNotBlank()) { "El responsable es obligatorio" }
        movimientos.actualizar(m.copy(sincronizado = false))
    }

    suspend fun eliminarMovimiento(m: Movimiento) {
        movimientos.eliminar(m)
    }
}