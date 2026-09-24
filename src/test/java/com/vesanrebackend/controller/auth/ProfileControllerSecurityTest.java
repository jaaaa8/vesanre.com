package com.vesanrebackend.controller.auth;

import com.vesanrebackend.dto.auth.UserProfileResponse;
import com.vesanrebackend.security.SecurityConfig;
import com.vesanrebackend.security.CorsConfig;
import com.vesanrebackend.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(value = ProfileController.class, properties = {
        "app.security.jwt.secret=test-secret-at-least-thirty-two-bytes-long",
        "app.cors.allowed-origin=http://localhost:5173"})
@Import({SecurityConfig.class, CorsConfig.class})
class ProfileControllerSecurityTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void profileRequiresBearerToken() throws Exception {
        mvc.perform(get("/api/profile/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotReadProviderProfile() throws Exception {
        mvc.perform(get("/api/profile/provider").with(SecurityMockMvcRequestPostProcessors.user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void providerCanReadProviderProfile() throws Exception {
        UUID id = UUID.randomUUID();
        when(authService.profile(id)).thenReturn(new UserProfileResponse(id, "provider@example.com", "Provider", null,
                Set.of("PROVIDER"), "PENDING"));

        mvc.perform(get("/api/profile/provider").with(SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> jwt.subject(id.toString()))
                        .authorities(() -> "ROLE_PROVIDER")))
                .andExpect(status().isOk());
    }

    @Test
    void providerCannotReadAdminProfile() throws Exception {
        mvc.perform(get("/api/profile/admin").with(SecurityMockMvcRequestPostProcessors.user("provider").roles("PROVIDER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void vitePreflightIsAllowed() throws Exception {
        mvc.perform(options("/api/profile/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
