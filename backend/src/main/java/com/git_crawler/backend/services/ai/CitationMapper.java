package com.git_crawler.backend.services.ai;

import com.git_crawler.backend.dto.CitationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CitationMapper {

    private final ObjectMapper objectMapper;

    public CitationDTO fromDocument(Document document) {
        if (document == null) {
            throw new IllegalArgumentException(
                    "Document cannot be null"
            );
        }

        Map<String, Object> metadata = document.getMetadata();

        return new CitationDTO(
                stringValue(metadata.get("filePath")),
                integerValue(metadata.get("startLine")),
                integerValue(metadata.get("endLine")),
                stringValue(metadata.get("language"))
        );
    }

    public String toJson(List<CitationDTO> citations) {
        try {
            return objectMapper.writeValueAsString(
                    citations == null ? List.of() : citations
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to serialize citations",
                    exception
            );
        }
    }

    public List<CitationDTO> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            List<CitationDTO> citations = objectMapper.readValue(
                    json,
                    new TypeReference<List<CitationDTO>>() {}
            );

            return citations == null ? List.of() : citations;
        } catch (Exception exception) {
            log.warn("Failed to deserialize stored citations", exception);
            return List.of();
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value == null) {
            return null;
        }

        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
