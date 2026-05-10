/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author ADMIN
 */
/**
 * Quản lý phiên đăng nhập bằng Token.
 *
 * Cách hoạt động:
 * 1. User đăng nhập thành công → tạo token ngẫu nhiên → lưu vào map (token → userId)
 * 2. Mỗi request sau đó, client gửi token trong header Authorization
 * 3. Server kiểm tra token trong map → biết được userId
 * 4. User đăng xuất → xóa token khỏi map
 *
 * ConcurrentHashMap: cho phép nhiều thread đọc/ghi đồng thời mà không bị xung đột.
 */
public class SessionManager {

    // Bảng lưu sessions: key = token (chuỗi ngẫu nhiên), value = userId
    private static final Map<String, Integer> sessions = new ConcurrentHashMap<>();
    // SecureRandom: tạo số ngẫu nhiên an toàn (khó đoán hơn Random thông thường)
    private static final SecureRandom random = new SecureRandom();

    /** Tạo phiên mới khi đăng nhập thành công. Trả về token để client lưu lại. */
    public static String createSession(int userId) {
        String token = generateToken();      // Tạo chuỗi ngẫu nhiên 32 bytes
        sessions.put(token, userId);         // Lưu vào map
        return token;                        // Trả về cho client
    }

    /** Lấy userId từ token. Trả về -1 nếu token không hợp lệ. */
    public static int getUserId(String token) {
        if (token == null) return -1;
        Integer userId = sessions.get(token);
        return userId != null ? userId : -1;
    }

    /** Xóa phiên khi đăng xuất */
    public static void removeSession(String token) {
        if (token != null) sessions.remove(token);
    }

    /** Kiểm tra token có hợp lệ không */
    public static boolean isValid(String token) {
        return token != null && sessions.containsKey(token);
    }

    /**
     * Tạo token ngẫu nhiên 32 bytes, encode thành Base64URL.
     * Ví dụ: "aBcDeFgHiJkLmNoPqRsTuVwXyZ012345678-_"
     */
    private static String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hash mật khẩu bằng SHA-256 + Salt ngẫu nhiên.
     * Format lưu trữ: "salt:hash" (hex)
     * Salt 16 bytes giúp chống rainbow table attack.
     * Ví dụ: "a1b2c3d4...:ef92b778bafe..."
     */
    public static String hashPassword(String password) {
        try {
            // Tạo salt ngẫu nhiên 16 bytes
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            StringBuilder saltHex = new StringBuilder();
            for (byte b : salt) saltHex.append(String.format("%02x", b));

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Hash = SHA-256(salt + password)
            md.update(salt);
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hashHex = new StringBuilder();
            for (byte b : hash) hashHex.append(String.format("%02x", b));

            return saltHex.toString() + ":" + hashHex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Xác thực mật khẩu.
     * Hỗ trợ cả format mới (salt:hash) và format cũ (chỉ hash, không salt)
     * để tương thích ngược với dữ liệu cũ.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            if (storedHash.contains(":")) {
                // Format mới: "salt:hash"
                String[] parts = storedHash.split(":", 2);
                String saltHex = parts[0];
                String expectedHash = parts[1];

                // Chuyển salt hex -> bytes
                byte[] salt = new byte[saltHex.length() / 2];
                for (int i = 0; i < salt.length; i++) {
                    salt[i] = (byte) Integer.parseInt(saltHex.substring(i * 2, i * 2 + 2), 16);
                }

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(salt);
                byte[] hash = md.digest(password.getBytes("UTF-8"));
                StringBuilder hashHex = new StringBuilder();
                for (byte b : hash) hashHex.append(String.format("%02x", b));

                return hashHex.toString().equals(expectedHash);
            } else {
                // Format cũ: hash không salt (tương thích ngược)
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(password.getBytes("UTF-8"));
                StringBuilder sb = new StringBuilder();
                for (byte b : hash) sb.append(String.format("%02x", b));
                return sb.toString().equals(storedHash);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error verifying password", e);
        }
    }
}
