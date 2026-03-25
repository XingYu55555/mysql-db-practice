package com.sqljudge.problemservice.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportRequest {

    @NotEmpty(message = "Problems list cannot be empty")
    @Valid
    private List<CreateProblemRequest> problems;
}