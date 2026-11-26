package com.example.modulos3y4.ui.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.modulos3y4.data.IssPosition
import com.example.modulos3y4.data.IssRepository
import com.example.modulos3y4.ui.theme.Modulos3y4Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Parte con estado: obtiene el ViewModel y observa su StateFlow. Se separa de
 * [WelcomeScreen] para que la pantalla siga siendo una funcion sin estado,
 * testeable y visible en los @Preview.
 */
@Composable
fun WelcomeRoute(
    username: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = viewModel(factory = WelcomeViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WelcomeScreen(
        username = username,
        uiState = uiState,
        onDownload = viewModel::onDownloadClick,
        onPeriodicSyncChange = viewModel::onPeriodicSyncChange,
        onLogout = onLogout,
        modifier = modifier
    )
}

@Composable
fun WelcomeScreen(
    username: String,
    uiState: WelcomeUiState,
    onDownload: () -> Unit,
    onPeriodicSyncChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¡Bienvenido, $username!",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Has iniciado sesión correctamente.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onDownload,
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = when (uiState.status) {
                    DownloadStatus.IDLE -> "Descargar posición de la ISS"
                    DownloadStatus.WAITING_FOR_NETWORK -> "Esperando conexión…"
                    DownloadStatus.DOWNLOADING -> "Descargando…"
                    DownloadStatus.RETRYING -> "Reintentando…"
                }
            )
        }

        // Sin este aviso el boton deshabilitado con su spinner parece colgado:
        // el trabajo sigue encolado y se ejecutara solo al volver la conexion.
        if (uiState.status == DownloadStatus.WAITING_FOR_NETWORK) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sin conexión. La descarga se reanudará automáticamente.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        PeriodicSyncOption(
            checked = uiState.isPeriodicSyncEnabled,
            onCheckedChange = onPeriodicSyncChange
        )

        uiState.position?.let { position ->
            Spacer(modifier = Modifier.height(16.dp))
            IssPositionCard(position = position)
        }

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = onLogout) {
            Text("Cerrar sesión")
        }
    }
}

/**
 * El Row completo es la zona tactil, no solo la casilla: con toggleable el
 * lector de pantalla anuncia una sola accion con rol de checkbox, por eso el
 * Checkbox recibe onCheckedChange = null y no vuelve a capturar el toque.
 */
@Composable
private fun PeriodicSyncOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Checkbox
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Actualizar automáticamente",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "En segundo plano, cada ${IssRepository.SYNC_INTERVAL_MINUTES} minutos.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun IssPositionCard(position: IssPosition, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Estación Espacial Internacional",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Latitud: ${formatCoordinate(position.latitude)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Longitud: ${formatCoordinate(position.longitude)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Medición de la API: ${formatInstant(position.timestamp * 1_000)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Descargado: ${formatInstant(position.fetchedAt)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatCoordinate(value: Double): String =
    String.format(Locale.getDefault(), "%.4f°", value)

// SimpleDateFormat en lugar de java.time: minSdk es 24 y asi se evita tener que
// habilitar el desugaring de la libreria estandar de Java.
private fun formatInstant(epochMillis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(epochMillis))

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    Modulos3y4Theme {
        WelcomeScreen(
            username = "usuario",
            uiState = WelcomeUiState(
                position = IssPosition(
                    latitude = -33.4489,
                    longitude = -70.6693,
                    timestamp = 1764100000,
                    fetchedAt = 1764100000_000
                )
            ),
            onDownload = {},
            onPeriodicSyncChange = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenDownloadingPreview() {
    Modulos3y4Theme {
        WelcomeScreen(
            username = "usuario",
            uiState = WelcomeUiState(status = DownloadStatus.DOWNLOADING),
            onDownload = {},
            onPeriodicSyncChange = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPeriodicSyncEnabledPreview() {
    Modulos3y4Theme {
        WelcomeScreen(
            username = "usuario",
            uiState = WelcomeUiState(isPeriodicSyncEnabled = true),
            onDownload = {},
            onPeriodicSyncChange = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenWaitingForNetworkPreview() {
    Modulos3y4Theme {
        WelcomeScreen(
            username = "usuario",
            uiState = WelcomeUiState(status = DownloadStatus.WAITING_FOR_NETWORK),
            onDownload = {},
            onPeriodicSyncChange = {},
            onLogout = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenErrorPreview() {
    Modulos3y4Theme {
        WelcomeScreen(
            username = "usuario",
            uiState = WelcomeUiState(errorMessage = "No se pudo conectar con la API. Revisa tu conexión."),
            onDownload = {},
            onPeriodicSyncChange = {},
            onLogout = {}
        )
    }
}
