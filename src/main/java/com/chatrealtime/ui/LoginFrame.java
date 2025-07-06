package com.chatrealtime.ui;

import com.chatrealtime.model.User;
import com.chatrealtime.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class LoginFrame extends JFrame {
    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JButton loginButton;
    private final JButton registerButton;
    private final String API_URL = "http://localhost:8081/api/";
    private final RestTemplate restTemplate;

    public LoginFrame() {
        this.restTemplate = new RestTemplate();
        
        // Forcer tous les utilisateurs à Offline au démarrage de l'application
        // (SUPPRIMÉ pour une gestion correcte de la présence)
        
        setTitle("ChatRealTime - Connexion");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        // Panel principal
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Logo ou titre
        JLabel titleLabel = new JLabel("ChatRealTime");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Panel de formulaire
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        formPanel.add(new JLabel("Email:"));
        emailField = new JTextField();
        formPanel.add(emailField);
        formPanel.add(new JLabel("Mot de passe:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);
        mainPanel.add(formPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Panel des boutons
        JPanel buttonPanel = new JPanel();
        loginButton = new JButton("Se connecter");
        registerButton = new JButton("S'inscrire");
        
        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> showRegisterDialog());
        
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        mainPanel.add(buttonPanel);

        add(mainPanel);
    }

    private void handleLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez remplir tous les champs",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Créer l'objet de requête
            Map<String, String> request = new HashMap<>();
            request.put("email", email);
            request.put("password", password);

            // Appeler le service d'authentification
            ResponseEntity<Map> response = restTemplate.postForEntity(
                API_URL + "auth/login",
                request,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userData = response.getBody();
                
                // Créer l'utilisateur avec les données reçues
                User user = new User();
                user.setId((String) userData.get("id"));
                user.setEmail(email);
                user.setName((String) userData.get("name"));
                user.setStatus("online");
                
                // Mettre à jour le statut dans Firebase
                com.google.firebase.database.DatabaseReference userRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("users").child(user.getId());
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("status", "Online");
                userRef.updateChildrenAsync(updates);
                
                // Stocker le token JWT pour les futures requêtes
                String token = (String) userData.get("token");
                
                // Ouvrir la fenêtre principale
                MainFrame mainFrame = new MainFrame(user);
                mainFrame.setVisible(true);
                this.dispose();
            }
        } catch (Exception e) {
            String errorMessage = "Erreur d'authentification. Vérifiez vos identifiants.";
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                errorMessage = "Email ou mot de passe incorrect";
            }
            JOptionPane.showMessageDialog(this,
                    errorMessage,
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showRegisterDialog() {
        JDialog dialog = new JDialog(this, "Inscription", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField nameField = new JTextField();
        JTextField emailRegField = new JTextField();
        JPasswordField passRegField = new JPasswordField();
        JPasswordField confirmPassField = new JPasswordField();

        panel.add(new JLabel("Nom:"));
        panel.add(nameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailRegField);
        panel.add(new JLabel("Mot de passe:"));
        panel.add(passRegField);
        panel.add(new JLabel("Confirmer:"));
        panel.add(confirmPassField);

        JButton registerBtn = new JButton("S'inscrire");
        registerBtn.addActionListener(e -> {
            String name = nameField.getText();
            String email = emailRegField.getText();
            String pass = new String(passRegField.getPassword());
            String confirm = new String(confirmPassField.getPassword());

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Veuillez remplir tous les champs",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!pass.equals(confirm)) {
                JOptionPane.showMessageDialog(dialog,
                        "Les mots de passe ne correspondent pas",
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // Créer l'objet de requête
                Map<String, String> request = new HashMap<>();
                request.put("name", name);
                request.put("email", email);
                request.put("password", pass);

                // Appeler le service d'inscription
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    API_URL + "auth/register",
                    request,
                    Map.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Inscription réussie! Vous pouvez maintenant vous connecter.",
                            "Succès",
                            JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Erreur lors de l'inscription: " + ex.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(registerBtn);
        
        dialog.setLayout(new BorderLayout());
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Use default look and feel
            }
            new LoginFrame().setVisible(true);
        });
    }
} 