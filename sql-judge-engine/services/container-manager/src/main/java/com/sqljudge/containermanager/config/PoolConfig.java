package com.sqljudge.containermanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.pool")
public class PoolConfig {
    private Integer size = 5;
    private Integer maxUses = 100;
}