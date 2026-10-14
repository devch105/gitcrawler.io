package com.git_crawler.backend.dto;


import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record CreateChatSessionRequest (
        @NotNull UUID repositoryId , String title
)

{ }
