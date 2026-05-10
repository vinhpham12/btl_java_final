/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.frontend.util;

/**
 *
 * @author ADMIN
 */
import java.awt.*;

/**
 * Quản lý các hằng số UI (Màu sắc, Font chữ, Kích thước) cho toàn bộ ứng dụng.
 * 
 * Theme TAH: Lấy cảm hứng từ những bông hoa nhảy múa trong gió.
 * Màu chủ đạo: Xanh bầu trời nhạt (Sky blue) + Vàng cát ấm (Sand yellow).
 * Nền: Màu chàm tối / hoàng hôn (Deep twilight).
 */
public class UIConstants {

    // === BẢNG MÀU NỀN (Tối, sang trọng) ===
    public static final Color BG_DARK = new Color(0x14, 0x17, 0x1F);          // Nền chính (Hoàng hôn tối)
    public static final Color BG_SURFACE = new Color(0x1C, 0x20, 0x2B);      // Nền nổi (Chàm tối)
    public static final Color BG_CARD = new Color(0x25, 0x2A, 0x37);         // Nền Card/Item
    public static final Color BG_HOVER = new Color(0x2E, 0x34, 0x44);        // Nền khi di chuột qua
    public static final Color BG_INPUT = new Color(0x22, 0x27, 0x33);        // Nền ô nhập liệu

    // === BẢNG MÀU CHỦ ĐẠO (Xanh bầu trời) ===
    public static final Color PRIMARY = new Color(0x7E, 0xC8, 0xE3);         // Xanh trời mềm mại
    public static final Color PRIMARY_HOVER = new Color(0xA5, 0xD8, 0xF0);   // Xanh trời sáng (khi hover)
    public static final Color PRIMARY_DARK = new Color(0x5B, 0xA8, 0xC8);    // Xanh trời đậm (khi nhấn)

    // === BẢNG MÀU NHẤN (Vàng cát ấm) ===
    public static final Color ACCENT = new Color(0xD4, 0xA7, 0x6A);          // Vàng cát
    public static final Color ACCENT_LIGHT = new Color(0xE8, 0xC8, 0x7A);    // Vàng cát sáng
    public static final Color ACCENT_SOFT = new Color(0xF0, 0xD9, 0xA8);     // Màu lúa mì mềm

    // === BẢNG MÀU CHỮ ===
    public static final Color TEXT_PRIMARY = new Color(0xF0, 0xEE, 0xE8);     // Trắng ấm (dễ đọc trên nền tối)
    public static final Color TEXT_SECONDARY = new Color(0x9A, 0xA0, 0xB0);   // Xanh xám nhạt (phụ đề)
    public static final Color TEXT_MUTED = new Color(0x5E, 0x65, 0x78);       // Xám chìm (text vô hiệu hóa)

    // === MÀU VIỀN & TRẠNG THÁI ===
    public static final Color BORDER = new Color(0x33, 0x38, 0x48);          // Viền mỏng
    public static final Color DIVIDER = new Color(0x2A, 0x2F, 0x3E);         // Đường chia cắt

    public static final Color SUCCESS = new Color(0x6B, 0xC9, 0x8A);         // Xanh lá (thành công)
    public static final Color ERROR = new Color(0xE0, 0x6B, 0x6B);           // Đỏ san hô (lỗi)
    public static final Color LIKE_RED = new Color(0xE8, 0x8A, 0x7A);        // Cam đào ấm (nút Like trái tim)

    // === MÀU WAVEFORM (Sóng âm) ===
    public static final Color WAVEFORM_PLAYED = new Color(0x7E, 0xC8, 0xE3); // Đã phát (Xanh trời)
    public static final Color WAVEFORM_UNPLAYED = new Color(0x3A, 0x40, 0x50);// Chưa phát (Xám tối)
    public static final Color WAVEFORM_BG = new Color(0x18, 0x1C, 0x26);     // Nền waveform

    // === MÀU GRADIENT (Hiệu ứng chuyển màu Logo) ===
    public static final Color GRADIENT_START = new Color(0x7E, 0xC8, 0xE3);  // Từ Xanh trời...
    public static final Color GRADIENT_END = new Color(0xD4, 0xA7, 0x6A);    // ...sang Vàng cát

    // === FONT CHỮ (Dùng font hệ thống Segoe UI hiện đại) ===
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_TINY = new Font("Segoe UI", Font.PLAIN, 10);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_ICON = new Font("Segoe UI Symbol", Font.PLAIN, 18); // Font hỗ trợ icon Unicode

    // === KÍCH THƯỚC BỐ CỤC (Layout Dimensions) ===
    public static final int SIDEBAR_WIDTH = 200;       // Chiều rộng menu trái
    public static final int PLAYER_BAR_HEIGHT = 80;    // Chiều cao thanh phát nhạc dưới cùng
    public static final int TOP_BAR_HEIGHT = 56;       // Chiều cao thanh công cụ trên cùng
    public static final int CARD_HEIGHT = 200;         // Chiều cao thẻ bài hát
    public static final int CARD_WIDTH = 180;          // Chiều rộng thẻ bài hát
    public static final int BORDER_RADIUS = 8;         // Độ bo góc chung
    public static final int PADDING = 16;              // Khoảng cách lề chuẩn
    public static final int PADDING_SMALL = 8;         // Khoảng cách lề nhỏ

    // === CẤU HÌNH API ===
    public static final String API_BASE_URL = "http://localhost:8081/api";

    /**
     * Tiện ích: Định dạng số giây thành chuỗi phút:giây (VD: 65 -> "1:05")
     */
    public static String formatDuration(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%d:%02d", min, sec);
    }

    /**
     * Tiện ích: Tạo viền bo góc dùng cho các component Swing.
     * Swing mặc định khó làm viền bo góc, nên ta dùng CompoundBorder (kết hợp viền vẽ tay và khoảng trống).
     */
    public static javax.swing.border.Border createRoundedBorder(Color color) {
        return javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(color, 1, true),
            javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12)
        );
    }
}
