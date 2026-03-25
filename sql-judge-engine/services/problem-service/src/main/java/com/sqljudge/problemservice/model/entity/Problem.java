package com.sqljudge.problemservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "problems")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "sql_type", nullable = false)
    private SqlType sqlType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProblemStatus status = ProblemStatus.DRAFT;

    @Column(name = "ai_assisted")
    @Builder.Default
    private Boolean aiAssisted = false;

    @Column(name = "init_sql", columnDefinition = "TEXT")
    private String initSql;

    @Column(name = "standard_answer", columnDefinition = "TEXT")
    private String standardAnswer;

    @Column(name = "expected_result", columnDefinition = "TEXT")
    private String expectedResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "expected_type")
    private ExpectedType expectedType;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    public enum SqlType {
        DQL, DML, DDL, DCL
    }

    public enum ProblemStatus {
        DRAFT, READY, PUBLISHED, ARCHIVED
    }

    public enum ExpectedType {
        RESULT_SET, DATA_SNAPSHOT, METADATA, PRIVILEGE
    }
}