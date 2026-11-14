package com.git_crawler.backend.services;

import com.git_crawler.backend.dto.IndexedStatusResponseDTO;
import com.git_crawler.backend.dto.RepositoryResponseDTO;
import com.git_crawler.backend.entity.Repository;
import com.git_crawler.backend.entity.User;
import com.git_crawler.backend.exceptions.NotFoundException;
import com.git_crawler.backend.repository.RepositoryRepository;
import com.git_crawler.backend.services.github.GithubApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


import com.git_crawler.backend.entity.IndexStatus;
import org.springframework.data.domain.PageRequest;
@Service
@RequiredArgsConstructor
@Slf4j
public class RepoService {

    private final RepositoryRepository repositoryRepository;
    private final UserService userService;
    private final GithubApiClient githubApiClient;

    private static Long toLong(Object value) {
        if(value instanceof Integer) {
            return ((Integer) value).longValue();
        }else  if(value instanceof Long) {
            return (Long) value;
        }else {
            throw new IllegalArgumentException("Object is Not Integer : "+value);
        }
    }



    @Transactional
    public List<RepositoryResponseDTO> syncAndListRepo(UUID userId) {

        User user = userService.requiredById(userId);
        String token = userService.decryptAccessToken(user);

        // 1. Fetch all repositories from GitHub
        List<Map<String, Object>> githubRepos =
                githubApiClient.listUserRepos(token);

        log.info("Number of User Repos found :: {}", githubRepos.size());

        // 2. Collect GitHub repository IDs
        Set<Long> githubRepoIds = githubRepos.stream()
                .map(repo -> ((Number) repo.get("id")).longValue())
                .collect(Collectors.toSet());

        // 3. Save or update repositories
        List<Repository> saved = new ArrayList<>();

        for (Map<String, Object> remoteRepo : githubRepos) {

            Long githubRepoId =
                    ((Number) remoteRepo.get("id")).longValue();

            Map<String, Object> owner =
                    (Map<String, Object>) remoteRepo.get("owner");

            Repository savedRepo = repositoryRepository
                    .findByUserIdAndGithubRepoId(userId, githubRepoId)
                    .orElseGet(Repository::new);

            savedRepo.setUserId(userId);
            savedRepo.setGithubRepoId(githubRepoId);
            savedRepo.setName((String) remoteRepo.get("name"));
            savedRepo.setDescription((String) remoteRepo.get("description"));
            savedRepo.setOwner((String) owner.get("login"));
            savedRepo.setFullName((String) remoteRepo.get("full_name"));
            savedRepo.setPrivate(Boolean.TRUE.equals(remoteRepo.get("private")));
            savedRepo.setDefaultBranch((String) remoteRepo.get("default_branch"));
            savedRepo.setLanguage((String) remoteRepo.get("language"));
            savedRepo.setHtmlUrl((String) remoteRepo.get("html_url"));
            savedRepo.setCloneUrl((String) remoteRepo.get("clone_url"));

            // Reactivate if it was previously deactivated
            savedRepo.setActive(true);

            saved.add(repositoryRepository.save(savedRepo));
        }

        // 4. Deactivate repositories no longer returned by GitHub
        if (!githubRepoIds.isEmpty()) {

            int deactivated =
                    repositoryRepository.deactivateMissingRepositories(
                            userId,
                            githubRepoIds
                    );

            log.info(
                    "Deactivated {} repositories missing from GitHub",
                    deactivated
            );
        }

        // 5. Update last synchronization timestamp
        user.setReposSyncedAt(Instant.now());

        // 6. Return only active repositories
        return saved.stream()
                .sorted(Comparator.comparing(
                        Repository::getFullName,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean needsInitialSync(UUID userId) {
        return userService.requiredById(userId).getReposSyncedAt() == null;
    }

    @Transactional(readOnly = true)
    public List<RepositoryResponseDTO> listRepos(UUID userId) {
        return repositoryRepository
                .findByUserIdAndActiveTrueOrderByFullNameAsc(userId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Repository requireOwned(UUID userId , UUID repoId) {
        return  repositoryRepository.findByIdAndUserId(repoId, userId)
                .orElseThrow(() -> new NotFoundException("Repository not found"));
    }

    @Transactional(readOnly = true)
    public IndexedStatusResponseDTO status (UUID userId, UUID repoId){
        Repository repo = requireOwned(userId, repoId);
        return toIndexedStatusResponseDTO(repo);
    }

    public RepositoryResponseDTO getRepo(UUID userId, UUID repoId){
         Repository repo =  requireOwned(userId,repoId);
         return toResponseDTO(repo);
    }

    public void getRepoTree(UUID userId, UUID repoId){
        User user = userService.requiredById(userId);
        String token = userService.decryptAccessToken(user);
        Repository currentRepo = repositoryRepository.findByIdAndUserId(repoId,userId).orElseThrow(()-> new NotFoundException("Repository not found"));

        Map<String, Object> repoTree =
                githubApiClient.getRepoTree(token, currentRepo.getOwner(), currentRepo.getName(),currentRepo.getDefaultBranch());
        log.info("Repository full name ::: {}", currentRepo.getFullName());
        log.info("Repository description :::  {}", currentRepo.getDescription());
        log.info("Repository language ::: {}", currentRepo.getLanguage());
        log.info("Repository html_url  ::: {}", currentRepo.getHtmlUrl());
        log.info("Repository Tree :::  {}" , repoTree);

    }





    private IndexedStatusResponseDTO toIndexedStatusResponseDTO(Repository repo) {
        return  IndexedStatusResponseDTO.builder()
                .repoId(repo.getId())
                .indexStatus(repo.getIndexStatus())
                .fileTotal(repo.getFilesTotal())
                .filesProcessed(repo.getFilesProcessed())
                .chunkCount(repo.getChunkCount())
                .indexedAt(repo.getIndexedAt())
                .errorMessage(repo.getErrorMessage())
                .build();
    }


    @Transactional(readOnly = true)
    public List<RepositoryResponseDTO> getRecentlyIndexedRepos(
            UUID userId,
            int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 20));

        return repositoryRepository
                .findByUserIdAndIndexStatusAndIndexedAtIsNotNullOrderByIndexedAtDesc(
                        userId,
                        IndexStatus.DONE,
                        PageRequest.of(0, safeLimit)
                )
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }
    public RepositoryResponseDTO toResponseDTO(Repository repository) {

        return RepositoryResponseDTO.builder()
                .id(repository.getId())
                .githubRepoId(repository.getGithubRepoId())
                .name(repository.getName())
                .description(repository.getDescription())
                .owner(repository.getOwner())
                .fullName(repository.getFullName())
                .isPrivate(repository.isPrivate())
                .defaultBranch(repository.getDefaultBranch())
                .language(repository.getLanguage())
                .htmlUrl(repository.getHtmlUrl())
                .errorMessage(repository.getErrorMessage())
                .indexStatus(repository.getIndexStatus())
                .indexedAt(repository.getIndexedAt())
                .chunkCount(repository.getChunkCount())
                .filesTotal(repository.getFilesTotal())
                .filesProcessed(repository.getFilesProcessed())
                .build();
    }
}
