package com.vesanrebackend.dto.provider;

import jakarta.validation.constraints.Size;

// null = not sent (unchanged); blank clears the description.
public record UpdateShopRequest(@Size(max = 5000) String description) {
}
