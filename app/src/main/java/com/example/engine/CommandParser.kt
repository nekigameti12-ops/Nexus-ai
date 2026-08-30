package com.example.engine

import java.util.Locale
import java.util.regex.Pattern

object CommandParser {

    /**
     * Parses raw user input into single or multi-action NexusIntents.
     */
    fun parse(input: String): NexusIntent {
        val cleanInput = input.trim()
        if (cleanInput.isEmpty()) {
            return NexusIntent.GeneralAiQuery("")
        }

        // Check for multi-action delimiters: "aur", "and", "then", "phir", "ke baad", comma
        val parts = splitCompoundCommand(cleanInput)
        if (parts.size > 1) {
            val subIntents = parts.map { parseSingleCommand(it) }
            // If all subIntents are recognized (or mostly non-general), treat as MultiAction
            val nonGeneralCount = subIntents.count { it !is NexusIntent.GeneralAiQuery }
            if (nonGeneralCount >= 1 && subIntents.size >= 2) {
                return NexusIntent.MultiAction(subIntents)
            }
        }

        return parseSingleCommand(cleanInput)
    }

    private fun splitCompoundCommand(text: String): List<String> {
        // Match separators: " aur ", " and ", " then ", " phir ", " ke baad ", " & "
        val delimiters = listOf(" aur ", " and ", " then ", " phir ", " ke baad ", " & ", " also ")
        var segments = listOf(text)

        for (delimiter in delimiters) {
            val newSegments = mutableListOf<String>()
            for (seg in segments) {
                if (seg.contains(delimiter, ignoreCase = true)) {
                    val split = seg.split(Regex(Pattern.quote(delimiter), RegexOption.IGNORE_CASE))
                    newSegments.addAll(split.map { it.trim() }.filter { it.isNotEmpty() })
                } else {
                    newSegments.add(seg)
                }
            }
            segments = newSegments
        }

        // If split on comma followed by a command verb
        val finalSegments = mutableListOf<String>()
        for (seg in segments) {
            if (seg.contains(",")) {
                val commaSplits = seg.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (commaSplits.size > 1 && commaSplits.any { isActionLikely(it) }) {
                    finalSegments.addAll(commaSplits)
                } else {
                    finalSegments.add(seg)
                }
            } else {
                finalSegments.add(seg)
            }
        }

        return finalSegments
    }

    private fun isActionLikely(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val actionKeywords = listOf("open", "kholo", "search", "call", "sms", "message", "volume", "brightness", "torch", "flashlight", "settings", "alarm", "play", "pause", "chalao", "band", "laga")
        return actionKeywords.any { lower.contains(it) }
    }

    fun parseSingleCommand(text: String): NexusIntent {
        val lower = text.lowercase(Locale.ROOT).trim()

        // 1. Flashlight / Torch
        if (matchesPattern(lower, listOf("flashlight on", "torch on", "torch jalao", "torch chalao", "turn on flashlight", "turn on torch", "flashlight chala do", "flashlight start", "batti jalao"))) {
            return NexusIntent.ToggleFlashlight(true)
        }
        if (matchesPattern(lower, listOf("flashlight off", "torch off", "torch band", "turn off flashlight", "turn off torch", "flashlight band karo", "flashlight stop", "batti bujhao"))) {
            return NexusIntent.ToggleFlashlight(false)
        }

        // 2. Volume Controls
        val volumePercentMatch = Regex("""(?:volume|awaaz|awaz)\s*(?:ko)?\s*(\d{1,3})\s*(?:percent|%|pe|par)?""").find(lower)
            ?: Regex("""(?:set volume|volume set)\s*(?:to)?\s*(\d{1,3})""").find(lower)
        if (volumePercentMatch != null) {
            val level = volumePercentMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 100) ?: 50
            return NexusIntent.SetVolume(levelPercent = level)
        }
        if (lower.contains("volume up") || lower.contains("volume badha") || lower.contains("awaaz badhao") || lower.contains("increase volume") || lower.contains("volume high")) {
            return NexusIntent.SetVolume(levelPercent = 0, isRelative = true, delta = 15)
        }
        if (lower.contains("volume down") || lower.contains("volume kam") || lower.contains("awaaz kam") || lower.contains("decrease volume") || lower.contains("volume low")) {
            return NexusIntent.SetVolume(levelPercent = 0, isRelative = true, delta = -15)
        }
        if (lower.contains("mute") || lower.contains("silent karo") || lower.contains("awaaz band")) {
            return NexusIntent.SetVolume(levelPercent = 0)
        }

