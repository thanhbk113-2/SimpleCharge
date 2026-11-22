package com.example.simplecharge.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simplecharge.data.ChargingDataPoint
import com.example.simplecharge.data.ChargingSession
import com.example.simplecharge.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingSessionDetailSheet(
    session: ChargingSession,
    strings: AppStrings,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Slate600)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: Title + Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.sessionDetailsTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    val dateFormat = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
                    val timeStr = dateFormat.format(Date(session.startTimeMs))
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Slate800)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = strings.close,
                        tint = Slate300,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Section
            Text(
                text = strings.chargingCurveTitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Biểu đồ Canvas mượt mà, trực quan
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    ChargingCurveChart(
                        session = session,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Thống kê cốt lõi dạng Grid 2x2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = strings.chargingSpeed,
                    value = String.format(Locale.US, "%.1f%% / phút", session.avgSpeedPctPerMin),
                    sub = String.format(Locale.US, "~%d%% / giờ", (session.avgSpeedPctPerMin * 60).roundToInt()),
                    icon = Icons.Default.Bolt,
                    iconTint = Emerald400,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = strings.chargingDuration,
                    value = formatDuration(session.durationMs),
                    sub = "+${session.levelGained}% pin",
                    icon = Icons.Default.Timer,
                    iconTint = Cyan400,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = strings.batteryGained,
                    value = "${session.startLevel}% → ${session.endLevel}%",
                    sub = "Mục tiêu: ${session.targetPct}%",
                    icon = Icons.Default.BatteryChargingFull,
                    iconTint = Cyan400,
                    modifier = Modifier.weight(1f)
                )

                val maxT = session.maxTemp
                val minT = session.minTemp
                val tempText = if (maxT > 5f) {
                    String.format(Locale.US, "%.1f°C", maxT)
                } else "--"
                val tempSub = if (minT > 5f && maxT > 5f) {
                    String.format(Locale.US, "Ban đầu: %.1f°C", minT)
                } else "Bình thường"

                StatCard(
                    title = strings.tempRange,
                    value = tempText,
                    sub = tempSub,
                    icon = Icons.Default.Thermostat,
                    iconTint = if (maxT > 40f) Amber400 else Emerald400,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Smart Note: Ghi chú thông minh, dễ hiểu
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate800.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = buildSmartInsight(session),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate300,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Slate800,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 11.sp),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    color = Slate400
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Biểu đồ đường cong tiến trình sạc:
 * - Đơn giản, rõ ràng, dễ hiểu
 * - Có mốc thời gian và mốc pin %
 * - Tương tác chạm để xem điểm cụ thể
 */
@Composable
fun ChargingCurveChart(
    session: ChargingSession,
    modifier: Modifier = Modifier
) {
    val rawPoints = session.validPoints
    // Chuẩn hóa điểm dữ liệu để vẽ
    val points = remember(rawPoints) {
        if (rawPoints.size >= 2) {
            rawPoints.sortedBy { it.timestampMs }
        } else {
            listOf(
                ChargingDataPoint(session.startTimeMs, session.startLevel, 0f),
                ChargingDataPoint(session.endTimeMs, session.endLevel, 0f)
            )
        }
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val startMs = points.first().timestampMs
    val totalDurationMs = (points.last().timestampMs - startMs).coerceAtLeast(60_000L)

    val minLevel = (points.minOf { it.level } - 5).coerceAtLeast(0)
    val maxLevel = (points.maxOf { it.level } + 5).coerceAtMost(100)
    val levelRange = (maxLevel - minLevel).coerceAtLeast(10)

    Column(modifier = modifier) {
        // Tooltip hiển thị khi người dùng chạm vào biểu đồ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selectedIndex != null && selectedIndex!! in points.indices) {
                val pt = points[selectedIndex!!]
                val elapsedMins = ((pt.timestampMs - startMs) / 60000.0).roundToInt()
                val tempStr = if (pt.tempC > 5f) " • 🌡️ ${String.format(Locale.US, "%.1f°C", pt.tempC)}" else ""
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Emerald500.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400)
                ) {
                    Text(
                        text = "⏱ +$elapsedMins phút  •  🔋 ${pt.level}%$tempStr",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Emerald400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    text = "Chạm vào biểu đồ để xem chi tiết từng thời điểm",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 10.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Vùng vẽ Canvas chính
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val paddingX = 24.dp.toPx()
                        val chartW = width - paddingX * 2
                        val fraction = ((offset.x - paddingX) / chartW).coerceIn(0f, 1f)
                        val targetTime = startMs + (fraction * totalDurationMs).toLong()
                        val nearestIdx = points.indices.minByOrNull {
                            kotlin.math.abs(points[it].timestampMs - targetTime)
                        } ?: 0
                        selectedIndex = nearestIdx
                    }
                }
                .pointerInput(points) {
                    detectDragGestures { change, _ ->
                        val width = size.width
                        val paddingX = 24.dp.toPx()
                        val chartW = width - paddingX * 2
                        val fraction = ((change.position.x - paddingX) / chartW).coerceIn(0f, 1f)
                        val targetTime = startMs + (fraction * totalDurationMs).toLong()
                        val nearestIdx = points.indices.minByOrNull {
                            kotlin.math.abs(points[it].timestampMs - targetTime)
                        } ?: 0
                        selectedIndex = nearestIdx
                    }
                }
        ) {
            val paddingX = 24.dp.toPx()
            val paddingY = 16.dp.toPx()
            val w = size.width - paddingX * 2
            val h = size.height - paddingY * 2

            // Vẽ các đường lưới ngang phụ (Dashed Grid Lines)
            val gridCount = 3
            for (i in 0..gridCount) {
                val y = paddingY + h * (1f - i.toFloat() / gridCount)
                drawLine(
                    color = Slate700.copy(alpha = 0.5f),
                    start = Offset(paddingX, y),
                    end = Offset(size.width - paddingX, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }

            // Chuyển đổi các điểm dữ liệu thành tọa độ màn hình
            val pixelPoints = points.map { pt ->
                val timeFrac = if (totalDurationMs > 0) {
                    ((pt.timestampMs - startMs).toFloat() / totalDurationMs).coerceIn(0f, 1f)
                } else 0f
                val levelFrac = ((pt.level - minLevel).toFloat() / levelRange).coerceIn(0f, 1f)
                val x = paddingX + timeFrac * w
                val y = paddingY + h * (1f - levelFrac)
                Offset(x, y)
            }

            // Tạo Path đường cong
            val linePath = Path()
            val fillPath = Path()

            if (pixelPoints.isNotEmpty()) {
                linePath.moveTo(pixelPoints.first().x, pixelPoints.first().y)
                fillPath.moveTo(pixelPoints.first().x, size.height - paddingY)
                fillPath.lineTo(pixelPoints.first().x, pixelPoints.first().y)

                for (i in 1 until pixelPoints.size) {
                    val p0 = pixelPoints[i - 1]
                    val p1 = pixelPoints[i]
                    // Vẽ đường nối mượt mà
                    val controlX = (p0.x + p1.x) / 2f
                    linePath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }

                fillPath.lineTo(pixelPoints.last().x, size.height - paddingY)
                fillPath.close()

                // 1. Vẽ Gradient bóng bên dưới đường cong
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Emerald400.copy(alpha = 0.25f), Color.Transparent),
                        startY = paddingY,
                        endY = size.height - paddingY
                    )
                )

                // 2. Vẽ đường cong sạc chính (Emerald)
                drawPath(
                    path = linePath,
                    color = Emerald400,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 3. Vẽ điểm đầu và điểm cuối
                val first = pixelPoints.first()
                val last = pixelPoints.last()
                drawCircle(color = Cyan400, radius = 5.dp.toPx(), center = first)
                drawCircle(color = Emerald400, radius = 6.dp.toPx(), center = last)
                drawCircle(color = Slate900, radius = 2.5.dp.toPx(), center = last)

                // 4. Nếu người dùng đang chạm chọn 1 điểm
                selectedIndex?.let { idx ->
                    if (idx in pixelPoints.indices) {
                        val sel = pixelPoints[idx]
                        // Đường dọi dọc
                        drawLine(
                            color = Emerald400.copy(alpha = 0.6f),
                            start = Offset(sel.x, paddingY),
                            end = Offset(sel.x, size.height - paddingY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                        // Chấm tròn nổi bật
                        drawCircle(color = Color.White, radius = 7.dp.toPx(), center = sel)
                        drawCircle(color = Emerald500, radius = 4.dp.toPx(), center = sel)
                    }
                }
            }
        }

        // Trục hoành: Mốc thời gian bắt đầu và kết thúc
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val totalMins = (totalDurationMs / 60000.0).roundToInt()
            Text(
                text = "0 phút (${points.first().level}%)",
                style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp)
            )
            if (totalMins > 10) {
                Text(
                    text = "${totalMins / 2}p",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate500, fontSize = 10.sp)
                )
            }
            Text(
                text = "${totalMins} phút (${points.last().level}%)",
                style = MaterialTheme.typography.labelSmall.copy(color = Emerald400, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            )
        }
    }
}

/**
 * Tạo nhận xét thông minh, tự nhiên về phiên sạc
 */
fun buildSmartInsight(session: ChargingSession): String {
    val speed = session.avgSpeedPctPerMin
    val maxT = session.maxTemp
    val levelGained = session.levelGained

    val speedDesc = when {
        speed >= 1.2f -> "⚡ Tốc độ sạc rất nhanh (~${(speed * 60).roundToInt()}%/giờ), công suất nạp pin tối ưu."
        speed >= 0.7f -> "Ổn định với tốc độ nạp chuẩn (~${(speed * 60).roundToInt()}%/giờ)."
        else -> "Tốc độ sạc chậm và êm ái, phù hợp sạc qua cổng USB."
    }

    val tempDesc = when {
        maxT > 41f -> "⚠️ Pin đạt mức nhiệt khá cao (${String.format(Locale.US, "%.1f°C", maxT)}), nên tháo ốp lưng khi sạc."
        maxT > 5f -> "✅ Nhiệt độ pin mát mẻ (${String.format(Locale.US, "%.1f°C", maxT)}), rất tốt cho tuổi thọ pin."
        else -> "Pin tăng đều đặn +$levelGained% trong suốt phiên sạc."
    }

    return "$speedDesc $tempDesc"
}
