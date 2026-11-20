package com.vesanrebackend.controller.admin;

import com.vesanrebackend.dto.admin.AdminUserItem;
import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.admin.ReasonRequest;
import com.vesanrebackend.entity.enums.UserStatus;
import com.vesanrebackend.service.admin.AdminUserService;
import com.vesanrebackend.util.Paging;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService adminUsers;

    public AdminUserController(AdminUserService adminUsers) {
        this.adminUsers = adminUsers;
    }

    @GetMapping
    public PageResponse<AdminUserItem> list(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) String role,
                                            @RequestParam(required = false) UserStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return adminUsers.list(q, role, status, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @PostMapping("/{id}/suspend")
    public AdminUserItem suspend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody ReasonRequest request) {
        return adminUsers.suspend(UUID.fromString(jwt.getSubject()), id, request.reason());
    }

    @PostMapping("/{id}/reactivate")
    public AdminUserItem reactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return adminUsers.reactivate(UUID.fromString(jwt.getSubject()), id);
    }
}
