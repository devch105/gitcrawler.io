package com.git_crawler.backend.services.indexing;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CodeChunker {

    private final TokenTextSplitter splitter;
    private final CodeFileFilter fileFilter;

    public CodeChunker(
            @Value("${app.indexing.chunk-size:800}") int chunkSize,
            CodeFileFilter fileFilter
    ) {

        int chunkTokens = Math.max(50, chunkSize / 4);

        this.splitter = TokenTextSplitter.builder()
                .withChunkSize(chunkTokens)
                .build();

        this.fileFilter = fileFilter;
    }

    public List<Document> chunkFile(
            String repoId,
            String filePath,
            String content
    ) {

        // Ignore empty files
        if (content == null || content.isBlank()) {
            return List.of();
        }

        // Ignore unsupported file types
        if (!fileFilter.isAllowed(filePath)) {
            return List.of();
        }

        String language = detectLanguage(filePath);

        // Create the initial document
        Document document = new Document(content);

        // Split into chunks
        List<Document> chunks = splitter.apply(List.of(document));

        // Add metadata to every chunk
        for (int i = 0; i < chunks.size(); i++) {

            Document chunk = chunks.get(i);

            chunk.getMetadata().put("repoId", repoId);
            chunk.getMetadata().put("filePath", filePath);
            chunk.getMetadata().put("language", language);
            chunk.getMetadata().put("chunkIndex", i);
            chunk.getMetadata().put("totalChunks", chunks.size());
        }

        return chunks;
    }

    private String detectLanguage(String filePath) {

        String extension = getExtension(filePath);

        return switch (extension) {

            case ".java" -> "java";
            case ".py" -> "python";
            case ".go" -> "go";

            case ".js" -> "javascript";
            case ".jsx" -> "javascript";
            case ".ts" -> "typescript";
            case ".tsx" -> "typescript";

            case ".html" -> "html";
            case ".css" -> "css";
            case ".xml" -> "xml";

            case ".yml", ".yaml" -> "yaml";
            case ".json" -> "json";

            default -> "unknown";
        };
    }

    private String getExtension(String filePath) {

        if (filePath == null || !filePath.contains(".")) {
            return "";
        }

        return filePath.substring(
                filePath.lastIndexOf('.')
        ).toLowerCase();
    }

    private static Document withChunkIndex(
            Document chunk,
            String repoId,
            String filePath,
            String language,
            int chunkIndex
    ){
        Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
        metadata.put("repoId", repoId);
        metadata.put("filePath", filePath);
        metadata.put("language", language);
        metadata.put("chunkIndex", chunkIndex);

        return new Document(chunk.getText(), metadata);
    }
}