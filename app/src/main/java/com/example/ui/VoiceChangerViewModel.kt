package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VoiceChangerApp
import com.example.apps.AppInfo
import com.example.apps.InstalledAppsManager
import com.example.audio.AudioEffectParams
import com.example.audio.AudioPlayerManager
import com.example.audio.LiveVoiceProcessor
import com.example.audio.VoiceEffectType
import com.example.data.RecordingDao
import com.example.data.RecordingItem
import com.example.data.VoicePreferences
import com.example.service.VoiceOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab {
    STUDIO,
    RECORDINGS,
    APPS_OVERLAY
}

class VoiceChangerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VoiceChangerApp
    private val liveProcessor: LiveVoiceProcessor = app.liveVoiceProcessor
    val playerManager: AudioPlayerManager = app.audioPlayerManager
    private val recordingDao: RecordingDao = app.database.recordingDao()
    val preferences: VoicePreferences = app.preferences

    // Tab Navigation
    private val _currentTab = MutableStateFlow(AppTab.STUDIO)
    val currentTab = _currentTab.asStateFlow()

    // Selected Voice Effect
    private val _selectedEffect = MutableStateFlow(VoiceEffectType.NORMAL)
    val selectedEffect = _selectedEffect.asStateFlow()

    // Custom Sliders Parameters
    private val _customParams = MutableStateFlow(preferences.loadCustomParams())
    val customParams = _customParams.asStateFlow()

    // Live Mic / Recording States from LiveVoiceProcessor
    val isLiveListening = liveProcessor.isLiveListening
    val isRecording = liveProcessor.isRecording
    val recordingDurationSec = liveProcessor.recordingDurationSec
    val liveAmplitude = liveProcessor.amplitude
    val liveVisualizerBars = liveProcessor.visualizerBars

    // Player States from AudioPlayerManager
    val isPlaying = playerManager.isPlaying
    val playerPositionMs = playerManager.currentPositionMs
    val playerTotalMs = playerManager.totalDurationMs
    val playerVisualizerBars = playerManager.playerVisualizerBars
    private val _activePlayingRecording = MutableStateFlow<RecordingItem?>(null)
    val activePlayingRecording = _activePlayingRecording.asStateFlow()

    // Recordings Flow
    private val _recordingsSearchQuery = MutableStateFlow("")
    val recordingsSearchQuery = _recordingsSearchQuery.asStateFlow()

    val recordingsList: StateFlow<List<RecordingItem>> = recordingDao.getAllRecordings()
        .combine(_recordingsSearchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter { it.title.contains(query, ignoreCase = true) || it.effectType.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Installed Apps
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _appsSearchQuery = MutableStateFlow("")
    val appsSearchQuery = _appsSearchQuery.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = _installedApps
        .combine(_appsSearchQuery) { apps, query ->
            if (query.isBlank()) apps
            else apps.filter { it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOverlayActive = preferences.isOverlayActive
    val favoriteEffects = preferences.favoriteEffects
    val isDarkMode = preferences.isDarkMode

    // Processing / Exporting State
    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress = _exportProgress.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    init {
        loadInstalledApps()
        updateDspParameters()
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectVoiceEffect(effect: VoiceEffectType) {
        _selectedEffect.value = effect
        updateDspParameters()
    }

    fun updateCustomParams(newParams: AudioEffectParams) {
        _customParams.value = newParams
        preferences.saveCustomParams(newParams)
        if (_selectedEffect.value == VoiceEffectType.CUSTOM) {
            updateDspParameters()
        }
    }

    private fun updateDspParameters() {
        val effect = _selectedEffect.value
        val params = if (effect == VoiceEffectType.CUSTOM) {
            _customParams.value
        } else {
            effect.defaultParams
        }
        liveProcessor.currentParams = params
        playerManager.activeParams = params
    }

    fun toggleLiveMic() {
        if (isLiveListening.value) {
            liveProcessor.stopLiveProcessing()
        } else {
            val ok = liveProcessor.startLiveProcessing(enableSpeakerPlayback = true)
            if (!ok) {
                _userMessage.value = "Microphone initialization failed. Please check permissions."
            }
        }
    }

    fun toggleRecording() {
        if (isRecording.value) {
            val dir = File(getApplication<VoiceChangerApp>().filesDir, "recordings").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val effectName = _selectedEffect.value.title
            val finalWav = File(dir, "Voice_${effectName}_$timeStamp.wav")

            val saved = liveProcessor.stopRecording(finalWav)
            if (saved != null) {
                val durationMs = liveProcessor.recordingDurationSec.value * 1000
                viewModelScope.launch {
                    val entity = RecordingItem(
                        title = "Voice $effectName ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())}",
                        filePath = saved.absolutePath,
                        effectType = _selectedEffect.value.name,
                        durationMs = durationMs,
                        fileSize = saved.length()
                    )
                    recordingDao.insertRecording(entity)
                    _userMessage.value = "Recording saved successfully!"
                }
            } else {
                _userMessage.value = "Could not save recording."
            }
        } else {
            val dir = File(getApplication<VoiceChangerApp>().filesDir, "recordings").apply { mkdirs() }
            val started = liveProcessor.startRecording(dir)
            if (started == null) {
                _userMessage.value = "Failed to start recording."
            }
        }
    }

    fun playRecording(recording: RecordingItem) {
        val file = File(recording.filePath)
        if (!file.exists()) {
            _userMessage.value = "Recording file not found on device."
            return
        }

        if (_activePlayingRecording.value?.id == recording.id && isPlaying.value) {
            playerManager.pause()
        } else {
            _activePlayingRecording.value = recording
            // Match effect of the recording or allow current active effect
            val loaded = playerManager.loadFile(file)
            if (loaded) {
                playerManager.play()
            } else {
                _userMessage.value = "Error reading audio file."
            }
        }
    }

    fun seekPlayer(posMs: Int) {
        playerManager.seekTo(posMs)
    }

    fun toggleFavorite(recording: RecordingItem) {
        viewModelScope.launch {
            recordingDao.toggleFavorite(recording.id, !recording.isFavorite)
        }
    }

    fun toggleFavoriteEffect(effectName: String) {
        preferences.toggleFavoriteEffect(effectName)
    }

    fun renameRecording(id: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            recordingDao.renameRecording(id, newTitle.trim())
        }
    }

    fun deleteRecording(recording: RecordingItem) {
        viewModelScope.launch {
            if (_activePlayingRecording.value?.id == recording.id) {
                playerManager.stop()
                _activePlayingRecording.value = null
            }
            try {
                val file = File(recording.filePath)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                // ignore
            }
            recordingDao.deleteRecording(recording)
            _userMessage.value = "Recording deleted."
        }
    }

    fun shareRecording(context: Context, recording: RecordingItem) {
        try {
            val file = File(recording.filePath)
            if (!file.exists()) {
                _userMessage.value = "File does not exist."
                return
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, recording.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Voice Clip via"))
        } catch (e: Exception) {
            e.printStackTrace()
            _userMessage.value = "Error sharing audio file."
        }
    }

    fun importAudioFile(uri: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            val dir = File(getApplication<VoiceChangerApp>().filesDir, "recordings").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val importedWav = File(dir, "Imported_$timeStamp.wav")

            val ok = playerManager.importAudioUriToWav(uri, importedWav)
            _isExporting.value = false
            if (ok) {
                val duration = try {
                    val pcmBytes = importedWav.length() - 44
                    ((pcmBytes.toDouble() / (44100 * 2)) * 1000).toInt()
                } catch (e: Exception) { 0 }

                val item = RecordingItem(
                    title = "Imported Audio $timeStamp",
                    filePath = importedWav.absolutePath,
                    effectType = VoiceEffectType.NORMAL.name,
                    durationMs = duration,
                    fileSize = importedWav.length(),
                    isImported = true
                )
                recordingDao.insertRecording(item)
                _userMessage.value = "Audio imported successfully! You can now apply any voice effect."
                _currentTab.value = AppTab.RECORDINGS
            } else {
                _userMessage.value = "Failed to decode imported audio format."
            }
        }
    }

    fun reApplyEffectAndExport(recording: RecordingItem, effect: VoiceEffectType) {
        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            _exportProgress.value = 0f
            val sourceFile = File(recording.filePath)
            if (!sourceFile.exists()) {
                _isExporting.value = false
                _userMessage.value = "Source file missing."
                return@launch
            }

            val dir = File(getApplication<VoiceChangerApp>().filesDir, "recordings").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val targetFile = File(dir, "${recording.title}_${effect.title}_$timeStamp.wav")

            val params = if (effect == VoiceEffectType.CUSTOM) _customParams.value else effect.defaultParams
            val success = com.example.audio.AudioDspEngine.processAudioFile(
                inputFile = sourceFile,
                outputFile = targetFile,
                params = params,
                onProgress = { p -> _exportProgress.value = p }
            )

            _isExporting.value = false
            if (success) {
                val newItem = RecordingItem(
                    title = "${recording.title} (${effect.title})",
                    filePath = targetFile.absolutePath,
                    effectType = effect.name,
                    durationMs = recording.durationMs,
                    fileSize = targetFile.length()
                )
                recordingDao.insertRecording(newItem)
                _userMessage.value = "New effect version saved to Recordings!"
            } else {
                _userMessage.value = "Export failed."
            }
        }
    }

    fun toggleOverlay(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            _userMessage.value = "Please grant 'Display over other apps' permission to enable the floating button."
            return
        }

        val willActivate = !isOverlayActive.value
        preferences.setOverlayActive(willActivate)
        if (willActivate) {
            VoiceOverlayService.start(context)
            _userMessage.value = "Floating quick controller enabled!"
        } else {
            VoiceOverlayService.stop(context)
            _userMessage.value = "Floating controller hidden."
        }
    }

    fun launchAppWithOverlay(context: Context, appInfo: AppInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            _userMessage.value = "Grant overlay permission first to use the floating voice controller with ${appInfo.appName}!"
            return
        }

        // Start overlay service
        preferences.setOverlayActive(true)
        VoiceOverlayService.start(context)

        // Launch target app
        appInfo.launchIntent?.let { intent ->
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                _userMessage.value = "Could not launch ${appInfo.appName}."
            }
        }
    }

    fun searchRecordings(query: String) {
        _recordingsSearchQuery.value = query
    }

    fun searchApps(query: String) {
        _appsSearchQuery.value = query
    }

    fun toggleDarkMode() {
        preferences.setDarkMode(!isDarkMode.value)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val list = InstalledAppsManager.getInstalledApps(getApplication())
            _installedApps.value = list
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveProcessor.release()
        playerManager.release()
    }
}
