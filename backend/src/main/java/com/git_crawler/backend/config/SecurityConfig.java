package com.git_crawler.backend.config;

import com.git_crawler.backend.security.GithubOAuth2UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GithubOAuth2UserService githubOAuth2UserService;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationSuccessHandler oauth2SuccessHandler,
            AuthenticationFailureHandler oauth2FailureHandler
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())

                // REST API
                .csrf(csrf -> csrf.disable())

                // Authorization
                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers(
                                "/",
                                "/auth/**",
                                "/login/**",
                                "/oauth2/**",
                                "/error"
                        ).permitAll()

                        // CORS preflight
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // API requires authentication
                        .requestMatchers(
                                "/api/**"
                        ).authenticated()

                        // Everything else
                        .anyRequest().permitAll()
                )

                // Session-based OAuth2 authentication
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                // Return 401 instead of redirecting to login
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )
                )

                // GitHub OAuth2
                .oauth2Login(oauth2 -> oauth2

                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(
                                        githubOAuth2UserService
                                )
                        )

                        // Successful OAuth login
                        .successHandler(
                                oauth2SuccessHandler
                        )

                        // Failed OAuth login
                        .failureHandler(
                                oauth2FailureHandler
                        )
                )

                // Logout
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("GITCRAWLER_SESSION")
                );

        return http.build();
    }

    /*
     * Static @Bean methods prevent SecurityConfig itself from becoming
     * part of the dependency chain for these handlers.
     */

    @Bean
    public static AuthenticationSuccessHandler oauth2SuccessHandler(
            @Value("${app.frontend-url}") String frontendUrl
    ) {

        SimpleUrlAuthenticationSuccessHandler successHandler =
                new SimpleUrlAuthenticationSuccessHandler();

        successHandler.setDefaultTargetUrl(
                frontendUrl + "/auth/callback"
        );

        return successHandler;
    }

    @Bean
    public static AuthenticationFailureHandler oauth2FailureHandler(
            @Value("${app.frontend-url}") String frontendUrl
    ) {

        SimpleUrlAuthenticationFailureHandler failureHandler =
                new SimpleUrlAuthenticationFailureHandler();

        failureHandler.setDefaultFailureUrl(
                frontendUrl + "/login?error=oauth_failed"
        );

        return failureHandler;
    }
}