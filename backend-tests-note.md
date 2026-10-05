# Ghi chú các test trong backend

Đối chiếu source ngày **05/10/2026**, tại `src/test/java/com/vesanrebackend`. Tài liệu mô tả test đang kiểm tra chức năng/API nào; **không phải báo cáo kết quả chạy test**. Lần chạy `gradlew test` ngày 05/10/2026 trên DB test local: **80/80 PASS**, 0 skip.

## 1. Tổng quan

Hiện có **15 lớp test, 80 method có `@Test`**, và **1 file hỗ trợ** không chứa test. Một method có thể kiểm tra nhiều tình huống/API, nên 80 không phải số endpoint hoặc số tình huống riêng lẻ.

| Loại | Số lớp | Số method | Phạm vi |
| --- | ---: | ---: | --- |
| Unit test | 6 | 30 | Gọi trực tiếp service/helper; giả lập repository hoặc dịch vụ ngoài khi cần. |
| Web MVC/security test | 4 | 20 | `@WebMvcTest` + MockMvc; kiểm tra phân quyền, validation, CORS; service được mock. |
| Spring integration/context test | 5 | 30 | Khởi động Spring; các lớp API gọi HTTP thật trên cổng ngẫu nhiên và kiểm tra PostgreSQL. Lớp mail kiểm tra transaction; lớp application chỉ kiểm tra context. |

Quy ước: `{id}`, `{venueId}`, `{courtId}`, `{imageId}`, `{userId}`, `{requestId}` là ID tài nguyên. Cột “API liên quan” trong bảng unit test chỉ ra API sử dụng logic đó, không có nghĩa test đã gọi HTTP.

## 2. Xác thực, hồ sơ và đăng ký nhà cung cấp

### [AuthFlowIntegrationTest.java](src/test/java/com/vesanrebackend/controller/auth/AuthFlowIntegrationTest.java) — 3 test

**Loại:** Integration, `@SpringBootTest(RANDOM_PORT)`, RestClient + JDBC.

| Test method | API chính | Chức năng/tình huống được kiểm tra |
| --- | --- | --- |
| `registerLoginProfileAndRoleChecks` | `POST /api/auth/register`, `POST /api/auth/login`, `GET/PATCH /api/profile/me`, `GET /api/profile/provider`, `GET /api/profile/admin` | Đăng ký thành công 201; trùng email/điện thoại 409; body sai và mật khẩu Unicode vượt giới hạn 400. Trường `role=PROVIDER` do client gửi không nâng quyền, tài khoản vẫn là CUSTOMER. Sai mật khẩu 401; email hoa vẫn đăng nhập được; đọc hồ sơ bằng JWT; phone toàn khoảng trắng 400, chuỗi rỗng được dùng để xóa phone; thiếu token 401, CUSTOMER đọc hồ sơ provider/admin 403. |
| `providerApplicationCreatesPendingProfileDraftShopAndVerificationWhileLoginStaysOpen` | `POST /api/profile/provider-application`, `POST /api/auth/login`, `GET /api/profile/provider` | Nộp đơn tạo ProviderProfile PENDING, Shop DRAFT, ProviderVerification PENDING; tài khoản vẫn chỉ có CUSTOMER. Kiểm tra ID tài khoản UUID v7, thiếu token 401, thiếu dữ liệu 400, nộp lại khi PENDING/VERIFIED 409. Người chờ duyệt vẫn đăng nhập được, thấy trạng thái PENDING và chưa được truy cập API profile provider. |
| `adminApprovalGrantsProviderRoleAndRejectedApplicantCanReapply` | `GET /api/admin/providers`, `POST /api/admin/providers/{userId}/approve`, `POST /api/admin/providers/{userId}/reject`, `POST /api/profile/provider-application`, `POST /api/auth/login` | Danh sách phân trang có hồ sơ và shop/verification. Duyệt cấp PROVIDER, kích hoạt shop, cập nhật verification và audit; duyệt lặp 409, ID không có 404, provider gọi API admin 403. Từ chối cần lý do hợp lệ, giữ shop DRAFT; người bị từ chối vẫn đăng nhập và nộp lại được. |

