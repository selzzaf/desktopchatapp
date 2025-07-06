package com.chatrealtime.controller;

import com.chatrealtime.model.User;
import com.chatrealtime.service.ContactService;
import com.chatrealtime.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ContactController {

    @Autowired
    private ContactService contactService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // Gestion des contacts
    @GetMapping("/contacts")
    public CompletableFuture<ResponseEntity<List<User>>> getContacts(@RequestHeader("Authorization") String token) {
        String userId = extractUserIdFromToken(token);
        return contactService.getContacts(userId)
            .thenApply(ResponseEntity::ok)
            .exceptionally(throwable -> ResponseEntity.<List<User>>badRequest().build());
    }

    @PostMapping("/contacts")
    public CompletableFuture<ResponseEntity<Void>> addContact(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> request) {
        String userId = extractUserIdFromToken(token);
        String contactId = request.get("contactId");

        if (contactId == null) {
            return CompletableFuture.completedFuture(
                ResponseEntity.<Void>badRequest().build());
        }

        return handleVoidFuture(contactService.addContact(userId, contactId));
    }

    @DeleteMapping("/contacts/{contactId}")
    public CompletableFuture<ResponseEntity<Void>> removeContact(
            @RequestHeader("Authorization") String token,
            @PathVariable String contactId) {
        String userId = extractUserIdFromToken(token);
        return handleVoidFuture(contactService.removeContact(userId, contactId));
    }

    private String extractUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    private CompletableFuture<ResponseEntity<Void>> handleVoidFuture(CompletableFuture<Void> future) {
        return future
            .<ResponseEntity<Void>>thenApply(result -> ResponseEntity.ok().build())
            .exceptionally(throwable -> ResponseEntity.badRequest().build());
    }
} 