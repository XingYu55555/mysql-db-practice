package com.sqljudge.judgeservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.judgeservice.model.client.AiJudgeResult;
import com.sqljudge.judgeservice.service.AiJudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiJudgeServiceImpl implements AiJudgeService {

    @Value("${app.ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${app.ai.timeout:30000}")
    private int aiTimeout;

    @Value("${app.ai.retry:2}")
    private int aiRetry;

    @Value("${app.ai.confidence-threshold.high:0.9}")
    private double highConfidenceThreshold;

    @Value("${app.ai.confidence-threshold.low:0.6}")
    private double lowConfidenceThreshold;

    @Value("${app.ai.api-key:}")
    private String apiKey;

    @Value("${app.ai.api-host:}")
    private String apiHost;

    @Value("${app.ai.model:qwen-turbo}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiJudgeResult judgeWithAi(String problemDescription, String standardAnswer, String studentAnswer, String sqlType) {
        if (!aiEnabled) {
            log.info("AI judging is disabled, using fallback");
            return fallbackMetadataCheck(standardAnswer, studentAnswer, sqlType);
        }

        // 检查 API Key 是否配置
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("AI API Key is not configured, using fallback");
            return fallbackMetadataCheck(standardAnswer, studentAnswer, sqlType);
        }

        int attempts = 0;
        int maxAttempts = aiRetry + 1;
        long startTime = System.currentTimeMillis();

        while (attempts < maxAttempts) {
            attempts++;
            try {
                AiJudgeResult result = callAiService(problemDescription, standardAnswer, studentAnswer, sqlType);

                if (result != null && result.isSuccess()) {
                    return processAiResult(result);
                }

                log.warn("AI service call failed, attempt {}/{}", attempts, maxAttempts);

                if (attempts < maxAttempts) {
                    Thread.sleep(1000 * attempts);
                }

            } catch (Exception e) {
                log.error("AI service call exception, attempt {}/{}: {}", attempts, maxAttempts, e.getMessage());

                if (attempts >= maxAttempts) {
                    return AiJudgeResult.builder()
                            .success(false)
                            .isCorrect(false)
                            .errorMessage("AI service failed after " + maxAttempts + " attempts: " + e.getMessage())
                            .status("AI_FAILED")
                            .build();
                }

                try {
                    Thread.sleep(1000 * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime >= aiTimeout) {
            return AiJudgeResult.builder()
                    .success(false)
                    .isCorrect(false)
                    .errorMessage("AI service timeout after " + (elapsedTime / 1000) + " seconds")
                    .status("AI_TIMEOUT")
                    .build();
        }

        return fallbackMetadataCheck(standardAnswer, studentAnswer, sqlType);
    }

    private AiJudgeResult callAiService(String problemDescription, String standardAnswer, String studentAnswer, String sqlType) {
        log.info("Calling AI service for semantic analysis, sqlType: {}", sqlType);

        String systemPrompt = "You are a SQL semantic analysis expert. Determine if student's SQL answer is semantically equivalent to the standard answer.\n\n" +
                "Rules:\n" +
                "1. Focus on semantics, not syntax details\n" +
                "2. Column order does not affect semantic equivalence\n" +
                "3. Keyword case does not affect semantic equivalence\n" +
                "4. Whitespace and newlines do not affect semantic equivalence\n\n" +
                "Return JSON format:\n" +
                "{\n" +
                "  \"isCorrect\": boolean,\n" +
                "  \"reason\": string,\n" +
                "  \"confidence\": float (0.0-1.0)\n" +
                "}";

        String userPrompt = String.format("Problem description:\n%s\n\nStandard answer:\n%s\n\nStudent answer:\n%s\n\nPlease analyze if the student answer is semantically equivalent to the standard answer.",
                problemDescription != null ? problemDescription : "",
                standardAnswer != null ? standardAnswer : "",
                studentAnswer != null ? studentAnswer : "");

        try {
            String response = callOpenAiApi(systemPrompt, userPrompt);
            return parseAiResponse(response);
        } catch (Exception e) {
            log.error("Failed to call AI service: {}", e.getMessage());
            throw new RuntimeException("AI service call failed", e);
        }
    }

    private String callOpenAiApi(String systemPrompt, String userPrompt) {
        log.debug("Calling OpenAI compatible API with model: {}", model);

        // 构建请求 URL (阿里云百炼 OpenAI 兼容端点)
        String apiUrl;
        if (apiHost != null && !apiHost.isEmpty()) {
            // 使用 OpenAI 兼容模式
            apiUrl = "https://" + apiHost + "/compatible-mode/v1/chat/completions";
        } else {
            // 默认使用阿里云百炼官方端点
            apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
        }

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        // 构建请求体 (OpenAI 兼容格式)
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 1500);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            log.debug("Sending request to AI API: {}", apiUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("AI API response received successfully");

                // 解析响应获取 content
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                JsonNode choicesNode = rootNode.path("choices");

                if (choicesNode.isArray() && choicesNode.size() > 0) {
                    JsonNode firstChoice = choicesNode.get(0);
                    JsonNode messageNode = firstChoice.path("message");
                    String content = messageNode.path("content").asText();
                    return content;
                } else {
                    throw new RuntimeException("Invalid AI response format: no choices found");
                }
            } else {
                throw new RuntimeException("AI API returned error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error calling AI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call AI API: " + e.getMessage(), e);
        }
    }

    private AiJudgeResult parseAiResponse(String response) {
        try {
            // 尝试从响应中提取 JSON 部分
            String jsonContent = extractJsonFromResponse(response);

            boolean isCorrect = jsonContent.contains("\"isCorrect\": true") || jsonContent.contains("\"isCorrect\":true");
            double confidence = 0.8;

            if (jsonContent.contains("\"confidence\":")) {
                String confStr = jsonContent.substring(jsonContent.indexOf("\"confidence\":") + 13);
                int endIdx = confStr.indexOf(",");
                if (endIdx < 0) {
                    endIdx = confStr.indexOf("}");
                }
                if (endIdx > 0) {
                    try {
                        confidence = Double.parseDouble(confStr.substring(0, endIdx).trim());
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse confidence value, using default");
                    }
                }
            }

            String reason = "";
            if (jsonContent.contains("\"reason\":")) {
                String reasonStr = jsonContent.substring(jsonContent.indexOf("\"reason\":") + 9);
                // 找到 reason 值的开始引号
                int startIdx = reasonStr.indexOf("\"");
                if (startIdx >= 0) {
                    reasonStr = reasonStr.substring(startIdx + 1);
                    int endIdx = reasonStr.indexOf("\"");
                    if (endIdx > 0) {
                        reason = reasonStr.substring(0, endIdx);
                    }
                }
            }

            return AiJudgeResult.builder()
                    .success(true)
                    .isCorrect(isCorrect)
                    .confidence(confidence)
                    .reason(reason)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            return AiJudgeResult.builder()
                    .success(false)
                    .errorMessage("Failed to parse AI response: " + e.getMessage())
                    .build();
        }
    }

    private String extractJsonFromResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "{}";
        }

        // 尝试找到 JSON 对象的开始和结束
        int startIdx = response.indexOf("{");
        int endIdx = response.lastIndexOf("}");

        if (startIdx >= 0 && endIdx > startIdx) {
            return response.substring(startIdx, endIdx + 1);
        }

        return response;
    }

    private AiJudgeResult processAiResult(AiJudgeResult result) {
        double confidence = result.getConfidence();

        if (confidence >= highConfidenceThreshold) {
            result.setStatus(result.isCorrect() ? "AI_APPROVED" : "AI_REJECTED");
            return result;
        } else if (confidence >= lowConfidenceThreshold) {
            result.setStatus(result.isCorrect() ? "AI_APPROVED" : "AI_REJECTED");
            log.warn("AI判定置信度较低: {}, reason: {}", confidence, result.getReason());
            return result;
        } else {
            result.setStatus("AI_LOW_CONFIDENCE");
            log.warn("AI判定置信度过低: {}, reason: {}", confidence, result.getReason());
            return result;
        }
    }

    private AiJudgeResult fallbackMetadataCheck(String standardAnswer, String studentAnswer, String sqlType) {
        log.info("Using fallback metadata check for sqlType: {}", sqlType);

        if (sqlType == null) {
            sqlType = "DDL";
        }

        if ("DDL".equalsIgnoreCase(sqlType)) {
            return ddlMetadataComparison(standardAnswer, studentAnswer);
        } else if ("DCL".equalsIgnoreCase(sqlType)) {
            return dclKeywordCheck(standardAnswer, studentAnswer);
        }

        return AiJudgeResult.builder()
                .success(true)
                .isCorrect(false)
                .reason("Fallback: Unable to determine equivalence")
                .confidence(0.0)
                .status("INCORRECT")
                .build();
    }

    private AiJudgeResult ddlMetadataComparison(String standard, String student) {
        String normalizedStandard = normalizeSql(standard);
        String normalizedStudent = normalizeSql(student);

        boolean equivalent = normalizedStandard.equals(normalizedStudent);

        return AiJudgeResult.builder()
                .success(true)
                .isCorrect(equivalent)
                .reason(equivalent ? "DDL structure matches (fallback)" : "DDL structure does not match (fallback)")
                .confidence(0.5)
                .status(equivalent ? "CORRECT" : "INCORRECT")
                .build();
    }

    private AiJudgeResult dclKeywordCheck(String standard, String student) {
        String normalizedStandard = normalizeSql(standard).toUpperCase();
        String normalizedStudent = normalizeSql(student).toUpperCase();

        boolean hasGrant = normalizedStandard.contains("GRANT");
        boolean hasRevoke = normalizedStandard.contains("REVOKE");
        boolean studentHasGrant = normalizedStudent.contains("GRANT");
        boolean studentHasRevoke = normalizedStudent.contains("REVOKE");

        boolean equivalent = (hasGrant == studentHasGrant) && (hasRevoke == studentHasRevoke);

        return AiJudgeResult.builder()
                .success(true)
                .isCorrect(equivalent)
                .reason(equivalent ? "DCL keywords match (fallback)" : "DCL keywords do not match (fallback)")
                .confidence(0.5)
                .status(equivalent ? "CORRECT" : "INCORRECT")
                .build();
    }

    private String normalizeSql(String sql) {
        if (sql == null) return "";
        return sql.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("(?i)\\b(VARCHAR|INT|INTEGER|BIGINT|SMALLINT|TINYINT)\\b", "TYPE")
                .replaceAll("(?i)\\b(NOT NULL|NULL|AUTO_INCREMENT|PRIMARY KEY)\\b", "CONSTRAINT")
                .replaceAll("(?i)\\s+", " ")
                .toUpperCase();
    }
}
