package com.example.quickcapture.capture

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

class VideoRecorderManager(private val context: Context) {

    private var activeRecording: Recording? = null
    private var videoCapture: VideoCapture<Recorder>? = null

    fun startRecording(
        lifecycleOwner: LifecycleOwner,
        outputFile: File,
        onStarted: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                
                // Utiliser la caméra arrière avec la qualité SD/HD optimisée pour consommer moins de batterie
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.SD)) // SD consomme beaucoup moins de batterie
                    .build()

                videoCapture = VideoCapture.withOutput(recorder)

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    videoCapture
                )

                val fileOutputOptions = FileOutputOptions.Builder(outputFile).build()

                activeRecording = videoCapture?.output
                    ?.prepareRecording(context, fileOutputOptions)
                    ?.withAudioEnabled()
                    ?.start(ContextCompat.getMainExecutor(context)) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> {
                                Log.d("VideoRecorder", "Capture vidéo démarrée: ${outputFile.name}")
                                onStarted()
                            }
                            is VideoRecordEvent.Finalize -> {
                                if (event.hasError()) {
                                    Log.e("VideoRecorder", "Erreur finalisation vidéo: ${event.error}")
                                    onError("Erreur d'enregistrement vidéo")
                                } else {
                                    Log.d("VideoRecorder", "Capture vidéo sauvegardée")
                                }
                            }
                        }
                    }

            } catch (e: Exception) {
                Log.e("VideoRecorder", "Erreur d'initialisation de la caméra", e)
                onError(e.message ?: "Erreur caméra")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopRecording() {
        try {
            activeRecording?.stop()
            activeRecording = null
        } catch (e: Exception) {
            Log.e("VideoRecorder", "Erreur lors de l'arrêt de la vidéo", e)
        }
    }
}
