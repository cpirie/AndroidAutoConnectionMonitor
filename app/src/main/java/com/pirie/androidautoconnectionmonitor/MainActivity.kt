package com.pirie.androidautoconnectionmonitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pirie.androidautoconnectionmonitor.managers.CarConnectionManager
import com.pirie.androidautoconnectionmonitor.managers.BluetoothMonitor
import com.pirie.androidautoconnectionmonitor.managers.WifiMonitor
import com.pirie.androidautoconnectionmonitor.managers.LogcatReader
import com.pirie.androidautoconnectionmonitor.ui.theme.AndroidAutoConnectionMonitorTheme
import com.pirie.androidautoconnectionmonitor.viewmodel.MainViewModel
import com.pirie.androidautoconnectionmonitor.viewmodel.CarConnectionState

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var carConnectionManager: CarConnectionManager
    private lateinit var bluetoothMonitor: BluetoothMonitor
    private lateinit var wifiMonitor: WifiMonitor
    private lateinit var logcatReader: LogcatReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        carConnectionManager = CarConnectionManager(applicationContext, viewModel)
        bluetoothMonitor = BluetoothMonitor(applicationContext, viewModel)
        wifiMonitor = WifiMonitor(applicationContext, viewModel)
        logcatReader = LogcatReader(applicationContext, viewModel)
        
        lifecycle.addObserver(carConnectionManager)
        lifecycle.addObserver(bluetoothMonitor)
        lifecycle.addObserver(wifiMonitor)
        lifecycle.addObserver(logcatReader)

        setContent {
            AndroidAutoConnectionMonitorTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val carConnectionState by viewModel.carConnectionState.collectAsState()
    val wifiHealth by viewModel.wifiHealth.collectAsState()
    val bluetoothDevices by viewModel.bluetoothDevices.collectAsState()
    val logEntries by viewModel.logcatStream.collectAsState()
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Check for READ_LOGS permission
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_LOGS) != PackageManager.PERMISSION_GRANTED) {
        showPermissionDialog = true
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Android Auto Connection Monitor",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            item {
                ConnectionStatusCard(carConnectionState)
            }
            
            item {
                WifiHealthCard(wifiHealth)
            }
            
            item {
                BluetoothDevicesCard(bluetoothDevices)
            }
            
            item {
                LogViewerCard(logEntries)
            }
        }

        if (showPermissionDialog) {
            ReadLogsPermissionDialog {
                showPermissionDialog = false
                // Optionally, you could guide the user to settings or provide a button to copy the ADB command.
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(connectionState: CarConnectionState) {
    val statusText = when (connectionState) {
        CarConnectionState.CONNECTED -> "Connected"
        CarConnectionState.CONNECTING -> "Connecting..."
        CarConnectionState.DISCONNECTED -> "Disconnected"
    }
    
    val statusColor = when (connectionState) {
        CarConnectionState.CONNECTED -> Color.Green
        CarConnectionState.CONNECTING -> Color.Blue
        CarConnectionState.DISCONNECTED -> Color.Red
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Android Auto Status",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "●",
                    color = statusColor,
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
fun WifiHealthCard(wifiHealth: com.pirie.androidautoconnectionmonitor.viewmodel.WifiHealth) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Wi-Fi Health",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Signal Strength", style = MaterialTheme.typography.bodyMedium)
                    val rssiColor = when {
                        wifiHealth.headUnitRssi > -67 -> Color.Green
                        wifiHealth.headUnitRssi > -80 -> Color(255, 165, 0) // Orange
                        else -> Color.Red
                    }
                    Text(
                        text = "${wifiHealth.headUnitRssi} dBm",
                        style = MaterialTheme.typography.titleMedium,
                        color = rssiColor
                    )
                }
                
                Column {
                    Text(text = "Channel", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (wifiHealth.channel > 0) "${wifiHealth.channel}" else "N/A",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Column {
                    Text(text = "Congestion", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${wifiHealth.networkCongestion} networks",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun BluetoothDevicesCard(devices: List<com.pirie.androidautoconnectionmonitor.viewmodel.BluetoothDevice>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Bluetooth Devices",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (devices.isEmpty()) {
                Text(
                    text = "No devices found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (device.isHeadUnit) "🚗" else "📱",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (device.isHeadUnit) {
                                    Text(
                                        text = "Head Unit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                val connectionColor = when (device.connectionState) {
                                    com.pirie.androidautoconnectionmonitor.viewmodel.BluetoothConnectionState.CONNECTED -> Color.Green
                                    com.pirie.androidautoconnectionmonitor.viewmodel.BluetoothConnectionState.CONNECTING -> Color.Blue
                                    com.pirie.androidautoconnectionmonitor.viewmodel.BluetoothConnectionState.DISCONNECTED -> Color.Red
                                    else -> Color.Gray
                                }
                                Text(
                                    text = "●",
                                    color = connectionColor,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = device.connectionState.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = connectionColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogViewerCard(logEntries: List<com.pirie.androidautoconnectionmonitor.viewmodel.LogEntry>) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new entries are added
    LaunchedEffect(logEntries.size) {
        if (logEntries.isNotEmpty()) {
            listState.animateScrollToItem(logEntries.size - 1)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Logs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (logEntries.isEmpty()) {
                Text(
                    text = "No logs available (READ_LOGS permission required)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp), // Fixed height for scrollable log area
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logEntries) { logEntry ->
                        LogEntryItem(logEntry)
                    }
                }
            }
        }
    }
}

@Composable
fun LogEntryItem(logEntry: com.pirie.androidautoconnectionmonitor.viewmodel.LogEntry) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeString = timeFormat.format(Date(logEntry.timestamp))
    
    val textColor = when {
        logEntry.isError -> Color.Red
        logEntry.isWarning -> Color(255, 165, 0) // Orange
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val backgroundColor = when {
        logEntry.isError -> Color.Red.copy(alpha = 0.1f)
        logEntry.isWarning -> Color(255, 165, 0).copy(alpha = 0.1f) // Orange
        else -> Color.Transparent
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = logEntry.tag,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = logEntry.message,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ReadLogsPermissionDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission Required") },
        text = {
            Text(
                "To enable log monitoring, please connect your phone to a computer with ADB and run the following command:\n\n" +
                        "adb shell pm grant ${LocalContext.current.packageName} android.permission.READ_LOGS"
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AndroidAutoConnectionMonitorTheme {
        val previewViewModel: MainViewModel = viewModel()
        // To preview different states, you can update the viewModel here, e.g.:
        // previewViewModel.updateCarConnectionState(CarConnectionState.CONNECTED)
        MainScreen(viewModel = previewViewModel)
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionDialogPreview() {
    AndroidAutoConnectionMonitorTheme {
        ReadLogsPermissionDialog { }
    }
}