package com.btl.frontend.ui;

import com.btl.frontend.api.ApiClient;
import com.btl.frontend.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Panel thong bao (notifications).
 */
public class NotificationPanel extends JPanel {

    public interface NotificationListener {
        void onUserClicked(int userId);
    }

    private final NotificationListener listener;
    private JPanel notifsContainer;
    private JLabel statusLabel;

    public NotificationPanel(NotificationListener listener) {
        this.listener = listener;
        setBackground(UIConstants.BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIConstants.BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel header = new JLabel("Notifications");
        header.setFont(UIConstants.FONT_TITLE);
        header.setForeground(UIConstants.TEXT_PRIMARY);

        JButton markAllBtn = new JButton("Mark All Read");
        markAllBtn.setFont(UIConstants.FONT_BUTTON);
        markAllBtn.setForeground(UIConstants.PRIMARY);
        markAllBtn.setBorderPainted(false);
        markAllBtn.setContentAreaFilled(false);
        markAllBtn.setFocusPainted(false);
        markAllBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        markAllBtn.addActionListener(e -> markAllAsRead());

        headerRow.add(header, BorderLayout.WEST);
        headerRow.add(markAllBtn, BorderLayout.EAST);
        content.add(headerRow);
        content.add(Box.createVerticalStrut(20));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UIConstants.FONT_BODY);
        statusLabel.setForeground(UIConstants.TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(statusLabel);
        content.add(Box.createVerticalStrut(8));

        notifsContainer = new JPanel();
        notifsContainer.setLayout(new BoxLayout(notifsContainer, BoxLayout.Y_AXIS));
        notifsContainer.setOpaque(false);
        notifsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(notifsContainer);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setBackground(UIConstants.BG_DARK);
        add(scroll, BorderLayout.CENTER);
    }

    public void loadNotifications() {
        statusLabel.setText("Loading...");
        notifsContainer.removeAll();
        notifsContainer.revalidate();

        new Thread(() -> {
            try {
                Map<String, Object> response = ApiClient.get("/notifications");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> notifs = (List<Map<String, Object>>) response.get("data");
                if (notifs == null) notifs = new ArrayList<>();
                final List<Map<String, Object>> fn = notifs;
                SwingUtilities.invokeLater(() -> {
                    notifsContainer.removeAll();
                    if (fn.isEmpty()) {
                        statusLabel.setText("No notifications yet");
                    } else {
                        statusLabel.setText(fn.size() + " notifications");
                        for (Map<String, Object> n : fn) {
                            notifsContainer.add(createNotifCard(n));
                            notifsContainer.add(Box.createVerticalStrut(4));
                        }
                    }
                    notifsContainer.revalidate();
                    notifsContainer.repaint();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Failed to load"));
            }
        }).start();
    }

    private JPanel createNotifCard(Map<String, Object> notif) {
        boolean isRead = JsonHelper.getBoolean(notif, "isRead");
        String type = JsonHelper.getString(notif, "type", "");
        String fromName = JsonHelper.getString(notif, "fromName", "Someone");
        String message = JsonHelper.getString(notif, "message", "");
        String createdAt = JsonHelper.getString(notif, "createdAt", "");
        int fromUserId = JsonHelper.getInt(notif, "fromUserId");
        int notifId = JsonHelper.getInt(notif, "id");
        if (createdAt.length() > 16) createdAt = createdAt.substring(0, 16);

        JPanel card = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBackground(isRead ? UIConstants.BG_SURFACE : UIConstants.BG_CARD);
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Icon icon = getNotifIcon(type);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setPreferredSize(new Dimension(36, 36));

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLabel = new JLabel(fromName + " " + message);
        nameLabel.setFont(isRead ? UIConstants.FONT_BODY : UIConstants.FONT_HEADING);
        nameLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel timeLabel = new JLabel(createdAt);
        timeLabel.setFont(UIConstants.FONT_TINY);
        timeLabel.setForeground(UIConstants.TEXT_MUTED);

        info.add(nameLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(timeLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        if (!isRead) {
            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UIConstants.PRIMARY);
                    g2.fillOval(0, 0, 8, 8);
                    g2.dispose();
                }
            };
            dot.setPreferredSize(new Dimension(8, 8));
            dot.setOpaque(false);
            rightPanel.add(dot);
        }

        card.add(iconLabel, BorderLayout.WEST);
        card.add(info, BorderLayout.CENTER);
        card.add(rightPanel, BorderLayout.EAST);

        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.setBackground(UIConstants.BG_HOVER); }
            public void mouseExited(MouseEvent e) { card.setBackground(isRead ? UIConstants.BG_SURFACE : UIConstants.BG_CARD); }
            public void mouseClicked(MouseEvent e) {
                if (!isRead) {
                    new Thread(() -> { try { ApiClient.put("/notifications/" + notifId + "/read", new HashMap<>()); } catch (Exception ex) {} }).start();
                }
                if (fromUserId > 0 && listener != null) listener.onUserClicked(fromUserId);
            }
        });
        return card;
    }

    private Icon getNotifIcon(String type) {
        if ("like".equals(type)) return IconFactory.heartIcon(18, UIConstants.LIKE_RED, true);
        if ("comment".equals(type)) return IconFactory.musicNoteIcon(18, UIConstants.PRIMARY);
        if ("follow".equals(type)) return IconFactory.plusIcon(18, UIConstants.SUCCESS);
        if ("repost".equals(type)) return IconFactory.repostIcon(18, UIConstants.ACCENT);
        return IconFactory.bellIcon(18, UIConstants.TEXT_SECONDARY);
    }

    private void markAllAsRead() {
        new Thread(() -> {
            try {
                ApiClient.put("/notifications/read-all", new HashMap<>());
                SwingUtilities.invokeLater(this::loadNotifications);
            } catch (Exception e) {
                System.err.println("[NotificationPanel] error: " + e.getMessage());
            }
        }).start();
    }

    public void fetchUnreadCount(java.util.function.Consumer<Integer> callback) {
        new Thread(() -> {
            try {
                Map<String, Object> resp = ApiClient.get("/notifications/unread-count");
                Map<String, Object> data = JsonHelper.getMap(resp, "data");
                int count = JsonHelper.getInt(data, "count");
                SwingUtilities.invokeLater(() -> callback.accept(count));
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> callback.accept(0));
            }
        }).start();
    }
}
