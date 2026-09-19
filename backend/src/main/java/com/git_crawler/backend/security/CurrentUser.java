package com.git_crawler.backend.security;

import com.git_crawler.backend.exceptions.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public AppUserPrincipal require() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof AppUserPrincipal principal)) {

            throw new UnauthorizedException("Unauthorized");
        }

        return principal;
    }
}