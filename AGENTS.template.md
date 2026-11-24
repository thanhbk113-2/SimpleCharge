# AGENTS.md — <Tên dự án>

> Điền các mục <...>. Xóa mục nào không áp dụng. Giữ file ngắn: mỗi dòng đều tốn token mỗi phiên.

## Mục đích

<1–2 câu: app làm gì, cho ai, nền tảng, ngôn ngữ hỗ trợ.>

## Stack & lệnh

- Stack: <ngôn ngữ / framework + phiên bản chính>
- Build: `<lệnh>`
- Test: `<lệnh>`
- Run / Deploy: `<lệnh>`

## Cấu trúc thư mục

```
<root>/
├── <file hoặc thư mục>   # 1 dòng: làm gì
├── <file hoặc thư mục>   # 1 dòng: làm gì
└── <file hoặc thư mục>   # 1 dòng: làm gì
```

## Luồng dữ liệu

1. <Sự kiện> → <ai xử lý> → <lưu ở đâu>
2. <Bước tiếp theo>
3. <Persistence: cái gì lưu ở đâu>

## Quy ước bắt buộc

- <Quy ước 1, viết dạng lệnh: "Mọi ... phải ...", "Luôn ...", "Không ...">
- <Quy ước 2>
- Sau mỗi tính năng xong hoặc đổi kiến trúc: cập nhật lại AGENTS.md.

## Chỗ dễ lỗi

- **<Tên lỗi>:** <nguyên nhân> → <cách tránh>
- **<Tên lỗi>:** <nguyên nhân> → <cách tránh>

## Trạng thái & việc dở

- Đã xong: <...>
- Đang làm: <...>
- Có thể làm tiếp: <...>

---

## Nguyên tắc agent (dùng chung mọi dự án)

### Tiết kiệm token

- Đọc đúng mục tiêu: không quét toàn repo hay thư mục build/cache (`node_modules`, `.gradle`, `build/`, `dist/`). Chỉ đọc file và dòng liên quan task.
- Gộp các thao tác độc lập (đọc nhiều file, chạy lệnh) vào cùng 1 lượt.
- Không chạy lặp lệnh kiểm tra thừa; nối build + test + run trong 1 lệnh.
- Trả lời ngắn, đúng trọng tâm; không giải thích điều hiển nhiên.

### Phong cách làm việc (Ponytail — tóm tắt)

Làm ít code nhất nhưng giải quyết trọn vẹn vấn đề. Thứ tự ưu tiên:

1. Có cần tồn tại không? Bỏ tính năng/tùy chọn không ai yêu cầu, nêu ra trong 1 dòng.
2. Đã có trong codebase? Dùng lại theo đúng cách code xung quanh đang làm.
3. Thư viện chuẩn / tính năng nền tảng? Dùng.
4. Dependency đã cài? Dùng. Không thêm dependency cho vài dòng code.
5. Còn lại: viết lượng code tối thiểu chạy được.

Quy tắc kèm theo:

- Trước khi viết: liệt kê mọi nơi thay đổi phải chạm tới (caller, test, fixture, config, export) và những gì có thể vỡ (mất dữ liệu, lộ dữ liệu, caller hỏng).
- Sửa lỗi: grep mọi caller trước, sửa tận gốc một lần ở code dùng chung.
- Code di chuyển/gộp phải giữ nguyên error handling và validation.
- Logic mới không tầm thường (nhánh, vòng lặp, parser, tiền, bảo mật) phải kèm 1 test nhỏ hoặc assert tự kiểm.
- Lối tắt có giới hạn: ghi comment `shortcut: <giới hạn>, <khi nào nên nâng cấp>`.
- Không bao giờ cắt: validation ở ranh giới tin cậy, xử lý lỗi chống mất dữ liệu, bảo mật, accessibility, thứ người dùng đã yêu cầu.
- Cuối mỗi phản hồi: 1–2 dòng nói đã bỏ qua gì / chưa kiểm tra gì, và rủi ro người dùng cần biết.
