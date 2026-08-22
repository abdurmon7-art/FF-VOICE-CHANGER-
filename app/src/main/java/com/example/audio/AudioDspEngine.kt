package com.example.audio

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh

/**
 * State object holding circular buffers, delay lines, and phase accumulators for streaming DSP.
 */
class DspState(val sampleRate: Int = 44100) {
    // Delay / Echo circular buffer (up to 1.0 second)
    val maxDelaySamples = sampleRate
    val delayBuffer = FloatArray(maxDelaySamples)
    var delayWriteIndex = 0

    // Robot ring modulation phase
    var robotPhase = 0.0

    // Alien / Vibrato LFO phase
    var lfoPhase = 0.0

    // Biquad filter state
    var biquadX1 = 0f
    var biquadX2 = 0f
    var biquadY1 = 0f
    var biquadY2 = 0f

    // Tone shaping state
    var tonePrevSample = 0f

    // Pitch shifter granular / overlap-add state
    // ~35ms grain window for low-latency responsiveness
    val grainSize = (sampleRate * 0.035).toInt().coerceAtLeast(256)
    val halfGrain = grainSize / 2
    val grainWindow = FloatArray(grainSize) { i ->
        // Smooth Hann window with raised cosine
        (0.5 * (1.0 - cos(2.0 * PI * i / (grainSize - 1)))).toFloat()
    }
    val pitchBuffer = FloatArray(grainSize * 4)
    var pitchWriteIndex = 0
    var grainPhase1 = 0f
    var grainPhase2 = grainSize * 0.5f

    fun reset() {
        delayBuffer.fill(0f)
        delayWriteIndex = 0
        robotPhase = 0.0
        lfoPhase = 0.0
        biquadX1 = 0f
        biquadX2 = 0f
        biquadY1 = 0f
        biquadY2 = 0f
        tonePrevSample = 0f
        pitchBuffer.fill(0f)
        pitchWriteIndex = 0
        grainPhase1 = 0f
        grainPhase2 = grainSize * 0.5f
    }
}

/**
 * High-performance digital signal processing engine for real-time low-latency voice transformation.
 */
object AudioDspEngine {

    const val DEFAULT_SAMPLE_RATE = 44100
    const val BUFFER_SIZE_SAMPLES = 512

    /**
     * Process an array of 16-bit PCM audio samples with ultra-low latency.
     */
    fun processPcm(
        input: ShortArray,
        params: AudioEffectParams,
        state: DspState,
        output: ShortArray? = null
    ): ShortArray {
        val out = output ?: ShortArray(input.size)
        val sampleRate = state.sampleRate
        val length = input.size

        if (length == 0) return out

        // Step 1: Pitch Shifting / Granular Modulation
        val pitchShifted = FloatArray(length)
        val hasPitchShift = abs(params.pitchFactor - 1.0f) > 0.02f || params.alienWobbleDepth > 0f

        if (hasPitchShift) {
            applyPitchShift(input, pitchShifted, params, state)
        } else {
            for (i in 0 until length) {
                pitchShifted[i] = input[i].toFloat() / 32768.0f
            }
        }

        // Step 2: Robot Ring Modulation / Metallic synthesis
        if (params.robotMix > 0f && params.robotRingFreqHz > 0f) {
            val twoPiFreq = 2.0 * PI * params.robotRingFreqHz / sampleRate
            val mix = params.robotMix.coerceIn(0f, 1f)
            val dryMix = 1f - mix

            for (i in 0 until length) {
                val carrier = sin(state.robotPhase).toFloat()
                state.robotPhase += twoPiFreq
                if (state.robotPhase > 2 * PI) state.robotPhase -= 2 * PI

                val dry = pitchShifted[i]
                val wet = dry * carrier
                pitchShifted[i] = (dry * dryMix) + (wet * mix)
            }
        }

        // Step 3: Frequency Filtering (Radio bandpass or Tone shaping)
        if (params.bandpassCenterHz > 0f && params.bandpassWidthHz > 0f) {
            applyBandpassFilter(pitchShifted, params.bandpassCenterHz, params.bandpassWidthHz, sampleRate, state)
        } else if (params.lowShelfGain != 1.0f || params.highShelfGain != 1.0f) {
            applyToneShaping(pitchShifted, params.lowShelfGain, params.highShelfGain, state)
        }

        // Step 4: Overdrive / Distortion
        if (params.distortion > 0.01f) {
            val drive = 1.0f + params.distortion * 4.5f
            val gainCompensation = 1.0f / (1.0f + params.distortion * 1.5f)
            for (i in 0 until length) {
                val x = pitchShifted[i] * drive
                // Hyperbolic tangent soft clipping
                pitchShifted[i] = (tanh(x.toDouble()).toFloat() * gainCompensation)
            }
        }

        // Step 5: Delay & Echo Line
        if (params.echoDelayMs > 5 && params.echoFeedback > 0.05f) {
            val delaySamples = min((sampleRate * (params.echoDelayMs / 1000.0)).toInt(), state.maxDelaySamples - 1)
            val fb = min(params.echoFeedback, 0.80f)
            val delayBuf = state.delayBuffer
            val bufLen = delayBuf.size

            for (i in 0 until length) {
                val readIdx = (state.delayWriteIndex - delaySamples + bufLen) % bufLen
                val echoSample = delayBuf[readIdx]
                val dry = pitchShifted[i]
                val mixed = dry + echoSample * 0.65f

                // Feedback into circular buffer with low-pass damping
                delayBuf[state.delayWriteIndex] = (dry + echoSample * fb) * 0.95f
                state.delayWriteIndex = (state.delayWriteIndex + 1) % bufLen

                pitchShifted[i] = mixed
            }
        }

        // Step 6: Master Soft Limiter to prevent clipping and convert back to 16-bit Short PCM
        for (i in 0 until length) {
            var sample = pitchShifted[i]
            // Smooth limiter curve
            if (sample > 0.95f) {
                sample = 0.95f + (sample - 0.95f) * 0.2f
            } else if (sample < -0.95f) {
                sample = -0.95f + (sample + 0.95f) * 0.2f
            }
            sample = sample.coerceIn(-0.99f, 0.99f)
            out[i] = (sample * 32767.0f).toInt().toShort()
        }

        return out
    }

