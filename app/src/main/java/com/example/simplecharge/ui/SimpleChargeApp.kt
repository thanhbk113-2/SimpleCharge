package com.example.simplecharge.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simplecharge.data.ActiveSessionState
import com.example.simplecharge.data.ChargingSession
import com.example.simplecharge.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleChargeApp(
    batteryPct: Int,
    isCharging: Boolean,
    plugType: String,
    currentMa: Int?,
    temperatureC: Float,
    voltageV: Float,
    targetPct: Int,
    language: String,
    activeSession: ActiveSessionState,
    elapsedMs: Long,
    recentSessions: List<ChargingSession>,
    onLanguageChanged: (String) -> Unit,
    onTargetPctChanged: (Int) -> Unit,
    onFinishSession: () -> Unit,
    onResetSession: () -> Unit,
    onSimulateLevelStep: (Int) -> Unit,
    onClearHistory: () -> Unit
) {
    val strings = remember(language) { Localization.get(language) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isCharging) Emerald500.copy(alpha = 0.2f) else Slate700),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCharging) Icons.Default.Bolt else Icons.Default.BatteryStd,
                                contentDescription = null,
                                tint = if (isCharging) Emerald400 else Slate400,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SimpleCharge",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = strings.appSubtitle,
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate400),
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        // Language switcher toggle chip [ EN | VI ]
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Slate800,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val next = if (language.equals("en", ignoreCase = true)) "vi" else "en"
                                    onLanguageChanged(next)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Default.Language,
                                    contentDescription = "Switch language",
                                    tint = Cyan400,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "EN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (language == "en") FontWeight.Bold else FontWeight.Normal,
                                        color = if (language == "en") Emerald400 else Slate400
                                    )
                                )
                                Text(
                                    text = "|",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = Slate600
                                    )
                                )
                                Text(
                                    text = "VI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = if (language == "vi") FontWeight.Bold else FontWeight.Normal,
                                        color = if (language == "vi") Emerald400 else Slate400
                                    )
                                )
                            }
                        }

                        // Charging status chip
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCharging) Emerald500.copy(alpha = 0.15f) else Slate700.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCharging) Emerald500.copy(alpha = 0.4f) else Slate600
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isCharging) Emerald400 else Slate400)
                                )
                                Text(
                                    text = if (isCharging) strings.charging else strings.unplugged,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = if (isCharging) Emerald400 else Slate400,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate900
                )
            )
        },
        containerColor = Slate900
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // 1. Current Battery & Status Card
            item {
                BatteryStatusCard(
                    batteryPct = batteryPct,
                    targetPct = targetPct,
                    isCharging = isCharging,
                    plugType = plugType,
                    currentMa = currentMa,
                    temperatureC = temperatureC,
                    voltageV = voltageV,
                    strings = strings
                )
            }

            // 2. Timer & Estimated Remaining Card
            item {
                TimerMetricsCard(
                    isCharging = isCharging,
                    batteryPct = batteryPct,
                    targetPct = targetPct,
                    activeSession = activeSession,
                    elapsedMs = elapsedMs,
                    strings = strings
                )
            }

            // 3. Target Charge % Controls
            item {
                TargetChargeCard(
                    targetPct = targetPct,
                    strings = strings,
                    onTargetPctChanged = onTargetPctChanged
                )
            }

            // 4. Manual Testing & Controls
            item {
                TestControlsCard(
                    activeSession = activeSession,
                    strings = strings,
                    onFinishSession = onFinishSession,
                    onResetSession = onResetSession,
                    onSimulateLevelStep = onSimulateLevelStep
                )
            }

            // 5. Recent Charging Sessions
            item {
                RecentSessionsCard(
                    recentSessions = recentSessions,
                    strings = strings,
                    onClearHistory = onClearHistory
                )
            }
        }
    }
}

