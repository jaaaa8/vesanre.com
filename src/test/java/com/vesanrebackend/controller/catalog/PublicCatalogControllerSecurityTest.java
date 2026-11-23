package com.vesanrebackend.controller.catalog;

import com.vesanrebackend.security.CorsConfig;
import com.vesanrebackend.security.SecurityConfig;
import com.vesanrebackend.service.catalog.PublicCatalogService;
import com.vesanrebackend.service.catalog.PublicVenueSearchService;
import com.vesanrebackend.service.review.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Loại test: @WebMvcTest + MockMvc (service mock) - kiểm tra các API public không cần đăng nhập.
 * API: GET /api/catalog/venues|locations (Task sau thêm venues/{id}, courts/{id}/availability).
 */
@WebMvcTest(value = PublicCatalogController.class, properties = {
        "app.security.jwt.secret=test-secret-at-least-thirty-two-bytes-long",
        "app.cors.allowed-origin=http://localhost:5173"})
@Import({SecurityConfig.class, CorsConfig.class})
class PublicCatalogControllerSecurityTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PublicVenueSearchService search;
    @MockitoBean
    private PublicCatalogService publicCatalog;
    @MockitoBean
    private ReviewService reviews;

    // API: GET /api/catalog/venues, GET /api/catalog/locations
    // Kiểm tra: không token vẫn trả 200.
    @Test
    void anonymousCanSearchAndListLocations() throws Exception {
        mvc.perform(get("/api/catalog/venues")).andExpect(status().isOk());
        mvc.perform(get("/api/catalog/locations")).andExpect(status().isOk());
    }

    // API: GET /api/catalog/venues/{id}
    // Kiểm tra: không token vẫn trả 200.
    @Test
    void anonymousCanReadVenueDetail() throws Exception {
        mvc.perform(get("/api/catalog/venues/" + UUID.randomUUID())).andExpect(status().isOk());
    }

    // API: GET /api/catalog/venues/{id}/availability?date=
    // Kiểm tra: không token vẫn trả 200.
    @Test
    void anonymousCanReadAvailability() throws Exception {
        mvc.perform(get("/api/catalog/venues/" + UUID.randomUUID() + "/availability").param("date", "2026-12-07"))
                .andExpect(status().isOk());
    }
}
