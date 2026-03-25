package com.sqljudge.submissionservice.repository;

import com.sqljudge.submissionservice.model.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Page<Submission> findByStudentId(Long studentId, Pageable pageable);
    Page<Submission> findByStudentIdAndProblemId(Long studentId, Long problemId, Pageable pageable);
    Page<Submission> findByStudentIdAndStatus(Long studentId, Submission.SubmissionStatus status, Pageable pageable);
    Page<Submission> findByStudentIdAndProblemIdAndStatus(Long studentId, Long problemId, Submission.SubmissionStatus status, Pageable pageable);
}
