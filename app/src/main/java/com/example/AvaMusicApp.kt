package com.example

import android.app.Application
import android.util.Log
import com.example.data.audio.AudioPlayerEngine
import com.example.data.audio.AudioSynthGenerator
import com.example.data.local.AppDatabase
import com.example.data.repository.MusicRepository
import com.example.widget.NovoWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AvaMusicApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { MusicRepository(this, database) }
    val playerEngine by lazy { AudioPlayerEngine(this) }

    override fun onCreate() {
        super.onCreate()

        // Global crash guard to log any unhandled exceptions gracefully
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AvaMusicApp", "CRITICAL UNCAUGHT EXCEPTION on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize default presets and scan user's device audio asynchronously with safe error guards
        applicationScope.launch {
            try {
                repository.initDefaultPresetsIfEmpty()
            } catch (t: Throwable) {
                Log.e("AvaMusicApp", "Failed to init presets: ${t.message}")
            }
            try {
                repository.deleteDemoTracks()
            } catch (t: Throwable) {
                Log.e("AvaMusicApp", "Failed to clear demo tracks: ${t.message}")
            }
            try {
                repository.scanDeviceAudio()
            } catch (t: Throwable) {
                Log.e("AvaMusicApp", "Failed to scan device audio: ${t.message}")
            }
            try {
                NovoWidgetProvider.refreshAll(applicationContext)
            } catch (t: Throwable) {
                Log.e("AvaMusicApp", "Failed to refresh widget: ${t.message}")
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            playerEngine.release()
        } catch (_: Throwable) {}
    }
}
