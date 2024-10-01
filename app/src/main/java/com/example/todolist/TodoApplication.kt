package com.example.todolist

import android.app.Application
import com.google.android.gms.ads.MobileAds

class TodoApplication : Application() {
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
}