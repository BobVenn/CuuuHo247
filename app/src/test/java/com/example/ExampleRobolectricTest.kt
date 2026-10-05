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
    assertEquals("0898212031", AppConfig.DEFAULT_RESCUE_HOTLINE)
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
    assertEquals("tel:0898212031", intent.data.toString())
  }

  @Test
  fun `rescue map location holds correct coordinates without google maps dependency`() {
    val location = com.example.ui.components.RescueMapLocation(21.0285, 105.8542)
    assertEquals(21.0285, location.latitude, 0.0001)
    assertEquals(105.8542, location.longitude, 0.0001)
  }

  @Test
  fun `rescue notification helper creates correct localized status notifications`() {
    val req = com.example.data.model.RescueRequest(
      id = "REQ_123456",
      staffName = "KTV Toàn",
      address = "Hà Nội",
      issueType = "Hết bình"
    )
    val accepted = com.example.util.RescueNotificationHelper.getStatusNotificationContent(req, AppConfig.RequestStatus.ACCEPTED)
    assertTrue(accepted.title.contains("KTV đã nhận đơn cứu hộ"))
    assertTrue(accepted.message.contains("KTV Toàn"))

    val enRoute = com.example.util.RescueNotificationHelper.getStatusNotificationContent(req, AppConfig.RequestStatus.EN_ROUTE)
    assertTrue(enRoute.title.contains("KTV đang di chuyển"))

    val arrived = com.example.util.RescueNotificationHelper.getStatusNotificationContent(req, AppConfig.RequestStatus.ARRIVED)
    assertTrue(arrived.title.contains("KTV đã đến hiện trường"))

    val completed = com.example.util.RescueNotificationHelper.getStatusNotificationContent(req, AppConfig.RequestStatus.COMPLETED)
    assertTrue(completed.title.contains("Cứu hộ hoàn tất"))
  }
}
