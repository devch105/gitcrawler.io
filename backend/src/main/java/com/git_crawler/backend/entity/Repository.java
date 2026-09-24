package com.git_crawler.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "repositories",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_repository_user_github_repo",
                        columnNames = {"user_id", "github_repo_id"}
                )
        },
        indexes = {
                @Index(name = "idx_repository_user_id", columnList = "user_id"),
                @Index(name = "idx_repository_github_repo_id", columnList = "github_repo_id"),
                @Index(name = "idx_repository_index_status", columnList = "index_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    // -------------------------
    // User relationship
    // -------------------------


    @Column(name = "user_id", nullable = false)
    private UUID userId;



    // -------------------------
    // GitHub information
    // -------------------------

    @Column(
            name = "github_repo_id",
            nullable = false
    )
    private Long githubRepoId;


    @Column(
            name = "owner",
            nullable = false,
            length = 100
    )
    private String owner;


    @Column(
            name = "name",
            nullable = false,
            length = 255
    )
    private String name;


    @Column(
            name = "full_name",
            nullable = false,
            length = 511
    )
    private String fullName;


    @Column(
            name = "description",
            columnDefinition = "TEXT"
    )
    private String description;


    @Column(
            name = "is_private",
            nullable = false
    )
    private boolean isPrivate;


    @Column(
            name = "default_branch",
            nullable = false,
            length = 255
    )
    private String defaultBranch;


    @Column(
            name = "language",
            length = 100
    )
    private String language;


    @Column(
            name = "html_url",
            nullable = false,
            length = 500
    )
    private String htmlUrl;


    @Column(
            name = "clone_url",

            length = 500
    )
    private String cloneUrl;


    // -------------------------
    // Indexing information
    // -------------------------

    @Enumerated(EnumType.STRING)
    @Column(
            name = "index_status",
            nullable = false,
            length = 30
    )
    @Builder.Default
    private IndexStatus indexStatus = IndexStatus.PENDING;


    @Column(
            name = "chunk_count",
            nullable = false
    )
    @Builder.Default
    private int chunkCount = 0;


    @Column(
            name = "files_processed",
            nullable = false
    )
    @Builder.Default
    private int filesProcessed = 0;


    @Column(
            name = "files_total",
            nullable = false
    )
    @Builder.Default
    private int filesTotal = 0;


    @Column(
            name = "error_message",
            columnDefinition = "TEXT"
    )
    private String errorMessage;


    // -------------------------
    // Timestamps
    // -------------------------

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;


    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (indexStatus == null) {
            indexStatus = IndexStatus.PENDING;
        }

        if (chunkCount < 0) {
            chunkCount = 0;
        }

        if (filesProcessed < 0) {
            filesProcessed = 0;
        }

        if (filesTotal < 0) {
            filesTotal = 0;
        }
    }


    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}