package com.example.craftket

import android.app.Application
import com.example.craftket.utilites.ImageLoader
import com.example.craftket.utilites.SignalManager

class App:Application() {
    override fun onCreate() {
        super.onCreate()
        ImageLoader.init(this)
        SignalManager.init(this)
    }
}