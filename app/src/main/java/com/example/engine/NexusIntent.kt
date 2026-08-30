package com.example.engine

sealed class NexusIntent {
    data class OpenApp(
        val appName: String,
        val packageName: String? = null
    ) : NexusIntent()

    data class CallContact(
        val contactName: String,
        val phoneNumber: String? = null
    ) : NexusIntent()

    data class SendSms(
        val recipientName: String,
        val phoneNumber: String? = null,
        val messageText: String
    ) : NexusIntent()

    data class SearchYouTube(
        val query: String
    ) : NexusIntent()

    object PlayMedia : NexusIntent()
    object PauseMedia : NexusIntent()
    object NextMedia : NexusIntent()

    data class SetVolume(
        val levelPercent: Int, // 0 to 100
        val isRelative: Boolean = false, // true if delta (+10% or -10%)
        val delta: Int = 0
    ) : NexusIntent()

    data class SetBrightness(
        val levelPercent: Int
    ) : NexusIntent()

    data class ToggleFlashlight(
        val enabled: Boolean
    ) : NexusIntent()

    enum class SettingsType {
        WIFI,
        BLUETOOTH,
        SOUND,
        DISPLAY,
        BATTERY,
        NOTIFICATIONS,
        APPS,
        AIRPLANE,
        GENERAL
    }

    data class OpenSettings(
        val type: SettingsType
    ) : NexusIntent()

    object GetBatteryInfo : NexusIntent()
    object GetDeviceInfo : NexusIntent()

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val label: String = "Nexus Alarm"
    ) : NexusIntent()

    data class CreateReminder(
        val title: String,
        val delayMinutes: Int = 0,
        val timeLabel: String = ""
    ) : NexusIntent()

    data class Calculate(
        val expression: String
    ) : NexusIntent()

    data class WebSearch(
        val query: String
    ) : NexusIntent()

    data class Translate(
        val sourceText: String,
        val targetLanguage: String
    ) : NexusIntent()

    data class MultiAction(
        val actions: List<NexusIntent>
    ) : NexusIntent()

    data class GeneralAiQuery(
        val query: String
    ) : NexusIntent()
}

data class IntentExecutionResult(
    val success: Boolean,
    val responseText: String,
    val requiresConfirmation: Boolean = false,
    val confirmationIntent: NexusIntent? = null,
    val canUndo: Boolean = false,
    val undoType: String? = null,
    val undoPreviousValue: String? = null
)
