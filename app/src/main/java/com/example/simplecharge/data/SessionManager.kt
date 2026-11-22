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
        private const val KEY_ACTIVE_DATA_POINTS = "active_data_points"
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

    fun startOrResumeSession(currentLevel: Int, currentTemp: Float = 0f): ActiveSessionState {
        if (!hasActiveSession()) {
            val now = System.currentTimeMillis()
            val target = getTargetPct()
            prefs.edit()
                .putBoolean(KEY_ACTIVE_EXISTS, true)
                .putLong(KEY_ACTIVE_START_TIME, now)
                .putInt(KEY_ACTIVE_START_LEVEL, currentLevel)
                .putInt(KEY_ACTIVE_TARGET_PCT, target)
                .putBoolean(KEY_ACTIVE_REACHED, false)
                .remove(KEY_ACTIVE_DATA_POINTS)
                .apply()

            // Ghi nhận điểm dữ liệu đầu tiên
            recordDataPoint(currentLevel, currentTemp)

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

    fun resetActiveSession(currentLevel: Int, currentTemp: Float = 0f): ActiveSessionState {
        val now = System.currentTimeMillis()
        val target = getTargetPct()
        prefs.edit()
            .putBoolean(KEY_ACTIVE_EXISTS, true)
            .putLong(KEY_ACTIVE_START_TIME, now)
            .putInt(KEY_ACTIVE_START_LEVEL, currentLevel)
            .putInt(KEY_ACTIVE_TARGET_PCT, target)
            .putBoolean(KEY_ACTIVE_REACHED, false)
            .remove(KEY_ACTIVE_DATA_POINTS)
            .apply()

        recordDataPoint(currentLevel, currentTemp)

        return ActiveSessionState(
            isActive = true,
            startTimeMs = now,
            startLevel = currentLevel,
            targetPct = target,
            isTargetReached = false
        )
    }

    /**
     * Ghi nhận một điểm dữ liệu (mức pin + nhiệt độ) cho phiên sạc hiện tại.
     * Tự động lọc để không ghi quá dày (chỉ ghi khi mức pin đổi hoặc sau 60 giây).
     */
    fun recordDataPoint(level: Int, tempC: Float = 0f) {
        if (!hasActiveSession()) return

        val now = System.currentTimeMillis()
        val points = getActiveDataPoints().toMutableList()

        if (points.isNotEmpty()) {
            val last = points.last()
            // Nếu cùng mức pin và chưa đủ 60 giây thì bỏ qua để tiết kiệm bộ nhớ
            if (last.level == level && (now - last.timestampMs) < 60_000L) {
                return
            }
        }

        points.add(ChargingDataPoint(now, level, tempC))
        // Giới hạn tối đa 120 điểm mỗi phiên sạc
        val trimmed = if (points.size > 120) points.takeLast(120) else points

        val array = JSONArray()
        trimmed.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ACTIVE_DATA_POINTS, array.toString()).apply()
    }

    fun getActiveDataPoints(): List<ChargingDataPoint> {
        val raw = prefs.getString(KEY_ACTIVE_DATA_POINTS, null) ?: return emptyList()
        val list = mutableListOf<ChargingDataPoint>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                list.add(ChargingDataPoint.fromJson(array.getJSONObject(i)))
            }
        } catch (_: Exception) {}
        return list
    }

    fun checkAndHandleBatteryLevel(currentLevel: Int, currentTemp: Float = 0f): ChargingSession? {
        val active = getActiveSession()
        if (active.isActive && !active.isTargetReached) {
            recordDataPoint(currentLevel, currentTemp)
            if (currentLevel >= active.targetPct) {
                prefs.edit().putBoolean(KEY_ACTIVE_REACHED, true).apply()
                return finishSession(currentLevel, currentTemp)
            }
        }
        return null
    }

    fun finishSession(endLevel: Int, endTemp: Float = 0f): ChargingSession? {
        val active = getActiveSession()
        if (!active.isActive) return null

        // Ghi điểm cuối cùng
        recordDataPoint(endLevel, endTemp)
        val dataPoints = getActiveDataPoints()

        val session = ChargingSession(
            startTimeMs = active.startTimeMs,
            endTimeMs = System.currentTimeMillis(),
            startLevel = active.startLevel,
            endLevel = endLevel,
            targetPct = active.targetPct,
            dataPoints = dataPoints
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
            .remove(KEY_ACTIVE_DATA_POINTS)
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
        } catch (_: Exception) {}
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
