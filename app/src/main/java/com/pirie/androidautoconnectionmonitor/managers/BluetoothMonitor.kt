package com.pirie.androidautoconnectionmonitor.managers

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.pirie.androidautoconnectionmonitor.viewmodel.BluetoothDevice
import com.pirie.androidautoconnectionmonitor.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * BluetoothMonitor manages Bluetooth device detection and monitoring.
 * It periodically scans for connected devices and updates the ViewModel.
 */
class BluetoothMonitor(
    private val context: Context,
    private val viewModel: MainViewModel
) : DefaultLifecycleObserver {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
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
                updateConnectedDevices()
                delay(2000) // Update every 2 seconds
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
    }

    private fun updateConnectedDevices() {
        if (!hasBluetoothPermissions()) {
            return
        }

        bluetoothAdapter?.let { adapter ->
            try {
                if (adapter.isEnabled) {
                    val connectedDevices = adapter.bondedDevices?.filter { device ->
                        // Check if device is currently connected by attempting to get connection state
                        // Note: This is a simplified check. For more accurate connection state,
                        // you'd need to use BluetoothProfile listeners for specific profiles
                        device.name != null
                    }?.map { device ->
                        BluetoothDevice(
                            name = device.name ?: "Unknown Device",
                            address = device.address,
                            isHeadUnit = isLikelyHeadUnit(device.name)
                        )
                    } ?: emptyList()

                    viewModel.updateBluetoothDevices(connectedDevices)
                } else {
                    viewModel.updateBluetoothDevices(emptyList())
                }
            } catch (e: SecurityException) {
                // Handle case where permissions were revoked
                viewModel.updateBluetoothDevices(emptyList())
            }
        }
    }

    private fun hasBluetoothPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLikelyHeadUnit(deviceName: String?): Boolean {
        if (deviceName == null) return false
        
        val headUnitIndicators = listOf(
            "ram", "uconnect", "dodge", "chrysler", "jeep",
            "carplay", "android auto", "head unit", "stereo"
        )
        
        return headUnitIndicators.any { 
            deviceName.lowercase().contains(it) 
        }
    }
}