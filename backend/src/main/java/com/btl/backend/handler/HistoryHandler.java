package com.btl.backend.handler;

import com.btl.backend.DAO.HistoryDAO;
import com.btl.backend.util.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.*;

/**
 * Handler cho lịch sử nghe nhạc.
 * GET /api/history — lấy danh sách lịch sử
 * POST /api/history — thêm lịch sử (khi phát nhạc)
 * DELETE /api/history — xóa toàn bộ lịch sử
 */
public class HistoryHandler implements HttpHandler {

    private final HistoryDAO historyDao = new HistoryDAO();

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

        String method = exchange.getRequestMethod();

        try {
            if ("GET".equals(method)) {
                // Lấy lịch sử nghe gần đây (tối đa 50 bài)
                List<Map<String, Object>> history = historyDao.getHistory(userId, 50);
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("status", "success");
                response.put("data", history);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.toJson(response));

            } else if ("POST".equals(method)) {
                // Ghi lại lượt nghe
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, Object> json = JsonHelper.parseJson(body);
                int trackId = JsonHelper.getInt(json, "trackId");
                if (trackId <= 0) {
                    HttpHelper.sendJsonResponse(exchange, 400, JsonHelper.errorResponse("trackId is required"));
                    return;
                }
                historyDao.addHistory(userId, trackId);
                HttpHelper.sendJsonResponse(exchange, 201, JsonHelper.successResponse("History recorded"));

            } else if ("DELETE".equals(method)) {
                // Xóa toàn bộ lịch sử
                historyDao.clearHistory(userId);
                HttpHelper.sendJsonResponse(exchange, 200, JsonHelper.successResponse("History cleared"));

            } else {
                HttpHelper.sendJsonResponse(exchange, 405, JsonHelper.errorResponse("Method not allowed"));
            }
        } catch (Exception e) {
            System.err.println("[HistoryHandler] Error: " + e.getMessage());
            HttpHelper.sendJsonResponse(exchange, 500, JsonHelper.errorResponse("Internal server error"));
        }
    }
}
