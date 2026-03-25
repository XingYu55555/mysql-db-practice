package com.sqljudge.judgeservice.service;

import com.sqljudge.judgeservice.model.client.AcquireContainerRequest;
import com.sqljudge.judgeservice.model.client.ContainerInfo;
import com.sqljudge.judgeservice.model.client.ReleaseContainerRequest;
import com.sqljudge.judgeservice.service.impl.ContainerClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContainerClientServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ContainerClientServiceImpl containerClientService;

    private static final String CONTAINER_MANAGER_URL = "http://localhost:8085";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(containerClientService, "containerManagerUrl", CONTAINER_MANAGER_URL);
    }

    @Test
    void testAcquireContainer_Success() {
        Long problemId = 100L;
        Integer timeout = 30000;

        ContainerInfo expectedContainer = ContainerInfo.builder()
                .containerId("container-1")
                .containerName("judge-container-1")
                .ipAddress("192.168.1.100")
                .mysqlPort(3306)
                .mysqlUser("judge")
                .connectionToken("test-token")
                .status("IN_USE")
                .build();

        when(restTemplate.postForObject(
                eq(CONTAINER_MANAGER_URL + "/api/container/acquire"),
                any(AcquireContainerRequest.class),
                eq(ContainerInfo.class)))
                .thenReturn(expectedContainer);

        ContainerInfo result = containerClientService.acquireContainer(problemId, timeout);

        assertNotNull(result);
        assertEquals("container-1", result.getContainerId());
        assertEquals("192.168.1.100", result.getIpAddress());
        assertEquals(3306, result.getMysqlPort());
        assertEquals("judge", result.getMysqlUser());
        assertEquals("test-token", result.getConnectionToken());

        verify(restTemplate).postForObject(
                eq(CONTAINER_MANAGER_URL + "/api/container/acquire"),
                any(AcquireContainerRequest.class),
                eq(ContainerInfo.class));
    }

    @Test
    void testAcquireContainer_Failure() {
        Long problemId = 100L;
        Integer timeout = 30000;

        when(restTemplate.postForObject(
                eq(CONTAINER_MANAGER_URL + "/api/container/acquire"),
                any(AcquireContainerRequest.class),
                eq(ContainerInfo.class)))
                .thenThrow(new RuntimeException("Container manager unavailable"));

        assertThrows(RuntimeException.class, () -> containerClientService.acquireContainer(problemId, timeout));

        verify(restTemplate).postForObject(
                eq(CONTAINER_MANAGER_URL + "/api/container/acquire"),
                any(AcquireContainerRequest.class),
                eq(ContainerInfo.class));
    }

    @Test
    void testReleaseContainer_Success() {
        String containerId = "container-1";
        Boolean resetDatabase = true;

        when(restTemplate.postForObject(
                eq(CONTAINER_MANAGER_URL + "/api/container/release"),
                any(ReleaseContainerRequest.class),
                eq(Object.class)))
                .thenReturn(null);

        assertDoesNotThrow(() -> containerClientService.releaseContainer(containerId, resetDatabase));

        verify(restTemplate).postForObject(
                eq(CONTAINER_MANAGER_URL + "/api/container/release"),
                any(ReleaseContainerRequest.class),
                eq(Object.class));
    }

    @Test
    void testReleaseContainer_Failure() {
        String containerId = "container-1";
        Boolean resetDatabase = true;

        when(restTemplate.postForObject(
                eq(CONTAINER_MANAGER_URL + "/api/container/release"),
                any(ReleaseContainerRequest.class),
                eq(Object.class)))
                .thenThrow(new RuntimeException("Release failed"));

        assertThrows(RuntimeException.class, () -> containerClientService.releaseContainer(containerId, resetDatabase));
    }

    @Test
    void testAcquireContainer_NullProblemId() {
        ContainerInfo expectedContainer = ContainerInfo.builder()
                .containerId("container-1")
                .status("IN_USE")
                .build();

        when(restTemplate.postForObject(
                eq(CONTAINER_MANAGER_URL + "/api/container/acquire"),
                any(AcquireContainerRequest.class),
                eq(ContainerInfo.class)))
                .thenReturn(expectedContainer);

        ContainerInfo result = containerClientService.acquireContainer(null, 30000);

        assertNotNull(result);
        assertEquals("container-1", result.getContainerId());
    }
}