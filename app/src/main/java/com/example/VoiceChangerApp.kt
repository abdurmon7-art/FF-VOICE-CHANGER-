package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.audio.AudioPlayerManager
import com.example.audio.LiveVoiceProcessor
import com.example.data.AppDatabase
import com.example.data.VoicePreferences

class VoiceChangerApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferences: VoicePreferences
        private set

    lateinit var liveVoiceProcessor: LiveVoiceProcessor
        private set

    lateinit var audioPlayerManager: AudioPlayerManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        preferences = VoicePreferences(this)
        liveVoiceProcessor = LiveVoiceProcessor(this)
        audioPlayerManager = AudioPlayerManager(this)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_OVERLAY,
                "Voice Changer Floating Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of floating voice changer overlay controls"
                setShowBadge(false)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_OVERLAY = "voice_changer_overlay_channel"
        lateinit var instance: VoiceChangerApp
            private set
    }
}
