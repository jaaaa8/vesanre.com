package com.vesanrebackend.service.notification;

import com.vesanrebackend.dto.notification.NotificationItem;
import com.vesanrebackend.dto.notification.NotificationPage;
import com.vesanrebackend.entity.Notification;
import com.vesanrebackend.repository.NotificationRepository;
import com.vesanrebackend.util.Paging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.UUID;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NotificationPage list(UUID userId, boolean unreadOnly, int page, int size) {
        var pageable = Paging.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Notification> p = unreadOnly ? notifications.findByUserIdAndReadAtIsNull(userId, pageable)
                : notifications.findByUserId(userId, pageable);
        var items = p.getContent().stream().map(n -> new NotificationItem(n.getId(), n.getNotificationType(),
                n.getTitle(), n.getBody(), n.getBooking() == null ? null : n.getBooking().getId(), n.getReadAt(),
                n.getCreatedAt())).toList();
        return new NotificationPage(items, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(),
                notifications.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public void markRead(UUID userId, UUID id) {
        Notification n = notifications.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (n.getReadAt() == null) {
            n.setReadAt(clock.instant());
        }
    }
}
