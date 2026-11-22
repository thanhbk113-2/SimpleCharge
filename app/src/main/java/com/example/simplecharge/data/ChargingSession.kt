package com.example.simplecharge.data

import org.json.JSONArray
import org.json.JSONObject

data class ChargingDataPoint(
    val timestampMs: Long,
    val level: Int,
    val tempC: Float
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("t", timestampMs)
        put("l", level)
        put("c", tempC.toDouble())
    }

    companion object {
        fun fromJson(json: JSONObject): ChargingDataPoint {
            return ChargingDataPoint(
                timestampMs = json.optLong("t", 0L),
                level = json.optInt("l", 0),
                tempC = json.optDouble("c", 0.0).toFloat()
            )
        }
    }
}

data class ChargingSession(
    val id: String = System.currentTimeMillis().toString(),
    val startTimeMs: Long,
    val endTimeMs: Long,
    val startLevel: Int,
    val endLevel: Int,
    val targetPct: Int,
    val dataPoints: List<ChargingDataPoint> = emptyList()
) {
    val durationMs: Long
        get() = (endTimeMs - startTimeMs).coerceAtLeast(0)

    val levelGained: Int
        get() = (endLevel - startLevel).coerceAtLeast(0)

    val validPoints: List<ChargingDataPoint>
        get() {
            if (dataPoints.isNotEmpty()) return dataPoints
            // Fallback nếu phiên sạc cũ chưa có dataPoints: tự sinh 2 điểm đầu & cuối
            return listOf(
                ChargingDataPoint(startTimeMs, startLevel, 0f),
                ChargingDataPoint(endTimeMs, endLevel, 0f)
            )
        }

    val minTemp: Float
        get() {
            val valid = dataPoints.map { it.tempC }.filter { it > 5f }
            return valid.minOrNull() ?: 0f
        }

    val maxTemp: Float
        get() {
            val valid = dataPoints.map { it.tempC }.filter { it > 5f }
            return valid.maxOrNull() ?: 0f
        }

    val avgSpeedPctPerMin: Float
        get() {
            val mins = durationMs / 60000.0
            return if (mins > 0 && levelGained > 0) (levelGained / mins).toFloat() else 0f
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("startTimeMs", startTimeMs)
        put("endTimeMs", endTimeMs)
        put("startLevel", startLevel)
        put("endLevel", endLevel)
        put("targetPct", targetPct)
        if (dataPoints.isNotEmpty()) {
            val arr = JSONArray()
            dataPoints.forEach { arr.put(it.toJson()) }
            put("dataPoints", arr)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ChargingSession {
            val dpList = mutableListOf<ChargingDataPoint>()
            val arr = json.optJSONArray("dataPoints")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    dpList.add(ChargingDataPoint.fromJson(arr.getJSONObject(i)))
                }
            }
            return ChargingSession(
                id = json.optString("id", System.currentTimeMillis().toString()),
                startTimeMs = json.optLong("startTimeMs", 0L),
                endTimeMs = json.optLong("endTimeMs", 0L),
                startLevel = json.optInt("startLevel", 0),
                endLevel = json.optInt("endLevel", 0),
                targetPct = json.optInt("targetPct", 90),
                dataPoints = dpList
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
