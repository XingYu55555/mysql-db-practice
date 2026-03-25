package com.sqljudge.judgeservice.service;

import com.sqljudge.judgeservice.model.client.ContainerInfo;

public interface ContainerClientService {
    ContainerInfo acquireContainer(Long problemId, Integer timeout);
    void releaseContainer(String containerId, Boolean resetDatabase);
}
