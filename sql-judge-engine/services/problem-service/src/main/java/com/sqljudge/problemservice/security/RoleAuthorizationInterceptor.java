package com.sqljudge.problemservice.security;

import com.sqljudge.problemservice.client.UserServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Interceptor to check role-based authorization for endpoints marked with @TeacherOnly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    private final UserServiceClient userServiceClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only check HandlerMethod (controller methods)
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        Class<?> controllerClass = handlerMethod.getBeanType();

        // Check if method or class has @TeacherOnly annotation
        boolean requiresTeacher = method.isAnnotationPresent(TeacherOnly.class)
                || controllerClass.isAnnotationPresent(TeacherOnly.class);

        if (!requiresTeacher) {
            return true;
        }

        log.debug("Endpoint requires TEACHER role: {}.{}", controllerClass.getSimpleName(), method.getName());

        // Get user ID from header
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            log.warn("Missing X-User-Id header for protected endpoint");
            sendForbidden(response, "Missing user identification");
            return false;
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            log.warn("Invalid X-User-Id header: {}", userIdHeader);
            sendForbidden(response, "Invalid user identification");
            return false;
        }

        // Check if user is a teacher
        if (!userServiceClient.isTeacher(userId)) {
            log.warn("User {} attempted to access teacher-only endpoint without permission", userId);
            sendForbidden(response, "Access denied: TEACHER role required");
            return false;
        }

        log.debug("User {} authorized as TEACHER for endpoint {}.{}", userId, controllerClass.getSimpleName(), method.getName());
        return true;
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String jsonResponse = String.format("{\"error\": \"Forbidden\", \"message\": \"%s\"}", message);
        response.getWriter().write(jsonResponse);
    }
}
