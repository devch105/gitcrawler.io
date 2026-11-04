
package com.git_crawler.backend.notification;

import com.git_crawler.backend.dto.NotificationEvent;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;
import lombok.RequiredArgsConstructor;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private static final String TOPIC = "notification-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CompletableFuture<SendResult<String, String>> publish(
            NotificationEvent event) {

        try {
            String json = objectMapper.writeValueAsString(event);
            return kafkaTemplate.send(TOPIC, event.eventId().toString(), json);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to serialize notification event", e);
        }
    }
}
