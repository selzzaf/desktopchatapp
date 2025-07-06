package com.chatrealtime.ui;

import com.chatrealtime.model.User;
import com.google.firebase.database.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.text.*;

public class ChatPanel extends JPanel {
    private final User currentUser;
    private final JTextPane textPane;
    private final JScrollPane textScrollPane;
    private final JTextField messageField;
    private final JButton sendButton;
    private String currentChatWith;
    private final DateTimeFormatter timeFormatter;
    private final JLabel titleLabel;
    private DatabaseReference messagesRef;
    private ValueEventListener messagesListener;

    public ChatPanel(User user) {
        this.currentUser = user;
        this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Titre du chat
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        titleLabel = new JLabel("Sélectionnez un contact ");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanel.add(titleLabel, BorderLayout.WEST);
        add(titlePanel, BorderLayout.NORTH);

        // Zone de chat (remplacée par JTextPane)
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Arial", Font.PLAIN, 14));
        textScrollPane = new JScrollPane(textPane);
        textScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(textScrollPane, BorderLayout.CENTER);

        // Zone de saisie du message
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        
        messageField = new JTextField();
        messageField.setEnabled(false);
        messageField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !messageField.getText().trim().isEmpty()) {
                    sendMessage();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });
        
        sendButton = new JButton("Envoyer");
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessage());

        // Ajout du bouton emoji
        JButton emojiButton = new JButton("😊");
        emojiButton.setFocusable(false);
        emojiButton.setMargin(new Insets(2, 4, 2, 4));
        emojiButton.addActionListener(e -> showEmojiPicker());
        
        JPanel inputPanel = new JPanel(new BorderLayout(2, 0));
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(emojiButton, BorderLayout.EAST);
        
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Initialiser la référence Firebase
        messagesRef = FirebaseDatabase.getInstance().getReference("messages");
    }

    public void setCurrentChat(String chatWith) {
        if (messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        this.currentChatWith = chatWith;
        textPane.setText("");
        textPane.requestFocus();
        Component[] components = getComponents();
        for (Component component : components) {
            if (component instanceof JPanel) {
                Component[] panelComponents = ((JPanel) component).getComponents();
                for (Component panelComponent : panelComponents) {
                    if (panelComponent instanceof JButton) {
                        panelComponent.setEnabled(true);
                    }
                }
            }
        }
        messageField.setEnabled(true);
        sendButton.setEnabled(true);
        messageField.requestFocus();
        // Extraire le vrai nom du contact (sans le statut)
        String contactName = chatWith.split(" \\(")[0];
        // Trouver l'ID du contact à partir de son nom
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("name").equalTo(contactName)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String contactId = userSnapshot.child("id").getValue(String.class);
                        if (contactId != null) {
                            // Marquer les messages comme lus (unread = false)
                            DatabaseReference unreadRef = FirebaseDatabase.getInstance().getReference("contacts").child(currentUser.getId()).child(contactId).child("unread");
                            unreadRef.setValue(false, null);
                            String[] ids = {currentUser.getId(), contactId};
                            java.util.Arrays.sort(ids);
                            String chatId = "private_" + ids[0] + "_" + ids[1];
                            DatabaseReference chatRef = FirebaseDatabase.getInstance()
                                .getReference("messages")
                                .child(chatId);
                            messagesListener = new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    textPane.setText("");
                                    java.util.List<java.util.Map<String, Object>> messages = new java.util.ArrayList<>();
                                    for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                                        try {
                                            String content = messageSnapshot.child("content").getValue(String.class);
                                            String senderId = messageSnapshot.child("senderId").getValue(String.class);
                                            Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                                            if (content != null && senderId != null && timestamp != null) {
                                                java.util.Map<String, Object> message = new java.util.HashMap<>();
                                                message.put("content", content);
                                                message.put("senderId", senderId);
                                                message.put("timestamp", timestamp);
                                                messages.add(message);
                                            }
                                        } catch (Exception e) {
                                            System.err.println("Erreur de lecture du message: " + e.getMessage());
                                        }
                                    }
                                    messages.sort((m1, m2) -> ((Long) m1.get("timestamp")).compareTo((Long) m2.get("timestamp")));
                                    for (java.util.Map<String, Object> msg : messages) {
                                        String time = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                                                .format(new java.util.Date((Long) msg.get("timestamp")));
                                        String content = (String) msg.get("content");
                                        String senderId = (String) msg.get("senderId");
                                        String formattedMessage;
                                        boolean isMine = senderId.equals(currentUser.getId());
                                        if (isMine) {
                                            formattedMessage = String.format("[%s] Moi: %s\n", time, content);
                                        } else {
                                            formattedMessage = String.format("[%s] %s: %s\n", time, contactName, content);
                                        }
                                        appendStyledMessage(formattedMessage, isMine);
                                    }
                                    textPane.revalidate();
                                    textPane.repaint();
                                    JScrollBar vertical = textScrollPane.getVerticalScrollBar();
                                    vertical.setValue(vertical.getMaximum());
                                }
                                @Override
                                public void onCancelled(DatabaseError error) {
                                    System.err.println("Error loading messages: " + error.getMessage());
                                    addSystemMessage("Erreur lors du chargement des messages: " + error.getMessage());
                                }
                            };
                            chatRef.addValueEventListener(messagesListener);
                            messagesRef = chatRef;
                        }
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) {
                    addSystemMessage("Erreur lors de la récupération du contact: " + error.getMessage());
                }
            });
    }

    private void sendMessage() {
        if (currentChatWith == null || messageField.getText().trim().isEmpty()) {
            return;
        }
        String messageContent = messageField.getText().trim();
        String messageId = UUID.randomUUID().toString();
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("content", messageContent);
        messageData.put("senderId", currentUser.getId());
        messageData.put("senderName", currentUser.getName());
        messageData.put("timestamp", System.currentTimeMillis());
        messagesRef.child(messageId).setValue(messageData, (error, ref) -> {
            if (error == null) {
                messageField.setText("");
                messageField.requestFocus();
                // Mettre à jour le champ unread pour le destinataire
                // Trouver l'ID du contact à partir du nom
                String contactName = currentChatWith.split(" \\(")[0];
                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                usersRef.orderByChild("name").equalTo(contactName)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                String contactId = userSnapshot.child("id").getValue(String.class);
                                if (contactId != null) {
                                    DatabaseReference unreadRef = FirebaseDatabase.getInstance().getReference("contacts").child(contactId).child(currentUser.getId()).child("unread");
                                    unreadRef.setValue(true, null);
                                }
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {}
                    });
            } else {
                addSystemMessage("Erreur d'envoi du message: " + error.getMessage());
            }
        });
    }

    public void showTypingIndicator(String username) {
        addSystemMessage(username + " est en train d'écrire...");
    }

    public void addSystemMessage(String message) {
        String timestamp = LocalDateTime.now().format(timeFormatter);
        String formattedMessage = String.format("[%s] %s\n", timestamp, message);
        appendStyledMessage(formattedMessage, true);
        textPane.revalidate();
        textPane.repaint();
        JScrollBar vertical = textScrollPane.getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    private void appendStyledMessage(String message, boolean isMine) {
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, Color.BLACK);
        StyleConstants.setFontSize(style, 14);
        if (isMine) {
            StyleConstants.setBackground(style, new Color(220, 248, 198)); // vert clair
            StyleConstants.setAlignment(style, StyleConstants.ALIGN_RIGHT);
        } else {
            StyleConstants.setBackground(style, new Color(187, 222, 251)); // bleu clair
            StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
        }
        try {
            int start = doc.getLength();
            doc.insertString(doc.getLength(), message, style);
            doc.setParagraphAttributes(start, message.length(), style, false);
        } catch (BadLocationException e) {
            // Handle document insertion error
        }
        textPane.setCaretPosition(doc.getLength());
    }

    public JTextPane getTextPane() {
        return textPane;
    }

    // Ajout du sélecteur d'emojis
    private void showEmojiPicker() {
        String[] emojis = {"😀", "😂", "😍", "😎", "😊", "😉", "😭", "😢", "😡", "👍", "🙏", "🎉", "❤️", "🔥", "😅", "😇", "😜", "🤔", "😱", "🥰"};
        JPopupMenu emojiMenu = new JPopupMenu();
        for (String emoji : emojis) {
            JMenuItem item = new JMenuItem(emoji);
            item.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            item.addActionListener(e -> insertEmoji(emoji));
            emojiMenu.add(item);
        }
        emojiMenu.show(messageField, messageField.getWidth() - 20, -emojiMenu.getPreferredSize().height);
    }

    private void insertEmoji(String emoji) {
        int pos = messageField.getCaretPosition();
        String text = messageField.getText();
        messageField.setText(text.substring(0, pos) + emoji + text.substring(pos));
        messageField.requestFocus();
        messageField.setCaretPosition(pos + emoji.length());
    }
} 