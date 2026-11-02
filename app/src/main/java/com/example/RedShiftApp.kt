package com.example

import android.app.Application
import android.util.Log
import com.example.service.DiagLog
import com.example.service.SettingsStore
import java.io.File
import java.io.FileWriter

class RedShiftApp : Application() {

    lateinit var settingsStore: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        DiagLog.init(filesDir)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // One line in the shared log keeps the crash in sequence with the events
                // that led to it (the stack itself goes to the crashes/ folder).
                DiagLog.d("Crash", "uncaught on ${thread.name}: $throwable")
                val crashDir = File(filesDir, "crashes")
                crashDir.mkdirs()
                val crashFile = File(crashDir, "crash_${System.currentTimeMillis()}.txt")
                FileWriter(crashFile).use { w ->
                    w.write("Thread: ${thread.name}\n")
                    w.write("Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n\n")
                    throwable.printStackTrace(java.io.PrintWriter(w))
                }
                Log.e("RedShiftCrash", "Crash saved to ${crashFile.absolutePath}", throwable)
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
        settingsStore = SettingsStore(this)
    }
}
