package com.example.firestick.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.connectsdk.device.ConnectableDevice
import com.connectsdk.service.capability.PowerControl
import com.connectsdk.service.capability.VolumeControl
import com.example.firestick.connectionandcommand.runAdb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainViewModel(private val application: Application) : AndroidViewModel(application) {

    var deviceIP: String = ""
    var deviceType: String = ""
    private var powerControl: PowerControl? = null
    private var volumeControl: VolumeControl? = null

    companion object {
        var tv: ConnectableDevice? = null
    }

    fun runCommand(keyCode: Int) {
        // Ensure execution on IO dispatcher for network or device operations
        viewModelScope.launch(Dispatchers.IO) {
            // Check if the deviceIP is valid, directly on the IO dispatcher thread
            if (deviceIP != "Unknown" && deviceIP.isNotEmpty() && deviceIP.isNotBlank()) {
                // Run ADB command in the background
                runAdb(application, "$deviceIP:5555", "shell input keyevent $keyCode")

            } else {
                // Show a toast if the device isn't paired, done on main thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        application,
                        "To begin, go to Settings and connect your TV",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun runVolumeCommand(keyCode: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if the deviceIP is valid, directly on the IO dispatcher thread
            if (deviceIP != "Unknown" && deviceIP.isNotEmpty() && deviceIP.isNotBlank()) {
                // Run ADB command in the background
                runAdb(application, "$deviceIP:5555", "shell cmd media_session volume --show --stream 3 --set 5")

            } else {
                // Show a toast if the device isn't paired, done on main thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        application,
                        "To begin, go to Settings and connect your TV",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun runKeyBoardCommand(command: String) {
        // Ensure execution on IO dispatcher for network or device operations
        viewModelScope.launch(Dispatchers.IO) {
            // Check if the deviceIP is valid, directly on the IO dispatcher thread
            if (deviceIP != "Unknown" && deviceIP.isNotEmpty() && deviceIP.isNotBlank()) {
                // Run ADB command in the background
                runAdb(application, "$deviceIP:5555", command)

            } else {
                // Show a toast if the device isn't paired, done on main thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        application,
                        "To begin, go to Settings and connect your TV",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun getVolumeControl(): VolumeControl? {
        if (tv != null) {
            volumeControl = tv!!.getCapability(VolumeControl::class.java)
            return volumeControl!!
        } else {
            return null
        }
    }



}