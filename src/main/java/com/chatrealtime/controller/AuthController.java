package com.chatrealtime.controller;

import com.chatrealtime.model.User;
import com.chatrealtime.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<User>> register(@RequestBody Map<String, String> request) {
        logger.info("Received registration request for email: {}", request.get("email"));
        
        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");

        return authService.register(email, password, name)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> {
                logger.error("Registration error: {}", throwable.getMessage());
                return ResponseEntity.badRequest().build();
            });
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        logger.info("Received login request for email: {}", credentials.get("email"));
        try {
            User user = authService.login(credentials.get("email"), credentials.get("password"));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Login error: {}", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public CompletableFuture<ResponseEntity<Void>> logout(@RequestHeader("Authorization") String token) {
        String userId = extractUserIdFromToken(token);
        
        return authService.logout(userId)
            .<ResponseEntity<Void>>thenApply(v -> ResponseEntity.ok().build())
            .exceptionally(throwable -> ResponseEntity.<Void>badRequest().build());
    }

    @PostMapping("/status")
    public CompletableFuture<ResponseEntity<Void>> updateStatus(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> request) {
        String userId = extractUserIdFromToken(token);
        String status = request.get("status");

        if (status == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.<Void>badRequest().build());
        }

        return authService.updateStatus(userId, status)
            .<ResponseEntity<Void>>thenApply(v -> ResponseEntity.ok().build())
            .exceptionally(throwable -> ResponseEntity.<Void>badRequest().build());
    }

    private String extractUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        // Extract user ID from JWT token
        return "userId"; // Implementation needed
    }
} 