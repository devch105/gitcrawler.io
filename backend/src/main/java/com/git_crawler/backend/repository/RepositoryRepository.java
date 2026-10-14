package com.git_crawler.backend.repository;

import com.git_crawler.backend.entity.IndexStatus;
import com.git_crawler.backend.entity.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryRepository extends JpaRepository<Repository, UUID> {

    List<Repository> findByUserIdOrderByFullNameAsc(UUID userId);

    Optional<Repository> findByIdAndUserId(UUID id, UUID userId);

    Optional<Repository> findByUserIdAndGithubRepoId(
            UUID userId,
            Long githubRepoId
    );

    List<Repository> findByUserIdAndIndexStatusOrderByIndexedAtDesc(
            UUID userId,
            IndexStatus indexStatus,
            Pageable pageable
    );

    List<Repository> findByUserIdAndIndexStatusAndIndexedAtIsNotNullOrderByIndexedAtDesc(
            UUID userId,
            IndexStatus indexStatus,
            Pageable pageable
    );
}