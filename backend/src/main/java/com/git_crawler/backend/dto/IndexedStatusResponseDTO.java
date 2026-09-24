package com.git_crawler.backend.dto;

import com.git_crawler.backend.entity.IndexStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@Builder
public class IndexedStatusResponseDTO {
    UUID repoId;
    IndexStatus indexStatus;
    int fileTotal;
    int filesProcessed;
    int chunkCount;
    Instant indexedAt;
    String errorMessage;
}
