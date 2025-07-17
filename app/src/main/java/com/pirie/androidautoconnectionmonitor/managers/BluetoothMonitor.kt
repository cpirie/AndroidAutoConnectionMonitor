package com.pirie.androidautoconnectionmonitor.managers

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.pirie.androidautoconnectionmonitor.viewmodel.BluetoothDevice
import com.pirie.androidautoconnectionmonitor.viewmodel.BluetoothConnectionState
import com.pirie.androidautoconnectionmonitor.viewmodel.LogEntry
import com.pirie.androidautoconnectionmonitor.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * BluetoothMonitor manages Bluetooth device detection and monitoring.
 * It uses profile listeners and broadcast receivers to track real-time connection states.
 */
class BluetoothMonitor(
    private val context: Context,
    private val viewModel: MainViewModel
) : DefaultLifecycleObserver {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var monitoringJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Profile listeners for accurate connection state tracking
    private var a2dpProfile: BluetoothA2dp? = null
    private var headsetProfile: BluetoothHeadset? = null
    private val connectedDevices = mutableMapOf<String, BluetoothConnectionState>()
    
    // Broadcast receiver for connection state changes
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AndroidBluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE, AndroidBluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<AndroidBluetoothDevice>(AndroidBluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { handleConnectionStateChange(it, BluetoothAdapter.STATE_CONNECTED) }
                }
                AndroidBluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE, AndroidBluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<AndroidBluetoothDevice>(AndroidBluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { handleConnectionStateChange(it, BluetoothAdapter.STATE_DISCONNECTED) }
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE, AndroidBluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra<AndroidBluetoothDevice>(AndroidBluetoothDevice.EXTRA_DEVICE)
                    }
                    val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)
                    device?.let { handleProfileConnectionStateChange(it, state) }
                }
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        setupBluetoothProfiles()
        registerBluetoothReceiver()
        startMonitoring()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        stopMonitoring()
        unregisterBluetoothReceiver()
        closeBluetoothProfiles()
    }

    private fun setupBluetoothProfiles() {
        if (!hasBluetoothPermissions()) return
        
        bluetoothAdapter?.let { adapter ->
            try {
                // Get A2DP profile for audio connections
                adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (profile == BluetoothProfile.A2DP) {
                            a2dpProfile = proxy as BluetoothA2dp
                        }
                    }
                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.A2DP) {
                            a2dpProfile = null
                        }
                    }
                }, BluetoothProfile.A2DP)

                // Get Headset profile for hands-free connections
                adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        if (profile == BluetoothProfile.HEADSET) {
                            headsetProfile = proxy as BluetoothHeadset
                        }
                    }
                    override fun onServiceDisconnected(profile: Int) {
                        if (profile == BluetoothProfile.HEADSET) {
                            headsetProfile = null
                        }
                    }
                }, BluetoothProfile.HEADSET)
            } catch (e: SecurityException) {
                Log.w("BluetoothMonitor", "Failed to setup Bluetooth profiles", e)
            }
        }
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(AndroidBluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(AndroidBluetoothDevice.ACTION_ACL_DISCONNECTED) 
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }

    private fun unregisterBluetoothReceiver() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver wasn't registered
        }
    }

    private fun closeBluetoothProfiles() {
        bluetoothAdapter?.let { adapter ->
            a2dpProfile?.let { adapter.closeProfileProxy(BluetoothProfile.A2DP, it) }
            headsetProfile?.let { adapter.closeProfileProxy(BluetoothProfile.HEADSET, it) }
        }
    }

    private fun startMonitoring() {
        monitoringJob = coroutineScope.launch {
            while (true) {
                updateConnectedDevices()
                delay(3000) // Update every 3 seconds
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
    }

    private fun handleConnectionStateChange(device: AndroidBluetoothDevice, state: Int) {
        if (!hasBluetoothPermissions()) return
        
        val connectionState = when (state) {
            BluetoothAdapter.STATE_CONNECTED -> BluetoothConnectionState.CONNECTED
            BluetoothAdapter.STATE_CONNECTING -> BluetoothConnectionState.CONNECTING
            BluetoothAdapter.STATE_DISCONNECTED -> BluetoothConnectionState.DISCONNECTED
            else -> BluetoothConnectionState.UNKNOWN
        }
        
        connectedDevices[device.address] = connectionState
        
        try {
            // Log the connection event
            val deviceName = device.name ?: "Unknown Device"
            val logMessage = "$deviceName (${device.address}) ${connectionState.name.lowercase()}"
            viewModel.addLogEntry(LogEntry(
                tag = "BluetoothMonitor",
                message = logMessage,
                isError = connectionState == BluetoothConnectionState.DISCONNECTED && isLikelyHeadUnit(deviceName)
            ))
        } catch (e: SecurityException) {
            // Handle case where BLUETOOTH_CONNECT permission was revoked
            Log.w("BluetoothMonitor", "Permission denied when accessing device name", e)
        }
        
        updateConnectedDevices()
    }
    
    private fun handleProfileConnectionStateChange(device: AndroidBluetoothDevice, state: Int) {
        if (!hasBluetoothPermissions()) return
        
        val connectionState = when (state) {
            BluetoothProfile.STATE_CONNECTED -> BluetoothConnectionState.CONNECTED
            BluetoothProfile.STATE_CONNECTING -> BluetoothConnectionState.CONNECTING
            BluetoothProfile.STATE_DISCONNECTED -> BluetoothConnectionState.DISCONNECTED
            else -> BluetoothConnectionState.UNKNOWN
        }
        
        connectedDevices[device.address] = connectionState
        
        try {
            // Log profile-specific events for head units
            val deviceName = device.name
            if (deviceName != null && isLikelyHeadUnit(deviceName)) {
                val logMessage = "Head unit $deviceName profile ${connectionState.name.lowercase()}"
                viewModel.addLogEntry(LogEntry(
                    tag = "BluetoothProfile",
                    message = logMessage,
                    isError = connectionState == BluetoothConnectionState.DISCONNECTED,
                    isWarning = connectionState == BluetoothConnectionState.CONNECTING
                ))
            }
        } catch (e: SecurityException) {
            // Handle case where BLUETOOTH_CONNECT permission was revoked
            Log.w("BluetoothMonitor", "Permission denied when accessing device name for profile", e)
        }
        
        updateConnectedDevices()
    }

    private fun updateConnectedDevices() {
        if (!hasBluetoothPermissions()) {
            return
        }

        bluetoothAdapter?.let { adapter ->
            try {
                if (adapter.isEnabled) {
                    val devices = mutableListOf<BluetoothDevice>()
                    
                    // Get devices from A2DP profile
                    a2dpProfile?.connectedDevices?.forEach { device ->
                        val connectionState = a2dpProfile?.getConnectionState(device) ?: BluetoothProfile.STATE_DISCONNECTED
                        if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                            try {
                                val deviceName = device.name ?: "Unknown Device"
                                devices.add(BluetoothDevice(
                                    name = deviceName,
                                    address = device.address,
                                    isHeadUnit = isLikelyHeadUnit(deviceName),
                                    connectionState = BluetoothConnectionState.CONNECTED
                                ))
                            } catch (e: SecurityException) {
                                // Skip this device if we can't access its name
                                Log.w("BluetoothMonitor", "Permission denied when accessing A2DP device name", e)
                            }
                        }
                    }
                    
                    // Get devices from Headset profile (for hands-free)
                    headsetProfile?.connectedDevices?.forEach { device ->
                        val connectionState = headsetProfile?.getConnectionState(device) ?: BluetoothProfile.STATE_DISCONNECTED
                        if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                            // Avoid duplicates if device is already in A2DP list
                            if (devices.none { it.address == device.address }) {
                                try {
                                    val deviceName = device.name ?: "Unknown Device"
                                    devices.add(BluetoothDevice(
                                        name = deviceName,
                                        address = device.address,
                                        isHeadUnit = isLikelyHeadUnit(deviceName),
                                        connectionState = BluetoothConnectionState.CONNECTED
                                    ))
                                } catch (e: SecurityException) {
                                    // Skip this device if we can't access its name
                                    Log.w("BluetoothMonitor", "Permission denied when accessing Headset device name", e)
                                }
                            }
                        }
                    }
                    
                    // Add any other bonded devices that might be connected but not in profiles
                    adapter.bondedDevices?.forEach { device ->
                        if (devices.none { it.address == device.address }) {
                            val storedState = connectedDevices[device.address] ?: BluetoothConnectionState.UNKNOWN
                            if (storedState == BluetoothConnectionState.CONNECTED) {
                                try {
                                    val deviceName = device.name ?: "Unknown Device"
                                    devices.add(BluetoothDevice(
                                        name = deviceName,
                                        address = device.address,
                                        isHeadUnit = isLikelyHeadUnit(deviceName),
                                        connectionState = storedState
                                    ))
                                } catch (e: SecurityException) {
                                    // Skip this device if we can't access its name
                                    Log.w("BluetoothMonitor", "Permission denied when accessing bonded device name", e)
                                }
                            }
                        }
                    }

                    viewModel.updateBluetoothDevices(devices)
                } else {
                    viewModel.updateBluetoothDevices(emptyList())
                }
            } catch (e: SecurityException) {
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
            "carplay", "android auto", "head unit", "stereo", "hands-free"
        )
        
        return headUnitIndicators.any { 
            deviceName.lowercase().contains(it) 
        }
    }
}