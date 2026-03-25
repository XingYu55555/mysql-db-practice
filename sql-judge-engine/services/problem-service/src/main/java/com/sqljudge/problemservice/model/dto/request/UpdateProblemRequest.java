package com.sqljudge.problemservice.model.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProblemRequest {
    private String title;
    private String description;
    private String difficulty;
    private Boolean aiAssisted;
    private String initSql;
    private String standardAnswer;
    private String expectedType;
}
