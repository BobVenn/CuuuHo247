package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.config.AppConfig
import com.example.data.model.RescueRequest

object RescueNotificationHelper {
    private const val TAG = "RescueNotificationHelper"
    const val CHANNEL_ID = "rescue_status_channel"
    const val CHANNEL_NAME = "Thông báo cứu hộ 24/7"
    const val EXTRA_REQUEST_ID = "extra_request_id"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Cập nhật trạng thái yêu cầu cứu hộ trực tiếp từ Firebase Realtime Database"
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 350, 150, 350)
                    setSound(soundUri, audioAttributes)
                    setShowBadge(true)
                }
                notificationManager.createNotificationChannel(channel)
                Log.i(TAG, "Notification channel $CHANNEL_ID created successfully")
            }
        }
    }

    data class NotificationContent(
        val title: String,
        val message: String,
        val badgeText: String
    )

    fun getStatusNotificationContent(request: RescueRequest, newStatus: String): NotificationContent {
        val shortId = request.id.takeLast(6).uppercase()
        val staffName = request.staffName ?: "KTV Nguyễn Văn Toàn"
        val staffPhone = request.staffPhone ?: AppConfig.DEFAULT_RESCUE_HOTLINE

        return when (newStatus) {
            AppConfig.RequestStatus.ACCEPTED -> NotificationContent(
                title = "⚡ KTV đã nhận đơn cứu hộ #$shortId",
                message = "$staffName đã tiếp nhận sự cố ${request.issueType} của bạn và đang chuẩn bị trang thiết bị.",
                badgeText = "Đã điều phối"
            )
            AppConfig.RequestStatus.EN_ROUTE -> NotificationContent(
                title = "🚗 KTV đang di chuyển đến vị trí bạn!",
                message = "$staffName đang trên đường tới ${request.address.take(35)}. Hotline: $staffPhone.",
                badgeText = "Đang đến"
            )
            AppConfig.RequestStatus.ARRIVED -> NotificationContent(
                title = "📍 KTV đã đến hiện trường (#$shortId)",
                message = "$staffName đã có mặt tại vị trí của bạn. Vui lòng liên hệ KTV qua số $staffPhone.",
                badgeText = "Đã đến nơi"
            )
            AppConfig.RequestStatus.IN_PROGRESS -> NotificationContent(
                title = "🔧 Đang xử lý sự cố xe (#$shortId)",
                message = "Kỹ thuật viên đang tiến hành kiểm tra và khắc phục sự cố ${request.issueType}.",
                badgeText = "Đang xử lý"
            )
            AppConfig.RequestStatus.COMPLETED -> NotificationContent(
                title = "✅ Cứu hộ hoàn tất thành công! (#$shortId)",
                message = "Sự cố xe đã được khắc phục xong. Bạn có thể xem biên lai & quét VietQR thanh toán.",
                badgeText = "Hoàn tất"
            )
            AppConfig.RequestStatus.CANCELLED -> NotificationContent(
                title = "❌ Yêu cầu cứu hộ đã hủy (#$shortId)",
                message = "Đơn cứu hộ cho phương tiện ${request.vehicleType} (${request.licensePlate}) đã đóng.",
                badgeText = "Đã hủy"
            )
            else -> NotificationContent(
                title = "🔔 Cập nhật yêu cầu cứu hộ #$shortId",
                message = "Trạng thái mới: $newStatus tại ${request.address}",
                badgeText = newStatus
            )
        }
    }

    fun showStatusNotification(context: Context, request: RescueRequest, newStatus: String) {
        try {
            // Check permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Skipping system push notification.")
                    return
                }
            }

            val content = getStatusNotificationContent(request, newStatus)

            // Intent to open MainActivity on this request
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_REQUEST_ID, request.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                request.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(content.title)
                .setContentText(content.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content.message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 350, 150, 350))
                .build()

            val notificationId = (request.id.hashCode() and 0x7FFFFFFF)
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            Log.i(TAG, "Status notification posted for request ${request.id}, status=$newStatus")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post status notification: ${e.message}", e)
        }
    }

    fun playAlertSoundAndVibration(context: Context) {
        try {
            // Audio beep
            val toneGenerator = ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 400)

            // Vibration
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 250), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 200, 100, 250), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "playAlertSoundAndVibration warning: ${e.message}")
        }
    }
}
