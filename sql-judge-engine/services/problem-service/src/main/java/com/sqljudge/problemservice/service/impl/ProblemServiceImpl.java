package com.sqljudge.problemservice.service.impl;

import com.sqljudge.problemservice.exception.InvalidStatusTransitionException;
import com.sqljudge.problemservice.exception.ProblemNotFoundException;
import com.sqljudge.problemservice.exception.UnauthorizedException;
import com.sqljudge.problemservice.model.dto.request.BatchImportError;
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
import com.sqljudge.problemservice.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemTagRepository problemTagRepository;
    private final TagRepository tagRepository;

    private static final Map<Problem.ProblemStatus, Set<Problem.ProblemStatus>> VALID_TRANSITIONS;

    static {
        VALID_TRANSITIONS = new HashMap<>();
        VALID_TRANSITIONS.put(Problem.ProblemStatus.DRAFT, Set.of(Problem.ProblemStatus.READY));
        VALID_TRANSITIONS.put(Problem.ProblemStatus.READY, Set.of(Problem.ProblemStatus.PUBLISHED, Problem.ProblemStatus.DRAFT));
        VALID_TRANSITIONS.put(Problem.ProblemStatus.PUBLISHED, Set.of(Problem.ProblemStatus.ARCHIVED, Problem.ProblemStatus.READY));
        VALID_TRANSITIONS.put(Problem.ProblemStatus.ARCHIVED, Set.of());
    }

    @Override
    @Transactional
    public ProblemResponse createProblem(CreateProblemRequest request, Long teacherId) {
        Problem problem = Problem.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .difficulty(request.getDifficulty() != null ?
                        Problem.Difficulty.valueOf(request.getDifficulty()) : Problem.Difficulty.MEDIUM)
                .sqlType(Problem.SqlType.valueOf(request.getSqlType()))
                .aiAssisted(request.getAiAssisted() != null ? request.getAiAssisted() : false)
                .initSql(request.getInitSql())
                .standardAnswer(request.getStandardAnswer())
                .expectedType(request.getExpectedType() != null ?
                        Problem.ExpectedType.valueOf(request.getExpectedType()) : null)
                .teacherId(teacherId)
                .status(Problem.ProblemStatus.DRAFT)
                .build();

        Problem saved = problemRepository.save(problem);
        return toResponse(saved);
    }

    @Override
    public ProblemDetailResponse getProblemDetail(Long problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId));

        List<TagResponse> tags = getTagsForProblem(problemId);

        return ProblemDetailResponse.builder()
                .problemId(problem.getId())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty().name())
                .sqlType(problem.getSqlType().name())
                .aiAssisted(problem.getAiAssisted())
                .status(problem.getStatus().name())
                .expectedType(problem.getExpectedType() != null ? problem.getExpectedType().name() : null)
                .teacherId(problem.getTeacherId())
                .initSql(problem.getInitSql())
                .standardAnswer(problem.getStandardAnswer())
                .expectedResult(problem.getExpectedResult())
                .createdAt(problem.getCreatedAt())
                .tags(tags)
                .build();
    }

    @Override
    public ProblemBasicResponse getProblem(Long problemId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId));

        return ProblemBasicResponse.builder()
                .problemId(problem.getId())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty().name())
                .sqlType(problem.getSqlType().name())
                .expectedType(problem.getExpectedType() != null ? problem.getExpectedType().name() : null)
                .teacherId(problem.getTeacherId())
                .createdAt(problem.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public ProblemResponse updateProblem(Long problemId, UpdateProblemRequest request, Long teacherId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId));

        if (!problem.getTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("Only the problem creator can update this problem");
        }

        if (request.getTitle() != null) problem.setTitle(request.getTitle());
        if (request.getDescription() != null) problem.setDescription(request.getDescription());
        if (request.getDifficulty() != null) {
            problem.setDifficulty(Problem.Difficulty.valueOf(request.getDifficulty()));
        }
        if (request.getAiAssisted() != null) problem.setAiAssisted(request.getAiAssisted());
        if (request.getInitSql() != null) problem.setInitSql(request.getInitSql());
        if (request.getStandardAnswer() != null) problem.setStandardAnswer(request.getStandardAnswer());
        if (request.getExpectedType() != null) {
            problem.setExpectedType(Problem.ExpectedType.valueOf(request.getExpectedType()));
        }

        Problem updated = problemRepository.save(problem);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProblem(Long problemId, Long teacherId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId));

        if (!problem.getTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("Only the problem creator can delete this problem");
        }

        problemTagRepository.deleteByProblemId(problemId);
        problemRepository.delete(problem);
    }

    @Override
    @Transactional
    public ProblemResponse updateProblemStatus(Long problemId, UpdateProblemStatusRequest request, Long teacherId) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId));

        if (!problem.getTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("Only the problem creator can update problem status");
        }

        Problem.ProblemStatus currentStatus = problem.getStatus();
        Problem.ProblemStatus targetStatus = Problem.ProblemStatus.valueOf(request.getStatus());

        if (!isValidTransition(currentStatus, targetStatus)) {
            throw new InvalidStatusTransitionException(currentStatus.name(), targetStatus.name());
        }

        problem.setStatus(targetStatus);
        Problem updated = problemRepository.save(problem);
        return toResponse(updated);
    }

    @Override
    public ProblemListResponse listProblems(int page, int size, String difficulty, String sqlType) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Problem> problemPage;

        if (difficulty != null && sqlType != null) {
            problemPage = problemRepository.findByDifficultyAndSqlType(
                    Problem.Difficulty.valueOf(difficulty),
                    Problem.SqlType.valueOf(sqlType),
                    pageable);
        } else if (difficulty != null) {
            problemPage = problemRepository.findByDifficulty(Problem.Difficulty.valueOf(difficulty), pageable);
        } else if (sqlType != null) {
            problemPage = problemRepository.findBySqlType(Problem.SqlType.valueOf(sqlType), pageable);
        } else {
            problemPage = problemRepository.findAll(pageable);
        }

        List<ProblemResponse> content = problemPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ProblemListResponse.builder()
                .content(content)
                .page(problemPage.getNumber() + 1)
                .size(problemPage.getSize())
                .totalElements(problemPage.getTotalElements())
                .totalPages(problemPage.getTotalPages())
                .build();
    }

    @Override
    public ProblemListResponse listMyProblems(Long teacherId, int page, int size, String status) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Problem> problemPage;

        if (status != null) {
            problemPage = problemRepository.findByTeacherIdAndStatus(
                    teacherId,
                    Problem.ProblemStatus.valueOf(status),
                    pageable);
        } else {
            problemPage = problemRepository.findByTeacherId(teacherId, pageable);
        }

        List<ProblemResponse> content = problemPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ProblemListResponse.builder()
                .content(content)
                .page(problemPage.getNumber() + 1)
                .size(problemPage.getSize())
                .totalElements(problemPage.getTotalElements())
                .totalPages(problemPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public BatchImportResponse batchImportProblems(BatchImportRequest request, Long teacherId) {
        List<ProblemResponse> successfulProblems = new ArrayList<>();
        List<BatchImportError> errors = new ArrayList<>();

        for (int i = 0; i < request.getProblems().size(); i++) {
            CreateProblemRequest problemRequest = request.getProblems().get(i);
            try {
                ProblemResponse response = createProblem(problemRequest, teacherId);
                successfulProblems.add(response);
            } catch (Exception e) {
                errors.add(BatchImportError.builder()
                        .index(i)
                        .title(problemRequest.getTitle())
                        .error(e.getMessage())
                        .build());
            }
        }

        return BatchImportResponse.builder()
                .successCount(successfulProblems.size())
                .failCount(errors.size())
                .problems(successfulProblems)
                .errors(errors)
                .build();
    }

    private boolean isValidTransition(Problem.ProblemStatus from, Problem.ProblemStatus to) {
        Set<Problem.ProblemStatus> validNextStates = VALID_TRANSITIONS.get(from);
        return validNextStates != null && validNextStates.contains(to);
    }

    private ProblemResponse toResponse(Problem problem) {
        List<TagResponse> tags = getTagsForProblem(problem.getId());

        return ProblemResponse.builder()
                .problemId(problem.getId())
                .title(problem.getTitle())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty().name())
                .sqlType(problem.getSqlType().name())
                .aiAssisted(problem.getAiAssisted())
                .status(problem.getStatus().name())
                .expectedType(problem.getExpectedType() != null ? problem.getExpectedType().name() : null)
                .teacherId(problem.getTeacherId())
                .createdAt(problem.getCreatedAt())
                .tags(tags)
                .build();
    }

    private List<TagResponse> getTagsForProblem(Long problemId) {
        List<ProblemTag> problemTags = problemTagRepository.findByProblemId(problemId);
        if (problemTags.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> tagIds = problemTags.stream()
                .map(ProblemTag::getTagId)
                .collect(Collectors.toList());

        return tagRepository.findAllById(tagIds).stream()
                .map(tag -> TagResponse.builder()
                        .tagId(tag.getId())
                        .name(tag.getName())
                        .color(tag.getColor())
                        .createdAt(tag.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}