package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.audio.AudioEffectParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoicePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("voice_changer_prefs", Context.MODE_PRIVATE)

    private val _favoriteEffects = MutableStateFlow<Set<String>>(loadFavorites())
    val favoriteEffects = _favoriteEffects.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(prefs.getBoolean(KEY_OVERLAY_ACTIVE, false))
    val isOverlayActive = _isOverlayActive.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, true))
    val isDarkMode = _isDarkMode.asStateFlow()

    private fun loadFavorites(): Set<String> {
        val defaultFavs = setOf("ROBOT", "MALE", "FEMALE", "CHIPMUNK", "MONSTER", "ECHO")
        return prefs.getStringSet(KEY_FAVORITE_EFFECTS, defaultFavs) ?: defaultFavs
    }

    fun toggleFavoriteEffect(effectName: String) {
        val current = _favoriteEffects.value.toMutableSet()
        if (current.contains(effectName)) {
            current.remove(effectName)
        } else {
            current.add(effectName)
        }
        prefs.edit().putStringSet(KEY_FAVORITE_EFFECTS, current).apply()
        _favoriteEffects.value = current
    }

    fun setOverlayActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_OVERLAY_ACTIVE, active).apply()
        _isOverlayActive.value = active
    }

    fun setDarkMode(dark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply()
        _isDarkMode.value = dark
    }

    fun saveCustomParams(params: AudioEffectParams) {
        prefs.edit()
            .putFloat(KEY_CUSTOM_PITCH, params.pitchFactor)
            .putFloat(KEY_CUSTOM_SPEED, params.speedFactor)
            .putInt(KEY_CUSTOM_ECHO_DELAY, params.echoDelayMs)
            .putFloat(KEY_CUSTOM_ECHO_FB, params.echoFeedback)
            .putFloat(KEY_CUSTOM_ROBOT_FREQ, params.robotRingFreqHz)
            .putFloat(KEY_CUSTOM_DISTORTION, params.distortion)
            .putFloat(KEY_CUSTOM_BASS, params.lowShelfGain)
            .putFloat(KEY_CUSTOM_TREBLE, params.highShelfGain)
            .apply()
    }

    fun loadCustomParams(): AudioEffectParams {
        return AudioEffectParams(
            pitchFactor = prefs.getFloat(KEY_CUSTOM_PITCH, 1.0f),
            speedFactor = prefs.getFloat(KEY_CUSTOM_SPEED, 1.0f),
            echoDelayMs = prefs.getInt(KEY_CUSTOM_ECHO_DELAY, 0),
            echoFeedback = prefs.getFloat(KEY_CUSTOM_ECHO_FB, 0.0f),
            robotRingFreqHz = prefs.getFloat(KEY_CUSTOM_ROBOT_FREQ, 0f),
            robotMix = if (prefs.getFloat(KEY_CUSTOM_ROBOT_FREQ, 0f) > 0f) 0.8f else 0f,
            distortion = prefs.getFloat(KEY_CUSTOM_DISTORTION, 0.0f),
            lowShelfGain = prefs.getFloat(KEY_CUSTOM_BASS, 1.0f),
            highShelfGain = prefs.getFloat(KEY_CUSTOM_TREBLE, 1.0f)
        )
    }

    companion object {
        private const val KEY_FAVORITE_EFFECTS = "key_favorite_effects"
        private const val KEY_OVERLAY_ACTIVE = "key_overlay_active"
        private const val KEY_DARK_MODE = "key_dark_mode"
        private const val KEY_CUSTOM_PITCH = "key_custom_pitch"
        private const val KEY_CUSTOM_SPEED = "key_custom_speed"
        private const val KEY_CUSTOM_ECHO_DELAY = "key_custom_echo_delay"
        private const val KEY_CUSTOM_ECHO_FB = "key_custom_echo_fb"
        private const val KEY_CUSTOM_ROBOT_FREQ = "key_custom_robot_freq"
        private const val KEY_CUSTOM_DISTORTION = "key_custom_distortion"
        private const val KEY_CUSTOM_BASS = "key_custom_bass"
        private const val KEY_CUSTOM_TREBLE = "key_custom_treble"
    }
}
