package com.sqljudge.resultservice.repository;

import com.sqljudge.resultservice.model.entity.JudgeResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface JudgeResultRepository extends JpaRepository<JudgeResult, Long> {
    Optional<JudgeResult> findBySubmissionId(Long submissionId);

    Page<JudgeResult> findBySubmissionIdIn(List<Long> submissionIds, Pageable pageable);

    Page<JudgeResult> findByStudentId(Long studentId, Pageable pageable);

    @Query("SELECT jr FROM JudgeResult jr WHERE jr.problemId = :problemId ORDER BY jr.score DESC, jr.createdAt ASC")
    Page<JudgeResult> findByProblemIdOrderByScoreDesc(@Param("problemId") Long problemId, Pageable pageable);

    @Query("SELECT jr FROM JudgeResult jr WHERE jr.studentId = :studentId AND jr.problemId = :problemId ORDER BY jr.score DESC LIMIT 1")
    Optional<JudgeResult> findBestByStudentIdAndProblemId(@Param("studentId") Long studentId, @Param("problemId") Long problemId);

    @Query("SELECT COUNT(jr) FROM JudgeResult jr WHERE jr.problemId = :problemId")
    Long countByProblemId(@Param("problemId") Long problemId);

    @Query("SELECT COUNT(jr) FROM JudgeResult jr WHERE jr.problemId = :problemId AND jr.status = 'CORRECT'")
    Long countAcceptedByProblemId(@Param("problemId") Long problemId);

    @Query("SELECT AVG(jr.score) FROM JudgeResult jr WHERE jr.problemId = :problemId")
    BigDecimal avgScoreByProblemId(@Param("problemId") Long problemId);

    @Query("SELECT MAX(jr.score) FROM JudgeResult jr WHERE jr.problemId = :problemId")
    BigDecimal maxScoreByProblemId(@Param("problemId") Long problemId);

    @Query("SELECT MIN(jr.score) FROM JudgeResult jr WHERE jr.problemId = :problemId")
    BigDecimal minScoreByProblemId(@Param("problemId") Long problemId);

    @Query("SELECT AVG(jr.executionTimeMs) FROM JudgeResult jr WHERE jr.problemId = :problemId AND jr.executionTimeMs IS NOT NULL")
    Long avgExecutionTimeByProblemId(@Param("problemId") Long problemId);

    @Query("SELECT COUNT(jr) FROM JudgeResult jr")
    Long countTotal();

    @Query("SELECT COUNT(jr) FROM JudgeResult jr WHERE jr.status = 'CORRECT'")
    Long countTotalAccepted();

    @Query("SELECT AVG(jr.score) FROM JudgeResult jr")
    BigDecimal avgScoreTotal();

    @Query("SELECT MAX(jr.score) FROM JudgeResult jr")
    BigDecimal maxScoreTotal();

    @Query("SELECT MIN(jr.score) FROM JudgeResult jr")
    BigDecimal minScoreTotal();

    @Query("SELECT AVG(jr.executionTimeMs) FROM JudgeResult jr WHERE jr.executionTimeMs IS NOT NULL")
    Long avgExecutionTimeTotal();

    List<JudgeResult> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}