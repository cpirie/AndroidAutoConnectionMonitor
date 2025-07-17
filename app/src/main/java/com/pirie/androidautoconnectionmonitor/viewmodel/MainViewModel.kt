package com.pirie.androidautoconnectionmonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MainViewModel serves as the central state holder for the application.
 * It maintains the state of all UI components and is observed by the UI.
 */
class MainViewModel : ViewModel() {

    // Car connection state
    private val _carConnectionState = MutableStateFlow(CarConnectionState.DISCONNECTED)
    val carConnectionState: StateFlow<CarConnectionState> = _carConnectionState.asStateFlow()

    // Wi-Fi health metrics
    private val _wifiHealth = MutableStateFlow(WifiHealth())
    val wifiHealth: StateFlow<WifiHealth> = _wifiHealth.asStateFlow()

    // Bluetooth connected devices
    private val _bluetoothDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val bluetoothDevices: StateFlow<List<BluetoothDevice>> = _bluetoothDevices.asStateFlow()

    // Logcat stream
    private val _logcatStream = MutableStateFlow<List<LogEntry>>(emptyList())
    val logcatStream: StateFlow<List<LogEntry>> = _logcatStream.asStateFlow()

    // Recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // Functions to update state
    fun updateCarConnectionState(state: CarConnectionState) {
        _carConnectionState.value = state
    }

    fun updateWifiHealth(health: WifiHealth) {
        _wifiHealth.value = health
    }

    fun updateBluetoothDevices(devices: List<BluetoothDevice>) {
        _bluetoothDevices.value = devices
    }

    fun addLogEntry(entry: LogEntry) {
        viewModelScope.launch {
            val currentList = _logcatStream.value.toMutableList()
            currentList.add(entry)
            // Keep only the last 100 entries to avoid excessive memory usage
            if (currentList.size > 100) {
                currentList.removeAt(0)
            }
            _logcatStream.value = currentList
        }
    }

    fun startRecording() {
        _isRecording.value = true
    }

    fun stopRecording() {
        _isRecording.value = false
    }
}

// Data classes for the state
enum class CarConnectionState {
    CONNECTED, CONNECTING, DISCONNECTED
}

data class WifiHealth(
    val headUnitRssi: Int = 0, // Signal strength in dBm
    val channel: Int = 0,      // Wi-Fi channel
    val networkCongestion: Int = 0 // Count of networks on same/adjacent channels
)

data class BluetoothDevice(
    val name: String,
    val address: String,
    val isHeadUnit: Boolean = false,
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.UNKNOWN
)

enum class BluetoothConnectionState {
    CONNECTED, DISCONNECTED, CONNECTING, UNKNOWN
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val tag: String,
    val message: String,
    val isError: Boolean = false,
    val isWarning: Boolean = false
)
