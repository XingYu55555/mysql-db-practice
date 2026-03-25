package com.sqljudge.judgeservice.model.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInfo {
    private String containerId;
    private String containerName;
    private String ipAddress;
    private Integer mysqlPort;
    private String mysqlUser;
    private String connectionToken;
    private String tokenExpiresAt;
    private String status;
}
