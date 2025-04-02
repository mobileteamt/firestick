package com.example.firestick

import android.Manifest
import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class AddDeviceActivity : AppCompatActivity() {

    private val TAG = "NearbyConnections"
    private val connectionsClient: ConnectionsClient by lazy {
        Nearby.getConnectionsClient(this)
    }

    // The device's advertising name and the service ID.
    private val deviceName = "Android TV"
    private val serviceId = "com.example.nearby"

    // The strategy for the connection
    private val strategy = Strategy.P2P_CLUSTER

    // Endpoint ID for the connected device
    private var endpointId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        // Start advertising and discovering devices
        startAdvertising()
        startDiscovery()

        // Request runtime permissions if necessary
        requestPermissions()
    }

    private fun requestPermissions() {
        val permissionsRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                Log.d(TAG, "Permission granted.")
            } else {
                Log.e(TAG, "Permission denied.")
            }
        }

        permissionsRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        )
    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(
            deviceName, // The name of your device
            serviceId, // Unique service ID for discovery
            object : ConnectionLifecycleCallback() {
                override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                    Log.d(TAG, "Connection initiated with endpoint: $endpointId")
                    // Accept the connection to establish it
                    connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                        override fun onPayloadReceived(endpointId: String, payload: Payload) {
                            // Handle the payload
                            Log.d(TAG, "Payload received from: $endpointId")
                        }

                        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                            Log.d(TAG, "Payload transfer update from: $endpointId")
                        }
                    })
                    this@AddDeviceActivity.endpointId = endpointId
                }

                override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                    if (result.status.isSuccess) {
                        Log.d(TAG, "Connected to $endpointId")
                    } else {
                        Log.d(TAG, "Connection failed with $endpointId")
                    }
                }

                override fun onDisconnected(endpointId: String) {
                    Log.d(TAG, "Disconnected from $endpointId")
                }
            },
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Advertising failed: ", exception)
        }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(
            serviceId, // The same service ID used during advertising
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    Log.d(TAG, "Found endpoint: ${info.endpointName}")
                    // Connect to the found device
                    connectionsClient.requestConnection(deviceName, endpointId, object : ConnectionLifecycleCallback() {
                        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
                            connectionsClient.acceptConnection(endpointId, object : PayloadCallback() {
                                override fun onPayloadReceived(endpointId: String, payload: Payload) {
                                    Log.d(TAG, "Payload received from: $endpointId")
                                }

                                override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                                    Log.d(TAG, "Payload transfer update from: $endpointId")
                                }
                            })
                        }

                        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                            if (result.status.isSuccess) {
                                Log.d(TAG, "Connected to device: $endpointId")
                            }
                        }

                        override fun onDisconnected(endpointId: String) {
                            Log.d(TAG, "Disconnected from device: $endpointId")
                        }
                    })
                }

                override fun onEndpointLost(endpointId: String) {
                    Log.d(TAG, "Endpoint lost: $endpointId")
                }
            },
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started successfully")
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Discovery failed: ", exception)
        }
    }

    // Retrieve the device's local IP address using Wi-Fi
    private fun getLocalIpAddress(): String {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo: WifiInfo = wifiManager.connectionInfo
        return android.text.format.Formatter.formatIpAddress(connectionInfo.ipAddress)
    }

    override fun onStop() {
        super.onStop()
        // Stop advertising and discovery when the activity stops
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
    }
}
