package com.example.modulos3y4.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.modulos3y4.data.AppDatabase
import com.example.modulos3y4.data.remote.IssFirestoreSource
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/**
 * Sube a Firestore los registros del historial que siguen pendientes.
 *
 * Va por WorkManager y no por una corutina de la pantalla porque la
 * sincronizacion debe completarse aunque el usuario cierre la app apenas
 * guarda, y reintentarse sola cuando vuelva la conexion.
 */
class FirestoreSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val source = IssFirestoreSource()

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).issHistoryDao()
        val pending = dao.pendingSync()
        if (pending.isEmpty()) return Result.success()

        return try {
            // Se marca registro por registro, no al final del lote: si el proceso
            // muere a mitad de camino, lo ya subido no se vuelve a enviar.
            pending.forEach { record ->
                // El Task de set() solo se completa cuando el servidor confirma la
                // escritura. Si Firestore no logra confirmar (sin red, permisos
                // denegados) no falla: la deja en su cola interna y reintenta solo,
                // dejando este Worker colgado hasta el limite de 10 minutos de
                // WorkManager. El timeout lo corta y deja que el backoff decida.
                withTimeoutOrNull(UPLOAD_TIMEOUT_MS) { source.upload(record) }
                    ?: return@doWork retryOrFail("La subida a Firestore tardó demasiado")

                dao.markSynced(record.id)
            }
            Result.success()
        } catch (_: IOException) {
            retryOrFail("No se pudo subir el historial. Revisa tu conexión.")
        } catch (e: Exception) {
            // Firestore lanza FirebaseFirestoreException ante fallos de red o de
            // permisos; el reintento cubre los transitorios y el backoff evita
            // insistir de inmediato cuando el problema son las reglas de acceso.
            retryOrFail(e.message ?: "No se pudo sincronizar con Firestore")
        }
    }

    private fun retryOrFail(message: String): Result =
        if (runAttemptCount < MAX_ATTEMPTS) {
            Result.retry()
        } else {
            Result.failure(workDataOf(KEY_ERROR to message))
        }

    companion object {
        const val KEY_ERROR = "error"
        private const val MAX_ATTEMPTS = 5
        private const val UPLOAD_TIMEOUT_MS = 20_000L
    }
}
