package com.sqljudge.judgeservice.service.impl;

import com.sqljudge.judgeservice.model.client.AcquireContainerRequest;
import com.sqljudge.judgeservice.model.client.ContainerInfo;
import com.sqljudge.judgeservice.model.client.ReleaseContainerRequest;
import com.sqljudge.judgeservice.service.ContainerClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerClientServiceImpl implements ContainerClientService {

    private final RestTemplate restTemplate;

    @Value("${app.container-manager.url}")
    private String containerManagerUrl;

    @Override
    public ContainerInfo acquireContainer(Long problemId, Integer timeout) {
        String url = containerManagerUrl + "/api/container/acquire";
        AcquireContainerRequest request = AcquireContainerRequest.builder()
                .problemId(problemId)
                .timeout(timeout)
                .build();

        log.info("Acquiring container from {} for problem {}", url, problemId);
        ContainerInfo container = restTemplate.postForObject(url, request, ContainerInfo.class);
        log.info("Acquired container: {}", container != null ? container.getContainerId() : "null");
        return container;
    }

    @Override
    public void releaseContainer(String containerId, Boolean resetDatabase) {
        String url = containerManagerUrl + "/api/container/release";
        ReleaseContainerRequest request = ReleaseContainerRequest.builder()
                .containerId(containerId)
                .resetDatabase(resetDatabase)
                .build();

        log.info("Releasing container: {}, resetDatabase: {}", containerId, resetDatabase);
        restTemplate.postForObject(url, request, Object.class);
        log.info("Container released: {}", containerId);
    }
}