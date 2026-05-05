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

/**
 * Cửa sổ chính của ứng dụng Frontend.
 * 
 * Bố cục (BorderLayout):
 * - Phía dưới (SOUTH): Thanh PlayerBar (luôn hiển thị để điều khiển nhạc)
 * - Bên trái (WEST): Thanh điều hướng Sidebar (Menu)
 * - Ở giữa (CENTER): Vùng nội dung chính (CardLayout)
 */
public class MainFrame extends JFrame {

    // Các thành phần cốt lõi
    private final AudioPlayer player = new AudioPlayer(); // Engine phát nhạc duy nhất
    private PlayerBar playerBar;                          // Thanh điều khiển phát nhạc
    private CardLayout contentLayout;                     // Quản lý việc chuyển đổi các màn hình
    private JPanel contentPanel;                          // Panel chứa tất cả các màn hình con
    private JPanel sidebar;                               // Thanh menu bên trái
    private Map<String, Object> currentUser;              // Thông tin user đang đăng nhập

    // Panels
    private HomePanel homePanel;
    private TrackPanel trackPanel;
    private UploadPanel uploadPanel;
    private SearchPanel searchPanel;
    private ProfilePanel profilePanel;
    private PlaylistPanel playlistPanel;

    // Active sidebar button tracking
    private JButton activeSidebarBtn = null;

