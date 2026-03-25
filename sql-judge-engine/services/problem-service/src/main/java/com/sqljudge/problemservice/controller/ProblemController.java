package com.sqljudge.problemservice.controller;

import com.sqljudge.problemservice.model.dto.request.BatchImportRequest;
import com.sqljudge.problemservice.model.dto.request.CreateProblemRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemStatusRequest;
import com.sqljudge.problemservice.model.dto.response.*;
import com.sqljudge.problemservice.security.TeacherOnly;
import com.sqljudge.problemservice.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problem")
@RequiredArgsConstructor
@Tag(name = "Problem", description = "Problem management endpoints")
public class ProblemController {

    private final ProblemService problemService;

    @PostMapping
    @TeacherOnly
    @Operation(summary = "Create a new problem")
    public ResponseEntity<ProblemResponse> createProblem(
            @Valid @RequestBody CreateProblemRequest request,
            @RequestHeader("X-User-Id") Long teacherId) {
        ProblemResponse response = problemService.createProblem(request, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List problems with pagination")
    public ResponseEntity<ProblemListResponse> listProblems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String sqlType) {
        ProblemListResponse response = problemService.listProblems(page, size, difficulty, sqlType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/teacher/my")
    @Operation(summary = "Get current teacher's problems")
    public ResponseEntity<ProblemListResponse> listMyProblems(
            @RequestHeader("X-User-Id") Long teacherId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        ProblemListResponse response = problemService.listMyProblems(teacherId, page, size, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{problemId}")
    @Operation(summary = "Get problem basic info")
    public ResponseEntity<ProblemBasicResponse> getProblem(@PathVariable Long problemId) {
        ProblemBasicResponse response = problemService.getProblem(problemId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{problemId}")
    @TeacherOnly
    @Operation(summary = "Update problem")
    public ResponseEntity<ProblemResponse> updateProblem(
            @PathVariable Long problemId,
            @Valid @RequestBody UpdateProblemRequest request,
            @RequestHeader("X-User-Id") Long teacherId) {
        ProblemResponse response = problemService.updateProblem(problemId, request, teacherId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{problemId}")
    @TeacherOnly
    @Operation(summary = "Delete problem")
    public ResponseEntity<Void> deleteProblem(
            @PathVariable Long problemId,
            @RequestHeader("X-User-Id") Long teacherId) {
        problemService.deleteProblem(problemId, teacherId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{problemId}/status")
    @TeacherOnly
    @Operation(summary = "Update problem status")
    public ResponseEntity<ProblemResponse> updateProblemStatus(
            @PathVariable Long problemId,
            @Valid @RequestBody UpdateProblemStatusRequest request,
            @RequestHeader("X-User-Id") Long teacherId) {
        ProblemResponse response = problemService.updateProblemStatus(problemId, request, teacherId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch")
    @TeacherOnly
    @Operation(summary = "Batch import problems")
    public ResponseEntity<BatchImportResponse> batchImportProblems(
            @Valid @RequestBody BatchImportRequest request,
            @RequestHeader("X-User-Id") Long teacherId) {
        BatchImportResponse response = problemService.batchImportProblems(request, teacherId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}