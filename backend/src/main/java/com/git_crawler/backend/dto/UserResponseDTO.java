package com.git_crawler.backend.dto;

import lombok.*;

import java.util.UUID;

@Data

@AllArgsConstructor

@Builder
public class UserResponseDTO {

    private UUID id;
    private Long githubId;
    private String githubusername;
    private String displayName;
    private String avatarUrl;
}
