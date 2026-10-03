package com.vesanrebackend.dto.provider;

import java.util.UUID;

public record ImageResponse(UUID id, String url, String altText, int sortOrder, boolean cover) {
}
