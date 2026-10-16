package com.git_crawler.backend.controller;

import com.git_crawler.backend.dto.UserResponseDTO;
import com.git_crawler.backend.entity.User;
import com.git_crawler.backend.security.AppUserPrincipal;
import com.git_crawler.backend.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final CurrentUser currentUser;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(
            HttpServletRequest request) {

        var session = request.getSession(false);

        System.out.println("=== AUTH ME REQUEST ===");
        System.out.println("Session exists: " + (session != null));
        System.out.println(
                "Session ID: " +
                        (session != null ? session.getId() : "none")
        );

        AppUserPrincipal loggedUser = currentUser.require();
        User user = loggedUser.getUser();

        System.out.println("Current user resolved successfully");
        System.out.println("========================");

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