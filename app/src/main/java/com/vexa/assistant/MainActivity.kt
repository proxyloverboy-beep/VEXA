package com.vexa.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var speech: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        val listen = findViewById<Button>(R.id.listen)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
        }

        speech = SpeechRecognizer.createSpeechRecognizer(this)
        listen.setOnClickListener { listenNow() }
        status.text = "VEXA ready — tap and speak"
    }

    private fun listenNow() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Voice recognition is not available on this phone"
            return
        }
        status.text = "Listening..."
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to VEXA")
        }
        speech.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                status.text = if (text.isBlank()) "I didn't catch that" else "You: $text\n\n${respond(text)}"
            }
            override fun onError(error: Int) { status.text = "I couldn't understand that. Try again." }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speech.startListening(intent)
    }

    private fun respond(command: String): String {
        val c = command.lowercase(Locale.getDefault())
        if (c.contains("heat") || c.contains("garam")) {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val temp = (bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_TEMPERATURE) / 10f)
            return if (temp >= 40f) "Sir, aapka phone heat ho raha hai. Thoda rest dena better hoga. Temperature: ${temp}°C" else "Sir, phone temperature normal lag raha hai: ${temp}°C"
        }
        return when {
            c.contains("hello") || c.contains("namaste") || c.contains("vexa") -> "Yes sir, VEXA online."
            c.contains("meaning") || c.contains("matlab") -> "Sir, jis word ya sentence ka matlab chahiye, woh bol dijiye."
            else -> "Sir, command samajh aayi: $command"
        }
    }

    override fun onDestroy() {
        speech.destroy()
        super.onDestroy()
    }
}
