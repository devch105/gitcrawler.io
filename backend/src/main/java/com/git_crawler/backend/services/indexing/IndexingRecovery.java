package com.git_crawler.backend.services.indexing;

import com.git_crawler.backend.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Fixes repos stuck in INDEXING:
 *  - on startup: any job that was running before a restart is dead
 *    (assumes a single backend instance, which is how you run on Render)
 *  - every minute: jobs that have not reported progress for a while are failed
 *
 * A job that is failed from here stops on its next progress write,
 * because progress updates only apply while the repo is INDEXING.
 */
@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class IndexingRecovery {

    private static final String MSG_RESTART =
            "Indexing was interrupted by a server restart. Please re-index.";
    private static final String MSG_STALE =
            "Indexing stopped responding. Please re-index.";

    private final RepositoryRepository repoRepository;

    @Value("${app.indexing.stale-after-minutes:15}")
    private long staleAfterMinutes;

    @EventListener(ApplicationReadyEvent.class)
    public void failInterruptedJobs() {

        int updated = repoRepository.failAllIndexing(MSG_RESTART, Instant.now());

        if (updated > 0) {
            log.warn("Marked {} interrupted indexing job(s) as FAILED after startup", updated);
        }
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void failStaleJobs() {

        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofMinutes(staleAfterMinutes));

        int updated = repoRepository.failStaleIndexing(cutoff, MSG_STALE, now);

        if (updated > 0) {
            log.warn("Marked {} stale indexing job(s) as FAILED", updated);
        }
    }
}