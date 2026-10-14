package com.git_crawler.backend.services;

import com.git_crawler.backend.dto.ChatMessageResponse;
import com.git_crawler.backend.dto.ChatSessionResponse;
import com.git_crawler.backend.dto.CreateChatSessionRequest;
import com.git_crawler.backend.entity.*;
import com.git_crawler.backend.exceptions.BadRequestException;
import com.git_crawler.backend.exceptions.NotFoundException;
import com.git_crawler.backend.repository.ChatMessageRepository;
import com.git_crawler.backend.repository.ChatSessionRepository;
import com.git_crawler.backend.services.ai.ChatPromptBuilder;
import com.git_crawler.backend.services.ai.ChatStreamHandler;
import com.git_crawler.backend.services.ai.CitationMapper;
import com.git_crawler.backend.services.ai.CodeContextRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.git_crawler.backend.dto.RecentChatResponse;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RepoService repoService;
    private final CodeContextRetriever  codeContextRetriever;
    private final ChatPromptBuilder  chatPromptBuilder;
    private final ChatStreamHandler chatStreamHandler;
    private final CitationMapper  citationMapper;

    @Transactional
    public ChatSessionResponse createSession(
            UUID userId,
            CreateChatSessionRequest request
    ) {
        Repository repo = repoService.requireOwned(
                userId,
                request.repositoryId()
        );

        if (repo.getIndexStatus() != IndexStatus.DONE) {
            throw new BadRequestException("Invalid repository");
        }

        String title = request.title() != null
                && !request.title().isBlank()
                ? request.title().trim()
                : "New Chat";

        ChatSession chatSession = ChatSession.builder()
                .userId(userId)
                .repositoryId(repo.getId())
                .title(title)
                .build();

        chatSession = chatSessionRepository.save(chatSession);

        return toSessionResponse(chatSession);
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> listSessions(UUID UserId , UUID repoId ){
        repoService.requireOwned(UserId,repoId);
        return chatSessionRepository.findByUserIdAndRepositoryIdOrderByCreatedAtDesc(UserId, repoId)
                .stream()
                .map(this :: toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(UUID userId , UUID sessionId){
        ChatSession session = requireSession(userId , sessionId);
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(this :: toMessageResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatSession requireSession(UUID userId , UUID sessionId){
        return chatSessionRepository.findByIdAndUserId(sessionId,userId).orElseThrow(()-> new NotFoundException("Chat session not found"));
    }


    public SseEmitter streamReply(UUID userId , UUID sessionId , String userContent){
         // 1. Ensure the session Exists
        ChatSession chatSession = requireSession(userId , sessionId);
        Repository repo = repoService.requireOwned(userId, chatSession.getRepositoryId());


        if (chatSession.getTitle() == null
                || chatSession.getTitle().isBlank()
                || chatSession.getTitle().equalsIgnoreCase("New Chat")) {

            chatSession.setTitle(generateTitle(userContent));

            chatSessionRepository.save(chatSession);
        }

        if(repo.getIndexStatus() != IndexStatus.DONE){
            throw new BadRequestException("Invalid repository");
        }

        //.2 Persist the User Message
        ChatMessage userMessage = chatMessageRepository.save(ChatMessage.builder()
                .sessionId(chatSession.getId())
                .role(MessageRole.USER)
                .content(userContent)
                .build());

        // 3. RAG Reterival find code chunk similar to this question

        var retrievedContext = codeContextRetriever.retrieve(repo.getId().toString(),userContent);


        // 4. BUILD LLM Prompts from retrieved context + question
        String systemPrompt = chatPromptBuilder.systemPrompt(repo.getFullName());
        String userPrompt = chatPromptBuilder.userPrompt(retrievedContext.contextText(),userContent);

        // 5. Stream OpenAI response to Client

        return chatStreamHandler.stream(
                chatSession.getId(),
                toMessageResponse(userMessage),
                retrievedContext.citation(),
                systemPrompt,
                userPrompt
        );
    }

    private ChatSessionResponse toSessionResponse(ChatSession session){
        return new ChatSessionResponse(
                session.getId(),
                session.getRepositoryId(),
                session.getTitle(),
                session.getCreatedAt()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage chatMessage){
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getRole(),
                chatMessage.getContent(),
                citationMapper.fromJson(chatMessage.getCitations()),
                chatMessage.getCreatedAt()
        );
    }


    private String generateTitle(String message) {
        String title = message
                .replaceAll("\\s+", " ")
                .trim();

        if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
        }

        return title;
    }


    @Transactional(readOnly = true)
    public List<RecentChatResponse> getRecentChats(UUID userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));

        return chatSessionRepository.findRecentChatsByUserId(
                userId,
                PageRequest.of(0, safeLimit)
        );
    }
}
