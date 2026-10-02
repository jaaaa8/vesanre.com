package com.vesanrebackend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Unique constraints lost to a concurrent request (email/phone) surface here instead of as 500.
    // Only unique_violation (23505) and exclusion_violation (23P01) are conflicts; NOT NULL/FK/CHECK are server bugs.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataIntegrity(DataIntegrityViolationException ex) {
        if (ex.getMostSpecificCause() instanceof SQLException sql
                && ("23505".equals(sql.getSQLState()) || "23P01".equals(sql.getSQLState()))) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Resource already exists");
        }
        log.error("Data integrity violation", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }
}
