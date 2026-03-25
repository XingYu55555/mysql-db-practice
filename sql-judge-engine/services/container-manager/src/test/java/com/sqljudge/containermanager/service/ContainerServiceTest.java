package com.sqljudge.containermanager.service;

import com.github.dockerjava.api.DockerClient;
import com.sqljudge.containermanager.config.PoolConfig;
import com.sqljudge.containermanager.model.dto.response.ContainerInfoResponse;
import com.sqljudge.containermanager.model.dto.response.PoolStatsResponse;
import com.sqljudge.containermanager.model.dto.response.PoolStatusResponse;
import com.sqljudge.containermanager.model.entity.Container;
import com.sqljudge.containermanager.repository.ContainerRepository;
import com.sqljudge.containermanager.service.impl.ContainerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContainerServiceTest {

    @Mock
    private ContainerRepository containerRepository;

    @Mock
    private DockerClient dockerClient;

    @Mock
    private PoolConfig poolConfig;

    private ContainerServiceImpl containerService;

    @BeforeEach
    void setUp() {
        containerService = new ContainerServiceImpl(containerRepository, dockerClient, poolConfig);
        lenient().when(poolConfig.getSize()).thenReturn(5);
        lenient().when(poolConfig.getMaxUses()).thenReturn(100);
    }

    @Test
    void acquireContainer_Success_EasyDifficulty() {
        Container availableContainer = createAvailableContainer();

        when(containerRepository.findAvailableContainer()).thenReturn(Optional.of(availableContainer));
        when(containerRepository.save(any())).thenReturn(availableContainer);

        ContainerInfoResponse response = containerService.acquireContainer(1L, 5000);

        assertNotNull(response);
        assertEquals("container-123", response.getContainerId());
        assertEquals("IN_USE", response.getStatus());
        assertNotNull(response.getConnectionToken());
        assertNotNull(response.getTokenExpiresAt());
        verify(containerRepository).save(any());
    }

    @Test
    void acquireContainer_Success_MediumDifficulty() {
        Container availableContainer = createAvailableContainer();

        when(containerRepository.findAvailableContainer()).thenReturn(Optional.of(availableContainer));
        when(containerRepository.save(any())).thenReturn(availableContainer);

        ContainerInfoResponse response = containerService.acquireContainer(1L, 20000);

        assertNotNull(response);
        assertEquals("container-123", response.getContainerId());
        assertEquals("IN_USE", response.getStatus());
    }

    @Test
    void acquireContainer_Success_HardDifficulty() {
        Container availableContainer = createAvailableContainer();

        when(containerRepository.findAvailableContainer()).thenReturn(Optional.of(availableContainer));
        when(containerRepository.save(any())).thenReturn(availableContainer);

        ContainerInfoResponse response = containerService.acquireContainer(1L, 60000);

        assertNotNull(response);
        assertEquals("container-123", response.getContainerId());
        assertEquals("IN_USE", response.getStatus());
    }

    @Test
    void acquireContainer_NoAvailable_ThrowsException() {
        when(containerRepository.findAvailableContainer()).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                containerService.acquireContainer(1L, 10000));

        assertEquals("No available containers in pool", exception.getMessage());
    }

    @Test
    void releaseContainer_Success() {
        Container container = createInUseContainer();

        when(containerRepository.findByDockerContainerId("container-123"))
                .thenReturn(Optional.of(container));
        when(containerRepository.save(any())).thenReturn(container);

        containerService.releaseContainer("container-123", false);

        verify(containerRepository).save(argThat(c ->
                c.getStatus() == Container.ContainerStatus.AVAILABLE &&
                        c.getConnectionToken() == null &&
                        c.getTokenExpiresAt() == null));
    }

    @Test
    void releaseContainer_NotFound_ThrowsException() {
        when(containerRepository.findByDockerContainerId("nonexistent"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                containerService.releaseContainer("nonexistent", false));

        assertTrue(exception.getMessage().contains("Container not found"));
    }

    @Test
    void getContainerInfo_Success() {
        Container container = createAvailableContainer();

        when(containerRepository.findByDockerContainerId("container-123"))
                .thenReturn(Optional.of(container));

        ContainerInfoResponse response = containerService.getContainerInfo("container-123");

        assertNotNull(response);
        assertEquals("container-123", response.getContainerId());
        assertEquals("judge-container-1", response.getContainerName());
        assertEquals("172.17.0.2", response.getIpAddress());
        assertEquals(3306, response.getMysqlPort());
    }

    @Test
    void getContainerInfo_NotFound_ThrowsException() {
        when(containerRepository.findByDockerContainerId("nonexistent"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                containerService.getContainerInfo("nonexistent"));

        assertTrue(exception.getMessage().contains("Container not found"));
    }

    @Test
    void destroyContainer_Success() {
        Container container = createAvailableContainer();

        when(containerRepository.findByDockerContainerId("container-123"))
                .thenReturn(Optional.of(container));
        when(containerRepository.save(any())).thenReturn(container);

        containerService.destroyContainer("container-123");

        verify(dockerClient).stopContainerCmd("container-123");
        verify(containerRepository).save(argThat(c ->
                c.getStatus() == Container.ContainerStatus.DESTROYED));
    }

    @Test
    void destroyContainer_NotFound_ThrowsException() {
        when(containerRepository.findByDockerContainerId("nonexistent"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                containerService.destroyContainer("nonexistent"));

        assertTrue(exception.getMessage().contains("Container not found"));
    }

    @Test
    void getPoolStatus_Success() {
        when(containerRepository.count()).thenReturn(5L);
        when(containerRepository.countByStatus(Container.ContainerStatus.AVAILABLE)).thenReturn(3L);
        when(containerRepository.countByStatus(Container.ContainerStatus.IN_USE)).thenReturn(2L);
        when(containerRepository.countByStatus(Container.ContainerStatus.ERROR)).thenReturn(0L);

        PoolStatusResponse response = containerService.getPoolStatus();

        assertNotNull(response);
        assertEquals(5, response.getTotalContainers());
        assertEquals(3, response.getAvailableContainers());
        assertEquals(2, response.getInUseContainers());
        assertEquals(0, response.getErrorContainers());
        assertEquals(5, response.getPoolCapacity());
        assertEquals("NORMAL", response.getStatus());
    }

    @Test
    void getPoolStatus_DepletedPool() {
        when(containerRepository.count()).thenReturn(5L);
        when(containerRepository.countByStatus(Container.ContainerStatus.AVAILABLE)).thenReturn(0L);
        when(containerRepository.countByStatus(Container.ContainerStatus.IN_USE)).thenReturn(5L);
        when(containerRepository.countByStatus(Container.ContainerStatus.ERROR)).thenReturn(0L);

        PoolStatusResponse response = containerService.getPoolStatus();

        assertEquals("DEPLETED", response.getStatus());
    }

    @Test
    void getPoolStats_Success() {
        when(containerRepository.count()).thenReturn(5L);
        when(containerRepository.countByStatus(Container.ContainerStatus.AVAILABLE)).thenReturn(3L);
        when(containerRepository.countByStatus(Container.ContainerStatus.IN_USE)).thenReturn(2L);
        when(containerRepository.countByStatus(Container.ContainerStatus.ERROR)).thenReturn(0L);

        PoolStatsResponse response = containerService.getPoolStats();

        assertNotNull(response);
        assertEquals(5, response.getTotalContainers());
        assertEquals(3, response.getAvailableContainers());
        assertEquals(2, response.getInUseContainers());
        assertEquals(0, response.getErrorContainers());
        assertEquals(5, response.getPoolCapacity());
        assertEquals(100, response.getMaxUses());
        assertNotNull(response.getContainersByStatus());
        assertEquals(3, response.getContainersByStatus().get("AVAILABLE"));
    }

    @Test
    void containerIsExpired_True() {
        Container container = new Container();
        container.setTokenExpiresAt(LocalDateTime.now().minusMinutes(10));

        assertTrue(container.isExpired());
    }

    @Test
    void containerIsExpired_False() {
        Container container = new Container();
        container.setTokenExpiresAt(LocalDateTime.now().plusMinutes(10));

        assertFalse(container.isExpired());
    }

    @Test
    void containerShouldRetire_True() {
        Container container = new Container();
        container.setMaxUses(100);
        container.setUseCount(100);

        assertTrue(container.shouldRetire());
    }

    @Test
    void containerShouldRetire_False() {
        Container container = new Container();
        container.setMaxUses(100);
        container.setUseCount(50);

        assertFalse(container.shouldRetire());
    }

    private Container createAvailableContainer() {
        return Container.builder()
                .id(1L)
                .dockerContainerId("container-123")
                .containerName("judge-container-1")
                .ipAddress("172.17.0.2")
                .mysqlPort(3306)
                .status(Container.ContainerStatus.AVAILABLE)
                .useCount(5)
                .maxUses(100)
                .build();
    }

    private Container createInUseContainer() {
        return Container.builder()
                .id(1L)
                .dockerContainerId("container-123")
                .containerName("judge-container-1")
                .ipAddress("172.17.0.2")
                .mysqlPort(3306)
                .status(Container.ContainerStatus.IN_USE)
                .useCount(5)
                .maxUses(100)
                .connectionToken("test-token")
                .tokenExpiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
    }
}