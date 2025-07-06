package com.chatrealtime.service;

import com.chatrealtime.model.Message;
import com.google.firebase.database.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class MessageService {
    
    private final DatabaseReference messagesRef;
    private final SimpMessagingTemplate messagingTemplate;
    
    public MessageService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.messagesRef = FirebaseDatabase.getInstance().getReference("messages");
    }
    
    public CompletableFuture<Message> sendMessage(Message message) {
        CompletableFuture<Message> future = new CompletableFuture<>();
        
        message.setTimestamp(LocalDateTime.now());
        message.setRead(false);
        
        String chatId = getChatId(message.getFrom(), message.getTo());
        String messageId = messagesRef.child(chatId).push().getKey();
        message.setId(messageId);
        
        messagesRef.child(chatId).child(messageId).setValue(message, (error, ref) -> {
            if (error != null) {
                future.completeExceptionally(error.toException());
            } else {
                // Envoyer via WebSocket
                String destination = "/topic/chat/" + message.getTo();
                messagingTemplate.convertAndSend(destination, message);
                future.complete(message);
            }
        });
        
        return future;
    }
    
    public CompletableFuture<List<Message>> getHistory(String userId1, String userId2, String type) {
        CompletableFuture<List<Message>> future = new CompletableFuture<>();
        String chatId = getChatId(userId1, userId2);
        
        messagesRef.child(chatId).orderByChild("timestamp")
            .limitToLast(100)  // Limiter à 100 messages
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<Message> messages = new ArrayList<>();
                    for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                        Message message = messageSnapshot.getValue(Message.class);
                        if (message != null) {
                            messages.add(message);
                        }
                    }
                    future.complete(messages);
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    future.completeExceptionally(error.toException());
                }
            });
        
        return future;
    }
    
    public void markAsRead(String messageId, String chatId) {
        messagesRef.child(chatId).child(messageId).child("read").setValueAsync(true);
    }
    
    public void subscribeToMessages(String userId, MessageListener listener) {
        String userChatPattern = "%" + userId + "%";
        messagesRef.orderByKey()
            .startAt(userChatPattern)
            .endAt(userChatPattern + "\uf8ff")
            .addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    Message message = snapshot.getValue(Message.class);
                    if (message != null && message.getTo().equals(userId)) {
                        listener.onNewMessage(message);
                    }
                }
                
                @Override
                public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                    // Gérer les mises à jour de messages (ex: marquage comme lu)
                }
                
                @Override
                public void onChildRemoved(DataSnapshot snapshot) {
                    // Gérer la suppression de messages
                }
                
                @Override
                public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                    // Non utilisé
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    listener.onError(error.toException());
                }
            });
    }
    
    private String getChatId(String userId1, String userId2) {
        // Pour les chats individuels, créer un ID unique et cohérent
        String[] ids = {userId1, userId2};
        Arrays.sort(ids);
        return "private_" + ids[0] + "_" + ids[1];
    }
    
    public interface MessageListener {
        void onNewMessage(Message message);
        void onError(Exception e);
    }
} 