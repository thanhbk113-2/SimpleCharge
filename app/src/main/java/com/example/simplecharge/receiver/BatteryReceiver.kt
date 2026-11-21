package com.example.simplecharge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.example.simplecharge.data.SessionManager
import com.example.simplecharge.service.ChargingForegroundService

/**
 * BatteryReceiver — kích hoạt khi app không chạy foreground.
 * Khi app đang chạy foreground service, service tự xử lý các event này.
 */
class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sessionManager = SessionManager(context)

        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val pct = getBatteryPct(context)
                sessionManager.startOrResumeSession(pct)
                // Khởi động foreground service để hiện notification
                ChargingForegroundService.start(context)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                // ✅ Tự động finish session khi rút sạc
                val active = sessionManager.getActiveSession()
                if (active.isActive) {
                    val pct = getBatteryPct(context)
                    sessionManager.finishSession(pct)
                    context.sendBroadcast(Intent(ChargingForegroundService.ACTION_SESSION_CHANGED))
                }
                // Service sẽ tự dừng khi nhận POWER_DISCONNECTED
            }
        }
    }

    private fun getBatteryPct(context: Context): Int {
        val bs = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bs?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = bs?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (scale > 0) (level * 100) / scale else level
    }
}
