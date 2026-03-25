package com.sqljudge.problemservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProblemRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be less than 200 characters")
    private String title;

    private String description;

    @Builder.Default
    private String difficulty = "MEDIUM";

    @NotBlank(message = "SQL type is required")
    private String sqlType;

    @Builder.Default
    private Boolean aiAssisted = false;

    private String initSql;

    private String standardAnswer;

    private String expectedType;
}