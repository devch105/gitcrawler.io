package com.git_crawler.backend.controller;

import com.git_crawler.backend.dto.*;
import com.git_crawler.backend.security.CurrentUser;
import com.git_crawler.backend.services.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CurrentUser currentUser;

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponse> createSession(
            @Valid @RequestBody CreateChatSessionRequest request
    ) {
        UUID userId = currentUser.require().getId();

        return ResponseEntity.ok(
                chatService.createSession(userId, request)
        );
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> getSessions(
            @RequestParam UUID repositoryId
    ) {
        UUID userId = currentUser.require().getId();

        return ResponseEntity.ok(
                chatService.listSessions(userId, repositoryId)
        );
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable UUID id
    ) {
        UUID userId = currentUser.require().getId();

        return ResponseEntity.ok(
                chatService.listMessages(userId, id)
        );
    }



    @GetMapping("/sessions/recent")
    public ResponseEntity<List<RecentChatResponse>> getRecentChats(
            @RequestParam(defaultValue = "5") int limit
    ) {
        UUID userId = currentUser.require().getId();

        return ResponseEntity.ok(
                chatService.getRecentChats(userId, limit)
        );
    }

    @PostMapping(
            value = "/sessions/{id}/messages",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter sendMessage(
            @PathVariable UUID id,
            @Valid @RequestBody ChatMessageRequest request
    ) {
        UUID userId = currentUser.require().getId();

        return chatService.streamReply(
                userId,
                id,
                request.content()
        );
    }
}
