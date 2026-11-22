# Prompt dùng cho AI coding

File tóm tắt dự án chính: `AGENTS.md` (thư mục gốc).
File `.clinerules/project-overview.md` chỉ trỏ tới `AGENTS.md`.

---

## A. CLINE

### A1. Đầu phiên mới

Đọc `AGENTS.md` và `.clinerules/project-overview.md`. Sau đó tóm tắt cho tôi trong 5-7 dòng: dự án làm gì, cấu trúc chính, trạng thái hiện tại. Không quét toàn bộ code. Chỉ đọc thêm file nào thật sự cần cho việc tôi yêu cầu tiếp theo.

### A2. Vào việc luôn

Dựa vào `AGENTS.md`, không đọc lại toàn bộ code. Việc cần làm: [mô tả việc]. Chỉ mở file liên quan, và nói rõ bạn sẽ đọc/sửa file nào trước khi làm.

---

## B. ANTIGRAVITY

### B1. Đầu phiên mới

Đọc `AGENTS.md` ở thư mục gốc. Sau đó tóm tắt cho tôi trong 5-7 dòng: dự án làm gì, cấu trúc chính, trạng thái hiện tại. Không quét toàn bộ code. Chỉ đọc thêm file nào thật sự cần cho việc tôi yêu cầu tiếp theo.

### B2. Vào việc luôn

Dựa vào `AGENTS.md`, không đọc lại toàn bộ code. Việc cần làm: [mô tả việc]. Chỉ mở file liên quan, và nói rõ bạn sẽ đọc/sửa file nào trước khi làm.

---

## C. DÙNG CHUNG CHO CẢ HAI

### C1. Cuối phiên

Cập nhật `AGENTS.md` với những gì đã thay đổi và việc còn dang dở.

### C2. Sau khi xong tính năng lớn

Cập nhật lại `AGENTS.md` theo thay đổi vừa rồi.

---

## Mẹo

- Muốn trỏ thẳng file cho AI, gõ ký tự at rồi chọn file từ danh sách gợi ý (cả Cline và Antigravity đều hỗ trợ). Đừng viết sẵn trong file này, vì AI sẽ tự đính kèm.
- Kiểm tra AI có đọc file không: hỏi "Dự án này dùng thư viện gì, file UI chính ở đâu?". Nếu trả lời đúng ngay mà không chạy lệnh khám phá, tức là đã nạp tốt.

---

## D. TẠO FILE TÓM TẮT CHO DỰ ÁN CHƯA CÓ (chạy 1 lần)

Dùng khi mở dự án cũ mà chưa có `AGENTS.md` hoặc `.clinerules/project-overview.md`.

### D1. Prompt dùng chung (Cline và Antigravity)

Kiểm tra xem dự án đã có `AGENTS.md` ở thư mục gốc và `.clinerules/project-overview.md` chưa.

Nếu `AGENTS.md` chưa có: đọc dự án một lần, theo thứ tự ưu tiên: file build (`build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`), `README.md`, rồi các file code chính. Bỏ qua thư mục `build`, `.gradle`, `.idea`, ảnh và file tự sinh. Sau đó tạo `AGENTS.md` ở thư mục gốc, tối đa khoảng 1.000 từ, gồm:

1. Mục đích dự án (2-3 câu)
2. Tính năng chính
3. Công nghệ và thư viện đang dùng (kèm phiên bản quan trọng)
4. Cấu trúc thư mục, mỗi file chính làm gì (kèm đường dẫn đầy đủ)
5. Luồng dữ liệu / luồng hoạt động chính
6. Quy ước code (ngôn ngữ giao diện, đặt tên, điều cấm)
7. Chỗ dễ lỗi hoặc cần lưu ý
8. Cách build và chạy
9. Trạng thái hiện tại: đã xong, đang làm dở, việc cần làm tiếp

Nếu `.clinerules/project-overview.md` chưa có: tạo file đó, chỉ ghi một dòng "Xem `AGENTS.md` ở thư mục gốc để biết tổng quan dự án."

Nếu cả hai đã có: không tạo lại, chỉ báo cho tôi biết và hỏi tôi có muốn cập nhật không.

Chỉ ghi những gì bạn thật sự thấy trong code, không đoán. Chỗ nào không chắc thì ghi "chưa rõ". Xong thì tóm tắt cho tôi 5 dòng những gì đã ghi.

### D2. Bản ngắn, chỉ cho Antigravity

Đọc dự án một lần (bỏ qua `build`, `.gradle`, `.idea`, ảnh) rồi tạo `AGENTS.md` ở thư mục gốc, dưới 1.000 từ: mục đích, tính năng, công nghệ, cấu trúc thư mục và vai trò từng file chính, luồng dữ liệu, quy ước, chỗ dễ lỗi, cách build, trạng thái hiện tại. Chỉ ghi điều có thật trong code.

### D3. Bản ngắn, chỉ cho Cline

Đọc dự án một lần (bỏ qua `build`, `.gradle`, `.idea`, ảnh) rồi tạo `AGENTS.md` ở thư mục gốc theo cấu trúc: mục đích, tính năng, công nghệ, cấu trúc thư mục và vai trò từng file chính, luồng dữ liệu, quy ước, chỗ dễ lỗi, cách build, trạng thái hiện tại. Sau đó tạo `.clinerules/project-overview.md` chỉ gồm một dòng trỏ tới `AGENTS.md`. Chỉ ghi điều có thật trong code.
