package com.git_crawler.backend.services.github;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GithubApiClient {

    private static  final String API_BASE = "https://api.github.com/";
    private static  final ParameterizedTypeReference<List<Map<String, Object>>> LIST_MAP = new ParameterizedTypeReference<>() {};
    private static  final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};

    private final RestClient.Builder clientBuilder;


    private RestClient client(String accessToken) {
        return clientBuilder.baseUrl(API_BASE)
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("Content-Type", "application/vnd.github+json")
                .defaultHeader(
                        "X-GitHub-API-Version",
                        "2026-03-10"
                )
                .defaultHeader(HttpHeaders.USER_AGENT, "GitCrawler")
                .build();
    }

    public List<Map<String, Object>> listUserRepos(String accessToken) {
        List<Map<String, Object>> all = new ArrayList<>();
        int page =1;

        while(page <= 10) {
            final int currentPage = page;
            List<Map<String,Object>> pageRepos = client(accessToken)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/user/repos")
                            .queryParam("affiliation", "owner,collaborator,organization_member")
                            .queryParam("sort", "updated")
                            .queryParam("per_page", 100)
                            .queryParam("page",currentPage)
                            .build()
                    )
                    .retrieve()
                    .body(LIST_MAP);
            if(pageRepos.isEmpty() || pageRepos==null) {
                break;
            }

            all.addAll(pageRepos);
            if(pageRepos.size()<100) {
                break;
            }
            page++;
        }
        return all;
    }

    public String getFileContent(String accessToken, String owner , String repo , String path) {
        Map<String,Object> body = client(accessToken)
                .get()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner,repo, path)
                .retrieve()
                .body(MAP);
        if(body==null) {
            return null;
        }
        Object encoding =body.get("encoding");
        Object content =body.get("content");
        if(encoding==null) {
            return null;
        }
        if("base64".equals(String.valueOf(encoding))){
            String raw = String.valueOf(content).replaceAll("\\s", "");
            return  new String(Base64.getDecoder().decode(raw), StandardCharsets.UTF_8);
        }
        return String.valueOf(content);
    }

    public Map<String , Object> getRepoTree(String accessToken, String owner, String repo , String branch){
        return client(accessToken)
                .get()
                .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1",owner,repo,branch)
                .retrieve()
                .body(MAP);
    }
 }
