package com.git_crawler.backend.services.github;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class GitHubRateLimiter {

    private final long delayMs;

    public GitHubRateLimiter(@Value("${app.github.api-delay-ms}") long delayMs) {
        this.delayMs = Math.max(delayMs, 0);
    }

    public void pause() throws InterruptedException {
        if(delayMs <=0){
            return;
        }
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(delayMs));
        }catch (InterruptedException e){
            log.error("Interrupted while sleeping in Rate Limiter");
            Thread.currentThread().interrupt();
        }
    }

}
