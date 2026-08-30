package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CommandParser
import com.example.engine.NexusIntent
import org.junit.Assert.assertEquals
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
    assertEquals("NEXUS AI", appName)
  }

  @Test
  fun `parse multilingual flashlight commands`() {
    val enOn = CommandParser.parse("Turn on flashlight")
    assertTrue(enOn is NexusIntent.ToggleFlashlight && (enOn as NexusIntent.ToggleFlashlight).enabled)

    val hiOn = CommandParser.parse("Torch jalao")
    assertTrue(hiOn is NexusIntent.ToggleFlashlight && (hiOn as NexusIntent.ToggleFlashlight).enabled)

    val hiOff = CommandParser.parse("Torch band karo")
    assertTrue(hiOff is NexusIntent.ToggleFlashlight && !(hiOff as NexusIntent.ToggleFlashlight).enabled)
  }

  @Test
  fun `parse volume and brightness commands`() {
    val vol50 = CommandParser.parse("Volume 50 percent karo")
    assertTrue(vol50 is NexusIntent.SetVolume && (vol50 as NexusIntent.SetVolume).levelPercent == 50)

    val bright = CommandParser.parse("Brightness 80 percent")
    assertTrue(bright is NexusIntent.SetBrightness && (bright as NexusIntent.SetBrightness).levelPercent == 80)
  }

  @Test
  fun `parse compound multi-action command`() {
    val compound = CommandParser.parse("Bluetooth settings kholo aur volume 70 percent kar do")
    assertTrue(compound is NexusIntent.MultiAction)
    val multi = compound as NexusIntent.MultiAction
    assertEquals(2, multi.actions.size)
  }
}

