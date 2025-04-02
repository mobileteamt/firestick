package com.example.firestick

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.indices
import androidx.lifecycle.lifecycleScope
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.device.ConnectableDeviceListener
import com.connectsdk.device.DevicePicker
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.DiscoveryManagerListener
import com.connectsdk.discovery.provider.CastDiscoveryProvider
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider
import com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider
import com.connectsdk.service.AirPlayService
import com.connectsdk.service.CastService
import com.connectsdk.service.DLNAService
import com.connectsdk.service.DeviceService
import com.connectsdk.service.DeviceService.PairingType
import com.connectsdk.service.RokuService
import com.connectsdk.service.WebOSTVService
import com.connectsdk.service.command.ServiceCommandError
import com.example.firestick.connectionandcommand.DataStoreManager
import com.example.firestick.databinding.ActivitySettingBinding
import com.example.firestick.viewmodel.DeviceInfomation
import com.example.firestick.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import tej.wifitoolslib.DevicesFinder
import tej.wifitoolslib.interfaces.OnDeviceFindListener
import tej.wifitoolslib.models.DeviceItem

internal class SettingActivity : AppCompatActivity() {
    private var binding: ActivitySettingBinding? = null
    //private val mainViewModel: MainViewModel by viewModels()
    private var mDiscoveryManager: DiscoveryManager? = null

    private var fireStick: ConnectableDevice? = null

    //private var dp: DevicePicker? = null
    //private var dialog: AlertDialog? = null
    private lateinit var dataStoreManager: DataStoreManager
    private var deviceName: String = ""
    private var deviceIP: String = ""

    val deviceList = mutableListOf<DeviceInfomation>()


    private val deviceListener: ConnectableDeviceListener = object : ConnectableDeviceListener {
        override fun onPairingRequired(
            device: ConnectableDevice,
            service: DeviceService,
            pairingType: PairingType
        ) {

            when (pairingType) {
                PairingType.FIRST_SCREEN -> Log.d("2ndScreenAPP", "First Screen")
                PairingType.PIN_CODE, PairingType.MIXED -> Log.d("2ndScreenAPP", "Pin Code")
                PairingType.NONE -> {}
                else -> {}
            }
        }

        override fun onConnectionFailed(device: ConnectableDevice, error: ServiceCommandError) {
            Log.d("2ndScreenAPP", "onConnectFailed")
            Log.d("2ndScreenAPP", "Failed to connect to " + device.ipAddress)

            if (fireStick != null) {
                fireStick!!.removeListener(this)
                fireStick!!.disconnect()
                fireStick = null
            }
        }

        override fun onDeviceReady(device: ConnectableDevice) {
            Log.d("2ndScreenAPP", "onPairingSuccess" + device.ipAddress)

        }

        override fun onDeviceDisconnected(device: ConnectableDevice) {
            Log.d("2ndScreenAPP", "Device Disconnected")
            if (fireStick != null && !fireStick!!.isConnected) {
                fireStick!!.removeListener(this)
                fireStick = null
            }
        }

        override fun onCapabilityUpdated(
            device: ConnectableDevice,
            added: List<String>,
            removed: List<String>
        ) {
            // Do something with capabilities updates if needed
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding!!.root)


        dataStoreManager = DataStoreManager.getInstance(applicationContext)

        DiscoveryManager.init(applicationContext)

        mDiscoveryManager = DiscoveryManager.getInstance()
        registerDeviceServices()
        //mDiscoveryManager?.registerDefaultDeviceTypes()
        mDiscoveryManager?.pairingLevel = DiscoveryManager.PairingLevel.ON

        DiscoveryManager.getInstance().start()

        binding!!.deviceDiscovery.setOnClickListener(View.OnClickListener { v: View? ->
            //setupPicker()
            fetchManualIP()
        })


        lifecycleScope.launch {
            dataStoreManager.deviceName.collect {
                deviceName = it
                binding!!.deviceSwitiching.text =
                    resources.getString(R.string.device_switiching) + deviceName + "\n" + deviceIP

            }
        }

        binding!!.btnBack.setOnClickListener { onBackPressed() }


    }


    private fun fetchManualIP() {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Fetching devices...")
        progressDialog.setCancelable(false)  // Prevent user from dismissing dialog manually
        progressDialog.show()

        deviceList.clear()

        mDiscoveryManager?.allDevices?.values?.forEach { device ->
            if (device is ConnectableDevice) {
                val deviceInfo =
                    DeviceInfomation(device.ipAddress, device.friendlyName, "connect", device)
                deviceList.add(deviceInfo)
            }
        }


        val devicesFinder = DevicesFinder(this, object : OnDeviceFindListener {
            override fun onStart() {
            }

            override fun onDeviceFound(deviceItem: DeviceItem?) {


            }

            override fun onComplete(deviceItemsList: List<DeviceItem?>?) {
                if (deviceItemsList != null) {
                    for (deviceItem in deviceItemsList) {
                        val deviceInfo: DeviceInfomation =
                            if (deviceItem!!.isDeviceNameAndIpAddressSame) {
                                DeviceInfomation(deviceItem.ipAddress, "Unknown", "ip", null)
                            } else {
                                DeviceInfomation(
                                    deviceItem.ipAddress,
                                    deviceItem.deviceName,
                                    "ip",
                                    null
                                )
                            }

                        val contains = deviceList.any {
                            it.ip == deviceItem.ipAddress
                        }
                        if (!contains)
                            deviceList.add(deviceInfo)
                    }
                }
                progressDialog.dismiss()
                showDeviceDialog(deviceList)
            }

            override fun onFailed(errorCode: Int) {
                progressDialog.dismiss()

            }
        })

        devicesFinder.setTimeout(1000).start()

    }


