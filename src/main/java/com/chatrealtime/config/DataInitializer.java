package com.chatrealtime.config;

import com.google.firebase.database.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Component
public class DataInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final FirebaseDatabase database;
    private static final int INIT_TIMEOUT_SECONDS = 30;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    @Autowired
    public DataInitializer(FirebaseDatabase database) {
        this.database = database;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationEvent(ContextRefreshedEvent event) {
        initializeDatabase();
    }

    private void initializeDatabase() {
        logger.info("Starting database initialization...");
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < MAX_RETRIES) {
            try {
                CompletableFuture<Void> initFuture = new CompletableFuture<>();
                
                // Structure de base pour l'application de chat
                Map<String, Object> initialData = new HashMap<>();
                initialData.put("users_metadata", new HashMap<>());
                initialData.put("presence", new HashMap<>());
                initialData.put("conversations", new HashMap<>());
                initialData.put("messages", new HashMap<>());
                initialData.put("user_relations", new HashMap<>());
                initialData.put("groups", new HashMap<>());
                initialData.put("last_messages", new HashMap<>());
                initialData.put("notifications", new HashMap<>());

                // Vérifier la connexion d'abord
                DatabaseReference connectedRef = database.getReference(".info/connected");
                connectedRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Boolean connected = snapshot.getValue(Boolean.class);
                        if (Boolean.TRUE.equals(connected)) {
                            // Une fois connecté, vérifier si la base de données est vide
                            database.getReference().addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (!snapshot.exists()) {
                                        // La base de données est vide, initialiser avec la structure de base
                                        database.getReference().updateChildren(initialData, (error, ref) -> {
                                            if (error != null) {
                                                logger.error("Error initializing database: {}", error.getMessage());
                                                initFuture.completeExceptionally(error.toException());
                                            } else {
                                                logger.info("Database initialized successfully");
                                                initFuture.complete(null);
                                            }
                                        });
                                    } else {
                                        logger.info("Database already initialized");
                                        initFuture.complete(null);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    logger.error("Database initialization cancelled: {}", error.getMessage());
                                    initFuture.completeExceptionally(error.toException());
                                }
                            });
                        } else {
                            initFuture.completeExceptionally(new RuntimeException("Not connected to Firebase"));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        logger.error("Connection check cancelled: {}", error.getMessage());
                        initFuture.completeExceptionally(error.toException());
                    }
                });

                // Attendre l'initialisation avec timeout
                try {
                    initFuture.get(INIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    return; // Succès, sortir de la boucle
                } catch (TimeoutException e) {
                    lastException = e;
                    logger.warn("Initialization attempt {} timed out, retrying...", retryCount + 1);
                } catch (InterruptedException | ExecutionException e) {
                    lastException = e;
                    logger.warn("Initialization attempt {} failed: {}", retryCount + 1, e.getMessage());
                }

                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (Exception e) {
                lastException = e;
                logger.error("Unexpected error during initialization attempt {}", retryCount + 1, e);
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Initialization interrupted", ie);
                    }
                }
            }
        }

        logger.error("Database initialization failed after {} attempts", MAX_RETRIES);
        throw new RuntimeException("Database initialization failed", lastException);
    }
} 