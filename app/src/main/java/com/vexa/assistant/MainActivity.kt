package com.vexa.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var speech: SpeechRecognizer
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        val listen = findViewById<Button>(R.id.listen)
        tts = TextToSpeech(this, this)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 10)
        }
        speech = SpeechRecognizer.createSpeechRecognizer(this)
        listen.setOnClickListener { listenNow() }
        status.text = "VEXA ONLINE\nTap the button and speak"
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            val preferred = listOf(Locale("hi", "IN"), Locale("en", "IN"), Locale.US)
            for (locale in preferred) {
                if (tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = locale
                    break
                }
            }
            tts.setSpeechRate(0.92f)
            tts.setPitch(1.02f)
        }
    }

    private fun speak(text: String) {
        if (::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VEXA_REPLY")
    }

    private fun listenNow() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            reply("Sir, is phone par voice recognition available nahi hai.")
            return
        }
        status.text = "🎙 Listening..."
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speech.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isBlank()) reply("Sir, mujhe command clear nahi mili. Dobara boliye.") else handleCommand(text)
            }
            override fun onError(error: Int) { reply("Sir, command clear nahi mili. Dobara boliye.") }
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

    private fun handleCommand(command: String) {
        val c = command.lowercase(Locale.getDefault()).trim()
        when {
            c.contains("heat") || c.contains("garam") || c.contains("temperature") || c.contains("tapman") -> checkTemperature()
            c.contains("camera") || c.contains("कैमरा") -> openCamera()
            c.contains("whatsapp") -> openPackage("com.whatsapp", "Haan sir, WhatsApp kholti hoon.")
            c.contains("youtube") -> openPackage("com.google.android.youtube", "Haan sir, YouTube kholti hoon.")
            c.contains("chrome") || c.contains("browser") -> openPackage("com.android.chrome", "Haan sir, Chrome kholti hoon.")
            c.contains("wifi") || c.contains("wi-fi") -> openSettings(Settings.ACTION_WIFI_SETTINGS, "Theek hai sir, Wi-Fi settings kholti hoon.")
            c.contains("bluetooth") -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Theek hai sir, Bluetooth settings kholti hoon.")
            c.contains("battery") || c.contains("charge") -> checkBattery()
            c.startsWith("call ") || c.startsWith("phone karo ") || c.contains("call karo") -> {
                val number = c.replace("call karo", "").replace("phone karo", "").replace("call", "").trim()
                if (number.matches(Regex("[0-9 +()-]{6,}"))) dial(number) else reply("Sir, kis number par call karni hai?")
            }
            c.contains("meaning") || c.contains("matlab") || c.contains("arth") -> reply("Haan sir, word ya sentence bataiye, main simple Hindi mein samjha deti hoon.")
            c.contains("hello") || c.contains("namaste") || c == "vexa" -> reply("Haan sir, boliye.")
            else -> reply("Haan sir, maine suna. Is kaam ko karne ke liye mujhe thoda aur detail chahiye.")
        }
    }

    private fun checkTemperature() {
        val batteryIntent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val raw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        if (raw <= 0) { reply("Sir, phone temperature ki reading available nahi hai."); return }
        val temp = raw / 10f
        if (temp >= 40f) reply("Sir, aapka phone heat ho raha hai. Temperature ${String.format(Locale.US, "%.1f", temp)} degree hai. Thoda rest dena better rahega.")
        else reply("Sir, temperature ${String.format(Locale.US, "%.1f", temp)} degree hai. Abhi theek hai.")
    }

    private fun checkBattery() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        reply("Sir, battery ${level} percent hai.")
    }

    private fun openPackage(pkg: String, message: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) { startActivity(launch); speak(message) }
        else reply("Sir, ye app phone mein installed nahi mil raha.")
    }

    private fun openCamera() {
        try { startActivity(Intent("android.media.action.IMAGE_CAPTURE")); speak("Haan sir, camera kholti hoon.") }
        catch (_: Exception) { reply("Sir, camera open nahi ho paya.") }
    }

    private fun openSettings(action: String, message: String) {
        try { startActivity(Intent(action)); speak(message) }
        catch (_: Exception) { reply("Sir, settings open nahi ho payi.") }
    }

    private fun dial(number: String) {
        speak("Haan sir, dialer kholti hoon.")
        startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number))))
    }

    private fun reply(text: String) {
        status.text = "VEXA\n\n$text"
        speak(text)
    }

    override fun onDestroy() {
        if (::speech.isInitialized) speech.destroy()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}
