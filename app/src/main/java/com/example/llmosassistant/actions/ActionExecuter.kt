package com.example.llmosassistant.actions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFocusRequest
import android.net.Uri
import android.provider.AlarmClock
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.llmosassistant.utils.ContactResolver
import android.media.AudioManager
import android.hardware.camera2.CameraManager
import com.example.llmosassistant.data.MemoryRepository
import com.example.llmosassistant.data.AssistantMemory
import com.example.llmosassistant.utils.PdfGenerator
import com.example.llmosassistant.utils.PdfShareHelper
import java.io.File
import android.hardware.camera2.CameraCharacteristics

class ActionExecutor(private val context: Context) {

    /* ---------------- PERMISSION HELPER ---------------- */

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    private val memoryRepository = MemoryRepository()
    /* ---------------- CORE EXECUTOR ---------------- */

    fun execute(intent: String?, app: String?) {

        val normalizedIntent = intent
            ?.trim()
            ?.uppercase()

        android.util.Log.d(
            "EXECUTOR",
            "EXECUTOR CALLED → intent=$normalizedIntent app=$app"
        )

        if (normalizedIntent != "OPEN_APP" || app.isNullOrBlank()) {
            android.util.Log.d("EXECUTOR", "Rejected command")
            return
        }

        when (app.lowercase().trim()) {
            "settings" -> {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return
            }
            "spotify" -> {
                openSpotify()
                return
            }
            "youtube" -> {
                val launchIntent = context.packageManager
                    .getLaunchIntentForPackage("com.google.android.youtube")

                launchIntent?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    context.startActivity(this)
                } ?: run {
                    Toast.makeText(context, "YouTube not installed", Toast.LENGTH_SHORT).show()
                }

                return
            }
        }

