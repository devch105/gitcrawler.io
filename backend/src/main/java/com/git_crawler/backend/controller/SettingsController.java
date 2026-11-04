package com.git_crawler.backend.controller;

import com.git_crawler.backend.dto.NotificationEmailResponse;
import com.git_crawler.backend.security.CurrentUser;
import com.git_crawler.backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserService userService;
    private final CurrentUser currentUser;
    @GetMapping("/email")
    public ResponseEntity<NotificationEmailResponse> getEmail() {

        var user = currentUser.require();
        NotificationEmailResponse res = userService.getEmail(user.getId());

        return ResponseEntity.ok().body(res);
    }

    @PutMapping("/email")
    public ResponseEntity<MessageResponse> updateEmail(
            @RequestBody UpdateEmailRequest request
    ) {
        var user = currentUser.require();

        userService.addEmail(
                user.getId(),
                request.email()
        );

        return ResponseEntity.ok(
                new MessageResponse("Email verification initiated")
        );
    }

    public record UpdateEmailRequest(String email) {}
    public record MessageResponse(String message) {}
}