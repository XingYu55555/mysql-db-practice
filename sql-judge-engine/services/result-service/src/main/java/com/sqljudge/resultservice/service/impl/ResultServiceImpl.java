package com.sqljudge.resultservice.service.impl;

import com.sqljudge.resultservice.model.dto.request.CreateResultRequest;
import com.sqljudge.resultservice.model.dto.response.*;
import com.sqljudge.resultservice.model.entity.JudgeResult;
import com.sqljudge.resultservice.repository.JudgeResultRepository;
import com.sqljudge.resultservice.service.ResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResultServiceImpl implements ResultService {

    private final JudgeResultRepository judgeResultRepository;

    @Override
    public ResultResponse createResult(CreateResultRequest request) {
        JudgeResult result = JudgeResult.builder()
                .submissionId(request.getSubmissionId())
                .problemId(request.getProblemId())
                .studentId(request.getStudentId())
                .score(request.getScore())
                .status(JudgeResult.ResultStatus.valueOf(request.getStatus()))
                .executionTimeMs(request.getExecutionTimeMs())
                .errorMessage(request.getErrorMessage())
                .build();

        JudgeResult saved = judgeResultRepository.save(result);

        return ResultResponse.builder()
                .resultId(saved.getId())
                .submissionId(saved.getSubmissionId())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public ResultDetailResponse getResultBySubmission(Long submissionId) {
        JudgeResult result = judgeResultRepository.findBySubmissionId(submissionId)
                .orElseThrow(() -> new RuntimeException("Result not found"));

        return ResultDetailResponse.builder()
                .resultId(result.getId())
                .submissionId(result.getSubmissionId())
                .problemId(result.getProblemId())
                .studentId(result.getStudentId())
                .score(result.getScore())
                .status(result.getStatus().name())
                .executionTimeMs(result.getExecutionTimeMs())
                .errorMessage(result.getErrorMessage())
                .createdAt(result.getCreatedAt())
                .build();
    }

    @Override
    public StudentResultListResponse getStudentResults(Long studentId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<JudgeResult> resultPage = judgeResultRepository.findByStudentId(studentId, pageable);

        List<ResultResponse> content = resultPage.getContent().stream()
                .map(r -> ResultResponse.builder()
                        .resultId(r.getId())
                        .submissionId(r.getSubmissionId())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return StudentResultListResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .build();
    }

    @Override
    public LeaderboardResponse getProblemLeaderboard(Long problemId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<JudgeResult> resultPage = judgeResultRepository.findByProblemIdOrderByScoreDesc(problemId, pageable);

        Map<Long, LeaderboardEntry> bestScores = new HashMap<>();
        for (JudgeResult r : resultPage.getContent()) {
            if (!bestScores.containsKey(r.getStudentId())) {
                LeaderboardEntry entry = LeaderboardEntry.builder()
                        .studentId(r.getStudentId())
                        .bestScore(r.getScore())
                        .latestSubmitTime(r.getCreatedAt())
                        .build();
                bestScores.put(r.getStudentId(), entry);
            }
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Long, LeaderboardEntry> entry : bestScores.entrySet()) {
            LeaderboardEntry le = entry.getValue();
            le.setRank(rank++);
            entries.add(le);
        }

        return LeaderboardResponse.builder()
                .problemId(problemId)
                .entries(entries)
                .page(page)
                .size(size)
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .build();
    }

    @Override
    public OverallLeaderboardResponse getOverallLeaderboard(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "score"));
        Page<JudgeResult> resultPage = judgeResultRepository.findAll(pageable);

        Map<Long, OverallLeaderboardEntry> studentStats = new HashMap<>();
        for (JudgeResult r : resultPage.getContent()) {
            OverallLeaderboardEntry entry = studentStats.getOrDefault(r.getStudentId(),
                    OverallLeaderboardEntry.builder()
                            .studentId(r.getStudentId())
                            .totalScore(BigDecimal.ZERO)
                            .problemsSolved(0)
                            .totalSubmissions(0)
                            .build());
            entry.setTotalScore(entry.getTotalScore().add(r.getScore()));
            entry.setTotalSubmissions(entry.getTotalSubmissions() + 1);
            if (r.getScore() != null && r.getScore().compareTo(BigDecimal.ZERO) > 0) {
                entry.setProblemsSolved(entry.getProblemsSolved() + 1);
            }
            studentStats.put(r.getStudentId(), entry);
        }

        List<OverallLeaderboardEntry> entries = studentStats.values().stream()
                .sorted((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()))
                .collect(Collectors.toList());

        int start = (page - 1) * size;
        int end = Math.min(start + size, entries.size());
        List<OverallLeaderboardEntry> pagedEntries = entries.subList(start, end);

        int rank = start + 1;
        for (OverallLeaderboardEntry e : pagedEntries) {
            e.setRank(rank++);
        }

        return OverallLeaderboardResponse.builder()
                .entries(pagedEntries)
                .page(page)
                .size(size)
                .totalElements((long) entries.size())
                .totalPages((int) Math.ceil((double) entries.size() / size))
                .build();
    }

    @Override
    public StatisticsResponse getStatistics(Long problemId) {
        if (problemId != null) {
            Long totalSubmissions = judgeResultRepository.countByProblemId(problemId);
            Long acceptedSubmissions = judgeResultRepository.countAcceptedByProblemId(problemId);
            BigDecimal acceptanceRate = totalSubmissions > 0
                    ? BigDecimal.valueOf(acceptedSubmissions).divide(BigDecimal.valueOf(totalSubmissions), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal averageScore = judgeResultRepository.avgScoreByProblemId(problemId);
            BigDecimal maxScore = judgeResultRepository.maxScoreByProblemId(problemId);
            BigDecimal minScore = judgeResultRepository.minScoreByProblemId(problemId);
            Long avgExecutionTime = judgeResultRepository.avgExecutionTimeByProblemId(problemId);

            return StatisticsResponse.builder()
                    .problemId(problemId)
                    .totalSubmissions(totalSubmissions)
                    .acceptedSubmissions(acceptedSubmissions)
                    .acceptanceRate(acceptanceRate.multiply(BigDecimal.valueOf(100)))
                    .averageScore(averageScore)
                    .maxScore(maxScore)
                    .minScore(minScore)
                    .averageExecutionTime(avgExecutionTime)
                    .build();
        } else {
            Long totalSubmissions = judgeResultRepository.countTotal();
            Long acceptedSubmissions = judgeResultRepository.countTotalAccepted();
            BigDecimal acceptanceRate = totalSubmissions > 0
                    ? BigDecimal.valueOf(acceptedSubmissions).divide(BigDecimal.valueOf(totalSubmissions), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal averageScore = judgeResultRepository.avgScoreTotal();
            BigDecimal maxScore = judgeResultRepository.maxScoreTotal();
            BigDecimal minScore = judgeResultRepository.minScoreTotal();
            Long avgExecutionTime = judgeResultRepository.avgExecutionTimeTotal();

            return StatisticsResponse.builder()
                    .totalSubmissions(totalSubmissions)
                    .acceptedSubmissions(acceptedSubmissions)
                    .acceptanceRate(acceptanceRate.multiply(BigDecimal.valueOf(100)))
                    .averageScore(averageScore)
                    .maxScore(maxScore)
                    .minScore(minScore)
                    .averageExecutionTime(avgExecutionTime)
                    .build();
        }
    }

    @Override
    public StudentReportResponse exportStudentReport(Long studentId) {
        List<JudgeResult> results = judgeResultRepository.findByStudentIdOrderByCreatedAtDesc(studentId);

        int totalSubmissions = results.size();
        int correctSubmissions = 0;
        BigDecimal totalScore = BigDecimal.ZERO;

        List<StudentReportResponse.SubmissionDetailDTO> submissionDetails = new ArrayList<>();

        for (JudgeResult r : results) {
            if (r.getScore() != null) {
                totalScore = totalScore.add(r.getScore());
                if ("CORRECT".equals(r.getStatus().name()) || "AI_APPROVED".equals(r.getStatus().name())) {
                    correctSubmissions++;
                }
            }
            submissionDetails.add(StudentReportResponse.SubmissionDetailDTO.builder()
                    .submissionId(r.getSubmissionId())
                    .problemId(r.getProblemId())
                    .score(r.getScore() != null ? r.getScore().intValue() : 0)
                    .status(r.getStatus().name())
                    .executionTimeMs(r.getExecutionTimeMs())
                    .submittedAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                    .build());
        }

        Double averageScore = totalSubmissions > 0
                ? totalScore.divide(BigDecimal.valueOf(totalSubmissions), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return StudentReportResponse.builder()
                .studentId(studentId)
                .totalSubmissions(totalSubmissions)
                .correctSubmissions(correctSubmissions)
                .averageScore(averageScore)
                .submissionDetails(submissionDetails)
                .build();
    }
}