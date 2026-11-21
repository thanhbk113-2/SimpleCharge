package com.example.simplecharge.data

import org.json.JSONObject

data class ChargingSession(
    val id: String = System.currentTimeMillis().toString(),
    val startTimeMs: Long,
    val endTimeMs: Long,
    val startLevel: Int,
    val endLevel: Int,
    val targetPct: Int
) {
    val durationMs: Long
        get() = (endTimeMs - startTimeMs).coerceAtLeast(0)

    val levelGained: Int
        get() = (endLevel - startLevel).coerceAtLeast(0)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("startTimeMs", startTimeMs)
        put("endTimeMs", endTimeMs)
        put("startLevel", startLevel)
        put("endLevel", endLevel)
        put("targetPct", targetPct)
    }

    companion object {
        fun fromJson(json: JSONObject): ChargingSession {
            return ChargingSession(
                id = json.optString("id", System.currentTimeMillis().toString()),
                startTimeMs = json.optLong("startTimeMs", 0L),
                endTimeMs = json.optLong("endTimeMs", 0L),
                startLevel = json.optInt("startLevel", 0),
                endLevel = json.optInt("endLevel", 0),
                targetPct = json.optInt("targetPct", 90)
            )
        }
    }
}

data class ActiveSessionState(
    val isActive: Boolean = false,
    val startTimeMs: Long = 0L,
    val startLevel: Int = 0,
    val targetPct: Int = 90,
    val isTargetReached: Boolean = false
)
