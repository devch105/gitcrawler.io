package com.gitcrawler.notification_service.service;


import com.gitcrawler.notification_service.dto.ResendEmailRequestDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final WebClient.Builder webClientBuilder;

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${resend.mail.from}")
    private String fromEmail;

    @PostConstruct
    public void init() {
        System.out.println("Initializing email service!!");

        System.out.println("From email: " + fromEmail);
    }

    public void sendEmail(
            String to,
            String subject,
            String body
    ) {

        ResendEmailRequestDTO request =
                new ResendEmailRequestDTO();

        request.setFrom(fromEmail);
        request.setTo(List.of(to));
        request.setSubject(subject);
        request.setHtml(body);

        try {

            String response = webClientBuilder.build()
                    .post()
                    .uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            clientResponse ->
                                    clientResponse.bodyToMono(String.class)
                                            .flatMap(errorBody -> {
                                                log.error("Resend Error Body: {}", errorBody);
                                                return reactor.core.publisher.Mono.error(
                                                        new RuntimeException(errorBody)
                                                );
                                            })
                    )
                    .bodyToMono(String.class)
                    .block();

            log.info("Email sent successfully with Response  : {} : to User :  {} ", response, to);

        } catch (Exception e) {

            log.error("Failed to send email", e);
            throw e;
        }
    }
}