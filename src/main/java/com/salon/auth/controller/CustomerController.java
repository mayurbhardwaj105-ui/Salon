package com.salon.auth.controller;

import com.salon.auth.model.Customer;
import com.salon.auth.repository.CustomerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Every endpoint here is protected: JwtAuthInterceptor already validated
 * the "auth_token" cookie before these methods run (see WebConfig).
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        String email = (String) request.getAttribute("customerEmail");

        Customer customer = customerRepository.findByEmail(email).orElse(null);
        if (customer == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Customer not found"));
        }

        return ResponseEntity.ok(Map.of(
                "id", customer.getId(),
                "fullName", customer.getFullName(),
                "email", customer.getEmail(),
                "phone", customer.getPhone()
        ));
    }
}
