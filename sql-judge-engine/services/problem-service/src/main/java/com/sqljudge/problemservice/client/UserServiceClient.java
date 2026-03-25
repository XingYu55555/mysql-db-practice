package com.sqljudge.problemservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Client to communicate with user-service for user information.
 */
@Component
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    // Simple cache for user roles to reduce calls to user-service
    private final ConcurrentHashMap<Long, CachedRole> roleCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5);

    public UserServiceClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get user role by user ID.
     *
     * @param userId the user ID
     * @return the role (TEACHER or STUDENT), or null if not found
     */
    public String getUserRole(Long userId) {
        if (userId == null) {
            return null;
        }

        // Check cache first
        CachedRole cached = roleCache.get(userId);
        if (cached != null && !cached.isExpired()) {
            log.debug("Using cached role for user {}", userId);
            return cached.getRole();
        }

        try {
            String url = userServiceUrl + "/api/user/" + userId;
            log.debug("Fetching user role from: {}", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String role = jsonNode.path("role").asText();

                if (role != null && !role.isEmpty()) {
                    // Cache the role
                    roleCache.put(userId, new CachedRole(role));
                    log.debug("Cached role '{}' for user {}", role, userId);
                    return role;
                }
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User not found: {}", userId);
        } catch (Exception e) {
            log.error("Error fetching user role for user {}: {}", userId, e.getMessage());
        }

        return null;
    }

    /**
     * Check if user has TEACHER role.
     *
     * @param userId the user ID
     * @return true if user is a teacher, false otherwise
     */
    public boolean isTeacher(Long userId) {
        String role = getUserRole(userId);
        return "TEACHER".equals(role);
    }

    /**
     * Clear cache for a specific user.
     *
     * @param userId the user ID
     */
    public void clearCache(Long userId) {
        roleCache.remove(userId);
    }

    /**
     * Clear all cached roles.
     */
    public void clearAllCache() {
        roleCache.clear();
    }

    /**
     * Cached role entry with TTL.
     */
    private static class CachedRole {
        private final String role;
        private final long timestamp;

        CachedRole(String role) {
            this.role = role;
            this.timestamp = System.currentTimeMillis();
        }

        String getRole() {
            return role;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
