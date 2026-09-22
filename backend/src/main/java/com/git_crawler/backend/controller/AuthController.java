package com.git_crawler.backend.controller;

import com.git_crawler.backend.dto.UserResponseDTO;
import com.git_crawler.backend.entity.User;
import com.git_crawler.backend.security.AppUserPrincipal;
import com.git_crawler.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CurrentUser currentUser;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me() {

        AppUserPrincipal loggedUser =
                currentUser.require();

        User user = loggedUser.getUser();

        return ResponseEntity.ok(
                new UserResponseDTO(
                        user.getId(),
                        user.getGithubId(),
                        user.getGithubUsername(),
                        user.getDisplayName(),
                        user.getAvatarUrl()
                )
        );
    }
}