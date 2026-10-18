package com.git_crawler.backend.controller;

import com.git_crawler.backend.dto.IndexedStatusResponseDTO;
import com.git_crawler.backend.dto.RepositoryResponseDTO;
import com.git_crawler.backend.entity.Repository;
import com.git_crawler.backend.security.CurrentUser;
import com.git_crawler.backend.services.RepoService;
import com.git_crawler.backend.services.indexing.IndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/repos")
public class RepoController {

    private final RepoService repoService;
    private final CurrentUser currentUser;
    private final IndexingService indexingService;

    @GetMapping
    public ResponseEntity<List<RepositoryResponseDTO>> listRepos() {
        UUID userId = currentUser.require().getId();

        if (repoService.needsInitialSync(userId)) {
            return ResponseEntity.ok(repoService.syncAndListRepo(userId)); // first time only
        }
        return ResponseEntity.ok(repoService.listRepos(userId));
    }

    @PostMapping("/sync")
    public ResponseEntity<List<RepositoryResponseDTO>> syncRepos() {
        UUID userId = currentUser.require().getId();
        return ResponseEntity.ok(repoService.syncAndListRepo(userId));
    }

    @GetMapping("/{id:[0-9a-fA-F-]{36}}")
    public ResponseEntity<RepositoryResponseDTO> getRepo(@PathVariable UUID id) {
        UUID userId = currentUser.require().getId();
        return  ResponseEntity.ok(repoService.getRepo(userId, id));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<IndexedStatusResponseDTO> getRepoStatus(@PathVariable UUID id) {
        UUID userId = currentUser.require().getId();
        return ResponseEntity.ok(repoService.status(userId, id));
    }

    @GetMapping("/{id}/tree")
    public ResponseEntity<String> getRepoTree(@PathVariable UUID id) {
        UUID userId = currentUser.require().getId();
        repoService.getRepoTree(userId, id);
        return ResponseEntity.ok("Check Your backend  logs ");
    }

    @PostMapping("/{id}/index")
    public ResponseEntity<RepositoryResponseDTO> startRepoIndexing(
            @PathVariable UUID id
    ) {
        UUID userId = currentUser.require().getId();

        log.info(
                "INDEX REQUEST -> repoId={}, userId={}",
                id,
                userId
        );

        Repository repo = indexingService.startIndexing(id, userId);

        try{
            indexingService.indexAsync(id, userId);
        }catch (TaskRejectedException e){
            indexingService.markFailed(id,"Server is Busy. please try again later");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Server is Busy. please try again later");
        }

        return ResponseEntity.ok(
                repoService.toResponseDTO(repo)
        );
    }

    @GetMapping("/recently-indexed")
    public ResponseEntity<List<RepositoryResponseDTO>> getRecentlyIndexedRepos(
            @RequestParam(defaultValue = "5") int limit
    ) {
        UUID userId = currentUser.require().getId();

        return ResponseEntity.ok(
                repoService.getRecentlyIndexedRepos(userId, limit)
        );
    }

  
}
