# Ghi chú: chống brute force cho đăng nhập

> Task riêng, chưa làm. Tách ra từ việc gộp đăng ký provider vào tài khoản chung (2026-10-03).
> Làm cùng đợt với [refresh token bằng cookie HttpOnly](refresh-token-note.md): rate limit áp cho cả `POST /api/auth/refresh`.

## Hiện trạng

- Chỉ có một endpoint đăng nhập: `POST /api/auth/login`.
- Chưa có rate limit, lockout hay captcha → có thể thử mật khẩu không giới hạn.
- `login()` đã chống dò email: email không tồn tại vẫn so với `DUMMY_HASH`, lỗi luôn là "Invalid credentials".
- Còn lỗ dò email ở `POST /api/auth/register` (409 "Email already registered").

Gộp/tách giao diện không ảnh hưởng brute force — kẻ tấn công gọi thẳng API.

## Hướng xử lý đề xuất

- Giới hạn số lần thử trên `/login` theo IP và theo email, ví dụ 5 lần mỗi phút, vượt thì trả 429.
- Không nên khóa cứng tài khoản sau N lần sai, vì người khác có thể cố tình gõ sai để khóa tài khoản của nạn nhân.
- Muốn chặt hơn thì thêm captcha sau vài lần sai, hoặc MFA cho tài khoản admin/provider.
