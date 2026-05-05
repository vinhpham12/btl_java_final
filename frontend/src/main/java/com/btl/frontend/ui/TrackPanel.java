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
 * Track detail panel - shows track info, waveform, comments.
 */
public class TrackPanel extends JPanel {

    private final AudioPlayer player;
    private final PlayerBar playerBar;
    private Map<String, Object> trackData;
    private WaveformPanel waveformPanel;
    private JPanel commentsPanel;
    private JTextField commentField;
    private JLabel likeLabel;
    private boolean isLiked;

    public TrackPanel(AudioPlayer player, PlayerBar playerBar) {
        this.player = player;
        this.playerBar = playerBar;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
    }

    public void loadTrack(Map<String, Object> track) {
        this.trackData = track;
        removeAll();
        buildUI();
        revalidate();
        repaint();
        loadComments();
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIConstants.BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        String title = JsonHelper.getString(trackData, "title", "Untitled");
        String artist = JsonHelper.getString(trackData, "artist", JsonHelper.getString(trackData, "uploaderName", "Unknown"));
        String genre = JsonHelper.getString(trackData, "genre", "");
        String desc = JsonHelper.getString(trackData, "description", "");
        int duration = JsonHelper.getInt(trackData, "durationSeconds");
        int plays = JsonHelper.getInt(trackData, "playCount");
        int likes = JsonHelper.getInt(trackData, "likeCount");
        isLiked = JsonHelper.getBoolean(trackData, "likedByCurrentUser");
        int trackId = JsonHelper.getInt(trackData, "id");

        // Header
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // Play button (hình tròn với icon play vector)
        JButton bigPlayBtn = IconFactory.circlePlayButton(64, UIConstants.PRIMARY, Color.WHITE);
        bigPlayBtn.addActionListener(e -> playCurrentTrack());

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.FONT_TITLE);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel artistLabel = new JLabel(artist + (genre.isEmpty() ? "" : " · " + genre));
        artistLabel.setFont(UIConstants.FONT_BODY);
        artistLabel.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel statsLabel = new JLabel(plays + " plays | " + UIConstants.formatDuration(duration));
        statsLabel.setFont(UIConstants.FONT_SMALL);
        statsLabel.setForeground(UIConstants.TEXT_MUTED);

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(artistLabel);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(statsLabel);

        header.add(bigPlayBtn, BorderLayout.WEST);
        header.add(titlePanel, BorderLayout.CENTER);

        content.add(header);
        content.add(Box.createVerticalStrut(20));

        // Waveform
        waveformPanel = new WaveformPanel(player);
        waveformPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        waveformPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        content.add(waveformPanel);
        content.add(Box.createVerticalStrut(16));

