package com.sqljudge.judgeservice.service;

import com.sqljudge.judgeservice.model.dto.JudgeResult;
import com.sqljudge.judgeservice.service.impl.ResultClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ResultClientServiceImpl resultClientService;

    private static final String RESULT_SERVICE_URL = "http://localhost:8086";
    private static final String API_KEY = "internal-api-key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resultClientService, "resultServiceUrl", RESULT_SERVICE_URL);
        ReflectionTestUtils.setField(resultClientService, "apiKey", API_KEY);
    }

    @Test
    void testSubmitResult_Success() {
        JudgeResult result = JudgeResult.builder()
                .submissionId(1L)
                .score(BigDecimal.valueOf(100))
                .status("CORRECT")
                .executionTimeMs(100L)
                .build();

        when(restTemplate.postForObject(
                eq(RESULT_SERVICE_URL + "/api/result"),
                any(HttpEntity.class),
                eq(Object.class)))
                .thenReturn(null);

        assertDoesNotThrow(() -> resultClientService.submitResult(result));

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(
                eq(RESULT_SERVICE_URL + "/api/result"),
                captor.capture(),
                eq(Object.class));

        HttpEntity<JudgeResult> capturedEntity = captor.getValue();
        HttpHeaders headers = capturedEntity.getHeaders();

        assertTrue(headers.containsKey("X-API-Key"));
        assertEquals(API_KEY, headers.getFirst("X-API-Key"));
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
    }

    @Test
    void testSubmitResult_Failure() {
        JudgeResult result = JudgeResult.builder()
                .submissionId(1L)
                .score(BigDecimal.ZERO)
                .status("ERROR")
                .errorMessage("Test error")
                .executionTimeMs(0L)
                .build();

        when(restTemplate.postForObject(
                eq(RESULT_SERVICE_URL + "/api/result"),
                any(HttpEntity.class),
                eq(Object.class)))
                .thenThrow(new RuntimeException("Result service unavailable"));

        assertThrows(RuntimeException.class, () -> resultClientService.submitResult(result));
    }

    @Test
    void testSubmitResult_WithErrorMessage() {
        JudgeResult result = JudgeResult.builder()
                .submissionId(2L)
                .score(BigDecimal.ZERO)
                .status("INCORRECT")
                .errorMessage("Result sets do not match")
                .executionTimeMs(50L)
                .build();

        when(restTemplate.postForObject(
                eq(RESULT_SERVICE_URL + "/api/result"),
                any(HttpEntity.class),
                eq(Object.class)))
                .thenReturn(null);

        assertDoesNotThrow(() -> resultClientService.submitResult(result));

        verify(restTemplate).postForObject(
                eq(RESULT_SERVICE_URL + "/api/result"),
                any(HttpEntity.class),
                eq(Object.class));
    }
}