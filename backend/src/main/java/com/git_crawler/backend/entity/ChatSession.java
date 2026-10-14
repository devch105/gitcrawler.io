package com.git_crawler.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table( name = "chat_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "user_id")
    private UUID userId;

    @Column(nullable = false , name = "repository_id")
    private UUID repositoryId;

    @Column(nullable = false , length = 200)
    private String title;

    @Column(nullable = false , updatable = false , name= "created_at")
    private Instant createdAt;


    @PrePersist
    void onCreate() {
        if(createdAt == null) {
            createdAt = Instant.now();
        }
        if(title == null || title.isBlank()) {
            title = "New Chat";
        }
    }
}
