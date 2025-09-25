package com.mtlc.studyplan.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Comprehensive Network Status Management
 * Provides real-time network connectivity monitoring and status
 */
object NetworkHelper {

    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _networkState = MutableStateFlow(NetworkState.UNKNOWN)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _isOnline = MutableLiveData<Boolean>()
    val isOnlineLiveData: LiveData<Boolean> = _isOnline

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        registerNetworkCallback()
        updateNetworkState()
        isInitialized = true
    }

    fun isOnline(): Boolean {
        if (!isInitialized) return false

        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun getNetworkState(): NetworkState {
        return _networkState.value
    }

    fun getConnectionType(): ConnectionType {
        if (!isInitialized || !isOnline()) return ConnectionType.NONE

        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        return when {
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> ConnectionType.WIFI
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> ConnectionType.CELLULAR
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> ConnectionType.ETHERNET
            else -> ConnectionType.OTHER
        }
    }

    suspend fun testInternetConnectivity(): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53), 3000)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getNetworkQuality(): NetworkQuality {
        if (!isOnline()) return NetworkQuality.NO_CONNECTION

        val connectionType = getConnectionType()
        return when (connectionType) {
            ConnectionType.WIFI, ConnectionType.ETHERNET -> NetworkQuality.EXCELLENT
            ConnectionType.CELLULAR -> {
                // In a real implementation, you could check signal strength
                // For now, assume good cellular connection
                NetworkQuality.GOOD
            }
            ConnectionType.OTHER -> NetworkQuality.FAIR
            ConnectionType.NONE -> NetworkQuality.NO_CONNECTION
        }
    }

    private fun registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    updateNetworkState()
                }

                override fun onLost(network: Network) {
                    updateNetworkState()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    updateNetworkState()
                }
            }

            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback?.let { callback ->
                connectivityManager.registerNetworkCallback(networkRequest, callback)
            }
        }
    }

    private fun updateNetworkState() {
        val isConnected = isOnline()
        val quality = getNetworkQuality()

        val newState = when {
            !isConnected -> NetworkState.DISCONNECTED
            quality == NetworkQuality.EXCELLENT || quality == NetworkQuality.GOOD -> NetworkState.CONNECTED
            quality == NetworkQuality.FAIR -> NetworkState.POOR
            else -> NetworkState.DISCONNECTED
        }

        _networkState.value = newState
        _isOnline.postValue(isConnected)
    }

    fun unregisterNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback?.let { callback ->
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (e: Exception) {
                    // Callback might not be registered
                }
            }
        }
        networkCallback = null
        isInitialized = false
    }

    // Utility methods for different scenarios
    fun canSync(): Boolean {
        return isOnline() && getNetworkQuality() != NetworkQuality.NO_CONNECTION
    }

    fun shouldAvoidHeavyOperations(): Boolean {
        val quality = getNetworkQuality()
        return quality == NetworkQuality.POOR || getConnectionType() == ConnectionType.CELLULAR
    }

    fun getConnectionStatusMessage(): String {
        return when (_networkState.value) {
            NetworkState.CONNECTED -> {
                val type = getConnectionType()
                val quality = getNetworkQuality()
                "Connected via ${type.displayName} (${quality.displayName})"
            }
            NetworkState.POOR -> "Poor connection - some features may be limited"
            NetworkState.DISCONNECTED -> "No internet connection"
            NetworkState.UNKNOWN -> "Checking connection..."
        }
    }
}

enum class NetworkState {
    CONNECTED,
    DISCONNECTED,
    POOR,
    UNKNOWN
}

enum class ConnectionType(val displayName: String) {
    WIFI("Wi-Fi"),
    CELLULAR("Mobile Data"),
    ETHERNET("Ethernet"),
    OTHER("Other"),
    NONE("None")
}

enum class NetworkQuality(val displayName: String) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    FAIR("Fair"),
    POOR("Poor"),
    NO_CONNECTION("No Connection")
}