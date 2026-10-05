package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase

class RescueApplication : Application() {

    companion object {
        private const val TAG = "RescueApp"
        lateinit var instance: RescueApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "RescueApplication starting up...")

        // Setup global uncaught exception handler to log crashes and prevent abrupt termination
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "CRITICAL ERROR in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            // Check if Firebase is already initialized
            val initialized = try { FirebaseApp.getApps(this).isNotEmpty() } catch (_: Exception) { false }
            if (!initialized) {
                val app = try {
                    FirebaseApp.initializeApp(this)
                } catch (e: Exception) {
                    Log.w(TAG, "Auto FirebaseApp.initializeApp failed, falling back to explicit options: ${e.message}")
                    null
                }

                if (app == null || FirebaseApp.getApps(this).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:342096270055:android:ed752cfcb1e14d3ff44842")
                        .setApiKey("AIzaSyBp0u6_-VVNUghScAyrZO-CoBNXOROys4I")
                        .setDatabaseUrl("https://gen-lang-client-0618945200-default-rtdb.firebaseio.com/")
                        .setProjectId("gen-lang-client-0618945200")
                        .setStorageBucket("gen-lang-client-0618945200.firebasestorage.app")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                    Log.i(TAG, "FirebaseApp initialized with explicit fallback options successfully")
                }
            }

            // Configure Firebase Realtime Database
            try {
                FirebaseDatabase.getInstance().setPersistenceEnabled(true)
                Log.i(TAG, "Firebase Database offline persistence enabled")
            } catch (e: Exception) {
                // Persistence might already be set or not supported in this process
                Log.w(TAG, "Firebase Database setPersistenceEnabled notice: ${e.message}")
            }

            // Initialize notification channels for real-time status alerts
            try {
                com.example.util.RescueNotificationHelper.initNotificationChannels(this)
            } catch (ne: Exception) {
                Log.w(TAG, "Notification channel initialization notice: ${ne.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase in Application class: ${e.message}", e)
        }
    }
}
