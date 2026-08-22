package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioDspEngine
import com.example.audio.AudioEffectParams
import com.example.audio.DspState
import com.example.audio.VoiceEffectType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Voice Changer", appName)
  }

  @Test
  fun `dsp engine transforms audio buffer correctly`() {
    val inputSamples = ShortArray(1024) { (it * 15).toShort() }
    val dspState = DspState(44100)

    for (effect in VoiceEffectType.entries) {
      dspState.reset()
      val output = AudioDspEngine.processPcm(inputSamples, effect.defaultParams, dspState)
      assertNotNull(output)
      assertEquals(inputSamples.size, output.size)
    }
  }

  @Test
  fun `dsp time speed resampling works`() {
    val inputSamples = ShortArray(1000) { 100 }
    val fastSamples = AudioDspEngine.applyTimeSpeed(inputSamples, 2.0f)
    assertEquals(500, fastSamples.size)

    val slowSamples = AudioDspEngine.applyTimeSpeed(inputSamples, 0.5f)
    assertEquals(2000, slowSamples.size)
  }
}
