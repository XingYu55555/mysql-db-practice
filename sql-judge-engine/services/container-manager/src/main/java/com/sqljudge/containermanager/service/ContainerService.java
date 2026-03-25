package com.sqljudge.containermanager.service;

import com.sqljudge.containermanager.model.dto.response.ContainerInfoResponse;
import com.sqljudge.containermanager.model.dto.response.HealthCheckResponse;
import com.sqljudge.containermanager.model.dto.response.PoolStatsResponse;
import com.sqljudge.containermanager.model.dto.response.PoolStatusResponse;

public interface ContainerService {
    ContainerInfoResponse acquireContainer(Long problemId, Integer timeout);
    void releaseContainer(String containerId, Boolean resetDatabase);
    ContainerInfoResponse getContainerInfo(String containerId);
    void destroyContainer(String containerId);
    HealthCheckResponse healthCheck(String containerId);
    PoolStatusResponse getPoolStatus();
    PoolStatsResponse getPoolStats();
    void initializePool();
    void cleanupExpiredContainers();
}
