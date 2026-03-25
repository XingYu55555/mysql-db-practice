package com.sqljudge.resultservice.model.dto.response;

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
public class ResultDetailResponse {
    private Long resultId;
    private Long submissionId;
    private Long problemId;
    private String problemTitle;
    private Long studentId;
    private String studentUsername;
    private BigDecimal score;
    private String status;
    private Long executionTimeMs;
    private String errorMessage;
    private LocalDateTime createdAt;
}
