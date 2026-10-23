package com.anmaoff.stockmanager

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anmaoff.stockmanager.data.Categoria
import com.anmaoff.stockmanager.data.CategoriaDao
import com.anmaoff.stockmanager.data.Movimiento
import com.anmaoff.stockmanager.data.MovimientoDao
import com.anmaoff.stockmanager.data.Producto
import com.anmaoff.stockmanager.data.ProductoDao
import com.anmaoff.stockmanager.data.StockDatabase
import com.anmaoff.stockmanager.data.TipoMovimiento
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MovimientoDaoTest {
    private lateinit var db: StockDatabase
    private lateinit var categorias: CategoriaDao
    private lateinit var productos: ProductoDao
    private lateinit var movimientos: MovimientoDao
    private var categoriaId = 0L

    @Before
    fun crearBaseEnMemoria() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, StockDatabase::class.java).build()
        categorias = db.categoriaDao()
        productos = db.productoDao()
        movimientos = db.movimientoDao()
        categoriaId = runBlocking { categorias.insertar(Categoria(nombre = "CPU")) }
    }

    @After
    fun cerrar() = db.close()

    private suspend fun crearProducto(codigo: String = "P001") {
        productos.insertar(Producto(codigo, "Ryzen 5 5600", "AMD", categoriaId, 5))
    }

    private fun mov(cantidad: Int = 10, tipo: String = TipoMovimiento.ENTRADA) = Movimiento(
        codigoProducto = "P001", cantidad = cantidad, tipo = tipo,
        almacen = "Central", responsable = "Ana", fecha = 1L
    )

    @Test
    fun crud_completo() = runBlocking {
        crearProducto()
        val id = movimientos.insertar(mov())                          // CREATE
        val leido = movimientos.obtenerPorId(id)!!                    // READ
        assertEquals(10, leido.cantidad)
        movimientos.actualizar(leido.copy(cantidad = 25))             // UPDATE
        assertEquals(25, movimientos.obtenerPorId(id)!!.cantidad)
        movimientos.eliminar(movimientos.obtenerPorId(id)!!)          // DELETE
        assertNull(movimientos.obtenerPorId(id))
    }

    @Test
    fun stock_suma_entradas_y_resta_salidas() = runBlocking {
        crearProducto()
        movimientos.insertarLote(listOf(mov(10), mov(3, TipoMovimiento.SALIDA)))
        assertEquals(7, movimientos.observarStock().first().first().stock)
    }

    @Test
    fun producto_sin_movimientos_tiene_stock_cero() = runBlocking {
        crearProducto()
        assertEquals(0, movimientos.observarStock().first().first().stock)
    }

    @Test
    fun producto_trae_su_categoria() = runBlocking {
        crearProducto()
        val item = productos.observarConCategoria().first().first()
        assertEquals("CPU", item.categoria.nombre)
    }

    @Test
    fun pendientes_y_marcar_sincronizados() = runBlocking {
        crearProducto()
        val id = movimientos.insertar(mov())
        assertEquals(1, movimientos.pendientes().size)
        movimientos.marcarSincronizados(listOf(id))
        assertTrue(movimientos.pendientes().isEmpty())
    }

    @Test(expected = Exception::class)
    fun integridad_rechaza_movimiento_de_producto_inexistente() {
        runBlocking { movimientos.insertar(mov()) }     // viola la llave foránea
    }

    @Test
    fun cascade_borra_movimientos_al_borrar_producto() = runBlocking {
        crearProducto()
        movimientos.insertar(mov())
        db.openHelper.writableDatabase.execSQL("DELETE FROM productos WHERE codigo='P001'")
        assertTrue(movimientos.buscarPorCodigo("P001").isEmpty())
    }

    @Test(expected = Exception::class)
    fun restrict_impide_borrar_categoria_con_productos() {
        runBlocking { crearProducto() }
        db.openHelper.writableDatabase.execSQL("DELETE FROM categorias WHERE id=$categoriaId")
    }
}