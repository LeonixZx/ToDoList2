package com.example.todolist

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.initialization.InitializationStatus

object AdManager {
    private const val TAG = "AdManager"

    fun initialize(context: Context) {
        MobileAds.initialize(context) { initializationStatus: InitializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for ((adapterClass, status) in statusMap) {
                Log.d(TAG, "Adapter name: $adapterClass, Description: ${status.description}, Latency: ${status.latency}")
            }
        }

        // Set your test device ID here
        val testDeviceIds = listOf("496458702")
        val configuration = RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build()
        MobileAds.setRequestConfiguration(configuration)

        // Log the configuration
        Log.d(TAG, "Test device IDs set: ${configuration.testDeviceIds}")
    }

    fun createAdRequest(): AdRequest {
        return AdRequest.Builder().build()
    }
}