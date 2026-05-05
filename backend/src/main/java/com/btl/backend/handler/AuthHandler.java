/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.btl.backend.handler;

/**
 *
 * @author ADMIN
 */
import com.btl.backend.DAO.UsersDAO;
import com.btl.backend.model.Users;
import com.btl.backend.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Xử lý các request liên quan đến Xác thực (Authentication).
 * Đường dẫn: /api/auth/*
 * 
 * Implement HttpHandler của com.sun.net.httpserver.
 * Mỗi request đến '/api/auth' sẽ gọi hàm handle() của class này.
 */
public class AuthHandler implements HttpHandler {

    private final UsersDAO userDao = new UsersDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Xử lý Preflight request của CORS (trình duyệt gửi OPTIONS trước POST/PUT/DELETE)
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpHelper.handleCors(exchange);
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Điều hướng request dựa vào URL và Method (Routing tự chế)
        try {
            if ("POST".equals(method) && path.endsWith("/register")) {
                handleRegister(exchange); // Đăng ký
            } else if ("POST".equals(method) && path.endsWith("/login")) {
                handleLogin(exchange);    // Đăng nhập
            } else if ("POST".equals(method) && path.endsWith("/logout")) {
                handleLogout(exchange);   // Đăng xuất
            } else if ("GET".equals(method) && path.endsWith("/me")) {
                handleMe(exchange);       // Lấy thông tin user hiện tại
            } else {
                // Trả về 404 nếu không tìm thấy route
                HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("Not found"));
            }
        } catch (Exception e) {
            System.err.println("[AuthHandler] Error: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }

    /** Xử lý POST /api/auth/register */
    private void handleRegister(HttpExchange exchange) throws IOException, SQLException {
        // 1. Đọc body JSON từ request
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, Object> json = JsonHelper.parseJson(body);

        // 2. Lấy các trường thông tin
        String username = JsonHelper.getString(json, "username");
        String password = JsonHelper.getString(json, "password");
        String displayName = JsonHelper.getString(json, "displayName");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            HttpHelper.sendJsonResponse(exchange, 400, JsonHelper.errorResponse("Username and password are required"));
            return;
        }
        if (displayName == null || displayName.isEmpty()) {
            displayName = username;
        }

        // 3. Kiểm tra user đã tồn tại chưa
        if (userDao.findByUsername(username) != null) {
            HttpHelper.sendJsonResponse(exchange, 409, JsonHelper.errorResponse("Username already exists"));
            return;
        }

        // 4. Mã hóa password và lưu vào DB
        String passwordHash = SessionManager.hashPassword(password);
        Users user = userDao.create(username, passwordHash, displayName);
        if (user == null) {
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Failed to create user"));
            return;
        }

        // 5. Tạo token đăng nhập ngay sau khi đăng ký thành công
        String token = SessionManager.createSession(user.getId());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);           // Token để client lưu lại
        response.put("user", user.toJson());    // Thông tin user
        
        // Trả về 201 Created cùng JSON data
        HttpHelper.sendJsonResponse(exchange, 201, JsonHelper.dataResponse(response));
    }

    /** Xử lý POST /api/auth/login */
    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, Object> json = JsonHelper.parseJson(body);

        String username = JsonHelper.getString(json, "username");
        String password = JsonHelper.getString(json, "password");

        if (username == null || password == null) {
            HttpHelper.sendJsonResponse(exchange, 400, JsonHelper.errorResponse("Username and password are required"));
            return;
        }

        // Tìm user trong DB và kiểm tra password (đã hash)
        Users user = userDao.findByUsername(username);
        if (user == null || !SessionManager.verifyPassword(password, user.getPasswordHash())) {
            HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Invalid username or password"));
            return;
        }

        String token = SessionManager.createSession(user.getId());
        // Refresh user data with counts
        user = userDao.findById(user.getId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        response.put("user", user.toJson());
        HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.dataResponse(response));
    }

    /** Xử lý POST /api/auth/logout */
    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = HttpHelper.getAuthToken(exchange); // Trích xuất từ Authorization header
        SessionManager.removeSession(token);              // Xóa khỏi RAM server
        HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Logged out"));
    }

    /** Xử lý GET /api/auth/me - Lấy thông tin user hiện tại (Dùng khi F5 trang) */
    private void handleMe(HttpExchange exchange) throws IOException {
        // Tự động kiểm tra token và trả về userId (hoặc -1 nếu token sai/hết hạn)
        int userId = HttpHelper.getAuthUserId(exchange);
        if (userId == -1) {
            HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated"));
            return;
        }
        Users user = userDao.findById(userId);
        if (user == null) {
            HttpHelper.sendJsonResponse(exchange, 404, JsonHelper.errorResponse("User not found"));
            return;
        }
        HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.dataResponse(user.toJson()));
    }
}
