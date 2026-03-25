package com.sqljudge.judgeservice.service.impl;

import com.sqljudge.judgeservice.model.client.ProblemDetail;
import com.sqljudge.judgeservice.service.ProblemClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemClientServiceImpl implements ProblemClientService {

    private final RestTemplate restTemplate;

    @Value("${app.problem-service.url}")
    private String problemServiceUrl;

    @Override
    public ProblemDetail getProblemDetail(Long problemId) {
        String url = problemServiceUrl + "/api/problem/" + problemId;
        log.info("Fetching problem detail from: {}", url);
        try {
            ProblemDetail detail = restTemplate.getForObject(url, ProblemDetail.class);
            return detail;
        } catch (Exception e) {
            log.error("Failed to fetch problem detail for problemId: {}", problemId, e);
            throw new RuntimeException("Failed to fetch problem detail", e);
        }
    }
}