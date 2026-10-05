package com.vesanrebackend.service.provider;

import com.vesanrebackend.dto.provider.ImageOrderRequest;
import com.vesanrebackend.dto.provider.ImageResponse;
import com.vesanrebackend.entity.ImageRow;
import com.vesanrebackend.entity.court.Court;
import com.vesanrebackend.entity.court.CourtImage;
import com.vesanrebackend.entity.venue.Venue;
import com.vesanrebackend.entity.venue.VenueImage;
import com.vesanrebackend.repository.CourtImageRepository;
import com.vesanrebackend.repository.CourtRepository;
import com.vesanrebackend.repository.VenueImageRepository;
import com.vesanrebackend.repository.VenueRepository;
import com.vesanrebackend.service.storage.ImageCleanup;
import com.vesanrebackend.service.storage.ImageStorage;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Venue/court galleries. Images are free fields: they apply at once in every status (spec D3).
@Service
public class ProviderImageService {
    private static final int MAX_IMAGES = 10;

    private final VenueRepository venues;
    private final CourtRepository courts;
    private final VenueImageRepository venueImages;
    private final CourtImageRepository courtImages;
    private final ImageStorage storage;
    private final ApplicationEventPublisher events;

    public ProviderImageService(VenueRepository venues, CourtRepository courts, VenueImageRepository venueImages,
                                CourtImageRepository courtImages, ImageStorage storage, ApplicationEventPublisher events) {
        this.venues = venues;
        this.courts = courts;
        this.venueImages = venueImages;
        this.courtImages = courtImages;
        this.storage = storage;
        this.events = events;
    }

    @Transactional
    public ImageResponse addVenueImage(UUID userId, UUID venueId, MultipartFile file, String altText) {
        Venue venue = ownedVenue(userId, venueId);
        VenueImage image = new VenueImage();
        image.setVenue(venue);
        fillNew(image, venueImages.findByVenueIdOrderBySortOrder(venueId), file, "vesanre/venues/" + venueId, altText);
        venueImages.saveAndFlush(image); // concurrent uploads collide here (409); the upload is dropped on rollback
        return toResponse(image);
    }

    @Transactional
    public ImageResponse addCourtImage(UUID userId, UUID courtId, MultipartFile file, String altText) {
        Court court = ownedCourt(userId, courtId);
        CourtImage image = new CourtImage();
        image.setCourt(court);
        fillNew(image, courtImages.findByCourtIdOrderBySortOrder(courtId), file, "vesanre/courts/" + courtId, altText);
        courtImages.saveAndFlush(image);
        return toResponse(image);
    }

    @Transactional
    public List<ImageResponse> reorderVenueImages(UUID userId, UUID venueId, List<ImageOrderRequest> items) {
        ownedVenue(userId, venueId);
        return reorder(venueImages.findByVenueIdOrderBySortOrder(venueId), items, venueImages);
    }

    @Transactional
    public List<ImageResponse> reorderCourtImages(UUID userId, UUID courtId, List<ImageOrderRequest> items) {
        ownedCourt(userId, courtId);
        return reorder(courtImages.findByCourtIdOrderBySortOrder(courtId), items, courtImages);
    }

    @Transactional
    public void deleteVenueImage(UUID userId, UUID venueId, UUID imageId) {
        ownedVenue(userId, venueId);
        VenueImage image = venueImages.findByIdAndVenueId(imageId, venueId).orElseThrow(() -> notFound("Image not found"));
        venueImages.delete(image);
        venueImages.flush(); // the old cover row must be gone before another row becomes cover
        afterDelete(image, venueImages.findByVenueIdOrderBySortOrder(venueId));
    }

    @Transactional
    public void deleteCourtImage(UUID userId, UUID courtId, UUID imageId) {
        ownedCourt(userId, courtId);
        CourtImage image = courtImages.findByIdAndCourtId(imageId, courtId).orElseThrow(() -> notFound("Image not found"));
        courtImages.delete(image);
        courtImages.flush();
        afterDelete(image, courtImages.findByCourtIdOrderBySortOrder(courtId));
    }

