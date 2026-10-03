package com.vesanrebackend.controller.admin;

import com.vesanrebackend.security.CorsConfig;
import com.vesanrebackend.security.SecurityConfig;
import com.vesanrebackend.service.admin.AdminProviderService;
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

@WebMvcTest(value = AdminProviderController.class, properties = {
        "app.security.jwt.secret=test-secret-at-least-thirty-two-bytes-long",
        "app.cors.allowed-origin=http://localhost:5173"})
@Import({SecurityConfig.class, CorsConfig.class})
class AdminProviderControllerSecurityTest {
    private static final String PATH = "/api/admin/providers";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AdminProviderService adminProviders;

    @Test
    void unauthenticatedIsUnauthorized() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(post(PATH + "/" + UUID.randomUUID() + "/approve")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerAndProviderAreForbiddenOnEveryEndpoint() throws Exception {
        for (String role : new String[]{"CUSTOMER", "PROVIDER"}) {
            RequestPostProcessor caller = as(role);
            String id = UUID.randomUUID().toString();
            mvc.perform(get(PATH).with(caller)).andExpect(status().isForbidden());
            mvc.perform(post(PATH + "/" + id + "/approve").with(caller)).andExpect(status().isForbidden());
            mvc.perform(post(PATH + "/" + id + "/reject").with(caller)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}")).andExpect(status().isForbidden());
        }
    }

    @Test
    void adminIsAllowed() throws Exception {
        RequestPostProcessor admin = as("ADMIN");
        String id = UUID.randomUUID().toString();
        mvc.perform(get(PATH).with(admin)).andExpect(status().isOk());
        mvc.perform(post(PATH + "/" + id + "/approve").with(admin)).andExpect(status().isOk());
        mvc.perform(post(PATH + "/" + id + "/reject").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"bad docs\"}")).andExpect(status().isOk());
    }

    @Test
    void adminInputIsValidated() throws Exception {
        RequestPostProcessor admin = as("ADMIN");
        String id = UUID.randomUUID().toString();
        mvc.perform(get(PATH + "?status=NOPE").with(admin)).andExpect(status().isBadRequest());
        mvc.perform(post(PATH + "/" + id + "/reject").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"  \"}")).andExpect(status().isBadRequest());
        mvc.perform(post(PATH + "/" + id + "/reject").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + "a".repeat(1001) + "\"}")).andExpect(status().isBadRequest());
    }

    private RequestPostProcessor as(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
                .authorities(() -> "ROLE_" + role);
    }
}
