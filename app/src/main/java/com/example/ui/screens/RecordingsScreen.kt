package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.VoiceEffectType
import com.example.data.RecordingItem
import com.example.ui.VoiceChangerViewModel
import com.example.ui.components.AudioEqualizerVisualizer
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.VibrantCrimson
import com.example.ui.theme.VibrantCrimsonBright
import com.example.ui.theme.VibrantDeepPurple
import com.example.ui.theme.VibrantLavender
import com.example.ui.theme.VibrantRosePink
import com.example.ui.theme.VibrantWarningAmber
import java.util.Locale

enum class RecordingFilter {
    ALL,
    FAVORITES,
    IMPORTED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    viewModel: VoiceChangerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordings by viewModel.recordingsList.collectAsState()
    val searchQuery by viewModel.recordingsSearchQuery.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.playerPositionMs.collectAsState()
    val totalDurationMs by viewModel.playerTotalMs.collectAsState()
    val activePlayingRecording by viewModel.activePlayingRecording.collectAsState()
    val playerVisualizerBars by viewModel.playerVisualizerBars.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    var activeFilter by remember { mutableStateOf(RecordingFilter.ALL) }
    var recordingToRename by remember { mutableStateOf<RecordingItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var recordingToApplyEffect by remember { mutableStateOf<RecordingItem?>(null) }

    // File picker launcher for importing audio files
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importAudioFile(it) }
    }

    val filteredList = recordings.filter { item ->
        when (activeFilter) {
            RecordingFilter.ALL -> true
            RecordingFilter.FAVORITES -> item.isFavorite
            RecordingFilter.IMPORTED -> item.isImported
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar & Import Action Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchRecordings(it) },
                placeholder = { Text("Search voice recordings...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchRecordings("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("recordings_search_bar")
            )

            // Import Audio Button with Vibrant Accent
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VibrantLavender,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { audioPickerLauncher.launch("audio/*") }
                    .testTag("import_audio_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Import Audio",
                        tint = VibrantDeepPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Import",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = VibrantDeepPurple
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = activeFilter == RecordingFilter.ALL,
                    onClick = { activeFilter = RecordingFilter.ALL },
                    label = { Text("All (${recordings.size})") },
                    shape = RoundedCornerShape(100.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibrantLavender,
                        selectedLabelColor = VibrantDeepPurple
                    )
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == RecordingFilter.FAVORITES,
                    onClick = { activeFilter = RecordingFilter.FAVORITES },
                    label = { Text("⭐ Favorites (${recordings.count { it.isFavorite }})") },
                    shape = RoundedCornerShape(100.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibrantLavender,
                        selectedLabelColor = VibrantDeepPurple
                    )
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == RecordingFilter.IMPORTED,
                    onClick = { activeFilter = RecordingFilter.IMPORTED },
                    label = { Text("📁 Imported (${recordings.count { it.isImported }})") },
                    shape = RoundedCornerShape(100.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VibrantLavender,
                        selectedLabelColor = VibrantDeepPurple
                    )
                )
            }
        }

        // Active Player Card if playing or active
        activePlayingRecording?.let { activeItem ->
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("active_player_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.5.dp, VibrantLavender)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeItem.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val effectPreset = VoiceEffectType.fromName(activeItem.effectType)
                            Text(
                                text = "Effect: ${effectPreset.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VibrantLavender
                            )
                        }

                        IconButton(
                            onClick = { viewModel.playRecording(activeItem) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(VibrantLavender)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = VibrantDeepPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AudioEqualizerVisualizer(
                        bars = playerVisualizerBars,
                        isActive = isPlaying,
                        activeColorStart = VibrantLavender,
                        activeColorEnd = VibrantRosePink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Scrubbing Slider
                    val effectiveTotal = if (totalDurationMs > 0) totalDurationMs else activeItem.durationMs.coerceAtLeast(1000)
                    Slider(
                        value = currentPositionMs.toFloat().coerceIn(0f, effectiveTotal.toFloat()),
                        onValueChange = { viewModel.seekPlayer(it.toInt()) },
                        valueRange = 0f..effectiveTotal.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = VibrantLavender,
                            activeTrackColor = VibrantLavender,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = formatDuration(currentPositionMs), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = formatDuration(effectiveTotal), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Loading indicator for processing / importing
        if (isExporting) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Applying voice effect DSP...",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = VibrantLavender,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Empty state or list
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching recordings found" else "No recordings yet",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Record voice from the Studio or tap Import to load audio files!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    val effectType = VoiceEffectType.fromName(item.effectType)
                    val isCurrentlyPlaying = activePlayingRecording?.id == item.id && isPlaying

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .testTag("recording_item_${item.id}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (activePlayingRecording?.id == item.id) VibrantLavender
                            else DarkCardBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play / Pause Circle
                                    IconButton(
                                        onClick = { viewModel.playRecording(item) },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(VibrantLavender.copy(alpha = 0.2f))
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentlyPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = VibrantLavender,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(100.dp),
                                                color = VibrantLavender.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = effectType.title,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VibrantLavender,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${formatDuration(item.durationMs)} • ${formatFileSize(item.fileSize)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.toggleFavorite(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Star",
                                        tint = if (item.isFavorite) VibrantWarningAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action Buttons Row (Re-apply effect, Share, Rename, Delete)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Change Effect Button
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = VibrantLavender.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { recordingToApplyEffect = item }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = VibrantLavender,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Change FX",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VibrantLavender
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Share
                                IconButton(
                                    onClick = { viewModel.shareRecording(context, item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Rename
                                IconButton(
                                    onClick = {
                                        recordingToRename = item
                                        renameText = item.title
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete
                                IconButton(
                                    onClick = { viewModel.deleteRecording(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = VibrantCrimsonBright.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    recordingToRename?.let { item ->
        AlertDialog(
            onDismissRequest = { recordingToRename = null },
            title = { Text("Rename Recording", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameRecording(item.id, renameText)
                        recordingToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantLavender, contentColor = VibrantDeepPurple)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Change / Apply Voice Effect Modal Bottom Sheet
    recordingToApplyEffect?.let { item ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { recordingToApplyEffect = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Apply Voice Effect to Recording",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Transform '${item.title}' and export a new audio clip with any effect:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(VoiceEffectType.entries) { effect ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, DarkCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val rec = recordingToApplyEffect
                                    recordingToApplyEffect = null
                                    if (rec != null) {
                                        viewModel.reApplyEffectAndExport(rec, effect)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = effect.themeColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = effect.icon,
                                            contentDescription = effect.title,
                                            tint = effect.themeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = effect.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = effect.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "Apply",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = VibrantLavender
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun formatDuration(ms: Int): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
    }
}
