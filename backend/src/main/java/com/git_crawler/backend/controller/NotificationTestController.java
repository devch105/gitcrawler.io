
package com.git_crawler.backend.controller;

import com.git_crawler.backend.dto.NotificationEvent;
import com.git_crawler.backend.notification.NotificationProducer;

import com.git_crawler.backend.services.notification.NotificationOutboxService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.CompletableFuture;



@RestController
@RequestMapping("/test/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationTestController {

    private final NotificationProducer notificationProducer;


    private final NotificationOutboxService outboxService;

    @PostMapping("/enqueue")
    public ResponseEntity<String> enqueueTestEvent() {

        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(),
                "INDEXING_COMPLETED",
                UUID.randomUUID(),
                "subscriptionairtel@gmail.com",
                "test-owner/test-repository",
                "SUCCESS",
                null,
                Instant.now()
        );

        outboxService.enqueue(event);

        return ResponseEntity.ok("Notification event added to outbox");
    }

    @PostConstruct
    public void init() {
        log.info("NotificationTestController registered successfully!");
    }

    @PostMapping("/publish")
    public CompletableFuture<ResponseEntity<Map<String, Object>>>
    publishNotification() {
        log.info("Notification Tester is running");
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(),
                "INDEXING_COMPLETED",
                UUID.randomUUID(),
                "subscriptionairtel@gmail.com",
                "GitCrawler",
                "SUCCESS",
                null,
                Instant.now()
        );

      return    notificationProducer.publish(event)
                .thenApply(result -> ResponseEntity.accepted().body(
                        Map.of(
                                "message", "Event published successfully",
                                "eventId", event.eventId().toString(),
                                "topic", result.getRecordMetadata().topic(),
                                "partition", result.getRecordMetadata().partition(),
                                "offset", result.getRecordMetadata().offset()
                        )
                ));
    }
}