    public MainFrame() {
        setTitle("TAH - Music Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UIConstants.BG_DARK);

        showLogin();
    }

    /** Hiển thị màn hình đăng nhập (Ẩn sidebar và player bar) */
    private void showLogin() {
        getContentPane().removeAll();
        LoginPanel loginPanel = new LoginPanel(userData -> {
            // Callback: Khi đăng nhập thành công, lưu thông tin user và hiển thị UI chính
            this.currentUser = userData;
            showMainUI();
        });
        getContentPane().add(loginPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /** Khởi tạo giao diện chính sau khi đăng nhập thành công */
    private void showMainUI() {
        getContentPane().removeAll();
        getContentPane().setLayout(new BorderLayout());

        // 1. Tạo thanh Player (nằm dưới cùng)
        playerBar = new PlayerBar(player);
        getContentPane().add(playerBar, BorderLayout.SOUTH);

        // 2. Vùng làm việc chính
        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.setBackground(UIConstants.BG_DARK);

        // 3. Thanh Sidebar (nằm bên trái)
        sidebar = createSidebar();
        mainArea.add(sidebar, BorderLayout.WEST);

        // 4. Vùng nội dung dùng CardLayout (như 1 chồng thẻ, mỗi thẻ là 1 màn hình)
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setBackground(UIConstants.BG_DARK);

        // Initialize all panels
        homePanel = new HomePanel(player, playerBar, new HomePanel.HomeListener() {
            @Override
            public void onTrackSelected(Map<String, Object> track) { navigateToTrack(track); }
            @Override
            public void onUserSelected(int userId) { navigateToProfile(userId); }
        });

        trackPanel = new TrackPanel(player, playerBar);
        uploadPanel = new UploadPanel();

        searchPanel = new SearchPanel(player, playerBar, new SearchPanel.SearchListener() {
            @Override
            public void onTrackSelected(Map<String, Object> track) { navigateToTrack(track); }
            @Override
            public void onUserSelected(int userId) { navigateToProfile(userId); }
        });

        profilePanel = new ProfilePanel(player, playerBar, track -> navigateToTrack(track));
        playlistPanel = new PlaylistPanel(player, playerBar);

        // Đăng ký các thẻ (Card) vào hệ thống
        contentPanel.add(homePanel, "home");
        contentPanel.add(trackPanel, "track");
        contentPanel.add(uploadPanel, "upload");
        contentPanel.add(searchPanel, "search");
        contentPanel.add(profilePanel, "profile");
        contentPanel.add(playlistPanel, "playlist");

        mainArea.add(contentPanel, BorderLayout.CENTER);
        getContentPane().add(mainArea, BorderLayout.CENTER);

        revalidate();
        repaint();

        // Tải dữ liệu ban đầu cho trang chủ
        homePanel.loadTracks("newest");
    }

    private JPanel createSidebar() {
        JPanel sb = new JPanel();
        sb.setBackground(UIConstants.BG_SURFACE);
        sb.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIConstants.BORDER));

        // Logo ứng dụng với hiệu ứng Gradient tự vẽ
        JLabel logo = new JLabel("  TAH") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Bật khử răng cưa cho chữ mịn hơn
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                // Tạo dải màu gradient từ Xanh trời sang Vàng cát
                GradientPaint gp = new GradientPaint(0, 0, UIConstants.GRADIENT_START, getWidth(), 0, UIConstants.GRADIENT_END);
                g2.setPaint(gp);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                // Vẽ chữ vào giữa theo chiều dọc
                g2.drawString(getText(), 16, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        logo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logo.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 56));
        logo.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 56));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sb.add(logo);
        sb.add(Box.createVerticalStrut(24));

        // Nav section
        JLabel navLabel = new JLabel("   MENU");
        navLabel.setFont(UIConstants.FONT_TINY);
        navLabel.setForeground(UIConstants.TEXT_MUTED);
        navLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sb.add(navLabel);
        sb.add(Box.createVerticalStrut(8));

        JButton homeBtn = createSidebarButton("Home", true);
        homeBtn.addActionListener(e -> { setActive(homeBtn); showCard("home"); homePanel.loadTracks("newest"); });
        sb.add(homeBtn);

        JButton searchBtn = createSidebarButton("Search", false);
        searchBtn.addActionListener(e -> { setActive(searchBtn); showCard("search"); });
        sb.add(searchBtn);

        sb.add(Box.createVerticalStrut(16));
        JLabel libLabel = new JLabel("   LIBRARY");
        libLabel.setFont(UIConstants.FONT_TINY);
        libLabel.setForeground(UIConstants.TEXT_MUTED);
        libLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sb.add(libLabel);
        sb.add(Box.createVerticalStrut(8));

        JButton uploadBtn = createSidebarButton("Upload", false);
        uploadBtn.addActionListener(e -> { setActive(uploadBtn); showCard("upload"); });
        sb.add(uploadBtn);

        JButton playlistBtn = createSidebarButton("Playlists", false);
        playlistBtn.addActionListener(e -> { setActive(playlistBtn); showCard("playlist"); playlistPanel.loadPlaylists(); });
        sb.add(playlistBtn);

        JButton profileBtn = createSidebarButton("My Profile", false);
        profileBtn.addActionListener(e -> {
            if (currentUser != null) {
                setActive(profileBtn);
                showCard("profile");
                profilePanel.loadProfile(JsonHelper.getInt(currentUser, "id"));
            }
        });
        sb.add(profileBtn);

        sb.add(Box.createVerticalGlue());

        // Logout button at bottom
        JButton logoutBtn = createSidebarButton("Log Out", false);
        logoutBtn.setForeground(UIConstants.TEXT_MUTED);
        logoutBtn.addActionListener(e -> {
            player.stop();
            ApiClient.setAuthToken(null);
            showLogin();
        });
        sb.add(logoutBtn);
        sb.add(Box.createVerticalStrut(16));

        activeSidebarBtn = homeBtn;
        return sb;
    }

    /** Tạo nút menu bên Sidebar với khả năng tự vẽ hiệu ứng Hover/Active */
    private JButton createSidebarButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (this == activeSidebarBtn) {
                    // Trạng thái đang chọn: Nền màu primary trong suốt (alpha 30)
                    g2.setColor(new Color(UIConstants.PRIMARY.getRed(), UIConstants.PRIMARY.getGreen(), UIConstants.PRIMARY.getBlue(), 30));
                    g2.fill(new RoundRectangle2D.Float(8, 0, getWidth() - 16, getHeight(), 8, 8));
                    // Thanh đánh dấu (Left accent) màu primary đậm
                    g2.setColor(UIConstants.PRIMARY);
                    g2.fillRoundRect(8, 4, 3, getHeight() - 8, 3, 3);
                } else if (getModel().isRollover()) {
                    // Trạng thái hover: Nền màu hover nhạt
                    g2.setColor(UIConstants.BG_HOVER);
                    g2.fill(new RoundRectangle2D.Float(8, 0, getWidth() - 16, getHeight(), 8, 8));
                }
                
                // Vẽ chữ
                g2.setColor(this == activeSidebarBtn ? UIConstants.PRIMARY : getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), 24, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setFont(UIConstants.FONT_BODY);
        btn.setForeground(UIConstants.TEXT_SECONDARY);
        btn.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 40));
        btn.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void setActive(JButton btn) {
        activeSidebarBtn = btn;
        sidebar.repaint();
    }

    /** Chuyển thẻ hiển thị trong CardLayout dựa theo tên */
    private void showCard(String name) {
        contentLayout.show(contentPanel, name);
    }

    private void navigateToTrack(Map<String, Object> track) {
        trackPanel.loadTrack(track);
        showCard("track");
    }

    private void navigateToProfile(int userId) {
        profilePanel.loadProfile(userId);
        showCard("profile");
    }
}

