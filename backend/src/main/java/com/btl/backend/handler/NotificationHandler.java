package com.btl.backend.handler;

import com.btl.backend.DAO.NotificationDAO;
import com.btl.backend.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.*;

/**
 * Handler cho thông báo.
 * GET /api/notifications — lấy danh sách
 * GET /api/notifications/unread-count — đếm chưa đọc
 * PUT /api/notifications/read-all — đánh dấu tất cả đã đọc
 * PUT /api/notifications/{id}/read — đánh dấu 1 thông báo đã đọc
 */
public class NotificationHandler implements HttpHandler {

    private final NotificationDAO notifDao = new NotificationDAO();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpHelper.handleCors(exchange);
            return;
        }

        int userId = HttpHelper.getAuthUserId(exchange);
        if (userId == -1) {
            HttpHelper.sendJsonResponse(exchange, 401, JsonHelper.errorResponse("Not authenticated"));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if ("GET".equals(method) && path.endsWith("/unread-count")) {
                // Đếm thông báo chưa đọc
                int count = notifDao.getUnreadCount(userId);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("count", count);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.dataResponse(data));

            } else if ("GET".equals(method)) {
                // Lấy danh sách thông báo (tối đa 50)
                List<Map<String, Object>> notifications = notifDao.getNotifications(userId, 50);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("status", "success");
                response.put("data", notifications);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(response));

            } else if ("PUT".equals(method) && path.endsWith("/read-all")) {
                // Đánh dấu tất cả đã đọc
                notifDao.markAllAsRead(userId);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("All notifications marked as read"));

            } else if ("PUT".equals(method) && path.matches(".*/notifications/\\d+/read")) {
                // Đánh dấu 1 thông báo đã đọc
                String idStr = path.replaceAll(".*/notifications/(\\d+)/read", "$1");
                int notifId = Integer.parseInt(idStr);
                notifDao.markAsRead(notifId, userId);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("Notification marked as read"));

            } else {
                HttpHelper.sendJsonResponse(exchange, 405, JsonHelper.errorResponse("Method not allowed"));
            }
        } catch (Exception e) {
            System.err.println("[NotificationHandler] Error: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }
}
