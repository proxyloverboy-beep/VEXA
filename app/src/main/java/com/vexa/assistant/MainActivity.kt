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
            tts.language = Locale("hi", "IN")
            tts.setSpeechRate(0.95f)
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "VEXA ko command dijiye")
        }
        speech.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isBlank()) reply("Sir, mujhe command clear nahi mili.") else handleCommand(text)
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
            c.contains("vexa") || c.contains("hello") || c.contains("namaste") ->
                reply("Yes sir, VEXA online. Aapki command ready hai.")

            c.contains("heat") || c.contains("garam") || c.contains("temperature") || c.contains("tapman") ->
                checkTemperature()

            c.contains("camera") || c.contains("कैमरा") -> openCamera()

            c.contains("whatsapp") -> openPackage("com.whatsapp", "Sir, WhatsApp khol raha hoon.")

            c.contains("youtube") -> openPackage("com.google.android.youtube", "Sir, YouTube khol raha hoon.")

            c.contains("chrome") || c.contains("browser") -> openPackage("com.android.chrome", "Sir, Chrome khol raha hoon.")

            c.contains("wifi") || c.contains("wi-fi") -> openSettings(Settings.ACTION_WIFI_SETTINGS, "Sir, Wi-Fi settings khol raha hoon.")

            c.contains("bluetooth") -> openSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Sir, Bluetooth settings khol raha hoon.")

            c.contains("battery") || c.contains("battery kitna") || c.contains("charge") -> checkBattery()

            c.startsWith("call ") || c.startsWith("phone karo ") || c.contains("call karo") -> {
                val number = c.replace("call karo", "").replace("phone karo", "").replace("call", "").trim()
                if (number.matches(Regex("[0-9 +()-]{6,}"))) {
                    dial(number, "Sir, dialer khol raha hoon.")
                } else reply("Sir, call ke liye number boliye. Main pehle dialer kholunga.")
            }

            c.contains("meaning") || c.contains("matlab") || c.contains("arth") ->
                reply("Sir, jis word ya sentence ka matlab chahiye, woh dobara clearly bol dijiye.")

            c.contains("phone kholo") || c.contains("phone open") || c.contains("unlock") ->
                reply("Sir, security ki wajah se VEXA phone ka lock bypass nahi karega. Main allowed apps aur settings voice se khol sakta hoon.")

            else -> reply("Sir, command mili: $command. Is command ka action abhi VEXA mein add karna baaki hai.")
        }
    }

    private fun checkTemperature() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_TEMPERATURE)
        if (raw <= 0) {
            reply("Sir, phone temperature sensor ki reading available nahi hai.")
            return
        }
        val temp = raw / 10f
        if (temp >= 40f) reply("Sir, aapka phone heat ho raha hai. Temperature ${temp} degree Celsius hai. Thoda rest dena better hoga.")
        else reply("Sir, phone temperature ${temp} degree Celsius hai. Abhi normal range mein lag raha hai.")
    }

    private fun checkBattery() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        reply("Sir, battery ${level} percent hai.")
    }

    private fun openPackage(pkg: String, message: String) {
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch != null) {
            reply(message)
            startActivity(launch)
        } else reply("Sir, ye app phone mein installed nahi mil raha.")
    }

    private fun openCamera() {
        try {
            reply("Sir, camera khol raha hoon.")
            startActivity(Intent("android.media.action.IMAGE_CAPTURE"))
        } catch (_: Exception) {
            reply("Sir, camera open nahi ho paya.")
        }
    }

    private fun openSettings(action: String, message: String) {
        try {
            reply(message)
            startActivity(Intent(action))
        } catch (_: Exception) {
            reply("Sir, settings open nahi ho payi.")
        }
    }

    private fun dial(number: String, message: String) {
        reply(message)
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
