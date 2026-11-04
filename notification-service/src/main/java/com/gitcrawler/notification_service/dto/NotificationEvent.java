package com.gitcrawler.notification_service.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(
        UUID eventId,
        String eventType,
        UUID userId,
        String email,
        String repositoryName,
        String status,
        String failureReason,
        Instant occurredAt
) {}