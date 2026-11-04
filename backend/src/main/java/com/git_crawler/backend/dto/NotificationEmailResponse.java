package com.git_crawler.backend.dto;


public record NotificationEmailResponse(
        String notificationEmail,
        boolean notificationEmailVerified
) {}
