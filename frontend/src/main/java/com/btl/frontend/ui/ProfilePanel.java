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
import java.util.*;
import java.util.List;

/**
 * User profile panel.
 */
public class ProfilePanel extends JPanel {

    public interface ProfileListener {
        void onTrackSelected(Map<String, Object> track);
    }

    private final AudioPlayer player;
    private final PlayerBar playerBar;
    private final ProfileListener profileListener;
    private JPanel contentPanel;

    public ProfilePanel(AudioPlayer player, PlayerBar playerBar, ProfileListener listener) {
        this.player = player;
        this.playerBar = playerBar;
        this.profileListener = listener;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
    }

    public void loadProfile(int userId) {
        removeAll();
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(UIConstants.BG_DARK);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel loading = new JLabel("Loading profile...");
        loading.setFont(UIConstants.FONT_BODY);
        loading.setForeground(UIConstants.TEXT_SECONDARY);
        contentPanel.add(loading);

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBackground(UIConstants.BG_DARK);
        add(scroll, BorderLayout.CENTER);
        revalidate();
        repaint();

        new Thread(() -> {
            try {
                Map<String, Object> userResp = ApiClient.get("/users/" + userId);
                Map<String, Object> userData = JsonHelper.getMap(userResp, "data");

                Map<String, Object> tracksResp = ApiClient.get("/users/" + userId + "/tracks");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tracks = (List<Map<String, Object>>) tracksResp.get("data");
                if (tracks == null) tracks = new ArrayList<>();

                final List<Map<String, Object>> finalTracks = tracks;
                SwingUtilities.invokeLater(() -> buildProfile(userData, finalTracks));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    contentPanel.removeAll();
                    JLabel err = new JLabel("Failed to load profile");
                    err.setForeground(UIConstants.ERROR);
                    contentPanel.add(err);
                    contentPanel.revalidate();
                });
            }
        }).start();
    }

    private void buildProfile(Map<String, Object> user, List<Map<String, Object>> tracks) {
        contentPanel.removeAll();

        String displayName = JsonHelper.getString(user, "displayName", "User");
        String username = JsonHelper.getString(user, "username", "");
        String bio = JsonHelper.getString(user, "bio", "");
        int trackCount = JsonHelper.getInt(user, "trackCount");
        int followers = JsonHelper.getInt(user, "followerCount");
        int following = JsonHelper.getInt(user, "followingCount");
        boolean isFollowing = JsonHelper.getBoolean(user, "isFollowing");
        int profileUserId = JsonHelper.getInt(user, "id");

        // Profile header with gradient banner
        JPanel banner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, UIConstants.GRADIENT_START, getWidth(), getHeight(), UIConstants.GRADIENT_END));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        banner.setPreferredSize(new Dimension(0, 120));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setLayout(new FlowLayout(FlowLayout.LEFT, 24, 24));

        // Avatar circle
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_SURFACE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(UIConstants.TEXT_PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
                FontMetrics fm = g2.getFontMetrics();
                String initial = displayName.substring(0, 1).toUpperCase();
                g2.drawString(initial, (getWidth() - fm.stringWidth(initial)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(72, 72));
        avatar.setOpaque(false);

        JPanel nameInfo = new JPanel();
        nameInfo.setLayout(new BoxLayout(nameInfo, BoxLayout.Y_AXIS));
        nameInfo.setOpaque(false);

        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(UIConstants.FONT_TITLE);
        nameLabel.setForeground(Color.WHITE);

        JLabel userLabel = new JLabel("@" + username);
        userLabel.setFont(UIConstants.FONT_BODY);
        userLabel.setForeground(new Color(255, 255, 255, 180));

        nameInfo.add(Box.createVerticalGlue());
        nameInfo.add(nameLabel);
        nameInfo.add(userLabel);
        nameInfo.add(Box.createVerticalGlue());

        banner.add(avatar);
        banner.add(nameInfo);

        contentPanel.add(banner);
        contentPanel.add(Box.createVerticalStrut(16));

        // Stats row
        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        statsRow.add(createStat(String.valueOf(trackCount), "Tracks"));
        statsRow.add(createStat(String.valueOf(followers), "Followers"));
        statsRow.add(createStat(String.valueOf(following), "Following"));

        contentPanel.add(statsRow);
        contentPanel.add(Box.createVerticalStrut(8));

        // Follow button (if not self)
        if (ApiClient.isAuthenticated()) {
            JButton followBtn = new JButton(isFollowing ? "Following" : "Follow");
            followBtn.setFont(UIConstants.FONT_BUTTON);
            followBtn.setForeground(isFollowing ? UIConstants.TEXT_PRIMARY : Color.WHITE);
            followBtn.setBackground(isFollowing ? UIConstants.BG_CARD : UIConstants.PRIMARY);
            followBtn.setBorderPainted(false);
            followBtn.setFocusPainted(false);
            followBtn.setPreferredSize(new Dimension(100, 32));
            followBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            followBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            followBtn.addActionListener(e -> {
                new Thread(() -> {
                    try {
                        if (followBtn.getText().equals("Follow")) {
                            ApiClient.post("/users/" + profileUserId + "/follow", new HashMap<>());
                            SwingUtilities.invokeLater(() -> {
                                followBtn.setText("Following");
                                followBtn.setBackground(UIConstants.BG_CARD);
                                followBtn.setForeground(UIConstants.TEXT_PRIMARY);
                                // Reload để cập nhật số follower
                                loadProfile(profileUserId);
                            });
                        } else {
                            ApiClient.delete("/users/" + profileUserId + "/follow");
                            SwingUtilities.invokeLater(() -> {
                                followBtn.setText("Follow");
                                followBtn.setBackground(UIConstants.PRIMARY);
                                followBtn.setForeground(Color.WHITE);
                                // Reload để cập nhật số follower
                                loadProfile(profileUserId);
                            });
                        }
                    } catch (Exception ex) { System.err.println("Follow error: " + ex.getMessage()); }
                }).start();
            });
            contentPanel.add(followBtn);
            contentPanel.add(Box.createVerticalStrut(8));
        }

        // Bio
        if (bio != null && !bio.isEmpty()) {
            JLabel bioLabel = new JLabel(bio);
            bioLabel.setFont(UIConstants.FONT_BODY);
            bioLabel.setForeground(UIConstants.TEXT_SECONDARY);
            bioLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(bioLabel);
        }
        contentPanel.add(Box.createVerticalStrut(16));

        // Tracks
        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.DIVIDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(sep);
        contentPanel.add(Box.createVerticalStrut(16));

        JLabel tracksHeader = new JLabel("Tracks (" + tracks.size() + ")");
        tracksHeader.setFont(UIConstants.FONT_SUBTITLE);
        tracksHeader.setForeground(UIConstants.TEXT_PRIMARY);
        tracksHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(tracksHeader);
        contentPanel.add(Box.createVerticalStrut(12));

        for (Map<String, Object> track : tracks) {
            contentPanel.add(createTrackRow(track));
            contentPanel.add(Box.createVerticalStrut(4));
        }

        if (tracks.isEmpty()) {
            JLabel empty = new JLabel("No tracks uploaded yet");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            contentPanel.add(empty);
        }

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private JPanel createStat(String value, String label) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel valLabel = new JLabel(value);
        valLabel.setFont(UIConstants.FONT_SUBTITLE);
        valLabel.setForeground(UIConstants.TEXT_PRIMARY);
        valLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(UIConstants.FONT_SMALL);
        nameLabel.setForeground(UIConstants.TEXT_MUTED);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(valLabel);
        panel.add(nameLabel);
        return panel;
    }

    private JPanel createTrackRow(Map<String, Object> track) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(UIConstants.BG_SURFACE);
        row.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton playBtn = IconFactory.iconButton(IconFactory.playIcon(16, UIConstants.PRIMARY));
        playBtn.addActionListener(e -> playTrack(track));

        JLabel titleLabel = new JLabel(JsonHelper.getString(track, "title", "Untitled"));
        titleLabel.setFont(UIConstants.FONT_HEADING);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel durLabel = new JLabel(UIConstants.formatDuration(JsonHelper.getInt(track, "durationSeconds")));
        durLabel.setFont(UIConstants.FONT_SMALL);
        durLabel.setForeground(UIConstants.TEXT_MUTED);

        row.add(playBtn, BorderLayout.WEST);
        row.add(titleLabel, BorderLayout.CENTER);
        row.add(durLabel, BorderLayout.EAST);

        row.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { profileListener.onTrackSelected(track); }
            public void mouseEntered(MouseEvent e) { row.setBackground(UIConstants.BG_HOVER); }
            public void mouseExited(MouseEvent e) { row.setBackground(UIConstants.BG_SURFACE); }
        });
        return row;
    }

    private void playTrack(Map<String, Object> track) {
        TrackPlayerUtil.playTrack(track, player, playerBar);
    }
}

