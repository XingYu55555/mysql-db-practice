package com.sqljudge.problemservice.model.dto.response;

import com.sqljudge.problemservice.model.dto.request.BatchImportError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportResponse {
    private Integer successCount;
    private Integer failCount;
    private List<ProblemResponse> problems;
    private List<BatchImportError> errors;
}