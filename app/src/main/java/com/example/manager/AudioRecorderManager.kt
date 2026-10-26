package com.example.manager

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null
    private var isRecording = false

    fun startRecording(fileName: String = "voice_memo_${System.currentTimeMillis()}.m4a"): File? {
        val audioDir = File(context.cacheDir, "audio_vault").apply { mkdirs() }
        val outputFile = File(audioDir, fileName)
        currentOutputFile = outputFile

        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            return outputFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to start recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            return null
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return currentOutputFile
        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            currentOutputFile
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to stop recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            currentOutputFile
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            currentOutputFile?.delete()
            currentOutputFile = null
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            if (isRecording) mediaRecorder?.maxAmplitude ?: 0 else 0
        } catch (e: Exception) {
            0
        }
    }

    fun playPreview(file: File, onCompletion: () -> Unit) {
        stopPreview()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    stopPreview()
                    onCompletion()
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Failed to play audio preview", e)
        }
    }

    fun stopPreview() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            mediaPlayer = null
        }
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
}
