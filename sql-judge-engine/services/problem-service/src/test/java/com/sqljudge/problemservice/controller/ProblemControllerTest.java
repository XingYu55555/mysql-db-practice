package com.sqljudge.problemservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.problemservice.exception.InvalidStatusTransitionException;
import com.sqljudge.problemservice.exception.ProblemNotFoundException;
import com.sqljudge.problemservice.exception.UnauthorizedException;
import com.sqljudge.problemservice.model.dto.request.BatchImportRequest;
import com.sqljudge.problemservice.model.dto.request.CreateProblemRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemStatusRequest;
import com.sqljudge.problemservice.model.dto.response.*;
import com.sqljudge.problemservice.service.ProblemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProblemController.class)
class ProblemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProblemService problemService;

    @Test
    void createProblem_Success() throws Exception {
        CreateProblemRequest request = CreateProblemRequest.builder()
                .title("Test Problem")
                .sqlType("DQL")
                .build();

        ProblemResponse response = ProblemResponse.builder()
                .problemId(1L)
                .title("Test Problem")
                .sqlType("DQL")
                .status("DRAFT")
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .tags(Collections.emptyList())
                .build();

        when(problemService.createProblem(any(), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/problem")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.problemId").value(1))
                .andExpect(jsonPath("$.title").value("Test Problem"));
    }

    @Test
    void listProblems_Success() throws Exception {
        ProblemResponse problem = ProblemResponse.builder()
                .problemId(1L)
                .title("Test Problem")
                .sqlType("DQL")
                .status("DRAFT")
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .tags(Collections.emptyList())
                .build();

        ProblemListResponse response = ProblemListResponse.builder()
                .content(List.of(problem))
                .page(1)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();

        when(problemService.listProblems(anyInt(), anyInt(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/problem")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].problemId").value(1))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void getProblem_Success() throws Exception {
        ProblemBasicResponse response = ProblemBasicResponse.builder()
                .problemId(1L)
                .title("Test Problem")
                .description("Test Description")
                .difficulty("MEDIUM")
                .sqlType("DQL")
                .expectedType("RESULT_SET")
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        when(problemService.getProblem(1L)).thenReturn(response);

        mockMvc.perform(get("/api/problem/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problemId").value(1))
                .andExpect(jsonPath("$.title").value("Test Problem"));
    }

    @Test
    void getProblem_NotFound() throws Exception {
        when(problemService.getProblem(999L)).thenThrow(new ProblemNotFoundException(999L));

        mockMvc.perform(get("/api/problem/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProblem_Success() throws Exception {
        UpdateProblemRequest request = UpdateProblemRequest.builder()
                .title("Updated Title")
                .build();

        ProblemResponse response = ProblemResponse.builder()
                .problemId(1L)
                .title("Updated Title")
                .sqlType("DQL")
                .status("DRAFT")
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .tags(Collections.emptyList())
                .build();

        when(problemService.updateProblem(eq(1L), any(), eq(1L))).thenReturn(response);

        mockMvc.perform(put("/api/problem/1")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void updateProblem_Unauthorized() throws Exception {
        UpdateProblemRequest request = UpdateProblemRequest.builder()
                .title("Updated Title")
                .build();

        when(problemService.updateProblem(eq(1L), any(), eq(999L)))
                .thenThrow(new UnauthorizedException("Only the problem creator can update this problem"));

        mockMvc.perform(put("/api/problem/1")
                        .header("X-User-Id", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteProblem_Success() throws Exception {
        mockMvc.perform(delete("/api/problem/1")
                        .header("X-User-Id", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProblem_Unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only the problem creator can delete this problem"))
                .when(problemService).deleteProblem(eq(1L), eq(999L));

        mockMvc.perform(delete("/api/problem/1")
                        .header("X-User-Id", "999"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateProblemStatus_Success() throws Exception {
        UpdateProblemStatusRequest request = UpdateProblemStatusRequest.builder()
                .status("READY")
                .build();

        ProblemResponse response = ProblemResponse.builder()
                .problemId(1L)
                .title("Test Problem")
                .sqlType("DQL")
                .status("READY")
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .tags(Collections.emptyList())
                .build();

        when(problemService.updateProblemStatus(eq(1L), any(), eq(1L))).thenReturn(response);

        mockMvc.perform(put("/api/problem/1/status")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void updateProblemStatus_InvalidTransition() throws Exception {
        UpdateProblemStatusRequest request = UpdateProblemStatusRequest.builder()
                .status("PUBLISHED")
                .build();

        when(problemService.updateProblemStatus(eq(1L), any(), eq(1L)))
                .thenThrow(new InvalidStatusTransitionException("DRAFT", "PUBLISHED"));

        mockMvc.perform(put("/api/problem/1/status")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batchImportProblems_Success() throws Exception {
        BatchImportRequest request = BatchImportRequest.builder()
                .problems(List.of(
                        CreateProblemRequest.builder().title("Problem 1").sqlType("DQL").build(),
                        CreateProblemRequest.builder().title("Problem 2").sqlType("DML").build()
                ))
                .build();

        BatchImportResponse response = BatchImportResponse.builder()
                .successCount(2)
                .failCount(0)
                .problems(List.of(
                        ProblemResponse.builder().problemId(1L).title("Problem 1").build(),
                        ProblemResponse.builder().problemId(2L).title("Problem 2").build()
                ))
                .errors(Collections.emptyList())
                .build();

        when(problemService.batchImportProblems(any(), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/problem/batch")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failCount").value(0));
    }

    @Test
    void listMyProblems_Success() throws Exception {
        ProblemResponse problem = ProblemResponse.builder()
                .problemId(1L)
                .title("My Problem")
                .sqlType("DQL")
                .status("DRAFT")
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .tags(Collections.emptyList())
                .build();

        ProblemListResponse response = ProblemListResponse.builder()
                .content(List.of(problem))
                .page(1)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();

        when(problemService.listMyProblems(eq(1L), anyInt(), anyInt(), any())).thenReturn(response);

        mockMvc.perform(get("/api/problem/teacher/my")
                        .header("X-User-Id", "1")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].problemId").value(1));
    }
}