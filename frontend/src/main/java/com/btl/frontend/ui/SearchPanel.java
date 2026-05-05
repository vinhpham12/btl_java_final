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
import com.btl.frontend.audio.AudioPlayer;
import com.btl.frontend.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Search panel - search tracks and users.
 */
public class SearchPanel extends JPanel {

    public interface SearchListener {
        void onTrackSelected(Map<String, Object> track);
        void onUserSelected(int userId);
    }

    private final AudioPlayer player;
    private final PlayerBar playerBar;
    private final SearchListener searchListener;
    private JTextField searchField;
    private JPanel resultsPanel;
    private JLabel statusLabel;

    public SearchPanel(AudioPlayer player, PlayerBar playerBar, SearchListener listener) {
        this.player = player;
        this.playerBar = playerBar;
        this.searchListener = listener;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIConstants.BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel header = new JLabel("Search");
        header.setFont(UIConstants.FONT_TITLE);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(header);
        content.add(Box.createVerticalStrut(16));

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setOpaque(false);
        searchBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setForeground(UIConstants.TEXT_PRIMARY);
        searchField.setBackground(UIConstants.BG_INPUT);
        searchField.setCaretColor(UIConstants.TEXT_PRIMARY);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) performSearch();
            }
        });

        JButton searchBtn = new JButton("Search");
        searchBtn.setFont(UIConstants.FONT_BUTTON);
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setBackground(UIConstants.PRIMARY);
        searchBtn.setBorderPainted(false);
        searchBtn.setFocusPainted(false);
        searchBtn.setPreferredSize(new Dimension(80, 40));
        searchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchBtn.addActionListener(e -> performSearch());

        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(searchBtn, BorderLayout.EAST);
        content.add(searchBar);
        content.add(Box.createVerticalStrut(16));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UIConstants.FONT_BODY);
        statusLabel.setForeground(UIConstants.TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(statusLabel);
        content.add(Box.createVerticalStrut(8));

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setOpaque(false);
        resultsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(resultsPanel);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBackground(UIConstants.BG_DARK);
        add(scroll, BorderLayout.CENTER);
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        statusLabel.setText("Searching...");
        resultsPanel.removeAll();
        resultsPanel.revalidate();

        new Thread(() -> {
            try {
                Map<String, Object> response = ApiClient.get("/search?q=" + java.net.URLEncoder.encode(query, "UTF-8") + "&type=all");
                Map<String, Object> data = JsonHelper.getMap(response, "data");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tracks = (List<Map<String, Object>>) data.get("tracks");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> users = (List<Map<String, Object>>) data.get("users");

                if (tracks == null) tracks = new ArrayList<>();
                if (users == null) users = new ArrayList<>();

                final List<Map<String, Object>> ft = tracks;
                final List<Map<String, Object>> fu = users;

                SwingUtilities.invokeLater(() -> {
                    resultsPanel.removeAll();
                    statusLabel.setText(ft.size() + " tracks, " + fu.size() + " users found");

                    if (!fu.isEmpty()) {
                        JLabel userHeader = new JLabel("Users");
                        userHeader.setFont(UIConstants.FONT_SUBTITLE);
                        userHeader.setForeground(UIConstants.TEXT_PRIMARY);
                        userHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
                        resultsPanel.add(userHeader);
                        resultsPanel.add(Box.createVerticalStrut(8));
                        for (Map<String, Object> user : fu) {
                            resultsPanel.add(createUserResult(user));
                            resultsPanel.add(Box.createVerticalStrut(4));
                        }
                        resultsPanel.add(Box.createVerticalStrut(16));
                    }

                    if (!ft.isEmpty()) {
                        JLabel trackHeader = new JLabel("Tracks");
                        trackHeader.setFont(UIConstants.FONT_SUBTITLE);
                        trackHeader.setForeground(UIConstants.TEXT_PRIMARY);
                        trackHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
                        resultsPanel.add(trackHeader);
                        resultsPanel.add(Box.createVerticalStrut(8));
                        for (Map<String, Object> track : ft) {
                            resultsPanel.add(createTrackResult(track));
                            resultsPanel.add(Box.createVerticalStrut(4));
                        }
                    }

                    resultsPanel.revalidate();
                    resultsPanel.repaint();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Search error"));
            }
        }).start();
    }

    private JPanel createTrackResult(Map<String, Object> track) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(UIConstants.BG_SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton playBtn = IconFactory.iconButton(IconFactory.playIcon(16, UIConstants.PRIMARY));
        playBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playBtn.addActionListener(e -> playTrack(track));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel title = new JLabel(JsonHelper.getString(track, "title", "Untitled"));
        title.setFont(UIConstants.FONT_HEADING);
        title.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel detail = new JLabel(JsonHelper.getString(track, "artist", "") + " · " + UIConstants.formatDuration(JsonHelper.getInt(track, "durationSeconds")));
        detail.setFont(UIConstants.FONT_SMALL);
        detail.setForeground(UIConstants.TEXT_SECONDARY);

        info.add(title);
        info.add(detail);

        card.add(playBtn, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { searchListener.onTrackSelected(track); }
            public void mouseEntered(MouseEvent e) { card.setBackground(UIConstants.BG_HOVER); }
            public void mouseExited(MouseEvent e) { card.setBackground(UIConstants.BG_SURFACE); }
        });
        return card;
    }

    private JPanel createUserResult(Map<String, Object> user) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(UIConstants.BG_SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel nameLabel = new JLabel(JsonHelper.getString(user, "displayName", JsonHelper.getString(user, "username", "User")));
        nameLabel.setFont(UIConstants.FONT_HEADING);
        nameLabel.setForeground(UIConstants.TEXT_PRIMARY);

        card.add(nameLabel, BorderLayout.CENTER);
        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { searchListener.onUserSelected(JsonHelper.getInt(user, "id")); }
            public void mouseEntered(MouseEvent e) { card.setBackground(UIConstants.BG_HOVER); }
            public void mouseExited(MouseEvent e) { card.setBackground(UIConstants.BG_SURFACE); }
        });
        return card;
    }

    private void playTrack(Map<String, Object> track) {
        int trackId = JsonHelper.getInt(track, "id");
        String title = JsonHelper.getString(track, "title", "Untitled");
        String artist = JsonHelper.getString(track, "artist", "Unknown");
        new Thread(() -> {
            try {
                ApiClient.post("/tracks/" + trackId + "/play", new HashMap<>());
                byte[] audioData = ApiClient.downloadBytes("/tracks/" + trackId + "/stream");
                if (audioData != null) {
                    SwingUtilities.invokeLater(() -> {
                        player.load(audioData);
                        player.play();
                        playerBar.setTrackInfo(title, artist);
                    });
                }
            } catch (Exception ex) { System.err.println("Play error: " + ex.getMessage()); }
        }).start();
    }
}

