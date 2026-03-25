package com.sqljudge.resultservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.resultservice.config.ApiKeyInterceptor;
import com.sqljudge.resultservice.config.WebConfig;
import com.sqljudge.resultservice.exception.GlobalExceptionHandler;
import com.sqljudge.resultservice.model.dto.request.CreateResultRequest;
import com.sqljudge.resultservice.model.dto.response.*;
import com.sqljudge.resultservice.service.ResultService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResultService resultService;

    private static final String API_KEY = "test-api-key";

    @Test
    void createResult_WithValidApiKey_ReturnsCreated() throws Exception {
        CreateResultRequest request = CreateResultRequest.builder()
                .submissionId(1L)
                .problemId(100L)
                .studentId(1000L)
                .score(BigDecimal.valueOf(100))
                .status("CORRECT")
                .executionTimeMs(50L)
                .build();

        ResultResponse response = ResultResponse.builder()
                .resultId(1L)
                .submissionId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        when(resultService.createResult(any())).thenReturn(response);

        mockMvc.perform(post("/api/result")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultId").value(1))
                .andExpect(jsonPath("$.submissionId").value(1));
    }

    @Test
    void createResult_WithInvalidApiKey_ReturnsUnauthorized() throws Exception {
        CreateResultRequest request = CreateResultRequest.builder()
                .submissionId(1L)
                .score(BigDecimal.valueOf(100))
                .status("CORRECT")
                .build();

        mockMvc.perform(post("/api/result")
                        .header("X-API-Key", "invalid-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createResult_WithMissingApiKey_ReturnsUnauthorized() throws Exception {
        CreateResultRequest request = CreateResultRequest.builder()
                .submissionId(1L)
                .score(BigDecimal.valueOf(100))
                .status("CORRECT")
                .build();

        mockMvc.perform(post("/api/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getResultBySubmission_ReturnsResult() throws Exception {
        Long submissionId = 1L;
        ResultDetailResponse response = ResultDetailResponse.builder()
                .resultId(1L)
                .submissionId(submissionId)
                .problemId(100L)
                .studentId(1000L)
                .score(BigDecimal.valueOf(85))
                .status("CORRECT")
                .executionTimeMs(30L)
                .createdAt(LocalDateTime.now())
                .build();

        when(resultService.getResultBySubmission(submissionId)).thenReturn(response);

        mockMvc.perform(get("/api/result/submission/{submissionId}", submissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultId").value(1))
                .andExpect(jsonPath("$.submissionId").value(1))
                .andExpect(jsonPath("$.problemId").value(100))
                .andExpect(jsonPath("$.studentId").value(1000))
                .andExpect(jsonPath("$.score").value(85))
                .andExpect(jsonPath("$.status").value("CORRECT"));
    }

    @Test
    void getResultBySubmission_NotFound_Returns404() throws Exception {
        Long submissionId = 999L;
        when(resultService.getResultBySubmission(submissionId))
                .thenThrow(new RuntimeException("Result not found"));

        mockMvc.perform(get("/api/result/submission/{submissionId}", submissionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Result not found"));
    }

    @Test
    void getStudentResults_ReturnsPagedResults() throws Exception {
        Long studentId = 1000L;
        ResultResponse result1 = ResultResponse.builder()
                .resultId(1L)
                .submissionId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        ResultResponse result2 = ResultResponse.builder()
                .resultId(2L)
                .submissionId(2L)
                .createdAt(LocalDateTime.now())
                .build();

        StudentResultListResponse response = StudentResultListResponse.builder()
                .content(Arrays.asList(result1, result2))
                .page(1)
                .size(10)
                .totalElements(2L)
                .totalPages(1)
                .build();

        when(resultService.getStudentResults(eq(studentId), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/result/student/{studentId}", studentId)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getProblemLeaderboard_ReturnsLeaderboard() throws Exception {
        Long problemId = 100L;
        LeaderboardEntry entry1 = LeaderboardEntry.builder()
                .rank(1)
                .studentId(1000L)
                .bestScore(BigDecimal.valueOf(100))
                .latestSubmitTime(LocalDateTime.now())
                .build();

        LeaderboardEntry entry2 = LeaderboardEntry.builder()
                .rank(2)
                .studentId(1001L)
                .bestScore(BigDecimal.valueOf(90))
                .latestSubmitTime(LocalDateTime.now())
                .build();

        LeaderboardResponse response = LeaderboardResponse.builder()
                .problemId(problemId)
                .entries(Arrays.asList(entry1, entry2))
                .page(1)
                .size(50)
                .totalElements(2L)
                .totalPages(1)
                .build();

        when(resultService.getProblemLeaderboard(eq(problemId), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/result/problem/{problemId}/leaderboard", problemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemId").value(100))
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].studentId").value(1000));
    }

    @Test
    void getOverallLeaderboard_ReturnsOverallRanking() throws Exception {
        OverallLeaderboardEntry entry1 = OverallLeaderboardEntry.builder()
                .rank(1)
                .studentId(1000L)
                .totalScore(BigDecimal.valueOf(190))
                .problemsSolved(2)
                .totalSubmissions(2)
                .build();

        OverallLeaderboardResponse response = OverallLeaderboardResponse.builder()
                .entries(List.of(entry1))
                .page(1)
                .size(50)
                .totalElements(1L)
                .totalPages(1)
                .build();

        when(resultService.getOverallLeaderboard(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/result/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].studentId").value(1000))
                .andExpect(jsonPath("$.entries[0].totalScore").value(190));
    }

    @Test
    void getStatistics_ReturnsStats() throws Exception {
        StatisticsResponse response = StatisticsResponse.builder()
                .totalSubmissions(100L)
                .acceptedSubmissions(75L)
                .acceptanceRate(BigDecimal.valueOf(75))
                .averageScore(BigDecimal.valueOf(82.5))
                .maxScore(BigDecimal.valueOf(100))
                .minScore(BigDecimal.valueOf(50))
                .averageExecutionTime(45L)
                .build();

        when(resultService.getStatistics(null)).thenReturn(response);

        mockMvc.perform(get("/api/result/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSubmissions").value(100))
                .andExpect(jsonPath("$.acceptedSubmissions").value(75))
                .andExpect(jsonPath("$.acceptanceRate").value(75))
                .andExpect(jsonPath("$.averageScore").value(82.5));
    }

    @Test
    void getStatistics_WithProblemId_ReturnsProblemStats() throws Exception {
        Long problemId = 100L;
        StatisticsResponse response = StatisticsResponse.builder()
                .problemId(problemId)
                .totalSubmissions(10L)
                .acceptedSubmissions(7L)
                .acceptanceRate(BigDecimal.valueOf(70))
                .averageScore(BigDecimal.valueOf(85.5))
                .maxScore(BigDecimal.valueOf(100))
                .minScore(BigDecimal.valueOf(60))
                .averageExecutionTime(50L)
                .build();

        when(resultService.getStatistics(problemId)).thenReturn(response);

        mockMvc.perform(get("/api/result/statistics")
                        .param("problemId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemId").value(100))
                .andExpect(jsonPath("$.totalSubmissions").value(10))
                .andExpect(jsonPath("$.acceptedSubmissions").value(7));
    }
}