    // Venue DRAFT delete: rows go with ON DELETE CASCADE; objects are dropped once that commits.
    public void deleteAllOfVenueAfterCommit(UUID venueId) {
        List<String> keys = new ArrayList<>(venueImages.findStorageKeysByVenueId(venueId));
        keys.addAll(courtImages.findStorageKeysByVenueId(venueId));
        if (!keys.isEmpty()) {
            events.publishEvent(new ImageCleanup.ImagesDeletedEvent(keys));
        }
    }

    // Queried, not read from Venue.images / Court.images: the replace-all PUTs bulk-delete and detach the entity.
    public List<ImageResponse> ofVenue(UUID venueId) {
        return toResponses(venueImages.findByVenueIdOrderBySortOrder(venueId));
    }

    public List<ImageResponse> ofCourt(UUID courtId) {
        return toResponses(courtImages.findByCourtIdOrderBySortOrder(courtId));
    }

    private List<ImageResponse> toResponses(List<? extends ImageRow> rows) {
        return rows.stream().map(this::toResponse).toList();
    }

    private void fillNew(ImageRow image, List<? extends ImageRow> current, MultipartFile file, String folder,
                         String altText) {
        if (current.size() >= MAX_IMAGES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "At most " + MAX_IMAGES + " images are allowed");
        }
        String key = storage.upload(file, folder);
        events.publishEvent(new ImageCleanup.ImageUploadedEvent(key));
        image.setStorageKey(key);
        image.setAltText(blankToNull(altText));
        image.setSortOrder(current.isEmpty() ? 0 : current.get(current.size() - 1).getSortOrder() + 1);
        image.setIsCover(current.isEmpty());
    }

    // Body must list exactly the current images; list order = sortOrder 0..n-1, first = cover.
    private List<ImageResponse> reorder(List<? extends ImageRow> current, List<ImageOrderRequest> items,
                                        JpaRepository<?, ?> repo) {
        Map<UUID, ImageRow> byId = new HashMap<>();
        current.forEach(r -> byId.put(r.getId(), r));
        Set<UUID> sent = items.stream().map(ImageOrderRequest::id).collect(Collectors.toSet());
        if (items.size() != current.size() || !sent.equals(byId.keySet())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body must list every image exactly once");
        }
        if (current.isEmpty()) {
            return List.of();
        }
        // Unique (owner, sort_order) and the single-cover index are checked per row: park every row above the
        // current max first, then write the final values.
        int offset = current.get(current.size() - 1).getSortOrder() + 1;
        current.forEach(r -> {
            r.setSortOrder(r.getSortOrder() + offset);
            r.setIsCover(false);
        });
        repo.flush();
        List<ImageRow> ordered = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ImageRow row = byId.get(items.get(i).id());
            row.setSortOrder(i);
            row.setIsCover(i == 0);
            row.setAltText(blankToNull(items.get(i).altText()));
            ordered.add(row);
        }
        repo.flush();
        return toResponses(ordered);
    }

    // sortOrder may keep gaps: new = max + 1, responses sort by it, PUT rewrites 0..n-1.
    private void afterDelete(ImageRow deleted, List<? extends ImageRow> remaining) {
        if (deleted.getIsCover() && !remaining.isEmpty()) {
            remaining.get(0).setIsCover(true);
        }
        events.publishEvent(new ImageCleanup.ImagesDeletedEvent(List.of(deleted.getStorageKey())));
    }

    private ImageResponse toResponse(ImageRow r) {
        return new ImageResponse(r.getId(), storage.url(r.getStorageKey()), r.getAltText(), r.getSortOrder(),
                r.getIsCover());
    }

    private Venue ownedVenue(UUID userId, UUID venueId) {
        return venues.findOwned(venueId, userId).orElseThrow(() -> notFound("Venue not found"));
    }

    private Court ownedCourt(UUID userId, UUID courtId) {
        return courts.findOwned(courtId, userId).orElseThrow(() -> notFound("Court not found"));
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
