package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.OutputRouteMode
import com.example.ui.VoiceChangerViewModel
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.VibrantCrimson
import com.example.ui.theme.VibrantDeepPurple
import com.example.ui.theme.VibrantLavender
import com.example.ui.theme.VibrantRosePink
import com.example.ui.theme.VibrantSuccessGreen
import com.example.ui.theme.VibrantWarningAmber

@Composable
fun AudioDiagnosticScreen(
    viewModel: VoiceChangerViewModel,
    onRequestMicPermission: () -> Unit,
    hasMicPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val diagnostics by viewModel.diagnostics.collectAsState()
    val isLiveListening by viewModel.isLiveListening.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header card with refresh
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_diagnostics_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio Routing Diagnostics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Hardware telemetry & Android routing status",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { viewModel.refreshAudioDiagnostics() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Diagnostics",
                            tint = VibrantLavender,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Live Subsystem Diagnostic Items
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "LIVE AUDIO SUBSYSTEM STATUS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VibrantLavender
                    )

                    // 1. Microphone Permission
                    DiagnosticRow(
                        icon = Icons.Default.Mic,
                        title = "Microphone Permission",
                        value = if (hasMicPermission) "Granted (RECORD_AUDIO)" else "Missing Permission",
                        isGood = hasMicPermission,
                        statusText = if (hasMicPermission) "OK" else "ACTION REQUIRED",
                        onClick = if (!hasMicPermission) onRequestMicPermission else null
                    )

                    // 2. Microphone Availability
                    DiagnosticRow(
                        icon = if (diagnostics.isAnotherAppRecording) Icons.Default.Warning else Icons.Default.CheckCircle,
                        title = "Microphone Availability",
                        value = if (diagnostics.isAnotherAppRecording) "In Use By Another App (Concurrent Capture)"
                        else if (diagnostics.isClientSilenced) "Mic Silenced by System"
                        else "Microphone Hardware Available",
                        isGood = !diagnostics.isAnotherAppRecording && !diagnostics.isClientSilenced,
                        statusText = if (diagnostics.isAnotherAppRecording) "SHARED" else "READY"
                    )

                    // 3. Voice Service Running
                    DiagnosticRow(
                        icon = Icons.Default.Router,
                        title = "Voice Service Pipeline",
                        value = if (isLiveListening) "Active Foreground DSP Engine" else "Idle (Tap Studio/Mic Test to Start)",
                        isGood = isLiveListening,
                        statusText = if (isLiveListening) "RUNNING" else "STANDBY"
                    )

                    // 4. Active Audio Input Device
                    DiagnosticRow(
                        icon = Icons.Default.Mic,
                        title = "Active Input Device",
                        value = diagnostics.activeInputDevice,
                        isGood = true,
                        statusText = "CONNECTED"
                    )

                    // 5. Active Audio Output Device
                    DiagnosticRow(
                        icon = if (diagnostics.isBluetoothConnected) Icons.Default.Bluetooth else Icons.Default.Speaker,
                        title = "Active Output Device",
                        value = diagnostics.activeOutputDevice,
                        isGood = true,
                        statusText = "CONNECTED"
                    )

                    // 6. Audio Sample Rate & Buffer
                    DiagnosticRow(
                        icon = Icons.Default.Info,
                        title = "DSP Engine Spec",
                        value = "${diagnostics.sampleRateHz} Hz • ${diagnostics.bufferSizeFrames} frames (${diagnostics.latencyMs}ms roundtrip)",
                        isGood = true,
                        statusText = "FAST"
                    )
                }
            }
        }

        // Output Route Mode Selector
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AUDIO ROUTING MODE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VibrantLavender
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutputRouteMode.entries.forEach { mode ->
                        val isSel = diagnostics.routeMode == mode
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) VibrantLavender.copy(alpha = 0.15f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isSel) VibrantLavender else DarkCardBorder.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setAudioRouteMode(mode) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSel) VibrantLavender else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = VibrantLavender,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Android Audio Security & Routing Architecture Explanation
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = VibrantDeepPurple.copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, VibrantLavender.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = VibrantLavender,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Android Audio Routing Security Model",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = VibrantLavender
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Why doesn't Android allow silent virtual mic injection into arbitrary 3rd party apps?\n\n" +
                                "1. Android OS Sandbox & SELinux: To protect user privacy against malware and eavesdropping, Android does not allow normal user-space apps to hijack or replace the raw hardware microphone input stream of other apps.\n\n" +
                                "2. Official Supported Routing Solutions:\n" +
                                "• 1-Tap Voice Note Share: Record transformed voice clips and share directly into WhatsApp, Discord, Telegram, or Messenger.\n" +
                                "• Headset / AUX Passthrough: Connect a 3.5mm TRRS splitter or Bluetooth SCO headset to feed changed voice directly into game or call mics.\n" +
                                "• Internal Playback Capture (Android 10+): Screen recorders and streaming apps can capture the transformed voice audio stream directly.\n" +
                                "• Floating Companion Bubble: Switch voice effects on the fly without closing your game or app.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    icon: ImageVector,
    title: String,
    value: String,
    isGood: Boolean,
    statusText: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isGood) VibrantSuccessGreen.copy(alpha = 0.15f) else VibrantWarningAmber.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGood) VibrantSuccessGreen else VibrantWarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = value,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isGood) VibrantSuccessGreen.copy(alpha = 0.15f) else VibrantWarningAmber.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, if (isGood) VibrantSuccessGreen.copy(alpha = 0.3f) else VibrantWarningAmber.copy(alpha = 0.3f))
        ) {
            Text(
                text = statusText,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isGood) VibrantSuccessGreen else VibrantWarningAmber,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

private fun Modifier.surfaceBackground(color: Color): Modifier = this.then(Modifier.clip(CircleShape))
