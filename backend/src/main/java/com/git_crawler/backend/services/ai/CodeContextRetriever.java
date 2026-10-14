package com.git_crawler.backend.services.ai;

import com.git_crawler.backend.dto.CitationDTO;
import com.git_crawler.backend.dto.RetrievedCodeChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeContextRetriever {

    private final VectorStore vectorStore;
    private final CitationMapper citationMapper;

    /**
     * Retrieves the most relevant code chunks for a question
     * from a single repository.
     */
    public RetrievedCodeChunk retrieve(
            String repoId, String question) {

        if (repoId == null || repoId.isBlank()) {
            throw new IllegalArgumentException("Repository ID is required");
        }

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question is required");
        }

        int resultLimit = Math.max(1, Math.min(RagSettings.TOP_K_CHUNKS, 20));


        var filter = new FilterExpressionBuilder().eq(RagSettings.META_DATA_REPO_ID, repoId.toString()).build();

        var search = SearchRequest.builder()
                .query(question)
                .topK(RagSettings.TOP_K_CHUNKS)
                .filterExpression(filter)
                .build();
        var documents = vectorStore.similaritySearch(search);

        var citations = documents.stream()
                .map(citationMapper :: fromDocument)
                .distinct()
                .toList();

        var contextText = documents.stream()
                .map(Document :: getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        if(contextText.isBlank()){
            contextText = "NO MATCH FOUND";
        }
        return new RetrievedCodeChunk(contextText,citations);
    }

    private String escapeFilterValue(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }


}