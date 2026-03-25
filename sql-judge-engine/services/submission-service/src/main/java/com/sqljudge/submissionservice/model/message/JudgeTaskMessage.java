package com.sqljudge.submissionservice.model.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeTaskMessage implements Serializable {
    private String messageId;
    private Long submissionId;
    private Long problemId;
    private String sqlContent;
    private Long studentId;
    private Integer timeLimit;
    private Integer maxMemory;
    private Integer retryCount;
    private String timestamp;
}
