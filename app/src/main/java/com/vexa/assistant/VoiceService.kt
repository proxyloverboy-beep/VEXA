package com.vexa.assistant

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.IBinder
import android.provider.ContactsContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

class VoiceService : Service(), TextToSpeech.OnInitListener {
    private lateinit var recognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech
    private var listening = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = Notification.Builder(this, "vexa_voice")
            .setContentTitle("VEXA is listening")
            .setContentText("Say “Hey VEXA” to give a command")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        startForeground(701, notification)
        tts = TextToSpeech(this, this)
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        startListening()
    }

    override fun onInit(result: Int) {
        if (result != TextToSpeech.SUCCESS) return
        val voice = tts.voices.orEmpty()
            .filter { it.locale.language == "hi" || (it.locale.language == "en" && it.locale.country == "IN") }
            .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.latency })
            .firstOrNull()
        if (voice != null) tts.voice = voice
        tts.setSpeechRate(0.92f)
        tts.setPitch(1.0f)
    }

    private fun startListening() {
        if (listening || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        listening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                listening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                if (text.isNotBlank()) process(text)
                restartListening()
            }
            override fun onError(error: Int) { listening = false; restartListening() }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(intent)
    }

    private fun restartListening() = android.os.Handler(mainLooper).postDelayed({ startListening() }, 350)

    private fun process(raw: String) {
        val c = raw.lowercase(Locale.getDefault())
        if (!c.contains("vexa") && !c.contains("वेक्सा")) return
        val command = c.replace(Regex("\\bhey\\s*vexa\\b"), "")
            .replace(Regex("\\bvexa\\b"), "")
            .replace("वेक्सा", "")
            .trim()
        if (command.isBlank()) return
        when {
            command.contains("call") || command.contains("phone") || command.contains("फोन") || command.contains("कॉल") -> callContact(command)
            command.contains("youtube") -> openApp("com.google.android.youtube", "YouTube khul gaya sir.")
            command.contains("chrome") || command.contains("browser") -> openApp("com.android.chrome", "Chrome khul gaya sir.")
            command.contains("camera") -> openIntent(Intent("android.media.action.IMAGE_CAPTURE"), "Camera khul gaya sir.")
            command.contains("battery") || command.contains("charge") -> battery()
            command.contains("heat") || command.contains("garam") || command.contains("temperature") -> temperature()
            else -> speak("Ye kaam abhi available nahi hai sir.")
        }
    }

    private fun callContact(command: String) {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            speak("Sir, call aur contacts permission chahiye."); return
        }
        val name = command.replace(Regex("call|phone|karo|kar do|करो|कर दो|फोन|कॉल"), " ").trim()
        if (name.isBlank()) { speak("Sir, contact ka naam chahiye."); return }
        var number: String? = null
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"), null)
        cursor?.use { if (it.moveToFirst()) number = it.getString(0) }
        if (number == null) { speak("Sir, $name contact nahi mila."); return }
        try {
            startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(number)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            speak("Call lag gaya hai sir.")
        } catch (_: Exception) { speak("Sir, call nahi lag paya.") }
    }

    private fun openApp(pkg: String, message: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) { speak("Sir, ye app installed nahi hai."); return }
        try { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); speak(message) }
        catch (_: Exception) { speak("Sir, app open nahi ho paya.") }
    }

    private fun openIntent(intent: Intent, message: String) {
        try { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); speak(message) }
        catch (_: Exception) { speak("Sir, ye kaam nahi ho paya.") }
    }

    private fun battery() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        speak("Battery ${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)} percent hai sir.")
    }

    private fun temperature() {
        val i = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val raw = i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        if (raw <= 0) { speak("Sir, temperature reading available nahi hai."); return }
        val t = raw / 10f
        if (t >= 40f) speak("Sir, phone heat ho raha hai. Temperature ${String.format(Locale.US, "%.1f", t)} degree hai.")
        else speak("Sir, phone temperature ${String.format(Locale.US, "%.1f", t)} degree hai.")
    }

    private fun speak(text: String) { if (::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VEXA_${System.currentTimeMillis()}") }

    private fun createChannel() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("vexa_voice", "VEXA Voice", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onDestroy() {
        if (::recognizer.isInitialized) recognizer.destroy()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
