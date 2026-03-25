package com.sqljudge.problemservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemBasicResponse {
    @Builder.Default
    private String responseType = "basic";
    private Long problemId;
    private String title;
    private String description;
    private String difficulty;
    private String sqlType;
    private String expectedType;
    private Long teacherId;
    private LocalDateTime createdAt;
}