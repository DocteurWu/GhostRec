package com.example.quickcapture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.quickcapture.service.QuickCaptureService
import com.example.quickcapture.utils.PreferencesHelper

class QuickCaptureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferencesHelper(this)
        val isVideo = prefs.captureMode == PreferencesHelper.MODE_VIDEO

        if (hasRequiredPermissions(isVideo)) {
            toggleService()
        } else {
            // Si les permissions manquent, ouvrir les paramètres MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        // Fermer l'activité immédiatement sans rien afficher à l'écran
        finish()
    }

    private fun toggleService() {
        val intent = Intent(this, QuickCaptureService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun hasRequiredPermissions(isVideo: Boolean): Boolean {
        val audioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!audioPermission) return false

        if (isVideo) {
            val cameraPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
            if (!cameraPermission) return false
        }

        return true
    }
}
