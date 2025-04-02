package com.example.firestick.viewmodel

import com.connectsdk.device.ConnectableDevice

data class DeviceInfomation(
    val ip: String,
    val name: String,
    val from: String,
    val connectableDevice: ConnectableDevice?
)