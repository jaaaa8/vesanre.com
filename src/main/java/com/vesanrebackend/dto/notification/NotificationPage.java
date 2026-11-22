package com.vesanrebackend.dto.notification;

import java.util.List;

public record NotificationPage(List<NotificationItem> items, int page, int size, long totalElements, int totalPages,
                               long unreadCount) {
}
