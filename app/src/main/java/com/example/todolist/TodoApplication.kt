package com.example.todolist

import android.app.Application

class TodoApplication : Application() {
    lateinit var adMobManager: AdMobManager

    override fun onCreate() {
        super.onCreate()
        adMobManager = AdMobManager(this)
        adMobManager.initialize()
    }
}