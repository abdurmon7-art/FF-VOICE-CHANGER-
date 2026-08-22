package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AppsOverlayScreen
import com.example.ui.screens.AudioDiagnosticScreen
import com.example.ui.screens.MicTestScreen
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.VibrantCrimson
import com.example.ui.theme.VibrantDeepPurple
import com.example.ui.theme.VibrantLavender
import com.example.ui.theme.VibrantSuccessGreen
import com.example.ui.theme.VibrantWarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: VoiceChangerViewModel,
    onRequestMicPermission: () -> Unit,
    hasMicPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isLiveListening by viewModel.isLiveListening.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isMicTesting by viewModel.isMicTesting.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = VibrantLavender,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = VibrantDeepPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Voice Changer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Dark / Light Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = if (isDarkMode) VibrantWarningAmber else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                // 1. Studio Tab
                NavigationBarItem(
                    selected = currentTab == AppTab.STUDIO,
                    onClick = { viewModel.setTab(AppTab.STUDIO) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (isLiveListening || isRecording) {
                                    Badge(containerColor = if (isRecording) VibrantCrimson else VibrantSuccessGreen)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Studio"
                            )
                        }
                    },
                    label = { Text("Studio", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VibrantDeepPurple,
                        selectedTextColor = VibrantLavender,
                        indicatorColor = VibrantLavender,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_studio")
                )

                // 2. Mic Test Tab
                NavigationBarItem(
                    selected = currentTab == AppTab.MIC_TEST,
                    onClick = { viewModel.setTab(AppTab.MIC_TEST) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (isMicTesting) {
                                    Badge(containerColor = VibrantSuccessGreen)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = "Mic Test"
                            )
                        }
                    },
                    label = { Text("Mic Test", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VibrantDeepPurple,
                        selectedTextColor = VibrantLavender,
                        indicatorColor = VibrantLavender,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_mictest")
                )

                // 3. Diagnostics Tab
                NavigationBarItem(
                    selected = currentTab == AppTab.DIAGNOSTICS,
                    onClick = { viewModel.setTab(AppTab.DIAGNOSTICS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (diagnostics.isAnotherAppRecording) {
                                    Badge(containerColor = VibrantWarningAmber)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = "Diagnostics"
                            )
                        }
                    },
                    label = { Text("Diagnostics", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VibrantDeepPurple,
                        selectedTextColor = VibrantLavender,
                        indicatorColor = VibrantLavender,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_diagnostics")
                )

                // 4. Compatibility Tab
                NavigationBarItem(
                    selected = currentTab == AppTab.COMPATIBILITY,
                    onClick = { viewModel.setTab(AppTab.COMPATIBILITY) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (isOverlayActive) {
                                    Badge(containerColor = VibrantLavender)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Apps & Routing"
                            )
                        }
                    },
                    label = { Text("Apps", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VibrantDeepPurple,
                        selectedTextColor = VibrantLavender,
                        indicatorColor = VibrantLavender,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_apps")
                )

                // 5. Recordings Tab
                NavigationBarItem(
                    selected = currentTab == AppTab.RECORDINGS,
                    onClick = { viewModel.setTab(AppTab.RECORDINGS) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Recordings"
                        )
                    },
                    label = { Text("Saved", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = VibrantDeepPurple,
                        selectedTextColor = VibrantLavender,
                        indicatorColor = VibrantLavender,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_recordings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                when (tab) {
                    AppTab.STUDIO -> StudioScreen(
                        viewModel = viewModel,
                        onRequestMicPermission = onRequestMicPermission,
                        hasMicPermission = hasMicPermission
                    )
                    AppTab.MIC_TEST -> MicTestScreen(
                        viewModel = viewModel,
                        onRequestMicPermission = onRequestMicPermission,
                        hasMicPermission = hasMicPermission
                    )
                    AppTab.DIAGNOSTICS -> AudioDiagnosticScreen(
                        viewModel = viewModel,
                        onRequestMicPermission = onRequestMicPermission,
                        hasMicPermission = hasMicPermission
                    )
                    AppTab.COMPATIBILITY -> AppsOverlayScreen(viewModel = viewModel)
                    AppTab.RECORDINGS -> RecordingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
