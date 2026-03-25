package com.sqljudge.problemservice.repository;

import com.sqljudge.problemservice.model.entity.ProblemTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemTagRepository extends JpaRepository<ProblemTag, Long> {
    List<ProblemTag> findByProblemId(Long problemId);
    void deleteByProblemId(Long problemId);
}