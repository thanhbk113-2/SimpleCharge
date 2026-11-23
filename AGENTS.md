# AGENTS.md — SimpleCharge

## Mục đích
Ứng dụng Android tối giản giúp sạc pin thông minh: đặt mức pin mục tiêu, theo dõi thời gian sạc, nhận ước tính thời gian hoàn thành, lưu lịch sử phiên sạc. Hỗ trợ tiếng Anh và tiếng Việt.

## Tính năng chính
1. **Live Battery Status:** vòng tròn pin có animation, đổi màu (Cyan sạc, Emerald đạt mục tiêu, Red ≤ 20%). Hiển thị mA, V, °C, loại cắm.
2. **Charging Progress & Timer:** đếm thời gian (HH:mm:ss), thanh tiến trình % đầu → % mục tiêu, lưu trạng thái qua khởi động lại app.
3. **Estimated Time Remaining:** ước tính ban đầu ~1%/phút; sau ≥ 30s và ≥ 1% tăng chuyển sang tốc độ thực (%/giờ).
4. **Custom Target Charge %:** slider 50–100%, nút ±1%, chip preset 80/85/90/95/100%, lưu cục bộ.
5. **Language Switcher:** EN/VI trên top bar, lưu qua SharedPreferences, toàn bộ UI localized bằng Kotlin (không dùng string resources Android ngoài `app_name`).
6. **Charging Session History & Trash:** lưu tối đa 30 phiên, phân loại [Đã lưu | Thùng rác]. Cho phép xóa phiên vào thùng rác, phục hồi từng phiên hoặc phục hồi tất cả, xóa vĩnh viễn từng phiên hoặc xóa sạch thùng rác với hộp thoại xác nhận. Chạm phiên để xem chi tiết (biểu đồ Canvas, thống kê, Smart Insight).
7. **Session Testing Controls:** Finish & Save, Reset Timer, Simulate +5% (demo/debug).
8. **Background monitoring & Auto Launch:** BroadcastReceiver (`ACTION_POWER_CONNECTED/DISCONNECTED`, `ACTION_BOOT_COMPLETED`) + Foreground Service theo dõi cắm/rút sạc cả khi app tắt. Tùy chọn tự động bật app khi cắm sạc (Toggle trong app). Notification tiến trình và thông báo đạt mục tiêu.

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
| MainActivity | `app/.../MainActivity.kt` | Entry point. Giữ toàn bộ UI state (`mutableStateOf`), đăng ký `BroadcastReceiver` trong foreground, ticker cập nhật elapsed time mỗi giây, gọi `SessionManager`, render `SimpleChargeApp`. |
| ChargingSession | `app/.../data/ChargingSession.kt` | Data model: `ChargingDataPoint`, `ChargingSession` (thời gian, %, dataPoints, tốc độ, nhiệt độ min/max), `ActiveSessionState`. Có JSON serialize/deserialize. |
| SessionManager | `app/.../data/SessionManager.kt` | Quản lý SharedPreferences (`simple_charge_prefs`): target %, ngôn ngữ, auto launch on connect, active session, data points (tối đa 120 điểm, lọc 60s), lịch sử lưu (tối đa 30 phiên), thùng rác (`deleted_sessions`), start/resume/reset/finish/clear session, move to trash/restore/delete permanently. |
| BatteryReceiver | `app/.../receiver/BatteryReceiver.kt` | Lắng nghe `ACTION_POWER_CONNECTED` / `ACTION_POWER_DISCONNECTED` và `ACTION_BOOT_COMPLETED` từ hệ thống (kể cả app tắt). Khởi động session, notification, foreground service, tự động mở app nếu bật tuỳ chọn, broadcast session changed. |
| ChargingForegroundService | `app/.../service/ChargingForegroundService.kt` | Foreground Service (`specialUse`) chạy nền theo dõi pin khi app không mở. Xử lý cắm/rút sạc, kiểm tra đạt mục tiêu, cập nhật notification chronometer. |
| NotificationHelper | `app/.../util/NotificationHelper.kt` | Tạo notification channel, build thông báo sạc/đạt mục tiêu/rút sạc, parse loại cắm (USB/AC/Wireless), kiểm tra quyền `POST_NOTIFICATIONS`. |
| SimpleChargeApp | `app/.../ui/SimpleChargeApp.kt` | Root Composable + tất cả card UI (BatteryStatusCard, TimerMetricsCard, TargetChargeCard, TestControlsCard, RecentSessionsCard). Chứa logic `calculateRemainingEstimate`, `formatDuration`, `SessionItemView`, `ChargingSessionDetailSheet`. |
| ChargingCurveChart | `app/.../ui/ChargingCurveChart.kt` | Vẽ biểu đồ đường cong sạc bằng `Canvas` (gradient fill, cubicTo, tương tác chạm kéo), `ChargingSessionDetailSheet` Modal Bottom Sheet, `buildSmartInsight`. |
| Localization | `app/.../ui/Localization.kt` | Định nghĩa `AppStrings` và bản dịch EN/VI (pure Kotlin). |
| Color.kt | `app/.../ui/theme/Color.kt` | Palette màu: Slate, Emerald, Amber, Cyan, Red. |
| Theme.kt | `app/.../ui/theme/Theme.kt` | `SimpleChargeTheme` MaterialTheme dark color scheme. |
| AndroidManifest | `app/src/main/AndroidManifest.xml` | Khai báo permissions (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`), activity `MainActivity`, service `ChargingForegroundService`, receiver `BatteryReceiver`. |

## Luồng dữ liệu
1. **Cắm sạc:** `BatteryReceiver` (app tắt) hoặc `MainActivity` (app mở) nhận `ACTION_POWER_CONNECTED` → `SessionManager.startOrResumeSession()` → hiển thị notification + khởi động `ChargingForegroundService`.
2. **Trong khi sạc:** `ACTION_BATTERY_CHANGED` cập nhật mức pin, `recordDataPoint` (tối đa 120 điểm, skip nếu cùng mức và < 60s), `checkAndHandleBatteryLevel` → nếu đạt target tự động finish session + thông báo.
3. **Rút sạc:** Tự động `finishSession`, thông báo kết quả (thời lượng, % tăng), dừng foreground service.
4. **App mở:** `MainActivity` ticker mỗi giây cập nhật `elapsedMs`, toàn bộ state luồng vào `SimpleChargeApp` để render Compose UI.
5. **Persistence:** `targetPct`, `language`, `auto_launch_on_charging`, `activeSession`, `dataPoints`, `completedSessions`, `deletedSessions` đều lưu trong SharedPreferences qua `SessionManager`.

## Trạng thái hiện tại & Việc còn dang dở
- **Trạng thái:** Các tính năng chính (theo dõi sạc real-time, notification & foreground service, biểu đồ đường cong sạc Canvas, tự khởi động lại qua boot completed, thùng rác khôi phục lịch sử) đã hoàn tất và chạy ổn định trên Android 14/15/16 (Oppo Find X8).
- **Việc có thể mở rộng tiếp theo:**
  1. Âm thanh chuông báo (Ringtone / Sound alert) khi pin chạm mức mục tiêu hoặc khi rút sạc.
  2. Cảnh báo quá nhiệt (Nhiệt độ pin > 40°C hoặc 45°C) để bảo vệ tuổi thọ pin.
  3. Xuất dữ liệu lịch sử sạc sang file CSV/JSON để backup nếu cần.

## Thư viện dùng
- Kotlin 1.9.24, Android Gradle Plugin 8.5.0
- Jetpack Compose BOM 2024.04.01, Material3, Material Icons Extended
- `androidx.activity:activity-compose`, `androidx.lifecycle:lifecycle-viewmodel-compose`
- compileSdk 34, minSdk 26, targetSdk 34, Java 17

## Lưu ý / chỗ dễ lỗi
- **Quan trọng:** `BroadcastReceiver` trong `MainActivity` chỉ đăng ký khi `onResume` và hủy trong `onPause` — chỉ hoạt động khi app ở foreground. Theo dõi nền dựa vào `BatteryReceiver` (manifest) và `ChargingForegroundService`.
- **Android 13+:** Cần quyền `POST_NOTIFICATIONS` runtime; nếu không granted, notification sẽ không hiển thị.
- **Foreground Service:** API 34+ dùng `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`; trên thiết bị hạn chế (OPPO/ColorOS) `startForegroundService` có thể ném exception, code đã có try-catch fallback.
- **Battery data:** `EXTRA_TEMPERATURE` là đơn vị 0.1°C, `EXTRA_VOLTAGE` là mV, `EXTRA_CURRENT_NOW` có thể là µA hoặc mA tùy thiết bị — code chia heuristic (>10000 thì chia 1000).
- **SharedPreferences:** dùng `.apply()` (asynchronous) — đọc ngay sau viết có thể chưa ghéra giá trị mới trong trường hợp cạnh tranh.
- **Simulated battery:** `simulatedBonusLevel` trong `MainActivity` chỉ dùng cho tính năng "Giả lập +5%", không ảnh hưởng pin thật.
- **UI localization:** Không dùng `R.string` (ngoài `app_name`); thêm chuỗi mới phải cập nhật cả `English` và `Vietnamese` trong `Localization.kt`.
- **Build:** Nếu build lỗi, kiểm tra `compose kotlinCompilerExtensionVersion = "1.5.14"` khớp với Kotlin 1.9.24.

---

## Nguyên tắc hoạt động của Agent (Tối ưu Token & Hiệu năng)

Dành cho AI Agent làm việc trên dự án này nhằm đảm bảo tính chuẩn xác, tiết kiệm token tối đa và không phá vỡ kiến trúc:

### 1. Token-Saving Guidelines (Tiết kiệm token context)
- **Đọc code đúng mục tiêu (Targeted Reads):** KHÔNG quét (read_file / grep) toàn bộ repo hoặc các thư mục build/cache (`.gradle`, `build/`). Chỉ đọc file và dòng thực sự liên quan đến task hiện tại.
- **Batch tool calls:** Gộp song song các thao tác độc lập (đọc nhiều file, chạy lệnh kết hợp) trong cùng 1 turn thay vì gọi tuần tự từng turn.
- **Tránh spam terminal:** Dùng chuỗi lệnh PowerShell với `;` hoặc `if ($?)` để thực hiện build + install + run trong một lượt thực thi duy nhất. Không chạy lặp lại các lệnh kiểm tra không cần thiết.
- **Phản hồi cô đọng (Concise Communication):** Trả lời đúng trọng tâm, ngắn gọn, súc tích; không giải thích lan man những điều hiển nhiên trừ khi người dùng yêu cầu phân tích chi tiết.

### 2. Quy tắc kiến trúc dự án
- **Localization:** Mọi chuỗi text hiển thị trên UI bắt buộc viết vào `Localization.kt` (cả 2 ngôn ngữ `English` và `Vietnamese`). Không tạo thêm chuỗi trong `strings.xml` (ngoài `app_name`).
- **State & Persistence:** UI state tập trung tại `MainActivity.kt`. Logic lưu trữ tập trung tại `SessionManager.kt`.
- **An toàn thiết bị thực (Android 14+ / Oppo ColorOS):** Luôn bọc try-catch khi gọi `startForegroundService` / `stopService`. Đảm bảo Service có khai báo type hợp lệ (`specialUse`) trong Manifest.
- **Cập nhật tài liệu:** Sau mỗi tính năng hoàn thành hoặc sửa đổi kiến trúc, luôn đồng bộ cập nhật lại `AGENTS.md`.