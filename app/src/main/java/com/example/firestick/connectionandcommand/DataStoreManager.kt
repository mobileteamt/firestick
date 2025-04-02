package com.example.firestick.connectionandcommand

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreManager(context: Context) {

    // Create the DataStore instance using preferencesDataStore
    private val Context.dataStore by preferencesDataStore(name = "device_info")
    private val dataStore = context.dataStore



    companion object {

        // Define keys for storing data
        private val DEVICE_NAME = stringPreferencesKey("device_name")
        private val DEVICE_IP = stringPreferencesKey("device_ip")
        private val DEVICE_TYPE = stringPreferencesKey("device_type")

        @Volatile
        private var INSTANCE: DataStoreManager? = null

        fun getInstance(context: Context): DataStoreManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataStoreManager(context).also { INSTANCE = it }
            }
        }
    }

    // Function to save the username
    suspend fun saveDeviceName(deviceName: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_NAME] = deviceName
        }
    }

    // Function to save the email
    suspend fun saveDeviceIP(deviceIP: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_IP] = deviceIP
        }
    }

    suspend fun saveDeviceType(deviceType: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_TYPE] = deviceType
        }
    }

    // Function to retrieve the username
    val deviceIP: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[DEVICE_IP] ?: "Unknown"  // Default value if no value exists
        }

    // Function to retrieve the email
    val deviceName: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[DEVICE_NAME] ?: "Unknown"  // Default value if no value exists
        }

    val deviceType: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[DEVICE_TYPE] ?: "Unknown"  // Default value if no value exists
        }
}
