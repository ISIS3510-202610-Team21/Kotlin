package com.example.spendantt.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ConnectivityObserver {

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val activeNetworks = mutableSetOf<Network>()

    fun register(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        _isConnected.value = checkInternet(cm)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                activeNetworks.add(network)
                _isConnected.value = true
            }

            override fun onLost(network: Network) {
                activeNetworks.remove(network)
                if (activeNetworks.isEmpty()) {
                    _isConnected.value = false
                }
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    activeNetworks.add(network)
                    _isConnected.value = true
                } else {
                    activeNetworks.remove(network)
                    _isConnected.value = activeNetworks.isNotEmpty()
                }
            }
        }

        networkCallback = callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
    }

    fun unregister(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let {
            try { cm.unregisterNetworkCallback(it) } catch (_: Exception) {}
        }
        networkCallback = null
        activeNetworks.clear()
    }

    fun hasInternet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return checkInternet(cm)
    }

    private fun checkInternet(cm: ConnectivityManager): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
