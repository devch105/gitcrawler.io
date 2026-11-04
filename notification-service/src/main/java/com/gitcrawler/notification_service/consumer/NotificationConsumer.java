package com.gitcrawler.notification_service.consumer;

import com.gitcrawler.notification_service.service.NotificationProcessor;
import tools.jackson.databind.ObjectMapper;
import com.gitcrawler.notification_service.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationProcessor notificationProcessor;

    @KafkaListener(
            topics = "notification-events",
            groupId = "notification-service-group"
    )
    public void consume(String message) {

        try {
            NotificationEvent event =
                    objectMapper.readValue(
                            message,
                            NotificationEvent.class
                    );

            log.info("Notification event received!");
            log.info("Event ID: {}", event.eventId());
            log.info("Event Type: {}", event.eventType());
            log.info("Email: {}", event.email());
            log.info("Repository: {}", event.repositoryName());
            log.info("Status: {}", event.status());
            notificationProcessor.process(event);

        } catch (Exception e) {
            log.error("Notification processing failed", e);
            throw e;
        }
    }
}