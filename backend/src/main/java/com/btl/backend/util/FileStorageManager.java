/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.util;

/**
 *
 * @author ADMIN
 */
import java.io.*;
import java.nio.file.*;

/**
 * Quản lý lưu trữ file trên đĩa cứng.
 *
 * Cấu trúc thư mục:
 *   uploads/
 *   ├── tracks/     ← File nhạc WAV
 *   ├── covers/     ← Ảnh bìa bài hát
 *   └── avatars/    ← Ảnh đại diện user
 *
 * Tên file được thêm timestamp để tránh trùng lặp.
 * Ví dụ: "1714700000000_my_song.wav"
 */
public class FileStorageManager {

    // === CÁC THƯ MỤC LƯU TRỮ ===
    private static final String BASE_DIR = "uploads";           // Thư mục gốc
    private static final String TRACKS_DIR = BASE_DIR + "/tracks";   // File nhạc
    private static final String COVERS_DIR = BASE_DIR + "/covers";   // Ảnh bìa
    private static final String AVATARS_DIR = BASE_DIR + "/avatars"; // Avatar

    /** Tạo các thư mục nếu chưa tồn tại - gọi 1 lần khi server khởi động */
    public static void initialize() {
        try {
            Files.createDirectories(Paths.get(TRACKS_DIR));  // Tạo cả thư mục cha
            Files.createDirectories(Paths.get(COVERS_DIR));
            Files.createDirectories(Paths.get(AVATARS_DIR));
            System.out.println("[Storage] Directories initialized");
        } catch (IOException e) {
            System.err.println("[Storage] Error creating directories: " + e.getMessage());
        }
    }

    /**
     * Lưu file nhạc WAV.
     * @param data      Dữ liệu file dạng byte array
     * @param originalFilename  Tên file gốc (ví dụ: "song.wav")
     * @return Đường dẫn file đã lưu (ví dụ: "uploads/tracks/1714700000_song.wav")
     */
    public static String saveTrack(byte[] data, String originalFilename) throws IOException {
        // Thêm timestamp vào tên file để đảm bảo duy nhất
        String filename = System.currentTimeMillis() + "_" + sanitize(originalFilename);
        Path path = Paths.get(TRACKS_DIR, filename);
        Files.write(path, data);                    // Ghi byte array ra file
        return path.toString().replace("\\", "/");   // Chuẩn hóa đường dẫn (Windows đùng \\)
    }

    public static String saveCover(byte[] data, String originalFilename) throws IOException {
        String filename = System.currentTimeMillis() + "_" + sanitize(originalFilename);
        Path path = Paths.get(COVERS_DIR, filename);
        Files.write(path, data);
        return path.toString().replace("\\", "/");
    }

    public static String saveAvatar(byte[] data, String originalFilename) throws IOException {
        String filename = System.currentTimeMillis() + "_" + sanitize(originalFilename);
        Path path = Paths.get(AVATARS_DIR, filename);
        Files.write(path, data);
        return path.toString().replace("\\", "/");
    }

    /** Đọc file thành byte array - dùng khi stream nhạc cho client */
    public static byte[] readFile(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    public static void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            System.err.println("[Storage] Error deleting file: " + e.getMessage());
        }
    }

    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Loại bỏ ký tự đặc biệt khỏi tên file (phòng chống path traversal attack).
     * Chỉ giữ lại: chữ cái, số, dấu chấm, gạch ngang, gạch dưới.
     */
    private static String sanitize(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

