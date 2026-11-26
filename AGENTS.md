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
| SessionManager | `app/.../data/SessionManager.kt` | Quản lý SharedPreferences (`simple_charge_prefs`): target %, ngôn ngữ, auto launch on connect, active session, data points (tối đa 120 điểm, lọc 60s), lịch sử lưu (tối đa 30 phiên), thùng rác (`deleted_sessions`), start/resume/reset/finish/clear session, move to trash/restore/delete permanently. Lọc bỏ phiên rác 0%->0% và chập chờn < 1.5s, tự động dọn session cũ. |
| BatteryReceiver | `app/.../receiver/BatteryReceiver.kt` | Lắng nghe `ACTION_POWER_CONNECTED` / `ACTION_POWER_DISCONNECTED` và `ACTION_BOOT_COMPLETED` từ hệ thống (kể cả app tắt). Khởi động session, notification, foreground service, tự động mở app nếu bật tuỳ chọn, broadcast session changed. Đọc % pin và sạc qua `NotificationHelper`. |
| ChargingForegroundService | `app/.../service/ChargingForegroundService.kt` | Foreground Service (`specialUse`) chạy nền theo dõi pin khi app không mở. Xử lý cắm/rút sạc, kiểm tra đạt mục tiêu, cập nhật notification chronometer. Đồng bộ qua `NotificationHelper`. |
| NotificationHelper | `app/.../util/NotificationHelper.kt` | Tạo notification channel, build thông báo sạc/đạt mục tiêu/rút sạc, parse loại cắm (USB/AC/Wireless), kiểm tra quyền `POST_NOTIFICATIONS`. Cung cấp tiện ích toàn cục `getBatteryPct()` (đọc trực tiếp phần cứng) và `isBatteryCharging()` (kiểm tra `EXTRA_PLUGGED > 0`). |
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
- **Trạng thái:**
  - Các tính năng chính (theo dõi sạc real-time, notification & foreground service, biểu đồ đường cong sạc Canvas, tự khởi động lại qua boot completed, thùng rác khôi phục lịch sử) hoạt động ổn định trên Android 14/15/16 (Oppo Find X8).
  - Đã khắc phục dứt điểm lỗi lưu hàng loạt phiên sạc ảo `0% -> 0%` và lỗi kẹt timer không reset / cắm lại đếm tiếp giây cũ khi rút sạc.
  - Cấu hình môi trường dev:
    - Đã cài đặt plugin và skill `ponytail` toàn cục (chế độ senior dev tối giản).
    - Đã cài đặt plugin và bộ skills `superpowers` (7.0.2) toàn cục + sao chép vào `~/.claude/skills` (hỗ trợ cả Claude Desktop tab Code & CLI). Tự động phối hợp quy trình: debugging, TDD, planning, review.
    - Tích hợp hook an toàn `security-guard.ps1` (chuyển lệnh xóa nguy hiểm sang prompt hỏi xác nhận `ask`, loại bỏ false-positive của `ni`).
    - Thêm các thư mục sinh tự động (`.graphify/`, `graphify-out/`, `.ponytail/`) vào `.gitignore`.
    - Duy trì knowledge graph qua `graphify` (`graphify-out/`) để tra cứu nhanh luồng dữ liệu và quan hệ giữa các component.
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
- **Tránh nhận diện nhầm trạng thái sạc khi vừa rút sạc:** Intent sticky `ACTION_BATTERY_CHANGED` có độ trễ cập nhật cờ `EXTRA_STATUS`. Bắt buộc kiểm tra `EXTRA_PLUGGED > 0` và dùng `NotificationHelper.isBatteryCharging(context)` để xác định chính xác trạng thái sạc.
- **Đọc pin an toàn trong BroadcastReceiver:** `context.registerReceiver(null, ...)` trong Receiver context có thể trả về `null`. Luôn ưu tiên dùng `BatteryManager.BATTERY_PROPERTY_CAPACITY` và fallback qua `NotificationHelper.getBatteryPct(context)`.
- **Dọn dẹp Active Session:** `SessionManager.finishSession()` luôn gọi `clearActiveSession()` vô điều kiện trước khi lưu để tránh cờ `active_exists = true` bị kẹt trong SharedPreferences làm đếm tiếp giây cũ.
- **Quan trọng:** `BroadcastReceiver` trong `MainActivity` chỉ đăng ký khi `onResume` và hủy trong `onPause` — chỉ hoạt động khi app ở foreground. Theo dõi nền dựa vào `BatteryReceiver` (manifest) và `ChargingForegroundService`.
- **Android 13+:** Cần quyền `POST_NOTIFICATIONS` runtime; nếu không granted, notification sẽ không hiển thị.
- **Foreground Service:** API 34+ dùng `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`; trên thiết bị hạn chế (OPPO/ColorOS) `startForegroundService` có thể ném exception, code đã có try-catch fallback.
- **Battery data:** `EXTRA_TEMPERATURE` là đơn vị 0.1°C, `EXTRA_VOLTAGE` là mV, `EXTRA_CURRENT_NOW` có thể là µA hoặc mA tùy thiết bị — code chia heuristic (>10000 thì chia 1000).
- **SharedPreferences:** dùng `.apply()` (asynchronous) — đọc ngay sau viết có thể chưa ghéra giá trị mới trong trường hợp cạnh tranh.
- **Simulated battery:** `simulatedBonusLevel` trong `MainActivity` chỉ dùng cho tính năng "Giả lập +5%", không ảnh hưởng pin thật.
- **UI localization:** Không dùng `R.string` (ngoài `app_name`); thêm chuỗi mới phải cập nhật cả `English` và `Vietnamese` trong `Localization.kt`.
- **Build:** Nếu build lỗi, kiểm tra `compose kotlinCompilerExtensionVersion = "1.5.14"` khớp với Kotlin 1.9.24.

