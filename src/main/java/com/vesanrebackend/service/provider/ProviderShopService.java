package com.vesanrebackend.service.provider;

import com.vesanrebackend.dto.provider.ShopChangeRequest;
import com.vesanrebackend.dto.provider.ShopResponse;
import com.vesanrebackend.dto.provider.UpdateShopRequest;
import com.vesanrebackend.entity.enums.ChangeRequestTargetType;
import com.vesanrebackend.entity.shop.Shop;
import com.vesanrebackend.repository.ShopRepository;
import com.vesanrebackend.service.storage.ImageCleanup;
import com.vesanrebackend.service.storage.ImageStorage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProviderShopService {
    private final ShopRepository shops;
    private final ProviderChangeRequestService changeRequests;
    private final ImageStorage storage;
    private final ApplicationEventPublisher events;

    public ProviderShopService(ShopRepository shops, ProviderChangeRequestService changeRequests, ImageStorage storage,
                               ApplicationEventPublisher events) {
        this.shops = shops;
        this.changeRequests = changeRequests;
        this.storage = storage;
        this.events = events;
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

    @Transactional
    public ShopResponse replaceLogo(UUID userId, MultipartFile file) {
        Shop shop = shopOf(userId);
        String key = storage.upload(file, "vesanre/shops/" + shop.getId());
        events.publishEvent(new ImageCleanup.ImageUploadedEvent(key));
        String old = shop.getLogoStorageKey();
        shop.setLogoStorageKey(key);
        if (old != null) {
            events.publishEvent(new ImageCleanup.ImagesDeletedEvent(List.of(old)));
        }
        return toResponse(shop);
    }

    @Transactional
    public void deleteLogo(UUID userId) {
        Shop shop = shopOf(userId);
        String old = shop.getLogoStorageKey();
        if (old != null) {
            shop.setLogoStorageKey(null);
            events.publishEvent(new ImageCleanup.ImagesDeletedEvent(List.of(old)));
        }
    }

    private ShopResponse toResponse(Shop shop) {
        return new ShopResponse(shop.getId(), shop.getSlug(), shop.getName(), shop.getDescription(),
                shop.getStatus().name(), changeRequests.pending(ChangeRequestTargetType.SHOP, shop.getId()),
                storage.url(shop.getLogoStorageKey()));
    }
}
