package com.example.simplecharge.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.simplecharge.data.SessionManager
import com.example.simplecharge.util.NotificationHelper

class ChargingForegroundService : Service() {

    companion object {
        const val TAG = "ChargingFgService"
        const val ACTION_STOP_SERVICE = "com.example.simplecharge.STOP_SERVICE"
        const val ACTION_SESSION_CHANGED = "com.example.simplecharge.SESSION_CHANGED"

        fun start(context: Context) {
            try {
                val intent = Intent(context, ChargingForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cannot startForegroundService (restricted by OS): ${e.message}")
                NotificationHelper.showChargingNotification(context)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, ChargingForegroundService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }

    private lateinit var sessionManager: SessionManager
    private var isReceiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    val pct = getCurrentBatteryPct()
                    sessionManager.startOrResumeSession(pct)
                    sendBroadcast(Intent(ACTION_SESSION_CHANGED).setPackage(packageName))
                    NotificationHelper.showChargingNotification(this@ChargingForegroundService)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    // ✅ Tự động hoàn thành session khi rút sạc
                    handleUnplugged()
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val isCharging = isCurrentlyCharging()
                    if (!isCharging) {
                        // Nếu pin báo không còn sạc nữa mà session vẫn đang active -> tự động kết thúc
                        if (sessionManager.hasActiveSession()) {
                            handleUnplugged()
                        }
                        return
                    }

                    val pct = getCurrentBatteryPct()
                    // Kiểm tra xem pin đã đạt mục tiêu chưa
                    val completed = sessionManager.checkAndHandleBatteryLevel(pct)
                    if (completed != null) {
                        sendBroadcast(Intent(ACTION_SESSION_CHANGED).setPackage(packageName))
                        NotificationHelper.showTargetReachedNotification(
                            this@ChargingForegroundService,
                            pct,
                            sessionManager.getTargetPct()
                        )
                    }
                    // Cập nhật thông báo tiến trình
                    NotificationHelper.showChargingNotification(this@ChargingForegroundService)
                }
            }
        }
    }

    private fun handleUnplugged() {
        val active = sessionManager.getActiveSession()
        val pct = getCurrentBatteryPct()
        val session = if (active.isActive) {
            sessionManager.finishSession(pct)
        } else null

        NotificationHelper.showDisconnectedNotification(this, pct, session)
        sendBroadcast(Intent(ACTION_SESSION_CHANGED).setPackage(packageName))
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        NotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            NotificationHelper.cancelNotification(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Bắt đầu Foreground Service với notification chuẩn
        try {
            val session = sessionManager.getActiveSession()
            val target = sessionManager.getTargetPct()
            val batteryPct = getCurrentBatteryPct()
            val isCharging = isCurrentlyCharging()
            val plugType = getCurrentPlugType(isCharging)

            val notification = NotificationHelper.buildChargingNotification(
                context = this,
                batteryPct = batteryPct,
                targetPct = target,
                isCharging = isCharging,
                startTimeMs = if (session.isActive) session.startTimeMs else 0L,
                startLevel = if (session.isActive) session.startLevel else batteryPct,
                plugType = plugType
            )

            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }

            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}", e)
            NotificationHelper.showChargingNotification(this)
            stopSelf()
            return START_NOT_STICKY
        }

        // Đăng ký nhận sự kiện thay đổi pin trong lúc sạc
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
            }
            registerReceiver(batteryReceiver, filter)
            isReceiverRegistered = true
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun getCurrentBatteryPct(): Int {
        val bs = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bs?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = bs?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (scale > 0) (level * 100) / scale else level
    }

    private fun isCurrentlyCharging(): Boolean {
        val bs = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = bs?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun getCurrentPlugType(isCharging: Boolean): String {
        val bs = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val chargePlug = bs?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        return NotificationHelper.parsePlugType(chargePlug, isCharging)
    }
}
