package com.chatrealtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import com.chatrealtime.ui.LoginFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.EventQueue;

@SpringBootApplication
public class ChatRealtimeApplication {
    private static LoginFrame loginFrame;

    public static void main(String[] args) {
        // Start Swing UI first
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Error starting UI: " + e.getMessage());
            }
        });

        // Start Spring Boot backend
        try {
            ConfigurableApplicationContext context = SpringApplication.run(ChatRealtimeApplication.class, args);
        } catch (Exception e) {
            System.err.println("Error starting backend: " + e.getMessage());
        }
    }
} 