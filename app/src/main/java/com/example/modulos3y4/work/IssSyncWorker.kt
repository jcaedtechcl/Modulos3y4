package com.example.modulos3y4.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.modulos3y4.data.AppDatabase
import com.example.modulos3y4.data.IssPosition
import com.example.modulos3y4.data.remote.NetworkModule
import retrofit2.HttpException
import java.io.IOException

/**
 * Descarga la posicion actual de la ISS y la guarda en Room.
 *
 * Se usa un CoroutineWorker y no una corutina del ViewModel para que la descarga
 * sobreviva a cambios de configuracion y a que el usuario deje la app en segundo
 * plano: WorkManager mantiene el trabajo encolado y lo reintenta solo. `doWork`
 * ya se ejecuta fuera del hilo principal, por eso no hace falta un withContext.
 */
class IssSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).issPositionDao()
        return try {
            val response = NetworkModule.issApi.getIssNow()
            val latitude = response.issPosition.latitude.toDoubleOrNull()
            val longitude = response.issPosition.longitude.toDoubleOrNull()

            if (latitude == null || longitude == null) {
                // Error de datos: reintentar daria siempre el mismo resultado.
                return failure("La respuesta de la API no tiene coordenadas validas")
            }

            dao.upsert(
                IssPosition(
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = response.timestamp,
                    fetchedAt = System.currentTimeMillis()
                )
            )
            Result.success()
        } catch (_: IOException) {
            // Fallo de red o timeout: puede resolverse solo, conviene reintentar.
            if (runAttemptCount < MAX_ATTEMPTS) {
                Result.retry()
            } else {
                failure("No se pudo conectar con la API. Revisa tu conexion.")
            }
        } catch (e: HttpException) {
            failure("La API respondio con error ${e.code()}")
        } catch (_: Exception) {
            failure("No se pudo leer la respuesta de la API")
        }
    }

    /** Devuelve el mensaje a la UI a traves de los datos de salida del trabajo. */
    private fun failure(message: String) = Result.failure(workDataOf(KEY_ERROR to message))

    companion object {
        const val KEY_ERROR = "error"
        private const val MAX_ATTEMPTS = 3
    }
}
