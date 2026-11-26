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
import com.example.modulos3y4.work.IssSyncWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Estado inmutable que consume la pantalla. */
data class WelcomeUiState(
    val position: IssPosition? = null,
    val isDownloading: Boolean = false,
    val errorMessage: String? = null
)

class WelcomeViewModel(private val repository: IssRepository) : ViewModel() {

    /**
     * El estado de la pantalla se deriva de dos fuentes: lo guardado en Room y el
     * estado del trabajo en WorkManager. Al exponerlo como StateFlow la UI siempre
     * tiene un valor con el que recomponer, incluso tras rotar la pantalla.
     */
    val uiState: StateFlow<WelcomeUiState> =
        combine(repository.lastPosition, repository.syncInfo) { position, infos ->
            val info = infos.lastOrNull()
            WelcomeUiState(
                position = position,
                isDownloading = info?.state == WorkInfo.State.ENQUEUED ||
                    info?.state == WorkInfo.State.RUNNING ||
                    info?.state == WorkInfo.State.BLOCKED,
                errorMessage = if (info?.state == WorkInfo.State.FAILED) {
                    info.outputData.getString(IssSyncWorker.KEY_ERROR)
                        ?: "No se pudo completar la descarga"
                } else {
                    null
                }
            )
        }.stateIn(
            scope = viewModelScope,
            // Deja de observar 5 s despues de que la UI desaparece, para sobrevivir
            // a cambios de configuracion sin reiniciar la suscripcion.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WelcomeUiState()
        )

    fun onDownloadClick() = repository.download()

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
                    IssRepository(
                        workManager = WorkManager.getInstance(application),
                        issPositionDao = AppDatabase.getInstance(application).issPositionDao()
                    )
                )
            }
        }
    }
}
