package com.vesanrebackend.dto.catalog;

import java.util.List;

public record LocationResponse(String city, List<String> districts) {
}
