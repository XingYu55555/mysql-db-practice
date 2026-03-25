package com.sqljudge.submissionservice.controller;

import com.sqljudge.submissionservice.model.dto.request.CreateSubmissionRequest;
import com.sqljudge.submissionservice.model.dto.response.SubmissionDetailResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionListResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionStatusResponse;
import com.sqljudge.submissionservice.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/submission")
@RequiredArgsConstructor
@Tag(name = "Submission", description = "Submission management endpoints")
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @Operation(summary = "Submit SQL answer for judging")
    public ResponseEntity<SubmissionResponse> createSubmission(
            @Valid @RequestBody CreateSubmissionRequest request,
            @RequestHeader("X-User-Id") Long studentId) {
        SubmissionResponse response = submissionService.createSubmission(request, studentId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping
    @Operation(summary = "List submission history with pagination")
    public ResponseEntity<SubmissionListResponse> listSubmissions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "submittedAt,desc") String sort,
            @RequestHeader("X-User-Id") Long studentId) {
        SubmissionListResponse response = submissionService.listSubmissions(studentId, problemId, status, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{submissionId}")
    @Operation(summary = "Get submission detail")
    public ResponseEntity<SubmissionDetailResponse> getSubmissionDetail(@PathVariable Long submissionId) {
        SubmissionDetailResponse response = submissionService.getSubmissionDetail(submissionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{submissionId}/status")
    @Operation(summary = "Get submission status")
    public ResponseEntity<SubmissionStatusResponse> getSubmissionStatus(@PathVariable Long submissionId) {
        SubmissionStatusResponse response = submissionService.getSubmissionStatus(submissionId);
        return ResponseEntity.ok(response);
    }
}
