package com.vesanrebackend.service.admin;

import com.vesanrebackend.entity.AuditLog;
import com.vesanrebackend.repository.AuditLogRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

@Component
public class AdminAudit {
    private final AuditLogRepository auditLogs;
    private final UserAccountRepository users;
    private final ObjectMapper objectMapper;

    public AdminAudit(AuditLogRepository auditLogs, UserAccountRepository users, ObjectMapper objectMapper) {
        this.auditLogs = auditLogs;
        this.users = users;
        this.objectMapper = objectMapper;
    }

    public void record(UUID adminId, String action, String entityType, UUID entityId,
                       Map<String, Object> before, Map<String, Object> after) {
        AuditLog log = new AuditLog();
        log.setActorUser(users.getReferenceById(adminId));
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setBeforeData(objectMapper.writeValueAsString(before));
        log.setAfterData(objectMapper.writeValueAsString(after));
        auditLogs.save(log);
    }
}
