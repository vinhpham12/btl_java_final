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
import com.btl.frontend.util.UIConstants;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Waveform visualization panel.
 * Shows audio waveform, current playback position, click-to-seek.
 */
public class WaveformPanel extends JPanel {

    private float[] waveformData;
    private final AudioPlayer player;
    private double playbackProgress = 0.0;

    public WaveformPanel(AudioPlayer player) {
        this.player = player;
        setBackground(UIConstants.WAVEFORM_BG);
        setPreferredSize(new Dimension(600, 80));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (waveformData != null && waveformData.length > 0 && player.getTotalSeconds() > 0) {
                    double ratio = (double) e.getX() / getWidth();
                    int seekTo = (int) (ratio * player.getTotalSeconds());
                    player.seek(seekTo);
                }
            }
        });

        // Position update timer
        Timer timer = new Timer(100, e -> {
            if (player.getTotalSeconds() > 0) {
                playbackProgress = (double) player.getCurrentSeconds() / player.getTotalSeconds();
                repaint();
            }
        });
        timer.start();
    }

    public void setWaveformData(float[] data) {
        this.waveformData = data;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (waveformData == null || waveformData.length == 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int centerY = h / 2;
        int barWidth = Math.max(2, w / waveformData.length);
        int gap = 1;
        int playedX = (int) (playbackProgress * w);

        for (int i = 0; i < waveformData.length; i++) {
            int x = (int) ((double) i / waveformData.length * w);
            int barH = Math.max(2, (int) (waveformData[i] * (h - 8)));

            if (x < playedX) {
                // Played portion - gradient orange
                g2.setColor(UIConstants.WAVEFORM_PLAYED);
            } else {
                g2.setColor(UIConstants.WAVEFORM_UNPLAYED);
            }

            // Draw mirrored bars
            int halfBar = barH / 2;
            g2.fillRoundRect(x, centerY - halfBar, Math.max(barWidth - gap, 1), barH, 2, 2);
        }

        // Playhead line
        if (playbackProgress > 0) {
            g2.setColor(Color.WHITE);
            g2.fillRect(playedX - 1, 0, 2, h);
        }

        g2.dispose();
    }
}

