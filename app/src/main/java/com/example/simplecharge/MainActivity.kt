package com.example.simplecharge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.simplecharge.data.ActiveSessionState
import com.example.simplecharge.data.ChargingSession
import com.example.simplecharge.data.SessionManager
import com.example.simplecharge.ui.SimpleChargeApp
import com.example.simplecharge.ui.theme.SimpleChargeTheme
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
    private var activeSessionState = mutableStateOf(ActiveSessionState())
    private var elapsedMsState = mutableLongStateOf(0L)
    private var recentSessionsState = mutableStateOf<List<ChargingSession>>(emptyList())

    // For manual simulation in testing
    private var simulatedBonusLevel = 0

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateBatteryInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        targetPctState.intValue = sessionManager.getTargetPct()
        languageState.value = sessionManager.getLanguage()
        recentSessionsState.value = sessionManager.getRecentSessions()

        // Read initial battery status
        updateBatteryInfo()

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
                val activeSession by activeSessionState
                val elapsedMs by elapsedMsState
                val recentSessions by recentSessionsState

                // Coroutine ticker: updates elapsed time and checks battery every second
                LaunchedEffect(isCharging, activeSession.isActive) {
                    while (true) {
                        if (activeSession.isActive) {
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
                    activeSession = activeSession,
                    elapsedMs = elapsedMs,
                    recentSessions = recentSessions,
                    onLanguageChanged = { newLang ->
                        languageState.value = newLang
                        sessionManager.setLanguage(newLang)
                    },
                    onTargetPctChanged = { newTarget ->
                        targetPctState.intValue = newTarget
                        sessionManager.setTargetPct(newTarget)
                        activeSessionState.value = sessionManager.getActiveSession()
                    },
                    onFinishSession = {
                        val session = sessionManager.finishSession(batteryPct)
                        if (session != null) {
                            recentSessionsState.value = sessionManager.getRecentSessions()
                        }
                        activeSessionState.value = sessionManager.getActiveSession()
                        elapsedMsState.longValue = 0L
                    },
                    onResetSession = {
                        val session = sessionManager.resetActiveSession(batteryPct)
                        activeSessionState.value = session
                        elapsedMsState.longValue = 0L
                    },
                    onSimulateLevelStep = { step ->
                        simulatedBonusLevel += step
                        updateBatteryInfo()
                    },
                    onClearHistory = {
                        sessionManager.clearHistory()
                        recentSessionsState.value = emptyList()
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
        updateBatteryInfo()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
    }

    private fun updateBatteryInfo() {
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val basePct = if (scale > 0) (rawLevel * 100) / scale else rawLevel
        val effectivePct = (basePct + simulatedBonusLevel).coerceIn(0, 100)
        batteryPctState.intValue = effectivePct

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val plugType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> if (isCharging) "Charging" else "Unplugged"
        }
        isChargingState.value = isCharging
        plugTypeState.value = plugType

        // Temperature & Voltage
        val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        temperatureCState.floatValue = rawTemp / 10f
        val rawVolt = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        voltageVState.floatValue = rawVolt / 1000f

        // Battery Current (mA)
        val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val rawCurrent = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        val currentMa = if (rawCurrent != 0 && rawCurrent != Integer.MIN_VALUE) {
            val absCurrent = kotlin.math.abs(rawCurrent)
            if (absCurrent > 10000) absCurrent / 1000 else absCurrent
        } else null
        currentMaState.value = currentMa

        // Session logic
        if (isCharging) {
            val session = sessionManager.startOrResumeSession(effectivePct)
            activeSessionState.value = session

            // Check if battery level reached target
            val completed = sessionManager.checkAndHandleBatteryLevel(effectivePct)
            if (completed != null) {
                recentSessionsState.value = sessionManager.getRecentSessions()
                activeSessionState.value = sessionManager.getActiveSession()
            }
        } else {
            activeSessionState.value = sessionManager.getActiveSession()
        }
    }
}
