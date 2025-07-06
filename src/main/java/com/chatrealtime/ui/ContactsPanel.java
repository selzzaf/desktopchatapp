package com.chatrealtime.ui;

import com.chatrealtime.model.User;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ContactsPanel extends JPanel {
    private final User currentUser;
    private final DefaultListModel<String> contactsListModel;
    private final JList<String> contactsList;
    private Consumer<String> contactSelectionListener;
    private ChatSelectionListener chatSelectionListener;
    private final Map<String, Boolean> unreadMap = new ConcurrentHashMap<>();
    private final Map<String, String> displayNameToId = new ConcurrentHashMap<>();

    public ContactsPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Panel pour les contacts
        JPanel contactsSection = new JPanel(new BorderLayout());
        contactsSection.setBorder(BorderFactory.createTitledBorder("Contacts"));
        contactsListModel = new DefaultListModel<>();
        contactsList = new JList<>(contactsListModel);
        contactsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        contactsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = contactsList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        contactsList.setSelectedIndex(index);
                        if (e.getButton() == MouseEvent.BUTTON3) { // Clic droit
                            showContactPropertiesMenu(e.getX(), e.getY());
                        }
                    }
                }
            }
        });
        contactsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String displayName = value.toString();
                String contactId = displayNameToId.get(displayName);
                if (displayName.contains("Online")) {
                    label.setForeground(new Color(0, 153, 0)); // Vert
                } else if (displayName.contains("Offline")) {
                    label.setForeground(Color.GRAY);
                } else {
                    label.setForeground(Color.BLACK);
                }
                if (contactId != null && unreadMap.getOrDefault(contactId, false)) {
                    label.setText("\uD83D\uDD34 " + displayName); // 🔴
                } else {
                    label.setText(displayName);
                }
                return label;
            }
        });
        contactsSection.add(new JScrollPane(contactsList), BorderLayout.CENTER);

        // Panel principal avec split
        add(contactsSection, BorderLayout.CENTER);

        // Boutons d'action
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JButton addContactButton = new JButton("Ajouter Contact");
        buttonPanel.add(addContactButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Ajout des listeners
        addContactButton.addActionListener(e -> showAddContactDialog());
        
        // Chargement initial des contacts
        refreshContacts();

        initializeListeners();
    }

    public void addContactSelectionListener(Consumer<String> listener) {
        this.contactSelectionListener = listener;
    }

    public void addChatSelectionListener(ChatSelectionListener listener) {
        this.chatSelectionListener = listener;
    }

    public void refreshContacts() {
        DatabaseReference contactsRef = FirebaseDatabase.getInstance().getReference("contacts").child(currentUser.getId());
        contactsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                java.util.List<String> tempList = new java.util.ArrayList<>();
                unreadMap.clear();
                displayNameToId.clear();
                int totalContacts = (int) snapshot.getChildrenCount();
                if (totalContacts == 0) {
                    contactsListModel.clear();
                    return;
                }
                final int[] processed = {0};
                for (DataSnapshot contactSnapshot : snapshot.getChildren()) {
                    String contactId = contactSnapshot.getKey();
                    Boolean unread = contactSnapshot.child("unread").getValue(Boolean.class);
                    unreadMap.put(contactId, unread != null && unread);
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(contactId);
                    userRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot userSnapshot) {
                            String name = userSnapshot.child("name").getValue(String.class);
                            String status = userSnapshot.child("status").getValue(String.class);
                            if (name != null) {
                                String statusLabel;
                                if ("Online".equalsIgnoreCase(status)) statusLabel = " (Online)";
                                else statusLabel = " (Offline)";
                                String displayName = name + statusLabel;
                                displayNameToId.put(displayName, contactId);
                                if (!tempList.contains(displayName)) tempList.add(displayName);
                            }
                            processed[0]++;
                            if (processed[0] == totalContacts) {
                                contactsListModel.clear();
                                for (String contact : tempList) {
                                    contactsListModel.addElement(contact);
                                }
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                            processed[0]++;
                            if (processed[0] == totalContacts) {
                                contactsListModel.clear();
                                for (String contact : tempList) {
                                    contactsListModel.addElement(contact);
                                }
                            }
                        }
                    });
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Erreur de lecture des contacts: " + error.getMessage());
            }
        });
    }

    public void showAddContactDialog() {
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        
        Object[] message = {
            "Nom:", nameField,
            "Email:", emailField
        };
        
        int option = JOptionPane.showConfirmDialog(this, message, 
            "Ajouter un contact", JOptionPane.OK_CANCEL_OPTION);
            
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
                                refreshContacts();
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

    private void initializeListeners() {
        contactsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedContact = contactsList.getSelectedValue();
                if (selectedContact != null) {
                    if (chatSelectionListener != null) {
                        chatSelectionListener.onChatSelected(selectedContact);
                    }
                }
            }
        });
    }

    private void showContactPropertiesMenu(int x, int y) {
        String selectedContact = contactsList.getSelectedValue();
        if (selectedContact == null) return;

        String contactName = selectedContact.split(" \\(")[0];
        
        JPopupMenu menu = new JPopupMenu();
        JMenuItem propertiesItem = new JMenuItem("Propriétés");
        JMenuItem editItem = new JMenuItem("Modifier");
        JMenuItem deleteItem = new JMenuItem("Supprimer");

        propertiesItem.addActionListener(e -> showContactProperties(contactName));
        editItem.addActionListener(e -> editSelectedContact());
        deleteItem.addActionListener(e -> deleteSelectedContact());

        menu.add(propertiesItem);
        menu.addSeparator();
        menu.add(editItem);
        menu.add(deleteItem);

        menu.show(contactsList, x, y);
    }

    private void showContactProperties(String contactName) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.orderByChild("name").equalTo(contactName)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String email = userSnapshot.child("email").getValue(String.class);
                        String status = userSnapshot.child("status").getValue(String.class);
                        String statusLabel;
                        if ("Online".equalsIgnoreCase(status)) statusLabel = "Online";
                        else statusLabel = "Offline";
                        
                        StringBuilder info = new StringBuilder();
                        info.append("Nom: ").append(contactName).append("\n");
                        info.append("Email: ").append(email).append("\n");
                        info.append("Statut: ").append(statusLabel);
                        
                        JOptionPane.showMessageDialog(null,
                            info.toString(),
                            "Propriétés du contact",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    System.err.println("Erreur de lecture des propriétés: " + error.getMessage());
                }
            });
    }

    private void deleteSelectedContact() {
        String selectedContact = contactsList.getSelectedValue();
        if (selectedContact != null) {
            String contactName = selectedContact.split(" \\(")[0]; // Enlever le statut
            
            int confirm = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment supprimer " + contactName + " ?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                usersRef.orderByChild("name").equalTo(contactName)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                userSnapshot.getRef().removeValueAsync();
                            }
                            refreshContacts();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            System.err.println("Erreur de suppression: " + error.getMessage());
                        }
                    });
            }
        }
    }

    private void editSelectedContact() {
        String selectedContact = contactsList.getSelectedValue();
        if (selectedContact == null) return;

        String contactName = selectedContact.split(" \\(")[0];
        String newName = JOptionPane.showInputDialog(this, "Nouveau nom pour le contact :", contactName);
        if (newName != null && !newName.trim().isEmpty()) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
            usersRef.orderByChild("name").equalTo(contactName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            userSnapshot.getRef().child("name").setValue(newName, (error, ref) -> {
                                if (error == null) {
                                    JOptionPane.showMessageDialog(ContactsPanel.this,
                                        "Nom du contact mis à jour !",
                                        "Succès",
                                        JOptionPane.INFORMATION_MESSAGE);
                                    refreshContacts();
                                } else {
                                    JOptionPane.showMessageDialog(ContactsPanel.this,
                                        "Erreur lors de la mise à jour du nom: " + error.getMessage(),
                                        "Erreur",
                                        JOptionPane.ERROR_MESSAGE);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        JOptionPane.showMessageDialog(ContactsPanel.this,
                            "Erreur lors de la modification du contact: " + error.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
        }
    }
} 