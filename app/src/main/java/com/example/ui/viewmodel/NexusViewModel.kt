package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.NexusAiEngine
import com.example.data.local.NexusDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.CommandHistory
import com.example.data.model.MessageSender
import com.example.data.model.MessageStatus
import com.example.data.repository.NexusSettings
import com.example.data.repository.UserPreferences
import com.example.engine.CommandParser
import com.example.engine.DeviceController
import com.example.engine.IntentExecutionResult
import com.example.engine.NexusIntent
import com.example.service.NexusAssistantService
import com.example.ui.components.OrbState
import com.example.ui.components.UndoState
import com.example.voice.DoubleClapDetector
import com.example.voice.SpeechRecognizerManager
import com.example.voice.SpeechState
import com.example.voice.TextToSpeechEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NexusTab {
    HOME,
    CHAT,
    PERMISSIONS,
    PRIVACY,
    SETTINGS
}

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val manifestPermission: String?,
    val isGranted: Boolean,
    val isCritical: Boolean = false
)

class NexusViewModel(application: Application) : AndroidViewModel(application) {

    private val db = NexusDatabase.getInstance(application)
    private val chatDao = db.chatDao()
    private val commandDao = db.commandDao()
    private val reminderDao = db.reminderDao()

    val userPrefs = UserPreferences(application)
    val settings: StateFlow<NexusSettings> = userPrefs.settings

    val deviceController = DeviceController(application)
    val aiEngine = NexusAiEngine(application)
    val ttsEngine = TextToSpeechEngine(application)

    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    // Navigation & UI States
    private val _currentTab = MutableStateFlow(NexusTab.HOME)
    val currentTab: StateFlow<NexusTab> = _currentTab.asStateFlow()

    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<NexusIntent?>(null)
    val pendingConfirmation: StateFlow<NexusIntent?> = _pendingConfirmation.asStateFlow()

    private val _undoState = MutableStateFlow(UndoState())
    val undoState: StateFlow<UndoState> = _undoState.asStateFlow()

    private val _permissionsList = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissionsList: StateFlow<List<PermissionItem>> = _permissionsList.asStateFlow()

