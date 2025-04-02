package com.example.firestick// DeviceListAdapter.kt

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.BaseAdapter
import android.widget.Button
import com.example.firestick.viewmodel.DeviceInfomation

// Adapter for displaying a list of devices
class DeviceListAdapter(
    private val context: Context,
    private val deviceList: List<DeviceInfomation>,
    private val onItemSelected: (DeviceInfomation,Int) -> Unit // Callback to handle item selection
) : BaseAdapter() {

    private var selectedDevice: DeviceInfomation? = null // Keep track of the selected device

    override fun getCount(): Int {
        return deviceList.size
    }

    override fun getItem(position: Int): Any {
        return deviceList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.device_list_item, parent, false)

        val device = deviceList[position]

        val nameTextView: TextView = view.findViewById(R.id.deviceNameTextView)
        val ipTextView: TextView = view.findViewById(R.id.ipAddressTextView)
        val checkBox: Button = view.findViewById(R.id.connect)

        nameTextView.text = device.name
        ipTextView.text = device.ip


        // Handle checkbox click - allow only one selection
        checkBox.setOnClickListener {
            selectedDevice = device
            onItemSelected(device,position) // Callback to notify selection
            notifyDataSetChanged() // Refresh the list to reflect the selection
        }

        return view
    }
}
