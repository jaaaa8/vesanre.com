# SportHub Backend

SportHub là nền tảng web marketplace kết nối người cần thuê sân thể thao với các đơn vị cung cấp sân. Hệ thống giúp tập trung thông tin sân, lịch trống, giá và đánh giá; đồng thời số hóa quy trình đặt sân và quản lý lịch.

## Mục đích

- **Khách hàng:** tìm kiếm, so sánh, xem lịch trống, đặt sân và đánh giá sau khi sử dụng.
- **Nhà cung cấp:** đăng và quản lý địa điểm, sân, hình ảnh, giá, giờ hoạt động và booking.
- **Quản trị viên:** xét duyệt nhà cung cấp/địa điểm, quản lý người dùng và giám sát nền tảng.

## Chức năng chính của MVP

- Đăng ký, đăng nhập và phân quyền theo vai trò.
- Tìm sân theo địa điểm, môn thể thao, giá và thời gian.
- Quản lý cấu trúc `Shop -> Venue -> Court` và lịch khả dụng.
- Đặt sân, đặt cọc, hủy và hoàn tiền ở mức cơ bản.
- Quản lý booking, đánh giá, thông báo và kiểm duyệt.
- Ngăn đặt trùng sân trong cùng khung giờ.

## Công nghệ

### Backend hiện tại

- Java 17, Spring Boot 4.1.1 và Gradle.
- Spring Data JPA.
- PostgreSQL.
- Flyway để quản lý migration cơ sở dữ liệu.
- Lombok và JUnit.

### Kiến trúc toàn hệ thống dự kiến

- Frontend: Next.js, React và TypeScript.
- Bảo mật: Spring Security và JWT.
- Hạ tầng/tích hợp: Docker, Cloud Storage, Map API và cổng thanh toán.
- Redis chỉ được bổ sung khi có nhu cầu cache hoặc rate limiting được chứng minh.

## Phạm vi repository

Repository này chứa backend của SportHub. Hiện tại dự án đã có schema PostgreSQL, migration Flyway và các entity nền tảng cho người dùng, nhà cung cấp, sân, booking, thanh toán, đánh giá, thông báo và audit. REST API cùng các luồng nghiệp vụ sẽ được hoàn thiện theo từng sprint.

## Ngoài phạm vi MVP

Ứng dụng di động, ghép người chơi, đề xuất bằng AI, định giá động, quản lý giải đấu và đối soát doanh thu nâng cao chưa thuộc phiên bản đầu tiên.