    // Room Database Streams
    val chatMessages: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentCommands: StateFlow<List<CommandHistory>> = commandDao.getRecentCommands(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Speech Recognizer Manager
    lateinit var speechRecognizerManager: SpeechRecognizerManager
    private var clapDetector: DoubleClapDetector? = null
    private var undoTimerJob: Job? = null

    init {
        speechRecognizerManager = SpeechRecognizerManager(
            context = application,
            onResult = { recognizedText ->
                processUserInput(recognizedText, isVoice = true)
            },
            onErrorMsg = { error ->
                _orbState.value = OrbState.ERROR
                viewModelScope.launch {
                    delay(2000)
                    _orbState.value = OrbState.IDLE
                }
            }
        )

        // Observe TTS Speaking state to update Orb
        viewModelScope.launch {
            ttsEngine.isSpeaking.collect { isSpeaking ->
                if (isSpeaking && _orbState.value != OrbState.LISTENING) {
                    _orbState.value = OrbState.SPEAKING
                } else if (!isSpeaking && _orbState.value == OrbState.SPEAKING) {
                    _orbState.value = OrbState.IDLE
                }
            }
        }

        // Observe Speech state to update Orb
        viewModelScope.launch {
            speechRecognizerManager.speechState.collect { speechState ->
                when (speechState) {
                    SpeechState.LISTENING -> _orbState.value = OrbState.LISTENING
                    SpeechState.PROCESSING -> _orbState.value = OrbState.THINKING
                    SpeechState.ERROR -> _orbState.value = OrbState.ERROR
                    SpeechState.IDLE -> {
                        if (!ttsEngine.isSpeaking.value && _orbState.value != OrbState.THINKING) {
                            _orbState.value = OrbState.IDLE
                        }
                    }
                }
            }
        }

        // Initialize Double-Clap Detector for in-app mode
        clapDetector = DoubleClapDetector(application) {
            handleDoubleClapWake()
        }

        refreshPermissions()
    }

    fun selectTab(tab: NexusTab) {
        _currentTab.value = tab
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun triggerHaptic(strong: Boolean = false) {
        if (!settings.value.hapticsEnabled || vibrator == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (strong) {
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    VibrationEffect.createOneShot(35, 100)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (strong) 80 else 35)
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }

    fun startListening() {
        triggerHaptic()
        val lang = settings.value.preferredLanguage
        speechRecognizerManager.startListening(lang)
    }

    fun stopListening() {
        speechRecognizerManager.stopListening()
    }

    fun stopSpeech() {
        ttsEngine.stop()
        _orbState.value = OrbState.IDLE
    }

    private fun handleDoubleClapWake() {
        triggerHaptic(true)
        val greeting = "Nexus online. How can I assist you?"
        speakOut(greeting)
        startListening()
    }

    fun processUserInput(rawInput: String, isVoice: Boolean = false) {
        val text = rawInput.trim()
        if (text.isEmpty()) return

        _inputText.value = ""
        triggerHaptic()

        viewModelScope.launch {
            // 1. Save User Message
            val userMsg = ChatMessage(
                sender = MessageSender.USER,
                text = text,
                isVoice = isVoice
            )
            chatDao.insertMessage(userMsg)

            // Check for Wake-Word only inputs (e.g. "Nexus", "Hey Nexus")
            val lower = text.lowercase()
            if (lower == "nexus" || lower == "hey nexus" || lower == "ok nexus" || lower == "hello nexus") {
                val reply = "Yes? I am listening."
                val aiMsg = ChatMessage(
                    sender = MessageSender.NEXUS,
                    text = reply,
                    status = MessageStatus.SUCCESS
                )
                chatDao.insertMessage(aiMsg)
                speakOut(reply)
                startListening()
                return@launch
            }

            // 2. Parse Intent
            _orbState.value = OrbState.THINKING
            val parsedIntent = CommandParser.parse(text)

            // 3. Execute Intent or Route to AI
            executeOrRouteIntent(parsedIntent, text)
        }
    }

    private suspend fun executeOrRouteIntent(intent: NexusIntent, originalQuery: String) {
        when (intent) {
            is NexusIntent.MultiAction -> {
                executeMultiAction(intent.actions, originalQuery)
            }

            is NexusIntent.CallContact -> {
                val prepResult = deviceController.prepareCall(intent.contactName)
                if (prepResult.requiresConfirmation) {
                    _pendingConfirmation.value = prepResult.confirmationIntent
                    val pendingMsg = ChatMessage(
                        sender = MessageSender.NEXUS,
                        text = prepResult.responseText,
                        status = MessageStatus.IDLE
                    )
                    chatDao.insertMessage(pendingMsg)
                    speakOut("Calling ${intent.contactName}. Please confirm.")
                } else {
                    executeDeviceIntentDirect(intent, originalQuery)
                }
            }

            is NexusIntent.SendSms -> {
                val prepResult = deviceController.prepareSms(intent.recipientName, intent.messageText)
                if (prepResult.requiresConfirmation) {
                    _pendingConfirmation.value = prepResult.confirmationIntent
                    val pendingMsg = ChatMessage(
                        sender = MessageSender.NEXUS,
                        text = prepResult.responseText,
                        status = MessageStatus.IDLE
                    )
                    chatDao.insertMessage(pendingMsg)
                    speakOut("Send SMS to ${intent.recipientName}?")
                } else {
                    executeDeviceIntentDirect(intent, originalQuery)
                }
            }

            is NexusIntent.GeneralAiQuery -> {
                handleAiQuery(originalQuery)
            }

            is NexusIntent.Calculate -> {
                val reply = "Calculated: ${intent.expression}"
                recordExecutedAction(originalQuery, "CALCULATE", reply, true)
                val aiMsg = ChatMessage(
                    sender = MessageSender.NEXUS,
                    text = reply,
                    status = MessageStatus.SUCCESS
                )
                chatDao.insertMessage(aiMsg)
                speakOut(reply)
                _orbState.value = OrbState.IDLE
            }

            is NexusIntent.Translate -> {
                handleAiQuery("Translate to ${intent.targetLanguage}: ${intent.sourceText}")
            }

            else -> {
                executeDeviceIntentDirect(intent, originalQuery)
            }
        }
    }

    private suspend fun executeMultiAction(actions: List<NexusIntent>, originalQuery: String) {
        val results = mutableListOf<String>()
        var overallSuccess = true

        for (action in actions) {
            val result = executeSingleDeviceIntent(action)
            results.add(result.responseText)
            if (!result.success) overallSuccess = false
            delay(250)
        }

        val combinedReport = results.joinToString("\n• ", prefix = "Executed multi-step commands:\n• ")
        recordExecutedAction(originalQuery, "MULTI_ACTION", combinedReport, overallSuccess)

        val aiMsg = ChatMessage(
            sender = MessageSender.NEXUS,
            text = combinedReport,
            status = if (overallSuccess) MessageStatus.SUCCESS else MessageStatus.ERROR
        )
        chatDao.insertMessage(aiMsg)
        speakOut(results.firstOrNull() ?: "Actions completed.")
        _orbState.value = OrbState.IDLE
    }

    private suspend fun executeDeviceIntentDirect(intent: NexusIntent, originalQuery: String) {
        val result = executeSingleDeviceIntent(intent)
        recordExecutedAction(
            originalQuery,
            intent.javaClass.simpleName,
            result.responseText,
            result.success,
            result.canUndo,
            result.undoType,
            result.undoPreviousValue
        )

        if (result.canUndo && result.undoType != null && result.undoPreviousValue != null) {
            showUndo(result.responseText, result.undoType, result.undoPreviousValue)
        }

        val aiMsg = ChatMessage(
            sender = MessageSender.NEXUS,
            text = result.responseText,
            status = if (result.success) MessageStatus.SUCCESS else MessageStatus.ERROR
        )
        chatDao.insertMessage(aiMsg)
        speakOut(result.responseText)
        _orbState.value = OrbState.IDLE
    }

    private fun executeSingleDeviceIntent(intent: NexusIntent): IntentExecutionResult {
        return when (intent) {
            is NexusIntent.OpenApp -> deviceController.openApp(intent.appName, intent.packageName)
            is NexusIntent.SearchYouTube -> deviceController.searchYouTube(intent.query)
            is NexusIntent.PlayMedia -> deviceController.sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY)
            is NexusIntent.PauseMedia -> deviceController.sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PAUSE)
            is NexusIntent.NextMedia -> deviceController.sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            is NexusIntent.ToggleFlashlight -> deviceController.toggleTorch(intent.enabled)
            is NexusIntent.SetVolume -> deviceController.setVolume(intent.levelPercent, intent.isRelative, intent.delta)
            is NexusIntent.SetBrightness -> deviceController.setBrightness(intent.levelPercent)
            is NexusIntent.OpenSettings -> deviceController.openSettings(intent.type)
            is NexusIntent.GetBatteryInfo -> deviceController.getBatteryInfo()
            is NexusIntent.GetDeviceInfo -> deviceController.getDeviceInfo()
            is NexusIntent.SetAlarm -> deviceController.setAlarm(intent.hour, intent.minute, intent.label)
            is NexusIntent.CreateReminder -> {
                deviceController.setAlarm(
                    hour = (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)),
                    minute = (java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE) + intent.delayMinutes) % 60,
                    label = intent.title
                )
                IntentExecutionResult(true, "Reminder set for '${intent.title}' in ${intent.timeLabel}.")
            }
            is NexusIntent.WebSearch -> deviceController.searchWeb(intent.query)
            else -> IntentExecutionResult(false, "Unsupported direct device intent.")
        }
    }

    private suspend fun handleAiQuery(query: String) {
        val historyTurns = chatMessages.value.takeLast(6).map {
            Pair(if (it.sender == MessageSender.USER) "USER" else "MODEL", it.text)
        }

        val aiResponse = aiEngine.query(query, historyTurns)
        val aiMsg = ChatMessage(
            sender = MessageSender.NEXUS,
            text = aiResponse,
            status = MessageStatus.SUCCESS
        )
        chatDao.insertMessage(aiMsg)
        speakOut(aiResponse)
        _orbState.value = OrbState.IDLE
    }

    fun confirmPendingAction() {
        val intent = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        triggerHaptic(true)

        viewModelScope.launch {
            when (intent) {
                is NexusIntent.CallContact -> {
                    val number = intent.phoneNumber ?: intent.contactName
                    val result = deviceController.executeCall(number)
                    val msg = ChatMessage(
                        sender = MessageSender.NEXUS,
                        text = result.responseText,
                        status = if (result.success) MessageStatus.SUCCESS else MessageStatus.ERROR
                    )
                    chatDao.insertMessage(msg)
                    speakOut(result.responseText)
                }
                is NexusIntent.SendSms -> {
                    val number = intent.phoneNumber ?: intent.recipientName
                    val result = deviceController.executeSendSms(number, intent.messageText)
                    val msg = ChatMessage(
                        sender = MessageSender.NEXUS,
                        text = result.responseText,
                        status = if (result.success) MessageStatus.SUCCESS else MessageStatus.ERROR
                    )
                    chatDao.insertMessage(msg)
                    speakOut(result.responseText)
                }
                else -> {
                    executeDeviceIntentDirect(intent, "Confirmed Action")
                }
            }
        }
    }

    fun cancelPendingAction() {
        _pendingConfirmation.value = null
        viewModelScope.launch {
            val msg = ChatMessage(
                sender = MessageSender.NEXUS,
                text = "Action cancelled by user.",
                status = MessageStatus.IDLE
            )
            chatDao.insertMessage(msg)
            speakOut("Action cancelled.")
        }
    }

    private fun showUndo(message: String, undoType: String, undoValue: String) {
        undoTimerJob?.cancel()
        _undoState.value = UndoState(
            visible = true,
            message = message,
            undoType = undoType,
            undoValue = undoValue
        )
        undoTimerJob = viewModelScope.launch {
            delay(6000)
            _undoState.value = UndoState(visible = false)
        }
    }

    fun performUndo() {
        val currentUndo = _undoState.value
        if (!currentUndo.visible || currentUndo.undoType.isEmpty()) return
        triggerHaptic()

        val result = deviceController.executeUndo(currentUndo.undoType, currentUndo.undoValue)
        _undoState.value = UndoState(visible = false)

        viewModelScope.launch {
            val msg = ChatMessage(
                sender = MessageSender.NEXUS,
                text = result.responseText,
                status = if (result.success) MessageStatus.SUCCESS else MessageStatus.ERROR
            )
            chatDao.insertMessage(msg)
            speakOut(result.responseText)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
            Toast.makeText(getApplication(), "Chat history cleared", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
            commandDao.clearHistory()
            reminderDao.clearAllReminders()
            Toast.makeText(getApplication(), "All local data deleted securely", Toast.LENGTH_SHORT).show()
        }
    }

    fun regenerateLastResponse() {
        val lastUserMsg = chatMessages.value.lastOrNull { it.sender == MessageSender.USER }
        if (lastUserMsg != null) {
            processUserInput(lastUserMsg.text)
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Nexus AI", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(getApplication(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun speakOut(text: String) {
        if (!settings.value.autoSpeakResponses) return
        val currentSettings = settings.value
        ttsEngine.speak(
            text = text,
            pitch = currentSettings.speechPitch,
            rate = currentSettings.speechRate,
            language = currentSettings.preferredLanguage
        )
    }

    private suspend fun recordExecutedAction(
        query: String,
        intentType: String,
        desc: String,
        success: Boolean,
        canUndo: Boolean = false,
        undoType: String? = null,
        undoVal: String? = null
    ) {
        val history = CommandHistory(
            commandText = query,
            intentType = intentType,
            description = desc,
            wasSuccessful = success,
            canUndo = canUndo,
            undoActionType = undoType,
            undoActionValue = undoVal
        )
        commandDao.insertCommand(history)
    }

    fun refreshPermissions() {
        val app = getApplication<Application>()
        val hasMic = ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val hasPhone = ContextCompat.checkSelfPermission(app, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val hasSms = ContextCompat.checkSelfPermission(app, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        val hasCamera = ContextCompat.checkSelfPermission(app, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        val list = listOf(
            PermissionItem("mic", "Microphone Access", "Required for real-time voice recognition and double-clap detection", Manifest.permission.RECORD_AUDIO, hasMic, true),
            PermissionItem("notifications", "Notifications", "Required for reminders, alarms, and background assistant status", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null, hasNotif),
            PermissionItem("contacts", "Contacts Access", "Used to search and match contact names like Mom or Rahul", Manifest.permission.READ_CONTACTS, hasContacts),
            PermissionItem("phone", "Phone Calls", "Allows Nexus to place direct phone calls after your confirmation", Manifest.permission.CALL_PHONE, hasPhone),
            PermissionItem("sms", "SMS Messaging", "Allows Nexus to compose and send SMS messages after your confirmation", Manifest.permission.SEND_SMS, hasSms),
            PermissionItem("camera", "Camera / Flashlight", "Used strictly for hardware flashlight and torch control", Manifest.permission.CAMERA, hasCamera)
        )
        _permissionsList.value = list
    }

    fun toggleDoubleClap(enabled: Boolean) {
        userPrefs.updateDoubleClap(enabled)
        if (enabled) {
            clapDetector?.start(settings.value.clapSensitivity)
        } else {
            clapDetector?.stop()
        }
    }

    fun toggleBackgroundService(enabled: Boolean) {
        userPrefs.updateBackgroundService(enabled)
        val app = getApplication<Application>()
        if (enabled) {
            NexusAssistantService.startService(app)
        } else {
            NexusAssistantService.stopService(app)
        }
    }

    override fun onCleared() {
        speechRecognizerManager.stopListening()
        ttsEngine.shutdown()
        clapDetector?.stop()
        super.onCleared()
    }
}
