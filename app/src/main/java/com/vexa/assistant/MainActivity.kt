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
import android.speech.tts.Voice
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
        if (result != TextToSpeech.SUCCESS) return
        val voices = tts.voices.orEmpty()
        val preferred = voices
            .filter { it.locale.language == "hi" || it.locale.language == "en" }
            .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.latency })
            .firstOrNull { it.locale.language == "hi" && it.locale.country == "IN" }
            ?: voices
                .filter { it.locale.language == "en" && it.locale.country == "IN" }
                .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.latency })
                .firstOrNull()
        if (preferred != null) tts.voice = preferred
        tts.setSpeechRate(0.88f)
        tts.setPitch(1.03f)
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
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        speech.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isBlank()) reply("Sir, command clear nahi mili. Dobara boliye.") else handleCommand(text)
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
            c.contains("whatsapp") -> openPackage("com.whatsapp", "WhatsApp khul gaya sir.")
            c.contains("youtube") && (c.contains("search") || c.contains("dhundo") || c.contains("dhoondo") || c.contains("khojo")) -> youtubeSearch(c)
            c.contains("youtube") -> openPackage("com.google.android.youtube", "YouTube khul gaya sir.")
            c.contains("chrome") && (c.contains("search") || c.contains("google") || c.contains("dhundo") || c.contains("dhoondo") || c.contains("khojo")) -> chromeSearch(c)
            c.contains("chrome") || c.contains("browser") -> openPackage("com.android.chrome", "Chrome khul gaya sir.")
            c.contains("wifi") || c.contains("wi-fi") -> openSettings(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi settings khul gayi sir.")
            c.contains("bluetooth") -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth settings khul gayi sir.")
            c.contains("battery") || c.contains("charge") -> checkBattery()
            c.startsWith("call ") || c.startsWith("phone karo ") || c.contains("call karo") -> {
                val number = c.replace("call karo", "").replace("phone karo", "").replace("call", "").trim()
                if (number.matches(Regex("[0-9 +()-]{6,}"))) dial(number) else reply("Sir, kis number par call karni hai?")
            }
            c.contains("meaning") || c.contains("matlab") || c.contains("arth") -> reply("Word ya sentence bataiye sir, main simple Hindi mein meaning samjha deti hoon.")
            c.contains("hello") || c.contains("namaste") || c == "vexa" -> reply("Haan sir, boliye.")
            else -> reply("Theek hai sir. Is kaam ko karne ke liye mujhe thodi aur detail chahiye.")
        }
    }

    private fun chromeSearch(command: String) {
        val query = command
            .replace("chrome", "", ignoreCase = true)
            .replace("google", "", ignoreCase = true)
            .replace("search karo", "", ignoreCase = true)
            .replace("search kar", "", ignoreCase = true)
            .replace("search", "", ignoreCase = true)
            .replace("dhundo", "", ignoreCase = true)
            .replace("dhoondo", "", ignoreCase = true)
            .replace("khojo", "", ignoreCase = true)
            .trim()
        if (query.isBlank()) {
            openPackage("com.android.chrome", "Chrome khul gaya sir.")
            return
        }
        val url = "https://www.google.com/search?q=" + Uri.encode(query)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { setPackage("com.android.chrome") })
            speak("Sir, search kar diya.")
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            speak("Sir, search kar diya.")
        }
    }

    private fun youtubeSearch(command: String) {
        val query = command
            .replace("youtube", "", ignoreCase = true)
            .replace("search karo", "", ignoreCase = true)
            .replace("search kar", "", ignoreCase = true)
            .replace("search", "", ignoreCase = true)
            .replace("dhundo", "", ignoreCase = true)
            .replace("dhoondo", "", ignoreCase = true)
            .replace("khojo", "", ignoreCase = true)
            .trim()
        if (query.isBlank()) {
            openPackage("com.google.android.youtube", "YouTube khul gaya sir.")
            return
        }
        val url = "https://www.youtube.com/results?search_query=" + Uri.encode(query)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { setPackage("com.google.android.youtube") })
            speak("Sir, YouTube par search kar diya.")
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            speak("Sir, YouTube par search kar diya.")
        }
    }

    private fun checkTemperature() {
        val batteryIntent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val raw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        if (raw <= 0) { reply("Sir, phone temperature ki reading available nahi hai."); return }
        val temp = raw / 10f
        if (temp >= 40f) reply("Sir, phone heat ho raha hai. Temperature ${String.format(Locale.US, "%.1f", temp)} degree hai. Thoda rest dena better rahega.")
        else reply("Sir, temperature ${String.format(Locale.US, "%.1f", temp)} degree hai. Abhi theek hai.")
    }

    private fun checkBattery() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        reply("Sir, battery ${level} percent hai.")
    }

    private fun openPackage(pkg: String, message: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            startActivity(launch)
            speak(message)
        } else reply("Sir, ye app phone mein installed nahi mil raha.")
    }

    private fun openCamera() {
        try { startActivity(Intent("android.media.action.IMAGE_CAPTURE")); speak("Camera khul gaya sir.") }
        catch (_: Exception) { reply("Sir, camera open nahi ho paya.") }
    }

    private fun openSettings(action: String, message: String) {
        try { startActivity(Intent(action)); speak(message) }
        catch (_: Exception) { reply("Sir, settings open nahi ho payi.") }
    }

    private fun dial(number: String) {
        speak("Sir, dialer khol diya.")
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
