package com.git_crawler.backend.services.notification;

import com.git_crawler.backend.entity.NotificationOutbox;
import com.git_crawler.backend.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxPublisher {

    private static final String TOPIC = "notification-events";

    private final NotificationOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.notification.outbox.polling-interval:3600000}")
    public void publishPendingEvents() {

        List<NotificationOutbox> events =
                outboxRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");

        for (NotificationOutbox event : events) {
            try {
                kafkaTemplate.send(
                        TOPIC,
                        event.getId().toString(),
                        event.getPayload()
                ).get();

                markPublished(event);

                log.info("Published notification event {}", event.getId());

            } catch (Exception e) {
                markFailed(event, e);
            }
        }
    }

    @Transactional
    public void markPublished(NotificationOutbox event) {
        event.setStatus("PUBLISHED");
        event.setPublishedAt(Instant.now());
        event.setLastError(null);
        outboxRepository.save(event);
    }

    @Transactional
    public void markFailed(NotificationOutbox event, Exception exception) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setLastError(exception.getMessage());

        if (event.getRetryCount() >= 10) {
            event.setStatus("FAILED");
        }

        outboxRepository.save(event);
    }
}