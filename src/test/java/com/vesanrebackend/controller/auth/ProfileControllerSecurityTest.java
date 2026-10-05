package com.vesanrebackend.controller.auth;

import com.vesanrebackend.dto.auth.UserProfileResponse;
import com.vesanrebackend.security.SecurityConfig;
import com.vesanrebackend.security.CorsConfig;
import com.vesanrebackend.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Loại test: @WebMvcTest + MockMvc (AuthService được mock) - kiểm tra xác thực, phân quyền và CORS.
 * API: /api/profile/me, /api/profile/provider, /api/profile/admin, /api/profile/provider-application.
 */
@WebMvcTest(value = ProfileController.class, properties = {
        "app.security.jwt.secret=test-secret-at-least-thirty-two-bytes-long",
        "app.cors.allowed-origin=http://localhost:5173"})
@Import({SecurityConfig.class, CorsConfig.class})
class ProfileControllerSecurityTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    // API: GET /api/profile/me
    // Kiểm tra: Không có token thì trả 401.
    @Test
    void profileRequiresBearerToken() throws Exception {
        mvc.perform(get("/api/profile/me"))
                .andExpect(status().isUnauthorized());
    }

    // API: GET /api/profile/provider
    // Kiểm tra: CUSTOMER bị chặn 403.
    @Test
    void customerCannotReadProviderProfile() throws Exception {
        mvc.perform(get("/api/profile/provider").with(SecurityMockMvcRequestPostProcessors.user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    // API: GET /api/profile/provider
    // Kiểm tra: JWT có ROLE_PROVIDER được phép, trả 200.
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

    // API: POST /api/profile/provider-application
    // Kiểm tra: Không token 401; JWT CUSTOMER (chưa cần role PROVIDER) nộp đơn được, trả 201.
    @Test
    void providerApplicationRequiresTokenButNoRole() throws Exception {
        UUID id = UUID.randomUUID();
        String json = "{\"legalName\":\"Legal Co\"}";
        when(authService.applyProvider(eq(id), any())).thenReturn(new UserProfileResponse(id, "member@example.com", "Member", null,
                Set.of("CUSTOMER"), "PENDING"));

        mvc.perform(post("/api/profile/provider-application").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/profile/provider-application").contentType(MediaType.APPLICATION_JSON).content(json)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .jwt(jwt -> jwt.subject(id.toString()))
                                .authorities(() -> "ROLE_CUSTOMER")))
                .andExpect(status().isCreated());
    }

    // API: GET /api/profile/admin
    // Kiểm tra: PROVIDER bị chặn 403.
    @Test
    void providerCannotReadAdminProfile() throws Exception {
        mvc.perform(get("/api/profile/admin").with(SecurityMockMvcRequestPostProcessors.user("provider").roles("PROVIDER")))
                .andExpect(status().isForbidden());
    }

    // API: OPTIONS /api/profile/me (CORS preflight)
    // Kiểm tra: Origin http://localhost:5173 với method PUT/DELETE được phép, trả 200.
    @Test
    void preflightAllowsPutAndDelete() throws Exception {
        for (String method : new String[]{"PUT", "DELETE"}) {
            mvc.perform(options("/api/profile/me")
                            .header("Origin", "http://localhost:5173")
                            .header("Access-Control-Request-Method", method))
                    .andExpect(status().isOk());
        }
    }

    // API: OPTIONS /api/profile/me (CORS preflight)
    // Kiểm tra: Preflight GET từ Vite trả 200 và header Access-Control-Allow-Origin đúng origin.
    @Test
    void vitePreflightIsAllowed() throws Exception {
        mvc.perform(options("/api/profile/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
