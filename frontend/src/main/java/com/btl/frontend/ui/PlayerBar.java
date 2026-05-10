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
import com.btl.frontend.audio.QueueManager;
import com.btl.frontend.util.UIConstants;
import com.btl.frontend.util.IconFactory;
import com.btl.frontend.util.JsonHelper;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Bottom player bar - fixed at the bottom of the window.
 * Shows: track info, play controls, progress slider, volume.
 * 
 * Quản lý playlist queue để hỗ trợ chuyển bài Next/Prev:
 * - setPlaylistQueue(): Set danh sách bài khi phát từ playlist
 * - clearPlaylistQueue(): Xóa queue khi phát bài ngoài playlist
 * - Nút Next: chuyển bài tiếp theo trong queue
 * - Nút Prev: click 1 lần = tua về đầu, click 2 lần nhanh = bài trước
 * - Auto-advance: bài hết → tự động phát bài tiếp theo
 */
public class PlayerBar extends JPanel {

    private final AudioPlayer player;
    private QueueManager queueManager;
    private JLabel trackTitle;
    private JLabel trackArtist;
    private JButton playPauseBtn;
    private JSlider progressSlider;
    private JSlider volumeSlider;
    private JLabel currentTimeLabel;
    private JLabel totalTimeLabel;
    private boolean isUserDragging = false;
    private String currentTrackTitle = "";
    private String currentTrackArtist = "";

    // === Playlist Queue - Quản lý danh sách bài trong playlist đang phát ===
    private List<Map<String, Object>> playlistQueue = new ArrayList<>(); // Danh sách bài trong playlist
    private int currentTrackIndex = -1;                                   // Vị trí bài đang phát (-1 = không có queue)

    // === Lịch sử phát nhạc - để hỗ trợ nút Prev khi không có playlist ===
    private final java.util.Deque<Map<String, Object>> trackHistory = new java.util.ArrayDeque<>(); // Stack lưu bài đã phát
    private Map<String, Object> currentPlayingTrack = null; // Track đang phát hiện tại (chứa id, title, artist)

    // === Double-click detection cho nút Prev ===
    private javax.swing.Timer prevClickTimer;    // Timer 500ms chờ click thứ 2
    private int prevClickCount = 0;  // Đếm số lần click

    public PlayerBar(AudioPlayer player) {
        this.player = player;
        setBackground(UIConstants.BG_SURFACE);
        setPreferredSize(new Dimension(0, UIConstants.PLAYER_BAR_HEIGHT));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        setLayout(new BorderLayout(12, 0));
        buildUI();
        setupPlayerListener();
    }

