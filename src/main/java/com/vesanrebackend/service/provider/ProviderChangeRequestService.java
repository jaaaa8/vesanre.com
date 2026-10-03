package com.vesanrebackend.service.provider;

import com.vesanrebackend.dto.provider.PendingChangeResponse;
import com.vesanrebackend.entity.catalog.CatalogChangeRequest;
import com.vesanrebackend.entity.enums.ChangeRequestStatus;
import com.vesanrebackend.entity.enums.ChangeRequestTargetType;
import com.vesanrebackend.repository.CatalogChangeRequestRepository;
import com.vesanrebackend.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

// Shared by shop and venue: one PENDING request per target (DB partial unique index is the race backstop).
@Service
public class ProviderChangeRequestService {
    private final CatalogChangeRequestRepository requests;
    private final UserAccountRepository users;
    private final ObjectMapper objectMapper;

    public ProviderChangeRequestService(CatalogChangeRequestRepository requests, UserAccountRepository users,
                                        ObjectMapper objectMapper) {
        this.requests = requests;
        this.users = users;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void submit(UUID userId, ChangeRequestTargetType type, UUID targetId, Map<String, Object> proposed) {
        if (proposed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No important field changed");
        }
        if (requests.findByTargetTypeAndTargetIdAndStatus(type, targetId, ChangeRequestStatus.PENDING).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A change request is already pending");
        }
        CatalogChangeRequest request = new CatalogChangeRequest();
        request.setTargetType(type);
        request.setTargetId(targetId);
        request.setProposed(objectMapper.writeValueAsString(proposed));
        request.setSubmittedBy(users.getReferenceById(userId));
        requests.saveAndFlush(request);
    }

    @Transactional(readOnly = true)
    public PendingChangeResponse pending(ChangeRequestTargetType type, UUID targetId) {
        return requests.findByTargetTypeAndTargetIdAndStatus(type, targetId, ChangeRequestStatus.PENDING)
                .map(r -> new PendingChangeResponse(r.getId(),
                        objectMapper.readValue(r.getProposed(), new TypeReference<Map<String, Object>>() { }),
                        r.getCreatedAt()))
                .orElse(null);
    }

    @Transactional
    public void cancel(UUID userId, UUID requestId) {
        CatalogChangeRequest request = requests.findByIdAndSubmitter(requestId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Change request not found"));
        if (request.getStatus() != ChangeRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Change request is already " + request.getStatus());
        }
        request.setStatus(ChangeRequestStatus.CANCELLED);
    }
}
