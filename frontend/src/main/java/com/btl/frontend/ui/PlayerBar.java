/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.frontend.ui;

/**
 *
 * @author ADMIN
 */

import com.btl.frontend.audio.AudioPlayer;
import com.btl.frontend.audio.QueueManager;
import com.btl.frontend.util.UIConstants;
import com.btl.frontend.util.IconFactory;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Bottom player bar - fixed at the bottom of the window.
 * Shows: track info, play controls, progress slider, volume.
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

        // Prev button
        JButton prevBtn = IconFactory.iconButton(IconFactory.prevIcon(24, UIConstants.TEXT_PRIMARY));
        prevBtn.setPreferredSize(new Dimension(36, 36));
        prevBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { prevBtn.setIcon(IconFactory.prevIcon(24, UIConstants.PRIMARY)); }
            public void mouseExited(MouseEvent e) { prevBtn.setIcon(IconFactory.prevIcon(24, UIConstants.TEXT_PRIMARY)); }
        });

        // Play/Pause button
        playPauseBtn = IconFactory.iconButton(IconFactory.playIcon(32, UIConstants.TEXT_PRIMARY));
        playPauseBtn.setPreferredSize(new Dimension(44, 44));

        // Next button
        JButton nextBtn = IconFactory.iconButton(IconFactory.nextIcon(24, UIConstants.TEXT_PRIMARY));
        nextBtn.setPreferredSize(new Dimension(36, 36));
        nextBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { nextBtn.setIcon(IconFactory.nextIcon(24, UIConstants.PRIMARY)); }
            public void mouseExited(MouseEvent e) { nextBtn.setIcon(IconFactory.nextIcon(24, UIConstants.TEXT_PRIMARY)); }
        });

        playPauseBtn.addActionListener(e -> player.togglePlayPause());
        prevBtn.addActionListener(e -> { if (queueManager != null) queueManager.playPrevious(); });
        nextBtn.addActionListener(e -> { if (queueManager != null) queueManager.playNext(); });

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
                });
            }
        });
    }

    public void setTrackInfo(String title, String artist) {
        this.currentTrackTitle = title;
        this.currentTrackArtist = artist;
        trackTitle.setText(title);
        trackArtist.setText(artist);
        totalTimeLabel.setText(UIConstants.formatDuration(player.getTotalSeconds()));
    }

    /** Kết nối QueueManager để Prev/Next hoạt động */
    public void setQueueManager(QueueManager qm) { this.queueManager = qm; }



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

