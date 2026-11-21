package com.example.simplecharge.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "simple_charge_prefs"
        private const val KEY_TARGET_PCT = "target_charge_pct"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_ACTIVE_EXISTS = "active_exists"
        private const val KEY_ACTIVE_START_TIME = "active_start_time"
        private const val KEY_ACTIVE_START_LEVEL = "active_start_level"
        private const val KEY_ACTIVE_TARGET_PCT = "active_target_pct"
        private const val KEY_ACTIVE_REACHED = "active_reached"
        private const val KEY_COMPLETED_SESSIONS = "completed_sessions"

        const val DEFAULT_TARGET_PCT = 90
        const val DEFAULT_LANGUAGE = "en"
        const val MIN_TARGET_PCT = 50
        const val MAX_TARGET_PCT = 100
    }

    fun getLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun getTargetPct(): Int {
        return prefs.getInt(KEY_TARGET_PCT, DEFAULT_TARGET_PCT)
    }

    fun setTargetPct(target: Int) {
        val clamped = target.coerceIn(MIN_TARGET_PCT, MAX_TARGET_PCT)
        prefs.edit().putInt(KEY_TARGET_PCT, clamped).apply()
        // If an active session is in progress, also update its target if not reached yet
        if (hasActiveSession() && !isActiveTargetReached()) {
            prefs.edit().putInt(KEY_ACTIVE_TARGET_PCT, clamped).apply()
        }
    }

    fun hasActiveSession(): Boolean {
        return prefs.getBoolean(KEY_ACTIVE_EXISTS, false)
    }

    fun isActiveTargetReached(): Boolean {
        return prefs.getBoolean(KEY_ACTIVE_REACHED, false)
    }

    fun getActiveSession(): ActiveSessionState {
        val exists = prefs.getBoolean(KEY_ACTIVE_EXISTS, false)
        if (!exists) {
            return ActiveSessionState(
                isActive = false,
                startTimeMs = 0L,
                startLevel = 0,
                targetPct = getTargetPct(),
                isTargetReached = false
            )
        }
        return ActiveSessionState(
            isActive = true,
            startTimeMs = prefs.getLong(KEY_ACTIVE_START_TIME, System.currentTimeMillis()),
            startLevel = prefs.getInt(KEY_ACTIVE_START_LEVEL, 0),
            targetPct = prefs.getInt(KEY_ACTIVE_TARGET_PCT, getTargetPct()),
            isTargetReached = prefs.getBoolean(KEY_ACTIVE_REACHED, false)
        )
    }

    fun startOrResumeSession(currentLevel: Int): ActiveSessionState {
        if (!hasActiveSession()) {
            val now = System.currentTimeMillis()
            val target = getTargetPct()
            prefs.edit()
                .putBoolean(KEY_ACTIVE_EXISTS, true)
                .putLong(KEY_ACTIVE_START_TIME, now)
                .putInt(KEY_ACTIVE_START_LEVEL, currentLevel)
                .putInt(KEY_ACTIVE_TARGET_PCT, target)
                .putBoolean(KEY_ACTIVE_REACHED, false)
                .apply()
            return ActiveSessionState(
                isActive = true,
                startTimeMs = now,
                startLevel = currentLevel,
                targetPct = target,
                isTargetReached = false
            )
        }
        return getActiveSession()
    }

    fun resetActiveSession(currentLevel: Int): ActiveSessionState {
        val now = System.currentTimeMillis()
        val target = getTargetPct()
        prefs.edit()
            .putBoolean(KEY_ACTIVE_EXISTS, true)
            .putLong(KEY_ACTIVE_START_TIME, now)
            .putInt(KEY_ACTIVE_START_LEVEL, currentLevel)
            .putInt(KEY_ACTIVE_TARGET_PCT, target)
            .putBoolean(KEY_ACTIVE_REACHED, false)
            .apply()
        return ActiveSessionState(
            isActive = true,
            startTimeMs = now,
            startLevel = currentLevel,
            targetPct = target,
            isTargetReached = false
        )
    }

    fun checkAndHandleBatteryLevel(currentLevel: Int): ChargingSession? {
        val active = getActiveSession()
        if (active.isActive && !active.isTargetReached) {
            if (currentLevel >= active.targetPct) {
                // Target reached! Mark reached and save session
                prefs.edit().putBoolean(KEY_ACTIVE_REACHED, true).apply()
                return finishSession(currentLevel)
            }
        }
        return null
    }

    fun finishSession(endLevel: Int): ChargingSession? {
        val active = getActiveSession()
        if (!active.isActive) return null

        val session = ChargingSession(
            startTimeMs = active.startTimeMs,
            endTimeMs = System.currentTimeMillis(),
            startLevel = active.startLevel,
            endLevel = endLevel,
            targetPct = active.targetPct
        )

        saveCompletedSession(session)
        clearActiveSession()
        return session
    }

    fun clearActiveSession() {
        prefs.edit()
            .putBoolean(KEY_ACTIVE_EXISTS, false)
            .remove(KEY_ACTIVE_START_TIME)
            .remove(KEY_ACTIVE_START_LEVEL)
            .remove(KEY_ACTIVE_TARGET_PCT)
            .remove(KEY_ACTIVE_REACHED)
            .apply()
    }

    fun getRecentSessions(): List<ChargingSession> {
        val raw = prefs.getString(KEY_COMPLETED_SESSIONS, null) ?: return emptyList()
        val list = mutableListOf<ChargingSession>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(ChargingSession.fromJson(obj))
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun saveCompletedSession(session: ChargingSession) {
        val existing = getRecentSessions().toMutableList()
        existing.add(0, session) // newest first
        val trimmed = existing.take(30)
        val array = JSONArray()
        for (item in trimmed) {
            array.put(item.toJson())
        }
        prefs.edit().putString(KEY_COMPLETED_SESSIONS, array.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_COMPLETED_SESSIONS).apply()
    }
}
