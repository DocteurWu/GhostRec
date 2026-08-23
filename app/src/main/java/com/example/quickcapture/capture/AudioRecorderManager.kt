package com.example.quickcapture.capture

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var audioManager: AudioManager? = null

    fun startRecording(outputFile: File, isLowPower: Boolean = true, isCall: Boolean = false): Boolean {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (isCall) {
            try {
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            } catch (e: Exception) {
                Log.w("AudioRecorder", "Impossible de régler le mode AudioManager", e)
            }
        }

        val audioSources = if (isCall) {
            listOf(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.CAMCORDER,
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            )
        } else {
            listOf(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.CAMCORDER
            )
        }

        for (source in audioSources) {
            try {
                recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(source)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                    if (isCall) {
                        setAudioSamplingRate(44100)
                        setAudioEncodingBitRate(96000)
                        setAudioChannels(1)
                    } else if (isLowPower) {
                        setAudioSamplingRate(22050)
                        setAudioEncodingBitRate(64000)
                        setAudioChannels(1)
                    } else {
                        setAudioSamplingRate(44100)
                        setAudioEncodingBitRate(96000)
                        setAudioChannels(2)
                    }

                    setOutputFile(outputFile.absolutePath)
                    prepare()
                    start()
                }
                Log.d("AudioRecorder", "Enregistrement démarré avec la source ID: $source -> ${outputFile.name}")
                return true
            } catch (e: Exception) {
                Log.w("AudioRecorder", "Source $source échouée, tentative suivante...", e)
                release()
            }
        }

        Log.e("AudioRecorder", "Impossible d'initialiser une source audio d'appel.")
        return false
    }

    fun stopRecording() {
        try {
            recorder?.stop()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Erreur lors de l'arrêt de MediaRecorder", e)
        } finally {
            release()
        }
    }

    private fun release() {
        try {
            recorder?.release()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Erreur release MediaRecorder", e)
        }
        recorder = null

        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.w("AudioRecorder", "Erreur réinitialisation mode AudioManager", e)
        }
    }
}
