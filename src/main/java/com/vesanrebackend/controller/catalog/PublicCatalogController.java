package com.vesanrebackend.controller.catalog;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.catalog.LocationResponse;
import com.vesanrebackend.dto.catalog.VenueCardResponse;
import com.vesanrebackend.service.catalog.PublicVenueSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class PublicCatalogController {
    private final PublicVenueSearchService search;

    public PublicCatalogController(PublicVenueSearchService search) {
        this.search = search;
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
}
