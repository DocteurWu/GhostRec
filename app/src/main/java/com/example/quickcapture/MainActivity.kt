package com.example.quickcapture

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.quickcapture.databinding.ActivityMainBinding
import com.example.quickcapture.utils.PreferencesHelper
import com.example.quickcapture.utils.StorageHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesHelper

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        updatePermissionsStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesHelper(this)

        setupUI()
        updatePermissionsStatus()
        refreshCapturesList()
    }

    private fun setupUI() {
        if (prefs.captureMode == PreferencesHelper.MODE_VIDEO) {
            binding.rbVideoAudio.isChecked = true
        } else {
            binding.rbAudioOnly.isChecked = true
        }

        binding.switchLowPowerAudio.isChecked = prefs.isLowPowerAudio

        binding.rgCaptureMode.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbVideoAudio) {
                prefs.captureMode = PreferencesHelper.MODE_VIDEO
            } else {
                prefs.captureMode = PreferencesHelper.MODE_AUDIO
            }
        }

        binding.switchLowPowerAudio.setOnCheckedChangeListener { _, isChecked ->
            prefs.isLowPowerAudio = isChecked
        }

        binding.btnGrantPermissions.setOnClickListener {
            requestAllPermissions()
        }

        binding.btnIgnoreBattery.setOnClickListener {
            requestBatteryExemption()
        }

        binding.btnMiuiAutostart.setOnClickListener {
            openMiuiAutostart()
        }

        binding.btnRefreshCaptures.setOnClickListener {
            refreshCapturesList()
        }
    }

    private fun updatePermissionsStatus() {
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val statusText = StringBuilder()
        statusText.append("Micro : ${if (audioGranted) "✅" else "❌"}")
        statusText.append(" | Caméra : ${if (cameraGranted) "✅" else "❌"}")
        statusText.append(" | Notif : ${if (notifGranted) "✅" else "❌"}")

        binding.tvPermissionsStatus.text = statusText.toString()
        val allOk = audioGranted && cameraGranted && notifGranted
        binding.tvPermissionsStatus.setTextColor(if (allOk) Color.parseColor("#00E5FF") else Color.parseColor("#FFAA00"))
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestBatteryExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Déjà exempté des optimisations batterie", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openMiuiAutostart() {
        val miuiIntents = listOf(
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
        )

        for (intent in miuiIntents) {
            try {
                startActivity(intent)
                return
            } catch (e: Exception) {
                // Ignore and try next
            }
        }

        // Fallback: App Settings
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun refreshCapturesList() {
        binding.layoutCapturesList.removeAllViews()
        val files = StorageHelper.getPrivateCaptures(this)

        if (files.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Aucune capture enregistrée pour le moment.\nUtilisez l'icône 'Capture Rapide' pour tester."
                setTextColor(Color.parseColor("#808080"))
                textSize = 14f
                setPadding(0, 16, 0, 16)
            }
            binding.layoutCapturesList.addView(emptyTv)
            return
        }

        for (file in files) {
            val card = createCaptureCard(file)
            binding.layoutCapturesList.addView(card)
        }
    }

    private fun createCaptureCard(file: File): MaterialCardView {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            setCardBackgroundColor(Color.parseColor("#2A2A3E"))
            radius = 8f
            cardElevation = 2f
            setPadding(16, 16, 16, 16)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val isVideo = file.extension.lowercase(Locale.getDefault()) == "mp4"
        val icon = if (isVideo) "🎬" else "🎙️"
        val sizeMb = String.format(Locale.getDefault(), "%.2f Mo", file.length().toDouble() / (1024 * 1024))

        val titleTv = TextView(this).apply {
            text = "$icon ${file.name}"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        }

        val metaTv = TextView(this).apply {
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))
            text = "Taille : $sizeMb | Date : $dateStr"
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 12f
            setPadding(0, 4, 0, 8)
        }

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val playBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            text = "Lire"
            setOnClickListener { openFile(file) }
        }

        val deleteBtn = MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Supprimer"
            setTextColor(Color.parseColor("#FF5555"))
            strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5555"))
            setOnClickListener {
                if (file.delete()) {
                    Toast.makeText(this@MainActivity, "Fichier supprimé", Toast.LENGTH_SHORT).show()
                    refreshCapturesList()
                }
            }
        }

        btnRow.addView(playBtn)
        btnRow.addView(deleteBtn)

        container.addView(titleTv)
        container.addView(metaTv)
        container.addView(btnRow)

        card.addView(container)
        return card
    }

    private fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val mimeType = if (file.extension.lowercase(Locale.getDefault()) == "mp4") "video/mp4" else "audio/mp4"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Impossible d'ouvrir le fichier : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
