package com.gitcrawler.notification_service.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class NotificationHealthController {


    @GetMapping
    public Map<String,String> health() {
        return Map.of("Service", "NotificationService",
                "Status", "UP");
    }
}