### [ProfileControllerSecurityTest.java](src/test/java/com/vesanrebackend/controller/auth/ProfileControllerSecurityTest.java) — 7 test

**Loại:** Web MVC/security; mock AuthService. Danh tính được đưa vào MockMvc bằng `user(...)` hoặc `jwt(...)`.

| Test method | API | Chức năng/tình huống được kiểm tra |
| --- | --- | --- |
| `profileRequiresBearerToken` | `GET /api/profile/me` | Request không có xác thực trả 401. |
| `customerCannotReadProviderProfile` | `GET /api/profile/provider` | CUSTOMER bị chặn 403. |
| `providerCanReadProviderProfile` | `GET /api/profile/provider` | JWT có ROLE_PROVIDER được phép đọc, trả 200. Đây là kiểm tra quyền ở controller với dữ liệu service giả lập. |
| `providerApplicationRequiresTokenButNoRole` | `POST /api/profile/provider-application` | Thiếu token 401; JWT CUSTOMER được nộp đơn, trả 201, không cần có sẵn PROVIDER. |
| `providerCannotReadAdminProfile` | `GET /api/profile/admin` | PROVIDER bị chặn 403. |
| `preflightAllowsPutAndDelete` | `OPTIONS /api/profile/me` | Preflight từ `http://localhost:5173` với method PUT/DELETE trả 200. Không kiểm tra sự tồn tại của PUT/DELETE trên route này. |
| `vitePreflightIsAllowed` | `OPTIONS /api/profile/me` | Preflight GET từ Vite trả 200 và header `Access-Control-Allow-Origin` đúng origin. |

### [AuthServiceTest.java](src/test/java/com/vesanrebackend/service/auth/AuthServiceTest.java) — 11 test

**Loại:** Unit; mock repository và PasswordEncoder, không gọi HTTP/DB thật.

| Test method | API liên quan | Logic được kiểm tra |
| --- | --- | --- |
| `applyCreatesPendingProfileDraftShopAndPendingVerification` | `POST /api/profile/provider-application` | Tạo profile PENDING, shop DRAFT và verification PENDING; giữ role CUSTOMER, không cấp PROVIDER; tên/slug shop và dữ liệu mặc định đúng. |
| `shopNameOverridesLegalNameWhenProvided` | `POST /api/profile/provider-application` | Có shopName thì dùng làm tên/slug shop thay vì legalName. |
| `reapplyAfterRejectionResetsToPendingKeepsSlugAndAddsVerification` | `POST /api/profile/provider-application` | Hồ sơ REJECTED nộp lại trở thành PENDING, xóa verifiedAt, cập nhật thông tin, giữ slug shop và thêm verification mới. |
| `applyIsConflictWhenPendingVerifiedOrSuspended` | `POST /api/profile/provider-application` | PENDING/VERIFIED/SUSPENDED không được nộp đơn, trả lỗi 409 và không tạo shop/verification. |
| `registerAlwaysCreatesCustomerOnly` | `POST /api/auth/register` | Đăng ký chỉ tạo CUSTOMER, không tạo hồ sơ provider/shop/verification. |
| `loginNormalizesEmailAndRejectsWrongPasswordOrInactiveAccount` | `POST /api/auth/login` | Tìm email đã chuẩn hóa; sai mật khẩu hoặc tài khoản SUSPENDED trả 401. |
| `loginForUnknownEmailStillRunsPasswordCheckBefore401` | `POST /api/auth/login` | Email không tồn tại vẫn thực hiện password check trước khi trả 401; test xác nhận lời gọi, không đo thời gian thực thi. |
| `customerWithPendingApplicationCanLogInAndSeesStatus` | `POST /api/auth/login` | CUSTOMER có hồ sơ PENDING vẫn đăng nhập và nhận providerStatus PENDING. |
| `verifiedProviderCanLogIn` | `POST /api/auth/login` | Provider VERIFIED đăng nhập được, nhận role và trạng thái đúng. |
| `updateProfileRejectsWhitespaceOnlyPhoneButAllowsClearing` | `PATCH /api/profile/me` | Phone toàn khoảng trắng bị từ chối 400 và giữ giá trị cũ; chuỗi rỗng xóa phone thành null. |
| `profileOfInactiveUserIsForbidden` | `GET /api/profile/me` và logic đọc profile dùng chung | Tài khoản SUSPENDED đọc hồ sơ bị lỗi 403. |

