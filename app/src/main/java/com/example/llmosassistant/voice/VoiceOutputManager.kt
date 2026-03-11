package com.example.llmosassistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceOutputManager(context: Context) {

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    fun speak(text: String) {
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "assistant_reply"
        )
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
