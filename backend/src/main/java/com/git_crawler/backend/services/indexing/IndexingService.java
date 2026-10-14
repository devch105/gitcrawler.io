package com.git_crawler.backend.services.indexing;

import com.git_crawler.backend.entity.IndexStatus;
import com.git_crawler.backend.entity.Repository;
import com.git_crawler.backend.exceptions.BadRequestException;
import com.git_crawler.backend.exceptions.NotFoundException;
import com.git_crawler.backend.repository.RepositoryRepository;
import com.git_crawler.backend.services.UserService;
import com.git_crawler.backend.services.github.GitHubRateLimiter;
import com.git_crawler.backend.services.github.GithubApiClient;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

    private final RepositoryRepository repoRepository;
    private final UserService userService;
    private final GithubApiClient githubApiClient;
    private final CodeFileFilter codeFileFilter;
    private final CodeChunker codeChunker;
    private final GitHubRateLimiter gitHubRateLimiter;
    private final VectorStore vectorStore;

    private static final int VECTOR_BATCH_SIZE = 32;
    private static final int PROGRESS_EVERY_N_FILES = 5;

    @Value("${app.indexing.max-file-bytes:102400}")
    private long maxFileBytes;


    // =========================================================
    // START INDEXING
    // =========================================================

    public Repository startIndexing(UUID repoId, UUID userId) {

        Repository repo = repoRepository
                .findByIdAndUserId(repoId, userId)
                .orElseThrow(() ->
                        new NotFoundException("Repository Not Found")
                );

        if (repo.getIndexStatus() == IndexStatus.INDEXING) {
            throw new BadRequestException("Indexing is already started");
        }

        repo.setIndexStatus(IndexStatus.INDEXING);
        repo.setFilesProcessed(0);
        repo.setFilesTotal(0);
        repo.setChunkCount(0);
        repo.setErrorMessage(null);
        repo.setUpdatedAt(Instant.now());

        return repoRepository.save(repo);
    }


    // =========================================================
    // ASYNC INDEXING
    // =========================================================

    @Async("indexingExecutor")
    public void indexAsync(UUID repoId, UUID userId) {

        try {

            doIndex(repoId, userId);

        } catch (Exception e) {

            log.error(
                    "Error indexing repository {}",
                    repoId,
                    e
            );

            markFailed(
                    repoId,
                    e.getMessage()
            );
        }
    }


    // =========================================================
    // MAIN INDEXING FLOW
    // =========================================================

    private void doIndex(
            UUID repoId,
            UUID userId
    ) throws InterruptedException {

        Repository repo = repoRepository
                .findByIdAndUserId(repoId, userId)
                .orElseThrow(() ->
                        new NotFoundException("Repository Not Found")
                );

        String token = userService.decryptAccessToken(
                userService.requiredById(userId)
        );


        // -----------------------------------------------------
        // 1. Delete previous vectors
        // -----------------------------------------------------

        deleteExistingVectors(repoId.toString());


        // -----------------------------------------------------
        // 2. Get repository tree
        // -----------------------------------------------------

        Map<String, Object> tree =
                githubApiClient.getRepoTree(
                        token,
                        repo.getOwner(),
                        repo.getName(),
                        repo.getDefaultBranch()
                );


        // -----------------------------------------------------
        // 3. Find indexable files
        // -----------------------------------------------------

        List<String> filePaths =
                listIndexableFiles(tree);


        // -----------------------------------------------------
        // 4. Initialize progress
        // -----------------------------------------------------

        updateProgress(
                repoId,
                filePaths.size(),
                0,
                0,
                IndexStatus.INDEXING,
                null
        );


        List<Document> batch = new ArrayList<>();

        int processed = 0;
        int totalChunks = 0;


        // -----------------------------------------------------
        // 5. Process every file
        // -----------------------------------------------------

        for (String path : filePaths) {

            try {

                String content =
                        githubApiClient.getFileContent(
                                token,
                                repo.getOwner(),
                                repo.getName(),
                                path
                        );


                // Skip files that are too large
                if (content != null &&
                        content.getBytes().length > maxFileBytes) {

                    log.warn(
                            "Skipping large file {} in {}",
                            path,
                            repoId
                    );

                    processed++;
                    continue;
                }


                // Chunk file
                List<Document> chunks =
                        codeChunker.chunkFile(
                                repoId.toString(),
                                path,
                                content
                        );


                batch.addAll(chunks);

                totalChunks += chunks.size();


                // -------------------------------------------------
                // Save vectors in batches
                // -------------------------------------------------

                if (batch.size() >= VECTOR_BATCH_SIZE) {

                    vectorStore.add(batch);

                    batch.clear();
                }


            } catch (Exception e) {

                log.warn(
                        "Skipping file {} in {} : {}",
                        path,
                        repoId,
                        e.getMessage()
                );
            }


            processed++;


            // -----------------------------------------------------
            // Update progress
            // -----------------------------------------------------

            if (
                    processed % PROGRESS_EVERY_N_FILES == 0
                            || processed == filePaths.size()
            ) {

                updateProgress(
                        repoId,
                        filePaths.size(),
                        processed,
                        totalChunks,
                        IndexStatus.INDEXING,
                        null
                );
            }


            // GitHub rate limiting
            gitHubRateLimiter.pause();
        }


        // ---------------------------------------------------------
        // 6. Add remaining vectors
        // ---------------------------------------------------------

        if (!batch.isEmpty()) {

            vectorStore.add(batch);
        }


        // ---------------------------------------------------------
        // 7. Mark indexing as completed
        // ---------------------------------------------------------

        markReady(
                repoId,
                filePaths.size(),
                processed,
                totalChunks,
                repo.getFullName()
        );
    }


    // =========================================================
    // HELPER METHODS
    // =========================================================


    /**
     * Mark repository indexing as FAILED.
     */
    private void markFailed(
            UUID repoId,
            String errorMessage
    ) {

        try {

            Repository repo = repoRepository
                    .findById(repoId)
                    .orElse(null);

            if (repo == null) {
                log.error(
                        "Cannot mark repository {} as FAILED because it was not found",
                        repoId
                );
                return;
            }

            repo.setIndexStatus(IndexStatus.FAILED);
            repo.setErrorMessage(
                    errorMessage != null
                            ? errorMessage
                            : "Unknown indexing error"
            );
            repo.setUpdatedAt(Instant.now());

            repoRepository.save(repo);

            log.info(
                    "Repository {} marked as FAILED",
                    repoId
            );

        } catch (Exception e) {

            log.error(
                    "Failed to update repository {} status to FAILED",
                    repoId,
                    e
            );
        }
    }


    /**
     * Mark repository indexing as READY.
     */

    @Transactional
    private void markReady(
            UUID repoId,
            int filesTotal,
            int filesProcessed,
            int chunkCount,
            String repoFullName
    ) {

        Repository repo = repoRepository
                .findById(repoId)
                .orElseThrow(() ->
                        new NotFoundException("Repository Not Found")
                );

        repo.setIndexStatus(IndexStatus.DONE);
        repo.setIndexedAt(Instant.now());
        repo.setFilesTotal(filesTotal);
        repo.setFilesProcessed(filesProcessed);
        repo.setChunkCount(chunkCount);
        repo.setErrorMessage(null);
        repo.setUpdatedAt(Instant.now());


        repoRepository.save(repo);

        log.info(
                "Repository {} indexing completed successfully. " +
                        "Files: {}/{} | Chunks: {}",
                repoFullName,
                filesProcessed,
                filesTotal,
                chunkCount
        );
    }


    /**
     * Delete all existing vectors belonging to a repository.
     *
     * Vectors are identified using the repoId metadata that
     * CodeChunker adds to every Document.
     */
    private void deleteExistingVectors(String repoId) {

        try {

            String filterExpression =
                    "repoId == '" + repoId + "'";

            vectorStore.delete(filterExpression);

            log.info(
                    "Deleted existing vectors for repository {}",
                    repoId
            );

        } catch (Exception e) {

            log.error(
                    "Failed to delete existing vectors for repository {}",
                    repoId,
                    e
            );

            throw e;
        }
    }


    /**
     * Extract indexable files from GitHub repository tree.
     */
    private List<String> listIndexableFiles(
            Map<String, Object> tree
    ) {

        List<String> filePaths = new ArrayList<>();

        if (tree == null) {
            return filePaths;
        }

        Object treeObject = tree.get("tree");

        if (!(treeObject instanceof List<?> entries)) {
            return filePaths;
        }

        for (Object entry : entries) {

            if (!(entry instanceof Map<?, ?> file)) {
                continue;
            }

            Object type = file.get("type");
            Object path = file.get("path");

            // We only want actual files
            if (!"blob".equals(type)) {
                continue;
            }

            if (!(path instanceof String filePath)) {
                continue;
            }

            // Use CodeFileFilter
            if (!codeFileFilter.isAllowed(filePath)) {
                continue;
            }

            filePaths.add(filePath);
        }

        return filePaths;
    }


    /**
     * Update repository indexing progress.
     */
  @Transactional
    private void updateProgress(
            UUID repoId,
            int filesTotal,
            int filesProcessed,
            int chunkCount,
            IndexStatus status,
            String errorMessage
    ) {

        Repository repo = repoRepository
                .findById(repoId)
                .orElseThrow(() ->
                        new NotFoundException("Repository Not Found")
                );

        repo.setFilesTotal(filesTotal);
        repo.setFilesProcessed(filesProcessed);
        repo.setChunkCount(chunkCount);
        repo.setIndexStatus(status);
        repo.setErrorMessage(errorMessage);
        repo.setUpdatedAt(Instant.now());

        repoRepository.save(repo);

        log.debug(
                "Indexing progress for {}: {}/{} files, {} chunks",
                repoId,
                filesProcessed,
                filesTotal,
                chunkCount
        );
    }
}