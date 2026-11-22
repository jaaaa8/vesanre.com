package com.vesanrebackend.dto.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthPatternsTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean valid(String email, String phone) {
        return validator.validate(new RegisterRequest(email, "correct-password", "Tester", phone)).isEmpty();
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
}
