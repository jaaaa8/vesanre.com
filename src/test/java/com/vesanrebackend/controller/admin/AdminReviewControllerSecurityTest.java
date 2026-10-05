package com.vesanrebackend.controller.admin;

import com.vesanrebackend.security.CorsConfig;
import com.vesanrebackend.security.SecurityConfig;
import com.vesanrebackend.service.admin.AdminChangeRequestService;
import com.vesanrebackend.service.admin.AdminVenueService;
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
 * Loại test: @WebMvcTest + MockMvc (AdminVenueService, AdminChangeRequestService được mock) - kiểm tra phân quyền và validation.
 * API: /api/admin/venues (list, detail, approve, reject, suspend, reactivate) và /api/admin/change-requests (list, approve, reject).
 */
@WebMvcTest(value = {AdminVenueController.class, AdminChangeRequestController.class}, properties = {
        "app.security.jwt.secret=test-secret-at-least-thirty-two-bytes-long",
        "app.cors.allowed-origin=http://localhost:5173"})
@Import({SecurityConfig.class, CorsConfig.class})
class AdminReviewControllerSecurityTest {
    private static final String PATH = "/api/admin/venues";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminVenueService adminVenues;

    @MockitoBean
    private AdminChangeRequestService adminChangeRequests;

    // API: GET /api/admin/venues
    // Kiểm tra: Không có token thì trả 401.
    @Test
    void unauthenticatedIsUnauthorized() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    // API: GET /api/admin/venues, GET .../{id}, POST .../{id}/approve|reactivate|reject|suspend
    // Kiểm tra: CUSTOMER và PROVIDER bị chặn 403 ở mọi endpoint quản lý venue.
    @Test
    void customerAndProviderAreForbiddenOnEveryEndpoint() throws Exception {
        for (String role : new String[]{"CUSTOMER", "PROVIDER"}) {
            RequestPostProcessor caller = as(role);
            String id = PATH + "/" + UUID.randomUUID();
            mvc.perform(get(PATH).with(caller)).andExpect(status().isForbidden());
            mvc.perform(get(id).with(caller)).andExpect(status().isForbidden());
            mvc.perform(post(id + "/approve").with(caller)).andExpect(status().isForbidden());
            mvc.perform(post(id + "/reactivate").with(caller)).andExpect(status().isForbidden());
            mvc.perform(post(id + "/reject").with(caller)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}")).andExpect(status().isForbidden());
            mvc.perform(post(id + "/suspend").with(caller)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}")).andExpect(status().isForbidden());
        }
    }

    // API: GET /api/admin/change-requests, POST .../{requestId}/approve, POST .../{requestId}/reject
    // Kiểm tra: CUSTOMER/PROVIDER bị chặn 403; ADMIN gửi targetType=NOPE trả 400.
    @Test
    void changeRequestsAreAdminOnly() throws Exception {
        String path = "/api/admin/change-requests";
        for (String role : new String[]{"CUSTOMER", "PROVIDER"}) {
            RequestPostProcessor caller = as(role);
            String id = path + "/" + UUID.randomUUID();
            mvc.perform(get(path).with(caller)).andExpect(status().isForbidden());
            mvc.perform(post(id + "/approve").with(caller)).andExpect(status().isForbidden());
            mvc.perform(post(id + "/reject").with(caller)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}")).andExpect(status().isForbidden());
        }
        mvc.perform(get(path + "?targetType=NOPE").with(as("ADMIN"))).andExpect(status().isBadRequest());
    }

    // API: GET /api/admin/venues?page|size, POST /api/admin/venues/{id}/reject
    // Kiểm tra: page=-1, size=0 và lý do toàn khoảng trắng đều trả 400.
    @Test
    void adminInputIsValidated() throws Exception {
        RequestPostProcessor admin = as("ADMIN");
        String id = PATH + "/" + UUID.randomUUID();
        mvc.perform(get(PATH + "?page=-1").with(admin)).andExpect(status().isBadRequest());
        mvc.perform(get(PATH + "?size=0").with(admin)).andExpect(status().isBadRequest());
        mvc.perform(post(id + "/reject").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"  \"}")).andExpect(status().isBadRequest());
    }

    private RequestPostProcessor as(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
                .authorities(() -> "ROLE_" + role);
    }
}
