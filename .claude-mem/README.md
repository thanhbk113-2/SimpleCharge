# Claude Mem Setup

## 1. Gateway Credentials

Mở nhanh bằng `Win + R` -> `%USERPROFILE%\.claude-mem\.env`:

```env
ANTHROPIC_BASE_URL=http://localhost:20128
ANTHROPIC_AUTH_TOKEN=omniroute
```

## 2.Cấu hình mô hình & xác thực (Window R + %USERPROFILE%\.claude-mem\settings.json)

Mở nhanh bằng `Win + R` -> `%USERPROFILE%\.claude-mem\settings.json`:

- `CLAUDE_MEM_CLAUDE_AUTH_METHOD`: Chuyển từ `"subscription"` sang `"gateway"` (ngăn claude-mem tự động ngắt kết nối do nhầm lẫn với quota của gói cá nhân).
- `CLAUDE_MEM_MODEL`: Đặt thành `"antigravity-combo"` (khớp với model định tuyến trên OmniRoute).
- `CLAUDE_MEM_TIER_ROUTING_ENABLED`: Tắt (`false`) và gán các tier model (`fast`, `smart`, `simple`, `summary`) về `"antigravity-combo"` để tránh trường hợp worker gọi model mặc định haiku/sonnet khiến OmniRoute báo lỗi `Unable to determine provider`.

## 3. Chạy lệnh kiểm tra đã hoạt động

```env
 powershell -ExecutionPolicy Bypass -File .\.claude-mem\check-claude-mem.ps1
```

## 4. Trạng thái hoạt động

- **Worker daemon**: Đã được khởi động lại thành công (PID: `14352`).
- **Đồng bộ cấu hình**: Đã áp dụng `"antigravity-combo"` cho `CLAUDE_MEM_MODEL` và toàn bộ các tier (`fast`, `smart`, `simple`, `summary`), đồng thời tắt `CLAUDE_MEM_TIER_ROUTING_ENABLED` (`false`) để tránh lỗi `Unable to determine provider` từ OmniRoute.

> **Lưu ý:** Tiến trình chạy nền của `claude-mem` sẽ tạm ngắt nếu OmniRoute dừng, và chỉ hoạt động ổn định trở lại khi service này đang chạy.
