package com.sqljudge.submissionservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDetailResponse {
    private Long submissionId;
    private Long problemId;
    private String problemTitle;
    private String sqlContent;
    private String status;
    private BigDecimal score;
    private Long executionTimeMs;
    private String judgeStatus;
    private String errorMessage;
    private LocalDateTime submittedAt;
}