@Composable
fun BatteryStatusCard(
    batteryPct: Int,
    targetPct: Int,
    isCharging: Boolean,
    plugType: String,
    currentMa: Int?,
    temperatureC: Float,
    voltageV: Float,
    strings: AppStrings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Battery Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .padding(8.dp)
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Slate700,
                    strokeWidth = 10.dp
                )
                // Battery Level Progress
                val animatedProgress by animateFloatAsState(
                    targetValue = (batteryPct / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    label = "battery_progress"
                )
                val ringColor = when {
                    batteryPct >= targetPct -> Emerald400
                    batteryPct <= 20 -> Red500
                    else -> Cyan400
                }
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = ringColor,
                    strokeWidth = 10.dp
                )
                // Center Level Text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$batteryPct",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate400
                            ),
                            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                        )
                    }
                    if (isCharging) {
                        val localizedPlugType = when (plugType) {
                            "AC Fast" -> strings.acFast
                            "USB" -> strings.usb
                            "Wireless" -> strings.wireless
                            else -> strings.charging
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = localizedPlugType,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Emerald400,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    } else {
                        Text(
                            text = strings.unplugged,
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate400)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Badges row: Current (mA), Temp, Voltage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricPill(
                    label = strings.current,
                    value = if (currentMa != null && currentMa > 0) "${currentMa} mA" else strings.active,
                    tint = if (isCharging) Emerald400 else Slate400
                )
                MetricPill(
                    label = strings.voltage,
                    value = if (voltageV > 0.5f) String.format(Locale.US, "%.2f V", voltageV) else "--",
                    tint = Cyan400
                )
                MetricPill(
                    label = strings.temp,
                    value = if (temperatureC > 0) String.format(Locale.US, "%.1f°C", temperatureC) else "--",
                    tint = Amber400
                )
            }
        }
    }
}

@Composable
fun MetricPill(label: String, value: String, tint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Slate900.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate400
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = tint
            )
        )
    }
}

@Composable
fun TimerMetricsCard(
    isCharging: Boolean,
    batteryPct: Int,
    targetPct: Int,
    activeSession: ActiveSessionState,
    elapsedMs: Long,
    strings: AppStrings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = strings.progressAndTimerTitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Two big metrics side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Metric 1: Elapsed Charging Time
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Slate900.copy(alpha = 0.6f))
                        .border(1.dp, Slate700, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Cyan400,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = strings.elapsedTime,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate400,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (activeSession.isActive) formatDuration(elapsedMs) else "00:00:00",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (activeSession.isActive) {
                            val gained = (batteryPct - activeSession.startLevel).coerceAtLeast(0)
                            "${strings.startedAt}: ${activeSession.startLevel}% (+$gained%)"
                        } else {
                            strings.plugInToStart
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = if (activeSession.isActive) Emerald400 else Slate400
                        ),
                        maxLines = 1
                    )
                }

                // Metric 2: Estimated Time Remaining
                val (remainingText, remainingSubtitle) = calculateRemainingEstimate(
                    isCharging = isCharging,
                    currentPct = batteryPct,
                    targetPct = targetPct,
                    activeSession = activeSession,
                    elapsedMs = elapsedMs,
                    strings = strings
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Slate900.copy(alpha = 0.6f))
                        .border(1.dp, Slate700, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = Amber400,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = strings.estRemaining,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate400,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (batteryPct >= targetPct) Emerald400 else Amber400
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = remainingSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Slate400
                        ),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Target Progress Bar
            val targetProgress = if (targetPct > activeSession.startLevel && activeSession.isActive) {
                ((batteryPct - activeSession.startLevel).toFloat() / (targetPct - activeSession.startLevel).toFloat())
                    .coerceIn(0f, 1f)
            } else if (batteryPct >= targetPct) {
                1f
            } else {
                (batteryPct.toFloat() / targetPct.toFloat()).coerceIn(0f, 1f)
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${strings.progressTowardsTarget} ($targetPct%)",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                    )
                    Text(
                        text = "${(targetProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (batteryPct >= targetPct) Emerald400 else Cyan400
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { targetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (batteryPct >= targetPct) Emerald400 else Emerald500,
                    trackColor = Slate700
                )
            }
        }
    }
}