        // 3. Brightness Controls
        val brightnessPercentMatch = Regex("""(?:brightness|roshni)\s*(?:ko)?\s*(\d{1,3})\s*(?:percent|%|pe|par)?""").find(lower)
            ?: Regex("""(?:set brightness)\s*(?:to)?\s*(\d{1,3})""").find(lower)
        if (brightnessPercentMatch != null) {
            val level = brightnessPercentMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 100) ?: 50
            return NexusIntent.SetBrightness(levelPercent = level)
        }
        if (lower.contains("brightness badha") || lower.contains("increase brightness") || lower.contains("brightness high") || lower.contains("full brightness")) {
            return NexusIntent.SetBrightness(levelPercent = 90)
        }
        if (lower.contains("brightness kam") || lower.contains("decrease brightness") || lower.contains("brightness low") || lower.contains("dim brightness")) {
            return NexusIntent.SetBrightness(levelPercent = 20)
        }

        // 4. Battery & Device Telemetry
        if (lower.contains("battery") || lower.contains("charge kitna") || lower.contains("charging status") || lower.contains("battery percent")) {
            return NexusIntent.GetBatteryInfo
        }
        if (lower.contains("device info") || lower.contains("phone model") || lower.contains("phone details") || lower.contains("system info") || lower.contains("storage info")) {
            return NexusIntent.GetDeviceInfo
        }

        // 5. YouTube Search & Playback
        if (lower.startsWith("youtube search") || lower.startsWith("search on youtube") || lower.contains("youtube par search") || lower.contains("youtube pe search") || lower.contains("youtube par") || lower.contains("youtube pe")) {
            val query = extractYouTubeQuery(text)
            if (query.isNotBlank()) {
                return NexusIntent.SearchYouTube(query)
            }
        }
        if (lower == "open youtube" || lower == "youtube kholo" || lower == "youtube chalao" || lower == "launch youtube" || lower == "youtube open karo") {
            return NexusIntent.OpenApp("YouTube", "com.google.android.youtube")
        }

        // 6. Media Controls
        if (lower == "play music" || lower == "music chalao" || lower == "gaana chalao" || lower == "resume media" || lower == "play song" || lower == "gaana bajao") {
            return NexusIntent.PlayMedia
        }
        if (lower == "pause music" || lower == "music roko" || lower == "gaana roko" || lower == "pause media" || lower == "stop music" || lower == "gaana band karo") {
            return NexusIntent.PauseMedia
        }
        if (lower == "next song" || lower == "agla gaana" || lower == "next track" || lower == "change song") {
            return NexusIntent.NextMedia
        }

        // 7. Phone Calls
        val callMatch = Regex("""^(?:call|phone lagao|phone karo|call karo|dial)\s+(?:to\s+)?(.+)""", RegexOption.IGNORE_CASE).find(text)
            ?: Regex("""^(.+)\s+(?:ko\s+)?(?:call karo|phone lagao|phone karo|dial karo)$""", RegexOption.IGNORE_CASE).find(text)
        if (callMatch != null) {
            val contact = callMatch.groupValues[1].replace(Regex("""\b(to|ko|please|karo|lagao)\b""", RegexOption.IGNORE_CASE), "").trim()
            if (contact.isNotBlank()) {
                return NexusIntent.CallContact(contactName = contact)
            }
        }

        // 8. SMS / Messaging
        val smsMatchWithBody = Regex("""(?:message|sms|bhejo|bolo)\s+(?:to\s+)?([a-zA-Z0-9\s]+?)\s+(?:ko\s+)?(?:bolo|message|that|saying)?\s*[:\-,]?\s*(.+)""", RegexOption.IGNORE_CASE).find(text)
        if (smsMatchWithBody != null) {
            val recipient = smsMatchWithBody.groupValues[1].trim()
            val msg = smsMatchWithBody.groupValues[2].trim()
            if (recipient.isNotBlank() && msg.isNotBlank() && !recipient.equals("me", ignoreCase = true)) {
                return NexusIntent.SendSms(recipientName = recipient, messageText = msg)
            }
        }
        val smsSimpleMatch = Regex("""^(?:send sms|send message|message karo|sms karo)\s+(?:to\s+)?(.+)""", RegexOption.IGNORE_CASE).find(text)
            ?: Regex("""^(.+)\s+ko\s+(?:message|sms)\s+karo""", RegexOption.IGNORE_CASE).find(text)
        if (smsSimpleMatch != null) {
            val recipient = smsSimpleMatch.groupValues[1].trim()
            if (recipient.isNotBlank()) {
                return NexusIntent.SendSms(recipientName = recipient, messageText = "Hello from Nexus AI")
            }
        }

        // 9. Settings Shortcuts
        if (lower.contains("wifi settings") || lower.contains("wi-fi settings") || lower.contains("wifi kholo") || lower.contains("open wifi")) {
            return NexusIntent.OpenSettings(NexusIntent.SettingsType.WIFI)
        }
        if (lower.contains("bluetooth settings") || lower.contains("bluetooth kholo") || lower.contains("open bluetooth")) {
            return NexusIntent.OpenSettings(NexusIntent.SettingsType.BLUETOOTH)
        }
        if (lower.contains("sound settings") || lower.contains("audio settings") || lower.contains("sound kholo")) {
            return NexusIntent.OpenSettings(NexusIntent.SettingsType.SOUND)
        }
        if (lower.contains("display settings") || lower.contains("screen settings") || lower.contains("display kholo")) {
            return NexusIntent.OpenSettings(NexusIntent.SettingsType.DISPLAY)
        }
        if (lower.contains("battery settings") || lower.contains("battery saver settings")) {
            return NexusIntent.OpenSettings(NexusIntent.SettingsType.BATTERY)
        }
        if (lower.contains("notification settings") || lower.contains("notifications settings")) {
            return NexusIntent.OpenSettings(NexusIntent.SettingsType.NOTIFICATIONS)
        }
        if (lower.contains("airplane mode") || lower.contains("flight mode")) {
            return NexusIntent.OpenSettings(NexusIntent.SettingsType.AIRPLANE)
        }
        if (lower == "open settings" || lower == "settings kholo" || lower == "settings open karo" || lower == "phone settings") {
            return NexusIntent.OpenSettings(NexusIntent.SettingsType.GENERAL)
        }

        // 10. Alarm & Timers & Reminders
        val alarmMatch = Regex("""(?:alarm|uthana)\s*(?:laga do|set karo|lagao|at)?\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm|baje)?""", RegexOption.IGNORE_CASE).find(lower)
            ?: Regex("""(?:set alarm for|set alarm at)\s*(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""", RegexOption.IGNORE_CASE).find(lower)
        if (alarmMatch != null) {
            var hour = alarmMatch.groupValues[1].toIntOrNull() ?: 7
            val minute = alarmMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            val ampm = alarmMatch.groupValues.getOrNull(3)?.lowercase(Locale.ROOT) ?: ""
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            return NexusIntent.SetAlarm(hour = hour, minute = minute, label = "Nexus Wake Alarm")
        }

        val reminderMatch = Regex("""(\d+)\s*(?:minute|min|ghante|hours?)\s*(?:baad|after|mein)\s*(?:yaad dilana|remind me|reminder)?\s*(?:to\s+|ki\s+)?(.*)""", RegexOption.IGNORE_CASE).find(lower)
        if (reminderMatch != null) {
            val minutes = reminderMatch.groupValues[1].toIntOrNull() ?: 10
            val title = reminderMatch.groupValues.getOrNull(2)?.trim()?.ifEmpty { "Nexus Reminder" } ?: "Nexus Reminder"
            return NexusIntent.CreateReminder(title = title, delayMinutes = minutes, timeLabel = "$minutes minutes")
        }

        // 11. Instant Math Calculations
        val mathMatch = tryExtractMath(lower)
        if (mathMatch != null) {
            return NexusIntent.Calculate(mathMatch)
        }

        // 12. App Launchers
        val appLaunchIntent = tryMatchAppLaunch(lower, text)
        if (appLaunchIntent != null) {
            return appLaunchIntent
        }

        // 13. Web Search / News / Weather
        if (lower.startsWith("search ") || lower.startsWith("google ") || lower.contains("weather") || lower.contains("mausam") || lower.contains("latest news") || lower.contains("khabar")) {
            val query = text.replace(Regex("""^(search|google|find|search for)\s+""", RegexOption.IGNORE_CASE), "").trim()
            return NexusIntent.WebSearch(query)
        }

        // 14. Translation
        if (lower.contains("translate") || lower.contains("anuvad") || lower.contains("mein translate karo")) {
            return NexusIntent.Translate(sourceText = text, targetLanguage = if (lower.contains("hindi")) "Hindi" else "English")
        }

        // Default to General AI query (Gemini knowledge / reasoning / conversation)
        return NexusIntent.GeneralAiQuery(text)
    }

    private fun extractYouTubeQuery(text: String): String {
        return text
            .replace(Regex("""(?i)^(youtube par|youtube pe|on youtube|in youtube|search on youtube|youtube search)\s*"""), "")
            .replace(Regex("""(?i)\s*(search karo|search kar|chalao|play karo|dikhao|videos|search)$"""), "")
            .trim()
    }

    private fun tryMatchAppLaunch(lower: String, original: String): NexusIntent? {
        val launchKeywords = listOf("open", "kholo", "chalao", "start", "launch", "open karo")
        val isLaunch = launchKeywords.any { lower.contains(it) }

        val appsMap = mapOf(
            "whatsapp" to Pair("WhatsApp", "com.whatsapp"),
            "instagram" to Pair("Instagram", "com.instagram.android"),
            "insta" to Pair("Instagram", "com.instagram.android"),
            "chrome" to Pair("Chrome", "com.android.chrome"),
            "browser" to Pair("Browser", "com.android.chrome"),
            "camera" to Pair("Camera", null),
            "gallery" to Pair("Gallery", null),
            "photos" to Pair("Google Photos", "com.google.android.apps.photos"),
            "calculator" to Pair("Calculator", null),
            "clock" to Pair("Clock", null),
            "spotify" to Pair("Spotify", "com.spotify.music"),
            "maps" to Pair("Google Maps", "com.google.android.apps.maps"),
            "gmail" to Pair("Gmail", "com.google.android.gm"),
            "mail" to Pair("Gmail", "com.google.android.gm"),
            "play store" to Pair("Google Play Store", "com.android.vending"),
            "telegram" to Pair("Telegram", "org.telegram.messenger"),
            "contacts" to Pair("Contacts", null),
            "dialer" to Pair("Phone Dialer", null),
            "phone" to Pair("Phone", null)
        )

        for ((key, pair) in appsMap) {
            if (lower.contains(key)) {
                if (isLaunch || lower.split(" ").any { it == key }) {
                    return NexusIntent.OpenApp(pair.first, pair.second)
                }
            }
        }

        if (isLaunch) {
            // Extract custom app name: "Open Flipkart", "Swiggy kholo"
            val words = original.split(" ")
            val filtered = words.filterNot { w ->
                val lw = w.lowercase(Locale.ROOT)
                launchKeywords.any { lw.contains(it) } || listOf("app", "the", "ko", "kar", "do", "please", "aur").contains(lw)
            }
            if (filtered.isNotEmpty()) {
                val candidateApp = filtered.joinToString(" ").trim()
                if (candidateApp.length in 2..30) {
                    return NexusIntent.OpenApp(candidateApp)
                }
            }
        }

        return null
    }

    private fun tryExtractMath(lower: String): String? {
        // e.g. "100 ka 25 percent", "calculate 45 * 12", "what is 20 + 35"
        val percentMatch = Regex("""(\d+(?:\.\d+)?)\s*(?:ka|of)\s*(\d+(?:\.\d+)?)\s*(?:percent|%|pratishat)""").find(lower)
        if (percentMatch != null) {
            val base = percentMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val pct = percentMatch.groupValues[2].toDoubleOrNull() ?: 0.0
            return "$pct% of $base = ${(base * pct) / 100.0}"
        }

        val basicArithmetic = Regex("""(?:calculate|what is|kitna hoga|solve)?\s*(\d+(?:\.\d+)?)\s*([\+\-\*\/xX÷])\s*(\d+(?:\.\d+)?)""").find(lower)
        if (basicArithmetic != null) {
            val a = basicArithmetic.groupValues[1].toDoubleOrNull() ?: return null
            val op = basicArithmetic.groupValues[2]
            val b = basicArithmetic.groupValues[3].toDoubleOrNull() ?: return null
            val result = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*", "x", "X" -> a * b
                "/", "÷" -> if (b != 0.0) a / b else "Division by zero undefined"
                else -> null
            }
            return "$a $op $b = $result"
        }
        return null
    }

    private fun matchesPattern(text: String, patterns: List<String>): Boolean {
        return patterns.any { text.contains(it, ignoreCase = true) }
    }
}
