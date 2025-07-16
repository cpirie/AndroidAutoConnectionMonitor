# **Product Requirements Document: AA Connect-Monitor**

Version: 1.1  
Date: June 7, 2025  
Author: Gemini (Android Engineering Specialist)  
Stakeholder: C\#/.NET Developer (End User)

### **1\. Introduction & Problem Statement**

Wireless Android Auto provides a seamless and convenient in-car experience, but it is susceptible to intermittent and frustrating connection drops. Standard Android OS tools do not offer a user-friendly way to diagnose the real-time factors influencing the connection, such as Wi-Fi signal health, channel congestion, and Bluetooth interference.

This document outlines the requirements for **AA Connect-Monitor**, a specialized Android application designed for a technically proficient user to troubleshoot and identify the root causes of wireless Android Auto disconnects between their OnePlus 13 and RAM 1500\.

### **2\. Vision & Objectives**

The vision is to create a dedicated diagnostic dashboard that empowers the user to move from "my connection dropped again" to "my connection dropped because of Wi-Fi channel congestion from a nearby hotspot."

* **Objective 1: Provide Real-Time Visibility:** Display all relevant live metrics for the wireless Android Auto connection in a single, consolidated interface.  
* **Objective 2: Enable Session Logging:** Allow the user to record all diagnostic data throughout a drive, ensuring that event data leading up to a disconnect is captured and can be exported.  
* **Objective 3: Facilitate Root Cause Analysis:** Leverage the logged data to provide a "smart" summary that correlates specific events (e.g., signal degradation) with connection failures.  
* **Objective 4: Minimize Noise:** Filter system-wide logs to show only events pertinent to Android Auto, Wi-Fi, and Bluetooth subsystems, making the data actionable.

### **3\. Target User**

* **Description:** A highly technical individual with extensive software development experience (C\#, Java) but new to Android development.  
* **Device Ecosystem:** OnePlus 13 (Phone), OnePlus Watch 3 (Peripheral), RAM 1500 with Uconnect 5 (Head Unit).  
* **Technical Comfort:** 100% comfortable with enabling Developer Options and using ADB (Android Debug Bridge) to grant necessary permissions.

### **4\. Functional Requirements & Features**talveri

The application will be a single-screen dashboard divided into three primary sections: **Live Status**, **Session Control**, and **Filtered Log Viewer**.

#### **Feature 1: Live Status Dashboard**

*As a user, I want to see the live health of my connection so I can understand the current state of my Android Auto session.*

* **UI Components:**  
  * **Connection Status Indicator:** A large, clear icon/text showing "Connected," "Connecting," or "Disconnected" to Android Auto.  
  * **Wi-Fi Health Monitor:**  
    * **Head Unit Signal Strength:** A real-time graph or large number displaying the RSSI (in dBm) from the phone to the RAM 1500's Wi-Fi network. Color-coded for quick assessment (e.g., Green \> \-67dBm, Yellow \-68 to \-80dBm, Red \< \-80dBm).  
    * **Wi-Fi Channel:** Displays the current 5GHz channel being used.  
    * **Network Congestion:** A count of other Wi-Fi networks detected on the *same* and *adjacent* channels.  
  * **Bluetooth Health Monitor:**  
    * **Head Unit Connection:** Indicates if Bluetooth is connected to the RAM 1500\.  
    * **Concurrent Devices:** A list of other connected Bluetooth devices (e.g., "OnePlus Watch 3").

#### **Feature 2: Session Recording, Analysis & Export**

*As a user, I want to record my entire drive so that when a disconnect happens, I can review the events that led up to it and export the results.*

* **UI Components:**  
  * A prominent **"Start Recording" / "Stop Recording"** button.  
* **Functionality:**  
  1. Tapping "Start Recording" begins capturing all data points from the Live Dashboard and the filtered logcat to a local session file.  
  2. The app will automatically log a "DISCONNECT" event marker when the Android Auto connection is lost.  
  3. Tapping "Stop Recording" finalizes the session log.  
  4. **Smart Analysis:** After stopping, a dialog or new screen will present a summary and an option to export.  
     * **Example Summary:**  
       **Session Analysis: Disconnect Detected**  
       * **Disconnect Time:** 8:23:15 PM  
       * **Probable Cause:** Severe Wi-Fi signal degradation.  
       * **Supporting Data:** In the 15 seconds before the disconnect, Wi-Fi signal to 'RAM-Uconnect' dropped from \-65dBm to \-82dBm.  
       * **Other Factors:** 3 other Wi-Fi networks were detected on Channel 149 during this time.

##### **Feature 2.1: Export Support Bundle**

*As a user, after a session is complete, I want to export a "Support Bundle" so I can paste it into an LLM or share it for external analysis.*

* **Trigger:** An "Export Bundle" button will be visible on the "Session Analysis" screen.  
* **Action:** Tapping the button will generate a single, structured .txt file.  
* **Bundle Contents:** The text file will contain:  
  * **Header:** App Version, Phone Model (OnePlus 13), Android OS Version.  
  * **Analysis Summary:** The full text from the "Smart Analysis" result.  
  * **Full Session Log:** The complete, timestamped log of metrics (RSSI, channel, etc.) and the filtered logcat output captured during the recording session.  
* **Export Mechanism:** The app will invoke the standard Android Share Sheet, allowing the user to send the generated text file to any compatible application (e.g., Copy to Clipboard, Save to Drive, Send with Gmail).

#### **Feature 3: Filtered Log Viewer**

*As a user, I want to see the raw logcat output, but only for events relevant to my connection, so I can perform my own deep-dive analysis.*

* **UI Component:** A scrollable text view within the main dashboard.  
* **Functionality:**  
  * This view will stream the output of logcat in real-time.  
  * **Crucially, it will be pre-filtered** to only show log tags related to:  
    * WifiManager (Wi-Fi state changes, signal strength)  
    * BluetoothManager (Connections, disconnections)  
    * AASession (Android Auto session lifecycle)  
    * car.projection (The core Android Auto projection service)  
  * The log will automatically highlight critical lines like "error," "warning," or specific disconnect messages.

### **5\. Technical Requirements & Considerations**

* **Permissions:** The app AndroidManifest.xml must declare the following:  
  * android.permission.ACCESS\_WIFI\_STATE  
  * android.permission.CHANGE\_WIFI\_STATE  
  * android.permission.ACCESS\_FINE\_LOCATION (Required for Wi-Fi scanning on modern Android versions).  
  * android.permission.BLUETOOTH\_CONNECT  
  * android.permission.BLUETOOTH\_SCAN  
  * android.permission.READ\_LOGS (Requires a one-time ADB command by the user).  
* **Developer Mode Prerequisite:** The app will need to detect if it has the READ\_LOGS permission. If not, it must display a clear, one-time message instructing the user on how to grant it via their computer:"To enable log monitoring, please connect your phone to a computer with ADB and run the following command:  
  adb shell pm grant your.package.name.here android.permission.READ\_LOGS"  
* **Android Auto Connection State:** The app must use the CarConnection API from the Android for Cars App Library to reliably detect the connection state of Android Auto. This is the official and most accurate method.

### **6\. Success Metrics**

The application will be considered successful if it allows the user to:

1. Consistently capture a session log that includes at least one random disconnect event.  
2. Use the "Smart Analysis" or the raw logs to form a strong, data-backed hypothesis for the cause of a disconnect.  
3. Successfully export a Support Bundle and use its contents to identify a recurring pattern.