# Project Overview: SimpleCharge

## Mục đích
Ứng dụng Android tối giản giúp sạc pin thông minh: đặt mức pin mục tiêu, theo dõi thời gian sạc, nhận ước tính thời gian hoàn thành, lưu lịch sử phiên sạc. Hỗ trợ tiếng Anh và tiếng Việt.

## Tính năng chính
1. **Live Battery Status:** vòng tròn pin có animation, đổi màu theo trạng thái (Cyan đang sạc, Emerald đạt mục tiêu, Red ≤ 20%). Hiển thị mA, V, °C, loại cắm.
2. **Charging Progress & Timer:** đếm thời gian sạc (HH:mm:ss), thanh tiến trình % đầu → % mục tiêu, lưu trạng thái qua khởi động lại app.
3. **Estimated Time Remaining:** ước tính ban đầu ~1%/phút; sau ≥ 30s và ≥ 1% tăng chuyển sang tốc độ thực (%/giờ).
4. **Custom Target Charge %:** slider 50–100%, nút ±1%, chip preset 80/85/90/95/100%, lưu cục bộ.
5. **Language Switcher:** EN/VI trên top bar, lưu qua SharedPreferences, toàn bộ UI localized bằng Kotlin (không dùng string resources Android).
6. **Charging Session History:** lưu tối đa 30 phiên, hiển thị 5 gần nhất. Chạm vào phiên để xem chi tiết (biểu đồ Canvas, thống kê, Smart Insight).
7. **Session Testing Controls:** Finish & Save, Reset Timer, Simulate +5% (dùng cho demo/debug).
8. **Background monitoring:** BroadcastReceiver + Foreground Service theo dõi cắm/rút sạc cả khi app tắt; notification tiến trình và thông báo đạt mục tiêu.

## Cấu trúc thư mục (nguồn chính)
```
app/src/main/java/com/example/simplecharge/
├── MainActivity.kt
├── data/
│   ├── ChargingSession.kt
│   └── SessionManager.kt
├── receiver/
│   └── BatteryReceiver.kt
├── service/
│   └── ChargingForegroundService.kt
├── util/
│   └── NotificationHelper.kt
├── ui/
│   ├── SimpleChargeApp.kt
│   ├── ChargingCurveChart.kt
│   ├── Localization.kt
│   └── theme/
│       ├── Color.kt
│       └── Theme.kt
app/src/main/AndroidManifest.xml
app/src/main/res/values/strings.xml   (chỉ app_name)
app/src/main/res/values/themes.xml
```

