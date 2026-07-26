package com.svd.svdagencies

import android.app.Application
import android.content.Context
import com.svd.svdagencies.notifications.SvdFirebaseMessagingService

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        context = applicationContext
        SvdFirebaseMessagingService.ensureDefaultChannel(this)
    }

    companion object {
        lateinit var instance: App
            private set
        
        lateinit var context: Context
            private set
    }
}
