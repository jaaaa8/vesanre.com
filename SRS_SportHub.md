# TÀI LIỆU ĐẶC TẢ YÊU CẦU PHẦN MỀM (SRS)
## DỰ ÁN: SPORTHUB - HỆ THỐNG ĐẶT SÂN VÀ QUẢN LÝ SÂN THỂ THAO ĐA NĂNG TẠI VIỆT NAM

---

**Thông tin đồ án & Phiên bản:**
- **Môn học:** CMU-SE 432 AIS – Capstone Project 1
- **Nhóm thực hiện:** C1SE.05 (C1SE.TKQPT)
- **Đơn vị:** Viện Đào tạo Quốc tế (International School), Đại học Duy Tân
- **Mã định danh dự án:** SportHub (`vesanre.com` - `sporthub.com`)
- **Phiên bản tài liệu:** Version 1.0 (Chuẩn IEEE 830 / IEEE 29148)
- **Ngày lập:** 28/09/2026
- **Giảng viên hướng dẫn (Mentor):** ThS. Lê Kim Hoàng

**Thành viên nhóm dự án:**
1. **Trần Phước Trí** (Leader / Scrum Master / BA / Fullstack / AI Dev / QA) - MSSV: 30219024883
2. **Nguyễn Văn Phúc Thiện** (UX/UI Designer / Frontend Dev / Backend Dev) - MSSV: 30211156792
3. **Đào Nhật Sơn** (Backend Dev / AI Dev) - MSSV: 28219048502
4. **Lê Anh Khoa** (UX/UI Designer / Frontend Dev) - MSSV: 30219056704
5. **Lê Nhật Khoa** (Backend Dev / AI Dev) - MSSV: 30211156705

---

