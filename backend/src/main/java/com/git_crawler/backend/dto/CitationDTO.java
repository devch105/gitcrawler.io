package com.git_crawler.backend.dto;

public record CitationDTO(
        String filePath,
        Integer startLine,
        Integer endLine,
        String language
){}
