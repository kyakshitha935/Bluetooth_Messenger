\# Bluetooth Messenger



Bluetooth Messenger is an offline Android messaging application that enables two Android devices to communicate and exchange text messages using Bluetooth Classic.



The application does not require an internet connection, mobile data, or Wi-Fi for messaging.



\## Features



\* 📱 Offline peer-to-peer messaging

\* 🔵 Bluetooth Classic communication

\* 🔍 Scan and discover nearby Bluetooth devices

\* 🔗 Connect two paired Android devices

\* 💬 Send and receive text messages in real time

\* 🖥️ Simple and beginner-friendly user interface

\* 🔐 Uses Android Bluetooth permissions

\* 📡 No internet connection required



\## Technologies Used



\* \*\*Language:\*\* Kotlin

\* \*\*UI:\*\* XML

\* \*\*Platform:\*\* Android

\* \*\*IDE:\*\* Android Studio

\* \*\*Communication:\*\* Bluetooth Classic (RFCOMM)

\* \*\*Build System:\*\* Gradle

\* \*\*Version Control:\*\* Git \& GitHub



\## How It Works



The application uses Bluetooth Classic RFCOMM communication.



\### Device 1 — Server



1\. Open the application.

2\. Enable Bluetooth.

3\. Tap \*\*Start Server\*\*.

4\. The application waits for another device to connect.



\### Device 2 — Client



1\. Open the application.

2\. Enable Bluetooth.

3\. Tap \*\*Scan Devices\*\*.

4\. Select the paired device.

5\. The application establishes a Bluetooth connection.



Once connected, both devices can send and receive messages.



\## Communication Flow



```text

Android Phone 1

&#x20;     │

&#x20;     │ Bluetooth RFCOMM

&#x20;     │

&#x20;     ▼

Bluetooth Connection

&#x20;     │

&#x20;     │

&#x20;     ▼

Android Phone 2



Phone 1 ←──── Text Messages ────→ Phone 2

```



\## Project Structure



```text

Bluetooth Messenger

│

├── app/

│   └── src/

│       └── main/

│           ├── java/

│           │   └── com/example/my\_app/

│           │       └── MainActivity.kt

│           │

│           ├── res/

│           │   └── layout/

│           │       └── activity\_main.xml

│           │

│           └── AndroidManifest.xml

│

├── build.gradle.kts

├── settings.gradle.kts

├── gradle.properties

├── gradlew

├── gradlew.bat

└── README.md

```



\## Bluetooth UUID



The application uses the following UUID for Bluetooth RFCOMM communication:



```text

00001101-0000-1000-8000-00805F9B34FB

```



\## Requirements



\* Android smartphone with Bluetooth support

\* Android Studio

\* Android SDK

\* Two Android devices for testing

\* Bluetooth enabled on both devices



\## Running the Project



1\. Clone the repository.

2\. Open the project in Android Studio.

3\. Allow Android Studio to synchronize Gradle.

4\. Connect an Android device or use an emulator with Bluetooth support.

5\. Build and run the application.

6\. Install the application on two Android devices.

7\. Pair the devices through Android Bluetooth settings.

8\. Start the server on one device.

9\. Scan and connect from the second device.

10\. Start exchanging messages.



\## Testing



The application was tested successfully on two Android smartphones using Bluetooth Classic communication.



The devices were able to:



\* Discover each other

\* Establish a Bluetooth connection

\* Send messages

\* Receive messages from the other device



\## Future Improvements



\* 💬 Chat bubbles for sent and received messages

\* 👤 Device/user names

\* 🕒 Message timestamps

\* 📜 Persistent chat history

\* 🔔 Message notifications

\* 🖼️ Image/file sharing

\* 🔄 Automatic reconnection

\* 🔐 End-to-end message encryption



\## Author



\*\*Yakshitha Kancharla\*\*



Computer Science and Engineering Student



GitHub: \[kyakshitha935](https://github.com/kyakshitha935)



