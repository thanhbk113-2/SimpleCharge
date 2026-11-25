package com.example.simplecharge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import com.example.simplecharge.MainActivity
import com.example.simplecharge.data.SessionManager
import com.example.simplecharge.service.ChargingForegroundService
import com.example.simplecharge.util.NotificationHelper

/**
 * BatteryReceiver — Lắng nghe sự kiện cắm/rút sạc và khởi động máy từ hệ thống Android
 * kể cả khi app đang tắt hoặc chạy nền.
 */
class BatteryReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BatteryReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sessionManager = SessionManager(context)

        Log.d(TAG, "onReceive action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                // Khởi tạo notification channel
                NotificationHelper.createNotificationChannel(context)

                // Kiểm tra xem máy vừa bật lên có đang cắm sạc không
                if (NotificationHelper.isBatteryCharging(context)) {
                    val pct = NotificationHelper.getBatteryPct(context)
                    val temp = getBatteryTemp(context)
                    sessionManager.startOrResumeSession(pct, temp)

                    val autoLaunch = sessionManager.isAutoLaunchEnabled()
                    // 1. Hiển thị thông báo trên thanh trạng thái (kèm fullScreenIntent nếu autoLaunch)
                    NotificationHelper.showChargingNotification(context, triggerAutoLaunch = autoLaunch)

                    // 2. Bắt đầu Foreground Service
                    ChargingForegroundService.start(context)

                    // 3. Tự động mở app nếu người dùng bật tùy chọn này
                    if (autoLaunch) {
                        launchMainActivity(context)
                    }
                }
            }

            Intent.ACTION_POWER_CONNECTED -> {
                val pct = NotificationHelper.getBatteryPct(context)
                val temp = getBatteryTemp(context)
                sessionManager.startOrResumeSession(pct, temp)

                val autoLaunch = sessionManager.isAutoLaunchEnabled()

                // 1. Hiển thị thông báo ngay lập tức trên thanh thông báo
                NotificationHelper.showChargingNotification(context, triggerAutoLaunch = autoLaunch)

                // 2. Khởi động service chạy nền (an toàn với try-catch trong service)
                ChargingForegroundService.start(context)

                // 3. Tự động mở app khi cắm sạc
                if (autoLaunch) {
                    launchMainActivity(context)
                }

                // 4. Thông báo cho MainActivity nếu đang mở
                context.sendBroadcast(
                    Intent(ChargingForegroundService.ACTION_SESSION_CHANGED)
                        .setPackage(context.packageName)
                )
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                // ✅ 1. Tự động finish session khi rút sạc
                val active = sessionManager.getActiveSession()
                val pct = NotificationHelper.getBatteryPct(context)
                val temp = getBatteryTemp(context)
                val session = if (active.isActive) {
                    sessionManager.finishSession(pct, temp)
                } else {
                    null
                }

                // 2. Cập nhật thông báo kết quả sạc
                NotificationHelper.showDisconnectedNotification(context, pct, session)

                // 3. Dừng foreground service
                ChargingForegroundService.stop(context)

                // 4. Thông báo cho MainActivity cập nhật lại danh sách lịch sử
                context.sendBroadcast(
                    Intent(ChargingForegroundService.ACTION_SESSION_CHANGED)
                        .setPackage(context.packageName)
                )
            }
        }
    }

    private fun launchMainActivity(context: Context) {
        try {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(launchIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot launch MainActivity directly: ${e.message}")
        }
    }

    private fun getBatteryTemp(context: Context): Float {
        val bs = context.applicationContext.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = bs?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return rawTemp / 10f
    }
}
