package com.mtlc.studyplan.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(
    private val context: Context
) {

    private val _isOnline = MutableStateFlow(isCurrentlyOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _connectionType = MutableStateFlow(getConnectionType())
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                _connectionType.value = getConnectionType()
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
                _connectionType.value = ConnectionType.NONE
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                _connectionType.value = getConnectionType()
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
    }


    private fun isCurrentlyOnline(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun getConnectionType(): ConnectionType {
        if (!isCurrentlyOnline()) return ConnectionType.NONE

        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            when {
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.WIFI
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.CELLULAR
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> ConnectionType.ETHERNET
                else -> ConnectionType.OTHER
            }
        } catch (e: Exception) {
            ConnectionType.OTHER
        }
    }

    fun cleanup() {
        networkCallback?.let { callback ->
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                // Handle unregistration failure
            }
        }

        networkCallback = null
    }
}

enum class ConnectionType {
    NONE,
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER
}