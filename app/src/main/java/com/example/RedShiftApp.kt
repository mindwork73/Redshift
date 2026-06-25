package com.example

import android.app.Application
import com.example.service.SettingsStore

class RedShiftApp : Application() {

    lateinit var settingsStore: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
    }
}