        openAnyApp(app)
    }

    /* ---------------- UNIVERSAL APP OPENER ---------------- */

    private fun openAnyApp(appName: String) {

        val pm = context.packageManager
        val search = appName.lowercase().trim()

        android.util.Log.d("EXECUTOR", "Trying to open: $search")

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val launcherApps = pm.queryIntentActivities(launcherIntent, 0)

        launcherApps.firstOrNull { info ->
            val label = info.loadLabel(pm)?.toString()?.lowercase() ?: ""
            val pkg = info.activityInfo.packageName.lowercase()

            label.contains(search) || pkg.contains(search)
        }?.let { match ->
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(
                    match.activityInfo.packageName,
                    match.activityInfo.name
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            return
        }

        val installed = pm.getInstalledPackages(0)

        installed.firstOrNull { pkg ->
            pkg.packageName.lowercase().contains(search)
        }?.let { pkg ->
            pm.getLaunchIntentForPackage(pkg.packageName)?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }

        val knownPackages = mapOf(
            "instagram" to "com.instagram.android",
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome"
        )

        knownPackages[search]?.let { packageName ->
            context.packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }
        memoryRepository.logActivity(
            type = "OPEN_APP",
            title = "Opened App",
            description = "App: $appName"
        )

        Toast.makeText(
            context,
            "App not found: $appName",
            Toast.LENGTH_SHORT
        ).show()
    }

    /* ---------------- WHATSAPP ---------------- */

    fun sendWhatsAppMessage(contactName: String, message: String) {
        val number = ContactResolver.getPhoneNumber(context, contactName)

        if (number == null) {
            Toast.makeText(context, "Contact not found", Toast.LENGTH_LONG).show()
            return
        }

        val uri = Uri.parse(
            "https://wa.me/${number.replace("\\s".toRegex(), "")}?text=${Uri.encode(message)}"
        )

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        memoryRepository.logActivity(
            "WHATSAPP_MESSAGE",
            "Sent WhatsApp Message",
            "Sent message to $contactName"
        )
    }

    /* ---------------- YOUTUBE ---------------- */

    fun searchYouTube(query: String) {
        val intent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        memoryRepository.logActivity(
            "YOUTUBE_SEARCH",
            "Searched YouTube",
            "User searched for $query"
        )

    }

    fun playYouTubeVideo(videoId: String) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/watch?v=$videoId")
        ).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    /* ---------------- SPOTIFY ---------------- */

    private fun openSpotify() {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("spotify:")
            ).apply {
                setPackage("com.spotify.music")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            val storeIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.spotify.music")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(storeIntent)
        }
    }

    fun playSpotifySong(query: String) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("spotify:search:${Uri.encode(query)}")
            ).apply {
                setPackage("com.spotify.music")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            val storeIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=com.spotify.music")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(storeIntent)
            memoryRepository.logActivity(
                type = "SPOTIFY_PLAY",
                title = "Played Spotify Song",
                description = "Query: $query"
            )
        }
    }

    /* ---------------- ALARM ---------------- */

    fun setAlarm(hour: Int, minute: Int, label: String?) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            if (!label.isNullOrEmpty()) {
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    /* ===================== CALL ===================== */

    fun callContactByName(name: String) {

        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            Toast.makeText(context, "Contacts permission not granted", Toast.LENGTH_LONG).show()
            return
        }

        val phone = ContactResolver.getPhoneNumber(context, name)

        if (phone == null) {
            Toast.makeText(context, "Contact not found", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    /* ===================== SEND SMS ===================== */

    fun sendSmsByContactName(name: String, message: String) {

        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            Toast.makeText(context, "Contacts permission not granted", Toast.LENGTH_LONG).show()
            return
        }

        val phone = ContactResolver.getPhoneNumber(context, name)

        if (phone == null) {
            Toast.makeText(context, "Contact not found", Toast.LENGTH_LONG).show()
            return
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
    //SYSTEM LEVEL
    fun controlSystem(setting: String?) {

        if (setting.isNullOrBlank()) return

        val intent = when (setting.lowercase().trim()) {

            "wifi" ->
                Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)

            "bluetooth" ->
                Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)

            "hotspot" ->
                Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)

            "display" ->
                Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)

            "battery" ->
                Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)

            "accessibility" ->
                Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)

            else -> null
        }

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            memoryRepository.logActivity(
                "SYSTEM_SETTINGS",
                "Opened $setting settings",
                "User opened $setting settings"
            )
        } else {
            Toast.makeText(context, "Unsupported setting", Toast.LENGTH_SHORT).show()
        }
        memoryRepository.logActivity(
            type = "SYSTEM_CONTROL",
            title = "Opened System Setting",
            description = "Setting: $setting"
        )
    }


    //VOLUME CONTROL
    fun controlVolume(action: String?, value: Int?) {

        if (action.isNullOrBlank()) return

        val audioManager =
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        when (action.lowercase().trim()) {

            "increase" -> {
                audioManager.adjustVolume(
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
            }

            "decrease" -> {
                audioManager.adjustVolume(
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
            }

            "mute" -> {
                audioManager.adjustVolume(
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
            }

            "max" -> {
                repeat(10) {
                    audioManager.adjustVolume(
                        AudioManager.ADJUST_RAISE,
                        0
                    )
                }
                audioManager.adjustVolume(
                    AudioManager.ADJUST_SAME,
                    AudioManager.FLAG_SHOW_UI
                )
            }

            "set" -> {
                if (value != null) {
                    val steps = (value / 10).coerceIn(0, 10)

                    // First go to minimum
                    repeat(15) {
                        audioManager.adjustVolume(
                            AudioManager.ADJUST_LOWER,
                            0
                        )
                    }

                    // Then raise step by step
                    repeat(steps) {
                        audioManager.adjustVolume(
                            AudioManager.ADJUST_RAISE,
                            0
                        )
                    }

                    audioManager.adjustVolume(
                        AudioManager.ADJUST_SAME,
                        AudioManager.FLAG_SHOW_UI
                    )
                }
            }
        }
        memoryRepository.logActivity(
            "VOLUME_CONTROL",
            "Volume $action",
            "User changed volume: $action"
        )
    }


    private var isFlashOn = false

    fun controlFlashlight(action: String?) {

        if (action.isNullOrBlank()) return

        val cameraManager =
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        try {

            var flashCameraId: String? = null

            for (id in cameraManager.cameraIdList) {

                val characteristics = cameraManager.getCameraCharacteristics(id)

                val flashAvailable =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)

                val lensFacing =
                    characteristics.get(CameraCharacteristics.LENS_FACING)

                if (flashAvailable == true &&
                    lensFacing == CameraCharacteristics.LENS_FACING_BACK
                ) {
                    flashCameraId = id
                    break
                }
            }

            if (flashCameraId == null) {
                Toast.makeText(context, "No flashlight found", Toast.LENGTH_SHORT).show()
                return
            }

            when (action.lowercase().trim()) {

                "on" -> {
                    cameraManager.setTorchMode(flashCameraId, true)
                    isFlashOn = true
                }

                "off" -> {
                    cameraManager.setTorchMode(flashCameraId, false)
                    isFlashOn = false
                }

                "toggle" -> {
                    isFlashOn = !isFlashOn
                    cameraManager.setTorchMode(flashCameraId, isFlashOn)
                }
            }

            memoryRepository.logActivity(
                "FLASHLIGHT",
                "Flashlight $action",
                "User turned flashlight $action"
            )

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Flashlight not available",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun sendLastYoutubeVideo(contactName: String) {

        val link = AssistantMemory.getYoutube()

        if (link == null) {
            Toast.makeText(context, "No video in memory", Toast.LENGTH_SHORT).show()
            return
        }

        val number = ContactResolver.getPhoneNumber(context, contactName)

        if (number == null) {
            Toast.makeText(context, "Contact not found", Toast.LENGTH_LONG).show()
            return
        }

        val uri = Uri.parse(
            "https://wa.me/${number.replace("\\s".toRegex(), "")}?text=${Uri.encode(link)}"
        )

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)

        memoryRepository.logActivity(
            "WHATSAPP_VIDEO",
            "Shared YouTube Video",
            "Sent video to $contactName"
        )
    }

    fun sendLastAssistantResponse(contactName: String) {

        val text = AssistantMemory.getText()

        if (text == null) {
            Toast.makeText(context, "Nothing to send", Toast.LENGTH_SHORT).show()
            return
        }

        sendWhatsAppMessage(contactName, text)
    }
    fun sendEmail(
        recipient: String,
        subject: String,
        body: String
    ) {

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            setPackage("com.google.android.gm")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {

            // fallback if Gmail not installed
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(fallbackIntent)
        }

        memoryRepository.logActivity(
            "EMAIL_SEND",
            "Prepared Email",
            "To: $recipient"
        )
    }
    fun sendLastResponseAsEmail(recipient: String, subject: String) {

        val text = com.example.llmosassistant.data.AssistantMemory.getText()

        if (text == null) {
            Toast.makeText(context, "Nothing to send", Toast.LENGTH_SHORT).show()
            return
        }

        sendEmail(recipient, subject, text)
    }
    fun generateStructuredPdf(
        topic: String,
        content: String
    ): File? {

        return PdfGenerator.generateStructuredPDF(
            context,
            topic,
            content
        )
    }
    fun openGeneratedPdf(file: File) {

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }
}
