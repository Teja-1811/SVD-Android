package com.svd.svdagencies

import android.app.Application
import android.content.Context

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        context = applicationContext
    }

    companion object {
        lateinit var instance: App
            private set
        
        lateinit var context: Context
            private set
    }
}
