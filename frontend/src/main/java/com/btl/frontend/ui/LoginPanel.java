/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.frontend.ui;

/**
 *
 * @author ADMIN
 */
import com.btl.frontend.api.ApiClient;
import com.btl.frontend.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;

/**
 * Login and Registration panel with modern dark theme.
 */
public class LoginPanel extends JPanel {

    public interface LoginListener {
        void onLoginSuccess(Map<String, Object> userData);
    }

    private final LoginListener listener;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField displayNameField;
    private JButton actionButton;
    private JLabel switchLabel;
    private JLabel statusLabel;
    private boolean isLoginMode = true;

    public LoginPanel(LoginListener listener) {
        this.listener = listener;
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        // Center card
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_SURFACE);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 500));
        card.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));

        // Logo/Title
        JLabel logo = new JLabel("TAH") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, UIConstants.GRADIENT_START, getWidth(), 0, UIConstants.GRADIENT_END);
                g2.setPaint(gp);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, fm.getAscent());
                g2.dispose();
            }
        };
        logo.setFont(new Font("Segoe UI", Font.BOLD, 32));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setPreferredSize(new Dimension(340, 50));
        logo.setMaximumSize(new Dimension(340, 50));
        card.add(logo);
        card.add(Box.createVerticalStrut(8));

        JLabel subtitle = new JLabel("Music flows like flowers in the wind");
        subtitle.setFont(UIConstants.FONT_BODY);
        subtitle.setForeground(UIConstants.ACCENT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(30));

        // Display name field (hidden in login mode)
        displayNameField = createStyledField("Display Name");
        displayNameField.setVisible(false);
        displayNameField.setMaximumSize(new Dimension(340, 44));
        card.add(displayNameField);
        card.add(Box.createVerticalStrut(12));

        // Username
        usernameField = createStyledField("Username");
        usernameField.setMaximumSize(new Dimension(340, 44));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(12));

        // Password
        passwordField = new JPasswordField();
        styleField(passwordField, "Password");
        passwordField.setMaximumSize(new Dimension(340, 44));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(20));

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(UIConstants.FONT_SMALL);
        statusLabel.setForeground(UIConstants.ERROR);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(8));

        // Action button
        actionButton = new JButton("Log In") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(UIConstants.PRIMARY_HOVER);
                } else {
                    g2.setColor(UIConstants.PRIMARY);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        actionButton.setFont(UIConstants.FONT_BUTTON);
        actionButton.setMaximumSize(new Dimension(340, 44));
        actionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionButton.setBorderPainted(false);
        actionButton.setContentAreaFilled(false);
        actionButton.setFocusPainted(false);
        actionButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        actionButton.addActionListener(e -> performAction());
        card.add(actionButton);
        card.add(Box.createVerticalStrut(20));

        // Switch mode link
        switchLabel = new JLabel("Don't have an account? Sign Up");
        switchLabel.setFont(UIConstants.FONT_SMALL);
        switchLabel.setForeground(UIConstants.PRIMARY);
        switchLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        switchLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { toggleMode(); }
            @Override
            public void mouseEntered(MouseEvent e) { switchLabel.setForeground(UIConstants.PRIMARY_HOVER); }
            @Override
            public void mouseExited(MouseEvent e) { switchLabel.setForeground(UIConstants.PRIMARY); }
        });
        card.add(switchLabel);

        // Add enter key support
        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performAction();
            }
        };
        usernameField.addKeyListener(enterKey);
        passwordField.addKeyListener(enterKey);
        displayNameField.addKeyListener(enterKey);

        add(card);
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        displayNameField.setVisible(!isLoginMode);
        actionButton.setText(isLoginMode ? "Log In" : "Sign Up");
        switchLabel.setText(isLoginMode ? "Don't have an account? Sign Up" : "Already have an account? Log In");
        statusLabel.setText(" ");
        revalidate();
        repaint();
    }

    private void performAction() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill in all fields");
            return;
        }

        actionButton.setEnabled(false);
        statusLabel.setForeground(UIConstants.TEXT_SECONDARY);
        statusLabel.setText(isLoginMode ? "Logging in..." : "Creating account...");

        new Thread(() -> {
            try {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("username", username);
                body.put("password", password);

                String path;
                if (isLoginMode) {
                    path = "/auth/login";
                } else {
                    String displayName = displayNameField.getText().trim();
                    if (displayName.isEmpty()) displayName = username;
                    body.put("displayName", displayName);
                    path = "/auth/register";
                }

                Map<String, Object> response = ApiClient.post(path, body);
                String status = JsonHelper.getString(response, "status");

                SwingUtilities.invokeLater(() -> {
                    if ("success".equals(status)) {
                        Map<String, Object> data = JsonHelper.getMap(response, "data");
                        String token = JsonHelper.getString(data, "token");
                        ApiClient.setAuthToken(token);
                        listener.onLoginSuccess(JsonHelper.getMap(data, "user"));
                    } else {
                        statusLabel.setForeground(UIConstants.ERROR);
                        statusLabel.setText(JsonHelper.getString(response, "message", "An error occurred"));
                        actionButton.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setForeground(UIConstants.ERROR);
                    statusLabel.setText("Connection error. Is the server running?");
                    actionButton.setEnabled(true);
                });
            }
        }).start();
    }

    private JTextField createStyledField(String placeholder) {
        JTextField field = new JTextField();
        styleField(field, placeholder);
        return field;
    }

    private void styleField(JTextField field, String placeholder) {
        field.setFont(UIConstants.FONT_BODY);
        field.setForeground(UIConstants.TEXT_PRIMARY);
        field.setBackground(UIConstants.BG_INPUT);
        field.setCaretColor(UIConstants.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        field.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Placeholder: dùng flag isPlaceholder thay vì so sánh text
        field.putClientProperty("placeholder", placeholder);
        field.putClientProperty("isPlaceholder", true);
        field.setText(placeholder);
        field.setForeground(UIConstants.TEXT_MUTED);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                Boolean isPlaceholder = (Boolean) field.getClientProperty("isPlaceholder");
                if (Boolean.TRUE.equals(isPlaceholder)) {
                    field.setText("");
                    field.setForeground(UIConstants.TEXT_PRIMARY);
                    field.putClientProperty("isPlaceholder", false);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(UIConstants.TEXT_MUTED);
                    field.putClientProperty("isPlaceholder", true);
                }
            }
        });
    }
}

