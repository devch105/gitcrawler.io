package com.git_crawler.backend.repository;

import com.git_crawler.backend.dto.RecentChatResponse;
import com.git_crawler.backend.entity.ChatSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepository
        extends JpaRepository<ChatSession, UUID> {

    List<ChatSession> findByUserIdAndRepositoryIdOrderByCreatedAtDesc(
            UUID userId,
            UUID repositoryId
    );

    Optional<ChatSession> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
        SELECT new com.git_crawler.backend.dto.RecentChatResponse(
            s.id,
            s.repositoryId,
            s.title,
            r.fullName,
            COALESCE(
                (SELECT MAX(m.createdAt)
                 FROM ChatMessage m
                 WHERE m.sessionId = s.id),
                s.createdAt
            )
        )
        FROM ChatSession s
        JOIN Repository r ON r.id = s.repositoryId
        WHERE s.userId = :userId
        ORDER BY COALESCE(
            (SELECT MAX(m.createdAt)
             FROM ChatMessage m
             WHERE m.sessionId = s.id),
            s.createdAt
        ) DESC
        """)
    List<RecentChatResponse> findRecentChatsByUserId(
            @Param("userId") UUID userId,
            Pageable pageable
    );
}