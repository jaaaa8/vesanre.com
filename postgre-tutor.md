# Hướng dẫn cài đặt PostgreSQL cho SportHub Backend

Tài liệu này dành cho thành viên mới sử dụng Windows 10/11, chưa quen PostgreSQL và pgAdmin. Làm lần lượt từ đầu đến cuối để tạo PostgreSQL local, kết nối pgAdmin, tạo database và chạy được `vesanre-backend`.

> **Phạm vi:** môi trường phát triển local trên Windows với PostgreSQL 18, pgAdmin 4, Java 17 và PowerShell.
>
> **Bảo mật:** không gửi mật khẩu PostgreSQL hoặc nội dung file `.env` lên nhóm chat, GitHub, tài liệu dùng chung hay ảnh chụp màn hình.

## 1. Hiểu đúng các thành phần

Trước khi cài đặt, cần phân biệt các khái niệm sau:

- **PostgreSQL Server:** chương trình database thực sự chạy ngầm trên máy tính.
- **pgAdmin 4:** giao diện quản trị PostgreSQL. pgAdmin không phải database server.
- **Server trong pgAdmin:** một cấu hình kết nối tới PostgreSQL Server. Thao tác **Register Server** không cài thêm PostgreSQL Server.
- **Role `postgres`:** tài khoản quản trị PostgreSQL được tạo lúc cài đặt.
- **Database:** vùng dữ liệu riêng của một ứng dụng, ví dụ `sporthub` hoặc `vesanre_test`.
- **Schema:** nhóm các bảng bên trong một database. Dự án này lưu bảng ứng dụng trong schema `sporthub`.
- **Flyway:** công cụ được backend sử dụng để tự tạo và cập nhật cấu trúc database.

Luồng thiết lập đúng là:

```text
Cài PostgreSQL Server
        ↓
PostgreSQL chạy ở localhost:5432
        ↓
Đăng ký kết nối trong pgAdmin
        ↓
Tạo database rỗng
        ↓
Cấu hình .env của backend
        ↓
Chạy backend để Flyway tự tạo schema và bảng
```

## 2. Tải bộ cài PostgreSQL

