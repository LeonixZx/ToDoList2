package com.example.todolist

import android.app.Application
import com.google.android.gms.ads.MobileAds
import androidx.work.Configuration
import androidx.work.WorkManager

class TodoApplication : Application(), Configuration.Provider {
    lateinit var adMobManager: AdMobManager
    lateinit var appOpenAdManager: AppOpenAdManager

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        AdManager.initialize(this)
        adMobManager = AdMobManager(this)
        adMobManager.initialize()
        appOpenAdManager = AppOpenAdManager(this)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    }
}