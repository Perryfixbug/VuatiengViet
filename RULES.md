# 🧩 RULES.md — Quy tắc làm việc với Git

## 1️⃣ Cấu trúc nhánh (branch)
- **main** → nhánh chính, chỉ merge code đã kiểm thử, ổn định.  
- **dev** → nhánh phát triển, dùng để test chung.  
- **feature/**`<tên-tính-năng>` → nhánh phát triển từng tính năng riêng.  
  - Ví dụ: `feature/login`, `feature/chat`, `feature/jdbc`

---

## 2️⃣ Quy tắc commit
- Commit ngắn gọn, nêu rõ mục đích.
- Dạng đề xuất:

| Type | Ý nghĩa |
|------|----------|
| feat | Thêm tính năng mới |
| fix | Sửa lỗi |
| refactor | Tối ưu, chỉnh sửa không đổi chức năng |
| docs | Cập nhật tài liệu, comment |
| style | Format, đặt tên, không đổi logic |
| chore | Thay đổi cấu hình, script, file hệ thống |