## 3. Provider: shop, địa điểm, sân và ảnh

### [ProviderCatalogIntegrationTest.java](src/test/java/com/vesanrebackend/controller/provider/ProviderCatalogIntegrationTest.java) — 15 test

**Loại:** Integration, HTTP thật + PostgreSQL; **ImageStorage được mock**. Fixture tạo provider đã xác minh bằng cập nhật JDBC và thêm role rồi đăng nhập; đây không phải luồng duyệt provider qua admin API.

| Test method | API chính | Chức năng/tình huống được kiểm tra |
| --- | --- | --- |
| `catalogIsPublicAndListsSeededSportsAndAmenities` | `GET /api/catalog/sports`, `GET /api/catalog/amenities` | Không cần token; có môn BADMINTON và tiện ích LIGHTING từ dữ liệu seed với ID/code/name/scope đúng. Provider cũng đọc được. |
| `shopReadUpdateAndChangeRequestFlow` | `GET/PATCH /api/provider/shop`, `POST /api/provider/shop/change-request`, `POST /api/provider/change-requests/{requestId}/cancel` | Sửa description trực tiếp; PATCH rỗng giữ dữ liệu. Đổi tên tạo pendingChange, chưa đổi tên thật; tên rỗng/không thay đổi 400, đơn pending thứ hai 409. Người khác hủy 404; chủ hủy 204, hủy lặp 409 và nộp lại được. |
| `venueCreateSubmitDeleteAndOwnership` | `GET/POST /api/provider/venues`, `GET/DELETE /api/provider/venues/{id}`, `POST /api/provider/venues/{id}/submit`, `PUT /api/provider/venues/{id}/amenities` | Tạo DRAFT/slug; kiểm tra dữ liệu và cặp tọa độ. Provider khác không thấy/truy cập tài nguyên, trả 404. Thiếu sân ACTIVE không submit được; có sân thì PENDING_REVIEW. Không submit lại hoặc xóa venue đang chờ duyệt; xóa DRAFT có sân trả 204, đọc lại 404. |
| `venueAmenitiesReplaceAllAndScope` | `PUT /api/provider/venues/{id}/amenities`, `GET /api/provider/venues/{id}` | Thay toàn bộ tiện ích, lặp lại được và dữ liệu đọc mới đúng. Sai scope, ID lạ, trùng ID hoặc phần tử null trả 400; scope BOTH hợp lệ, danh sách rỗng xóa hết. |
| `venuePatchAndChangeRequestByStatus` | `PATCH /api/provider/venues/{id}`, `POST /api/provider/venues/{id}/change-request`, `POST /api/provider/change-requests/{requestId}/cancel` | DRAFT/REJECTED sửa trường quan trọng trực tiếp; PENDING_REVIEW/ACTIVE bị giới hạn. PATCH trộn thay đổi bị cấm với trường tự do bị từ chối toàn bộ. Gửi lại giá trị quan trọng không đổi vẫn cho sửa trường tự do; tọa độ khác scale nhưng cùng giá trị không tính là thay đổi. ACTIVE/SUSPENDED tạo change request, giữ dữ liệu hiện hành; kiểm tra trùng đơn, hủy/nộp lại và quyền sở hữu. |
| `courtCreateGetPatchAndOwnership` | `POST /api/provider/venues/{venueId}/courts`, `GET/PATCH /api/provider/courts/{id}`, `GET /api/provider/venues/{venueId}` | Tạo sân ACTIVE, chuẩn hóa code, trả dữ liệu chi tiết. Code trùng cùng venue 409, khác venue hợp lệ. Kiểm tra capacity, step/min/max và status; PATCH kiểm tra lại dữ liệu kết hợp với giá trị cũ. Provider khác tạo/đọc/sửa trả 404. |
| `courtReplaceAllPutsReturnNewDataAndAreRepeatable` | `PUT /api/provider/courts/{id}/sports`, `/amenities`, `/operating-hours`, `/pricing-rules`; `GET /api/provider/courts/{id}` | Thay toàn bộ từng nhóm cấu hình, gọi lặp và đọc lại nhận dữ liệu mới, không ghi đè nhóm khác. Môn thể thao cần đúng một primary; tiện ích đúng scope; giờ hoạt động đủ 7 ngày riêng biệt, giờ hợp lệ; giá không chồng lấn, khoảng phút/giá hợp lệ. ID lạ/trùng/null và provider khác được kiểm tra. |
| `venueCourtHoursPricingSportsThenSubmitPendingReview` | `POST /api/provider/venues`, `POST /api/provider/venues/{venueId}/courts`, `PUT /api/provider/courts/{courtId}/operating-hours`, `/pricing-rules`, `/sports`, `POST /api/provider/venues/{venueId}/submit` | Chuỗi tạo venue → tạo sân → cấu hình giờ/giá/môn → submit thành PENDING_REVIEW. Submit khi chưa có sân trả 409. Không chứng minh giờ/giá/môn là điều kiện bắt buộc của submit. |
| `venueImagesUploadLimitAndOwnership` | `POST /api/provider/venues/{id}/images`, `GET /api/provider/venues/{id}` | Upload ảnh, URL/thư mục, ảnh đầu là cover, sortOrder/altText đúng; altText quá 255 ký tự 400, người khác upload 404, ảnh thứ 11 trả 409. |
| `venueImagesReorderAndDelete` | `POST/PUT /api/provider/venues/{id}/images`, `DELETE /api/provider/venues/{id}/images/{imageId}`, `GET /api/provider/venues/{id}` | Danh sách reorder phải chứa đúng toàn bộ ID; thiếu/trùng/ID lạ 400. Đổi thứ tự, altText và cover, gọi lặp được; người khác 404. Xóa cover thì ảnh kế tiếp làm cover và yêu cầu storage xóa object sau commit; xóa lặp 404, xóa hết được gallery rỗng. |
| `courtImagesUploadReorderDelete` | `POST/PUT /api/provider/courts/{id}/images`, `DELETE /api/provider/courts/{id}/images/{imageId}`, `GET /api/provider/courts/{id}` | Upload đúng thư mục sân, reorder và xóa ảnh, cover còn lại đúng; provider khác upload 404. |
| `imageUploadRolledBackDeletesObject` | `POST /api/provider/venues/{id}/images` | Mock storage trả key đã tồn tại để gây vi phạm unique/rollback 409; xác nhận lời gọi xóa object nhằm dọn upload khi rollback. |
| `oversizedUploadIsRejectedBeforeStorage` | `POST /api/provider/venues/{id}/images` | File vượt 5 MiB bị chặn trước storage và không tạo row ảnh. Test chấp nhận HTTP 413 **hoặc ResourceAccessException**; không khẳng định server luôn trả 413. |
| `shopLogoReplaceAndDelete` | `GET /api/provider/shop`, `PUT/DELETE /api/provider/shop/logo` | Ban đầu không có logo; upload/thay logo cập nhật URL và yêu cầu xóa object cũ. Xóa logo trả 204, URL thành null; xóa khi không có logo vẫn 204. |
| `deletingDraftVenueDropsVenueAndCourtImageObjects` | `DELETE /api/provider/venues/{id}`; API upload ảnh venue/court để chuẩn bị dữ liệu | Xóa venue DRAFT trả 204, yêu cầu storage xóa cả ảnh venue và sân; kiểm tra row ảnh sân tương ứng đã bị xóa. |

