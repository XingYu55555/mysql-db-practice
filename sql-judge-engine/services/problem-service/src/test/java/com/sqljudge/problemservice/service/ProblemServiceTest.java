package com.sqljudge.problemservice.service;

import com.sqljudge.problemservice.exception.InvalidStatusTransitionException;
import com.sqljudge.problemservice.exception.ProblemNotFoundException;
import com.sqljudge.problemservice.exception.UnauthorizedException;
import com.sqljudge.problemservice.model.dto.request.BatchImportRequest;
import com.sqljudge.problemservice.model.dto.request.CreateProblemRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemStatusRequest;
import com.sqljudge.problemservice.model.dto.response.*;
import com.sqljudge.problemservice.model.entity.Problem;
import com.sqljudge.problemservice.model.entity.ProblemTag;
import com.sqljudge.problemservice.model.entity.Tag;
import com.sqljudge.problemservice.repository.ProblemRepository;
import com.sqljudge.problemservice.repository.ProblemTagRepository;
import com.sqljudge.problemservice.repository.TagRepository;
import com.sqljudge.problemservice.service.impl.ProblemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock
    private ProblemRepository problemRepository;

    @Mock
    private ProblemTagRepository problemTagRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private ProblemServiceImpl problemService;

    private Problem testProblem;
    private CreateProblemRequest createRequest;
    private UpdateProblemRequest updateRequest;

    @BeforeEach
    void setUp() {
        testProblem = Problem.builder()
                .id(1L)
                .title("Test Problem")
                .description("Test Description")
                .difficulty(Problem.Difficulty.MEDIUM)
                .sqlType(Problem.SqlType.DQL)
                .status(Problem.ProblemStatus.DRAFT)
                .aiAssisted(false)
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = CreateProblemRequest.builder()
                .title("Test Problem")
                .description("Test Description")
                .difficulty("MEDIUM")
                .sqlType("DQL")
                .aiAssisted(false)
                .build();

        updateRequest = UpdateProblemRequest.builder()
                .title("Updated Title")
                .description("Updated Description")
                .difficulty("HARD")
                .build();
    }

    @Test
    void createProblem_Success() {
        when(problemRepository.save(any(Problem.class))).thenReturn(testProblem);
        when(problemTagRepository.findByProblemId(any())).thenReturn(Collections.emptyList());

        ProblemResponse response = problemService.createProblem(createRequest, 1L);

        assertNotNull(response);
        assertEquals("Test Problem", response.getTitle());
        assertEquals("DQL", response.getSqlType());
        assertEquals("DRAFT", response.getStatus());
        verify(problemRepository).save(any(Problem.class));
    }

    @Test
    void getProblemDetail_Success() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));
        when(problemTagRepository.findByProblemId(1L)).thenReturn(Collections.emptyList());

        ProblemDetailResponse response = problemService.getProblemDetail(1L);

        assertNotNull(response);
        assertEquals(1L, response.getProblemId());
        assertEquals("Test Problem", response.getTitle());
        assertEquals("Test Description", response.getDescription());
    }

    @Test
    void getProblemDetail_NotFound() {
        when(problemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProblemNotFoundException.class, () -> problemService.getProblemDetail(999L));
    }

    @Test
    void getProblem_Success() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));

        ProblemBasicResponse response = problemService.getProblem(1L);

        assertNotNull(response);
        assertEquals(1L, response.getProblemId());
        assertEquals("Test Problem", response.getTitle());
    }

    @Test
    void updateProblem_Success() {
        Problem updatedProblem = Problem.builder()
                .id(1L)
                .title("Updated Title")
                .description("Updated Description")
                .difficulty(Problem.Difficulty.HARD)
                .sqlType(Problem.SqlType.DQL)
                .status(Problem.ProblemStatus.DRAFT)
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));
        when(problemRepository.save(any(Problem.class))).thenReturn(updatedProblem);
        when(problemTagRepository.findByProblemId(any())).thenReturn(Collections.emptyList());

        ProblemResponse response = problemService.updateProblem(1L, updateRequest, 1L);

        assertNotNull(response);
        assertEquals("Updated Title", response.getTitle());
        assertEquals("HARD", response.getDifficulty());
    }

    @Test
    void updateProblem_Unauthorized() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));

        assertThrows(UnauthorizedException.class,
                () -> problemService.updateProblem(1L, updateRequest, 999L));
    }

    @Test
    void deleteProblem_Success() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));
        doNothing().when(problemTagRepository).deleteByProblemId(1L);
        doNothing().when(problemRepository).delete(any(Problem.class));

        assertDoesNotThrow(() -> problemService.deleteProblem(1L, 1L));
        verify(problemRepository).delete(testProblem);
    }

    @Test
    void deleteProblem_Unauthorized() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));

        assertThrows(UnauthorizedException.class,
                () -> problemService.deleteProblem(1L, 999L));
    }

    @Test
    void updateProblemStatus_DraftToReady_Success() {
        Problem draftProblem = Problem.builder()
                .id(1L)
                .title("Test")
                .sqlType(Problem.SqlType.DQL)
                .status(Problem.ProblemStatus.DRAFT)
                .teacherId(1L)
                .build();

        Problem readyProblem = Problem.builder()
                .id(1L)
                .title("Test")
                .sqlType(Problem.SqlType.DQL)
                .status(Problem.ProblemStatus.READY)
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        UpdateProblemStatusRequest statusRequest = UpdateProblemStatusRequest.builder()
                .status("READY")
                .build();

        when(problemRepository.findById(1L)).thenReturn(Optional.of(draftProblem));
        when(problemRepository.save(any(Problem.class))).thenReturn(readyProblem);
        when(problemTagRepository.findByProblemId(any())).thenReturn(Collections.emptyList());

        ProblemResponse response = problemService.updateProblemStatus(1L, statusRequest, 1L);

        assertNotNull(response);
        assertEquals("READY", response.getStatus());
    }

    @Test
    void updateProblemStatus_InvalidTransition() {
        UpdateProblemStatusRequest statusRequest = UpdateProblemStatusRequest.builder()
                .status("PUBLISHED")
                .build();

        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));

        assertThrows(InvalidStatusTransitionException.class,
                () -> problemService.updateProblemStatus(1L, statusRequest, 1L));
    }

    @Test
    void updateProblemStatus_Unauthorized() {
        UpdateProblemStatusRequest statusRequest = UpdateProblemStatusRequest.builder()
                .status("READY")
                .build();

        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));

        assertThrows(UnauthorizedException.class,
                () -> problemService.updateProblemStatus(1L, statusRequest, 999L));
    }

    @Test
    void listProblems_Success() {
        Page<Problem> problemPage = new PageImpl<>(List.of(testProblem));
        when(problemRepository.findAll(any(Pageable.class))).thenReturn(problemPage);
        when(problemTagRepository.findByProblemId(any())).thenReturn(Collections.emptyList());

        ProblemListResponse response = problemService.listProblems(1, 10, null, null);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getPage());
    }

    @Test
    void listProblems_WithFilters() {
        Page<Problem> problemPage = new PageImpl<>(List.of(testProblem));
        when(problemRepository.findByDifficulty(eq(Problem.Difficulty.MEDIUM), any(Pageable.class)))
                .thenReturn(problemPage);
        when(problemTagRepository.findByProblemId(any())).thenReturn(Collections.emptyList());

        ProblemListResponse response = problemService.listProblems(1, 10, "MEDIUM", null);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void listMyProblems_Success() {
        Page<Problem> problemPage = new PageImpl<>(List.of(testProblem));
        when(problemRepository.findByTeacherId(eq(1L), any(Pageable.class))).thenReturn(problemPage);
        when(problemTagRepository.findByProblemId(any())).thenReturn(Collections.emptyList());

        ProblemListResponse response = problemService.listMyProblems(1L, 1, 10, null);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void listMyProblems_WithStatusFilter() {
        Page<Problem> problemPage = new PageImpl<>(List.of(testProblem));
        when(problemRepository.findByTeacherIdAndStatus(eq(1L), eq(Problem.ProblemStatus.DRAFT), any(Pageable.class)))
                .thenReturn(problemPage);
        when(problemTagRepository.findByProblemId(any())).thenReturn(Collections.emptyList());

        ProblemListResponse response = problemService.listMyProblems(1L, 1, 10, "DRAFT");

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void batchImportProblems_Success() {
        CreateProblemRequest problem1 = CreateProblemRequest.builder()
                .title("Problem 1")
                .sqlType("DQL")
                .build();
        CreateProblemRequest problem2 = CreateProblemRequest.builder()
                .title("Problem 2")
                .sqlType("DML")
                .build();

        BatchImportRequest batchRequest = BatchImportRequest.builder()
                .problems(List.of(problem1, problem2))
                .build();

        Problem savedProblem1 = Problem.builder()
                .id(1L)
                .title("Problem 1")
                .sqlType(Problem.SqlType.DQL)
                .status(Problem.ProblemStatus.DRAFT)
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        Problem savedProblem2 = Problem.builder()
                .id(2L)
                .title("Problem 2")
                .sqlType(Problem.SqlType.DML)
                .status(Problem.ProblemStatus.DRAFT)
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        when(problemRepository.save(any(Problem.class)))
                .thenReturn(savedProblem1)
                .thenReturn(savedProblem2);
        when(problemTagRepository.findByProblemId(any())).thenReturn(Collections.emptyList());

        BatchImportResponse response = problemService.batchImportProblems(batchRequest, 1L);

        assertNotNull(response);
        assertEquals(2, response.getSuccessCount());
        assertEquals(0, response.getFailCount());
        assertEquals(2, response.getProblems().size());
    }

    @Test
    void batchImportProblems_PartialFailure() {
        CreateProblemRequest problem1 = CreateProblemRequest.builder()
                .title("Valid Problem")
                .sqlType("DQL")
                .build();
        CreateProblemRequest problem2 = CreateProblemRequest.builder()
                .title("Invalid Problem")
                .sqlType("INVALID_SQL_TYPE")
                .build();

        BatchImportRequest batchRequest = BatchImportRequest.builder()
                .problems(List.of(problem1, problem2))
                .build();

        Problem savedProblem = Problem.builder()
                .id(1L)
                .title("Valid Problem")
                .sqlType(Problem.SqlType.DQL)
                .status(Problem.ProblemStatus.DRAFT)
                .teacherId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        when(problemRepository.save(any(Problem.class)))
                .thenReturn(savedProblem)
                .thenThrow(new RuntimeException("Invalid SQL type"));
        when(problemTagRepository.findByProblemId(any())).thenReturn(Collections.emptyList());

        BatchImportResponse response = problemService.batchImportProblems(batchRequest, 1L);

        assertNotNull(response);
        assertEquals(1, response.getSuccessCount());
        assertEquals(1, response.getFailCount());
        assertEquals(1, response.getProblems().size());
        assertEquals(1, response.getErrors().size());
    }
}