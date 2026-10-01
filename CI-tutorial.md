# Hướng dẫn CI (GitHub Actions) cho team

Tài liệu này dành cho mọi người trong team, kể cả người chưa từng dùng CI. Đọc từ trên xuống dưới, làm theo từng bước.

---

## Mục lục

1. [CI là gì? Tại sao cần?](#1-ci-là-gì-tại-sao-cần)
2. [CI của dự án này làm những gì](#2-ci-của-dự-án-này-làm-những-gì)
3. [Database PostgreSQL trong CI hoạt động thế nào](#3-database-postgresql-trong-ci-hoạt-động-thế-nào)
4. [Xem kết quả CI trên GitHub](#4-xem-kết-quả-ci-trên-github)
5. [Chạy lại CI thủ công](#5-chạy-lại-ci-thủ-công)
6. [Chạy test trên máy mình trước khi push](#6-chạy-test-trên-máy-mình-trước-khi-push)
7. [Code test nằm ở đâu](#7-code-test-nằm-ở-đâu)
8. [Viết test mới cho chức năng của mình](#8-viết-test-mới-cho-chức-năng-của-mình)
9. [Những lỗi hay làm CI bị đỏ và cách sửa](#9-những-lỗi-hay-làm-ci-bị-đỏ-và-cách-sửa)
10. [Checklist trước khi push](#10-checklist-trước-khi-push)
11. [Câu hỏi thường gặp](#11-câu-hỏi-thường-gặp)

---

## 1. CI là gì? Tại sao cần?

**CI (Continuous Integration)** là một "người kiểm tra tự động". Mỗi lần ai đó đẩy code lên GitHub, GitHub sẽ:

1. Mượn một máy tính Linux sạch (không có gì trên đó).
2. Tải code mới nhất của bạn về máy đó.
3. Cài Java, dựng database, rồi chạy **toàn bộ test**.
4. Báo kết quả: ✅ **xanh** (tất cả test qua) hoặc ❌ **đỏ** (có test hỏng hoặc code không biên dịch được).

**Tại sao cần?**

- Bạn sửa chức năng A nhưng vô tình làm hỏng chức năng B của bạn khác → CI phát hiện ngay.
- Code "chạy được trên máy tôi" chưa chắc chạy được trên máy khác (thiếu biến môi trường, thiếu file `.env`...) → CI chạy trên máy sạch nên sẽ lộ ra.
- Trước khi merge Pull Request vào `master`, cả team nhìn vào dấu ✅/❌ để biết code có an toàn không.

> **Quy tắc của team:** CI đỏ thì **không merge**. Sửa cho xanh rồi mới merge.

---

## 2. CI của dự án này làm những gì

File cấu hình CI: **`.github/workflows/ci.yml`** (nằm ở thư mục gốc của repo `vesanre-backend`).

Nội dung file (có giải thích từng phần):

```yaml
name: CI                      # Tên workflow hiển thị trên tab Actions

on:                           # KHI NÀO CI chạy
  push:                       # → mỗi lần push lên BẤT KỲ nhánh nào
  pull_request:               # → mỗi lần mở / cập nhật Pull Request

jobs:
  test:                       # Chỉ có 1 job tên là "test"
    runs-on: ubuntu-latest    # Chạy trên máy Linux Ubuntu của GitHub
    services:
      postgres:               # Dựng thêm 1 database PostgreSQL chạy kèm
        image: postgres:18
        env:
          POSTGRES_DB: sporthub
          POSTGRES_USER: sporthub
          POSTGRES_PASSWORD: sporthub
        ports:
          - 5432:5432
        options: >-           # Đợi database sẵn sàng rồi mới chạy test
          --health-cmd "pg_isready -U sporthub"
          --health-interval 5s
          --health-timeout 5s
          --health-retries 10
    env:                      # Biến môi trường mà ứng dụng Spring cần
      SPORTHUB_DB_URL: jdbc:postgresql://localhost:5432/sporthub
      SPORTHUB_DB_USERNAME: sporthub
      SPORTHUB_DB_PASSWORD: sporthub
      SPORTHUB_JWT_SECRET: ci-only-secret-at-least-thirty-two-bytes
    steps:
      - uses: actions/checkout@v4          # Bước 1: tải code về
      - uses: actions/setup-java@v4        # Bước 2: cài Java 17
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v4   # Bước 3: cài Gradle (có cache)
      - run: bash ./gradlew test           # Bước 4: CHẠY TOÀN BỘ TEST
```

**Tóm lại, CI hiện tại CHỈ làm 1 việc: chạy `./gradlew test`.**

CI **không** làm:

- Không kiểm tra format / lint code.
- Không đo coverage.
- Không build file `.jar` để deploy.
- Không deploy lên server.

---

## 3. Database PostgreSQL trong CI hoạt động thế nào

Đây là phần nhiều người hiểu nhầm, đọc kỹ:

| Câu hỏi | Trả lời |
|---|---|
| Database trong CI có phải là một server chạy liên tục không? | **Không.** Nó là một container Docker **tạo mới mỗi lần CI chạy** và **bị xóa khi CI chạy xong**. |
| Dữ liệu test lần trước còn ở lần sau không? | **Không.** Mỗi lần chạy, database trống trơn. Flyway tự tạo bảng lại từ đầu bằng file `src/main/resources/db/migration/V1__...sql`. |
| Nó có liên quan tới database trên máy tôi / database thật không? | **Không.** Hoàn toàn tách biệt. |
| Cấu hình database (tên, user, mật khẩu) có bị thay đổi khi người khác push không? | **Không.** Cấu hình nằm trong file `ci.yml` đã commit. Chỉ đổi khi có người **sửa file `ci.yml`**. |
| Mật khẩu `sporthub` và `SPORTHUB_JWT_SECRET` trong `ci.yml` có phải bí mật không? | **Không.** Chúng chỉ dùng cho CI. **Tuyệt đối không** dùng các giá trị này cho server thật. |
| Dữ liệu seed (tài khoản `customer@test.sporthub.local`...) có trong CI không? | **Không.** Dữ liệu seed chỉ được nạp khi bật profile `testdata`. CI không bật profile này. |

---

## 4. Xem kết quả CI trên GitHub

### Cách 1: Xem trên trình duyệt (dễ nhất)

1. Mở: <https://github.com/jaaaa8/vesanre.com/actions>
2. Bên trái chọn workflow **CI**.
3. Danh sách các lần chạy hiện ra. Mỗi dòng có:
   - ✅ dấu tích xanh = qua hết
   - ❌ dấu X đỏ = có lỗi
   - 🟡 vòng tròn vàng = đang chạy
4. Bấm vào một lần chạy → bấm vào job **test** → bấm mở bước **Run bash ./gradlew test** để xem log chi tiết.

### Cách 2: Xem ngay trong Pull Request

Mở Pull Request của bạn, kéo xuống cuối trang sẽ thấy mục **Checks**. Bấm **Details** cạnh dòng CI để xem log.

### Cách đọc log khi CI đỏ

Trong log bước `Run bash ./gradlew test`, tìm các dòng như:

```
AuthServiceTest > rejectsPublicAdminRegistration() FAILED
    org.opentest4j.AssertionFailedError: expected: 400 but was: 500
        at ...AuthServiceTest.java:68
```

- Dòng 1: **test nào** hỏng (`AuthServiceTest`, method `rejectsPublicAdminRegistration`).
- Dòng 2: **lý do** (mong đợi 400 nhưng nhận 500).
- Dòng 3: **file và số dòng** trong test.

Nếu thấy `Compilation failed` / `error: cannot find symbol` → code **không biên dịch được**, chưa tới bước chạy test.

Nếu thấy `ApplicationContext failure threshold exceeded` hoặc `Could not resolve placeholder 'SPORTHUB_...'` → ứng dụng Spring **không khởi động được** (thường do thiếu biến môi trường hoặc migration SQL sai). Xem [mục 9](#9-những-lỗi-hay-làm-ci-bị-đỏ-và-cách-sửa).

---

## 5. Chạy lại CI thủ công

> ⚠️ Workflow hiện **chưa có** trigger `workflow_dispatch`, nên trên GitHub **không có** nút "Run workflow". Dùng một trong các cách dưới.

### Cách A: Bấm "Re-run" trên web (không cần sửa code)

Dùng khi CI lỗi do mạng / GitHub chập chờn, muốn chạy lại y nguyên.

1. Vào <https://github.com/jaaaa8/vesanre.com/actions>
2. Bấm vào lần chạy muốn chạy lại.
3. Góc trên bên phải bấm **Re-run jobs** → **Re-run all jobs**.

### Cách B: Dùng GitHub CLI (`gh`)

Cài `gh` một lần: <https://cli.github.com/> → sau khi cài, đăng nhập:

```powershell
gh auth login
```

Sau đó (chạy trong thư mục `vesanre-backend`):

```powershell
# Xem 5 lần chạy CI gần nhất (cột đầu là trạng thái, số trong [] là RUN_ID)
gh run list --workflow=ci.yml --limit 5

# Chạy lại một lần chạy (thay 123456789 bằng RUN_ID thật)
gh run rerun 123456789

# Chỉ chạy lại các job bị lỗi
gh run rerun 123456789 --failed

# Theo dõi trực tiếp đến khi xong
gh run watch 123456789

# Xem log của các bước bị lỗi
gh run view 123456789 --log-failed
```

### Cách C: Push một commit rỗng

Vì CI chạy mỗi lần push, chỉ cần push một commit không đổi gì:

```powershell
git commit --allow-empty -m "ci: trigger"
git push
```

### Cách D (tùy chọn, cần cả team đồng ý): Thêm nút "Run workflow"

Sửa phần `on:` trong `.github/workflows/ci.yml` thành:

```yaml
on:
  push:
  pull_request:
  workflow_dispatch:
```

Sau khi thay đổi này được merge vào **`master`**, tab Actions → CI sẽ có nút **Run workflow** (chọn nhánh rồi bấm chạy). Hoặc dùng lệnh:

```powershell
gh workflow run ci.yml --ref ten-nhanh-cua-ban
```

---

## 6. Chạy test trên máy mình trước khi push

**Nên làm việc này mỗi lần trước khi push**: sửa lỗi trên máy nhanh hơn nhiều so với đợi CI.

### Bước 1: Cần có sẵn

- **Java 17** (kiểm tra: `java -version`).
- **PostgreSQL** đang chạy trên máy. Nếu chưa cài, xem file `postgre-tutor.md` trong repo, hoặc dùng Docker (bước 2, cách B).

### Bước 2: Chuẩn bị database

**Cách A: Dùng PostgreSQL đã cài trên máy** + file `.env`

1. Tạo một database trống, ví dụ `sporthub` (hoặc tốt hơn là `sporthub_test` riêng cho test, vì integration test sẽ ghi user ngẫu nhiên vào database).
2. Copy file `.env.example` thành `.env` (cùng thư mục `vesanre-backend`).
3. Mở `.env`, điền:

   ```
   SPORTHUB_DB_URL=jdbc:postgresql://localhost:5432/sporthub
   SPORTHUB_DB_USERNAME=postgres
   SPORTHUB_DB_PASSWORD=mat-khau-postgres-cua-ban
   SPORTHUB_JWT_SECRET=mot-chuoi-bat-ky-dai-it-nhat-32-ky-tu-abcdef
   ```

4. File `.env` **không được commit** (đã có trong `.gitignore`). Đừng bao giờ `git add -f .env`.

**Cách B: Dựng PostgreSQL bằng Docker giống hệt CI** (không cần `.env`)

```powershell
# Chỉ cần chạy 1 lần để tạo container
docker run -d --name sporthub-pg -e POSTGRES_DB=sporthub -e POSTGRES_USER=sporthub -e POSTGRES_PASSWORD=sporthub -p 5432:5432 postgres:18

# Những lần sau chỉ cần bật lại
docker start sporthub-pg
```

Rồi đặt biến môi trường trong **cửa sổ PowerShell đang dùng** (đóng cửa sổ là mất, mở lại phải đặt lại):

```powershell
$env:SPORTHUB_DB_URL="jdbc:postgresql://localhost:5432/sporthub"
$env:SPORTHUB_DB_USERNAME="sporthub"
$env:SPORTHUB_DB_PASSWORD="sporthub"
$env:SPORTHUB_JWT_SECRET="ci-only-secret-at-least-thirty-two-bytes"
```

> Nếu máy đã có PostgreSQL khác chiếm cổng 5432, đổi `-p 5433:5432` và URL thành `localhost:5433`.

### Bước 3: Chạy test

Mở PowerShell tại thư mục `vesanre-backend`:

```powershell
# Chạy toàn bộ test (giống CI)
.\gradlew.bat test

# Chỉ chạy 1 class test
.\gradlew.bat test --tests "*AuthServiceTest"

# Chỉ chạy 1 method test
.\gradlew.bat test --tests "*AuthServiceTest.rejectsPublicAdminRegistration"
```

(Trên Git Bash / macOS / Linux thì dùng `./gradlew test`.)

### Bước 4: Đọc kết quả

- Cuối cùng thấy `BUILD SUCCESSFUL` → ✅ qua hết.
- Thấy `BUILD FAILED` → ❌ có lỗi. Mở báo cáo chi tiết bằng trình duyệt:

  ```
  vesanre-backend\build\reports\tests\test\index.html
  ```

  Trang này liệt kê test nào hỏng, lý do và stack trace.

> Trong IntelliJ: chuột phải thư mục `src/test/java` → **Run 'All Tests'**. Nhưng nhớ IntelliJ cũng cần biến môi trường / file `.env` như trên.

---

## 7. Code test nằm ở đâu

```
vesanre-backend/
├── src/
│   ├── main/java/com/vesanrebackend/     ← code chính
│   └── test/java/com/vesanrebackend/     ← CODE TEST Ở ĐÂY
│       ├── VesanreBackendApplicationTests.java
│       ├── controller/auth/
│       │   ├── AuthFlowIntegrationTest.java
│       │   └── ProfileControllerSecurityTest.java
│       ├── security/
│       │   └── JwtServiceTest.java
│       └── service/auth/
│           └── AuthServiceTest.java
```

| File | Loại test | Cần DB? | Kiểm tra gì |
|---|---|---|---|
| `VesanreBackendApplicationTests` | `@SpringBootTest` | Có | Ứng dụng khởi động được |
| `AuthFlowIntegrationTest` | `@SpringBootTest` (chạy server thật) | Có | Đăng ký → đăng nhập → xem profile → kiểm tra quyền, gọi HTTP thật |
| `ProfileControllerSecurityTest` | `@WebMvcTest` | Không | Bắt buộc Bearer token; customer/provider/admin chỉ đọc được profile đúng quyền; CORS cho Vite |
| `JwtServiceTest` | Unit test | Không | Token tạo ra đọc lại được, đúng subject, roles, thời hạn |
| `AuthServiceTest` | Unit test (Mockito) | Không | Đăng ký provider, chặn tự đăng ký admin, login sai mật khẩu / tài khoản bị khóa |

**Quy tắc cấu trúc thư mục:** đường dẫn trong `test` **giống** đường dẫn trong `main`.

- Code: `src/main/java/com/vesanrebackend/service/booking/BookingService.java`
- Test: `src/test/java/com/vesanrebackend/service/booking/BookingServiceTest.java`

---

## 8. Viết test mới cho chức năng của mình

### Tin tốt: KHÔNG cần sửa `ci.yml`

Lệnh `./gradlew test` **tự động tìm và chạy mọi method có `@Test`** trong `src/test/java`. Bạn chỉ cần tạo file test đúng chỗ, push lên, CI tự chạy.

### 5 điều kiện bắt buộc để test được chạy

1. **File nằm trong `src/test/java/com/vesanrebackend/...`** (package `com.vesanrebackend` hoặc package con).
   Với `@SpringBootTest` / `@WebMvcTest`, đây là điều kiện bắt buộc: Spring cần tìm thấy class `VesanreBackendApplication` ở package cha.
2. **Import đúng `@Test` của JUnit 5:**

   ```java
   import org.junit.jupiter.api.Test;   // ✅ ĐÚNG
   import org.junit.Test;               // ❌ SAI (JUnit 4, dự án không có → lỗi biên dịch)
   ```

3. **Method test không `private`, không `static`**, kiểu trả về `void`.
4. **Class không `abstract`.**
5. **Không gắn `@Disabled`** (gắn vào thì test bị bỏ qua).

Tên class nên kết thúc bằng `Test` (ví dụ `BookingServiceTest`) cho dễ tìm, theo quy ước hiện có.

### Chọn loại test nào?

```
Bạn muốn test gì?
│
├── Logic trong 1 Service (tính giá, kiểm tra điều kiện, ném lỗi...)
│     → Unit test + Mockito      (nhanh, KHÔNG cần DB)   ← ƯU TIÊN LOẠI NÀY
│
├── Controller: đúng URL? đúng status? đúng quyền (role)?
│     → @WebMvcTest              (nhanh, KHÔNG cần DB)
│
└── Cả luồng thật từ HTTP → Service → Database
      → @SpringBootTest          (chậm, CẦN DB)          ← chỉ dùng cho luồng quan trọng
```

### Mẫu 1: Unit test cho Service (copy và sửa)

Xem mẫu thật: `src/test/java/com/vesanrebackend/service/auth/AuthServiceTest.java`

```java
package com.vesanrebackend.service.booking;   // ← đổi theo package của bạn

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    // 1. Tạo "đồ giả" (mock) cho các repository mà service cần
    private final BookingRepository bookings = mock(BookingRepository.class);

    // 2. Tạo service thật, truyền mock vào
    private final BookingService service = new BookingService(bookings);

    @Test
    void rejectsBookingWhenCourtAlreadyTaken() {
        // Chuẩn bị (Arrange): dạy mock trả về gì
        when(bookings.existsOverlap(any(), any(), any())).thenReturn(true);

        // Thực hiện + Kiểm tra (Act + Assert)
        assertThatThrownBy(() -> service.create(/* ... */))
                .hasMessageContaining("already booked");

        // Kiểm tra: không được lưu gì vào DB
        verify(bookings, never()).save(any());
    }
}
```

### Mẫu 2: Test Controller + phân quyền (`@WebMvcTest`)

Xem mẫu thật: `src/test/java/com/vesanrebackend/controller/auth/ProfileControllerSecurityTest.java`

```java
package com.vesanrebackend.controller.booking;

import com.vesanrebackend.security.CorsConfig;
import com.vesanrebackend.security.SecurityConfig;
import com.vesanrebackend.service.booking.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;  // ← Spring Boot 4, KHÔNG phải package cũ
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BookingController.class, properties = {
        "app.security.jwt.secret=test-secret-at-least-thirty-two-bytes-long",
        "app.cors.allowed-origin=http://localhost:5173"})
@Import({SecurityConfig.class, CorsConfig.class})   // ← bắt buộc để test được phân quyền
class BookingControllerSecurityTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean                    // ← service giả, controller gọi vào đây
    private BookingService bookingService;

    @Test
    void bookingsRequireBearerToken() throws Exception {
        mvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
    }
}
```

> ⚠️ Dự án dùng **Spring Boot 4**. Nhiều hướng dẫn trên mạng (Spring Boot 2/3) import `WebMvcTest` từ `org.springframework.boot.test.autoconfigure.web.servlet` và dùng `@MockBean`. Ở dự án này phải dùng **đúng import như mẫu trên** và `@MockitoBean`.

### Mẫu 3: Integration test cần DB (`@SpringBootTest`)

Xem mẫu thật: `src/test/java/com/vesanrebackend/controller/auth/AuthFlowIntegrationTest.java`

Quy tắc **bắt buộc** khi viết loại này:

- **Dữ liệu phải ngẫu nhiên / không trùng.** Mọi test trong một lần chạy dùng **chung một database**. Nếu 2 test cùng tạo user `a@example.com` → test sau bị lỗi trùng (409).
  Làm như mẫu thật:

  ```java
  String suffix = UUID.randomUUID().toString().substring(0, 8);
  String email = "it-" + suffix + "@example.com";
  ```

- **Tự tạo dữ liệu mình cần**, đừng dựa vào tài khoản seed `customer@test.sporthub.local`: CI **không** nạp dữ liệu seed.
- **Không phụ thuộc thứ tự chạy test.** JUnit không đảm bảo test A chạy trước test B.

---

## 9. Những lỗi hay làm CI bị đỏ và cách sửa

### Lỗi 1: "Chạy trên máy tôi được mà CI lại đỏ"

**Nguyên nhân thường gặp nhất:** bạn thêm biến môi trường mới vào `application.yaml` **không có giá trị mặc định**, ví dụ:

```yaml
app:
  payment:
    api-key: ${SPORTHUB_PAYMENT_API_KEY}     # ← không có mặc định
```

Máy bạn có giá trị này trong `.env`, nhưng `.env` không được commit, còn CI chỉ có 4 biến trong `ci.yml`. Kết quả: mọi `@SpringBootTest` đều lỗi `Could not resolve placeholder 'SPORTHUB_PAYMENT_API_KEY'`.

**Cách sửa (chọn 1):**

- Thêm giá trị mặc định: `${SPORTHUB_PAYMENT_API_KEY:dummy-for-dev}` (chỉ dùng khi giá trị giả an toàn).
- Hoặc thêm biến vào khối `env:` trong `.github/workflows/ci.yml`:

  ```yaml
      env:
        SPORTHUB_DB_URL: ...
        ...
        SPORTHUB_PAYMENT_API_KEY: ci-dummy-key
  ```

- **Đồng thời** thêm biến mới vào `.env.example` để người khác biết.
- **Không bao giờ** đưa key/mật khẩu **thật** vào `ci.yml` hay `.env.example`. Nếu CI thật sự cần bí mật, dùng GitHub Secrets (hỏi trưởng nhóm).

### Lỗi 2: Migration SQL (Flyway) sai

Flyway chạy khi Spring khởi động → SQL sai thì **mọi** `@SpringBootTest` đều đỏ cùng lúc. Log sẽ có `FlywayException` / `Migration V2__... failed`.

**Cách tránh:**

- Chạy `.\gradlew.bat test` trên máy trước khi push.
- File migration mới đặt trong `src/main/resources/db/migration/`, đặt tên tăng dần: `V2__mo_ta.sql`, `V3__mo_ta.sql`...
- **Không sửa file `V1__...sql` đã merge.** Muốn đổi schema thì tạo file `V` mới. (CI không báo lỗi vì DB luôn mới, nhưng database trên máy mọi người sẽ bị lỗi checksum.)
- Hai người cùng tạo `V2__...` → trùng version → lỗi. Báo trong nhóm trước khi tạo migration mới.

### Lỗi 3: Lỗi biên dịch

Chỉ cần **1 file** (kể cả file test) không biên dịch được → **toàn bộ** CI đỏ. Hay gặp:

- Import `org.junit.Test` (JUnit 4) → đổi sang `org.junit.jupiter.api.Test`.
- Đổi tên / đổi tham số constructor của class trong `main` nhưng quên sửa test đang dùng nó → sửa test theo.

### Lỗi 4: Test trùng dữ liệu

Log có `409`, `duplicate key value violates unique constraint`... → dùng dữ liệu ngẫu nhiên (xem [Mẫu 3](#mẫu-3-integration-test-cần-db-springboottest)).

### Lỗi 5: Test dựa vào dữ liệu seed

Test tìm `customer@test.sporthub.local` rồi báo không thấy → CI không nạp seed. Cho test tự tạo dữ liệu, hoặc gắn `@ActiveProfiles("testdata")` lên class test (chỉ khi thật sự cần).

### Lỗi 6: Nhánh không chạy CI

Bạn push mà không thấy CI chạy → kiểm tra nhánh của bạn có file `.github/workflows/ci.yml` không. Nhánh tạo từ `master` mới nhất thì luôn có. Nếu nhánh quá cũ: `git merge origin/master` (hoặc hỏi trưởng nhóm).

---

## 10. Checklist trước khi push

Copy danh sách này, tick từng dòng:

- [ ] Đã `git pull` / merge `master` mới nhất vào nhánh mình.
- [ ] Đã chạy `.\gradlew.bat test` trên máy và thấy `BUILD SUCCESSFUL`.
- [ ] Chức năng mới có ít nhất 1 test (ưu tiên unit test cho Service).
- [ ] Test dùng `org.junit.jupiter.api.Test`, nằm trong package `com.vesanrebackend...`.
- [ ] Integration test dùng dữ liệu ngẫu nhiên, không dựa vào seed.
- [ ] Nếu thêm biến `${SPORTHUB_...}` mới: đã có mặc định **hoặc** đã thêm vào `ci.yml`, **và** đã thêm vào `.env.example`.
- [ ] Nếu thêm migration: tên `V<số tiếp theo>__...sql`, không sửa file `V` cũ.
- [ ] **Không** commit file `.env`.
- [ ] Sau khi push: mở tab Actions / Pull Request, đợi CI ✅ rồi mới nhờ review / merge.

---

## 11. Câu hỏi thường gặp

**H: CI chạy mất bao lâu?**
Vài phút. Lần đầu lâu hơn vì phải tải Gradle và thư viện; các lần sau có cache nên nhanh hơn.

**H: CI đỏ nhưng không phải do code của tôi thì sao?**
Mở log xem test nào hỏng. Nếu lỗi ở phần của người khác, báo trong nhóm. Nếu nghi do mạng / GitHub lỗi (log có `Connection reset`, `timeout` khi tải thư viện), bấm **Re-run** ([mục 5](#5-chạy-lại-ci-thủ-công)).

**H: Tôi có được tắt / xóa test để CI xanh không?**
**Không.** Test hỏng nghĩa là có gì đó sai. Nếu test sai vì yêu cầu đã đổi, sửa test và ghi rõ lý do trong Pull Request.

**H: Chạy test trên máy có làm bẩn database dev của tôi không?**
Có. `AuthFlowIntegrationTest` ghi user ngẫu nhiên vào database trong `.env`. Nên tạo database riêng cho test (ví dụ `sporthub_test`) hoặc dùng Docker như [mục 6, cách B](#bước-2-chuẩn-bị-database).

**H: Muốn CI làm thêm việc (lint, coverage, build jar...) thì sao?**
Sửa `.github/workflows/ci.yml` và thảo luận với team trước, vì thay đổi này ảnh hưởng tới mọi người.

**H: Tôi không có quyền bấm Re-run?**
Cần quyền ghi (write) trên repo. Nhờ người có quyền, hoặc dùng [cách C](#cách-c-push-một-commit-rỗng) (push commit rỗng lên nhánh của mình).
