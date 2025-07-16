package com.pirie.androidautoconnectionmonitor.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.pirie.androidautoconnectionmonitor.viewmodel.MainViewModel
import com.pirie.androidautoconnectionmonitor.viewmodel.WifiHealth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * WifiMonitor manages Wi-Fi network scanning and health monitoring.
 * It periodically scans for networks, monitors RSSI, and calculates congestion.
 */
class WifiMonitor(
    private val context: Context,
    private val viewModel: MainViewModel
) : DefaultLifecycleObserver {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var monitoringJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        startMonitoring()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        stopMonitoring()
    }

    private fun startMonitoring() {
        monitoringJob = coroutineScope.launch {
            while (true) {
                updateWifiHealth()
                delay(3000) // Update every 3 seconds
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
    }

    private fun updateWifiHealth() {
        if (!hasLocationPermission()) {
            return
        }

        try {
            val currentWifiInfo = getCurrentWifiInfo()
            val scanResults = getScanResults()
            
            val wifiHealth = if (currentWifiInfo != null) {
                val currentChannel = getChannelFromFrequency(currentWifiInfo.frequency)
                val congestion = calculateNetworkCongestion(scanResults, currentChannel)
                
                WifiHealth(
                    headUnitRssi = currentWifiInfo.rssi,
                    channel = currentChannel,
                    networkCongestion = congestion
                )
            } else {
                WifiHealth() // Default values when not connected
            }
            
            viewModel.updateWifiHealth(wifiHealth)
        } catch (e: SecurityException) {
            // Handle case where permissions were revoked
            viewModel.updateWifiHealth(WifiHealth())
        }
    }

    private fun getCurrentWifiInfo(): WifiInfo? {
        return if (wifiManager.isWifiEnabled) {
            try {
                wifiManager.connectionInfo
            } catch (e: SecurityException) {
                null
            }
        } else {
            null
        }
    }

    private fun getScanResults(): List<ScanResult> {
        return try {
            if (wifiManager.isWifiEnabled) {
                // Trigger a new scan
                wifiManager.startScan()
                // Return cached results (scan results are updated asynchronously)
                wifiManager.scanResults ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    private fun getChannelFromFrequency(frequency: Int): Int {
        return when {
            frequency in 2412..2484 -> {
                // 2.4 GHz band: Channel = (Frequency - 2412) / 5 + 1
                (frequency - 2412) / 5 + 1
            }
            frequency in 5170..5825 -> {
                // 5 GHz band: Channel = (Frequency - 5000) / 5
                (frequency - 5000) / 5
            }
            frequency in 5955..7115 -> {
                // 6 GHz band (Wi-Fi 6E): Channel = (Frequency - 5950) / 5
                (frequency - 5950) / 5
            }
            else -> 0 // Unknown frequency
        }
    }

    private fun calculateNetworkCongestion(scanResults: List<ScanResult>, currentChannel: Int): Int {
        if (currentChannel == 0) return 0
        
        val adjacentChannels = getAdjacentChannels(currentChannel)
        
        return scanResults.count { result ->
            val channel = getChannelFromFrequency(result.frequency)
            channel == currentChannel || adjacentChannels.contains(channel)
        }
    }

    private fun getAdjacentChannels(channel: Int): List<Int> {
        return when {
            channel in 1..14 -> {
                // 2.4 GHz: Each channel overlaps with ±2 channels
                listOf(channel - 2, channel - 1, channel + 1, channel + 2).filter { it in 1..14 }
            }
            channel > 14 -> {
                // 5 GHz and 6 GHz: 20 MHz channels typically don't overlap, but check ±1 for safety
                listOf(channel - 1, channel + 1)
            }
            else -> emptyList()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if the current Wi-Fi network is likely the head unit based on SSID patterns
     */
    private fun isHeadUnitNetwork(ssid: String?): Boolean {
        if (ssid == null) return false
        
        val headUnitIndicators = listOf(
            "ram", "uconnect", "dodge", "chrysler", "jeep",
            "carplay", "android auto", "direct"
        )
        
        return headUnitIndicators.any { 
            ssid.lowercase().contains(it) 
        }
    }
}