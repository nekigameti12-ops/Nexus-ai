package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.UserPreferences
import com.example.voice.DoubleClapDetector

class NexusAssistantService : Service() {

    private var clapDetector: DoubleClapDetector? = null
    private lateinit var userPrefs: UserPreferences

    override fun onCreate() {
        super.onCreate()
        userPrefs = UserPreferences(this)
        createNotificationChannel()

        clapDetector = DoubleClapDetector(this) {
            // On double clap detected: Launch MainActivity with action
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                action = ACTION_DOUBLE_CLAP_WAKE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(launchIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildForegroundNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val settings = userPrefs.settings.value
        if (settings.doubleClapEnabled) {
            clapDetector?.start(settings.clapSensitivity)
        } else {
            clapDetector?.stop()
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nexus Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Nexus AI active for background commands and double-clap activation"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NEXUS AI Online")
            .setContentText("Subsystems active. Listening for wake actions.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        clapDetector?.stop()
        clapDetector = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "nexus_assistant_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DOUBLE_CLAP_WAKE = "com.example.ACTION_DOUBLE_CLAP_WAKE"

        fun startService(context: Context) {
            val intent = Intent(context, NexusAssistantService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NexusAssistantService::class.java)
            context.stopService(intent)
        }
    }
}
