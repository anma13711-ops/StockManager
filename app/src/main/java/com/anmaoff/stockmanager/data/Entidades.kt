package com.anmaoff.stockmanager.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Familia de componentes: CPU, GPU, RAM, SSD, etc. (lado "uno" de Categoría -> Productos). */
@Entity(
    tableName = "categorias",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String
)

/**
 * Componente de PC del catálogo.
 * RESTRICT: no se puede borrar una categoría que todavía tiene productos.
 */
@Entity(
    tableName = "productos",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["categoriaId"])]
)
data class Producto(
    @PrimaryKey val codigo: String,      // SKU del componente
    val nombre: String,
    val marca: String,
    val categoriaId: Long,
    val stockMinimo: Int = 5             // por debajo de esto se muestra alerta
)

/**
 * Entrada o salida de mercancía (lado "muchos" de Producto -> Movimientos).
 * CASCADE: si se borra un producto, se borran sus movimientos.
 */
@Entity(
    tableName = "movimientos",
    foreignKeys = [
        ForeignKey(
            entity = Producto::class,
            parentColumns = ["codigo"],
            childColumns = ["codigoProducto"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["codigoProducto"]),
        Index(value = ["sincronizado", "fecha"])
    ]
)
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val codigoProducto: String,
    val cantidad: Int,
    val tipo: String,                    // "ENTRADA" o "SALIDA"
    val almacen: String,
    val responsable: String,
    val fecha: Long,                     // milisegundos desde epoch
    val sincronizado: Boolean = false
)

/** Relación de Room: un producto junto con su categoría (JOIN automático). */
data class ProductoConCategoria(
    @Embedded val producto: Producto,
    @Relation(parentColumn = "categoriaId", entityColumn = "id")
    val categoria: Categoria
)

/** Resultado de la consulta de stock: solo las columnas que la pantalla necesita. */
data class StockResumen(
    val codigo: String,
    val nombre: String,
    val marca: String,
    val categoria: String,
    val stockMinimo: Int,
    val stock: Int
)

object TipoMovimiento {
    const val ENTRADA = "ENTRADA"
    const val SALIDA = "SALIDA"
}