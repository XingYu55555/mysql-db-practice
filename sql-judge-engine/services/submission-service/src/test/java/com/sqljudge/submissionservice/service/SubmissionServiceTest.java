package com.sqljudge.submissionservice.service;

import com.sqljudge.submissionservice.model.dto.request.CreateSubmissionRequest;
import com.sqljudge.submissionservice.model.dto.response.SubmissionDetailResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionListResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionStatusResponse;
import com.sqljudge.submissionservice.model.entity.Submission;
import com.sqljudge.submissionservice.repository.SubmissionRepository;
import com.sqljudge.submissionservice.service.impl.SubmissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private MessagePublisherService messagePublisherService;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    @Test
    void createSubmission_Success() {
        CreateSubmissionRequest request = CreateSubmissionRequest.builder()
                .problemId(1L)
                .sqlContent("SELECT * FROM users")
                .build();

        Submission savedSubmission = Submission.builder()
                .id(1L)
                .problemId(1L)
                .studentId(1L)
                .sqlContent("SELECT * FROM users")
                .status(Submission.SubmissionStatus.JUDGING)
                .submittedAt(LocalDateTime.now())
                .build();

        when(submissionRepository.save(any())).thenReturn(savedSubmission);
        doNothing().when(messagePublisherService).publishJudgeTask(any());

        SubmissionResponse response = submissionService.createSubmission(request, 1L);

        assertNotNull(response);
        assertEquals(1L, response.getSubmissionId());
        assertEquals("JUDGING", response.getStatus());
        verify(submissionRepository, atLeast(1)).save(any());
        verify(messagePublisherService).publishJudgeTask(any());
    }

    @Test
    void createSubmission_PublishFails() {
        CreateSubmissionRequest request = CreateSubmissionRequest.builder()
                .problemId(1L)
                .sqlContent("SELECT * FROM users")
                .build();

        Submission savedSubmission = Submission.builder()
                .id(1L)
                .problemId(1L)
                .studentId(1L)
                .sqlContent("SELECT * FROM users")
                .status(Submission.SubmissionStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        when(submissionRepository.save(any())).thenReturn(savedSubmission);
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(messagePublisherService).publishJudgeTask(any());

        assertThrows(RuntimeException.class, () -> submissionService.createSubmission(request, 1L));

        verify(submissionRepository, times(2)).save(any());
    }

    @Test
    void listSubmissions_NoFilters() {
        Submission submission = Submission.builder()
                .id(1L)
                .problemId(1L)
                .studentId(1L)
                .sqlContent("SELECT * FROM users")
                .status(Submission.SubmissionStatus.SUCCESS)
                .submittedAt(LocalDateTime.now())
                .build();

        Page<Submission> page = new PageImpl<>(Collections.singletonList(submission));
        when(submissionRepository.findByStudentId(eq(1L), any(Pageable.class))).thenReturn(page);

        SubmissionListResponse response = submissionService.listSubmissions(1L, null, null, 1, 10, "submittedAt,desc");

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1L, response.getContent().get(0).getSubmissionId());
    }

    @Test
    void listSubmissions_WithProblemIdFilter() {
        Submission submission = Submission.builder()
                .id(1L)
                .problemId(100L)
                .studentId(1L)
                .sqlContent("SELECT * FROM users")
                .status(Submission.SubmissionStatus.SUCCESS)
                .submittedAt(LocalDateTime.now())
                .build();

        Page<Submission> page = new PageImpl<>(Collections.singletonList(submission));
        when(submissionRepository.findByStudentIdAndProblemId(eq(1L), eq(100L), any(Pageable.class))).thenReturn(page);

        SubmissionListResponse response = submissionService.listSubmissions(1L, 100L, null, 1, 10, "submittedAt,desc");

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(100L, response.getContent().get(0).getProblemId());
    }

    @Test
    void listSubmissions_WithStatusFilter() {
        Submission submission = Submission.builder()
                .id(1L)
                .problemId(1L)
                .studentId(1L)
                .sqlContent("SELECT * FROM users")
                .status(Submission.SubmissionStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        Page<Submission> page = new PageImpl<>(Collections.singletonList(submission));
        when(submissionRepository.findByStudentIdAndStatus(eq(1L), eq(Submission.SubmissionStatus.PENDING), any(Pageable.class))).thenReturn(page);

        SubmissionListResponse response = submissionService.listSubmissions(1L, null, "PENDING", 1, 10, "submittedAt,desc");

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("PENDING", response.getContent().get(0).getStatus());
    }

    @Test
    void getSubmissionDetail_Success() {
        Submission submission = Submission.builder()
                .id(1L)
                .problemId(1L)
                .studentId(1L)
                .sqlContent("SELECT * FROM users")
                .status(Submission.SubmissionStatus.SUCCESS)
                .submittedAt(LocalDateTime.now())
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        SubmissionDetailResponse response = submissionService.getSubmissionDetail(1L);

        assertNotNull(response);
        assertEquals(1L, response.getSubmissionId());
        assertEquals("SELECT * FROM users", response.getSqlContent());
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void getSubmissionDetail_NotFound() {
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> submissionService.getSubmissionDetail(999L));
    }

    @Test
    void getSubmissionStatus_Success() {
        Submission submission = Submission.builder()
                .id(1L)
                .problemId(1L)
                .studentId(1L)
                .sqlContent("SELECT * FROM users")
                .status(Submission.SubmissionStatus.JUDGING)
                .submittedAt(LocalDateTime.now())
                .build();

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

        SubmissionStatusResponse response = submissionService.getSubmissionStatus(1L);

        assertNotNull(response);
        assertEquals(1L, response.getSubmissionId());
        assertEquals("JUDGING", response.getStatus());
    }

    @Test
    void getSubmissionStatus_NotFound() {
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> submissionService.getSubmissionStatus(999L));
    }
}
