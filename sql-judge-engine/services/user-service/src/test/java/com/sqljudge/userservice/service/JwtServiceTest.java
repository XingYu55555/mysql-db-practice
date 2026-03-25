package com.sqljudge.userservice.service;

import com.sqljudge.userservice.model.entity.User;
import com.sqljudge.userservice.service.impl.JwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtServiceImpl jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "secretKey", "mySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLong123456789");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .role(User.Role.STUDENT)
                .build();
    }

    @Test
    void generateToken_Success() {
        String token = jwtService.generateToken(testUser);

        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    void extractUserId_Success() {
        String token = jwtService.generateToken(testUser);
        Long userId = jwtService.extractUserId(token);

        assertEquals(1L, userId);
    }

    @Test
    void extractUsername_Success() {
        String token = jwtService.generateToken(testUser);
        String username = jwtService.extractUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        String token = jwtService.generateToken(testUser);
        boolean isValid = jwtService.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        boolean isValid = jwtService.validateToken("invalid-token");

        assertFalse(isValid);
    }
}
