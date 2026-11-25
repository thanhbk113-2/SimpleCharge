package com.example.simplecharge.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.simplecharge.MainActivity
import com.example.simplecharge.data.ChargingSession
import com.example.simplecharge.data.SessionManager
import com.example.simplecharge.service.ChargingForegroundService

object NotificationHelper {

    const val CHANNEL_ID = "simplecharge_charging_channel"
    const val CHANNEL_NAME = "SimpleCharge - Giám sát sạc"
    const val NOTIFICATION_ID = 1001
    const val NOTIFICATION_ID_TARGET = 1002

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị tiến trình và thời gian sạc pin theo thời gian thực"
                setShowBadge(false)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun getBatteryPct(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val cap = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        if (cap in 1..100) return cap

        val bs = context.applicationContext.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bs?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bs?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        if (level >= 0 && scale > 0) {
            val pct = (level * 100) / scale
            if (pct in 1..100) return pct
        }
        return if (cap > 0) cap else 50
    }

    fun isBatteryCharging(context: Context): Boolean {
        val bs = context.applicationContext.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plug = bs?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        if (plug <= 0) return false

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        if (bm?.isCharging == true) return true
        val status = bs?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun getLocalizedPlugType(plugType: String): String {
        return when (plugType) {
            "AC Fast" -> "Sạc nhanh AC"
            "USB" -> "Sạc USB"
            "Wireless" -> "Sạc không dây"
            else -> "Đang sạc"
        }
    }

    fun parsePlugType(chargePlug: Int, isCharging: Boolean): String {
        if (!isCharging || chargePlug <= 0) return "Unplugged"
        return when {
            (chargePlug and BatteryManager.BATTERY_PLUGGED_WIRELESS) != 0 -> "Wireless"
            (chargePlug and BatteryManager.BATTERY_PLUGGED_AC) != 0 -> "AC Fast"
            (chargePlug and BatteryManager.BATTERY_PLUGGED_USB) != 0 -> "USB"
            else -> "Charging"
        }
    }

    fun buildChargingNotification(
        context: Context,
        batteryPct: Int,
        targetPct: Int,
        isCharging: Boolean,
        startTimeMs: Long = 0L,
        startLevel: Int = 0,
        plugType: String = "",
        autoLaunch: Boolean = false
    ): Notification {
        createNotificationChannel(context)

        val openAppPi = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPi = PendingIntent.getService(
            context,
            1,
            Intent(context, ChargingForegroundService::class.java).apply {
                action = ChargingForegroundService.ACTION_STOP_SERVICE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val plugDisplay = getLocalizedPlugType(plugType)
        val titleText = when {
            isCharging && batteryPct >= targetPct -> "✅ Đã đạt mục tiêu — $batteryPct%"
            isCharging -> "⚡ $plugDisplay: $batteryPct% (Mục tiêu: $targetPct%)"
            else -> "🔌 Đã rút sạc — $batteryPct%"
        }

        val levelGained = if (startTimeMs > 0 && batteryPct >= startLevel) batteryPct - startLevel else 0
        val contentText = if (startTimeMs > 0 && isCharging) {
            "Bắt đầu: $startLevel% (+$levelGained%) • $plugDisplay"
        } else if (!isCharging) {
            "Đã ngắt kết nối sạc pin"
        } else {
            "Mục tiêu sạc: $targetPct% • $plugDisplay"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setProgress(100, batteryPct.coerceIn(0, 100), false)
            .setContentIntent(openAppPi)
            .setOngoing(isCharging)
            .setOnlyAlertOnce(true)
            .setSilent(!autoLaunch)

        if (autoLaunch && isCharging) {
            // Tự động bật màn hình và mở app khi cắm sạc
            builder.setFullScreenIntent(openAppPi, true)
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        }

        // Chỉ đếm giờ khi ĐANG SẠC và có session hợp lệ
        if (startTimeMs > 0 && isCharging) {
            builder.setUsesChronometer(true)
            builder.setWhen(startTimeMs)
            builder.setShowWhen(true)
        } else {
            builder.setUsesChronometer(false)
            builder.setShowWhen(false)
        }

        if (isCharging) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Tắt thông báo",
                stopPi
            )
        }

        return builder.build()
    }

    fun showChargingNotification(context: Context, triggerAutoLaunch: Boolean = false) {
        if (!hasNotificationPermission(context)) return

        try {
            val sessionManager = SessionManager(context)
            val active = sessionManager.getActiveSession()
            val target = sessionManager.getTargetPct()
            val autoLaunch = triggerAutoLaunch && sessionManager.isAutoLaunchEnabled()

            val batteryStatus = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val rawLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val pct = if (scale > 0) (rawLevel * 100) / scale else rawLevel

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val plugType = parsePlugType(chargePlug, isCharging)

            val notification = buildChargingNotification(
                context = context,
                batteryPct = pct,
                targetPct = target,
                isCharging = isCharging,
                startTimeMs = if (active.isActive) active.startTimeMs else 0L,
                startLevel = if (active.isActive) active.startLevel else pct,
                plugType = plugType,
                autoLaunch = autoLaunch
            )

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
        }
    }

    fun showDisconnectedNotification(context: Context, endLevel: Int, session: ChargingSession?) {
        if (!hasNotificationPermission(context)) return

        try {
            createNotificationChannel(context)
            val openAppPi = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val titleText = "🔌 Đã rút sạc — Pin đạt $endLevel%"
            val contentText = if (session != null) {
                val durationMins = (session.durationMs / 60000L).coerceAtLeast(0)
                val durationSecs = (session.durationMs / 1000L) % 60
                val durationStr = if (durationMins > 0) "$durationMins phút $durationSecs giây" else "$durationSecs giây"
                "⏱ Thời gian sạc: $durationStr • Tăng +${session.levelGained}% (từ ${session.startLevel}% → $endLevel%)"
            } else {
                "Phiên sạc đã kết thúc và được lưu lại."
            }

            // Notification khi rút sạc: TẮT CHRONOMETER, hiển thị thời điểm rút sạc
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setProgress(0, 0, false)
                .setUsesChronometer(false)
                .setShowWhen(true)
                .setWhen(session?.endTimeMs ?: System.currentTimeMillis())
                .setContentIntent(openAppPi)
                .setAutoCancel(true)
                .setOngoing(false)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
        }
    }

    fun showTargetReachedNotification(context: Context, batteryPct: Int, target: Int) {
        if (!hasNotificationPermission(context)) return

        try {
            createNotificationChannel(context)
            val openAppPi = PendingIntent.getActivity(
                context,
                2,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                .setContentTitle("✅ Đã đạt mục tiêu sạc $target%!")
                .setContentText("Pin hiện tại: $batteryPct%. Bạn có thể rút sạc ngay để bảo vệ tuổi thọ pin!")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Pin hiện tại: $batteryPct%. Bạn có thể rút sạc ngay để bảo vệ tuổi thọ pin!"))
                .setContentIntent(openAppPi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notify(NOTIFICATION_ID_TARGET, notification)
        } catch (_: Exception) {
        }
    }

    fun cancelNotification(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }
}
