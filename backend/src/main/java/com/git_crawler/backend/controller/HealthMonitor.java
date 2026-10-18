package com.git_crawler.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.util.repeat.RepeatSpec;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Slf4j
public class HealthMonitor {

    @GetMapping
    public ResponseEntity<Map<String,String>> health() {
        Map<String,String> map = new HashMap<>();
        log.info("---------------------> Health is Up");
        map.put("status", "UP");
        return ResponseEntity.ok(map);
    }
}
