package com.sqljudge.containermanager.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "containers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "docker_container_id")
    private String dockerContainerId;

    @Column(name = "container_name")
    private String containerName;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "mysql_port")
    private Integer mysqlPort;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContainerStatus status = ContainerStatus.AVAILABLE;

    @Column(name = "use_count")
    private Integer useCount = 0;

    @Column(name = "max_uses")
    private Integer maxUses = 100;

    @Column(name = "connection_token")
    private String connectionToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "problem_id")
    private Long problemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private Difficulty difficulty;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public enum ContainerStatus {
        AVAILABLE, IN_USE, ERROR, DESTROYED
    }

    public boolean isExpired() {
        return tokenExpiresAt != null && LocalDateTime.now().isAfter(tokenExpiresAt);
    }

    public boolean shouldRetire() {
        return maxUses != null && useCount != null && useCount >= maxUses;
    }
}
