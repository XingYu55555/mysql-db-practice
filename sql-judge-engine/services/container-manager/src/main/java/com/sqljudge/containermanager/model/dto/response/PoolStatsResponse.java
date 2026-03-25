package com.sqljudge.containermanager.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolStatsResponse {
    private Integer totalContainers;
    private Integer availableContainers;
    private Integer inUseContainers;
    private Integer errorContainers;
    private Integer poolCapacity;
    private Integer maxUses;
    private Double utilizationRate;
    private Long totalAcquisitions;
    private Long totalReleases;
    private Map<String, Integer> containersByStatus;
}