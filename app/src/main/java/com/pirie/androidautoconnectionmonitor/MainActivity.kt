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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pirie.androidautoconnectionmonitor.managers.CarConnectionManager
import com.pirie.androidautoconnectionmonitor.ui.theme.AndroidAutoConnectionMonitorTheme
import com.pirie.androidautoconnectionmonitor.viewmodel.MainViewModel
import com.pirie.androidautoconnectionmonitor.viewmodel.CarConnectionState

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var carConnectionManager: CarConnectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        carConnectionManager = CarConnectionManager(applicationContext, viewModel)
        lifecycle.addObserver(carConnectionManager)

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
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Check for READ_LOGS permission
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_LOGS) != PackageManager.PERMISSION_GRANTED) {
        showPermissionDialog = true
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Android Auto Connection Monitor",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            ConnectionStatusView(carConnectionState)
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
fun ConnectionStatusView(connectionState: CarConnectionState) {
    val statusText = when (connectionState) {
        CarConnectionState.CONNECTED -> "Status: Connected"
        CarConnectionState.CONNECTING -> "Status: Connecting..."
        CarConnectionState.DISCONNECTED -> "Status: Disconnected"
    }
    Text(text = statusText, style = MaterialTheme.typography.titleLarge)
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