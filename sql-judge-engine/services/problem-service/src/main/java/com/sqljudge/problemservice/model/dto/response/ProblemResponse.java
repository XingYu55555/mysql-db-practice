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
public class ProblemResponse {
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
    private List<TagResponse> tags;
}
