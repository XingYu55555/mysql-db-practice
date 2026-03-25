package com.sqljudge.containermanager.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckResponse {
    private String containerId;
    private Boolean healthy;
    private String mysqlConnected;
    private String message;
}