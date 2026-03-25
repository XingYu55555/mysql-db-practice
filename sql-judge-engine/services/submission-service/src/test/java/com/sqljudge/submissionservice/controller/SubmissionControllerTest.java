package com.sqljudge.submissionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.submissionservice.model.dto.request.CreateSubmissionRequest;
import com.sqljudge.submissionservice.model.dto.response.SubmissionDetailResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionListResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionStatusResponse;
import com.sqljudge.submissionservice.service.SubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubmissionController.class)
class SubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubmissionService submissionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createSubmission_Success() throws Exception {
        CreateSubmissionRequest request = CreateSubmissionRequest.builder()
                .problemId(1L)
                .sqlContent("SELECT * FROM users")
                .build();

        SubmissionResponse response = SubmissionResponse.builder()
                .submissionId(1L)
                .problemId(1L)
                .status("JUDGING")
                .submittedAt(LocalDateTime.now())
                .build();

        when(submissionService.createSubmission(any(), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.submissionId").value(1))
                .andExpect(jsonPath("$.problemId").value(1))
                .andExpect(jsonPath("$.status").value("JUDGING"));
    }

    @Test
    void createSubmission_ValidationError() throws Exception {
        CreateSubmissionRequest request = CreateSubmissionRequest.builder()
                .problemId(null)
                .sqlContent("")
                .build();

        mockMvc.perform(post("/api/submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listSubmissions_Success() throws Exception {
        SubmissionListResponse response = SubmissionListResponse.builder()
                .content(Collections.singletonList(
                        SubmissionResponse.builder()
                                .submissionId(1L)
                                .problemId(1L)
                                .status("SUCCESS")
                                .submittedAt(LocalDateTime.now())
                                .build()
                ))
                .page(1)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();

        when(submissionService.listSubmissions(eq(1L), isNull(), isNull(), eq(1), eq(10), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/submission")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].submissionId").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listSubmissions_WithFilters() throws Exception {
        SubmissionListResponse response = SubmissionListResponse.builder()
                .content(Collections.emptyList())
                .page(1)
                .size(10)
                .totalElements(0L)
                .totalPages(0)
                .build();

        when(submissionService.listSubmissions(eq(1L), eq(100L), eq("SUCCESS"), eq(1), eq(10), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/submission")
                        .param("problemId", "100")
                        .param("status", "SUCCESS")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getSubmissionDetail_Success() throws Exception {
        SubmissionDetailResponse response = SubmissionDetailResponse.builder()
                .submissionId(1L)
                .problemId(1L)
                .sqlContent("SELECT * FROM users")
                .status("SUCCESS")
                .build();

        when(submissionService.getSubmissionDetail(1L)).thenReturn(response);

        mockMvc.perform(get("/api/submission/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value(1))
                .andExpect(jsonPath("$.sqlContent").value("SELECT * FROM users"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void getSubmissionDetail_NotFound() throws Exception {
        when(submissionService.getSubmissionDetail(999L))
                .thenThrow(new RuntimeException("Submission not found"));

        mockMvc.perform(get("/api/submission/999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSubmissionStatus_Success() throws Exception {
        SubmissionStatusResponse response = SubmissionStatusResponse.builder()
                .submissionId(1L)
                .status("JUDGING")
                .build();

        when(submissionService.getSubmissionStatus(1L)).thenReturn(response);

        mockMvc.perform(get("/api/submission/1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value(1))
                .andExpect(jsonPath("$.status").value("JUDGING"));
    }
}