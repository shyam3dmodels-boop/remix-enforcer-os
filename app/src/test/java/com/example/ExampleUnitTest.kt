package com.example

import com.example.core.recorder.AudioChunkRecorder
import com.example.core.recorder.DisguiseProfile
import com.example.core.recorder.TelegramUploadWorker
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testTelegramUploadWorkerTagsAndKeys() {
    assertEquals("telegram_upload", TelegramUploadWorker.TAG_TELEGRAM_UPLOAD)
    assertEquals("progress_percent", TelegramUploadWorker.KEY_PROGRESS_PERCENT)
    assertEquals("progress_bytes_sent", TelegramUploadWorker.KEY_PROGRESS_BYTES_SENT)
    assertEquals("progress_total_bytes", TelegramUploadWorker.KEY_PROGRESS_TOTAL_BYTES)
    assertEquals("progress_status_msg", TelegramUploadWorker.KEY_PROGRESS_STATUS_MSG)
  }

  @Test
  fun testAudioChunkTimeFormatting() {
    assertEquals("00:00", AudioChunkRecorder.formatSeconds(0))
    assertEquals("00:05", AudioChunkRecorder.formatSeconds(5))
    assertEquals("01:00", AudioChunkRecorder.formatSeconds(60))
    assertEquals("15:00", AudioChunkRecorder.formatSeconds(900))
    assertEquals("01:15:30", AudioChunkRecorder.formatSeconds(4530))
  }

  @Test
  fun testDisguiseProfilesContent() {
    val profiles = DisguiseProfile.values()
    assertEquals(5, profiles.size)
    for (profile in profiles) {
      assertTrue(profile.displayName.isNotBlank())
      assertTrue(profile.notificationTitle.isNotBlank())
      assertTrue(profile.notificationText.isNotBlank())
      assertTrue(profile.subText.isNotBlank())
    }
  }

  @Test
  fun testSixHourSleepCalculation() {
    val baseTime = 1700000000000L
    val sixHours = 6L * 60 * 60 * 1000L

    // Direct 6 hours
    val wakeDirect = com.example.core.alarm.SmartAlarmScheduler.calculate6HourWakeTime(baseTime, bufferMinutes = 0)
    assertEquals(baseTime + sixHours, wakeDirect)

    // With 15 minute buffer
    val wakeWith15Min = com.example.core.alarm.SmartAlarmScheduler.calculate6HourWakeTime(baseTime, bufferMinutes = 15)
    assertEquals(baseTime + sixHours + (15 * 60 * 1000L), wakeWith15Min)

    // Bedtime calculation
    val targetWake = baseTime + sixHours
    val bedtime = com.example.core.alarm.SmartAlarmScheduler.calculateBedtimeForWake(targetWake, bufferMinutes = 0)
    assertEquals(baseTime, bedtime)
  }
}
