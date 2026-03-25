package com.sqljudge.containermanager.controller;

import com.sqljudge.containermanager.model.dto.request.AcquireContainerRequest;
import com.sqljudge.containermanager.model.dto.request.ReleaseContainerRequest;
import com.sqljudge.containermanager.model.dto.response.ContainerInfoResponse;
import com.sqljudge.containermanager.model.dto.response.HealthCheckResponse;
import com.sqljudge.containermanager.service.ContainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/container")
@RequiredArgsConstructor
@Tag(name = "Container", description = "Container management endpoints (internal)")
public class ContainerController {

    private final ContainerService containerService;

    @PostMapping("/acquire")
    @Operation(summary = "Acquire a container from pool")
    public ResponseEntity<ContainerInfoResponse> acquireContainer(
            @RequestBody(required = false) AcquireContainerRequest request) {
        Long problemId = request != null ? request.getProblemId() : null;
        Integer timeout = request != null ? request.getTimeout() : 10000;
        ContainerInfoResponse response = containerService.acquireContainer(problemId, timeout);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/release")
    @Operation(summary = "Release a container back to pool")
    public ResponseEntity<Void> releaseContainer(@Valid @RequestBody ReleaseContainerRequest request) {
        containerService.releaseContainer(request.getContainerId(), request.getResetDatabase());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{containerId}")
    @Operation(summary = "Get container info")
    public ResponseEntity<ContainerInfoResponse> getContainerInfo(@PathVariable String containerId) {
        ContainerInfoResponse response = containerService.getContainerInfo(containerId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{containerId}")
    @Operation(summary = "Destroy a container")
    public ResponseEntity<Void> destroyContainer(@PathVariable String containerId) {
        containerService.destroyContainer(containerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{containerId}/health")
    @Operation(summary = "Container health check")
    public ResponseEntity<HealthCheckResponse> healthCheck(@PathVariable String containerId) {
        HealthCheckResponse response = containerService.healthCheck(containerId);
        return ResponseEntity.ok(response);
    }
}