    /* private fun setupPicker() {
         dp = DevicePicker(this)
         dialog = dp!!.getPickerDialog("Device List") { parent, view, position, id ->
             fireStick = parent.getItemAtPosition(position) as ConnectableDevice
             if (fireStick != null) {
                 fireStick!!.addListener(deviceListener)
                 fireStick!!.setPairingType(null)
                 fireStick!!.connect()

                 lifecycleScope.launch {
                     dataStoreManager.saveDeviceIP(fireStick!!.ipAddress)
                     dataStoreManager.saveDeviceName(fireStick!!.friendlyName)
                     binding!!.deviceSwitiching.text =
                         resources.getString(R.string.device_switiching) + deviceName

                 }

                 dp!!.pickDevice(fireStick)
                 if (dialog != null) {
                     dialog!!.dismiss()
                 }
             }

             for (i in parent.indices) {
                 if (position != i) {
                     val tv: ConnectableDevice = parent.getItemAtPosition(i) as ConnectableDevice
                     tv.addListener(deviceListener)
                     tv.setPairingType(null)
                     tv.connect()
                     dp!!.pickDevice(tv)
                     MainViewModel.tv = tv

                 }
             }
         }
         dialog!!.show()
     }*/


    fun showDeviceDialog(deviceList: List<DeviceInfomation>) {
        var dialog: AlertDialog? = null
        val dialogView = layoutInflater.inflate(R.layout.dialog_device_list, null)

        val listView: ListView = dialogView.findViewById(R.id.deviceListView)

        val adapter = DeviceListAdapter(this, deviceList) { selectedDevice, position ->
            lifecycleScope.launch {
                dataStoreManager.saveDeviceIP(selectedDevice.ip)
                dataStoreManager.saveDeviceName(selectedDevice.name)
                dataStoreManager.saveDeviceType(selectedDevice.from)
                binding!!.deviceSwitiching.text =
                    resources.getString(R.string.device_switiching) + deviceName
            }

            if (selectedDevice.from == "connect") {
                fireStick = selectedDevice.connectableDevice as ConnectableDevice
                fireStick!!.addListener(deviceListener)
                fireStick!!.setPairingType(null)
                fireStick!!.connect()
            }


            for (i in deviceList.indices) {
                if (position != i) {
                    if (deviceList[i].from == "connect") {
                        val tv: ConnectableDevice =
                            deviceList[i].connectableDevice as ConnectableDevice
                        tv.addListener(deviceListener)
                        tv.setPairingType(null)
                        tv.connect()
                        MainViewModel.tv = tv
                    }
                }
            }

            if (dialog != null)
                dialog?.dismiss()

        }

        listView.adapter = adapter


        // Create and show the dialog
        dialog = AlertDialog.Builder(this)
            .setTitle("Select Device")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

    }

    private fun registerDeviceServices() {
        // Register Android TV devices with SSDP discovery provider
        /*mDiscoveryManager?.registerDeviceService(
            DeviceService::class.java,
            SSDPDiscoveryProvider::class.java
        )*/

        // Register Cast devices (e.g., Chromecast) with SSDP discovery provider
        mDiscoveryManager?.registerDeviceService(
            CastService::class.java,
            SSDPDiscoveryProvider::class.java
        )

        // Register Roku devices with SSDP discovery provider
        mDiscoveryManager?.registerDeviceService(
            RokuService::class.java,
            SSDPDiscoveryProvider::class.java
        )

        // Register DLNA devices with SSDP discovery provider
        mDiscoveryManager?.registerDeviceService(
            DLNAService::class.java,
            SSDPDiscoveryProvider::class.java
        )

        // Register AirPlay devices with ZeroConf discovery provider
        mDiscoveryManager?.registerDeviceService(
            AirPlayService::class.java,
            ZeroconfDiscoveryProvider::class.java
        )

        // Register WebOS TV devices with SSDP discovery provider
        mDiscoveryManager?.registerDeviceService(
            WebOSTVService::class.java,
            SSDPDiscoveryProvider::class.java
        )

        // Register DIAL devices (e.g., Roku, Smart TVs) with DIAL discovery provider
        /*mDiscoveryManager?.registerDeviceService(
            DIALDevice::class.java,
            DIALDiscoveryProvider::class.java
        )*/

    }

}

