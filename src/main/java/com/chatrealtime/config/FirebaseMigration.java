package com.chatrealtime.config;

import com.google.firebase.database.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class FirebaseMigration {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseMigration.class);
    private final FirebaseDatabase database;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public FirebaseMigration(FirebaseDatabase database, PasswordEncoder passwordEncoder) {
        this.database = database;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationEvent(ContextRefreshedEvent event) {
        migrateData();
    }

    private void migrateData() {
        try {
            logger.info("Starting data migration check...");
            logger.debug("Initializing migration process with database URL: {}", database.getReference().toString());
            CompletableFuture<Void> migrationFuture = new CompletableFuture<>();

            // Vérifier les données existantes
            database.getReference().addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    try {
                        logger.debug("Checking existing data structure...");
                        Map<String, Object> updates = new HashMap<>();
                        
                        // Vérifier et créer les utilisateurs
                        if (!snapshot.hasChild("users_metadata")) {
                            logger.info("users_metadata node missing, creating sample data...");
                            updates.put("users_metadata", createSampleUsers());
                        } else {
                            logger.info("users_metadata exists with {} users", snapshot.child("users_metadata").getChildrenCount());
                        }

                        // Vérifier et créer la présence
                        if (!snapshot.hasChild("presence")) {
                            logger.info("presence node missing, creating sample data...");
                            updates.put("presence", createSamplePresence(getExistingUserIds(snapshot)));
                        } else {
                            logger.info("presence exists with {} records", snapshot.child("presence").getChildrenCount());
                        }

                        // Vérifier et créer les relations utilisateurs
                        if (!snapshot.hasChild("user_relations")) {
                            logger.info("user_relations node missing, creating sample data...");
                            updates.put("user_relations", createSampleUserRelations(getExistingUserIds(snapshot)));
                        } else {
                            logger.info("user_relations exists with {} records", snapshot.child("user_relations").getChildrenCount());
                        }

                        // Vérifier et créer les groupes
                        if (!snapshot.hasChild("groups")) {
                            logger.info("groups node missing, creating sample data...");
                            updates.put("groups", createSampleGroups());
                        } else {
                            logger.info("groups exists with {} records", snapshot.child("groups").getChildrenCount());
                        }

                        // Vérifier et créer les conversations
                        if (!snapshot.hasChild("conversations")) {
                            logger.info("conversations node missing, creating sample data...");
                            updates.put("conversations", createSampleConversations(getExistingUserIds(snapshot)));
                        } else {
                            logger.info("conversations exists with {} records", snapshot.child("conversations").getChildrenCount());
                        }

                        // Vérifier et créer les messages
                        if (!snapshot.hasChild("messages")) {
                            logger.info("messages node missing, creating sample data...");
                            Set<String> conversationIds = snapshot.hasChild("conversations") ?
                                ((Map<String, Object>) snapshot.child("conversations").getValue()).keySet() :
                                createSampleConversations(getExistingUserIds(snapshot)).keySet();
                            updates.put("messages", createSampleMessages(conversationIds, getExistingUserIds(snapshot)));
                        } else {
                            logger.info("messages exists with {} records", snapshot.child("messages").getChildrenCount());
                        }

                        // Vérifier et créer les derniers messages
                        if (!snapshot.hasChild("last_messages")) {
                            logger.info("last_messages node missing, creating sample data...");
                            Set<String> conversationIds = snapshot.hasChild("conversations") ?
                                ((Map<String, Object>) snapshot.child("conversations").getValue()).keySet() :
                                createSampleConversations(getExistingUserIds(snapshot)).keySet();
                            updates.put("last_messages", createSampleLastMessages(conversationIds, getExistingUserIds(snapshot)));
                        } else {
                            logger.info("last_messages exists with {} records", snapshot.child("last_messages").getChildrenCount());
                        }

                        // Si des mises à jour sont nécessaires
                        if (!updates.isEmpty()) {
                            logger.info("Found {} missing nodes, applying migrations...", updates.size());
                            database.getReference().updateChildren(updates, (error, ref) -> {
                                if (error != null) {
                                    logger.error("Error during data migration: {} at ref: {}", error.getMessage(), ref);
                                    migrationFuture.completeExceptionally(error.toException());
                                } else {
                                    logger.info("Successfully migrated {} nodes", updates.size());
                                    migrationFuture.complete(null);
                                }
                            });
                        } else {
                            logger.info("All data structures exist, no migration needed");
                            migrationFuture.complete(null);
                        }
                    } catch (Exception e) {
                        logger.error("Error during migration process: {}", e.getMessage(), e);
                        migrationFuture.completeExceptionally(e);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    logger.error("Data migration cancelled: {} at ref: {}", error.getMessage(), error.getCode());
                    migrationFuture.completeExceptionally(error.toException());
                }
            });

            // Attendre la fin de la migration avec timeout
            try {
                logger.debug("Waiting for migration to complete...");
                migrationFuture.get(30, TimeUnit.SECONDS);
                logger.info("Migration process completed successfully");
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.error("Error during data migration: {}", e.getMessage(), e);
                throw new RuntimeException("Data migration failed", e);
            }

        } catch (Exception e) {
            logger.error("Unexpected error during data migration: {}", e.getMessage(), e);
            throw new RuntimeException("Data migration failed", e);
        }
    }

    private Set<String> getExistingUserIds(DataSnapshot snapshot) {
        if (snapshot.hasChild("users_metadata")) {
            return ((Map<String, Object>) snapshot.child("users_metadata").getValue()).keySet();
        }
        return createSampleUsers().keySet();
    }

    private Map<String, Object> createSampleUsers() {
        Map<String, Object> users = new HashMap<>();
        
        // Utilisateur 1
        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", "user1");
        user1.put("name", "John Doe");
        user1.put("email", "john@example.com");
        user1.put("password", passwordEncoder.encode("password123"));
        user1.put("status", "active");
        user1.put("createdAt", System.currentTimeMillis());
        users.put("user1", user1);

        // Utilisateur 2
        Map<String, Object> user2 = new HashMap<>();
        user2.put("id", "user2");
        user2.put("name", "Jane Smith");
        user2.put("email", "jane@example.com");
        user2.put("password", passwordEncoder.encode("password123"));
        user2.put("status", "active");
        user2.put("createdAt", System.currentTimeMillis());
        users.put("user2", user2);

        // Utilisateur 3
        Map<String, Object> user3 = new HashMap<>();
        user3.put("id", "user3");
        user3.put("name", "Bob Wilson");
        user3.put("email", "bob@example.com");
        user3.put("password", passwordEncoder.encode("password123"));
        user3.put("status", "active");
        user3.put("createdAt", System.currentTimeMillis());
        users.put("user3", user3);

        return users;
    }

    private Map<String, Object> createSamplePresence(Set<String> userIds) {
        Map<String, Object> presence = new HashMap<>();
        for (String userId : userIds) {
            Map<String, Object> userPresence = new HashMap<>();
            userPresence.put("status", "online");
            userPresence.put("lastOnline", System.currentTimeMillis());
            presence.put(userId, userPresence);
        }
        return presence;
    }

    private Map<String, Object> createSampleUserRelations(Set<String> userIds) {
        Map<String, Object> relations = new HashMap<>();
        List<String> userList = new ArrayList<>(userIds);

        for (int i = 0; i < userList.size(); i++) {
            Map<String, Object> userContacts = new HashMap<>();
            for (int j = 0; j < userList.size(); j++) {
                if (i != j) {
                    Map<String, Object> contact = new HashMap<>();
                    contact.put("status", "accepted");
                    contact.put("since", System.currentTimeMillis());
                    userContacts.put(userList.get(j), contact);
                }
            }
            relations.put(userList.get(i), userContacts);
        }
        return relations;
    }

    private Map<String, Object> createSampleGroups() {
        Map<String, Object> groups = new HashMap<>();

        // Groupe 1
        Map<String, Object> group1 = new HashMap<>();
        group1.put("id", "group1");
        group1.put("name", "Project Team");
        group1.put("description", "Team discussion group");
        group1.put("createdAt", System.currentTimeMillis());
        group1.put("createdBy", "user1");
        groups.put("group1", group1);

        // Groupe 2
        Map<String, Object> group2 = new HashMap<>();
        group2.put("id", "group2");
        group2.put("name", "Friends Chat");
        group2.put("description", "Friends group chat");
        group2.put("createdAt", System.currentTimeMillis());
        group2.put("createdBy", "user2");
        groups.put("group2", group2);

        return groups;
    }

    private Map<String, Object> createSampleConversations(Set<String> userIds) {
        Map<String, Object> conversations = new HashMap<>();
        List<String> userList = new ArrayList<>(userIds);

        for (int i = 0; i < userList.size(); i++) {
            for (int j = i + 1; j < userList.size(); j++) {
                String conversationId = userList.get(i) + "_" + userList.get(j);
                Map<String, Object> conversation = new HashMap<>();
                conversation.put("type", "private");
                conversation.put("createdAt", System.currentTimeMillis());
                
                Map<String, Boolean> participants = new HashMap<>();
                participants.put(userList.get(i), true);
                participants.put(userList.get(j), true);
                conversation.put("participants", participants);
                
                conversations.put(conversationId, conversation);
            }
        }

        return conversations;
    }

    private Map<String, Object> createSampleMessages(Set<String> conversationIds, Set<String> userIds) {
        Map<String, Object> messages = new HashMap<>();
        List<String> userList = new ArrayList<>(userIds);

        for (String conversationId : conversationIds) {
            Map<String, Object> conversationMessages = new HashMap<>();
            
            // Ajouter quelques messages exemple
            for (int i = 0; i < 3; i++) {
                String messageId = UUID.randomUUID().toString();
                Map<String, Object> message = new HashMap<>();
                message.put("senderId", userList.get(i % userList.size()));
                message.put("content", "Sample message " + (i + 1));
                message.put("timestamp", System.currentTimeMillis() - (1000 * 60 * i)); // Messages espacés d'une minute
                message.put("status", "delivered");
                
                conversationMessages.put(messageId, message);
            }
            
            messages.put(conversationId, conversationMessages);
        }

        return messages;
    }

    private Map<String, Object> createSampleLastMessages(Set<String> conversationIds, Set<String> userIds) {
        Map<String, Object> lastMessages = new HashMap<>();
        List<String> userList = new ArrayList<>(userIds);

        for (String conversationId : conversationIds) {
            Map<String, Object> lastMessage = new HashMap<>();
            lastMessage.put("senderId", userList.get(0));
            lastMessage.put("content", "Last message in conversation");
            lastMessage.put("timestamp", System.currentTimeMillis());
            lastMessage.put("status", "delivered");
            
            lastMessages.put(conversationId, lastMessage);
        }

        return lastMessages;
    }
} 