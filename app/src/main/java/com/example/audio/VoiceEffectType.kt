package com.example.audio

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Waves
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Defined voice preset effect types supported by the Voice Changer DSP.
 */
enum class VoiceEffectType(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val themeColor: Color,
    val defaultParams: AudioEffectParams
) {
    NORMAL(
        title = "Normal",
        description = "Natural clean voice pass-through",
        icon = Icons.Default.Mic,
        themeColor = Color(0xFF64748B),
        defaultParams = AudioEffectParams(pitchFactor = 1.0f, speedFactor = 1.0f)
    ),
    MALE(
        title = "Male Voice",
        description = "Deeper tone with warm lower resonance",
        icon = Icons.Default.Male,
        themeColor = Color(0xFF2563EB),
        defaultParams = AudioEffectParams(
            pitchFactor = 0.82f,
            speedFactor = 1.0f,
            lowShelfGain = 1.4f,
            highShelfGain = 0.85f
        )
    ),
    FEMALE(
        title = "Female Voice",
        description = "Elevated pitch with bright clear harmonics",
        icon = Icons.Default.Female,
        themeColor = Color(0xFFEC4899),
        defaultParams = AudioEffectParams(
            pitchFactor = 1.30f,
            speedFactor = 1.0f,
            lowShelfGain = 0.85f,
            highShelfGain = 1.35f
        )
    ),
    HIGH(
        title = "High Voice",
        description = "Crisp, bright high-pitched vocal boost",
        icon = Icons.Default.MusicNote,
        themeColor = Color(0xFF06B6D4),
        defaultParams = AudioEffectParams(
            pitchFactor = 1.40f,
            speedFactor = 1.0f,
            lowShelfGain = 0.70f,
            highShelfGain = 1.50f
        )
    ),
    CHILD(
        title = "Child Voice",
        description = "Light high-pitched youthful voice",
        icon = Icons.Default.Face,
        themeColor = Color(0xFFF59E0B),
        defaultParams = AudioEffectParams(
            pitchFactor = 1.50f,
            speedFactor = 1.05f,
            lowShelfGain = 0.75f,
            highShelfGain = 1.45f
        )
    ),
    DEEP(
        title = "Deep Voice",
        description = "Ultra low pitch with heavy sub-bass boost",
        icon = Icons.Default.RecordVoiceOver,
        themeColor = Color(0xFF6366F1),
        defaultParams = AudioEffectParams(
            pitchFactor = 0.65f,
            speedFactor = 0.96f,
            lowShelfGain = 2.0f,
            highShelfGain = 0.70f
        )
    ),
    ROBOT(
        title = "Robot Voice",
        description = "Metallic ring-modulated android synthesis",
        icon = Icons.Default.SmartToy,
        themeColor = Color(0xFF06B6D4),
        defaultParams = AudioEffectParams(
            pitchFactor = 1.0f,
            speedFactor = 1.0f,
            robotRingFreqHz = 68f,
            robotMix = 0.85f,
            metallicResonance = 0.6f
        )
    ),
    MONSTER(
        title = "Monster Voice",
        description = "Menacing low growl with overdrive crunch",
        icon = Icons.Default.Waves,
        themeColor = Color(0xFFDC2626),
        defaultParams = AudioEffectParams(
            pitchFactor = 0.58f,
            speedFactor = 0.92f,
            distortion = 0.45f,
            lowShelfGain = 2.2f,
            echoDelayMs = 40,
            echoFeedback = 0.35f
        )
    ),
    ALIEN(
        title = "Alien Voice",
        description = "Sinusoidal frequency wobble and extraterrestrial delay",
        icon = Icons.Default.GraphicEq,
        themeColor = Color(0xFF10B981),
        defaultParams = AudioEffectParams(
            pitchFactor = 1.15f,
            speedFactor = 1.0f,
            alienWobbleSpeed = 8.5f,
            alienWobbleDepth = 0.35f,
            echoDelayMs = 90,
            echoFeedback = 0.45f
        )
    ),
    ECHO(
        title = "Echo Voice",
        description = "Multi-tap spatial acoustic echo reflections",
        icon = Icons.Default.Headphones,
        themeColor = Color(0xFF8B5CF6),
        defaultParams = AudioEffectParams(
            pitchFactor = 1.0f,
            speedFactor = 1.0f,
            echoDelayMs = 220,
            echoFeedback = 0.55f
        )
    ),
    RADIO(
        title = "Radio Voice",
        description = "Vintage walkie-talkie & telephone bandpass filter",
        icon = Icons.Default.Radio,
        themeColor = Color(0xFFD97706),
        defaultParams = AudioEffectParams(
            pitchFactor = 1.0f,
            speedFactor = 1.0f,
            bandpassCenterHz = 1600f,
            bandpassWidthHz = 1800f,
            distortion = 0.25f
        )
    ),
    CARTOON(
        title = "Cartoon Voice",
        description = "Bouncy animated character with vibrato",
        icon = Icons.Default.MusicNote,
        themeColor = Color(0xFFA855F7),
        defaultParams = AudioEffectParams(
            pitchFactor = 1.45f,
            speedFactor = 1.12f,
            alienWobbleSpeed = 6.0f,
            alienWobbleDepth = 0.15f
        )
    ),
    CHIPMUNK(
        title = "Chipmunk Voice",
        description = "Super fast and ultra high-pitched squeak",
        icon = Icons.Default.Speed,
        themeColor = Color(0xFFF97316),
        defaultParams = AudioEffectParams(
            pitchFactor = 1.85f,
            speedFactor = 1.25f,
            highShelfGain = 1.6f
        )
    ),
    SLOW(
        title = "Slow Voice",
        description = "Deep sluggish slow-motion playback",
        icon = Icons.Default.Phone,
        themeColor = Color(0xFF64748B),
        defaultParams = AudioEffectParams(
            pitchFactor = 0.76f,
            speedFactor = 0.75f
        )
    ),
    FAST(
        title = "Fast Voice",
        description = "Energetic fast-paced voice acceleration",
        icon = Icons.Default.Speed,
        themeColor = Color(0xFF14B8A6),
        defaultParams = AudioEffectParams(
            pitchFactor = 1.25f,
            speedFactor = 1.35f
        )
    ),
    CUSTOM(
        title = "Custom FX",
        description = "Fine-tune your own pitch, speed, and sound parameters",
        icon = Icons.Default.Tune,
        themeColor = Color(0xFF6366F1),
        defaultParams = AudioEffectParams()
    );

    companion object {
        fun fromName(name: String?): VoiceEffectType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NORMAL
        }
    }
}

/**
 * Parameters for audio effect processing
 */
data class AudioEffectParams(
    val pitchFactor: Float = 1.0f,       // 0.5f to 2.0f
    val speedFactor: Float = 1.0f,       // 0.5f to 2.0f
    val echoDelayMs: Int = 0,            // 0 to 500 ms
    val echoFeedback: Float = 0.0f,      // 0.0f to 0.85f
    val robotRingFreqHz: Float = 0f,     // 0 = disabled, 30 to 200 Hz
    val robotMix: Float = 0f,            // 0.0 to 1.0
    val metallicResonance: Float = 0f,   // 0.0 to 1.0
    val distortion: Float = 0.0f,        // 0.0f to 1.0f overdrive
    val lowShelfGain: Float = 1.0f,      // 0.5f to 2.5f bass boost
    val highShelfGain: Float = 1.0f,     // 0.5f to 2.5f treble boost
    val bandpassCenterHz: Float = 0f,    // 0 = disabled, center freq
    val bandpassWidthHz: Float = 0f,
    val alienWobbleSpeed: Float = 0f,    // 0 = disabled, Hz (e.g. 8Hz)
    val alienWobbleDepth: Float = 0f     // 0.0 to 0.8
)
