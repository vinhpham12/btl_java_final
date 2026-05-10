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
 * Playlist management panel.
 * Hiển thị danh sách playlist và chi tiết playlist (bao gồm các track).
 * Hỗ trợ: Tạo mới, xóa playlist, xem track trong playlist, xóa track khỏi playlist.
 */
public class PlaylistPanel extends JPanel {

    private final AudioPlayer player;
    private final PlayerBar playerBar;
    private JPanel listPanel;

    // Panel hiển thị chi tiết playlist (danh sách track bên trong)
    private JPanel detailPanel;
    private JPanel detailTracksPanel;
    private JLabel detailTitle;
    private JLabel detailTrackCount;
    private CardLayout cardLayout;
    private JPanel cardPanel;

    // Lưu thông tin playlist đang xem chi tiết
    private int currentDetailPlaylistId = -1;

    // Lưu danh sách track của playlist đang xem để hỗ trợ Next/Prev
    private List<Map<String, Object>> currentDetailTracks = new ArrayList<>();

    // Static reference để có thể refresh từ dialog
    private static PlaylistPanel activeInstance = null;

    public PlaylistPanel(AudioPlayer player, PlayerBar playerBar) {
        this.player = player;
        this.playerBar = playerBar;
        activeInstance = this; // Lưu instance để refresh từ dialog
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(UIConstants.BG_DARK);

        // === Card 1: Danh sách playlist ===
        JPanel listView = new JPanel(new BorderLayout());
        listView.setBackground(UIConstants.BG_DARK);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIConstants.BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Header row
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel header = new JLabel("My Playlists");
        header.setFont(UIConstants.FONT_TITLE);
        header.setForeground(UIConstants.TEXT_PRIMARY);

        JButton createBtn = new JButton("+ New Playlist");
        createBtn.setFont(UIConstants.FONT_BUTTON);
        createBtn.setForeground(Color.WHITE);
        createBtn.setBackground(UIConstants.PRIMARY);
        createBtn.setBorderPainted(false);
        createBtn.setFocusPainted(false);
        createBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createBtn.addActionListener(e -> createPlaylist());

        headerRow.add(header, BorderLayout.WEST);
        headerRow.add(createBtn, BorderLayout.EAST);
        content.add(headerRow);
        content.add(Box.createVerticalStrut(20));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(listPanel);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBackground(UIConstants.BG_DARK);
        listView.add(scroll, BorderLayout.CENTER);

        // === Card 2: Chi tiết playlist (danh sách track) ===
        detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBackground(UIConstants.BG_DARK);

        JPanel detailContent = new JPanel();
        detailContent.setLayout(new BoxLayout(detailContent, BoxLayout.Y_AXIS));
        detailContent.setBackground(UIConstants.BG_DARK);
        detailContent.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Back button + Title
        JPanel detailHeader = new JPanel(new BorderLayout());
        detailHeader.setOpaque(false);
        detailHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton backBtn = new JButton(" Back");
        backBtn.setIcon(IconFactory.backIcon(14, UIConstants.PRIMARY));
        backBtn.setFont(UIConstants.FONT_BUTTON);
        backBtn.setForeground(UIConstants.PRIMARY);
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            cardLayout.show(cardPanel, "list");
            loadPlaylists(); // Refresh danh sách khi quay lại
        });

        detailTitle = new JLabel("Playlist");
        detailTitle.setFont(UIConstants.FONT_TITLE);
        detailTitle.setForeground(UIConstants.TEXT_PRIMARY);

        detailHeader.add(backBtn, BorderLayout.WEST);
        detailHeader.add(detailTitle, BorderLayout.CENTER);
        detailContent.add(detailHeader);
        detailContent.add(Box.createVerticalStrut(8));

        detailTrackCount = new JLabel("0 tracks");
        detailTrackCount.setFont(UIConstants.FONT_BODY);
        detailTrackCount.setForeground(UIConstants.TEXT_SECONDARY);
        detailTrackCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailContent.add(detailTrackCount);
        detailContent.add(Box.createVerticalStrut(16));

        // Danh sách track trong playlist
        detailTracksPanel = new JPanel();
        detailTracksPanel.setLayout(new BoxLayout(detailTracksPanel, BoxLayout.Y_AXIS));
        detailTracksPanel.setOpaque(false);
        detailTracksPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailContent.add(detailTracksPanel);

        JScrollPane detailScroll = new JScrollPane(detailContent);
        detailScroll.setBorder(null);
        detailScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.getVerticalScrollBar().setUnitIncrement(16);
        detailScroll.getVerticalScrollBar().setBackground(UIConstants.BG_DARK);
        detailPanel.add(detailScroll, BorderLayout.CENTER);

        // Đăng ký 2 card
        cardPanel.add(listView, "list");
        cardPanel.add(detailPanel, "detail");

        add(cardPanel, BorderLayout.CENTER);
    }

    public void loadPlaylists() {
        listPanel.removeAll();
        cardLayout.show(cardPanel, "list");
        new Thread(() -> {
            try {
                Map<String, Object> response = ApiClient.get("/playlists");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> playlists = (List<Map<String, Object>>) response.get("data");
                if (playlists == null) playlists = new ArrayList<>();

                final List<Map<String, Object>> finalPlaylists = playlists;
                SwingUtilities.invokeLater(() -> {
                    listPanel.removeAll();
                    if (finalPlaylists.isEmpty()) {
                        JLabel empty = new JLabel("No playlists yet. Create one!");
                        empty.setFont(UIConstants.FONT_BODY);
                        empty.setForeground(UIConstants.TEXT_MUTED);
                        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                        listPanel.add(empty);
                    } else {
                        for (Map<String, Object> pl : finalPlaylists) {
                            listPanel.add(createPlaylistCard(pl));
                            listPanel.add(Box.createVerticalStrut(8));
                        }
                    }
                    listPanel.revalidate();
                    listPanel.repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JLabel err = new JLabel("Failed to load playlists");
                    err.setForeground(UIConstants.ERROR);
                    listPanel.add(err);
                    listPanel.revalidate();
                });
            }
        }).start();
    }

    private JPanel createPlaylistCard(Map<String, Object> playlist) {
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
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Icon playlist - vector
        JLabel icon = new JLabel(IconFactory.musicNoteIcon(24, UIConstants.ACCENT));
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(40, 40));
        icon.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        String name = JsonHelper.getString(playlist, "name", "Playlist");
        int trackCount = JsonHelper.getInt(playlist, "trackCount");

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(UIConstants.FONT_HEADING);
        nameLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel countLabel = new JLabel(trackCount + " tracks");
        countLabel.setFont(UIConstants.FONT_SMALL);
        countLabel.setForeground(UIConstants.TEXT_SECONDARY);

        info.add(nameLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(countLabel);

        // Buttons panel (right side)
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);

        // Delete button
        JButton deleteBtn = IconFactory.iconButton(IconFactory.closeIcon(14, UIConstants.TEXT_MUTED));
        deleteBtn.setPreferredSize(new Dimension(30, 30));
        deleteBtn.addActionListener(e -> {
            deletePlaylist(JsonHelper.getInt(playlist, "id"));
        });
        deleteBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent ev) { deleteBtn.setIcon(IconFactory.closeIcon(14, UIConstants.ERROR)); }
            public void mouseExited(MouseEvent ev) { deleteBtn.setIcon(IconFactory.closeIcon(14, UIConstants.TEXT_MUTED)); }
        });
        buttons.add(deleteBtn);

        card.add(icon, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.EAST);

        // Click card => mở chi tiết playlist
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBackground(UIConstants.BG_HOVER); }
            public void mouseExited(MouseEvent e) { card.setBackground(UIConstants.BG_SURFACE); }
            public void mouseClicked(MouseEvent e) {
                // Không mở detail nếu click vào nút delete
                if (e.getSource() == card) {
                    int plId = JsonHelper.getInt(playlist, "id");
                    String plName = JsonHelper.getString(playlist, "name", "Playlist");
                    openPlaylistDetail(plId, plName);
                }
            }
        });

        return card;
    }

    /**
     * Mở chi tiết playlist: hiển thị các bài hát trong playlist.
     */
    private void openPlaylistDetail(int playlistId, String playlistName) {
        currentDetailPlaylistId = playlistId;
        detailTitle.setText("  " + playlistName);
        detailTracksPanel.removeAll();
        detailTrackCount.setText("Loading...");
        cardLayout.show(cardPanel, "detail");

        new Thread(() -> {
            try {
                Map<String, Object> response = ApiClient.get("/playlists/" + playlistId);
                Map<String, Object> data = JsonHelper.getMap(response, "data");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tracks = (List<Map<String, Object>>) data.get("tracks");
                if (tracks == null) tracks = new ArrayList<>();

                final List<Map<String, Object>> finalTracks = tracks;
                SwingUtilities.invokeLater(() -> {
                    // Lưu danh sách track để dùng cho playlist queue
                    currentDetailTracks = new ArrayList<>(finalTracks);
                    detailTracksPanel.removeAll();
                    detailTrackCount.setText(finalTracks.size() + " tracks");
                    if (finalTracks.isEmpty()) {
                        JLabel empty = new JLabel("No tracks in this playlist yet. Add tracks from the Home or Track page!");
                        empty.setFont(UIConstants.FONT_BODY);
                        empty.setForeground(UIConstants.TEXT_MUTED);
                        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                        detailTracksPanel.add(empty);
                    } else {
                        for (int i = 0; i < finalTracks.size(); i++) {
                            Map<String, Object> track = finalTracks.get(i);
                            detailTracksPanel.add(createPlaylistTrackCard(track, i + 1, playlistId));
                            detailTracksPanel.add(Box.createVerticalStrut(4));
                        }
                    }
                    detailTracksPanel.revalidate();
                    detailTracksPanel.repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    detailTrackCount.setText("Error loading playlist");
                });
            }
        }).start();
    }

    /**
     * Tạo card hiển thị 1 track bên trong playlist detail.
     * Bao gồm: số thứ tự, play button, tên track, artist, nút remove.
     */
    private JPanel createPlaylistTrackCard(Map<String, Object> track, int position, int playlistId) {
        JPanel card = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBackground(UIConstants.BG_SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Left: Position number + Play button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        JLabel posLabel = new JLabel(String.valueOf(position));
        posLabel.setFont(UIConstants.FONT_BODY);
        posLabel.setForeground(UIConstants.TEXT_MUTED);
        posLabel.setPreferredSize(new Dimension(24, 30));
        posLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton playBtn = IconFactory.iconButton(IconFactory.playIcon(16, UIConstants.PRIMARY));
        playBtn.setPreferredSize(new Dimension(32, 32));
        playBtn.addActionListener(e -> playTrack(track));

        leftPanel.add(posLabel);
        leftPanel.add(playBtn);

        // Center: Track info
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        String title = JsonHelper.getString(track, "title", "Untitled");
        String artist = JsonHelper.getString(track, "artist", JsonHelper.getString(track, "uploaderName", "Unknown"));
        int duration = JsonHelper.getInt(track, "durationSeconds");

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.FONT_HEADING);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel artistLabel = new JLabel(artist + " \u00B7 " + UIConstants.formatDuration(duration));
        artistLabel.setFont(UIConstants.FONT_SMALL);
        artistLabel.setForeground(UIConstants.TEXT_SECONDARY);

        info.add(titleLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(artistLabel);

        // Right: Remove button
        JButton removeBtn = IconFactory.iconButton(IconFactory.closeIcon(14, UIConstants.TEXT_MUTED));
        removeBtn.setPreferredSize(new Dimension(32, 32));
        removeBtn.setToolTipText("Remove from playlist");
        removeBtn.addActionListener(e -> {
            int trackId = JsonHelper.getInt(track, "id");
            removeTrackFromPlaylist(playlistId, trackId);
        });
        removeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent ev) { removeBtn.setIcon(IconFactory.closeIcon(14, UIConstants.ERROR)); }
            public void mouseExited(MouseEvent ev) { removeBtn.setIcon(IconFactory.closeIcon(14, UIConstants.TEXT_MUTED)); }
        });

        card.add(leftPanel, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(removeBtn, BorderLayout.EAST);

        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBackground(UIConstants.BG_HOVER); }
            public void mouseExited(MouseEvent e) { card.setBackground(UIConstants.BG_SURFACE); }
        });

        return card;
    }

    /**
     * Phát 1 track từ playlist.
     * Set toàn bộ danh sách track vào PlayerBar queue để hỗ trợ Next/Prev.
     */
    private void playTrack(Map<String, Object> track) {
        int trackId = JsonHelper.getInt(track, "id");
        String title = JsonHelper.getString(track, "title", "Untitled");
        String artist = JsonHelper.getString(track, "artist", "Unknown");

        // Tìm index của track trong danh sách hiện tại
        int trackIndex = -1;
        for (int i = 0; i < currentDetailTracks.size(); i++) {
            if (JsonHelper.getInt(currentDetailTracks.get(i), "id") == trackId) {
                trackIndex = i;
                break;
            }
        }

        // Set playlist queue vào PlayerBar để hỗ trợ chuyển bài Next/Prev
        if (trackIndex >= 0) {
            playerBar.setPlaylistQueue(currentDetailTracks, trackIndex);
        }

        new Thread(() -> {
            try {
                ApiClient.post("/tracks/" + trackId + "/play", new HashMap<>());
                byte[] audioData = ApiClient.downloadBytes("/tracks/" + trackId + "/stream");
                if (audioData != null) {
                    SwingUtilities.invokeLater(() -> {
                        player.load(audioData);
                        player.play();
                        playerBar.setTrackInfo(title, artist);
                        playerBar.setCurrentTrackData(trackId, title, artist);
                    });
                }
            } catch (Exception ex) {
                System.err.println("Error playing track: " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Xóa track khỏi playlist.
     */
    private void removeTrackFromPlaylist(int playlistId, int trackId) {
        new Thread(() -> {
            try {
                ApiClient.delete("/playlists/" + playlistId + "/tracks/" + trackId);
                SwingUtilities.invokeLater(() -> {
                    // Refresh chi tiết playlist
                    openPlaylistDetail(playlistId, detailTitle.getText().trim());
                });
            } catch (Exception e) {
                System.err.println("Remove track error: " + e.getMessage());
            }
        }).start();
    }

    private void createPlaylist() {
        String name = JOptionPane.showInputDialog(this, "Playlist name:", "New Playlist", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        new Thread(() -> {
            try {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("name", name.trim());
                body.put("description", "");
                body.put("isPublic", true);
                ApiClient.post("/playlists", body);
                SwingUtilities.invokeLater(this::loadPlaylists);
            } catch (Exception e) {
                System.err.println("Create playlist error: " + e.getMessage());
            }
        }).start();
    }

    private void deletePlaylist(int playlistId) {
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this playlist?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                ApiClient.delete("/playlists/" + playlistId);
                SwingUtilities.invokeLater(this::loadPlaylists);
            } catch (Exception e) {
                System.err.println("Delete playlist error: " + e.getMessage());
            }
        }).start();
    }

    // ====================================================================
    // STATIC UTILITY: Hiển thị dialog "Add to Playlist" cho bất kỳ track nào
    // Được gọi từ HomePanel, TrackPanel, SearchPanel, v.v.
    // ====================================================================

    /**
     * Hiển thị dialog cho phép user chọn playlist để thêm track vào.
     * @param parent Component cha (để hiển thị dialog)
     * @param trackId ID của track cần thêm
     * @param trackTitle Tên track (để hiển thị trong dialog)
     */
    public static void showAddToPlaylistDialog(Component parent, int trackId, String trackTitle) {
        new Thread(() -> {
            try {
                // 1. Lấy danh sách playlist của user
                Map<String, Object> response = ApiClient.get("/playlists");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> playlists = (List<Map<String, Object>>) response.get("data");
                if (playlists == null) playlists = new ArrayList<>();

                if (playlists.isEmpty()) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(parent,
                            "You don't have any playlists yet.\nGo to Playlists to create one first!",
                            "No Playlists", JOptionPane.INFORMATION_MESSAGE);
                    });
                    return;
                }

                // 2. Tạo danh sách tên playlist để hiển thị
                final List<Map<String, Object>> finalPlaylists = playlists;
                String[] playlistNames = new String[playlists.size()];
                for (int i = 0; i < playlists.size(); i++) {
                    String plName = JsonHelper.getString(playlists.get(i), "name", "Playlist");
                    int plTrackCount = JsonHelper.getInt(playlists.get(i), "trackCount");
                    playlistNames[i] = plName + " (" + plTrackCount + " tracks)";
                }

                SwingUtilities.invokeLater(() -> {
                    // 3. Hiển thị dialog chọn playlist
                    String selected = (String) JOptionPane.showInputDialog(parent,
                        "Add \"" + trackTitle + "\" to playlist:",
                        "Add to Playlist",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        playlistNames,
                        playlistNames[0]);

                    if (selected == null) return; // User cancelled

                    // 4. Tìm playlist ID từ tên đã chọn
                    int selectedIndex = -1;
                    for (int i = 0; i < playlistNames.length; i++) {
                        if (playlistNames[i].equals(selected)) {
                            selectedIndex = i;
                            break;
                        }
                    }

                    if (selectedIndex >= 0) {
                        int playlistId = JsonHelper.getInt(finalPlaylists.get(selectedIndex), "id");
                        // 5. Gọi API thêm track vào playlist
                        new Thread(() -> {
                            try {
                                Map<String, Object> body = new LinkedHashMap<>();
                                body.put("trackId", trackId);
                                Map<String, Object> result = ApiClient.post("/playlists/" + playlistId + "/tracks", body);
                                String status = JsonHelper.getString(result, "status");
                                SwingUtilities.invokeLater(() -> {
                                    if ("success".equals(status)) {
                                        JOptionPane.showMessageDialog(parent,
                                            "\"" + trackTitle + "\" has been added to the playlist!",
                                            "Success", JOptionPane.INFORMATION_MESSAGE);
                                        // Refresh danh sách playlist nếu đang mở
                                        if (activeInstance != null) {
                                            activeInstance.loadPlaylists();
                                        }
                                    } else {
                                        String msg = JsonHelper.getString(result, "message", "Failed to add track");
                                        JOptionPane.showMessageDialog(parent, msg,
                                            "Error", JOptionPane.ERROR_MESSAGE);
                                    }
                                });
                            } catch (Exception ex) {
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(parent,
                                        "Error: " + ex.getMessage(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                });
                            }
                        }).start();
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parent,
                        "Could not load playlists: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
}
