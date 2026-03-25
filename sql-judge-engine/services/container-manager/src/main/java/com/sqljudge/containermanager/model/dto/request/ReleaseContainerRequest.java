package com.sqljudge.containermanager.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseContainerRequest {

    @NotBlank(message = "Container ID is required")
    private String containerId;

    private Boolean resetDatabase;
}