---

## Nguyên tắc hoạt động của Agent — Triết lý Ponytail (Senior Dev Lười & Hiệu năng cao)

Dành cho AI Agent làm việc trên dự án này: Code tốt nhất là code không bao giờ phải viết. Giải quyết triệt để vấn đề với lượng code mới ít nhất, tiết kiệm token tối đa và bảo toàn kiến trúc gốc.

### 1. Triết lý Ponytail: Thay đổi nhỏ nhất mà đầy đủ
- **Trước khi viết:** Đọc kỹ yêu cầu và code liên quan. Xác định mọi nơi thay đổi phải chạm tới (caller, receiver, service, persistence). Đánh giá rủi ro: liệu có làm hỏng luồng sạc, kẹt session hay mất dữ liệu không. Đó mới là phạm vi — tính năng thừa thì không.
- **Thứ tự ưu tiên giải pháp:**
  1. *Nó có cần tồn tại không?* Bỏ qua tính năng thừa, tùy chọn không ai yêu cầu. Yêu cầu chung chung thì chọn phiên bản nhỏ nhất làm được việc cốt lõi.
  2. *Codebase đã có chưa?* Dùng helper (`NotificationHelper`), data manager (`SessionManager`), component sẵn có theo cùng pattern.
  3. *Nền tảng Android có sẵn?* Tận dụng Android BatteryManager, Jetpack Compose có sẵn trước khi nghĩ đến giải pháp tự chế.
  4. *Dependency sẵn có?* Dùng các thư viện đã khai báo. Không thêm thư viện ngoài chỉ vì vài dòng code.
  5. *Đơn giản & súc tích:* Gói gọn dễ hiểu; diff ngắn nhất chạy được và đúng ở các trường hợp biên (edge cases) sẽ thắng.
- **Sửa lỗi tận gốc một lần:** Trước khi sửa, tra cứu (grep) mọi nơi gọi hàm liên quan. Sửa dứt điểm nguyên nhân gốc ở phần code dùng chung (`SessionManager`, `NotificationHelper`), không vá ngọn rải rác.
- **Không abstraction thừa:** Không wrapper, chuyển đổi kiểu phức tạp, config hay code "để dành cho tương lai". Giữ nguyên các tầng (Data, Service, Receiver, UI Compose).
- **Quy ước shortcut:** Nếu chọn giải pháp tắt có giới hạn đã biết, phải có comment ngắn: `// shortcut: <giới hạn>, <khi nào nên nâng cấp>`.
- **Kết thúc mỗi câu trả lời:** Luôn dành 1–2 dòng cuối nêu rõ: những gì bạn đã bỏ qua hoặc chưa kiểm tra, và rủi ro người dùng cần lưu ý (nếu có).

### 2. Những thứ KHÔNG BAO GIỜ được cắt bỏ
- **An toàn thiết bị & bảo vệ dữ liệu:** Luôn giữ try-catch khi gọi `startForegroundService` / `stopService` (đặc biệt trên Android 14+ và thiết bị Oppo/ColorOS).
- **Session Lifecycle:** Luôn đảm bảo `clearActiveSession()` khi kết thúc sạc để không làm hỏng dữ liệu phiên sau.
- **Validation ranh giới:** Kiểm tra mức pin hợp lệ (1–100%), tránh ghi nhận phiên ảo `0% -> 0%`.
- **Mọi yêu cầu người dùng đã chỉ định rõ.**

### 3. Token-Saving Guidelines (Tiết kiệm token context)
- **Targeted Reads:** KHÔNG quét toàn bộ repo hoặc thư mục build/cache (`.gradle`, `build/`). Chỉ đọc file và dòng thực sự liên quan.
- **Batch tool calls:** Gộp song song các thao tác độc lập (đọc nhiều file, chạy lệnh kết hợp) trong cùng 1 lượt.
- **Tránh spam terminal:** Chạy build/test bằng chuỗi lệnh gộp, không gọi các lệnh kiểm tra lặp lại thừa thãi.
- **Phản hồi cô đọng:** Trả lời thẳng vào trọng tâm, súc tích, không giải thích lan man những điều hiển nhiên.

### 4. Quy tắc kiến trúc dự án
- **Localization:** Mọi chuỗi text trên UI bắt buộc viết vào `Localization.kt` (cả 2 ngôn ngữ `English` và `Vietnamese`). Không tạo chuỗi trong `strings.xml` (ngoài `app_name`).
- **State & Persistence:** UI state tập trung tại `MainActivity.kt`. Logic lưu trữ tập trung tại `SessionManager.kt`.
- **Cập nhật tài liệu:** Sau mỗi thay đổi kiến trúc hoặc tính năng, luôn đồng bộ cập nhật lại `AGENTS.md`.