package com.gitcrawler.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResendEmailRequestDTO {

    private String from;

    private List<String> to;

    private String subject;

    private String html;
}