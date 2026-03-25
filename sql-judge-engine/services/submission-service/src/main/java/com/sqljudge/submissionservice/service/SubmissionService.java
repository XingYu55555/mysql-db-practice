package com.sqljudge.submissionservice.service;

import com.sqljudge.submissionservice.model.dto.request.CreateSubmissionRequest;
import com.sqljudge.submissionservice.model.dto.response.SubmissionDetailResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionListResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionStatusResponse;
import com.sqljudge.submissionservice.model.entity.Submission;

public interface SubmissionService {
    SubmissionResponse createSubmission(CreateSubmissionRequest request, Long studentId);
    SubmissionListResponse listSubmissions(Long studentId, Long problemId, String status, int page, int size, String sort);
    SubmissionDetailResponse getSubmissionDetail(Long submissionId);
    SubmissionStatusResponse getSubmissionStatus(Long submissionId);
}