    /**
     * Pitch shifting using granular overlap-add with linear sub-sample interpolation.
     */
    private fun applyPitchShift(
        input: ShortArray,
        output: FloatArray,
        params: AudioEffectParams,
        state: DspState
    ) {
        val grainSize = state.grainSize
        val window = state.grainWindow
        val pitchBuf = state.pitchBuffer
        val bufSize = pitchBuf.size
        val sampleRate = state.sampleRate

        // Write input into circular pitch buffer
        for (sample in input) {
            pitchBuf[state.pitchWriteIndex] = sample.toFloat() / 32768.0f
            state.pitchWriteIndex = (state.pitchWriteIndex + 1) % bufSize
        }

        val basePitch = params.pitchFactor
        val wobbleSpeed = params.alienWobbleSpeed
        val wobbleDepth = params.alienWobbleDepth

        for (i in input.indices) {
            var currentPitch = basePitch

            if (wobbleSpeed > 0f && wobbleDepth > 0f) {
                val lfo = sin(state.lfoPhase).toFloat()
                state.lfoPhase += (2.0 * PI * wobbleSpeed / sampleRate)
                if (state.lfoPhase > 2 * PI) state.lfoPhase -= (2 * PI)
                currentPitch = basePitch + (lfo * wobbleDepth)
            }

            val p1 = state.grainPhase1
            val p2 = state.grainPhase2

            val i1 = p1.toInt().coerceIn(0, grainSize - 1)
            val i2 = p2.toInt().coerceIn(0, grainSize - 1)

            val w1 = window[i1]
            val w2 = window[i2]

            val readPos1 = (state.pitchWriteIndex - input.size + i - grainSize + i1 + bufSize * 2) % bufSize
            val readPos2 = (state.pitchWriteIndex - input.size + i - grainSize + i2 + bufSize * 2) % bufSize

            val s1 = pitchBuf[readPos1] * w1
            val s2 = pitchBuf[readPos2] * w2

            output[i] = (s1 + s2)

            state.grainPhase1 += currentPitch
            if (state.grainPhase1 >= grainSize) {
                state.grainPhase1 -= grainSize
            }

            state.grainPhase2 += currentPitch
            if (state.grainPhase2 >= grainSize) {
                state.grainPhase2 -= grainSize
            }
        }
    }