## MỤC LỤC
1. [GIỚI THIỆU (INTRODUCTION)](#1-giới-thiệu-introduction)
   - 1.1 Mục đích tài liệu (Purpose)
   - 1.2 Phạm vi sản phẩm (Scope of the System)
   - 1.3 Thuật ngữ và từ viết tắt (Definitions and Acronyms)
   - 1.4 Tài liệu tham chiếu (References)
   - 1.5 Tổng quan cấu trúc tài liệu (Document Overview)
2. [MÔ TẢ TỔNG QUAN HỆ THỐNG (OVERALL DESCRIPTION)](#2-mô-tả-tổng-quan-hệ-thống-overall-description)
   - 2.1 Bối cảnh hệ thống & Sơ đồ ngữ cảnh (System Context Diagram)
   - 2.2 Các tác nhân và bên liên quan (Actors & Stakeholders)
   - 2.3 Đặc điểm người dùng & Ma trận phân quyền (User Classes & RBAC Matrix)
   - 2.4 Môi trường vận hành & Công nghệ nền tảng (Tech Stack & Environment)
   - 2.5 Ràng buộc thiết kế và triển khai (Design & Implementation Constraints)
   - 2.6 Giả định và phụ thuộc (Assumptions & Dependencies)
3. [YÊU CẦU CHỨC NĂNG CHI TIẾT THEO 4 SPRINT (DETAILED FUNCTIONAL REQUIREMENTS)](#3-yêu-cầu-chức-năng-chi-tiết-theo-4-sprint)
   - 3.1 Sprint 1: Nền tảng hệ thống & Định danh (System Foundation and Identity)
   - 3.2 Sprint 2: Sàn giao dịch, Quản lý sân & Lịch trống (Marketplace, Venues and Availability)
   - 3.3 Sprint 3: Đặt sân, Khóa slot, Đặt cọc & Hoàn tiền (Booking, Deposit and Refund)
   - 3.4 Sprint 4: Quản trị hệ thống, Đánh giá, Thông báo & Sẵn sàng phát hành (Administration, Quality, and Release Readiness)
4. [YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS - ISO 25010)](#4-yêu-cầu-phi-chức-năng-iso-25010)
   - 4.1 Hiệu năng (Performance Efficiency)
   - 4.2 An toàn và Bảo mật (Security & OWASP Compliance)
   - 4.3 Tính tin cậy và toàn vẹn dữ liệu (Reliability & Data Integrity)
   - 4.4 Khả năng mở rộng (Scalability)
   - 4.5 Tính khả dụng (Usability & UX)
   - 4.6 Khả năng bảo trì và vận hành (Maintainability & DevOps)
5. [YÊU CẦU GIAO DIỆN HỆ THỐNG (SYSTEM INTERFACES)](#5-yêu-cầu-giao-diện-hệ-thống-system-interfaces)
   - 5.1 Giao diện người dùng (User Interfaces)
   - 5.2 Giao diện phần mềm & API (Software Interfaces)
   - 5.3 Giao diện truyền thông & Dịch vụ bên ngoài (Communication & Third-party Services)
6. [MA TRẬN TRUY XUẤT NGUỒN GỐC YÊU CẦU (TRACEABILITY MATRIX)](#6-ma-trận-truy-xuất-nguồn-gốc-yêu-cầu-traceability-matrix)

---

## 1. GIỚI THIỆU (INTRODUCTION)

### 1.1 Mục đích tài liệu (Purpose)
Tài liệu Đặc tả Yêu cầu Phần mềm (Software Requirements Specification - SRS) này định nghĩa toàn diện các yêu cầu nghiệp vụ, yêu cầu chức năng, yêu cầu phi chức năng, quy tắc kiểm soát đồng thời (concurrency control), cơ chế bảo mật và giao diện tích hợp cho hệ thống **SportHub (Vesanre)** phiên bản MVP. 

Tài liệu được biên soạn nhằm:
- Làm cơ sở pháp lý và kỹ thuật thống nhất giữa Nhóm phát triển (Nhóm C1SE.05), Giảng viên hướng dẫn (Mentor) và Hội đồng đánh giá Capstone Project 1 (CMU-SE 432).
- Cung cấp kim chỉ nam cho các giai đoạn Thiết kế kiến trúc (HLD/LLD), Hiện thực hóa mã nguồn (Development Sprints), Thiết lập kịch bản kiểm thử (QA/Testing), và Triển khai hệ thống (DevOps Deployment).

### 1.2 Phạm vi sản phẩm (Scope of the System)

#### 1.2.1 Tên sản phẩm và khái niệm cốt lõi
- **Tên hệ thống:** SportHub (Nền tảng đặt sân và quản trị sân thể thao đa năng - Multi-Sport Court Booking Marketplace).
- **Mục tiêu chính:** Số hóa toàn diện quy trình tìm kiếm, kiểm tra lịch trống theo thời gian thực (real-time availability), đặt cọc giữ chỗ trực tuyến và tự động hóa công tác vận hành, xếp lịch sân bãi cho các chủ sân tại Việt Nam.

#### 1.2.2 Phạm vi chức năng bao gồm trong MVP (In-Scope)
Hệ thống MVP được phát triển gói gọn trong 4 Sprint phát triển (116 ngày làm việc từ 18/08/2026 đến 12/12/2026), bao gồm:
1. **Xác thực và phân quyền (Identity & RBAC):** Đăng ký, đăng nhập tài khoản chuẩn mật khẩu và Google OAuth2, phân tách 3 vai trò độc lập: Khách hàng (Customer), Chủ sân (Venue Provider), và Quản trị viên (Admin).
2. **Sàn thể thao & Tìm kiếm đa năng (Marketplace & Discovery):** Tìm kiếm sân theo địa phương, phân loại môn thể thao (Bóng đá, Pickleball, Cầu lông, Bóng chuyền, Bóng rổ, Tennis), lọc theo khoảng giá, tiện ích đi kèm; hiển thị chi tiết hình ảnh, địa chỉ và biểu đồ lịch sân.
3. **Mô hình tổ chức sân phân cấp (Shop -> Venue -> Court Hierarchy):** Cho phép chủ sân quản lý nhiều chi nhánh cơ sở, nhiều sân con theo từng bộ môn, cấu hình khung giờ hoạt động và biểu giá linh hoạt (giờ cao điểm vs giờ bình thường).
4. **Lõi đặt sân & Chống đặt trùng (Real-time Slot Locking & Concurrency Control):** Đặt sân tức thì, cơ chế khóa tạm giữ chỗ (Reservation Hold), kiểm soát tranh chấp đồng thời bằng ràng buộc loại trừ thời gian (PostgreSQL Exclusion Constraints) ngăn chặn tuyệt đối tình trạng double-booking.
5. **Thanh toán cọc & Quy trình hoàn tiền (Deposit Payment & Automated Refund):** Tích hợp cổng thanh toán trực tuyến qua cơ chế Webhook bảo đảm tính lũy thừa (Idempotency), hóa đơn xác nhận điện tử và chính sách hoàn hủy cọc có kiểm soát.
6. **Bảng điều khiển quản trị & Kiểm duyệt (Admin Verification & Management):** Duyệt hoặc từ chối hồ sơ đăng ký sân của chủ cơ sở trước khi niêm yết công khai; quản lý danh sách người dùng và nhật ký giao dịch.
7. **Đánh giá & Thông báo tự động:** Hệ thống đánh giá/chấm điểm sau lượt chơi hoàn tất, tự động gửi hóa đơn và thông báo nhắc lịch qua Email và Web Alerts.

#### 1.2.3 Phạm vi không bao gồm trong MVP (Out-of-Scope)
Để đảm bảo tiến độ và chất lượng cho Capstone 1, các tính năng sau được dời sang các giai đoạn sau:
- Ghép trận thể thao cộng đồng (Social player & team matching).
- Gợi ý sân thông minh sử dụng học máy cá nhân hóa (AI-driven recommendation engines).
- Tự động định giá theo thuật toán cung-cầu động (Dynamic algorithmic surge pricing - chỉ áp dụng cấu hình thủ công theo khung giờ).
- Hệ thống quản lý giải đấu (Tournament management, bracket, bảng xếp hạng).
- Ứng dụng di động Native (Native iOS/Android apps - MVP tập trung hoàn thiện Web Application có thiết kế chuẩn Responsive).

### 1.3 Thuật ngữ và từ viết tắt (Definitions and Acronyms)

| Thuật ngữ / Viết tắt | Ý nghĩa chi tiết |
| :--- | :--- |
| **SRS** | Software Requirements Specification - Tài liệu đặc tả yêu cầu phần mềm |
| **MVP** | Minimum Viable Product - Sản phẩm khả dụng tối thiểu |
| **RBAC** | Role-Based Access Control - Cơ chế kiểm soát truy cập dựa trên vai trò |
| **JWT** | JSON Web Token - Chuỗi mã hóa stateless xác thực danh tính phiên người dùng |
| **IDOR** | Insecure Direct Object References - Lỗ hổng tham chiếu đối tượng trực tiếp |
| **Exclusion Constraint** | Ràng buộc loại trừ trong PostgreSQL (`EXCLUDE USING GIST`), dùng để ngăn các khoảng thời gian bị giao nhau |
| **tstzrange** | Kiểu dữ liệu khoảng thời gian có kèm múi giờ trong PostgreSQL |
| **Slot** | Một khung giờ cố định hoặc linh hoạt của một sân thể thao cụ thể (VD: 17:30 - 18:30) |
| **Double Booking** | Lỗi nghiêm trọng khi hai khách hàng khác nhau cùng đặt thành công một sân tại cùng một khung giờ |
| **Idempotency** | Tính lũy thừa: một thao tác gọi nhiều lần (ví dụ webhook thanh toán) vẫn cho ra kết quả duy nhất, không nhân đôi tiền |
| **Outbox Pattern** | Mẫu thiết kế phần mềm đảm bảo tính nhất quán giữa việc lưu Database và gửi sự kiện/thông báo ra ngoài |
| **Hold / Lock** | Trạng thái giữ chỗ tạm thời (thường 5 - 10 phút) để khách hàng thực hiện thanh toán cọc trước khi slot bị giải phóng |

### 1.4 Tài liệu tham chiếu (References)
1. Đề xuất dự án Capstone: *C1SE.05 - Project Proposal: SportHub - Multi-Sport Court Booking Marketplace* (International School, Duy Tan University, 2026).
2. Tiêu chuẩn quốc tế *IEEE Std 830-1998* - Recommended Practice for Software Requirements Specifications.
3. Tiêu chuẩn *ISO/IEC/IEEE 29148:2018* - Systems and software engineering — Life cycle processes — Requirements engineering.
4. Tiêu chuẩn chất lượng phần mềm *ISO/IEC 25010:2011* - Systems and software Quality Requirements and Evaluation (SQuaRE).
5. OWASP Application Security Verification Standard (ASVS 4.0) & OWASP API Security Top 10.
6. PostgreSQL 16 Reference Documentation: *Range Types and GiST Exclusion Constraints*.

### 1.5 Tổng quan cấu trúc tài liệu (Document Overview)
- **Chương 2** cung cấp cái nhìn tổng thể về mô hình ngữ cảnh, các tác nhân, phân quyền, môi trường công nghệ và các giả định cốt lõi.
- **Chương 3** chi tiết hóa toàn bộ các Yêu cầu Chức năng (Functional Requirements - FR) được bóc tách tỉ mỉ theo 4 Sprint của chu kỳ Agile/Scrum.
- **Chương 4** xác lập các tiêu chí định lượng cho Yêu cầu Phi Chức năng (NFR) theo chuẩn ISO 25010.
- **Chương 5** đặc tả các giao diện phần mềm, phần cứng, cơ sở dữ liệu và các API tích hợp dịch vụ bên thứ ba.
- **Chương 6** thể hiện ma trận truy xuất nguồn gốc (Traceability Matrix) liên kết mục tiêu dự án, FR, NFR và ca kiểm thử.

---

## 2. MÔ TẢ TỔNG QUAN HỆ THỐNG (OVERALL DESCRIPTION)

### 2.1 Bối cảnh hệ thống & Sơ đồ ngữ cảnh (System Context Diagram)
SportHub đóng vai trò là một sàn thương mại điện tử kết nối 2 phía cung - cầu trong ngành dịch vụ sân bãi thể thao, đồng thời tích hợp chặt chẽ với các nền tảng hạ tầng bên thứ ba.

```mermaid
flowchart TD
    subgraph External_Users [Người dùng tương tác]
        C[Khách hàng / Vận động viên - Customer]
        P[Chủ sân thể thao - Venue Provider]
        A[Quản trị viên hệ thống - Admin]
    end

    subgraph SportHub_System [Hệ Thống SportHub]
        Core[Cổng Dịch Vụ Web & API SportHub<br/>(Next.js + Spring Boot)]
        DB[(Cơ Sở Dữ Liệu PostgreSQL<br/>+ Bộ đệm Redis)]
        Core <--> DB
    end

    subgraph External_Services [Hệ thống Dịch vụ Bên thứ Ba]
        OAuth[Google OAuth2 Service<br/>(Xác thực tài khoản)]
        Pay[Cổng Thanh Toán Trực Tuyến<br/>(VNPay / MoMo / ZaloPay)]
        Storage[Cloud Storage<br/>(Lưu trữ hình ảnh/Media sân)]
        Maps[Google Maps / OpenStreetMap<br/>(Tọa độ & Bản đồ số)]
        Noti[Email SMTP Service<br/>(Gửi thông báo & Vé điện tử)]
    end

    C -->|Tìm sân, Xem lịch, Đặt chỗ, Trả cọc, Đánh giá| Core
    P -->|Đăng ký sân, Cấu hình giá/giờ, Quản lý lịch| Core
    A -->|Kiểm duyệt cơ sở, Khóa tài khoản, Báo cáo| Core

    Core <-->|Xác thực Google ID Token| OAuth
    Core <-->|Tạo giao dịch cọc & Nhận Webhook xác nhận| Pay
    Core -->|Tải lên & Lấy URL ảnh sân bãi| Storage
    Core <-->|Geocoding & Hiển thị vị trí cơ sở| Maps
    Core -->|Gửi Email hóa đơn, Cảnh báo nhắc lịch| Noti
```

### 2.2 Các tác nhân và bên liên quan (Actors & Stakeholders)

| Tác nhân (Actor) | Bản chất | Mô tả vai trò và trách nhiệm trong hệ thống |
| :--- | :--- | :--- |
| **Khách vãng lai (Guest)** | Người dùng chưa đăng nhập | Tìm kiếm, xem danh sách sân bãi, duyệt lịch trống, xem giá và đánh giá mà không cần tài khoản. Bắt buộc đăng nhập khi thực hiện hành động đặt cọc. |
| **Khách hàng (Customer)** | Vận động viên / Người chơi thể thao | Đăng nhập tài khoản cá nhân, chọn sân, chọn khung giờ, thanh toán cọc giữ chỗ trực tuyến, quản lý lịch đặt cá nhân, nhận mã vé QR, viết nhận xét/chấm điểm sau trận. |
| **Chủ sân (Venue Provider)** | Chủ cơ sở / Quản lý cụm sân | Đăng ký thông tin thương hiệu (Shop), các cụm sân (Venues) và sân thành phần (Courts); cấu hình giờ mở cửa, giá ngày thường/cao điểm; tạo lịch thủ công cho khách vãng lai trực tiếp; theo dõi doanh thu cọc. |
| **Quản trị viên (Admin)** | Đội ngũ quản trị SportHub | Kiểm duyệt tính pháp lý và độ chính xác của hồ sơ sân trước khi hiển thị ra sàn; khóa tài khoản vi phạm; giám sát doanh thu toàn hệ thống; tra cứu đối soát giao dịch thanh toán lỗi. |
| **Hệ thống xác thực Google** | Dịch vụ bên ngoài | Cung cấp luồng định danh Google Identity Services (OAuth2/OIDC) giúp đăng nhập an toàn, tiện lợi. |
| **Cổng thanh toán điện tử** | Dịch vụ bên ngoài | Xử lý thanh toán tiền cọc tức thì, gửi thông báo Webhook bảo đảm tính lũy thừa và hỗ trợ lệnh hoàn tiền khi hủy lịch hợp lệ. |

### 2.3 Đặc điểm người dùng & Ma trận phân quyền (User Classes & RBAC Matrix)

| Chức năng chính | Guest | Customer | Venue Provider | Admin |
| :--- | :---: | :---: | :---: | :---: |
| Đăng ký / Đăng nhập (Mật khẩu & Google OAuth) | Có | Có | Có | Đăng nhập nội bộ |
| Tìm kiếm, lọc sân, xem chi tiết & biểu đồ lịch trống | Có | Có | Có | Có |
| Tạo phiên đặt cọc & Khóa slot thời gian thực | Không | **Có** | Không | Không |
| Nhận vé xác nhận điện tử & Xem lịch sử đặt của mình | Không | **Có** | Không | Không |
| Đánh giá & Bình luận sân đã từng chơi | Không | **Có (Đã hoàn tất)** | Không | Giám sát/Xóa vi phạm |
| Đăng ký mở cơ sở sân & Cập nhật hồ sơ nhà cung cấp | Không | Có (Đăng ký thành Provider) | **Có** | Kiểm duyệt hồ sơ |
| Quản lý cụm sân, sân con, tiện ích, ảnh & bảng giá | Không | Không | **Có (Sân sở hữu)** | Quản lý toàn bộ |
| Khóa slot thủ công cho khách đặt trực tiếp tại quầy | Không | Không | **Có (Sân sở hữu)** | Không |
| Xem bảng doanh thu cọc và danh sách khách đặt tại sân | Không | Không | **Có (Sân sở hữu)** | Xem toàn hệ thống |
| Phê duyệt / Từ chối hồ sơ sân niêm yết lên sàn | Không | Không | Không | **Có** |
| Khóa / Mở khóa người dùng vi phạm quy tắc | Không | Không | Không | **Có** |

### 2.4 Môi trường vận hành & Công nghệ nền tảng (Tech Stack & Environment)
Căn cứ theo cam kết kỹ thuật trong Đề cương Proposal C1SE.05:
- **Tầng Giao diện (Frontend Application):** 
  - Framework: Next.js (App Router), React.js, TypeScript.
  - Thiết kế: TailwindCSS / Vanilla CSS hiện đại, responsive hoàn toàn trên Mobile, Tablet và Desktop.
  - State Management & Data Fetching: React Query / Context API, Axios.
- **Tầng Máy chủ (Backend Application):**
  - Ngôn ngữ & Framework: Java 17+, Spring Boot 3.x.
  - Bảo mật: Spring Security 6.x tích hợp JWT (JSON Web Tokens) không trạng thái (stateless).
  - Kiến trúc: Layered Architecture (Controller - Service - Repository - DTO - Entity).
- **Tầng Dữ liệu & Lưu trữ (Persistence & Cache):**
  - Cơ sở dữ liệu chính: PostgreSQL 16 (Hỗ trợ phần mở rộng `btree_gist`, kiểu dữ liệu dải thời gian `tstzrange` để thiết lập Exclusion Constraints chống đặt trùng tuyệt đối).
  - Bộ đệm & Khóa phân tán: Redis (Caching lịch trống, lưu khóa tạm phiên giữ slot).
  - Quản lý di chuyển schema: Liquibase hoặc Flyway.
- **Hạ tầng & Triển khai (DevOps & Hosting):**
  - Đóng gói ứng dụng: Docker & Docker Compose đa dịch vụ.
  - Quản lý mã nguồn & CI/CD: GitHub Actions, GitHub Team Workspace.
  - Máy chủ đám mây: VPS Linux / Render / Vercel. Cơ sở dữ liệu: PostgreSQL lưu trữ ổn định.
  - Dịch vụ tích hợp: Cổng thanh toán (VNPay / MoMo Sandbox), Cloudinary / AWS S3 (Media), SendGrid / SMTP Gmail (Email thông báo).

### 2.5 Ràng buộc thiết kế và triển khai (Design & Implementation Constraints)
1. **Ràng buộc chi phí Capstone:** Ngân sách thực tế tối ưu hóa duy trì dưới mức **$60/tháng** theo cam kết của Proposal (tận dụng mã nguồn mở, tier miễn phí và gói sinh viên).
2. **Ràng buộc thời gian phát triển:** Dự án hoàn tất chuẩn chỉ 4 Sprint trong vòng 116 ngày trước cột mốc bàn giao tháng 12/2026.
3. **Ràng buộc công nghệ cơ sở dữ liệu:** Bắt buộc sử dụng PostgreSQL cho mô hình dữ liệu quan hệ giao dịch (ACID) do yêu cầu bắt buộc áp dụng **Exclusion Constraints** loại trừ chồng lấn thời gian.
4. **Ràng buộc chuẩn mã nguồn:** Tuân thủ tiêu chuẩn lập trình của OWASP Cheat Sheet Series (Xác thực mật khẩu BCrypt với cost factor >= 12, CSRF/CORS phòng hộ, kiểm soát IDOR tầng Service).

### 2.6 Giả định và phụ thuộc (Assumptions & Dependencies)
1. Các chủ sân thể thao đồng ý với phương thức cọc trực tuyến qua cổng thanh toán và chấp nhận quy tắc không nhận tiền mặt ngoài hệ thống đối với các slot đã khóa trên nền tảng.
2. Khách hàng sử dụng ứng dụng có điện thoại thông minh kết nối Internet ổn định, có tài khoản ngân hàng hoặc ví điện tử quét mã VietQR / cổng thanh toán.
3. Các dịch vụ bên thứ ba (Google OAuth2 API, Cổng thanh toán Sandbox, SMTP Server) hoạt động ổn định với độ khả dụng tối thiểu 99.5%.

---

## 3. YÊU CẦU CHỨC NĂNG CHI TIẾT THEO 4 SPRINT

### 3.1 SPRINT 1: NỀN TẢNG HỆ THỐNG & ĐỊNH DANH (System Foundation and Identity)
*Thời lượng: 24 ngày (01/09/2026 – 24/09/2026)*  
*Mục tiêu chính: Thiết lập kiến trúc dữ liệu gốc, cơ chế bảo mật xác thực, phân quyền 3 vai trò và quản lý hồ sơ cá nhân.*

#### [FR-01] Xác thực tài khoản bằng Email và Mật khẩu
- **Mã Use Case:** `UC-01` (Đăng ký) & `UC-02` (Đăng nhập)
- **Tác nhân:** Khách vãng lai (Guest)
- **Mô tả chi tiết:**
  - `FR-01.1`: Hệ thống PHẢI cho phép người dùng đăng ký tài khoản mới bằng Email hợp lệ, Mật khẩu và Họ tên.
  - `FR-01.2`: Mật khẩu PHẢI có độ dài tối thiểu 8 ký tự, bao gồm ít nhất một chữ hoa, một chữ thường, một chữ số và một ký tự đặc biệt. Mật khẩu lưu trong cơ sở dữ liệu PHẢI được băm bằng thuật toán BCrypt.
  - `FR-01.3`: Hệ thống PHẢI từ chối đăng ký nếu Email đã tồn tại trong cơ sở dữ liệu với thông báo lỗi thích hợp.
  - `FR-01.4`: Khi đăng nhập bằng Email và Mật khẩu chính xác, hệ thống PHẢI trả về Access Token (JWT với hạn tồn tại ngắn, ví dụ 15-30 phút) và Refresh Token (hạn 7 ngày) được lưu trữ an toàn.
  - `FR-01.5`: Hệ thống PHẢI giới hạn tần suất đăng nhập sai (tối đa 5 lần liên tiếp trong 15 phút, sau đó tạm khóa IP/tài khoản trong 15 phút để chống Brute Force).

#### [FR-02] Đăng nhập bằng tài khoản Google (Google OAuth2)
- **Mã Use Case:** `UC-03`
- **Tác nhân:** Khách vãng lai (Guest), Google OAuth Service
- **Mô tả chi tiết:**
  - `FR-02.1`: Hệ thống PHẢI cung cấp nút "Đăng nhập bằng Google" tuân theo hướng dẫn giao diện của Google Identity Services.
  - `FR-02.2`: Backend PHẢI tiếp nhận Google ID Token từ Frontend, thực hiện thẩm định chữ ký số thông qua Google Public Keys.
  - `FR-02.3`: Nếu Email từ Google chưa từng đăng ký, hệ thống PHẢI tự động khởi tạo bản ghi người dùng mới với vai trò mặc định là `ROLE_CUSTOMER`, kích hoạt trạng thái tài khoản tự động.
  - `FR-02.4`: Hệ thống PHẢI liên kết tài khoản Google và phát hành cặp Access Token/Refresh Token chuẩn của SportHub.

#### [FR-03] Kiểm tra và thẩm định phân quyền (Validate Credentials & RBAC)
- **Mã Use Case:** `UC-04` (Quan hệ `<<include>>` với Đăng nhập và các luồng API nghiệp vụ)
- **Tác nhân:** Hệ thống (Backend Spring Security Filter)
- **Mô tả chi tiết:**
  - `FR-03.1`: Mọi yêu cầu HTTP gửi đến các endpoint được bảo vệ PHẢI đính kèm Access Token trong tiêu đề `Authorization: Bearer <token>`.
  - `FR-03.2`: Bộ lọc Spring Security Filter PHẢI giải mã token, kiểm tra hạn sử dụng, tính toàn vẹn chữ ký và trích xuất danh sách quyền (`ROLE_CUSTOMER`, `ROLE_PROVIDER`, `ROLE_ADMIN`).
  - `FR-03.3`: Hệ thống PHẢI trả về mã lỗi HTTP `401 Unauthorized` nếu token không hợp lệ/hết hạn, và HTTP `403 Forbidden` nếu người dùng không đủ quyền hạn truy cập tài nguyên.
  - `FR-03.4`: Cơ chế Refresh Token PHẢI cho phép cấp mới Access Token mà không bắt người dùng đăng nhập lại khi Refresh Token còn hiệu lực.

#### [FR-04] Quên mật khẩu và Khôi phục tài khoản
- **Mã Use Case:** `UC-05` (Quan hệ `<<extend>>` từ màn hình Đăng nhập)
- **Tác nhân:** Khách vãng lai (Người dùng quên mật khẩu)
- **Mô tả chi tiết:**
  - `FR-04.1`: Người dùng gửi yêu cầu đặt lại mật khẩu bằng cách nhập Email đã đăng ký.
  - `FR-04.2`: Hệ thống PHẢI tạo ra một mã Token đặt lại mật khẩu dùng một lần (One-Time Password/Reset Token) có thời hạn hiệu lực chính xác là 15 phút.
  - `FR-04.3`: Hệ thống PHẢI gửi liên kết đặt lại mật khẩu chứa token đến hòm thư người dùng qua Email Service.
  - `FR-04.4`: Người dùng nhấp vào link, nhập mật khẩu mới thỏa mãn chính sách bảo mật để hoàn tất khôi phục.

#### [FR-05] Quản lý thông tin hồ sơ cá nhân (Manage Profile)
- **Mã Use Case:** `UC-06`
- **Tác nhân:** Người dùng đã xác thực (Customer, Venue Provider, Admin)
- **Mô tả chi tiết:**
  - `FR-05.1`: Người dùng có quyền xem thông tin chi tiết hồ sơ: Họ tên, Số điện thoại, Email, Ảnh đại diện (Avatar).
  - `FR-05.2`: Người dùng có quyền cập nhật Họ tên, Số điện thoại, tải lên ảnh đại diện mới (định dạng JPG/PNG dung lượng tối đa 5MB).
  - `FR-05.3`: Người dùng có quyền thay đổi mật khẩu hiện tại bằng cách nhập đúng mật khẩu cũ và xác nhận mật khẩu mới.
  - `FR-05.4`: Khách hàng (Customer) có quyền gửi đơn đăng ký nâng cấp tài khoản trở thành Chủ sân (Venue Provider) kèm theo thông tin pháp lý / căn cước công dân.

#### [FR-06] Đăng xuất khỏi hệ thống (Logout)
- **Mã Use Case:** `UC-07`
- **Tác nhân:** Người dùng đã xác thực
- **Mô tả chi tiết:**
  - `FR-06.1`: Hệ thống PHẢI cho phép người dùng đăng xuất khỏi phiên làm việc hiện tại.
  - `FR-06.2`: Phía Client PHẢI hủy bỏ việc lưu trữ Access Token và Refresh Token trong bộ nhớ/cookie.
  - `FR-06.3`: Phía Server PHẢI đưa Refresh Token tương ứng vào danh sách thu hồi (Revocation Blacklist trong Redis) để ngăn chặn việc tái sử dụng trái phép.

---

### 3.2 SPRINT 2: SÀN GIAO DỊCH, QUẢN LÝ SÂN & LỊCH TRỐNG (Marketplace, Venues and Availability)
*Thời lượng: 25 ngày (22/09/2026 – 19/10/2026)*  
*Mục tiêu chính: Xây dựng mô hình phân cấp cơ sở thể thao, các công cụ quản lý sân cho chủ cơ sở, cùng hệ thống tìm kiếm, xem chi tiết và biểu đồ lịch trống cho người chơi.*

#### Mô hình quan hệ thực thể sân bãi phân cấp (Hierarchy)
Hệ thống quản lý cụm sân được tổ chức chuẩn hóa theo mô hình 4 cấp:
$$\text{Chủ sở hữu (Provider User)} \longrightarrow \text{Thương hiệu/Cửa hàng (Shop)} \longrightarrow \text{Chi nhánh cơ sở (Venue)} \longrightarrow \text{Sân con (Court)}$$
- **Shop:** Đại diện cho tư cách kinh doanh (VD: "Hệ thống thể thao Vesanre").
- **Venue:** Địa điểm vật lý thực tế có địa chỉ, tọa độ bản đồ, giờ mở/đóng cửa (VD: "Vesanre Chi nhánh Hải Châu - Đà Nẵng").
- **Court:** Từng sân bóng cụ thể trong cơ sở, gắn với một loại hình môn thể thao (VD: "Sân bóng đá số 1", "Sân Pickleball VIP 2", "Sân Cầu lông B3").

#### [FR-07] Quản lý cơ sở sân bãi dành cho Chủ sân (Venue & Court CRUD)
- **Tác nhân:** Chủ sân (Venue Provider)
- **Mô tả chi tiết:**
  - `FR-07.1`: Chủ sân PHẢI có quyền tạo mới, cập nhật thông tin Chi nhánh (Venue): Tên cơ sở, Địa chỉ chi tiết, Tỉnh/Thành phố, Quận/Huyện, Tọa độ GPS, Số điện thoại hỗ trợ, Mô tả giới thiệu, Tiện ích đi kèm (Wifi, Nhà tắm, Nước uống, Đỗ xe ô tô, Đèn chiếu sáng).
  - `FR-07.2`: Chủ sân PHẢI có quyền tải lên danh sách hình ảnh thực tế của cơ sở (tối đa 10 ảnh chất lượng cao/cơ sở).
  - `FR-07.3`: Trong từng Venue, Chủ sân PHẢI có quyền thêm mới các Sân con (Court): Đặt tên sân, chọn Bộ môn thể thao (Bóng đá, Pickleball, Cầu lông, Bóng chuyền, Bóng rổ, Tennis), Loại mặt sân (Cỏ nhân tạo, Sàn gỗ, Thảm cao su), Trạng thái hoạt động (Hoạt động / Tạm bảo trì).
  - `FR-07.4`: Hệ thống PHẢI kiểm tra quyền sở hữu (Ownership Check) trên từng API xử lý: Chủ sân chỉ được phép xem, sửa hoặc xóa cơ sở và sân con thuộc quyền sở hữu của chính mình, ngăn chặn tuyệt đối lỗi IDOR.

#### [FR-08] Cấu hình biểu giá linh hoạt & Khung giờ cao điểm/thấp điểm
- **Tác nhân:** Chủ sân (Venue Provider)
- **Mô tả chi tiết:**
  - `FR-08.1`: Chủ sân PHẢI có quyền định cấu hình khung giờ mở cửa và đóng cửa cho toàn Venue (VD: 06:00 đến 23:00 hàng ngày).
  - `FR-08.2`: Chủ sân PHẢI có quyền thiết lập độ dài mỗi slot tiêu chuẩn (mặc định 60 phút, hoặc linh hoạt 90 phút).
  - `FR-08.3`: Hệ thống PHẢI cho phép cấu hình giá động theo khung thời gian:
    - **Giá ngày thường (Off-peak hours):** Áp dụng từ 06:00 đến 16:00 các ngày thứ Hai đến thứ Sáu.
    - **Giá cao điểm (Peak hours):** Áp dụng từ 16:00 đến 23:00 các ngày trong tuần và toàn bộ khung giờ thứ Bảy, Chủ Nhật.
  - `FR-08.4`: Chủ sân PHẢI có quyền thiết lập tỷ lệ tiền cọc tối thiểu (Ví dụ: 30%, 50% hoặc 100% giá trị slot) cần phải trả trước để giữ chỗ.

#### [FR-09] Tìm kiếm và Lọc sân đa tiêu chí (Multi-criteria Search)
- **Tác nhân:** Khách vãng lai (Guest), Khách hàng (Customer)
- **Mô tả chi tiết:**
  - `FR-09.1`: Người dùng có thể tìm kiếm sân theo Từ khóa tên cơ sở, Tên đường hoặc Khu vực hành chính (Quận/Huyện/Thành phố).
  - `FR-09.2`: Hệ thống PHẢI cung cấp bộ lọc kết hợp:
    - Phân loại bộ môn thể thao (Bóng đá, Pickleball, Cầu lông, Bóng chuyền, Bóng rổ, Tennis).
    - Khoảng giá trên mỗi giờ (VD: Từ 100.000 VNĐ đến 300.000 VNĐ).
    - Tiện ích đi kèm (Có bãi đỗ xe ô tô, có điều hòa/nhà tắm, có cho thuê dụng cụ).
    - Đánh giá xếp hạng (Từ 4 sao trở lên).
  - `FR-09.3`: Kết quả tìm kiếm PHẢI hỗ trợ phân trang (Pagination với 10-20 kết quả/trang) và sắp xếp theo: Giá thấp đến cao, Đánh giá cao nhất, Khoảng cách gần nhất (khi người dùng cấp quyền vị trí).

#### [FR-10] Hiển thị Chi tiết Sân bãi & Lưới lịch trống (Venue Details & Availability Grid)
- **Tác nhân:** Khách vãng lai (Guest), Khách hàng (Customer)
- **Mô tả chi tiết:**
  - `FR-10.1`: Màn hình chi tiết Venue PHẢI hiển thị đầy đủ: Thư viện hình ảnh, Bản đồ định vị, Địa chỉ, Danh sách tiện ích, Nội quy sân bãi và Điểm đánh giá trung bình.
  - `FR-10.2`: Hệ thống PHẢI hiển thị giao diện Lưới lịch (Calendar / Slot Grid) trực quan theo từng ngày được chọn:
    - Trục tung/hoành phân tách theo từng Sân con (Court 1, Court 2,...) và các khung giờ trong ngày.
    - Phân định rõ 3 trạng thái của từng slot:
      - **Xanh (Trống / Available):** Khách có thể bấm vào để tiến hành đặt chỗ. Kèm giá tiền cụ thể của khung giờ.
      - **Vàng (Đang giữ chỗ / Locked Pending):** Đang có người giữ slot thanh toán trong vòng 5-10 phút.
      - **Đỏ / Xám (Đã đặt / Booked / Maintenance):** Đã có khách đặt thành công hoặc sân đang bảo trì, không thể bấm chọn.

---

### 3.3 SPRINT 3: ĐẶT SÂN, KHÓA SLOT, ĐẶT CỌC & HOÀN TIỀN (Booking, Deposit and Refund)
*Thời lượng: 24 ngày (20/10/2026 – 12/11/2026)*  
*Mục tiêu chính: Trọng tâm cốt lõi của nền tảng; thực thi thuật toán kiểm soát đồng thời chống đặt trùng, quy trình khóa slot tạm thời, thanh toán cọc bảo đảm tính lũy thừa và hủy lịch hoàn cọc.*

#### Cơ chế kỹ thuật chống Đặt trùng (Double-Booking Prevention Core)
Hệ thống sử dụng bảng `court_schedule_blocks` làm **nguồn chân lý duy nhất (Single Source of Truth)** cho tình trạng chiếm dụng sân.
- Dữ liệu thời gian của mỗi slot được lưu trữ dưới dạng kiểu khoảng thời gian có múi giờ: `time_slot tstzrange`.
- Thiết lập ràng buộc loại trừ PostgreSQL Exclusion Constraint:
  ```sql
  ALTER TABLE court_schedule_blocks
  ADD CONSTRAINT no_overlapping_court_bookings
  EXCLUDE USING gist (
      court_id WITH =,
      time_slot WITH &&
  ) WHERE (status IN ('HELD', 'CONFIRMED'));
  ```
- **Ý nghĩa kỹ thuật:** Bất kỳ hai giao dịch nào cố tình tạo block thời gian giao nhau (`&&`) trên cùng một `court_id` với trạng thái `HELD` (Đang giữ chỗ) hoặc `CONFIRMED` (Đã thanh toán cọc) sẽ lập tức bị cơ sở dữ liệu chặn đứng với ngoại lệ `23P01 exclusion_violation`, ngăn chặn hoàn toàn race condition ở tầng thấp nhất.

#### [FR-11] Quy trình Đặt sân và Khóa slot thời gian thực (Slot Reservation Hold)
- **Tác nhân:** Khách hàng (Customer)
- **Mô tả chi tiết:**
  - `FR-11.1`: Khách hàng chọn một hoặc nhiều slot trống hợp lệ trên Lưới lịch và nhấn "Đặt sân".
  - `FR-11.2`: Backend khởi tạo một giao dịch Database Transaction:
    - Chèn bản ghi vào `court_schedule_blocks` với trạng thái `HELD`, thời gian hết hạn giữ chỗ `expires_at = NOW() + INTERVAL '10 MINUTES'`.
    - Tạo đơn đặt sân tạm thời `bookings` với trạng thái `PENDING_PAYMENT`.
  - `FR-11.3`: Nếu xảy ra xung đột đồng thời (khách khác bấm trước trong cùng mili-giây), hệ thống PHẢI bắt lỗi Exclusion Constraint, rollback giao dịch và thông báo: *"Khung giờ này vừa có người khác giữ chỗ, vui lòng chọn khung giờ khác!"*.
  - `FR-11.4`: Trong thời gian 10 phút giữ chỗ, slot trên giao diện chung sẽ chuyển sang màu vàng (Locked). Nếu sau 10 phút khách hàng không hoàn tất thanh toán, hệ thống tự động (Background Job / Scheduled Task) chuyển trạng thái block sang `EXPIRED`, giải phóng slot về trạng thái khả dụng cho người khác.

#### [FR-12] Thanh toán tiền cọc qua Cổng thanh toán trực tuyến
- **Tác nhân:** Khách hàng (Customer), Cổng thanh toán (VNPay / MoMo)
- **Mô tả chi tiết:**
  - `FR-12.1`: Hệ thống hiển thị bảng tóm tắt chi phí: Tổng tiền sân, Tiền cọc yêu cầu thanh toán (VD: 30%), Số tiền còn lại phải trả tại sân khi đến chơi.
  - `FR-12.2`: Hệ thống tạo URL thanh toán an toàn chuyển hướng sang cổng thanh toán hoặc hiển thị mã VietQR động chứa chính xác số tiền và mã đơn `booking_id`.
  - `FR-12.3`: **Xử lý Webhook / IPN bảo đảm tính lũy thừa (Idempotency):**
    - Khi nhận tín hiệu thanh toán thành công từ Cổng thanh toán, Backend PHẢI xác minh chữ ký bảo mật (HMAC SHA512 Checksum).
    - Backend kiểm tra đối soát số tiền nhận được so với số tiền cọc trong cơ sở dữ liệu.
    - Bản ghi giao dịch được xử lý dựa trên `payment_transaction_id` duy nhất; nếu webhook bị gửi lại lần 2 do mạng lag, hệ thống trả về mã thành công ngay mà không cộng dồn hay kích hoạt logic trùng lặp.
  - `FR-12.4`: Khi thanh toán cọc thành công, trạng thái đơn `bookings` chuyển thành `CONFIRMED`, `court_schedule_blocks` chuyển thành `CONFIRMED`.
  - `FR-12.5`: Hệ thống phát hành **Mã vé điện tử (Booking Pass)** kèm mã QR độc nhất để khách hàng xuất trình khi đến nhận sân.

#### [FR-13] Khóa slot thủ công dành cho Chủ sân (Manual On-site Booking)
- **Tác nhân:** Chủ sân (Venue Provider)
- **Mô tả chi tiết:**
  - `FR-13.1`: Chủ sân có quyền khóa nhanh các slot trên sân của mình dành cho khách gọi điện thoại hoặc khách quen đặt tại quầy.
  - `FR-13.2`: Bản ghi khóa thủ công được tạo ngay với trạng thái `CONFIRMED` và ghi nhận ghi chú `BOOKED_OFFLINE` (không cần chuyển hướng thanh toán qua cổng điện tử).

#### [FR-14] Hủy lịch đặt & Quy tắc hoàn trả tiền cọc (Cancellation & Refund)
- **Tác nhân:** Khách hàng (Customer), Chủ sân (Venue Provider), Cổng thanh toán
- **Mô tả chi tiết:**
  - `FR-14.1`: Khách hàng có quyền yêu cầu hủy đơn đặt sân đã xác nhận trực tiếp trên hệ thống.
  - `FR-14.2`: **Chính sách hoàn cọc tự động căn cứ theo mốc thời gian:**
    - **Hủy trước giờ bắt đầu > 24 giờ:** Hoàn lại **100%** số tiền cọc đã thanh toán (trừ phí giao dịch cổng nếu có).
    - **Hủy trước giờ bắt đầu từ 12 giờ đến 24 giờ:** Hoàn lại **50%** số tiền cọc.
    - **Hủy trước giờ bắt đầu < 12 giờ:** Không hoàn cọc (**0%**), toàn bộ tiền cọc được đền bù cho chủ sân để bù đắp chi phí cơ hội.
  - `FR-14.3`: Khi lệnh hủy hợp lệ được xác nhận, hệ thống gọi API Refund của Cổng thanh toán, cập nhật trạng thái đơn thành `CANCELLED_REFUNDED`, đồng thời cập nhật `court_schedule_blocks` thành `CANCELLED` để giải phóng slot tức thì lên sàn.

---

### 3.4 SPRINT 4: QUẢN TRỊ HỆ THỐNG, ĐÁNH GIÁ, THÔNG BÁO & SẴN SÀNG PHÁT HÀNH (Administration, Quality, and Release Readiness)
*Thời lượng: 24 ngày (13/11/2026 – 07/12/2026)*  
*Mục tiêu chính: Bảng điều khiển kiểm duyệt của Quản trị viên, cơ chế đánh giá hậu mãi, hệ thống thông báo tự động và chuẩn bị bàn giao hoàn chỉnh.*

#### [FR-15] Kiểm duyệt và Duyệt hồ sơ sân bãi (Admin Venue Approval)
- **Tác nhân:** Quản trị viên (Admin)
- **Mô tả chi tiết:**
  - `FR-15.1`: Mọi cơ sở (Venue) mới do Chủ sân đăng ký mặc định ở trạng thái `PENDING_APPROVAL` và chưa được hiển thị trên trang tìm kiếm công khai.
  - `FR-15.2`: Admin có giao diện duyệt chi tiết: Xem giấy tờ chứng minh, hình ảnh sân, địa chỉ, bảng giá niêm yết.
  - `FR-15.3`: Admin có quyền thực hiện:
    - **Phê duyệt (Approve):** Chuyển trạng thái Venue sang `ACTIVE`, sân chính thức mở bán trên sàn.
    - **Từ chối (Reject):** Bắt buộc nhập lý do từ chối (VD: Địa chỉ không có thật, ảnh vi phạm bản quyền) và gửi thông báo phản hồi cho Chủ sân.
  - `FR-15.4`: Admin có quyền tạm khóa (`SUSPENDED`) bất kỳ cơ sở nào có nhiều phản ánh xấu từ khách hàng.

#### [FR-16] Quản lý người dùng và Giám sát hệ thống (User & Platform Oversight)
- **Tác nhân:** Quản trị viên (Admin)
- **Mô tả chi tiết:**
  - `FR-16.1`: Admin có quyền tra cứu, tìm kiếm toàn bộ danh sách tài khoản theo Email, Tên, Số điện thoại và Vai trò.
  - `FR-16.2`: Admin có quyền khóa tài khoản (`BAN`) đối với người dùng gian lận, phá hoại hoặc chủ sân vi phạm quy chế.
  - `FR-16.3`: Bảng điều khiển quản trị (Dashboard) hiển thị các chỉ số tổng quan: Tổng số người dùng, Tổng số cơ sở hoạt động, Tổng số lượt đặt sân trong tháng, Tổng giá trị cọc luân chuyển qua sàn.

#### [FR-17] Hệ thống Đánh giá và Nhận xét sân bãi (Rating & Reviews)
- **Tác nhân:** Khách hàng (Customer)
- **Mô tả chi tiết:**
  - `FR-17.1`: Chỉ những khách hàng đã có đơn đặt sân ở trạng thái `COMPLETED` (đã qua giờ chơi thành công) mới có quyền gửi đánh giá cho cơ sở đó (Ngăn chặn đánh giá ảo).
  - `FR-17.2`: Nội dung đánh giá bao gồm: Điểm sao từ 1 đến 5, nhận xét văn bản và hình ảnh chụp thực tế (tùy chọn).
  - `FR-17.3`: Hệ thống tự động tính toán lại Điểm đánh giá trung bình của Venue theo công thức trọng số và cập nhật hiển thị công khai.
  - `FR-17.4`: Chủ sân có quyền gửi phản hồi (Reply) đối với từng nhận xét của khách hàng.

#### [FR-18] Hệ thống Thông báo tự động (Automated Notifications)
- **Tác nhân:** Hệ thống (Notification Engine), Khách hàng, Chủ sân
- **Mô tả chi tiết:**
  - `FR-18.1`: **Thông báo Đặt cọc thành công:** Gửi Email biên nhận chi tiết ngay lập tức kèm Mã QR vé vào cổng cho Khách hàng; đồng thời gửi cảnh báo thông báo (Web Alert/Push) cho Chủ sân về slot vừa được đặt.
  - `FR-18.2`: **Nhắc lịch trước trận đấu:** Tự động gửi Email/SMS nhắc nhở khách hàng trước giờ bóng lăn 02 tiếng.
  - `FR-18.3`: **Thông báo Hủy lịch & Hoàn tiền:** Thông báo xác nhận hủy lịch và thời gian dự kiến tiền cọc về tài khoản ngân hàng.

---

## 4. YÊU CẦU PHI CHỨC NĂNG (NON-FUNCTIONAL REQUIREMENTS - ISO 25010)

### 4.1 Hiệu năng và Tải hệ thống (Performance Efficiency)
- **[NFR-01] Thời gian phản hồi API:** Đối với 95% các yêu cầu đọc dữ liệu thông thường (tìm kiếm sân, xem chi tiết cơ sở, xem biểu đồ lịch trống), thời gian phản hồi (API Latency) PHẢI dưới **500 ms** trong điều kiện mạng bình thường.
- **[NFR-02] Thời gian thực thi giao dịch đặt sân:** Thao tác khóa slot (`HELD`) và kiểm tra ràng buộc Exclusion Constraint PHẢI hoàn tất trong vòng dưới **1000 ms**.
- **[NFR-03] Khả năng xử lý đồng thời:** Hệ thống chịu tải tối thiểu **200 người dùng hoạt động đồng thời (CCU)** tại giai đoạn MVP mà không gây treo ứng dụng hoặc tràn bộ nhớ JVM.

### 4.2 An toàn và Bảo mật hệ thống (Security & OWASP Compliance)
- **[NFR-04] Mã hóa mật khẩu:** Mật khẩu người dùng bắt buộc được mã hóa một chiều bằng **BCrypt** với hệ số `strength = 12`. Không bao giờ lưu trữ hoặc ghi log mật khẩu gốc dạng bản rõ.
- **[NFR-05] Bảo mật phiên làm việc (JWT Security):** Access Token có thời hạn tối đa 30 phút, ký bằng thuật toán HMAC-SHA256 với Secret Key có độ dài tối thiểu 256 bits lấy từ biến môi trường. Lưu trữ token phía Client tuân thủ chính sách phòng chống XSS/CSRF.
- **[NFR-06] Kiểm soát truy cập và phòng chống IDOR:** Mọi thao tác cập nhật dữ liệu của Chủ sân (sửa sân, sửa giá, xem báo cáo) PHẢI xác minh điều kiện `venue.provider_id == current_user.id` tại tầng Business Service.
- **[NFR-07] Phòng vệ tiêm mã độc (Injection & XSS):** 
  - Toàn bộ truy vấn cơ sở dữ liệu bắt buộc thông qua Spring Data JPA / Hibernate có Parameterized Queries.
  - Dữ liệu đầu vào từ người dùng (đánh giá, mô tả sân) PHẢI được kiểm tra (Sanitize) và mã hóa ký tự đầu ra (HTML entity encoding).
- **[NFR-08] An toàn tải lên tệp tin (File Upload Security):** Chỉ chấp nhận định dạng ảnh hợp lệ (`image/jpeg`, `image/png`, `image/webp`). Giới hạn dung lượng mỗi ảnh tối đa 5MB. File được đổi tên bằng chuỗi UUID ngẫu nhiên trước khi lưu lên Cloud Storage, không lưu trực tiếp trong thư mục webroot.

### 4.3 Tính tin cậy và toàn vẹn dữ liệu (Reliability & Data Integrity)
- **[NFR-09] Chống xung đột dữ liệu (Zero Double-Booking Guarantee):** Nhờ cơ chế PostgreSQL Exclusion Constraint `tstzrange`, tỷ lệ đặt trùng sân trên hệ thống cam kết bằng **0%**.
- **[NFR-10] Tính lũy thừa thanh toán (Webhook Idempotency):** Mọi sự kiện Webhook từ cổng thanh toán PHẢI được kiểm tra qua bảng `idempotent_consumer_keys`. Tuyệt đối không xảy ra tình trạng trừ tiền hoặc hoàn tiền lặp lại khi mạng bị chập chờn.
- **[NFR-11] Tính sẵn sàng (Availability):** Mục tiêu thời gian hoạt động của hệ thống (Uptime) đạt tối thiểu **99.0%** trong thời gian diễn ra dự án Capstone.
- **[NFR-12] Sao lưu dữ liệu định kỳ (Backup & Recovery):** Cơ sở dữ liệu PostgreSQL được cấu hình tự động sao lưu định kỳ hàng ngày (Daily Automated Dump). Thời gian phục hồi mục tiêu (RTO) dưới 2 giờ và mất mát dữ liệu tối đa (RPO) dưới 24 giờ.

### 4.4 Khả năng mở rộng (Scalability)
- **[NFR-13] Kiến trúc không trạng thái (Stateless Backend):** Máy chủ Spring Boot không lưu session trong bộ nhớ ram cục bộ, cho phép nhân bản nhiều instance phía sau Load Balancer khi lượng người dùng tăng cao trong tương lai.
- **[NFR-14] Tối ưu hóa bộ đệm (Caching Strategy):** Dữ liệu danh mục môn thể thao, danh sách tỉnh thành và thông tin chi tiết cơ sở ít thay đổi được lưu bộ đệm tại Redis với TTL hợp lý (15 - 60 phút) nhằm giảm tải cho PostgreSQL.

### 4.5 Tính khả dụng và Trải nghiệm người dùng (Usability & UX)
- **[NFR-15] Thiết kế tương thích thiết bị (Responsive Web Design):** Giao diện web hiển thị tối ưu và mượt mà trên tất cả các kích thước màn hình phổ biến: Điện thoại thông minh (từ 375px), Máy tính bảng (768px - 1024px) và Màn hình máy tính để bàn (>= 1200px).
- **[NFR-16] Khả năng tiếp cận thao tác:** Khách hàng có thể hoàn tất quy trình từ lúc tìm sân đến khi nhận mã QR đặt cọc thành công trong không quá **5 bước nhấp chuột / chạm màn hình**.

### 4.6 Khả năng bảo trì và vận hành (Maintainability & DevOps)
- **[NFR-17] Container hóa đồng nhất:** Toàn bộ thành phần hệ thống (Frontend, Backend, PostgreSQL, Redis) PHẢI khởi chạy đồng bộ được thông qua một lệnh duy nhất: `docker-compose up -d`.
- **[NFR-18] Ghi nhật ký tập trung (Structured Logging):** Hệ thống ứng dụng định dạng log chuẩn (JSON hoặc Timestamp - Log Level - Class - Message), che giấu các thông tin nhạy cảm (số thẻ, mật khẩu, JWT token) trong log.

---

## 5. YÊU CẦU GIAO DIỆN HỆ THỐNG (SYSTEM INTERFACES)

### 5.1 Giao diện người dùng (User Interfaces)
Hệ thống bao gồm 4 phân hệ giao diện chính:
1. **Phân hệ Trang chủ & Khám phá (Public & Customer Portal):**
   - Thanh tìm kiếm thông minh nổi bật (Môn thể thao, Khu vực, Thời gian).
   - Bộ sưu tập sân nổi bật, sân gần đây, ưu đãi giờ vàng.
   - Trang kết quả tìm kiếm với bộ lọc linh hoạt dạng thanh bên (Sidebar).
   - Trang chi tiết cơ sở thể thao với thư viện hình ảnh đa phương tiện, bản đồ vị trí, và Ma trận lịch sân trực quan theo khung thời gian thực.
2. **Phân hệ Đặt chỗ & Thanh toán (Booking & Checkout Flow):**
   - Màn hình chọn nhanh các slot trống trên lưới trực quan.
   - Trang tóm tắt đơn đặt, nhập mã khuyến mãi (nếu có) và hiển thị số tiền cọc quy định.
   - Cửa sổ quét mã VietQR động tự động cập nhật trạng thái khi thanh toán thành công.
   - Trang hiển thị Vé điện tử (Booking Confirmation Pass) chứa mã QR định danh đơn.
3. **Phân hệ Bảng điều khiển Chủ sân (Provider Management Portal):**
   - Bảng tổng quan trạng thái các sân trong ngày theo dạng Timeline ngang.
   - Màn hình tạo mới và hiệu chỉnh cơ sở, danh sách sân con và biểu giá giờ cao điểm.
   - Công cụ bấm khóa nhanh slot trực tiếp tại quầy cho khách vãng lai.
   - Báo cáo số liệu lượt khách và tiền cọc tích lũy theo tuần/tháng.
4. **Phân hệ Quản trị viên (Admin Portal):**
   - Danh sách hồ sơ đăng ký sân chờ duyệt kèm công cụ xem xét tài liệu chứng minh.
   - Bảng quản lý người dùng, phân quyền và khóa tài khoản vi phạm.
   - Bảng điều khiển theo dõi sức khỏe hệ thống và lưu vết kiểm toán (Audit Logs).

### 5.2 Giao diện phần mềm & Thiết kế API (Software Interfaces & RESTful API)
Backend cung cấp chuẩn giao tiếp RESTful API với định dạng dữ liệu JSON, phân tách theo các nhóm tài nguyên:

| Phương thức | Endpoint Mẫu | Phân quyền | Mục đích nghiệp vụ |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public | Đăng ký tài khoản người dùng mới |
| `POST` | `/api/v1/auth/login` | Public | Đăng nhập hệ thống, nhận cặp JWT Tokens |
| `POST` | `/api/v1/auth/google` | Public | Xác thực đăng nhập qua Google OAuth2 |
| `POST` | `/api/v1/auth/refresh-token` | Public | Cấp mới Access Token từ Refresh Token |
| `GET` | `/api/v1/users/me` | Authenticated | Lấy thông tin hồ sơ người dùng đang đăng nhập |
| `PUT` | `/api/v1/users/me` | Authenticated | Cập nhật thông tin cá nhân và ảnh đại diện |
| `GET` | `/api/v1/venues` | Public | Tìm kiếm danh sách sân bãi theo bộ lọc |
| `GET` | `/api/v1/venues/{id}` | Public | Lấy chi tiết thông tin và tiện ích cơ sở |
| `GET` | `/api/v1/venues/{id}/availability` | Public | Lấy sơ đồ lịch và tình trạng slot theo ngày |
| `POST` | `/api/v1/provider/venues` | Provider | Tạo mới chi nhánh sân thể thao |
| `PUT` | `/api/v1/provider/courts/{id}/pricing` | Provider | Cập nhật biểu giá giờ cao điểm / thấp điểm |
| `POST` | `/api/v1/bookings/hold` | Customer | Khóa tạm giữ slot trong 10 phút để thanh toán |
| `POST` | `/api/v1/bookings/{id}/pay-deposit`| Customer | Lấy thông tin mã QR / URL cổng thanh toán |
| `POST` | `/api/v1/payments/webhook` | Gateway Server| Tiếp nhận Webhook xác nhận giao dịch cọc |
| `POST` | `/api/v1/bookings/{id}/cancel` | Customer/Prov | Yêu cầu hủy lịch và hoàn trả tiền cọc |
| `GET` | `/api/v1/admin/venues/pending` | Admin | Lấy danh sách sân chờ kiểm duyệt |
| `PUT` | `/api/v1/admin/venues/{id}/approve` | Admin | Phê duyệt niêm yết sân lên hệ thống |

### 5.3 Giao diện truyền thông & Dịch vụ bên ngoài (Communication & Third-party Interfaces)
1. **Google Identity Services (OAuth2):** Giao thức HTTPS RESTful, truyền nhận JWT ID Token để định danh tài khoản Google.
2. **Cổng thanh toán trực tuyến (Payment Gateway - VNPay / MoMo):** 
   - Tạo yêu cầu thanh toán qua HTTPS POST có ký số Checksum HMAC-SHA512.
   - Nhận tín hiệu Instant Payment Notification (IPN Webhook) bất đồng bộ bảo đảm cơ chế tái thử (Retry) và Idempotency.
3. **Dịch vụ Bản đồ số (Map & Geocoding):** Tích hợp OpenStreetMap / Mapbox / Google Maps API để chuyển đổi địa chỉ thành tọa độ (Latitude, Longitude) và hiển thị bản đồ trực quan.
4. **Dịch vụ Lưu trữ Đám mây (Cloud Media Storage):** Tích hợp Cloudinary API hoặc AWS S3 SDK để tải lên, tự động nén kích thước và sinh link CDN phục vụ hình ảnh sân bãi tốc độ cao.
5. **Dịch vụ Thư điện tử (Notification SMTP):** Giao thức SMTP / SendGrid API gửi thư xác nhận đặt cọc thành công, đính kèm mã vé điện tử QR Code và thư nhắc lịch.

---

## 6. MA TRẬN TRUY XUẤT NGUỒN GỐC YÊU CẦU (TRACEABILITY MATRIX)

Ma trận dưới đây thể hiện tính liên kết 2 chiều giữa Mục tiêu đề tài (Proposal Objectives), Yêu cầu Chức năng (FR), Use Case, Sprint hiện thực và Phương pháp kiểm thử nghiệm thu (Verification Method):

| ID Yêu cầu | Tên Chức Năng | Mục tiêu Proposal | Sprint | Use Case ID | Phương pháp Nghiệm thu (Verification) |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **FR-01** | Đăng ký & Đăng nhập chuẩn | Mục 6.1.2 & FR01 | Sprint 1 | UC-01, UC-02 | Test tự động (Unit Test BCrypt, Integration Test API JWT, Form validation) |
| **FR-02** | Đăng nhập Google OAuth2 | Mục 6.1.2 & FR01 | Sprint 1 | UC-03 | Kiểm thử luồng xác thực Google Token Sandbox |
| **FR-03** | Xác thực & Phân quyền RBAC | Mục 7.2.1 FR01 & NFR02| Sprint 1 | UC-04 | Kiểm thử phân quyền Endpoint (401 vs 403 Test Matrix) |
| **FR-04** | Quên / Đặt lại mật khẩu | Mục 7.1 | Sprint 1 | UC-05 | Kiểm thử luồng sinh OTP/Reset Token hết hạn 15 phút |
| **FR-05** | Quản lý thông tin hồ sơ | Mục 6.1.2 | Sprint 1 | UC-06 | Kiểm thử CRUD Profile & Upload ảnh đại diện |
| **FR-06** | Đăng xuất & Thu hồi Token | Mục 7.2.1 FR01 | Sprint 1 | UC-07 | Kiểm thử Blacklist Token trong Redis sau đăng xuất |
| **FR-07** | Quản lý cơ sở sân (Venue CRUD)| Mục 6.1.2 & FR06 | Sprint 2 | UC-08 | Kiểm thử phân cấp Shop-Venue-Court, ngăn chặn IDOR |
| **FR-08** | Cấu hình biểu giá linh hoạt | Mục 6.1.2 & FR06 | Sprint 2 | UC-09 | Kiểm thử tính toán giá tự động khung cao điểm vs thấp điểm |
| **FR-09** | Tìm kiếm & Lọc đa tiêu chí | Mục 6.1.2 & FR02 | Sprint 2 | UC-11 | Kiểm thử tìm kiếm theo môn thể thao, vị trí, khoảng giá |
| **FR-10** | Chi tiết sân & Lưới lịch trống | Mục 6.1.2 & FR03, FR04| Sprint 2 | UC-10, UC-12 | Kiểm thử render lịch trống theo ngày, màu sắc trạng thái |
| **FR-11** | Khóa slot & Chống đặt trùng | Mục 6.1.2 & FR05 | Sprint 3 | UC-13 | **Kiểm thử đồng thời (JMeter Concurrency Test)** với 50 luồng cùng đặt 1 slot |
| **FR-12** | Thanh toán cọc & Webhook | Mục 6.1.2 & FR05 | Sprint 3 | UC-14 | Kiểm thử thanh toán sandbox, chữ ký số và tính lũy thừa lặp Webhook |
| **FR-13** | Khóa slot thủ công tại quầy | Mục 6.1.2 & FR06 | Sprint 3 | UC-13 | Kiểm thử chủ sân khóa slot cho khách offline |
| **FR-14** | Hủy lịch & Hoàn tiền cọc | Mục 6.1.2 & FR05 | Sprint 3 | UC-15 | Kiểm thử tính chính xác tỷ lệ hoàn tiền theo mốc 24h / 12h |
| **FR-15** | Quản trị duyệt hồ sơ sân | Mục 6.1.2 & FR07 | Sprint 4 | UC-17 | Kiểm thử luồng Approve/Reject và kích hoạt niêm yết |
| **FR-16** | Quản trị người dùng & Giám sát| Mục 6.1.2 & FR07 | Sprint 4 | UC-18 | Kiểm thử khóa tài khoản và hiển thị thống kê |
| **FR-17** | Đánh giá & Nhận xét sân | Mục 6.1.2 & FR08 | Sprint 4 | UC-19 | Kiểm thử ràng buộc chỉ người đã chơi mới được đánh giá |
| **FR-18** | Thông báo tự động Email/Alert| Mục 7.2.1 FR09 | Sprint 4 | UC-20 | Kiểm thử gửi Email vé QR Code và thông báo nhắc lịch |

---

## 7. ĐIỀU KHOẢN PHÊ DUYỆT TÀI LIỆU (APPROVAL SIGN-OFF)

Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) này đã được rà soát kỹ lưỡng, đối chiếu với Đề cương Đồ án Capstone 1 (CMU-SE 432 AIS) và các biên bản thống nhất kỹ thuật của nhóm C1SE.05.

| Đại diện | Họ và Tên | Chữ ký | Ngày phê duyệt |
| :--- | :--- | :--- | :--- |
| **Trưởng nhóm / Scrum Master** | Trần Phước Trí | __________________ | ____ / ____ / 2026 |
| **Thành viên phụ trách UX/UI & FE**| Nguyễn Văn Phúc Thiện| __________________ | ____ / ____ / 2026 |
| **Thành viên phụ trách Backend** | Đào Nhật Sơn | __________________ | ____ / ____ / 2026 |
| **Thành viên phụ trách Frontend** | Lê Anh Khoa | __________________ | ____ / ____ / 2026 |
| **Thành viên phụ trách Backend** | Lê Nhật Khoa | __________________ | ____ / ____ / 2026 |
| **Giảng viên hướng dẫn (Mentor)** | ThS. Lê Kim Hoàng | __________________ | ____ / ____ / 2026 |
