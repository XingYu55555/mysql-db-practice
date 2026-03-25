package com.sqljudge.judgeservice.model.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDetail {
    private Long problemId;
    private String title;
    private String description;
    private String difficulty;
    private String sqlType;
    private Boolean aiAssisted;
    private String status;
    private String expectedType;
    private Long teacherId;
    private String initSql;
    private String standardAnswer;
    private String expectedResult;
}