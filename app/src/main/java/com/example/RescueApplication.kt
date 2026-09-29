package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class RescueApplication : Application() {

    companion object {
        private const val TAG = "RescueApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "RescueApplication starting up...")

        // Setup global uncaught exception handler to log crashes and prevent abrupt termination
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "CRITICAL ERROR in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            // Ensure Firebase is properly initialized
            val app = FirebaseApp.initializeApp(this)
            Log.i(TAG, "FirebaseApp initialized: ${app?.name}")

            // Configure Firebase Realtime Database
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true)
                Log.i(TAG, "Firebase Database offline persistence enabled")
            } catch (e: Exception) {
                // Persistence might already be set or not supported in this process
                Log.w(TAG, "Firebase Database setPersistenceEnabled notice: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase in Application class: ${e.message}", e)
        }
    }
}
