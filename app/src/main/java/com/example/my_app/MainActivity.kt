package com.example.my_app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.UUID

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 100
        private const val REQUEST_ENABLE_BLUETOOTH = 101

        private const val APP_NAME = "BluetoothMessenger"

        private val APP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var listDevices: ListView
    private lateinit var tvStatus: TextView
    private lateinit var tvMessages: TextView
    private lateinit var etMessage: EditText
    private lateinit var chatScroll: ScrollView

    private lateinit var deviceAdapter: ArrayAdapter<String>

    private val deviceList = ArrayList<BluetoothDevice>()
    private val deviceNames = ArrayList<String>()

    private var selectedDevice: BluetoothDevice? = null

    private var acceptThread: AcceptThread? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null

    private var receiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initializeViews()
        initializeBluetooth()
        setupButtons()
        setupDeviceList()
        registerBluetoothReceiver()

        requestBluetoothPermissions()
    }

    private fun initializeViews() {

        listDevices = findViewById(R.id.listDevices)
        tvStatus = findViewById(R.id.tvStatus)
        tvMessages = findViewById(R.id.tvMessages)
        etMessage = findViewById(R.id.etMessage)
        chatScroll = findViewById(R.id.chatScroll)

        deviceAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            deviceNames
        )

        listDevices.adapter = deviceAdapter
    }

    private fun initializeBluetooth() {

        val bluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        bluetoothAdapter = bluetoothManager.adapter

        if (!::bluetoothAdapter.isInitialized) {
            showStatus("Bluetooth is not available")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            showStatus("Bluetooth is turned off")
        } else {
            showStatus("Bluetooth is ready")
        }
    }

    private fun setupButtons() {

        findViewById<Button>(R.id.btnStartServer).setOnClickListener {

            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions()
                return@setOnClickListener
            }

            if (!bluetoothAdapter.isEnabled) {
                enableBluetooth()
                return@setOnClickListener
            }

            startServer()
        }

        findViewById<Button>(R.id.btnScan).setOnClickListener {

            if (!hasBluetoothPermissions()) {
                requestBluetoothPermissions()
                return@setOnClickListener
            }

            if (!bluetoothAdapter.isEnabled) {
                enableBluetooth()
                return@setOnClickListener
            }

            startDiscovery()
        }

        findViewById<Button>(R.id.btnSend).setOnClickListener {

            sendMessage()
        }
    }

    private fun setupDeviceList() {

        listDevices.setOnItemClickListener { _, _, position, _ ->

            if (position >= deviceList.size) {
                return@setOnItemClickListener
            }

            selectedDevice = deviceList[position]

            val device = selectedDevice ?: return@setOnItemClickListener

            connectToDevice(device)
        }
    }

    // ---------------------------------------------------------
    // PERMISSIONS
    // ---------------------------------------------------------

    private fun requestBluetoothPermissions() {

        val permissions = ArrayList<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)

        } else {

            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val permissionsToRequest = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {

            requestPermissions(
                permissionsToRequest.toTypedArray(),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        }
    }

    private fun hasBluetoothPermissions(): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            checkSelfPermission(
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&

                    checkSelfPermission(
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED

        } else {

            checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {

            if (hasBluetoothPermissions()) {

                showStatus("Bluetooth permissions granted")

            } else {

                showStatus("Bluetooth permissions are required")

                Toast.makeText(
                    this,
                    "Please allow Bluetooth permissions",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ---------------------------------------------------------
    // BLUETOOTH ENABLE
    // ---------------------------------------------------------

    private fun enableBluetooth() {

        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        try {

            val intent = Intent(
                BluetoothAdapter.ACTION_REQUEST_ENABLE
            )

            startActivityForResult(
                intent,
                REQUEST_ENABLE_BLUETOOTH
            )

        } catch (e: SecurityException) {

            showStatus("Bluetooth permission denied")
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {

            if (bluetoothAdapter.isEnabled) {

                showStatus("Bluetooth enabled")

            } else {

                showStatus("Bluetooth is still disabled")
            }
        }
    }

    // ---------------------------------------------------------
    // DEVICE DISCOVERY
    // ---------------------------------------------------------

    private fun startDiscovery() {

        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        try {

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            deviceList.clear()
            deviceNames.clear()
            deviceAdapter.notifyDataSetChanged()

            // Add already paired devices first
            val pairedDevices = bluetoothAdapter.bondedDevices

            for (device in pairedDevices) {

                addDevice(device)
            }

            showStatus("Scanning for nearby devices...")

            bluetoothAdapter.startDiscovery()

        } catch (e: SecurityException) {

            showStatus("Bluetooth permission denied")

        } catch (e: Exception) {

            showStatus("Unable to start discovery")
        }
    }

    private fun addDevice(device: BluetoothDevice) {

        if (deviceList.contains(device)) {
            return
        }

        deviceList.add(device)

        val name = device.name ?: "Unknown Device"

        val displayName =
            "$name\n${device.address}"

        deviceNames.add(displayName)

        runOnUiThread {
            deviceAdapter.notifyDataSetChanged()
        }
    }

    // ---------------------------------------------------------
    // BROADCAST RECEIVER
    // ---------------------------------------------------------

    private val bluetoothReceiver = object : BroadcastReceiver() {

        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {

            when (intent?.action) {

                BluetoothDevice.ACTION_FOUND -> {

                    if (!hasBluetoothPermissions()) {
                        return
                    }

                    val device: BluetoothDevice? =

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )

                        } else {

                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE
                            )
                        }

                    device?.let {

                        addDevice(it)
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {

                    runOnUiThread {

                        showStatus(
                            "Discovery finished. Select a device to connect."
                        )
                    }
                }
            }
        }
    }

    private fun registerBluetoothReceiver() {

        if (receiverRegistered) {
            return
        }

        val filter = IntentFilter()

        filter.addAction(
            BluetoothDevice.ACTION_FOUND
        )

        filter.addAction(
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            registerReceiver(
                bluetoothReceiver,
                filter,
                Context.RECEIVER_EXPORTED
            )

        } else {

            @Suppress("DEPRECATION")
            registerReceiver(
                bluetoothReceiver,
                filter
            )
        }

        receiverRegistered = true
    }

    // ---------------------------------------------------------
    // SERVER
    // ---------------------------------------------------------

    private fun startServer() {

        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        acceptThread?.cancel()

        acceptThread = AcceptThread()

        acceptThread?.start()

        showStatus(
            "Server started. Waiting for another phone..."
        )
    }

    private inner class AcceptThread : Thread() {

        private var serverSocket: BluetoothServerSocket? = null

        override fun run() {

            try {

                if (!hasBluetoothPermissions()) {
                    return
                }

                serverSocket =
                    bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                        APP_NAME,
                        APP_UUID
                    )

                val socket = serverSocket?.accept()

                if (socket != null) {

                    runOnUiThread {

                        showStatus(
                            "Phone connected"
                        )
                    }

                    manageConnectedSocket(socket)
                }

            } catch (e: SecurityException) {

                runOnUiThread {

                    showStatus(
                        "Bluetooth permission denied"
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {

                    showStatus(
                        "Server stopped"
                    )
                }

            } finally {

                try {
                    serverSocket?.close()
                } catch (_: Exception) {
                }
            }
        }

        fun cancel() {

            try {
                serverSocket?.close()
            } catch (_: Exception) {
            }
        }
    }

    // ---------------------------------------------------------
    // CLIENT
    // ---------------------------------------------------------

    private fun connectToDevice(
        device: BluetoothDevice
    ) {

        if (!hasBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        try {

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

        } catch (_: Exception) {
        }

        connectThread?.cancel()

        connectThread = ConnectThread(device)

        connectThread?.start()

        showStatus(
            "Connecting to ${device.name ?: "device"}..."
        )
    }

    private inner class ConnectThread(
        private val device: BluetoothDevice
    ) : Thread() {

        private var socket: BluetoothSocket? = null

        override fun run() {

            try {

                if (!hasBluetoothPermissions()) {
                    return
                }

                socket =
                    device.createRfcommSocketToServiceRecord(
                        APP_UUID
                    )

                socket?.connect()

                val connectedSocket = socket

                if (connectedSocket != null) {

                    runOnUiThread {

                        showStatus(
                            "Connected to ${device.name ?: "device"}"
                        )
                    }

                    manageConnectedSocket(
                        connectedSocket
                    )
                }

            } catch (e: Exception) {

                runOnUiThread {

                    showStatus(
                        "Connection failed"
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Could not connect to device",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                try {

                    socket?.close()

                } catch (_: Exception) {
                }
            }
        }

        fun cancel() {

            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }

    // ---------------------------------------------------------
    // CONNECTED SOCKET
    // ---------------------------------------------------------

    private fun manageConnectedSocket(
        socket: BluetoothSocket
    ) {

        connectedThread?.cancel()

        connectedThread =
            ConnectedThread(socket)

        connectedThread?.start()
    }

    private inner class ConnectedThread(
        private val socket: BluetoothSocket
    ) : Thread() {

        private var reader: BufferedReader? = null
        private var writer: PrintWriter? = null

        override fun run() {

            try {

                reader = BufferedReader(
                    InputStreamReader(
                        socket.inputStream,
                        Charsets.UTF_8
                    )
                )

                writer = PrintWriter(
                    OutputStreamWriter(
                        socket.outputStream,
                        Charsets.UTF_8
                    ),
                    true
                )

                while (!isInterrupted) {

                    val message = reader?.readLine()
                        ?: break

                    runOnUiThread {

                        appendMessage(
                            "Other: $message"
                        )
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    showStatus(
                        "Connection closed"
                    )
                }

            } finally {

                try {
                    socket.close()
                } catch (_: Exception) {
                }
            }
        }

        fun write(message: String) {

            Thread {

                try {

                    writer?.println(message)

                    runOnUiThread {

                        appendMessage(
                            "You: $message"
                        )
                    }

                } catch (e: Exception) {

                    runOnUiThread {

                        showStatus(
                            "Failed to send message"
                        )
                    }
                }

            }.start()
        }

        fun cancel() {

            interrupt()

            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }

    // ---------------------------------------------------------
    // SEND MESSAGE
    // ---------------------------------------------------------

    private fun sendMessage() {

        val message =
            etMessage.text.toString().trim()

        if (message.isEmpty()) {
            return
        }

        val connection = connectedThread

        if (connection == null) {

            Toast.makeText(
                this,
                "Connect to another phone first",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        connection.write(message)

        etMessage.text.clear()
    }

    // ---------------------------------------------------------
    // UI
    // ---------------------------------------------------------

    private fun showStatus(
        message: String
    ) {

        runOnUiThread {

            tvStatus.text =
                "Status: $message"
        }
    }

    private fun appendMessage(
        message: String
    ) {

        tvMessages.append(
            "\n$message"
        )

        chatScroll.post {

            chatScroll.fullScroll(
                ScrollView.FOCUS_DOWN
            )
        }
    }

    // ---------------------------------------------------------
    // CLEANUP
    // ---------------------------------------------------------

    override fun onDestroy() {

        try {

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

        } catch (_: Exception) {
        }

        acceptThread?.cancel()
        connectThread?.cancel()
        connectedThread?.cancel()

        if (receiverRegistered) {

            try {
                unregisterReceiver(
                    bluetoothReceiver
                )
            } catch (_: Exception) {
            }

            receiverRegistered = false
        }

        super.onDestroy()
    }
}