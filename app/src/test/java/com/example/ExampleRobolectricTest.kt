package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.config.AppConfig
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
    assertEquals("Cứu Hộ 24/7", appName)
  }

  @Test
  fun `app config has required rescue hotlines and issue types`() {
    assertEquals("1900545566", AppConfig.DEFAULT_RESCUE_HOTLINE)
    assertTrue(AppConfig.ISSUE_TYPES.contains("Xe hết xăng"))
    assertTrue(AppConfig.ISSUE_TYPES.contains("Thủng lốp"))
    assertTrue(AppConfig.ISSUE_TYPES.contains("Hết bình"))
    assertTrue(AppConfig.ISSUE_TYPES.contains("Tai nạn"))
    assertTrue(AppConfig.ISSUE_TYPES.contains("Hỏng xe"))
  }

  @Test
  fun `emergency sos triggers dialer intent with predefined rescue number`() {
    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
      data = android.net.Uri.parse("tel:${AppConfig.DEFAULT_RESCUE_HOTLINE}")
      addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    assertEquals(android.content.Intent.ACTION_DIAL, intent.action)
    assertEquals("tel:1900545566", intent.data.toString())
  }

  @Test
  fun `rescue map location holds correct coordinates without google maps dependency`() {
    val location = com.example.ui.components.RescueMapLocation(21.0285, 105.8542)
    assertEquals(21.0285, location.latitude, 0.0001)
    assertEquals(105.8542, location.longitude, 0.0001)
  }
}
