package com.sqljudge.containermanager.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolStatusResponse {
    private Integer totalContainers;
    private Integer availableContainers;
    private Integer inUseContainers;
    private Integer errorContainers;
    private Integer poolCapacity;
    private String status;
}
