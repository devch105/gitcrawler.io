package com.git_crawler.backend.services.notification;

import com.git_crawler.backend.dto.NotificationEvent;
import com.git_crawler.backend.entity.NotificationOutbox;
import com.git_crawler.backend.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private final NotificationOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueue(NotificationEvent event) {

        try {
            String payload = objectMapper.writeValueAsString(event);

            NotificationOutbox outbox = NotificationOutbox.builder()
                    .eventType(event.eventType())
                    .aggregateId(event.userId())
                    .payload(payload)
                    .status("PENDING")
                    .createdAt(Instant.now())
                    .retryCount(0)
                    .build();

            outboxRepository.save(outbox);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to enqueue notification event", e
            );
        }
    }
}