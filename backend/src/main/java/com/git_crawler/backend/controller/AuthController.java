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

import java.util.Map;

@RestController
@RequestMapping("/auth/")
@RequiredArgsConstructor
public class AuthController {
    private final CurrentUser currentUser;


    @GetMapping("login")
    public Map<String,String> loginUrl() {
        return Map.of("url", "/oauth2/authorization/github");
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me(){
        AppUserPrincipal loggedUser = currentUser.require();
        if(loggedUser == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = loggedUser.getUser();
        return  ResponseEntity.ok(new UserResponseDTO(
                user.getId(),
                user.getGithubId(),
                user.getGithubUsername(),
                user.getDisplayName(),
                user.getAvatarUrl()
        ));
    }
}
