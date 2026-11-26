package com.example.modulos3y4.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Informa si hay conexion util a internet.
 *
 * Existe para poder diferenciar en la UI dos situaciones que WorkManager
 * representa igual (estado ENQUEUED): el trabajo esta a punto de ejecutarse, o
 * esta detenido esperando que aparezca la red.
 *
 * Se exige NET_CAPABILITY_VALIDATED, el mismo criterio que usa el constraint
 * NetworkType.CONNECTED del Worker: una red conectada pero sin salida real a
 * internet no satisface la restriccion, y la UI no debe decir que si.
 */
class NetworkMonitor(context: Context) {

    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    val isOnline: Flow<Boolean> = callbackFlow {
        // registerNetworkCallback no notifica nada si no existe ninguna red, por
        // lo que hay que emitir el estado inicial a mano o el Flow nunca emitiria.
        trySend(isCurrentlyOnline())

        val callback = object : ConnectivityManager.NetworkCallback() {
            // Puede haber varias redes a la vez (wifi y datos): basta con una util.
            private val usableNetworks = mutableSetOf<Network>()

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                if (capabilities.isUsable()) {
                    usableNetworks += network
                } else {
                    usableNetworks -= network
                }
                trySend(usableNetworks.isNotEmpty())
            }

            override fun onLost(network: Network) {
                usableNetworks -= network
                trySend(usableNetworks.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()

    private fun isCurrentlyOnline(): Boolean {
        val active = connectivityManager.activeNetwork ?: return false
        return connectivityManager.getNetworkCapabilities(active)?.isUsable() == true
    }

    private fun NetworkCapabilities.isUsable(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
