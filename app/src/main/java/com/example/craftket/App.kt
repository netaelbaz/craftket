package com.example.craftket

import android.app.Application
import com.example.craftket.utilites.ImageLoader

class App:Application() {
    override fun onCreate() {
        super.onCreate()
        ImageLoader.init(this)
    }
}