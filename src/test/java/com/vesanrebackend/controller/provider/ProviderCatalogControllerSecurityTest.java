package com.vesanrebackend.controller.provider;

import com.vesanrebackend.controller.catalog.CatalogController;
import com.vesanrebackend.repository.AmenityRepository;
import com.vesanrebackend.repository.SportRepository;
import com.vesanrebackend.security.CorsConfig;
import com.vesanrebackend.security.SecurityConfig;
import com.vesanrebackend.service.provider.ProviderChangeRequestService;
import com.vesanrebackend.service.provider.ProviderCourtService;
import com.vesanrebackend.service.provider.ProviderShopService;
import com.vesanrebackend.service.provider.ProviderVenueService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = {ProviderShopController.class, ProviderVenueController.class, ProviderCourtController.class,
        CatalogController.class}, properties = {
        "app.security.jwt.secret=test-secret-at-least-thirty-two-bytes-long",
        "app.cors.allowed-origin=http://localhost:5173"})
@Import({SecurityConfig.class, CorsConfig.class})
class ProviderCatalogControllerSecurityTest {
    private static final String ID = UUID.randomUUID().toString();
    private static final String[] GETS = {"/api/provider/shop", "/api/provider/venues",
            "/api/provider/venues/" + ID, "/api/provider/courts/" + ID};

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProviderShopService shops;
    @MockitoBean
    private ProviderChangeRequestService changeRequests;
    @MockitoBean
    private ProviderVenueService venues;
    @MockitoBean
    private ProviderCourtService courts;
    @MockitoBean
    private SportRepository sports;
    @MockitoBean
    private AmenityRepository amenities;

    @Test
    void unauthenticatedIsUnauthorized() throws Exception {
        for (String path : GETS) mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/provider/change-requests/" + ID + "/cancel")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/provider/courts/" + ID + "/sports").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerAndAdminAreForbidden() throws Exception {
        for (String role : new String[]{"CUSTOMER", "ADMIN"}) {
            RequestPostProcessor caller = as(role);
            for (String path : GETS) mvc.perform(get(path).with(caller)).andExpect(status().isForbidden());
            mvc.perform(post("/api/provider/change-requests/" + ID + "/cancel").with(caller)).andExpect(status().isForbidden());
            mvc.perform(put("/api/provider/courts/" + ID + "/sports").with(caller)
                    .contentType(MediaType.APPLICATION_JSON).content("[]")).andExpect(status().isForbidden());
        }
    }

    @Test
    void providerIsAllowed() throws Exception {
        RequestPostProcessor provider = as("PROVIDER");
        for (String path : GETS) mvc.perform(get(path).with(provider)).andExpect(status().isOk());
        mvc.perform(post("/api/provider/change-requests/" + ID + "/cancel").with(provider)).andExpect(status().isNoContent());
        mvc.perform(put("/api/provider/courts/" + ID + "/sports").with(provider)
                .contentType(MediaType.APPLICATION_JSON).content("[]")).andExpect(status().isOk());
    }

    @Test
    void invalidListElementIsBadRequest() throws Exception {
        RequestPostProcessor provider = as("PROVIDER");
        mvc.perform(put("/api/provider/courts/" + ID + "/sports").with(provider)
                .contentType(MediaType.APPLICATION_JSON).content("[{\"sportId\":null,\"primary\":true}]"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/provider/courts/" + ID + "/sports").with(provider)
                .contentType(MediaType.APPLICATION_JSON).content("[null]")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/provider/courts/" + ID + "/operating-hours").with(provider)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"weekday\":9,\"opensAt\":\"08:00\",\"closesAt\":\"22:00\"}]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void catalogIsPublicForGetOnly() throws Exception {
        mvc.perform(get("/api/catalog/sports")).andExpect(status().isOk());
        mvc.perform(get("/api/catalog/amenities")).andExpect(status().isOk());
        mvc.perform(post("/api/catalog/sports")).andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor as(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))
                .authorities(() -> "ROLE_" + role);
    }
}
