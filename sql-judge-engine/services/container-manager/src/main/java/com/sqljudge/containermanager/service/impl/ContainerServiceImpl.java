package com.sqljudge.containermanager.service.impl;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.sqljudge.containermanager.config.PoolConfig;
import com.sqljudge.containermanager.model.dto.response.ContainerInfoResponse;
import com.sqljudge.containermanager.model.dto.response.HealthCheckResponse;
import com.sqljudge.containermanager.model.dto.response.PoolStatsResponse;
import com.sqljudge.containermanager.model.dto.response.PoolStatusResponse;
import com.sqljudge.containermanager.model.entity.Container;
import com.sqljudge.containermanager.model.entity.Difficulty;
import com.sqljudge.containermanager.repository.ContainerRepository;
import com.sqljudge.containermanager.service.ContainerService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerServiceImpl implements ContainerService {

    private final ContainerRepository containerRepository;
    private final DockerClient dockerClient;
    private final PoolConfig poolConfig;

    @Value("${app.docker.judge-image}")
    private String judgeImage;

    @Value("${app.docker.network}")
    private String network;

    @Value("${app.mysql.root-password}")
    private String mysqlRootPassword;

    private final AtomicLong totalAcquisitions = new AtomicLong(0);
    private final AtomicLong totalReleases = new AtomicLong(0);

    @PostConstruct
    public void init() {
        initializePool();
    }

    @Override
    @Transactional
    public void initializePool() {
        log.info("Initializing container pool...");
        long currentCount = containerRepository.count();
        log.info("Current container count in DB: {}", currentCount);

        int targetSize = poolConfig.getSize();
        if (currentCount < targetSize) {
            int containersToCreate = targetSize - (int) currentCount;
            log.info("Creating {} new containers for pool", containersToCreate);

            for (int i = 0; i < containersToCreate; i++) {
                try {
                    createAndRegisterContainer(i);
                } catch (Exception e) {
                    log.error("Failed to create container {}: {}", i, e.getMessage());
                }
            }
        }

        log.info("Container pool initialization complete");
    }

    private void createAndRegisterContainer(int index) {
        String containerName = "judge-container-" + System.currentTimeMillis() + "-" + index;
        int hostPort = 3306 + index;

        ExposedPort mysqlExposedPort = ExposedPort.tcp(3306);
        Ports portBindings = new Ports();
        portBindings.bind(mysqlExposedPort, Ports.Binding.bindPort(hostPort));

        CreateContainerCmd createCommand = dockerClient.createContainerCmd(judgeImage)
                .withName(containerName)
                .withExposedPorts(mysqlExposedPort)
                .withEnv("MYSQL_ROOT_PASSWORD=" + mysqlRootPassword)
                .withHostName("mysql-" + containerName)
                .withNetworkMode(network)
                .withPortBindings(portBindings);

        CreateContainerResponse response = createCommand.exec();
        String containerId = response.getId();

        dockerClient.startContainerCmd(containerId).exec();

        String ipAddress = extractIpAddress(containerId);

        Container container = Container.builder()
                .dockerContainerId(containerId)
                .containerName(containerName)
                .ipAddress(ipAddress)
                .mysqlPort(hostPort)
                .status(Container.ContainerStatus.AVAILABLE)
                .useCount(0)
                .maxUses(poolConfig.getMaxUses())
                .build();

        containerRepository.save(container);
        log.info("Created container: {} with IP: {}", containerName, ipAddress);
    }

    private String extractIpAddress(String containerId) {
        try {
            InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
            if (inspectResponse != null && inspectResponse.getNetworkSettings() != null) {
                Map<String, ContainerNetwork> networks = inspectResponse.getNetworkSettings().getNetworks();
                if (networks != null && networks.containsKey(network)) {
                    ContainerNetwork networkSettings = networks.get(network);
                    if (networkSettings != null && networkSettings.getIpAddress() != null) {
                        return networkSettings.getIpAddress();
                    }
                }
                if (networks != null && !networks.isEmpty()) {
                    ContainerNetwork firstNetwork = networks.values().iterator().next();
                    if (firstNetwork != null) {
                        return firstNetwork.getIpAddress();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract IP for container {}: {}", containerId, e.getMessage());
        }
        return "172.17.0.2";
    }

    @Override
    @Transactional
    public ContainerInfoResponse acquireContainer(Long problemId, Integer timeout) {
        log.info("Acquiring container for problem: {}, timeout: {}ms", problemId, timeout);

        Difficulty difficulty = calculateDifficulty(timeout);
        int timeoutMinutes = difficulty.getTimeoutMinutes();

        Container container = containerRepository.findAvailableContainer()
                .orElseThrow(() -> new RuntimeException("No available containers in pool"));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(timeoutMinutes);

        container.setStatus(Container.ContainerStatus.IN_USE);
        container.setConnectionToken(token);
        container.setTokenExpiresAt(expiresAt);
        container.setUseCount(container.getUseCount() + 1);
        container.setProblemId(problemId);
        container.setDifficulty(difficulty);
        container.setLastUsedAt(LocalDateTime.now());
        containerRepository.save(container);

        totalAcquisitions.incrementAndGet();

        log.info("Container acquired: {}, token expires at: {}", container.getDockerContainerId(), expiresAt);

        return ContainerInfoResponse.builder()
                .containerId(container.getDockerContainerId())
                .containerName(container.getContainerName())
                .ipAddress(container.getIpAddress())
                .mysqlPort(container.getMysqlPort())
                .mysqlUser("judge")
                .connectionToken(token)
                .tokenExpiresAt(expiresAt.toString())
                .status(container.getStatus().name())
                .build();
    }

    private Difficulty calculateDifficulty(Integer timeout) {
        if (timeout == null) {
            return Difficulty.MEDIUM;
        }
        if (timeout <= 10000) {
            return Difficulty.EASY;
        } else if (timeout <= 30000) {
            return Difficulty.MEDIUM;
        } else {
            return Difficulty.HARD;
        }
    }

    @Override
    @Transactional
    public void releaseContainer(String containerId, Boolean resetDatabase) {
        log.info("Releasing container: {}, resetDatabase: {}", containerId, resetDatabase);

        Container container = containerRepository.findByDockerContainerId(containerId)
                .orElseThrow(() -> new RuntimeException("Container not found: " + containerId));

        if (resetDatabase != null && resetDatabase) {
            resetContainerDatabase(container);
        }

        container.setStatus(Container.ContainerStatus.AVAILABLE);
        container.setConnectionToken(null);
        container.setTokenExpiresAt(null);
        container.setProblemId(null);
        container.setDifficulty(null);
        containerRepository.save(container);

        totalReleases.incrementAndGet();

        log.info("Container released: {}", containerId);
    }

    private void resetContainerDatabase(Container container) {
        log.info("Resetting database for container: {}", container.getDockerContainerId());
        try {
            String jdbcUrl = String.format("jdbc:mysql://%s:%d/mysql",
                    container.getIpAddress(), container.getMysqlPort());
            try (Connection conn = DriverManager.getConnection(jdbcUrl, "root", mysqlRootPassword)) {
                log.debug("Successfully connected to container MySQL for reset");
            }
        } catch (SQLException e) {
            log.warn("Could not reset database for container {}: {}",
                    container.getDockerContainerId(), e.getMessage());
        }
    }

    @Override
    public ContainerInfoResponse getContainerInfo(String containerId) {
        Container container = containerRepository.findByDockerContainerId(containerId)
                .orElseThrow(() -> new RuntimeException("Container not found: " + containerId));

        return ContainerInfoResponse.builder()
                .containerId(container.getDockerContainerId())
                .containerName(container.getContainerName())
                .ipAddress(container.getIpAddress())
                .mysqlPort(container.getMysqlPort())
                .mysqlUser("judge")
                .connectionToken(container.getConnectionToken())
                .tokenExpiresAt(container.getTokenExpiresAt() != null ?
                        container.getTokenExpiresAt().toString() : null)
                .status(container.getStatus().name())
                .build();
    }

    @Override
    @Transactional
    public void destroyContainer(String containerId) {
        log.info("Destroying container: {}", containerId);

        Container container = containerRepository.findByDockerContainerId(containerId)
                .orElseThrow(() -> new RuntimeException("Container not found: " + containerId));

        try {
            dockerClient.stopContainerCmd(containerId).exec();
            dockerClient.removeContainerCmd(containerId).exec();
        } catch (Exception e) {
            log.warn("Error stopping/removing Docker container {}: {}", containerId, e.getMessage());
        }

        container.setStatus(Container.ContainerStatus.DESTROYED);
        containerRepository.save(container);

        log.info("Container destroyed: {}", containerId);
    }

    @Override
    public HealthCheckResponse healthCheck(String containerId) {
        log.debug("Performing health check for container: {}", containerId);

        Container container = containerRepository.findByDockerContainerId(containerId)
                .orElseThrow(() -> new RuntimeException("Container not found: " + containerId));

        boolean healthy = false;
        String mysqlConnected = "UNKNOWN";
        String message = "Health check completed";

        try {
            InspectContainerResponse inspectResponse =
                    dockerClient.inspectContainerCmd(containerId).exec();

            if (inspectResponse != null && inspectResponse.getState() != null) {
                healthy = inspectResponse.getState().getRunning();
                if (!healthy) {
                    message = "Container is not running";
                    mysqlConnected = "DISCONNECTED";
                }
            }

            if (healthy) {
                String jdbcUrl = String.format("jdbc:mysql://%s:%d/mysql",
                        container.getIpAddress(), container.getMysqlPort());
                try (Connection conn = DriverManager.getConnection(jdbcUrl, "root", mysqlRootPassword)) {
                    mysqlConnected = "CONNECTED";
                    message = "Container is healthy, MySQL is reachable";
                } catch (SQLException e) {
                    mysqlConnected = "ERROR: " + e.getMessage();
                    message = "Container is running but MySQL is not reachable";
                    healthy = false;
                }
            }

        } catch (Exception e) {
            message = "Health check failed: " + e.getMessage();
            healthy = false;
            mysqlConnected = "ERROR";
        }

        return HealthCheckResponse.builder()
                .containerId(containerId)
                .healthy(healthy)
                .mysqlConnected(mysqlConnected)
                .message(message)
                .build();
    }

    @Override
    public PoolStatusResponse getPoolStatus() {
        long total = containerRepository.count();
        long available = containerRepository.countByStatus(Container.ContainerStatus.AVAILABLE);
        long inUse = containerRepository.countByStatus(Container.ContainerStatus.IN_USE);
        long error = containerRepository.countByStatus(Container.ContainerStatus.ERROR);

        String status;
        if (available == 0) {
            status = "DEPLETED";
        } else if (available < poolConfig.getSize() / 2) {
            status = "LOW";
        } else {
            status = "NORMAL";
        }

        return PoolStatusResponse.builder()
                .totalContainers((int) total)
                .availableContainers((int) available)
                .inUseContainers((int) inUse)
                .errorContainers((int) error)
                .poolCapacity(poolConfig.getSize())
                .status(status)
                .build();
    }

    @Override
    public PoolStatsResponse getPoolStats() {
        long total = containerRepository.count();
        long available = containerRepository.countByStatus(Container.ContainerStatus.AVAILABLE);
        long inUse = containerRepository.countByStatus(Container.ContainerStatus.IN_USE);
        long error = containerRepository.countByStatus(Container.ContainerStatus.ERROR);

        Map<String, Integer> containersByStatus = new HashMap<>();
        containersByStatus.put("AVAILABLE", (int) available);
        containersByStatus.put("IN_USE", (int) inUse);
        containersByStatus.put("ERROR", (int) error);

        double utilizationRate = total > 0 ? (double) inUse / total * 100 : 0;

        return PoolStatsResponse.builder()
                .totalContainers((int) total)
                .availableContainers((int) available)
                .inUseContainers((int) inUse)
                .errorContainers((int) error)
                .poolCapacity(poolConfig.getSize())
                .maxUses(poolConfig.getMaxUses())
                .utilizationRate(utilizationRate)
                .totalAcquisitions(totalAcquisitions.get())
                .totalReleases(totalReleases.get())
                .containersByStatus(containersByStatus)
                .build();
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredContainers() {
        log.debug("Running expired container cleanup...");

        List<Container> inUseContainers = containerRepository.findAllByStatus(
                Container.ContainerStatus.IN_USE);

        for (Container container : inUseContainers) {
            if (container.isExpired()) {
                log.info("Container {} token expired, releasing", container.getDockerContainerId());
                releaseContainer(container.getDockerContainerId(), false);
            }
        }

        List<Container> containersToRetire = containerRepository.findContainersNeedingReplacement();
        for (Container container : containersToRetire) {
            if (container.getStatus() != Container.ContainerStatus.DESTROYED) {
                log.info("Container {} exceeded max uses, replacing", container.getDockerContainerId());
                destroyContainer(container.getDockerContainerId());
            }
        }
    }
}