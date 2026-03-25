package com.sqljudge.resultservice.controller;

import com.sqljudge.resultservice.model.dto.request.CreateResultRequest;
import com.sqljudge.resultservice.model.dto.response.*;
import com.sqljudge.resultservice.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/result")
@RequiredArgsConstructor
@Tag(name = "Result", description = "Result management endpoints")
@SecurityRequirement(name = "BearerAuth")
public class ResultController {

    private final ResultService resultService;

    @PostMapping
    @Operation(summary = "Create judging result (internal)", description = "judge-service callback to store judging result")
    public ResponseEntity<ResultResponse> createResult(@Valid @RequestBody CreateResultRequest request) {
        ResultResponse response = resultService.createResult(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/submission/{submissionId}")
    @Operation(summary = "Get result by submission ID", description = "Get judging result details by submission ID")
    public ResponseEntity<ResultDetailResponse> getResultBySubmission(@PathVariable Long submissionId) {
        ResultDetailResponse response = resultService.getResultBySubmission(submissionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get student results", description = "Get all results for a student")
    public ResponseEntity<StudentResultListResponse> getStudentResults(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        StudentResultListResponse response = resultService.getStudentResults(studentId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/problem/{problemId}/leaderboard")
    @Operation(summary = "Get problem leaderboard", description = "Get leaderboard for a specific problem")
    public ResponseEntity<LeaderboardResponse> getProblemLeaderboard(
            @PathVariable Long problemId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        LeaderboardResponse response = resultService.getProblemLeaderboard(problemId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get overall leaderboard", description = "Get overall ranking across all problems")
    public ResponseEntity<OverallLeaderboardResponse> getOverallLeaderboard(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        OverallLeaderboardResponse response = resultService.getOverallLeaderboard(page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get judging statistics", description = "Get statistics for judging results (teacher only)")
    public ResponseEntity<StatisticsResponse> getStatistics(@RequestParam(required = false) Long problemId) {
        StatisticsResponse response = resultService.getStatistics(problemId);
        return ResponseEntity.ok(response);
    }
}