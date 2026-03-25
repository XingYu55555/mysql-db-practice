package com.sqljudge.resultservice.service;

import com.sqljudge.resultservice.model.dto.request.CreateResultRequest;
import com.sqljudge.resultservice.model.dto.response.*;

public interface ResultService {
    ResultResponse createResult(CreateResultRequest request);

    ResultDetailResponse getResultBySubmission(Long submissionId);

    StudentResultListResponse getStudentResults(Long studentId, int page, int size);

    LeaderboardResponse getProblemLeaderboard(Long problemId, int page, int size);

    OverallLeaderboardResponse getOverallLeaderboard(int page, int size);

    StatisticsResponse getStatistics(Long problemId);

    StudentReportResponse exportStudentReport(Long studentId);
}