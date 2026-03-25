package com.sqljudge.problemservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDetailResponse {
    private Long problemId;
    private String title;
    private String description;
    private String difficulty;
    private String sqlType;
    private Boolean aiAssisted;
    private String status;
    private String expectedType;
    private Long teacherId;
    private LocalDateTime createdAt;
    private String initSql;
    private String standardAnswer;
    private String expectedResult;
    private List<TagResponse> tags;
}
