package com.sqljudge.judgeservice.service.impl;

import com.sqljudge.judgeservice.model.dto.JudgeResult;
import com.sqljudge.judgeservice.service.ResultClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResultClientServiceImpl implements ResultClientService {

    private final RestTemplate restTemplate;

    @Value("${app.result-service.url}")
    private String resultServiceUrl;

    @Value("${app.result-service.api-key}")
    private String apiKey;

    @Override
    public void submitResult(JudgeResult result) {
        String url = resultServiceUrl + "/api/result";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);

        HttpEntity<JudgeResult> entity = new HttpEntity<>(result, headers);
        log.info("Submitting result for submission: {}, status: {}, score: {}",
                result.getSubmissionId(), result.getStatus(), result.getScore());

        try {
            restTemplate.postForObject(url, entity, Object.class);
            log.info("Result submitted successfully for submission: {}", result.getSubmissionId());
        } catch (Exception e) {
            log.error("Failed to submit result for submission: {}", result.getSubmissionId(), e);
            throw new RuntimeException("Failed to submit result", e);
        }
    }
}