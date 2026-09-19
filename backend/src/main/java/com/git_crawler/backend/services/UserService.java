package com.git_crawler.backend.services;

import com.git_crawler.backend.entity.User;
import com.git_crawler.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TextEncryptor tokenEncryptor;


    @Transactional(readOnly = true)
    public User requiredById(UUID id) {
        return userRepository.findById(id).orElseThrow(()-> new RuntimeException("User not found"));
    }

    public String decryptAccessToken(User user){
        return  tokenEncryptor.decrypt(user.getAccessToken());
    }

    private static Long toLong(Object value){
        if(value instanceof Long) {
            return (Long) value;
        }
        return Long.parseLong(value.toString());
    }


    @Transactional
    public User upsertFromGithub(
            Map<String, Object> attributes,
            String accessToken,
            String scopes
    ) {

        // GitHub ID
        Long githubId = ((Number) attributes.get("id")).longValue();

        // GitHub username
        String githubUsername = (String) attributes.get("login");

        // GitHub email
        String githubEmail = (String) attributes.get("email");

        // GitHub display name
        String displayName = (String) attributes.get("name");

        // GitHub avatar
        String avatarUrl = (String) attributes.get("avatar_url");


        // Find existing user
        User user = userRepository
                .findByGithubId(githubId)
                .orElseGet(User::new);


        // Update GitHub information
        user.setGithubId(githubId);
        user.setGithubUsername(githubUsername);
        user.setGithubEmail(githubEmail);
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);


        // Encrypt access token before storing it
        user.setAccessToken(
                tokenEncryptor.encrypt(accessToken)
        );


        // Store OAuth scopes
        user.setTokenScopes(scopes);


        return userRepository.save(user);
    }


}
