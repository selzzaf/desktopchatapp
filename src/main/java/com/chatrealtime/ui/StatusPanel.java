package com.chatrealtime.ui;

import com.chatrealtime.model.User;
import javax.swing.*;
import java.awt.*;

public class StatusPanel extends JPanel {
    private final User currentUser;
    private final JLabel statusLabel;
    private final JLabel notificationLabel;
    private Timer notificationTimer;
    private StatusChangeListener statusChangeListener;

    public StatusPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Panel gauche pour le statut
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Online");
        statusLabel.setIcon(createStatusIcon("Online"));
        leftPanel.add(statusLabel);

        // Panel droit pour les notifications
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        notificationLabel = new JLabel("");
        rightPanel.add(notificationLabel);

        // Ajout des panels
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        // Initialisation du timer pour les notifications
        notificationTimer = new Timer(5000, e -> clearNotification());
        notificationTimer.setRepeats(false);

        // Menu popup pour changer le statut
        JPopupMenu statusMenu = createStatusMenu();
        statusLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                statusMenu.show(statusLabel, evt.getX(), evt.getY());
            }
        });
    }

    private JPopupMenu createStatusMenu() {
        JPopupMenu menu = new JPopupMenu();
        String[] statuses = {"Online", "Offline"};
        for (String status : statuses) {
            JMenuItem item = new JMenuItem(status);
            item.setIcon(createStatusIcon(status));
            item.addActionListener(e -> updateStatus(status));
            menu.add(item);
        }
        return menu;
    }

    private ImageIcon createStatusIcon(String status) {
        int size = 10;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) image.getGraphics();
        switch (status) {
            case "Online":
                g2.setColor(new Color(0, 153, 0)); // Vert
                break;
            case "Offline":
            default:
                g2.setColor(Color.GRAY);
        }
        g2.fillOval(0, 0, size, size);
        g2.dispose();
        return new ImageIcon(image);
    }

    public void setStatusChangeListener(StatusChangeListener listener) {
        this.statusChangeListener = listener;
    }

    public void setStatusUIOnly(String status) {
        statusLabel.setText(status);
        statusLabel.setIcon(createStatusIcon(status));
    }

    public void updateStatus(String status) {
        statusLabel.setText(status);
        statusLabel.setIcon(createStatusIcon(status));
        if (statusChangeListener != null) {
            statusChangeListener.onStatusChanged(status);
        }
    }

    public void showNotification(String message) {
        notificationLabel.setText(message);
        if (notificationTimer.isRunning()) {
            notificationTimer.restart();
        } else {
            notificationTimer.start();
        }
    }

    private void clearNotification() {
        notificationLabel.setText("");
    }

    public interface StatusChangeListener {
        void onStatusChanged(String status);
    }
} 