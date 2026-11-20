package com.vesanrebackend.controller.booking;

import com.vesanrebackend.controller.provider.ProviderBookingController;
import com.vesanrebackend.security.CorsConfig;
import com.vesanrebackend.security.SecurityConfig;
import com.vesanrebackend.service.booking.BookingService;
import com.vesanrebackend.service.booking.ProviderBookingService;
import com.vesanrebackend.service.review.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Loại test: @WebMvcTest + MockMvc (service mock) - kiểm tra phân quyền.
 * API: POST /api/bookings, GET /api/provider/bookings.
 */
@WebMvcTest(value = {BookingController.class, ProviderBookingController.class}, properties = {
        "app.security.jwt.secret=test-secret-at-least-thirty-two-bytes-long",
        "app.cors.allowed-origin=http://localhost:5173"})
@Import({SecurityConfig.class, CorsConfig.class})
class BookingControllerSecurityTest {
    // Valid body: validation runs before @PreAuthorize, so an empty body would give 400 instead of 403.
    private static final String VALID_BODY = "{\"courtId\":\"" + UUID.randomUUID() + "\",\"date\":\"2030-01-01\",\"startTime\":\"18:00\",\"endTime\":\"19:00\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BookingService bookings;
    @MockitoBean
    private ProviderBookingService providerBookings;
    @MockitoBean
    private ReviewService reviews;

    // Kiểm tra: không token -> 401.
    @Test
    void unauthenticatedIsUnauthorized() throws Exception {
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/provider/bookings")).andExpect(status().isUnauthorized());
    }

    // Kiểm tra: CUSTOMER không gọi được API provider -> 403.
    @Test
    void customerCannotUseProviderApi() throws Exception {
        mvc.perform(get("/api/provider/bookings").with(as("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/provider/wallet").with(as("CUSTOMER"))).andExpect(status().isForbidden());
    }

    // Kiểm tra: PROVIDER không đặt sân được -> 403.
    @Test
    void providerCannotBook() throws Exception {
        mvc.perform(post("/api/bookings").with(as("PROVIDER")).contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BODY)).andExpect(status().isForbidden());
    }

    // Kiểm tra: PROVIDER (không có CUSTOMER) không đánh giá được -> 403.
    @Test
    void providerCannotReview() throws Exception {
        mvc.perform(post("/api/bookings/" + UUID.randomUUID() + "/review").with(as("PROVIDER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5}")).andExpect(status().isForbidden());
    }

    private RequestPostProcessor as(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
                .authorities(() -> "ROLE_" + role);
    }
}
