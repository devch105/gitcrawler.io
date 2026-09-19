package com.git_crawler.backend.repository;

import com.git_crawler.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Override
    Optional<User> findById(UUID id);
    
    Optional<User> findByGithubId(Long githubId);
}
