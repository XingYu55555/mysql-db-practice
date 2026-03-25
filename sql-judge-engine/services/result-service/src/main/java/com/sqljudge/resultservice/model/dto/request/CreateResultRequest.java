package com.sqljudge.resultservice.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateResultRequest {

    @NotNull(message = "Submission ID is required")
    private Long submissionId;

    private Long problemId;

    private Long studentId;

    @NotNull(message = "Score is required")
    private BigDecimal score;

    @NotNull(message = "Status is required")
    private String status;

    private Long executionTimeMs;
    private String errorMessage;
    private Map<String, Object> metadata;
}