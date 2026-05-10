/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.util;

/**
 *
 * @author ADMIN
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/app_phat_nhac?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "";
    private static final int POOL_SIZE = 10;
    private static BlockingQueue<Connection> connectionPool;
    private static boolean initialized = false;

    public static synchronized void initialize() {
        if (initialized)
            return;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            createDatabaseIfNotExists(); // Đảm bảo database tồn tại trước khi tạo pool
            connectionPool = new ArrayBlockingQueue<>(POOL_SIZE);
            for (int i = 0; i < POOL_SIZE; i++) {
                connectionPool.add(createConnection());
            }
            initialized = true;
            System.out.println("[DB] Connection pool initialized with " + POOL_SIZE + " connections");
            initializedTables();
        } catch (Exception e) {
            System.err.println("[DB] Failed to initialize: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    // tao 1 connect moi den mysql
    public static Connection createConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // tu dong tao database neu chua co
    private static void createDatabaseIfNotExists() {
        String rootUrl = "jdbc:mysql://localhost:3306/?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection conn = DriverManager.getConnection(rootUrl, USER, PASS);
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS app_phat_nhac CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            System.out.println("[DB] Database 'app_phat_nhac' checked/created successfully.");
        } catch (SQLException e) {
            System.err.println("[DB] Failed to create database: " + e.getMessage());
        }
    }

    // muon 1 connection tu pool — kiem tra isValid thay vi chi isClosed
    public static Connection getConnection() {
        try {
            Connection conn = connectionPool.take();
            // isValid(2) kiểm tra connection thực sự hoạt động (gửi ping tới DB, timeout
            // 2s)
            // isClosed() chỉ kiểm tra trạng thái local, không phát hiện được connection bị
            // MySQL đóng
            if (!conn.isValid(2)) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
                conn = createConnection();
            }
            return conn;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get connection", e);
        }
    }

    public static void releaseConnection(Connection conn) {
        if (conn != null) {
            try {
                if (conn.isValid(1)) {
                    connectionPool.offer(conn); // tra lai pool neu con song
                } else {
                    try {
                        conn.close();
                    } catch (SQLException ignored) {
                    }
                    // Tạo connection mới thay thế để pool không bị shrink
                    try {
                        connectionPool.offer(createConnection());
                    } catch (SQLException createErr) {
                        System.err.println("[DB] Failed to create replacement connection: " + createErr.getMessage());
                        // Pool sẽ bị giảm 1 connection, nhưng không crash server
                    }
                }
            } catch (SQLException e) {
                System.err.println("[DB] Error releasing connection: " + e.getMessage());
                // Đảm bảo pool không mất connection
                try {
                    connectionPool.offer(createConnection());
                } catch (SQLException createErr) {
                    System.err.println("[DB] Failed to create replacement connection: " + createErr.getMessage());
                }
            }
        }
    }

    private static void initializedTables() {// tu dong tao cac bang neu thieu trong database
        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {
            // Users
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS users (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(50) UNIQUE NOT NULL,
                            password_hash VARCHAR(255) NOT NULL,
                            display_name VARCHAR(100) NOT NULL,
                            bio TEXT,
                            avatar_path VARCHAR(500),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """);

            // Tracks
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS tracks (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            title VARCHAR(200) NOT NULL,
                            artist VARCHAR(200),
                            genre VARCHAR(50),
                            description TEXT,
                            file_path VARCHAR(500) NOT NULL,
                            cover_path VARCHAR(500),
                            duration_seconds INT DEFAULT 0,
                            play_count INT DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                    """);

            // Playlists
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS playlists (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            name VARCHAR(200) NOT NULL,
                            description TEXT,
                            is_public BOOLEAN DEFAULT TRUE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                    """);

            // Playlist tracks
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS playlist_tracks (
                            playlist_id INT NOT NULL,
                            track_id INT NOT NULL,
                            position INT NOT NULL,
                            added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (playlist_id, track_id),
                            FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
                            FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
                        )
                    """);

            // Likes
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS likes (
                            user_id INT NOT NULL,
                            track_id INT NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, track_id),
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
                        )
                    """);

            // Comments
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS comments (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            track_id INT NOT NULL,
                            content TEXT NOT NULL,
                            timestamp_seconds INT DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
                        )
                    """);
            // Follows
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS follows (
                            follower_id INT NOT NULL,
                            following_id INT NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (follower_id, following_id),
                            FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                    """);

            // Listening History — lịch sử nghe nhạc
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS listening_history (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            track_id INT NOT NULL,
                            listened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
                        )
                    """);

            // Notifications — thông báo
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS notifications (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            type VARCHAR(20) NOT NULL,
                            message TEXT NOT NULL,
                            from_user_id INT,
                            track_id INT,
                            is_read BOOLEAN DEFAULT FALSE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                        )
                    """);

            // Reposts — đăng lại bài hát
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS reposts (
                            user_id INT NOT NULL,
                            track_id INT NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, track_id),
                            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
                        )
                    """);

            System.out.println("[DB] Tao bang thanh cong");
        } catch (SQLException e) {
            System.err.println("[DB] Loi tao bang: " + e.getMessage());
        } finally {
            releaseConnection(conn);
        }
    }

    public static void shutdown() {// dong tat ca connection khi server tat
        if (connectionPool != null) {
            for (Connection conn : connectionPool) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
                ;
            }
        }
        System.out.println("[DB] Da dong tat ca cong ket noi");
    }

     public static void main(String[] args) {
     try {
     DBConnection.initialize();
     Connection conn = getConnection();
     System.out.println("Kết nối MySQL thành công!");
     conn.close();
     } catch (SQLException e) {
     System.out.println("Lỗi kết nối: " + e.getMessage());
     }
     }

}
