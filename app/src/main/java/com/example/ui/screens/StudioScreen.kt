package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEffectParams
import com.example.audio.VoiceEffectType
import com.example.ui.VoiceChangerViewModel
import com.example.ui.components.AudioEqualizerVisualizer
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardBorderSelected
import com.example.ui.theme.VibrantCrimson
import com.example.ui.theme.VibrantCrimsonBright
import com.example.ui.theme.VibrantDeepPurple
import com.example.ui.theme.VibrantLavender
import com.example.ui.theme.VibrantRosePink
import com.example.ui.theme.VibrantSuccessGreen
import com.example.ui.theme.VibrantWarningAmber
import java.util.Locale

@Composable
fun StudioScreen(
    viewModel: VoiceChangerViewModel,
    onRequestMicPermission: () -> Unit,
    hasMicPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedEffect by viewModel.selectedEffect.collectAsState()
    val customParams by viewModel.customParams.collectAsState()
    val isLiveListening by viewModel.isLiveListening.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()
    val visualizerBars by viewModel.liveVisualizerBars.collectAsState()
    val favoriteEffects by viewModel.favoriteEffects.collectAsState()

    var showCustomTuneSheet by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Visualizer & Action Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_visualizer_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Badge Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = VibrantLavender,
                            border = BorderStroke(1.dp, VibrantLavender)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = selectedEffect.icon,
                                    contentDescription = null,
                                    tint = VibrantDeepPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedEffect.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VibrantDeepPurple
                                )
                            }
                        }

                        if (isRecording) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = VibrantRosePink,
                                border = BorderStroke(1.dp, VibrantCrimson.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(VibrantCrimson)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val mins = recordingDurationSec / 60
                                    val secs = recordingDurationSec % 60
                                    Text(
                                        text = String.format(Locale.US, "REC %02d:%02d", mins, secs),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = VibrantCrimson
                                    )
                                }
                            }
                        } else if (isLiveListening) {
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = VibrantSuccessGreen.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, VibrantSuccessGreen.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = VibrantSuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "LIVE MIC ON",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = VibrantSuccessGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Animated Equalizer Waveform with Vibrant Gradient
                    AudioEqualizerVisualizer(
                        bars = visualizerBars,
                        isActive = isLiveListening || isRecording,
                        activeColorStart = VibrantLavender,
                        activeColorEnd = VibrantRosePink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Big Action Controls (Live Mic Toggle + Record Button)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Live Mic Listen Switch Button
                        FilledTonalButton(
                            onClick = {
                                if (!hasMicPermission) {
                                    onRequestMicPermission()
                                } else {
                                    viewModel.toggleLiveMic()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.testTag("live_mic_toggle_button"),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isLiveListening) VibrantSuccessGreen.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = if (isLiveListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Live Mic",
                                tint = if (isLiveListening) VibrantSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isLiveListening) "Live Voice ON" else "Listen Live",
                                color = if (isLiveListening) VibrantSuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Vibrant Record Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .then(if (isRecording) Modifier.scale(pulseScale) else Modifier)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = if (isRecording) listOf(VibrantCrimson, Color(0xFF601410))
                                        else listOf(Color(0xFFE21D48), VibrantCrimson)
                                    )
                                )
                                .clickable {
                                    if (!hasMicPermission) {
                                        onRequestMicPermission()
                                    } else {
                                        viewModel.toggleRecording()
                                    }
                                }
                                .testTag("record_button")
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // Custom Tuning Toggle Card
        item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        showCustomTuneSheet = !showCustomTuneSheet
                        if (showCustomTuneSheet) {
                            viewModel.selectVoiceEffect(VoiceEffectType.CUSTOM)
                        }
                    }
                    .testTag("custom_tune_toggle"),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedEffect == VoiceEffectType.CUSTOM)
                        VibrantLavender.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (selectedEffect == VoiceEffectType.CUSTOM) VibrantLavender
                    else DarkCardBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = VibrantLavender.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = VibrantLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Custom Pitch & Speed Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (showCustomTuneSheet) "Tap to collapse sliders" else "Fine-tune sliders & audio parameters",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = if (showCustomTuneSheet) "Hide" else "Tune",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = VibrantLavender
                    )
                }
            }
        }

        // Custom Parameter Sliders Accordion
        item(span = { GridItemSpan(maxLineSpan) }) {
            AnimatedVisibility(
                visible = showCustomTuneSheet,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, VibrantLavender.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "AUDIO PARAMETERS FINE-TUNING",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = VibrantLavender
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Pitch Slider
                        CustomSliderItem(
                            label = "Voice Pitch",
                            valueText = String.format(Locale.US, "%.2fx", customParams.pitchFactor),
                            value = customParams.pitchFactor,
                            range = 0.5f..2.0f,
                            onValueChange = {
                                viewModel.updateCustomParams(customParams.copy(pitchFactor = it))
                            }
                        )

                        // Speed Slider
                        CustomSliderItem(
                            label = "Playback Speed",
                            valueText = String.format(Locale.US, "%.2fx", customParams.speedFactor),
                            value = customParams.speedFactor,
                            range = 0.5f..2.0f,
                            onValueChange = {
                                viewModel.updateCustomParams(customParams.copy(speedFactor = it))
                            }
                        )

                        // Echo Delay Slider
                        CustomSliderItem(
                            label = "Echo Delay",
                            valueText = "${customParams.echoDelayMs} ms",
                            value = customParams.echoDelayMs.toFloat(),
                            range = 0f..400f,
                            onValueChange = {
                                val delay = it.toInt()
                                val fb = if (delay > 0 && customParams.echoFeedback == 0f) 0.45f else customParams.echoFeedback
                                viewModel.updateCustomParams(customParams.copy(echoDelayMs = delay, echoFeedback = fb))
                            }
                        )

                        // Robot Ring Modulation
                        CustomSliderItem(
                            label = "Robot Ring Frequency",
                            valueText = if (customParams.robotRingFreqHz > 0f) "${customParams.robotRingFreqHz.toInt()} Hz" else "Off",
                            value = customParams.robotRingFreqHz,
                            range = 0f..180f,
                            onValueChange = {
                                val mix = if (it > 0f) 0.85f else 0f
                                viewModel.updateCustomParams(customParams.copy(robotRingFreqHz = it, robotMix = mix))
                            }
                        )

                        // Overdrive Distortion
                        CustomSliderItem(
                            label = "Overdrive Distortion",
                            valueText = "${(customParams.distortion * 100).toInt()}%",
                            value = customParams.distortion,
                            range = 0.0f..1.0f,
                            onValueChange = {
                                viewModel.updateCustomParams(customParams.copy(distortion = it))
                            }
                        )

                        // Bass Boost
                        CustomSliderItem(
                            label = "Bass Resonance",
                            valueText = String.format(Locale.US, "%.1fx", customParams.lowShelfGain),
                            value = customParams.lowShelfGain,
                            range = 0.5f..2.5f,
                            onValueChange = {
                                viewModel.updateCustomParams(customParams.copy(lowShelfGain = it))
                            }
                        )
                    }
                }
            }
        }

        // Favorites Quick Carousel
        val favEffectsList = VoiceEffectType.entries.filter { favoriteEffects.contains(it.name) }
        if (favEffectsList.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐ Favorite Voice Effects",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${favEffectsList.size} pinned",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(favEffectsList) { effect ->
                            val isSelected = selectedEffect == effect
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) VibrantLavender else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) VibrantLavender else DarkCardBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.selectVoiceEffect(effect) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = effect.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) VibrantDeepPurple else effect.themeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = effect.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) VibrantDeepPurple else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: All Voice Effects
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Voice Presets",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        // Grid of All Voice Effects
        items(VoiceEffectType.entries) { effect ->
            val isSelected = selectedEffect == effect
            val isFav = favoriteEffects.contains(effect.name)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { viewModel.selectVoiceEffect(effect) }
                    .testTag("effect_card_${effect.name.lowercase()}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) VibrantLavender.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) VibrantLavender else DarkCardBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) VibrantLavender else effect.themeColor.copy(alpha = 0.18f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = effect.icon,
                                    contentDescription = effect.title,
                                    tint = if (isSelected) VibrantDeepPurple else effect.themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.toggleFavoriteEffect(effect.name) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) VibrantWarningAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = effect.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isSelected) VibrantLavender else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = effect.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CustomSliderItem(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Text(
                    text = valueText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = VibrantLavender,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = VibrantLavender,
                activeTrackColor = VibrantLavender,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
