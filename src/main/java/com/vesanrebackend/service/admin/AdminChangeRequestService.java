package com.vesanrebackend.service.admin;

import com.vesanrebackend.dto.admin.AdminChangeRequestResponse;
import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.entity.catalog.CatalogChangeRequest;
import com.vesanrebackend.entity.enums.ChangeRequestStatus;
import com.vesanrebackend.entity.enums.ChangeRequestTargetType;
import com.vesanrebackend.entity.shop.Shop;
import com.vesanrebackend.entity.venue.Venue;
import com.vesanrebackend.repository.CatalogChangeRequestRepository;
import com.vesanrebackend.repository.ShopRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import com.vesanrebackend.repository.VenueRepository;
import com.vesanrebackend.service.mail.ReviewMailer;
import com.vesanrebackend.service.provider.ProviderVenueService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminChangeRequestService {
    private static final String ENTITY_TYPE = "CATALOG_CHANGE_REQUEST";

    private final CatalogChangeRequestRepository requests;
    private final VenueRepository venues;
    private final ShopRepository shops;
    private final UserAccountRepository users;
    private final AdminAudit audit;
    private final ReviewMailer mailer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public AdminChangeRequestService(CatalogChangeRequestRepository requests, VenueRepository venues, ShopRepository shops,
                                     UserAccountRepository users, AdminAudit audit, ReviewMailer mailer,
                                     ObjectMapper objectMapper, Clock clock) {
        this.requests = requests;
        this.venues = venues;
        this.shops = shops;
        this.users = users;
        this.audit = audit;
        this.mailer = mailer;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    private record Target(String name, String ownerEmail, Map<String, Object> current) {
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminChangeRequestResponse> list(ChangeRequestStatus status, ChangeRequestTargetType type,
                                                         Pageable pageable) {
        Page<CatalogChangeRequest> page = type == null ? requests.findByStatus(status, pageable)
                : requests.findByStatusAndTargetType(status, type, pageable);
        // ponytail: one target lookup per row (size <= 100); batch by target ids if the queue page gets slow.
        return PageResponse.of(page.map(r -> {
            Map<String, Object> proposed = proposed(r);
            return toResponse(r, proposed, target(r, proposed, false));
        }));
    }

    @Transactional
    public AdminChangeRequestResponse approve(UUID adminId, UUID id) {
        CatalogChangeRequest request = lockPending(id);
        Map<String, Object> proposed = proposed(request);
        Target target = target(request, proposed, true);
        review(request, ChangeRequestStatus.APPROVED, adminId, null);
        audit.record(adminId, "CHANGE_REQUEST_APPROVED", ENTITY_TYPE, id, target.current(), withTarget(request, proposed));
        mailer.send(target.ownerEmail(), "[Vesanre] Yêu cầu thay đổi " + target.name() + " đã được duyệt",
                "Yêu cầu thay đổi thông tin " + target.name() + " của bạn đã được duyệt và áp dụng.", null);
        return toResponse(request, proposed, target);
    }

    @Transactional
    public AdminChangeRequestResponse reject(UUID adminId, UUID id, String reason) {
        CatalogChangeRequest request = lockPending(id);
        Map<String, Object> proposed = proposed(request);
        Target target = target(request, proposed, false);
        review(request, ChangeRequestStatus.REJECTED, adminId, reason);
        Map<String, Object> after = withTarget(request, Map.of("status", "REJECTED"));
        after.put("reason", reason);
        audit.record(adminId, "CHANGE_REQUEST_REJECTED", ENTITY_TYPE, id, Map.of("status", "PENDING"), after);
        mailer.send(target.ownerEmail(), "[Vesanre] Yêu cầu thay đổi " + target.name() + " bị từ chối",
                "Yêu cầu thay đổi thông tin " + target.name() + " của bạn đã bị từ chối. Thông tin hiện tại được giữ nguyên.",
                reason);
        return toResponse(request, proposed, target);
    }

    // The row lock makes a second admin wait, then see the new status and get 409.
    private CatalogChangeRequest lockPending(UUID id) {
        CatalogChangeRequest request = requests.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Change request not found"));
        if (request.getStatus() != ChangeRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Change request is already " + request.getStatus());
        }
        return request;
    }

    // apply=true locks the target first: Venue/Shop have no @Version, so writing an unlocked copy would put back
    // a status (e.g. SUSPENDED) committed in between. Lock order is always request -> target.
    // Target always exists (venues with change requests and shops cannot be deleted); orElseThrow is a 500 by design.
    private Target target(CatalogChangeRequest r, Map<String, Object> proposed, boolean apply) {
        if (r.getTargetType() == ChangeRequestTargetType.VENUE) {
            Venue venue = (apply ? venues.findByIdForUpdate(r.getTargetId()) : venues.findById(r.getTargetId())).orElseThrow();
            Target target = new Target(venue.getName(), venue.getShop().getOwner().getUser().getEmail(),
                    ProviderVenueService.importantValues(venue, proposed.keySet()));
            if (apply) {
                ProviderVenueService.applyImportant(venue, proposed);
            }
            return target;
        }
        Shop shop = (apply ? shops.findByIdForUpdate(r.getTargetId()) : shops.findById(r.getTargetId())).orElseThrow();
        Map<String, Object> current = new LinkedHashMap<>();
        current.put("name", shop.getName());
        Target target = new Target(shop.getName(), shop.getOwner().getUser().getEmail(), current);
        if (apply) {
            shop.setName((String) proposed.get("name")); // slug stays: shared links keep working (spec A6)
        }
        return target;
    }

    // Floats straight to BigDecimal: coordinates never pass through double.
    @SuppressWarnings("unchecked")
    private Map<String, Object> proposed(CatalogChangeRequest r) {
        return objectMapper.readerFor(Map.class).with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .readValue(r.getProposed());
    }

    private void review(CatalogChangeRequest r, ChangeRequestStatus status, UUID adminId, String reason) {
        r.setStatus(status);
        r.setReviewedBy(users.getReferenceById(adminId));
        r.setReviewedAt(clock.instant());
        r.setRejectionReason(reason);
    }

    private static Map<String, Object> withTarget(CatalogChangeRequest r, Map<String, Object> data) {
        Map<String, Object> out = new LinkedHashMap<>(data);
        out.put("targetType", r.getTargetType().name());
        out.put("targetId", r.getTargetId());
        return out;
    }

    private static AdminChangeRequestResponse toResponse(CatalogChangeRequest r, Map<String, Object> proposed, Target target) {
        return new AdminChangeRequestResponse(r.getId(), r.getTargetType().name(), r.getTargetId(), target.name(),
                target.current(), proposed, r.getSubmittedBy().getEmail(), r.getStatus().name(), r.getCreatedAt());
    }
}
