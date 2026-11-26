package com.vesanrebackend.dto.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import com.vesanrebackend.dto.review.CreateReviewRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPatternsTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean valid(String email, String phone) {
        return validator.validate(new RegisterRequest(email, "correct-password1", "Tester", phone)).isEmpty();
    }

    // Kiểm tra: email phải có dấu chấm ở domain; phone theo đầu số di động VN, rỗng/null hợp lệ.
    @Test
    void emailAndPhoneFollowSharedPatterns() {
        assertThat(valid("a@b.vn", null)).isTrue();
        assertThat(valid("a@b", null)).isFalse();
        assertThat(valid("a b@c.vn", null)).isFalse();

        assertThat(valid("a@b.vn", "0901234567")).isTrue();
        assertThat(valid("a@b.vn", "+84381234567")).isTrue();
        assertThat(valid("a@b.vn", "")).isTrue();
        assertThat(valid("a@b.vn", "0201234567")).isFalse();
        assertThat(valid("a@b.vn", "090123456")).isFalse();
        assertThat(valid("a@b.vn", "090 123 4567")).isFalse();
        assertThat(valid("a@b.vn", "abc!!")).isFalse();

        assertThat(validator.validate(new UpdateProfileRequest(null, "0123")).isEmpty()).isFalse();
        assertThat(validator.validate(new UpdateProfileRequest(null, "")).isEmpty()).isTrue();
    }

    private boolean ok(Object request) {
        return validator.validate(request).isEmpty();
    }

    // Kiểm tra: tên, mật khẩu, đơn chủ sân, bình luận theo cùng regex với FE.
    @Test
    void newFieldsFollowSharedPatterns() {
        assertThat(ok(new RegisterRequest("a@b.vn", "Demo@123", "Tester", null))).isTrue();
        assertThat(ok(new RegisterRequest("a@b.vn", "correct-password", "Tester", null))).isFalse();
        assertThat(ok(new RegisterRequest("a@b.vn", "Demo@123", "Tester1", null))).isFalse();
        assertThat(ok(new RegisterRequest("a@b.vn", "Demo@123", "Nguyễn An", null))).isTrue();

        assertThat(ok(new UpdateProfileRequest("An1", null))).isFalse();
        assertThat(ok(new UpdateProfileRequest(null, null))).isTrue();

        assertThat(ok(new ProviderApplicationRequest("Công ty TNHH Goal Time", "", "", null))).isTrue();
        assertThat(ok(new ProviderApplicationRequest("Công ty", "04012", null, null))).isFalse();
        assertThat(ok(new ProviderApplicationRequest("A", null, null, null))).isFalse();

        assertThat(ok(new CreateReviewRequest(5, null))).isTrue();
        assertThat(ok(new CreateReviewRequest(5, "Sân đẹp, nhân viên thân thiện"))).isTrue();
        assertThat(ok(new CreateReviewRequest(5, "aaaaaaaaaaaa"))).isFalse();
        assertThat(ok(new CreateReviewRequest(5, "ngắn"))).isFalse();
    }
}
