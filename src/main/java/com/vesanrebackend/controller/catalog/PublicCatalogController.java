package com.vesanrebackend.controller.catalog;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.catalog.VenueAvailabilityResponse;
import com.vesanrebackend.dto.catalog.LocationResponse;
import com.vesanrebackend.dto.catalog.PublicVenueDetailResponse;
import com.vesanrebackend.dto.catalog.VenueCardResponse;
import com.vesanrebackend.dto.review.ReviewView;
import com.vesanrebackend.service.catalog.PublicCatalogService;
import com.vesanrebackend.service.catalog.PublicVenueSearchService;
import com.vesanrebackend.service.review.ReviewService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog")
public class PublicCatalogController {
    private final PublicVenueSearchService search;
    private final PublicCatalogService publicCatalog;
    private final ReviewService reviews;

    public PublicCatalogController(PublicVenueSearchService search, PublicCatalogService publicCatalog,
                                   ReviewService reviews) {
        this.search = search;
        this.publicCatalog = publicCatalog;
        this.reviews = reviews;
    }

    @GetMapping("/venues")
    public PageResponse<VenueCardResponse> venues(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) String city,
                                                  @RequestParam(required = false) String district,
                                                  @RequestParam(required = false) String sport,
                                                  @RequestParam(required = false) BigDecimal minPrice,
                                                  @RequestParam(required = false) BigDecimal maxPrice,
                                                  @RequestParam(defaultValue = "newest") String sort,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return search.search(q, city, district, sport, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/locations")
    public List<LocationResponse> locations() {
        return search.locations();
    }

    @GetMapping("/venues/{id}")
    public PublicVenueDetailResponse venue(@PathVariable UUID id) {
        return publicCatalog.detail(id);
    }

    @GetMapping("/venues/{id}/reviews")
    public PageResponse<ReviewView> reviews(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return reviews.publicList(id, page, size);
    }

    @GetMapping("/venues/{id}/availability")
    public VenueAvailabilityResponse availability(@PathVariable UUID id,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return publicCatalog.venueAvailability(id, date);
    }
}
