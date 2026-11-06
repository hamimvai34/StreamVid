package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class MediaServiceCommand {
    TOGGLE_PLAY_PAUSE,
    STOP_PLAYBACK
}

object BackgroundPlaybackBridge {
    private val _commands = MutableSharedFlow<MediaServiceCommand>(extraBufferCapacity = 8)
    val commands: SharedFlow<MediaServiceCommand> = _commands.asSharedFlow()

    fun emitCommand(command: MediaServiceCommand) {
        _commands.tryEmit(command)
    }
}

class BackgroundMediaService : Service() {

    companion object {
        private const val CHANNEL_ID = "streamvault_playback_channel"
        private const val NOTIFICATION_ID = 4091

        const val ACTION_START_OR_UPDATE = "com.example.action.START_OR_UPDATE"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.action.TOGGLE_PLAY_PAUSE"
        const val ACTION_STOP = "com.example.action.STOP"

        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_CHANNEL = "extra_channel"
        private const val EXTRA_IS_PLAYING = "extra_is_playing"
        private const val EXTRA_IS_AUDIO_ONLY = "extra_is_audio_only"

        fun updatePlaybackService(
            context: Context,
            title: String,
            channelTitle: String,
            isPlaying: Boolean,
            isAudioOnly: Boolean
        ) {
            try {
                val intent = Intent(context, BackgroundMediaService::class.java).apply {
                    action = ACTION_START_OR_UPDATE
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_CHANNEL, channelTitle)
                    putExtra(EXTRA_IS_PLAYING, isPlaying)
                    putExtra(EXTRA_IS_AUDIO_ONLY, isAudioOnly)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
            }
        }

        fun stopPlaybackService(context: Context) {
            try {
                val intent = Intent(context, BackgroundMediaService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_PLAY_PAUSE -> {
                BackgroundPlaybackBridge.emitCommand(MediaServiceCommand.TOGGLE_PLAY_PAUSE)
            }
            ACTION_STOP -> {
                BackgroundPlaybackBridge.emitCommand(MediaServiceCommand.STOP_PLAYBACK)
                releaseLocks()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_START_OR_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "StreamVault Media"
                val channel = intent.getStringExtra(EXTRA_CHANNEL) ?: "Approved Channel"
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)
                val isAudio = intent.getBooleanExtra(EXTRA_IS_AUDIO_ONLY, false)

                if (isPlaying) {
                    acquireLocks()
                } else {
                    releaseLocks()
                }

                val notification = buildNotification(title, channel, isPlaying, isAudio)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (_: Exception) {
                }
            }
        }
        return START_STICKY
    }

    private fun acquireLocks() {
        try {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "StreamVault::BackgroundAudioWakeLock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(4 * 60 * 60 * 1000L) // Up to 4 hours
            }

            if (wifiLock == null) {
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val lockType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wm.createWifiLock(lockType, "StreamVault::BackgroundWifiLock").apply {
                    setReferenceCounted(false)
                }
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(
        title: String,
        channelName: String,
        isPlaying: Boolean,
        isAudioOnly: Boolean
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, BackgroundMediaService::class.java).apply {
            action = ACTION_TOGGLE_PLAY_PAUSE
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BackgroundMediaService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeLabel = if (isAudioOnly) "Audio Background Play" else "Video Background Play"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText("$channelName • $modeLabel")
            .setSubText(if (isPlaying) "Playing in Background" else "Paused")
            .setContentIntent(contentPendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                togglePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "StreamVault Background Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps approved channel audio and video playing when the app is minimized"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        releaseLocks()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
