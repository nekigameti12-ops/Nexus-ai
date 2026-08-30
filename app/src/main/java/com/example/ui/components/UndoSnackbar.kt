package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NexusAmber
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusSurfaceCard
import com.example.ui.theme.NexusTextPrimary

data class UndoState(
    val visible: Boolean = false,
    val message: String = "",
    val undoType: String = "",
    val undoValue: String = ""
)

@Composable
fun UndoSnackbar(
    undoState: UndoState,
    onUndoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = undoState.visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(NexusSurfaceCard, RoundedCornerShape(12.dp))
                .border(1.dp, NexusAmber.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = undoState.message,
                color = NexusTextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onUndoClick,
                colors = ButtonDefaults.textButtonColors(contentColor = NexusCyan),
                modifier = Modifier.testTag("undo_action_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo Action",
                    tint = NexusCyan,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("UNDO", fontWeight = FontWeight.Bold)
            }
        }
    }
}
