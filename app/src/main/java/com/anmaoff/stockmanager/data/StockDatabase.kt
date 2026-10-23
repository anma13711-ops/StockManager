package com.anmaoff.stockmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Categoria::class, Producto::class, Movimiento::class],
    version = 1,
    exportSchema = false
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun categoriaDao(): CategoriaDao
    abstract fun productoDao(): ProductoDao
    abstract fun movimientoDao(): MovimientoDao

    companion object {
        @Volatile private var instancia: StockDatabase? = null

        // Singleton: una sola conexión abierta para toda la app.
        fun get(context: Context): StockDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stockmanager.db"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        // Datos iniciales: solo se cargan cuando la base se crea por primera vez.
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            val categorias = listOf(
                                "CPU", "GPU", "Memoria RAM", "Almacenamiento",
                                "Tarjeta madre", "Fuente de poder"
                            )
                            categorias.forEach {
                                db.execSQL("INSERT INTO categorias (nombre) VALUES ('$it')")
                            }
                            // Productos de ejemplo (categoriaId 1 = CPU, 2 = GPU, 3 = RAM, 4 = Almacenamiento)
                            db.execSQL("INSERT INTO productos VALUES ('CPU-R5-5600', 'Ryzen 5 5600', 'AMD', 1, 5)")
                            db.execSQL("INSERT INTO productos VALUES ('GPU-RTX-4060', 'GeForce RTX 4060', 'NVIDIA', 2, 3)")
                            db.execSQL("INSERT INTO productos VALUES ('RAM-DDR5-16', 'Fury Beast 16GB DDR5', 'Kingston', 3, 10)")
                            db.execSQL("INSERT INTO productos VALUES ('SSD-NVME-1TB', 'NV2 1TB NVMe', 'Kingston', 4, 8)")
                        }
                    })
                    .build().also { instancia = it }
            }
    }
}