# **Technical Specification: AA Connect-Monitor**

Version: 1.0  
Date: June 7, 2025  
Related Document: PRD: AA Connect-Monitor (Version 1.1)

### **1\. Overview**

This document provides the engineering blueprint for the **AA Connect-Monitor** application. It details the chosen technology stack, software architecture, and a phased development plan with measurable milestones to guide the implementation. The goal is to build a robust and maintainable diagnostic tool based on the requirements outlined in the PRD.

### **2\. Development Platform & Technology Stack**

The application will be developed as a native Android app using modern, industry-standard tools.

* **IDE:** Android Studio (Latest Stable Version)  
* **Language:** **Kotlin**. Its conciseness, null-safety, and first-class support for coroutines make it the ideal choice for handling the asynchronous data streams required for this app.  
* **UI Toolkit:** **Jetpack Compose**. A modern, declarative UI framework that simplifies building the dynamic, real-time dashboard. It allows for a more flexible and less error-prone UI development process compared to the traditional XML layout system.  
* **Architecture:** **MVVM (Model-View-ViewModel)**. This pattern will be used to separate the UI (View) from the business logic and state (ViewModel), leading to a more testable and organized codebase.  
* **Key Libraries & APIs:**  
  * **androidx.car.app:car-app-library**: The official library for interacting with Android Auto. This is essential for accurately detecting the car connection state.  
  * **kotlinx.coroutines**: For managing all asynchronous operations, including network scanning, log reading, and file I/O, without blocking the main thread.  
  * **androidx.lifecycle:lifecycle-viewmodel-compose**: To create and manage the ViewModel within the Jetpack Compose environment.  
  * **Android WifiManager API**: To scan for Wi-Fi networks, get signal strength (RSSI), and identify channel information.  
  * **Android BluetoothManager API**: To query the state of Bluetooth connections and identify connected devices.

### **3\. Software Architecture**

The application will be structured around a central ViewModel that serves as the single source of truth for the UI (the Composable screen). Various manager classes will be responsible for fetching data and updating the ViewModel.

* **MainViewModel.kt**: The core ViewModel. It will hold the entire state of the UI as StateFlow objects (e.g., carConnectionState, wifiHealth, bluetoothDevices, logcatStream). The UI will observe these flows and recompose automatically when data changes.  
* **CarConnectionManager.kt**: A class responsible for initializing the CarConnection API and listening for connection/disconnection events. It will update the MainViewModel accordingly.  
* **WifiMonitor.kt**: This class will encapsulate the WifiManager logic. It will run in a coroutine scope to periodically scan for networks, identify the RAM 1500's SSID, calculate congestion, and push updates to the MainViewModel.  
* **LogcatReader.kt**: A crucial component that will run the logcat command-line process in a background coroutine. It will read the process's input stream line-by-line, apply the required filters (WifiManager, AASession, etc.), and feed the formatted string into the MainViewModel.  
* **SessionManager.kt**: This class will manage the "Start/Stop Recording" functionality. When active, it will observe all state changes in the MainViewModel and write them, along with the logcat stream, to a timestamped session file in the app's internal storage.  
* **ui/MainScreen.kt**: A Jetpack Compose file containing all the @Composable functions to build the user interface as defined in the PRD. It will read its state directly from the MainViewModel.

### **4\. Implementation Plan & Milestones**

This project will be developed in five distinct, sequential milestones.

#### **Milestone 1: Project Setup & Core Functionality**

* **Goal:** Establish the project foundation and get the core Android Auto connection status working.  
* **Tasks:**  
  1. Create a new Android Studio project configured with Kotlin and Jetpack Compose.  
  2. Add all necessary permissions (ACCESS\_WIFI\_STATE, BLUETOOTH\_SCAN, etc.) to AndroidManifest.xml.  
  3. Add the androidx.car.app library dependency.  
  4. Implement the CarConnectionManager and integrate it into the MainViewModel.  
  5. Create a basic Jetpack Compose UI to display the connection status: "Connected," "Connecting," or "Disconnected".  
  6. Implement the ADB permission check for READ\_LOGS on startup, with a clear instructional dialog if it's missing.  
* **Measurable Outcome:** The app can be launched. It correctly displays the real-time connection status to the RAM 1500\. The permission check works as expected.

#### **Milestone 2: Live Data \- Bluetooth & Wi-Fi**

* **Goal:** Populate the dashboard with live Bluetooth and Wi-Fi health metrics.  
* **Tasks:**  
  1. Implement the BluetoothManager logic to get a list of concurrently connected devices and display them in the UI.  
  2. Implement the WifiMonitor to perform scans.  
  3. Display the RSSI of the head unit's Wi-Fi network.  
  4. Implement the logic to count competing networks and display the count.  
* **Measurable Outcome:** The UI correctly lists the "OnePlus Watch 3," shows a real-time dBm value for the Uconnect Wi-Fi, and displays a number for channel congestion.

#### **Milestone 3: Real-Time Logcat Viewer**

* **Goal:** Get the filtered logcat stream running and displayed in the app.  
* **Tasks:**  
  1. Implement the LogcatReader class.  
  2. Create a coroutine that safely starts and manages the logcat process.  
  3. Implement the string filtering logic to match the required tags from the PRD.  
  4. Create a scrollable LazyColumn in the UI to display the live log stream.  
  5. Implement basic highlighting for lines containing "error" or "warning".  
* **Measurable Outcome:** When the app is running, the log viewer on the main screen actively streams new, filtered log entries.

#### **Milestone 4: Session Recording**

* **Goal:** Implement the ability to record all diagnostic data to a file.  
* **Tasks:**  
  1. Implement the SessionManager class.  
  2. Add the "Start/Stop Recording" button to the UI.  
  3. When "Start" is tapped, the SessionManager begins writing all ViewModel state changes and logcat entries to a file in the app's internal directory.  
  4. When "Stop" is tapped, the file is closed and saved.  
* **Measurable Outcome:** A user can successfully record a session. A log file containing all the dashboard data and logcat output from the session is created and accessible within the app's data folder.

#### **Milestone 5: Smart Analysis & Support Bundle Export**

* **Goal:** Add the final analysis and export features.  
* **Tasks:**  
  1. Implement the parsing logic that reads a completed session file.  
  2. Implement the "Smart Analysis" algorithm to detect disconnects and correlate them with preceding events (e.g., RSSI drops).  
  3. Display the analysis results in a dialog or a new screen.  
  4. Implement the "Export Support Bundle" button, which formats all session data into a single string.  
  5. Use the Android Share Sheet API to allow the user to export this string.  
* **Measurable Outcome:** After stopping a recording where a disconnect occurred, a summary dialog appears. The user can then successfully use the share feature to copy the full support bundle to their clipboard or send it via email.