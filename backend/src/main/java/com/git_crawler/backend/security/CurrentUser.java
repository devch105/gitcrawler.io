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

        System.out.println("=== CURRENT USER DEBUG ===");
        System.out.println("Authentication: " + authentication);

        if (authentication != null) {
            System.out.println(
                    "Authentication class: "
                            + authentication.getClass().getName()
            );

            System.out.println(
                    "Principal: "
                            + authentication.getPrincipal()
            );

            System.out.println(
                    "Principal class: "
                            + authentication.getPrincipal()
                            .getClass().getName()
            );

            System.out.println(
                    "Authenticated: "
                            + authentication.isAuthenticated()
            );
        }

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof AppUserPrincipal principal)) {

            throw new UnauthorizedException("Unauthorized");
        }

        return principal;
    }
}