# Android Auto Connect Monitor

**AA Connect-Monitor** is a specialized Android diagnostic application designed to troubleshoot wireless Android Auto connection issues. Built specifically for technical users who need real-time visibility into connection health, signal strength, and system events.

## 🚗 Overview

Wireless Android Auto provides convenience but can suffer from frustrating connection drops. Standard Android tools don't offer user-friendly ways to diagnose real-time factors like Wi-Fi signal health, channel congestion, and Bluetooth interference. 

AA Connect-Monitor bridges this gap by providing a consolidated diagnostic dashboard that moves you from *"my connection dropped again"* to *"my connection dropped because of Wi-Fi channel congestion from a nearby hotspot."*

## ✨ Features

### Real-Time Monitoring
- **Android Auto Connection Status** - Live tracking of Connected/Connecting/Disconnected states
- **Wi-Fi Health Metrics** - Signal strength (RSSI), channel information, and network congestion analysis
- **Bluetooth Device Monitoring** - Real-time connection states with head unit detection
- **Filtered System Logs** - Curated logcat viewer showing only Android Auto, Wi-Fi, and Bluetooth events

### Advanced Diagnostics
- **Session Recording** - Capture full diagnostic sessions for later analysis
- **Smart Analysis** - Correlates events (signal drops) with connection failures
- **Export Functionality** - Generate support bundles for sharing diagnostic data
- **Color-Coded Alerts** - Visual indicators for signal quality and connection health

### Technical Capabilities
- **Profile-Specific Monitoring** - Tracks A2DP and Headset Bluetooth profiles separately
- **Multi-Level Filtering** - Reduces log noise while preserving critical diagnostic information
- **Real-Time Updates** - Live dashboard with 2-3 second refresh intervals
- **Permission Handling** - Graceful degradation when advanced permissions unavailable

## 📱 Requirements

- **Android 15+ (API 35)**
- **OnePlus 13** (primary target, may work on other devices)
- **Vehicle**: RAM 1500 with Uconnect 5 (or similar Android Auto head unit)
- **Development Setup**: Android Studio, ADB access for READ_LOGS permission

## 🔧 Installation & Setup

### 1. Build & Install
```bash
./gradlew build
./gradlew installDebug
```

### 2. Grant Permissions
The app will prompt for standard permissions (Location, Bluetooth) through the Android UI.

For advanced log monitoring, grant READ_LOGS permission via ADB:
```bash
adb shell pm grant com.pirie.androidautoconnectionmonitor android.permission.READ_LOGS
```

### 3. Enable Developer Options
On your device:
1. Settings > About Phone > Build Number (tap 7 times)
2. Settings > System > Developer Options > USB Debugging

## 🏗️ Architecture

**Technology Stack:**
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with StateFlow
- **Key Libraries**: androidx.car.app, Kotlin Coroutines

**Core Components:**
- `MainActivity.kt` - Main UI with card-based layout
- `MainViewModel.kt` - Central state management using StateFlow
- `CarConnectionManager.kt` - Android Auto connection monitoring via CarConnection API
- `BluetoothMonitor.kt` - Real-time Bluetooth profile tracking (A2DP/Headset)
- `WifiMonitor.kt` - Wi-Fi scanning, RSSI monitoring, and congestion analysis
- `LogcatReader.kt` - Filtered system log streaming and parsing

## 📊 Dashboard Overview

### Connection Status Card
- Real-time Android Auto state with color-coded indicators
- Visual connection status (🟢 Connected, 🔵 Connecting, 🔴 Disconnected)

### Wi-Fi Health Card
- **Signal Strength**: RSSI with color coding (Green >-67dBm, Orange -67 to -80dBm, Red <-80dBm)
- **Channel Information**: Current 2.4GHz/5GHz/6GHz channel
- **Network Congestion**: Count of competing networks on same/adjacent channels

### Bluetooth Devices Card
- List of connected devices with connection states
- Automatic head unit detection (🚗 icon)
- Real-time connection status indicators

### System Logs Card
- Filtered logcat stream showing only relevant events
- Error/warning highlighting with colored backgrounds
- Auto-scrolling with timestamps
- Monospace font for readability

## 🔍 Troubleshooting

### Common Issues

**"No logs available (READ_LOGS permission required)"**
- Grant permission via ADB: `adb shell pm grant com.pirie.androidautoconnectionmonitor android.permission.READ_LOGS`
- App functions without this permission, but log viewer won't work

**Bluetooth devices not showing connection states**
- Ensure Location and Bluetooth permissions are granted
- Check that device is properly paired and connected

**Wi-Fi metrics showing zeros**
- Grant Location permission (required for Wi-Fi scanning on Android 6+)
- Ensure Wi-Fi is enabled and connected to a network

### Development Issues

**Build failures**
- Ensure Android SDK is properly configured
- Check that all required permissions are declared in AndroidManifest.xml
- Verify Kotlin version compatibility

**Permission errors during development**
- Use `@Suppress("DEPRECATION")` for API compatibility across Android versions
- Wrap Bluetooth device name access in try-catch blocks

## 🛣️ Development Roadmap

The project follows a structured 5-milestone development plan:

- ✅ **Milestone 1**: Core setup + Android Auto connection monitoring
- ✅ **Milestone 2**: Live Wi-Fi and Bluetooth metrics
- ✅ **Milestone 3**: Real-time filtered logcat viewer + enhanced Bluetooth monitoring
- 🚧 **Milestone 4**: Session recording functionality
- 📋 **Milestone 5**: Smart analysis & export features

## 🤝 Contributing

This project was developed specifically for diagnosing Android Auto connection issues on OnePlus 13 + RAM 1500 configurations. While contributions are welcome, the focus remains on wireless Android Auto diagnostics.

### Development Setup
1. Clone the repository
2. Open in Android Studio
3. Install Java 21 for gradle builds
4. Connect OnePlus 13 device for testing

### Code Style
- Follow existing Kotlin conventions
- Use Jetpack Compose for UI components
- Maintain MVVM architecture patterns
- Add appropriate error handling for permission-based operations

## 📄 License

This project is provided as-is for educational and diagnostic purposes. See the technical specifications and PRD in the `docs/` directory for detailed requirements and implementation guidance.

---

**Target User**: Technical professionals comfortable with Android development, ADB, and diagnostic tools.
**Primary Use Case**: Real-world troubleshooting of wireless Android Auto connection issues while driving.