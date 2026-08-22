package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Player and converter for recorded and imported audio files with real-time dynamic DSP effect preview.
 */
class AudioPlayerManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var playbackJob: Job? = null
    private var audioTrack: AudioTrack? = null

    // Currently loaded audio file and raw PCM buffer
    private var currentFile: File? = null
    private var originalPcmSamples: ShortArray? = null
    private var sampleRate: Int = 44100
    private var channels: Int = 1

    // State flows
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0)
    val totalDurationMs = _totalDurationMs.asStateFlow()

    private val _playerVisualizerBars = MutableStateFlow(List(16) { 0.08f })
    val playerVisualizerBars = _playerVisualizerBars.asStateFlow()

    var activeParams: AudioEffectParams = AudioEffectParams()
        @Synchronized set

    /**
     * Loads a WAV audio file into memory for instant DSP playback.
     */
    fun loadFile(file: File): Boolean {
        stop()
        if (!file.exists()) return false

        try {
            val stream = FileInputStream(file)
            val header = ByteArray(44)
            val read = stream.read(header)
            if (read < 44) {
                stream.close()
                return false
            }

            sampleRate = ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
            channels = ByteBuffer.wrap(header, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            if (sampleRate !in 8000..96000) sampleRate = 44100
            if (channels < 1) channels = 1

            val pcmBytes = stream.readBytes()
            stream.close()

            val shortCount = pcmBytes.size / 2
            val shorts = ShortArray(shortCount)
            ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

            originalPcmSamples = shorts
            currentFile = file

            val durationMs = ((shorts.size.toDouble() / (sampleRate * channels)) * 1000).toInt()
            _totalDurationMs.value = durationMs
            _currentPositionMs.value = 0
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Play or resume audio with current effect DSP parameters
     */
    @Synchronized
    fun play(fromPositionMs: Int = _currentPositionMs.value) {
        val samples = originalPcmSamples ?: return
        if (samples.isEmpty()) return

        stop()

        val startSample = ((fromPositionMs / 1000.0) * sampleRate * channels).toInt().coerceIn(0, samples.size - 1)

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = max(minBufSize * 2, 4096)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack?.play()
        _isPlaying.value = true

        playbackJob = scope.launch {
            val chunkSize = 2048
            val dspState = DspState(sampleRate)
            var currentIdx = startSample
            val outBuffer = ShortArray(chunkSize)

            while (isActive && _isPlaying.value && currentIdx < samples.size) {
                val params = activeParams
                val effectiveSpeed = if (params.speedFactor > 0.3f) params.speedFactor else 1.0f

                val len = min(chunkSize, samples.size - currentIdx)
                val chunk = samples.copyOfRange(currentIdx, currentIdx + len)

                // Resample for speed if needed
                val resampledChunk = if (effectiveSpeed != 1.0f) {
                    AudioDspEngine.applyTimeSpeed(chunk, effectiveSpeed)
                } else {
                    chunk
                }

                val processed = AudioDspEngine.processPcm(
                    input = resampledChunk,
                    params = params,
                    state = dspState,
                    output = if (resampledChunk.size == chunkSize) outBuffer else null
                )

                audioTrack?.write(processed, 0, processed.size)

                currentIdx += len
                val currentMs = ((currentIdx.toDouble() / (sampleRate * channels)) * 1000).toInt()
                _currentPositionMs.value = min(currentMs, _totalDurationMs.value)

                // Compute visualizer
                computeVisualizer(processed)
            }

            if (currentIdx >= samples.size) {
                withContext(Dispatchers.Main) {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0
                    _playerVisualizerBars.value = List(16) { 0.08f }
                }
            }
        }
    }

    private fun computeVisualizer(samples: ShortArray) {
        val barCount = 16
        val sliceSize = samples.size / barCount
        if (sliceSize > 0) {
            val bars = List(barCount) { idx ->
                var sum = 0.0
                val start = idx * sliceSize
                for (s in start until min(start + sliceSize, samples.size)) {
                    sum += abs(samples[s].toDouble())
                }
                val avg = (sum / sliceSize) / 10000.0
                avg.toFloat().coerceIn(0.08f, 1.0f)
            }
            _playerVisualizerBars.value = bars
        }
    }

    @Synchronized
    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            // ignore
        }
    }

    @Synchronized
    fun stop() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioTrack = null
        _playerVisualizerBars.value = List(16) { 0.08f }
    }

    fun seekTo(positionMs: Int) {
        val wasPlaying = _isPlaying.value
        _currentPositionMs.value = positionMs.coerceIn(0, _totalDurationMs.value)
        if (wasPlaying) {
            play(positionMs)
        }
    }

    /**
     * Decodes any external audio file (MP3, M4A, AAC, WAV, OGG) via MediaExtractor & MediaCodec into 16-bit PCM WAV.
     */
    suspend fun importAudioUriToWav(uri: Uri, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                extractor.release()
                return@withContext false
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/mp4a-latm"
            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val tempPcmFile = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}.pcm")
            val pcmOut = FileOutputStream(tempPcmFile)

            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false
            val timeoutUs = 5000L

            while (!isEos) {
                val inIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEos = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                var outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                while (outIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outputBuffer.get(chunk)

                        // Convert stereo to mono if needed
                        if (channelCount == 2) {
                            val monoBytes = ByteArray(chunk.size / 2)
                            var m = 0
                            var c = 0
                            while (c < chunk.size - 3) {
                                val left = (chunk[c].toInt() and 0xff) or (chunk[c + 1].toInt() shl 8)
                                val right = (chunk[c + 2].toInt() and 0xff) or (chunk[c + 3].toInt() shl 8)
                                val mono = ((left + right) / 2).toShort()
                                monoBytes[m] = (mono.toInt() and 0xff).toByte()
                                monoBytes[m + 1] = ((mono.toInt() shr 8) and 0xff).toByte()
                                m += 2
                                c += 4
                            }
                            pcmOut.write(monoBytes, 0, m)
                        } else {
                            pcmOut.write(chunk)
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEos = true
                        break
                    }
                    outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                }
            }

            pcmOut.flush()
            pcmOut.close()

            // Convert raw PCM to standard WAV
            val rawBytes = tempPcmFile.readBytes()
            val shortCount = rawBytes.size / 2
            val shorts = ShortArray(shortCount)
            ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

            AudioDspEngine.writeWavFile(outputFile, shorts, sampleRate, channels = 1)
            tempPcmFile.delete()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                codec?.stop()
                codec?.release()
                extractor.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun release() {
        stop()
    }
}
