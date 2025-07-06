package com.chatrealtime.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    private FirebaseDatabase database;
    private int retryCount = 0;
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 5000;

    @Value("${firebase.database.url}")
    private String databaseUrl;

    @Value("${firebase.credentials.path}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() {
        try {
            initializeFirebase();
            setupDatabaseConnection();
        } catch (Exception e) {
            logger.error("Error initializing Firebase: {}", e.getMessage());
            System.exit(1);
        }
    }

    private void initializeFirebase() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount = new ClassPathResource(credentialsPath).getInputStream();
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(databaseUrl)
                    .build();

            FirebaseApp.initializeApp(options);
            logger.info("Firebase has been initialized");
        }
    }

    private void setupDatabaseConnection() {
        database = FirebaseDatabase.getInstance();
        DatabaseReference connectedRef = database.getReference(".info/connected");
        
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    logger.info("Connected to Firebase Realtime Database");
                    retryCount = 0;
                } else {
                    logger.warn("Disconnected from Firebase Realtime Database");
                    if (retryCount < MAX_RETRIES) {
                        retryCount++;
                        logger.info("Attempting to reconnect to Firebase... (Attempt {}/{})", retryCount, MAX_RETRIES);
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                            database.goOnline();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        logger.error("Failed to connect to Firebase after {} attempts", MAX_RETRIES);
                        System.exit(1);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                logger.error("Firebase connection error: {}", error.getMessage());
            }
        });
    }

    @Bean
    public FirebaseDatabase firebaseDatabase() {
        return database;
    }
} 