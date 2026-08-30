package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.NexusIntent
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusGreen
import com.example.ui.theme.NexusRed
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusSurfaceVariant
import com.example.ui.theme.NexusTextMuted
import com.example.ui.theme.NexusTextPrimary
import com.example.ui.theme.NexusTextSecondary

@Composable
fun SmartConfirmationDialog(
    intent: NexusIntent,
    onConfirm: (NexusIntent) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NexusSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, NexusCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (intent) {
                    is NexusIntent.CallContact -> {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Confirmation",
                            tint = NexusGreen,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Confirm Phone Call",
                            color = NexusTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nexus is ready to place a call to:",
                            color = NexusTextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = intent.contactName + if (!intent.phoneNumber.isNullOrBlank()) " (${intent.phoneNumber})" else "",
                            color = NexusCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cancel_call_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusTextMuted)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { onConfirm(intent) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_call_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NexusGreen, contentColor = Color.Black)
                            ) {
                                Text("Call Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is NexusIntent.SendSms -> {
                        var editableMessage by remember { mutableStateOf(intent.messageText) }
                        var isEditing by remember { mutableStateOf(false) }

                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = "SMS Confirmation",
                            tint = NexusCyan,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Confirm Outgoing SMS",
                            color = NexusTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Recipient: ${intent.recipientName} (${intent.phoneNumber ?: "Default"})",
                            color = NexusCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isEditing) {
                            OutlinedTextField(
                                value = editableMessage,
                                onValueChange = { editableMessage = it },
                                label = { Text("Message Body", color = NexusTextSecondary) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = NexusSurfaceVariant,
                                    unfocusedContainerColor = NexusSurfaceVariant,
                                    focusedTextColor = NexusTextPrimary,
                                    unfocusedTextColor = NexusTextPrimary,
                                    focusedIndicatorColor = NexusCyan,
                                    unfocusedIndicatorColor = NexusBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = NexusSurfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = editableMessage,
                                    color = NexusTextPrimary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cancel_sms_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusRed)
                            ) {
                                Text("Cancel", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { isEditing = !isEditing },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_sms_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusAmber)
                            ) {
                                Text(if (isEditing) "Done" else "Edit", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    onConfirm(intent.copy(messageText = editableMessage))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_sms_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = NexusCyan, contentColor = Color.Black)
                            ) {
                                Text("Send", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    else -> {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Confirm Action",
                            tint = NexusAmber,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Confirm Action",
                            color = NexusTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Do you want Nexus to execute this system action?",
                            color = NexusTextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexusTextMuted)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = { onConfirm(intent) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NexusCyan, contentColor = Color.Black)
                            ) {
                                Text("Confirm", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
