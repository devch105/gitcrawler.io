package com.git_crawler.backend.services.github;

import com.git_crawler.backend.exceptions.GithubTokenInvalidException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class GithubApiClient {

    private static final String API_BASE = "https://api.github.com";
    private static final String API_VERSION = "2026-03-10";
    private static final int PER_PAGE = 100;
    private static final int MAX_PAGES = 10;

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST_MAP =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> MAP =
            new ParameterizedTypeReference<>() {};

    /**
     * One client for all users. It carries NO token: the Authorization header is
     * added per request, so one user's token can never leak into another user's call.
     */
    private final RestClient client;

    public GithubApiClient(RestClient.Builder builder) {

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build()
        );
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        // clone(): never mutate the injected builder
        this.client = builder.clone()
                .baseUrl(API_BASE)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", API_VERSION)
                .defaultHeader(HttpHeaders.USER_AGENT, "GitCrawler")
                // GitHub answers 401 when the token was revoked or expired.
                // Do NOT map 403 here: GitHub uses it for rate limits.
                .defaultStatusHandler(
                        status -> status.value() == 401,
                        (request, response) -> {
                            throw new GithubTokenInvalidException();
                        })
                .build();
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    public List<Map<String, Object>> listUserRepos(String accessToken) {

        List<Map<String, Object>> all = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++) {

            final int currentPage = page;

            List<Map<String, Object>> pageRepos = client
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/user/repos")
                            .queryParam("affiliation", "owner,collaborator,organization_member")
                            .queryParam("sort", "updated")
                            .queryParam("per_page", PER_PAGE)
                            .queryParam("page", currentPage)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .retrieve()
                    .body(LIST_MAP);

            if (pageRepos == null || pageRepos.isEmpty()) {
                break;
            }

            all.addAll(pageRepos);

            if (pageRepos.size() < PER_PAGE) {
                break;
            }
        }

        return all;
    }

    public String getFileContent(String accessToken, String owner, String repo, String path) {

        Map<String, Object> body = client
                .get()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .retrieve()
                .body(MAP);

        if (body == null) {
            return null;
        }

        Object encoding = body.get("encoding");

        if (encoding == null || !(body.get("content") instanceof String content)) {
            return null;
        }

        if ("base64".equals(String.valueOf(encoding))) {
            String raw = content.replaceAll("\\s", "");
            return new String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8);
        }

        return content;
    }

    public Map<String, Object> getRepoTree(String accessToken, String owner, String repo, String branch) {

        return client
                .get()
                .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", owner, repo, branch)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .retrieve()
                .body(MAP);
    }
}