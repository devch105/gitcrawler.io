package com.git_crawler.backend.dto;

import com.git_crawler.backend.entity.MessageRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        MessageRole role,
        String content,
        List<CitationDTO> citations,
        Instant createdAt
) {
}
