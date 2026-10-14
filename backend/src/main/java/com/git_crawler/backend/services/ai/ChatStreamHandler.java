package com.git_crawler.backend.services.ai;

import com.git_crawler.backend.dto.ChatMessageResponse;
import com.git_crawler.backend.dto.CitationDTO;
import com.git_crawler.backend.entity.ChatMessage;
import com.git_crawler.backend.entity.MessageRole;
import com.git_crawler.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatStreamHandler {
    private  final ChatModel chatModel;
    private final ChatMessageRepository chatMessageRepository;
    private final CitationMapper citationMapper;


    public SseEmitter stream(
            UUID sessionId,
            ChatMessageResponse savedUserMessage,
            List<CitationDTO> citations,
            String SystemPromt,
            String userPrompt
    ){
        SseEmitter emitter = new SseEmitter(RagSettings.STREAM_TIMEOUT_MS);

        StringBuilder fullReply = new StringBuilder();
        try{
            emitter.send(SseEmitter.event()
                    .name("user_message")
                    .data(savedUserMessage));

            ChatClient.builder(chatModel)
                    .build()
                    .prompt()
                    .system(SystemPromt)
                    .user(userPrompt)
                    .stream()
                    .content()
                    .doOnNext(token -> appendToken(emitter,fullReply,token))
                    .doOnError(err -> {
                        log.error("Chat Stream Error : ", err.getMessage());
                    })
                    .doOnComplete(()-> completeStream(emitter,sessionId, fullReply,citations))
                    .subscribe();
        }catch (Exception ex){
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    private void completeStream(SseEmitter emitter, UUID sessionId, StringBuilder fullReply, List<CitationDTO> citations) {
        try{
            ChatMessage assistant = chatMessageRepository.save(ChatMessage.builder()
                    .sessionId(sessionId)
                    .role(MessageRole.ASSISTANT)
                    .content(fullReply.toString())
                    .citations(citationMapper.toJson(citations))
                    .build());

            emitter.send(SseEmitter.event()
                    .name("assistant_message")
                    .data(toMessageResponse(assistant)));
            emitter.send(SseEmitter.event()
                    .name("done").data("[DONE}"));
            emitter.complete();
        }catch (Exception e){
            log.error("Chat Stream Error : ");
            emitter.completeWithError(e);
        }
    }

    private  ChatMessageResponse toMessageResponse(ChatMessage message) {
        return  new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                citationMapper.fromJson(message.getCitations()),
                message.getCreatedAt()
        );
    }

    private  void appendToken(SseEmitter emitter,StringBuilder fullReply, String token){
        fullReply.append(token);
        try{
            emitter.send(SseEmitter.event()
                    .name("token")
                    .data(token, MediaType.APPLICATION_JSON));
        }catch (Exception ex){
            log.error("Errorn in appending Token : ");
            throw new IllegalStateException(ex.getMessage());
        }
    }


}
