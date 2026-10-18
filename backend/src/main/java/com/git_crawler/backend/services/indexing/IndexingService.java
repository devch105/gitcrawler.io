package com.git_crawler.backend.services.indexing;

import com.git_crawler.backend.entity.Repository;
import com.git_crawler.backend.exceptions.BadRequestException;
import com.git_crawler.backend.exceptions.GithubTokenInvalidException;
import com.git_crawler.backend.exceptions.NotFoundException;
import com.git_crawler.backend.repository.RepositoryRepository;
import com.git_crawler.backend.services.UserService;
import com.git_crawler.backend.services.github.GitHubRateLimiter;
import com.git_crawler.backend.services.github.GithubApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Indexing lifecycle:
 *
 *   (PENDING | DONE | FAILED) --startIndexing--> INDEXING --> DONE
 *                                                        \--> FAILED
 *
 * Every state change is a single conditional UPDATE ("... where status = INDEXING"),
 * so two jobs cannot start for the same repo, and a job that was failed from the
 * outside (restart recovery, stale sweeper) notices on its next progress write
 * and stops instead of overwriting the state.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

    private static final int VECTOR_BATCH_SIZE = 32;
    private static final int PROGRESS_EVERY_N_FILES = 5;
    private static final double MAX_FAILED_FILE_RATIO = 0.5;

    // Messages stored on the repo and shown to the user. Never put raw exception text here.
    static final String MSG_GENERIC = "Indexing failed. Please try again.";
    static final String MSG_TOKEN = "GitHub connection expired. Reconnect GitHub, then re-index.";
    static final String MSG_INTERRUPTED = "Indexing was interrupted. Please try again.";
    static final String MSG_FILE_ERRORS = "Most files could not be fetched from GitHub. Please try again.";

    private final RepositoryRepository repoRepository;
    private final UserService userService;
    private final GithubApiClient githubApiClient;
    private final CodeFileFilter codeFileFilter;
    private final CodeChunker codeChunker;
    private final GitHubRateLimiter gitHubRateLimiter;
    private final VectorStore vectorStore;

    @Value("${app.indexing.max-file-bytes:102400}")
    private long maxFileBytes;


    // =========================================================
    // START INDEXING (atomic)
    // =========================================================

    @Transactional
    public Repository startIndexing(UUID repoId, UUID userId) {

        int updated = repoRepository.tryMarkIndexing(repoId, userId, Instant.now());

        if (updated == 0) {
            // Zero rows: either the repo is not this user's, or a job is already running.
            repoRepository.findByIdAndUserId(repoId, userId)
                    .orElseThrow(() -> new NotFoundException("Repository Not Found"));

            throw new BadRequestException("Indexing is already in progress");
        }

        return repoRepository.findByIdAndUserId(repoId, userId)
                .orElseThrow(() -> new NotFoundException("Repository Not Found"));
    }


    // =========================================================
    // ASYNC INDEXING
    // =========================================================

    @Async("indexingExecutor")
    public void indexAsync(UUID repoId, UUID userId) {

        try {

            doIndex(repoId, userId);

        } catch (IndexingAbortedException e) {

            // Someone else already moved this repo out of INDEXING. Leave the state alone.
            log.warn("Indexing of repository {} stopped: {}", repoId, e.getMessage());

        } catch (IndexingFailedException e) {

            log.warn("Indexing of repository {} failed: {}", repoId, e.getMessage());
            markFailed(repoId, e.getMessage());

        } catch (GithubTokenInvalidException e) {

            log.warn("GitHub token rejected while indexing repository {}", repoId);
            markFailed(repoId, MSG_TOKEN);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            log.warn("Indexing of repository {} was interrupted", repoId);
            markFailed(repoId, MSG_INTERRUPTED);

        } catch (Exception e) {

            log.error("Error indexing repository {}", repoId, e);
            markFailed(repoId, MSG_GENERIC);
        }
    }


    // =========================================================
    // MAIN INDEXING FLOW
    // =========================================================

    private void doIndex(UUID repoId, UUID userId) throws InterruptedException {

        Repository repo = repoRepository
                .findByIdAndUserId(repoId, userId)
                .orElseThrow(() -> new NotFoundException("Repository Not Found"));

        String token = userService.decryptAccessToken(
                userService.requiredById(userId)
        );

        // 1. Remove vectors from any previous run
        deleteExistingVectors(repoId);

        // 2. Repository tree -> indexable files
        Map<String, Object> tree = githubApiClient.getRepoTree(
                token,
                repo.getOwner(),
                repo.getName(),
                repo.getDefaultBranch()
        );

        List<String> filePaths = listIndexableFiles(tree);
        int total = filePaths.size();

        reportProgress(repoId, total, 0, 0);

        // 3. Process files
        List<Document> batch = new ArrayList<>();
        int processed = 0;
        int storedChunks = 0;
        int failedFiles = 0;

        for (String path : filePaths) {

            // Only fetching/chunking errors are skippable per file.
            // A revoked token must fail the whole job, not be skipped file by file.
            try {

                String content = githubApiClient.getFileContent(
                        token,
                        repo.getOwner(),
                        repo.getName(),
                        path
                );

                if (content != null) {
                    if (content.getBytes(StandardCharsets.UTF_8).length <= maxFileBytes) {
                        batch.addAll(codeChunker.chunkFile(repoId.toString(), path, content));
                    } else {
                        log.debug("Skipping large file {} in {}", path, repoId);
                    }
                }

            } catch (GithubTokenInvalidException e) {

                throw e;

            } catch (Exception e) {

                failedFiles++;
                log.warn("Skipping file {} in {}: {}", path, repoId, e.getMessage());
            }

            processed++;

            // Outside the try: embedding or database failures must fail the job,
            // otherwise it would finish as DONE with missing vectors.
            if (batch.size() >= VECTOR_BATCH_SIZE) {
                storedChunks += flush(batch);
            }

            if (processed % PROGRESS_EVERY_N_FILES == 0 || processed == total) {
                reportProgress(repoId, total, processed, storedChunks);
            }

            gitHubRateLimiter.pause();
        }

        storedChunks += flush(batch);

        // If most files could not be fetched, this is not a success
        if (total > 0 && failedFiles > total * MAX_FAILED_FILE_RATIO) {
            throw new IndexingFailedException(MSG_FILE_ERRORS);
        }

        // 4. Complete
        int updated = repoRepository.markDone(
                repoId, total, processed, storedChunks, Instant.now()
        );

        if (updated == 0) {
            throw new IndexingAbortedException(repoId);
        }

        log.info(
                "Repository {} indexed. Files: {}/{} | Chunks: {} | Skipped: {}",
                repo.getFullName(), processed, total, storedChunks, failedFiles
        );
    }


    // =========================================================
    // STATE HELPERS
    // =========================================================

    /**
     * Mark a repository as FAILED. Public so the controller can call it when
     * the executor rejects a job. Only affects repos that are still INDEXING.
     */
    public void markFailed(UUID repoId, String userMessage) {

        try {

            int updated = repoRepository.markFailed(repoId, userMessage, Instant.now());

            if (updated > 0) {
                log.info("Repository {} marked as FAILED", repoId);
            }

        } catch (Exception e) {

            log.error("Failed to mark repository {} as FAILED", repoId, e);
        }
    }

    /**
     * Writes progress. Returns normally only if the repo is still INDEXING;
     * otherwise the job was cancelled or failed from the outside, so stop.
     */
    private void reportProgress(UUID repoId, int total, int processed, int chunks) {

        int updated = repoRepository.updateProgress(
                repoId, total, processed, chunks, Instant.now()
        );

        if (updated == 0) {
            throw new IndexingAbortedException(repoId);
        }
    }

    private int flush(List<Document> batch) {

        if (batch.isEmpty()) {
            return 0;
        }

        int size = batch.size();
        vectorStore.add(batch);
        batch.clear();

        return size;
    }


    // =========================================================
    // VECTORS AND FILE LISTING
    // =========================================================

    /**
     * Deletes all vectors for a repository. repoId is a UUID, so it cannot
     * carry quotes or filter syntax into the expression.
     */
    private void deleteExistingVectors(UUID repoId) {

        vectorStore.delete("repoId == '" + repoId + "'");

        log.info("Deleted existing vectors for repository {}", repoId);
    }

    private List<String> listIndexableFiles(Map<String, Object> tree) {

        List<String> filePaths = new ArrayList<>();

        if (tree == null) {
            return filePaths;
        }

        if (Boolean.TRUE.equals(tree.get("truncated"))) {
            log.warn("GitHub returned a truncated tree; only part of the repository will be indexed");
        }

        if (!(tree.get("tree") instanceof List<?> entries)) {
            return filePaths;
        }

        for (Object entry : entries) {

            if (!(entry instanceof Map<?, ?> file)) {
                continue;
            }

            if (!"blob".equals(file.get("type"))) {
                continue;
            }

            if (!(file.get("path") instanceof String filePath)) {
                continue;
            }

            // Skip oversized files before spending an API call on them
            if (file.get("size") instanceof Number size && size.longValue() > maxFileBytes) {
                continue;
            }

            if (!codeFileFilter.isAllowed(filePath)) {
                continue;
            }

            filePaths.add(filePath);
        }

        return filePaths;
    }


    // =========================================================
    // INTERNAL EXCEPTIONS
    // =========================================================

    /** The repo left INDEXING behind this job's back. */
    private static final class IndexingAbortedException extends RuntimeException {
        IndexingAbortedException(UUID repoId) {
            super("state of repository " + repoId + " was changed externally");
        }
    }

    /** A failure whose message is safe to show to the user. */
    private static final class IndexingFailedException extends RuntimeException {
        IndexingFailedException(String userMessage) {
            super(userMessage);
        }
    }
}