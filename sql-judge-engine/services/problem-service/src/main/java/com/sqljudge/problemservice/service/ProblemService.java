package com.sqljudge.problemservice.service;

import com.sqljudge.problemservice.model.dto.request.BatchImportRequest;
import com.sqljudge.problemservice.model.dto.request.CreateProblemRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemRequest;
import com.sqljudge.problemservice.model.dto.request.UpdateProblemStatusRequest;
import com.sqljudge.problemservice.model.dto.response.*;

public interface ProblemService {
    ProblemResponse createProblem(CreateProblemRequest request, Long teacherId);

    ProblemDetailResponse getProblemDetail(Long problemId);

    ProblemBasicResponse getProblem(Long problemId);

    ProblemResponse updateProblem(Long problemId, UpdateProblemRequest request, Long teacherId);

    void deleteProblem(Long problemId, Long teacherId);

    ProblemResponse updateProblemStatus(Long problemId, UpdateProblemStatusRequest request, Long teacherId);

    ProblemListResponse listProblems(int page, int size, String difficulty, String sqlType);

    ProblemListResponse listMyProblems(Long teacherId, int page, int size, String status);

    BatchImportResponse batchImportProblems(BatchImportRequest request, Long teacherId);
}