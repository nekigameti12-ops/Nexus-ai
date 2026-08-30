package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NexusOrb
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: NexusViewModel, onNavigateChat: () -> Unit) {
    val orbState by viewModel.orbState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val recentCommands by viewModel.recentCommands.collectAsState()
    val speechState by viewModel.speechRecognizerManager.speechState.collectAsState()
    val soundLevel by viewModel.speechRecognizerManager.soundLevel.collectAsState()
    val partialText by viewModel.speechRecognizerManager.partialText.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. HUD Top Status Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexusSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, NexusBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (orbState == OrbState.ERROR) NexusAmber else NexusGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NEXUS CORE // v3.5",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusCyan,
                        letterSpacing = 1.5.sp
                    )
                }

                Text(
                    text = when (orbState) {
                        OrbState.IDLE -> "SYSTEM READY"
                        OrbState.LISTENING -> "AUDIO INPUT ACTIVE"
                        OrbState.THINKING -> "NEURAL PROCESSING"
                        OrbState.SPEAKING -> "VOICE TRANSMITTING"
                        OrbState.ERROR -> "ATTENTION REQUIRED"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = when (orbState) {
                        OrbState.IDLE -> NexusTextSecondary
                        OrbState.LISTENING -> NexusAmber
                        OrbState.THINKING -> NexusNeonViolet
                        OrbState.SPEAKING -> NexusGreen
                        OrbState.ERROR -> Color(0xFFEF4444)
                    }
                )
            }
        }

        // 2. Interactive Nexus Orb
        item {
            Spacer(modifier = Modifier.height(24.dp))
            NexusOrb(
                orbState = orbState,
                size = 190.dp,
                amplitude = soundLevel,
                onClick = {
                    if (speechState == SpeechState.LISTENING) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // State Subtext or Partial recognition display
            if (speechState == SpeechState.LISTENING && partialText.isNotBlank()) {
                Text(
                    text = "\"$partialText\"",
                    color = NexusCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                Text(
                    text = when (orbState) {
                        OrbState.IDLE -> "Tap orb or say \"Hey Nexus\""
                        OrbState.LISTENING -> "Listening... Speak your command"
                        OrbState.THINKING -> "Executing requested operations..."
                        OrbState.SPEAKING -> "Tap orb to interrupt speech"
                        OrbState.ERROR -> "Voice error. Tap to retry."
                    },
                    color = NexusTextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            WaveformVisualizer(
                isActive = speechState == SpeechState.LISTENING || orbState == OrbState.SPEAKING,
                amplitude = soundLevel,
                height = 24.dp,
                barCount = 14
            )
        }

        // 3. Command Input Bar
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexusSurface, RoundedCornerShape(24.dp))
                    .border(1.dp, NexusBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (speechState == SpeechState.LISTENING) viewModel.stopListening()
                        else viewModel.startListening()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (speechState == SpeechState.LISTENING) NexusAmber.copy(alpha = 0.2f)
                            else NexusCyan.copy(alpha = 0.12f),
                            CircleShape
                        )
                        .testTag("home_mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        tint = if (speechState == SpeechState.LISTENING) NexusAmber else NexusCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.setInputText(it) },
                    placeholder = { Text("Ask or command Nexus...", color = NexusTextMuted, fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = NexusTextPrimary,
                        unfocusedTextColor = NexusTextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("home_input_field"),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.processUserInput(inputText)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NexusCyan, CircleShape)
                        .testTag("home_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Command",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 4. Quick Action Grid
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUICK COMMANDS",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NexusCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Hindi & English",
                    fontSize = 11.sp,
                    color = NexusTextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                QuickActionCard(
                    icon = Icons.Default.FlashlightOn,
                    label = "Torch On/Off",
                    hint = "Torch jalao",
                    color = NexusAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.processUserInput("Flashlight on") }
                )
                QuickActionCard(
                    icon = Icons.Default.VolumeUp,
                    label = "Set Volume",
                    hint = "Volume 50%",
                    color = NexusCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.processUserInput("Volume 50 percent karo") }
                )
                QuickActionCard(
                    icon = Icons.Default.PlayArrow,
                    label = "YouTube",
                    hint = "Music search",
                    color = Color(0xFFFF4444),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.processUserInput("YouTube par relax music search karo") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                QuickActionCard(
                    icon = Icons.Default.Call,
                    label = "Make Call",
                    hint = "Call Mom",
                    color = NexusGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.processUserInput("Call Mom") }
                )
                QuickActionCard(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "Battery Info",
                    hint = "Battery check",
                    color = NexusNeonViolet,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.processUserInput("Battery percentage kitna hai?") }
                )
                QuickActionCard(
                    icon = Icons.Default.Settings,
                    label = "Bluetooth",
                    hint = "Open settings",
                    color = NexusBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.processUserInput("Bluetooth settings kholo") }
                )
            }
        }

        // 5. Recent History Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = NexusCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RECENT OPERATIONS",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NexusCyan,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Open Chat ➔",
                    fontSize = 12.sp,
                    color = NexusCyan,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onNavigateChat() }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (recentCommands.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NexusSurfaceCard)
                ) {
                    Text(
                        text = "No recent operations yet. Try asking: \"Volume 60% karo aur Bluetooth settings kholo\"",
                        color = NexusTextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(recentCommands.take(4)) { cmd ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = NexusSurfaceCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "\"${cmd.commandText}\"",
                                color = NexusTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = cmd.description,
                                color = NexusTextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (cmd.wasSuccessful) NexusGreen.copy(alpha = 0.15f)
                                    else NexusAmber.copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (cmd.wasSuccessful) "DONE" else "FAILED",
                                color = if (cmd.wasSuccessful) NexusGreen else NexusAmber,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    hint: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NexusSurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexusTextPrimary
                )
                Text(
                    text = hint,
                    fontSize = 10.sp,
                    color = NexusTextMuted
                )
            }
        }
    }
}
