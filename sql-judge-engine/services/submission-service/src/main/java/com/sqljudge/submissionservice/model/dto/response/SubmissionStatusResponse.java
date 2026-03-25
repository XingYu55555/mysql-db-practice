package com.sqljudge.submissionservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionStatusResponse {
    private Long submissionId;
    private String status;
    private BigDecimal score;
    private String judgeStatus;
}