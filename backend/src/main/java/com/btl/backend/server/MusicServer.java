/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.server;

/**
 *
 * @author ADMIN
 */
import com.btl.backend.handler.*;
import com.btl.backend.util.DBConnection;
import com.btl.backend.util.FileStorageManager;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

/**
 * Điểm khởi đầu của TAH Backend Server.
 *
 * Sử dụng com.sun.net.httpserver.HttpServer - HTTP server tích hợp sẵn trong JDK.
 * Không cần Spring Boot, Tomcat hay bất kỳ framework nào.
 *
 * Luồng khởi động:
 * 1. Khởi tạo DB (connection pool + tạo bảng)
 * 2. Khởi tạo thư mục lưu file
 * 3. Đăng ký các route (URL → Handler)
 * 4. Bắt đầu lắng nghe trên port 8080
 */
public class MusicServer {

    private static final int PORT = 8081; // Port mặc định

    public static void main(String[] args) {
        try {
            System.out.println("=== TAH Backend Server ===");
            System.out.println("Initializing...");

            // Bước 1: Khởi tạo hạ tầng
            DBConnection.initialize();        // Tạo connection pool + bảng
            FileStorageManager.initialize();      // Tạo thư mục uploads/

            // Create HTTP server
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Bước 2: Đăng ký routes (URL → Handler xử lý)
            // Mỗi createContext map 1 URL prefix đến 1 handler class
            server.createContext("/api/auth", new AuthHandler());         // Đăng ký/nhập/xuất
            server.createContext("/api/tracks", new TrackRequestRouter()); // Bài hát (cần router phụ)
            server.createContext("/api/playlists", new PlaylistHandler()); // Playlist
            server.createContext("/api/users", new UserHandler());        // Profile/Follow
            server.createContext("/api/search", new SearchHandler());     // Tìm kiếm
            server.createContext("/api/comments", new CommentHandler());  // Bình luận

            // Bước 3: Tạo thread pool 10 threads để xử lý đồng thời
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));

            // Bước 4: Khởi động server
            server.start();
            System.out.println("Server started on http://localhost:" + PORT);
            System.out.println("Press Ctrl+C to stop");

            // Shutdown hook: chạy khi user nhấn Ctrl+C để dọn dẹp tài nguyên
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down...");
                server.stop(2);              // Dừng server (chờ tối đa 2s)
                DBConnection.shutdown();  // Đóng tất cả connection
                System.out.println("Server stopped.");
            }));

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Router phụ cho các request liên quan đến Track.
     *
     * Tại sao cần router này?
     * Vì HttpServer match URL theo prefix, không hỗ trợ pattern như Spring.
     * Tất cả request bắt đầu bằng "/api/tracks" đều vào đây,
     * ta phải tự phân luồng dựa trên URL pattern.
     *
     * Ví dụ:
     *   /api/tracks              → TrackHandler  (danh sách, upload)
     *   /api/tracks/5/stream     → StreamHandler (stream nhạc)
     *   /api/tracks/5/like       → LikeHandler   (like/unlike)
     *   /api/tracks/5/comments   → CommentHandler(bình luận)
     */
    static class TrackRequestRouter implements com.sun.net.httpserver.HttpHandler {
        private final TrackHandler trackHandler = new TrackHandler();
        private final StreamHandler streamHandler = new StreamHandler();
        private final LikeHandler likeHandler = new LikeHandler();
        private final CommentHandler commentHandler = new CommentHandler();

        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
            String path = exchange.getRequestURI().getPath();

            // Dùng regex để match URL pattern: \\d+ = 1 hoặc nhiều chữ số
            if (path.matches("/api/tracks/\\d+/stream")) {
                streamHandler.handle(exchange);       // Stream file nhạc
            } else if (path.matches("/api/tracks/\\d+/like")) {
                likeHandler.handle(exchange);         // Like/Unlike
            } else if (path.matches("/api/tracks/\\d+/comments")) {
                commentHandler.handle(exchange);      // Bình luận
            } else {
                trackHandler.handle(exchange);        // CRUD track thông thường
            }
        }
    }
}