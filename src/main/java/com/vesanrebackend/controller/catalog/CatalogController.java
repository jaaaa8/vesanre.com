package com.vesanrebackend.controller.catalog;

import com.vesanrebackend.dto.catalog.AmenityResponse;
import com.vesanrebackend.dto.catalog.SportResponse;
import com.vesanrebackend.repository.AmenityRepository;
import com.vesanrebackend.repository.SportRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final SportRepository sports;
    private final AmenityRepository amenities;

    public CatalogController(SportRepository sports, AmenityRepository amenities) {
        this.sports = sports;
        this.amenities = amenities;
    }

    @GetMapping("/sports")
    public List<SportResponse> sports() {
        return sports.findByIsActiveTrueOrderByName().stream()
                .map(s -> new SportResponse(s.getId(), s.getCode(), s.getName())).toList();
    }

    @GetMapping("/amenities")
    public List<AmenityResponse> amenities() {
        return amenities.findByIsActiveTrueOrderByName().stream()
                .map(a -> new AmenityResponse(a.getId(), a.getCode(), a.getName(), a.getAllowedScope())).toList();
    }
}
