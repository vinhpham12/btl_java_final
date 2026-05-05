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
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Upload panel - allows users to upload WAV tracks.
 */
public class UploadPanel extends JPanel {

    private JTextField titleField, artistField, genreField;
    private JTextArea descriptionArea;
    private JLabel fileLabel;
    private JButton uploadBtn;
    private JLabel statusLabel;
    private byte[] selectedFileData;
    private String selectedFileName;

    public UploadPanel() {
        setBackground(UIConstants.BG_DARK);
        setLayout(new GridBagLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(UIConstants.BG_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER, 1, true),
            BorderFactory.createEmptyBorder(32, 32, 32, 32)
        ));
        card.setPreferredSize(new Dimension(500, 620));

        // Title
        JLabel header = new JLabel("Upload Track");
        header.setFont(UIConstants.FONT_TITLE);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(header);
        card.add(Box.createVerticalStrut(24));

        // File chooser area
        JPanel dropZone = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_INPUT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                float[] dash = {6, 4};
                g2.setColor(UIConstants.BORDER);
                g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, dash, 0));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 12, 12));
                g2.dispose();
            }
        };
        dropZone.setOpaque(false);
        dropZone.setPreferredSize(new Dimension(436, 80));
        dropZone.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        dropZone.setAlignmentX(Component.LEFT_ALIGNMENT);
        dropZone.setCursor(new Cursor(Cursor.HAND_CURSOR));

        fileLabel = new JLabel("Click to select a WAV file...", SwingConstants.CENTER);
        fileLabel.setFont(UIConstants.FONT_BODY);
        fileLabel.setForeground(UIConstants.TEXT_SECONDARY);
        dropZone.add(fileLabel, BorderLayout.CENTER);

        dropZone.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) { selectFile(); }
        });

        card.add(dropZone);
        card.add(Box.createVerticalStrut(20));

        // Form fields
        card.add(createLabel("Title *"));
        titleField = createTextField("Enter track title");
        card.add(titleField);
        card.add(Box.createVerticalStrut(12));

        card.add(createLabel("Artist"));
        artistField = createTextField("Artist name");
        card.add(artistField);
        card.add(Box.createVerticalStrut(12));

        card.add(createLabel("Genre"));
        genreField = createTextField("e.g. Electronic, Rock, Hip-Hop");
        card.add(genreField);
        card.add(Box.createVerticalStrut(12));

        card.add(createLabel("Description"));
        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setFont(UIConstants.FONT_BODY);
        descriptionArea.setForeground(UIConstants.TEXT_PRIMARY);
        descriptionArea.setBackground(UIConstants.BG_INPUT);
        descriptionArea.setCaretColor(UIConstants.TEXT_PRIMARY);
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        descriptionArea.setLineWrap(true);
        descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        descriptionArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        card.add(descriptionArea);
        card.add(Box.createVerticalStrut(16));

        // Status
        statusLabel = new JLabel(" ");
        statusLabel.setFont(UIConstants.FONT_SMALL);
        statusLabel.setForeground(UIConstants.TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(8));

        // Upload button
        uploadBtn = new JButton("Upload") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? (getModel().isRollover() ? UIConstants.PRIMARY_HOVER : UIConstants.PRIMARY) : UIConstants.BG_HOVER);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        uploadBtn.setFont(UIConstants.FONT_BUTTON);
        uploadBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        uploadBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        uploadBtn.setBorderPainted(false);
        uploadBtn.setContentAreaFilled(false);
        uploadBtn.setFocusPainted(false);
        uploadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        uploadBtn.addActionListener(e -> performUpload());
        card.add(uploadBtn);

        add(card);
    }

    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("WAV Audio Files", "wav"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                selectedFileData = Files.readAllBytes(file.toPath());
                selectedFileName = file.getName();
                fileLabel.setText(selectedFileName + " (" + (selectedFileData.length / 1024) + " KB)");
                fileLabel.setForeground(UIConstants.SUCCESS);
                // Auto-fill title if empty
                if (titleField.getText().isEmpty() || titleField.getForeground() == UIConstants.TEXT_MUTED) {
                    String name = selectedFileName.replace(".wav", "").replace("_", " ");
                    titleField.setText(name);
                    titleField.setForeground(UIConstants.TEXT_PRIMARY);
                }
            } catch (IOException ex) {
                fileLabel.setText("Error reading file");
                fileLabel.setForeground(UIConstants.ERROR);
            }
        }
    }

    private void performUpload() {
        if (selectedFileData == null) {
            statusLabel.setForeground(UIConstants.ERROR);
            statusLabel.setText("Please select a WAV file first");
            return;
        }

        String title = titleField.getText().trim();
        if (title.isEmpty() || titleField.getForeground() == UIConstants.TEXT_MUTED) {
            statusLabel.setForeground(UIConstants.ERROR);
            statusLabel.setText("Title is required");
            return;
        }

        uploadBtn.setEnabled(false);
        statusLabel.setForeground(UIConstants.TEXT_SECONDARY);
        statusLabel.setText("Uploading...");

        new Thread(() -> {
            try {
                Map<String, String> fields = new LinkedHashMap<>();
                fields.put("title", title);
                String artist = artistField.getText().trim();
                if (!artist.isEmpty() && artistField.getForeground() != UIConstants.TEXT_MUTED) fields.put("artist", artist);
                String genre = genreField.getText().trim();
                if (!genre.isEmpty() && genreField.getForeground() != UIConstants.TEXT_MUTED) fields.put("genre", genre);
                String desc = descriptionArea.getText().trim();
                if (!desc.isEmpty()) fields.put("description", desc);

                Map<String, Object> response = ApiClient.uploadMultipart("/tracks", fields, "file", selectedFileData, selectedFileName);
                String status = JsonHelper.getString(response, "status");

                SwingUtilities.invokeLater(() -> {
                    if ("success".equals(status)) {
                        statusLabel.setForeground(UIConstants.SUCCESS);
                        statusLabel.setText("Track uploaded successfully!");
                        resetForm();
                    } else {
                        statusLabel.setForeground(UIConstants.ERROR);
                        statusLabel.setText(JsonHelper.getString(response, "message", "Upload failed"));
                    }
                    uploadBtn.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setForeground(UIConstants.ERROR);
                    statusLabel.setText("Connection error");
                    uploadBtn.setEnabled(true);
                });
            }
        }).start();
    }

    private void resetForm() {
        selectedFileData = null;
        selectedFileName = null;
        fileLabel.setText("Click to select a WAV file...");
        fileLabel.setForeground(UIConstants.TEXT_SECONDARY);
        titleField.setText("");
        artistField.setText("");
        genreField.setText("");
        descriptionArea.setText("");
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UIConstants.FONT_SMALL);
        label.setForeground(UIConstants.TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return label;
    }

    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(UIConstants.FONT_BODY);
        field.setForeground(UIConstants.TEXT_MUTED);
        field.setBackground(UIConstants.BG_INPUT);
        field.setCaretColor(UIConstants.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        field.setText(placeholder);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) { field.setText(""); field.setForeground(UIConstants.TEXT_PRIMARY); }
            }
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) { field.setText(placeholder); field.setForeground(UIConstants.TEXT_MUTED); }
            }
        });
        return field;
    }
}

