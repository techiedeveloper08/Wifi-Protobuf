package com.wifitask

import android.app.Application
import com.wifitask.wifi.WifiClient

class App: Application() {

    lateinit var wifiClient: WifiClient

    init {
        instance = this
    }

    companion object {
        lateinit var instance: App
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        wifiClient = WifiClient().apply { context = this@App }
    }
}