### [ProviderCatalogControllerSecurityTest.java](src/test/java/com/vesanrebackend/controller/provider/ProviderCatalogControllerSecurityTest.java) — 5 test

**Loại:** Web MVC/security; mock service và repository catalog. Bộ route được thử gồm GET shop/venues/venue/court; POST cancel change request; PUT court sports; POST venue images; DELETE court image/shop logo.

| Test method | API | Chức năng/tình huống được kiểm tra |
| --- | --- | --- |
| `unauthenticatedIsUnauthorized` | Bộ route nêu trên | Request không xác thực trả 401. |
| `customerAndAdminAreForbidden` | Bộ route nêu trên | CUSTOMER/ADMIN không có PROVIDER bị chặn 403. |
| `providerIsAllowed` | GET shop/venues/venue/court; POST cancel; PUT court sports | PROVIDER được phép: GET/PUT 200, cancel 204. Không thử nhánh thành công upload/xóa ảnh trong method này. |
| `invalidListElementIsBadRequest` | `PUT /api/provider/courts/{id}/sports`, `/operating-hours` | sportId null, phần tử null và weekday 9 bị validation chặn 400. |
| `catalogIsPublicForGetOnly` | `GET /api/catalog/sports`, `GET /api/catalog/amenities`, `POST /api/catalog/sports` | GET công khai 200; POST không xác thực trả 401. Không chứng minh có API tạo môn thể thao bằng POST. |

