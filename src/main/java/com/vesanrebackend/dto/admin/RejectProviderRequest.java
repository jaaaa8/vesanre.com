package com.vesanrebackend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectProviderRequest(@NotBlank @Size(max = 1000) String reason) {
}
