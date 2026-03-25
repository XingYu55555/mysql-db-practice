package com.sqljudge.judgeservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResult {
    private Long submissionId;
    private BigDecimal score;
    private String status;
    private String errorMessage;
    private Long executionTimeMs;
}