## 4. Admin: duyệt provider, venue và yêu cầu thay đổi

### [AdminProviderServiceTest.java](src/test/java/com/vesanrebackend/service/admin/AdminProviderServiceTest.java) — 8 test

**Loại:** Unit; mock repository, audit và mailer, dùng Clock cố định.

API liên quan: `POST /api/admin/providers/{userId}/approve` và `/reject`.

| Test method | Logic được kiểm tra |
| --- | --- |
| `approveVerifiesProfileApprovesVerificationActivatesShopAndAudits` | Duyệt làm profile VERIFIED, verification APPROVED, shop ACTIVE; reviewer/thời gian/response và audit trước/sau đúng. |
| `approveGrantsProviderRoleOnceAndRejectGrantsNothing` | Thực tế method này kiểm tra cấp role PROVIDER khi duyệt và không thêm role trùng khi đã có. Nhánh từ chối được kiểm tra ở method kế tiếp. |
| `rejectDoesNotGrantProviderRole` | Từ chối không gọi repository cấp role. |
| `rejectStoresReasonAndLeavesShopAndVerifiedAtUntouched` | Từ chối làm profile/verification REJECTED, lưu lý do/reviewer/thời gian; fixture shop vẫn DRAFT, verifiedAt vẫn null; ghi audit. |
| `approveAndRejectMailTheApplicant` | Duyệt/từ chối gọi mailer đúng người nhận/tiêu đề; từ chối truyền lý do. Không gửi email thật. |
| `notPendingIsConflict` | Fixture profile VERIFIED: approve/reject trả 409, không gọi audit/mailer. Không duyệt mọi trạng thái ngoài PENDING trong method này. |
| `unknownProviderIsNotFound` | Provider không có trả 404 ở approve/reject. |
| `missingPendingVerificationIsConflictAndChangesNothing` | Thiếu verification PENDING trả 409; profile vẫn PENDING, không ghi audit. |

### [AdminProviderControllerSecurityTest.java](src/test/java/com/vesanrebackend/controller/admin/AdminProviderControllerSecurityTest.java) — 4 test

**Loại:** Web MVC/security; mock AdminProviderService.

API: `GET /api/admin/providers`, `POST /api/admin/providers/{userId}/approve`, `POST /api/admin/providers/{userId}/reject`.

| Test method | Chức năng/tình huống được kiểm tra |
| --- | --- |
| `unauthenticatedIsUnauthorized` | GET danh sách và POST approve thiếu xác thực trả 401. |
| `customerAndProviderAreForbiddenOnEveryEndpoint` | CUSTOMER/PROVIDER bị chặn 403 ở cả ba endpoint. |
| `adminIsAllowed` | ADMIN gọi cả ba endpoint được 200 với service mock. |
| `adminInputIsValidated` | Status không hợp lệ, page âm, size 0, lý do từ chối trống hoặc quá 1000 ký tự trả 400. |

### [AdminReviewIntegrationTest.java](src/test/java/com/vesanrebackend/controller/admin/AdminReviewIntegrationTest.java) — 9 test

**Loại:** Integration qua AdminApiTestSupport; HTTP + DB thật, mail sender mock. Các test cạnh tranh dùng transaction JDBC giữ row lock và request bất đồng bộ. Một phía cạnh tranh được mô phỏng bằng SQL, không phải cả hai phía đều gọi API.

