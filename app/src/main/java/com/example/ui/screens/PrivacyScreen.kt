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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceCard
import com.example.ui.theme.NexusTextMuted
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.viewmodel.NexusViewModel

@Composable
fun PrivacyScreen(viewModel: NexusViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NexusBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Privacy Header
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
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Privacy & Security",
                    tint = NexusGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "PRIVACY & DATA POLICY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusGreen,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Your privacy is paramount. Nexus is designed with strict on-device data safety.",
                        fontSize = 12.sp,
                        color = NexusTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Privacy Guarantees
        item {
            PrivacyItemCard(
                icon = Icons.Default.MicNone,
                iconTint = NexusCyan,
                title = "Local Audio Processing",
                description = "Microphone audio for wake-words and sound amplitude is analyzed solely in volatile RAM on-device. Audio is never recorded to audio files, stored permanently, or streamed to third-party ad networks."
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            PrivacyItemCard(
                icon = Icons.Default.Lock,
                iconTint = NexusGreen,
                title = "Safe Android Architecture",
                description = "Nexus operates exclusively through official Android SDK APIs (CameraManager, AudioManager, SmsManager). It never requests root access, system exploit vectors, or unmanaged background recordings."
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            PrivacyItemCard(
                icon = Icons.Default.PhoneAndroid,
                iconTint = NexusAmber,
                title = "Explicit Action Safeguards",
                description = "Critical actions like placing direct phone calls and sending SMS messages require your visual confirmation before dispatch, preventing accidental or unintended communications."
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            PrivacyItemCard(
                icon = Icons.Default.Shield,
                iconTint = NexusBlue,
                title = "Local On-Device Database",
                description = "Chat history and operation logs are saved in an isolated Room SQLite database on your device. You retain 100% control to clear or purge this data at any time."
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Data Destruction Actions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NexusSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, NexusRed.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DATA MANAGEMENT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NexusRed,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Purge cached conversation turns and executed command histories from local storage.",
                        fontSize = 12.sp,
                        color = NexusTextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearChatHistory() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("privacy_clear_chat_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear Chat", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { viewModel.deleteAllData() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("privacy_delete_all_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NexusRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Wipe",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe All Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun PrivacyItemCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NexusSurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NexusTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = NexusTextSecondary
                )
            }
        }
    }
}
