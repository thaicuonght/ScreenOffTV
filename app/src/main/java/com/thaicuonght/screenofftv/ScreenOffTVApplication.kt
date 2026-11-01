package com.thaicuonght.screenofftv

import android.app.Application

class ScreenOffTVApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        createNotificationChannels()
    }
}