    private void buildUI() {
        // Left: Track info
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        infoPanel.setPreferredSize(new Dimension(200, UIConstants.PLAYER_BAR_HEIGHT));

        trackTitle = new JLabel("No track selected");
        trackTitle.setFont(UIConstants.FONT_HEADING);
        trackTitle.setForeground(UIConstants.TEXT_PRIMARY);
        trackTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        trackArtist = new JLabel("--");
        trackArtist.setFont(UIConstants.FONT_SMALL);
        trackArtist.setForeground(UIConstants.TEXT_SECONDARY);
        trackArtist.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(trackTitle);
        infoPanel.add(Box.createVerticalStrut(4));
        infoPanel.add(trackArtist);
        infoPanel.add(Box.createVerticalGlue());

        // Center: Controls + Progress
        JPanel centerPanel = new JPanel(new BorderLayout(0, 4));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        controls.setOpaque(false);

        // Prev button - hỗ trợ double-click detection
        JButton prevBtn = IconFactory.iconButton(IconFactory.prevIcon(24, UIConstants.TEXT_PRIMARY));
        prevBtn.setPreferredSize(new Dimension(36, 36));
        prevBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { prevBtn.setIcon(IconFactory.prevIcon(24, UIConstants.PRIMARY)); }
            public void mouseExited(MouseEvent e) { prevBtn.setIcon(IconFactory.prevIcon(24, UIConstants.TEXT_PRIMARY)); }
        });
        // Logic Prev: click 1 lần = tua về đầu, click 2 lần nhanh (trong 500ms) = bài trước
        prevBtn.addActionListener(e -> handlePrevClick());

        // Play/Pause button
        playPauseBtn = IconFactory.iconButton(IconFactory.playIcon(32, UIConstants.TEXT_PRIMARY));
        playPauseBtn.setPreferredSize(new Dimension(44, 44));

        // Next button - chuyển bài tiếp theo trong playlist queue
        JButton nextBtn = IconFactory.iconButton(IconFactory.nextIcon(24, UIConstants.TEXT_PRIMARY));
        nextBtn.setPreferredSize(new Dimension(36, 36));
        nextBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { nextBtn.setIcon(IconFactory.nextIcon(24, UIConstants.PRIMARY)); }
            public void mouseExited(MouseEvent e) { nextBtn.setIcon(IconFactory.nextIcon(24, UIConstants.TEXT_PRIMARY)); }
        });
        // Nút Next: chuyển bài tiếp trong playlist, nếu không có playlist → phát ngẫu nhiên
        nextBtn.addActionListener(e -> {
            if (!playNextTrack()) {
                playRandomTrack();
            }
        });

        playPauseBtn.addActionListener(e -> player.togglePlayPause());


        controls.add(prevBtn);
        controls.add(playPauseBtn);
        controls.add(nextBtn);

        // Progress bar
        JPanel progressPanel = new JPanel(new BorderLayout(8, 0));
        progressPanel.setOpaque(false);

        currentTimeLabel = new JLabel("0:00");
        currentTimeLabel.setFont(UIConstants.FONT_TINY);
        currentTimeLabel.setForeground(UIConstants.TEXT_SECONDARY);
        currentTimeLabel.setPreferredSize(new Dimension(40, 14));

        totalTimeLabel = new JLabel("0:00");
        totalTimeLabel.setFont(UIConstants.FONT_TINY);
        totalTimeLabel.setForeground(UIConstants.TEXT_SECONDARY);
        totalTimeLabel.setPreferredSize(new Dimension(40, 14));
        totalTimeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        progressSlider = new JSlider(0, 100, 0);
        progressSlider.setOpaque(false);
        progressSlider.setFocusable(false);
        progressSlider.setUI(new CustomSliderUI(progressSlider, UIConstants.PRIMARY, UIConstants.BG_HOVER));
        progressSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { isUserDragging = true; }
            @Override
            public void mouseReleased(MouseEvent e) {
                isUserDragging = false;
                int seconds = (int) ((progressSlider.getValue() / 100.0) * player.getTotalSeconds());
                player.seek(seconds);
            }
        });

        progressPanel.add(currentTimeLabel, BorderLayout.WEST);
        progressPanel.add(progressSlider, BorderLayout.CENTER);
        progressPanel.add(totalTimeLabel, BorderLayout.EAST);

        centerPanel.add(controls, BorderLayout.NORTH);
        centerPanel.add(progressPanel, BorderLayout.CENTER);

        // Right: Volume
        JPanel volumePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        volumePanel.setOpaque(false);
        volumePanel.setPreferredSize(new Dimension(160, UIConstants.PLAYER_BAR_HEIGHT));
        volumePanel.setBorder(BorderFactory.createEmptyBorder(28, 0, 28, 16));

        JLabel volIcon = new JLabel(IconFactory.volumeIcon(18, UIConstants.TEXT_SECONDARY));

        volumeSlider = new JSlider(0, 100, 80);
        volumeSlider.setOpaque(false);
        volumeSlider.setPreferredSize(new Dimension(100, 20));
        volumeSlider.setFocusable(false);
        volumeSlider.setUI(new CustomSliderUI(volumeSlider, UIConstants.PRIMARY, UIConstants.BG_HOVER));
        volumeSlider.addChangeListener(e -> player.setVolume(volumeSlider.getValue() / 100f));

        volumePanel.add(volIcon);
        volumePanel.add(volumeSlider);

        add(infoPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(volumePanel, BorderLayout.EAST);
    }

    private void setupPlayerListener() {
        player.addListener(new AudioPlayer.PlayerListener() {
            @Override
            public void onPositionChanged(int currentSeconds, int totalSeconds) {
                SwingUtilities.invokeLater(() -> {
                    if (!isUserDragging) {
                        int progress = totalSeconds > 0 ? (int) ((currentSeconds * 100.0) / totalSeconds) : 0;
                        progressSlider.setValue(progress);
                    }
                    currentTimeLabel.setText(UIConstants.formatDuration(currentSeconds));
                    totalTimeLabel.setText(UIConstants.formatDuration(totalSeconds));
                });
            }

            @Override
            public void onStateChanged(AudioPlayer.PlayState state) {
                SwingUtilities.invokeLater(() -> {
                    if (state == AudioPlayer.PlayState.PLAYING) {
                        playPauseBtn.setIcon(IconFactory.pauseIcon(32, UIConstants.TEXT_PRIMARY));
                    } else {
                        playPauseBtn.setIcon(IconFactory.playIcon(32, UIConstants.TEXT_PRIMARY));
                    }
                });
            }

            @Override
            public void onTrackFinished() {
                SwingUtilities.invokeLater(() -> {
                    progressSlider.setValue(0);
                    currentTimeLabel.setText("0:00");
                    // Auto-advance: thử phát bài tiếp theo trong playlist queue
                    // Nếu không có queue hoặc đã hết → phát bài ngẫu nhiên
                    if (!playNextTrack()) {
                        playRandomTrack();
                    }
                });
            }
        });
    }

    public void setTrackInfo(String title, String artist) {
        // Lưu bài hiện tại vào lịch sử trước khi chuyển bài mới
        if (currentPlayingTrack != null) {
            trackHistory.push(new HashMap<>(currentPlayingTrack));
            // Giới hạn lịch sử tối đa 50 bài
            while (trackHistory.size() > 50) {
                trackHistory.removeLast();
            }
        }
        this.currentTrackTitle = title;
        this.currentTrackArtist = artist;
        trackTitle.setText(title);
        trackArtist.setText(artist);
        totalTimeLabel.setText(UIConstants.formatDuration(player.getTotalSeconds()));
    }

    /**
     * Lưu thông tin track đang phát (gọi từ bên ngoài khi biết trackId).
     * Dùng để nút Prev có thể phát lại bài trước đó.
     */
    public void setCurrentTrackData(int trackId, String title, String artist) {
        currentPlayingTrack = new HashMap<>();
        currentPlayingTrack.put("id", trackId);
        currentPlayingTrack.put("title", title);
        currentPlayingTrack.put("artist", artist);
    }
    /** Kết nối QueueManager để Prev/Next hoạt động */
    public void setQueueManager(QueueManager qm) { this.queueManager = qm; }


    // ====================================================================
    // PLAYLIST QUEUE MANAGEMENT
    // ====================================================================

    /**
     * Set playlist queue khi user phát 1 bài từ playlist detail.
     * Toàn bộ danh sách track trong playlist được lưu lại để hỗ trợ Next/Prev.
     *
     * @param tracks     Danh sách tất cả track trong playlist
     * @param startIndex Vị trí bài đang được phát (0-based)
     */
    public void setPlaylistQueue(List<Map<String, Object>> tracks, int startIndex) {
        this.playlistQueue = new ArrayList<>(tracks); // Copy để tránh thay đổi ngoài ý muốn
        this.currentTrackIndex = startIndex;
    }

    /**
     * Xóa playlist queue. Gọi khi phát bài từ ngoài playlist (Home, Search, Track, Profile).
     * Sau khi xóa, nút Next/Prev sẽ không chuyển bài.
     */
    public void clearPlaylistQueue() {
        this.playlistQueue.clear();
        this.currentTrackIndex = -1;
    }

    /**
     * Phát bài tiếp theo trong playlist queue.
     * Nếu đang ở bài cuối hoặc không có queue → trả về false.
     * @return true nếu đã chuyển bài thành công, false nếu không thể chuyển
     */
    private boolean playNextTrack() {
        if (playlistQueue.isEmpty() || currentTrackIndex < 0) return false;
        int nextIndex = currentTrackIndex + 1;
        if (nextIndex >= playlistQueue.size()) return false; // Đã là bài cuối
        playTrackAtIndex(nextIndex);
        return true;
    }

    /**
     * Phát bài trước trong playlist queue.
     * Nếu đang ở bài đầu tiên hoặc không có queue → không làm gì.
     */
    private void playPrevTrack() {
        if (playlistQueue.isEmpty() || currentTrackIndex < 0) return;
        int prevIndex = currentTrackIndex - 1;
        if (prevIndex < 0) return; // Đã là bài đầu tiên
        playTrackAtIndex(prevIndex);
    }

    /**
     * Phát bài tại vị trí index trong playlist queue.
     * Download audio từ server và phát.
     */
    private void playTrackAtIndex(int index) {
        if (index < 0 || index >= playlistQueue.size()) return;
        currentTrackIndex = index;
        Map<String, Object> track = playlistQueue.get(index);

        int trackId = JsonHelper.getInt(track, "id");
        String title = JsonHelper.getString(track, "title", "Untitled");
        String artist = JsonHelper.getString(track, "artist",
                JsonHelper.getString(track, "uploaderName", "Unknown"));

        new Thread(() -> {
            try {
                // Tăng play count
                ApiClient.post("/tracks/" + trackId + "/play", new HashMap<>());
                // Download audio data
                byte[] audioData = ApiClient.downloadBytes("/tracks/" + trackId + "/stream");
                if (audioData != null) {
                    SwingUtilities.invokeLater(() -> {
                        player.load(audioData);
                        player.play();
                        setTrackInfo(title, artist);
                        setCurrentTrackData(trackId, title, artist);
                    });
                }
            } catch (Exception ex) {
                System.err.println("[PlayerBar] Error playing track at index " + index + ": " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Phát bài ngẫu nhiên khi không có playlist queue hoặc đã hết playlist.
     * Lấy danh sách track từ server (phổ biến nhất) và chọn ngẫu nhiên 1 bài.
     */
    private void playRandomTrack() {
        new Thread(() -> {
            try {
                // Lấy danh sách track phổ biến từ server
                Map<String, Object> response = ApiClient.get("/tracks?sort=popular&limit=30");
                String status = JsonHelper.getString(response, "status");
                if (!"success".equals(status)) return;

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tracks = (List<Map<String, Object>>) response.get("data");
                if (tracks == null || tracks.isEmpty()) return;

                // Chọn ngẫu nhiên 1 bài (tránh chọn lại bài đang phát)
                java.util.Random random = new java.util.Random();
                Map<String, Object> randomTrack = null;
                for (int attempt = 0; attempt < 5; attempt++) {
                    Map<String, Object> candidate = tracks.get(random.nextInt(tracks.size()));
                    String candidateTitle = JsonHelper.getString(candidate, "title", "");
                    if (!candidateTitle.equals(currentTrackTitle) || tracks.size() <= 1) {
                        randomTrack = candidate;
                        break;
                    }
                }
                if (randomTrack == null) randomTrack = tracks.get(random.nextInt(tracks.size()));

                int trackId = JsonHelper.getInt(randomTrack, "id");
                String title = JsonHelper.getString(randomTrack, "title", "Untitled");
                String artist = JsonHelper.getString(randomTrack, "artist",
                        JsonHelper.getString(randomTrack, "uploaderName", "Unknown"));

                // Tăng play count + download audio
                ApiClient.post("/tracks/" + trackId + "/play", new HashMap<>());
                byte[] audioData = ApiClient.downloadBytes("/tracks/" + trackId + "/stream");
                if (audioData != null) {
                    SwingUtilities.invokeLater(() -> {
                        clearPlaylistQueue(); // Đảm bảo queue rỗng
                        player.load(audioData);
                        player.play();
                        setTrackInfo(title, artist);
                        setCurrentTrackData(trackId, title, artist);
                    });
                }
            } catch (Exception ex) {
                System.err.println("[PlayerBar] Error playing random track: " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Xử lý logic double-click cho nút Prev:
     * - Click 1 lần: Đợi 500ms, nếu không có click thứ 2 → tua về đầu bài (seek(0))
     * - Click 2 lần (trong 500ms): Cancel timer → chuyển về bài trước trong queue
     *   hoặc phát lại bài trước trong lịch sử nếu không có playlist
     */
    private void handlePrevClick() {
        prevClickCount++;
        if (prevClickCount == 1) {
            // Click lần 1: bắt đầu timer 500ms
            prevClickTimer = new javax.swing.Timer(500, evt -> {
                // Hết 500ms mà không có click lần 2 → tua về đầu bài
                prevClickCount = 0;
                player.seek(0);
            });
            prevClickTimer.setRepeats(false);
            prevClickTimer.start();
        } else if (prevClickCount >= 2) {
            // Click lần 2 trong 500ms → chuyển bài trước
            if (prevClickTimer != null) {
                prevClickTimer.stop();
            }
            prevClickCount = 0;
            // Nếu có playlist queue → chuyển bài trong playlist
            // Nếu không có playlist → phát lại bài trước trong lịch sử
            if (!playlistQueue.isEmpty() && currentTrackIndex > 0) {
                playPrevTrack();
            } else {
                playFromHistory();
            }
        }
    }

    /**
     * Phát lại bài trước đó từ lịch sử (khi không có playlist).
     * Lấy bài từ đỉnh stack trackHistory, download và phát.
     */
    private void playFromHistory() {
        if (trackHistory.isEmpty()) return;
        Map<String, Object> prevTrack = trackHistory.pop();

        int trackId = JsonHelper.getInt(prevTrack, "id");
        String title = JsonHelper.getString(prevTrack, "title", "Untitled");
        String artist = JsonHelper.getString(prevTrack, "artist", "Unknown");

        new Thread(() -> {
            try {
                ApiClient.post("/tracks/" + trackId + "/play", new HashMap<>());
                byte[] audioData = ApiClient.downloadBytes("/tracks/" + trackId + "/stream");
                if (audioData != null) {
                    SwingUtilities.invokeLater(() -> {
                        clearPlaylistQueue();
                        player.load(audioData);
                        player.play();
                        // Set trực tiếp không qua setTrackInfo để không push lại vào history
                        currentTrackTitle = title;
                        currentTrackArtist = artist;
                        trackTitle.setText(title);
                        trackArtist.setText(artist);
                        totalTimeLabel.setText(UIConstants.formatDuration(player.getTotalSeconds()));
                        currentPlayingTrack = new HashMap<>(prevTrack);
                    });
                }
            } catch (Exception ex) {
                System.err.println("[PlayerBar] Error playing from history: " + ex.getMessage());
            }
        }).start();
    }

    /**
     * Custom slider UI with rounded track and thumb.
     */
    static class CustomSliderUI extends javax.swing.plaf.basic.BasicSliderUI {
        private final Color fillColor;
        private final Color trackColor;

        CustomSliderUI(JSlider slider, Color fill, Color track) {
            super(slider);
            this.fillColor = fill;
            this.trackColor = track;
        }

        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = trackRect.y + trackRect.height / 2 - 2;
            g2.setColor(trackColor);
            g2.fill(new RoundRectangle2D.Float(trackRect.x, y, trackRect.width, 4, 4, 4));
            int fillWidth = thumbRect.x - trackRect.x + thumbRect.width / 2;
            g2.setColor(fillColor);
            g2.fill(new RoundRectangle2D.Float(trackRect.x, y, fillWidth, 4, 4, 4));
            g2.dispose();
        }

        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = thumbRect.x + thumbRect.width / 2;
            int cy = thumbRect.y + thumbRect.height / 2;
            g2.setColor(Color.WHITE);
            g2.fillOval(cx - 6, cy - 6, 12, 12);
            g2.dispose();
        }
    }
}