@Composable
fun TargetChargeCard(
    targetPct: Int,
    strings: AppStrings,
    onTargetPctChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = strings.customTargetTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                    )
                    Text(
                        text = strings.customTargetSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Emerald500.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "$targetPct%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald400
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Slider with [-] and [+] controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = { onTargetPctChanged((targetPct - 1).coerceAtLeast(50)) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Slate700,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease target", modifier = Modifier.size(18.dp))
                }

                Slider(
                    value = targetPct.toFloat(),
                    onValueChange = { onTargetPctChanged(it.toInt()) },
                    valueRange = 50f..100f,
                    steps = 49,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Emerald400,
                        activeTrackColor = Emerald500,
                        inactiveTrackColor = Slate700
                    )
                )

                FilledIconButton(
                    onClick = { onTargetPctChanged((targetPct + 1).coerceAtMost(100)) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Slate700,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase target", modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick preset chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(80, 85, 90, 95, 100).forEach { preset ->
                    val isSelected = targetPct == preset
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTargetPctChanged(preset) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Emerald500 else Slate700,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Emerald400 else Slate600
                        )
                    ) {
                        Text(
                            text = "$preset%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Slate200,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TestControlsCard(
    activeSession: ActiveSessionState,
    strings: AppStrings,
    onFinishSession: () -> Unit,
    onResetSession: () -> Unit,
    onSimulateLevelStep: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Science,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = strings.sessionTestingTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                )
            }
            Text(
                text = strings.sessionTestingSubtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = Slate400),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onFinishSession,
                    enabled = activeSession.isActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald600,
                        disabledContainerColor = Slate700
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.finishAndSave, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onResetSession,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Slate200
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.resetTimer, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Simulate level jump button
            OutlinedButton(
                onClick = { onSimulateLevelStep(5) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Cyan400
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan500.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.simulateJump, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
fun RecentSessionsCard(
    recentSessions: List<ChargingSession>,
    strings: AppStrings,
    onClearHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.recentSessionsTitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400
                        )
                    )
                    if (recentSessions.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = Slate700,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = "${recentSessions.size}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate200,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (recentSessions.isNotEmpty()) {
                    Text(
                        text = strings.clear,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Slate400,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onClearHistory() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (recentSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate900.copy(alpha = 0.4f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.PowerOff,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = strings.noCompletedSessions,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Slate400
                            )
                        )
                        Text(
                            text = strings.noSessionsSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate600,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
                    recentSessions.take(5).forEach { session ->
                        SessionItemView(session = session, dateFormat = dateFormat)
                    }
                }
            }
        }
    }
}

@Composable
fun SessionItemView(
    session: ChargingSession,
    dateFormat: SimpleDateFormat
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate900.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Start -> End time
                val startStr = dateFormat.format(Date(session.startTimeMs))
                val endStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.endTimeMs))
                Text(
                    text = "$startStr → $endStr",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate400,
                        fontSize = 11.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Start % -> End %
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${session.startLevel}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Emerald400,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(16.dp)
                    )
                    Text(
                        text = "${session.endLevel}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (session.endLevel >= session.targetPct) Emerald400 else Slate200
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Emerald500.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "+${session.levelGained}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Duration
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDuration(session.durationMs),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Cyan400
                    )
                )
                val durationMins = (session.durationMs / 60000.0)
                if (durationMins > 0 && session.levelGained > 0) {
                    val rate = session.levelGained / durationMins
                    Text(
                        text = String.format(Locale.US, "%.1f%%/min", rate),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    )
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun calculateRemainingEstimate(
    isCharging: Boolean,
    currentPct: Int,
    targetPct: Int,
    activeSession: ActiveSessionState,
    elapsedMs: Long,
    strings: AppStrings
): Pair<String, String> {
    if (currentPct >= targetPct) {
        return Pair(strings.targetReached, strings.zeroMinsRemaining)
    }

    if (!isCharging) {
        return Pair("--", strings.notCharging)
    }

    val remainingPct = (targetPct - currentPct).coerceAtLeast(0)
    val levelGained = if (activeSession.isActive) {
        (currentPct - activeSession.startLevel).coerceAtLeast(0)
    } else {
        0
    }

    // If we have observed at least 1% gained and at least 30 seconds of charging:
    if (levelGained > 0 && elapsedMs >= 30_000L) {
        val speedPctPerMs = levelGained.toDouble() / elapsedMs.toDouble()
        val remainingMs = (remainingPct / speedPctPerMs).toLong()
        val remainingMins = (remainingMs / 60000L).coerceAtLeast(1)
        val speedPerHour = (speedPctPerMs * 3600000.0).toInt()
        return Pair(
            "~$remainingMins ${strings.minsSuffix}",
            "${strings.observedRatePrefix}$speedPerHour${strings.observedRateSuffix}"
        )
    }

    // Initial placeholder / estimate when just started:
    val fallbackMins = (remainingPct * 1.8).toInt().coerceAtLeast(1)
    return Pair(
        "~$fallbackMins ${strings.minsSuffix}",
        strings.initialEstimateAwaiting
    )
}
