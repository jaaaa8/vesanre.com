package com.vesanrebackend.controller.admin;

import com.vesanrebackend.dto.admin.AuditLogItem;
import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.service.admin.AdminAuditQueryService;
import com.vesanrebackend.util.Paging;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {
    private final AdminAuditQueryService auditQuery;

    public AdminAuditController(AdminAuditQueryService auditQuery) {
        this.auditQuery = auditQuery;
    }

    @GetMapping
    public PageResponse<AuditLogItem> list(@RequestParam(required = false) String entityType,
                                           @RequestParam(required = false) UUID actorId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return auditQuery.list(entityType, actorId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }
}
