package com.sqljudge.containermanager.controller;

import com.sqljudge.containermanager.model.dto.response.PoolStatsResponse;
import com.sqljudge.containermanager.model.dto.response.PoolStatusResponse;
import com.sqljudge.containermanager.service.ContainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pool")
@RequiredArgsConstructor
@Tag(name = "Pool", description = "Container pool management endpoints")
public class PoolController {

    private final ContainerService containerService;

    @GetMapping("/status")
    @Operation(summary = "Get pool status")
    public ResponseEntity<PoolStatusResponse> getPoolStatus() {
        PoolStatusResponse response = containerService.getPoolStatus();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get pool statistics")
    public ResponseEntity<PoolStatsResponse> getPoolStats() {
        PoolStatsResponse response = containerService.getPoolStats();
        return ResponseEntity.ok(response);
    }
}