| Test method | API chính | Chức năng/tình huống được kiểm tra |
| --- | --- | --- |
| `venueFullCycleWithAuditAndMails` | `GET /api/admin/venues`, `GET /api/admin/venues/{id}`, `POST /api/admin/venues/{id}/reject`, `/approve`, `/suspend`, `/reactivate`; `POST /api/provider/venues/{id}/submit` | Đọc danh sách/chi tiết có sân; từ chối → provider nộp lại → duyệt → đình chỉ → khôi phục. Kiểm tra trạng thái, audit và 4 email/lý do. Chuyển trạng thái sai 409 hoặc ID lạ 404 không phát sinh lời gọi mail thêm. |
| `venueListIsPagedOldestFirst` | `GET /api/admin/venues` | items/totalElements/totalPages đúng, size giới hạn 100; venue được tạo trước xuất hiện trước. |
| `venueChangeRequestApproveAppliesExactCoordinates` | `GET /api/admin/change-requests?targetType=VENUE/SHOP`, `POST /api/admin/change-requests/{requestId}/approve`, `POST /api/provider/venues/{id}/change-request` | Lọc loại yêu cầu, current chứa đúng trường thay đổi, tên/người nộp đúng; duyệt cập nhật tên/tọa độ đủ 6 chữ số thập phân, giữ venue ACTIVE, lưu reviewer/audit/email. Duyệt lại 409, ID lạ 404; tạo đơn mới sau duyệt được. |
| `rejectKeepsDataStoresReasonAndMails` | `POST /api/admin/change-requests/{requestId}/reject` | Từ chối giữ tên venue cũ, lưu trạng thái/lý do và phát email chứa lý do. |
| `shopChangeRequestApproveKeepsSlug` | `POST /api/provider/shop/change-request`, `POST /api/admin/change-requests/{requestId}/approve` | Duyệt tên shop mới nhưng giữ slug hiện tại. |
| `approvingAChangeRequestDoesNotUndoAConcurrentSuspend` | `POST /api/admin/change-requests/{requestId}/approve` | Duyệt thay đổi venue khi SQL đình chỉ đồng thời: request phải chờ lock; việc duyệt không ghi đè trạng thái đình chỉ. |
| `providerEditDoesNotUndoAConcurrentSuspend` | `PATCH /api/provider/venues/{id}` | Sửa description đồng thời với SQL đình chỉ: chỉnh sửa không khôi phục status ACTIVE từ dữ liệu cũ. |
| `cancelDoesNotOverwriteAConcurrentApprove` | `POST /api/provider/change-requests/{requestId}/cancel` | SQL duyệt đơn đang giữ lock; cancel chờ rồi trả 409, không ghi đè đơn đã APPROVED thành CANCELLED. |
| `providerShopEditDoesNotUndoAConcurrentSuspend` | `PATCH /api/provider/shop` | SQL đình chỉ shop đồng thời với sửa description: cuối cùng shop vẫn SUSPENDED, description mới được lưu. |

### [AdminReviewControllerSecurityTest.java](src/test/java/com/vesanrebackend/controller/admin/AdminReviewControllerSecurityTest.java) — 4 test

**Loại:** Web MVC/security; mock AdminVenueService và AdminChangeRequestService.

| Test method | API | Chức năng/tình huống được kiểm tra |
| --- | --- | --- |
| `unauthenticatedIsUnauthorized` | `GET /api/admin/venues` | Không xác thực trả 401. |
| `customerAndProviderAreForbiddenOnEveryEndpoint` | `GET /api/admin/venues`, `GET /api/admin/venues/{id}`, `POST /api/admin/venues/{id}/approve`, `/reject`, `/suspend`, `/reactivate` | CUSTOMER/PROVIDER bị chặn 403 ở các API duyệt/quản lý trạng thái venue. |
| `changeRequestsAreAdminOnly` | `GET /api/admin/change-requests`, `POST /api/admin/change-requests/{requestId}/approve`, `/reject` | CUSTOMER/PROVIDER bị chặn 403; ADMIN gửi targetType không hợp lệ trả 400. Không có nhánh ADMIN thành công trong method này. |
| `adminInputIsValidated` | `GET /api/admin/venues`, `POST /api/admin/venues/{id}/reject` | page âm, size 0 và lý do toàn khoảng trắng trả 400. |

## 5. JWT, lỗi DB, lưu ảnh và gửi email

