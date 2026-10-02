package com.vesanrebackend.controller.auth;

import com.vesanrebackend.dto.auth.ProviderApplicationRequest;
import com.vesanrebackend.dto.auth.UpdateProfileRequest;
import com.vesanrebackend.dto.auth.UserProfileResponse;
import com.vesanrebackend.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final AuthService authService;

    public ProfileController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.profile(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping("/me")
    public UserProfileResponse update(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(UUID.fromString(jwt.getSubject()), request);
    }

    @PostMapping("/provider-application")
    public ResponseEntity<UserProfileResponse> applyProvider(@AuthenticationPrincipal Jwt jwt,
                                                             @Valid @RequestBody ProviderApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.applyProvider(UUID.fromString(jwt.getSubject()), request));
    }

    @GetMapping("/provider")
    @PreAuthorize("hasRole('PROVIDER')")
    public UserProfileResponse providerProfile(@AuthenticationPrincipal Jwt jwt) {
        return authService.profile(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse adminProfile(@AuthenticationPrincipal Jwt jwt) {
        return authService.profile(UUID.fromString(jwt.getSubject()));
    }
}
