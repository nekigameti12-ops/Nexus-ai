package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: NexusViewModel) {
    val settings by viewModel.settings.collectAsState()
    var langExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NexusSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, NexusBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Nexus Settings",
                    tint = NexusCyan,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "ASSISTANT CONFIGURATION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Customize speech voice, wake-triggers, and interaction behavior.",
                        fontSize = 12.sp,
                        color = NexusTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 1: Voice & Audio Output
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NexusSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Engine",
                            tint = NexusCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VOICE & SPEECH SYNTHESIS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto-Speak Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Speak Responses", color = NexusTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Nexus reads out answers and action statuses aloud", color = NexusTextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.autoSpeakResponses,
                            onCheckedChange = { viewModel.userPrefs.updateAutoSpeak(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NexusCyan, checkedTrackColor = NexusBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Speech Rate Slider
                    Text(
                        text = "Speech Rate: ${String.format(Locale.ROOT, "%.2fx", settings.speechRate)}",
                        color = NexusTextPrimary,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = settings.speechRate,
                        onValueChange = { viewModel.userPrefs.updateSpeechRate(it) },
                        valueRange = 0.6f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = NexusCyan, activeTrackColor = NexusCyan, inactiveTrackColor = NexusSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Speech Pitch Slider
                    Text(
                        text = "Speech Pitch: ${String.format(Locale.ROOT, "%.2fx", settings.speechPitch)}",
                        color = NexusTextPrimary,
                        fontSize = 13.sp
                    )
                    Slider(
                        value = settings.speechPitch,
                        onValueChange = { viewModel.userPrefs.updateSpeechPitch(it) },
                        valueRange = 0.6f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = NexusNeonViolet, activeTrackColor = NexusNeonViolet, inactiveTrackColor = NexusSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preferred Language Dropdown
                    Text("Preferred Voice Language", color = NexusTextPrimary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    ExposedDropdownMenuBox(
                        expanded = langExpanded,
                        onExpandedChange = { langExpanded = !langExpanded }
                    ) {
                        OutlinedTextField(
                            value = when (settings.preferredLanguage) {
                                "hi" -> "Hindi (हिंदी)"
                                "en" -> "English (Indian / International)"
                                else -> "Auto Detect (Hindi / English / Hinglish)"
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = NexusSurfaceVariant,
                                unfocusedContainerColor = NexusSurfaceVariant,
                                focusedTextColor = NexusTextPrimary,
                                unfocusedTextColor = NexusTextPrimary,
                                focusedIndicatorColor = NexusCyan,
                                unfocusedIndicatorColor = NexusBorder
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false },
                            modifier = Modifier.background(NexusSurface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Auto Detect (Hindi / English / Hinglish)", color = NexusTextPrimary) },
                                onClick = {
                                    viewModel.userPrefs.updatePreferredLanguage("auto")
                                    langExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Hindi (हिंदी)", color = NexusTextPrimary) },
                                onClick = {
                                    viewModel.userPrefs.updatePreferredLanguage("hi")
                                    langExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("English (Indian / International)", color = NexusTextPrimary) },
                                onClick = {
                                    viewModel.userPrefs.updatePreferredLanguage("en")
                                    langExpanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.ttsEngine.speak(
                                "Nexus online. All systems nominal.",
                                settings.speechPitch,
                                settings.speechRate,
                                settings.preferredLanguage
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Test", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Voice Synthesis")
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section 2: Wake & Trigger Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NexusSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WbIncandescent,
                            contentDescription = "Triggers",
                            tint = NexusAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WAKE & ACTIVATION TRIGGERS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Wake Word Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wake Word (\"Nexus\")", color = NexusTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Responds when you say \"Nexus\" or \"Hey Nexus\"", color = NexusTextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.wakeWordEnabled,
                            onCheckedChange = { viewModel.userPrefs.updateWakeWord(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NexusCyan, checkedTrackColor = NexusBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Double-Clap Activation Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Double-Clap Wake Trigger", color = NexusTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Wakes Nexus when 2 sharp claps are detected locally", color = NexusTextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.doubleClapEnabled,
                            onCheckedChange = { viewModel.toggleDoubleClap(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NexusAmber, checkedTrackColor = Color(0xFFB45309))
                        )
                    }

                    if (settings.doubleClapEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Clap Sensitivity: ${String.format(Locale.ROOT, "%.0f%%", settings.clapSensitivity * 100)}",
                            color = NexusTextPrimary,
                            fontSize = 13.sp
                        )
                        Slider(
                            value = settings.clapSensitivity,
                            onValueChange = { viewModel.userPrefs.updateClapSensitivity(it) },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = NexusAmber, activeTrackColor = NexusAmber, inactiveTrackColor = NexusSurfaceVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Background Assistant Service Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Assistant Service", color = NexusTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Keeps foreground notification active for continuous availability", color = NexusTextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.backgroundServiceEnabled,
                            onCheckedChange = { viewModel.toggleBackgroundService(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NexusGreen, checkedTrackColor = Color(0xFF047857))
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section 3: Feedback & Haptics
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NexusSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Haptics",
                            tint = NexusNeonViolet,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HAPTICS & SYSTEM FEEDBACK",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusNeonViolet
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Haptic Feedback", color = NexusTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Subtle mechanical pulses when voice triggers and buttons activate", color = NexusTextSecondary, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.hapticsEnabled,
                            onCheckedChange = { viewModel.userPrefs.updateHaptics(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NexusNeonViolet, checkedTrackColor = NexusBlue)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section 4: About & Specs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NexusSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = NexusCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NEXUS AI SYSTEM INFORMATION",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NexusCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "NEXUS AI Personal Assistant\nVersion 3.5.0 (Build 2026.08)\nPowered by Gemini 3.5 Flash & Jetpack Compose\nEncrypted Local Storage: Active",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = NexusTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
