package com.example.quickcapture.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.quickcapture.capture.AudioRecorderManager
import com.example.quickcapture.capture.VideoRecorderManager
import com.example.quickcapture.utils.HapticHelper
import com.example.quickcapture.utils.PreferencesHelper
import com.example.quickcapture.utils.StorageHelper
import java.io.File

class QuickCaptureService : LifecycleService() {

    private var audioRecorder: AudioRecorderManager? = null
    private var videoRecorder: VideoRecorderManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentRecordedFile: File? = null

    companion object {
        var isRunning = false
            private set
        const val CHANNEL_ID = "video_player_silent_channel"
        const val NOTIF_ID = 2002
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (isRunning) {
            stopRecording()
            return START_NOT_STICKY
        }

        startForegroundServiceSilent()
        acquirePartialWakeLock()
        startRecording()

        return START_STICKY
    }

    private fun startForegroundServiceSilent() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lecteur Vidéo Animé")
            .setContentText("Lecture vidéo en arrière-plan...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val prefs = PreferencesHelper(this)
            val serviceType = if (prefs.captureMode == PreferencesHelper.MODE_VIDEO) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIF_ID, notification, serviceType)
        } else {
            startForeground(NOTIF_ID, notification)
        }

        isRunning = true
    }

    private fun acquirePartialWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "QuickCapture::LowPowerWakeLock"
        ).apply {
            acquire(2 * 60 * 60 * 1000L)
        }
    }

    private fun startRecording() {
        val prefs = PreferencesHelper(this)
        HapticHelper.vibrateStart(this)

        val isVideo = prefs.captureMode == PreferencesHelper.MODE_VIDEO
        val outputFile = StorageHelper.getOutputFile(this, isVideo)
        currentRecordedFile = outputFile

        if (isVideo) {
            videoRecorder = VideoRecorderManager(this)
            videoRecorder?.startRecording(
                lifecycleOwner = this,
                outputFile = outputFile,
                onStarted = {
                    Log.d("QuickCaptureService", "Enregistrement vidéo démarré")
                },
                onError = { error ->
                    Log.e("QuickCaptureService", "Erreur vidéo: $error")
                    stopRecording()
                }
            )
        } else {
            audioRecorder = AudioRecorderManager(this)
            val success = audioRecorder?.startRecording(outputFile, isLowPower = prefs.isLowPowerAudio) ?: false
            if (!success) {
                stopRecording()
            }
        }
    }

    private fun stopRecording() {
        audioRecorder?.stopRecording()
        audioRecorder = null

        videoRecorder?.stopRecording()
        videoRecorder = null

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null

        HapticHelper.vibrateStop(this)

        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (isRunning) {
            stopRecording()
        }
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Service Lecteur Multimédia",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
