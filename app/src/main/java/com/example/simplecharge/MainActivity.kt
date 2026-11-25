package com.example.simplecharge

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.simplecharge.data.ActiveSessionState
import com.example.simplecharge.data.ChargingSession
import com.example.simplecharge.data.SessionManager
import com.example.simplecharge.service.ChargingForegroundService
import com.example.simplecharge.ui.SimpleChargeApp
import com.example.simplecharge.ui.theme.SimpleChargeTheme
import com.example.simplecharge.util.NotificationHelper
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    // Live state
    private var batteryPctState = mutableIntStateOf(50)
    private var isChargingState = mutableStateOf(false)
    private var plugTypeState = mutableStateOf("USB")
    private var currentMaState = mutableStateOf<Int?>(null)
    private var temperatureCState = mutableFloatStateOf(0f)
    private var voltageVState = mutableFloatStateOf(0f)
    private var targetPctState = mutableIntStateOf(90)
    private var languageState = mutableStateOf("en")
    private var autoLaunchState = mutableStateOf(true)
    private var activeSessionState = mutableStateOf(ActiveSessionState())
    private var elapsedMsState = mutableLongStateOf(0L)
    private var recentSessionsState = mutableStateOf<List<ChargingSession>>(emptyList())
    private var deletedSessionsState = mutableStateOf<List<ChargingSession>>(emptyList())

    // For manual simulation in testing
    private var simulatedBonusLevel = 0

    // Receiver lắng nghe thay đổi battery (chỉ khi app ở foreground)
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateBatteryInfo()
        }
    }

    // Receiver nhận broadcast từ Service/BatteryReceiver khi có sự kiện (auto-finish, target reached)
    private val sessionChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ChargingForegroundService.ACTION_SESSION_CHANGED) {
                reloadSessionState()
            }
        }
    }
    private var sessionReceiverRegistered = false

    // Xin quyền POST_NOTIFICATIONS (Android 13+)
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startChargingServiceIfNeeded()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        targetPctState.intValue = sessionManager.getTargetPct()
        languageState.value = sessionManager.getLanguage()
        autoLaunchState.value = sessionManager.isAutoLaunchEnabled()
        recentSessionsState.value = sessionManager.getRecentSessions()
        deletedSessionsState.value = sessionManager.getDeletedSessions()

        updateBatteryInfo()
        requestNotificationPermissionAndStartService()

        setContent {
            SimpleChargeTheme {
                val batteryPct by batteryPctState
                val isCharging by isChargingState
                val plugType by plugTypeState
                val currentMa by currentMaState
                val temperatureC by temperatureCState
                val voltageV by voltageVState
                val targetPct by targetPctState
                val language by languageState
                val autoLaunch by autoLaunchState
                val activeSession by activeSessionState
                val elapsedMs by elapsedMsState
                val recentSessions by recentSessionsState
                val deletedSessions by deletedSessionsState

                // Coroutine ticker: cập nhật elapsed time mỗi giây CHỈ KHI ĐANG CẮM SẠC & SESSION ĐANG ACTIVE
                LaunchedEffect(isCharging, activeSession.isActive) {
                    while (true) {
                        if (isCharging && activeSession.isActive) {
                            elapsedMsState.longValue = (System.currentTimeMillis() - activeSession.startTimeMs).coerceAtLeast(0L)
                        } else {
                            elapsedMsState.longValue = 0L
                        }
                        updateBatteryInfo()
                        delay(1000L)
                    }
                }

                SimpleChargeApp(
                    batteryPct = batteryPct,
                    isCharging = isCharging,
                    plugType = plugType,
                    currentMa = currentMa,
                    temperatureC = temperatureC,
                    voltageV = voltageV,
                    targetPct = targetPct,
                    language = language,
                    autoLaunchEnabled = autoLaunch,
                    activeSession = activeSession,
                    elapsedMs = elapsedMs,
                    recentSessions = recentSessions,
                    deletedSessions = deletedSessions,
                    onLanguageChanged = { newLang ->
                        languageState.value = newLang
                        sessionManager.setLanguage(newLang)
                    },
                    onAutoLaunchChanged = { enabled ->
                        autoLaunchState.value = enabled
                        sessionManager.setAutoLaunchEnabled(enabled)
                    },
                    onTargetPctChanged = { newTarget ->
                        targetPctState.intValue = newTarget
                        sessionManager.setTargetPct(newTarget)
                        activeSessionState.value = sessionManager.getActiveSession()
                        if (isChargingState.value) {
                            NotificationHelper.showChargingNotification(this@MainActivity)
                        }
                    },
                    onFinishSession = {
                        val session = sessionManager.finishSession(batteryPct, temperatureC)
                        if (session != null) {
                            recentSessionsState.value = sessionManager.getRecentSessions()
                            NotificationHelper.showDisconnectedNotification(this@MainActivity, batteryPct, session)
                        } else {
                            NotificationHelper.cancelNotification(this@MainActivity)
                        }
                        ChargingForegroundService.stop(this@MainActivity)
                        activeSessionState.value = sessionManager.getActiveSession()
                        elapsedMsState.longValue = 0L
                    },
                    onResetSession = {
                        val session = sessionManager.resetActiveSession(batteryPct, temperatureC)
                        activeSessionState.value = session
                        elapsedMsState.longValue = 0L
                        if (isChargingState.value) {
                            NotificationHelper.showChargingNotification(this@MainActivity)
                        }
                    },
                    onSimulateLevelStep = { step ->
                        simulatedBonusLevel += step
                        updateBatteryInfo()
                    },
                    onClearHistory = {
                        sessionManager.moveAllToTrash()
                        reloadSessionState()
                    },
                    onMoveSessionToTrash = { id ->
                        sessionManager.moveToTrash(id)
                        reloadSessionState()
                    },
                    onRestoreSession = { id ->
                        sessionManager.restoreSession(id)
                        reloadSessionState()
                    },
                    onRestoreAllSessions = {
                        sessionManager.restoreAllSessions()
                        reloadSessionState()
                    },
                    onPermanentlyDeleteSession = { id ->
                        sessionManager.permanentlyDeleteSession(id)
                        reloadSessionState()
                    },
                    onClearTrash = {
                        sessionManager.clearTrash()
                        reloadSessionState()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)

        if (!sessionReceiverRegistered) {
            val sessionFilter = IntentFilter(ChargingForegroundService.ACTION_SESSION_CHANGED)
            ContextCompat.registerReceiver(
                this,
                sessionChangedReceiver,
                sessionFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            sessionReceiverRegistered = true
        }

        reloadSessionState()
        updateBatteryInfo()
    }

    override fun onPause() {
        super.onPause()
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        if (sessionReceiverRegistered) {
            try { unregisterReceiver(sessionChangedReceiver) } catch (_: Exception) {}
            sessionReceiverRegistered = false
        }
    }

    private fun reloadSessionState() {
        activeSessionState.value = sessionManager.getActiveSession()
        recentSessionsState.value = sessionManager.getRecentSessions()
        deletedSessionsState.value = sessionManager.getDeletedSessions()
        if (!activeSessionState.value.isActive || !isChargingState.value) {
            elapsedMsState.longValue = 0L
        }
    }

    private fun updateBatteryInfo() {
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val basePct = if (rawLevel >= 0 && scale > 0) {
            (rawLevel * 100) / scale
        } else {
            NotificationHelper.getBatteryPct(this)
        }
        val effectivePct = (basePct + simulatedBonusLevel).coerceIn(0, 100)
        batteryPctState.intValue = effectivePct

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val isCharging = chargePlug > 0 && NotificationHelper.isBatteryCharging(this)
        val plugType = NotificationHelper.parsePlugType(chargePlug, isCharging)

        isChargingState.value = isCharging
        plugTypeState.value = plugType

        val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        temperatureCState.floatValue = rawTemp / 10f
        val rawVolt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        voltageVState.floatValue = rawVolt / 1000f

        val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val rawCurrent = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        val currentMa = if (rawCurrent != 0 && rawCurrent != Integer.MIN_VALUE) {
            val absCurrent = kotlin.math.abs(rawCurrent)
            if (absCurrent > 10000) absCurrent / 1000 else absCurrent
        } else null
        currentMaState.value = currentMa

        // Session logic
        if (isCharging) {
            val session = sessionManager.startOrResumeSession(effectivePct, temperatureCState.floatValue)
            activeSessionState.value = session
            sessionManager.recordDataPoint(effectivePct, temperatureCState.floatValue)

            val completed = sessionManager.checkAndHandleBatteryLevel(effectivePct, temperatureCState.floatValue)
            if (completed != null) {
                recentSessionsState.value = sessionManager.getRecentSessions()
                activeSessionState.value = sessionManager.getActiveSession()
                NotificationHelper.showTargetReachedNotification(this, effectivePct, targetPctState.intValue)
            }
        } else {
            // ✅ Tự động finish session khi rút sạc (khi app đang mở)
            val active = sessionManager.getActiveSession()
            if (active.isActive) {
                val session = sessionManager.finishSession(effectivePct, temperatureCState.floatValue)
                if (session != null) {
                    NotificationHelper.showDisconnectedNotification(this, effectivePct, session)
                }
                ChargingForegroundService.stop(this)
            }
            activeSessionState.value = sessionManager.getActiveSession()
            recentSessionsState.value = sessionManager.getRecentSessions()
            elapsedMsState.longValue = 0L
        }
    }

    private fun requestNotificationPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startChargingServiceIfNeeded()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            startChargingServiceIfNeeded()
        }
    }

    private fun startChargingServiceIfNeeded() {
        if (isChargingState.value) {
            NotificationHelper.showChargingNotification(this)
            ChargingForegroundService.start(this)
        }
    }
}
