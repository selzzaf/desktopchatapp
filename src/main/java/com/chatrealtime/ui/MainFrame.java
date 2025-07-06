package com.chatrealtime.ui;

import com.chatrealtime.model.User;
import org.springframework.web.client.RestTemplate;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.chatrealtime.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Date;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class MainFrame extends JFrame {
    private final JPanel contactsPanel;
    private final JPanel chatPanel;
    private final JPanel statusPanel;
    private final User currentUser;
    private final String API_URL = "http://localhost:8081/api/";
    private final RestTemplate restTemplate;

    public MainFrame(User user) {
        this.currentUser = user;
        this.restTemplate = new RestTemplate();
        
        setTitle("ChatRealTime - " + user.getName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        // Layout principal
        setLayout(new BorderLayout());

        // Panel de contacts (gauche)
        contactsPanel = new ContactsPanel(currentUser);
        add(new JScrollPane(contactsPanel), BorderLayout.WEST);

        // Panel de chat (centre)
        chatPanel = new ChatPanel(currentUser);
        add(chatPanel, BorderLayout.CENTER);

        // Panel de statut (bas)
        statusPanel = new StatusPanel(currentUser);
        ((StatusPanel) statusPanel).setStatusChangeListener(new StatusPanel.StatusChangeListener() {
            @Override
            public void onStatusChanged(String status) {
                updateUserStatus(status);
            }
        });
        add(statusPanel, BorderLayout.SOUTH);

        // Barre de menu
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        // Initialiser la connexion WebSocket
        initializeWebSocket();
        
        // Écouter les sélections de chats
        ((ContactsPanel) contactsPanel).addChatSelectionListener(chatName -> {
            ((ChatPanel) chatPanel).setCurrentChat(chatName);
        });

        // Forcer le statut Online à l'ouverture
        // updateUserStatus("Online");

        // Listener pour passer Offline à la fermeture de la fenêtre
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                updateUserStatus("Offline");
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Menu Profil
        JMenu profileMenu = new JMenu("Profil");
        JMenuItem editProfileItem = new JMenuItem("Modifier le profil");
        JMenuItem deleteAccountItem = new JMenuItem("Supprimer le compte");
        JMenuItem logoutItem = new JMenuItem("Déconnexion");
        
        deleteAccountItem.addActionListener(e -> handleDeleteAccount());
        logoutItem.addActionListener(e -> handleLogout());
        
        editProfileItem.addActionListener(e -> showEditProfileDialog());
        
        profileMenu.add(editProfileItem);
        profileMenu.addSeparator();
        profileMenu.add(deleteAccountItem);
        profileMenu.addSeparator();
        profileMenu.add(logoutItem);
        
        // Menu Contacts
        JMenu contactsMenu = new JMenu("Contacts");
        JMenuItem addContactItem = new JMenuItem("Ajouter un contact");
        
        addContactItem.addActionListener(e -> showAddContactDialog());
        
        contactsMenu.add(addContactItem);
        
        // Menu Aide
        JMenu helpMenu = new JMenu("Aide");
        JMenuItem aboutItem = new JMenuItem("À propos");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        
        menuBar.add(profileMenu);
        menuBar.add(contactsMenu);
        menuBar.add(helpMenu);
        
        return menuBar;
    }

    private void showEditProfileDialog() {
        JDialog dialog = new JDialog(this, "Modifier le profil", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField nameField = new JTextField(currentUser.getName());
        JPasswordField newPassField = new JPasswordField();
        JPasswordField confirmPassField = new JPasswordField();

        panel.add(new JLabel("Nom:"));
        panel.add(nameField);
        panel.add(new JLabel("Nouveau mot de passe:"));
        panel.add(newPassField);
        panel.add(new JLabel("Confirmer:"));
        panel.add(confirmPassField);

        JButton saveButton = new JButton("Enregistrer");
        saveButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newPass = new String(newPassField.getPassword());
            String confirm = new String(confirmPassField.getPassword());

            if (!newPass.isEmpty() && !newPass.equals(confirm)) {
                JOptionPane.showMessageDialog(dialog,
                    "Les mots de passe ne correspondent pas",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getId());

                Map<String, Object> updates = new HashMap<>();
                updates.put("name", newName);
                if (!newPass.isEmpty()) {
                    updates.put("password", newPass);
                }

                userRef.updateChildrenAsync(updates);
                currentUser.setName(newName);
                setTitle("ChatRealTime - " + newName);
                
                JOptionPane.showMessageDialog(dialog,
                    "Profil mis à jour avec succès!",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                    "Erreur lors de la mise à jour: " + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);
        
        dialog.setLayout(new BorderLayout());
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }

    private void handleLogout() {
        try {
            updateUserStatus("offline");
            
            // Déconnexion de Firebase
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.getId());
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("online", false);
            userRef.updateChildrenAsync(updates)
                .addListener(() -> {
                    dispose();
                    new LoginFrame().setVisible(true);
                }, Runnable::run);
        } catch (Exception e) {
            System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
            dispose();
            new LoginFrame().setVisible(true);
        }
    }

    private void showAboutDialog() {
        String aboutText = "ChatRealTime v1.0\n\n" +
                "Une application de chat en temps réel développée avec:\n" +
                "- Java Swing pour l'interface graphique\n" +
                "- Spring Boot pour le backend\n" +
                "- Firebase pour la persistance des données\n" +
                "- WebSocket pour la communication en temps réel\n\n" +
                "© 2025 Tous droits réservés";
                
        JTextArea textArea = new JTextArea(aboutText);
        textArea.setEditable(false);
        textArea.setBackground(null);
        textArea.setFont(new Font("Dialog", Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
        JOptionPane.showMessageDialog(this,
                textArea,
                "À propos de ChatRealTime",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAddContactDialog() {
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        Object[] message = {
            "Nom:", nameField,
            "Email:", emailField
        };
        int option = JOptionPane.showConfirmDialog(null, message, "Ajouter un contact", 
            JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            if (!name.isEmpty() && !email.isEmpty()) {
                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                usersRef.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                    String contactId = userSnapshot.child("id").getValue(String.class);
                                    // Ajouter le contact dans contacts/{currentUser.id}/{contactId} avec la bonne structure
                                    DatabaseReference contactsRef = FirebaseDatabase.getInstance().getReference("contacts").child(currentUser.getId()).child(contactId);
                                    Map<String, Object> contactData = new HashMap<>();
                                    contactData.put("unread", false);
                                    contactsRef.setValue(contactData, null);
                                    // Mettre à jour le nom si besoin
                                    userSnapshot.getRef().child("name").setValue(name, null);
                                }
                                JOptionPane.showMessageDialog(null, 
                                    "Contact ajouté avec succès!",
                                    "Succès",
                                    JOptionPane.INFORMATION_MESSAGE);
                                // Rafraîchir la liste des contacts
                                ((ContactsPanel) contactsPanel).refreshContacts();
                            } else {
                                JOptionPane.showMessageDialog(null, 
                                    "Cet email n'existe pas .",
                                    "Erreur",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                            JOptionPane.showMessageDialog(null, 
                                "Erreur lors de la vérification de l'email: " + error.getMessage(),
                                "Erreur",
                                JOptionPane.ERROR_MESSAGE);
                        }
                    });
            }
        }
    }

    private void updateUserStatus(String status) {
        try {
            // Mettre à jour le statut dans Firebase directement
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.getId());
            Map<String, Object> updates = new HashMap<>();
            String statusKey = status;
            updates.put("status", statusKey);
            userRef.updateChildrenAsync(updates)
                .addListener(() -> {
                    ((StatusPanel) statusPanel).setStatusUIOnly(status);
                }, Runnable::run);
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour du statut: " + e.getMessage());
        }
    }

    private void initializeWebSocket() {
        try {
            // Mettre à jour le statut initial
            updateUserStatus("online");
            
            // Écouter les changements de statut des autres utilisateurs
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
            usersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    updateContacts();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("Erreur de synchronisation: " + error.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Erreur d'initialisation WebSocket: " + e.getMessage());
        }
    }

    public void updateStatus(String status) {
        ((StatusPanel) statusPanel).updateStatus(status);
        updateUserStatus(status);
    }

    public void addNewMessage(String from, String message) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("content", message);
        messageData.put("senderName", from);
        messageData.put("timestamp", System.currentTimeMillis());
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonMessage = mapper.writeValueAsString(messageData);
            handleMessage(jsonMessage);
        } catch (Exception e) {
            ((ChatPanel) chatPanel).addSystemMessage("Erreur lors de l'affichage du message");
        }
    }

    public void updateContacts() {
        ((ContactsPanel) contactsPanel).refreshContacts();
    }

    public void handleMessage(String message) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode messageNode = mapper.readTree(message);
            
            String content = messageNode.get("content").asText();
            String sender = messageNode.get("senderName").asText();
            long timestamp = messageNode.get("timestamp").asLong();
            
            String formattedMessage = String.format("[%s] %s: %s\n",
                new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date(timestamp)),
                sender,
                content);
                
            if (chatPanel instanceof ChatPanel) {
                ((ChatPanel) chatPanel).getTextPane().setText(
                    ((ChatPanel) chatPanel).getTextPane().getText() + formattedMessage
                );
                ((ChatPanel) chatPanel).getTextPane().setCaretPosition(
                    ((ChatPanel) chatPanel).getTextPane().getDocument().getLength()
                );
            }
        } catch (Exception e) {
            // Handle message parsing error
        }
    }

    private void handleDeleteAccount() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Ceci est une suppression définitive de votre compte. Êtes-vous sûr ?",
                "Suppression du compte",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getId());
                userRef.removeValueAsync().addListener(() -> {
                    dispose();
                    new LoginFrame().setVisible(true);
                }, Runnable::run);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erreur lors de la suppression: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
} 