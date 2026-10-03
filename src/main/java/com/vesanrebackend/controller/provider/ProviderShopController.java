package com.vesanrebackend.controller.provider;

import com.vesanrebackend.dto.provider.ShopChangeRequest;
import com.vesanrebackend.dto.provider.ShopResponse;
import com.vesanrebackend.dto.provider.UpdateShopRequest;
import com.vesanrebackend.service.provider.ProviderChangeRequestService;
import com.vesanrebackend.service.provider.ProviderShopService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/provider")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderShopController {
    private final ProviderShopService shops;
    private final ProviderChangeRequestService changeRequests;

    public ProviderShopController(ProviderShopService shops, ProviderChangeRequestService changeRequests) {
        this.shops = shops;
        this.changeRequests = changeRequests;
    }

    @PutMapping(path = "/shop/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ShopResponse replaceLogo(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
        return shops.replaceLogo(UUID.fromString(jwt.getSubject()), file);
    }

    @DeleteMapping("/shop/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLogo(@AuthenticationPrincipal Jwt jwt) {
        shops.deleteLogo(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/shop")
    public ShopResponse get(@AuthenticationPrincipal Jwt jwt) {
        return shops.get(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping("/shop")
    public ShopResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateShopRequest request) {
        return shops.update(UUID.fromString(jwt.getSubject()), request);
    }

    @PostMapping("/shop/change-request")
    @ResponseStatus(HttpStatus.CREATED)
    public ShopResponse submitChange(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ShopChangeRequest request) {
        return shops.submitChange(UUID.fromString(jwt.getSubject()), request);
    }

    @PostMapping("/change-requests/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        changeRequests.cancel(UUID.fromString(jwt.getSubject()), id);
    }
}
