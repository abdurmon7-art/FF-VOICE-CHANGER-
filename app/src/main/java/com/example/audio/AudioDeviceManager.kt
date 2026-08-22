package com.example.audio

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OutputRouteMode(val label: String, val description: String) {
    AUTO("Auto Default", "Automatically routes to active headphones, Bluetooth, or speaker"),
    SPEAKER("Loudspeaker", "Force audio through built-in phone speaker"),
    HEADSET_PASSTHROUGH("Headset Passthrough", "Low-latency pass-through to wired headset / AUX line"),
    BLUETOOTH_SCO("Bluetooth Hands-Free", "Routes mic audio to Bluetooth headset SCO channel")
}

data class AudioDiagnosticsState(
    val hasMicPermission: Boolean = false,
    val isMicAvailable: Boolean = true,
    val isAnotherAppRecording: Boolean = false,
    val isClientSilenced: Boolean = false,
    val isVoiceServiceRunning: Boolean = false,
    val activeInputDevice: String = "Detecting...",
    val activeOutputDevice: String = "Detecting...",
    val inputDeviceType: Int = AudioDeviceInfo.TYPE_BUILTIN_MIC,
    val outputDeviceType: Int = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
    val isHeadsetConnected: Boolean = false,
    val isBluetoothConnected: Boolean = false,
    val isBluetoothScoActive: Boolean = false,
    val sampleRateHz: Int = 44100,
    val bufferSizeFrames: Int = 256,
    val latencyMs: Int = 18,
    val audioMode: Int = AudioManager.MODE_NORMAL,
    val routeMode: OutputRouteMode = OutputRouteMode.AUTO
)

/**
 * Robust audio device routing, focus management, and concurrency detection manager.
 */
class AudioDeviceManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _diagnostics = MutableStateFlow(AudioDiagnosticsState())
    val diagnostics = _diagnostics.asStateFlow()

    private var audioDeviceCallback: AudioDeviceCallback? = null
    private var recordingCallback: AudioManager.AudioRecordingCallback? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private var isScoStarted = false

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    refreshDeviceRouting()
                }
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                    val state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR)
                    _diagnostics.value = _diagnostics.value.copy(
                        isBluetoothScoActive = state == AudioManager.SCO_AUDIO_STATE_CONNECTED
                    )
                    refreshDeviceRouting()
                }
            }
        }
    }

    init {
        registerCallbacks()
        refreshDeviceRouting()
    }

    private fun registerCallbacks() {
        // Register Headset & SCO BroadcastReceiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        }
        context.registerReceiver(headsetReceiver, filter)

        // Register AudioDeviceCallback for real-time peripheral detection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    refreshDeviceRouting()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    refreshDeviceRouting()
                }
            }
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        }

        // Register AudioRecordingCallback on Android 10+ (API 29+) to detect active/concurrent recordings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            recordingCallback = object : AudioManager.AudioRecordingCallback() {
                override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
                    val totalConfigs = configs?.size ?: 0
                    var anotherAppRecording = false
                    var clientSilenced = false

                    if (configs != null) {
                        for (config in configs) {
                            if (config.isClientSilenced) {
                                clientSilenced = true
                            }
                            // If there are multiple capture configs active or non-matching UID
                            if (totalConfigs > 1) {
                                anotherAppRecording = true
                            }
                        }
                    }

                    _diagnostics.value = _diagnostics.value.copy(
                        isAnotherAppRecording = anotherAppRecording,
                        isClientSilenced = clientSilenced,
                        isMicAvailable = !clientSilenced
                    )
                }
            }
            audioManager.registerAudioRecordingCallback(recordingCallback!!, null)
        }
    }

    fun setVoiceServiceRunning(running: Boolean) {
        _diagnostics.value = _diagnostics.value.copy(isVoiceServiceRunning = running)
    }

    fun setLatencyMeasurement(ms: Int) {
        _diagnostics.value = _diagnostics.value.copy(latencyMs = ms)
    }

    fun setRouteMode(mode: OutputRouteMode) {
        _diagnostics.value = _diagnostics.value.copy(routeMode = mode)
        applyRouteMode(mode)
    }

    private fun applyRouteMode(mode: OutputRouteMode) {
        try {
            when (mode) {
                OutputRouteMode.AUTO -> {
                    stopBluetoothSco()
                    audioManager.isSpeakerphoneOn = false
                    audioManager.mode = AudioManager.MODE_NORMAL
                }
                OutputRouteMode.SPEAKER -> {
                    stopBluetoothSco()
                    audioManager.mode = AudioManager.MODE_NORMAL
                    audioManager.isSpeakerphoneOn = true
                }
                OutputRouteMode.HEADSET_PASSTHROUGH -> {
                    stopBluetoothSco()
                    audioManager.isSpeakerphoneOn = false
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                }
                OutputRouteMode.BLUETOOTH_SCO -> {
                    startBluetoothSco()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        refreshDeviceRouting()
    }

    private fun startBluetoothSco() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.startBluetoothSco()
            audioManager.isBluetoothScoOn = true
            isScoStarted = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopBluetoothSco() {
        if (isScoStarted) {
            try {
                audioManager.isBluetoothScoOn = false
                audioManager.stopBluetoothSco()
                isScoStarted = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun requestAudioFocus(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            // Focus lost
                        }
                    }
                    .build()

                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            false
        }
    }

    fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    @SuppressLint("NewApi")
    fun refreshDeviceRouting() {
        val hasMicPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        var inputDeviceName = "Built-in Microphone"
        var outputDeviceName = "Built-in Speaker"
        var inputType = AudioDeviceInfo.TYPE_BUILTIN_MIC
        var outputType = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        var hasHeadset = false
        var hasBluetooth = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)

            // Detect preferred input
            for (dev in inputDevices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        inputDeviceName = "Headset Microphone (${dev.productName.ifEmpty { "Wired" }})"
                        inputType = dev.type
                        hasHeadset = true
                        break
                    }
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        inputDeviceName = "Bluetooth Mic (${dev.productName.ifEmpty { "SCO" }})"
                        inputType = dev.type
                        hasBluetooth = true
                        break
                    }
                    AudioDeviceInfo.TYPE_USB_DEVICE -> {
                        inputDeviceName = "USB Audio Input (${dev.productName})"
                        inputType = dev.type
                        break
                    }
                }
            }

            // Detect preferred output
            for (dev in outputDevices) {
                when (dev.type) {
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        outputDeviceName = "Headphones / AUX Line (${dev.productName.ifEmpty { "Connected" }})"
                        outputType = dev.type
                        hasHeadset = true
                        break
                    }
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        outputDeviceName = "Bluetooth Audio (${dev.productName.ifEmpty { "Wireless" }})"
                        outputType = dev.type
                        hasBluetooth = true
                        break
                    }
                    AudioDeviceInfo.TYPE_USB_DEVICE -> {
                        outputDeviceName = "USB Audio Output (${dev.productName})"
                        outputType = dev.type
                        break
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (audioManager.isWiredHeadsetOn) {
                hasHeadset = true
                inputDeviceName = "Wired Headset Mic"
                outputDeviceName = "Wired Headphones"
            } else if (audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn) {
                hasBluetooth = true
                inputDeviceName = "Bluetooth Mic"
                outputDeviceName = "Bluetooth Audio"
            }
        }

        val optimalSampleRate = try {
            val sr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            sr?.toIntOrNull() ?: 44100
        } catch (e: Exception) {
            44100
        }

        val optimalBufferFrames = try {
            val fp = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            fp?.toIntOrNull() ?: 256
        } catch (e: Exception) {
            256
        }

        _diagnostics.value = _diagnostics.value.copy(
            hasMicPermission = hasMicPerm,
            activeInputDevice = inputDeviceName,
            activeOutputDevice = outputDeviceName,
            inputDeviceType = inputType,
            outputDeviceType = outputType,
            isHeadsetConnected = hasHeadset,
            isBluetoothConnected = hasBluetooth,
            sampleRateHz = optimalSampleRate,
            bufferSizeFrames = optimalBufferFrames,
            audioMode = audioManager.mode
        )
    }

    fun release() {
        try {
            context.unregisterReceiver(headsetReceiver)
        } catch (e: Exception) {
            // ignore
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && audioDeviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && recordingCallback != null) {
            audioManager.unregisterAudioRecordingCallback(recordingCallback!!)
        }
        stopBluetoothSco()
        abandonAudioFocus()
    }
}
