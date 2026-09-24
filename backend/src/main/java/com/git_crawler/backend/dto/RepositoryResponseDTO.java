package com.git_crawler.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.git_crawler.backend.entity.IndexStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class RepositoryResponseDTO {
    UUID id;
    Long githubRepoId;
    String name;
    String description;
    String owner;
    String fullName;
    @JsonProperty("isPrivate") boolean isPrivate;
    String defaultBranch;
    String language;
    String htmlUrl;
    String errorMessage;
    IndexStatus indexStatus;
    Instant indexedAt;
    int chunkCount;
    int filesTotal;
    int filesProcessed;
}
