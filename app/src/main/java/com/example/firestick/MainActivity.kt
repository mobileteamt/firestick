package com.example.firestick

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.discovery.DiscoveryManager
import com.connectsdk.discovery.provider.SSDPDiscoveryProvider
import com.connectsdk.discovery.provider.ZeroconfDiscoveryProvider
import com.connectsdk.service.AirPlayService
import com.connectsdk.service.CastService
import com.connectsdk.service.DLNAService
import com.connectsdk.service.RokuService
import com.connectsdk.service.WebOSTVService
import com.example.firestick.connectionandcommand.DataStoreManager
import com.example.firestick.connectionandcommand.KeyEventCodes
import com.example.firestick.databinding.ActivityMainBinding
import com.example.firestick.viewmodel.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import tej.wifitoolslib.DevicesFinder
import tej.wifitoolslib.interfaces.OnDeviceFindListener
import tej.wifitoolslib.models.DeviceItem

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainViewModel: MainViewModel by viewModels()
    private var volume: Float = 0.12f
    private var mute: Boolean = false
    private var mDiscoveryManager: DiscoveryManager? = null
    private var shutDown: Boolean = false

    // Variables for controlling debounce behavior
    private var lastCommandTime: Long = 0
    private val commandDelay: Long = 100 // Minimum time between commands (in milliseconds)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        DiscoveryManager.init(applicationContext)

        mDiscoveryManager = DiscoveryManager.getInstance()
        registerDeviceServices()
        //mDiscoveryManager?.registerDefaultDeviceTypes()
        mDiscoveryManager?.pairingLevel = DiscoveryManager.PairingLevel.ON

        DiscoveryManager.getInstance().start()

        val dataStoreManager = DataStoreManager.getInstance(application)

        lifecycleScope.launch {
            dataStoreManager.deviceType.distinctUntilChanged()  // Only collect if the value changes
                .collect { deviceType ->
                    mainViewModel.deviceType = deviceType
                }
        }

        lifecycleScope.launch {
            dataStoreManager.deviceIP
                .distinctUntilChanged()  // Only collect if the value changes
                .collect { deviceIP ->
                    mainViewModel.deviceIP = deviceIP
                    if (mainViewModel.deviceIP != "" && (!mainViewModel.deviceIP.equals(
                            "Unknown",
                            true
                        ))
                    )
                        showSyncAlert()
                }
        }

        binding.btnPower.setOnClickListener {
            if (shutDown) {
                shutDown = false
                runCommandWithDebounce(KeyEventCodes.KEYCODE_POWER_ON)
            } else {
                shutDown = true
                runCommandWithDebounce(KeyEventCodes.KEYCODE_POWER_OFF)
            }
        }

        binding.btnMouse.setOnClickListener {
            val intent = Intent(this, AirMouseActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        binding.btnKeypad.setOnClickListener {
            val intent = Intent(this, TrackPadActivity::class.java)
            startActivity(intent)
        }

//        binding.btnVolumeUp.setOnClickListener {
//            runCommandWithDebounce(KeyEventCodes.KEYCODE_VOLUME_UP)
//        }
//
//        binding.btnVolumeDown.setOnClickListener {
//            runCommandWithDebounce(KeyEventCodes.KEYCODE_VOLUME_DOWN)
//        }
        
        fun adjustVolume(increase: Boolean) {
            val command = if (increase) {
                "service call audio 3 i32 3 i32 0 i32 1"
            } else {
                "service call audio 3 i32 3 i32 0 i32 2"
            }

            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.btnVolumeUp.setOnClickListener { adjustVolume(true) }
        binding.btnVolumeDown.setOnClickListener { adjustVolume(false) }


        binding.btnMute.setOnClickListener {
            mute = !mute
            binding.btnMute.background = if (mute) {
                resources.getDrawable(R.drawable.mute)
            } else {
                resources.getDrawable(R.drawable.unmute)
            }
           // mainViewModel.getVolumeControl()?.setMute(mute, null)
            runCommandWithDebounce(KeyEventCodes.KEYCODE_MUTE)
        }


        // Handle Navigation buttons with debounce (debounce for direction keys)
        binding.btnLeft.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_DPAD_LEFT) }
        binding.btnUp.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_DPAD_UP) }
        binding.btnRight.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_DPAD_RIGHT) }
        binding.btnDown.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_DPAD_DOWN) }

        // Handle Channel Up and Down (same as navigation buttons)
        binding.channelUp.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_DPAD_UP) }
        binding.channelDown.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_DPAD_DOWN) }

        // Handle Standard Buttons (Back, Ok, Home, Menu, etc.)
        binding.btnBack.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_BACK) }
        binding.btnOk.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_BUTTON_START) }
        binding.btnHome.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_HOME) }
        binding.btnMenu.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MENU) }

        // Handle Media Buttons (Playback, Playforward, etc.)
        binding.btnPlayback.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_PREVIOUS) }
        binding.btnPlayforward.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_NEXT) }

        binding.imgForward.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_FAST_FORWARD) }
        binding.imgBackward.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_REWIND) }

        binding.play.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_PLAY) }
        binding.playPause.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_PLAY_PAUSE) }
    }

    // Function for executing commands with debounce to prevent too fast executions
    private fun runCommandWithDebounce(command: Int) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastCommandTime >= commandDelay) {
            lastCommandTime = currentTime
            mainViewModel.runCommand(command)
        }
    }

    private fun showSyncAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(resources.getString(R.string.device_sync))
            .setCancelable(false) // Makes the dialog non-dismissible by outside touch

        builder.setPositiveButton(resources.getString(R.string.ok)) { dialog, _ ->
            dialog.dismiss()
            setupPicker()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun setupPicker() {
        if (mDiscoveryManager?.allDevices!!.keys.size > 0) {
            for (i in mDiscoveryManager?.allDevices!!.keys) {
                if (mainViewModel.deviceIP != i) {
                    MainViewModel.tv = mDiscoveryManager?.allDevices!![i] as ConnectableDevice
                }
            }
            /*if (mDiscoveryManager?.allDevices!!.keys.size > 1) {
                for (i in mDiscoveryManager?.allDevices!!.keys) {
                    if (mainViewModel.deviceIP != i) {
                        MainViewModel.tv = mDiscoveryManager?.allDevices!![i] as ConnectableDevice
                    }
                }
            } else {
                if (mainViewModel.deviceType != "" && (!mainViewModel.deviceType.equals(
                        "Unknown",
                        true
                    )) && mainViewModel.deviceType == "connect"
                ) {
                    MainViewModel.tv =
                        mDiscoveryManager?.allDevices!![mainViewModel.deviceIP] as ConnectableDevice
                }
            }*/
        }
    }


    @SuppressLint("MissingPermission")
    private fun registerDeviceServices() {
        // Register Android TV devices with SSDP discovery provider
        /*  mDiscoveryManager?.registerDeviceService(
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