1. Mở trang tải chính thức:
   [PostgreSQL Downloads for Windows](https://www.postgresql.org/download/windows/)
2. Chọn **Download the installer** để chuyển đến bộ cài Windows do EDB cung cấp.
3. Tải PostgreSQL 18 dành cho Windows 64-bit.
4. Chạy file cài đặt bằng quyền người dùng Windows bình thường. Nếu Windows hỏi quyền quản trị, chọn **Yes**.

Bộ cài nên có các thành phần sau:

- PostgreSQL Server
- pgAdmin 4
- Command Line Tools
- Stack Builder

Giữ chọn ba thành phần đầu. Stack Builder không cần cho dự án này và có thể bỏ qua sau khi cài xong.

## 3. Cài PostgreSQL từng bước

### Bước 1: Installation Directory

Giữ đường dẫn mặc định:

```text
C:\Program Files\PostgreSQL\18
```

### Bước 2: Select Components

Giữ chọn:

- PostgreSQL Server
- pgAdmin 4
- Command Line Tools

Stack Builder là tùy chọn; backend không cần cài thêm package từ Stack Builder.

### Bước 3: Data Directory

Giữ đường dẫn mặc định, thường là:

```text
C:\Program Files\PostgreSQL\18\data
```

Không đặt thư mục dữ liệu bên trong thư mục dự án Git.

### Bước 4: Password

Đặt mật khẩu cho role quản trị `postgres`.

Yêu cầu:

- Dùng mật khẩu riêng cho máy local.
- Ghi lại trong trình quản lý mật khẩu cá nhân.
- Không gửi mật khẩu này cho thành viên khác.
- Không dùng Master Password của pgAdmin thay cho mật khẩu này.

Mỗi thành viên có thể dùng mật khẩu PostgreSQL khác nhau vì `.env` là file local và không được commit.

### Bước 5: Port

Giữ port mặc định:

```text
5432
```

Nếu máy đã có PostgreSQL khác dùng port 5432, cần chọn port khác và ghi nhớ port đó. Khi cấu hình backend, port trong JDBC URL phải trùng với port đã cài.

### Bước 6: Locale

Giữ lựa chọn mặc định của hệ thống nếu nhóm không có yêu cầu riêng.

### Bước 7: Install

Kiểm tra lại thông tin rồi chọn **Next** để cài đặt. Khi trình cài đặt hỏi có chạy Stack Builder không, có thể bỏ chọn và kết thúc.

## 4. Kiểm tra PostgreSQL Server đang chạy

### Cách 1: Kiểm tra bằng Windows Services

1. Nhấn `Windows + R`.
2. Nhập `services.msc` rồi nhấn Enter.
3. Tìm service có tên tương tự:

```text
postgresql-x64-18
```

4. Cột **Status** phải là **Running**.
5. Nếu service đang dừng, nhấp chuột phải và chọn **Start**.

### Cách 2: Kiểm tra bằng PowerShell

Mở PowerShell và chạy:

```powershell
Get-Service *postgres*
```

Kết quả cần có một service PostgreSQL với trạng thái `Running`.

Có thể kiểm tra port bằng công cụ đi kèm PostgreSQL:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\pg_isready.exe' -h localhost -p 5432
```

Kết quả thành công:

```text
localhost:5432 - accepting connections
```

Nếu đã cài PostgreSQL vào thư mục hoặc port khác, thay đường dẫn hoặc port trong lệnh cho đúng.

## 5. Mở pgAdmin lần đầu

1. Mở Start Menu.
2. Tìm và chạy **pgAdmin 4**.
3. Chờ giao diện pgAdmin mở trong cửa sổ ứng dụng hoặc trình duyệt local.
4. Nếu pgAdmin yêu cầu tạo **Master Password**, hãy đặt một mật khẩu local dễ quản lý.

Master Password chỉ bảo vệ các mật khẩu kết nối được pgAdmin lưu trên máy. Nó không phải mật khẩu của role PostgreSQL `postgres`.

| Loại mật khẩu | Mục đích |
|---|---|
| PostgreSQL password | Đăng nhập PostgreSQL bằng role `postgres`; backend dùng mật khẩu này |
| pgAdmin Master Password | Mở kho mật khẩu mà pgAdmin lưu trên máy |

## 6. Đăng ký PostgreSQL Server trong pgAdmin

Một số bản cài tự tạo sẵn server local. Nếu đã thấy server PostgreSQL và kết nối được, có thể chuyển sang mục 7.

Nếu chưa có server:

1. Trong cây bên trái, nhấp chuột phải vào **Servers**.
2. Chọn **Register → Server…**.
3. Tại tab **General**, nhập:

```text
Name: Local PostgreSQL 18
```

Tên này chỉ là nhãn hiển thị trong pgAdmin.

4. Chuyển sang tab **Connection** và nhập:

| Trường | Giá trị |
|---|---|
| Host name/address | `localhost` |
| Port | `5432` |
| Maintenance database | `postgres` |
| Username | `postgres` |
| Password | Mật khẩu role `postgres` đã đặt khi cài |
| Save password | Bật nếu đây là máy cá nhân |

5. Chọn **Save**.

Không nhập `http://localhost`, không thêm `/`, và không nhập tên database dự án vào ô Host.

Khi thành công, server xuất hiện dưới mục **Servers** và có thể mở các node `Databases`, `Login/Group Roles` và `Tablespaces`.

## 7. Kiểm tra kết nối bằng Query Tool

1. Mở cây:

```text
Servers
└── Local PostgreSQL 18
    └── Databases
        └── postgres
```

2. Nhấp chuột phải database `postgres`.
3. Chọn **Query Tool**.
4. Chạy:

```sql
SELECT version();
SELECT current_database();
SELECT current_user;
```

Kết quả đúng cần cho thấy:

- PostgreSQL đang hoạt động.
- Database hiện tại là `postgres`.
- User hiện tại là `postgres`.

## 8. Tạo database cho dự án

Nên tạo hai database riêng:

| Database | Mục đích |
|---|---|
| `sporthub` | Chạy local bình thường, không nạp tài khoản mẫu |
| `vesanre_test` | Chạy profile `testdata` và chạy integration test |

Không dùng database đang chứa dữ liệu cá nhân hoặc dữ liệu quan trọng để chạy test.

### Cách 1: Tạo bằng giao diện pgAdmin

1. Mở `Servers → Local PostgreSQL 18`.
2. Nhấp chuột phải **Databases**.
3. Chọn **Create → Database…**.
4. Tại tab **General**, nhập:

```text
Database: sporthub
Owner: postgres
```

5. Giữ Encoding là `UTF8` và các trường còn lại ở mặc định.
6. Chọn **Save**.
7. Lặp lại các bước trên để tạo database:

```text
vesanre_test
```

### Cách 2: Tạo bằng Query Tool

Chỉ dùng cách này nếu chưa tạo bằng giao diện. Mở Query Tool của database `postgres`, bật Auto-commit và chạy từng câu lệnh:

```sql
CREATE DATABASE sporthub
    WITH OWNER = postgres
    ENCODING = 'UTF8';

CREATE DATABASE vesanre_test
    WITH OWNER = postgres
    ENCODING = 'UTF8';
```

Sau khi tạo, nhấp chuột phải vào **Databases** và chọn **Refresh**.

## 9. Clone dự án và mở đúng thư mục backend

Nếu chưa clone repository:

```powershell
git clone https://github.com/jaaaa8/vesanre.com.git vesanre-backend
cd vesanre-backend
```

Nếu đã clone, mở PowerShell tại thư mục có các file `build.gradle`, `gradlew.bat` và `.env.example`.

Ví dụ:

```powershell
cd D:\PJ_MNG\vesanre-backend
```

Không chạy lệnh Gradle từ thư mục cha `D:\PJ_MNG`.

## 10. Tạo và cấu hình file `.env`

Tại thư mục gốc backend, chạy:

```powershell
Copy-Item .env.example .env
```

Mở `.env` bằng VS Code hoặc IntelliJ.

### Cấu hình chạy database `sporthub`

```properties
SPORTHUB_DB_URL=jdbc:postgresql://localhost:5432/sporthub
SPORTHUB_DB_USERNAME=postgres
SPORTHUB_DB_PASSWORD=<mat-khau-role-postgres-tren-may-cua-ban>
SPORTHUB_JWT_SECRET=<chuoi-bi-mat-ngau-nhien-toi-thieu-32-byte>
SPORTHUB_JWT_ISSUER=sporthub
SPORTHUB_JWT_ACCESS_TOKEN_TTL_SECONDS=900
SPORTHUB_CORS_ALLOWED_ORIGIN=http://localhost:5173
```

Thay toàn bộ giá trị nằm giữa `<` và `>` bằng giá trị thật. Không giữ ký tự `<` hoặc `>` trong file `.env`.

### Cấu hình chạy database `vesanre_test`

Chỉ đổi database trong URL:

```properties
SPORTHUB_DB_URL=jdbc:postgresql://localhost:5432/vesanre_test
SPORTHUB_DB_USERNAME=postgres
SPORTHUB_DB_PASSWORD=<mat-khau-role-postgres-tren-may-cua-ban>
SPORTHUB_JWT_SECRET=<chuoi-bi-mat-ngau-nhien-toi-thieu-32-byte>
SPORTHUB_JWT_ISSUER=sporthub
SPORTHUB_JWT_ACCESS_TOKEN_TTL_SECONDS=900
SPORTHUB_CORS_ALLOWED_ORIGIN=http://localhost:5173
```

### Tạo JWT secret an toàn bằng PowerShell

Chạy:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Sao chép chuỗi Base64 được in ra và đặt làm `SPORTHUB_JWT_SECRET`.

Quy tắc khi sửa `.env`:

- File phải nằm cùng cấp với `build.gradle`.
- Không thêm dấu cách quanh dấu `=`.
- Không đặt giá trị trong dấu nháy đơn hoặc nháy kép.
- Nếu mật khẩu PostgreSQL có ký tự `\`, viết thành `\\` trong `.env`.
- Mật khẩu `SPORTHUB_DB_PASSWORD` là mật khẩu role `postgres`, không phải Master Password của pgAdmin.
- Không commit `.env`. Repository đã ignore file này.
- Có thể commit `.env.example`, nhưng file mẫu không được chứa bí mật thật.

## 11. Chạy backend lần đầu

Đảm bảo Java 17 đã được cấu hình:

```powershell
java -version
.\gradlew.bat --version
```

Cả hai lệnh cần hiển thị Java 17 cho project.

### Chạy bình thường với database `sporthub`

Đặt `SPORTHUB_DB_URL` trỏ tới `sporthub`, sau đó chạy:

```powershell
.\gradlew.bat bootRun
```

### Chạy với dữ liệu thử trong `vesanre_test`

Đặt `SPORTHUB_DB_URL` trỏ tới `vesanre_test`, sau đó chạy:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=testdata"
```

Profile `testdata` chỉ dành cho local/test. Không bật profile này trên staging hoặc production.

Trong lần chạy đầu tiên, Flyway tự thực hiện:

1. Cài extension PostgreSQL `btree_gist` nếu chưa có.
2. Tạo schema `sporthub`.
3. Tạo toàn bộ bảng, khóa ngoại, index và constraint.
4. Ghi lịch sử migration vào `public.flyway_schema_history`.
5. Nếu profile `testdata` được bật, nạp tài khoản thử.

Không tự chạy file `V1__create_sporthub_schema.sql` trong pgAdmin.

## 12. Nhận biết backend đã chạy thành công

Backend chạy thành công khi cuối log có hai dòng tương tự:

```text
Tomcat started on port 8080 (http) with context path '/'
Started VesanreBackendApplication
```

Giữ terminal này mở. Nếu đóng terminal hoặc nhấn `Ctrl+C`, backend sẽ dừng.

Mở một PowerShell khác và kiểm tra port:

```powershell
Test-NetConnection localhost -Port 8080
```

Kết quả cần có:

```text
TcpTestSucceeded : True
```

Trang `http://localhost:8080/` có thể trả về `401` hoặc `404` vì backend không cung cấp trang chủ công khai. Điều quan trọng là Tomcat đang lắng nghe cổng 8080 và API hoạt động.

## 13. Kiểm tra schema và bảng trong pgAdmin

Sau khi backend đã chạy Flyway thành công:

1. Quay lại pgAdmin.
2. Mở database đang dùng, ví dụ `vesanre_test`.
3. Nhấp chuột phải **Schemas** và chọn **Refresh**.
4. Mở:

```text
Databases
└── vesanre_test
    └── Schemas
        ├── sporthub
        │   └── Tables
        └── public
            └── Tables
                └── flyway_schema_history
```

Các bảng ứng dụng nằm trong `sporthub`, không nằm trong `public`.

Kiểm tra bằng Query Tool của đúng database:

```sql
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema = 'sporthub'
ORDER BY table_name;
```

Kiểm tra lịch sử Flyway:

```sql
SELECT installed_rank, version, description, type, success
FROM public.flyway_schema_history
ORDER BY installed_rank;
```

Kiểm tra extension:

```sql
SELECT extname
FROM pg_extension
WHERE extname = 'btree_gist';
```

## 14. Tài khoản thử của profile `testdata`

Khi chạy đúng profile `testdata`, Flyway tạo ba tài khoản local:

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Customer | `customer@test.sporthub.local` | `SportHub@123` |
| Provider | `provider@test.sporthub.local` | `SportHub@123` |
| Admin | `admin@test.sporthub.local` | `SportHub@123` |

Các tài khoản này chỉ dùng cho local/test.

## 15. Chạy test với database riêng

Integration test có ghi dữ liệu vào database. Trước khi chạy test:

1. Đảm bảo `.env` đang trỏ tới `vesanre_test`.
2. Đảm bảo PostgreSQL đang chạy.
3. Chạy:

```powershell
.\gradlew.bat test
```

Không chạy test với database chứa dữ liệu cần giữ.

## 16. Dừng và chạy lại backend

Để dừng backend, quay lại terminal đang chạy `bootRun` và nhấn:

```text
Ctrl+C
```

Để chạy lại với dữ liệu thử:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=testdata"
```

Flyway không tạo lại migration đã chạy thành công. Repeatable seed test được viết để có thể chạy lại an toàn trên database test.

## 17. Khắc phục lỗi thường gặp

### Lỗi: không thấy PostgreSQL trong Windows Services

Nguyên nhân thường gặp:

- Chỉ cài pgAdmin nhưng chưa cài PostgreSQL Server.
- Quá trình cài PostgreSQL Server thất bại.

Cách xử lý:

1. Mở Apps trong Windows Settings.
2. Kiểm tra PostgreSQL 18 đã được cài.
3. Nếu chưa có, chạy lại installer và chọn **PostgreSQL Server**.

### Lỗi: `connection refused` khi kết nối pgAdmin

Kiểm tra:

1. Service PostgreSQL có trạng thái `Running`.
2. Host là `localhost`.
3. Port trong pgAdmin đúng với port đã chọn lúc cài, mặc định là `5432`.
4. Không có firewall hoặc phần mềm bảo mật chặn kết nối local.

Chạy:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\pg_isready.exe' -h localhost -p 5432
```

### Lỗi: `password authentication failed for user "postgres"`

Backend đã tìm thấy PostgreSQL nhưng mật khẩu không đúng.

Kiểm tra:

1. Dùng mật khẩu role `postgres` đã đặt trong installer.
2. Không dùng Master Password của pgAdmin.
3. Sửa `SPORTHUB_DB_PASSWORD` trong `.env`.
4. Không đặt mật khẩu trong dấu nháy.
5. Nếu mật khẩu có `\`, viết thành `\\` trong `.env`.

Kiểm tra trực tiếp bằng `psql`:

```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -p 5432 -U postgres -d vesanre_test -W
```

Lệnh sẽ hỏi mật khẩu mà không in mật khẩu ra màn hình.

Nếu pgAdmin vẫn kết nối được bằng mật khẩu đã lưu và cần đổi mật khẩu local, mở Query Tool rồi chạy:

```sql
ALTER ROLE postgres WITH PASSWORD '<mat-khau-local-moi>';
```

Sau đó cập nhật `.env` và mật khẩu đã lưu trong pgAdmin. Nếu không còn bất kỳ kết nối quản trị nào, không tự sửa `pg_hba.conf` sang chế độ `trust`; hãy nhờ trưởng nhóm hỗ trợ khôi phục tài khoản local.

### Lỗi: `database "sporthub" does not exist`

Tên database trong `SPORTHUB_DB_URL` chưa tồn tại hoặc bị gõ sai.

Ví dụ URL đúng:

```properties
SPORTHUB_DB_URL=jdbc:postgresql://localhost:5432/sporthub
```

Tạo database trong pgAdmin hoặc đổi URL sang database đã tạo.

### Lỗi: `Could not resolve placeholder 'SPORTHUB_DB_URL'`

Spring Boot không đọc được biến bắt buộc.

Kiểm tra:

- `.env` nằm cùng cấp với `build.gradle`.
- Tên file là chính xác `.env`, không phải `.env.txt`.
- Chạy Gradle từ thư mục gốc backend.
- File có đủ `SPORTHUB_DB_URL`, `SPORTHUB_DB_USERNAME` và `SPORTHUB_DB_PASSWORD`.

### Lỗi: `Could not resolve placeholder 'SPORTHUB_JWT_SECRET'`

Thêm `SPORTHUB_JWT_SECRET` vào `.env`. Secret phải có tối thiểu 32 byte. Dùng lệnh PowerShell tại mục 10 để tạo secret ngẫu nhiên.

### Lỗi: `Detected applied migration not resolved locally: seed identity test data`

Database đã từng chạy seed test nhưng lần hiện tại không bật profile `testdata`.

Chạy đúng lệnh:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=testdata"
```

Không chạy Flyway `repair` cho lỗi này.

### Lỗi: không thấy bảng trong pgAdmin

Kiểm tra đúng đường dẫn:

```text
Databases → database đang dùng → Schemas → sporthub → Tables
```

Nhấp chuột phải `Tables` và chọn **Refresh**. Các bảng ứng dụng không nằm trong `public`.

### Lỗi: `permission denied to create extension btree_gist`

Role kết nối không có đủ quyền trên database.

Với môi trường local của tài liệu này:

- Database nên có owner là `postgres`.
- `.env` nên dùng `SPORTHUB_DB_USERNAME=postgres`.
- Không tự xóa câu lệnh `CREATE EXTENSION` khỏi migration.

### Lỗi: port 5432 đã được sử dụng

Máy có thể đang chạy một PostgreSQL khác. Kiểm tra:

```powershell
Get-NetTCPConnection -LocalPort 5432 -State Listen
Get-Service *postgres*
```

Không cài thêm PostgreSQL vào cùng port. Dùng instance đang có hoặc chọn port khác, sau đó cập nhật cả pgAdmin và `SPORTHUB_DB_URL`.

Ví dụ dùng port 5433:

```properties
SPORTHUB_DB_URL=jdbc:postgresql://localhost:5433/sporthub
```

### Lỗi: port 8080 đã được sử dụng

Kiểm tra process đang giữ port:

```powershell
$connection = Get-NetTCPConnection -LocalPort 8080 -State Listen
Get-Process -Id $connection.OwningProcess
```

Dừng đúng ứng dụng đang dùng port 8080 hoặc dừng phiên backend cũ bằng `Ctrl+C`. Không kết thúc process khi chưa biết đó là ứng dụng nào.

### Lỗi frontend `ERR_CONNECTION_REFUSED`

Lỗi này nghĩa là không có backend lắng nghe tại cổng 8080.

Backend chỉ sẵn sàng sau khi log có:

```text
Tomcat started on port 8080
Started VesanreBackendApplication
```

Nếu terminal đã trả lại dấu nhắc lệnh hoặc hiển thị `BUILD FAILED`, backend không còn chạy.

### VS Code hỏi cài extension để debug `Dotenv`

Bạn đã nhấn F5 khi đang mở file `.env`. File `.env` là file cấu hình, không phải chương trình để debug.

Chọn **Cancel**, sau đó chạy backend trong terminal bằng lệnh Gradle hoặc mở `VesanreBackendApplication.java` và chọn **Run**.

## 18. Những thao tác không được tự ý thực hiện

- Không commit hoặc gửi file `.env`.
- Không chạy Flyway `repair` khi chưa được trưởng nhóm duyệt.
- Không sửa file migration đã chạy trên database dùng chung.
- Không xóa database để thử sửa lỗi nếu chưa sao lưu hoặc chưa được phép.
- Không chạy profile `testdata` trên staging hoặc production.
- Không dùng database thật để chạy integration test.
- Không sửa `pg_hba.conf` sang `trust` để bỏ qua mật khẩu.

## 19. Checklist hoàn tất

Mỗi thành viên tự kiểm tra toàn bộ danh sách:

- [ ] PostgreSQL Server 18 đã được cài.
- [ ] Service PostgreSQL đang `Running`.
- [ ] `pg_isready` báo `accepting connections`.
- [ ] pgAdmin mở được.
- [ ] Đã phân biệt Master Password và mật khẩu role `postgres`.
- [ ] Server `Local PostgreSQL 18` kết nối thành công trong pgAdmin.
- [ ] Query Tool chạy được `SELECT version()`.
- [ ] Database `sporthub` đã tồn tại.
- [ ] Database `vesanre_test` đã tồn tại.
- [ ] `.env` nằm ở thư mục gốc backend.
- [ ] `.env` trỏ đúng host, port và database.
- [ ] `.env` dùng đúng mật khẩu role `postgres`.
- [ ] JWT secret có tối thiểu 32 byte.
- [ ] `java -version` và Gradle sử dụng Java 17.
- [ ] Flyway chạy thành công.
- [ ] Schema `sporthub` và các bảng đã xuất hiện.
- [ ] `public.flyway_schema_history` đã xuất hiện.
- [ ] Backend có log `Started VesanreBackendApplication`.
- [ ] `Test-NetConnection localhost -Port 8080` trả về `True`.
- [ ] `.env` không xuất hiện trong `git status`.

## 20. Tài liệu chính thức

- [PostgreSQL Downloads for Windows](https://www.postgresql.org/download/windows/)
- [PostgreSQL: Creating a Database](https://www.postgresql.org/docs/current/tutorial-createdb.html)
- [pgAdmin: Connecting to a Server](https://www.pgadmin.org/docs/pgadmin4/latest/connecting.html)
- [pgAdmin: Connect to Server](https://www.pgadmin.org/docs/pgadmin4/latest/connect_to_server.html)
- [pgAdmin: Database Dialog](https://www.pgadmin.org/docs/pgadmin4/latest/database_dialog.html)
- [PostgreSQL: CREATE EXTENSION](https://www.postgresql.org/docs/current/sql-createextension.html)
