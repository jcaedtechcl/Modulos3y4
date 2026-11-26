package com.example.modulos3y4.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.modulos3y4.data.AppDatabase
import com.example.modulos3y4.data.IssPosition
import com.example.modulos3y4.data.IssRepository
import com.example.modulos3y4.data.NetworkMonitor
import com.example.modulos3y4.work.IssSyncWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * En que punto va la descarga. WorkManager no distingue por si solo entre "ya
 * va a correr" y "detenido esperando red": ambos son el estado ENQUEUED, y
 * mostrarlos igual hace que la app parezca colgada cuando no hay conexion.
 */
enum class DownloadStatus {
    IDLE,
    WAITING_FOR_NETWORK,
    DOWNLOADING,
    RETRYING
}

/** Estado inmutable que consume la pantalla. */
data class WelcomeUiState(
    val position: IssPosition? = null,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val errorMessage: String? = null,
    val isPeriodicSyncEnabled: Boolean = false
) {
    val isBusy: Boolean get() = status != DownloadStatus.IDLE
}

class WelcomeViewModel(
    private val repository: IssRepository,
    networkMonitor: NetworkMonitor
) : ViewModel() {

    /**
     * El estado de la pantalla se deriva de tres fuentes: lo guardado en Room, el
     * estado del trabajo en WorkManager y la conectividad real. Al exponerlo como
     * StateFlow la UI siempre tiene un valor con el que recomponer, incluso tras
     * rotar la pantalla.
     */
    val uiState: StateFlow<WelcomeUiState> = combine(
        repository.lastPosition,
        repository.syncInfo,
        repository.periodicSyncInfo,
        networkMonitor.isOnline
    ) { position, infos, periodicInfos, isOnline ->
        val info = infos.lastOrNull()
        WelcomeUiState(
            position = position,
            status = info.toDownloadStatus(isOnline),
            errorMessage = if (info?.state == WorkInfo.State.FAILED) {
                info.outputData.getString(IssSyncWorker.KEY_ERROR)
                    ?: "No se pudo completar la descarga"
            } else {
                null
            },
            // Un trabajo periodico nunca llega a SUCCEEDED: alterna entre ENQUEUED
            // y RUNNING mientras sigue activo, y solo termina si se cancela.
            isPeriodicSyncEnabled = periodicInfos.any { !it.state.isFinished }
        )
    }.stateIn(
        scope = viewModelScope,
        // Deja de observar 5 s despues de que la UI desaparece, para sobrevivir
        // a cambios de configuracion sin reiniciar la suscripcion.
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WelcomeUiState()
    )

    fun onDownloadClick() = repository.download()

    fun onPeriodicSyncChange(enabled: Boolean) = repository.setPeriodicSyncEnabled(enabled)

    /**
     * runAttemptCount > 0 significa que el intento anterior fallo y el Worker
     * esta dentro de su ventana de backoff, no en su primera ejecucion.
     */
    private fun WorkInfo?.toDownloadStatus(isOnline: Boolean): DownloadStatus = when (this?.state) {
        WorkInfo.State.RUNNING ->
            if (runAttemptCount > 0) DownloadStatus.RETRYING else DownloadStatus.DOWNLOADING

        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> when {
            !isOnline -> DownloadStatus.WAITING_FOR_NETWORK
            runAttemptCount > 0 -> DownloadStatus.RETRYING
            else -> DownloadStatus.DOWNLOADING
        }

        else -> DownloadStatus.IDLE
    }

    companion object {
        /**
         * Factory manual para no arrastrar una libreria de inyeccion de dependencias
         * en un proyecto de este tamano.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("Falta el Application en el CreationExtras")
                WelcomeViewModel(
                    repository = IssRepository(
                        workManager = WorkManager.getInstance(application),
                        issPositionDao = AppDatabase.getInstance(application).issPositionDao()
                    ),
                    networkMonitor = NetworkMonitor(application)
                )
            }
        }
    }
}
