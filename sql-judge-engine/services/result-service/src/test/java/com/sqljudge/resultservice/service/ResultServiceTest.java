package com.sqljudge.resultservice.service;

import com.sqljudge.resultservice.model.dto.request.CreateResultRequest;
import com.sqljudge.resultservice.model.dto.response.*;
import com.sqljudge.resultservice.model.entity.JudgeResult;
import com.sqljudge.resultservice.repository.JudgeResultRepository;
import com.sqljudge.resultservice.service.impl.ResultServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private JudgeResultRepository judgeResultRepository;

    @InjectMocks
    private ResultServiceImpl resultService;

    @Test
    void createResult_Success() {
        CreateResultRequest request = CreateResultRequest.builder()
                .submissionId(1L)
                .problemId(100L)
                .studentId(1000L)
                .score(BigDecimal.valueOf(100))
                .status("CORRECT")
                .executionTimeMs(50L)
                .build();

        JudgeResult savedResult = JudgeResult.builder()
                .id(1L)
                .submissionId(1L)
                .problemId(100L)
                .studentId(1000L)
                .score(BigDecimal.valueOf(100))
                .status(JudgeResult.ResultStatus.CORRECT)
                .executionTimeMs(50L)
                .createdAt(LocalDateTime.now())
                .build();

        when(judgeResultRepository.save(any())).thenReturn(savedResult);

        ResultResponse response = resultService.createResult(request);

        assertNotNull(response);
        assertEquals(1L, response.getResultId());
        assertEquals(1L, response.getSubmissionId());
        verify(judgeResultRepository).save(any());
    }

    @Test
    void getResultBySubmission_Success() {
        Long submissionId = 1L;
        JudgeResult result = JudgeResult.builder()
                .id(1L)
                .submissionId(submissionId)
                .problemId(100L)
                .studentId(1000L)
                .score(BigDecimal.valueOf(85))
                .status(JudgeResult.ResultStatus.CORRECT)
                .executionTimeMs(30L)
                .createdAt(LocalDateTime.now())
                .build();

        when(judgeResultRepository.findBySubmissionId(submissionId)).thenReturn(Optional.of(result));

        ResultDetailResponse response = resultService.getResultBySubmission(submissionId);

        assertNotNull(response);
        assertEquals(1L, response.getResultId());
        assertEquals(submissionId, response.getSubmissionId());
        assertEquals(100L, response.getProblemId());
        assertEquals(1000L, response.getStudentId());
        assertEquals(0, BigDecimal.valueOf(85).compareTo(response.getScore()));
        assertEquals("CORRECT", response.getStatus());
    }

    @Test
    void getResultBySubmission_NotFound() {
        Long submissionId = 999L;
        when(judgeResultRepository.findBySubmissionId(submissionId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> resultService.getResultBySubmission(submissionId));
    }

    @Test
    void getStudentResults_Success() {
        Long studentId = 1000L;
        JudgeResult result1 = JudgeResult.builder()
                .id(1L)
                .submissionId(1L)
                .studentId(studentId)
                .score(BigDecimal.valueOf(100))
                .status(JudgeResult.ResultStatus.CORRECT)
                .createdAt(LocalDateTime.now())
                .build();

        JudgeResult result2 = JudgeResult.builder()
                .id(2L)
                .submissionId(2L)
                .studentId(studentId)
                .score(BigDecimal.valueOf(80))
                .status(JudgeResult.ResultStatus.INCORRECT)
                .createdAt(LocalDateTime.now())
                .build();

        List<JudgeResult> results = Arrays.asList(result1, result2);
        Page<JudgeResult> page = new PageImpl<>(results);

        when(judgeResultRepository.findByStudentId(eq(studentId), any(Pageable.class))).thenReturn(page);

        StudentResultListResponse response = resultService.getStudentResults(studentId, 1, 10);

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        assertEquals(1, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(2L, response.getTotalElements());
    }

    @Test
    void getProblemLeaderboard_Success() {
        Long problemId = 100L;
        JudgeResult result1 = JudgeResult.builder()
                .id(1L)
                .submissionId(1L)
                .problemId(problemId)
                .studentId(1000L)
                .score(BigDecimal.valueOf(100))
                .status(JudgeResult.ResultStatus.CORRECT)
                .createdAt(LocalDateTime.now())
                .build();

        JudgeResult result2 = JudgeResult.builder()
                .id(2L)
                .submissionId(2L)
                .problemId(problemId)
                .studentId(1001L)
                .score(BigDecimal.valueOf(90))
                .status(JudgeResult.ResultStatus.CORRECT)
                .createdAt(LocalDateTime.now())
                .build();

        List<JudgeResult> results = Arrays.asList(result1, result2);
        Page<JudgeResult> page = new PageImpl<>(results);

        when(judgeResultRepository.findByProblemIdOrderByScoreDesc(eq(problemId), any(Pageable.class))).thenReturn(page);

        LeaderboardResponse response = resultService.getProblemLeaderboard(problemId, 1, 50);

        assertNotNull(response);
        assertEquals(problemId, response.getProblemId());
        assertEquals(2, response.getEntries().size());
        assertEquals(1000L, response.getEntries().get(0).getStudentId());
        assertEquals(0, BigDecimal.valueOf(100).compareTo(response.getEntries().get(0).getBestScore()));
    }

    @Test
    void getOverallLeaderboard_Success() {
        JudgeResult result1 = JudgeResult.builder()
                .id(1L)
                .submissionId(1L)
                .problemId(100L)
                .studentId(1000L)
                .score(BigDecimal.valueOf(100))
                .status(JudgeResult.ResultStatus.CORRECT)
                .createdAt(LocalDateTime.now())
                .build();

        JudgeResult result2 = JudgeResult.builder()
                .id(2L)
                .submissionId(2L)
                .problemId(101L)
                .studentId(1000L)
                .score(BigDecimal.valueOf(90))
                .status(JudgeResult.ResultStatus.CORRECT)
                .createdAt(LocalDateTime.now())
                .build();

        List<JudgeResult> results = Arrays.asList(result1, result2);
        Page<JudgeResult> page = new PageImpl<>(results);

        when(judgeResultRepository.findAll(any(Pageable.class))).thenReturn(page);

        OverallLeaderboardResponse response = resultService.getOverallLeaderboard(1, 50);

        assertNotNull(response);
        assertEquals(1, response.getEntries().size());
        assertEquals(1000L, response.getEntries().get(0).getStudentId());
        assertEquals(0, BigDecimal.valueOf(190).compareTo(response.getEntries().get(0).getTotalScore()));
        assertEquals(2, response.getEntries().get(0).getTotalSubmissions());
    }

    @Test
    void getStatistics_WithProblemId_Success() {
        Long problemId = 100L;
        when(judgeResultRepository.countByProblemId(problemId)).thenReturn(10L);
        when(judgeResultRepository.countAcceptedByProblemId(problemId)).thenReturn(7L);
        when(judgeResultRepository.avgScoreByProblemId(problemId)).thenReturn(BigDecimal.valueOf(85.5));
        when(judgeResultRepository.maxScoreByProblemId(problemId)).thenReturn(BigDecimal.valueOf(100));
        when(judgeResultRepository.minScoreByProblemId(problemId)).thenReturn(BigDecimal.valueOf(60));
        when(judgeResultRepository.avgExecutionTimeByProblemId(problemId)).thenReturn(50L);

        StatisticsResponse response = resultService.getStatistics(problemId);

        assertNotNull(response);
        assertEquals(problemId, response.getProblemId());
        assertEquals(10L, response.getTotalSubmissions());
        assertEquals(7L, response.getAcceptedSubmissions());
        assertEquals(0, BigDecimal.valueOf(70).compareTo(response.getAcceptanceRate()));
        assertEquals(0, BigDecimal.valueOf(85.5).compareTo(response.getAverageScore()));
    }

    @Test
    void getStatistics_WithoutProblemId_Success() {
        when(judgeResultRepository.countTotal()).thenReturn(100L);
        when(judgeResultRepository.countTotalAccepted()).thenReturn(75L);
        when(judgeResultRepository.avgScoreTotal()).thenReturn(BigDecimal.valueOf(82.5));
        when(judgeResultRepository.maxScoreTotal()).thenReturn(BigDecimal.valueOf(100));
        when(judgeResultRepository.minScoreTotal()).thenReturn(BigDecimal.valueOf(50));
        when(judgeResultRepository.avgExecutionTimeTotal()).thenReturn(45L);

        StatisticsResponse response = resultService.getStatistics(null);

        assertNotNull(response);
        assertNull(response.getProblemId());
        assertEquals(100L, response.getTotalSubmissions());
        assertEquals(75L, response.getAcceptedSubmissions());
        assertEquals(0, BigDecimal.valueOf(75).compareTo(response.getAcceptanceRate()));
    }
}