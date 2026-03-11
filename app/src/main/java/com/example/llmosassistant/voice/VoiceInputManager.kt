package com.example.llmosassistant.voice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.content.ContextCompat

class VoiceInputManager(
    private val activity: Activity,
    private val onResult: (String) -> Unit
) {

    private var recognizer: SpeechRecognizer? = null

    fun startListening() {

        // 1️⃣ Permission check (hard stop if missing)
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(activity, "Mic permission not granted", Toast.LENGTH_LONG).show()
            return
        }

        // 2️⃣ Speech service availability
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            Toast.makeText(activity, "Speech recognition not available", Toast.LENGTH_LONG).show()
            return
        }

        // 3️⃣ Clean previous instance (OEM fix)
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(activity)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, activity.packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(activity, "Listening…", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle) {
                val list =
                    results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!list.isNullOrEmpty()) {
                    onResult(list[0])
                }
            }

            override fun onError(error: Int) {
                Toast.makeText(activity, "Speech error: $error", Toast.LENGTH_LONG).show()

                // 🔁 Fallback to system speech UI (guaranteed)
                launchFallbackSpeechUI()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer?.startListening(intent)
    }

    private fun launchFallbackSpeechUI() {
        val fallbackIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }
        activity.startActivityForResult(fallbackIntent, 999)
    }
}
