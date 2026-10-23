package com.anmaoff.stockmanager.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anmaoff.stockmanager.data.Movimiento
import com.anmaoff.stockmanager.data.StockDatabase
import kotlinx.coroutines.delay
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Simulación del servidor central de la empresa.
 * En un proyecto real aquí iría una llamada HTTP (por ejemplo con Retrofit):
 *   @POST("movimientos/lote") suspend fun enviar(@Body lote: List<MovimientoDto>)
 */
object ServidorCentral {
    suspend fun enviar(lote: List<Movimiento>): Boolean {
        delay(800)      // simula la latencia de la red
        return true     // simula una respuesta HTTP 200
    }
}

/** Tarea en segundo plano: envía los movimientos pendientes en lotes. */
class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val dao = StockDatabase.get(applicationContext).movimientoDao()
        return try {
            while (true) {
                val lote = dao.pendientes(limite = 100)
                if (lote.isEmpty()) break
                if (!ServidorCentral.enviar(lote)) return Result.retry()
                // Solo se marcan como sincronizados DESPUÉS de que el servidor respondió bien.
                dao.marcarSincronizados(lote.map { it.id })
            }
            Result.success()
        } catch (e: IOException) {
            Result.retry()      // sin red: WorkManager lo reintenta más tarde
        }
    }
}

object SyncScheduler {
    private const val NOMBRE = "sync_movimientos"

    /** La tarea solo se ejecuta cuando el teléfono tiene conexión a internet. */
    fun programar(context: Context) {
        val peticion = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.APPEND_OR_REPLACE, peticion)
    }
}