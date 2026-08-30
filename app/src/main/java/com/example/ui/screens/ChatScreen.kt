package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.data.model.MessageStatus
import com.example.ui.components.OrbState
import com.example.ui.components.WaveformVisualizer
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusBackground
import com.example.ui.theme.NexusBlue
import com.example.ui.theme.NexusBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusNeonViolet
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceCard
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextMuted
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.viewmodel.NexusViewModel
import com.example.voice.SpeechState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(viewModel: NexusViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val orbState by viewModel.orbState.collectAsState()
    val speechState by viewModel.speechRecognizerManager.speechState.collectAsState()
    val soundLevel by viewModel.speechRecognizerManager.soundLevel.collectAsState()
    val partialText by viewModel.speechRecognizerManager.partialText.collectAsState()
    val isSpeaking by viewModel.ttsEngine.isSpeaking.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusBackground)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NexusSurface)
                .border(1.dp, NexusBorder)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            brush = Brush.radialGradient(listOf(NexusCyan, NexusBlue)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Nexus AI",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "NEXUS AI CONVERSATION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isSpeaking) "Speaking..." else if (speechState == SpeechState.LISTENING) "Listening..." else "Neural Core Ready",
                        fontSize = 11.sp,
                        color = if (isSpeaking) NexusGreen else if (speechState == SpeechState.LISTENING) NexusAmber else NexusTextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSpeaking) {
                    IconButton(
                        onClick = { viewModel.stopSpeech() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.StopCircle,
                            contentDescription = "Stop Voice Output",
                            tint = NexusAmber
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.clearChatHistory() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = NexusTextMuted
                    )
                }
            }
        }

        // Messages Stream
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Neural Wave",
                        tint = NexusCyan.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nexus Neural Link Initialized",
                        color = NexusTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Speak or type anything in Hindi, Hinglish, or English.\nTry: \"Volume 70% karo aur YouTube pe songs chalao\"",
                        color = NexusTextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    items(messages) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            onCopy = { viewModel.copyToClipboard(msg.text) },
                            onSpeak = {
                                viewModel.ttsEngine.speak(
                                    msg.text,
                                    viewModel.settings.value.speechPitch,
                                    viewModel.settings.value.speechRate,
                                    viewModel.settings.value.preferredLanguage
                                )
                            },
                            onRegenerate = { viewModel.regenerateLastResponse() }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }

        // Live Voice Partial Result Overlay
        AnimatedVisibility(
            visible = speechState == SpeechState.LISTENING && partialText.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(NexusSurfaceCard, RoundedCornerShape(12.dp))
                    .border(1.dp, NexusAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WaveformVisualizer(isActive = true, amplitude = soundLevel, height = 20.dp, barCount = 6)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = partialText,
                    color = NexusAmber,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bottom Input Console
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NexusSurface)
                .border(1.dp, NexusBorder)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (speechState == SpeechState.LISTENING) viewModel.stopListening()
                    else viewModel.startListening()
                },
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (speechState == SpeechState.LISTENING) NexusAmber.copy(alpha = 0.25f)
                        else NexusCyan.copy(alpha = 0.12f),
                        CircleShape
                    )
                    .testTag("chat_mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (speechState == SpeechState.LISTENING) NexusAmber else NexusCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.setInputText(it) },
                placeholder = { Text("Command or ask anything...", color = NexusTextMuted, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = NexusSurfaceVariant,
                    unfocusedContainerColor = NexusSurfaceVariant,
                    focusedTextColor = NexusTextPrimary,
                    unfocusedTextColor = NexusTextPrimary,
                    focusedIndicatorColor = NexusCyan,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.processUserInput(inputText)
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .background(NexusCyan, CircleShape)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onRegenerate: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) NexusSurfaceVariant else NexusSurfaceCard
            ),
            modifier = if (!isUser) {
                Modifier
                    .widthIn(max = 320.dp)
                    .border(
                        1.dp,
                        NexusCyan.copy(alpha = 0.25f),
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
                    )
            } else {
                Modifier.widthIn(max = 320.dp)
            }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header (Voice tag or Sender)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "YOU" else "NEXUS AI",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) NexusTextSecondary else NexusCyan
                    )

                    if (message.isVoice) {
                        Text(
                            text = "🎤 VOICE",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NexusAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message Text
                Text(
                    text = message.text,
                    color = NexusTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Action Bar for AI response
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = NexusTextMuted
                    )

                    if (!isUser) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Read Aloud",
                                tint = NexusTextMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onSpeak() }
                            )
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Response",
                                tint = NexusTextMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onCopy() }
                            )
                        }
                    }
                }
            }
        }
    }
}
