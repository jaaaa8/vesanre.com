package com.vesanrebackend.dto.review;

import com.vesanrebackend.dto.auth.AuthPatterns;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateReviewRequest(
        @NotNull @Min(1) @Max(5) Integer rating, @Pattern(regexp = AuthPatterns.REVIEW_COMMENT) String comment) {
}
