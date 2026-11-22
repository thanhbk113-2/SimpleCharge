package com.example.simplecharge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.example.simplecharge.data.SessionManager
import com.example.simplecharge.service.ChargingForegroundService
import com.example.simplecharge.util.NotificationHelper

/**
 * BatteryReceiver — Lắng nghe sự kiện cắm/rút sạc từ hệ thống Android
 * kể cả khi app đang tắt hoặc chạy nền.
 */
class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sessionManager = SessionManager(context)

        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val pct = getBatteryPct(context)
                val temp = getBatteryTemp(context)
                sessionManager.startOrResumeSession(pct, temp)

                // 1. Hiển thị thông báo ngay lập tức trên thanh thông báo
                NotificationHelper.showChargingNotification(context)

                // 2. Khởi động service chạy nền (an toàn với try-catch trong service)
                ChargingForegroundService.start(context)

                // 3. Thông báo cho MainActivity nếu đang mở
                context.sendBroadcast(
                    Intent(ChargingForegroundService.ACTION_SESSION_CHANGED)
                        .setPackage(context.packageName)
                )
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                // ✅ 1. Tự động finish session khi rút sạc
                val active = sessionManager.getActiveSession()
                val pct = getBatteryPct(context)
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

    private fun getBatteryPct(context: Context): Int {
        val bs = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bs?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = bs?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (scale > 0) (level * 100) / scale else level
    }

    private fun getBatteryTemp(context: Context): Float {
        val bs = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = bs?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return rawTemp / 10f
    }
}
