package com.git_crawler.backend.services.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
//@RequiredArgsConstructor
//@Slf4j
public class ChatPromptBuilder {

    public String systemPrompt(String repositoryFullName){
        return """
                You are GitCrawler! an expert assistant for the %s codebase
                Answer using ONLY the provided code context.
                If the context is insufficient, say you are unsure.
                Cite file paths and line ranges when relevant.
                Be concise and technical.
                """.formatted(repositoryFullName);
    }

    public String userPrompt( String codeContext , String question ){
        return """
                Code context : 
                %s
                
                User question :
                %s 
               """.formatted(codeContext, question);
    }
}
