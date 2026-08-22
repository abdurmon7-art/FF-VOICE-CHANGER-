package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
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
 * Controller for real-time microphone voice transformation, live playback monitoring,
 * and concurrent high-quality audio recording.
 */
class LiveVoiceProcessor(private val context: Context) {

    private val sampleRate = AudioDspEngine.DEFAULT_SAMPLE_RATE
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private var processingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Current active effect parameters
    var currentParams: AudioEffectParams = AudioEffectParams()
        @Synchronized set

    // State flows for UI
    private val _isLiveListening = MutableStateFlow(false)
    val isLiveListening = _isLiveListening.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec = _recordingDurationSec.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude = _amplitude.asStateFlow()

    private val _visualizerBars = MutableStateFlow(List(16) { 0.1f })
    val visualizerBars = _visualizerBars.asStateFlow()

    // Recording storage
    private var activeRecordingFile: File? = null
    private var activePcmStream: FileOutputStream? = null
    private var recordedSamplesCount = 0L
    private var recordingStartTime = 0L

    /**
     * Start live microphone processing.
     * @param enableSpeakerPlayback if true, outputs transformed mic audio to speaker/headphones
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    fun startLiveProcessing(enableSpeakerPlayback: Boolean = true): Boolean {
        if (_isLiveListening.value && processingJob?.isActive == true) {
            return true
        }

        try {
            val minBufSizeIn = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
            val minBufSizeOut = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)

            if (minBufSizeIn <= 0 || minBufSizeOut <= 0) return false

            val inBufferSize = max(minBufSizeIn * 2, 4096)
            val outBufferSize = max(minBufSizeOut * 2, 4096)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfigIn,
                audioFormat,
                inBufferSize
            )

            // Fallback to MIC if VOICE_COMMUNICATION fails
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

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
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

            startProcessingLoop(enableSpeakerPlayback)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            stopLiveProcessing()
            return false
        }
    }

    private fun startProcessingLoop(enableSpeakerPlayback: Boolean) {
        processingJob?.cancel()
        processingJob = scope.launch {
            val chunkSize = 1024
            val inBuffer = ShortArray(chunkSize)
            val outBuffer = ShortArray(chunkSize)
            val dspState = DspState(sampleRate)
            val byteBuffer = ByteArray(chunkSize * 2)

            while (isActive && _isLiveListening.value) {
                val record = audioRecord ?: break
                val read = record.read(inBuffer, 0, chunkSize)
                if (read > 0) {
                    val params = currentParams
                    val processedChunk = AudioDspEngine.processPcm(
                        input = if (read == chunkSize) inBuffer else inBuffer.copyOf(read),
                        params = params,
                        state = dspState,
                        output = outBuffer
                    )

                    // Stream to speaker if enabled
                    if (enableSpeakerPlayback) {
                        audioTrack?.write(processedChunk, 0, read)
                    }

                    // Save to active recording if recording is on
                    if (_isRecording.value) {
                        saveChunkToRecording(processedChunk, read)
                    }

                    // Compute amplitude and visualizer bars
                    computeVisualizer(processedChunk, read)
                }
            }
        }
    }

    private fun computeVisualizer(samples: ShortArray, length: Int) {
        var sumSquares = 0.0
        var peak = 0
        for (i in 0 until length) {
            val sample = abs(samples[i].toInt())
            if (sample > peak) peak = sample
            sumSquares += (sample * sample)
        }

        val rms = sqrt(sumSquares / length)
        val normalizedAmp = (rms / 12000.0).toFloat().coerceIn(0.02f, 1.0f)
        _amplitude.value = normalizedAmp

        // Pseudo spectrum bars using sub-slices
        val barCount = 16
        val sliceSize = length / barCount
        if (sliceSize > 0) {
            val bars = List(barCount) { idx ->
                var sliceSum = 0.0
                val start = idx * sliceSize
                for (s in start until min(start + sliceSize, length)) {
                    sliceSum += abs(samples[s].toDouble())
                }
                val avg = (sliceSum / sliceSize) / 10000.0
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

        // Auto start mic if not listening
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
        processingJob?.cancel()
        processingJob = null

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

        _amplitude.value = 0f
        _visualizerBars.value = List(16) { 0.08f }
    }

    fun release() {
        stopRecording(File(context.cacheDir, "discard.wav"))
        stopLiveProcessing()
    }
}
