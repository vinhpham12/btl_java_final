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
import java.util.regex.Pattern;

/**
 * Điểm khởi đầu của TAH Backend Server.
 *
 * Sử dụng com.sun.net.httpserver.HttpServer - HTTP server tích hợp sẵn trong JDK.
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
            server.createContext("/api/users", new UserHandler());        // Profile/Follow/Reposts
            server.createContext("/api/search", new SearchHandler());     // Tìm kiếm
            server.createContext("/api/comments", new CommentHandler());  // Bình luận
            server.createContext("/api/history", new HistoryHandler());   // Lịch sử nghe
            server.createContext("/api/notifications", new NotificationHandler()); // Thông báo

            // Bước 3: Tạo thread pool 50 threads để xử lý đồng thời
            // 10 threads quá ít cho I/O-bound server (stream file lớn block thread lâu)
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(50));

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
        private final RepostHandler repostHandler = new RepostHandler();

        // Pre-compiled regex patterns — tránh compile lại mỗi request
        private static final Pattern STREAM_PATTERN = Pattern.compile("/api/tracks/\\d+/stream");
        private static final Pattern LIKE_PATTERN = Pattern.compile("/api/tracks/\\d+/like");
        private static final Pattern COMMENT_PATTERN = Pattern.compile("/api/tracks/\\d+/comments");
        private static final Pattern PLAY_PATTERN = Pattern.compile("/api/tracks/\\d+/play");
        private static final Pattern REPOST_PATTERN = Pattern.compile("/api/tracks/\\d+/repost");

        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException {
            String path = exchange.getRequestURI().getPath();

            // Dùng pre-compiled regex thay vì String.matches() (compile mỗi lần)
            if (STREAM_PATTERN.matcher(path).matches()) {
                streamHandler.handle(exchange);       // Stream file nhạc
            } else if (LIKE_PATTERN.matcher(path).matches()) {
                likeHandler.handle(exchange);         // Like/Unlike
            } else if (COMMENT_PATTERN.matcher(path).matches()) {
                commentHandler.handle(exchange);      // Bình luận
            } else if (REPOST_PATTERN.matcher(path).matches()) {
                repostHandler.handle(exchange);       // Repost/Unrepost
            } else {
                trackHandler.handle(exchange);        // CRUD track thông thường
            }
        }
    }
}