### [JwtServiceTest.java](src/test/java/com/vesanrebackend/security/JwtServiceTest.java) — 2 test

**Loại:** Unit. Liên quan token trả về từ `POST /api/auth/login` và cấu hình bảo mật dùng chung.

| Test method | Logic được kiểm tra |
| --- | --- |
| `refusesShortOrPlaceholderSecret` | Từ chối JWT secret ngắn hoặc còn placeholder bằng IllegalStateException. |
| `issuedTokenIsAcceptedByDecoderWithSubjectRolesAndTtl` | Token do encoder tạo được decoder chấp nhận; tokenType Bearer, subject UUID, issuer, roles và TTL 900 giây đúng. Không thử token hết hạn/sai chữ ký trong method này. |

### [GlobalExceptionHandlerTest.java](src/test/java/com/vesanrebackend/exception/GlobalExceptionHandlerTest.java) — 2 test

**Loại:** Unit; gọi handler trực tiếp với exception mô phỏng. Không gắn với một API riêng.

| Test method | Logic được kiểm tra |
| --- | --- |
| `uniqueAndExclusionViolationsAre409` | SQLSTATE `23505` (unique) và `23P01` (exclusion) được chuyển thành ProblemDetail 409. Không phải test nghiệp vụ đặt sân/chống trùng lịch. |
| `otherIntegrityViolationsAre500WithGenericDetail` | Vi phạm NOT NULL trả 500 với thông báo chung `Unexpected server error`; exception không có SQL cause cũng trả 500. |

### [ImageStorageTest.java](src/test/java/com/vesanrebackend/service/storage/ImageStorageTest.java) — 5 test

**Loại:** Unit; mock Cloudinary/Uploader. Liên quan API upload ảnh venue/court và logo shop, không gọi Cloudinary thật.

| Test method | Logic được kiểm tra |
| --- | --- |
| `validImagesUploadIntoFolderWithAllowedFormats` | Byte signature JPEG/PNG/WEBP được chấp nhận; public ID có folder + UUID và options format đúng. Dữ liệu test là byte mẫu, không chứng minh khả năng giải mã ảnh đầy đủ. |
| `fakeOrEmptyFilesAreRejectedBeforeNetwork` | File giả/không được hỗ trợ, rỗng, header thiếu hoặc null bị lỗi 400 trước lời gọi uploader. |
| `cloudinaryFailureIsBadGateway` | IOException hoặc RuntimeException khi upload chuyển thành lỗi 502. |
| `deleteNeverThrows` | Object không có hoặc IOException khi xóa không làm lời gọi delete ném lỗi; options xóa đúng. |
| `urlIsBuiltLocallyFromPublicId` | SDK dựng URL HTTPS từ public ID, null trả null; không truy cập URL để kiểm tra ảnh tồn tại. |

### [ReviewMailerTest.java](src/test/java/com/vesanrebackend/service/mail/ReviewMailerTest.java) — 2 test

**Loại:** Unit; mock ApplicationEventPublisher. Liên quan thông báo email khi admin duyệt/từ chối provider, venue và change request.

| Test method | Logic được kiểm tra |
| --- | --- |
| `nullOrBlankRecipientPublishesNothing` | Người nhận null/rỗng/toàn khoảng trắng không phát event. |
| `realRecipientPublishesEvent` | Người nhận có giá trị thì publish event ReviewMail. Không xác minh toàn bộ nội dung event trong method này. |

### [ReviewMailerIntegrationTest.java](src/test/java/com/vesanrebackend/service/mail/ReviewMailerIntegrationTest.java) — 2 test

**Loại:** Spring integration; TransactionTemplate thật, JavaMailSender mock; không gọi API HTTP hoặc SMTP thật.

| Test method | Logic được kiểm tra |
| --- | --- |
| `sendsAfterCommitWithReason` | Transaction commit dẫn đến gọi send bất đồng bộ; from/to/subject/body và lý do đúng. |
| `rolledBackTransactionSendsNothing` | Transaction rollback không gọi gửi mail trong khoảng quan sát của test. |

## 6. Khởi động ứng dụng và file hỗ trợ

### [VesanreBackendApplicationTests.java](src/test/java/com/vesanrebackend/VesanreBackendApplicationTests.java) — 1 test

