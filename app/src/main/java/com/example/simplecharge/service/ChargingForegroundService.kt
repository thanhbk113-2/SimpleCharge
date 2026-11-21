package com.example.simplecharge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.simplecharge.MainActivity
import com.example.simplecharge.data.SessionManager

class ChargingForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "simplecharge_charging_channel"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_ID_TARGET = 1002
        const val ACTION_STOP_SERVICE = "com.example.simplecharge.STOP_SERVICE"
        const val ACTION_SESSION_CHANGED = "com.example.simplecharge.SESSION_CHANGED"

        fun start(context: Context) {
            val intent = Intent(context, ChargingForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ChargingForegroundService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var sessionManager: SessionManager
    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private var isReceiverRegistered = false

    // Ticker: cập nhật notification mỗi giây khi đang sạc
    private val tickRunnable = object : Runnable {
        override fun run() {
            updateNotification()
            handler.postDelayed(this, 1000L)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    val pct = getCurrentBatteryPct()
                    sessionManager.startOrResumeSession(pct)
                    sendBroadcast(Intent(ACTION_SESSION_CHANGED))
                    updateNotification()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    // ✅ Tự động finish session khi rút sạc
                    val active = sessionManager.getActiveSession()
                    if (active.isActive) {
                        val pct = getCurrentBatteryPct()
                        sessionManager.finishSession(pct)
                        // Thông báo cho UI reload lịch sử
                        sendBroadcast(Intent(ACTION_SESSION_CHANGED))
                    }
                    updateNotification()
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val pct = getCurrentBatteryPct()
                    val completed = sessionManager.checkAndHandleBatteryLevel(pct)
                    if (completed != null) {
                        sendBroadcast(Intent(ACTION_SESSION_CHANGED))
                        showTargetReachedNotification(pct)
                    }
                    updateNotification()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
            }
            registerReceiver(batteryReceiver, filter)
            isReceiverRegistered = true
        }

        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickRunnable)
        if (isReceiverRegistered) {
            try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
            isReceiverRegistered = false
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─── Notification ──────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SimpleCharge — Theo dõi sạc",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Hiển thị tiến trình sạc pin theo thời gian thực"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val batteryPct = getCurrentBatteryPct()
        val isCharging = isCurrentlyCharging()
        val active = sessionManager.getActiveSession()
        val target = sessionManager.getTargetPct()

        val openAppPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, ChargingForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = when {
            isCharging && batteryPct >= target -> "✅ Đã đạt mục tiêu — $batteryPct%"
            isCharging -> "⚡ Đang sạc — $batteryPct% / $target%"
            else -> "🔌 Đã rút sạc — $batteryPct%"
        }

        val contentText = if (active.isActive) {
            val elapsedMs = (System.currentTimeMillis() - active.startTimeMs).coerceAtLeast(0L)
            val gained = (batteryPct - active.startLevel).coerceAtLeast(0)
            val elapsedStr = formatDuration(elapsedMs)
            "⏱ $elapsedStr  •  +$gained%  •  Mục tiêu: ${active.targetPct}%"
        } else if (!isCharging) {
            "Cắm sạc để bắt đầu theo dõi phiên mới"
        } else {
            "Đang sạc — Mục tiêu: $target%"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setProgress(100, batteryPct.coerceIn(0, 100), false)
            .setContentIntent(openAppPi)
            .setOngoing(isCharging)
            .setSilent(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dừng theo dõi",
                stopPi
            )
            .build()
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun showTargetReachedNotification(batteryPct: Int) {
        val target = sessionManager.getTargetPct()
        val openAppPi = PendingIntent.getActivity(
            this, 2,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("✅ Đã đạt mục tiêu sạc!")
            .setContentText("Pin đạt $batteryPct% (mục tiêu $target%) — Có thể rút sạc ngay!")
            .setContentIntent(openAppPi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notificationManager.notify(NOTIFICATION_ID_TARGET, n)
    }

    // ─── Battery helpers ───────────────────────────────────────────────────

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

    private fun formatDuration(ms: Long): String {
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
