package com.vesanrebackend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void uniqueAndExclusionViolationsAre409() {
        for (String state : new String[]{"23505", "23P01"}) {
            ProblemDetail problem = handler.dataIntegrity(
                    new DataIntegrityViolationException("boom", new SQLException("violation", state)));
            assertThat(problem.getStatus()).isEqualTo(409);
        }
    }

    @Test
    void otherIntegrityViolationsAre500WithGenericDetail() {
        ProblemDetail notNull = handler.dataIntegrity(
                new DataIntegrityViolationException("boom", new SQLException("null value in column \"x\"", "23502")));
        assertThat(notNull.getStatus()).isEqualTo(500);
        assertThat(notNull.getDetail()).isEqualTo("Unexpected server error");

        assertThat(handler.dataIntegrity(new DataIntegrityViolationException("no sql cause")).getStatus()).isEqualTo(500);
    }
}
