package com.example.modulos3y4.data

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
    private val issPositionDao: IssPositionDao
) {

    /** Ultima posicion guardada; emite de nuevo cuando el Worker escribe en Room. */
    val lastPosition: Flow<IssPosition?> = issPositionDao.observeLatest()

    /** Estado del trabajo de descarga, para saber si esta en curso o si fallo. */
    val syncInfo: Flow<List<WorkInfo>> =
        workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME)

    fun download() {
        val request = OneTimeWorkRequestBuilder<IssSyncWorker>()
            // Si no hay red, WorkManager espera a que haya en lugar de fallar.
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        // KEEP: si el usuario toca "Descargar" varias veces no se encolan descargas
        // duplicadas, se respeta la que ya esta corriendo.
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        private const val WORK_NAME = "iss_sync"
    }
}
