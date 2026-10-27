package com.svd.svdagencies

import android.app.Application
import android.content.Context
import com.svd.svdagencies.notifications.SvdFirebaseMessagingService
import com.svd.svdagencies.utils.KeepAliveWorker
import com.svd.svdagencies.utils.SessionManager
import com.svd.svdagencies.utils.UserRole

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        context = applicationContext
        SvdFirebaseMessagingService.ensureDefaultChannel(this)
        
        // Start the periodic keep-alive pings ONLY for Admin
        val session = SessionManager(this)
        if (session.getRole() == UserRole.ADMIN) {
            KeepAliveWorker.startNow(this)
        }
    }

    companion object {
        lateinit var instance: App
            private set
        
        lateinit var context: Context
            private set
    }
}
