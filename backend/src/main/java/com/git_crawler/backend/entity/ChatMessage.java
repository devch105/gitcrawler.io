package com.git_crawler.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id" , nullable = false)
    private  UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false , length = 20)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String citations;

    @Column(name = "created_at", nullable = false , updatable = false)
    private Instant createdAt;


    @PrePersist
    void  onCreate() {
        if(createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
