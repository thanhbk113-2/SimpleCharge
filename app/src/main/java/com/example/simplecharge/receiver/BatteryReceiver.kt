package com.example.simplecharge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.example.simplecharge.data.SessionManager

class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sessionManager = SessionManager(context)

        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                val batteryStatus = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                val pct = if (scale > 0) (level * 100) / scale else level
                sessionManager.startOrResumeSession(pct)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                // Device unplugged - if there was an active session with some elapsed time, finish it
                val active = sessionManager.getActiveSession()
                if (active.isActive) {
                    val batteryStatus = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
                    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                    val pct = if (scale > 0) (level * 100) / scale else level
                    sessionManager.finishSession(pct)
                }
            }
        }
    }
}
