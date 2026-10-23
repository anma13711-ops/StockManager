package com.anmaoff.stockmanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(categoria: Categoria): Long

    @Query("SELECT * FROM categorias ORDER BY nombre")
    fun observarTodas(): Flow<List<Categoria>>
}

@Dao
interface ProductoDao {
    // IGNORE: si el código ya existe, no se duplica ni se sobrescribe.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(producto: Producto): Long

    @Update
    suspend fun actualizar(producto: Producto): Int

    /** Cada producto con su categoría (Room hace el JOIN gracias a @Relation). */
    @Transaction
    @Query("SELECT * FROM productos ORDER BY codigo")
    fun observarConCategoria(): Flow<List<ProductoConCategoria>>

    @Query("SELECT * FROM productos WHERE codigo = :codigo")
    suspend fun buscarPorCodigo(codigo: String): Producto?
}

@Dao
interface MovimientoDao {

    // ---------- CREATE ----------
    @Insert
    suspend fun insertar(movimiento: Movimiento): Long

    /** Inserción por lote: Room la ejecuta en UNA sola transacción. */
    @Insert
    suspend fun insertarLote(movimientos: List<Movimiento>)

    // ---------- READ ----------
    /** Flow: la pantalla se actualiza sola cuando cambia la tabla. */
    @Query("SELECT * FROM movimientos ORDER BY fecha DESC")
    fun observarTodos(): Flow<List<Movimiento>>

    @Query("SELECT * FROM movimientos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Movimiento?

    @Query("SELECT * FROM movimientos WHERE codigoProducto = :codigo ORDER BY fecha DESC")
    suspend fun buscarPorCodigo(codigo: String): List<Movimiento>

    /** Cola de sincronización: usa el índice (sincronizado, fecha) y limita las filas. */
    @Query("SELECT * FROM movimientos WHERE sincronizado = 0 ORDER BY fecha ASC LIMIT :limite")
    suspend fun pendientes(limite: Int = 100): List<Movimiento>

    /**
     * Stock actual de cada producto, calculado dentro de SQLite (no en memoria).
     * LEFT JOIN: un producto sin movimientos aparece con stock 0.
     */
    @Query(
        """
        SELECT p.codigo AS codigo, p.nombre AS nombre, p.marca AS marca,
               c.nombre AS categoria, p.stockMinimo AS stockMinimo,
               COALESCE(SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.cantidad ELSE -m.cantidad END), 0) AS stock
        FROM productos p
        INNER JOIN categorias c ON c.id = p.categoriaId        LEFT JOIN movimientos m ON m.codigoProducto = p.codigo
        GROUP BY p.codigo
        ORDER BY p.codigo
        """
    )
    fun observarStock(): Flow<List<StockResumen>>

    // ---------- UPDATE ----------
    @Update
    suspend fun actualizar(movimiento: Movimiento): Int

    @Query("UPDATE movimientos SET sincronizado = 1 WHERE id IN (:ids)")
    suspend fun marcarSincronizados(ids: List<Long>)

    // ---------- DELETE ----------
    @Delete
    suspend fun eliminar(movimiento: Movimiento): Int
}