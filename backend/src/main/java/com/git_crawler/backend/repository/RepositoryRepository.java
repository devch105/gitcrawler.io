package com.git_crawler.backend.repository;

import com.git_crawler.backend.entity.IndexStatus;
import com.git_crawler.backend.entity.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


// Imports needed:
   import org.springframework.data.jpa.repository.Modifying;
   import org.springframework.data.jpa.repository.Query;
   import org.springframework.data.repository.query.Param;
   import org.springframework.transaction.annotation.Transactional;
   import java.time.Instant;
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
    // Add these methods to your existing RepositoryRepository interface.

    /**
     * Atomically move a repo to INDEXING. Returns 0 if the repo is not owned by the
     * user, or if it is already INDEXING (so two clicks cannot start two jobs).
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Repository r
           set r.indexStatus = com.git_crawler.backend.entity.IndexStatus.INDEXING,
               r.filesProcessed = 0,
               r.filesTotal = 0,
               r.chunkCount = 0,
               r.errorMessage = null,
               r.updatedAt = :now
         where r.id = :id
           and r.userId = :userId
           and (r.indexStatus is null
                or r.indexStatus <> com.git_crawler.backend.entity.IndexStatus.INDEXING)
        """)
    int tryMarkIndexing(@Param("id") UUID id,
                        @Param("userId") UUID userId,
                        @Param("now") Instant now);

    /** Progress write. Returns 0 if the repo is no longer INDEXING (job should stop). */
    @Transactional
    @Modifying
    @Query("""
        update Repository r
           set r.filesTotal = :total,
               r.filesProcessed = :processed,
               r.chunkCount = :chunks,
               r.updatedAt = :now
         where r.id = :id
           and r.indexStatus = com.git_crawler.backend.entity.IndexStatus.INDEXING
        """)
    int updateProgress(@Param("id") UUID id,
                       @Param("total") int total,
                       @Param("processed") int processed,
                       @Param("chunks") int chunks,
                       @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("""
        update Repository r
           set r.indexStatus = com.git_crawler.backend.entity.IndexStatus.DONE,
               r.indexedAt = :now,
               r.filesTotal = :total,
               r.filesProcessed = :processed,
               r.chunkCount = :chunks,
               r.errorMessage = null,
               r.updatedAt = :now
         where r.id = :id
           and r.indexStatus = com.git_crawler.backend.entity.IndexStatus.INDEXING
        """)
    int markDone(@Param("id") UUID id,
                 @Param("total") int total,
                 @Param("processed") int processed,
                 @Param("chunks") int chunks,
                 @Param("now") Instant now);

    @Transactional
    @Modifying
    @Query("""
        update Repository r
           set r.indexStatus = com.git_crawler.backend.entity.IndexStatus.FAILED,
               r.errorMessage = :message,
               r.updatedAt = :now
         where r.id = :id
           and r.indexStatus = com.git_crawler.backend.entity.IndexStatus.INDEXING
        """)
    int markFailed(@Param("id") UUID id,
                   @Param("message") String message,
                   @Param("now") Instant now);

    /** Startup recovery: every job that was running when the server stopped is dead. */
    @Transactional
    @Modifying
    @Query("""
        update Repository r
           set r.indexStatus = com.git_crawler.backend.entity.IndexStatus.FAILED,
               r.errorMessage = :message,
               r.updatedAt = :now
         where r.indexStatus = com.git_crawler.backend.entity.IndexStatus.INDEXING
        """)
    int failAllIndexing(@Param("message") String message,
                        @Param("now") Instant now);

    /** Sweeper: jobs that stopped reporting progress (hung or lost). */
    @Transactional
    @Modifying
    @Query("""
        update Repository r
           set r.indexStatus = com.git_crawler.backend.entity.IndexStatus.FAILED,
               r.errorMessage = :message,
               r.updatedAt = :now
         where r.indexStatus = com.git_crawler.backend.entity.IndexStatus.INDEXING
           and r.updatedAt < :cutoff
        """)
    int failStaleIndexing(@Param("cutoff") Instant cutoff,
                          @Param("message") String message,
                          @Param("now") Instant now);
}