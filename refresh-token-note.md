# Ghi chú: refresh token bằng cookie HttpOnly

> Task riêng, chưa làm. Chốt hướng ngày 2026-10-04 sau khi đối chiếu security với dự án M6 (`D:\smcs\M6_THERMAL_POWER_PLANT_API`).
> Làm cùng đợt với [chống brute force đăng nhập](login-brute-force-note.md), vì `/refresh` cũng là endpoint công khai.

## Hiện trạng

- Chỉ có access token JWT (HS256, TTL 900s, `JwtService`), kiểm tra bằng OAuth2 Resource Server.
- Hết 15 phút là người dùng phải đăng nhập lại.
- Role nằm trong token: provider vừa được duyệt (V3 cấp role `PROVIDER`) phải tự đăng nhập lại mới có quyền.
- Admin khóa user thì token đang có vẫn dùng được tới khi hết hạn.
- `POST /api/auth/logout` chỉ trả 204, frontend tự xóa `sporthub_session` khỏi storage.

## Hướng đã chốt

Access token giữ nguyên (Bearer, lưu ở frontend như hiện tại). Refresh token là **chuỗi ngẫu nhiên** trong cookie:

```
Set-Cookie: refresh_token=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=<ttl>
```

- JavaScript không đọc được → XSS không lấy được token sống lâu.
- `Path=/api/auth` thay vì `/api/auth/refresh`: `/logout` cũng cần cookie để thu hồi token trong DB. Các endpoint dưới `/api/auth` chỉ là login/register/refresh/logout nên phạm vi vẫn hẹp.

### Backend

1. **Migration V5** — bảng `sporthub.refresh_tokens`:
   - `token_hash char(64)` unique (SHA-256 hex của token, không lưu token gốc).
   - `user_id` FK tới `user_accounts`, **nhiều token/user** (mỗi thiết bị một phiên; M6 dùng `@OneToOne` nên đăng nhập máy B đá văng máy A — không làm vậy).
   - `expires_at`, `created_at`; index theo `user_id`.
2. **Sinh token**: 32 byte `SecureRandom`, Base64URL. Không dùng JWT cho refresh token — DB đã là nguồn sự thật, và M6 bị lỗi refresh JWT được filter chấp nhận như access token vì ký cùng key.
3. **`POST /api/auth/login`**: như cũ, thêm `Set-Cookie` refresh token.
4. **`POST /api/auth/refresh`** (permitAll):
   - Rotation nguyên tử: `DELETE FROM refresh_tokens WHERE token_hash = ? RETURNING user_id, expires_at`. Hai request cùng token thì chỉ một bên lấy được dòng, bên kia 401 — không cần `@Version`.
   - Hết hạn hoặc user không `ACTIVE` → 401 và xóa cookie.
   - Lấy lại role từ DB, cấp access token mới + refresh token mới (cookie mới).
5. **`POST /api/auth/logout`**: xóa dòng của token trong cookie, trả `Set-Cookie` với `Max-Age=0`.
6. **Thu hồi toàn bộ token của user** khi admin khóa tài khoản và khi có tính năng đổi mật khẩu.
7. **Dọn rác**: xóa dòng hết hạn (job `@Scheduled` hằng ngày, hoặc xóa luôn khi refresh). Không gấp.
8. Cấu hình mới: `SPORTHUB_JWT_REFRESH_TOKEN_TTL_SECONDS` (đề xuất 14 ngày), cờ `Secure` tắt được cho môi trường test HTTP nếu cần.

### CORS và dev

- **Dev**: thêm `server.proxy` cho `/api` trong `vesanre-frontend/vite.config.js` → frontend gọi cùng origin (`VITE_API_URL` để rỗng), cookie `SameSite=Strict` hoạt động, không cần CORS credentials.
- **Prod**: nếu frontend và API khác origin thì bật `setAllowCredentials(true)` trong `CorsConfig`, **giữ origin cố định** như hiện tại (không dùng wildcard như M6). `SameSite=Strict` đòi hỏi hai bên cùng site (cùng eTLD+1); khác site thì phải đổi sang `SameSite=None` + chống CSRF — quyết định khi chốt hạ tầng deploy.

### Frontend

- `fetch` gọi `/api/auth/*` với `credentials: 'include'` (hoặc `same-origin` khi đi qua proxy).
- `api.js`: gặp 401 thì gọi `/api/auth/refresh` **một lần** (gom các request đồng thời vào cùng một promise), thành công thì gửi lại request gốc, thất bại thì `clearSession()` như hiện tại.
- Khi tải trang mà không có access token hợp lệ, có thể gọi refresh để khôi phục phiên.

## Kiểm tra khi làm

- Login trả cookie đủ cờ `HttpOnly`, `Secure`, `SameSite=Strict`, `Path=/api/auth`.
- Refresh hợp lệ → token cũ hết dùng được (dùng lại → 401).
- Hai refresh đồng thời cùng token → đúng một thành công (test có đồng bộ, không dùng sleep).
- User bị khóa → refresh 401; provider vừa được duyệt → access token mới có `PROVIDER`.
- Logout → refresh bằng cookie cũ → 401.
