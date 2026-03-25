package com.sqljudge.submissionservice.service.impl;

import com.sqljudge.submissionservice.model.dto.request.CreateSubmissionRequest;
import com.sqljudge.submissionservice.model.dto.response.SubmissionDetailResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionListResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionResponse;
import com.sqljudge.submissionservice.model.dto.response.SubmissionStatusResponse;
import com.sqljudge.submissionservice.model.entity.Submission;
import com.sqljudge.submissionservice.model.message.JudgeTaskMessage;
import com.sqljudge.submissionservice.repository.SubmissionRepository;
import com.sqljudge.submissionservice.service.MessagePublisherService;
import com.sqljudge.submissionservice.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final MessagePublisherService messagePublisherService;

    @Override
    public SubmissionResponse createSubmission(CreateSubmissionRequest request, Long studentId) {
        Submission submission = Submission.builder()
                .problemId(request.getProblemId())
                .studentId(studentId)
                .sqlContent(request.getSqlContent())
                .status(Submission.SubmissionStatus.PENDING)
                .build();

        Submission saved = submissionRepository.save(submission);

        JudgeTaskMessage taskMessage = JudgeTaskMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .submissionId(saved.getId())
                .problemId(request.getProblemId())
                .sqlContent(request.getSqlContent())
                .studentId(studentId)
                .timeLimit(30)
                .maxMemory(1024)
                .retryCount(0)
                .timestamp(LocalDateTime.now().toString())
                .build();

        try {
            messagePublisherService.publishJudgeTask(taskMessage);
        } catch (Exception e) {
            saved.setStatus(Submission.SubmissionStatus.FAILED);
            submissionRepository.save(saved);
            throw new RuntimeException("Failed to publish judge task", e);
        }

        saved.setStatus(Submission.SubmissionStatus.JUDGING);
        submissionRepository.save(saved);

        return SubmissionResponse.builder()
                .submissionId(saved.getId())
                .problemId(saved.getProblemId())
                .status(saved.getStatus().name())
                .submittedAt(saved.getSubmittedAt())
                .build();
    }

    @Override
    public SubmissionListResponse listSubmissions(Long studentId, Long problemId, String status, int page, int size, String sort) {
        Sort sortOrder = parseSort(sort);
        Pageable pageable = PageRequest.of(page - 1, size, sortOrder);

        Page<Submission> submissionPage;

        if (problemId != null && status != null) {
            Submission.SubmissionStatus submissionStatus = Submission.SubmissionStatus.valueOf(status.toUpperCase());
            submissionPage = submissionRepository.findByStudentIdAndProblemIdAndStatus(studentId, problemId, submissionStatus, pageable);
        } else if (problemId != null) {
            submissionPage = submissionRepository.findByStudentIdAndProblemId(studentId, problemId, pageable);
        } else if (status != null) {
            Submission.SubmissionStatus submissionStatus = Submission.SubmissionStatus.valueOf(status.toUpperCase());
            submissionPage = submissionRepository.findByStudentIdAndStatus(studentId, submissionStatus, pageable);
        } else {
            submissionPage = submissionRepository.findByStudentId(studentId, pageable);
        }

        List<SubmissionResponse> content = submissionPage.getContent().stream()
                .map(this::toSubmissionResponse)
                .collect(Collectors.toList());

        return SubmissionListResponse.builder()
                .content(content)
                .page(submissionPage.getNumber() + 1)
                .size(submissionPage.getSize())
                .totalElements(submissionPage.getTotalElements())
                .totalPages(submissionPage.getTotalPages())
                .build();
    }

    @Override
    public SubmissionDetailResponse getSubmissionDetail(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        return SubmissionDetailResponse.builder()
                .submissionId(submission.getId())
                .problemId(submission.getProblemId())
                .sqlContent(submission.getSqlContent())
                .status(submission.getStatus().name())
                .submittedAt(submission.getSubmittedAt())
                .build();
    }

    @Override
    public SubmissionStatusResponse getSubmissionStatus(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        return SubmissionStatusResponse.builder()
                .submissionId(submission.getId())
                .status(submission.getStatus().name())
                .build();
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "submittedAt");
        }

        String[] parts = sort.split(",");
        String field = parts[0];
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, field);
    }

    private SubmissionResponse toSubmissionResponse(Submission submission) {
        return SubmissionResponse.builder()
                .submissionId(submission.getId())
                .problemId(submission.getProblemId())
                .status(submission.getStatus().name())
                .submittedAt(submission.getSubmittedAt())
                .build();
    }
}
