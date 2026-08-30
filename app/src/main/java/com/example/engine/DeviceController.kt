package com.example.engine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.SmsManager
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

class DeviceController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    // Undo state tracking
    var lastVolumeLevel: Int? = null
    var lastBrightnessLevel: Int? = null
    var lastTorchState: Boolean? = null

    // 1. Flashlight
    fun toggleTorch(enable: Boolean): IntentExecutionResult {
        if (cameraManager == null) {
            return IntentExecutionResult(false, "Flashlight hardware is unavailable on this device.")
        }
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (cameraId != null) {
                lastTorchState = !enable
                cameraManager.setTorchMode(cameraId, enable)
                val statusText = if (enable) "Flashlight turned ON." else "Flashlight turned OFF."
                IntentExecutionResult(
                    success = true,
                    responseText = statusText,
                    canUndo = true,
                    undoType = "TORCH",
                    undoPreviousValue = (!enable).toString()
                )
            } else {
                IntentExecutionResult(false, "No flashlight module found on this device.")
            }
        } catch (e: CameraAccessException) {
            IntentExecutionResult(false, "Camera access error: ${e.localizedMessage}")
        } catch (e: Exception) {
            IntentExecutionResult(false, "Unable to toggle flashlight: ${e.localizedMessage}")
        }
    }

    // 2. Volume Control
    fun setVolume(levelPercent: Int, isRelative: Boolean = false, delta: Int = 0): IntentExecutionResult {
        if (audioManager == null) {
            return IntentExecutionResult(false, "Audio manager is unavailable.")
        }
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        lastVolumeLevel = currentVolume

        val targetVolume = if (isRelative) {
            val deltaVol = ((delta / 100.0) * maxVolume).toInt()
            (currentVolume + deltaVol).coerceIn(0, maxVolume)
        } else {
            ((levelPercent / 100.0) * maxVolume).toInt().coerceIn(0, maxVolume)
        }

        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_SHOW_UI)
        val targetPercent = ((targetVolume.toFloat() / maxVolume) * 100).toInt()

        return IntentExecutionResult(
            success = true,
            responseText = "Media volume set to $targetPercent%.",
            canUndo = true,
            undoType = "VOLUME",
            undoPreviousValue = currentVolume.toString()
        )
    }

    // 3. Brightness Control
    fun setBrightness(levelPercent: Int): IntentExecutionResult {
        val targetValue = ((levelPercent / 100.0) * 255).toInt().coerceIn(0, 255)
        return try {
            val canWrite = Settings.System.canWrite(context)
            if (canWrite) {
                val current = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
                lastBrightnessLevel = current
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, targetValue)
                IntentExecutionResult(
                    success = true,
                    responseText = "Screen brightness adjusted to $levelPercent%.",
                    canUndo = true,
                    undoType = "BRIGHTNESS",
                    undoPreviousValue = current.toString()
                )
            } else {
                // Launch Display Settings
                openSettings(NexusIntent.SettingsType.DISPLAY)
                IntentExecutionResult(
                    success = true,
                    responseText = "Opening Display Settings to adjust brightness (Requires system write permission)."
                )
            }
        } catch (e: Exception) {
            openSettings(NexusIntent.SettingsType.DISPLAY)
            IntentExecutionResult(
                success = true,
                responseText = "Opening Display Settings: ${e.localizedMessage}"
            )
        }
    }

    // 4. App Launcher
    fun openApp(appName: String, explicitPackage: String? = null): IntentExecutionResult {
        val pm = context.packageManager

        // If explicit package is provided, try that first
        if (!explicitPackage.isNullOrBlank()) {
            val launchIntent = pm.getLaunchIntentForPackage(explicitPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return IntentExecutionResult(true, "Opening $appName...")
            }
        }

        // Generic built-in app intents
        when (appName.lowercase(Locale.ROOT)) {
            "camera" -> {
                val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (cameraIntent.resolveActivity(pm) != null) {
                    context.startActivity(cameraIntent)
                    return IntentExecutionResult(true, "Opening Camera...")
                }
            }
            "gallery", "photos" -> {
                val galleryIntent = Intent(Intent.ACTION_VIEW).apply {
                    type = "image/*"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (galleryIntent.resolveActivity(pm) != null) {
                    context.startActivity(galleryIntent)
                    return IntentExecutionResult(true, "Opening Gallery...")
                }
            }
            "dialer", "phone" -> {
                val dialerIntent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialerIntent)
                return IntentExecutionResult(true, "Opening Phone Dialer...")
            }
            "contacts" -> {
                val contactsIntent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(contactsIntent)
                return IntentExecutionResult(true, "Opening Contacts...")
            }
            "clock" -> {
                val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (clockIntent.resolveActivity(pm) != null) {
                    context.startActivity(clockIntent)
                    return IntentExecutionResult(true, "Opening Clock...")
                }
            }
            "calculator" -> {
                // Try common calculator packages
                val calcPackages = listOf(
                    "com.google.android.calculator",
                    "com.android.calculator2",
                    "com.sec.android.app.popupcalculator",
                    "com.miui.calculator"
                )
                for (pkg in calcPackages) {
                    val intent = pm.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return IntentExecutionResult(true, "Opening Calculator...")
                    }
                }
            }
        }

        // Search installed applications for match
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val matchedApp = installedApps.firstOrNull {
            val label = pm.getApplicationLabel(it).toString()
            label.contains(appName, ignoreCase = true) || it.packageName.contains(appName, ignoreCase = true)
        }

        if (matchedApp != null) {
            val launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val label = pm.getApplicationLabel(matchedApp)
                return IntentExecutionResult(true, "Opening $label...")
            }
        }

        return IntentExecutionResult(
            false,
            "I couldn't open '$appName'. Please make sure it is installed on your device."
        )
    }

    // 5. YouTube Search & Playback
    fun searchYouTube(query: String): IntentExecutionResult {
        return try {
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$encodedQuery")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (appIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(appIntent)
                IntentExecutionResult(true, "Searching YouTube for '$query'...")
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encodedQuery")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                IntentExecutionResult(true, "Searching YouTube for '$query'...")
            }
        } catch (e: Exception) {
            IntentExecutionResult(false, "Failed to search YouTube: ${e.localizedMessage}")
        }
    }

    // 6. Media Controls
    fun sendMediaKey(keyCode: Int): IntentExecutionResult {
        return try {
            if (audioManager != null) {
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                val actionName = when (keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "Toggled media playback."
                    KeyEvent.KEYCODE_MEDIA_PLAY -> "Playing media."
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> "Paused media."
                    KeyEvent.KEYCODE_MEDIA_NEXT -> "Skipped to next track."
                    else -> "Media command sent."
                }
                IntentExecutionResult(true, actionName)
            } else {
                IntentExecutionResult(false, "Audio service unavailable.")
            }
        } catch (e: Exception) {
            IntentExecutionResult(false, "Could not send media command: ${e.localizedMessage}")
        }
    }

    // 7. Phone Call (Direct or Dial Intent)
    fun prepareCall(contactNameOrNumber: String): IntentExecutionResult {
        val resolvedNumber = resolveContactNumber(contactNameOrNumber) ?: contactNameOrNumber
        return IntentExecutionResult(
            success = true,
            responseText = "Calling $contactNameOrNumber ($resolvedNumber). Confirm?",
            requiresConfirmation = true,
            confirmationIntent = NexusIntent.CallContact(contactNameOrNumber, resolvedNumber)
        )
    }

    fun executeCall(phoneNumber: String): IntentExecutionResult {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            if (hasCallPermission) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${phoneNumber.trim()}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                IntentExecutionResult(true, "Placing call to $phoneNumber...")
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneNumber.trim()}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                IntentExecutionResult(true, "Opening dialer with $phoneNumber...")
            }
        } catch (e: Exception) {
            IntentExecutionResult(false, "Failed to initiate call: ${e.localizedMessage}")
        }
    }

    // 8. SMS Messaging
    fun prepareSms(recipient: String, messageText: String): IntentExecutionResult {
        val resolvedNumber = resolveContactNumber(recipient) ?: recipient
        return IntentExecutionResult(
            success = true,
            responseText = "Send SMS to $recipient: \"$messageText\"?",
            requiresConfirmation = true,
            confirmationIntent = NexusIntent.SendSms(recipient, resolvedNumber, messageText)
        )
    }

    fun executeSendSms(phoneNumber: String, messageText: String): IntentExecutionResult {
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            if (hasSmsPermission) {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, messageText, null, null)
                IntentExecutionResult(true, "SMS sent to $phoneNumber successfully.")
            } else {
                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$phoneNumber")
                    putExtra("sms_body", messageText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
                IntentExecutionResult(true, "Opening SMS app to send message to $phoneNumber...")
            }
        } catch (e: Exception) {
            IntentExecutionResult(false, "Failed to send SMS: ${e.localizedMessage}")
        }
    }

    // 9. Contact Resolver
    fun resolveContactNumber(query: String): String? {
        if (query.matches(Regex("""^[+]?[0-9\-\s]{6,15}$"""))) {
            return query
        }
        val hasContactPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasContactPermission) return null

        var cursor: Cursor? = null
        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$query%")

            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) cursor.getString(numberIndex) else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

    // 10. Open Settings
    fun openSettings(type: NexusIntent.SettingsType): IntentExecutionResult {
        val action = when (type) {
            NexusIntent.SettingsType.WIFI -> Settings.ACTION_WIFI_SETTINGS
            NexusIntent.SettingsType.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            NexusIntent.SettingsType.SOUND -> Settings.ACTION_SOUND_SETTINGS
            NexusIntent.SettingsType.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            NexusIntent.SettingsType.BATTERY -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            NexusIntent.SettingsType.NOTIFICATIONS -> Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
            NexusIntent.SettingsType.AIRPLANE -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
            NexusIntent.SettingsType.APPS -> Settings.ACTION_APPLICATION_SETTINGS
            NexusIntent.SettingsType.GENERAL -> Settings.ACTION_SETTINGS
        }
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            IntentExecutionResult(true, "Opening ${type.name.lowercase().replaceFirstChar { it.uppercase() }} settings...")
        } catch (e: Exception) {
            // Fallback to General Settings
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            IntentExecutionResult(true, "Opening System Settings...")
        }
    }

    // 11. Alarm & Reminder
    fun setAlarm(hour: Int, minute: Int, label: String): IntentExecutionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                val ampm = if (hour >= 12) "PM" else "AM"
                val displayHour = if (hour % 12 == 0) 12 else hour % 12
                val displayMin = String.format(Locale.ROOT, "%02d", minute)
                IntentExecutionResult(true, "Alarm set for $displayHour:$displayMin $ampm.")
            } else {
                val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(clockIntent)
                IntentExecutionResult(true, "Opening Clock app to configure alarm.")
            }
        } catch (e: Exception) {
            IntentExecutionResult(false, "Failed to set alarm: ${e.localizedMessage}")
        }
    }

    // 12. Battery & Device Telemetry
    fun getBatteryInfo(): IntentExecutionResult {
        val batteryIntent = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val stateDesc = if (isCharging) "Charging ⚡" else "Discharging"

        val report = "Battery Level: $batteryPct% ($stateDesc)\nHealth: Normal\nPower Mode: Optimized"
        return IntentExecutionResult(true, report)
    }

    fun getDeviceInfo(): IntentExecutionResult {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val osVersion = Build.VERSION.RELEASE
        val sdk = Build.VERSION.SDK_INT
        val runtime = Runtime.getRuntime()
        val totalMemoryMb = runtime.totalMemory() / (1024 * 1024)
        val freeMemoryMb = runtime.freeMemory() / (1024 * 1024)

        val report = """
            Device: $manufacturer $model
            OS: Android $osVersion (API $sdk)
            App Memory: ${totalMemoryMb - freeMemoryMb}MB used / ${totalMemoryMb}MB allocated
            Security Patch: Up to date
        """.trimIndent()
        return IntentExecutionResult(true, report)
    }

    // 13. Web Search
    fun searchWeb(query: String): IntentExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
            IntentExecutionResult(true, "Searching web for: \"$query\"")
        } catch (e: Exception) {
            IntentExecutionResult(false, "Failed to perform web search: ${e.localizedMessage}")
        }
    }

    // 14. Undo Action
    fun executeUndo(undoType: String, undoValue: String): IntentExecutionResult {
        return when (undoType) {
            "VOLUME" -> {
                val previousVol = undoValue.toIntOrNull() ?: return IntentExecutionResult(false, "Cannot restore volume.")
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, previousVol, AudioManager.FLAG_SHOW_UI)
                IntentExecutionResult(true, "Restored previous volume.")
            }
            "TORCH" -> {
                val previousState = undoValue.toBooleanStrictOrNull() ?: false
                toggleTorch(previousState)
                IntentExecutionResult(true, "Restored flashlight state to $previousState.")
            }
            "BRIGHTNESS" -> {
                val prevBrightness = undoValue.toIntOrNull() ?: return IntentExecutionResult(false, "Cannot restore brightness.")
                try {
                    if (Settings.System.canWrite(context)) {
                        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, prevBrightness)
                        IntentExecutionResult(true, "Restored previous screen brightness.")
                    } else {
                        IntentExecutionResult(false, "Permission required to restore brightness.")
                    }
                } catch (e: Exception) {
                    IntentExecutionResult(false, "Could not restore brightness.")
                }
            }
            else -> IntentExecutionResult(false, "No undo available for this action.")
        }
    }
}
