package com.git_crawler.backend.exceptions;

public class GithubTokenInvalidException extends RuntimeException {

    public GithubTokenInvalidException() {
        super("Github access token is invalid");
    }
}
