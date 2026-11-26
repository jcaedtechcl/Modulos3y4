package com.example.modulos3y4.data

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.modulos3y4.work.IssSyncWorker
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Unica fuente de verdad para la posicion de la ISS.
 *
 * La UI nunca habla con Retrofit ni con WorkManager directamente: pide la descarga
 * y observa los flows que expone este repositorio.
 */
class IssRepository(
    private val workManager: WorkManager,
    private val issPositionDao: IssPositionDao,
    private val issHistoryDao: IssHistoryDao
) {

    /** Ultima posicion descargada; emite de nuevo cuando el Worker escribe en Room. */
    val lastPosition: Flow<IssPosition?> = issPositionDao.observeLatest()

    /** Registros que el usuario decidio conservar, del mas reciente al mas antiguo. */
    val history: Flow<List<IssPositionRecord>> = issHistoryDao.observeAll()

    /** Estado de la descarga manual, para saber si esta en curso o si fallo. */
    val syncInfo: Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME)

    /**
     * Estado de la sincronizacion periodica. La UI deriva de aqui si la casilla
     * esta marcada: WorkManager ya persiste si el trabajo existe, de modo que no
     * hace falta guardar la preferencia por separado y no hay dos fuentes de
     * verdad que puedan quedar desincronizadas.
     */
    val periodicSyncInfo: Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(PERIODIC_WORK_NAME)

    /** Si no hay red, WorkManager espera a que haya en lugar de fallar. */
    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun download() {
        val request = OneTimeWorkRequestBuilder<IssSyncWorker>()
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        // KEEP: si el usuario toca "Descargar" varias veces no se encolan descargas
        // duplicadas, se respeta la que ya esta corriendo.
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /**
     * Copia una lectura al historial. La descarga por si sola no almacena nada
     * permanente: solo lo que pasa por aqui queda guardado.
     */
    suspend fun saveToHistory(position: IssPosition) {
        issHistoryDao.insert(
            IssPositionRecord(
                latitude = position.latitude,
                longitude = position.longitude,
                timestamp = position.timestamp,
                savedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteFromHistory(id: Long) = issHistoryDao.deleteById(id)

    suspend fun clearHistory() = issHistoryDao.clear()

    /**
     * Activa o desactiva la actualizacion automatica en segundo plano.
     *
     * Reutiliza el mismo IssSyncWorker de la descarga manual: el trabajo a
     * realizar es identico, solo cambia quien lo dispara.
     */
    fun setPeriodicSyncEnabled(enabled: Boolean) {
        if (!enabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<IssSyncWorker>(
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        // KEEP para no reiniciar el ciclo si ya estaba activo: con UPDATE, volver
        // a marcar la casilla retrasaria la siguiente ejecucion.
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        private const val WORK_NAME = "iss_sync"
        private const val PERIODIC_WORK_NAME = "iss_sync_periodic"

        /**
         * 15 minutos es el intervalo minimo que admite WorkManager
         * (PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS); pedir menos no
         * acelera nada, el sistema lo eleva a este valor igual.
         */
        const val SYNC_INTERVAL_MINUTES = 15L
    }
}
