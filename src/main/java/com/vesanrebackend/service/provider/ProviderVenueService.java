package com.vesanrebackend.service.provider;

import com.vesanrebackend.dto.provider.CreateVenueRequest;
import com.vesanrebackend.dto.provider.UpdateVenueRequest;
import com.vesanrebackend.dto.provider.VenueAmenityRequest;
import com.vesanrebackend.dto.provider.VenueChangeRequest;
import com.vesanrebackend.dto.provider.VenueDetailResponse;
import com.vesanrebackend.dto.provider.VenueSummaryResponse;
import com.vesanrebackend.entity.catalog.Amenity;
import com.vesanrebackend.entity.enums.AmenityScope;
import com.vesanrebackend.entity.enums.ChangeRequestTargetType;
import com.vesanrebackend.entity.enums.CourtStatus;
import com.vesanrebackend.entity.enums.VenueStatus;
import com.vesanrebackend.entity.venue.Venue;
import com.vesanrebackend.entity.venue.VenueAmenity;
import com.vesanrebackend.repository.AmenityRepository;
import com.vesanrebackend.repository.CourtRepository;
import com.vesanrebackend.repository.VenueAmenityRepository;
import com.vesanrebackend.repository.VenueRepository;
import com.vesanrebackend.util.SlugGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProviderVenueService {
    private final ProviderShopService shops;
    private final ProviderChangeRequestService changeRequests;
    private final VenueRepository venues;
    private final CourtRepository courts;
    private final VenueAmenityRepository venueAmenities;
    private final AmenityRepository amenities;

    public ProviderVenueService(ProviderShopService shops, ProviderChangeRequestService changeRequests,
                                VenueRepository venues, CourtRepository courts,
                                VenueAmenityRepository venueAmenities, AmenityRepository amenities) {
        this.shops = shops;
        this.changeRequests = changeRequests;
        this.venues = venues;
        this.courts = courts;
        this.venueAmenities = venueAmenities;
        this.amenities = amenities;
    }

    @Transactional(readOnly = true)
    public List<VenueSummaryResponse> list(UUID userId) {
        return venues.findByShopIdOrderByCreatedAtDesc(shops.shopOf(userId).getId()).stream()
                .map(v -> new VenueSummaryResponse(v.getId(), v.getSlug(), v.getName(), v.getDistrict(), v.getCity(),
                        v.getStatus().name()))
                .toList();
    }

    @Transactional
    public VenueDetailResponse create(UUID userId, CreateVenueRequest request) {
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude and longitude must be provided together");
        }
        Venue venue = new Venue();
        venue.setShop(shops.shopOf(userId));
        venue.setName(request.name().trim());
        // Random suffix keeps (shop_id, slug) unique, same as shop slugs.
        venue.setSlug(SlugGenerator.generate(venue.getName(), "venue"));
        venue.setDescription(blankToNull(request.description()));
        venue.setAddressLine(request.addressLine().trim());
        venue.setWard(blankToNull(request.ward()));
        venue.setDistrict(request.district().trim());
        venue.setCity(request.city().trim());
        venue.setProvince(blankToNull(request.province()));
        venue.setPostalCode(blankToNull(request.postalCode()));
        venue.setLatitude(request.latitude());
        venue.setLongitude(request.longitude());
        venue.setPhone(blankToNull(request.phone()));
        venues.saveAndFlush(venue);
        return toDetail(venue);
    }

    @Transactional(readOnly = true)
    public VenueDetailResponse get(UUID userId, UUID id) {
        return toDetail(owned(userId, id));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Venue venue = owned(userId, id);
        if (venue.getStatus() != VenueStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a DRAFT venue can be deleted");
        }
        courts.deleteByVenueId(id); // courts reference the venue with ON DELETE RESTRICT
        venues.delete(venue);
        venues.flush();
    }

    @Transactional
    public VenueDetailResponse submit(UUID userId, UUID id) {
        Venue venue = owned(userId, id);
        if (venue.getStatus() != VenueStatus.DRAFT && venue.getStatus() != VenueStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a DRAFT or REJECTED venue can be submitted");
        }
        if (!courts.existsByVenueIdAndStatus(id, CourtStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Venue needs at least one ACTIVE court to submit");
        }
        venue.setStatus(VenueStatus.PENDING_REVIEW);
        return toDetail(venue);
    }

    @Transactional
    public VenueDetailResponse replaceAmenities(UUID userId, UUID id, List<VenueAmenityRequest> items) {
        Venue venue = owned(userId, id);
        Set<UUID> ids = new HashSet<>();
        for (VenueAmenityRequest item : items) {
            if (!ids.add(item.amenityId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate amenityId " + item.amenityId());
            }
        }
        Map<UUID, Amenity> found = amenities.findAllById(ids).stream()
                .collect(Collectors.toMap(Amenity::getId, Function.identity()));
        for (UUID amenityId : ids) {
            Amenity amenity = found.get(amenityId);
            if (amenity == null || !amenity.getIsActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown or inactive amenity " + amenityId);
            }
            if (amenity.getAllowedScope() == AmenityScope.COURT) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Amenity " + amenity.getCode() + " is not allowed on a venue");
            }
        }
        venueAmenities.deleteByVenueId(id); // bulk delete first: Hibernate would otherwise INSERT before DELETE
        for (VenueAmenityRequest item : items) {
            VenueAmenity link = new VenueAmenity();
            link.setVenue(venue);
            link.setAmenity(found.get(item.amenityId()));
            link.getId().setVenueId(id);
            link.getId().setAmenityId(item.amenityId());
            link.setDetails(blankToNull(item.details()));
            venueAmenities.save(link);
        }
        venueAmenities.flush();
        return toDetail(venue);
    }

    @Transactional
    public VenueDetailResponse update(UUID userId, UUID id, UpdateVenueRequest request) {
        Venue venue = owned(userId, id);
        Map<String, Object> changes = importantChanges(venue, request.important());
        if (!changes.isEmpty()) {
            if (venue.getStatus() != VenueStatus.DRAFT && venue.getStatus() != VenueStatus.REJECTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Important fields of a " + venue.getStatus() + " venue can only change via a change request");
            }
            applyImportant(venue, changes);
        }
        if (request.description() != null) {
            venue.setDescription(blankToNull(request.description()));
        }
        if (request.phone() != null) {
            venue.setPhone(blankToNull(request.phone()));
        }
        return toDetail(venue);
    }

    @Transactional
    public VenueDetailResponse submitChange(UUID userId, UUID id, VenueChangeRequest request) {
        Venue venue = owned(userId, id);
        if (venue.getStatus() != VenueStatus.ACTIVE && venue.getStatus() != VenueStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an ACTIVE or SUSPENDED venue takes change requests");
        }
        changeRequests.submit(userId, ChangeRequestTargetType.VENUE, id, importantChanges(venue, request));
        return toDetail(venue);
    }

    // Single place that decides which important fields differ (null = not sent). Values: trimmed String
    // (null clears an optional field) or BigDecimal. Strings compare trimmed, coordinates by compareTo.
    static Map<String, Object> importantChanges(Venue v, VenueChangeRequest r) {
        Map<String, Object> out = new LinkedHashMap<>();
        text(out, "name", r.name(), v.getName(), true);
        text(out, "addressLine", r.addressLine(), v.getAddressLine(), true);
        text(out, "ward", r.ward(), v.getWard(), false);
        text(out, "district", r.district(), v.getDistrict(), true);
        text(out, "city", r.city(), v.getCity(), true);
        text(out, "province", r.province(), v.getProvince(), false);
        text(out, "postalCode", r.postalCode(), v.getPostalCode(), false);
        BigDecimal lat = r.latitude() != null ? r.latitude() : v.getLatitude();
        BigDecimal lon = r.longitude() != null ? r.longitude() : v.getLongitude();
        if ((lat == null) != (lon == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latitude and longitude must be provided together");
        }
        number(out, "latitude", r.latitude(), v.getLatitude());
        number(out, "longitude", r.longitude(), v.getLongitude());
        return out;
    }

    // Also meant for admin approval (part 4): numbers may arrive as any Number from JSON.
    static void applyImportant(Venue v, Map<String, Object> c) {
        if (c.containsKey("name")) v.setName((String) c.get("name"));
        if (c.containsKey("addressLine")) v.setAddressLine((String) c.get("addressLine"));
        if (c.containsKey("ward")) v.setWard((String) c.get("ward"));
        if (c.containsKey("district")) v.setDistrict((String) c.get("district"));
        if (c.containsKey("city")) v.setCity((String) c.get("city"));
        if (c.containsKey("province")) v.setProvince((String) c.get("province"));
        if (c.containsKey("postalCode")) v.setPostalCode((String) c.get("postalCode"));
        if (c.containsKey("latitude")) v.setLatitude(new BigDecimal(c.get("latitude").toString()));
        if (c.containsKey("longitude")) v.setLongitude(new BigDecimal(c.get("longitude").toString()));
    }

    private static void text(Map<String, Object> out, String key, String sent, String current, boolean required) {
        if (sent == null) {
            return;
        }
        String value = blankToNull(sent);
        if (value == null && required) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, key + " cannot be blank");
        }
        if (!Objects.equals(value, current)) {
            out.put(key, value);
        }
    }

    private static void number(Map<String, Object> out, String key, BigDecimal sent, BigDecimal current) {
        if (sent != null && (current == null || sent.compareTo(current) != 0)) {
            out.put(key, sent);
        }
    }

    private Venue owned(UUID userId, UUID id) {
        return venues.findOwned(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue not found"));
    }

    private VenueDetailResponse toDetail(Venue v) {
        return new VenueDetailResponse(v.getId(), v.getSlug(), v.getName(), v.getDescription(), v.getAddressLine(),
                v.getWard(), v.getDistrict(), v.getCity(), v.getProvince(), v.getPostalCode(), v.getLatitude(),
                v.getLongitude(), v.getPhone(), v.getStatus().name(),
                courts.findByVenueIdOrderByCode(v.getId()).stream()
                        .map(c -> new VenueDetailResponse.CourtItem(c.getId(), c.getCode(), c.getName(), c.getStatus().name()))
                        .toList(),
                venueAmenities.findByVenueId(v.getId()).stream()
                        .map(a -> new VenueDetailResponse.AmenityItem(a.getAmenity().getId(), a.getAmenity().getCode(),
                                a.getAmenity().getName(), a.getDetails()))
                        .toList(),
                changeRequests.pending(ChangeRequestTargetType.VENUE, v.getId()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
