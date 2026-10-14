package com.git_crawler.backend.dto;

import java.util.List;

public record RetrievedCodeChunk(
        String contextText,
        List<CitationDTO> citation
) {}
