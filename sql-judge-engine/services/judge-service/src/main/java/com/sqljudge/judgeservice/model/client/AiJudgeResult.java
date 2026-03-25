package com.sqljudge.judgeservice.model.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiJudgeResult {
    private boolean isCorrect;
    private String reason;
    private double confidence;
    private boolean success;
    private String errorMessage;
    private String status;
}