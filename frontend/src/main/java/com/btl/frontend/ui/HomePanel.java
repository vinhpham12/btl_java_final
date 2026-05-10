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
 * Home panel - shows trending tracks and recent uploads.
 */
public class HomePanel extends JPanel {

    public interface HomeListener {
        void onTrackSelected(Map<String, Object> track);
        void onUserSelected(int userId);
    }

    private final AudioPlayer player;
    private final PlayerBar playerBar;
    private final HomeListener listener;
    private JPanel tracksGrid;
    private JLabel statusLabel;

    public HomePanel(AudioPlayer player, PlayerBar playerBar, HomeListener listener) {
        this.player = player;
        this.playerBar = playerBar;
        this.listener = listener;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(UIConstants.BG_DARK);
        mainContent.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Header
        JLabel header = new JLabel("Discover");
        header.setFont(UIConstants.FONT_TITLE);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(header);
        mainContent.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Explore the latest tracks uploaded by the community");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(sub);
        mainContent.add(Box.createVerticalStrut(24));

        // Sort buttons
        JPanel sortBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        sortBar.setOpaque(false);
        sortBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        sortBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        String[] sorts = {"Newest", "Popular", "Most Liked"};
        String[] sortValues = {"newest", "popular", "likes"};
        for (int i = 0; i < sorts.length; i++) {
            final String sortVal = sortValues[i];
            JButton btn = createPillButton(sorts[i], i == 0);
            btn.addActionListener(e -> {
                for (Component c : sortBar.getComponents()) {
                    if (c instanceof JButton) {
                        c.setForeground(UIConstants.TEXT_SECONDARY);
                        c.setBackground(UIConstants.BG_CARD);
                    }
                }
                btn.setForeground(Color.WHITE);
                btn.setBackground(UIConstants.PRIMARY);
                loadTracks(sortVal);
            });
            sortBar.add(btn);
        }
        mainContent.add(sortBar);
        mainContent.add(Box.createVerticalStrut(20));

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setFont(UIConstants.FONT_BODY);
        statusLabel.setForeground(UIConstants.TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(statusLabel);
        mainContent.add(Box.createVerticalStrut(8));

        // Tracks grid
        tracksGrid = new JPanel();
        tracksGrid.setLayout(new BoxLayout(tracksGrid, BoxLayout.Y_AXIS));
        tracksGrid.setOpaque(false);
        tracksGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(tracksGrid);

        JScrollPane scroll = new JScrollPane(mainContent);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBackground(UIConstants.BG_DARK);
        add(scroll, BorderLayout.CENTER);
    }

    public void loadTracks(String sort) {
        statusLabel.setText("Loading tracks...");
        tracksGrid.removeAll();
        tracksGrid.revalidate();
        tracksGrid.repaint();

        new Thread(() -> {
            try {
                Map<String, Object> response = ApiClient.get("/tracks?sort=" + sort + "&limit=30");
                String status = JsonHelper.getString(response, "status");
                if (!"success".equals(status)) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Failed to load tracks"));
                    return;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tracks = (List<Map<String, Object>>) response.get("data");
                if (tracks == null) tracks = new ArrayList<>();

                final List<Map<String, Object>> finalTracks = tracks;
                SwingUtilities.invokeLater(() -> {
                    tracksGrid.removeAll();
                    if (finalTracks.isEmpty()) {
                        statusLabel.setText("No tracks found. Be the first to upload!");
                    } else {
                        statusLabel.setText(finalTracks.size() + " tracks");
                        for (Map<String, Object> track : finalTracks) {
                            tracksGrid.add(createTrackCard(track));
                            tracksGrid.add(Box.createVerticalStrut(2));
                        }
                    }
                    tracksGrid.revalidate();
                    tracksGrid.repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Connection error"));
            }
        }).start();
    }

    private JPanel createTrackCard(Map<String, Object> track) {
        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBackground(UIConstants.BG_SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Play button
        JButton playBtn = IconFactory.iconButton(IconFactory.playIcon(20, UIConstants.PRIMARY));
        playBtn.setPreferredSize(new Dimension(40, 40));

        // Info
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        String title = JsonHelper.getString(track, "title", "Untitled");
        String artist = JsonHelper.getString(track, "artist", JsonHelper.getString(track, "uploaderName", "Unknown"));
        String genre = JsonHelper.getString(track, "genre", "");
        int duration = JsonHelper.getInt(track, "durationSeconds");
        int plays = JsonHelper.getInt(track, "playCount");
        int likes = JsonHelper.getInt(track, "likeCount");

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.FONT_HEADING);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel detailLabel = new JLabel(artist + (genre.isEmpty() ? "" : " · " + genre) + " · " + UIConstants.formatDuration(duration));
        detailLabel.setFont(UIConstants.FONT_SMALL);
        detailLabel.setForeground(UIConstants.TEXT_SECONDARY);

        info.add(titleLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(detailLabel);

        // Stats
        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        stats.setOpaque(false);

        JLabel playsLabel = new JLabel(plays + " plays");
        playsLabel.setFont(UIConstants.FONT_SMALL);
        playsLabel.setForeground(UIConstants.TEXT_MUTED);

        JLabel likesLabel = new JLabel(" " + likes);
        likesLabel.setIcon(IconFactory.heartIcon(14, UIConstants.TEXT_MUTED, false));
        likesLabel.setFont(UIConstants.FONT_SMALL);
        likesLabel.setForeground(UIConstants.TEXT_MUTED);

        // Nút "+" để thêm track vào playlist
        JButton addPlBtn = IconFactory.iconButton(IconFactory.plusIcon(14, UIConstants.TEXT_MUTED));
        addPlBtn.setPreferredSize(new Dimension(28, 28));
        addPlBtn.setToolTipText("Add to playlist");
        addPlBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent ev) { addPlBtn.setIcon(IconFactory.plusIcon(14, UIConstants.ACCENT)); }
            public void mouseExited(MouseEvent ev) { addPlBtn.setIcon(IconFactory.plusIcon(14, UIConstants.TEXT_MUTED)); }
        });
        addPlBtn.addActionListener(e -> {
            int tId = JsonHelper.getInt(track, "id");
            PlaylistPanel.showAddToPlaylistDialog(this, tId, title);
        });

        stats.add(playsLabel);
        stats.add(likesLabel);
        stats.add(addPlBtn);

        card.add(playBtn, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(stats, BorderLayout.EAST);

        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { card.setBackground(UIConstants.BG_HOVER); }
            @Override
            public void mouseExited(MouseEvent e) { card.setBackground(UIConstants.BG_SURFACE); }
            @Override
            public void mouseClicked(MouseEvent e) { listener.onTrackSelected(track); }
        });

        // Play button action
        playBtn.addActionListener(e -> playTrack(track));

        return card;
    }

    private void playTrack(Map<String, Object> track) {
        int trackId = JsonHelper.getInt(track, "id");
        String title = JsonHelper.getString(track, "title", "Untitled");
        String artist = JsonHelper.getString(track, "artist", "Unknown");

        new Thread(() -> {
            try {
                // Increment play count
                ApiClient.post("/tracks/" + trackId + "/play", new HashMap<>());
                // Download audio
                byte[] audioData = ApiClient.downloadBytes("/tracks/" + trackId + "/stream");
                if (audioData != null) {
                    SwingUtilities.invokeLater(() -> {
                        player.load(audioData);
                        player.play();
                        playerBar.setTrackInfo(title, artist);
                        playerBar.clearPlaylistQueue(); // Không phải playlist → xóa queue
                        playerBar.setCurrentTrackData(trackId, title, artist);
                    });
                }
            } catch (Exception ex) {
                System.err.println("Error playing track: " + ex.getMessage());
            }
        }).start();
    }

    private JButton createPillButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setFont(UIConstants.FONT_SMALL);
        btn.setForeground(active ? Color.WHITE : UIConstants.TEXT_SECONDARY);
        btn.setBackground(active ? UIConstants.PRIMARY : UIConstants.BG_CARD);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(90, 30));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}

