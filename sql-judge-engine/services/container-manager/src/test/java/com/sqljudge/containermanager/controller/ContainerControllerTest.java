package com.sqljudge.containermanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.containermanager.model.dto.request.AcquireContainerRequest;
import com.sqljudge.containermanager.model.dto.request.ReleaseContainerRequest;
import com.sqljudge.containermanager.model.dto.response.ContainerInfoResponse;
import com.sqljudge.containermanager.model.dto.response.HealthCheckResponse;
import com.sqljudge.containermanager.model.dto.response.PoolStatsResponse;
import com.sqljudge.containermanager.model.dto.response.PoolStatusResponse;
import com.sqljudge.containermanager.service.ContainerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ContainerController.class, PoolController.class})
class ContainerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContainerService containerService;

    @Test
    void acquireContainer_Success() throws Exception {
        AcquireContainerRequest request = new AcquireContainerRequest();
        request.setProblemId(1L);
        request.setTimeout(10000);

        ContainerInfoResponse response = ContainerInfoResponse.builder()
                .containerId("container-123")
                .containerName("judge-container-1")
                .ipAddress("172.17.0.2")
                .mysqlPort(3306)
                .mysqlUser("judge")
                .connectionToken("test-token")
                .tokenExpiresAt(LocalDateTime.now().plusMinutes(10).toString())
                .status("IN_USE")
                .build();

        when(containerService.acquireContainer(eq(1L), eq(10000))).thenReturn(response);

        mockMvc.perform(post("/api/container/acquire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.containerId").value("container-123"))
                .andExpect(jsonPath("$.status").value("IN_USE"))
                .andExpect(jsonPath("$.connectionToken").value("test-token"));

        verify(containerService).acquireContainer(eq(1L), eq(10000));
    }

    @Test
    void acquireContainer_WithNullProblemId() throws Exception {
        AcquireContainerRequest request = new AcquireContainerRequest();
        request.setProblemId(null);
        request.setTimeout(10000);

        ContainerInfoResponse response = ContainerInfoResponse.builder()
                .containerId("container-123")
                .status("IN_USE")
                .build();

        when(containerService.acquireContainer(eq(null), eq(10000))).thenReturn(response);

        mockMvc.perform(post("/api/container/acquire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.containerId").value("container-123"));
    }

    @Test
    void releaseContainer_Success() throws Exception {
        ReleaseContainerRequest request = new ReleaseContainerRequest();
        request.setContainerId("container-123");
        request.setResetDatabase(true);

        mockMvc.perform(post("/api/container/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(containerService).releaseContainer(eq("container-123"), eq(true));
    }

    @Test
    void getContainerInfo_Success() throws Exception {
        ContainerInfoResponse response = ContainerInfoResponse.builder()
                .containerId("container-123")
                .containerName("judge-container-1")
                .ipAddress("172.17.0.2")
                .mysqlPort(3306)
                .mysqlUser("judge")
                .status("AVAILABLE")
                .build();

        when(containerService.getContainerInfo("container-123")).thenReturn(response);

        mockMvc.perform(get("/api/container/container-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.containerId").value("container-123"))
                .andExpect(jsonPath("$.containerName").value("judge-container-1"))
                .andExpect(jsonPath("$.ipAddress").value("172.17.0.2"))
                .andExpect(jsonPath("$.mysqlPort").value(3306))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        verify(containerService).getContainerInfo("container-123");
    }

    @Test
    void destroyContainer_Success() throws Exception {
        mockMvc.perform(delete("/api/container/container-123"))
                .andExpect(status().isNoContent());

        verify(containerService).destroyContainer("container-123");
    }

    @Test
    void healthCheck_Success() throws Exception {
        HealthCheckResponse response = HealthCheckResponse.builder()
                .containerId("container-123")
                .healthy(true)
                .mysqlConnected("CONNECTED")
                .message("Container is healthy, MySQL is reachable")
                .build();

        when(containerService.healthCheck("container-123")).thenReturn(response);

        mockMvc.perform(get("/api/container/container-123/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.containerId").value("container-123"))
                .andExpect(jsonPath("$.healthy").value(true))
                .andExpect(jsonPath("$.mysqlConnected").value("CONNECTED"));

        verify(containerService).healthCheck("container-123");
    }

    @Test
    void getPoolStatus_Success() throws Exception {
        PoolStatusResponse response = PoolStatusResponse.builder()
                .totalContainers(5)
                .availableContainers(3)
                .inUseContainers(2)
                .errorContainers(0)
                .poolCapacity(5)
                .status("NORMAL")
                .build();

        when(containerService.getPoolStatus()).thenReturn(response);

        mockMvc.perform(get("/api/pool/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalContainers").value(5))
                .andExpect(jsonPath("$.availableContainers").value(3))
                .andExpect(jsonPath("$.inUseContainers").value(2))
                .andExpect(jsonPath("$.errorContainers").value(0))
                .andExpect(jsonPath("$.poolCapacity").value(5))
                .andExpect(jsonPath("$.status").value("NORMAL"));

        verify(containerService).getPoolStatus();
    }

    @Test
    void getPoolStats_Success() throws Exception {
        Map<String, Integer> containersByStatus = new HashMap<>();
        containersByStatus.put("AVAILABLE", 3);
        containersByStatus.put("IN_USE", 2);
        containersByStatus.put("ERROR", 0);

        PoolStatsResponse response = PoolStatsResponse.builder()
                .totalContainers(5)
                .availableContainers(3)
                .inUseContainers(2)
                .errorContainers(0)
                .poolCapacity(5)
                .maxUses(100)
                .utilizationRate(40.0)
                .totalAcquisitions(100L)
                .totalReleases(98L)
                .containersByStatus(containersByStatus)
                .build();

        when(containerService.getPoolStats()).thenReturn(response);

        mockMvc.perform(get("/api/pool/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalContainers").value(5))
                .andExpect(jsonPath("$.availableContainers").value(3))
                .andExpect(jsonPath("$.inUseContainers").value(2))
                .andExpect(jsonPath("$.maxUses").value(100))
                .andExpect(jsonPath("$.utilizationRate").value(40.0))
                .andExpect(jsonPath("$.totalAcquisitions").value(100))
                .andExpect(jsonPath("$.totalReleases").value(98))
                .andExpect(jsonPath("$.containersByStatus.AVAILABLE").value(3));

        verify(containerService).getPoolStats();
    }
}