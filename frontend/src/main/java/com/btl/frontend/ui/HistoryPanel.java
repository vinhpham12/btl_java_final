package com.btl.frontend.ui;

import com.btl.frontend.api.ApiClient;
import com.btl.frontend.audio.AudioPlayer;
import com.btl.frontend.audio.QueueManager;
import com.btl.frontend.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Panel lịch sử nghe nhạc.
 * Hiển thị danh sách bài đã nghe gần đây, nút xóa lịch sử.
 */
public class HistoryPanel extends JPanel {

    private final AudioPlayer player;
    private final PlayerBar playerBar;
    private QueueManager queueManager;
    private JPanel tracksContainer;
    private JLabel statusLabel;

    public HistoryPanel(AudioPlayer player, PlayerBar playerBar) {
        this.player = player;
        this.playerBar = playerBar;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    public void setQueueManager(QueueManager qm) { this.queueManager = qm; }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIConstants.BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Header
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel header = new JLabel("Listening History");
        header.setFont(UIConstants.FONT_TITLE);
        header.setForeground(UIConstants.TEXT_PRIMARY);

        JButton clearBtn = new JButton("Clear All");
        clearBtn.setFont(UIConstants.FONT_BUTTON);
        clearBtn.setForeground(UIConstants.ERROR);
        clearBtn.setBorderPainted(false);
        clearBtn.setContentAreaFilled(false);
        clearBtn.setFocusPainted(false);
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                "Xóa toàn bộ lịch sử nghe?", "Xác nhận",
                JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) clearHistory();
        });

        headerRow.add(header, BorderLayout.WEST);
        headerRow.add(clearBtn, BorderLayout.EAST);
        content.add(headerRow);
        content.add(Box.createVerticalStrut(8));

        JLabel sub = new JLabel("Recently played tracks");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(sub);
        content.add(Box.createVerticalStrut(20));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UIConstants.FONT_BODY);
        statusLabel.setForeground(UIConstants.TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(statusLabel);
        content.add(Box.createVerticalStrut(8));

        tracksContainer = new JPanel();
        tracksContainer.setLayout(new BoxLayout(tracksContainer, BoxLayout.Y_AXIS));
        tracksContainer.setOpaque(false);
        tracksContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(tracksContainer);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBackground(UIConstants.BG_DARK);
        add(scroll, BorderLayout.CENTER);
    }

    public void loadHistory() {
        statusLabel.setText("Loading history...");
        tracksContainer.removeAll();
        tracksContainer.revalidate();
        tracksContainer.repaint();

        new Thread(() -> {
            try {
                Map<String, Object> response = ApiClient.get("/history");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> history = (List<Map<String, Object>>) response.get("data");
                if (history == null) history = new ArrayList<>();
                final List<Map<String, Object>> finalHistory = history;

                SwingUtilities.invokeLater(() -> {
                    tracksContainer.removeAll();
                    if (finalHistory.isEmpty()) {
                        statusLabel.setText("No listening history yet. Start playing some tracks!");
                    } else {
                        statusLabel.setText(finalHistory.size() + " tracks played recently");
                        for (Map<String, Object> track : finalHistory) {
                            tracksContainer.add(createHistoryCard(track));
                            tracksContainer.add(Box.createVerticalStrut(2));
                        }
                    }
                    tracksContainer.revalidate();
                    tracksContainer.repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Failed to load history"));
            }
        }).start();
    }

    private JPanel createHistoryCard(Map<String, Object> track) {
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
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Play button
        JButton playBtn = IconFactory.iconButton(IconFactory.playIcon(18, UIConstants.PRIMARY));
        playBtn.setPreferredSize(new Dimension(36, 36));

        // Info
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        String title = JsonHelper.getString(track, "title", "Untitled");
        String artist = JsonHelper.getString(track, "artist", JsonHelper.getString(track, "uploaderName", "Unknown"));
        int duration = JsonHelper.getInt(track, "durationSeconds");
        String listenedAt = JsonHelper.getString(track, "listenedAt", "");
        // Chỉ lấy phần ngày/giờ
        if (listenedAt.length() > 16) listenedAt = listenedAt.substring(0, 16);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.FONT_HEADING);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel detailLabel = new JLabel(artist + " · " + UIConstants.formatDuration(duration));
        detailLabel.setFont(UIConstants.FONT_SMALL);
        detailLabel.setForeground(UIConstants.TEXT_SECONDARY);

        info.add(titleLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(detailLabel);

        // Right: time + queue button
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        JLabel timeLabel = new JLabel(listenedAt);
        timeLabel.setFont(UIConstants.FONT_TINY);
        timeLabel.setForeground(UIConstants.TEXT_MUTED);

        JButton queueBtn = IconFactory.iconButton(IconFactory.queueIcon(14, UIConstants.TEXT_MUTED));
        queueBtn.setPreferredSize(new Dimension(28, 28));
        queueBtn.setToolTipText("Add to queue");
        queueBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent ev) { queueBtn.setIcon(IconFactory.queueIcon(14, UIConstants.ACCENT)); }
            public void mouseExited(MouseEvent ev) { queueBtn.setIcon(IconFactory.queueIcon(14, UIConstants.TEXT_MUTED)); }
        });
        queueBtn.addActionListener(e -> {
            if (queueManager != null) queueManager.addToQueue(track);
        });

        right.add(timeLabel);
        right.add(queueBtn);

        card.add(playBtn, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);

        // Hover
        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBackground(UIConstants.BG_HOVER); }
            public void mouseExited(MouseEvent e) { card.setBackground(UIConstants.BG_SURFACE); }
        });

        playBtn.addActionListener(e -> {
            if (queueManager != null) queueManager.playNow(track);
            else TrackPlayerUtil.playTrack(track, player, playerBar);
        });

        return card;
    }

    private void clearHistory() {
        new Thread(() -> {
            try {
                ApiClient.delete("/history");
                SwingUtilities.invokeLater(() -> loadHistory());
            } catch (Exception e) {
                System.err.println("[HistoryPanel] Clear error: " + e.getMessage());
            }
        }).start();
    }
}
