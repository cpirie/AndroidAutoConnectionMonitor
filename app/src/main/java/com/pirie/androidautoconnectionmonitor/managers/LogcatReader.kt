package com.pirie.androidautoconnectionmonitor.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.pirie.androidautoconnectionmonitor.viewmodel.LogEntry
import com.pirie.androidautoconnectionmonitor.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * LogcatReader manages reading and filtering system logs in real-time.
 * It filters logs to show only Android Auto, Wi-Fi, and Bluetooth related events.
 */
class LogcatReader(
    private val context: Context,
    private val viewModel: MainViewModel
) : DefaultLifecycleObserver {

    private var logcatJob: Job? = null
    private var logcatProcess: Process? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // High-priority tags for critical events only
    private val criticalTags = setOf(
        "CarProjectionService", "AndroidAuto", "AASession", "car.projection",
        "CarConnection", "ProjectionService"
    )
    
    // Medium-priority tags for connection events
    private val connectionTags = setOf(
        "BluetoothA2dp", "BluetoothHeadset", "WifiManager"
    )

    // Critical keywords that indicate important events
    private val criticalKeywords = setOf(
        "connected", "disconnected", "connection", "projection", "android auto",
        "head unit", "uconnect", "handshake", "session"
    )
    
    // Error keywords that always get logged
    private val errorKeywords = setOf(
        "error", "failed", "exception", "crash", "disconnect", "timeout", "abort",
        "denied", "refused", "unavailable"
    )
    
    // Warning keywords
    private val warningKeywords = setOf(
        "warning", "warn", "retry", "reconnect", "unstable", "weak", "slow"
    )

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        if (hasReadLogsPermission()) {
            startLogcatReading()
        } else {
            Log.i("LogcatReader", "READ_LOGS permission not granted, logcat reader disabled")
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        stopLogcatReading()
    }

    private fun startLogcatReading() {
        logcatJob = coroutineScope.launch {
            try {
                startLogcatProcess()
            } catch (e: Exception) {
                Log.e("LogcatReader", "Failed to start logcat reading", e)
                withContext(Dispatchers.Main) {
                    viewModel.addLogEntry(LogEntry(
                        tag = "LogcatReader",
                        message = "Failed to start log monitoring: ${e.message}",
                        isError = true
                    ))
                }
            }
        }
    }

    private fun stopLogcatReading() {
        logcatJob?.cancel()
        logcatProcess?.destroy()
        logcatProcess = null
    }

    private suspend fun startLogcatProcess() {
        try {
            // Clear existing logs and start fresh
            val clearCommand = arrayOf("logcat", "-c")
            Runtime.getRuntime().exec(clearCommand).waitFor()

            // Start logcat with time stamps and specific format
            val logcatCommand = arrayOf(
                "logcat",
                "-v", "time",          // Include timestamps
                "-s",                  // Silent mode (only show specified tags)
                getFilterString()      // Our filtered tags
            )

            logcatProcess = Runtime.getRuntime().exec(logcatCommand)
            
            logcatProcess?.let { process ->
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                
                // Add initial log entry
                withContext(Dispatchers.Main) {
                    viewModel.addLogEntry(LogEntry(
                        tag = "LogcatReader",
                        message = "Log monitoring started",
                        isError = false
                    ))
                }

                try {
                    var line: String? = null
                    while (process.isAlive && reader.readLine().also { line = it } != null) {
                        line?.let { logLine ->
                            processLogLine(logLine)
                        }
                    }
                } catch (e: IOException) {
                    Log.w("LogcatReader", "Error reading logcat", e)
                } finally {
                    reader.close()
                }
            }
        } catch (e: Exception) {
            Log.e("LogcatReader", "Error in logcat process", e)
            throw e
        }
    }

    private fun getFilterString(): String {
        // Create filter string for logcat -s option focusing on high-value tags
        // Format: "Tag:Level Tag:Level ..."
        val allRelevantTags = criticalTags + connectionTags
        return allRelevantTags.joinToString(" ") { "$it:I" } // I = Info level and above (reduces noise)
    }

    private suspend fun processLogLine(logLine: String) {
        try {
            val logEntry = parseLogLine(logLine)
            logEntry?.let { entry ->
                withContext(Dispatchers.Main) {
                    viewModel.addLogEntry(entry)
                }
            }
        } catch (e: Exception) {
            Log.w("LogcatReader", "Error processing log line: $logLine", e)
        }
    }

    private fun parseLogLine(logLine: String): LogEntry? {
        if (logLine.isBlank()) return null

        try {
            // Android logcat format: "MM-DD HH:MM:SS.mmm  PID  TID LEVEL TAG     : MESSAGE"
            // We'll use a simpler parsing approach
            
            val parts = logLine.split(":", limit = 2)
            if (parts.size < 2) return null

            val headerPart = parts[0].trim()
            val message = parts[1].trim()

            // Extract tag (usually the last word before the colon)
            val headerWords = headerPart.split(Regex("\\s+"))
            val tag = if (headerWords.size >= 2) {
                headerWords[headerWords.size - 1]
            } else {
                "System"
            }

            val messageLC = message.lowercase()
            val tagLC = tag.lowercase()
            
            // Check if this is critical Android Auto content
            val isCriticalTag = criticalTags.any { criticalTag: String ->
                tagLC.contains(criticalTag.lowercase()) || messageLC.contains(criticalTag.lowercase())
            }
            
            // Check if this is a connection-related event
            val isConnectionTag = connectionTags.any { connectionTag: String ->
                tagLC.contains(connectionTag.lowercase())
            }
            
            // Check for critical keywords that always matter
            val hasCriticalKeyword = criticalKeywords.any { keyword: String ->
                messageLC.contains(keyword)
            }
            
            // Check for errors (always include)
            val isError = errorKeywords.any { keyword: String -> messageLC.contains(keyword) }
            
            // Check for warnings
            val isWarning = !isError && warningKeywords.any { keyword: String -> messageLC.contains(keyword) }
            
            // Only include logs that meet our criteria:
            // 1. Critical Android Auto tags
            // 2. Connection events with critical keywords
            // 3. Any error messages
            // 4. Warnings from connection tags
            val isRelevant = isCriticalTag || 
                           (isConnectionTag && hasCriticalKeyword) ||
                           isError ||
                           (isConnectionTag && isWarning)

            if (!isRelevant) return null

            return LogEntry(
                tag = tag,
                message = message,
                isError = isError,
                isWarning = isWarning
            )
        } catch (e: Exception) {
            Log.w("LogcatReader", "Failed to parse log line: $logLine", e)
            return null
        }
    }

    private fun hasReadLogsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_LOGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Manually trigger a restart of logcat reading (useful if permission is granted after startup)
     */
    fun restartLogcatReading() {
        if (hasReadLogsPermission()) {
            stopLogcatReading()
            startLogcatReading()
        }
    }
}