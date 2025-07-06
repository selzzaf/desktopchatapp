package com.chatrealtime.service;

import com.chatrealtime.model.User;
import com.chatrealtime.security.JwtTokenProvider;
import com.google.firebase.database.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    private final FirebaseDatabase database;
    private final BCryptPasswordEncoder passwordEncoder;
    
    public AuthService(FirebaseDatabase database) {
        this.database = database;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }
    
    public CompletableFuture<User> register(String email, String password, String name) {
        CompletableFuture<User> future = new CompletableFuture<>();
        DatabaseReference usersRef = database.getReference("users");
        
        // Log registration attempt
        logger.info("Attempting to register user with email: {}", email);
        
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    logger.warn("Registration failed: Email already exists: {}", email);
                    future.completeExceptionally(new RuntimeException("Email déjà utilisé"));
                    return;
                }
                
                String hashedPassword = passwordEncoder.encode(password);
                String userId = UUID.randomUUID().toString();
                
                User newUser = new User();
                newUser.setId(userId);
                newUser.setEmail(email);  // Ensure email is set correctly
                newUser.setPassword(hashedPassword);
                newUser.setName(name != null && !name.isEmpty() ? name : email);  // Use email as name if name is not provided
                newUser.setStatus("offline");
                
                // Create a new user with a specific ID instead of using push()
                DatabaseReference newUserRef = usersRef.child(userId);
                
                newUserRef.setValue(newUser, (error, ref) -> {
                    if (error != null) {
                        logger.error("Failed to save user to database: {}", error.getMessage());
                        future.completeExceptionally(error.toException());
                    } else {
                        logger.info("Successfully registered user: {}", email);
                        future.complete(newUser);
                    }
                });
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                logger.error("Registration cancelled: {}", databaseError.getMessage());
                future.completeExceptionally(databaseError.toException());
            }
        });
        
        return future;
    }
    
    public User login(String email, String password) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        Query query = usersRef.orderByChild("email").equalTo(email);
        
        CompletableFuture<User> future = new CompletableFuture<>();
        
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    future.completeExceptionally(new RuntimeException("Utilisateur non trouvé"));
                    return;
                }

                DataSnapshot userSnapshot = dataSnapshot.getChildren().iterator().next();
                User user = userSnapshot.getValue(User.class);
                logger.debug("User retrieved: {}", user != null ? user.getEmail() : "null");
                
                String userPassword = user.getPassword() == null ? "" : user.getPassword().trim();
                if (user != null && (
                      (userPassword.startsWith("$2a$") && passwordEncoder.matches(password, userPassword))
                      || password.equals(userPassword)
                )) {
                    // Mettre à jour le statut en ligne
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("online", true);
                    updates.put("status", "online");
                    userSnapshot.getRef().updateChildren(updates, (error, ref) -> {
                        if (error != null) {
                            future.completeExceptionally(error.toException());
                            return;
                        }
                        future.complete(user);
                    });
                } else {
                    future.completeExceptionally(new RuntimeException("Mot de passe incorrect"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });

        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
    
    public CompletableFuture<Void> logout(String userId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DatabaseReference usersRef = database.getReference("users");
        usersRef.child(userId).child("status").setValue("offline", (error, ref) -> {
            if (error != null) {
                future.completeExceptionally(error.toException());
            } else {
                future.complete(null);
            }
        });
        return future;
    }
    
    public CompletableFuture<Void> updateStatus(String userId, String status) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        DatabaseReference usersRef = database.getReference("users");
        usersRef.child(userId).child("status").setValue(status, (error, ref) -> {
            if (error != null) {
                future.completeExceptionally(error.toException());
            } else {
                future.complete(null);
            }
        });
        return future;
    }
} 