`contextLoads`: dùng `@SpringBootTest` để kiểm tra Spring application context khởi động được với cấu hình/dependency hiện hành. Không kiểm tra một API hoặc nghiệp vụ cụ thể.

### [AdminApiTestSupport.java](src/test/java/com/vesanrebackend/AdminApiTestSupport.java) — 0 test

Lớp abstract hỗ trợ AdminReviewIntegrationTest: khởi động server cổng ngẫu nhiên; tạo customer/admin/provider và venue chờ duyệt; đăng nhập; gửi GET/POST/PATCH; đọc các trang danh sách; chờ email mock và chờ row lock trong PostgreSQL. JavaMailSender được mock. Role admin và trạng thái/role provider được chuẩn bị bằng JDBC. Đây là fixture/helper, không tính là lớp test độc lập.

## 7. Tra cứu nhanh theo chức năng

| Chức năng | Test nên đọc |
| --- | --- |
| Đăng ký/đăng nhập/hồ sơ | AuthFlowIntegrationTest, AuthServiceTest, ProfileControllerSecurityTest |
| Đăng ký provider và admin duyệt/từ chối | AuthFlowIntegrationTest, AuthServiceTest, AdminProviderServiceTest, AdminProviderControllerSecurityTest |
| Danh mục môn thể thao/tiện ích công khai | ProviderCatalogIntegrationTest, ProviderCatalogControllerSecurityTest |
| Shop, venue, court, cấu hình giờ/giá/môn/tiện ích | ProviderCatalogIntegrationTest, ProviderCatalogControllerSecurityTest |
| Ảnh venue/court và logo shop | ProviderCatalogIntegrationTest, ProviderCatalogControllerSecurityTest, ImageStorageTest |
| Admin duyệt venue/change request; audit/email; cạnh tranh cập nhật | AdminReviewIntegrationTest, AdminReviewControllerSecurityTest |
| JWT | JwtServiceTest; AuthFlowIntegrationTest sử dụng token thật qua HTTP |
| Chuyển lỗi ràng buộc DB thành HTTP error | GlobalExceptionHandlerTest; các integration test kiểm tra một số lỗi 409 thật |
| Event mail và ranh giới commit/rollback | ReviewMailerTest, ReviewMailerIntegrationTest |
| Spring context | VesanreBackendApplicationTests |

Trong thư mục test hiện tại chưa có test riêng cho API booking, payment/refund, review khách hàng, notification hoặc logout. Những luồng tạo tài khoản/provider xuất hiện trong helper chủ yếu phục vụ chuẩn bị dữ liệu, không tự tạo thêm test case độc lập. Note này không đánh giá độ bao phủ toàn bộ yêu cầu sản phẩm.

## 8. Điều kiện và cách chạy

Build dùng Java 17, Gradle Wrapper và JUnit Platform. Các lớp Spring integration/context sử dụng datasource của ứng dụng; không có `src/test/resources` hay profile test riêng trong cây test hiện tại. Cấu hình lấy các biến `SPORTHUB_DB_URL`, `SPORTHUB_DB_USERNAME`, `SPORTHUB_DB_PASSWORD`, `SPORTHUB_JWT_SECRET` từ môi trường hoặc `.env`. Dùng PostgreSQL dành riêng cho test: các API/JDBC trong test tạo và sửa dữ liệu thật; không có cơ chế rollback toàn bộ từng HTTP test được khai báo ở các lớp này.

Workflow [.github/workflows/ci.yml](.github/workflows/ci.yml) khởi tạo PostgreSQL và chạy `gradlew test`.

Chạy từ thư mục backend sau khi cấu hình môi trường test:

```powershell
# Toàn bộ test
.\gradlew.bat test

# Một lớp test
.\gradlew.bat test --tests 'com.vesanrebackend.controller.provider.ProviderCatalogIntegrationTest'

# Một method
.\gradlew.bat test --tests 'com.vesanrebackend.controller.auth.AuthFlowIntegrationTest.registerLoginProfileAndRoleChecks'
```

Kết quả HTML sau khi chạy: `build/reports/tests/test/index.html`. Việc có tên test trong note không xác nhận test đã chạy hoặc đang PASS.