    /**
     * Biquad bandpass filter for radio / walkie-talkie effect.
     */
    private fun applyBandpassFilter(
        buffer: FloatArray,
        centerFreq: Float,
        bandwidth: Float,
        sampleRate: Int,
        state: DspState
    ) {
        val omega = 2.0 * PI * centerFreq / sampleRate
        val sn = sin(omega)
        val cs = cos(omega)
        val alpha = sn * (bandwidth / (2.0 * centerFreq))

        val b0 = alpha.toFloat()
        val b1 = 0f
        val b2 = (-alpha).toFloat()
        val a0 = (1.0 + alpha).toFloat()
        val a1 = (-2.0 * cs).toFloat()
        val a2 = (1.0 - alpha).toFloat()

        for (i in buffer.indices) {
            val x0 = buffer[i]
            val y0 = (b0 / a0) * x0 + (b1 / a0) * state.biquadX1 + (b2 / a0) * state.biquadX2 -
                    (a1 / a0) * state.biquadY1 - (a2 / a0) * state.biquadY2

            state.biquadX2 = state.biquadX1
            state.biquadX1 = x0
            state.biquadY2 = state.biquadY1
            state.biquadY1 = y0

            buffer[i] = y0 * 1.5f
        }
    }

    /**
     * Tone shaping shelf filter for bass and treble boosting.
     */
    private fun applyToneShaping(
        buffer: FloatArray,
        lowShelf: Float,
        highShelf: Float,
        state: DspState
    ) {
        var prev = state.tonePrevSample
        for (i in buffer.indices) {
            val cur = buffer[i]
            val lowComponent = (cur + prev) * 0.5f
            val highComponent = cur - lowComponent
            prev = cur

            buffer[i] = (lowComponent * lowShelf) + (highComponent * highShelf)
        }
        state.tonePrevSample = prev
    }

    /**
     * Offline file processor: converts raw/processed audio into standard 16-bit PCM WAV.
     */
    fun processAudioFile(
        inputFile: File,
        outputFile: File,
        params: AudioEffectParams,
        onProgress: (Float) -> Unit = {}
    ): Boolean {
        return try {
            val inputStream = FileInputStream(inputFile)
            val header = ByteArray(44)
            val readHeader = inputStream.read(header)
            if (readHeader < 44) {
                inputStream.close()
                return false
            }

            val sampleRate = ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val channels = ByteBuffer.wrap(header, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

            val actualSampleRate = if (sampleRate in 8000..96000) sampleRate else DEFAULT_SAMPLE_RATE

            val rawBytes = inputStream.readBytes()
            inputStream.close()

            val shortCount = rawBytes.size / 2
            val shortBuffer = ShortArray(shortCount)
            ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer)

            val resampled = if (params.speedFactor != 1.0f && params.speedFactor > 0.3f) {
                applyTimeSpeed(shortBuffer, params.speedFactor)
            } else {
                shortBuffer
            }

            val dspState = DspState(actualSampleRate)
            val chunkSize = 2048
            val processed = ShortArray(resampled.size)

            var offset = 0
            while (offset < resampled.size) {
                val len = min(chunkSize, resampled.size - offset)
                val chunk = resampled.copyOfRange(offset, offset + len)
                val processedChunk = processPcm(chunk, params, dspState)
                System.arraycopy(processedChunk, 0, processed, offset, len)
                offset += len
                onProgress(offset.toFloat() / resampled.size)
            }

            writeWavFile(outputFile, processed, actualSampleRate, channels = 1)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Resample audio for playback speed changes.
     */
    fun applyTimeSpeed(input: ShortArray, speed: Float): ShortArray {
        if (speed <= 0.0f || speed == 1.0f) return input
        val outLength = (input.size / speed).toInt()
        val output = ShortArray(outLength)

        for (i in 0 until outLength) {
            val srcPos = i * speed
            val idx0 = srcPos.toInt()
            val idx1 = min(idx0 + 1, input.size - 1)
            val frac = srcPos - idx0

            val sample = (input[idx0] * (1.0 - frac) + input[idx1] * frac).toInt()
            output[i] = max(-32768, min(32767, sample)).toShort()
        }
        return output
    }

    /**
     * Creates a standard RIFF/WAVE header and writes 16-bit PCM audio samples.
     */
    fun writeWavFile(
        file: File,
        pcmSamples: ShortArray,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        channels: Int = 1
    ) {
        val byteData = ByteArray(pcmSamples.size * 2)
        ByteBuffer.wrap(byteData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmSamples)

        val totalAudioLen = byteData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 16 for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM format
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // Block align
        header[33] = 0
        header[34] = 16 // Bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        val fos = FileOutputStream(file)
        fos.write(header)
        fos.write(byteData)
        fos.flush()
        fos.close()
    }
}
