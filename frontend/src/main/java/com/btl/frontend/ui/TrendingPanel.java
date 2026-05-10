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
 * Panel bang xep hang trending.
 * Hien thi top bai hat theo tuan / thang / tat ca.
 */
public class TrendingPanel extends JPanel {

    public interface TrendingListener {
        void onTrackSelected(Map<String, Object> track);
    }

    private final AudioPlayer player;
    private final PlayerBar playerBar;
    private final TrendingListener listener;
    private QueueManager queueManager;
    private JPanel tracksContainer;
    private JLabel statusLabel;

    public TrendingPanel(AudioPlayer player, PlayerBar playerBar, TrendingListener listener) {
        this.player = player;
        this.playerBar = playerBar;
        this.listener = listener;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    public void setQueueManager(QueueManager qm) {
        this.queueManager = qm;
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIConstants.BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel header = new JLabel("Trending Charts");
        header.setFont(UIConstants.FONT_TITLE);
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(header);
        content.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Most popular tracks right now");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(sub);
        content.add(Box.createVerticalStrut(24));

        // Period filter buttons
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterBar.setOpaque(false);
        filterBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        String[] labels = { "This Week", "This Month", "All Time" };
        String[] values = { "week", "month", "all" };
        for (int i = 0; i < labels.length; i++) {
            final String val = values[i];
            JButton btn = createPillButton(labels[i], i == 0);
            btn.addActionListener(e -> {
                for (Component c : filterBar.getComponents()) {
                    if (c instanceof JButton) {
                        c.setForeground(UIConstants.TEXT_SECONDARY);
                        c.setBackground(UIConstants.BG_CARD);
                    }
                }
                btn.setForeground(Color.WHITE);
                btn.setBackground(UIConstants.PRIMARY);
                loadTrending(val);
            });
            filterBar.add(btn);
        }
        content.add(filterBar);
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

    public void loadTrending(String period) {
        statusLabel.setText("Loading trending...");
        tracksContainer.removeAll();
        tracksContainer.revalidate();

        new Thread(() -> {
            try {
                Map<String, Object> response = ApiClient.get("/tracks/trending?period=" + period + "&limit=30");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tracks = (List<Map<String, Object>>) response.get("data");
                if (tracks == null)
                    tracks = new ArrayList<>();
                final List<Map<String, Object>> ft = tracks;

                SwingUtilities.invokeLater(() -> {
                    tracksContainer.removeAll();
                    if (ft.isEmpty()) {
                        statusLabel.setText("No trending tracks yet");
                    } else {
                        statusLabel.setText("Top " + ft.size() + " tracks");
                        for (int i = 0; i < ft.size(); i++) {
                            tracksContainer.add(createRankedCard(i + 1, ft.get(i)));
                            tracksContainer.add(Box.createVerticalStrut(2));
                        }
                    }
                    tracksContainer.revalidate();
                    tracksContainer.repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Failed to load trending"));
            }
        }).start();
    }

    private JPanel createRankedCard(int rank, Map<String, Object> track) {
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

        // Rank number with special colors for top 3
        JLabel rankLabel = new JLabel("#" + rank);
        rankLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        rankLabel.setPreferredSize(new Dimension(44, 44));
        rankLabel.setHorizontalAlignment(SwingConstants.CENTER);
        if (rank == 1)
            rankLabel.setForeground(UIConstants.ACCENT);
        else if (rank == 2)
            rankLabel.setForeground(UIConstants.ACCENT_SOFT);
        else if (rank == 3)
            rankLabel.setForeground(new Color(0xCD, 0x7F, 0x32));
        else
            rankLabel.setForeground(UIConstants.TEXT_MUTED);

        // Play button
        JButton playBtn = IconFactory.iconButton(IconFactory.playIcon(18, UIConstants.PRIMARY));
        playBtn.setPreferredSize(new Dimension(36, 36));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(rankLabel);
        leftPanel.add(playBtn);

        // Info
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        String title = JsonHelper.getString(track, "title", "Untitled");
        String artist = JsonHelper.getString(track, "artist", JsonHelper.getString(track, "uploaderName", "Unknown"));
        int plays = JsonHelper.getInt(track, "playCount");
        int likes = JsonHelper.getInt(track, "likeCount");
        int duration = JsonHelper.getInt(track, "durationSeconds");

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.FONT_HEADING);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel detailLabel = new JLabel(artist + " · " + UIConstants.formatDuration(duration));
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

        JButton queueBtn = IconFactory.iconButton(IconFactory.queueIcon(14, UIConstants.TEXT_MUTED));
        queueBtn.setPreferredSize(new Dimension(28, 28));
        queueBtn.setToolTipText("Add to queue");
        queueBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent ev) {
                queueBtn.setIcon(IconFactory.queueIcon(14, UIConstants.ACCENT));
            }

            public void mouseExited(MouseEvent ev) {
                queueBtn.setIcon(IconFactory.queueIcon(14, UIConstants.TEXT_MUTED));
            }
        });
        queueBtn.addActionListener(e -> {
            if (queueManager != null)
                queueManager.addToQueue(track);
        });

        stats.add(playsLabel);
        stats.add(likesLabel);
        stats.add(queueBtn);

        card.add(leftPanel, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(stats, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBackground(UIConstants.BG_HOVER);
            }

            public void mouseExited(MouseEvent e) {
                card.setBackground(UIConstants.BG_SURFACE);
            }

            public void mouseClicked(MouseEvent e) {
                if (listener != null)
                    listener.onTrackSelected(track);
            }
        });

        playBtn.addActionListener(e -> {
            if (queueManager != null)
                queueManager.playNow(track);
            else
                TrackPlayerUtil.playTrack(track, player, playerBar);
        });

        return card;
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
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setFont(UIConstants.FONT_SMALL);
        btn.setForeground(active ? Color.WHITE : UIConstants.TEXT_SECONDARY);
        btn.setBackground(active ? UIConstants.PRIMARY : UIConstants.BG_CARD);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(100, 30));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
