package com.chatrealtime.controller;

import com.chatrealtime.model.Message;
import com.chatrealtime.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Arrays;

@Controller
public class WebSocketController {

    @Autowired
    private MessageService messageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        String userId = getUserId(headerAccessor);
        message.setFrom(userId);
        messageService.sendMessage(message)
            .thenAccept(sentMessage -> {
                // Le message est envoyé via le MessageService qui utilise SimpMessagingTemplate
            })
            .exceptionally(throwable -> {
                // Gérer l'erreur
                return null;
            });
    }

    @MessageMapping("/chat.typing")
    public void notifyTyping(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        String userId = getUserId(headerAccessor);
        // Envoyer une notification de saisie aux autres utilisateurs
        String destination = "/topic/chat/" + message.getTo() + "/typing";
        // Le MessageService s'occupe d'envoyer la notification
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        String userId = getUserId(headerAccessor);
        String chatId = getChatId(message.getFrom(), message.getTo());
        messageService.markAsRead(message.getId(), chatId);
    }

    private String getUserId(SimpMessageHeaderAccessor headerAccessor) {
        // Récupérer l'ID utilisateur depuis le token JWT dans les en-têtes
        String token = headerAccessor.getFirstNativeHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            // Extract user ID from JWT token
            return "userId"; // Implementation needed
        }
        throw new IllegalStateException("Non authentifié");
    }

    private String getChatId(String userId1, String userId2) {
        // Pour les chats individuels, créer un ID unique et cohérent
        String[] ids = {userId1, userId2};
        Arrays.sort(ids);
        return "private_" + ids[0] + "_" + ids[1];
    }
} 