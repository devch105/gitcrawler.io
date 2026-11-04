package com.git_crawler.backend.services.indexing;

import com.git_crawler.backend.dto.NotificationEvent;
import com.git_crawler.backend.entity.Repository;
import com.git_crawler.backend.entity.User;
import com.git_crawler.backend.repository.RepositoryRepository;
import com.git_crawler.backend.services.UserService;
import com.git_crawler.backend.services.notification.NotificationOutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingFinalizationService {

    private final RepositoryRepository repoRepository;
    private final UserService userService;
    private final NotificationOutboxService outboxService;

    @Transactional
    public boolean finalizeSuccess(
            UUID repoId,
            UUID userId,
            int total,
            int processed,
            int storedChunks
    ) {

        Repository repo = repoRepository
                .findByIdAndUserId(repoId, userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Repository not found during finalization"
                ));

        int updated = repoRepository.markDone(
                repoId,
                total,
                processed,
                storedChunks,
                Instant.now()
        );

        if (updated == 0) {
            log.warn("Repository {} was no longer INDEXING", repoId);
            return false;
        }

        enqueueNotification(
                userId,
                repo.getFullName(),
                "INDEXING_COMPLETED",
                "SUCCESS",
                null
        );

        log.info("Repository {} successfully finalized", repo.getFullName());

        return true;
    }

    @Transactional
    public boolean finalizeFailure(
            UUID repoId,
            UUID userId,
            String failureReason
    ) {

        Repository repo = repoRepository
                .findByIdAndUserId(repoId, userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Repository not found during failure finalization"
                ));

        int updated = repoRepository.markFailed(
                repoId,
                failureReason,
                Instant.now()
        );

        if (updated == 0) {
            log.warn(
                    "Repository {} was no longer INDEXING. Failure notification skipped.",
                    repoId
            );
            return false;
        }

        enqueueNotification(
                userId,
                repo.getFullName(),
                "INDEXING_FAILED",
                "FAILED",
                failureReason
        );

        log.info("Repository {} marked as FAILED", repo.getFullName());

        return true;
    }

    private void enqueueNotification(
            UUID userId,
            String repositoryName,
            String eventType,
            String status,
            String failureReason
    ) {

        User user = userService.requiredById(userId);

        String email = user.getNotificationEmail();

        if (email == null || email.isBlank()) {
            log.warn(
                    "Notification skipped for user {}: GitHub email unavailable",
                    userId
            );
            return;
        }

        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID(),
                eventType,
                userId,
                email,
                repositoryName,
                status,
                failureReason,
                Instant.now()
        );

        outboxService.enqueue(event);
    }
}