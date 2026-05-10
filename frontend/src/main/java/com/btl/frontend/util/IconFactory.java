package com.btl.frontend.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Factory tạo các Icon đồ họa vector (vẽ bằng Graphics2D).
 * Không dùng file ảnh, không dùng Unicode - vẽ trực tiếp bằng code.
 */
public class IconFactory {

    // ======== PLAY(tam giác phải) ========
    public static Icon playIcon(int size, Color color) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                int[] xp = {x + size / 4, x + size / 4, x + size * 3 / 4};
                int[] yp = {y + size / 6, y + size * 5 / 6, y + size / 2};
                g2.fillPolygon(xp, yp, 3);
                g2.dispose();
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    // ======== PAUSE||(2 thanh dọc) ========
    public static Icon pauseIcon(int size, Color color) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                int bw = size / 5; // bar width
                int bh = size * 3 / 5; // bar height
                int by = y + size / 5;
                g2.fillRoundRect(x + size / 4 - bw / 2, by, bw, bh, 2, 2);
                g2.fillRoundRect(x + size * 3 / 4 - bw / 2, by, bw, bh, 2, 2);
                g2.dispose();
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    // ======== PREVIOUS(thanh + tam giác trái) ========
    public static Icon prevIcon(int size, Color color) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                int cy = y + size / 2;
                // Thanh dọc bên trái
                g2.fillRect(x + size / 5, y + size / 4, 2, size / 2);
                // Tam giác trái
                int[] xp = {x + size * 4 / 5, x + size * 4 / 5, x + size / 5 + 3};
                int[] yp = {y + size / 4, y + size * 3 / 4, cy};
                g2.fillPolygon(xp, yp, 3);
                g2.dispose();
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    // ======== NEXT(tam giác phải + thanh) ========
    public static Icon nextIcon(int size, Color color) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                int cy = y + size / 2;
                // Tam giác phải
                int[] xp = {x + size / 5, x + size / 5, x + size * 4 / 5 - 3};
                int[] yp = {y + size / 4, y + size * 3 / 4, cy};
                g2.fillPolygon(xp, yp, 3);
                // Thanh dọc bên phải
                g2.fillRect(x + size * 4 / 5 - 2, y + size / 4, 2, size / 2);
                g2.dispose();
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    // ======== HEART (trái tim - filled hoặc outline) ========
    public static Icon heartIcon(int size, Color color, boolean filled) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                Path2D heart = new Path2D.Double();
                double cx = x + size / 2.0;
                double top = y + size * 0.3;
                double bot = y + size * 0.85;
                double left = x + size * 0.12;
                double right = x + size * 0.88;
                heart.moveTo(cx, bot);
                heart.curveTo(left - size * 0.1, y + size * 0.55, left, y + size * 0.1, cx, top);
                heart.moveTo(cx, bot);
                heart.curveTo(right + size * 0.1, y + size * 0.55, right, y + size * 0.1, cx, top);
                if (filled) {
                    g2.fill(heart);
                } else {
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.draw(heart);
                }
                g2.dispose();
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    // ======== VOLUME (loa) ========
    public static Icon volumeIcon(int size, Color color) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                // Thân loa (hình chữ nhật nhỏ)
                int bx = x + size / 5;
                int by = y + size * 2 / 5;
                int bw = size / 5;
                int bh = size / 5;
                g2.fillRect(bx, by, bw, bh);
                // Phễu loa (tam giác)
                int[] xp = {bx + bw, bx + bw, x + size * 3 / 5};
                int[] yp = {by, by + bh, y + size / 5};
                g2.fillPolygon(new int[]{bx + bw, x + size * 3 / 5, x + size * 3 / 5, bx + bw},
                               new int[]{by, y + size / 5, y + size * 4 / 5, by + bh}, 4);
                // Sóng âm
                g2.setStroke(new BasicStroke(1.2f));
                int cx = x + size * 3 / 5;
                int cy = y + size / 2;
                g2.drawArc(cx - size / 8, cy - size / 6, size / 4, size / 3, -45, 90);
                g2.drawArc(cx - size / 12, cy - size / 8, size / 5, size / 4, -45, 90);
                g2.dispose();
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    // ======== PLUS(thêm vào playlist) ========
    public static Icon plusIcon(int size, Color color) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = x + size / 2;
                int cy = y + size / 2;
                int r = size / 3;
                g2.drawLine(cx - r, cy, cx + r, cy);     // ngang
                g2.drawLine(cx, cy - r, cx, cy + r);     // dọc
                g2.dispose();
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    // ======== BACK(mũi tên trái) ========
    public static Icon backIcon(int size, Color color) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cy = y + size / 2;
                // Thân mũi tên
                g2.drawLine(x + size / 5, cy, x + size * 4 / 5, cy);
                // Đầu mũi tên
                g2.drawLine(x + size / 5, cy, x + size * 2 / 5, cy - size / 4);
                g2.drawLine(x + size / 5, cy, x + size * 2 / 5, cy + size / 4);
                g2.dispose();
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    // ======== CLOSE (xóa/đóng) ========
    public static Icon closeIcon(int size, Color color) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int p = size / 4;
                g2.drawLine(x + p, y + p, x + size - p, y + size - p);
                g2.drawLine(x + size - p, y + p, x + p, y + size - p);
                g2.dispose();
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    // ======== MUSIC NOTE (nốt nhạc cho playlist) ========
    public static Icon musicNoteIcon(int size, Color color) {
        return new Icon() {
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                // 2 nốt nhạc nối nhau
                int noteR = size / 5;
                // Nốt trái
                int lx = x + size / 4;
                int ly = y + size * 3 / 4;
                g2.fillOval(lx - noteR, ly - noteR / 2, noteR * 2, noteR);
                // Nốt phải
                int rx = x + size * 3 / 4;
                int ry = y + size * 5 / 8;
                g2.fillOval(rx - noteR, ry - noteR / 2, noteR * 2, noteR);
                // Thanh dọc
                g2.setStroke(new BasicStroke(2f));
                g2.drawLine(lx + noteR, ly - noteR / 2, lx + noteR, y + size / 5);
                g2.drawLine(rx + noteR, ry - noteR / 2, rx + noteR, y + size / 8);
                // Thanh ngang nối
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(lx + noteR, y + size / 5, rx + noteR, y + size / 8);
                g2.dispose();
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    // ======== Tạo JButton với Icon (không có text) ========
    public static JButton iconButton(Icon icon) {
        JButton btn = new JButton(icon);
        btn.setText("");
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ======== Tạo JButton play đặc biệt (hình tròn màu primary) ========
    public static JButton circlePlayButton(int size, Color bgColor, Color fgColor) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillOval(0, 0, getWidth(), getHeight());
                // Vẽ tam giác play
                g2.setColor(fgColor);
                int w = getWidth();
                int h = getHeight();
                int[] xp = {w / 3 + 2, w / 3 + 2, w * 2 / 3 + 2};
                int[] yp = {h / 4, h * 3 / 4, h / 2};
                g2.fillPolygon(xp, yp, 3);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(size, size));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
