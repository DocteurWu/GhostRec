package com.example.quickcapture.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("quick_capture_prefs", Context.MODE_PRIVATE)

    companion object {
        const val MODE_AUDIO = "AUDIO"
        const val MODE_VIDEO = "VIDEO"

        private const val KEY_MODE = "capture_mode"
        private const val KEY_LOW_POWER = "low_power_audio"
    }

    var captureMode: String
        get() = prefs.getString(KEY_MODE, MODE_AUDIO) ?: MODE_AUDIO
        set(value) = prefs.edit().putString(KEY_MODE, value).apply()

    var isLowPowerAudio: Boolean
        get() = prefs.getBoolean(KEY_LOW_POWER, true)
        set(value) = prefs.edit().putBoolean(KEY_LOW_POWER, value).apply()
}
