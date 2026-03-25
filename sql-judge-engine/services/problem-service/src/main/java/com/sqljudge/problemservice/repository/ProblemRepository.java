package com.sqljudge.problemservice.repository;

import com.sqljudge.problemservice.model.entity.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long>, JpaSpecificationExecutor<Problem> {
    Page<Problem> findAll(Pageable pageable);
    List<Problem> findByTeacherId(Long teacherId);
    Page<Problem> findByTeacherId(Long teacherId, Pageable pageable);
    Page<Problem> findByTeacherIdAndStatus(Long teacherId, Problem.ProblemStatus status, Pageable pageable);
    Page<Problem> findByDifficultyAndSqlType(Problem.Difficulty difficulty, Problem.SqlType sqlType, Pageable pageable);
    Page<Problem> findByDifficulty(Problem.Difficulty difficulty, Pageable pageable);
    Page<Problem> findBySqlType(Problem.SqlType sqlType, Pageable pageable);
}
