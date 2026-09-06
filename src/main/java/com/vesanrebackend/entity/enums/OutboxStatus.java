package com.vesanrebackend.entity.enums;

public enum OutboxStatus {
    PENDING, PROCESSING, RETRY_PENDING, PUBLISHED, FAILED
}
