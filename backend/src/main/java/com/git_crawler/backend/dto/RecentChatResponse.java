package com.git_crawler.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record RecentChatResponse(
        UUID id,
        UUID repositoryId,
        String title,
        String repositoryFullName,
        Instant lastActivityAt
) {
}
