package com.vesanrebackend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loại test: unit - gọi trực tiếp GlobalExceptionHandler với exception mô phỏng (không HTTP, không DB).
 * Thành phần: GlobalExceptionHandler.dataIntegrity (chuyển lỗi ràng buộc DB thành ProblemDetail cho mọi API).
 */
class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // Thành phần: GlobalExceptionHandler.dataIntegrity
    // Kiểm tra: SQLSTATE 23505 (unique) và 23P01 (exclusion) được chuyển thành 409.
    @Test
    void uniqueAndExclusionViolationsAre409() {
        for (String state : new String[]{"23505", "23P01"}) {
            ProblemDetail problem = handler.dataIntegrity(
                    new DataIntegrityViolationException("boom", new SQLException("violation", state)));
            assertThat(problem.getStatus()).isEqualTo(409);
        }
    }

    // Thành phần: GlobalExceptionHandler.dataIntegrity
    // Kiểm tra: Vi phạm khác (NOT NULL 23502) hoặc không có SQL cause trả 500, detail chung "Unexpected server error".
    @Test
    void otherIntegrityViolationsAre500WithGenericDetail() {
        ProblemDetail notNull = handler.dataIntegrity(
                new DataIntegrityViolationException("boom", new SQLException("null value in column \"x\"", "23502")));
        assertThat(notNull.getStatus()).isEqualTo(500);
        assertThat(notNull.getDetail()).isEqualTo("Unexpected server error");

        assertThat(handler.dataIntegrity(new DataIntegrityViolationException("no sql cause")).getStatus()).isEqualTo(500);
    }
}
