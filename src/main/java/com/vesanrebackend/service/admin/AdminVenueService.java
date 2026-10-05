package com.vesanrebackend.service.admin;

import com.vesanrebackend.dto.admin.AdminVenueDetailResponse;
import com.vesanrebackend.dto.admin.AdminVenueSummaryResponse;
import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.provider.VenueDetailResponse;
import com.vesanrebackend.entity.enums.VenueStatus;
import com.vesanrebackend.entity.venue.Venue;
import com.vesanrebackend.repository.CourtRepository;
import com.vesanrebackend.repository.VenueRepository;
import com.vesanrebackend.service.mail.ReviewMailer;
import com.vesanrebackend.service.provider.ProviderCourtService;
import com.vesanrebackend.service.provider.ProviderVenueService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminVenueService {
    private static final String ENTITY_TYPE = "VENUE";

    private final VenueRepository venues;
    private final CourtRepository courts;
    private final ProviderVenueService providerVenues;
    private final ProviderCourtService providerCourts;
    private final AdminAudit audit;
    private final ReviewMailer mailer;

    public AdminVenueService(VenueRepository venues, CourtRepository courts, ProviderVenueService providerVenues,
                             ProviderCourtService providerCourts, AdminAudit audit, ReviewMailer mailer) {
        this.venues = venues;
        this.courts = courts;
        this.providerVenues = providerVenues;
        this.providerCourts = providerCourts;
        this.audit = audit;
        this.mailer = mailer;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminVenueSummaryResponse> list(VenueStatus status, Pageable pageable) {
        return PageResponse.of(venues.findByStatusWithShop(status, pageable).map(v -> new AdminVenueSummaryResponse(
                v.getId(), v.getName(), v.getShop().getId(), v.getShop().getName(), v.getAddressLine(),
                v.getDistrict(), v.getCity(), v.getStatus().name(), v.getCreatedAt())));
    }

    @Transactional(readOnly = true)
    public AdminVenueDetailResponse get(UUID id) {
        Venue venue = venues.findById(id).orElseThrow(AdminVenueService::notFound);
        return new AdminVenueDetailResponse(providerVenues.toDetail(venue),
                courts.findByVenueIdOrderByCode(id).stream().map(providerCourts::toDetail).toList());
    }

    @Transactional
    public VenueDetailResponse approve(UUID adminId, UUID id) {
        return decide(adminId, id, VenueStatus.PENDING_REVIEW, VenueStatus.ACTIVE, "VENUE_APPROVED", null,
                "đã được duyệt", "Địa điểm %s của bạn đã được duyệt và đang hiển thị công khai.");
    }

    @Transactional
    public VenueDetailResponse reject(UUID adminId, UUID id, String reason) {
        return decide(adminId, id, VenueStatus.PENDING_REVIEW, VenueStatus.REJECTED, "VENUE_REJECTED", reason,
                "bị từ chối", "Địa điểm %s của bạn bị từ chối. Bạn có thể chỉnh sửa và gửi duyệt lại.");
    }

    @Transactional
    public VenueDetailResponse suspend(UUID adminId, UUID id, String reason) {
        return decide(adminId, id, VenueStatus.ACTIVE, VenueStatus.SUSPENDED, "VENUE_SUSPENDED", reason,
                "bị tạm đình chỉ", "Địa điểm %s của bạn bị tạm đình chỉ và không còn hiển thị công khai.");
    }

    @Transactional
    public VenueDetailResponse reactivate(UUID adminId, UUID id) {
        return decide(adminId, id, VenueStatus.SUSPENDED, VenueStatus.ACTIVE, "VENUE_REACTIVATED", null,
                "đã được khôi phục", "Địa điểm %s của bạn đã được khôi phục và hiển thị công khai trở lại.");
    }

    // The row lock makes a second admin wait, then see the new status and get 409.
    private VenueDetailResponse decide(UUID adminId, UUID id, VenueStatus from, VenueStatus to, String action,
                                       String reason, String subjectTail, String message) {
        Venue venue = venues.findByIdForUpdate(id).orElseThrow(AdminVenueService::notFound);
        if (venue.getStatus() != from) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Venue is not " + from);
        }
        venue.setStatus(to);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", to.name());
        if (reason != null) {
            after.put("reason", reason);
        }
        audit.record(adminId, action, ENTITY_TYPE, id, Map.of("status", from.name()), after);
        mailer.send(venue.getShop().getOwner().getUser().getEmail(),
                "[Vesanre] Địa điểm " + venue.getName() + " " + subjectTail, message.formatted(venue.getName()), reason);
        return providerVenues.toDetail(venue);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found");
    }
}
