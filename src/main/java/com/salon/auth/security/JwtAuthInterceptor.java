package com.salon.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Reads the JWT stored in the "auth_token" cookie on every request to a
 * protected endpoint, validates it, and exposes the authenticated
 * customer's email/id as request attributes for controllers to use.
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    public static final String COOKIE_NAME = "auth_token";

    private final JwtUtil jwtUtil;

    public JwtAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = extractTokenFromCookies(request);

        if (token == null || !jwtUtil.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Missing or invalid authentication token\"}");
            return false;
        }

        Claims claims = jwtUtil.validateAndGetClaims(token);
        request.setAttribute("customerEmail", claims.getSubject());
        request.setAttribute("customerId", claims.get("customerId"));
        return true;
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