## Mỗi file chính làm gì
| File | Đường dẫn | Chức năng |
|------|-----------|-----------|
| MainActivity | `app/.../MainActivity.kt` | Entry point. Giữ toàn bộ UI state (`mutableStateOf`), đăng ký `BroadcastReceiver` trong foreground, ticker cập nhật elapsed time mỗi giây, gọi `SessionManager`, hiển thị `SimpleChargeApp`. |
| ChargingSession | `app/.../data/ChargingSession.kt` | Data model: `ChargingDataPoint` (timestamp, level, temp), `ChargingSession` (thời gian, %, dataPoints, tốc độ, nhiệt độ min/max), `ActiveSessionState`. Có JSON serialize/deserialize. |
| SessionManager | `app/.../data/SessionManager.kt` | Quản lý SharedPreferences (`simple_charge_prefs`): target %, ngôn ngữ, active session, data points (tối đa 120 điểm, lọc 60s), lịch sử (tối đa 30 phiên), start/resume/reset/finish/clear session. |
| BatteryReceiver | `app/.../receiver/BatteryReceiver.kt` | Lắng nghe `ACTION_POWER_CONNECTED` / `ACTION_POWER_DISCONNECTED` từ hệ thống (kể cả app tắt). Khởi động session, notification, foreground service, broadcast session changed. |
| ChargingForegroundService | `app/.../service/ChargingForegroundService.kt` | Foreground Service (`specialUse`) chạy nền theo dõi pin khi app không mở. Xử lý cắm/rút sạc, kiểm tra đạt mục tiêu, cập nhật notification chronometer. |
| NotificationHelper | `app/.../util/NotificationHelper.kt` | Tạo notification channel, build thông báo sạc/đạt mục tiêu/rút sạc, parse loại cắm (USB/AC/Wireless), kiểm tra quyền `POST_NOTIFICATIONS`. |
| SimpleChargeApp | `app/.../ui/SimpleChargeApp.kt` | Root Composable + tất cả các card UI (BatteryStatusCard, TimerMetricsCard, TargetChargeCard, TestControlsCard, RecentSessionsCard). Chứa logic `calculateRemainingEstimate`, `formatDuration`, `SessionItemView`, `ChargingSessionDetailSheet`. |
| ChargingCurveChart | `app/.../ui/ChargingCurveChart.kt` | Vẽ biểu đồ đường cong sạc bằng `Canvas` (gradient fill, cubicTo, tương tác chạm kéo), `ChargingSessionDetailSheet` Modal Bottom Sheet, `buildSmartInsight`. |
| Localization | `app/.../ui/Localization.kt` | Định nghĩa `AppStrings` và bản dịch EN/VI (pure Kotlin, không dùng `strings.xml` ngoài `app_name`). |
| Color.kt | `app/.../ui/theme/Color.kt` | Palette màu: Slate, Emerald, Amber, Cyan, Red. |
| Theme.kt | `app/.../ui/theme/Theme.kt` | `SimpleChargeTheme` MaterialTheme dark color scheme. |
| AndroidManifest | `app/src/main/AndroidManifest.xml` | Khai báo permissions (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`), activity `MainActivity`, service `ChargingForegroundService`, receiver `BatteryReceiver`. |

## Luồng dữ liệu
1. **Cắm sạc:** `BatteryReceiver` (app tắt) hoặc `MainActivity` (app mở) nhận `ACTION_POWER_CONNECTED` → `SessionManager.startOrResumeSession()` → hiển thị notification + khởi động `ChargingForegroundService`.
2. **Trong khi sạc:** `ACTION_BATTERY_CHANGED` cập nhật mức pin, `recordDataPoint` (tối đa 120 điểm, skip nếu cùng mức và < 60s), `checkAndHandleBatteryLevel` → nếu đạt target tự động finish session + thông báo.
3. **Rút sạc:** Tự động `finishSession`, thông báo kết quả (thời lượng, % tăng), dừng foreground service.
4. **App mở:** `MainActivity` ticker mỗi giây cập nhật `elapsedMs`, toàn bộ state luồng vào `SimpleChargeApp` để render Compose UI.
5. **Persistence:** `targetPct`, `language`, `activeSession`, `dataPoints`, `completedSessions` đều lưu trong `SharedPreferences` qua `SessionManager`.

## Thư viện chính
- Kotlin 1.9.24, Android Gradle Plugin 8.5.0
- Jetpack Compose BOM 2024.04.01, Material3, Material Icons Extended
- `androidx.activity:activity-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`
- compileSdk 34, minSdk 26, targetSdk 34, Java 17

## Lưu ý / chỗ dễ lỗi
- **Quan trọng:** `BroadcastReceiver` trong `MainActivity` chỉ đăng ký khi `onResume` và hủy trong `onPause` — chỉ hoạt động khi app ở foreground. Theo dõi nền dựa vào `BatteryReceiver` (manifest) và `ChargingForegroundService`.
- **Android 13+:** Cần quyền `POST_NOTIFICATIONS` runtime, nếu không granted notification sẽ không hiển thị (`hasNotificationPermission` trả về false).
- **Foreground Service:** API 34+ dùng `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`; trên thiết bị bị hạn chế (OPPO/ColorOS...) `startForegroundService` có thể ném exception, code đã có try-catch fallback.
- **Battery data:** `EXTRA_TEMPERATURE` là đơn vị 0.1°C, `EXTRA_VOLTAGE` là mV, `EXTRA_CURRENT_NOW` có thể là µA hoặc mA tùy thiết bị — code đang chia theo heuristic (>10000 thì chia 1000).
- **SharedPreferences:** `SessionManager` dùng `.apply()` (asynchronous) — đọc ngay sau viết có thể chưa ghéra giá trị mới trong một số trường hợp cạnh tranh.
- **Simulated battery:** `simulatedBonusLevel` trong `MainActivity` chỉ dùng cho tính năng "Giả lập +5%", không ảnh hưởng pin thật.
- **UI localization:** Không dùng `R.string` (ngoài `app_name`), thêm chuỗi mới phải cập nhật cả `English` và `Vietnamese` trong `Localization.kt`.
- **Build:** Nếu build lỗi, kiểm tra `compose kotlinCompilerExtensionVersion = "1.5.14"` khớp với Kotlin 1.9.24.

---
**Quy tắc:** Sau mỗi tính năng lớn hoàn thành, cập nhật lại file này theo thay đổi vừa rồi.