        // Like and action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        likeLabel = new JLabel(" " + likes);
        likeLabel.setIcon(isLiked ? IconFactory.heartIcon(18, UIConstants.LIKE_RED, true) : IconFactory.heartIcon(18, UIConstants.TEXT_SECONDARY, false));
        likeLabel.setFont(UIConstants.FONT_HEADING);
        likeLabel.setForeground(isLiked ? UIConstants.LIKE_RED : UIConstants.TEXT_SECONDARY);
        likeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        likeLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { toggleLike(trackId); }
        });
        actions.add(likeLabel);

        // Nút "Add to Playlist" với icon plus
        JButton addToPlaylistBtn = new JButton(" Add to Playlist");
        addToPlaylistBtn.setIcon(IconFactory.plusIcon(16, UIConstants.ACCENT));
        addToPlaylistBtn.setFont(UIConstants.FONT_BUTTON);
        addToPlaylistBtn.setForeground(UIConstants.ACCENT);
        addToPlaylistBtn.setBorderPainted(false);
        addToPlaylistBtn.setContentAreaFilled(false);
        addToPlaylistBtn.setFocusPainted(false);
        addToPlaylistBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addToPlaylistBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent ev) {
                addToPlaylistBtn.setForeground(UIConstants.ACCENT_LIGHT);
                addToPlaylistBtn.setIcon(IconFactory.plusIcon(16, UIConstants.ACCENT_LIGHT));
            }
            public void mouseExited(MouseEvent ev) {
                addToPlaylistBtn.setForeground(UIConstants.ACCENT);
                addToPlaylistBtn.setIcon(IconFactory.plusIcon(16, UIConstants.ACCENT));
            }
        });
        addToPlaylistBtn.addActionListener(e -> {
            PlaylistPanel.showAddToPlaylistDialog(this, trackId, title);
        });
        actions.add(addToPlaylistBtn);

        content.add(actions);
        content.add(Box.createVerticalStrut(8));

        // Description
        if (desc != null && !desc.isEmpty()) {
            JLabel descLabel = new JLabel("<html><body style='width:500px'>" + desc + "</body></html>");
            descLabel.setFont(UIConstants.FONT_BODY);
            descLabel.setForeground(UIConstants.TEXT_SECONDARY);
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(descLabel);
            content.add(Box.createVerticalStrut(16));
        }

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.DIVIDER);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        content.add(sep);
        content.add(Box.createVerticalStrut(16));

        // Comment input
        JLabel commentsHeader = new JLabel("Comments");
        commentsHeader.setFont(UIConstants.FONT_SUBTITLE);
        commentsHeader.setForeground(UIConstants.TEXT_PRIMARY);
        commentsHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(commentsHeader);
        content.add(Box.createVerticalStrut(12));

        JPanel commentInputPanel = new JPanel(new BorderLayout(8, 0));
        commentInputPanel.setOpaque(false);
        commentInputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        commentInputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        commentField = new JTextField();
        commentField.setFont(UIConstants.FONT_BODY);
        commentField.setForeground(UIConstants.TEXT_PRIMARY);
        commentField.setBackground(UIConstants.BG_INPUT);
        commentField.setCaretColor(UIConstants.TEXT_PRIMARY);
        commentField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        commentField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) postComment(trackId);
            }
        });

        JButton postBtn = new JButton("Post");
        postBtn.setFont(UIConstants.FONT_BUTTON);
        postBtn.setForeground(Color.WHITE);
        postBtn.setBackground(UIConstants.PRIMARY);
        postBtn.setBorderPainted(false);
        postBtn.setFocusPainted(false);
        postBtn.setPreferredSize(new Dimension(70, 34));
        postBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        postBtn.addActionListener(e -> postComment(trackId));

        commentInputPanel.add(commentField, BorderLayout.CENTER);
        commentInputPanel.add(postBtn, BorderLayout.EAST);
        content.add(commentInputPanel);
        content.add(Box.createVerticalStrut(16));

        // Comments list
        commentsPanel = new JPanel();
        commentsPanel.setLayout(new BoxLayout(commentsPanel, BoxLayout.Y_AXIS));
        commentsPanel.setOpaque(false);
        commentsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(commentsPanel);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBackground(UIConstants.BG_DARK);
        add(scroll, BorderLayout.CENTER);
    }

    private void playCurrentTrack() {
        int trackId = JsonHelper.getInt(trackData, "id");
        String title = JsonHelper.getString(trackData, "title", "Untitled");
        String artist = JsonHelper.getString(trackData, "artist", "Unknown");

        new Thread(() -> {
            try {
                ApiClient.post("/tracks/" + trackId + "/play", new HashMap<>());
                byte[] audioData = ApiClient.downloadBytes("/tracks/" + trackId + "/stream");
                if (audioData != null) {
                    SwingUtilities.invokeLater(() -> {
                        player.load(audioData);
                        player.play();
                        playerBar.setTrackInfo(title, artist);
                        // Load waveform
                        float[] waveform = player.getWaveformData(200);
                        waveformPanel.setWaveformData(waveform);
                    });
                }
            } catch (Exception ex) {
                System.err.println("Error playing: " + ex.getMessage());
            }
        }).start();
    }

    private void toggleLike(int trackId) {
        new Thread(() -> {
            try {
                Map<String, Object> response;
                if (isLiked) {
                    response = ApiClient.delete("/tracks/" + trackId + "/like");
                } else {
                    response = ApiClient.post("/tracks/" + trackId + "/like", new HashMap<>());
                }
                Map<String, Object> data = JsonHelper.getMap(response, "data");
                boolean newLiked = JsonHelper.getBoolean(data, "liked");
                int newCount = JsonHelper.getInt(data, "likeCount");
                SwingUtilities.invokeLater(() -> {
                    isLiked = newLiked;
                    likeLabel.setText(" " + newCount);
                    likeLabel.setIcon(isLiked ? IconFactory.heartIcon(18, UIConstants.LIKE_RED, true) : IconFactory.heartIcon(18, UIConstants.TEXT_SECONDARY, false));
                    likeLabel.setForeground(isLiked ? UIConstants.LIKE_RED : UIConstants.TEXT_SECONDARY);
                });
            } catch (Exception ex) {
                System.err.println("Like error: " + ex.getMessage());
            }
        }).start();
    }

    private void postComment(int trackId) {
        String text = commentField.getText().trim();
        if (text.isEmpty()) return;
        int timestamp = player.getCurrentSeconds();

        new Thread(() -> {
            try {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("content", text);
                body.put("timestampSeconds", timestamp);
                Map<String, Object> response = ApiClient.post("/tracks/" + trackId + "/comments", body);
                if ("success".equals(JsonHelper.getString(response, "status"))) {
                    SwingUtilities.invokeLater(() -> {
                        commentField.setText("");
                        loadComments();
                    });
                }
            } catch (Exception ex) {
                System.err.println("Comment error: " + ex.getMessage());
            }
        }).start();
    }

    private void loadComments() {
        int trackId = JsonHelper.getInt(trackData, "id");
        new Thread(() -> {
            try {
                Map<String, Object> response = ApiClient.get("/tracks/" + trackId + "/comments");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> comments = (List<Map<String, Object>>) response.get("data");
                if (comments == null) comments = new ArrayList<>();
                final List<Map<String, Object>> finalComments = comments;

                SwingUtilities.invokeLater(() -> {
                    commentsPanel.removeAll();
                    for (Map<String, Object> c : finalComments) {
                        commentsPanel.add(createCommentCard(c));
                        commentsPanel.add(Box.createVerticalStrut(6));
                    }
                    if (finalComments.isEmpty()) {
                        JLabel empty = new JLabel("No comments yet. Be the first!");
                        empty.setFont(UIConstants.FONT_BODY);
                        empty.setForeground(UIConstants.TEXT_MUTED);
                        commentsPanel.add(empty);
                    }
                    commentsPanel.revalidate();
                    commentsPanel.repaint();
                });
            } catch (Exception ex) {
                System.err.println("Load comments error: " + ex.getMessage());
            }
        }).start();
    }

    private JPanel createCommentCard(Map<String, Object> comment) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        String user = JsonHelper.getString(comment, "displayName", JsonHelper.getString(comment, "username", "User"));
        String content = JsonHelper.getString(comment, "content", "");
        int ts = JsonHelper.getInt(comment, "timestampSeconds");

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel userLabel = new JLabel(user + "  @ " + UIConstants.formatDuration(ts));
        userLabel.setFont(UIConstants.FONT_SMALL);
        userLabel.setForeground(UIConstants.PRIMARY);

        JLabel contentLabel = new JLabel(content);
        contentLabel.setFont(UIConstants.FONT_BODY);
        contentLabel.setForeground(UIConstants.TEXT_PRIMARY);

        left.add(userLabel);
        left.add(Box.createVerticalStrut(2));
        left.add(contentLabel);

        card.add(left, BorderLayout.CENTER);
        return card;
    }
}

