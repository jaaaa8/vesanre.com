package com.vesanrebackend.dto.provider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShopChangeRequest(@NotBlank @Size(max = 160) String name) {
}
