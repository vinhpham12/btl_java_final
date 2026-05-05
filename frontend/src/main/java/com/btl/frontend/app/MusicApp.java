/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.frontend.app;

/**
 *
 * @author ADMIN
 */
import com.btl.frontend.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

/**
 * Điểm khởi đầu (Entry point) của ứng dụng Frontend (TAH Music Player).
 * Nơi chứa hàm main() khởi chạy UI.
 */
public class MusicApp {

    public static void main(String[] args) {
        // Cấu hình UIManager: Thay đổi giao diện mặc định của Java Swing
        try {
            // Sử dụng CrossPlatformLookAndFeel để giao diện giống nhau trên Windows/Mac/Linux
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

            // === ÁP DỤNG THEME TAH (Twilight Palette) ===
            Color bgDark = new Color(0x14, 0x17, 0x1F);
            Color bgSurface = new Color(0x1C, 0x20, 0x2B);
            Color bgInput = new Color(0x22, 0x27, 0x33);
            Color textWarm = new Color(0xF0, 0xEE, 0xE8);

            // Ghi đè màu nền mặc định của các thành phần Swing cơ bản (Panel, Text, Button...)
            // Tránh tình trạng chớp nháy màu trắng hoặc viền trắng khó chịu
            UIManager.put("Panel.background", bgDark);
            UIManager.put("OptionPane.background", bgSurface);
            UIManager.put("OptionPane.messageForeground", textWarm);
            UIManager.put("TextField.background", bgInput);
            UIManager.put("TextField.foreground", textWarm);
            UIManager.put("TextField.caretForeground", textWarm);
            UIManager.put("TextArea.background", bgInput);
            UIManager.put("TextArea.foreground", textWarm);
            UIManager.put("Button.background", new Color(0x2E, 0x34, 0x44));
            UIManager.put("Button.foreground", textWarm);
            UIManager.put("ComboBox.background", bgInput);
            UIManager.put("ComboBox.foreground", textWarm);
            UIManager.put("ScrollPane.background", bgDark);
            UIManager.put("ScrollBar.background", bgDark);
            UIManager.put("ScrollBar.thumb", new Color(0x3A, 0x40, 0x50));
            UIManager.put("ScrollBar.track", bgSurface);
            UIManager.put("List.background", bgSurface);
            UIManager.put("List.foreground", textWarm);
            UIManager.put("FileChooser.background", bgSurface);

        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }

        // Khởi chạy giao diện người dùng trên luồng Event Dispatch Thread (EDT)
        // Bắt buộc đối với Swing để tránh lỗi đồng bộ (Concurrency)
        SwingUtilities.invokeLater(() -> {
            System.out.println("=== TAH Music Player ===");
            System.out.println("Starting application...");
            System.out.println("Make sure the backend server is running on http://localhost:8081");

            // Tạo và hiển thị cửa sổ chính
            MainFrame frame = new MainFrame();
            frame.setVisible(true);

            System.out.println("Application started successfully!");
        });
    }
}