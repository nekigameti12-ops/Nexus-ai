package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.NexusAssistantService
import com.example.ui.components.SmartConfirmationDialog
import com.example.ui.components.UndoSnackbar
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NexusBackground
import com.example.ui.theme.NexusBorder
import com.example.ui.theme.NexusCyan
import com.example.ui.theme.NexusSurface
import com.example.ui.theme.NexusTextMuted
import com.example.ui.theme.NexusTextSecondary
import com.example.ui.viewmodel.NexusTab
import com.example.ui.viewmodel.NexusViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NexusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleWakeIntent(intent)

        setContent {
            MyApplicationTheme {
                NexusAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWakeIntent(intent)
    }

    private fun handleWakeIntent(intent: Intent?) {
        if (intent?.action == NexusAssistantService.ACTION_DOUBLE_CLAP_WAKE) {
            viewModel.startListening()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }
}

@Composable
fun NexusAppRoot(viewModel: NexusViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val undoState by viewModel.undoState.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NexusBackground,
        bottomBar = {
            NexusBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Content
            when (currentTab) {
                NexusTab.HOME -> HomeScreen(viewModel = viewModel, onNavigateChat = { viewModel.selectTab(NexusTab.CHAT) })
                NexusTab.CHAT -> ChatScreen(viewModel = viewModel)
                NexusTab.PERMISSIONS -> PermissionsScreen(viewModel = viewModel)
                NexusTab.PRIVACY -> PrivacyScreen(viewModel = viewModel)
                NexusTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }

            // Undo Floating Bar
            UndoSnackbar(
                undoState = undoState,
                onUndoClick = { viewModel.performUndo() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            )

            // Confirmation Dialog for Calls / SMS
            pendingConfirmation?.let { intent ->
                SmartConfirmationDialog(
                    intent = intent,
                    onConfirm = { viewModel.confirmPendingAction() },
                    onDismiss = { viewModel.cancelPendingAction() }
                )
            }
        }
    }
}

@Composable
fun NexusBottomNavBar(
    currentTab: NexusTab,
    onTabSelected: (NexusTab) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(NexusSurface)
            .border(1.dp, NexusBorder),
        containerColor = NexusSurface,
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            Triple(NexusTab.HOME, "Home", Icons.Default.Home),
            Triple(NexusTab.CHAT, "Chat", Icons.Default.ChatBubble),
            Triple(NexusTab.PERMISSIONS, "Access", Icons.Default.Security),
            Triple(NexusTab.PRIVACY, "Privacy", Icons.Default.Shield),
            Triple(NexusTab.SETTINGS, "Settings", Icons.Default.Settings)
        )

        items.forEach { (tab, label, icon) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) NexusCyan else NexusTextMuted
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NexusCyan else NexusTextSecondary
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = NexusCyan.copy(alpha = 0.15f),
                    selectedIconColor = NexusCyan,
                    selectedTextColor = NexusCyan,
                    unselectedIconColor = NexusTextMuted,
                    unselectedTextColor = NexusTextMuted
                ),
                modifier = Modifier.testTag("nav_tab_${label.lowercase()}")
            )
        }
    }
}
