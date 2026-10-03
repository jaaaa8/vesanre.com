package com.vesanrebackend.controller.provider;

import com.vesanrebackend.dto.provider.ImageOrderRequest;
import com.vesanrebackend.dto.provider.ImageResponse;
import com.vesanrebackend.service.provider.ProviderImageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/provider")
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderImageController {
    private final ProviderImageService images;

    public ProviderImageController(ProviderImageService images) {
        this.images = images;
    }

    @PostMapping(path = "/venues/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageResponse addVenueImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) @Size(max = 255) String altText) {
        return images.addVenueImage(userId(jwt), id, file, altText);
    }

    @PutMapping("/venues/{id}/images")
    public List<ImageResponse> reorderVenueImages(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                  @RequestBody List<@NotNull @Valid ImageOrderRequest> items) {
        return images.reorderVenueImages(userId(jwt), id, items);
    }

    @DeleteMapping("/venues/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVenueImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID imageId) {
        images.deleteVenueImage(userId(jwt), id, imageId);
    }

    @PostMapping(path = "/courts/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageResponse addCourtImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) @Size(max = 255) String altText) {
        return images.addCourtImage(userId(jwt), id, file, altText);
    }

    @PutMapping("/courts/{id}/images")
    public List<ImageResponse> reorderCourtImages(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                  @RequestBody List<@NotNull @Valid ImageOrderRequest> items) {
        return images.reorderCourtImages(userId(jwt), id, items);
    }

    @DeleteMapping("/courts/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourtImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID imageId) {
        images.deleteCourtImage(userId(jwt), id, imageId);
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
