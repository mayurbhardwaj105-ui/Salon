package com.salon.auth.controller;

import com.salon.auth.dto.LoginRequest;
import com.salon.auth.dto.RegisterRequest;
import com.salon.auth.model.Customer;
import com.salon.auth.repository.CustomerRepository;
import com.salon.auth.security.JwtAuthInterceptor;
import com.salon.auth.security.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public AuthController(CustomerRepository customerRepository, JwtUtil jwtUtil) {
        this.customerRepository = customerRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (customerRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "An account with this email already exists"));
        }

        Customer customer = new Customer();
        customer.setFullName(req.getFullName());
        customer.setEmail(req.getEmail());
        customer.setPhone(req.getPhone());
        customer.setPasswordHash(passwordEncoder.encode(req.getPassword()));

        customerRepository.save(customer);

        Map<String, Object> body = new HashMap<>();
        body.put("message", "Registration successful");
        body.put("customerId", customer.getId());
        body.put("email", customer.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        Customer customer = customerRepository.findByEmail(req.getEmail()).orElse(null);

        if (customer == null || !passwordEncoder.matches(req.getPassword(), customer.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        String token = jwtUtil.generateToken(customer.getEmail(), customer.getId());

        Cookie cookie = new Cookie(JwtAuthInterceptor.COOKIE_NAME, token);
        cookie.setHttpOnly(true);   // not accessible from JavaScript -> mitigates XSS token theft
        cookie.setSecure(true);     // only sent over HTTPS (use false only for local http testing)
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60);  // 1 hour, matches JWT expiration
        // cookie.setAttribute("SameSite", "Strict"); // set via response header below for Servlet <6 compatibility
        response.addCookie(cookie);
        response.setHeader("Set-Cookie",
                String.format("%s=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=Strict",
                        JwtAuthInterceptor.COOKIE_NAME, token, 60 * 60));

        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "customerId", customer.getId(),
                "fullName", customer.getFullName()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(JwtAuthInterceptor.COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // expire immediately
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
