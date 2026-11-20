package com.vesanrebackend.service.admin;

import com.vesanrebackend.dto.admin.AuditLogItem;
import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.entity.AuditLog;
import com.vesanrebackend.entity.account.UserAccount;
import com.vesanrebackend.repository.AuditLogRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class AdminAuditQueryService {
    private final AuditLogRepository auditLogs;
    private final ObjectMapper objectMapper;

    public AdminAuditQueryService(AuditLogRepository auditLogs, ObjectMapper objectMapper) {
        this.auditLogs = auditLogs;
        this.objectMapper = objectMapper;
    }

    // Read-only transaction: actorUser is LAZY.
    @Transactional(readOnly = true)
    public PageResponse<AuditLogItem> list(String entityType, UUID actorId, Pageable pageable) {
        Specification<AuditLog> spec = Specification.unrestricted();
        if (entityType != null && !entityType.isBlank()) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("entityType"), entityType.trim()));
        }
        if (actorId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("actorUser").get("id"), actorId));
        }
        return PageResponse.of(auditLogs.findAll(spec, pageable).map(this::toItem));
    }

    private AuditLogItem toItem(AuditLog log) {
        UserAccount actor = log.getActorUser();
        return new AuditLogItem(log.getId(), log.getAction(), log.getEntityType(), log.getEntityId(),
                actor == null ? null : new AuditLogItem.Actor(actor.getId(), actor.getDisplayName()),
                json(log.getBeforeData()), json(log.getAfterData()), log.getCreatedAt());
    }

    private JsonNode json(String raw) {
        return raw == null ? null : objectMapper.readTree(raw);
    }
}
