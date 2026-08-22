package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Controller for real-time low-latency microphone voice transformation, live playback monitoring,
 * hardware acoustic echo cancellation, and concurrent high-quality audio recording.
 */
class LiveVoiceProcessor(
    private val context: Context,
    val audioDeviceManager: AudioDeviceManager
) {

    private val sampleRate = AudioDspEngine.DEFAULT_SAMPLE_RATE
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    // Hardware acoustic enhancement effects
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var gainControl: AutomaticGainControl? = null

    private var processingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Current active effect parameters
    var currentParams: AudioEffectParams = AudioEffectParams()
        @Synchronized set

    // State flows for UI & Diagnostics
    private val _isLiveListening = MutableStateFlow(false)
    val isLiveListening = _isLiveListening.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec = _recordingDurationSec.asStateFlow()

    private val _inputAmplitude = MutableStateFlow(0f)
    val inputAmplitude = _inputAmplitude.asStateFlow()

    private val _outputAmplitude = MutableStateFlow(0f)
    val outputAmplitude = _outputAmplitude.asStateFlow()

    private val _visualizerBars = MutableStateFlow(List(16) { 0.08f })
    val visualizerBars = _visualizerBars.asStateFlow()

    private val _measuredLatencyMs = MutableStateFlow(16)
    val measuredLatencyMs = _measuredLatencyMs.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    // Recording storage
    private var activeRecordingFile: File? = null
    private var activePcmStream: FileOutputStream? = null
    private var recordedSamplesCount = 0L
    private var recordingStartTime = 0L

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
    }

    /**
     * Start live low-latency microphone processing.
     * @param enableSpeakerPlayback if true, outputs transformed mic audio to selected audio route
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun startLiveProcessing(enableSpeakerPlayback: Boolean = true): Boolean {
        if (_isLiveListening.value && processingJob?.isActive == true) {
            return true
        }

        try {
            audioDeviceManager.requestAudioFocus()

            val minBufSizeIn = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
            val minBufSizeOut = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)

            if (minBufSizeIn <= 0 || minBufSizeOut <= 0) return false

            val inBufferSize = max(minBufSizeIn * 2, 2048)
            val outBufferSize = max(minBufSizeOut * 2, 2048)

            // Prefer VOICE_COMMUNICATION for automatic hardware echo cancellation and low latency
            audioRecord = try {
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    inBufferSize
                )
            } catch (e: Exception) {
                null
            }

            // Fallback to MIC if VOICE_COMMUNICATION fails or is uninitialized
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfigIn,
                    audioFormat,
                    inBufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return false
            }

            val sessionId = audioRecord?.audioSessionId ?: 0
            if (sessionId != 0) {
                attachHardwareAudioFx(sessionId)
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setFlags(AudioAttributes.FLAG_LOW_LATENCY)
                    }
                }
                .build()

            val audioFormatSpec = AudioFormat.Builder()
                .setEncoding(audioFormat)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfigOut)
                .build()

            audioTrack = AudioTrack(
                audioAttributes,
                audioFormatSpec,
                outBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            audioRecord?.startRecording()
            audioTrack?.play()

            _isLiveListening.value = true
            audioDeviceManager.setVoiceServiceRunning(true)

            startProcessingLoop(enableSpeakerPlayback)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            stopLiveProcessing()
            return false
        }
    }

    private fun attachHardwareAudioFx(sessionId: Int) {
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
                    enabled = true
                }
            }
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
                    enabled = true
                }
            }
            if (AutomaticGainControl.isAvailable()) {
                gainControl = AutomaticGainControl.create(sessionId)?.apply {
                    enabled = true
                }
            }
        } catch (e: Exception) {
            // Hardware effects not available on some emulators/devices
        }
    }

    private fun releaseHardwareAudioFx() {
        try {
            echoCanceler?.release()
            noiseSuppressor?.release()
            gainControl?.release()
        } catch (e: Exception) {
            // ignore
        }
        echoCanceler = null
        noiseSuppressor = null
        gainControl = null
    }

    private fun startProcessingLoop(enableSpeakerPlayback: Boolean) {
        processingJob?.cancel()
        processingJob = scope.launch {
            // Use optimal low-latency buffer chunk (512 samples ~= 11.6ms at 44.1kHz)
            val chunkSize = 512
            val inBuffer = ShortArray(chunkSize)
            val outBuffer = ShortArray(chunkSize)
            val dspState = DspState(sampleRate)

            var cycleCount = 0
            var accumulatedLatencyUs = 0L

            while (isActive && _isLiveListening.value) {
                val record = audioRecord ?: break
                val startCycleTime = SystemClock.elapsedRealtimeNanos()

                val read = record.read(inBuffer, 0, chunkSize)
                if (read > 0) {
                    // Check if muted
                    if (_isMuted.value) {
                        inBuffer.fill(0, 0, read)
                    }

                    // Compute input amplitude
                    val inAmp = computeRms(inBuffer, read)
                    _inputAmplitude.value = inAmp

                    val params = currentParams
                    val processedChunk = AudioDspEngine.processPcm(
                        input = if (read == chunkSize) inBuffer else inBuffer.copyOf(read),
                        params = params,
                        state = dspState,
                        output = outBuffer
                    )

                    // Write to AudioTrack if playback is enabled
                    if (enableSpeakerPlayback) {
                        audioTrack?.write(processedChunk, 0, read)
                    }

                    // Save to active recording if recording is enabled
                    if (_isRecording.value) {
                        saveChunkToRecording(processedChunk, read)
                    }

                    // Compute output amplitude and visualizer
                    val outAmp = computeRms(processedChunk, read)
                    _outputAmplitude.value = outAmp
                    computeVisualizer(processedChunk, read)

                    val endCycleTime = SystemClock.elapsedRealtimeNanos()
                    val cycleDurationMs = ((endCycleTime - startCycleTime) / 1_000_000L).toInt() + 10 // hardware roundtrip estimate
                    accumulatedLatencyUs += cycleDurationMs
                    cycleCount++

                    if (cycleCount >= 20) {
                        val avgLatency = (accumulatedLatencyUs / cycleCount).toInt().coerceIn(8, 90)
                        _measuredLatencyMs.value = avgLatency
                        audioDeviceManager.setLatencyMeasurement(avgLatency)
                        cycleCount = 0
                        accumulatedLatencyUs = 0
                    }
                }
            }
        }
    }

    private fun computeRms(samples: ShortArray, length: Int): Float {
        var sumSquares = 0.0
        for (i in 0 until length) {
            val sample = samples[i].toInt()
            sumSquares += (sample * sample)
        }
        val rms = sqrt(sumSquares / length)
        return (rms / 14000.0).toFloat().coerceIn(0.0f, 1.0f)
    }

    private fun computeVisualizer(samples: ShortArray, length: Int) {
        val barCount = 16
        val sliceSize = length / barCount
        if (sliceSize > 0) {
            val bars = List(barCount) { idx ->
                var sliceSum = 0.0
                val start = idx * sliceSize
                for (s in start until min(start + sliceSize, length)) {
                    sliceSum += abs(samples[s].toDouble())
                }
                val avg = (sliceSum / sliceSize) / 9000.0
                avg.toFloat().coerceIn(0.08f, 1.0f)
            }
            _visualizerBars.value = bars
        }
    }

    @Synchronized
    private fun saveChunkToRecording(samples: ShortArray, length: Int) {
        try {
            val stream = activePcmStream ?: return
            val byteBuffer = ByteBuffer.allocate(length * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until length) {
                byteBuffer.putShort(samples[i])
            }
            stream.write(byteBuffer.array())
            recordedSamplesCount += length

            val elapsedSec = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
            _recordingDurationSec.value = elapsedSec
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Starts recording the live transformed audio to a temporary file.
     */
    @Synchronized
    fun startRecording(targetDir: File): File? {
        if (_isRecording.value) return activeRecordingFile

        if (!_isLiveListening.value) {
            startLiveProcessing(enableSpeakerPlayback = false)
        }

        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val tempPcm = File(targetDir, "temp_rec_${System.currentTimeMillis()}.pcm")
            activePcmStream = FileOutputStream(tempPcm)
            activeRecordingFile = tempPcm
            recordedSamplesCount = 0L
            recordingStartTime = System.currentTimeMillis()
            _recordingDurationSec.value = 0
            _isRecording.value = true
            return tempPcm
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Stops recording and saves the audio as a standard WAV file.
     */
    @Synchronized
    fun stopRecording(finalWavFile: File): File? {
        if (!_isRecording.value) return null

        _isRecording.value = false
        val pcmFile = activeRecordingFile
        try {
            activePcmStream?.flush()
            activePcmStream?.close()
            activePcmStream = null

            if (pcmFile != null && pcmFile.exists() && recordedSamplesCount > 0) {
                val pcmBytes = pcmFile.readBytes()
                val shortCount = pcmBytes.size / 2
                val shortBuffer = ShortArray(shortCount)
                ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer)

                AudioDspEngine.writeWavFile(finalWavFile, shortBuffer, sampleRate, channels = 1)
                pcmFile.delete()
                activeRecordingFile = null
                return finalWavFile
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pcmFile?.delete()
            activeRecordingFile = null
        }
        return null
    }

    @Synchronized
    fun stopLiveProcessing() {
        _isLiveListening.value = false
        audioDeviceManager.setVoiceServiceRunning(false)
        processingJob?.cancel()
        processingJob = null

        releaseHardwareAudioFx()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioTrack = null

        audioDeviceManager.abandonAudioFocus()

        _inputAmplitude.value = 0f
        _outputAmplitude.value = 0f
        _visualizerBars.value = List(16) { 0.08f }
    }

    fun release() {
        stopRecording(File(context.cacheDir, "discard.wav"))
        stopLiveProcessing()
    }
}
