package com.vesanrebackend.service.provider;

import com.vesanrebackend.dto.provider.ShopChangeRequest;
import com.vesanrebackend.dto.provider.ShopResponse;
import com.vesanrebackend.dto.provider.UpdateShopRequest;
import com.vesanrebackend.entity.enums.ChangeRequestTargetType;
import com.vesanrebackend.entity.shop.Shop;
import com.vesanrebackend.repository.ShopRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
public class ProviderShopService {
    private final ShopRepository shops;
    private final ProviderChangeRequestService changeRequests;

    public ProviderShopService(ShopRepository shops, ProviderChangeRequestService changeRequests) {
        this.shops = shops;
        this.changeRequests = changeRequests;
    }

    // Shop of the caller; 404 when absent. Venue/court services reuse it for ownership checks.
    public Shop shopOf(UUID userId) {
        return shops.findByOwnerUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop not found"));
    }

    @Transactional(readOnly = true)
    public ShopResponse get(UUID userId) {
        return toResponse(shopOf(userId));
    }

    @Transactional
    public ShopResponse update(UUID userId, UpdateShopRequest request) {
        Shop shop = shopOf(userId);
        String description = request.description();
        if (description != null) {
            shop.setDescription(description.isBlank() ? null : description.trim());
        }
        return toResponse(shop);
    }

    @Transactional
    public ShopResponse submitChange(UUID userId, ShopChangeRequest request) {
        Shop shop = shopOf(userId);
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be blank");
        }
        // Unchanged fields are dropped; an empty proposal is rejected by the change request service.
        Map<String, Object> proposed = name.equals(shop.getName()) ? Map.of() : Map.of("name", name);
        changeRequests.submit(userId, ChangeRequestTargetType.SHOP, shop.getId(), proposed);
        return toResponse(shop);
    }

    private ShopResponse toResponse(Shop shop) {
        return new ShopResponse(shop.getId(), shop.getSlug(), shop.getName(), shop.getDescription(),
                shop.getStatus().name(), changeRequests.pending(ChangeRequestTargetType.SHOP, shop.getId()));
    }
}
