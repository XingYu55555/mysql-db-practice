package com.sqljudge.userservice.service;

import com.sqljudge.userservice.model.entity.User;

public interface JwtService {
    String generateToken(User user);
    Long extractUserId(String token);
    String extractUsername(String token);
    boolean validateToken(String token);
}
