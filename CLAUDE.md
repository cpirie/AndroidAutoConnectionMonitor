# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**AA Connect-Monitor** is an Android diagnostic app for troubleshooting wireless Android Auto connection issues. The app monitors Wi-Fi health, Bluetooth connections, and system logs to help identify root causes of connection drops between a OnePlus 13 and RAM 1500 with Uconnect 5.

## Build Commands

```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run unit tests
./gradlew test

# Run instrumented tests (requires Android device/emulator)
./gradlew connectedAndroidTest
```

## Architecture

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Key Dependencies**: 
  - androidx.car.app library for Android Auto integration
  - Kotlin Coroutines for asynchronous operations
  - StateFlow for reactive UI updates

### Core Components

- `MainActivity.kt` - Main entry point with permission handling
- `MainViewModel.kt` - Central state holder for all UI data using StateFlow
- `CarConnectionManager.kt` - Manages Android Auto connection state via CarConnection API
- Planned managers: `WifiMonitor.kt`, `LogcatReader.kt`, `SessionManager.kt`

### Data Flow

The MainViewModel serves as the single source of truth, holding StateFlow objects for:
- Car connection state (CONNECTED/CONNECTING/DISCONNECTED)
- Wi-Fi health metrics (RSSI, channel, congestion)
- Bluetooth device list
- Filtered logcat stream
- Recording session state

## Key Requirements

### Permissions Required
- `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE`
- `ACCESS_FINE_LOCATION` (for Wi-Fi scanning)
- `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN`
- `READ_LOGS` (requires ADB grant: `adb shell pm grant com.pirie.androidautoconnectionmonitor android.permission.READ_LOGS`)

### Target SDK
- Minimum SDK: 35 (Android 15+)
- Target SDK: 35
- Java Version: 21

## Development Notes

- The app displays a permission dialog for READ_LOGS with ADB command instructions
- Uses CarConnection API for accurate Android Auto state detection
- Implements real-time data monitoring through coroutines
- Session recording captures all diagnostic data to local files
- Smart analysis correlates events (like RSSI drops) with disconnects
- Export functionality generates support bundles via Android Share Sheet

## Architecture Phases

The project follows a 5-milestone development plan:
1. Core setup + Android Auto connection status
2. Live Bluetooth & Wi-Fi metrics
3. Real-time filtered logcat viewer
4. Session recording functionality
5. Smart analysis & export features

## Testing

- Unit tests: `app/src/test/` (JUnit)
- Instrumented tests: `app/src/androidTest/` (Android Test Framework)
- Car app testing utilities available via `car-app-testing` dependency