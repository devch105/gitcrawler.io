package com.git_crawler.backend.controller;

import com.git_crawler.backend.dto.IndexedStatusResponseDTO;
import com.git_crawler.backend.dto.RepositoryResponseDTO;
import com.git_crawler.backend.security.CurrentUser;
import com.git_crawler.backend.services.RepoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController

@RequiredArgsConstructor
@RequestMapping("/api/repos")
public class RepoController {

    private final RepoService repoService;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<List<RepositoryResponseDTO>> listRepos(@RequestParam(name = "refresh", defaultValue = "true")
                                                                 boolean refresh) {
        UUID userId = currentUser.require().getId();

        if(refresh) {
            return ResponseEntity.ok(repoService.syncAndListRepo(userId));
        }
        return ResponseEntity.ok(repoService.listRepos(userId));

    }

    @GetMapping("/{id}")
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
}
