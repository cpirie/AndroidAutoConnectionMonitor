package com.pirie.androidautoconnectionmonitor.managers

import android.content.Context
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.pirie.androidautoconnectionmonitor.viewmodel.CarConnectionState
import com.pirie.androidautoconnectionmonitor.viewmodel.MainViewModel

/**
 * CarConnectionManager is responsible for initializing the CarConnection API
 * and listening for connection/disconnection events.
 */
class CarConnectionManager(
    private val context: Context,
    private val viewModel: MainViewModel
) : DefaultLifecycleObserver {

    private var carConnection: CarConnection? = null
    private var lifecycleOwner: LifecycleOwner? = null
    
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        lifecycleOwner = owner
        initialize()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        // No need to remove listeners since we're using LiveData
        carConnection = null
        lifecycleOwner = null
    }

    private fun initialize() {
        lifecycleOwner?.let { owner ->
            // Create CarConnection instance directly using the constructor
            carConnection = CarConnection(context).apply {
                // Observe the LiveData for connection type changes
                getType().observe(owner) { connectionType ->
                    when (connectionType) {
                        CarConnection.CONNECTION_TYPE_PROJECTION -> {
                            viewModel.updateCarConnectionState(CarConnectionState.CONNECTED)
                        }
                        else -> {
                            viewModel.updateCarConnectionState(CarConnectionState.DISCONNECTED)
                        }
                    }
                }
            }
        }
    }
}
