package com.rohit.splitsmart

import android.app.Application
import com.rohit.splitsmart.utils.NotificationUtils

class SplitSmartApp : Application() {
    override fun onCreate() {
        super.onCreate()
        //NotificationUtils.createNotificationChannel(this)
    }
}